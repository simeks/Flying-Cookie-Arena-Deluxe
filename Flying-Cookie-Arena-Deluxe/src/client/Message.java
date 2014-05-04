package client;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

public abstract class Message implements Serializable {
	private static final long serialVersionUID = 8351610195856354822L;

	public enum Type {
		HELLO,
		PEER_ID,
		PEER_LIST,
		CHAT_MSG
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
	
	private static final long serialVersionUID = -2521784129383032208L;

	public HelloMessage() {
		super(Type.HELLO);
	}
}

/// Sent from the master peer to a new connecting peer.
class PeerIdMessage extends Message {
	private static final long serialVersionUID = -2636395897207369412L;
	public int peerId; // New peers id.
	
	public PeerIdMessage(int peerId) {
		super(Type.PEER_ID);
		this.peerId = peerId;
	}
}

class PeerListMessage extends Message {
	private static final long serialVersionUID = -3905149137955891510L;

	public class RawPeer implements Serializable {
		private static final long serialVersionUID = 13090252435035400L;
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

class ChatMessage extends Message {
	private static final long serialVersionUID = 4685526243305385459L;
	public String message;
	
	public ChatMessage(String message) {
		super(Type.CHAT_MSG);
		this.message = message;
	}
}
