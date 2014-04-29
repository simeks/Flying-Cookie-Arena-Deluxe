package client;

import java.util.HashMap;
import java.util.Map;

public class Session {
	Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
	
	/// @brief Sends a message to all peers.
	public void sendToAll(boolean reliable) {
		
	}
	
	/// @brief Sends a message to the specified peer.
	public void sentToPeer(int peer, boolean reliable) {
		peers.get(peer).send(reliable);
	}
}
