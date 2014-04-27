package client;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;

public class CampFire extends Entity {
	private static final ColorRGBA lightColor = ColorRGBA.White; 
	private Node node;
	private PointLight pointLight;
	private ParticleEmitter flame;
	private boolean lit = true;
	

	public Vector3f getPosition()
	{
		return node.getWorldTranslation();
	}
	
	public Node getNode()
	{
		return node;
	}
	
	public CampFire(World world, Vector3f position)
	{
		super(world);
				
		node = new Node();
		
		AssetManager assetManager = Application.getInstance().getAssetManager();
		
		// Place some wood for the fire
		Spatial wood = assetManager.loadModel("Models/campfire.obj");
		
		// Create a invisible box used for bounds as there was some problem using picking with the imported blender model.
		//	(The bounds where above the model)
		Geometry bounds = new Geometry("bounds", new Box(1.0f, 2.0f, 1.0f));
		bounds.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
		bounds.setCullHint(CullHint.Always);
		
		flame = new ParticleEmitter("flame", Type.Triangle, 30);
	    Material flameMaterial = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
	    
	    flameMaterial.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
	    
	    flame.setMaterial(flameMaterial);
	    flame.setImagesX(2); 
	    flame.setImagesY(2); 
	    flame.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   
	    flame.setStartColor(new ColorRGBA(1f, 0.8f, 0.3f, 0.5f));
	    flame.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
	    flame.setStartSize(1.5f);
	    flame.setEndSize(0.1f);
	    flame.setGravity(0, 0, 0);
	    flame.setLowLife(1.0f);
	    flame.setHighLife(4.0f);
	    flame.getParticleInfluencer().setVelocityVariation(0.3f);

    	pointLight = new PointLight();
		pointLight.setColor(lightColor);
    	pointLight.setRadius(200);
    	pointLight.setPosition(position.add(new Vector3f(0,2,0)));
    	
    	// The light doesn't seem to be working when it's connected to the campfire node so we need to keep track of
    	//	it separately.
    	world.getRootNode().addLight(pointLight); 

	    
	    flame.setLocalTranslation(new Vector3f(0,0.75f,0));
		node.attachChild(bounds);
	    node.attachChild(flame);
		node.attachChild(wood);
		node.move(position);

		world.getRootNode().attachChild(node);
    	
	}
}
