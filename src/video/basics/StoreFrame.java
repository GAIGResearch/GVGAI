package video.basics;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */
public class StoreFrame {
	
	public StoreFrame()
	{
		
	}

	public void saveImage(File file, JComponent panel){
        BufferedImage bi = new BufferedImage(panel.getSize().width, panel.getSize().height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        panel.paint(g);
        g.dispose();
        try{
            ImageIO.write(bi,"png",file);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}