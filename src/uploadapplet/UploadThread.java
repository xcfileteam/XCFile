package uploadapplet;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import commonapplet.*;

public class UploadThread extends SwingWorker<Void,Void>{
	public static final long PART_SIZE = 8388608L;
	
	public int itemid;
	public boolean cancelflag;
	public File file;
	public String fileid;
	public String fileseckey;
	public Map<Long,String> blobkeyMap;
	public FileChannel fileChannel;
	public AtomicLong netSize;
	public AtomicLong realSize;
	
	private String filename;
	private RandomAccessFile RAF;
	private List<String> serverList;
	private BlockingQueue<Runnable> threadQueue;
	private ThreadPoolExecutor threadPool;
	private double progNum;
	private boolean tweakFlag;
	private LinkedList<Pair<Long,Long>> progQueue;
	
	public UploadThread(int itemid,File file){
		try{
			this.itemid = itemid;
			this.file = file;
			
			filename = file.getName();
			
			cancelflag = false;
			blobkeyMap = Collections.synchronizedSortedMap(new TreeMap<Long,String>());
			RAF = new RandomAccessFile(file,"r");
			fileChannel = RAF.getChannel();
			serverList = new ArrayList<String>();
			threadQueue = new LinkedBlockingQueue<Runnable>();
			threadPool = new ThreadPoolExecutor(2,Integer.MAX_VALUE,Long.MAX_VALUE,TimeUnit.HOURS,threadQueue);
			netSize = new AtomicLong(0L);
			realSize = new AtomicLong(0L);
			progNum = 0.0;
			tweakFlag = false;
			progQueue = new LinkedList<Pair<Long,Long>>();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void getList(Long filesize) throws Exception{
		HttpURLConnection conn;
		BufferedOutputStream outb;
		BufferedReader reader;
		String param;
		
		String serverLink;
		
		conn = (HttpURLConnection)new URL(UploadApplet.ListServer + "/list").openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestProperty("Cache-Control","no-cache,max-age=0");
		conn.setRequestProperty("Pragma","no-cache");
		
		outb = new BufferedOutputStream(conn.getOutputStream());
		param = "size=" + URLEncoder.encode(String.valueOf(filesize),"UTF-8");
		outb.write(param.getBytes("UTF-8"));
		outb.flush();
		
		reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		fileid = reader.readLine();
		fileseckey = reader.readLine();
		while(true){
			serverLink = reader.readLine();
			if(serverLink == null){
				break;
			}
			serverList.add(serverLink);
		}
		
		outb.close();
		reader.close();
	}
	private void postUpload(String serverlink) throws Exception{
		int index;
		int retry;
		
		String serverString;
		Object[] blobkeyList;
		String blobkeyString;
		
		HttpURLConnection conn;
		BufferedOutputStream outb;
		StringBuilder header;
		int resCode;
		
		serverString = serverList.get(0);
		for(index = 1;index < serverList.size();index++){
			serverString += "|" + serverList.get(index);
		}

		blobkeyList = blobkeyMap.values().toArray();
		blobkeyString = (String)blobkeyList[0];
		for(index = 1;index < blobkeyList.length;index++){
			blobkeyString += "|" + (String)blobkeyList[index];
		}

		retry = 8;
		while(retry > 0){
			try{
				conn = (HttpURLConnection)new URL(serverlink + "/upload").openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setRequestProperty("Cache-Control","no-cache,max-age=0");
				conn.setRequestProperty("Pragma","no-cache");
				
				header = new StringBuilder();
				header.append(fileid);
				header.append("\r\n");
				header.append(fileseckey);
				header.append("\r\n");
				header.append("create\r\n");
				header.append(filename);
				header.append("\r\n");
				header.append(String.valueOf(file.length()));
				header.append("\r\n");
				header.append(serverString);
				header.append("\r\n");
				header.append(blobkeyString);
				header.append("\r\n");
				
				if(UploadApplet.loginFlag == false){
					header.append("\r\n");
				}else{
					header.append(UploadApplet.userSecId);
					header.append("\r\n");
					header.append(UploadApplet.userSecKey);
					header.append("\r\n");
				}
				
				outb = new BufferedOutputStream(conn.getOutputStream());
				outb.write(header.toString().getBytes("UTF-8"));
				outb.flush();
				
				resCode = conn.getResponseCode();
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
				retry--;
			}
		}
		if(retry == 0){
			throw new Exception("Error");
		}
	}
	private boolean updateLoop() throws Exception{
		Long prevNetSize;
		Long deltaNetSize;
		Long currRealSize;
		Long startTime;
		Long endTime;
		Long nowSpeed;
		int coreSize;
		
		prevNetSize = progQueue.getLast().second;
		currRealSize = realSize.get();
		progQueue.add(new Pair<Long,Long>(new Date().getTime(),netSize.get()));
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
			}else if(coreSize > 2 && nowSpeed < 102400L * (long)(coreSize - 1)){
				threadPool.setCorePoolSize(coreSize - 1);
			}
		}
		
		if(file.length() > currRealSize){
			progNum += (100.0 - progNum) / (double)(file.length() - currRealSize) * (double)deltaNetSize;
			UploadApplet.updateProg(itemid,progNum,nowSpeed);
			
			realSize.addAndGet(deltaNetSize);
		}else{
			UploadApplet.updateProg(itemid,100.0,nowSpeed);
		}
		
		if(cancelflag == true){
			threadPool.shutdownNow();
			fileChannel.close();
			RAF.close();
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

		String serverHead;
		Long fileSize;
		Long partSize;
		Long subSize;
		Long offset;
		String serverLink;
		PartThread partThread;
		int threadCount;
		int delayTime;
		
		try{
			if(cancelflag == true){
				return null;
			}
			
			fileSize = file.length();
			getList(fileSize);
			if(UploadApplet.loginFlag == true){
				serverHead = UploadApplet.userLink;
			}else{
				serverHead = serverList.get(new Random().nextInt(serverList.size()));
			}
				
			partSize = fileSize / serverList.size();
			index = 0;
			offset = 0L;
			
			threadCount = 0;
			if((partSize / PART_SIZE) * serverList.size() > 2){
				delayTime = 2000;
			}else{
				delayTime = 200;
			}
			
			progQueue.add(new Pair<Long,Long>(new Date().getTime(),netSize.get()));
			while(offset < fileSize){
				if((fileSize - offset) < partSize){
					partSize = fileSize - offset;
				}
				
				serverLink = serverList.get(index);
				subSize = 0L;
				while(subSize < partSize){
					if((partSize - subSize) < PART_SIZE){
						partThread = new PartThread(serverLink,offset + subSize,(partSize - subSize),this);	
						threadPool.execute(partThread);
						subSize = partSize;
					}else{
						partThread = new PartThread(serverLink,offset + subSize,PART_SIZE,this);
						threadPool.execute(partThread);
						subSize += PART_SIZE;
					}
					
					threadCount++;
					if(threadCount <= 2){
						Thread.sleep(delayTime);
					}
					
					updateLoop();
				}
				
				index = (index + 1) % serverList.size();
				offset += partSize;
			}
			
			tweakFlag = true;
			while(updateLoop() == true){
				Thread.sleep(1000);
			}
			
			postUpload(serverHead);
			fileChannel.close();
			RAF.close();
			UploadApplet.updateState(itemid,"link");
			UploadApplet.showLink(itemid,fileid,fileSize,serverHead + "/down/" + fileid + "/" + filename);
			
			System.out.println("Done");
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
}

class PartThread extends SwingWorker<Void,Void>{
	private String serverlink;
	private Long offset;
	private Long partsize;
	private UploadThread uploadThread;
	
	
	public PartThread(String serverlink,Long offset,Long partsize,UploadThread uploadthread){
		this.serverlink = serverlink;
		this.offset = offset;
		this.partsize = partsize;
		this.uploadThread = uploadthread;
	}
	
	@Override
	protected Void doInBackground() throws Exception{
		int retry;
		
		HttpURLConnection conn;
		WritableByteChannel wbc;
		FileChannel fc;
		BufferedReader reader;
		
		int deltaRealSize;
		StringBuilder header;
		byte[] byteHeader;
		Long nowSize;
		Long nowOffset;
		int subLimit;
		int readSize;
		int writeSize;
		ByteBuffer fbuf;
		int resCode;
		
		deltaRealSize = 0;
		retry = 8;
		while(retry > 0 && this.isCancelled() == false){
			uploadThread.realSize.addAndGet(-deltaRealSize);
			deltaRealSize = 0;
			try{		
				conn = (HttpURLConnection)new URL(serverlink + "/upload").openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setConnectTimeout(60000);
				conn.setReadTimeout(60000);
				conn.setRequestProperty("Cache-Control","no-cache,max-age=0");
				conn.setRequestProperty("Pragma","no-cache");
				conn.setRequestProperty("Content-Type","application/octet-stream");
				
				header = new StringBuilder();
				header.append(uploadThread.fileid);
				header.append("\r\n");
				header.append(uploadThread.fileseckey);
				header.append("\r\n");
				header.append("upload\r\n");
				header.append(String.valueOf(partsize));
				header.append("\r\n");
				byteHeader = header.toString().getBytes("UTF-8");
				
				conn.setFixedLengthStreamingMode((int)(byteHeader.length + partsize));
				
				wbc = Channels.newChannel(conn.getOutputStream());
				wbc.write(ByteBuffer.wrap(byteHeader));
			
				fc = uploadThread.fileChannel;
				fbuf = ByteBuffer.allocateDirect(524288);
				nowSize = 0L;
				nowOffset = offset;
				while(nowSize < partsize){
					fbuf.clear();
					if(partsize - nowSize < 524288){
						fbuf.limit((int)(partsize - nowSize));
					}
					readSize = fc.read(fbuf,nowOffset);
					fbuf.flip();
					
					subLimit = 0;
					while(subLimit < readSize){
						subLimit = Math.min(subLimit + 65536,readSize);
						fbuf.limit(subLimit);
						writeSize = 0;
						while(fbuf.hasRemaining() == true){
							writeSize += wbc.write(fbuf);
						}
						uploadThread.netSize.addAndGet(writeSize);
						deltaRealSize += writeSize;
					}
					
					nowSize += readSize;
					nowOffset += readSize;
				}
				
				resCode = conn.getResponseCode();
				if(resCode == 200){
					reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					uploadThread.blobkeyMap.put(offset,reader.readLine());
					reader.close();
				}
				wbc.close();
				
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
				retry--;
			}
		}
		
		return null;
	}
}