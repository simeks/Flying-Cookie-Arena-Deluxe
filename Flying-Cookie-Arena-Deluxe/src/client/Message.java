package client;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

public abstract class Message implements Serializable {
	public enum Type {
		HELLO,
		PEER_ID,
		PEER_LIST
	};
	
	Type type;
	int peer; // Peer that sent this message
	
	public Message(Type type) {
		this.type = type;
		this.peer = Application.getInstance().getSession().getMyPeerId();
	}
}

/// Message sent from a new peer to the master peer when connecting.
class HelloMessage extends Message {
	
	public HelloMessage() {
		super(Type.HELLO);
	}
}

/// Sent from the master peer to a new connecting peer.
class PeerIdMessage extends Message {
	public int peerId; // New peers id.
	
	public PeerIdMessage(int peerId) {
		super(Type.PEER_ID);
		this.peerId = peerId;
	}
}

class PeerListMessage extends Message {
	public class RawPeer implements Serializable {
		public int peerId;
		public InetAddress addr;
		public int port;
		
		RawPeer(int peerId, InetAddress addr, int port) {
			this.peerId = peerId;
			this.addr = addr;
			this.port = port;
		}
	}
	public ArrayList<RawPeer> peers = new ArrayList<RawPeer>();
	
	public PeerListMessage() {
		super(Type.PEER_LIST);
	}
}
