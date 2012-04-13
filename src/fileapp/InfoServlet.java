package fileapp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.blobstore.*;
import commonapp.Sec;
import commonapp.Pair;

@SuppressWarnings("serial")
public class InfoServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(InfoServlet.class.getName());
	public static final long PART_SIZE = 8388608L;
	
	public void doOptions(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		resp.addHeader("Access-Control-Allow-Origin","*");
		resp.addHeader("Access-Control-Allow-Headers","Origin,X-Prototype-Version,X-Requested-With,Content-type,Accept");
		resp.addHeader("Access-Control-Allow-Methods","POST");
		resp.addHeader("Access-Control-Max-Age","3628800");
	}
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;

		DatastoreService ds;
		MemcacheService ms;
		BlobstoreService bs;
		Queue taskqueue;
		
		String type;
		
		String serverName;
		
		Key key;
		Query q;
		List<Entity> entityList;
		Entity entity;
		
		String usersecid;
		String userseckey;
		String userid;
		
		FileObj fileObj;
		List<FileObj> userFileObjList;
		List<String> userFileIdList;
		Map<String,Object> userFileIdMap;
		
		String fileid;
		String[] serverList;
		String[] blobkeyList;
		Long partSize;
		Long offset;
		int blobkeyIndex;
		int subCount;
		int subIndex;
		List<String> delBlobKeyList;
		Long delBlobIndex;
		
		resp.addHeader("Access-Control-Allow-Origin","*");
		resp.setContentType("text/plain");
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
			bs = BlobstoreServiceFactory.getBlobstoreService();
			
			serverName = req.getServerName();
			
			type = req.getParameter("type");
			if(type.equals("usergetfilelist") == true){
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				userFileObjList = (List<FileObj>)ms.get("cache_UserFileObjList_" + userid);
				if(userFileObjList == null){
					key = KeyFactory.createKey("FileObjGroup",1L);
					q = new Query("FileObj",key);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					q.addSort("userid");
					q.addSort("timestamp",SortDirection.DESCENDING);
					entityList = ds.prepare(q).asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(4096));

					userFileObjList = new ArrayList<FileObj>();
					for(index = 0;index < entityList.size();index++){
						userFileObjList.add(new FileObj());
					}
					for(index = 0;index < entityList.size();index++){
						userFileObjList.get(entityList.size() - index - 1).getDB(entityList.get(index));
					}
					ms.put("cache_UserFileObjList_" + userid,userFileObjList);
				}
				
				userFileIdList = new ArrayList<String>();
				for(index = 0;index < userFileObjList.size();index++){
					userFileIdList.add("user_DelFileID_" + userFileObjList.get(index).fileid);
				}

				userFileIdMap = ms.getAll(userFileIdList);				
				for(index = 0;index < userFileObjList.size();index++){
					fileObj = userFileObjList.get(index);
					if(userFileIdMap.containsKey("user_DelFileID_" + fileObj.fileid) == true){
						continue;
					}
					
					resp.getWriter().println(fileObj.fileid);
					resp.getWriter().println(fileObj.filename);
					resp.getWriter().println(fileObj.filesize);
					resp.getWriter().println(fileObj.timestamp);
					resp.getWriter().println("http://" + serverName + "/down/" + fileObj.fileid + "/" + fileObj.filename);
				}
			}else if(type.equals("userdelfile") == true){
				taskqueue = QueueFactory.getQueue("user");
				
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				fileid = req.getParameter("fileid");
				
				key = KeyFactory.createKey("FileObjGroup",1L);
				q = new Query("FileObj",key);
				q.addFilter("fileid",FilterOperator.EQUAL,fileid);
				q.addFilter("userid",FilterOperator.EQUAL,userid);
				entity = ds.prepare(q).asSingleEntity();
				fileObj = new FileObj();
				fileObj.getDB(entity);
				
				ds.delete(entity.getKey());
				ms.delete("cache_FileObj_" + fileid);
				ms.put("user_DelFileID_" + fileid,true);
				
				serverList = fileObj.serverlist.getValue().split("\\|");
				blobkeyList = fileObj.blobkeylist.getValue().split("\\|");
				partSize = fileObj.filesize / serverList.length;
				blobkeyIndex = 0;
				
				index = 0;
				offset = 0L;
				while(offset < fileObj.filesize){
					if((fileObj.filesize - offset) < partSize){
						partSize = fileObj.filesize - offset;
					}
					
					subCount = (int)(partSize / PART_SIZE);
					if((partSize % PART_SIZE) > 0L){
						subCount++;
					}
					
					delBlobKeyList = new ArrayList<String>();
					for(subIndex = 0;subIndex < subCount;subIndex++){
						delBlobKeyList.add(blobkeyList[blobkeyIndex]);
						blobkeyIndex++;
					}
					
					delBlobIndex = ms.increment("user_DelBlobIndex_" + serverList[index],1L,0L);
					ms.put("user_DelBlobList_" + serverList[index] + String.valueOf(delBlobIndex - 1),new Pair<List<String>,Long>(delBlobKeyList,partSize));
					
					if(ms.put("task_User_DelBlob_" + serverList[index],true,Expiration.byDeltaSeconds(310),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
						taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","userdeltask").param("serverlink",serverList[index]).countdownMillis(300000));
					}
					
					index = (index + 1) % serverList.length;
					offset += partSize;
				}
			}else if(type.equals("userdelblob") == true){
				taskqueue = QueueFactory.getQueue("state");
				
				if(req.getParameter("serverkey").equals(Sec.ServerKey) == false){
					throw new Exception("ServerKey Wrong");
				}
				
				delBlobKeyList = Arrays.asList(req.getParameter("delblobkeylist").split("\\|"));
				for(index = 0;index < delBlobKeyList.size();index++){
					try{
						bs.delete(new BlobKey(delBlobKeyList.get(index)));
					}catch(Exception e){
						log.log(Level.WARNING,"Error",e);
					}
				}
				
				ms.increment("state_BlobDec",Long.valueOf(req.getParameter("delblobsize")),0L);
				if(ms.put("task_State_BlobDec",true,Expiration.byDeltaSeconds(610),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
					taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","stateblobdec").countdownMillis(600000));
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}
