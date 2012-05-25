package indexapp;

import java.io.*;
import java.util.*;
import com.google.appengine.api.datastore.*;

public class ListObj implements Comparable<ListObj>,Serializable{
	private static final long serialVersionUID = 1L;
	
	public String link;
	public Long storesize;
	
	public static Map<String,ListObj> getDBMap(DatastoreService ds) throws Exception{
		int index;
		
		Key key;	
		Entity entity;
		
		String metadata;
		String[] partData;
		Map<String,ListObj> listObjMap;
		ListObj listObj;
		
		key = KeyFactory.createKey(KeyFactory.createKey("ListServerObjGroup",1L),"ListServerObj",1L);
		entity = ds.get(key);
		metadata = ((Text)entity.getProperty("metadata")).getValue();
		
		partData = metadata.split("\\|");
		listObjMap = new HashMap<String,ListObj>();
		for(index = 0;index < partData.length;index += 2){
			listObj = new ListObj();
			listObj.link = partData[index];
			listObj.storesize = Long.valueOf(partData[index + 1]);
			listObjMap.put(listObj.link,listObj);
		}
		
		return listObjMap;
	}
	
	public static void putDBMap(DatastoreService ds,Map<String,ListObj> listobjmap){	
		int index;
		
		Key key;
		Entity entity;
		
		ListObj[] listObjArray;
		StringBuilder metadata;
		
		listObjArray = listobjmap.values().toArray(new ListObj[listobjmap.size()]);
		metadata = new StringBuilder();
		
		metadata.append(listObjArray[0].link);
		metadata.append("|");
		metadata.append(String.valueOf(listObjArray[0].storesize));
		for(index = 1;index < listObjArray.length;index++){
			metadata.append("|");
			metadata.append(listObjArray[index].link);
			metadata.append("|");
			metadata.append(String.valueOf(listObjArray[index].storesize));
		}
		
		key = KeyFactory.createKey(KeyFactory.createKey("ListServerObjGroup",1L),"ListServerObj",1L);
		entity = new Entity(key);
		entity.setProperty("metadata",new Text(metadata.toString()));
		ds.put(entity);
	}
	
	@Override
	public int compareTo(ListObj obj){
		if(this.storesize == obj.storesize){
			return 0;
		}else if(this.storesize < obj.storesize){
			return -1;
		}else{
			return 1;
		}
	}
}
