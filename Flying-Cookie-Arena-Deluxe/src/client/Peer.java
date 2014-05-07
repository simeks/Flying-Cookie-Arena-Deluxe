package client;

import java.net.InetAddress;

public class Peer {
	private int id;
	private NetWrite netWrite;
	private InetAddress addr;
	private int port;
	
	
	public Peer(int id, NetWrite netWrite, InetAddress addr, int port) {
		this.id = id;
		this.netWrite = netWrite;
		this.addr = addr;
		this.port = port;
	}
	
	public void send(Message msg, boolean reliable) {
		netWrite.send(addr, port, msg, reliable);
	}
	public void send(Message msg, int TTL, SessionReliableCallback callback) {
		netWrite.send(addr, port, msg, TTL, callback);
	}
	
	public InetAddress getDestAddr() {
		return addr;
	}
	
	public int getDestPort() {
		return port;
	}
	
	public NetWrite getNetWrite() {
		return netWrite;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
}
