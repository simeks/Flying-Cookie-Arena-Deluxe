package client;

import com.jme3.math.Vector3f;

public abstract class Entity {
	public enum Type {
		CRATE,
		CHARACTER
	};
	
	protected int id;
	protected World world;
	protected Vector3f position;
	
	public Entity(World world) {
		this.world = world;
	}
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
}
