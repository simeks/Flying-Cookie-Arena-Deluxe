package client;

import java.util.Map;

public interface LobbyServerCallback {
	
	public void onReceiveServerList(Map<String, String> servers, String status);
	public void onAck();
}
