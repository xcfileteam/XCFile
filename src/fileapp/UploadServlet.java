package fileapp;

import java.io.*;
import java.nio.*;
import java.util.List;
import java.util.Date;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.files.*;
import com.google.appengine.api.taskqueue.*;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import commonapp.*;

@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(UploadServlet.class.getName());
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		DatastoreService ds;
		MemcacheService ms;
		FileService fs;
		Queue taskqueue;
		
		BufferedInputStream inb;
		
		String fileid;
		String fileseckey;
		String type;
		
		String filename;
		Long filesize;
		String serverlist;
		String blobkeylist;
		String usersecid;
		String userseckey;
		String userid;
		FileObj fileObj;
		IdentifiableValue idenValue;
		List<FileObj> userFileObjList;
		
		Long partsize;
		AppEngineFile file;
		FileWriteChannel fWriter;
		byte[] fbuf;
		Long len;
		int rl;
		
		resp.setContentType("text/plain");
		
		try{
			ms = MemcacheServiceFactory.getMemcacheService();
			
			inb = new BufferedInputStream(req.getInputStream());
			
			fileid = readLine(inb);
			fileseckey = readLine(inb);
			type = readLine(inb);
			
			if(Sec.createSecKey(fileid).equals(fileseckey) == false){
				throw new Exception("SecKey Wrong");
			}
			
			if(type.equals("create") == true){
				ds = DatastoreServiceFactory.getDatastoreService();
				
				filename = Common.EncodeURI(readLine(inb));
				filesize = Long.valueOf(readLine(inb));
				serverlist = readLine(inb);
				blobkeylist = readLine(inb);
				
				usersecid = readLine(inb);
				if(usersecid.equals("") == true){
					userid = null;
				}else{
					userseckey = readLine(inb);
					userid = Sec.getLogin(usersecid,"http://" + req.getServerName(),userseckey);
					if(userid == null){
						throw new Exception("User Wrong");
					}
				}
				
				fileObj = new FileObj();
				fileObj.fileid = fileid;
				fileObj.filename = filename;
				fileObj.filesize = filesize;
				fileObj.timestamp = new Date().getTime();
				fileObj.serverlist = serverlist;
				fileObj.blobkeylist = blobkeylist;
				fileObj.userid = userid;
				fileObj.putDB(ds);
				ms.put("cache_FileObj_" + fileObj.fileid,fileObj);
				
				if(userid != null){
					while(true){
						idenValue = ms.getIdentifiable("cache_UserFileObjList_" + userid);
						if(idenValue == null){
							break;
						}
						userFileObjList = (List<FileObj>)idenValue.getValue();
						userFileObjList.add(fileObj);
						if(ms.putIfUntouched("cache_UserFileObjList_" + userid,idenValue,userFileObjList) == true){
							break;
						}
					}
				}
			}else if(type.equals("upload") == true){
				fs = FileServiceFactory.getFileService();
				taskqueue = QueueFactory.getQueue("state");
				
				partsize = Long.valueOf(readLine(inb));

				file = fs.createNewBlobFile("application/octet-stream",fileid);
				fWriter = fs.openWriteChannel(file,true);
				fbuf = new byte[524288];
				len = partsize;
				while(len > 0){
					rl = inb.read(fbuf);
					if(rl == -1){
						break;
					}
					fWriter.write(ByteBuffer.wrap(fbuf,0,rl));
					len -= rl;
				}
				fWriter.closeFinally();

				ms.increment("state_BlobInc",partsize - len,0L);
				if(ms.put("task_State_BlobInc",true,Expiration.byDeltaSeconds(610),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
					taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","stateblobinc").countdownMillis(600000));
				}
				
				resp.getWriter().print(fs.getBlobKey(file).getKeyString());
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
	
	public String readLine(InputStream in) throws IOException{
		int index;
		byte[] ret;
		
		ret = new byte[4096];
		index = 0;
		while(true){
			ret[index] = (byte)in.read();
			if(ret[index] == -1){
				return null;
			}
			if(ret[index] == '\n'){
				break;
			}else if(ret[index] != '\r'){
				index++;
			}
		}
		
		return new String(ret,0,index,"UTF-8");
	}
}
