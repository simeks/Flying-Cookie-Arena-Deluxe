package client;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
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
	
	private AnimControl animControl;
	private AnimChannel animChannelTop;
	private AnimChannel animChannelBase;
	private CharacterControl  controller;
	private Spatial node;
	
	private Vector3f velocity = new Vector3f(0,0,0);
	private boolean sprint = false;


	public Character(World world, int entityId, Vector3f position)
	{
		super(world, entityId, Type.CHARACTER);
		
		AssetManager assetManager = Application.getInstance().getAssetManager();
		BulletAppState bulletAppState = Application.getInstance().getBulletAppState();
		
		// Load the character model
		node = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
		
		// Create a controller for the character
		// I guess BetterCharacterControl would be a better choice but I don't seem to get it working without the character bugging through the floor.
		CapsuleCollisionShape shape = new CapsuleCollisionShape(3.0f, 4.0f, 1);
		controller = new CharacterControl(shape, 0.05f);
		node.addControl(controller);
		bulletAppState.getPhysicsSpace().add(controller);
		
		controller.setJumpSpeed(25.0f);

		animControl = node.getControl(AnimControl.class);
		animChannelTop = animControl.createChannel();
		animChannelBase = animControl.createChannel();
		
		animChannelTop.setAnim("IdleTop");
		animChannelBase.setAnim("IdleBase");

		world.getRootNode().attachChild(node);
		
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
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");

			velocity = velocity.add(new Vector3f(0,0,35.0f));
			
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(0,0,-35.0f));
			
		}
		else if(move == Movement.MOVE_LEFT)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(25.0f,0,0));
			
		}
		else if(move == Movement.MOVE_RIGHT)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(-25.0f,0,0));
			
		}
		// Set to idle animation if character stopped
		if(velocity.length() == 0)
		{
	    	idle();
		}
	}
	
	public void stopMovement(Movement move)
	{
		if(move == Movement.MOVE_FORWARD)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(0,0,-35.0f));
			
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(0,0,35.0f));
			
		}
		else if(move == Movement.MOVE_LEFT)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(-25.0f,0,0));
			
		}
		else if(move == Movement.MOVE_RIGHT)
		{
			animChannelTop.setAnim("RunTop");
			animChannelBase.setAnim("RunBase");
			velocity = velocity.add(new Vector3f(25.0f,0,0));
			
		}
		// Set to idle animation if character stopped
		if(velocity.length() == 0)
		{
	    	idle();
		}
	}	
	
	// Sets the character to the idle animation
	public void idle()
	{
		animChannelTop.setAnim("IdleTop");
		animChannelBase.setAnim("IdleBase");
		animChannelTop.setLoopMode(LoopMode.Loop);
		animChannelBase.setLoopMode(LoopMode.Loop);
		
    	velocity = new Vector3f(0,0,0);
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
		Quaternion roty = new Quaternion(new float[] {0.0f, angle * FastMath.DEG_TO_RAD, 0.0f});
		controller.setViewDirection(roty.mult(controller.getViewDirection()));
	}

    public Vector3f getPosition()
    {
    	return node.getWorldTranslation();
    }
    
	public void update(float tpf)
	{
		Vector3f relVelocity = node.getLocalRotation().mult(velocity).mult(tpf);
		if(sprint)
			relVelocity.multLocal(2.0f);
		controller.setPhysicsLocation(controller.getPhysicsLocation().add(relVelocity));		
	}
	
	public void setPosition(Vector3f position)
	{
		controller.setPhysicsLocation(position);
	}
	
	public void setDirection(Vector3f direction)
	{
		controller.setViewDirection(direction);
	}
	

	
}
