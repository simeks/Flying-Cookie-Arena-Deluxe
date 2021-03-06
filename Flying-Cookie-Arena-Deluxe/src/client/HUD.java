package client;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

// Heads-up display for the character view
public class HUD {
	private Node guiNode;
	private BitmapText flagText;
	private int numberOfFlags = 0;
	
	/// @brief Sets the number of flags for the flag counter in the HUD.
	public void setNumberOfFlags(int n) {
		numberOfFlags = n;
		flagText.setText("Flags: " + n);
	}
	
	/// @return The number of flags currently displayed.
	public int getNumberOfFlags() {
		return numberOfFlags;
	}
	
	/// Constructor
	/// @param assetManager JMonkey asset manager.
	/// @param parentNode The node which we should attach the HUD to.
	public HUD(AssetManager assetManager, Node parentNode)
	{ 
		BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Console.fnt");
		guiNode = new Node("HUD");
		parentNode.attachChild(guiNode);
		
		flagText = new BitmapText(guiFont, false);
		flagText.setLocalScale(2.5f);
		flagText.setLocalTranslation(20, 40, 0); // position
		flagText.setColor(ColorRGBA.White);
		guiNode.attachChild(flagText);
		

		guiNode.setQueueBucket(Bucket.Gui);
		
		setNumberOfFlags(0);
	}
}
