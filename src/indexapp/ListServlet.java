package indexapp;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import commonapp.*;

@SuppressWarnings("serial")
public class ListServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(ListServlet.class.getName());
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		
		Long size;
		
		ListObj[] listObjArray;
		Map<String,ListObj> listObjMap;
		String fileid;
		String seckey;
		int serverCount;
		
		resp.setContentType("text/plain");
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
			
			size = Long.valueOf(req.getParameter("size"));
			
			listObjArray  = (ListObj[])ms.get("cache_ListObjArray");
			if(listObjArray == null){
				listObjMap = ListObj.getDBMap(ds);
				listObjArray = listObjMap.values().toArray(new ListObj[listObjMap.size()]);
				ms.put("cache_ListObjArray",listObjArray,null,SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			}
			
			Arrays.sort(listObjArray);
			
			if(size == -1){
				for(index = 0;index < listObjArray.length;index++){
					resp.getWriter().println(listObjArray[index].link);
				}
			}else{
				fileid = Sec.createFileID(ms);
				seckey = Sec.createSecKey(fileid);
				
				resp.getWriter().println(fileid);
				resp.getWriter().println(seckey);
				
				if(size <= 8388608L){
					resp.getWriter().println(listObjArray[0].link);
				}else{
					serverCount = Math.min(listObjArray.length,(int)Math.ceil(Math.ceil(Math.sqrt(size * 32L)) / 1024.0));
					
					for(index = 0;index < serverCount;index++){
						resp.getWriter().println(listObjArray[index].link);
					}
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
		}
	}
}
