package indexapp;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import commonapp.*;

@SuppressWarnings("serial")
public class ListServlet extends HttpServlet{
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		
		Long size;
		
		List<ListObj> listObjList;
		Key key;
		Query q;
		List<Entity> entityList;
		ListObj listObj;
		String fileid;
		String seckey;
		
		resp.setContentType("text/plain");
		
		ds = DatastoreServiceFactory.getDatastoreService();
		ms = MemcacheServiceFactory.getMemcacheService();
		
		size = Long.valueOf(req.getParameter("size"));
		
		listObjList  = (List<ListObj>)ms.get("cache_ListObj");
		if(listObjList == null){
			key = KeyFactory.createKey("ListObjGroup",1L);
			q = new Query("ListObj",key);
			entityList = ds.prepare(q).asList(FetchOptions.Builder.withLimit(4096));
			listObjList = new ArrayList<ListObj>();
			for(index = 0;index < entityList.size();index++){
				listObj = new ListObj();
				listObj.getDB(entityList.get(index));
				listObjList.add(listObj);
			}
			Collections.sort(listObjList);
			ms.put("cache_ListObj",listObjList);
		}
		
		if(size == -1){
			for(index = 0;index < listObjList.size();index++){
				resp.getWriter().println(listObjList.get(index).link);
			}
		}else{
			fileid = Sec.createFileID(ms);
			seckey = Sec.createSecKey(fileid);
			
			resp.getWriter().println(fileid);
			resp.getWriter().println(seckey);
			
			if(size <= 8388608L){
				resp.getWriter().println(listObjList.get(0).link);
			}else{
				for(index = 0;index < listObjList.size();index++){
					resp.getWriter().println(listObjList.get(index).link);
				}
			}
		}
	}
}
