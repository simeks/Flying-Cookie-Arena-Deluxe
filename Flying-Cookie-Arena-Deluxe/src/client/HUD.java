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
	
	private Picture hammerPic;
	private Picture handPic;
	private Picture brushPic;

	private BitmapText toolText;
	private BitmapText tooltipText;
	
	private Node guiNode;
	
	public enum Tool
	{
		NONE,
		HAMMER,
		HAND,
		BRUSH
	};
	
	public enum BrushColor
	{
		RED,
		GREEN,
		BLUE
	};
	
	private Tool activeTool = Tool.NONE;
	private BrushColor brushColor = BrushColor.RED;
	
	// Hides the currently visible tool
	private void removeActiveTool()
	{
		if(activeTool == Tool.HAMMER)
		{
			guiNode.detachChild(hammerPic);
		}
		else if(activeTool == Tool.HAND)
		{
			guiNode.detachChild(handPic);
		}
		else if(activeTool == Tool.BRUSH)
		{
			guiNode.detachChild(brushPic);
		}
		activeTool = Tool.NONE;
		toolText.setText("");
	}
	
	// Changes the active tool indicator in the hud
	public void changeTool(Tool tool)
	{
		Tool previousTool = activeTool;
		removeActiveTool();
		
		if(tool == Tool.HAMMER)
		{
			guiNode.attachChild(hammerPic);
			activeTool = Tool.HAMMER;
			toolText.setText("Hammer");
		}
		else if(tool == Tool.HAND)
		{
			guiNode.attachChild(handPic);
			activeTool = Tool.HAND;
			toolText.setText("Hand");
		}
		else if(tool == Tool.BRUSH)
		{
			guiNode.attachChild(brushPic);
			activeTool = Tool.BRUSH;
			
			// Let the user cycle through the colors by pressing the brush key multiple times
			if(previousTool == Tool.BRUSH)
			{
				if(brushColor == BrushColor.RED)
				{
					brushColor = BrushColor.GREEN;
					toolText.setText("Paint brush (Green) (Press repeatedly to cycle between the available colors)");
				}
				else if(brushColor == BrushColor.GREEN)
				{
					brushColor = BrushColor.BLUE;
					toolText.setText("Paint brush (Blue) (Press repeatedly to cycle between the available colors)");
				}
				else if(brushColor == BrushColor.BLUE)
				{
					brushColor = BrushColor.RED;
					toolText.setText("Paint brush (Red) (Press repeatedly to cycle between the available colors)");
				}
				
			}
			else
			{
				brushColor = BrushColor.RED;
				toolText.setText("Paint brush (Red) (Press repeatedly to cycle between the available colors)");
			}
		}
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
	
	public Tool getActiveTool()
	{
		return activeTool;
	}
	public BrushColor getBrushColor()
	{
		return brushColor;
	}
	
	public HUD(AssetManager assetManager, Node parentNode)
	{ 
		BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Console.fnt");
		guiNode = new Node("HUD");
		parentNode.attachChild(guiNode);

		toolText = new BitmapText(guiFont, false);          
		toolText.setSize(guiFont.getCharSet().getRenderedSize());
		toolText.setColor(ColorRGBA.White);
		toolText.setText(""); 
		toolText.setLocalTranslation(20, pictureSize + 40, 0); // position
		
		guiNode.attachChild(toolText);
		
		hammerPic = new Picture("Hammer");
		hammerPic.setImage(assetManager, "HUD/hammer.png", false);
		hammerPic.setWidth(pictureSize);
		hammerPic.setHeight(pictureSize);
		hammerPic.setPosition(20, 20);
		
		
		handPic = new Picture("Hand");
		handPic.setImage(assetManager, "HUD/hand.png", false);
		handPic.setWidth(pictureSize);
		handPic.setHeight(pictureSize);
		handPic.setPosition(20, 20);
		
		brushPic = new Picture("Brush");
		brushPic.setImage(assetManager, "HUD/brush.png", false);
		brushPic.setWidth(pictureSize);
		brushPic.setHeight(pictureSize);
		brushPic.setPosition(20, 20);
		

		tooltipText = new BitmapText(guiFont, false);          
		tooltipText.setSize(guiFont.getCharSet().getRenderedSize());
		tooltipText.setColor(ColorRGBA.White);
		tooltipText.setText(""); 
		tooltipText.setLocalTranslation(20, pictureSize + 80, 0); // position

		guiNode.attachChild(tooltipText);
		
		
		guiNode.setQueueBucket(Bucket.Gui);
		
	}
}
