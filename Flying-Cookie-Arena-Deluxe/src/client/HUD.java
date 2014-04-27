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
	static final float pictureSize = 64.0f; // Size in pixels of the small "tool" pictures.

	private BitmapText weaponText;
	private BitmapText tooltipText;
	
	private Node guiNode;
	
	public enum Weapon
	{
		NONE
		// TODO: Add weapons here
	};
	
	
	private Weapon activeWeapon = Weapon.NONE;
	
	// Hides the currently visible tool
	private void removeActiveTool()
	{
		activeWeapon = Weapon.NONE;
		weaponText.setText("");
	}
	
	// Changes the active weapon indicator on the hud
	public void changeWeapon(Weapon weapon)
	{
		Weapon previousWeapon = activeWeapon;
		removeActiveTool();
		
	}
	public void setToolTip(String tooltip)
	{
		if(tooltip != "")
		{
			tooltipText.setText("ToolTip: " + tooltip);
		}
		else
		{
			tooltipText.setText("");
		}
	}
	
	
	public HUD(AssetManager assetManager, Node parentNode)
	{ 
		BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Console.fnt");
		guiNode = new Node("HUD");
		parentNode.attachChild(guiNode);

		weaponText = new BitmapText(guiFont, false);          
		weaponText.setSize(guiFont.getCharSet().getRenderedSize());
		weaponText.setColor(ColorRGBA.White);
		weaponText.setText(""); 
		weaponText.setLocalTranslation(20, pictureSize + 40, 0); // position
		
		guiNode.attachChild(weaponText);
		
		tooltipText = new BitmapText(guiFont, false);          
		tooltipText.setSize(guiFont.getCharSet().getRenderedSize());
		tooltipText.setColor(ColorRGBA.White);
		tooltipText.setText(""); 
		tooltipText.setLocalTranslation(20, pictureSize + 80, 0); // position

		guiNode.attachChild(tooltipText);
		
		
		guiNode.setQueueBucket(Bucket.Gui);
		
	}
}
