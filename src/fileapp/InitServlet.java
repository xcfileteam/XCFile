package fileapp;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

@SuppressWarnings("serial")
public class InitServlet extends HttpServlet{
	public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		DatastoreService ds;
		MemcacheService ms;

		Key key;
		Entity entity;
		
		ds = DatastoreServiceFactory.getDatastoreService();
		ms = MemcacheServiceFactory.getMemcacheService();
		
		key = KeyFactory.createKey("PhoneObjGroup",1L);
		entity = new Entity("PhoneObj",key);
		entity.setProperty("server","xcfileappab");
		ds.put(entity);
	}
}
