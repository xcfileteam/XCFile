package fileapp;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.blobstore.*;
import commonapp.Common;
import commonapp.Sec;
import commonapp.Pair;

@SuppressWarnings("serial")
public class TaskServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(TaskServlet.class.getName());
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
		
		MemcacheService ms;
		URLFetchService us;
		
		String type;
		
		String serverName;
		Long oldValue;
		Long newValue;
		IdentifiableValue idenValue;
		HTTPRequest httpReq;
		String param;
		
		int subIndex;
		List<String> delBlobKeyList;
		String serverlink;
		List<String> delBlobListKey;
		Map<String,Object> delBlobListMap;
		Object[] delBlobListArray;
		StringBuffer delBlobKeyString;
		Long delBlobSize;
		
		resp.addHeader("Access-Control-Allow-Origin","*");
		resp.setContentType("text/plain");
		
		try{
			ms = MemcacheServiceFactory.getMemcacheService();
			us = URLFetchServiceFactory.getURLFetchService();
			
			serverName = req.getServerName();
			
			type = req.getParameter("type");
			if(type.equals("stateblobinc") == true){
				ms.delete("task_State_BlobInc");
				
				newValue = (Long)ms.get("state_BlobInc");
				oldValue = (Long)ms.get("state_BlobInc_Old");
				
				if(newValue == null){
					ms.delete("state_BlobInc_Old");
					return;
				}else if(oldValue == null){
					oldValue = 0L;
				}
				ms.put("state_BlobInc_Old",newValue);
				
				idenValue = ms.getIdentifiable("state_BlobInc");
				if(idenValue != null){
					ms.putIfUntouched("state_BlobInc",idenValue,idenValue.getValue());
				}
				
				httpReq = new HTTPRequest(new URL(Common.ListServer + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(10));
				param = "type=" + URLEncoder.encode("stateblobinc","UTF-8") + "&" +
						"serverkey=" + URLEncoder.encode(Sec.ServerKey,"UTF-8") + "&" +
						"name=" + URLEncoder.encode(serverName.split(".appspot.com")[0],"UTF-8") + "&" +
						"value=" + URLEncoder.encode(String.valueOf(newValue - oldValue),"UTF-8");
				httpReq.setPayload(param.getBytes("UTF-8"));
				us.fetch(httpReq);
			}else if(type.equals("stateblobdec") == true){
				ms.delete("task_State_BlobDec");
				
				newValue = (Long)ms.get("state_BlobDec");
				oldValue = (Long)ms.get("state_BlobDec_Old");
				
				if(newValue == null){
					ms.delete("state_BlobDec_Old");
					return;
				}else if(oldValue == null){
					oldValue = 0L;
				}
				ms.put("state_BlobDec_Old",newValue);
				
				idenValue = ms.getIdentifiable("state_BlobDec");
				if(idenValue != null){
					ms.putIfUntouched("state_BlobDec",idenValue,idenValue.getValue());
				}
				
				httpReq = new HTTPRequest(new URL(Common.ListServer + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(10));
				param = "type=" + URLEncoder.encode("stateblobinc","UTF-8") + "&" +
						"serverkey=" + URLEncoder.encode(Sec.ServerKey,"UTF-8") + "&" +
						"name=" + URLEncoder.encode(serverName.split(".appspot.com")[0],"UTF-8") + "&" +
						"value=" + URLEncoder.encode(String.valueOf(-(newValue - oldValue)),"UTF-8");
				httpReq.setPayload(param.getBytes("UTF-8"));
				us.fetch(httpReq);
			}else if(type.equals("userdeltask") == true){
				serverlink = req.getParameter("serverlink");
				
				ms.delete("task_User_DelBlob_" + serverlink);
				newValue = (Long)ms.get("user_DelBlobIndex_" + serverlink);
				oldValue = (Long)ms.get("user_DelBlobIndex_Old_" + serverlink);
				
				Thread.sleep(50);	//Wait put user_DelBlobList
				
				if(newValue == null){
					ms.delete("user_DelBlobIndex_Old_" + serverlink);
					return;
				}else if(oldValue == null){
					oldValue = 0L;
				}
				ms.put("user_DelBlobIndex_Old_" + serverlink,newValue);
							
				msKey = "user_DelBlobList_" + serverlink;
				delBlobListKey = new ArrayList<String>();
				for(;oldValue < newValue;oldValue++){
					delBlobListKey.add(msKey + String.valueOf(oldValue));
				}
				
				delBlobListMap = ms.getAll(delBlobListKey);
				delBlobListArray = delBlobListMap.values().toArray();
				delBlobKeyString = new StringBuffer();
				delBlobSize = 0L;
				for(index = 0;index < delBlobListArray.length;index++){
					delBlobKeyList = ((Pair<List<String>,Long>)delBlobListArray[index]).first;
					delBlobSize += ((Pair<List<String>,Long>)delBlobListArray[index]).second;
					for(subIndex = 0;subIndex < delBlobKeyList.size();subIndex++){
						delBlobKeyString.append(delBlobKeyList.get(subIndex));
						delBlobKeyString.append("|");
					}
				}
						
				if(delBlobKeyString.length() == 0){
					return;
				}
						
				idenValue = ms.getIdentifiable("user_DelBlobIndex_" + serverlink);
				if(idenValue != null){
					ms.putIfUntouched("user_DelBlobIndex_" + serverlink,idenValue,idenValue.getValue());
				}
				
				httpReq = new HTTPRequest(new URL(serverlink + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60));
				param = "type=" + URLEncoder.encode("userdelblob","UTF-8") + "&" +
						"serverkey=" + URLEncoder.encode(Sec.ServerKey,"UTF-8") + "&" +
						"delblobkeylist=" + URLEncoder.encode(delBlobKeyString.substring(0,delBlobKeyString.length() - 1),"UTF-8") + "&" +
						"delblobsize=" + URLEncoder.encode(String.valueOf(delBlobSize),"UTF-8");
				httpReq.setPayload(param.getBytes("UTF-8"));
				us.fetch(httpReq);
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}