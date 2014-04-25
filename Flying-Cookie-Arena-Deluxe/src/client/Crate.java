package client;

import java.util.Random;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

// Simple crate primitive with texturing and normal mapping.
public class Crate implements Entity {
	// Mass of each crate
	static final float mass = 5.0f;
	
	// Initial size of each crate
	static final float size = 5.0f;
	
	// All crates will share the same mesh.
	static final private Box boxMesh = new Box(size*0.5f, size*0.5f, size*0.5f);
	
	private BulletAppState bulletAppState;
	private Geometry geometry;
	private Material material;
	private RigidBodyControl rigidBodyControl;
	private Node roomNode;
	
	
	public Geometry getGeometry()
	{
		return geometry;
	}

	// Called when the user interacts with this object.
	public void interact()
	{
		// Not interactable so do nothing
	}
	// Returns the tool-tip for this object.
	public String getToolTip()
	{
		return "Press [1,2,3] to select different tools.";
	}

	// Performs a test checking if the object intersects the specified ray.
	public void collideWith(Ray ray, CollisionResults results)
	{
		geometry.collideWith(ray, results);
	}
	public Vector3f getPosition()
	{
		return geometry.getWorldTranslation();
	}
	
	
	// Constructor: Creates a crate at the specified position and attaches it to the specified scene node.
	public Crate(BulletAppState bulletAppState, AssetManager assetManager, Node roomNode, Vector3f position)
	{
		this.roomNode = roomNode;
		this.bulletAppState = bulletAppState;
		
		// Create a new node with a new instance of the shared box mesh created earlier.
		geometry = new Geometry("Box", boxMesh);
		geometry.move(position);
    	

		// Load the diffuse texture
		Texture diffuseTexture = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");

		// Load the normal texture
		Texture normalTexture = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall_normal.jpg");	
    	
    	// Create a new material for the box and enable light shading.
		material = new Material(assetManager, 
				"Common/MatDefs/Light/Lighting.j3md");
		
		// Material properties
		material.setTexture("DiffuseMap", diffuseTexture);
		material.setTexture("NormalMap", normalTexture);
		material.setBoolean("UseMaterialColors",true);
		
		// The crates uses the same texture as the wall so randomize the colors to make it more distinct.
		Random rand = new Random();
		ColorRGBA color = new ColorRGBA(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1.0f);
		material.setColor("Diffuse", color);
		material.setColor("Ambient", color);
		
		material.setColor("Specular", ColorRGBA.White);
		material.setFloat("Shininess", 64.0f);
	    
		geometry.setMaterial(material);
    	
		// Attach node to the room scene node.
		this.roomNode.attachChild(geometry);
    	
        
        // Setup physics properties
        rigidBodyControl = new RigidBodyControl(mass);
        geometry.addControl(rigidBodyControl);
        bulletAppState.getPhysicsSpace().add(rigidBodyControl);
	}
	
	public void destroy()
	{
		bulletAppState.getPhysicsSpace().remove(rigidBodyControl);
	}
	
	// Notifies this object that it has been picked up, character is the character that picked the object up.
	public void pickup(Character character)
	{
		// Disable the physics simulation on this object while its beeing held
        rigidBodyControl.setKinematic(true);
        
        // Detach from the object from current node and attach it to the character
        roomNode.detachChild(geometry);
        character.getNode().attachChild(geometry);
        
        geometry.setLocalTranslation(0, 3, 12);
	}
	// Notifies this object that it has been dropped
	public void drop(Character character)
	{
		// We will first need to recalculate the local transformations for the node so they are relative to the room node
		//	and not the character.
		Vector3f translation = geometry.getWorldTranslation(); // World translation
		translation = translation.subtract(roomNode.getLocalTranslation()); // Local translation (Relative to room)
		
		Quaternion rot = geometry.getWorldRotation(); // World rotation
		rot = roomNode.getWorldRotation().inverse().mult(rot); // Local rotation (Relative to room)
		
		// Detach it from the character and put it back to the room node again
		character.getNode().detachChild(geometry);
		roomNode.attachChild(geometry);
		
		geometry.setLocalTranslation(translation);
		geometry.setLocalRotation(rot);
		
		
		// Enable physics again
        rigidBodyControl.setKinematic(false);
	}
	public void setColor(ColorRGBA color)
	{
		material.setColor("Diffuse", color);
		material.setColor("Ambient", color);
	}
}
