package client;

public interface SessionCallback {
	
	/// @brief when a session to connected
	public void onSuccess();
	
	/// @brief when a session failed to connect
	public void onFailure(String error);
	
	/// @brief Called when the session got disconnected.
	public void onDisconnect(String reason);
	
	/// @brief Called when a peer is disconnected from the session.
	public void onPeerDisconnect(int peerId, String reason);
	
	/// @brief Called when there's a new master peer assigned.
	public void onNewMaster(int oldMaster, int newMaster);
}
