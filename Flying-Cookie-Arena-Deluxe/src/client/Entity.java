package client;

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
	
	public Entity(int ownerId, World world, int entityId, Type entityType) {
		this.ownerPeer = ownerId;
		this.world = world;
		this.entityId = entityId;
		this.entityType = entityType;
	}

	public abstract void update(float tpf);
	
	public abstract void destroy();
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
	public abstract void setPosition(Vector3f position);
	
	public abstract Quaternion getRotation();
	public abstract void setRotation(Quaternion rotation);
	
	public abstract Vector3f getVelocity();
	public abstract void setVelocity(Vector3f velocity);

	public abstract void setCollisionGroup(int group);
	public abstract Spatial getSpatial();
	
	public void processStateMessage(EntityStateMessage msg) {
		setPosition(msg.position);
		setRotation(msg.rotation);
		setVelocity(msg.velocity);
	}
	public EntityStateMessage buildStateMessage() {
		return new EntityStateMessage(entityId, getPosition(), getRotation(), getVelocity());
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
