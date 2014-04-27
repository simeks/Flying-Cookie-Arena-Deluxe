package client;


public interface GameState {

	public enum GameStateId
	{
		MENU_STATE,
		LOBBY_STATE,
		MAIN_STATE
	};
	
	// Called whenever the game enters this state
	public abstract void enterState();
	
	// Called whenever the game exits this state
	public abstract void exitState();
	
	// Called once every frame
	// @param dt Time passed since last frame, in seconds.
	public abstract void update(float dt);
	
}
