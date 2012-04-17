package indexapp;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

import commonapp.*;

@SuppressWarnings("serial")
public class ListServlet extends HttpServlet{
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		
		Long size;
		
		List<String> listObjKeyList;
		Map<String,Object> listObjMap;
		ListObj[] listObjArray;
		Key key;
		Query q;
		List<Entity> entityList;
		List<ListObj> listObjList;
		ListObj listObj;
		String fileid;
		String seckey;
		int serverCount;
		
		resp.setContentType("text/plain");
		
		ds = DatastoreServiceFactory.getDatastoreService();
		ms = MemcacheServiceFactory.getMemcacheService();
		
		size = Long.valueOf(req.getParameter("size"));
		
		listObjKeyList  = (List<String>)ms.get("cache_ListObjKeyList");
		listObjArray = null;
		if(listObjKeyList != null){
			listObjMap = (Map<String,Object>)ms.getAll(listObjKeyList);
			if(listObjMap.size() == listObjKeyList.size()){
				listObjArray = listObjMap.values().toArray(new ListObj[listObjMap.size()]);
			}
		}
			
		if(listObjArray == null){
			key = KeyFactory.createKey("ListObjGroup",1L);
			q = new Query("ListObj",key);
			entityList = ds.prepare(q).asList(FetchOptions.Builder.withLimit(4096));
			listObjKeyList = new ArrayList<String>();
			listObjList = new ArrayList<ListObj>();
			for(index = 0;index < entityList.size();index++){
				listObj = new ListObj();
				listObj.getDB(entityList.get(index));
				
				listObjKeyList.add("cache_ListObj_" + listObj.link);
				ms.put("cache_ListObj_" + listObj.link,listObj,null,SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
				
				listObjList.add(listObj);
			}
			ms.put("cache_ListObjKeyList",listObjKeyList);
			listObjArray = listObjList.toArray(new ListObj[listObjList.size()]);
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
	}
}
