package client;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class MyScreenController implements ScreenController {
	
	GameState gs;
	
	public MyScreenController(GameState gs) {
		this.gs = gs;
	}
	
	public void joinGameLobby(String server) {
		System.out.println("derp");
		if(gs instanceof MenuState) {
			((MenuState) gs).joinGameLobby(server);
		}
	}
	
	public void createGameLobby() {
		if(gs instanceof MenuState) {
			((MenuState) gs).createGameLobby();
		}
	}
	
	public void quit() {
		if(gs instanceof MenuState) {
			((MenuState) gs).quit();
		}
	}
	
	public void directConnect() {
		if(gs instanceof MenuState) {
			((MenuState) gs).directConnect();
		}
	}

	public void exitLobbyState() {
		if(gs instanceof LobbyState) {
			((LobbyState) gs).exitLobbyState();
		}
	}
	
	public void toggleReady() {
		if(gs instanceof LobbyState) {
			((LobbyState) gs).toggleReady();
		}
	}
	
	
	@Override
	public void bind(Nifty arg0, Screen arg1) { }

	@Override
	public void onEndScreen() { }

	@Override
	public void onStartScreen() { }
	
};
