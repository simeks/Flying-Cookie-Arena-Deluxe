package client;

import java.net.InetAddress;

public class Peer {
	private int id;
	private NetWrite netWrite;
	private InetAddress addr;
	private int port;
	private String peerName;
	
	private long lastHeartbeat; // When did we last communicate with this peer?
	private long lastPing = -1; // When did we last send a ping to this peer.
	
	public Peer(int id, NetWrite netWrite, InetAddress addr, int port) {
		this.id = id;
		this.netWrite = netWrite;
		this.addr = addr;
		this.port = port;
		
		lastHeartbeat = System.currentTimeMillis();
		lastPing = 0;
	}
	
	/* Another constructor so user can choose a nickname
	 *TODO : eventually replace default constructor with this one later on
	 */
	public Peer(String peerName, int id, NetWrite netWrite, InetAddress addr, int port) {
		this.peerName = peerName;
		this.id = id;
		this.netWrite = netWrite;
		this.addr = addr;
		this.port = port;
		
		lastHeartbeat = System.currentTimeMillis();
		lastPing = 0;
	}
	
	public void send(Message msg, boolean reliable) {
		netWrite.send(addr, port, msg, reliable);
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
	
	public void setPeerName(String name){
		this.peerName = name;
	}
	
	public String getPeerName(){
		return peerName;
	}

	public boolean equals(Peer p) {
		if(p.getDestAddr().getHostAddress().equals(this.getDestAddr().getHostAddress())
				&& p.getDestPort() == this.getDestPort()) {
			return true;
		}
		return false;
	}
	
	public void heartbeat() {
		lastHeartbeat = System.currentTimeMillis();
	}
	
	public long getLastHeartbeat() {
		return lastHeartbeat;
	}
	
	public void setLastPing(long lastPing) {
		this.lastPing = lastPing;
	}
	public long getLastPing() {
		return lastPing;
	}
	
	
}
