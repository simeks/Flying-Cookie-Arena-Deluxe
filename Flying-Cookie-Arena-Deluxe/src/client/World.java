package client;

import java.util.ArrayList;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.HillHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

public class World {
	private BulletAppState bulletAppState;
	private AssetManager assetManager;
	private Node rootNode; // Root scene node for this world and its content
	private TerrainQuad terrain;

	// List of all crates in the world
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	// Spawns a box at the specified world coordinates
	public void spawnCampFire(Vector3f position) {
		CampFire fire = new CampFire(this, position);
		entities.add(fire);
	}


	// Spawns a box at the specified world coordinates
	public void spawnBox(Vector3f position) {
		Crate crate = new Crate(this, position);
		entities.add(crate);
	}
	
	public Node getRootNode()
	{
		return rootNode;
	}

	public World() {
		this.bulletAppState = Application.getInstance().getBulletAppState();
		this.assetManager = Application.getInstance().getAssetManager();

		// Create a root scene node for this world, all objects in the world will
		// later be attached to this node.
		rootNode = new Node("room");
		Application.getInstance().getRootNode().attachChild(rootNode);

		// Terrain
		{
			Material terrainMaterial = new Material(assetManager,
					"Materials/TerrainLighting.j3md");

			Texture texture = assetManager
					.loadTexture("Textures/Terrain/splat/grass.jpg");
			texture.setWrap(WrapMode.Repeat);
			terrainMaterial.setTexture("DiffuseMap", texture);
			terrainMaterial.setFloat("DiffuseMap_0_scale", 50.0f);

			texture = assetManager
					.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
			texture.setWrap(WrapMode.Repeat);
			terrainMaterial.setTexture("NormalMap", texture);

			AbstractHeightMap heightmap = null;
			try {
				heightmap = new HillHeightMap(1025, 1000, 1.0f, 300.0f, 15);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			heightmap.load();

			int patchSize = 65;
			terrain = new TerrainQuad("Terrain", patchSize, 1025,
					heightmap.getHeightMap());
			terrain.setMaterial(terrainMaterial);
			rootNode.attachChild(terrain);

			RigidBodyControl rigidBodyControl = new RigidBodyControl(0.0f);
			terrain.addControl(rigidBodyControl);

			bulletAppState.getPhysicsSpace().add(rigidBodyControl);
		}

		// Spawn some boxes
		for (int i = 0; i < 8; ++i) {
			spawnBox(new Vector3f(-50, 100 + i * 5, 150));
		}

		// Setup ambient light source
		AmbientLight ambientLight = new AmbientLight();
		ambientLight.setColor(ColorRGBA.White.mult(0.2f));
		rootNode.addLight(ambientLight);

		// Setup directional light source
		DirectionalLight directionalLight = new DirectionalLight();
		directionalLight.setColor(ColorRGBA.White.mult(0.4f));
		directionalLight.setDirection(new Vector3f(-0.5f, -0.55f, 0.5f)
				.normalizeLocal());
		rootNode.addLight(directionalLight);

		// Spawn some initial camp fires
		spawnCampFire(new Vector3f(10, 79, 80));

		// Create the sky
		rootNode.attachChild(SkyFactory.createSky(assetManager,
				"Textures/Sky/Bright/BrightSky.dds", false));
	}
}
