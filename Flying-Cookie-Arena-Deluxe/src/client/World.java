package client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
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
	

	public static final int COLLISION_GROUP_CRATE = PhysicsCollisionObject.COLLISION_GROUP_01;
	public static final int COLLISION_GROUP_CHARACTER = PhysicsCollisionObject.COLLISION_GROUP_02;
	public static final int COLLISION_GROUP_FLAG = PhysicsCollisionObject.COLLISION_GROUP_03;
	public static final int COLLISION_GROUP_CAMP_FIRE = PhysicsCollisionObject.COLLISION_GROUP_04;
	public static final int COLLISION_GROUP_TERRAIN = PhysicsCollisionObject.COLLISION_GROUP_05;
	public static final int COLLISION_GROUP_NOTHING = PhysicsCollisionObject.COLLISION_GROUP_16;
	
	// List of all crates in the world
	private ArrayList<Entity> entities = new ArrayList<Entity>();

    
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
    
    
    public void resetPhysics(){
        
    }
    
    

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
				heightmap = new HillHeightMap(1025, 1000, 1.0f, 300.0f, 15);
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
	}
	
	/// @brief Updates all entities in the world.
	public void update(float tpf) {
		for(Entity entity : entities) {
			entity.update(tpf);
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
					session.sendToAll(msg, true);
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
	
	/// @brief Destroys any spawned objects.
	public void clear() {
		entities.clear();
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
			entity = new Character(msg.peer, this, msg.entityId, msg.position);
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
		}
	}
	
	// Spawns a box at the specified world coordinates
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


	// Spawns a box at the specified world coordinates
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
	
	public Character spawnCharacter(Vector3f position) {
		int id = generateEntityID();
		Character character = new Character(Application.getInstance().getSession().getMyPeerId(), 
				this, id, position);
		entities.add(character);
		character.setCollisionGroup(COLLISION_GROUP_CHARACTER);
		character.getSpatial().setUserData("id", id);
		
		broadcastNewEntity(character, position);
		
		return character;
	}

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
	
	public void destroyEntity(Entity entity) {
		entities.remove(entity);
		entity.destroy();
		broadcastDestroyEntity(entity);
	}
	
	/// @brief Removes all entities owned by the specified peer.
	public void removeEntities(int peer) {
		Iterator<Entity> it = entities.iterator();
		while(it.hasNext()) {
			Entity entity = it.next();

			if(entity.getOwner() == peer) {
				entity.destroy();
				it.remove();
			}
		}		
	}
	
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
}
