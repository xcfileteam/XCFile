<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="com.google.appengine.api.datastore.*"%>
<%@ page import="com.google.appengine.api.memcache.*"%>
<%@ page import="com.google.appengine.api.urlfetch.*"%>
<%@ page import="com.google.appengine.api.users.*"%>
<%@ page import="commonapp.*"%>
<%
	DatastoreService ds;
	MemcacheService ms;
	URLFetchService us;
	UserService userService;

	List<String> loginData;
	String userSecId;
	String userLink;
	String userSecKey;
	
	ds = DatastoreServiceFactory.getDatastoreService();
	ms = MemcacheServiceFactory.getMemcacheService();
	us = URLFetchServiceFactory.getURLFetchService();
	userService = UserServiceFactory.getUserService();
	
	loginData = Sec.createLogin(ds,ms,us,userService);
	if(loginData == null){
		userSecId = "";
		userLink = "";
		userSecKey = "";
	}else{
		userSecId = loginData.get(0);
		userLink = loginData.get(1);
		userSecKey = loginData.get(2);
	}
%>

<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<title>XC File Lab</title>
	</head>
	
	<link rel="stylesheet" type="text/css" href="flatbutton.css"/>
	
	<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js"></script>
	<script type="text/javascript" src="indexupload.js"></script>
	<script type="text/javascript" src="indexuser.js"></script>
	<script type="text/javascript">
		var usersecid = '<%= userSecId%>';
		var userlink = '<%= userLink%>';
		var userseckey = '<%= userSecKey%>';
	
		var loginflag;
		var e_div_mask;
		
		function init(){
			var index;
			
			var e_div_buttonlist;
			
			if(usersecid == ''){
				loginflag = false;
			}else{
				loginflag = true;
			}
			
			e_div_buttonlist = document.getElementsByTagName('div');
			for(index = 0;index < e_div_buttonlist.length;index++){
				if(e_div_buttonlist[index].className == 'button_r'){
					Event.observe(e_div_buttonlist[index],'mouseover',function(){this.className='button_r_hi';});
					Event.observe(e_div_buttonlist[index],'mouseout',function(){this.className='button_r';});
				}else if(e_div_buttonlist[index].className == 'button_b'){
					Event.observe(e_div_buttonlist[index],'mouseover',function(){this.className='button_b_hi';});
					Event.observe(e_div_buttonlist[index],'mouseout',function(){this.className='button_b';});
				}else if(e_div_buttonlist[index].className == 'button_gray'){
					Event.observe(e_div_buttonlist[index],'mouseover',function(){this.className='button_gray_hi';});
					Event.observe(e_div_buttonlist[index],'mouseout',function(){this.className='button_gray';});
				}
			}
			
			e_div_mask = $('div_mask');
			
			e_div_upload = $('div_upload');
			e_div_upload_close = $('div_upload_close');
			e_div_uploadlist = $('div_uploadlist');	
			e_div_uploaditem_ori = $('div_uploaditem_ori');
			uploadCount = 0;
			
			if(loginflag == true){
				e_div_upload.style.visibility = 'hidden';
				
				e_div_user = $('div_user');
				e_div_user.style.display = '';
				
				e_div_user_taglist = $('div_user_taglist');
				e_div_user_tagitem_ori = $('div_user_tagitem_ori');
				e_div_taginfo = $('div_taginfo');
				e_input_taginfo_tagname = $('input_taginfo_tagname');
				e_div_taginfo_setbutton = $('div_taginfo_setbutton');
				e_div_taginfo_delbutton = $('div_taginfo_delbutton');
				
				e_div_user_filelist = $('div_user_filelist');
				e_div_user_fileitem_ori = $('div_user_fileitem_ori');
				e_div_fileinfo = $('div_fileinfo');
				e_table_fileinfo_filename = $('table_fileinfo_filename');
				e_input_fileinfo_filename = $('input_fileinfo_filename');
				e_div_fileinfo_taglist = $('div_fileinfo_taglist');
				e_div_fileinfo_tagitem_ori = $('div_fileinfo_tagitem_ori');
				e_div_fileinfo_setbutton = $('div_fileinfo_setbutton');
				userlistCount = 0;
				
				resize();
			
				e_div_user_tagselect = userCreateTagItem(encodeURIComponent('All'));
				e_div_user_tagselect.e_div_point.style.visibility = 'visible';
				
				userGetList();
			}	
		}
		function resize(){	
			e_div_mask.style.height = window.innerHeight + 'px';
			e_div_uploadlist.style.height = window.innerHeight - e_div_uploadlist.offsetTop - 160 + 'px';
			
			if(loginflag == true){
				e_div_user.style.height = window.innerHeight - e_div_user.offsetTop - 34 + 'px';
				e_div_user_taglist.style.height = window.innerHeight - e_div_user.offsetTop - 84 + 'px';
				e_div_user_filelist.style.height = window.innerHeight - e_div_user.offsetTop - 84 + 'px';
				e_div_fileinfo_taglist.style.height = window.innerHeight - e_div_fileinfo_taglist.offsetTop - 364 + 'px';
			}
		}
	</script>
	
	<body style="margin:0px;" onload="init();" onresize="resize();">
		<div style="width:auto; margin:10px 10px 0px 10px; z-index:1;">
			<a href="http://xcfilelab.appspot.com"><img border=0 src="xcfilelabbar.png"/></a>
			<%
				if(userSecId.equals("")){
			%>
				<div class="button_b" style="float:right;" onclick="location.href = '<%= userService.createLoginURL("/")%>';">登入</div>
			<%
				}else{
			%>
				<div class="button_b" style="float:right;" onclick="location.href = '<%= userService.createLogoutURL("/")%>';">登出</div>
			<%		
				}
			%>
		</div>
		
		<div id="div_mask" style="width:100%; background-color:rgba(0,0,0,0.7); position:absolute; left:0px; top:0px; z-index:500; display:none;"></div>
		
		<div style="width:100%; text-align:center; position:absolute; left:0px; top:90px;">
			<div id="div_upload" style="width:62%; margin:50px auto 0px auto; padding:0px 10px 10px 10px; background-color:#FFFFFF; text-align:left; position:relative; z-index:1000;">
				<div id="div_upload_close" class="button_gray" style="margin:0px 0px 0px auto; visibility:hidden;" onclick="
					var index;	
					var e_div_uploaditem;
					var delList;
	
					delList = new Array();
					for(index = 0;index < div_uploadlist.childNodes.length;index++){
						e_div_uploaditem = div_uploadlist.childNodes[index];
						if(e_div_uploaditem.itemid != undefined){
							UploadApplet.cancelUpload(e_div_uploaditem.itemid);
							delList.push(e_div_uploaditem);
						}
					}
					
					for(index = 0;index < delList.length;index++){
						e_div_uploaditem.parentNode.removeChild(delList[index]);
					}
					
					uploadCount = 0;
					e_div_mask.style.display = 'none';
					e_div_upload.style.visibility = 'hidden';
					e_div_upload_close.style.visibility = 'hidden';
				">關閉</div>
				
				<div style="height:80px; margin:8px auto 0px auto; background-color:#FDFDFD; border:solid 1px #D9D9D9;">
					<script type="text/javascript" src="http://www.java.com/js/deployJava.js"></script>
				    <script type="text/javascript">
				    	var attributes = {code:'uploadapplet.UploadApplet',id:'UploadApplet',style:'width:100%; height:100%; margin:0px auto 0px auto;'};
						var parameters = {'jnlp_href':'uploadappletlab1.jnlp',
										'usersecid':usersecid,
										'userlink':userlink,
										'userseckey':userseckey}; 
						deployJava.runApplet(attributes,parameters,'1.6'); 
				    </script>
				</div>
				<div id="div_uploadlist" style="margin:10px 0px 0px 0px; text-align:center; background-color:#FDFDFD; border:solid 1px #D9D9D9; overflow-x:hidden; overflow-y:auto; display:none;">
					<div id="div_uploaditem_ori" style="width:81%; margin:2px auto 2px auto; display:none; text-align:center;">
						<table style="width:100%; margin:0px auto; border-collapse:collapse;">
							<colgroup>
								<col style="width:auto;"/>
								<col style="width:80px;"/>
							</colgroup>
							<tr>
								<td><input type="text" readonly=readonly style="width:100%; padding:0px 0px 0px 5px; background-color:#FDFDFD; border:0px; border-bottom:solid 1px #D9D9D9; font-size:20px;"/></td>
								<td>
									<div class="button_b" onclick="
										var e_div_uploaditem;
									
										e_div_uploaditem = this.e_div_uploaditem;
										UploadApplet.deleteUpload(e_div_uploaditem.itemid);
										e_div_uploaditem.parentNode.removeChild(e_div_uploaditem);
										
										uploadCount--;
										if(uploadCount == 0){
											e_div_uploadlist.style.display = 'none';
										}
									">移除</div>
									
									<div class="button_b" style="display:none;" onclick="
										var e_div_uploaditem;
									
										e_div_uploaditem = this.e_div_uploaditem;
										UploadApplet.cancelUpload(e_div_uploaditem.itemid);
										e_div_uploaditem.parentNode.removeChild(e_div_uploaditem);
										
										uploadCount--;
										if(uploadCount == 0){
											e_div_uploadlist.style.display = 'none';
										}
									">取消</div>
								</td>
							</tr>
							<tr style="display:none;">
								<td><div style="width:100%; height:24px; background-color:#D9D9D9; position:relative;">
									<div style="width:0%; height:24px; background-color:#5D92FF; position:absolute; left:0px; top:0px; z-index:100;"></div>
									<div style="width:100%; height:24px; line-height:24px; font-weight:bold; text-align:center; position:absolute; left:0px; top:0px; z-index:200;">等待中</div>
								</div></td>
								<td><div style="width:100%; height:24px; line-height:24px; font-size:small; text-align:right;">0B/s</div></td>
							</tr>
							<tr style="display:none;">
								<td><input type="text" readonly=readonly style="width:100%; padding:0px 0px 0px 5px; background-color:#36454F; border:0px; color:#FFFFFF; font-size:20px;" onclick="this.select();"/></td>
								<td><div class="button_b" onclick="
									var e_div_uploaditem;
								
									e_div_uploaditem = this.e_div_uploaditem;
									UploadApplet.copyLink(e_div_uploaditem.link);
								">複製</div></td>
							</tr>
						</table>
					</div>
				</div>
			</div>
		</div>
		
		<div style="width:100%; text-align:center; position:absolute; left:0px; top:90px;">
			<div id="div_taginfo" style="width:38%; margin:50px auto 0px auto; padding:0px 10px 10px 10px; background-color:#FFFFFF; text-align:left; position:relative; z-index:1000; display:none;">
				<div id="div_taginfo_close" class="button_gray" style="margin:0px 0px 0px auto;" onclick="
					e_div_mask.style.display = 'none';
					e_div_taginfo.style.display = 'none';
				">關閉</div>
				
				<div style="margin:8px auto 0px auto; background-color:#FDFDFD; border:solid 1px #D9D9D9;">
					<table id="table_taginfo_filename" style="width:62%; margin:10px auto 0px auto; border-collapse:collapse;">
						<col style="width:80px;"/>
						<col style="width:auto;"/>
						<tr>
							<td><div style="width:100%; height:24px; line-height:24px; background-color:#36454F; color:#FFFFFF; font-weight:bold; text-align:center;">標籤名稱</div></td>
							<td><input id="input_taginfo_tagname" type=text style="width:100%; height:24px; border:solid 1px #D9D9D9;"/></td>
						</tr>
					</table>
					
					<div style="width:62%; margin:80px auto 10px auto;">
						<div class="button_gray" style="width:60px; float:right;" onclick="
							e_div_mask.style.display = 'none';
							e_div_taginfo.style.display = 'none';
						">取消</div>
						<div id="div_taginfo_setbutton" class="button_b" style="width:60px; margin:0px 5px 0px 0px; float:right;" onclick="
							userSetTag(this.e_div_user_tagitem);
							e_div_mask.style.display = 'none';
							e_div_taginfo.style.display = 'none';
						">確定</div>
						<div id="div_taginfo_delbutton" class="button_b" style="width:60px; margin:0px 5px 0px 0px; float:right;" onclick="
							userDelTag(this.e_div_user_tagitem);
							e_div_mask.style.display = 'none';
							e_div_taginfo.style.display = 'none';
						">刪除</div>
						<div style="clear:both;"></div>
					</div>
				</div>
			</div>
		</div>
		
		<div style="width:100%; text-align:center; position:absolute; left:0px; top:90px;">
			<div id="div_fileinfo" style="width:38%; margin:50px auto 0px auto; padding:0px 10px 10px 10px; background-color:#FFFFFF; text-align:left; position:relative; z-index:1000; display:none;">
				<div id="div_fileinfo_close" class="button_gray" style="margin:0px 0px 0px auto;" onclick="
					e_div_mask.style.display = 'none';
					e_div_fileinfo.style.display = 'none';
				">關閉</div>
				
				<div style="margin:8px auto 0px auto; background-color:#FDFDFD; border:solid 1px #D9D9D9;">
					<table id="table_fileinfo_filename" style="width:62%; margin:10px auto 10px auto; border-collapse:collapse;">
						<col style="width:80px;"/>
						<col style="width:auto;"/>
						<tr>
							<td><div style="width:100%; height:24px; line-height:24px; background-color:#36454F; color:#FFFFFF; font-weight:bold; text-align:center;">檔案名稱</div></td>
							<td><input id="input_fileinfo_filename" type=text style="width:100%; height:24px; border:solid 1px #D9D9D9;"/></td>
						</tr>
					</table>
					
					<div style="width:62%; margin:10px auto 0px auto;">
						<div style="width:80px; height:24px; line-height:24px; background-color:#36454F; color:#FFFFFF; font-weight:bold; text-align:center; float:left;">標籤管理</div>
						<div class="button_b" style="float:right;" onclick="
							userAddTag();
						">新增標籤</div>
						<div style="clear:both;"></div>
					</div>
					
					<div id="div_fileinfo_taglist" style="width:62%; min-height:80px; margin:10px auto 0px auto; border:solid 1px #D9D9D9; overflow-x:hidden; overflow-y:auto;">
						<div id="div_fileinfo_tagitem_ori" style="border-bottom:solid 1px #D9D9D9; display:none;"
						onmouseover="
							this.style.backgroundColor='#F6F6F6';
						"
						onmouseout="
							this.style.backgroundColor='';
						"
						onclick="
						">
							<table style="width:100%; height:24px; border-collapse:collapse;">
								<colgroup>
									<col style="width:25px; text-align:center;"/>
									<col style="width:auto;"/>
								</colgroup>
								<tr>
									<td><div style="width:23px; height:23px;"><input type="checkbox" onchange="
										this.parentNode.style.backgroundColor = '';
									"/></div></td>
									<td><div style="width:100%; color:#36454F; font-size:16px; font-weight:bold;"></div></td>
								</tr>
							</table>
						</div>
					</div>
					
					<div style="width:62%; margin:10px auto 10px auto;">
						<div class="button_gray" style="float:right;" onclick="
							e_div_mask.style.display = 'none';
							e_div_fileinfo.style.display = 'none';
						">取消</div>
						<div id="div_fileinfo_setbutton" class="button_b" style="margin:0px 5px 0px 0px; float:right;" onclick="
							userFileInfoSet(this.e_div_user_fileitem);
							e_div_mask.style.display = 'none';
							e_div_fileinfo.style.display = 'none';
						">確定</div>
						<div style="clear:both;"></div>
					</div>
				</div>
			</div>
		</div>
		
		<div id="div_user" style="width:100%; text-align:left; position:absolute; left:0px; top:140px; z-index:1; display:none;">
			<div style="height:24px; line-height:24px; padding:0px 0px 0px 10px; background-color:#36454F; color:#FFFFFF; font-weight:bold;">檔案管理</div>
			<table style="width:100%; background-color:#FDFDFD; border-collapse:collapse;"><tr>
				<td valign="top" style="width:200px;">
					<div style="width:100%;">
						<div style="height:50px; border-bottom:solid 1px #D9D9D9;">
							<table style="width:100%; height:100%; border-collapse:collapse;">
								<tr><td><div class="button_b" style="margin:0px 0px 0px 10px;" onclick="
									userAddTag();
								">新增標籤</div></td></tr>
							</table>
						</div>
						
						<div id="div_user_taglist" style="overflow-x:hidden; overflow-y:auto;">
							<div id="div_user_tagitem_ori" style="border-bottom:solid 1px #D9D9D9; display:none;"
							onmouseover="
								this.style.backgroundColor='#F6F6F6';
								this.e_div_operbutton.style.display = '';
							"
							onmouseout="
								this.style.backgroundColor='';
								this.e_div_operbutton.style.display = 'none';
							"
							onclick="
								if(event.target != this.e_div_operbutton){
									e_div_user_tagselect.e_div_point.style.visibility = 'hidden';
									e_div_user_tagselect = this;
									this.e_div_point.style.visibility = 'visible';
									
									userSwitchFileList();
								}
							">
								<table style="width:100%; height:50px; border-collapse:collapse;">
									<colgroup>
										<col style="width:10px;"/>
										<col style="width:100px;"/>
										<col style="width:auto;"/>
									</colgroup>
									<tr>
										<td><div style="width:100%; height:100%; background-color:#36454F; visibility:hidden;"></div></td>
										<td><div style="width:100%; color:#36454F; font-size:16px; font-weight:bold; word-wrap:break-word; word-break:break-all;"></div></td>
										<td><div class="button_b" style="width:40px; margin:0px 10px 0px auto; display:none" onclick="
											e_input_taginfo_tagname.value = decodeURIComponent(this.e_div_user_tagitem.tagobj.tagname);
										
											e_div_taginfo_setbutton.e_div_user_tagitem = this.e_div_user_tagitem;
											e_div_taginfo_delbutton.e_div_user_tagitem = this.e_div_user_tagitem;
										
											e_div_mask.style.display = '';
											e_div_taginfo.style.display = '';
											
											resize();
										">設定</div></td>
									</tr>
								</table>
							</div>
						</div>
					</div>
				</td>
				<td valign="top" style="border-left:solid 1px #D9D9D9;">
					<div style="height:50px; border-bottom:solid 1px #D9D9D9;">
						<table style="width:100%; height:100%; border-collapse:collapse;">
							<colgroup>
								<col style="width:25px; text-align:center;"/>
								<col style="width:200px;"/>
								<col style="width:auto;"/>
							</colgroup>
							<tr>
								<td><input type="checkbox" onclick="
									var fileid;
								
									for(fileid in fileMap){
										fileMap[fileid].e_div_user_fileitem.e_input_check.checked = this.checked;
									}
								"/></td>
								<td>
									<div class="button_b" style="float:left;" onclick="
										if(confirm('確定刪除檔案?') == true){
											userDelFileItem(null);
										}
									">刪除所選</div>
									<div class="button_b" style="margin:0px 0px 0px 5px; float:left;" onclick="
										var index;
										var tagcountmap;
										var fileCount;
										var fileid;
										var fileObj;
										var tagname;
										var tagObj;
										
										tagcountmap = new Object();
										fileCount = 0;
										for(fileid in fileMap){
											fileObj = fileMap[fileid];
											
											if(fileObj.e_div_user_fileitem.e_input_check.checked == true){
												for(tagname in fileObj.tagnamemap){
													if(tagcountmap[tagname] == undefined){
														tagcountmap[tagname] = 0;
													}
													tagcountmap[tagname] += 1;
												}
												
												fileCount++;
											}
										}
										
										for(index = 0;index < e_div_fileinfo_taglist.childNodes.length;index++){
											e_div_fileinfo_tagitem = e_div_fileinfo_taglist.childNodes[index];
											if(e_div_fileinfo_tagitem.tagname != undefined){
												if(tagcountmap[e_div_fileinfo_tagitem.tagname] != undefined){
													e_div_fileinfo_tagitem.e_input_check.checked = true;
													
													if(tagcountmap[e_div_fileinfo_tagitem.tagname] == fileCount){
														e_div_fileinfo_tagitem.e_div_state.style.backgroundColor = '';
													}else{
														e_div_fileinfo_tagitem.e_div_state.style.backgroundColor = '#D9D9D9';
													}
												}else{
													e_div_fileinfo_tagitem.e_input_check.checked = false;
													e_div_fileinfo_tagitem.e_div_state.style.backgroundColor = '';
												}
											}
										}
										
										e_div_fileinfo_setbutton.e_div_user_fileitem = null;
										
										e_table_fileinfo_filename.style.display = 'none';
										e_div_mask.style.display = '';
										e_div_fileinfo.style.display = '';
									">標籤所選</div>
									<div style="clear:both;"></div>
								</td>
								<td>
									<div class="button_r" style="margin:0px 10px 0px 0px; float:right;" onclick="
										e_div_mask.style.display = '';
										e_div_upload.style.visibility = 'visible';
										e_div_upload_close.style.visibility = 'visible';
									">上傳檔案</div>
									<div style="clear:both;"></div>
								</td>
							</tr>
						</table>
					</div>
				
					<div id="div_user_filelist" style="overflow-x:hidden; overflow-y:auto;">
						<div id="div_user_fileitem_ori" style="border-bottom:solid 1px #D9D9D9; display:none;"
						onmouseover="
							this.style.backgroundColor='#F6F6F6';
							this.e_div_oper.style.display = '';
						"
						onmouseout="
							this.style.backgroundColor='';
							this.e_div_oper.style.display = 'none';
						">
							<table style="width:100%; height:50px; border-collapse:collapse;">
								<colgroup>
									<col style="width:25px; text-align:center;"/>
									<col style="width:15%;"/>
									<col style="width:50%;"/>
									<col style="width:60px;"/>
									<col style="width:auto;"/>
								</colgroup>
								<tr>
									<td><input type="checkbox"/></td>
									<td><div style="width:100%; font-size:12px;"></div></td>
									<td><div style="width:100%;"><a target="_blank" style="color:#36454F; font-size:12px; font-weight:bold;"></a><span style="margin:0px 0px 0px 5px; color:#D9D9D9; font-size:12px; font-weight:bold;"></span></div></td>
									<td><div style="width:100%; font-size:12px; text-align:right;"></div></td>
									<td><div style="width:150px; margin:0px 10px 0px auto; display:none;">
										<div class="button_b" style="float:right;" onclick="
											var index;
											var fileObj;
											var e_div_fileinfo_tagitem;
										
											fileObj = this.e_div_user_fileitem.fileobj;
											
											e_input_fileinfo_filename.value = decodeURIComponent(fileObj.filename);
											
											for(index = 0;index < e_div_fileinfo_taglist.childNodes.length;index++){
												e_div_fileinfo_tagitem = e_div_fileinfo_taglist.childNodes[index];
												if(e_div_fileinfo_tagitem.tagname != undefined){
													if(fileObj.tagnamemap[e_div_fileinfo_tagitem.tagname] != undefined){
														e_div_fileinfo_tagitem.e_input_check.checked = true;
													}else{
														e_div_fileinfo_tagitem.e_input_check.checked = false;
													}
													e_div_fileinfo_tagitem.e_div_state.style.backgroundColor = '';
												}
											}
											
											e_div_fileinfo_setbutton.e_div_user_fileitem = this.e_div_user_fileitem;
											
											e_table_fileinfo_filename.style.display = '';
											e_div_mask.style.display = '';
											e_div_fileinfo.style.display = '';
											
											resize();
										">標籤,設定</div>
										<div class="button_b" style="width:60px; margin:0px 5px 0px 0px; float:right;" onclick="
											if(confirm('確定刪除檔案?') == true){
												userDelFileItem(this.e_div_user_fileitem);
											}
										">刪除</div>
										<div style="clear:both;"></div>
									</div></td>
								</tr>
							</table>
						</div>
					</div>
				</td>
			</tr></table>
		</div>
	</body>
</html>
