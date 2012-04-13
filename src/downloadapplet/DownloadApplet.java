package downloadapplet;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import netscape.javascript.*;
import commonapplet.*;

@SuppressWarnings("serial")
public class DownloadApplet extends JApplet implements ActionListener{
	public static JSObject js;
	public static String filename;
	public static Long filesize;
	public static List<String> serverlist;
	public static List<String> blobkeylist;
	
	private FlatButton buttonCon;
	private DownloadThread downloadThread;
	
	public void init(){
		int index;
		String[] serverPart;
		String[] blobkeyPart;
		
		js = JSObject.getWindow(this);
		
		filename = this.getParameter("filename");
		filesize = Long.valueOf(this.getParameter("filesize"));

		serverPart = this.getParameter("serverlist").split("\\|");
		serverlist = new ArrayList<String>();
		for(index = 0;index < serverPart.length;index++){
			serverlist.add(serverPart[index]);
		}
		
		blobkeyPart = this.getParameter("blobkeylist").split("\\|");
		blobkeylist = new ArrayList<String>();
		for(index = 0;index < blobkeyPart.length;index++){
			blobkeylist.add(blobkeyPart[index]);
		}
		
		try{
			SwingUtilities.invokeAndWait(new Runnable(){
				public void run(){
					swingInit();
				}
			});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void swingInit(){
		Container con;
		GridBagConstraints gridCon;
		
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e){}
		this.setSize(100,50);
		con = getContentPane();
		con.setBackground(new Color(253,253,253));
		con.setLayout(new GridBagLayout());
		
		gridCon = new GridBagConstraints();
		gridCon.fill = GridBagConstraints.BOTH;
		gridCon.weightx = 1;
		gridCon.weighty = 1;
		gridCon.gridwidth = 1;
		gridCon.gridheight = 1;
		
		buttonCon = new FlatButton("下載檔案");
		gridCon.gridx = 0;
		gridCon.gridy = 0;
		buttonCon.setActionCommand("save");
		buttonCon.addActionListener(this);
		this.add(buttonCon,gridCon);
	}
	
	//JavaScript
	public static void updateState(String state){
		try{
			js.call("updateState",new Object[]{state});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void updateProg(double value,Long speed){
		try{
			js.call("updateProg",new Object[]{value,speed});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("save") == true){
			File file;
			JFileChooser fileChooser;
			
			fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setSelectedFile(new File(filename));
			if(fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
				buttonCon.setText("取消");
				buttonCon.setActionCommand("cancel");
				
				file = fileChooser.getSelectedFile();
				downloadThread = new DownloadThread(file,filesize,serverlist,blobkeylist);
				downloadThread.execute();
			}
		}else if(e.getActionCommand().equals("cancel") == true){
			downloadThread.cancelflag = true;
			
			buttonCon.setText("下載檔案");
			buttonCon.setActionCommand("save");
		}
	}
}
