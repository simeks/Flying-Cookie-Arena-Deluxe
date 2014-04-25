package client;

import client.HUD.Tool;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class CharacterView {
	private InputManager inputManager;
	private World level;
	private Camera cam;
	private Node cameraNode;
	private Character character;
	
	private float cameraAngle = 0.0f; // We don't rotate the character on the x-axis, only the camera, so this is handled here separately.
	
	private HUD hud;
	
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
	
	private float timeElapsed = 0.0f;
	public void update(float tpf)
	{
		Quaternion rotx = new Quaternion(new float[] {cameraAngle*FastMath.DEG_TO_RAD, 0.0f, 0.0f});
		if(cameraNode != null)
		{
			cameraNode.setLocalRotation(rotx);
		}		
		
		character.update(tpf);
    	inputManager.setCursorVisible(false);

	}
	

    // Initializes input, essentially starting to listen for user input.
    private void initInput() {
    	
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
        inputManager.addMapping("UseTool", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Pickup", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Tool1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Tool2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Tool3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));

    }
	
    public void attachCamera(Camera camera)
    {
    	cam = camera;
    	
    	cameraNode.attachChild(new CameraNode("camera", camera));
    	cameraNode.setLocalTranslation(0, 2, 1);
    	
        // Make our action listener listen for the mapped actions to allow it to control the character.
        inputManager.addListener(actionListener, "Jump", "MoveLeft", "MoveRight", "MoveForward", 
        		"MoveBackward", "UseTool", "Sprint", "Tool1", "Tool2", "Tool3", "Interact");
        inputManager.addListener(analogListener, "RotateX", "RotateY", "invRotateX", "invRotateY");
        
        // Hide the mouse cursor
    	inputManager.setCursorVisible(false);
    }
    public void detachCamera()
    {
    	cam = null;
    	cameraNode.detachChildNamed("camera");
    	
    	// Unregister our action listeners as we don't want the user to be able to control the character when it's not active.
    	inputManager.removeListener(actionListener);
    	inputManager.removeListener(analogListener);
    	
    	inputManager.setCursorVisible(true);
    	
    	character.idle();
    }
    
    public Character getCharacter()
    {
    	return character;
    }
    
	public CharacterView(InputManager inputManager, BulletAppState bulletAppState, AssetManager assetManager, World level, Node rootNode)
	{
		this.inputManager = inputManager;
		this.level = level;
		
		hud = new HUD(assetManager, rootNode);
		
		character = new Character(bulletAppState, assetManager, level, rootNode);
		
		cameraNode = new Node();
		character.getNode().attachChild(cameraNode);
		
		initInput();
	}
}
