package client;

import java.io.Serializable;

public abstract class Message implements Serializable {
	public enum Type {
		HELLO,
		WELCOME,
		HEARTBEAT
	};
	
	Type type;
	int peer;
	
	public Message(Type type, int peer) {
		this.type = type;
		this.peer = peer;
	}
}

class HelloMessage extends Message {

	public HelloMessage(Type type, int peer) {
		super(type, peer);
		// TODO Auto-generated constructor stub
	}
	
}


