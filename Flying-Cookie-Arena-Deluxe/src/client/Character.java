package client;

import java.util.Iterator;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Character extends Entity {

	@Override
	public void interact(Character character) {
		
		Iterator<Spatial> iter = ((Node) node).getChildren().iterator();
		while(iter.hasNext()) {
			Spatial child = iter.next();
			
			if (child.getUserData("id") != null){
				if(world.getEntity((Integer) child.getUserData("id")) instanceof Flag){

					((Flag)world.getEntity((Integer) child.getUserData("id"))).editEntity();
					((Flag)world.getEntity((Integer) child.getUserData("id"))).pickupFlag(character.getNode());

					return;
				}
				
			}
		}

	}

	public static final int MOVEMENT_DELAY = 200; // Delay i ms for convergence
	public static final boolean NET_DEBUG = false;
	public static final float WALK_SPEED = 30.0f;
	public static final float STRAFE_SPEED = 25.0f;
	public static final float SPRINT_MULTIPLIER = 1.5f;


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
	private Spatial debugNode;
	private Quaternion roty = new Quaternion(new float[]{0.0f, (float) Math.PI, 0.0f});

	private Vector3f targetPosition = new Vector3f(0,0,0);
	private Quaternion targetRotation = new Quaternion();

	private long targetStateTimestamp = 0;



	private Vector3f velocity = new Vector3f(0,0,0);
	private boolean sprint = false;


	public Character(int ownerId, World world, int entityId, Vector3f position)
	{
		super(ownerId, world, entityId, Type.CHARACTER);

		AssetManager assetManager = Application.getInstance().getAssetManager();
		BulletAppState bulletAppState = Application.getInstance().getBulletAppState();

		// Load the character model
		node = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
		if(NET_DEBUG) {
			debugNode = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
		}

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
		if(NET_DEBUG) {
			world.getRootNode().attachChild(debugNode);
		}

		// Only the owner peer is responsible for simulating the physics for the character entity.
		if(isOwner()) {
			bulletAppState.getPhysicsSpace().add(controller);
		}

		if(position != null) {
			setPosition(position);
		}

		setFlags(FLAG_STATIC_OWNERSHIP);
	}

	public Node getNode()
	{
		return (Node)node;
	}

	public void startMovement(Movement move)
	{
		if(move == Movement.MOVE_FORWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,WALK_SPEED));
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,-WALK_SPEED));

		}
		else if(move == Movement.MOVE_LEFT)
		{
			velocity = velocity.add(new Vector3f(STRAFE_SPEED,0,0));

		}
		else if(move == Movement.MOVE_RIGHT)
		{
			velocity = velocity.add(new Vector3f(-STRAFE_SPEED,0,0));

		}
	}

	public void stopMovement(Movement move)
	{
		if(move == Movement.MOVE_FORWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,-WALK_SPEED));
		}
		else if(move == Movement.MOVE_BACKWARD)
		{
			velocity = velocity.add(new Vector3f(0,0,WALK_SPEED));
		}
		else if(move == Movement.MOVE_LEFT)
		{
			velocity = velocity.add(new Vector3f(-STRAFE_SPEED,0,0));

		}
		else if(move == Movement.MOVE_RIGHT)
		{
			velocity = velocity.add(new Vector3f(STRAFE_SPEED,0,0));

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
		return controller.getPhysicsLocation();
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
			return velocity.mult(SPRINT_MULTIPLIER);
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

	public void setTargetState(Vector3f targetPosition, Quaternion targetRotation, long targetStateTimestamp)
	{
		this.targetPosition = targetPosition;
		this.targetRotation = targetRotation;
		this.targetStateTimestamp = targetStateTimestamp;
	}


	@Override
	public void processStateMessage(EntityStateMessage msg) {
		if(msg.customData != null) processCustomStateMessage(msg.customData);
		setTargetState(msg.position, msg.rotation, msg.timestamp);
		setVelocity(msg.velocity);
	}

	@Override
	public void setCollisionGroup(int group) {
		//node.getControl(CharacterControl.class).setCollisionGroup(group);
	}

	@Override
	public void update(float tpf)
	{
		Vector3f relVelocity = node.getLocalRotation().mult(getVelocity()).mult(tpf);

		if(!isOwner()) {
			// Convergence: We want to reach the target position in (MOVEMENT_DELAY - timestamp) milliseconds.
			long currentTime = System.currentTimeMillis();
			if(currentTime < (targetStateTimestamp + MOVEMENT_DELAY)) {
				float scalar = Math.min(1.0f, (float)(currentTime - targetStateTimestamp) / (float)MOVEMENT_DELAY);

				Vector3f currentPosition = getPosition();
				Vector3f direction = targetPosition.subtract(currentPosition);
				Vector3f newPos = currentPosition.add(direction.mult(scalar));
				setPosition(newPos);

				Quaternion currentRotation = getRotation();
				Quaternion newRotation = currentRotation;
				newRotation.slerp(targetRotation, scalar);
				setRotation(newRotation);

			}

			if(NET_DEBUG) {
				debugNode.setLocalTranslation(targetPosition);
			}

		}
		else
		{
			controller.setPhysicsLocation(controller.getPhysicsLocation().add(relVelocity));	
		}

		controller.setViewDirection(roty.mult(new Vector3f(0,0,1)));


		updateAnimation();
	}

	/// @brief Updates the current animation state depending on the characters velocity.
	private void updateAnimation() {
		if(animState == AnimationState.IDLE) // Is the character currently in the idling animation?
		{
			if(getVelocity().length() >= 10.0f) { // Moving 
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
	public Spatial getSpatial() {
		return node;
	}

	@Override
	public void destroy() {
		Application.getInstance().getBulletAppState().getPhysicsSpace().remove(node.getControl(CharacterControl.class));
		world.getRootNode().detachChild(node);
	}

	@Override
	public void collideWith(Ray ray, CollisionResults results)
	{
		node.collideWith(ray, results);
	}	
}
