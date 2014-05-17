package client;

import java.util.Random;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Texture;


public class Flag extends Entity {

	static final float poleHeight = 200.0f;
	static final float flagHeight = 5.0f;
	static final float radius = 0.5f;
	static final int samples = 16;
	static final private Cylinder c = new Cylinder(samples, samples, radius, poleHeight, true);
	static final private Box b = new Box(0.1f, flagHeight/2, flagHeight/2);
	
	private boolean isPickable = true;
	private Node node;
	
	public Flag(int ownerId, World world, int entityId, Vector3f position) {
		super(ownerId, world, entityId, Type.FLAG);

		node = new Node("flag"+entityId);

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
        GhostControl ghostControl = new GhostControl(shape);
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
		return null;
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
		world.getRootNode().detachChild(node);
	}

}
