package client;

import java.util.Map;

/// @brief callback from lobby server connection
public interface LobbyServerCallback {
	
	/// @brief callback when something is received
	public void onReceiveServerList(Map<String, String> servers, String status);
	
	/// @brief callback when something goes wrong
	public void onError(String msg);
}
