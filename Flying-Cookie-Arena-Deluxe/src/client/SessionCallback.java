package client;

public interface SessionCallback {
	public void onSuccess();
	public void onFailure(String error);
	
	/// Called when the session got disconnected.
	public void onDisconnect(String reason);
	
	/// Called when a peer is disconnected from the session.
	public void onPeerDisconnect(int peerId, String reason);
	
	/// Called when there's a new master peer assigned.
	public void onNewMaster(int oldMaster, int newMaster);
}
