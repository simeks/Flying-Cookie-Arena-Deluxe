package client;

import java.net.InetAddress;

public class Peer {
	private int id;
	private NetWrite netWrite;
	private InetAddress addr;
	private int port;
	private String peerName; //TODO this is not used ATM, can be used in a GUI upgrade in the future
	
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
	
	/* send a message (reliable if needed) */
	public void send(Message msg, boolean reliable) {
		netWrite.send(addr, port, msg, reliable);
	}
	/* get the destination internet address */
	public InetAddress getDestAddr() {
		return addr;
	}
	
	/* returns the portnumber of the peer */
	public int getDestPort() {
		return port;
	}
	/* returns the netwrite object of a peer */
	public NetWrite getNetWrite() {
		return netWrite;
	}
	/* get the id of a peer */
	public int getId() {
		return id;
	}
	/* set id of a peer */
	public void setId(int id) {
		this.id = id;
	}
	/* set the peer name */
	public void setPeerName(String name){
		this.peerName = name;
	}
	
	/* returns the name of the peer */
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
