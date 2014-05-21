package client;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import javax.naming.directory.BasicAttributes;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public abstract class Message implements Serializable {
	private static final long serialVersionUID = 8351610195856354822L;

	public enum Type {
		HELLO,
		PEER_ID,
		PEER_LIST,
		
		PING,
		PONG,
		PEER_TIMED_OUT, // Notify all other peers about a lost peer
		KICKED, // Sent to a kicked peer
		
		CHAT_MSG,
		CREATE_ENTITY,
		DESTROY_ENTITY,
		ENTITY_STATE,
		ENTITY_EVENT
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

class PingMessage extends Message {
	private static final long serialVersionUID = -1182365839943998551L;

	public PingMessage() {
		super(Type.PING);
	}
}

class PongMessage extends Message {
	private static final long serialVersionUID = 5844168245440896356L;

	public PongMessage() {
		super(Type.PONG);
	}
}

class PeerTimeOutMessage extends Message {
	private static final long serialVersionUID = 3688046389297696524L;
	public int timedOutPeerId;
	public PeerTimeOutMessage(int timedOutPeerId) {
		super(Type.PEER_TIMED_OUT);
		this.timedOutPeerId = timedOutPeerId;
	}
}

class KickedMessage extends Message {
	private static final long serialVersionUID = 1217488624618728877L;
	public String reason;
	public KickedMessage(String reason) {
		super(Type.KICKED);
		this.reason = reason;
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

class CreateEntityMessage extends Message {
	private static final long serialVersionUID = 2358686921269512335L;
	public int entityId;
	public Entity.Type entityType;
	public Vector3f position;
	public Quaternion rotation;
	public Serializable customData;
	
	/// TODO public EntityStateMessage state;
	
	CreateEntityMessage(int entityId, Entity.Type entityType, Vector3f position, Quaternion rotation) {
		super(Type.CREATE_ENTITY);
		this.entityId = entityId;
		this.entityType = entityType;
		this.position = position;
		this.rotation = rotation;
	}
}

class DestroyEntityMessage extends Message {
	private static final long serialVersionUID = -662257788394323903L;
	public int entityId;
	
	DestroyEntityMessage(int entityId) {
		super(Type.DESTROY_ENTITY);
		this.entityId = entityId;
	}
}

class EntityStateMessage extends Message {
	private static final long serialVersionUID = -6643225186660773294L;

	public int entityId;
	public Vector3f position;
	public Quaternion rotation;
	public Vector3f velocity;
	public Serializable customData;
	public long timestamp;
	
	EntityStateMessage(int entityId, Vector3f position, Quaternion rotation, Vector3f velocity) {
		super(Type.ENTITY_STATE);
		this.entityId = entityId;
		this.position = position;
		this.rotation = rotation;
		this.velocity = velocity;
		this.timestamp = System.currentTimeMillis();
	}
	EntityStateMessage(int entityId, Vector3f position, Quaternion rotation, Vector3f velocity, Serializable customData) {
		super(Type.ENTITY_STATE);
		this.entityId = entityId;
		this.position = position;
		this.rotation = rotation;
		this.velocity = velocity;
		this.customData = customData;
		this.timestamp = System.currentTimeMillis();
	}
}

class EntityEventMessage extends Message {
	private static final long serialVersionUID = -7263225364667973224L;
	public Serializable customData;
	public int entityId;
	
	public EntityEventMessage(int entityId, Serializable customData) {
		super(Type.ENTITY_EVENT);
		this.customData = customData;
		this.entityId = entityId;
	}
	
}

/// TODO EntityEventMessage 