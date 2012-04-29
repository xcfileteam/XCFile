var e_div_user;
var e_div_fileinfo;

var e_div_user_taglist;
var e_div_user_tagitem_ori;
var e_div_user_tagselect;
var e_div_taginfo;
var e_input_taginfo_tagname;
var e_div_taginfo_setbutton;
var e_div_taginfo_delbutton;

var e_div_user_filelist;
var e_div_user_fileitem_ori;
var e_table_fileinfo_filename;
var e_input_fileinfo_filename;
var e_div_fileinfo_taglist;
var e_div_fileinfo_tagitem_ori;
var e_div_fileinfo_setbutton;

var fileMap;
var tagMap;

function userGetList(){
	var e_div_user;
	
	e_div_user = $('div_user');
	
	new Ajax.Request(userlink + '/info',{
		method:'post',
		parameters:{'type':'usergetlist','usersecid':usersecid,'userseckey':userseckey},
		onSuccess:function(transport){
			var index;
			var subIndex;
			
			var fileObj;
			var tagObj;
			
			var partData;
			var subPartData;
			
			var fileid;
			var filename;
			var filesize;
			var timestamp;
			var filelink;
			var e_a;
			var e_div_user_fileitem;
			
			var tagname;
			var fileidlist;
			var partFileIdList;
			var e_div_user_tagitem;
			
			fileMap = new Object(); 
			tagMap = new Object();
			
			partData = transport.responseText.split('<-->\n');
			
			subPartData = partData[0].split('\n');
			for(index = 0;index < (subPartData.length - 1);index += 5){	
				fileid = subPartData[index];
				filename = subPartData[index + 1];
				filesize = parseInt(subPartData[index + 2],10);
				timestamp = parseInt(subPartData[index + 3],10);
				filelink = subPartData[index + 4];
				
				e_div_user_fileitem = userCreateFileItem(fileid,filename,filesize,timestamp,filelink);
				fileObj = e_div_user_fileitem.fileobj;
				fileMap[fileObj.fileid] = fileObj;
			}
			
			subPartData = partData[1].split('\n');
			for(index = 0;index < (subPartData.length - 1);index += 2){	
				tagname = subPartData[index];
				fileidlist = subPartData[index + 1];
				
				e_div_user_tagitem = userCreateTagItem(tagname,fileidlist);
				tagObj = e_div_user_tagitem.tagobj;
				tagMap[tagObj.tagname] = tagObj;
				
				userFileInfoCreateTagItem(tagObj.tagname);
				
				if(tagObj.fileidlist != ''){
					partFileIdList = tagObj.fileidlist.split('|');
					for(subIndex = 0;subIndex < partFileIdList.length;subIndex++){
						fileMap[partFileIdList[subIndex]].tagnamemap[tagObj.tagname] = tagObj.tagname;
						
						e_div_user_fileitem = fileMap[partFileIdList[subIndex]].e_div_user_fileitem;
						if(e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
							e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
						}
						e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagObj.tagname);
					}
				}
			}
		}
	});
}

