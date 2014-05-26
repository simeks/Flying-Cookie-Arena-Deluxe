package client;

import java.util.Random;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class AICharacter extends Character {

	public AICharacter(int ownerId, World world, int entityId, Vector3f position) {
		super(ownerId, world, entityId, position, 2.0f);
		entityType = Type.AI_CHARACTER;
		flags = 0;

	}

	private Character target = null;
	private float targetTimer = 0.0f;
	
	private void findTarget() {
		
		for(Entity e : world.getEntities()) {
			if(e instanceof Character) {
				Character c = (Character)e;
				
				if((target == null) || target.getNode().getChildren().size() < c.getNode().getChildren().size()) {
					target = c;
				}
			}
		}		
	}
	
	private boolean moving = false;
	
	@Override
	public void update(float tpf) {
		
		// Only simulate AI we own
		if(isOwner()) {
			targetTimer += tpf;
			
			if(targetTimer > 1.0f) {
				findTarget();
				targetTimer = 0.0f;
			}
			
			if(target != null) {
				Vector3f dir = target.getPosition().mult(new Vector3f(1,0,1)).subtract(getPosition().mult(new Vector3f(1,0,1)));
				

				Quaternion q = new Quaternion();
				q.lookAt(dir.normalize(), Vector3f.UNIT_Y);

				setRotation(q);
				
				Random rand = new Random();
				if((dir.length() + rand.nextInt(5)) > 30) {
					
					if(!moving) {
						velocity = velocity.add(new Vector3f(0,0, 20.0f));
						moving = true;
					}
				
				}
				else {
					moving = false;
					velocity = velocity.add(new Vector3f(0,0, -20.0f));
					velocity = new Vector3f(0,0,0);
				}
			}
			

		}
		
		// TODO Auto-generated method stub
		super.update(tpf);
	}

	
}
