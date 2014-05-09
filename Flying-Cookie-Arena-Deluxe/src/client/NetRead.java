package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NetRead implements Runnable {
	public class ReceivedMessage implements Delayed {
		final public static int MAX_TTL = 5000;
		public InetAddress senderAddr;
		public int senderPort;
		public long timeReceived;
		public Message msg;
		
		ReceivedMessage(InetAddress senderAddr, int senderPort, Message msg) {
			this.senderAddr = senderAddr;
			this.senderPort = senderPort;
			this.msg = msg;
			this.timeReceived = System.currentTimeMillis();
		}

		@Override
		public int compareTo(Delayed d) {
			return (int) (getDelay(TimeUnit.MILLISECONDS)-d.getDelay(TimeUnit.MILLISECONDS));
		}

		@Override
		public long getDelay(TimeUnit u) {
			return getReadDelay()-(System.currentTimeMillis()-timeReceived);
		}
	}
	
	private DatagramSocket socket;
	private NetWrite netWrite;
	private DelayQueue<ReceivedMessage> incomingMessages = new DelayQueue<ReceivedMessage>();
	
	// all missed that is expected to come, based on packet.id. each string ip:port holds Int packet.id and 
	// long time ms detected missing. the highest packet.id is latest received for that in:port. 
	private ConcurrentHashMap<String, ConcurrentSkipListMap<Integer, Long>> latestReliables = 
			new ConcurrentHashMap<String, ConcurrentSkipListMap<Integer, Long>>();
	private ConcurrentHashMap<String, ConcurrentSkipListMap<Long, ArrayList<Integer>>> latestReliablesFlipped = 
			new ConcurrentHashMap<String, ConcurrentSkipListMap<Long, ArrayList<Integer>>>(); // for optimization
	private long timestampLatestClared = 0;
	private volatile boolean quit = false;
	private long readDelay = 0; // for testing and debugging
	
	public NetRead(DatagramSocket socket, NetWrite netWrite) {
		this.socket = socket;
		this.netWrite = netWrite;
	}
	
	@Override
	public void run() {
		try {
			while(!quit) {
				byte[] recvData = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvData, 1024);
				socket.receive(recvPacket);

				parsePacket(recvPacket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			socket.close();
		}
	}
	
	public long getReadDelay() {
		return readDelay;
	}
	public void setReadDelay(long delay) {
		this.readDelay = delay;
	}
	
	/// @brief Pops a packet from the incoming packet queue
	public ReceivedMessage popMessage() {
		return incomingMessages.poll();
	}
	
	public void stop() {
		quit = true;
	}
	
	private void sendAck(InetAddress destAddr, int destPort, int packetId) {
		AckPacket packet = new AckPacket(0, packetId);
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			
			oos.writeObject(packet);
			oos.close();
			
			byte[] data = bos.toByteArray();
			DatagramPacket udpPacket = new DatagramPacket(data, data.length, destAddr, destPort);

			socket.send(udpPacket);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void parsePacket(final DatagramPacket udpPacket) {
		NetPacket packet = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(udpPacket.getData()));
			
			packet = (NetPacket) ois.readObject();
			ois.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(packet.type == NetPacket.RELIABLE_MESSAGE) {
			long timeStamp = System.currentTimeMillis();
			if(hasBeenProcessed(packet, udpPacket.getAddress().getHostAddress()+":"+udpPacket.getPort(), timeStamp)) {
				return;
			}
			if(timestampLatestClared + 5000 < timeStamp) {
				cleanReliable(timeStamp-5000);
				timestampLatestClared = timeStamp;
			}
		}
		
		if(packet.type == NetPacket.MESSAGE || packet.type == NetPacket.RELIABLE_MESSAGE) {
			incomingMessages.add(new ReceivedMessage(udpPacket.getAddress(), udpPacket.getPort(), ((MessagePacket)packet).msg));
		}
		else if(packet.type == NetPacket.ACK) {
			if(getReadDelay() > 0) {
				final int ackId = ((AckPacket)packet).ackId;

				new Timer().schedule(new TimerTask() {          
				    @Override
				    public void run() {
				    	netWrite.ackPacket(ackId);
				    }
				}, getReadDelay());
			} else {
				netWrite.ackPacket(((AckPacket)packet).ackId);
			}
		}
		
		if(packet.type == NetPacket.RELIABLE_MESSAGE) {
			if(getReadDelay() > 0) {
				final int packetId = packet.id;
				new Timer().schedule(new TimerTask() {          
				    @Override
				    public void run() {
				    	sendAck(udpPacket.getAddress(), udpPacket.getPort(), packetId);
				    }
				}, getReadDelay());
			} else {
				sendAck(udpPacket.getAddress(), udpPacket.getPort(), packet.id);
			}
		}
	}
	
	/// @brief checks if this packet already have been processed. 
	private boolean hasBeenProcessed(NetPacket packet, String id, long timeStamp) {
		//timeStamp = timeStamp*1000; // room for 999 packets each ms from this peer
		
		ArrayList<Integer> arr = new ArrayList<Integer>();
		
		if(latestReliables.containsKey(id)) {
			ConcurrentSkipListMap<Integer, Long> map = latestReliables.get(id);
			ConcurrentSkipListMap<Long, ArrayList<Integer>> mapFlipped = latestReliablesFlipped.get(id);
			int lastRecievedId = map.lastKey();
			long lastRecievedTime = map.get(lastRecievedId);
			
			// if packet is next in order
			if(lastRecievedId+1 == packet.id) {
				map.remove(lastRecievedId);
				mapFlipped.remove(lastRecievedTime);
				
			// if packet was processed last
			} else  if(lastRecievedId == packet.id) {
				return true;
			
			// if they between lastRecivedId and this was missed
			} else  if(lastRecievedId < packet.id) {
				
				map.remove(lastRecievedId);
				mapFlipped.remove(lastRecievedTime);

				
				for(int i = lastRecievedId+1; i < packet.id; i++) {
					map.put(i, lastRecievedTime);
					arr.add(i);
				}
				mapFlipped.put(lastRecievedTime, arr);
				arr = new ArrayList<Integer>();
				
			// if packet was missed
			} else if(map.containsKey(packet.id)) { 
				map.remove(id);
				mapFlipped.remove(id);
				return false; // is not the latest
				
			// if packet was processed a long time ago
			} else { 
				return true;
			}


			if(lastRecievedTime > timeStamp) {
				timeStamp = lastRecievedTime+1;
			}

			arr.add(packet.id);
			map.put(packet.id, timeStamp);
			mapFlipped.put(timeStamp, arr);
			latestReliables.put(id, map);
			latestReliablesFlipped.put(id, mapFlipped);
			return false;
		}
		
		// have never received from this ip:port recently.
		ConcurrentSkipListMap<Integer, Long> map = new ConcurrentSkipListMap<Integer, Long>();
		map.put(packet.id, timeStamp);
		latestReliables.put(id, map); // fill with last received. 
		
		ConcurrentSkipListMap<Long, ArrayList<Integer>> mapFlipped = new ConcurrentSkipListMap<Long, ArrayList<Integer>>();
		arr.add(packet.id);
		mapFlipped.put(timeStamp, arr);
		latestReliablesFlipped.put(id, mapFlipped);
		return false;
	}
	/// @brief removes still tracking reliable packets.
	private void cleanReliable(long timeStamp) {
		for (Map.Entry<String, ConcurrentSkipListMap<Integer, Long>> e : latestReliables.entrySet()) {
		    ConcurrentSkipListMap<Integer, Long> map = e.getValue();
		    if(map.size() < 1) {
			    ConcurrentSkipListMap<Long, ArrayList<Integer>> mapFlipped = latestReliablesFlipped.get(e.getKey());
			    ConcurrentNavigableMap<Long, ArrayList<Integer>> old = mapFlipped.headMap(timeStamp);
				for (Entry<Long, ArrayList<Integer>> oldEntety : old.entrySet()) {
					for (Integer s : oldEntety.getValue()) {
						map.remove(oldEntety.getValue());
					}
					mapFlipped.remove(oldEntety.getKey());
				}
				latestReliables.get(e.getKey()).putAll(map);
				latestReliablesFlipped.get(e.getKey()).putAll(mapFlipped);
		    }
		}
	}
}
