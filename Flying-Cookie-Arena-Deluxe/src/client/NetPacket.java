package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class NetPacket implements Serializable {
	public static final int MESSAGE = 0;
	public static final int RELIABLE_MESSAGE = 1; // Reliable messages requires an acknowledgement from the receiver.
	public static final int ACK = 2;
	
	public int id;
	public int type;
	public long sentTime; // Time when the packet was last sent.
	
	
	public NetPacket(int type, int id) {
		this.type = type;
		this.id = id;
		this.sentTime = 0;
	}
	
	public NetPacket(int type) {
		this.type = type;
		id = -1;
		sentTime = 0;
	}
	
}

class MessagePacket extends NetPacket {
	public Message msg;
	
	public MessagePacket(int id, Message msg, boolean reliable) {
		super((reliable ? RELIABLE_MESSAGE : MESSAGE), id);
		this.msg = msg;
	}
	
}

class AckPacket extends NetPacket {
	public int ackId; // Packet to acknowledge
	
	public AckPacket(int id, int ackId) {
		super(ACK, id);
		this.ackId = ackId;
	}
	
}


