package indexapp;

import java.io.*;
import com.google.appengine.api.datastore.*;

public class ListObj implements Comparable<ListObj>,Serializable{
	private static final long serialVersionUID = 1L;
	
	public String link;
	public Long storesize;
	
	public void getDB(Entity entity){
		this.link = (String)entity.getProperty("link");
		this.storesize = (Long)entity.getProperty("storesize");
	}
	
	public void putDB(DatastoreService ds,Key key){
		Entity entity;
		
		entity = new Entity(key);
		entity.setProperty("link",this.link);
		entity.setProperty("storesize",this.storesize);
		
		ds.put(entity);
	}
	public void putDB(DatastoreService ds,Transaction txn,Entity entity){
		entity.setProperty("link",this.link);
		entity.setProperty("storesize",this.storesize);
		
		ds.put(txn,entity);
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
