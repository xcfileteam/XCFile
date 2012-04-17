package indexapp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import commonapp.*;

@SuppressWarnings("serial")
public class InfoServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(InfoServlet.class.getName());
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{	
		DatastoreService ds;
		MemcacheService ms;
		
		Key key;
		Transaction txn;
		int retry;
		Entity entity;
		ListObj listObj;
		
		String type;
		String name;
		Long value;
		resp.setContentType("text/plain");
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
			
			type = req.getParameter("type");
			if(type.equals("stateblobinc") == true){
				if(req.getParameter("serverkey").equals(Sec.ServerKey) == false){
					throw new Exception("ServerKey Wrong");
				}
				
				name = req.getParameter("name");
				value = Long.valueOf(req.getParameter("value"));
				
				key = KeyFactory.createKey(KeyFactory.createKey("ListObjGroup",1L),"ListObj",Common.ServerNameToId(name));
				listObj = new ListObj();
				retry = 8;
				while(retry > 0){
					txn = ds.beginTransaction();
					try{
						entity = ds.get(txn,key);
						listObj.getDB(entity);
						listObj.storesize += value;
						listObj.putDB(ds,txn,entity);
						
						txn.commit();
						
						ms.put("cache_ListObj_" + listObj.link,listObj);
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
