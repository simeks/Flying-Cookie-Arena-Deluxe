package client;

import java.util.Properties;

import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.Chat;
import de.lessvoid.nifty.controls.ChatTextSendEvent;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.chatcontrol.ChatBoxViewConverter;
import de.lessvoid.nifty.controls.chatcontrol.ChatControl;
import de.lessvoid.nifty.controls.chatcontrol.builder.ChatBuilder;
import de.lessvoid.nifty.controls.scrollpanel.builder.ScrollPanelBuilder;
import de.lessvoid.nifty.controls.textfield.builder.TextFieldBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.xml.xpp3.Attributes;

public class LobbyState implements GameState {

	private NiftyJmeDisplay niftyDisplay;

	/// @brief called when exit is clicked. 
	public void exitLobbyState() {
		Application.getInstance().getSession().disconnect();
		Application.getInstance().changeState(GameState.GameStateId.MENU_STATE);
	}

	/// @brief called when ready is clicked
	public void toggleReady() {
		Application.getInstance().changeState(GameState.GameStateId.MAIN_STATE);
	}

	/// @brief called when the screen is ready. 
	public void onStartScreen() {
		addPlayer("You");
	}
	
	private void addPlayer(String name) {
		Nifty nifty = niftyDisplay.getNifty();
		Chat chat = nifty.getCurrentScreen().findNiftyControl("LobbyChat", Chat.class);
	    chat.addPlayer(name, nifty.getRenderEngine().createImage(nifty.getCurrentScreen(), "Textures/avatar1.png", false));
	}

	/// @brief called when message was sent. 
	public void onSendText(String text, Chat chat) {
		System.out.println(text);
		Nifty nifty = niftyDisplay.getNifty();
		chat.receivedChatLine(text, nifty.getRenderEngine().createImage(nifty.getCurrentScreen(), "Textures/avatar1.png", false));

	    ChatMessage chatMsg = new ChatMessage(text);
	    try {
	    	Application.getInstance().getSession().sendToAll(chatMsg, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void enterState() {
		Application app = Application.getInstance();
	    Nifty nifty = niftyDisplay.getNifty();
	    nifty.gotoScreen("ServerLobbyScreen"); // start the screen
	    
	    app.getGuiViewPort().addProcessor(niftyDisplay);
	    app.getFlyByCamera().setDragToRotate(true);

	    app.getSession().registerEffect(Message.Type.CHAT_MSG, new MessageEffect() {
			
			@Override
			public void execute(Message m) {
				Chat chat = niftyDisplay.getNifty().getCurrentScreen().findNiftyControl("LobbyChat", Chat.class);
				Nifty nifty = niftyDisplay.getNifty();
				chat.receivedChatLine(((ChatMessage)m).message, nifty.getRenderEngine().createImage(nifty.getCurrentScreen(), "Textures/avatar1.png", false));
				System.out.println("Chat (Peer: " + m.peer + "): " + ((ChatMessage)m).message);
			}
		});
	 
	    app.getWorld().clear();
	}

	@Override
	public void exitState() {
		Application app = Application.getInstance();
		app.getSession().unregisterEffect(Message.Type.CHAT_MSG);
		
		app.getLobbyServerConnection().close();
    	InputManager inputManager = app.getInputManager();
    	inputManager.setCursorVisible(true);
	}

	@Override
	public void update(float dt) {
		// TODO Auto-generated method stub

	}
	
	public LobbyState() {
		niftyDisplay = Application.getInstance().getNiftyDisplay();
		Nifty nifty = niftyDisplay.getNifty();
		
		nifty.loadStyleFile("nifty-default-styles.xml");
	    nifty.loadControlFile("nifty-default-controls.xml");
	 
	    final LobbyState state = this;
	    
	    nifty.addScreen("ServerLobbyScreen", new ScreenBuilder("Nifty Screen") {{
	    	
	    	controller(new client.MyScreenController(state));
	 
	        layer(new LayerBuilder("LobbyLayer") {{
	        	childLayoutVertical();
	        	backgroundColor("#ccc");
	            
	        	panel(new PanelBuilder("LobbyTopPanel") {{
	            	childLayoutCenter();
    				height("10%");
    				width("100%");
			   
	        		text(new TextBuilder() {{
	        			text("Flying Cookie Arena Deluxe - Game Lobby!");
        				font("Interface/Fonts/Default.fnt");
	               		color("#000");
        				height("100%");
        				width("100%");
	                }});
	        	}});
	            
	            panel(new PanelBuilder("LobbyChatPanel") {{
	            	childLayoutCenter();
    				height("70%");
    				width("100%");
    				
	               	control(new ChatBuilder("LobbyChat", 20) {{
	               		alignCenter();
	               		valignBottom();
	       				height("90%");
	       				width("100%");
	       		    	controller(new ChatControl());
               		}});
	            }});
	            
	            panel(new PanelBuilder("LobbyButtonPanel") {{
	            	childLayoutCenter();
    				height("20%");
    				width("100%");
	 
    				control(new ButtonBuilder("quitLobbyButton", "Quit"){{
	                    alignLeft();
	                    valignCenter();
	                    height("10%");
	                    width("30%");
	                    marginLeft("20%");
	                    interactOnClick("exitLobbyState()");
	                }});
	               	
	                control(new ButtonBuilder("readyLobbyButton", "Ready"){{
	                    alignRight();
	                    valignCenter();
	                    height("10%");
	                    width("30%");
	                    marginRight("20%");
	                    interactOnClick("toggleReady()");
	                }});
            	}});
	        }});
	    }}.build(nifty));
	}

}
