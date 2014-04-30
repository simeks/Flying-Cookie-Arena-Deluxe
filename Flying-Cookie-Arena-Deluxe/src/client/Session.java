package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Session {
	private enum State {
		DISCONNECTED,
		AWAITING_CONNECTION, // State while we're waiting for the master peer to acknowledge us.
		CONNECTED
	};
	
	private State state = State.DISCONNECTED;
	private NetRead netRead = null;
	private int myPeerId = -1;
	
	private Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
	
	/// @brief Creates a new empty session.
	public void createSession(int myPort) throws Exception {
		if(state == State.DISCONNECTED) {
			netRead = new NetRead(myPort);
			myPeerId = 0;

			state = State.CONNECTED;
		}
		else {
			// Already initialized
			throw new Exception("Session already initialized.");
		}
	}
	
	/// @brief Connects to an existing session.
	/// @param destAddr Address to the master peer of the session.
	/// @param destPort Port to the master peer of the session.
	public void connectToSession(InetAddress destAddr, int destPort, int myPort) throws Exception {
		if(state == State.DISCONNECTED) {
			netRead = new NetRead(myPort);
			state = State.AWAITING_CONNECTION;

			// We just assume that the master has the id 0, we can change that later.
			Peer masterPeer = new Peer(0, destAddr, destPort);
			peers.put(0, masterPeer);

			// Send hello to master
			HelloMessage msg = new HelloMessage(InetAddress.getLocalHost(), myPort);
			masterPeer.send(msg, true);
			
		}
		else {
			// Already initialized
			throw new Exception("Session already initialized.");
		}
		
	}
	
	
	/// @brief Sends a message to all peers.
	public void sendToAll(Message msg, boolean reliable) throws Exception {
		if(state == State.CONNECTED) {
			for(Peer peer : peers.values()) {
				peer.send(msg, reliable);	
			}
		}
		else {
			throw new Exception("Session not initialized.");
		}
	}
	
	/// @brief Sends a message to the specified peer.
	public void sentToPeer(Message msg, int peer, boolean reliable) throws Exception {
		if(state == State.CONNECTED) {
			peers.get(peer).send(msg, reliable);
		}
		else {
			throw new Exception("Session not initialized.");
		}
	}
	
	public void update() {
		if(state != State.DISCONNECTED) {
			processIncoming();
		}
	}
	
	public int getMyPeerId() {
		return myPeerId;
	}
	
	
	/// @brief Processes incoming packets.
	private void processIncoming() {
		if(state != State.DISCONNECTED) {
			DatagramPacket udpPacket;
			while((udpPacket = netRead.popPacket()) != null) {
				NetPacket packet = new NetPacket();
				try {
					packet.read(new DataInputStream(new ByteArrayInputStream(udpPacket.getData())));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// Send ACK if the packet requires that.
				if((packet.flags & NetPacket.RELIABLE) != 0) {
					Peer peer = peers.get(packet.peer);
					peer.getNetWrite().sendAck(packet.id);
				}
				
				System.out.println("Incoming packet: " + packet.msg.toString());
			}
		}
	}
}
