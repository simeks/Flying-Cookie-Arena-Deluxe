package client;

import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.chatcontrol.ChatBoxViewConverter;
import de.lessvoid.nifty.controls.chatcontrol.builder.ChatBuilder;
import de.lessvoid.nifty.controls.scrollpanel.builder.ScrollPanelBuilder;
import de.lessvoid.nifty.controls.textfield.builder.TextFieldBuilder;

public class LobbyState implements GameState {

	private NiftyJmeDisplay niftyDisplay;
	
	public void exitLobbyState() {
		Application.getInstance().getSession().disconnect();
		Application.getInstance().changeState(GameState.GameStateId.MENU_STATE);
	}
	
	public void toggleReady() {
		Application.getInstance().changeState(GameState.GameStateId.MAIN_STATE);
	}
	
	@Override
	public void enterState() {
		Application app = Application.getInstance();
	    Nifty nifty = niftyDisplay.getNifty();
	    
	    app.getGuiViewPort().addProcessor(niftyDisplay);
	    app.getFlyByCamera().setDragToRotate(true);

	    
	    
	    //nifty.registerMusic("mymusic", "Interface/xyz.ogg");
	    
	    nifty.gotoScreen("ServerLobbyScreen"); // start the screen
	}

	@Override
	public void exitState() {
		Application.getInstance().getLobbyServerConnection().close();
    	InputManager inputManager = Application.getInstance().getInputManager();
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
    				
	               	control(new ChatBuilder("LobbyChat", 0) {{
	               		alignCenter();
	               		valignBottom();
	       				height("90%");
	       				width("100%");
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
