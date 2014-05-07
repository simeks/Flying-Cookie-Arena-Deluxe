package client;

import com.jme3.math.Vector3f;

public abstract class Entity {
	public enum Type {
		CRATE,
		CHARACTER,
		CAMP_FIRE
	};
	
	protected int entityId;
	protected World world;
	
	public Entity(World world, int entityId) {
		this.world = world;
		this.entityId = entityId;
	}
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
	public abstract void setPosition(Vector3f position);
	
	public void parseStateMessage(EntityStateMessage msg) {
		setPosition(msg.position);
	}
	public EntityStateMessage buildStateMessage() {
		return new EntityStateMessage(entityId, getPosition());
	}
	
}
