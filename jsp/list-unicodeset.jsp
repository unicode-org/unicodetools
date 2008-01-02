<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>UnicodeSet Demo</title>
<link rel="stylesheet" type="text/css" href="index.css">

<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="com.ibm.icu.text.*" %> 
<%@ page import="com.ibm.icu.lang.*" %>
<%@ page import="com.ibm.icu.impl.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="jsp.*" %>
<%@ page import="org.unicode.cldr.icu.*" %>

</head>

<body>

<%
		request.setCharacterEncoding("UTF-8");
		
		String setA = request.getParameter("a");
		if (setA == null) {
			setA = "[:ASCII:]";
		}
		boolean abbreviate = request.getParameter("abb") != null;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeUtilities.getSimpleSet(setA, a, abbreviate);
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		String sizeStr = nf.format(a.size());
%>
<h1>UnicodeSet Demo</h1>
<p>
	<a target="character" href="character.jsp">character</a> |
	<a target="properties" href="properties.jsp">properties</a> |
	<a target="list" href="list-unicodeset.jsp">unicode-set</a> |
	<a target="compare" href="unicodeset.jsp">compare-sets</a> |
	<a target="help" href="index.jsp">help</a>
</p>
<form name="myform" action="http://unicode.org/cldr/utility/list-unicodeset.jsp" method="POST">
  <p><textarea name="a" rows="3" cols="10" style="width: 100%"><%=setA%></textarea></p>
  <p><input type="submit" value="Show Set" />&nbsp;
  <input type="checkbox" <%=abbreviate ? "checked" : ""%> name="abb"><label for="abb">abbreviate</label></p>
  <hr>
  <p><%= sizeStr %> Code Points</p>
  <hr>
  <p><%=a_out%></p>
  <hr>
  <% UnicodeUtilities.showSet(a, abbreviate, out); %>
</form>
<p>Version 3, Built with ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>
<p></p>

</body>

</html>
