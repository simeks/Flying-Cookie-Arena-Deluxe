package client;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ScreenBuilder;

public class MainState implements GameState {
	private World world;
	private Node cameraNode;
	private Character character;
	
	private float cameraAngle = 0.0f; // We don't rotate the character on the x-axis, only the camera, so this is handled here separately.
	

	// Action listener for handling user interaction.
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
			else if (name.equals("Quit")) {
				exitState();
				Application.getInstance().changeState(GameStateId.LOBBY_STATE);
			}
		}
	};	
	
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
    	world = new World();

		Nifty nifty = Application.getInstance().getNiftyDisplay().getNifty();
	    nifty.loadStyleFile("nifty-default-styles.xml");
	    nifty.loadControlFile("nifty-default-controls.xml");
	    
		final GameState state = this;
		nifty.addScreen("hud", new ScreenBuilder("Nifty Screen") {{
			controller(new client.MyScreenController(state));
		}}.build(nifty));
		
		initInput();
	}
	
    // Initializes input, essentially starting to listen for user input.
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

    }

	@Override
	public void enterState() {
		Application.getInstance().getRootNode().attachChild(world.getRootNode());

    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.deleteMapping(Application.INPUT_MAPPING_EXIT);
        inputManager.addListener(actionListener, "Jump", "MoveLeft", "MoveRight", "MoveForward", 
        		"MoveBackward", "Sprint", "Quit");
        inputManager.addListener(analogListener, "RotateX", "RotateY", "invRotateX", "invRotateY");

        // Hide the mouse cursor
    	inputManager.setCursorVisible(false);
    	
		Application.getInstance().getNiftyDisplay().getNifty().gotoScreen("hud");

		character = world.spawnCharacter(new Vector3f(0, 0, 0));
		cameraNode = new Node();
    	cameraNode.setLocalTranslation(0, 2, 1);
		character.getNode().attachChild(cameraNode);
		
		Camera camera = Application.getInstance().getCamera();
		cameraNode.attachChild(new CameraNode("camera", camera));
		
	}

	@Override
	public void exitState() {
		character.getNode().detachAllChildren();
		world.destroyEntity(character);

    	cameraNode.detachChildNamed("camera");
    	
		Application.getInstance().getRootNode().detachChild(world.getRootNode());

		// Unregister input
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.removeListener(actionListener);
    	inputManager.removeListener(analogListener);	

    	inputManager.setCursorVisible(true);
    	
    	
	}

	@Override
	public void update(float tpf) {
		Quaternion rotx = new Quaternion(new float[] {cameraAngle*FastMath.DEG_TO_RAD, 0.0f, 0.0f});
		if(cameraNode != null)
		{
			cameraNode.setLocalRotation(rotx);
		}		
		
		character.update(tpf);
		
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.setCursorVisible(false);


	}
	

}
