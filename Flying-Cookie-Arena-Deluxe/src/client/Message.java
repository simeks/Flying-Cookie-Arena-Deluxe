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


