package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Session {
	
	private enum State {
		DISCONNECTED,
		AWAITING_CONNECTION, // State while we're waiting for the master peer to acknowledge us.
		CONNECTED
	};
	
	private DatagramSocket socket = null;
	
	private State state = State.DISCONNECTED;
	private NetRead netRead = null;
	private NetWrite netWrite = null;
	private int myPeerId = -1;
	private int masterPeerId = -1;
	private int nextPeerId = 0;
	
	private Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
	
	/// @brief Creates a new empty session.
	public void createSession(int myPort) throws Exception {
		if(state == State.DISCONNECTED) {
			socket = new DatagramSocket(myPort);
			netWrite = new NetWrite(socket);
			netRead = new NetRead(socket, netWrite);
			new Thread(netRead).start();
			
			myPeerId = 0;
			masterPeerId = 0;

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
	public void connectToSession(InetAddress destAddr, int destPort) throws Exception {
		if(state == State.DISCONNECTED) {
			socket = new DatagramSocket();
			netWrite = new NetWrite(socket);
			netRead = new NetRead(socket, netWrite);
			new Thread(netRead).start();
			
			state = State.AWAITING_CONNECTION;

			// We just assume that the master has the id 0, we can change that later.
			Peer masterPeer = new Peer(nextPeerId, netWrite, destAddr, destPort);
			peers.put(nextPeerId, masterPeer);
			masterPeerId = nextPeerId;
			++nextPeerId;
			
			// Send hello to master
			HelloMessage msg = new HelloMessage();
			masterPeer.send(msg, true);
			
		}
		else {
			// Already initialized
			throw new Exception("Session already initialized.");
		}
		
	}
	
	public void disconnect() {
		if(state != State.DISCONNECTED) {
			socket.close();
			netRead.stop();
			netRead = null;
			peers.clear();
			myPeerId = -1;
			state = State.DISCONNECTED;
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
	
	/// @brief Returns true if the local peer is the master of this session.
	public boolean isMaster() {
		return (myPeerId == masterPeerId);
	}
	
	/// @brief Processes incoming packets.
	private void processIncoming() {
		if(state != State.DISCONNECTED) {
			NetRead.ReceivedMessage recvMsg = null;
			while((recvMsg = netRead.popMessage()) != null) {
				switch(recvMsg.msg.type) {
					case HELLO:
					{
						HelloMessage helloMsg = (HelloMessage)recvMsg.msg;
						
						// If the peer id is invalid (Not set) and we are the master, generate and send a new ID to the peer.
						if(helloMsg.peer < 0 && isMaster()) {
							System.out.println("New peer connected : " + recvMsg.senderAddr.getHostAddress() + ":" + recvMsg.senderPort);
							
							Peer peer = new Peer(nextPeerId++, netWrite, recvMsg.senderAddr, recvMsg.senderPort);
							
							PeerIdMessage idMsg = new PeerIdMessage(peer.getId());
							peer.send(idMsg, true);
							
							// Send peer list
							PeerListMessage listMsg = new PeerListMessage();
							
							for(Peer p : peers.values()) {
								listMsg.peers.add(listMsg.new RawPeer(p.getId(), p.getDestAddr(), p.getDestPort()));
							}
							
							peer.send(listMsg, true);
							
							peers.put(peer.getId(), peer);
						}
					}
					break;
					case PEER_ID:
					{
						// Update my peer id
						myPeerId = ((PeerIdMessage)recvMsg.msg).peerId;
					
						System.out.println("Peer ID assigned : " + myPeerId);
						
						// Update master peer id if needed
						if(masterPeerId != recvMsg.msg.peer && !isMaster()) {
							Peer masterPeer = peers.get(masterPeerId);
							peers.remove(masterPeerId);
							
							masterPeer.setId(recvMsg.msg.peer);
							peers.put(masterPeer.getId(), masterPeer);
							
							masterPeerId = recvMsg.msg.peer;
						}
					}
					break;
					case PEER_LIST:
					{
						System.out.println("Peer list:");
						for(PeerListMessage.RawPeer p : ((PeerListMessage)recvMsg.msg).peers) {
							Peer newPeer = new Peer(p.peerId, netWrite, p.addr, p.port);
							peers.put(p.peerId, newPeer);

							System.out.println("Peer: " + p.addr.getHostAddress() + ":" + p.port);
							
							// Send hello to new peer
						}
						
					}
					break;
				}

			}
		}
	}
	
}
