<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>UnicodeSet Demo</title>
<link rel="stylesheet" type="text/css" href="index.css">

<%@page contentType="text/html;charset=UTF-8" pageEncoding="ISO-8859-1"%>
<%@ page import="com.ibm.icu.text.*" %> 
<%@ page import="com.ibm.icu.lang.*" %>
<%@ page import="com.ibm.icu.impl.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="jsp.*" %>
<%@ page import="org.unicode.cldr.icu.*" %>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.net.URLDecoder"%>
</head>

<body>

<%
		request.setCharacterEncoding("UTF-8");
		//response.setContentType("text/html;charset=UTF-8"); //this is redundant
		String queryString = request.getQueryString();
		
		UtfParameters utfParameters = new UtfParameters(queryString);
		
		String setA = utfParameters.getParameter("a");

		if (setA == null) {
			setA = "[:ASCII:]";
		}
		boolean abbreviate = request.getParameter("abb") != null;
		boolean ucdFormat = request.getParameter("ucd") != null;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeUtilities.getSimpleSet(setA, a, abbreviate);
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		String sizeStr = nf.format(a.size());
		//   action="http://unicode.org/cldr/utility/list-unicodeset.jsp" method="POST"
%>
<h1>UnicodeSet Demo</h1>
<p>
	<a target="character" href="character.jsp">character</a> |
	<a target="properties" href="properties.jsp">properties</a> |
	<a target="list" href="list-unicodeset.jsp">unicode-set</a> |
	<a target="compare" href="unicodeset.jsp">compare-sets</a> |
	<a target="help" href="index.jsp">help</a>
</p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=setA%></textarea></td>
    </tr>
    <tr>
      <td><input id='main' type="submit" value="Show Set" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;
      <input type="checkbox" <%=abbreviate ? "checked" : ""%> name="abb"><label for="abb">abbreviate</label>
      <input type="checkbox" <%=ucdFormat ? "checked" : ""%> name="ucd"><label for="ucd">UCD format</label></td>
    </tr>
</table>
  <p><%= sizeStr %> Code Points</p>
  <hr>
  <p><%=a_out%></p>
  <hr>
  <% UnicodeUtilities.showSet(a, abbreviate, ucdFormat, out); %>
</form>
<p>Version 3.2<br>
ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %><br>
Unicode version: <%= com.ibm.icu.lang.UCharacter.getUnicodeVersion().toString() %><br>
Parameter: a=<%= setA %><br>
Parameter: abb=<%= abbreviate %><br>
Parameter: ucd=<%= ucdFormat %><br>
</body>

</html>
