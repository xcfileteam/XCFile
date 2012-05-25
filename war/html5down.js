var e_a_html5test;

var threadPool;
var threadHead;
var coreSize;
var runCount;
var netSize;
var realSize;
var progQueue;
var work = null;

function html5Init(){
	var index;
	var PART_SIZE = 8388608;
	
	var partSize;
	var offset;
	var blobkeyIndex;
	var serverlink;
	var subSize;
	var thread;
	
	e_a_html5test = $('a_html5test');
	e_a_html5test.download = filename;
	
	threadPool = new Array();
	threadHead = 0;
	coreSize = 2;
	runCount = 0;
	netSize = 0;
	realSize = 0;
	progQueue = new Array();
	
	partSize = Math.floor(filesize / serverlist.length);
	index = 0;
	offset = 0;
	blobkeyIndex = 0;
	while(offset < filesize){
		if((filesize - offset) < partSize){
			partSize = filesize - offset;
		}
		
		serverlink = serverlist[index];
		subSize = 0;
		while(subSize < partSize){
			thread = new Object();
			thread.serverlink = serverlink;
			thread.blobkey = blobkeylist[blobkeyIndex];
			thread.partoffset = offset + subSize;
			
			if((partSize - subSize) < PART_SIZE){
				thread.partsize = partSize - subSize;
				subSize = partSize;
			}else{
				thread.partsize = PART_SIZE;
				subSize += PART_SIZE;
			}
			
			threadPool.push(thread);
			
			blobkeyIndex++;
		}
		
		index = (index + 1) % serverlist.length;
		offset += partSize;
	}
	
	if(work == null){
		work = new Worker('../../html5worker.js');
	}
	work.onmessage = workCallback;
	work.postMessage({'type':'init','filesize':filesize});
}
function html5Close(){
	work.postMessage({'type':'close'});
}
function speedLoop(){
	var evt;
	var nowNetSize;
	var nowTime;
	var nowSpeed;

	if(state == 'cancel'){
		work.postMessage({'type':'cancel'});
		state = 'init';
		updateState('init');
		return;
	}
	if(state == 'done'){
		updateState('done');
		
		evt = document.createEvent('MouseEvents');
		evt.initMouseEvent('click',true,true,window,0,0,0,0,0,false,false,false,false,0,null);
		e_a_html5test.dispatchEvent(evt);
		
		return;
	}
	
	nowNetSize = netSize;
	nowTime = new Date().getTime();
	while(progQueue.length > 1){
		if((nowTime - progQueue[0].time) > 10000){
			progQueue.shift();
		}else{
			break;
		}
	}
	progQueue.push({'time':nowTime,'netsize':nowNetSize});
	
	if((nowTime - progQueue[0].time) > 0){
		nowSpeed = (nowNetSize - progQueue[0].netsize) / (nowTime - progQueue[0].time) * 1000;
	}else{
		nowSpeed = 0;
	}
	
	if(nowSpeed > (coreSize * 102400)){
		coreSize++;
		runThread();
	}else if(nowSpeed < ((coreSize - 1) * 102400)){
		coreSize--;
	}
	
	updateProg(((nowNetSize / filesize) * 100),nowSpeed);
	
	setTimeout(speedLoop,500);
}
function runThread(){
	var threadlist;
	
	threadlist = new Array();
	for(;(runCount < coreSize) && (threadHead < threadPool.length);runCount++,threadHead++){
		threadlist.push(threadPool[threadHead]);
	}
	
	if(threadlist.length > 0){
		work.postMessage({'type':'create','threadlist':threadlist});
	}
}
function workCallback(e){
	if(e.data.type == 'init'){
		e_a_html5test.href = e.data.url;
		runThread();
		progQueue.push({'time':new Date().getTime(),'netsize':0});
		speedLoop();
	}else if(e.data.type == 'done'){
		runCount--;
		runThread();
		
		if(runCount == 0){
			state = 'done';
		}
	}else if(e.data.type == 'prog'){
		netSize += event.data.value;
	}
}