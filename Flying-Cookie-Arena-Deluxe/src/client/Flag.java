package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;


public class Flag extends Entity {
	
    public final int CARRYD = 0;
    public final int DROPPED = 1;
    public final int SPAWN = 2;
	private int state;
	private int latestState;
    
	static final float poleHeight = 10.0f;
	static final float flagHeight = 2.5f;
	static final float radius = 0.5f;
	static final int samples = 16;
	static final private Cylinder c = new Cylinder(samples, samples, radius, poleHeight, true);
	static final private Box b = new Box(0.1f, flagHeight/2, flagHeight/2);
	
	private Node node;
	private GhostControl ghostControl;
	private Vector3f originalPosition;
	
	public Flag(int ownerId, World world, int entityId, Vector3f position) {
		super(ownerId, world, entityId, Type.FLAG);
		
		setState(SPAWN);
		setLatestState(SPAWN);
		
		node = new Node("flag"+entityId);
		originalPosition = position;
		
		AssetManager assetManager = Application.getInstance().getAssetManager();

		
		Geometry pole = new Geometry("flagPole", c);
		Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		mat.setBoolean("UseMaterialColors",true);
		mat.setColor("Ambient", ColorRGBA.Brown);
		mat.setColor("Diffuse", ColorRGBA.Brown);
		pole.setMaterial(mat);
		Quaternion poleRotation = new Quaternion().fromAngleAxis( FastMath.PI/2 , new Vector3f(1,0,0) );
		pole.setLocalRotation(poleRotation);
		
		
		Geometry flag = new Geometry("flag", b);
		Material mat2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		mat2.setBoolean("UseMaterialColors",true);
		mat2.setColor("Ambient", ColorRGBA.Black);
		mat2.setColor("Diffuse", ColorRGBA.Black);
		flag.setMaterial(mat2);
		flag.setLocalRotation(new Quaternion().fromAngleAxis( FastMath.PI/2 , new Vector3f(0,1,0) ));
		flag.setLocalTranslation(new Vector3f(flagHeight/2+radius, poleHeight/2-flagHeight/2, 0));
		
		
		node.attachChild(pole);
		node.attachChild(flag);
		node.move(position);

		BulletAppState bulletAppState = Application.getInstance().getBulletAppState();
		BoxCollisionShape shape = new BoxCollisionShape(new Vector3f(radius+5, poleHeight/2, radius+5));
		
		// for custom events (like pick up the flag)
        ghostControl = new GhostControl(shape);
        ghostControl.setCollideWithGroups(World.COLLISION_GROUP_NOTHING);
        node.addControl(ghostControl);
	    bulletAppState.getPhysicsSpace().add(ghostControl);
        
	    // so entities dont go thru the flag
		RigidBodyControl rigidBodyControl = new RigidBodyControl(0);
		//rigidBodyControl.setMass(1f);
		//rigidBodyControl.setKinematic(true);
		node.addControl(rigidBodyControl);
		bulletAppState.getPhysicsSpace().add(rigidBodyControl);
		
		ghostControl.setPhysicsRotation(poleRotation);
		
		world.getRootNode().attachChild(node);
	}
	
	public void pickupFlag(Node attachHere) {
		node.getControl(GhostControl.class).setEnabled(false);
		node.getControl(RigidBodyControl.class).setEnabled(false);
		world.getRootNode().detachChild(node);
		setPosition(new Vector3f(0,0,0));
		attachHere.attachChild(node);
		setState(CARRYD);
	}
	
	public void dropFlag(Vector3f position) {
		node.getControl(GhostControl.class).setEnabled(true);
		node.getControl(RigidBodyControl.class).setEnabled(true);
		node.getParent().detachChild(node);
		node.move(position);
		world.getRootNode().attachChild(node);
		setState(DROPPED);
	}
	
	public void returnFlag() {
		node.getControl(GhostControl.class).setEnabled(true);
		node.getControl(RigidBodyControl.class).setEnabled(true);
		node.getParent().detachChild(node);
		node.move(originalPosition);
		world.getRootNode().attachChild(node);
		setState(SPAWN);
	}
	

	@Override
	protected void processCustomStateMessage(Serializable data) {
		if(data instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) data;
			if(map.containsKey("state")) {
				int ownsersState = (Integer) map.get("state");
				if(ownsersState == SPAWN) {
					returnFlag();
					setLatestState(SPAWN);
				} else if(ownsersState == CARRYD && map.containsKey("attachHereEntityId")) {
					Entity s = world.getEntity((Integer) map.get("attachHereEntityId"));
					if(s != null && s.getSpatial() instanceof Node) {
						pickupFlag(((Node) s.getSpatial()));
					}
					setLatestState(CARRYD);
				} else if(ownsersState == DROPPED && map.containsKey("dropFlagPosition") && map.get("dropFlagPosition") instanceof Vector3f) {
					dropFlag((Vector3f) map.get("dropFlagPosition"));
					setLatestState(DROPPED);
				}
			}
		}
	}
	
	@Override
	protected Serializable getCustomData() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("state", state);
		if(stateIs(CARRYD) && node.getParent().getUserData("id") != null) {
			map.put("attachHereEntityId", node.getParent().getUserData("id"));
		} else if(stateIs(DROPPED)) {
			map.put("dropFlagPosition", getPosition());
		}
		return (Serializable) map;
	}
	
	private void setState(int state) {
		this.state = state;
	}
	private void setLatestState(int state) {
		this.latestState = state;
	}
	private boolean stateIs(int state) {
		return (this.state == state)? true : false;
	}

	@Override
	protected boolean hasCustomStateChanged() {
 		return !(stateIs(latestState));
	}
	
	@Override
	public void update(float tpf) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setPosition(Vector3f position) {
		node.setLocalTranslation(position);
	}

	@Override
	public Vector3f getPosition() {
		if(state == CARRYD) {
			return node.getLocalTranslation();
		}
		return node.getWorldTranslation();
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
		return new Vector3f();
	}

	@Override
	public void setVelocity(Vector3f velocity) {
		
	}

	@Override
	public void setCollisionGroup(int group) {
		node.getControl(GhostControl.class).setCollisionGroup(group);
	}

	@Override
	public Spatial getSpatial() {
		return node;
	}

	@Override
	public void destroy() {
	    Application.getInstance().getBulletAppState().getPhysicsSpace().add(ghostControl);
	    Application.getInstance().getBulletAppState().getPhysicsSpace().add(node.getControl(RigidBodyControl.class));
		world.getRootNode().detachChild(node);
	}
	
	@Override
	public void collideWith(Ray ray, CollisionResults results)
	{
		node.collideWith(ray, results);
	}	

}
