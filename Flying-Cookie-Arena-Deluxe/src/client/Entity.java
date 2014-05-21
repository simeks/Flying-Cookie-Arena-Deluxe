package client;

import java.io.Serializable;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public abstract class Entity {
	public enum Type {
		CRATE,
		CHARACTER,
		FLAG,
		CAMP_FIRE
	}

	
	protected int ownerPeer;
	protected int entityId;
	protected Type entityType;
	protected World world;

	private Vector3f latestPosition = new Vector3f();
	private Vector3f latestVelocity = new Vector3f();
	private Quaternion latestRotation = new Quaternion();
	private long latestStateBuild;
	private final static long MAX_STATE_SILINCE = 1000; 
	
	
	public Entity(int ownerId, World world, int entityId, Type entityType) {
		this.ownerPeer = ownerId;
		this.world = world;
		this.entityId = entityId;
		this.entityType = entityType;
		this.latestStateBuild = 0;
	}

	public abstract void update(float tpf);
	
	public abstract void destroy();
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
	protected abstract void setPosition(Vector3f position);
	
	public abstract Quaternion getRotation();
	protected abstract void setRotation(Quaternion rotation);
	
	public abstract Vector3f getVelocity();
	protected abstract void setVelocity(Vector3f velocity);

	public abstract void setCollisionGroup(int group);
	public abstract Spatial getSpatial();
	
	/// @brief determines if the custom state (other then basic movement) have changed. 
	protected boolean hasCustomStateChanged() {
		return false;
	}

	/// @brief called when the state have changed and a message to the other peers is being built. 
	protected Serializable getCustomData() {
		return null;
	}
	/// @brief called when the peer owner have new customData. Is processed before basic movement. 
	protected void processCustomStateMessage(Serializable data) { }
	
	public final void processStateMessage(EntityStateMessage msg) {
		if(msg.customData != null) processCustomStateMessage(msg.customData);
		setPosition(msg.position);
		setRotation(msg.rotation);
		setVelocity(msg.velocity);
	}
	
	public final EntityStateMessage buildStateMessage() {
		if(System.currentTimeMillis() < latestStateBuild+MAX_STATE_SILINCE
				|| (!hasMovementStateChanged() && !hasCustomStateChanged())) {
			return null;
		}
		latestPosition = getPosition().clone();
		latestRotation= getRotation().clone();
		latestVelocity = getVelocity().clone();
		Serializable data = null;
		if(hasCustomStateChanged()) {
			data = getCustomData();
		}
		return new EntityStateMessage(entityId, getPosition(), getRotation(), getVelocity(), data);
	}
	
	protected final boolean hasMovementStateChanged() {
		return (!getPosition().equals(latestPosition)
				|| !getRotation().equals(latestRotation)
				|| !getVelocity().equals(latestVelocity));
	}

	public final void processEventMessage(EntityEventMessage e) {
		processCustomStateMessage(e.customData);
	}
	public final boolean sendEventMessage() {
		EntityEventMessage msg = new EntityEventMessage(entityId, getCustomData());
		Session session = Application.getInstance().getSession();
		try {
			session.sendToAll(msg, true);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
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
		return (ownerPeer == Application.getInstance().getSession().getMyPeerId());
	}


}
