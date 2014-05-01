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
import java.util.concurrent.LinkedBlockingQueue;

public class NetRead implements Runnable {
	public class ReceivedMessage {
		public InetAddress senderAddr;
		public int senderPort;
		
		public Message msg;
		
		ReceivedMessage(InetAddress senderAddr, int senderPort, Message msg) {
			this.senderAddr = senderAddr;
			this.senderPort = senderPort;
			this.msg = msg;
		}
	}
	
	private DatagramSocket socket;
	private NetWrite netWrite;
	private LinkedBlockingQueue<ReceivedMessage> incomingMessages = new LinkedBlockingQueue<ReceivedMessage>(); 
	private LinkedBlockingQueue<Integer> incomingAcks = new LinkedBlockingQueue<Integer>(); 
	private volatile boolean quit = false;
	
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
	
	private void parsePacket(DatagramPacket udpPacket) {
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
		
		if(packet.type == NetPacket.MESSAGE || packet.type == NetPacket.RELIABLE_MESSAGE) {
			incomingMessages.add(new ReceivedMessage(udpPacket.getAddress(), udpPacket.getPort(), ((MessagePacket)packet).msg));
		}
		else if(packet.type == NetPacket.ACK) {
			netWrite.ackPacket(((AckPacket)packet).ackId);
		}
		
		if(packet.type == NetPacket.RELIABLE_MESSAGE) {
			sendAck(udpPacket.getAddress(), udpPacket.getPort(), packet.id);
		}
	}
}
