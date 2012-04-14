package fileapp;

import java.io.*;
import java.nio.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.blobstore.*;
import com.google.appengine.api.files.*;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(DownloadServlet.class.getName());
	
	public void retData(String blobkeystring,OutputStream out) throws Exception{
		FileService fs;
		
		AppEngineFile file;
		FileReadChannel fReader;
		BufferedOutputStream outb;
		byte[] fbuf;
		ByteBuffer bufb;
		int rl;		
		
		fs = FileServiceFactory.getFileService();
		
		file = fs.getBlobFile(new BlobKey(blobkeystring));
		fReader = fs.openReadChannel(file,false);
		outb = new BufferedOutputStream(out);
		fbuf = new byte[524288];
		bufb = ByteBuffer.wrap(fbuf);
		while(true){
			bufb.clear();
			rl = fReader.read(bufb);
			if(rl == -1){
				break;
			}
			outb.write(fbuf,0,rl);
		}
		outb.flush();
	}
	
	public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		DatastoreService ds;
		MemcacheService ms;
		
		String fileid;
		String blobkey;
		
		FileObj fileObj;
		Key key;
		Query q;
		String serverLink;
				
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			ms = MemcacheServiceFactory.getMemcacheService();
		
			blobkey = req.getParameter("blobkey");
			if(blobkey != null){
				resp.setContentType("application/octet-stream");
				retData(blobkey,resp.getOutputStream());
			}else{
				fileid = req.getRequestURI().split("/")[2];
				
				fileObj = (FileObj)ms.get("cache_FileObj_" + fileid);
				if(fileObj == null){
					key = KeyFactory.createKey("FileObjGroup",1L);
					q = new Query("FileObj",key);
					q.addFilter("fileid",FilterOperator.EQUAL,fileid);
					fileObj = new FileObj();
					fileObj.getDB(ds.prepare(q).asSingleEntity());
					ms.put("cache_FileObj_" + fileObj.fileid,fileObj);
				}
				
				if(fileObj.filesize <= 8388608L){
					serverLink = fileObj.serverlist;
					if(serverLink.equals("http://" + req.getServerName()) == true){
						resp.setContentType("application/octet-stream");
						retData(fileObj.blobkeylist,resp.getOutputStream());
					}else{
						resp.sendRedirect(serverLink + "/down/" + fileObj.filename + "?blobkey=" + fileObj.blobkeylist);
					}
				}else{
					req.setAttribute("filename",fileObj.filename);
					req.setAttribute("filesize",String.valueOf(fileObj.filesize));
					req.setAttribute("serverlist",fileObj.serverlist);
					req.setAttribute("blobkeylist",fileObj.blobkeylist);
					this.getServletContext().getRequestDispatcher("/down.jsp").forward(req,resp);
				}
			}
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR");
		}
	}
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		String blobkeystring;
		
		resp.setContentType("application/octet-stream");
		
		try{
			blobkeystring = req.getParameter("blobkey");
			retData(blobkeystring,resp.getOutputStream());
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			resp.sendError(500,"ERROR"); 
		}
	}
}
