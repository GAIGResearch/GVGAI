package agents.mrtndwrd;

import java.util.Map;
import java.util.HashMap;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Simple DefaultHashMap from
 * https://stackoverflow.com/questions/7519339/hashmap-to-return-default-value-for-non-found-keys
 */
public class DefaultHashMap<K,V> extends HashMap<K,V> 
{
	protected V defaultValue;

	public DefaultHashMap(V defaultValue) 
	{
		this.defaultValue = defaultValue;
	}

	@Override
	public V get(Object k) 
	{
		return containsKey(k) ? super.get(k) : defaultValue;
	}

	@Override
	public String toString()
	{
		String s = "";
		ArrayList<Entry> entryArrayList = new ArrayList<Entry>(this.entrySet());
		for (Entry e : entryArrayList)
		{
			s += String.format("%s -> %s\n", e.getKey(), e.getValue());
		}
		return s;
	}


}
