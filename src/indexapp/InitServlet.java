package indexapp;

import java.io.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import commonapp.*;

@SuppressWarnings("serial")
public class InitServlet extends HttpServlet{
	public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		DatastoreService ds;
		MemcacheService ms;
		
		Key key;
		String host;
		ListObj listObj;
		
		ds = DatastoreServiceFactory.getDatastoreService();
		ms = MemcacheServiceFactory.getMemcacheService();
		
		ms.clearAll();
		
		//resp.getWriter().print(ms.increment("Noa",1L,0L));
		
		host = req.getParameter("host");
		if(host != null){
			key = KeyFactory.createKey(KeyFactory.createKey("ListObjGroup",1L),"ListObj",Common.ServerNameToId(host.split(".appspot.com")[0]));
			
			listObj = new ListObj();
			listObj.link = "http://" + host;
			listObj.storesize = 0L;
			listObj.putDB(ds,key);
			ms.delete("cache_ListObj");
		}
	}
}
