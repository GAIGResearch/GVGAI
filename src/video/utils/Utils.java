package video.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class Utils 
{
	public static void feedComboBox(JComboBox<String> cbx, ArrayList<String> strings)
	{
		List<String> al = strings;
		// add elements to al, including duplicates
		Set<String> hs = new HashSet<>();
		hs.addAll(al);
		al.clear();
		al.addAll(hs);
		Collections.sort(al);
		for (String string : al) 
		{
			cbx.addItem(string);
		}
	}

	public static void feedComboBox(JComboBox<String> cbx, String [] strings)
	{
		for (String string : strings) 
		{
			cbx.addItem(string);
		}
	}
	
	public static boolean isValueValid(JSONArray array, String value)
	{
		for (int i = 0; i < array.size(); i++) 
		{
			JSONObject obj = (JSONObject) array.get(i);
			if (obj.containsValue(value))
			{
				return true;
			}
		}
		return false;
	}
	
	public static void deleteFolder(File file) throws IOException
	{
		if(!file.exists())
			return;
		
		for (File childFile : file.listFiles()) {

			if (childFile.isDirectory()) {
				deleteFolder(childFile);
			} else {
				if (!childFile.delete()) {
					throw new IOException();
				}
			}
		}

		if (!file.delete()) {
			throw new IOException();
		}
	}
	
}
