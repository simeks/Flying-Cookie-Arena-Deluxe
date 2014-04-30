package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.LinkedBlockingQueue;

/// @brief Class that reads incoming UDP packets.
public class NetRead implements Runnable {
	private DatagramSocket socket;
	private LinkedBlockingQueue<DatagramPacket> incomingPackets = new LinkedBlockingQueue<DatagramPacket>(); 
	
	public NetRead(int port) throws Exception {
		socket = new DatagramSocket(port);
	}
	
	@Override
	public void run() {
		byte[] recvData = new byte[1024];
		DatagramPacket recvPacket = new DatagramPacket(recvData, 1024);
		try {
			socket.receive(recvPacket);
			incomingPackets.add(recvPacket);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/// @brief Pops a packet from the incoming packet queue
	public DatagramPacket popPacket() {
		return incomingPackets.poll();
	}
}