function userCreateTagItem(tagname,fileidlist){
	var e_div_user_tagitem;
	var tagObj;
	
	var divs;
	
	e_div_user_tagitem = e_div_user_tagitem_ori.cloneNode(true);
	e_div_user_tagitem.id = 'div_user_tagitem_' + tagname;
	
	tagObj = new Object();
	tagObj.tagname = tagname;
	tagObj.fileidlist = fileidlist;
	tagObj.e_div_user_tagitem = e_div_user_tagitem;
	
	e_div_user_tagitem.tagobj = tagObj;
	
	divs = e_div_user_tagitem.getElementsByTagName('div');
	e_div_user_tagitem.e_div_point = divs[0];
	divs[1].appendChild(document.createTextNode(decodeURIComponent(tagname)));
	e_div_user_tagitem.e_div_tagname = divs[1];
	divs[2].e_div_user_tagitem = e_div_user_tagitem;
	e_div_user_tagitem.e_div_operbutton = divs[2];
	divs[2].observe('mouseover',function(){this.className='button_b_hi';});
	divs[2].observe('mouseout',function(){this.className='button_b';});
	
	e_div_user_tagitem.style.display = '';
	e_div_user_taglist.appendChild(e_div_user_tagitem);
	
	return e_div_user_tagitem;
}
function userAddTag(){
	var tagname;
	var e_div_user_tagitem;
	var tagObj;
	
	tagname = prompt('新增標籤名稱');
	if(tagname != null && tagname != ''){
		tagname = encodeURIComponent(tagname);
		if(tagname != 'All' && tagMap[tagname] == undefined){
			new Ajax.Request(userlink + '/info',{
				method:'post',
				parameters:{'type':'useraddtag','usersecid':usersecid,'userseckey':userseckey,'tagname':decodeURIComponent(tagname)}
			});	
			
			e_div_user_tagitem = userCreateTagItem(tagname,'');
			tagObj = e_div_user_tagitem.tagobj;
			tagMap[tagObj.tagname] = tagObj;
			
			userFileInfoCreateTagItem(tagObj.tagname);
		}else{
			alert('標籤名稱已經存在');
		}
	}
}
function userDelTag(e_div_user_tagitem){
	var tagObj;
	var fileid;
	var fileObj;
	var tagname;
	
	tagObj = e_div_user_tagitem.tagobj;
	
	new Ajax.Request(userlink + '/info',{
		method:'post',
		parameters:{'type':'userdeltag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname}
	});
	
	delete tagMap[tagObj.tagname];
	e_div_user_taglist.removeChild(tagObj.e_div_user_tagitem);
	e_div_fileinfo_taglist.removeChild($('div_fileinfo_tagitem_' + tagObj.tagname));
	
	for(fileid in fileMap){
		fileObj = fileMap[fileid];
		
		if(fileObj.tagnamemap[tagObj.tagname] != undefined){
			delete fileObj.tagnamemap[tagObj.tagname];
			
			fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue = ''
			for(tagname in fileObj.tagnamemap){
				if(fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
					fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
				}
				fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagname);
			}
		}
	}
	
	if(e_div_user_tagselect.tagobj.tagname == tagObj.tagname){
		e_div_user_tagselect = $('div_user_tagitem_All');
		userSwitchFileList();
	}
}
function userSetTag(e_div_user_tagitem){
	var tagObj;
	var e_div_fileinfo_tagitem;
	var fileid;
	var fileObj;
	var newtagname;
	var tagname;
	
	tagObj = e_div_user_tagitem.tagobj;
	
	if(e_input_taginfo_tagname.value != decodeURIComponent(tagObj.tagname)){
		new Ajax.Request(userlink + '/info',{
			method:'post',
			parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'newtagname':e_input_taginfo_tagname.value,'fileidlist':tagObj.fileidlist}
		});
		
		newtagname = encodeURIComponent(e_input_taginfo_tagname.value);
		
		delete tagMap[tagObj.tagname];
		tagMap[newtagname] = tagObj;
		tagObj.e_div_user_tagitem.e_div_tagname.firstChild.nodeValue = decodeURIComponent(newtagname);
		e_div_fileinfo_tagitem = $('div_fileinfo_tagitem_' + tagObj.tagname);
		e_div_fileinfo_tagitem.id = 'div_fileinfo_tagitem_' + newtagname;
		e_div_fileinfo_tagitem.tagname = newtagname;
		e_div_fileinfo_tagitem.e_div_tagname.firstChild.nodeValue = decodeURIComponent(newtagname);
		
		for(fileid in fileMap){
			fileObj = fileMap[fileid];
			
			if(fileObj.tagnamemap[tagObj.tagname] != undefined){
				delete fileObj.tagnamemap[tagObj.tagname];
				fileObj.tagnamemap[newtagname] = newtagname;
				
				fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue = ''
				for(tagname in fileObj.tagnamemap){
					if(fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
						fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
					}
					fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagname);
				}
			}
		}
		
		tagObj.tagname = newtagname;
	}
}

