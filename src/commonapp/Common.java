package commonapp;

import java.net.*;

public class Common{
	public static final String ListServer = "http://xcfilelab.appspot.com";
	
	public static String EncodeURI(String data) throws Exception{
		String encodeData;
		
		encodeData = URLEncoder.encode(data,"UTF-8");
		encodeData = encodeData.replace("+","%20");
		encodeData = encodeData.replace("%7E","~");
		encodeData = encodeData.replace("%27","'");
		encodeData = encodeData.replace("%28","(");
		encodeData = encodeData.replace("%29",")");
		encodeData = encodeData.replace("%21","!");
		
		return encodeData;
	}
}
