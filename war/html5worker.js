var fs;
var fileEntry = null;
var cancelFlag;

onmessage = function(e){
	var index;
	var writer;
	var threadlist;
	var thread;
	
	if(e.data.type == 'init'){
		cancelFlag = false;
		
		if(fileEntry != null){
			fileEntry.remove();
		}
		
		fs = webkitRequestFileSystemSync(PERSISTENT,e.data.filesize);
		fileEntry = fs.root.getFile(new Date().getTime(),{create:true,exclusive:false});
		writer = fileEntry.createWriter();
		writer.truncate(e.data.filesize);
		
		postMessage({'type':'init','url':fileEntry.toURL()});
	}else if(e.data.type == 'create'){
		threadlist = e.data.threadlist;
		for(index = 0;index < threadlist.length;index++){		
			thread = threadlist[index];
			partThread(fileEntry.createWriter(),thread.serverlink,thread.blobkey,thread.partoffset,thread.partsize);
		}
	}else if(e.data.type == 'cancel'){
		cancelFlag = true;
	}else if(e.data.type == 'close'){
		fileEntry.remove();
	}
}

function partThread(writer,serverlink,blobkey,partoffset,partsize){
	var req;
	var loaded;
	
	req = new XMLHttpRequest();
	req.open('POST',serverlink + '/download',true);
	req.setRequestHeader('Content-type','application/x-www-form-urlencoded');
	req.responseType = 'arraybuffer';
	
	req.onreadystatechange = function(e){
		var blobBuilder;

		if(cancelFlag == true){
			return;
		}
		
		if(this.readyState == 4){
			if(this.status == 200){
				blobBuilder = new WebKitBlobBuilder();
				blobBuilder.append(req.response);
				
				writer.seek(partoffset);
				writer.write(blobBuilder.getBlob('application/octet-stream'));
				
				postMessage({'type':'done','value':partsize});
			}else if(this.status == 500){
				partThread(writer,serverlink,blobkey,partoffset,partsize);
			}
		}
	}
	req.onerror = function(e){
		if(cancelFlag == true){
			return;
		}
		
		partThread(writer,serverlink,blobkey,partoffset,partsize);
	}
	
	loaded = 0;
	req.onprogress = function(e){
		if(cancelFlag == true){
			this.abort();
			return;
		}
		
		postMessage({'type':'prog','value':(e.loaded - loaded)});
		loaded = e.loaded;
	}
	
	req.send('blobkey=' + encodeURI(blobkey));
}