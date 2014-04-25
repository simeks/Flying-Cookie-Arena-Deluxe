package client;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;

public interface Entity {
	
	// Performs a test checking if the object intersects the specified ray.
	public abstract void collideWith(Ray ray, CollisionResults results);
	
	// Returns the position of the object
	public abstract Vector3f getPosition();
}
