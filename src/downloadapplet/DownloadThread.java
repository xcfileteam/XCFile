package downloadapplet;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import commonapplet.Pair;

public class DownloadThread extends SwingWorker<Void,Void>{
	public static final long PART_SIZE = 8388608L;
	
	public boolean cancelflag;
	public FileChannel fileChannel;
	
	private File file;
	private Long filesize;
	private List<String> serverlist;
	private List<String> blobkeylist;
	private RandomAccessFile RAF;
	private BlockingQueue<Runnable> threadQueue;
	private ThreadPoolExecutor threadPool;
	private Long progSize;
	private LinkedList<Pair<Long,Long>> progQueue;
	
	public DownloadThread(File file,Long filesize,List<String> serverlist,List<String> blobkeylist){
		try{
			this.file = file;
			this.filesize = filesize;
			this.serverlist = serverlist;
			this.blobkeylist = blobkeylist;
			
			cancelflag = false;
			RAF = new RandomAccessFile(file,"rw");
			fileChannel = RAF.getChannel();
			threadQueue = new LinkedBlockingQueue<Runnable>();
			threadPool = new ThreadPoolExecutor(10,Integer.MAX_VALUE,Long.MAX_VALUE,TimeUnit.HOURS,threadQueue);
			progSize = 0L;
			progQueue = new LinkedList<Pair<Long,Long>>();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized void updateProg(int len){
		progSize += len;
	}
	
	private boolean updateLoop() throws Exception{
		Long startTime;
		Long endTime;
		boolean tweakFlag;
		Long nowSpeed;
		int coreSize;
		
		progQueue.add(new Pair<Long,Long>(new Date().getTime(),progSize));
		endTime = progQueue.getLast().first;
		tweakFlag = false;
		while(true){
			startTime = progQueue.getFirst().first;
			
			if(progQueue.size() == 2){
				break;
			}
			if((endTime - startTime) >= 5000){
				progQueue.poll();
				tweakFlag = true;
			}else{
				break;
			}
		}
		nowSpeed = (progQueue.getLast().second - progQueue.getFirst().second) / (endTime - startTime) * 1000L;
			
		if(tweakFlag == true){
			coreSize = threadPool.getCorePoolSize();
			
			if(nowSpeed >= 307200L * (long)coreSize){
				threadPool.setCorePoolSize(coreSize + 1);
			}else if(nowSpeed < 307200L * (long)(coreSize - 1)){
				threadPool.setCorePoolSize(coreSize - 1);
			}
		}
		
		DownloadApplet.updateProg(((double)progSize / (double)filesize) * 100.0,nowSpeed);
	
		if(cancelflag == true){
			threadPool.shutdownNow();
			fileChannel.close();
			RAF.close();
			file.delete();
			
			DownloadApplet.updateState("init");
			
			throw new InterruptedException();
		}
		if(threadPool.getActiveCount() == 0){
			return false;
		}
		return true;
	}

	@Override
	protected Void doInBackground() throws Exception{
		int index;
		
		Long partSize;
		Long subSize;
		Long offset;
		int blobkeyIndex;
		String serverlink;
		PartThread partThread;
		int threadCount;
		int delayTime;
		
		try{
			DownloadApplet.updateState("download");
			
			partSize = filesize / serverlist.size();
			index = 0;
			offset = 0L;
			blobkeyIndex = 0;
			
			threadCount = 0;
			if((partSize / PART_SIZE) * serverlist.size() > 10){
				delayTime = 2000;
			}else{
				delayTime = 200;
			}
			
			progQueue.add(new Pair<Long,Long>(new Date().getTime(),progSize));
			while(offset < filesize){
				if((filesize - offset) < partSize){
					partSize = filesize - offset;
				}
				
				serverlink = serverlist.get(index);
				subSize = 0L;
				while(subSize < partSize){
					if((partSize - subSize) < PART_SIZE){
						partThread = new PartThread(serverlink,blobkeylist.get(blobkeyIndex),offset + subSize,partSize - subSize,this);
						threadPool.execute(partThread);
						subSize = partSize;
					}else{
						partThread = new PartThread(serverlink,blobkeylist.get(blobkeyIndex),offset + subSize,PART_SIZE,this);
						threadPool.execute(partThread);
						subSize += PART_SIZE;
					}
					
					blobkeyIndex++;
					
					threadCount++;
					if(threadCount <= 10){
						Thread.sleep(delayTime);
					}
					
					updateLoop();
				}
				
				index = (index + 1) % serverlist.size();
				offset += partSize;
			}
			
			while(updateLoop() == true){
				Thread.sleep(500);
			}
			
			fileChannel.close();
			RAF.close();
			DownloadApplet.updateState("done");
			
			System.out.println("Done");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
}

class PartThread extends SwingWorker<Void,Void>{
	private String serverlink;
	private String blobkey;
	private Long offset;
	private Long partsize;
	private DownloadThread downloadThread;
	
	public PartThread(String serverlink,String blobkey,Long offset,Long partsize,DownloadThread downloadThread){
		this.serverlink = serverlink;
		this.blobkey = blobkey;
		this.offset = offset;
		this.partsize = partsize;
		this.downloadThread = downloadThread;
	}
	
	@Override
	public Void doInBackground() throws Exception{
		int retry;
		
		HttpURLConnection conn;
		BufferedOutputStream outb;
		ReadableByteChannel rbc;
		FileChannel fc;
	
		String param;
		long nowOffset;
		int rl;
		ByteBuffer fbuf;
		int resCode;
		
		retry = 8;
		while(retry > 0 && this.isCancelled() == false){
			try{
				conn = (HttpURLConnection)new URL(serverlink + "/download").openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setRequestProperty("Cache-Control","no-cache,max-age=0");
				conn.setRequestProperty("Pragma","no-cache");
				
				outb = new BufferedOutputStream(conn.getOutputStream());
				param = "blobkey=" + URLEncoder.encode(blobkey,"UTF-8");
				outb.write(param.getBytes("UTF-8"));
				outb.flush();
				
				resCode = conn.getResponseCode();
				if(resCode == 200){
					rbc = Channels.newChannel(conn.getInputStream());
					fc = downloadThread.fileChannel;
					fbuf = ByteBuffer.allocateDirect(131072);
					nowOffset = offset;
					while((nowOffset - offset) < partsize){	
						fbuf.clear();
						rl = rbc.read(fbuf);
						if(rl == -1){
							break;
						}
						fbuf.flip();
						
						while(fbuf.hasRemaining() == true){
							fc.write(fbuf,nowOffset);
						}
						
						downloadThread.updateProg(rl);
						nowOffset += rl;
					}
					rbc.close();
					
					break;
				}
				outb.close();
				
				if(resCode == 200){
					break;
				}
			}catch(ClosedChannelException e){
				break;
			}catch(Exception e){
				e.printStackTrace();
			}
			
			retry--;
		}
		
		return null;
	}
}
