package uploadapplet;

import java.io.*;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import netscape.javascript.*;
import commonapplet.*;

@SuppressWarnings("serial")
public class UploadApplet extends JApplet implements ActionListener{
	public static final String ListServer = "http://xcfilelab.appspot.com";
	
	public static JSObject js;
	public static String userSecId;
	public static String userLink;
	public static String userSecKey;
	public static boolean loginFlag;
	public static int uploadCount;
	public static ExecutorService threadPool;
	public static Queue<UploadThread> queueUpload;
	public static Map<Integer,UploadThread> mapUpload;
	
	private FlatButton buttonOpen;
	private FlatButton buttonUpload;
	
	public void init(){
		try{
			js = JSObject.getWindow(this);
			
			userSecId = this.getParameter("usersecid");
			userLink = this.getParameter("userlink");
			userSecKey = this.getParameter("userseckey");
			if(userSecId.equals("") == true){
				loginFlag = false;
			}else{
				loginFlag = true;
			}
			
			uploadCount = 0;
			threadPool = Executors.newFixedThreadPool(1);
			queueUpload = new LinkedBlockingQueue<UploadThread>();
			mapUpload = new HashMap<Integer,UploadThread>();
			
			SwingUtilities.invokeAndWait(new Runnable(){
				public void run(){
					swingInit();
				}
			});
		}catch(Exception e){}
	}
	
	private void swingInit(){
		Container con;
		GridBagConstraints gridCon;
		
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e){}
		con = getContentPane();
		con.setBackground(new Color(253,253,253));
		con.setLayout(new GridBagLayout());
		this.setTransferHandler(new FileDropHandler());
		
		gridCon = new GridBagConstraints();
		gridCon.fill = GridBagConstraints.BOTH;
		gridCon.weightx = 0;
		gridCon.weighty = 0;
		gridCon.gridwidth = 1;
		gridCon.gridheight = 1;
		
		buttonOpen = new FlatButton("加入檔案");
		gridCon.gridx = 0;
		gridCon.gridy = 0;
		gridCon.insets = new Insets(0,0,0,2);
		buttonOpen.setPreferredSize(new Dimension(100,50));
		buttonOpen.setActionCommand("open");
		buttonOpen.addActionListener(this);
		this.add(buttonOpen,gridCon);
		
		buttonUpload = new FlatButton("上傳");
		gridCon.gridx = 1;
		gridCon.gridy = 0;
		gridCon.insets = new Insets(0,2,0,0);
		buttonUpload.setPreferredSize(new Dimension(100,50));
		buttonUpload.setActionCommand("upload");
		buttonUpload.addActionListener(this);
		this.add(buttonUpload,gridCon);
	}
	
	//JavaScript
	public static void addUpload(File file){
		UploadThread uploadThread;
		
		try{
			uploadThread = new UploadThread(uploadCount,file);
			queueUpload.add(uploadThread);
			mapUpload.put(uploadCount,uploadThread);
			uploadCount++;
			
			js.call("uploadCreateItem",new Object[]{uploadThread.itemid,uploadThread.file.getName()});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void updateState(int itemid,String state){
		try{
			js.call("uploadUpdateState",new Object[]{itemid,state});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void updateProg(int itemid,double progvalue,Long speed){
		try{
			js.call("uploadUpdateProg",new Object[]{itemid,progvalue,speed});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void showLink(int itemid,String fileid,Long filesize,String link){
		try{
			js.call("uploadShowLink",new Object[]{itemid,fileid,filesize,link});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void deleteUpload(int itemid){
		UploadThread uploadThread;
		
		uploadThread = mapUpload.get(itemid);
		if(uploadThread != null){
			uploadThread.cancelflag = true;
		}
	}
	public static void cancelUpload(int itemid){
		UploadThread uploadThread;
		
		uploadThread = mapUpload.get(itemid);
		if(uploadThread != null){
			uploadThread.cancelflag = true;
		}
	}
	public static void copyLink(String link){
		SystemClipboard.copyvalue(link);
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		if(e.getActionCommand().equals("open") == true){
			int index;
			File[] fileList;
			
			JFileChooser fileChooser;
			
			fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(true);
			if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
				fileList = fileChooser.getSelectedFiles();
				for(index = 0;index < fileList.length;index++){
					if(fileList[index].length() > 0L){
						addUpload(fileList[index]);
					}
				}
			}
		}else if(e.getActionCommand().equals("upload") == true){
			UploadThread uploadThread;
			
			while(queueUpload.isEmpty() == false){
				uploadThread = queueUpload.poll();
				if(uploadThread.cancelflag == false){
					updateState(uploadThread.itemid,"upload");
					threadPool.submit(uploadThread);
				}
			}
		}
	}
}