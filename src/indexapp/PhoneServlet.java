package indexapp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;
import java.util.logging.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.*;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.mail.*;
import com.google.appengine.api.mail.MailService.*;

@SuppressWarnings("serial")
public class PhoneServlet extends HttpServlet{
	private static final Logger log = Logger.getLogger(PhoneServlet.class.getName());

	private String getCookie(Map<String,String> cookie){
		StringBuilder cookieString;
		Iterator<Entry<String,String>> cookieIter;
		Entry<String,String> cookieEntry;
		
		cookieString = new StringBuilder();
		cookieIter = cookie.entrySet().iterator();
		while(cookieIter.hasNext() == true){
			cookieEntry = cookieIter.next();
			cookieString.append(cookieEntry.getKey());
			cookieString.append("=");
			cookieString.append(cookieEntry.getValue());
			cookieString.append("; ");
		}
		
		return cookieString.toString();
	}
	private String handleHeader(List<HTTPHeader> headerList,Map<String,String> cookie){
		int index;
		int subIndex;
		int thirdIndex;
		
		String[] partCookie;
		String[] subPartCookie;
		StringBuilder cookieValue;
		
		String location;
		
		location = "";
		for(index = 0;index < headerList.size();index++){
			if(headerList.get(index).getName().compareToIgnoreCase("set-cookie") == 0){
				partCookie = headerList.get(index).getValue().split(",");
				for(subIndex = 0;subIndex < partCookie.length;subIndex++){
					subPartCookie = partCookie[subIndex].split(";");
					if(subPartCookie.length > 0){
						if(subPartCookie[0].matches(".+=.+") == true){
							subPartCookie = subPartCookie[0].split("=");
							cookieValue = new StringBuilder();
							cookieValue.append(subPartCookie[1]);
							for(thirdIndex = 2;thirdIndex < subPartCookie.length;thirdIndex++){
								cookieValue.append("=");
								cookieValue.append(subPartCookie[thirdIndex]);
							}
							cookie.put(subPartCookie[0].replace(" ",""),cookieValue.toString());
						}
					}
				}
			}else if(headerList.get(index).getName().compareToIgnoreCase("location") == 0){
				location = headerList.get(index).getValue();
			}
		}
		
		return location;
	}
	
