<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Character Properties</title>
<link rel="stylesheet" type="text/css" href="index.css">
<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Utilities: Character Properties</h1>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/character"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<div style='text-align:center'>
<form name="myform" action="character.jsp" method="get">
  <%
		request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
		String text = utfParameters.getParameter("a", "\u2615", "\u2615");

		int cp = UnicodeJsp.parseCode(text,null,null);
		//text = UTF16.valueOf(cp);
	    String nextHex = "character.jsp?a=" + Utility.hex(cp < 0x110000 ? cp+1 : 0, 4);
		String prevHex = "character.jsp?a=" + Utility.hex(cp > 0 ? cp-1 : 0x10FFFF, 4);
%>
  <p>
  <input name="B3" type="button" value="-" onClick="window.location.href='<%=prevHex%>'">
  <input name="a" type="text" style='text-align:center; font-size:150%' size="10" value="<%=text%>">
  <input name="B2" type="button" value="+" onClick="window.location.href='<%=nextHex%>'"><br>
  <input name="B1" type="submit" value="Show">
  </p>
</form>
<%
	UnicodeJsp.showProperties(cp, out); 
%>
</div>
<p>The list includes both Unicode Character Properties and some additions (like idna2003 or subhead)</p>
<%@ include file="footer.jsp" %>
</body>

</html>
