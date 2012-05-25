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
		String msKey;

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
		
		FileObj fileObj;
		List<String> userFileCacheKey;
		Map<String,Object> userFileObjMap;
		FileObj[] userFileObjArray;
		List<FileObj> userFileObjList;
		
		TagObj tagObj;
		List<String> userTagCacheKey;
		Map<String,Object> userTagObjMap;
		TagObj[] userTagObjArray;
		List<TagObj> userTagObjList;
		
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
				
				userFileCacheKey = (List<String>)ms.get("user_FileCacheKey_" + userid);
				userFileObjMap = null;
				if(userFileCacheKey != null){
					userFileObjMap = ms.getAll(userFileCacheKey);
					if(userFileObjMap.size() < userFileCacheKey.size()){
						userFileObjMap = null;
					}
				}
				
				if(userFileObjMap == null){
					key = KeyFactory.createKey("FileObjGroup",1L);
					q = new Query("FileObj",key);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					entityList = ds.prepare(q).asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(4096));

					userFileCacheKey = new ArrayList<String>();
					userFileObjMap = new HashMap<String,Object>();
					for(index = 0;index < entityList.size();index++){
						fileObj = new FileObj();
						fileObj.getDB(entityList.get(index));
						
						msKey = "cache_FileObj_" + fileObj.fileid;
						userFileCacheKey.add(msKey);
						userFileObjMap.put(msKey,fileObj);
					}
					ms.putAll(userFileObjMap);
					ms.put("user_FileCacheKey_" + userid,userFileCacheKey);
				}
				
				userFileObjArray = userFileObjMap.values().toArray(new FileObj[userFileObjMap.size()]);
				userFileObjList = new ArrayList<FileObj>();
				for(index = 0;index < userFileObjArray.length;index++){
					if(userFileObjArray[index] != null){
						userFileObjList.add(userFileObjArray[index]);
					}
				}
				Collections.sort(userFileObjList);
								
				for(index = 0;index < userFileObjList.size();index++){
					fileObj = userFileObjList.get(index);
					
					resp.getWriter().println(fileObj.fileid);
					resp.getWriter().println(fileObj.filename);
					resp.getWriter().println(fileObj.filesize);
					resp.getWriter().println(fileObj.timestamp);
					resp.getWriter().println("http://" + serverName + "/down/" + fileObj.fileid + "/" + fileObj.filename);
				}
				
				resp.getWriter().println("<-->");
				
				userTagCacheKey = (List<String>)ms.get("user_TagCacheKey_" + userid);
				userTagObjMap = null;
				if(userTagCacheKey != null){
					userTagObjMap = ms.getAll(userTagCacheKey);
					if(userTagObjMap.size() < userTagCacheKey.size()){
						userTagObjMap = null;
					}
				}
				
				if(userTagObjMap == null){
					key = KeyFactory.createKey("TagObjGroup",1L);
					q = new Query("TagObj",key);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					entityList = ds.prepare(q).asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(4096));
					
					userTagCacheKey = new ArrayList<String>();
					userTagObjMap = new HashMap<String,Object>();
					for(index = 0;index < entityList.size();index++){
						tagObj = new TagObj();
						tagObj.getDB(entityList.get(index));
						
						msKey = "cache_TagObj_" + userid + "_" + tagObj.tagname;
						userTagCacheKey.add(msKey);
						userTagObjMap.put(msKey,tagObj);
					}
					ms.putAll(userTagObjMap);
					ms.put("user_TagCacheKey_" + userid,userTagCacheKey);
				}
				
				userTagObjArray = userTagObjMap.values().toArray(new TagObj[userTagObjMap.size()]);
				userTagObjList = new ArrayList<TagObj>();
				for(index = 0;index < userTagObjArray.length;index++){
					if(userTagObjArray[index] != null){
						userTagObjList.add(userTagObjArray[index]);
					}
				}
				Collections.sort(userTagObjList);
						
				for(index = 0;index < userTagObjList.size();index++){
					tagObj = userTagObjList.get(index);
					
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
				ms.put("cache_FileObj_" + fileid,null);
				
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
					
					delBlobIndex = (ms.increment("user_DelBlobIndex_" + serverList[index],1L,0L) & 0xFFFFFFFFL) - 1L;
					ms.put("user_DelBlobList_" + serverList[index] + "_" + String.valueOf(delBlobIndex),new Pair<List<String>,Long>(delBlobKeyList,partSize));
					
					if(ms.put("task_User_DelBlob_" + serverList[index],true,Expiration.byDeltaSeconds(70),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
						taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","userdeltask").param("serverlink",serverList[index]).countdownMillis(60000));
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
				if(ms.put("task_State_BlobDec",true,Expiration.byDeltaSeconds(70),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
					taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","stateblobdec").countdownMillis(60000));
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
						
						msKey = "cache_FileObj_" + fileObj.fileid;
						while(true){
							idenValue = ms.getIdentifiable(msKey);
							if(idenValue != null){
								if(idenValue.getValue() == null){
									break;
								}
								if(ms.putIfUntouched(msKey,idenValue,fileObj) == true){
									break;
								}
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
				
				key = KeyFactory.createKey("TagObjGroup",1L);
				q = new Query("TagObj",key);
				q.addFilter("tagname",FilterOperator.EQUAL,tagname);
				q.addFilter("userid",FilterOperator.EQUAL,userid);
				if(ds.prepare(q).asSingleEntity() != null){
					throw new Exception("TagName Duplicate");
				}
				
				tagObj = new TagObj();
				tagObj.userid = userid;
				tagObj.tagname = tagname;
				tagObj.timestamp = new Date().getTime();
				tagObj.fileidlist = "";
				tagObj.putDB(ds);
				
				msKey = "cache_TagObj_" + userid + "_" + tagObj.tagname;
				ms.put(msKey,tagObj);
				while(true){
					idenValue = ms.getIdentifiable("user_TagCacheKey_" + userid);
					if(idenValue == null){
						break;
					}
					userTagCacheKey = (List<String>)idenValue.getValue();
					userTagCacheKey.add(msKey);
					if(ms.putIfUntouched("user_TagCacheKey_" + userid,idenValue,userTagCacheKey) == true){
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
				ms.put("cache_TagObj_" + userid + "_" + tagname,null);
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
					
					key = KeyFactory.createKey("TagObjGroup",1L);
					q = new Query("TagObj",key);
					q.addFilter("tagname",FilterOperator.EQUAL,newtagname);
					q.addFilter("userid",FilterOperator.EQUAL,userid);
					if(ds.prepare(q).asSingleEntity() != null){
						throw new Exception("TagName Duplicate");
					}
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
						
						if(newtagname == null){
							msKey = "cache_TagObj_" + userid + "_" + tagObj.tagname;
							while(true){
								idenValue = ms.getIdentifiable(msKey);
								if(idenValue != null){
									if(idenValue.getValue() == null){
										break;
									}
									if(ms.putIfUntouched(msKey,idenValue,tagObj) == true){
										break;
									}
								}
							}
						}else{
							ms.put("cache_TagObj_" + userid + "_" + tagname,null);
							
							msKey = "cache_TagObj_" + userid + "_" + newtagname;
							ms.put(msKey,tagObj);
							while(true){
								idenValue = ms.getIdentifiable("user_TagCacheKey_" + userid);
								if(idenValue == null){
									break;
								}
								userTagCacheKey = (List<String>)idenValue.getValue();
								userTagCacheKey.add(msKey);
								if(ms.putIfUntouched("user_TagCacheKey_" + userid,idenValue,userTagCacheKey) == true){
									break;
								}
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
