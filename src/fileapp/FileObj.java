package fileapp;

import java.io.*;
import com.google.appengine.api.datastore.*;

public class FileObj implements Comparable<FileObj>,Serializable{
	private static final long serialVersionUID = 1L;
	
	public String fileid;
	public Long timestamp;
	public String userid;
	public Text metadata;
	public String filename;
	public Long filesize;
	public String serverlist;
	public String blobkeylist;
	
	public void getDB(Entity entity){
		String[] partMetaData;
		
		this.fileid = (String)entity.getProperty("fileid");
		this.timestamp = (Long)entity.getProperty("timestamp");
		this.userid = (String)entity.getProperty("userid");
		this.metadata = (Text)entity.getProperty("metadata");
		
		partMetaData = this.metadata.getValue().split("<>");
		
		this.filename = partMetaData[0];
		this.filesize = Long.valueOf(partMetaData[1]);
		this.serverlist = partMetaData[2];
		this.blobkeylist = partMetaData[3];
	}

	public void putDB(DatastoreService ds){
		Key key;
		Entity entity;
		StringBuilder metadataString;
		
		key = KeyFactory.createKey("FileObjGroup",1L);
		entity = new Entity("FileObj",key);

		entity.setProperty("fileid",this.fileid);
		entity.setProperty("timestamp",this.timestamp);
		entity.setProperty("userid",this.userid);
		
		metadataString = new StringBuilder();
		metadataString.append(this.filename);
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.filesize));
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.serverlist));
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.blobkeylist));
		
		entity.setProperty("metadata",new Text(metadataString.toString()));
		
		ds.put(entity);
	}
	public void putDB(DatastoreService ds,Transaction txn,Entity entity){
		StringBuilder metadataString;

		entity.setProperty("fileid",this.fileid);
		entity.setProperty("timestamp",this.timestamp);
		entity.setProperty("userid",this.userid);
		
		metadataString = new StringBuilder();
		metadataString.append(this.filename);
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.filesize));
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.serverlist));
		metadataString.append("<>");
		metadataString.append(String.valueOf(this.blobkeylist));
		
		entity.setProperty("metadata",new Text(metadataString.toString()));
		
		ds.put(txn,entity);
	}
	
	@Override
	public int compareTo(FileObj obj){
		if(this.timestamp == obj.timestamp){
			return 0;
		}else if(this.timestamp < obj.timestamp){
			return -1;
		}else{
			return 1;
		}
	}
}
