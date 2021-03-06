package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

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
		
		Random rand = new Random();
		mat2.setColor("Ambient", new ColorRGBA(
			rand.nextFloat(),
			rand.nextFloat(),
			rand.nextFloat(),
			1f
		));
		mat2.setColor("Diffuse", new ColorRGBA(
			rand.nextFloat(),
			rand.nextFloat(),
			rand.nextFloat(),
			1f
		));
		
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
	
	/// @brief places the flag on the given node and sets edits the hud if owner is involved. 
	public void pickupFlag(Node attachHere) {
		node.getControl(GhostControl.class).setEnabled(false);
		node.getControl(RigidBodyControl.class).setEnabled(false);
		
		Spatial oldNode = node.getParent();
		
		world.getRootNode().detachChild(node);
		setPosition(new Vector3f(0,flagHeight/2,-radius*2));
		
		int flags = 0;
		Iterator<Spatial> myIter = attachHere.getChildren().iterator();
		while(myIter.hasNext()) {
			Spatial myChild = myIter.next();
			if (myChild.getUserData("id") != null){
				if(world.getEntity((Integer) myChild.getUserData("id")) instanceof Flag){
					flags++;
				}
			}
		}
		
		// update hud
		if(attachHere.getUserData("id") != null) {
			Entity character = world.getEntity((Integer)attachHere.getUserData("id"));
			if(character != null && character.isOwner()) {
				Application.getInstance().getHud().setNumberOfFlags(flags+1);
			}
		}
		if(oldNode.getUserData("id") != null) {
			Entity oldCharacter = world.getEntity((Integer)oldNode.getUserData("id"));
			if(oldCharacter != null && oldCharacter.isOwner()) {
				Application.getInstance().getHud().setNumberOfFlags(Application.getInstance().getHud().getNumberOfFlags()-1);
			}
		}
		
		attachHere.attachChild(node);
		setPosition(new Vector3f(getPosition().getX(), getPosition().getY()+flags*Flag.flagHeight, getPosition().getZ()));
		
		setState(CARRYD);
	}
	
	/// @brief places flag at a position
	public void dropFlag(Vector3f position) {
		node.getControl(GhostControl.class).setEnabled(true);
		node.getControl(RigidBodyControl.class).setEnabled(true);
		node.getParent().detachChild(node);
		node.move(position);
		world.getRootNode().attachChild(node);
		setState(DROPPED);
	}
	
	/// @brief returns flag to its original position
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
