package client;

import java.net.InetAddress;

public class Peer {
	private int id;
	private InetAddress destAddr;
	private int destPort;
	private NetWrite netWrite;
	
	public Peer(int id, InetAddress destAddr, int destPort) {
		this.id = id;
		this.destAddr = destAddr;
		this.destPort = destPort;
		try {
			netWrite = new NetWrite(destAddr, destPort);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void send(Message msg, boolean reliable) {
		netWrite.send(msg, reliable);
	}
	
	public InetAddress getDestAddr() {
		return destAddr;
	}
	
	public int getDestPort() {
		return destPort;
	}
	
	public NetWrite getNetWrite() {
		return netWrite;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
}
