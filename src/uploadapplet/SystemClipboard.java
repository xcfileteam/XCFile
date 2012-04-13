package uploadapplet;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class SystemClipboard{
	private static String value;
	public static void copyvalue(String value){
		SystemClipboard.value = value;		
		java.security.AccessController.doPrivileged(new java.security.PrivilegedAction(){
			public Object run(){
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(SystemClipboard.value),null);
				return "";
			}
		});
	}
}
