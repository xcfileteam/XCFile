var e_div_upload;
var e_div_upload_close;
var e_div_uploadlist;
var e_div_uploaditem_ori;
var uploadCount;

function uploadCreateItem(itemid,filename){
	var e_div_uploaditem;
	var inputs;
	var trs;
	var divs;
	
	e_div_uploaditem = e_div_uploaditem_ori.cloneNode(true);
	e_div_uploaditem.id = 'div_uploaditem_' + itemid;
	e_div_uploaditem.itemid = itemid;
	e_div_uploaditem.filename = filename;
	
	inputs = e_div_uploaditem.getElementsByTagName('input');
	inputs[0].value = filename;
	e_div_uploaditem.e_input_link = inputs[1];
	
	trs = e_div_uploaditem.getElementsByTagName('tr');
	e_div_uploaditem.e_tr_prog = trs[1];
	e_div_uploaditem.e_tr_link = trs[2];
	
	divs = e_div_uploaditem.getElementsByTagName('div');
	divs[0].e_div_uploaditem = e_div_uploaditem;
	divs[0].observe('mouseover',function(){this.className='button_b_hi';});
	divs[0].observe('mouseout',function(){this.className='button_b';});
	e_div_uploaditem.e_div_remove = divs[0];
	divs[1].e_div_uploaditem = e_div_uploaditem;
	divs[1].observe('mouseover',function(){this.className='button_b_hi';});
	divs[1].observe('mouseout',function(){this.className='button_b';});
	e_div_uploaditem.e_div_cancel = divs[1];
	e_div_uploaditem.e_div_progbox = divs[2];	
	e_div_uploaditem.e_div_progvalue = divs[3];	
	e_div_uploaditem.e_div_progtext = divs[4];
	e_div_uploaditem.e_div_speed = divs[5];	
	divs[6].e_div_uploaditem = e_div_uploaditem;
	divs[6].observe('mouseover',function(){this.className='button_b_hi';});
	divs[6].observe('mouseout',function(){this.className='button_b';});
	
	e_div_uploaditem.style.display = '';
	e_div_uploadlist.appendChild(e_div_uploaditem);
	e_div_uploadlist.style.display = '';
	
	uploadCount++;
	
	resize();
}

function uploadUpdateState(itemid,state){
	var e_div_uploaditem;
	
	e_div_uploaditem = $('div_uploaditem_' + itemid);
	
	if(state == 'upload'){
		e_div_uploaditem.e_tr_prog.style.display = '';
		e_div_uploaditem.e_div_remove.style.display = 'none';
		e_div_uploaditem.e_div_cancel.style.display = '';
	}else if(state == 'link'){
		e_div_uploaditem.e_tr_link.style.display = '';
		e_div_uploaditem.e_tr_prog.style.display = 'none';
		e_div_uploaditem.e_div_cancel.style.display = 'none';
		
		e_div_uploaditem.e_div_progbox.style.display = 'none';
		e_div_uploaditem.e_div_progvalue.style.display = 'none';
		e_div_uploaditem.e_div_progtext.style.display = 'none';
	}
}
function uploadUpdateProg(itemid,progvalue,speed){
	var e_div_uploaditem;
	var e_div_progvalue;
	var e_div_progtext;
	var e_div_speed;
	var num;
	
	e_div_uploaditem = $('div_uploaditem_' + itemid);
	e_div_progvalue = e_div_uploaditem.e_div_progvalue;
	e_div_progtext = e_div_uploaditem.e_div_progtext;
	e_div_speed = e_div_uploaditem.e_div_speed;
	
	if(progvalue >= 100.0){
		e_div_progvalue.style.width = '100%';
		e_div_progtext.childNodes[0].nodeValue = '處理中';
		e_div_speed.childNodes[0].nodeValue = '';
	}else{
		num = new Number(progvalue);
		e_div_progvalue.style.width = num.toFixed(0) + '%';
		e_div_progtext.childNodes[0].nodeValue = num.toFixed(1) + '%';
		
		if(speed >= 1073741824){
			num = new Number(speed / 1073741824.0);
			speed = num.toFixed(1) + 'GB/s';
		}else if(speed >= 1048576){
			num = new Number(speed / 1048576);
			speed = num.toFixed(1) + 'MB/s';
		}else if(speed >= 1024){
			num = new Number(speed / 1024);
			speed = num.toFixed(1) + 'KB/s';
		}else{
			speed = speed + 'B/s';
		}
		e_div_speed.childNodes[0].nodeValue = speed;
	}
}
function uploadShowLink(itemid,fileid,filesize,link){
	var e_div_uploaditem;
	var fileObj;

	e_div_uploaditem = $('div_uploaditem_' + itemid);
	e_div_uploaditem.link = link;
	e_div_uploaditem.e_input_link.value = link;
	
	if(loginflag == true){
		e_div_user_fileitem = userCreateFileItem(fileid,e_div_uploaditem.filename,filesize,new Date().getTime(),e_div_uploaditem.link);
		fileObj = e_div_user_fileitem.fileobj;
		fileMap[fileObj.fileid] = fileObj;
	}
}