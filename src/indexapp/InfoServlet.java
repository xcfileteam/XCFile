package indexapp;

import java.io.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import commonapp.*;

@SuppressWarnings("serial")
public class InfoServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(InfoServlet.class.getName());
	
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{	
		MemcacheService ms;
		Queue taskqueue;
		
		String type;
		String name;
		Long value;
		
		Long blobIncIndex;
		
		resp.setContentType("text/plain");
		
		try{
			ms = MemcacheServiceFactory.getMemcacheService();
			
			type = req.getParameter("type");
			if(type.equals("stateblobinc") == true){
				taskqueue = QueueFactory.getQueue("state");
				
				if(req.getParameter("serverkey").equals(Sec.ServerKey) == false){
					throw new Exception("ServerKey Wrong");
				}
				
				name = req.getParameter("name");
				value = Long.valueOf(req.getParameter("value"));
				
				blobIncIndex = ((Long)ms.increment("state_BlobIncIndex",1L,0L) & 0xFFFFFFFFL) - 1L;
				ms.put("state_BlobIncList_" + String.valueOf(blobIncIndex),new Pair<String,Long>("http://" + name,value));
				
				if(ms.put("task_State_BlobInc",true,Expiration.byDeltaSeconds(70),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
					taskqueue.add(TaskOptions.Builder.withUrl("/task").method(Method.POST).param("type","stateblobinc").countdownMillis(60000));
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
}
