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
	public static final int FLAG_STATIC_OWNERSHIP = 0x1; //< Entity cannot change owner, meaning it will be destroyed when owner disconnects.
	
	/// The various entity types available.
	public enum Type {
		CRATE,
		CHARACTER,
		FLAG,
		CAMP_FIRE,
		AI_CHARACTER
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
	
	/// @brief to be called from World!
	public Entity(int ownerId, World world, int entityId, Type entityType) {
		this.ownerPeer = ownerId;
		this.world = world;
		this.entityId = entityId;
		this.entityType = entityType;
		this.latestStateBuild = 0;
	}

	/// @brief Called whenever a character interacts with this entity.
	public void interact(Character character) {}
	
	/// @brief Updates this entity. Should be called once every frame.
	public abstract void update(float tpf);
	
	/// @brief Destroys this entity, called when the entity is destroyed and removed from the world.
	public abstract void destroy();
	
	/// @brief Returns the position of the object
	public abstract Vector3f getPosition();
	
	/// @brief Sets the position of this entity.
	protected abstract void setPosition(Vector3f position);
	
	/// @return The rotation of this entity.
	public abstract Quaternion getRotation();
	
	/// @brief Sets the rotation of this entity.
	protected abstract void setRotation(Quaternion rotation);
	
	/// @return The velocity of this entity.
	public abstract Vector3f getVelocity();
	
	/// @brief Sets the velocity of this entity.
	protected abstract void setVelocity(Vector3f velocity);

	/// @brief sets the collision group, world handles this when an entity is created
	/// @see World.COLLISION_GROUP_X variables
	public abstract void setCollisionGroup(int group);
	
	/// @return Spatial node in the JMonkey scene tree.
	public abstract Spatial getSpatial();
	
	/// @brief determines if the custom state (other then basic movement) have changed. 
	protected boolean hasCustomStateChanged() {
		return false;
	}
	
	/// @brief will ask for ownership for this entity 
	/// @return boolean if successfully sent the message
	public final boolean editEntity() {
		return editEntity(null);
	}

	/// @brief will ask for ownership for this entity 
	/// @param EntityCallback c  called when ownership transfer is a successes or failure
	/// @see EntityCallback
	/// @return boolean if successfully sent the message
	public final boolean editEntity(EntityCallback c) {
		if(c == null) {
			
			// build default callback that sends what we probably edited something to the other peers. 
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
		
		// we can edit if we alredy is owner
		if(isOwner()) {
			c.onCanEdit();
			return true;
		}
		
		// send ownership transfer request
		EntityRequestOwnerMessage msg = new EntityRequestOwnerMessage(entityId);
		try {
			Application.getInstance().getSession().sendToPeer(msg, getOwner(), true);
		} catch (Exception e) {
			e.printStackTrace();
			c.onFailedEdit("Failed send message");
			return false;
		}
		
		// handles the callback timeout
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

	/// @brief when a new state message is received about this entity
	public void processStateMessage(EntityStateMessage msg) {
		if(msg.customData != null) processCustomStateMessage(msg.customData);
		setPosition(msg.position);
		setRotation(msg.rotation);
		setVelocity(msg.velocity);
	}
	
	/// @brief builds a stateMessage or returns null if nothing have changed and we recently sent something. 
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
	
	/// @return True if the state has changed.
	protected final boolean hasMovementStateChanged() {
		return (!getPosition().equals(latestPosition)
				|| !getRotation().equals(latestRotation)
				|| !getVelocity().equals(latestVelocity));
	}

	/// @brief called when a new event message is received about this entity
	public final void processEventMessage(EntityEventMessage e) {
		processCustomStateMessage(e.state.customData);
	}
	
	/// @brief sends states in a reliable fashion. 
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
	
	/// @return The id of this entity.
	public int getId() {
		return entityId;
	}
	/// @return The type of this entity.
	public Type getType() {
		return entityType;
	}
	/// @return The id of the peer that owns this entity.
	public int getOwner() {
		return ownerPeer;
	}
	
	/// @brief if this peer is owner of this entity (in context of frequent regeneration ownership)
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

	/// @return Entity flags for this entity.
	/// @see FLAG_STATIC_OWNERSHIP
	public int getFlags() {
		return flags;
	}
	/// @brief Sets flags for this entity.
	/// @see FLAG_STATIC_OWNERSHIP
	public void setFlags(int flags) {
		this.flags = flags;
	}

	// Performs a test checking if the object intersects the specified ray.
	public abstract void collideWith(Ray ray, CollisionResults results);
}

/// @brief callback for entity ownership change. It is up to the entity to handle when happens when. 
interface EntityCallback {
	/// @brief when the transfer went thru
	public void onCanEdit();
	/// @brief when the ownership transfer failed
	public void onFailedEdit(String reason);
	/// @brief gets the maximum time for holding the request open before calling onFailedEdit(String) for timeout reason. 
	public int getTimeout();
}
