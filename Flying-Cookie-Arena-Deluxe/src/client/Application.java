package client;
 
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
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
	static private Application sInstance = null;
	private BulletAppState bulletAppState;

	private Map<GameState.GameStateId, GameState> gameStates = 
			new EnumMap<GameState.GameStateId, GameState>(GameState.GameStateId.class);
	private GameState.GameStateId currentState;
	
	enum CameraView
	{
		CHARACTER_VIEW,
		GOD_VIEW
	};
	CameraView currentView;
	
	
    public static void main(String[] args){
        Application.getInstance().start();
    }

    public void changeState(GameState.GameStateId state) {
    	if(state != currentState) {
    		gameStates.get(currentState).exitState();
    		gameStates.get(state).enterState();
    		currentState = state;
    	}
    }
    
    
    @Override
	public void simpleUpdate(float tpf) {
		gameStates.get(currentState).update(tpf);
		
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

    	cam.setFrustumFar(2000.0f);

		gameStates.put(GameState.GameStateId.MENU_STATE, new MenuState());
		gameStates.put(GameState.GameStateId.LOBBY_STATE, new LobbyState());
		gameStates.put(GameState.GameStateId.MAIN_STATE, new MainState());
		
		currentState = GameState.GameStateId.MAIN_STATE;
		gameStates.get(currentState).enterState();
    }
	
	public BulletAppState getBulletAppState() {
		return bulletAppState;
	}
	
	static public Application getInstance() {
		if(sInstance == null)
		{
			sInstance = new Application();
		}
		return sInstance;
	}
}
