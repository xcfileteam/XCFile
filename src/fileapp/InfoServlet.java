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

import commonapp.Common;
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
		
		String serverName;
		String type;
		
		Key key;
		Query q;
		Transaction txn;
		int retry;
		List<Entity> entityList;
		Entity entity;
		
		String usersecid;
		String userseckey;
		String userid;
		
		TagObj tagObj;
		Map<String,Object> userTagObjMap;
		List<String> userTagNameList;
		Map<String,Object> userTagNameMap; 
		String delTagObjKeyString;
		TagObj[] userTagObjArray;
		
		FileObj fileObj;
		Map<String,Object> userFileObjMap;
		List<String> userFileIdList;
		Map<String,Object> userFileIdMap;
		FileObj[] userFileObjArray;
		
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
		
		String filename;
		
		String tagname;
		String newtagname;
		String fileidlist;
		IdentifiableValue idenValue;
		
		resp.addHeader("Access-Control-Allow-Origin","*");
		resp.setContentType("text/plain");
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
			bs = BlobstoreServiceFactory.getBlobstoreService();
			
			serverName = req.getServerName();
			
			type = req.getParameter("type");
			if(type.equals("usergetlist") == true){
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				userFileObjMap = (Map<String,Object>)ms.get("cache_UserFileObjMap_" + userid);
				if(userFileObjMap == null){
					key = KeyFactory.createKey("FileObjGroup",1L);
					q = new Query("FileObj",key);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					q.addSort("userid");
					q.addSort("timestamp",SortDirection.DESCENDING);
					entityList = ds.prepare(q).asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(4096));

					userFileObjMap = new HashMap<String,Object>();
					for(index = 0;index < entityList.size();index++){
						fileObj = new FileObj();
						fileObj.getDB(entityList.get(index));
						userFileObjMap.put(fileObj.fileid,fileObj);
					}
					ms.put("cache_UserFileObjMap_" + userid,userFileObjMap);
				}
				
				userFileObjArray = userFileObjMap.values().toArray(new FileObj[userFileObjMap.size()]);
				Arrays.sort(userFileObjArray);
				
				userFileIdList = new ArrayList<String>();
				for(index = 0;index < userFileObjArray.length;index++){
					userFileIdList.add("user_DelFileID_" + userFileObjArray[index].fileid);
				}

				userFileIdMap = ms.getAll(userFileIdList);				
				for(index = 0;index < userFileObjArray.length;index++){
					fileObj = userFileObjArray[index];
					if(userFileIdMap.containsKey("user_DelFileID_" + fileObj.fileid) == true){
						continue;
					}
					
					resp.getWriter().println(fileObj.fileid);
					resp.getWriter().println(fileObj.filename);
					resp.getWriter().println(fileObj.filesize);
					resp.getWriter().println(fileObj.timestamp);
					resp.getWriter().println("http://" + serverName + "/down/" + fileObj.fileid + "/" + fileObj.filename);
				}
				
				resp.getWriter().println("<-->");
				
				userTagObjMap = (Map<String,Object>)ms.get("cache_UserTagObjMap_" + userid);
				if(userTagObjMap == null){
					key = KeyFactory.createKey("TagObjGroup",1L);
					q = new Query("TagObj",key);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					entityList = ds.prepare(q).asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(4096));
					
					userTagObjMap = new HashMap<String,Object>();
					for(index = 0;index < entityList.size();index++){
						tagObj = new TagObj();
						tagObj.getDB(entityList.get(index));
						userTagObjMap.put(tagObj.tagname,tagObj);
					}
					ms.put("cache_UserTagObjMap_" + userid,userTagObjMap);
				}
				
				userTagObjArray = userTagObjMap.values().toArray(new TagObj[userTagObjMap.size()]);
				Arrays.sort(userTagObjArray);
				
				userTagNameList = new ArrayList<String>();
				delTagObjKeyString = "user_DelTagName_" + userid + "_";
				for(index = 0;index < userTagObjArray.length;index++){
					userTagNameList.add(delTagObjKeyString + userTagObjArray[index].tagname);
				}
				
				userTagNameMap = ms.getAll(userTagNameList);				
				for(index = 0;index < userTagObjArray.length;index++){
					tagObj = userTagObjArray[index];
					if(userTagNameMap.containsKey(delTagObjKeyString + tagObj.tagname) == true){
						continue;
					}
					
					resp.getWriter().println(tagObj.tagname);
					resp.getWriter().println(tagObj.fileidlist);
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
				
				serverList = fileObj.serverlist.split("\\|");
				blobkeyList = fileObj.blobkeylist.split("\\|");
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
						bs.delete(new BlobKey(delBlobKeyList.get(index)),new BlobKey(delBlobKeyList.get(index)));
					}catch(Exception e){
						log.log(Level.WARNING,"Error",e);
					}
				}
				
				ms.increment("state_BlobDec",Long.valueOf(req.getParameter("delblobsize")),0L);
				if(ms.put("task_State_BlobDec",true,Expiration.byDeltaSeconds(610),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
					taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","stateblobdec").countdownMillis(600000));
				}
			}else if(type.equals("usersetfile") == true){
				taskqueue = QueueFactory.getQueue("user");
				
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				fileid = req.getParameter("fileid");
				filename = Common.EncodeURI(req.getParameter("filename"));
				
				key = KeyFactory.createKey("FileObjGroup",1L);
				q = new Query("FileObj",key);
				q.addFilter("fileid",FilterOperator.EQUAL,fileid);
				q.addFilter("userid",FilterOperator.EQUAL,userid);
				key = ds.prepare(q).asSingleEntity().getKey();
				
				fileObj = new FileObj();
				retry = 8;
				while(retry > 0){
					txn = ds.beginTransaction();
					try{
						entity = ds.get(txn,key);
						fileObj.getDB(entity);
						fileObj.filename = filename;
						fileObj.putDB(ds,txn,entity);
						
						txn.commit();
						
						while(true){
							idenValue = ms.getIdentifiable("cache_UserFileObjMap_" + userid);
							if(idenValue == null){
								break;
							}
							userFileObjMap = (Map<String,Object>)idenValue.getValue();
							userFileObjMap.put(fileObj.fileid,fileObj);
							if(ms.putIfUntouched("cache_UserFileObjMap_" + userid,idenValue,userFileObjMap) == true){
								break;
							}
						}
						
						break;
					}catch(ConcurrentModificationException e){
						retry--;
					}finally{
						if(txn.isActive()){
							txn.rollback();
						}
					}
				}
			}else if(type.equals("useraddtag") == true){
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				tagname = Common.EncodeURI(req.getParameter("tagname"));
				
				tagObj = new TagObj();
				tagObj.userid = userid;
				tagObj.tagname = tagname;
				tagObj.timestamp = new Date().getTime();
				tagObj.fileidlist = "";
				tagObj.putDB(ds);
				
				while(true){
					idenValue = ms.getIdentifiable("cache_UserTagObjMap_" + userid);
					if(idenValue == null){
						break;
					}
					userTagObjMap = (Map<String,Object>)idenValue.getValue();
					userTagObjMap.put(tagObj.tagname,tagObj);
					if(ms.putIfUntouched("cache_UserTagObjMap_" + userid,idenValue,userTagObjMap) == true){
						break;
					}
				}
			}else if(type.equals("userdeltag") == true){
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				tagname = req.getParameter("tagname");
				
				key = KeyFactory.createKey("TagObjGroup",1L);
				q = new Query("TagObj",key);
				q.addFilter("tagname",FilterOperator.EQUAL,tagname);
				q.addFilter("userid",FilterOperator.EQUAL,userid);
				key = ds.prepare(q).asSingleEntity().getKey();		
				ds.delete(key);
				
				while(true){
					idenValue = ms.getIdentifiable("cache_UserTagObjMap_" + userid);
					if(idenValue == null){
						break;
					}
					userTagObjMap = (Map<String,Object>)idenValue.getValue();
					userTagObjMap.remove(tagname);
					if(ms.putIfUntouched("cache_UserTagObjMap_" + userid,idenValue,userTagObjMap) == true){
						break;
					}
				}
			}else if(type.equals("usersettag") == true){
				usersecid = req.getParameter("usersecid");
				userseckey = req.getParameter("userseckey");
				
				userid = Sec.getLogin(usersecid,"http://" + serverName,userseckey);
				if(userid == null){
					throw new Exception("User Wrong");
				}
				
				tagname = req.getParameter("tagname");
				newtagname = req.getParameter("newtagname");
				if(newtagname != null){
					newtagname = Common.EncodeURI(newtagname);
				}
				fileidlist = req.getParameter("fileidlist");
				
				key = KeyFactory.createKey("TagObjGroup",1L);
				q = new Query("TagObj",key);
				q.addFilter("tagname",FilterOperator.EQUAL,tagname);
				q.addFilter("userid",FilterOperator.EQUAL,userid);
				key = ds.prepare(q).asSingleEntity().getKey();
				
				tagObj = new TagObj();
				retry = 8;
				while(retry > 0){
					txn = ds.beginTransaction();
					try{
						entity = ds.get(txn,key);
						tagObj.getDB(entity);
						if(newtagname != null){
							tagObj.tagname = newtagname;
						}
						tagObj.fileidlist = fileidlist;
						tagObj.putDB(ds,txn,entity);
						
						txn.commit();
						
						while(true){
							idenValue = ms.getIdentifiable("cache_UserTagObjMap_" + userid);
							if(idenValue == null){
								break;
							}
							userTagObjMap = (Map<String,Object>)idenValue.getValue();
							if(newtagname != null){
								userTagObjMap.remove(tagname);
							}
							userTagObjMap.put(tagObj.tagname,tagObj);
							if(ms.putIfUntouched("cache_UserTagObjMap_" + userid,idenValue,userTagObjMap) == true){
								break;
							}
						}
						
						break;
					}catch(ConcurrentModificationException e){
						retry--;
					}finally{
						if(txn.isActive()){
							txn.rollback();
						}
					}
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}
