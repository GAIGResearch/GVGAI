package video.gui.elements;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */

public class FrameLabel extends JLabel
{
	private String thisStringFrame;
	
	public FrameLabel() {
		super();
		setBounds(27, 47, 300, 200);
		setThisStringFrame("");
	}
	
	public FrameLabel(int anchorX, int anchorY, int width, int height)
	{
		super();
		setBounds(anchorX, anchorY, width, height);
	}
	
	public void updateFrame(String frame)
	{
		BufferedImage img = bufferingImage(frame);
		
		ImageIcon imgThisImg = new ImageIcon(
				img.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_DEFAULT));
		this.setIcon(imgThisImg);
		setThisStringFrame(frame);
	}

	/**
	 * @param frame
	 * @return
	 */
	public BufferedImage bufferingImage(String frame) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(frame));
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return img;
	}

	public String getThisStringFrame() {
		return thisStringFrame;
	}

	public void setThisStringFrame(String thisFrame) {
		this.thisStringFrame = thisFrame;
	}
	
}