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
	private Long netSize;
	private Long realSize;
	private double progNum;
	private boolean tweakFlag;
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
			threadPool = new ThreadPoolExecutor(2,Integer.MAX_VALUE,Long.MAX_VALUE,TimeUnit.HOURS,threadQueue);
			netSize = 0L;
			realSize = 0L;
			progNum = 0.0;
			tweakFlag = false;
			progQueue = new LinkedList<Pair<Long,Long>>();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized void incNetSize(int len){
		netSize += len;
	}
	public synchronized void incRealSize(long len){
		realSize += len;
	}
	
	private boolean updateLoop() throws Exception{
		Long prevNetSize;
		Long deltaNetSize;
		Long startTime;
		Long endTime;
		Long nowSpeed;
		int coreSize;
		
		prevNetSize = progQueue.getLast().second;
		progQueue.add(new Pair<Long,Long>(new Date().getTime(),netSize));
		deltaNetSize = progQueue.getLast().second - prevNetSize;
		endTime = progQueue.getLast().first;
		while(true){
			startTime = progQueue.getFirst().first;
			
			if(progQueue.size() == 2){
				break;
			}
			if((endTime - startTime) >= 10000){
				progQueue.poll();
			}else{
				break;
			}
		}
		nowSpeed = (progQueue.getLast().second - progQueue.getFirst().second) / (endTime - startTime) * 1000L;
			
		if(tweakFlag == true){
			coreSize = threadPool.getCorePoolSize();
			
			if(nowSpeed >= 102400L * (long)coreSize){
				threadPool.setCorePoolSize(coreSize + 1);
			}else if(nowSpeed < 102400L * (long)(coreSize - 1)){
				threadPool.setCorePoolSize(coreSize - 1);
			}
		}
		
		if(filesize > realSize){
			progNum += (100.0 - progNum) / (double)(filesize - realSize) * (double)deltaNetSize;
			DownloadApplet.updateProg(progNum,nowSpeed);
			
			incRealSize(deltaNetSize);
		}else{
			DownloadApplet.updateProg(100.0,nowSpeed);
		}
	
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
			if((partSize / PART_SIZE) * serverlist.size() > 2){
				delayTime = 2000;
			}else{
				delayTime = 200;
			}
			
			progQueue.add(new Pair<Long,Long>(new Date().getTime(),netSize));
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
					if(threadCount <= 2){
						Thread.sleep(delayTime);
					}
					
					updateLoop();
				}
				
				index = (index + 1) % serverlist.size();
				offset += partSize;
			}
			
			tweakFlag = true;
			while(updateLoop() == true){
				Thread.sleep(1000);
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
	
		int rollbackRealSize;
		String param;
		long nowOffset;
		int subLimit;
		int readSize;
		int rl;
		ByteBuffer fbuf;
		int resCode;
		
		rollbackRealSize = 0;
		retry = 8;
		while(retry > 0 && this.isCancelled() == false){
			downloadThread.incRealSize(-rollbackRealSize);
			rollbackRealSize = 0;
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
					fbuf = ByteBuffer.allocateDirect(524288);
					nowOffset = offset;
					while((nowOffset - offset) < partsize){	
						fbuf.clear();
						readSize = 0;
						subLimit = 0;
						while(subLimit < 524288){
							subLimit = Math.min(subLimit + 65536,524288);
							fbuf.limit(subLimit);
							
							rl = 0;
							while(fbuf.hasRemaining() == true){
								rl = rbc.read(fbuf);
								if(rl == -1){
									break;
								}
								downloadThread.incNetSize(rl);
								rollbackRealSize += rl;
								readSize += rl;
							}
							if(rl == -1){
								break;
							}
						}
						fbuf.flip();

						while(fbuf.hasRemaining() == true){
							fc.write(fbuf,nowOffset);
						}

						nowOffset += readSize;
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
			}catch(UnknownHostException e){
				e.printStackTrace();
				Thread.sleep(2000);
			}catch(Exception e){
				e.printStackTrace();
			}
			
			retry--;
		}
		
		return null;
	}
}
