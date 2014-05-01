package client;

import java.net.InetAddress;
import java.util.Vector;

import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.scrollpanel.builder.ScrollPanelBuilder;
import de.lessvoid.nifty.controls.textfield.builder.TextFieldBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.DefaultScreenController;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/// @brief State for the menu the user sees when starting the game.
public class MenuState implements GameState {
	
	private NiftyJmeDisplay niftyDisplay;
	private Vector<String> serversInList;
	
	@Override
	public void enterState() {
		Nifty nifty = niftyDisplay.getNifty();
		Application.getInstance().getFlyByCamera().setDragToRotate(true);
	    nifty.gotoScreen("ServerListScreen"); // start the screen
	    
	    nifty = niftyDisplay.getNifty();
	    //nifty.getCurrentScreen().findElementByName("ServerListServerNameField").setFocus();

	    Vector<String> servers = new Vector<String>();
	    servers.add("127.0.0.1");
	    updateServerList(servers, "");
	}

	@Override
	public void exitState() {
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.setCursorVisible(true);
	}

	@Override
	public void update(float dt) {
		// TODO Auto-generated method stub
		
	}
	
	public void updateServerList(Vector<String> servers, String nextUpdate) {
		Nifty nifty = niftyDisplay.getNifty();
		Screen screen = nifty.getCurrentScreen();
		Element serverListPanel = screen.findElementByName("ServerListList");
		// add new
		for(final String server : servers) {
			if(!serversInList.contains(server)) {
				serversInList.add(server);
				
				serverListPanel.add(
					new ButtonBuilder(serversInList.indexOf(server)+"", server) {{
						childLayoutCenter();
						visibleToMouse(true);
						interactOnClick("joinGameLobby(" + server + ")");
						// wrap(true);
					}}.build(nifty, screen, serverListPanel)
				);
				screen.layoutLayers();
			}
		}
		
		// remove old
		for(String server : serversInList) {
			if(!servers.contains(server)) {
				serversInList.remove(server);
				serverListPanel.findElementByName(serversInList.indexOf(server)+"").markForRemoval();
			}
		}
		
		screen.findNiftyControl("ServerListStatus", TextField.class).setText("Update interval is "+nextUpdate);
	}
	
	// @brief callback from join button
	public void joinGameLobby(String server) {
		String peer = server;
		//Application.getSession().addPeer(peer);

		try {
			Application.getInstance().getSession().connectToSession(InetAddress.getByName("localhost"), Application.GAME_PORT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Application.getInstance().changeState(GameState.GameStateId.LOBBY_STATE);
	}

	// @brief callback from create button
	public void createGameLobby() {
		//String peer = niftyDisplay.getNifty().getCurrentScreen().findNiftyControl("ServerListList", TextField.class).getText();
		//Application.getSession().addPeer(peer);
		try {
			Application.getInstance().getSession().createSession(Application.GAME_PORT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Application.getInstance().changeState(GameState.GameStateId.LOBBY_STATE);
	}

	// @brief callback from direct connect button
	public void directConnect() {
		//String peer = niftyDisplay.getNifty().getCurrentScreen().findNiftyControl("ServerListList", TextField.class).getText();
		//Application.getSession().addPeer(peer);
		Application.getInstance().changeState(GameState.GameStateId.LOBBY_STATE);
	}

	// @brief callback from quit button
	public void quit() {
		Application.getInstance().stop();
	}
	
	public MenuState() {
		niftyDisplay = Application.getInstance().getNiftyDisplay();
		serversInList = new Vector<String>();
		
		Nifty nifty = niftyDisplay.getNifty();
		
	    nifty.loadStyleFile("nifty-default-styles.xml");
	    nifty.loadControlFile("nifty-default-controls.xml");
	 
	    final MenuState state = this;
	    
	    
	    nifty.addScreen("ServerListScreen", new ScreenBuilder("Nifty Screen") {{
	    	
	    	 controller(new client.MyScreenController(state));
	 
	        layer(new LayerBuilder("ServerListLayer") {{
	        	childLayoutVertical();
	        	backgroundColor("#ccc");
	            
	        	panel(new PanelBuilder("ServerListTopPanel") {{
	            	childLayoutCenter();
    				height("10%");
    				width("100%");
			   
	        		text(new TextBuilder("ServerListTitleText") {{
	        			text("Flying Cookie Arena Deluxe");
        				font("Interface/Fonts/Default.fnt");
	               		color("#000");
        				height("100%");
        				width("100%");
	                }});
	        	}});
	            
	            panel(new PanelBuilder("ServerListList") {{
	            	childLayoutCenter();
    				height("70%");
    				width("100%");
    				
    				text(new TextBuilder("ServerListStatus") {{
	               		alignRight();
	               		valignTop();
	        			text("Loading... ");
	               		color("#000");
	       				font("Interface/Fonts/Default.fnt");
	       				height("10%");
	       				width("100%");
	                }});
	               
	               	control(new ScrollPanelBuilder("ServerListScroll") {{
	               		alignCenter();
	               		valignBottom();
	       				height("90%");
	       				width("100%");
               		}});
	               
	            }});
	            
	            panel(new PanelBuilder("ServerListButtonPanel") {{
	            	childLayoutCenter();
    				height("20%");
    				width("100%");
	 
	               	control(new TextFieldBuilder("ServerListServerNameField"){{
	       				font("Interface/Fonts/Default.fnt");
	               		color("#fff");
	               		text("");
	                    alignLeft();
	                    valignTop();
	                    height("20%");
	                    width("30%");
	                    marginLeft("20%");
	                    marginTop("20%");
	                }});
	               	
	                control(new ButtonBuilder("ServerListNewGameButton", "Start new game"){{
	                    alignRight();
	                    valignTop();
	                    height("20%");
	                    width("30%");
	                    marginRight("20%");
	                    marginTop("20%");
	                    interactOnClick("createGameLobby()");
	                }});
	                
	                control(new TextFieldBuilder("ServerListDirectConnectField"){{
	       				font("Interface/Fonts/Default.fnt");
	               		color("#fff");
	               		text("");
	                    alignLeft();
	                    valignBottom();
	                    height("20%");
	                    width("30%");
	                    marginLeft("20%");
	                    marginBottom("10%");
	                }});
	               	
	                control(new ButtonBuilder("ServerListDirectConnectButton", "Direct Connect"){{
	                    alignRight();
	                    valignBottom();
	                    height("20%");
	                    width("30%");
	                    marginRight("20%");
	                    marginBottom("10%");
	                    interactOnClick("directConnect()");
	                }});
	                

	                control(new ButtonBuilder("ServerListQuitButton", "Quit"){{
	                    alignRight();
	                    valignBottom();
	                    height("20%");
	                    width("10%");
	                    marginRight("90%");
	                    marginBottom("10%");
	                    interactOnClick("quit()");
	                }});
            	}});
	        }});
	    }}.build(nifty));
	    
	    //nifty.registerMusic("mymusic", "Interface/xyz.ogg");
	}
	
}
