package client;

import java.awt.Event;
import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

import client.Session.State;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public abstract class Entity {
	public static final int FLAG_STATIC_OWNERSHIP = 0x1; // Entity cannot change owner, meaning it will be destroyed when owner disconnects.
	
	
	public enum Type {
		CRATE,
		CHARACTER,
		FLAG,
		CAMP_FIRE
	}
	
	protected int ownerPeer;
	protected int entityId;
	protected Type entityType;
	protected int flags = 0;
	protected World world;

	private Vector3f latestPosition = new Vector3f();
	private Vector3f latestVelocity = new Vector3f();
	private Quaternion latestRotation = new Quaternion();
	private long latestStateBuild;
	private final static long MAX_STATE_SILINCE = 1000; 
	
	private EntityCallback callback = null;
	
	
	public Entity(int ownerId, World world, int entityId, Type entityType) {
		this.ownerPeer = ownerId;
		this.world = world;
		this.entityId = entityId;
		this.entityType = entityType;
		this.latestStateBuild = 0;
	}

	/// @brief Called whenever a character interacts with an entity.
	public void interact(Character character) {}
	
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
	
	public final boolean editEntity() {
		return editEntity(null);
	}
	public final boolean editEntity(EntityCallback c) {
		if(c == null) {
			
			// build default callback that sends what we probably edited to the other peers. 
			c = new EntityCallback() {
				
				@Override
				public void onFailedEdit(String reason) { }
				
				@Override
				public void onCanEdit() {
					sendEventMessage();
				}
				
				@Override
				public int getTimeout() { return 0; }
			};
		}
		if(isOwner()) {
			c.onCanEdit();
			return true;
		}
		
		EntityRequestOwnerMessage msg = new EntityRequestOwnerMessage(entityId);
		
		try {
			Application.getInstance().getSession().sendToPeer(msg, getOwner(), true);
		} catch (Exception e) {
			e.printStackTrace();
			c.onFailedEdit("Failed send message");
			return false;
		}
		
		setCallback(c);
		if(c.getTimeout() > 0) {
			final EntityCallback finalCallback = c;
			new Timer().schedule(new TimerTask() {
			    @Override
			    public void run() {
			    	if(!isOwner()) {
			    		finalCallback.onFailedEdit("Timed out");
			    	}
			    }
			}, c.getTimeout());
		}
		return true;
	}
	
	private synchronized EntityCallback getCallback() {
		return callback;
	}
	private synchronized void setCallback(EntityCallback c) {
		callback = c;
	}

	/// @brief called when the state have changed and a message to the other peers is being built. Overwrite me! 
	protected Serializable getCustomData() {
		return null;
	}
	/// @brief called when the peer owner have new customData. Is processed before basic movement. 
	protected void processCustomStateMessage(Serializable data) { }
	
	public void processStateMessage(EntityStateMessage msg) {
		if(msg.customData != null) processCustomStateMessage(msg.customData);
		setPosition(msg.position);
		setRotation(msg.rotation);
		setVelocity(msg.velocity);
	}
	
	public final EntityStateMessage buildStateMessage() {
		long timestamp = System.currentTimeMillis();
		boolean updateState = (latestStateBuild+MAX_STATE_SILINCE < timestamp);
		if(!updateState && !hasMovementStateChanged() && !hasCustomStateChanged()) {
			return null;
		}
		latestPosition = getPosition().clone();
		latestRotation= getRotation().clone();
		latestVelocity = getVelocity().clone();
		Serializable data = null;
		if(updateState || hasCustomStateChanged()) {
			data = getCustomData();
		}
		latestStateBuild = timestamp;
		return new EntityStateMessage(entityId, getPosition(), getRotation(), getVelocity(), data);
	}
	
	protected final boolean hasMovementStateChanged() {
		return (!getPosition().equals(latestPosition)
				|| !getRotation().equals(latestRotation)
				|| !getVelocity().equals(latestVelocity));
	}

	public final void processEventMessage(EntityEventMessage e) {
		processCustomStateMessage(e.state.customData);
	}
	public final boolean sendEventMessage() {
		EntityEventMessage msg = new EntityEventMessage(
			new EntityStateMessage(entityId, getPosition(), getRotation(), getVelocity(), getCustomData())
		);
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
		
		EntityCallback c = getCallback();
		if(isOwner() && c != null) {
			c.onCanEdit();
		}
	}
	
	/// @brief Returns true if the current peer owns this entity.
	public boolean isOwner() {
		return (ownerPeer == Application.getInstance().getSession().getMyPeerId());
	}

	public int getFlags() {
		return flags;
	}
	public void setFlags(int flags) {
		this.flags = flags;
	}

	// Performs a test checking if the object intersects the specified ray.
	public abstract void collideWith(Ray ray, CollisionResults results);
}

interface EntityCallback {
	public void onCanEdit();
	public void onFailedEdit(String reason);
	public int getTimeout();
}
