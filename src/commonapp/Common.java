package commonapp;

import com.google.appengine.api.memcache.*;

public class Common{
	public static final String ListServer = "http://xcfilelab.appspot.com";
	
	public static Long ServerNameToId(String name){
		int index;
		Long id;
		char[] array;
		
		array = name.split("xcfilelab")[1].toCharArray();
		id = 0L;
		for(index = 0;index < array.length;index++){
			id *= 256L;
			id += (long)array[index];
		}
		
		return id; 
	}
}
