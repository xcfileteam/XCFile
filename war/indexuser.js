var e_div_user;
var e_div_userlist;
var e_div_useritem_ori;
var userlistCount;

function userGetFilelist(){
	var e_div_user;
	
	e_div_user = $('div_user');
	
	new Ajax.Request(userlink + '/info',{
		method:'post',
		parameters:{'type':'usergetfilelist','usersecid':usersecid,'userseckey':userseckey},
		onSuccess:function(transport){
			var index;
			
			var partData;
			var fileid;
			var filename;
			var filesize;
			var timestamp;
			var filelink;
			var e_a;
			
			partData = transport.responseText.split('\n');
			for(index = 0;index < (partData.length - 1);index += 5){
				fileid = partData[index];
				filename = decodeURIComponent(partData[index + 1]);
				filesize = decodeURIComponent(partData[index + 2]);
				timestamp = parseInt(partData[index + 3],10);
				filelink = partData[index + 4];
				
				userCreateItem(fileid,filename,filesize,timestamp,filelink);
			}
		}
	});
}

function userCreateItem(fileid,filename,filesize,timestamp,link){
	var e_div_useritem;
	
	var date;
	var year;
	var month;
	var day;
	var hour;
	var minute;
	var num;
	
	var divs;
	var e_a_link;
	
	e_div_useritem = e_div_useritem_ori.cloneNode(true);
	e_div_useritem.id = 'div_useritem_' + userlistCount;
	e_div_useritem.fileid = fileid;
	
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
	
	divs = e_div_useritem.getElementsByTagName('div');
	divs[0].appendChild(document.createTextNode(year + '/' + month + '/' + day));
	divs[0].appendChild(new Element('br'));
	divs[0].appendChild(document.createTextNode(hour + ':' + minute));
	divs[2].appendChild(document.createTextNode(filesize));
	divs[3].e_div_useritem = e_div_useritem;
	divs[3].observe('mouseover',function(){this.className='button_b_hi';});
	divs[3].observe('mouseout',function(){this.className='button_b';});
	e_div_useritem.e_div_del = divs[3];
	
	e_a_link = e_div_useritem.getElementsByTagName('a')[0];
	e_a_link.href = link;
	e_a_link.appendChild(document.createTextNode(filename));
	
	e_div_useritem.style.display = '';
	e_div_userlist.insertBefore(e_div_useritem,e_div_userlist.firstChild);
	
	userlistCount++;
}

function userDelItem(e_div_useritem){
	var index;
	var delList;
	
	if(e_div_useritem != null){
		new Ajax.Request(userlink + '/info',{
			method:'post',
			parameters:{'type':'userdelfile','usersecid':usersecid,'userseckey':userseckey,'fileid':e_div_useritem.fileid}
		});
		e_div_userlist.removeChild(e_div_useritem);
	}else{
		delList = new Array();
		for(index = 0;index < e_div_userlist.childNodes.length;index++){
			e_div_useritem = e_div_userlist.childNodes[index];
			if(e_div_useritem.fileid != undefined){
				if(e_div_useritem.getElementsByTagName('input')[0].checked == true){
					
					new Ajax.Request(userlink + '/info',{
						method:'post',
						parameters:{'type':'userdelfile','usersecid':usersecid,'userseckey':userseckey,'fileid':e_div_useritem.fileid}
					});
					delList.push(e_div_useritem);	
				}
			}
		}	
		
		for(index = 0;index < delList.length;index++){
			e_div_userlist.removeChild(delList[index]);	
		}	
	}
}