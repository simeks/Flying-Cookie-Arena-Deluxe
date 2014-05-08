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
public class Crate extends Entity {
	// Mass of each crate
	static final float mass = 5.0f;

	// Initial size of each crate
	static final float size = 5.0f;

	// All crates will share the same mesh.
	static final private Box boxMesh = new Box(size * 0.5f, size * 0.5f,
			size * 0.5f);

	private Geometry geometry;
	private Material material;
	private RigidBodyControl rigidBodyControl;

	// Constructor: Creates a crate at the specified position and attaches it to
	// the specified scene node.
	public Crate(World world, int entityId, Vector3f position) {
		super(world, entityId, Type.CRATE);

		// Create a new node with a new instance of the shared box mesh created
		// earlier.
		geometry = new Geometry("Box", boxMesh);
		geometry.move(position);

		AssetManager assetManager = Application.getInstance().getAssetManager();

		// Load the diffuse texture
		Texture diffuseTexture = assetManager
				.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");

		// Load the normal texture
		Texture normalTexture = assetManager
				.loadTexture("Textures/Terrain/BrickWall/BrickWall_normal.jpg");

		// Create a new material for the box and enable light shading.
		material = new Material(assetManager,
				"Common/MatDefs/Light/Lighting.j3md");

		// Material properties
		material.setTexture("DiffuseMap", diffuseTexture);
		material.setTexture("NormalMap", normalTexture);
		material.setBoolean("UseMaterialColors", true);

		// The crates uses the same texture as the wall so randomize the colors
		// to make it more distinct.
		Random rand = new Random();
		ColorRGBA color = new ColorRGBA(rand.nextFloat(), rand.nextFloat(),
				rand.nextFloat(), 1.0f);
		material.setColor("Diffuse", color);
		material.setColor("Ambient", color);

		material.setColor("Specular", ColorRGBA.White);
		material.setFloat("Shininess", 64.0f);

		geometry.setMaterial(material);

		// Attach node to the room scene node.
		world.getRootNode().attachChild(geometry);

		BulletAppState bulletAppState = Application.getInstance()
				.getBulletAppState();

		// Setup physics properties
		rigidBodyControl = new RigidBodyControl(mass);
		geometry.addControl(rigidBodyControl);
		bulletAppState.getPhysicsSpace().add(rigidBodyControl);
	}

	@Override
	public void setPosition(Vector3f position) {
		geometry.move(position);
	}

	@Override
	public Vector3f getPosition() {
		return geometry.getWorldTranslation();
	}

	@Override
	public Quaternion getRotation() 
	{
		return geometry.getLocalRotation();
	}
	@Override
	public void setRotation(Quaternion rotation)
	{
		geometry.setLocalRotation(rotation);
	}

	@Override
	public void update(float tpf) {
		// TODO Auto-generated method stub
		
	}
}
