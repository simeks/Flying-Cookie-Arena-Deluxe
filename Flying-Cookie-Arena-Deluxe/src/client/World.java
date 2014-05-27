package client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.terrain.heightmap.HillHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

public class World {
	private BulletAppState bulletAppState;
	private AssetManager assetManager;
	private Node rootNode; // Root scene node for this world and its content
	private TerrainQuad terrain;
	private int nextEntityId = 0;
	
	// Our different collision groups for jmonkey
	public static final int COLLISION_GROUP_CRATE = PhysicsCollisionObject.COLLISION_GROUP_01;
	public static final int COLLISION_GROUP_CHARACTER = PhysicsCollisionObject.COLLISION_GROUP_02;
	public static final int COLLISION_GROUP_FLAG = PhysicsCollisionObject.COLLISION_GROUP_03;
	public static final int COLLISION_GROUP_CAMP_FIRE = PhysicsCollisionObject.COLLISION_GROUP_04;
	public static final int COLLISION_GROUP_TERRAIN = PhysicsCollisionObject.COLLISION_GROUP_05;
	public static final int COLLISION_GROUP_NOTHING = PhysicsCollisionObject.COLLISION_GROUP_16;
	
	// List of all crates in the world
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	/// @return A list of all the entities in the world.
	public ArrayList<Entity> getEntities() {
		return entities;
	}

	/// @brief audio nodes
	private AudioNode[] audio_jump;
	private AudioNode[] audio_joined;
	private AudioNode[] audio_fire;
	private AudioNode[] audio_backstab;
	private AudioNode[] audio_dc;
	
	/// @brief Plays the jump sound effect at the specified position.
	public void playJump(Vector3f position) {
		Random Dice = new Random();
		int n = Dice.nextInt(audio_jump.length);
		audio_jump[n].setLocalTranslation(position);
		audio_jump[n].playInstance();
	}

	/// @brief Plays the join game sound effect.
	public void playJoinedGame() {
		Random Dice = new Random();
		int n = Dice.nextInt(audio_joined.length);
		audio_joined[n].playInstance();
	}

	/// @brief Plays the "flag steal" sound effect at the specified position.
	public void playBackstab(Vector3f position) {
		Random Dice = new Random();
		int n = Dice.nextInt(audio_backstab.length);
		audio_backstab[n].setLocalTranslation(position);
		audio_backstab[n].playInstance();
	}
	
	/// @brief Plays the "ignite campfire" sound effect at the specified position.
	public void playFire(Vector3f position) {
		Random Dice = new Random();
		int n = Dice.nextInt(audio_fire.length);
		audio_fire[n].setLocalTranslation(position);
		audio_fire[n].playInstance();
	}

	/// @brief Plays the disconnect sound effect.
	public void playDisconnect() {
		Random Dice = new Random();
		int n = Dice.nextInt(audio_dc.length);
		audio_dc[n].playInstance();
	}
	
	/// @brief Initializes the sound effects.
	public void initAudio() {

		String fires[] = { "fire1.wav", "fire2.wav", "fire3.wav" };
		audio_fire = new AudioNode[fires.length];
		for(int i=0; i<fires.length; i++) {
			audio_fire[i] = new AudioNode(assetManager, "Sounds/Effects/"+fires[i], false);
			audio_fire[i].setPositional(true);
			audio_fire[i].setLocalTranslation(new Vector3f(0,0,0)); // changes later
			audio_fire[i].setLooping(false);
			audio_fire[i].setVolume(1);
			getRootNode().attachChild(audio_fire[i]);
		}
		
		String jumps[] = { "jump1.wav", "jump2.wav", "jump3.wav" };
		audio_jump = new AudioNode[jumps.length];
		for(int i=0; i<jumps.length; i++) {
			audio_jump[i] = new AudioNode(assetManager, "Sounds/Effects/"+jumps[i], false);
			audio_jump[i].setPositional(true);
			audio_jump[i].setLocalTranslation(new Vector3f(0,0,0)); // changes later
			audio_jump[i].setLooping(false);
			audio_jump[i].setVolume(0.5f);
			getRootNode().attachChild(audio_jump[i]);
		}

		String backstab[] = { "backstab1.wav", "backstab2.wav", "backstab3.wav" };
		audio_backstab = new AudioNode[backstab.length];
		for(int i=0; i<backstab.length; i++) {
			audio_backstab[i] = new AudioNode(assetManager, "Sounds/Effects/"+backstab[i], false);
			audio_backstab[i].setPositional(true);
			audio_backstab[i].setLocalTranslation(new Vector3f(0,0,0)); // changes later
			audio_backstab[i].setLooping(false);
			audio_backstab[i].setVolume(1);
			getRootNode().attachChild(audio_backstab[i]);
		}


		String dc[] = { "dc1.wav", "dc2.wav", "dc3.wav" };
		audio_dc = new AudioNode[dc.length];
		for(int i=0; i<dc.length; i++) {
			audio_dc[i] = new AudioNode(assetManager, "Sounds/Effects/"+dc[i], false);
			audio_dc[i].setPositional(false);
			audio_dc[i].setLooping(false);
			audio_dc[i].setVolume(1);
			getRootNode().attachChild(audio_dc[i]);
		}

		String joined[] = { "newplayerjoined.wav" };
		audio_joined = new AudioNode[joined.length];
		for(int i=0; i<joined.length; i++) {
			audio_joined[i] = new AudioNode(assetManager, "Sounds/Effects/"+joined[i], false);
			audio_joined[i].setPositional(false);
			audio_joined[i].setLooping(false);
			audio_joined[i].setVolume(1);
			getRootNode().attachChild(audio_joined[i]);
		}
	}
    
