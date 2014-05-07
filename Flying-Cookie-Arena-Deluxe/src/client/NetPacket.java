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
	public static final int DEFAULT_TTL = 5000;
	
	public int id;
	public int type;
	public long sentTime; // Time when the packet was last sent.
	public long sentTimeFirst; // Time when the packet was first sent. 
	protected long TTL; // Time to live for the packet, used if is reliable. // (IP header TTL can only be set for multicast sockets in java...)
	
	
	public NetPacket(int type, int id) {
		this.type = type;
		this.id = id;
		this.sentTime = 0;
		this.sentTimeFirst = 0;
		this.TTL = -1;
	}
	
	public NetPacket(int type, int id, long TTL) {
		this.type = type;
		this.id = id;
		this.sentTime = 0;
		this.sentTimeFirst = 0;
		this.TTL = TTL;
	}
	
	public NetPacket(int type) {
		this.type = type;
		id = -1;
		sentTime = 0;
		this.sentTimeFirst = 0;
		this.TTL = -1;
	}
	
	public long getTTL() {
		return TTL;
	}
	
	public boolean hasExpired(long atTimeStampMs) {
		if(-1 < getTTL() && getTTL() < atTimeStampMs - sentTimeFirst) {
			System.out.println("derp");
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		String typeName = "?";
		if(type == 0) typeName = "MESSAGE";
		if(type == 1) typeName = "RELIABLE_MESSAGE";
		if(type == 2) typeName = "ACK";
		return "type:"+typeName+", id:"+id+", timeSentLast:"+sentTime+", timeSentFirst:"+sentTimeFirst+", TTL:"+getTTL();
	}
}

class MessagePacket extends NetPacket {
	public Message msg;
	
	public MessagePacket(int id, Message msg, boolean reliable) {
		super((reliable ? RELIABLE_MESSAGE : MESSAGE), id, (reliable ? NetPacket.DEFAULT_TTL : -1));
		this.msg = msg;
	}

	public MessagePacket(int id, Message msg, long TTL) {
		super(RELIABLE_MESSAGE, id, TTL);
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


