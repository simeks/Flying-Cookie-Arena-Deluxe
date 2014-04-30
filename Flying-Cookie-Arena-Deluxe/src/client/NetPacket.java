package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class NetPacket {
	public static final byte RELIABLE = 0x1;
	
	public int id;
	public int peer; // Senders peer-id
	public byte flags;
	public long sentTime; // Time when the packet was last sent.
	
	public Message msg;
	
	public NetPacket(int id, int peer, byte flags, Message msg) {
		this.id = id;
		this.peer = peer;
		this.flags = flags;
		this.msg = msg;
		this.sentTime = 0;
	}
	
	public NetPacket() {
		id = -1;
		flags = 0;
		msg = null;
		sentTime = 0;
	}
	
	
	/// @brief Writes the packet to the specified stream.
	public void write(DataOutputStream s) throws IOException {
		s.writeInt(id);
		s.writeInt(peer);
		s.writeByte(flags);
		s.writeLong(sentTime);
		
		ObjectOutputStream oos = new ObjectOutputStream(s);
		oos.writeObject(msg);
	}
	
	/// @brief Reads a packet from the specified stream.
	public void read(DataInputStream s) throws IOException {
		id = s.readInt();
		peer = s.readInt();
		flags = s.readByte();
		sentTime = s.readLong();
		
		ObjectInputStream ois = new ObjectInputStream(s);
		try {
			msg = (Message)ois.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
