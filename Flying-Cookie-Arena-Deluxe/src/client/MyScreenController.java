package client;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.Chat;
import de.lessvoid.nifty.controls.ChatTextSendEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/***
 * @brief Callback for Nifty (gui). 
 * 
 */
public class MyScreenController implements ScreenController {
	
	GameState gs;
	
	public MyScreenController(GameState gs) {
		this.gs = gs;
	}
	
	public void joinGameLobby(String server) {
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

	/// @brief when a test is sent from chat
	@NiftyEventSubscriber(pattern=".*LobbyChat")
	public final void onSendText(final String id, final ChatTextSendEvent event) {
		if(!(gs instanceof LobbyState)) {
			return;
		}
		String text = event.getText();
		Chat chat = event.getChatControl();
		if (!text.equals("") && chat instanceof Chat) {
			((LobbyState) gs).onSendText(text, chat);
		}
	}
	
	
	@Override
	public void bind(Nifty arg0, Screen arg1) { }

	@Override
	public void onEndScreen() { }

	/// @brief when the gui is ready
	@Override
	public void onStartScreen() {
		if(gs instanceof LobbyState) {
			((LobbyState) gs).onStartScreen();
		}
	}
	
};
