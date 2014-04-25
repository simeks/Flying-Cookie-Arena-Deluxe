package client;

import client.Character.Movement;

import com.jme3.math.Vector3f;

// Simple AI that follows a target
public class AI {
	final static float minDistance = 50.0f; // Minimum distance to target
	final static float sprintDistance = 150.0f; // Distance required for the AI to start sprinting
	
	private Character character;
	private Character target = null;
	private boolean moving = false;
	
	public void setTarget(Character character)
	{
		target = character;
	}
	
	public void update(float tpf)
	{
		if(target != null)
		{
			Vector3f dir = target.getPosition().subtract(character.getPosition()); // Direction to target

			// Rotate the character to face the target
			character.setDirection(dir.normalize());
			
			if(dir.length() > sprintDistance)
			{
				// Sprint towards target
				if(!moving) // Avoid restarting the animation if we're already moving
				{
					character.startMovement(Movement.MOVE_FORWARD);
				}
				character.setSprint(true);
				moving = true;
			}
			else if(dir.length() > minDistance)
			{
				// Walk towards target
				if(!moving) // Avoid restarting the animation if we're already moving
				{
					character.startMovement(Movement.MOVE_FORWARD);
				}
				character.setSprint(false);
				moving = true;
			}
			else
			{
				character.setSprint(false);
				character.idle();
				moving = false;
			}
		}
		character.update(tpf);
	}
	
	public AI(Character character)
	{
		this.character = character;
		
	}
}
