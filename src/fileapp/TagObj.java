package fileapp;

import java.io.*;
import com.google.appengine.api.datastore.*;

public class TagObj implements Comparable<TagObj>,Serializable{
	private static final long serialVersionUID = 1L;

	public String tagname;
	public String userid;
	public Text metadata;
	public Long timestamp;
	public String fileidlist;
	
	public void getDB(Entity entity){
		String[] partMetadata;
		
		this.tagname = (String)entity.getProperty("tagname");
		this.userid = (String)entity.getProperty("userid");
		this.metadata = (Text)entity.getProperty("metadata");
		
		partMetadata = metadata.getValue().split("<>");
		
		this.timestamp = Long.valueOf(partMetadata[0]);
		if(partMetadata.length > 1){
			this.fileidlist = partMetadata[1];
		}
	}
	
	public void putDB(DatastoreService ds){
		Key key;
		Entity entity;
		
		StringBuilder metadataString;
		
		key = KeyFactory.createKey("TagObjGroup",1L);
		entity = new Entity("TagObj",key);
		
		entity.setProperty("tagname",this.tagname);
		entity.setProperty("userid",this.userid);
		
		metadataString = new StringBuilder();
		metadataString.append(String.valueOf(this.timestamp));
		metadataString.append("<>");
		metadataString.append(this.fileidlist);
		
		entity.setProperty("metadata",new Text(metadataString.toString()));
		
		ds.put(entity);
	}
	public void putDB(DatastoreService ds,Transaction txn,Entity entity){
		StringBuilder metadataString;
		
		entity.setProperty("tagname",this.tagname);
		entity.setProperty("userid",this.userid);
		
		metadataString = new StringBuilder();
		metadataString.append(String.valueOf(this.timestamp));
		metadataString.append("<>");
		metadataString.append(this.fileidlist);
		
		entity.setProperty("metadata",new Text(metadataString.toString()));
		
		ds.put(txn,entity);
	}
	
	@Override
	public int compareTo(TagObj obj){
		if(this.timestamp == obj.timestamp){
			return 0;
		}else if(this.timestamp < obj.timestamp){
			return -1;
		}else{
			return 1;
		}
	}
}