    /* pause game physics */
    public void pausePhysics(){
        
            try {this.bulletAppState.setEnabled(false);
                
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("Failed to pause game physics...");
            }
        }
    
    /* unpause game physics */
    public void unpausePhysics(){
        
            try {this.bulletAppState.setEnabled(true);
                
            } catch (Exception e){
                e.printStackTrace();
                System.out.println("Failed to unpause game physics...");
            }
        }
    
   /* reset all physics when the game resets */
    public void resetPhysics(){
    	 /*
         * TODO
         */
    	this.bulletAppState = null;
    	try {
    		this.bulletAppState = Application.getInstance().getBulletAppState();
    	} catch (Exception e){
    		e.printStackTrace();
    		System.out.println("Could not retrieve default physics");
    		
    	}
    	
    }
    
    
    /// Constructor
	public World() {
		this.bulletAppState = Application.getInstance().getBulletAppState();
		this.assetManager = Application.getInstance().getAssetManager();

		// Create a root scene node for this world, all objects in the world will
		// later be attached to this node.
		rootNode = new Node("room");
		Application.getInstance().getRootNode().attachChild(rootNode);

		// debug collisions
		//bulletAppState.getPhysicsSpace().enableDebug(assetManager);
		
		// Terrain
		{
			Material terrainMaterial = new Material(assetManager,
					"Materials/TerrainLighting.j3md");

			Texture texture = assetManager
					.loadTexture("Textures/Terrain/splat/grass.jpg");
			texture.setWrap(WrapMode.Repeat);
			terrainMaterial.setTexture("DiffuseMap", texture);
			terrainMaterial.setFloat("DiffuseMap_0_scale", 50.0f);

			texture = assetManager
					.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
			texture.setWrap(WrapMode.Repeat);
			terrainMaterial.setTexture("NormalMap", texture);

			AbstractHeightMap heightmap = null;
			try {
				heightmap = new HillHeightMap(1025, 1, 1.0f, 2.0f, 255);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			heightmap.load();

			int patchSize = 65;
			terrain = new TerrainQuad("Terrain", patchSize, 1025,
					heightmap.getHeightMap());
			terrain.setMaterial(terrainMaterial);
			rootNode.attachChild(terrain);

			RigidBodyControl rigidBodyControl = new RigidBodyControl(0.0f);
			terrain.addControl(rigidBodyControl);

			rigidBodyControl.setCollisionGroup(COLLISION_GROUP_TERRAIN);

			bulletAppState.getPhysicsSpace().add(rigidBodyControl);
		}
		
		// Setup ambient light source
		AmbientLight ambientLight = new AmbientLight();
		ambientLight.setColor(ColorRGBA.White.mult(0.3f));
		rootNode.addLight(ambientLight);

		// Setup directional light source
		DirectionalLight directionalLight = new DirectionalLight();
		directionalLight.setColor(ColorRGBA.White.mult(0.7f));
		directionalLight.setDirection(new Vector3f(-0.5f, -0.85f, 0.5f)
				.normalizeLocal());
		
		rootNode.addLight(directionalLight);

		// Create the sky
		rootNode.attachChild(SkyFactory.createSky(assetManager,
				"Textures/Sky/Bright/BrightSky.dds", false));
		
		// sound
		initAudio();
	}
	
	/// @brief Updates all entities in the world.
	public void update(float tpf) {
		for(Entity entity : entities) {
			entity.update(tpf);
		}
	}
	
	/// Tries to interact with any object found in the specified direction
	/// closestDistance is the closest distance to an object required to interact
	/// with it.
	public void interactWithItem(Character character, Vector3f direction, float closestDistance) {
		Ray ray = new Ray(character.getPosition(), direction.normalize());

		Entity closestObject = null;
		for (Entity o : entities) {
			// We can't interact with our own character
			if(o instanceof Character && ((Character)o).isOwner()) {
				continue;
			}
			
			CollisionResults results = new CollisionResults();
			o.collideWith(ray, results);

			if (results.size() > 0) {
				if (results.getClosestCollision().getDistance() < closestDistance) {
					closestObject = o;
					closestDistance = results.getClosestCollision().getDistance();
				}
			}
		}

		// Interact with the closest object, if any
		if (closestObject != null) {
			closestObject.interact(character);
		}
	}	
	
	/// Broadcasts the current state of all entities owned by this peer to all other peers.
	/// This is used for frequent state regeneration and is meant to be called frequently 
	///		during the session. All messages will be sent unreliably as we don't care if we
	///		drop a few packets as the content will get outdated really quickly. 
	public void broadcastWorldState() {
		Session session = Application.getInstance().getSession();
		for(Entity entity : entities) {
			if(entity.getOwner() == session.getMyPeerId()) {
				EntityStateMessage msg = entity.buildStateMessage();
				if(msg != null) {
					try {
						session.sendToAll(msg, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/// @brief Broadcasts the creation of the world, sending CREATE_ENTITY messages for all entities.
	/// This is meant to be sent to newly connected peers as they have no spawned entities.
	/// @param peer The peer that should receive the messages.
	public void broadcastWorldCreation(int peer) {
		Session session = Application.getInstance().getSession();
		for(Entity entity : entities) {
			if(entity.getOwner() == session.getMyPeerId()) {
				try {
					CreateEntityMessage msg = new CreateEntityMessage(entity.getId(), entity.getType(), 
							entity.getPosition(), entity.getRotation());
					session.sendToPeer(msg, peer, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/// @brief Processes the specified state message.
	public void processEntityState(EntityStateMessage msg) {
		Entity entity = getEntity(msg.entityId);
		if(entity != null) {
			entity.processStateMessage(msg);
		}
	}

	/// @brief Processes an incoming entity event message.
	public void processEntityEvent(EntityEventMessage msg) {
		Entity entity = getEntity(msg.entityId);
		if(entity != null) {
			entity.processEventMessage(msg);
		}
	}
	
	
	/// @brief Processes an incoming entity creation message.
	public void processCreateEntity(CreateEntityMessage msg) {
		Entity entity = null;
		switch(msg.entityType) {
		case CAMP_FIRE:
			entity = new CampFire(msg.peer, this, msg.entityId, msg.position);
			entity.setCollisionGroup(COLLISION_GROUP_CAMP_FIRE);
			break;
		case CHARACTER:
			entity = new Character(msg.peer, this, msg.entityId, msg.position, 1.0f);
			entity.setCollisionGroup(COLLISION_GROUP_CHARACTER);
			break;
		case AI_CHARACTER:
			entity = new AICharacter(msg.peer, this, msg.entityId, msg.position);
			entity.setCollisionGroup(COLLISION_GROUP_CHARACTER);
			break;
		case CRATE:
			entity = new Crate(msg.peer, this, msg.entityId, msg.position);
			entity.setCollisionGroup(COLLISION_GROUP_CRATE);
			break;
		case FLAG:
			entity = new Flag(msg.peer, this, msg.entityId, msg.position);
			entity.setCollisionGroup(COLLISION_GROUP_FLAG);
			break;
		}
		if(entity != null) {
			entity.getSpatial().setUserData("id", msg.entityId);
			entities.add(entity);
		}
	}
	
	/// @brief Processes an incoming entity destroy message
	public void processDestroyEntity(DestroyEntityMessage msg) {
		Entity entity = getEntity(msg.entityId);
		if(entity != null) {
			entities.remove(entity);
			entity.destroy();
		}
	}
	
	/// @brief Spawns a box at the specified world coordinates
	public CampFire spawnCampFire(Vector3f position) {
		int id = generateEntityID();
		CampFire fire = new CampFire(Application.getInstance().getSession().getMyPeerId(), 
				this, id,  position);
		entities.add(fire);
		fire.setCollisionGroup(COLLISION_GROUP_CAMP_FIRE);
		fire.getSpatial().setUserData("id", id);

		broadcastNewEntity(fire, position);
		
		return fire;
	}


	/// @brief Spawns a box at the specified world coordinates
	public Crate spawnBox(Vector3f position) {
		int id = generateEntityID();
		Crate crate = new Crate(Application.getInstance().getSession().getMyPeerId(), 
				this, id, position);
		entities.add(crate);
		crate.setCollisionGroup(COLLISION_GROUP_CRATE);
		crate.getSpatial().setUserData("id", id);

		broadcastNewEntity(crate, position);
		
		return crate;
	}
	
	/// @brief Spawns a character entity.
	/// @param position Initial position of the entity.
	public Character spawnCharacter(Vector3f position) {
		int id = generateEntityID();
		Character character = new Character(Application.getInstance().getSession().getMyPeerId(), 
				this, id, position, 1.0f);
		entities.add(character);
		character.setCollisionGroup(COLLISION_GROUP_CHARACTER);
		character.getSpatial().setUserData("id", id);
		
		broadcastNewEntity(character, position);
		
		return character;
	}

	/// @brief Spawns an AI-controlled character entity.
	/// @param position Initial position of the entity.
	public AICharacter spawnAICharacter(Vector3f position) {
		int id = generateEntityID();
		AICharacter character = new AICharacter(Application.getInstance().getSession().getMyPeerId(), 
				this, id, position);
		entities.add(character);
		character.setCollisionGroup(COLLISION_GROUP_CHARACTER);
		character.getSpatial().setUserData("id", id);
		
		broadcastNewEntity(character, position);
		
		return character;
	}

	/// @brief Spawns a flag entity.
	/// @param position Initial position of the entity.
	public Flag spawnFlag(Vector3f position) {
		int id = generateEntityID();
		Flag flag = new Flag(Application.getInstance().getSession().getMyPeerId(), 
				this, id, position);
		entities.add(flag);
		flag.setCollisionGroup(COLLISION_GROUP_FLAG);
		flag.getSpatial().setUserData("id", id);
		
		broadcastNewEntity(flag, position);
		
		return flag;
	}
	/// Destroys the specified entity, removing it from the world.
	public void destroyEntity(Entity entity) {
		entities.remove(entity);
		entity.destroy();
		if(entity.ownerPeer == Application.getInstance().getSession().getMyPeerId()) {
			broadcastDestroyEntity(entity);
		}
	}
	
	/// @brief Migrates all entities from one peer to another.
	///	This will destroy any peer that cannot be migrated (Like character entities).
	public void migrateEntities(int newPeer, int oldPeer) {
		Iterator<Entity> it = entities.iterator();
		while(it.hasNext()) {
			Entity entity = it.next();

			if(entity.getOwner() == oldPeer) {
				if((entity.getFlags() & Entity.FLAG_STATIC_OWNERSHIP) != 0)
				{
					// Destroy if we cannot change ownership
					entity.destroy();
					it.remove();
					
				}
				else
				{
					// Otherwise we just change owner
					entity.setOwner(newPeer);
				}
			}
		}
	}
	
	/// Clears the world, destroying any existing entities.
	public void clear() {
		for(Entity e : entities) {
			e.destroy();
		}
		entities.clear();
	}
	
	/// @return Root JMonkey node for this world.
	public Node getRootNode()
	{
		return rootNode;
	}

	/// @brief Returns the entity with the specified ID.
	public Entity getEntity(int id) {
		for(Entity entity : entities) {
			if(entity.getId() == id) {
				return entity;
			}
		}
		return null;
	}
	
	/// @brief Broadcast that we created a new entity.
	private void broadcastNewEntity(Entity entity, Vector3f position) {
		CreateEntityMessage msg = new CreateEntityMessage(entity.getId(), entity.getType(), position, entity.getRotation());
		try {
			if(Application.getInstance().getSession().getState() == Session.State.CONNECTED) {
				Application.getInstance().getSession().sendToAll(msg, true);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/// @brief Broadcast that we are destroying an entity owned by this peer.
	private void broadcastDestroyEntity(Entity entity) {
		DestroyEntityMessage msg = new DestroyEntityMessage(entity.getId());
		try {
			if(Application.getInstance().getSession().getState() == Session.State.CONNECTED) {
				Application.getInstance().getSession().sendToAll(msg, true);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/// @brief Generates a unique entity ID.
	private int generateEntityID() {
		// Combine the peer id with an entity id to generate an id that is unique over the network.
		int peerId = Application.getInstance().getSession().getMyPeerId();
		int entityId = ((nextEntityId++) & 0x0000FFFF) | ((peerId & 0x0000FFFF) << 16);
		
		return entityId;
	}

	/// @brief Processes a message about a changed ownership.
	public void processEntityOwnerChange(EntityNewOwnerMessage m) {
		Entity entity = getEntity(m.entityId);
		if(entity != null) {
			entity.setOwner(m.ownerId);
		}
	}
}
