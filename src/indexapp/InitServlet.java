package indexapp;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;

@SuppressWarnings("serial")
public class InitServlet extends HttpServlet{
	public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		
		Key key;
		Query q;
		List<Entity> entityList;
		Entity entity;
		String host;
		Map<String,ListObj> listObjMap;
		ListObj listObj;
		
		ds = DatastoreServiceFactory.getDatastoreService();
		ms = MemcacheServiceFactory.getMemcacheService();
		
		ms.clearAll();
		
		listObjMap = new HashMap<String,ListObj>();
		
		key = KeyFactory.createKey("ListObjGroup",1L);
		q = new Query("ListObj",key);
		entityList = ds.prepare(q).asList(FetchOptions.Builder.withLimit(4096));
		for(index = 0;index < entityList.size();index++){
			entity = entityList.get(index);
			
			listObj = new ListObj();
			listObj.link = (String)entity.getProperty("link");
			listObj.storesize = (Long)entity.getProperty("storesize");
			
			listObjMap.put(listObj.link,listObj);
		}
		ListObj.putDBMap(ds,listObjMap);
		
		host = req.getParameter("host");
		if(host != null){
			
		}
	}
}
