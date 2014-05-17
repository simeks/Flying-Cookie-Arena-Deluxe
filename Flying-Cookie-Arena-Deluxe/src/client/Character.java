package client;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Character extends Entity {
	public enum Movement
	{
		MOVE_FORWARD,
		MOVE_BACKWARD,
		MOVE_LEFT,
		MOVE_RIGHT
	};
	public enum AnimationState
	{
		IDLE,
		RUNNING
	};
	private AnimationState animState = AnimationState.IDLE;
	
	private AnimControl animControl;
	private AnimChannel animChannelTop;
	private AnimChannel animChannelBase;
	private CharacterControl  controller;
	private Spatial node;
	private Quaternion roty = new Quaternion();
	
	private Vector3f velocity = new Vector3f(0,0,0);
	private boolean sprint = false;


	public Character(int ownerId, World world, int entityId, Vector3f position)
	{
		super(ownerId, world, entityId, Type.CHARACTER);
		
		AssetManager assetManager = Application.getInstance().getAssetManager();
		BulletAppState bulletAppState = Application.getInstance().getBulletAppState();
		
		// Load the character model
		node = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
		
		// Create a controller for the character
		// I guess BetterCharacterControl would be a better choice but I don't seem to get it working without the character bugging through the floor.
		CapsuleCollisionShape shape = new CapsuleCollisionShape(3.0f, 4.0f, 1);
		controller = new CharacterControl(shape, 0.05f);
		node.addControl(controller);
		
		controller.setJumpSpeed(25.0f);
		controller.addCollideWithGroup(World.COLLISION_GROUP_TERRAIN);

		animControl = node.getControl(AnimControl.class);
		animChannelTop = animControl.createChannel();
		animChannelBase = animControl.createChannel();
		
		animChannelTop.setAnim("IdleTop");
		animChannelBase.setAnim("IdleBase");

		world.getRootNode().attachChild(node);
		
		// Only the owner peer is responsible for simulating the physics for the character entity.
		if(isOwner()) {
			bulletAppState.getPhysicsSpace().add(controller);
		}
		
		if(position != null) {
			setPosition(position);
		}
	}
	
	public Node getNode()
	{
		return (Node)node;
	}
	
	public void startMovement(Movement move)
	{
		if(move == Movement.MOVE_FORWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,35.0f));
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,-35.0f));
			
		}
		else if(move == Movement.MOVE_LEFT)
		{
			velocity = velocity.add(new Vector3f(25.0f,0,0));
			
		}
		else if(move == Movement.MOVE_RIGHT)
		{
			velocity = velocity.add(new Vector3f(-25.0f,0,0));
			
		}
	}
	
	public void stopMovement(Movement move)
	{
		if(move == Movement.MOVE_FORWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,-35.0f));
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,35.0f));
		}
		else if(move == Movement.MOVE_LEFT)
		{
			velocity = velocity.add(new Vector3f(-25.0f,0,0));
			
		}
		else if(move == Movement.MOVE_RIGHT)
		{
			velocity = velocity.add(new Vector3f(25.0f,0,0));
			
		}
	}	
	
	
	public void setSprint(boolean sprint)
	{
		this.sprint = sprint;
	}
	
	public void jump()
	{
		controller.jump();
	}
	
	// Rotates the character the specified number of degrees on the y-axis.
	public void rotate(float angle)
	{
		roty = roty.mult(new Quaternion(new float[] {0.0f, angle * FastMath.DEG_TO_RAD, 0.0f}));
	}
	
	@Override
    public Vector3f getPosition()
    {
    	return node.getWorldTranslation();
    }
	@Override
	public Quaternion getRotation() 
	{
		return roty;
	}
	@Override
	public void setRotation(Quaternion rotation)
	{
		roty = rotation;
	}
	
	@Override
	public Vector3f getVelocity()
	{
		if(sprint)
			return velocity.mult(2.0f);
		else
			return velocity;
	}
	
	@Override
	public void setVelocity(Vector3f velocity)
	{
		this.velocity = velocity;
	}

	public void setPosition(Vector3f position)
	{
		controller.setPhysicsLocation(position);
	}
	
	@Override
	public void setCollisionGroup(int group) {
		//node.getControl(CharacterControl.class).setCollisionGroup(group);
	}
    
	@Override
	public void update(float tpf)
	{
		Vector3f relVelocity = node.getLocalRotation().mult(getVelocity()).mult(tpf);

		controller.setPhysicsLocation(controller.getPhysicsLocation().add(relVelocity));	
		controller.setViewDirection(roty.mult(new Vector3f(0,0,1)));
		
		updateAnimation();
	}
	
	/// @brief Updates the current animation state depending on the characters velocity.
	private void updateAnimation() {
		if(animState == AnimationState.IDLE) // Is the character currently in the idling animation?
		{
			if(getVelocity().length() >= 20.0f) { // Moving 
				animChannelTop.setAnim("RunTop");
				animChannelBase.setAnim("RunBase");
				animChannelTop.setLoopMode(LoopMode.Loop);
				animChannelBase.setLoopMode(LoopMode.Loop);
				animState = AnimationState.RUNNING;
			}
		}
		else if (getVelocity().length() <= 1.0f) { // Standing still
			animChannelTop.setAnim("IdleTop");
			animChannelBase.setAnim("IdleBase");
			animChannelTop.setLoopMode(LoopMode.Loop);
			animChannelBase.setLoopMode(LoopMode.Loop);

			animState = AnimationState.IDLE;
		}
	}


	@Override
	public void destroy() {
		world.getRootNode().detachChild(node);
	}
}
