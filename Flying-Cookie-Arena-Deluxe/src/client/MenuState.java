package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import client.NetRead.ReceivedMessage;

import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.StyleBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.controls.console.builder.ConsoleBuilder;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
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
	    
	    nifty.getCurrentScreen().findNiftyControl("ServerListStatus", Label.class).setText("Loading... ");
	    
	    //nifty = niftyDisplay.getNifty();
	    //nifty.getCurrentScreen().findElementByName("ServerListServerNameField").setFocus();
	    
	    LobbyServerConnection serverConnection = Application.getInstance().getLobbyServerConnection();
	    serverConnection.setCallback(new LobbyServerCallback() {
			@Override
			public void onReceiveServerList(Map<String, String> servers, String status) {
				updateServerList(servers, status);
			}

			@Override
			public void onAck() {
				// TODO Auto-generated method stub
				
			}
		});
		if(!serverConnection.isConnected()) {
			serverConnection.connect();
			new Thread(serverConnection).start();
		}
		serverConnection.subscribeToServerList();
	}

	@Override
	public void exitState() {
    	InputManager inputManager = Application.getInstance().getInputManager();
    	inputManager.setCursorVisible(true);
	}

	@Override
	public void update(float dt) {

		if(Application.getInstance().getSession().getState() == Session.State.CONNECTED) {
	    	Application.getInstance().getLobbyServerConnection().close();
			Application.getInstance().changeState(GameState.GameStateId.LOBBY_STATE);
		}
		
	}
	
	public void updateServerList(Map<String, String> servers, String nextUpdate) {
		Nifty nifty = niftyDisplay.getNifty();
		Screen screen = nifty.getCurrentScreen();
		Element serverListPanel = screen.findElementByName("ServerListList2");
		
		// add new
		for (Map.Entry<String, String> entry : servers.entrySet()) {
		    final String server = entry.getKey();
		    String serverName = entry.getValue();
		    
			if(!serversInList.contains(server)) {
				serversInList.add(server);
				
				serverListPanel.add(
					new ButtonBuilder(serversInList.indexOf(server)+"", serverName) {{
						childLayoutCenter();
						visibleToMouse(true);
						interactOnClick("joinGameLobby(" + server + ")");
						marginTop(String.valueOf(10*(serversInList.indexOf(server))+1)+"%");
						height("10%");
						width("100%");
						textHAlignLeft();
						valignTop();
						alignLeft();
					}}.build(nifty, screen, serverListPanel)
				);
				screen.layoutLayers();
			}
		}
		
		// remove old
		Vector<String> removeUs = new Vector<String>();
		for(String server : serversInList) {
			if(!servers.containsKey(server)) {
				serverListPanel.findElementByName(serversInList.indexOf(server)+"").markForRemoval();
				removeUs.add(server);
				// todo: reposition to fill this hole.. 
			}
		}
		for(String server : removeUs) {
			serversInList.remove(server);
		}
		
		screen.findNiftyControl("ServerListStatus", Label.class).setText("Update interval is "+nextUpdate);
	}
	
	// @brief callback from join button
	public void joinGameLobby(String server) {
		try {
			Application.getInstance().getSession().connectToSession(InetAddress.getByName("localhost"), Application.GAME_PORT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO: Do some stuff here to indicate that we're trying to join a session...
	}

	// @brief callback from create button
	public void createGameLobby() {
		try {
			Application.getInstance().getSession().createSession(Application.GAME_PORT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		String name = Application.getInstance().getNiftyDisplay().getNifty().
				getCurrentScreen().findNiftyControl("ServerListServerNameField", TextField.class).getText();
    	Application.getInstance().getLobbyServerConnection().createServer(name, Application.GAME_PORT);
    	
		Application.getInstance().changeState(GameState.GameStateId.LOBBY_STATE);
	}

	// @brief callback from direct connect button
	public void directConnect() {
		String ip = Application.getInstance().getNiftyDisplay().getNifty().
				getCurrentScreen().findNiftyControl("ServerListDirectConnectField", TextField.class).getText();
		
		SessionReliableCallback callback = new SessionReliableCallback() {

			@Override
			public void onExpire(long timeDelayed, long TTL) {
				System.out.println("fail! "+timeDelayed+">="+TTL);
			}

			@Override
			public void onAck(long timeDelayed, long TTL) {
				System.out.println("ack! "+timeDelayed+"<"+TTL);
			}

			@Override
			public void onRetry(long timeDelayed, long TTL) {
				System.out.println("retry! "+timeDelayed+"<"+TTL);
			}
		};
		Application.getInstance().getSession().setReadDelay(5000);
		try {
			Application.getInstance().getSession().connectToSession(InetAddress.getByName(ip), Application.GAME_PORT, 3000, callback);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}

	// @brief callback from quit button
	public void quit() {
		exitState();
		Application.getInstance().getLobbyServerConnection().close();
		Application.getInstance().stop();
	}
	
	public MenuState() {
		niftyDisplay = Application.getInstance().getNiftyDisplay();
		serversInList = new Vector<String>();
		
		Nifty nifty = niftyDisplay.getNifty();
		
	    nifty.loadStyleFile("nifty-default-styles.xml");
	    nifty.loadControlFile("nifty-default-controls.xml");
	 
	    // issue #609.. http://hub.jmonkeyengine.org/forum/topic/nifty-labels-inside-a-scrollpanel-control/
	    new StyleBuilder() {{
	    	id("my-scrollpanel#scrollpanel");
	    	backgroundColor("#ffff");
	    	onActiveEffect(new EffectBuilder("ImageOverlay") {{
				name("imageOverlay");
				overlay(true);
				filename("blackborder.png");
				imageMode("resize:1,30,1,1,1,30,1,1,1,30,1,1");
				post(true);
			}});
    	} }.build(nifty);
    	
    	// remove horizontal scroll
    	new StyleBuilder() {{
	    	id("nifty-horizontal-scrollbar#panel");
	    	height("0px");
	    	width("0px");
	    	childLayoutHorizontal();
    	}}.build(nifty);
    	new StyleBuilder() {{
	    	id("nifty-horizontal-scrollbar#left");
	    	height("0px");
	    	width("0px");
    	}}.build(nifty);
    	new StyleBuilder() {{
	    	id("nifty-horizontal-scrollbar#right");
	    	height("0px");
	    	width("0px");
    	}}.build(nifty);
    	new StyleBuilder() {{
    		childLayoutAbsolute();
	    	id("nifty-horizontal-scrollbar#background");
	    	height("0px");
	    	width("0px");
    	}}.build(nifty);
    	new StyleBuilder() {{
	    	id("nifty-horizontal-scrollbar#position");
	    	height("0px");
	    	width("0px");
    	}}.build(nifty);
	    
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
    				
    				control(new LabelBuilder("ServerListStatus") {{
	               		alignRight();
	               		valignTop();
	        			text("Loading... ");
	               		color("#000");
	       				font("Interface/Fonts/Default.fnt");
	       				height("10%");
	       				width("50%");
	       				textHAlignRight();
	       				marginRight("10px");
	       				textVAlignBottom();
	       				wrap(true);
	                }});
    				
	               	control(new ScrollPanelBuilder("ServerListScroll") {{
	               		alignCenter();
	               		valignBottom();
	       				height("90%");
	       				width("100%");
	       				style("my-scrollpanel");
	       				
	       				panel(new PanelBuilder("ServerListList2") {{
	    	            	childLayoutCenter();
	        				height("100%");
	        				width("100%");
	       				}});
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
