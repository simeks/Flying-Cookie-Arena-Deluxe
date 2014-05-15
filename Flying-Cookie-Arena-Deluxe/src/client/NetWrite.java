package client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class NetWrite  {
	private class OutgoingPacket {
		public InetAddress addr;
		public int port;
		public NetPacket packet;
		public SessionCallback callback;

		public OutgoingPacket(InetAddress addr, int port, NetPacket packet) {
			this.addr = addr;
			this.port = port;
			this.packet = packet;
			this.callback = null;
		}
	}
	
	private DatagramSocket socket;

	private LinkedBlockingQueue<OutgoingPacket> unackedPackets = new LinkedBlockingQueue<OutgoingPacket>(); // Holds sent packets that haven't been acked yet.
	private int nextPacketId = 0;
	
	
	public NetWrite(DatagramSocket socket) {
		this.socket = socket;
	}

	public void update() {
		
		long threshold = 500; // 0.5s, TODO: Change this.
		long currentTime = System.currentTimeMillis();
		
		// Check if any packets needs resending
		for(OutgoingPacket packet : unackedPackets) {
			if((packet.packet.sentTime + threshold) < currentTime) {
				sendPacket(packet);
			}
		}
	}
	
	public void send(InetAddress destAddr, int destPort, Message msg, boolean reliable) {
		OutgoingPacket packet = new OutgoingPacket(destAddr, destPort, new MessagePacket(nextPacketId++, msg));
		sendPacket(packet);
		if(reliable) {
			unackedPackets.add(packet);
		}
	}
	
	/// @brief Acknowledges that the specified packet have been received.
	public void ackPacket(int packetId) {
		for(OutgoingPacket packet : unackedPackets) {
			if(packet.packet.id == packetId) {
				stopTrackReliable(packet);
				return;
			}
		}
	}
	
	private void stopTrackReliable(OutgoingPacket packet) {
		unackedPackets.remove(packet);
	}
	
	private void sendPacket(OutgoingPacket packet) {
		long time = System.currentTimeMillis();
		packet.packet.sentTime = time;
		if(packet.packet.sentTimeFirst == 0) {
			packet.packet.sentTimeFirst = time;
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(packet.packet);
			oos.close();
			
			byte[] data = bos.toByteArray();
			DatagramPacket udpPacket = new DatagramPacket(data, data.length, packet.addr, packet.port);
			
			socket.send(udpPacket);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
