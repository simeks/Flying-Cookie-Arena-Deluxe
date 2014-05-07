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
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NetRead implements Runnable {
	public class ReceivedMessage implements Delayed {
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
		public long getDelay(TimeUnit arg0) {
			return getReadDelay()-(System.currentTimeMillis()-timeReceived);
		}
	}
	
	private DatagramSocket socket;
	private NetWrite netWrite;
	private DelayQueue<ReceivedMessage> incomingMessages = new DelayQueue<ReceivedMessage>(); 
	private LinkedBlockingQueue<Integer> incomingAcks = new LinkedBlockingQueue<Integer>();
	private volatile boolean quit = false;
	private long readDelay = 0;
	
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
		try {
			AckPacket packet = new AckPacket(0, packetId);
			
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
			if(isReliablePacketProcessed(packet)) {
				return;
			}
		}
		
		if(packet.type == NetPacket.MESSAGE || packet.type == NetPacket.RELIABLE_MESSAGE) {
			incomingMessages.add(new ReceivedMessage(udpPacket.getAddress(), udpPacket.getPort(), ((MessagePacket)packet).msg));
		}
		else if(packet.type == NetPacket.ACK) {
			if(getReadDelay() > 0) {
				final int ackId = ((AckPacket)packet).ackId;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							TimeUnit.MILLISECONDS.sleep(getReadDelay());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						netWrite.ackPacket(ackId);
					}
				}).start();
			} else {
				netWrite.ackPacket(((AckPacket)packet).ackId);
			}
		}
		
		if(packet.type == NetPacket.RELIABLE_MESSAGE) {
			if(getReadDelay() > 0) {
				final int packetId = packet.id;
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							TimeUnit.MILLISECONDS.sleep(getReadDelay());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						sendAck(udpPacket.getAddress(), udpPacket.getPort(), packetId);
					}
				}).start();
			} else {
				sendAck(udpPacket.getAddress(), udpPacket.getPort(), packet.id);
			}
		}
	}
	
	private boolean isReliablePacketProcessed(NetPacket packet) {
		if(packet.hasExpired(System.currentTimeMillis())) {
			return true; // packet is no longer relevant. 
		}
		// TODO: check if packet is processed somehow... 
		return false;
	}
}
