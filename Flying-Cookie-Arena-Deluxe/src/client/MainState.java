package client;

import java.util.Random;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ScreenBuilder;

public class MainState implements GameState {
	private Node cameraNode;
	private Character character;
	
	private float cameraAngle = 0.0f; // We don't rotate the character on the x-axis, only the camera, so this is handled here separately.


	/// @brief when a collision between entitys in correct collision groups happen. ment for custom events
	PhysicsCollisionListener collisionListener = new PhysicsCollisionListener() {
		
		@Override
		public void collision(PhysicsCollisionEvent event) {
			// parse the event
			Spatial myCharacter;
			Spatial other;
			if(event.getNodeA().getName().equals("myCharacter")) {
				myCharacter = event.getNodeA();
				other = event.getNodeB();
			} else if(event.getNodeB().getName().equals("myCharacter")) {
				myCharacter = event.getNodeB();
				other = event.getNodeA();
			} else {
				return;
			}
			
			// ignore terrain
			if(other.getName().equals("Terrain")) {
				return;
			}
			
			// pickup newly spawned flag
			Entity OtherEntity = null;
			if(other.getUserData("id") != null) {
				OtherEntity = Application.getInstance().getWorld().getEntity((Integer)other.getUserData("id"));
			}
			if(other.getName().substring(0, 4).equals("flag")) {
				if(OtherEntity instanceof Flag) {
					Flag flag = ((Flag)OtherEntity);
					flag.editEntity();
					flag.pickupFlag(character.getNode());
				}
			}
		}
	};