function userCreateFileItem(fileid,filename,filesize,timestamp,link){
	var e_div_user_fileitem;
	var fileObj;
	
	var date;
	var year;
	var month;
	var day;
	var hour;
	var minute;
	var num;
	
	var divs;
	var e_a_link;
	
	e_div_user_fileitem = e_div_user_fileitem_ori.cloneNode(true);
	e_div_user_fileitem.id = 'div_user_fileitem_' + fileid;
	
	fileObj = new Object();
	fileObj.fileid = fileid;
	fileObj.filename = filename;
	fileObj.filesize = filesize;
	fileObj.timestamp = timestamp;
	fileObj.filelink = link;
	fileObj.tagnamemap = new Object();
	fileObj.e_div_user_fileitem = e_div_user_fileitem;
	
	e_div_user_fileitem.fileobj = fileObj;
	
	date = new Date(timestamp);
	year = new String(date.getFullYear()).substring(2,4);
	month = date.getMonth();
	if(month < 10){
		month = '0' + month;
	}
	day = date.getDate();
	if(day < 10){
		day = '0' + day;
	}
	hour = date.getHours();
	minute = date.getMinutes();
	if(minute < 10){
		minute = '0' + minute;
	}
	
	if(filesize >= 1073741824){
		num = new Number(filesize / 1073741824.0);
		filesize = num.toFixed(1) + 'GB';
	}else if(filesize >= 1048576){
		num = new Number(filesize / 1048576);
		filesize = num.toFixed(1) + 'MB';
	}else if(filesize >= 1024){
		num = new Number(filesize / 1024);
		filesize = num.toFixed(1) + 'KB';
	}else{
		filesize = filesize + 'B';
	}
	
	divs = e_div_user_fileitem.getElementsByTagName('div');
	divs[0].appendChild(document.createTextNode(year + '/' + month + '/' + day));
	divs[0].appendChild(new Element('br'));
	divs[0].appendChild(document.createTextNode(hour + ':' + minute));
	divs[2].appendChild(document.createTextNode(filesize));
	e_div_user_fileitem.e_div_oper = divs[3];
	divs[4].e_div_user_fileitem = e_div_user_fileitem;
	divs[4].observe('mouseover',function(){this.className='button_b_hi';});
	divs[4].observe('mouseout',function(){this.className='button_b';});
	divs[5].e_div_user_fileitem = e_div_user_fileitem;
	divs[5].observe('mouseover',function(){this.className='button_b_hi';});
	divs[5].observe('mouseout',function(){this.className='button_b';});
	
	e_div_user_fileitem.e_input_check = e_div_user_fileitem.getElementsByTagName('input')[0];
	
	e_a_link = e_div_user_fileitem.getElementsByTagName('a')[0];
	e_a_link.href = link;
	e_a_link.appendChild(document.createTextNode(decodeURIComponent(filename)));
	e_div_user_fileitem.e_a_link = e_a_link;
	
	e_div_user_fileitem.e_span_tagstring = e_div_user_fileitem.getElementsByTagName('span')[0];
	e_div_user_fileitem.e_span_tagstring.appendChild(document.createTextNode(''));
	
	if(e_div_user_tagselect.tagobj.tagname == 'All'){
		e_div_user_fileitem.style.display = '';
	}
	e_div_user_filelist.insertBefore(e_div_user_fileitem,e_div_user_filelist.firstChild);
	
	return e_div_user_fileitem;
}
function userDelFileItem(e_div_user_fileitem){
	var index;
	var fileid;
	var fileObj;
	var tagObj;
	var tagname;
	var partFileIdList;
	var delList;
	
	if(e_div_user_fileitem != null){
		new Ajax.Request(userlink + '/info',{
			method:'post',
			parameters:{'type':'userdelfile','usersecid':usersecid,'userseckey':userseckey,'fileid':e_div_user_fileitem.fileobj.fileid}
		});
		
		fileObj = e_div_user_fileitem.fileobj;
		delete fileMap[fileObj.fileid];
		for(tagname in fileObj.tagnamemap){
			tagObj = tagMap[tagname];
			
			partFlieIdList = tagObj.fileidlist.split('|');
			tagObj.fileidlist = '';
			for(index = 0;index < partFlieIdList.length;index++){
				if(partFlieIdList[index] != fileObj.fileid){
					if(tagObj.fileidlist != ''){
						tagObj.fileidlist += '|';
					}
					tagObj.fileidlist += partFlieIdList[index];
				}
			}
			
			new Ajax.Request(userlink + '/info',{
				method:'post',
				parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
			});
		}
		
		e_div_user_filelist.removeChild(e_div_user_fileitem);
	}else{
		delList = new Array();
		for(fileid in fileMap){
			fileObj = fileMap[fileid];
			e_div_user_fileitem = fileObj.e_div_user_fileitem;
			
			if(e_div_user_fileitem.e_input_check.checked == true){
				new Ajax.Request(userlink + '/info',{
					method:'post',
					parameters:{'type':'userdelfile','usersecid':usersecid,'userseckey':userseckey,'fileid':fileObj.fileid}
				});
				delList.push(e_div_user_fileitem);	
			
				delete fileMap[fileObj.fileid];
				for(tagname in fileObj.tagnamemap){
					tagObj = tagMap[tagname];
					
					partFlieIdList = tagObj.fileidlist.split('|');
					tagObj.fileidlist = '';
					for(index = 0;index < partFlieIdList.length;index++){
						if(partFlieIdList[index] != fileObj.fileid){
							if(tagObj.fileidlist != ''){
								tagObj.fileidlist += '|';
							}
							tagObj.fileidlist += partFlieIdList[index];
						}
					}
					
					new Ajax.Request(userlink + '/info',{
						method:'post',
						parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
					});
				}
			}
		}
		
		for(index = 0;index < delList.length;index++){
			e_div_user_filelist.removeChild(delList[index]);	
		}	
	}
}
function userSwitchFileList(){
	var index;
	var fileid;
	var fileObj;
	var tagObj;
	var partFileIdList;
	
	for(fileid in fileMap){
		fileObj = fileMap[fileid];
		fileObj.e_div_user_fileitem.e_input_check.checked = false;
		fileObj.e_div_user_fileitem.style.display = 'none';
	}
	
	tagObj = e_div_user_tagselect.tagobj;
	if(tagObj.tagname == 'All'){
		for(fileid in fileMap){
			fileObj = fileMap[fileid];
			fileObj.e_div_user_fileitem.style.display = '';
		}
	}else{
		if(tagObj.fileidlist != ''){
			partFileIdList = tagObj.fileidlist.split('|');
			for(subIndex = 0;subIndex < partFileIdList.length;subIndex++){
				fileObj = fileMap[partFileIdList[subIndex]];
				fileObj.e_div_user_fileitem.style.display = '';
			}
		}
	}
}

