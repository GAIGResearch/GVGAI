package video.basics;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;

import core.game.Observation;
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
	
	public void saveGameState(File file, ArrayList<Observation>[][] gamestate) {
		JSONObject obj = new JSONObject();
		Gson gson = new Gson();
		int counter = 0;
		for (ArrayList<Observation>[] list : gamestate) {
			JSONArray layer = new JSONArray();
			for(ArrayList<Observation> arrayList : list) {
				JSONArray row = new JSONArray();
				for(Observation obs : arrayList) {
					String obsJson = gson.toJson(obs);
					row.add(obsJson);
				}
				layer.add(row);
			}
			obj.put(counter, layer);
			counter++;
			
		}
		try(FileWriter writer = new FileWriter(file)) {
			writer.write(obj.toJSONString());
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
}