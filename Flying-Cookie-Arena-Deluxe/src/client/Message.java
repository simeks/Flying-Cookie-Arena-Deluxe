package client;

import java.io.Serializable;
import java.net.InetAddress;

public abstract class Message implements Serializable {
	public enum Type {
		HELLO,
		YOUR_PEER_ID,
		PEER_LIST
	};
	
	Type type;
	
	public Message(Type type) {
		this.type = type;
	}
}

/// Message sent from a new peer to the master peer when connecting.
class HelloMessage extends Message {
	InetAddress addr;
	int port;
	
	public HelloMessage(InetAddress addr, int port) {
		super(Type.HELLO);
		this.addr = addr;
		this.port = port;
	}
}

/// Sent from the master peer to a new connecting peer.
class YourPeerIdMessage extends Message {
	int peerId; // New peers id.
	int masterPeerId; // Id of master peer.
	
	public YourPeerIdMessage(int peerId, int masterPeerId) {
		super(Type.HELLO);
		this.peerId = peerId;
		this.masterPeerId = masterPeerId;
	}
}

