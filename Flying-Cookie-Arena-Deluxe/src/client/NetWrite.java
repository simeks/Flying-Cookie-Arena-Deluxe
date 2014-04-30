package client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/// @brief Class handling connection to a specific destination.
public class NetWrite {
	private InetAddress destAddr;
	private int destPort;
	
	private DatagramSocket socket;
	
	private ArrayList<NetPacket> unackedPackets = new ArrayList<NetPacket>(); // Holds sent packets that haven't been acked yet.
	private int nextPacketId = 0;
	
	/// @brief Constructor
	/// @param addr Destination address.
	/// @param port Destination port.
	public NetWrite(InetAddress destAddr, int destPort) throws Exception {
		this.destAddr = destAddr;
		this.destPort = destPort;
		
		socket = new DatagramSocket();
		
	}
	
	public void update() {
		
		long threshold = 500; // 0.5s, TODO: Change this.
		long currentTime = System.currentTimeMillis();
		
		// Check if any packets needs resending
		for(NetPacket packet : unackedPackets) {
			if((packet.sentTime + threshold) < currentTime) {
				sendPacket(packet);
			}
		}
	}
	
	public void send(Message msg, boolean reliable) {
		byte flags = 0;
		if(reliable) {
			flags |= NetPacket.RELIABLE;
		}
		
		NetPacket packet = new NetPacket(nextPacketId++, Application.getInstance().getSession().getMyPeerId(), flags, msg);
		sendPacket(packet);
		
		if(reliable) {
			unackedPackets.add(packet);
		}
			
	}
	
	public void sendAck(int packetId) {
		
	}
	
	/// @brief Acknowledges that the specified packet have been received.
	public void ackPacket(int packetId) {
		for(NetPacket packet : unackedPackets) {
			if(packet.id == packetId) {
				unackedPackets.remove(packet);
				return;
			}
		}
	}
	
	private void sendPacket(NetPacket packet) {
		try {
			packet.sentTime = System.currentTimeMillis();
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			packet.write(new DataOutputStream(bos));
			
			byte[] data = bos.toByteArray();
			DatagramPacket udpPacket = new DatagramPacket(data, data.length, destAddr, destPort);
			socket.send(udpPacket);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