	/// @brief Action listener for handling user interaction.
	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("MoveLeft")) {
				if(keyPressed)
					character.startMovement(Character.Movement.MOVE_LEFT);
				else
					character.stopMovement(Character.Movement.MOVE_LEFT);
					
			}
			else if (name.equals("MoveRight")) {
				if(keyPressed)
					character.startMovement(Character.Movement.MOVE_RIGHT);
				else
					character.stopMovement(Character.Movement.MOVE_RIGHT);
			}
			else if (name.equals("MoveForward")) {
				if(keyPressed)
					character.startMovement(Character.Movement.MOVE_FORWARD);
				else
					character.stopMovement(Character.Movement.MOVE_FORWARD);
			}
			else if (name.equals("MoveBackward")) {
				if(keyPressed)
					character.startMovement(Character.Movement.MOVE_BACKWARD);
				else
					character.stopMovement(Character.Movement.MOVE_BACKWARD);
			}
			else if (name.equals("Jump") && keyPressed) {
				character.jump();
			}
			else if (name.equals("Sprint")) {
				character.setSprint(keyPressed);
			}
			else if (name.equals("Interact") && keyPressed) {
				Camera camera = Application.getInstance().getCamera();
				Application.getInstance().getWorld().interactWithItem(character, camera.getDirection(), 15.0f);
			}
			else if (name.equals("Quit")) {
				Application.getInstance().changeState(GameStateId.LOBBY_STATE);
			}
		}
	};	
	/// @brief Listener for moving the character
	private AnalogListener analogListener = new AnalogListener() {
		public void onAnalog(String name, float value, float tpf)
		{
			if (name.equals("RotateX")) {
				cameraAngle = Math.max(cameraAngle - value*50.0f, -(float)90);
			}
			else if (name.equals("RotateY")) {
				character.rotate(-value*50.0f);
			}
			else if (name.equals("invRotateX")) {
				cameraAngle = Math.min(cameraAngle + value*50.0f, (float)90);
			}
			else if (name.equals("invRotateY")) {
				character.rotate(value*50.0f);

			}
		}
	};

	public MainState() {
		// create gui screen (empty hud)
		Nifty nifty = Application.getInstance().getNiftyDisplay().getNifty();
	    nifty.loadStyleFile("nifty-default-styles.xml");
	    nifty.loadControlFile("nifty-default-controls.xml");
		final GameState state = this;
		nifty.addScreen("hud", new ScreenBuilder("Nifty Screen") {{
			controller(new client.MyScreenController(state));
			
		}}.build(nifty));
		
		
		initInput();
	}
	
	/// @brief Initializes input, essentially starting to listen for user input.
    private void initInput() {
    	InputManager inputManager = Application.getInstance().getInputManager();
    	
        // Map different inputs to named actions.
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("MoveLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("MoveRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("RotateY", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("RotateX", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("invRotateY", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("invRotateX", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Quit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));

    }
    

	@Override
	public void enterState() {
		World world = Application.getInstance().getWorld();
		
		Application.getInstance().getRootNode().attachChild(world.getRootNode());

		// id and peerCount as input for placing things
		int id = Application.getInstance().getSession().getMyPeerId();
		int count = Application.getInstance().getSession().getPeerCount();
		
		// add character (before inputlisteners!)
		if(character == null) {
			character = world.spawnCharacter(new Vector3f((id-count/2)*20, 15, (id-count/2)*20));
			
			cameraNode = new Node();
	    	cameraNode.setLocalTranslation(0, 2, 1);
			character.getNode().attachChild(cameraNode);
			
			Node characterNode = character.getNode();
			
			// set collision with flags so the callback triggers
			characterNode.getControl(CharacterControl.class).setCollideWithGroups(World.COLLISION_GROUP_FLAG);
			characterNode.setName("myCharacter");

			// Lets spawn some campfires if we're the master
			Random rand = new Random();
			if(Application.getInstance().getSession().isMaster()) {
				for(int i = 0; i < 5; ++i) {
					world.spawnCampFire(new Vector3f(rand.nextInt(400)-200, 0.25f, rand.nextInt(400)-200));

				}
				
				// Spawn some AI character
				world.spawnAICharacter(new Vector3f(rand.nextInt(200)-50, 30, rand.nextInt(200)-50));
			}
			
			// spawn flags
			for(int i = 0; i < 3; ++i) {
				Flag flag = world.spawnFlag(new Vector3f(rand.nextInt(400)-200, Flag.poleHeight*0.5f, rand.nextInt(400)-200));
			}
		}
		
		// place camera
		Camera camera = Application.getInstance().getCamera();
		cameraNode.attachChild(new CameraNode("camera", camera));
			
		// init listeners
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.deleteMapping(Application.INPUT_MAPPING_EXIT);
        inputManager.addListener(actionListener, "Jump", "MoveLeft", "MoveRight", "MoveForward", 
        		"MoveBackward", "Sprint", "Quit", "Interact");
        inputManager.addListener(analogListener, "RotateX", "RotateY", "invRotateX", "invRotateY");

        // Hide the mouse cursor
    	inputManager.setCursorVisible(false);
    	
    	// start hud screen
		Application.getInstance().getNiftyDisplay().getNifty().gotoScreen("hud");
		
		// add collision listener
		Application.getInstance().getBulletAppState().getPhysicsSpace().addCollisionListener(collisionListener);
		
	}

	@Override
	public void exitState() {
		World world = Application.getInstance().getWorld();
		
		// remove collision listener
		Application.getInstance().getBulletAppState().getPhysicsSpace().removeCollisionListener(collisionListener);

		// remove camera
    	cameraNode.detachChildNamed("camera");
    	
    	// dont render world
		Application.getInstance().getRootNode().detachChild(world.getRootNode());

		// Unregister input
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.removeListener(actionListener);
    	inputManager.removeListener(analogListener);

    	// give user cursor
    	inputManager.setCursorVisible(true);
	}

	@Override
	public void update(float tpf) {
		
		// Don't rotate upside down etc. 
		Quaternion rotx = new Quaternion(new float[] {cameraAngle*FastMath.DEG_TO_RAD, 0.0f, 0.0f});
		if(cameraNode != null)
		{
			cameraNode.setLocalRotation(rotx);
		}		
		
		// remove cursor
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.setCursorVisible(false);

    	// update world
    	Application.getInstance().getWorld().update(tpf);

	}
	

}
