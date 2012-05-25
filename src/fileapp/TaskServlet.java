package fileapp;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import com.google.appengine.api.urlfetch.*;
import commonapp.Common;
import commonapp.Sec;
import commonapp.Pair;

@SuppressWarnings("serial")
public class TaskServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(TaskServlet.class.getName());

	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		int subIndex;
		String msKey;
		
		MemcacheService ms;
		URLFetchService us;
		
		String type;
		
		String serverName;
		Long oldValue;
		Long newValue;
		IdentifiableValue idenValue;
		HTTPRequest httpReq;
		StringBuilder param;
		
		List<String> delBlobKeyList;
		String serverlink;
		List<String> delBlobListKey;
		Map<String,Object> delBlobListMap;
		Object[] delBlobListArray;
		Long delBlobSize;
		int delBlobKeyCount;
		StringBuilder delBlobKeyString;
		List<Future<HTTPResponse>> respList;
		
		resp.setContentType("text/plain");
		
		try{
			ms = MemcacheServiceFactory.getMemcacheService();
			us = URLFetchServiceFactory.getURLFetchService();
			
			serverName = req.getServerName();
			
			type = req.getParameter("type");
			if(type.equals("stateblobinc") == true){
				ms.delete("task_State_BlobInc");
				
				newValue = (Long)ms.get("state_BlobInc");
				if(newValue == null){
					return;
				}
				ms.increment("state_BlobInc",-newValue);
				
				idenValue = ms.getIdentifiable("state_BlobInc");
				if(idenValue != null){
					ms.putIfUntouched("state_BlobInc",idenValue,idenValue.getValue());
				}
				
				httpReq = new HTTPRequest(new URL(Common.ListServer + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(10));
				param = new StringBuilder();
				param.append("type=");
				param.append(URLEncoder.encode("stateblobinc","UTF-8"));
				param.append("&");
				param.append("serverkey=");
				param.append(URLEncoder.encode(Sec.ServerKey,"UTF-8"));
				param.append("&");
				param.append("name=");
				param.append(URLEncoder.encode(serverName,"UTF-8"));
				param.append("&");
				param.append("value=");
				param.append(URLEncoder.encode(String.valueOf(newValue),"UTF-8"));
				httpReq.setPayload(param.toString().getBytes("UTF-8"));
				us.fetch(httpReq);
			}else if(type.equals("stateblobdec") == true){
				ms.delete("task_State_BlobDec");
				
				newValue = (Long)ms.get("state_BlobDec");
				if(newValue == null){
					return;
				}
				ms.increment("state_BlobDec",-newValue);
				
				idenValue = ms.getIdentifiable("state_BlobDec");
				if(idenValue != null){
					ms.putIfUntouched("state_BlobDec",idenValue,idenValue.getValue());
				}
				
				httpReq = new HTTPRequest(new URL(Common.ListServer + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(10));
				param = new StringBuilder();
				param.append("type=");
				param.append(URLEncoder.encode("stateblobinc","UTF-8"));
				param.append("&");
				param.append("serverkey=");
				param.append(URLEncoder.encode(Sec.ServerKey,"UTF-8"));
				param.append("&");
				param.append("name=");
				param.append(URLEncoder.encode(serverName,"UTF-8"));
				param.append("&");
				param.append("value=");
				param.append(URLEncoder.encode(String.valueOf(-newValue),"UTF-8"));
				httpReq.setPayload(param.toString().getBytes("UTF-8"));
				us.fetch(httpReq);
			}else if(type.equals("userdeltask") == true){
				serverlink = req.getParameter("serverlink");
				ms.delete("task_User_DelBlob_" + serverlink);
				
				msKey = "user_DelBlobIndex_" + serverlink;
				newValue = (Long)ms.get(msKey);
				if(newValue == null){
					return;
				}
				oldValue = (newValue >>> 32L);
				newValue &= 0xFFFFFFFFL;
				ms.increment(msKey,((newValue - oldValue) << 32L));
				
				idenValue = ms.getIdentifiable(msKey);
				if(idenValue != null){
					ms.putIfUntouched(msKey,idenValue,idenValue.getValue());
				}
				
				Thread.sleep(50);	//Wait put user_DelBlobList
							
				msKey = "user_DelBlobList_" + serverlink + "_";
				delBlobListKey = new ArrayList<String>();
				for(;oldValue < newValue;oldValue++){
					delBlobListKey.add(msKey + String.valueOf(oldValue));
				}
				
				delBlobListMap = ms.getAll(delBlobListKey);
				delBlobListArray = delBlobListMap.values().toArray();
				delBlobKeyString = new StringBuilder();
				delBlobSize = 0L;
				delBlobKeyCount = 0;
				respList = new ArrayList<Future<HTTPResponse>>();
				for(index = 0;index < delBlobListArray.length;index++){
					delBlobKeyList = ((Pair<List<String>,Long>)delBlobListArray[index]).first;
					delBlobSize += ((Pair<List<String>,Long>)delBlobListArray[index]).second;
					for(subIndex = 0;subIndex < delBlobKeyList.size();subIndex++){
						delBlobKeyString.append(delBlobKeyList.get(subIndex));
						delBlobKeyString.append("|");
						delBlobKeyCount++;
						
						if(delBlobKeyCount >= 16){
							httpReq = new HTTPRequest(new URL(serverlink + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60));
							param = new StringBuilder();
							param.append("type=");
							param.append(URLEncoder.encode("userdelblob","UTF-8"));
							param.append("&");
							param.append("serverkey=");
							param.append(URLEncoder.encode(Sec.ServerKey,"UTF-8"));
							param.append("&");
							param.append("delblobkeylist=");
							param.append(URLEncoder.encode(delBlobKeyString.substring(0,delBlobKeyString.length() - 1),"UTF-8"));
							param.append("&");
							param.append("delblobsize=");
							param.append(URLEncoder.encode(String.valueOf(delBlobSize),"UTF-8"));
							httpReq.setPayload(param.toString().getBytes("UTF-8"));
							respList.add(us.fetchAsync(httpReq));
							
							delBlobKeyString = new StringBuilder();
							delBlobSize = 0L;
							delBlobKeyCount = 0;
						}
					}
				}
				if(delBlobKeyString.length() > 0){
					httpReq = new HTTPRequest(new URL(serverlink + "/info"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(60));
					param = new StringBuilder();
					param.append("type=");
					param.append(URLEncoder.encode("userdelblob","UTF-8"));
					param.append("&");
					param.append("serverkey=");
					param.append(URLEncoder.encode(Sec.ServerKey,"UTF-8"));
					param.append("&");
					param.append("delblobkeylist=");
					param.append(URLEncoder.encode(delBlobKeyString.substring(0,delBlobKeyString.length() - 1),"UTF-8"));
					param.append("&");
					param.append("delblobsize=");
					param.append(URLEncoder.encode(String.valueOf(delBlobSize),"UTF-8"));
					httpReq.setPayload(param.toString().getBytes("UTF-8"));
					respList.add(us.fetchAsync(httpReq));
				}
				
				for(index = 0;index < respList.size();index++){
					respList.get(index).get();
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}
