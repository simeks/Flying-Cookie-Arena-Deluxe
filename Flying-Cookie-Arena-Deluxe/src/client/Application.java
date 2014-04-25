package client;
 
import java.util.ArrayList;
import java.util.Random;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.util.SkyFactory;

// Our main class
public class Application extends SimpleApplication {
	private BulletAppState bulletAppState;
	private World level;
	private CharacterView characterView;

	private ArrayList<AI> bots = new ArrayList<AI>();
	
	enum CameraView
	{
		CHARACTER_VIEW,
		GOD_VIEW
	};
	CameraView currentView;
	
	
    public static void main(String[] args){
    	Application app = new Application();
        app.start();
    }

    
    @Override
	public void simpleUpdate(float tpf) {
    	
    	characterView.update(tpf);
    	
    	// Update ai
    	for(AI ai : bots)
    	{
    		ai.update(tpf);
    	}
    	
		super.simpleUpdate(tpf);
	}

    
	@Override
    public void simpleInitApp() {
		flyCam.setEnabled(false);
		setDisplayStatView(false); 
		setDisplayFps(false);
		
    	// Initialize bullet for physics.
    	bulletAppState = new BulletAppState();
    	stateManager.attach(bulletAppState);

    	level = new World(bulletAppState, assetManager, rootNode, 250, 250);
    	characterView = new CharacterView(inputManager, bulletAppState, assetManager, level, rootNode);
    	
    	cam.setFrustumFar(2000.0f);
    	characterView.attachCamera(cam);
    	currentView = CameraView.CHARACTER_VIEW;
    	

    	
		
		for (int i = 0; i < 2; ++i)
		{
			
			// Spawn a bot
			Character aiCharacter = new Character(bulletAppState, assetManager, level, rootNode);
			aiCharacter.setPosition(new Vector3f(20 + i * -20, 100, 80 + i * 5));
			
			
			AI ai = new AI(aiCharacter);
			ai.setTarget(characterView.getCharacter());
			bots.add(ai);
		}
    }
}
