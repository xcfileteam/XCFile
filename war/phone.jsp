<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%@ page contentType="text/html;charset=UTF-8"%>

<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<title>XC File Lab</title>
	
		<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/prototype/1.7.0.0/prototype.js"></script>
		<script type="text/javascript">
			var type = '<%= (String)request.getAttribute("type")%>';
		
			function init(){
				if(type == 'phone'){
					$('div_info_phone').style.display = '';
				}else if(type == 'code'){
					$('tr_account').style.display = 'none';
					$('span_data').firstChild.nodeValue = '驗證碼';
					$('div_info_code').style.display = '';
				}else if(type == 'done'){
					$('div_form').style.display = 'none';
					$('div_info_done').style.display = '';
				}else if(type == 'none'){
					$('div_form').style.display = 'none';
					$('div_info_none').style.display = '';
				}else if(type == 'error'){
					$('div_form').style.display = 'none';
					$('div_info_error').style.display = '';
				}
			}
		</script>
	</head>
	
	<body style="margin:0px;" onload="init();">
		<div style="margin:10px 10px 0px 10px;"><a href="http://xcfilelab.appspot.com"><img border=0 src="xcfilelabbar.png"/></a></div>
		
		<div style="width:100%; margin:50px 0px 0px 0px; text-align:center;">
			<div id="div_info_phone" style="width:38%; margin:0px auto; text-align:left; display:none;">
				<h2>XC File&nbsp和在網路上分享資源的人需要你的協助</h2>
				只要透過捐贈行動電話號碼提供&nbspXC File&nbsp進行&nbspGoogle App Engine&nbsp的行動電話認證(<a href="https://developers.google.com/appengine/kb/sms">這是什麼?</a>)，你就可以幫助在網路上免費分享心血的人擁有更好的檔案分享空間。<br/><br/>
				XC File&nbsp不會紀錄和將你的行動電話號碼用於其他用途。<br/>
				填寫你的&nbspGoogle&nbsp帳號(通常是你的&nbspGmail&nbsp信箱)，為的是將來實施帳號分級制時，你將會被列為捐贈者帳號，享有更好的分享空間服務。
				<br/><br/>
				行動電話號碼需要加上國碼，台灣地區為&nbsp886，並且號碼開頭的&nbsp0&nbsp要刪除。(例如行動電話號碼是&nbsp<strong>0912345678</strong>，就輸入&nbsp<strong>886912345678</strong>)
			</div>
			<div id="div_info_code" style="width:38%; margin:0px auto; text-align:left; display:none;">
				<h2>最後一個步驟</h2>
				現在只要填寫你的行動電話所收到的驗證碼(通常10分鐘內會收到)，就可以完成行動電話號碼捐贈。<br/>
				如果一直未收到驗證碼，請<a href="/phone">重試</a>。<br/><br/>
				如果重試後狀況仍然無法排除，請發訊息給我們的客戶服務人員&nbsp<a href="https://www.facebook.com/profile.php?id=100002460867714">Fred&nbspWhite</a><br/><br/>
			</div>
			<div id="div_info_done" style="width:38%; margin:0px auto; text-align:left; display:none;">
				<h2>恭喜你</h2>
				恭喜你已經完成行動電話號碼捐贈，XC File&nbsp和在網路上分享資源的人感謝你的協助。<br/><br/>
				<a href="http://xcfilespace.appspot.com">回到&nbspXC File</a>
			</div>
			<div id="div_info_none" style="width:38%; margin:0px auto; text-align:left; display:none;">
				<h2>十分抱歉</h2>
				目前捐贈額度已滿，由於伺服器帳戶是人工申請，因此一次無法提供大量的捐贈額度。<br/>
				我們將會盡快補充新的額度。<br/><br/>
				<a href="http://xcfilespace.appspot.com">回到&nbspXC File</a>
			</div>
			<div id="div_info_error" style="width:38%; margin:0px auto; text-align:left; display:none;">
				<h2>Oops!</h2>
				處理過程中發生了點狀況，請確定你輸入的行動電話號碼或驗證碼是正確的，並且行動電話號碼之前未用來進行過&nbspGoogle App Engine&nbsp的行動電話認證。<br/><br/>
				如果重試後狀況仍然無法排除，請發訊息給我們的客戶服務人員&nbsp<a href="https://www.facebook.com/profile.php?id=100002460867714">Fred&nbspWhite</a><br/><br/>
				<a href="/phone">重試</a>
			</div>
			<div id="div_form" style="width:38%; margin:30px auto 0px auto; text-align:left;">
				<form id="form_phone" action="phone" method="post">
					<input id="input_type" type="hidden" name="type" value="<%= (String)request.getAttribute("type")%>">
					<input id="input_phoneid" type="hidden" name="phoneid" value="<%= (String)request.getAttribute("phoneid")%>">
					<table>
						<tr id="tr_account">
							<td><span>Google&nbsp帳號</span></td><td><input type="text" name="account" value="可不填寫"></td>
						</tr>
						<tr>
							<td><span id="span_data">行動電話號碼</span></td><td><input type="text" name="data"><input type="submit" value="確定"></td>
						</tr>
					</table>
				</form>
			</div>
		</div>
	</body>
</html>
