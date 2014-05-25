package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
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
	
	public CampFire(int ownerId, World world, int entityId, Vector3f position)
	{
		super(ownerId, world, entityId, Type.CAMP_FIRE);
				
		node = new Node();
		
		AssetManager assetManager = Application.getInstance().getAssetManager();
		
		// Place some wood for the fire
		Spatial wood = assetManager.loadModel("Models/campfire.obj");
		
		// Create a invisible box used for bounds as there was some problem using picking with the imported blender model.
		//	(The bounds where above the model)
		Geometry bounds = new Geometry("bounds", new Box(1.0f, 2.0f, 1.0f));
		bounds.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
		bounds.setCullHint(CullHint.Always);
		
		flame = new ParticleEmitter("flame", ParticleMesh.Type.Triangle, 30);
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

	public void setLit(boolean lit)
	{
		if(this.lit == lit)
			return;
		
		if(!lit)
		{
		    flame.setLowLife(0.0f);
		    flame.setHighLife(0.0f);
			pointLight.setColor(ColorRGBA.Black);
			this.lit = false;
		}
		else
		{
		    flame.setLowLife(1.0f);
		    flame.setHighLife(4.0f);
			pointLight.setColor(lightColor);
			this.lit = true;
		}
	}
	
	@Override
	public void interact() {
		// Toggle lit
		setLit(lit == false);
		
		try {
			Application.getInstance().getSession().sendToAll(new EntityEventMessage(buildStateMessage()), true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void setPosition(Vector3f position) {
    	pointLight.setPosition(position.add(new Vector3f(0,2,0)));
		node.setLocalTranslation(position);
	}

	@Override
	public Quaternion getRotation() 
	{
		return node.getLocalRotation();
	}
	@Override
	public void setRotation(Quaternion rotation)
	{
		node.setLocalRotation(rotation);
	}

	@Override
	public Vector3f getVelocity()
	{
		return new Vector3f(0,0,0);
	}
	
	@Override
	public void setVelocity(Vector3f velocity)
	{
	}
	
	@Override
	public void setCollisionGroup(int group) {
		//node.getControl(RigidBodyControl.class).setCollisionGroup(group);
	}
	
	@Override
	public void update(float tpf) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void processCustomStateMessage(Serializable data) {
		if(data instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) data;
			if(map.containsKey("lit")) {
				setLit((boolean) map.get("lit"));
				
			}
		}
	}
	
	@Override
	protected Serializable getCustomData() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("lit", lit);
		return (Serializable) map;
	}	

	@Override
	protected boolean hasCustomStateChanged() {
 		return true;
	}

	@Override
	public void destroy() {
		world.getRootNode().detachChild(node);
	}

	@Override
	public Spatial getSpatial() {
		return node;
	}
	@Override
	public void collideWith(Ray ray, CollisionResults results)
	{
		node.collideWith(ray, results);
	}	
}