function userFileInfoCreateTagItem(tagname){
	var e_div_fileinfo_tagitem;
	
	var divs;

	e_div_fileinfo_tagitem = e_div_fileinfo_tagitem_ori.cloneNode(true);
	e_div_fileinfo_tagitem.id = 'div_fileinfo_tagitem_' + tagname;
	e_div_fileinfo_tagitem.tagname = tagname;
	
	divs = e_div_fileinfo_tagitem.getElementsByTagName('div');
	e_div_fileinfo_tagitem.e_div_state = divs[0];
	divs[1].appendChild(document.createTextNode(decodeURIComponent(tagname)));
	e_div_fileinfo_tagitem.e_div_tagname = divs[1];
	
	e_div_fileinfo_tagitem.e_input_check = e_div_fileinfo_tagitem.getElementsByTagName('input')[0];
	
	e_div_fileinfo_tagitem.style.display = '';
	e_div_fileinfo_taglist.appendChild(e_div_fileinfo_tagitem);
	
	return e_div_fileinfo_tagitem;
}
function userFileInfoSet(e_div_user_fileitem){
	var index;
	var subIndex;
	
	var fileid;
	var fileObjList;
	var fileObj;
	var tagObj;
	var tagnamemap;
	var tagname;
	var e_div_fileinfo_tagitem;
	var chgFlag;
	var igonreFlag;
	var partFileIdList;
	var chgTagNameMap;
	
	if(e_div_user_fileitem != null){
		fileObj = e_div_user_fileitem.fileobj;
		
		if(e_input_fileinfo_filename.value != decodeURIComponent(fileObj.filename)){
			fileObj.filename = encodeURIComponent(e_input_fileinfo_filename.value);
			
			new Ajax.Request(userlink + '/info',{
				method:'post',
				parameters:{'type':'usersetfile','usersecid':usersecid,'userseckey':userseckey,'fileid':fileObj.fileid,'filename':e_input_fileinfo_filename.value}
			});
			
			fileObj.e_div_user_fileitem.e_a_link.firstChild.nodeValue = decodeURIComponent(fileObj.filename);
		}
		
		e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue = '';
		tagnamemap = new Object();
		for(index = 0;index < e_div_fileinfo_taglist.childNodes.length;index++){
			e_div_fileinfo_tagitem = e_div_fileinfo_taglist.childNodes[index];
			if(e_div_fileinfo_tagitem.tagname != undefined){
				if(e_div_fileinfo_tagitem.e_input_check.checked == true){
					tagname = e_div_fileinfo_tagitem.tagname;
					tagnamemap[tagname] = tagname;
					
					if(fileObj.tagnamemap[tagname] == undefined){
						tagObj = tagMap[tagname];
						if(tagObj.fileidlist != ''){
							tagObj.fileidlist += '|';
						}
						tagObj.fileidlist += fileObj.fileid;
						
						new Ajax.Request(userlink + '/info',{
							method:'post',
							parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
						});
					}
					
					if(e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
						e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
					}
					e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagname);
				}
			}
		}
		
		for(tagname in fileObj.tagnamemap){
			if(tagnamemap[tagname] == undefined){
				tagObj = tagMap[tagname];
				
				partFlieIdList = tagObj.fileidlist.split('|');
				tagObj.fileidlist = '';
				for(index = 0;index < partFlieIdList.length;index++){
					if(partFlieIdList[index] != fileObj.fileid){
						if(tagObj.fileidlist != ''){
							tagObj.fileidlist += '|';
						}
						tagObj.fileidlist += partFlieIdList[index];
					}
				}
				
				new Ajax.Request(userlink + '/info',{
					method:'post',
					parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
				});
			}
		}
		
		fileObj.tagnamemap = tagnamemap;
	}else{
		fileObjList = new Array();
		index = 0;
		for(fileid in fileMap){
			fileObj = fileMap[fileid];
			
			if(fileObj.e_div_user_fileitem.e_input_check.checked == true){
				fileObjList[index] = fileObj;
				fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue = ''
				index++;
			}
		}
		
		tagnamemap = new Object();
		for(index = 0;index < e_div_fileinfo_taglist.childNodes.length;index++){
			e_div_fileinfo_tagitem = e_div_fileinfo_taglist.childNodes[index];
			if(e_div_fileinfo_tagitem.tagname != undefined){
				if(e_div_fileinfo_tagitem.e_input_check.checked == true){
					tagname = e_div_fileinfo_tagitem.tagname;
					tagnamemap[tagname] = tagname;
					tagObj = tagMap[tagname];
								
					if(e_div_fileinfo_tagitem.e_div_state.style.backgroundColor == ''){
						igonreFlag = false;
					}else{
						igonreFlag = true;
					}
					
					chgFlag = false;
					for(subIndex = 0;subIndex < fileObjList.length;subIndex++){
						fileObj = fileObjList[subIndex];
						
						if(fileObj.tagnamemap[tagname] == undefined){
							if(igonreFlag == false){
								if(tagObj.fileidlist != ''){
									tagObj.fileidlist += '|';
								}
								tagObj.fileidlist += fileObj.fileid;
								fileObj.tagnamemap[tagname] = tagObj;
								
								if(fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
									fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
								}
								fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagname);
								
								chgFlag = true;
							}
						}else{
							if(fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue != ''){
								fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += ',';
							}
							fileObj.e_div_user_fileitem.e_span_tagstring.firstChild.nodeValue += decodeURIComponent(tagname);
						}
					}
					
					if(chgFlag == true){
						new Ajax.Request(userlink + '/info',{
							method:'post',
							parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
						});
					}
				}
			}
		}
		
		chgTagNameMap = new Object();
		for(index = 0;index < fileObjList.length;index++){
			fileObj = fileObjList[index];
			
			for(tagname in fileObj.tagnamemap){
				if(tagnamemap[tagname] == undefined){
					tagObj = tagMap[tagname];
					chgTagNameMap[tagname] = tagname;
					
					partFlieIdList = tagObj.fileidlist.split('|');
					tagObj.fileidlist = '';
					for(subIndex = 0;subIndex < partFlieIdList.length;subIndex++){
						if(partFlieIdList[subIndex] != fileObj.fileid){
							if(tagObj.fileidlist != ''){
								tagObj.fileidlist += '|';
							}
							tagObj.fileidlist += partFlieIdList[subIndex];
						}
					}
				}
			}
		}
		
		for(tagname in chgTagNameMap){
			tagObj = tagMap[tagname];
			
			new Ajax.Request(userlink + '/info',{
				method:'post',
				parameters:{'type':'usersettag','usersecid':usersecid,'userseckey':userseckey,'tagname':tagObj.tagname,'fileidlist':tagObj.fileidlist}
			});
		}
	}
	
	userSwitchFileList();
}