package client;
 
import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.util.SkyFactory;

// Our main class
public class Application extends SimpleApplication {
	static final int GAME_PORT = 23456; // TODO: Allow the user to change this in the application.
	static final int NET_RATE = 10; // Number of times to broadcast the world state per second.
	
	static private Application sInstance = null;
	private NiftyJmeDisplay niftyDisplay = null;
	private BulletAppState bulletAppState;
	private Session session = new Session();
	private World world = null;
	private LobbyServerConnection lobbyServer = new LobbyServerConnection("130.240.202.79", 5000);

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
		Logger.getLogger("com.jme3").setLevel(Level.SEVERE);
        Application.getInstance().start();
    }

    public void changeState(GameState.GameStateId state) {
    	if(state != currentState) {
    		gameStates.get(currentState).exitState();
    		gameStates.get(state).enterState();
    		currentState = state;
    	}
    }
    
    private float timeElapsed = 0.0f; // Time elapsed since last tick (in seconds)
    
    @Override
	public void simpleUpdate(float tpf) {
    	timeElapsed += tpf;
    	
		gameStates.get(currentState).update(tpf);
		session.update();
		super.simpleUpdate(tpf);
		
		if(timeElapsed >= (1.0f/(float)NET_RATE)) {

			if(session.getState() == Session.State.CONNECTED) {
				world.broadcastWorldState();
			}
			timeElapsed = 0.0f;
		}
	}

	@Override
    public void simpleInitApp() {
		setPauseOnLostFocus(false);
		
		flyCam.setEnabled(false);
		setDisplayStatView(false); 
		setDisplayFps(false);
		
    	// Initialize bullet for physics.
    	bulletAppState = new BulletAppState();
    	stateManager.attach(bulletAppState);

    	cam.setFrustumFar(2000.0f);

    	world = new World();
    	
    	// init nifty for gui.
    	niftyDisplay = new NiftyJmeDisplay(
			getAssetManager(), 
			getInputManager(), 
			getAudioRenderer(), 
			getGuiViewPort()
		);
    	getGuiViewPort().addProcessor(niftyDisplay);
    	
		gameStates.put(GameState.GameStateId.MENU_STATE, new MenuState());
		gameStates.put(GameState.GameStateId.LOBBY_STATE, new LobbyState());
		gameStates.put(GameState.GameStateId.MAIN_STATE, new MainState());
		
		currentState = GameState.GameStateId.MENU_STATE;
		gameStates.get(currentState).enterState();
		
		session.registerEffect(Message.Type.CREATE_ENTITY, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				world.processCreateEntity((CreateEntityMessage)m);
			}
		});
		session.registerEffect(Message.Type.DESTROY_ENTITY, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				world.processDestroyEntity((DestroyEntityMessage)m);
			}
		});
		session.registerEffect(Message.Type.ENTITY_STATE, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				world.processEntityState((EntityStateMessage)m);
			}
		});
		session.registerEffect(Message.Type.ENTITY_EVENT, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				world.processEntityEvent((EntityEventMessage)m);
			}
		});
		/**
		 *  broadcast new entityOwnerShip to all peers
		 */
		session.registerEffect(Message.Type.ENTITY_REQ_OWN_CHANGE, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				EntityRequestOwnerMessage msg = (EntityRequestOwnerMessage)m;
				try {
					session.sendToAll(new EntityNewOwnerMessage(m.peer, msg.entityId), true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
    }
	@Override
	public void destroy() { 
		session.unregisterEffect(Message.Type.CREATE_ENTITY);
		session.unregisterEffect(Message.Type.DESTROY_ENTITY);
		session.unregisterEffect(Message.Type.ENTITY_STATE);
		
		lobbyServer.close();
		session.disconnect();
		super.destroy();
	}
	
	public BulletAppState getBulletAppState() {
		return bulletAppState;
	}
	
	public NiftyJmeDisplay getNiftyDisplay() {
		return niftyDisplay;
	}
	
	public Session getSession() {
		return session;
	}

	public World getWorld() {
		return world;
	}
	
	public LobbyServerConnection getLobbyServerConnection() {
		return lobbyServer;
	}
	
	static public Application getInstance() {
		if(sInstance == null)
		{
			sInstance = new Application();
		}
		return sInstance;
	}
}
