package indexapp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;

import commonapp.*;

@SuppressWarnings("serial")
public class TaskServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(TaskServlet.class.getName());
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{	
		DatastoreService ds;
		MemcacheService ms;
		
		String type;
		Long oldValue;
		Long newValue;
		IdentifiableValue idenValue;
		List<String> stateBlobIncKey;
		Map<String,Object> stateBlobIncMap;
		Map<String,ListObj> listObjMap;
		Iterator<Object> objIter;
		Pair<String,Long> stateBlobIncPair;
		
		resp.setContentType("text/plain");
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
			
			type = req.getParameter("type");
			if(type.equals("stateblobinc") == true){
				ms.delete("task_State_BlobInc");
				
				newValue = (Long)ms.get("state_BlobIncIndex");
				if(newValue == null){
					return;
				}
				oldValue = (newValue >>> 32L);
				newValue &= 0xFFFFFFFFL;
				ms.increment("state_BlobIncIndex",((newValue - oldValue) << 32L));
				
				idenValue = ms.getIdentifiable("state_BlobIncIndex");
				if(idenValue != null){
					ms.putIfUntouched("state_BlobIncIndex",idenValue,idenValue.getValue());
				}
				
				Thread.sleep(50);	//Wait put state_BlobIncList
				
				stateBlobIncKey = new ArrayList<String>();
				for(;oldValue < newValue;oldValue++){
					stateBlobIncKey.add("state_BlobIncList_" + String.valueOf(oldValue));
				}
				
				stateBlobIncMap = ms.getAll(stateBlobIncKey);
				listObjMap = ListObj.getDBMap(ds);
				objIter = stateBlobIncMap.values().iterator();
				while(objIter.hasNext() == true){
					stateBlobIncPair = (Pair)objIter.next();
					listObjMap.get(stateBlobIncPair.first).storesize += stateBlobIncPair.second;
				}
				ListObj.putDBMap(ds,listObjMap);
				
				ms.put("cache_ListObjArray",listObjMap.values().toArray(new ListObj[listObjMap.size()]));
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}
