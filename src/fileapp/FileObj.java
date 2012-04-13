package fileapp;

import indexapp.ListObj;

import java.io.*;
import com.google.appengine.api.datastore.*;

public class FileObj implements Comparable<FileObj>,Serializable{
	private static final long serialVersionUID = 1L;
	
	public String fileid;
	public String filename;
	public Long filesize;
	public Long timestamp;
	public Text serverlist;
	public Text blobkeylist;
	public String userid;
	
	public void getDB(Entity entity){
		this.fileid = (String)entity.getProperty("fileid");
		this.filename = (String)entity.getProperty("filename");
		this.filesize = (Long)entity.getProperty("filesize");
		this.timestamp = (Long)entity.getProperty("timestamp");
		this.serverlist = (Text)entity.getProperty("serverlist");
		this.blobkeylist = (Text)entity.getProperty("blobkeylist");
		this.userid = (String)entity.getProperty("userid");
	}
	
	public void putDB(DatastoreService ds){
		Key key;
		Entity entity;
		
		key = KeyFactory.createKey("FileObjGroup",1L);
		entity = new Entity("FileObj",key);
		
		entity.setProperty("fileid",this.fileid);
		entity.setProperty("filename",this.filename);
		entity.setProperty("filesize",this.filesize);
		entity.setProperty("timestamp",this.timestamp);
		entity.setProperty("serverlist",this.serverlist);
		entity.setProperty("blobkeylist",this.blobkeylist);
		entity.setProperty("userid",this.userid);
		
		ds.put(entity);
	}
	
	@Override
	public int compareTo(FileObj obj){
		if(this.timestamp == obj.timestamp){
			return 0;
		}else if(this.timestamp > obj.timestamp){
			return -1;
		}else{
			return 1;
		}
	}
}
