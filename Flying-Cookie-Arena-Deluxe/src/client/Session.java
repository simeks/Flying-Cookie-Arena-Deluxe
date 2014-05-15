package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Session {
	static final private int PEER_TIMEOUT = 15000; // Number of milliseconds without activity before we timeout a peer

	public enum State {
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
	private long readDelay = 0;

	private SessionCallback sessionCallback;

	private Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
	private Map<Message.Type, MessageEffect> messageEffects = new EnumMap<Message.Type, MessageEffect>(
			Message.Type.class);
	

	// / @brief Creates a new empty session.
	public void createSession(int myPort, SessionCallback c) throws Exception {
		if (state == State.DISCONNECTED) {
			socket = new DatagramSocket(myPort);
			netWrite = new NetWrite(socket);
			netRead = new NetRead(socket, netWrite);
			netRead.setReadDelay(readDelay);
			new Thread(netRead).start();

			myPeerId = 0;
			masterPeerId = 0;
			nextPeerId = myPeerId + 1;

			state = State.CONNECTED;
			sessionCallback = c;
			
		} else {
			// Already initialized
			throw new Exception("Session already initialized.");
		}
	}

	public void setConnectionCallback(SessionCallback c) {
		sessionCallback = c;
	}

	// / @brief Connects to an existing session.
	// / @param destAddr Address to the master peer of the session.
	// / @param destPort Port to the master peer of the session.
	public void connectToSession(InetAddress destAddr, int destPort)
			throws Exception {
		connectToSession(destAddr, destPort, null);
	}

	public void connectToSession(InetAddress destAddr, int destPort,
			SessionCallback c) throws Exception {
		setConnectionCallback(c);
		if (state == State.DISCONNECTED) {
			socket = new DatagramSocket();
			netWrite = new NetWrite(socket);
			netRead = new NetRead(socket, netWrite);
			netRead.setReadDelay(readDelay);
			new Thread(netRead).start();

			state = State.AWAITING_CONNECTION;

			// We just assume that the master has the id 0, we can change that
			// later.
			Peer masterPeer = new Peer(nextPeerId, netWrite, destAddr, destPort);
			peers.put(nextPeerId, masterPeer);
			masterPeerId = nextPeerId;
			++nextPeerId;

			// Send hello to master
			HelloMessage msg = new HelloMessage();
			masterPeer.send(msg, true);
			

		} else {
			// Already initialized
			throw new Exception("Session already initialized.");
		}

	}

	public void disconnect() {
		if (state != State.DISCONNECTED) {
			if(sessionCallback != null) {
				sessionCallback.onDisconnect("Disconnected.");
			}
			cleanup();
			state = State.DISCONNECTED;
		}
	}
	

	// / @brief for debuging and testing, sets read delay in ms on alla packets
	// read from socket.
	public void setReadDelay(long delay) {
		readDelay = delay;
		if (state != State.DISCONNECTED) {
			netRead.setReadDelay(delay);
		}
	}

	// / @brief Sends a message to all peers.
	public void sendToAll(Message msg, boolean reliable) throws Exception {
		if (state == State.CONNECTED) {
			for (Peer peer : peers.values()) {
				peer.send(msg, true);
			}
		} else {
			throw new Exception("Session not initialized.");
		}
	}

	// / @brief Sends a message to the specified peer.
	public void sendToPeer(Message msg, int peer, boolean reliable)
			throws Exception {
		if (state == State.CONNECTED) {
			peers.get(peer).send(msg, reliable);
		} else {
			throw new Exception("Session not initialized.");
		}
	}

	public void update() {
		if (state != State.DISCONNECTED) {
			netWrite.update();
			processIncoming();
			
			Iterator<Map.Entry<Integer, Peer>> it = peers.entrySet().iterator();
			while(it.hasNext()) {
				Peer p = it.next().getValue();
				long currentTime = System.currentTimeMillis();
				
				if((currentTime - p.getLastHeartbeat()) > PEER_TIMEOUT) { // Peer timed out, kick him!
					if(sessionCallback != null) {
						sessionCallback.onPeerDisconnect(p.getId(), "Peer " + p.getId() + "  timed out");
					}
					it.remove();
					
					// Notify all the connected peers that the peer timed out
					try {
						sendToAll(new PeerTimeOutMessage(p.getId()), true);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if((currentTime - p.getLastHeartbeat()) > 5000) { // No activity for 5 seconds, ping to make sure peer is alive
					// Make sure not to spam a ping every frame
					if((currentTime - p.getLastPing()) > 5000) {
						sendPing(p.getId());
						p.setLastPing(currentTime);
					}
				}
				
			}

		}
	}

	public int getMyPeerId() {
		return myPeerId;
	}

	// / @brief Returns true if the local peer is the master of this session.
	public boolean isMaster() {
		return (myPeerId == masterPeerId);
	}

	public State getState() {
		return state;
	}

	public void registerEffect(Message.Type type, MessageEffect effect) {
		messageEffects.put(type, effect);
	}

	public void unregisterEffect(Message.Type type) {
		messageEffects.remove(type);
	}

	// / @brief Processes incoming packets.
	private void processIncoming() {
		if (state != State.DISCONNECTED) {
			NetRead.ReceivedMessage recvMsg = null;
			while ((recvMsg = netRead.popMessage()) != null) {
				if (recvMsg.msg.type == Message.Type.HELLO) {
					HelloMessage helloMsg = (HelloMessage) recvMsg.msg;

					// If the peer id is invalid (Not set) and we are the
					// master, generate and send a new ID to the peer.
					if (helloMsg.peer < 0 && isMaster()) {
						System.out.println("New peer connected : "
								+ recvMsg.senderAddr.getHostAddress() + ":"
								+ recvMsg.senderPort);

						Peer peer = new Peer(nextPeerId++, netWrite,
								recvMsg.senderAddr, recvMsg.senderPort);

						for (Peer p : peers.values()) {
							if (p.equals(peer)) {
								System.out.println("Host "
										+ recvMsg.senderAddr.getHostAddress()
										+ ":" + recvMsg.senderPort
										+ " alredy exist!");
								// TODO: send error message to client.
								// peer.send(new ErrorMessage( ... ), true);
								return;
							}
						}
						PeerIdMessage idMsg = new PeerIdMessage(peer.getId());
						peer.send(idMsg, true);

						// Send peer list
						PeerListMessage listMsg = new PeerListMessage();

						for (Peer p : peers.values()) {
							listMsg.peers.add(listMsg.new RawPeer(p.getId(), p
									.getDestAddr(), p.getDestPort()));
						}
						// We also addd ourself (the master peer) to the end of the list.
						listMsg.peers.add(listMsg.new RawPeer(getMyPeerId(), null, -1));

						// TODO: aggregate?
						peer.send(listMsg, true);

						peers.put(peer.getId(), peer);
					} else {
						// Otherwise we just put the new peer into our peer list
						if (helloMsg.peer >= 0
								&& !peers.containsKey(helloMsg.peer)) {
							peers.put(helloMsg.peer, new Peer(helloMsg.peer,
									netWrite, recvMsg.senderAddr,
									recvMsg.senderPort));
						}
					}

				} else if (recvMsg.msg.type == Message.Type.PEER_ID) {
					// Update my peer id
					myPeerId = ((PeerIdMessage) recvMsg.msg).peerId;

					System.out.println("Peer ID assigned : " + myPeerId);

					// Update master peer id if needed
					if (masterPeerId != recvMsg.msg.peer && !isMaster()) {
						System.out.println("Master peer : (local: " + masterPeerId + ", global: " + recvMsg.msg.peer + ")");
						Peer masterPeer = peers.get(masterPeerId);
						peers.remove(masterPeerId);

						masterPeer.setId(recvMsg.msg.peer);
						peers.put(masterPeer.getId(), masterPeer);

						masterPeerId = recvMsg.msg.peer;
					}

				} else if (recvMsg.msg.type == Message.Type.PEER_LIST) {
					System.out.println("Peer list:");
					state = State.CONNECTED;
					for (PeerListMessage.RawPeer p : ((PeerListMessage) recvMsg.msg).peers) {
						// Skip the master peer as we have already added that peer to the list
						if(p.peerId == masterPeerId || p.addr == null) {
							continue;
						}
						
						Peer newPeer = new Peer(p.peerId, netWrite, p.addr,
								p.port);
						peers.put(p.peerId, newPeer);

						System.out.println("Peer: " + p.addr.getHostAddress()
								+ ":" + p.port);

						// Send hello to new peer
						try {
							sendToPeer(new HelloMessage(), p.peerId, true);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					sessionCallback.onSuccess();
					
				} else if (recvMsg.msg.type == Message.Type.PING) {
					System.out.println("PING from peer " + recvMsg.msg.peer);
					
					// Return a pong message
					try {
						sendToPeer(new PongMessage(), recvMsg.msg.peer, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (recvMsg.msg.type == Message.Type.PONG) {
					System.out.println("PONG from peer " + recvMsg.msg.peer);
				} else if (recvMsg.msg.type == Message.Type.PEER_TIMED_OUT) {
					// A peer desynced from the network so we will need to kick him
					
					PeerTimeOutMessage msg = (PeerTimeOutMessage)recvMsg.msg;
					if(peers.containsKey(msg.timedOutPeerId)) { 
						// First we send him a message that he have desynced (If we actually can reach him)
						try {
							sendToPeer(new KickedMessage("Desync."), msg.timedOutPeerId, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(sessionCallback != null) {
							sessionCallback.onPeerDisconnect(msg.timedOutPeerId, "Peer " + msg.timedOutPeerId + " timed out.");
						}
						// We then remove him from our peers
						peers.remove(msg.timedOutPeerId);
						
					}
					
				} else if(recvMsg.msg.type == Message.Type.KICKED) {
					// We just got kicked :(
					if(sessionCallback != null) {
						sessionCallback.onDisconnect("Kicked: " + ((KickedMessage)recvMsg.msg).reason);
					}
					cleanup();
					state = State.DISCONNECTED;

				}
				
				
				if(peers.containsKey(recvMsg.msg.peer)) {
					peers.get(recvMsg.msg.peer).heartbeat();
				}
				
				
				if (messageEffects.containsKey(recvMsg.msg.type)) {
					messageEffects.get(recvMsg.msg.type).execute(recvMsg.msg);
				}

			}
		}
	}
	
	/// Sends a ping to the specified peer.
	private void sendPing(int peer) { 
		System.out.println("PING to peer " + peer);
		try {
			sendToPeer(new PingMessage(), peer, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void cleanup() {
		socket.close();
		netRead.stop();
		netRead = null;
		netWrite = null;
		peers.clear();
		myPeerId = -1;
			
		
	}

}
