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
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		
		Key key;
		Transaction txn;
		int retry;
		Entity entity;
		ListObj listObj;
		List<ListObj> listObjList;
		
		String type;
		String name;
		Long value;
		IdentifiableValue identyValue;
		
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
						
						identyValue = ms.getIdentifiable("cache_ListObj");
						if(identyValue != null){
							listObjList = (List<ListObj>)identyValue.getValue();
							for(index = 0;index < listObjList.size();index++){
								if(listObjList.get(index).link.equals(listObj.link) == true){
									listObjList.get(index).storesize = listObj.storesize;
									Collections.sort(listObjList);
									ms.putIfUntouched("cache_ListObj",identyValue,listObjList);
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
