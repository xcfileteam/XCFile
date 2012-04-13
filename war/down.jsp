<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%@ page contentType="text/html;charset=UTF-8"%>

<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<title>XC File Lab</title>
	</head>
	
	<link rel="stylesheet" type="text/css" href="flatbutton.css" />
	
	<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js"></script>
	<script type="text/javascript">
		var filename = decodeURIComponent("<%= (String)request.getAttribute("filename")%>");	//Use "" for URI
		var filesize = '<%= (String)request.getAttribute("filesize")%>';
	
		var e_div_progvalue;
		var e_div_progtext;
		var e_div_speed;
	
		function init(){
			var size
			var num;
			
			e_div_progvalue = $("div_progvalue");
			e_div_progtext = $("div_progtext");
			e_div_speed = $("div_speed");
			
			document.getElementsByName('input_name')[0].value = filename;
			
			size = parseInt(filesize,10);
			if(size >= 1073741824){
				num = new Number(size / 1073741824.0);
				size = num.toFixed(1) + 'GB';
			}else if(size >= 1048576){
				num = new Number(size / 1048576);
				size = num.toFixed(1) + 'MB';
			}else if(size >= 1024){
				num = new Number(size / 1024);
				size = num.toFixed(1) + 'KB';
			}else{
				size = size + 'B';
			}
			$('div_size').childNodes[0].nodeValue = size;
		}
		
		function updateState(state){
			if(state == 'init'){
				e_div_progvalue.style.width = '0%';
				e_div_progtext.childNodes[0].nodeValue = '等待中';
				e_div_speed.childNodes[0].nodeValue = ' ';
			}else if(state == 'download'){
				$('table_prog').style.display = '';
			}else if(state == 'done'){
				$('DownloadApplet').style.display = 'none';
				e_div_progvalue.style.width = '100%';
				e_div_progtext.childNodes[0].nodeValue = '下載完成';
				e_div_speed.childNodes[0].nodeValue = ' ';
			}
		}
		function updateProg(progvalue,speed){
			if(progvalue > 100.0){
				progvalue = 100.0;
			}
			
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
	</script>
	
	<body style="margin:0px;" onload="init();">
		<div style="margin:10px 10px 0px 10px;"><a href="http://xcfilelab.appspot.com"><img border=0 src="../../xcfilelabbar.png"/></a></div>
		<div style="width:100%; margin:160px 0px 0px 0px; text-align:center;">
			<div style="width:62%; height:150px; margin:0px auto; background-color:#FDFDFD; border:solid 1px #D9D9D9; text-align:center;">
				<table style="width:62%; height:100px; margin:0px auto; text-align:left;"><tr>
					<td><table style="width:100%;">
						<tr>
							<td style="width:40px; font-weight:bold;">檔名:</td>
							<td><input name="input_name" type="text" readonly=readonly style="width:100%; border-width:0px; background-color:#FDFDFD;"/></td>
						</tr>
						<tr>
							<td style="width:40px; font-weight:bold;">大小:</td>
							<td><div id="div_size"> </div></td>
						</tr>
					</table></td>
					<td style="width:100px;">
						<script type="text/javascript" src="http://www.java.com/js/deployJava.js"></script>
					    <script type="text/javascript">
					    	var attributes = {code:'downloadapplet.DownloadApplet',id:'DownloadApplet',width:100,height:50};
							var parameters = {'jnlp_href':'../../downloadapplet.jnlp',
											'filename':filename,
											'filesize':filesize,
											'serverlist':'<%= (String)request.getAttribute("serverlist") %>',
											'blobkeylist':'<%= (String)request.getAttribute("blobkeylist") %>'}; 
							deployJava.runApplet(attributes,parameters,'1.6'); 
					    </script>
					</td>
				</tr></table>
				<table id="table_prog" style="width:62%; margin:0px auto;">
					<tr>
						<td style="width:auto;"><div style="width:100%; height:24px; background-color:#D9D9D9; position:relative;">
							<div id="div_progvalue" style="width:0%; height:24px; background-color:#5D92FF; position:absolute; left:0px; top:0px; z-index:100;"></div>
							<div id="div_progtext" style="width:100%; height:24px; font-weight:bold; line-height:24px; text-align:center; position:absolute; left:0px; top:0px; z-index:200;">等待中</div>
						</div></td>
						<td style="width:100px"><div id="div_speed" style="width:100%; height:24px; font-size:small; line-height:24px; text-align:right;"> </div></td>
					<tr>
				</table>
			</div>
		</div>
	</body>
</html>
