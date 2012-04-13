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
	
	<link rel="stylesheet" type="text/css" href="flatbutton.css" />
	
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
		
			//loginflag=true;
			
			if(loginflag == true){
				e_div_upload.style.visibility = 'hidden';
				
				$('div_user').style.display = '';
				
				e_div_user = $('div_user');
				e_div_userlist = $('div_userlist');
				e_div_useritem_ori = $('div_useritem_ori');
				userlistCount = 0;
				
				resize();
				
				userGetFilelist();
			}
			
			/*for(index=0;index<25;index++){
				userCreateItem('sdfdsf','02595_oiawindmills_19werewewr20x108fdg0.jpg',1023*1024,new Date().getTime(),'http://xcfilelabd.appspot.com/down/rtR73Bx4/02595_oiawindmills_1920x1080.jpg');
			}*/
		}
		function resize(){
			e_div_mask.style.height = window.innerHeight + 'px';
			e_div_uploadlist.style.height = window.innerHeight - e_div_uploadlist.offsetTop - 160 + 'px';
			
			if(loginflag == true){
				e_div_user.style.height = window.innerHeight - e_div_user.offsetTop - 34 + 'px';
				e_div_userlist.style.height = window.innerHeight - e_div_user.offsetTop - 84 + 'px';
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
		
		<div style="text-align:center;">
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
						var parameters = {'jnlp_href':'uploadapplet.jnlp',
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
								<td><input type="text" readonly=readonly style="width:100%; padding:0px 0px 0px 5px; background-color:#36454F; color:#FFFFFF; border:0px; font-size:20px;" onclick="this.select();"/></td>
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
		
		<div id="div_user" style="width:100%; text-align:left; position:absolute; left:0px; top:140px; z-index:1; display:none;">
			<div style="height:24px; line-height:24px; padding:0px 0px 0px 10px; background-color:#36454F; color:#FFFFFF; font-weight:bold;">檔案管理</div>
			<table style="width:100%; background-color:#FDFDFD; border-collapse:collapse;"><tr>
				<td valign="top" style="width:160px;">
					<div id="div_usertag" style="width:100%;"></div>
				</td>
				<td valign="top" style="border-left:solid 1px #D9D9D9;">
					<div style="height:50px; border-bottom:solid 1px #D9D9D9;">
						<table style="width:100%; height:100%; border-collapse:collapse;">
							<colgroup>
								<col style="width:20px;"/>
								<col style="width:80px;"/>
								<col style="width:auto;"/>
							</colgroup>
							<tr>
								<td><input type="checkbox" onclick="
									var index;
									var e_div_useritem;
								
									for(index = 0;index < e_div_userlist.childNodes.length;index++){
										e_div_useritem = e_div_userlist.childNodes[index];
										if(e_div_useritem.fileid != undefined){
											e_div_useritem.getElementsByTagName('input')[0].checked = this.checked;
										}
									}
								"/></td>
								<td><div class="button_b" onclick="
									if(confirm('確定刪除檔案?') == true){
										userDelItem(null);
									}
								">刪除所選</div></td>
								<td><div class="button_r" style="margin:0px 10px 0px auto;" onclick="
									e_div_mask.style.display = '';
									e_div_upload.style.visibility = 'visible';
									e_div_upload_close.style.visibility = 'visible';
								">上傳檔案</div></td>
							</tr>
						</table>
					</div>
				
					<div id="div_userlist" style="overflow-x:hidden; overflow-y:auto;">
						<div id="div_useritem_ori" style="border-bottom:solid 1px #D9D9D9; display:none;"
						onmouseover="
							this.style.backgroundColor='#F6F6F6';
							this.e_div_del.style.display = '';
						"
						onmouseout="
							this.style.backgroundColor='';
							this.e_div_del.style.display = 'none';
						">
							<table style="width:100%; height:50px; border-collapse:collapse;">
								<colgroup>
									<col style="width:20px;"/>
									<col style="width:15%;"/>
									<col style="width:50%;"/>
									<col style="width:60px;"/>
									<col style="width:auto;"/>
								</colgroup>
								<tr>
									<td><input type="checkbox"/></td>
									<td><div style="width:100%; font-size:12px;"></div></td>
									<td><div style="width:100%;"><a target="_blank" style="color:#36454F; font-size:12px; font-weight:bold;"></a></div></td>
									<td><div style="width:100%; font-size:12px; text-align:right;"></div></td>
									<td><div class="button_b" style="margin:0px 10px 0px auto; display:none" onclick="
											if(confirm('確定刪除檔案?') == true){
												userDelItem(this.e_div_useritem);
											}
									">刪除</div></td>
								</tr>
							</table>
						</div>
					</div>
				</td>
			</tr></table>
		</div>
	</body>
</html>