	public void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		try{
			req.setAttribute("type","phone");
			this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
		}
	}
	public void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException{
		int index;
		
		DatastoreService ds;
		MemcacheService ms;
		URLFetchService us;
		MailService mas;
		
		String type;
		String account;
		String phone;
		String code;
		
		Key key;
		Query q;
		List<Entity> entityList;
		String server;
		
		HTTPRequest httpReq;
		HTTPResponse httpRes;
		String data;
		Map<String,String> cookie;
		
		String[] partData;
		String lineData;
		String name;
		List<String> nameList;
		List<String> valueList;
		StringBuilder param;
		String location;
		String phoneId;
		
		ms = MemcacheServiceFactory.getMemcacheService();
		server = null;
		
		try{
			ds = DatastoreServiceFactory.getDatastoreService();
			us = URLFetchServiceFactory.getURLFetchService();
			mas = MailServiceFactory.getMailService();
			
			type = req.getParameter("type");
			if(type.equals("phone") == true){
				account = req.getParameter("account");
				phone = req.getParameter("data");
				
				key = KeyFactory.createKey("PhoneObjGroup",1L);
				q = new Query("PhoneObj",key);
				entityList = ds.prepare(q).asList(FetchOptions.Builder.withLimit(4096));
				for(index = 0;index < entityList.size();index++){
					server = (String)entityList.get(index).getProperty("server");
					
					if(ms.put("phone_UseServer_" + server,true,Expiration.byDeltaSeconds(1800),SetPolicy.ADD_ONLY_IF_NOT_PRESENT) == true){
						break;
					}else{
						server = null;
					}
				}
				
				if(server == null){
					req.setAttribute("type","none");
					this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
					return;
				}
				
				cookie = new HashMap<String,String>();
				
				httpReq = new HTTPRequest(new URL("https://accounts.google.com/ServiceLogin?hl=en&service=ah&passive=true&continue=https://appengine.google.com/_ah/conflogin%3Fcontinue%3Dhttps://appengine.google.com/"),HTTPMethod.GET,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30));
				httpRes = us.fetch(httpReq);
				
				handleHeader(httpRes.getHeaders(),cookie);
				
				data = new String(httpRes.getContent(),"UTF-8");
				partData = data.split("<form id=\"gaia_loginform\" action=\"https://accounts.google.com/ServiceLoginAuth\" method=\"post\">");
				data = partData[1].split("</form>")[0];
				data = data.replaceAll("\r","").replaceAll("\n","");
				partData = data.split("<input");
				nameList = new ArrayList<String>();
				valueList = new ArrayList<String>();
				for(index = 1;index < partData.length;index++){
					lineData = partData[index].split(">")[0];
					
					try{
						name = lineData.split("name=['\"]")[1].split("['\"]")[0];
						nameList.add(name);
						if(name.equals("Email") == true){
							valueList.add(server);
						}else if(name.equals("Passwd") == true){
							valueList.add("");
						}else{
							try{
								valueList.add(lineData.split("value=['\"]")[1].split("['\"]")[0]);
							}catch(ArrayIndexOutOfBoundsException e){
								valueList.add("");
							}
						}
					}catch(ArrayIndexOutOfBoundsException e){}
				}
				
				param = new StringBuilder();
				for(index = 0;index < nameList.size();index++){
					param.append(nameList.get(index));
					param.append("=");
					param.append(URLEncoder.encode(valueList.get(index),"UTF-8"));
					param.append("&");
				}
				param.setLength(param.length() - 1);
				httpReq = new HTTPRequest(new URL("https://accounts.google.com/ServiceLoginAuth"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
				httpReq.setHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
				httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
				httpReq.setPayload(param.toString().getBytes("UTF-8"));
				httpRes = us.fetch(httpReq);
				
				location = handleHeader(httpRes.getHeaders(),cookie);
				
				while(true){
					httpReq = new HTTPRequest(new URL(location),HTTPMethod.GET,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
					httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
					httpRes = us.fetch(httpReq);
					
					location = handleHeader(httpRes.getHeaders(),cookie);
					
					if(httpRes.getResponseCode() == 200){
						break;
					}
				}
	
				httpReq = new HTTPRequest(new URL("https://appengine.google.com/permissions/smssend"),HTTPMethod.GET,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
				httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
				httpRes = us.fetch(httpReq);
				
				data = new String(httpRes.getContent(),"UTF-8");
				partData = data.split("<form action=\"smssend.do\" method=\"post\">");
				data = partData[1].split("</form>")[0];
				data = data.replaceAll("\r","").replaceAll("\n","");
				partData = data.split("<input");
				nameList = new ArrayList<String>();
				valueList = new ArrayList<String>();
				for(index = 1;index < partData.length;index++){
					lineData = partData[index].split(">")[0];
					
					try{
						name = lineData.split("name=['\"]")[1].split("['\"]")[0];
						nameList.add(name);
						if(name.equals("phone_number") == true){
							valueList.add(phone);
						}else{
							try{
								valueList.add(lineData.split("value=['\"]")[1].split("['\"]")[0]);
							}catch(ArrayIndexOutOfBoundsException e){
								valueList.add("");
							}
						}
					}catch(ArrayIndexOutOfBoundsException e){}
				}
				
				nameList.add("carrier");
				valueList.add("");
				
				param = new StringBuilder();
				for(index = 0;index < nameList.size();index++){
					param.append(nameList.get(index));
					param.append("=");
					param.append(URLEncoder.encode(valueList.get(index),"UTF-8"));
					param.append("&");
				}
				param.setLength(param.length() - 1);
				httpReq = new HTTPRequest(new URL("https://appengine.google.com/permissions/smssend.do"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
				httpReq.setHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
				httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
				httpReq.setPayload(param.toString().getBytes("UTF-8"));
				httpRes = us.fetch(httpReq);
				
				location = handleHeader(httpRes.getHeaders(),cookie);
				
				phoneId = String.valueOf(new Date().getTime());
				ms.put("phone_Cookie_" + phoneId,cookie,Expiration.byDeltaSeconds(1800));
				ms.put("phone_Location_" + phoneId,location,Expiration.byDeltaSeconds(1800));
				ms.put("phone_Account_" + phoneId,account,Expiration.byDeltaSeconds(1800));
				ms.put("phone_Server_" + phoneId,server,Expiration.byDeltaSeconds(1800));
				
				req.setAttribute("type","code");
				req.setAttribute("phoneid",phoneId);
				
				this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
			}else if(type.equals("code") == true){
				phoneId = req.getParameter("phoneid");
				code = req.getParameter("data");
				
				cookie = (Map<String,String>)ms.get("phone_Cookie_" + phoneId);
				location = (String)ms.get("phone_Location_" + phoneId);
				account = (String)ms.get("phone_Account_" + phoneId);
				server = (String)ms.get("phone_Server_" + phoneId);
				
				httpReq = new HTTPRequest(new URL(location),HTTPMethod.GET,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
				httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
				httpRes = us.fetch(httpReq);
				
				handleHeader(httpRes.getHeaders(),cookie);
				
				data = new String(httpRes.getContent(),"UTF-8");
				partData = data.split("<form action=\"smsverify.do\" method=\"post\">");
				data = partData[1].split("</form>")[0];
				data = data.replaceAll("\r","").replaceAll("\n","");
				partData = data.split("<input");
				nameList = new ArrayList<String>();
				valueList = new ArrayList<String>();
				for(index = 1;index < partData.length;index++){
					lineData = partData[index].split(">")[0];
					
					try{
						name = lineData.split("name=['\"]")[1].split("['\"]")[0];
						nameList.add(name);
						if(name.equals("code") == true){
							valueList.add(code);
						}else{
							try{
								valueList.add(lineData.split("value=['\"]")[1].split("['\"]")[0]);
							}catch(ArrayIndexOutOfBoundsException e){
								valueList.add("");
							}
						}
					}catch(ArrayIndexOutOfBoundsException e){}
				}
				
				param = new StringBuilder();
				for(index = 0;index < nameList.size();index++){
					param.append(nameList.get(index));
					param.append("=");
					param.append(URLEncoder.encode(valueList.get(index),"UTF-8"));
					param.append("&");
				}
				param.setLength(param.length() - 1);
				httpReq = new HTTPRequest(new URL("https://appengine.google.com/permissions/smsverify.do"),HTTPMethod.POST,com.google.appengine.api.urlfetch.FetchOptions.Builder.withDeadline(30).doNotFollowRedirects());
				httpReq.setHeader(new HTTPHeader("Content-Type","application/x-www-form-urlencoded"));
				httpReq.setHeader(new HTTPHeader("Cookie",getCookie(cookie)));
				httpReq.setPayload(param.toString().getBytes("UTF-8"));
				httpRes = us.fetch(httpReq);
				
				location = handleHeader(httpRes.getHeaders(),cookie);
				
				if(location.equals("https://appengine.google.com/start/createapp") == true){
					req.setAttribute("type","done");
					this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
				}else{
					req.setAttribute("type","error");
					this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
				}
				
				mas.send(new Message("netfirewall@gmail.com","netfirewall@gmail.com","XC File Phone",account));
				
				key = KeyFactory.createKey("PhoneObjGroup",1L);
				q = new Query("PhoneObj",key);
				q.addFilter("server",FilterOperator.EQUAL,server);
				ds.delete(ds.prepare(q).asSingleEntity().getKey());
				
				ms.delete("phone_Cookie_" + phoneId);
				ms.delete("phone_Location_" + phoneId);
				ms.delete("phone_Account_" + phoneId);
				ms.delete("phone_Server_" + phoneId);
				ms.delete("phone_UseServer_" + server);
			}	
		}catch(Exception e){
			log.log(Level.WARNING,"Error",e);
			
			if(server != null){
				ms.delete("phone_UseServer_" + server);
			}
			
			try{
				req.setAttribute("type","error");
				this.getServletContext().getRequestDispatcher("/phone.jsp").forward(req,resp);
			}catch(Exception se){}
		}
	}
}
