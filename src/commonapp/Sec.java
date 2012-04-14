package commonapp;

import java.net.*;
import java.util.*;
import java.math.*;
import java.security.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.*;
import com.google.appengine.api.memcache.*;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.urlfetch.*;
import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.users.*;

public class Sec{
	public static final String ServerKey = "~6W~wd{O:#U)zIG>OrZ!pJrr^L9C*?g-8~fd!@-nsdf)LI:z0ew~V3+Q4qrf*Skzx9qw8ALE]:$).M6Kb'`L.vZ/bk>)CRcw(RBnwY6VR-_`<{+Ow6n\t>',wy'-gj),|S2iSHeit(ulZXYkfmYo";
	
	private static final char[] UIDMap = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
			'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','Z','Y','Z',
			'0','1','2','3','4','5','6','7','8','9'};
	private static final byte[] UserSecIdAES = {-15,-11,54,37,-70,73,78,18,75,-29,-53,-123,-117,-110,-13,56};
	
	public static String createFileID(MemcacheService ms){
		Long num;
		Long ret;
		String fileid;
		
		fileid = "";
		num = (new Date().getTime()) * 62L;
		
		ret = ms.increment("sec_FileID_Count",1);
		if(ret == null){
			ms.put("sec_FileID_Count",0L,Expiration.byDeltaSeconds(86400),SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			ret = ms.increment("sec_FileID_Count",1);
		}
		num += (ret - 1L) % 62L;
		
		while(num > 0L){
			fileid += UIDMap[(int)(num % 62L)];
			num /= 62L;
		}
		return fileid;
	}
	public static String createSecKey(String data){
		MessageDigest md;
		
		try{
			md = MessageDigest.getInstance("SHA-512");
		}catch(Exception e){
			md = null;
		}
		
		md.update((data + "E(UZ(&#ss%^wzds7s87s(*@s*(&s92-a89fe450*2&^@783ad97(*^dfEnhad^&*fg543a9l?>p[DAapd[fsjh&*S6Ha3kiuo8)Ks6^&S6haASDdx&*s><?L{P)XD>L?4093.lzx0sapzahu23789dHJ").getBytes());

		return new BigInteger(md.digest()).toString(16);
	}
	
	public static List<String> createLogin(DatastoreService ds,MemcacheService ms,URLFetchService us,UserService userservice) throws Exception{
		User user;
		String userId;
		String[] serverList;
		String userSecId;
		String userLink;
		String userSecKey;
		
		Key key;
		Query q;
		Entity entity;
		HTTPRequest httpReq;
		Cipher cipherAES;
		SecretKeySpec keySpec;
		List<String> ret;
		
		user = userservice.getCurrentUser();
		if(user != null){
			userId = user.getUserId();
			
			userLink = (String)ms.get("User_Link_" + userId);
			if(userLink == null){
				key = KeyFactory.createKey("UserLinkObjGroup",1L);
				q = new Query("UserLinkObj",key);
				q.addFilter("user",FilterOperator.EQUAL,user);
				entity = ds.prepare(q).asSingleEntity();
				
				if(entity == null){
					httpReq = new HTTPRequest(new URL(Common.ListServer + "/list"),HTTPMethod.POST,FetchOptions.Builder.withDeadline(10));
					httpReq.setPayload("size=-1".getBytes("UTF-8"));
					serverList = new String(us.fetch(httpReq).getContent()).split("\n");
					userLink = serverList[new Random().nextInt(serverList.length)];
					
					entity = new Entity("UserLinkObj",key);
					entity.setProperty("user",user);
					entity.setProperty("link",userLink);
					ds.put(entity);
				}else{
					userLink = (String)entity.getProperty("link");
				}
				
				ms.put("User_Link_" + userId,userLink);
			}
			
			keySpec = new SecretKeySpec(UserSecIdAES,"AES");
			cipherAES = Cipher.getInstance("AES");
			cipherAES.init(Cipher.ENCRYPT_MODE,keySpec);
			
			userSecId = new BigInteger(cipherAES.doFinal(userId.getBytes())).toString(16);
			userSecKey = createSecKey(userSecId + "|" + userLink);
		
			ret = new ArrayList<String>();
			ret.add(userSecId);
			ret.add(userLink);
			ret.add(userSecKey);
		}else{
			ret = null;
		}
		
		return ret;
	}
	public static String getLogin(String userSecId,String userLink,String userSecKey) throws Exception{
		Cipher cipherAES;
		SecretKeySpec keySpec;
		
		if(createSecKey(userSecId + "|" + userLink).equals(userSecKey) == false){
			return null;
		}
		
		keySpec = new SecretKeySpec(UserSecIdAES,"AES");
		cipherAES = Cipher.getInstance("AES");
		cipherAES.init(Cipher.DECRYPT_MODE,keySpec);
		
		return new String(cipherAES.doFinal(new BigInteger(userSecId,16).toByteArray()));
	}
}
