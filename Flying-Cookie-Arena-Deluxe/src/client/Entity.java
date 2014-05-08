package client;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public abstract class Entity {
	public enum Type {
		CRATE,
		CHARACTER,
		CAMP_FIRE
	};
	
	protected int ownerPeer;
	protected int entityId;
	protected Type entityType;
	protected World world;
	
	public Entity(int ownerId, World world, int entityId, Type entityType) {
		this.ownerPeer = ownerId;
		this.world = world;
		this.entityId = entityId;
		this.entityType = entityType;
	}

	public abstract void update(float tpf);
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
	public abstract void setPosition(Vector3f position);
	
	public abstract Quaternion getRotation();
	public abstract void setRotation(Quaternion rotation);
	
	public void processStateMessage(EntityStateMessage msg) {
		setPosition(msg.position);
		setRotation(msg.rotation);
	}
	public EntityStateMessage buildStateMessage() {
		return new EntityStateMessage(entityId, getPosition(), getRotation());
	}
	
	public int getId() {
		return entityId;
	}
	public Type getType() {
		return entityType;
	}
	public int getOwner() {
		return ownerPeer;
	}
	
	public void setOwner(int peerId) {
		ownerPeer = peerId;
	}
	
	/// @brief Returns true if the current peer owns this entity.
	public boolean isOwner() {
		System.out.println("E " + ownerPeer + "; S " + Application.getInstance().getSession().getMyPeerId());
		return (ownerPeer == Application.getInstance().getSession().getMyPeerId());
	}
}
