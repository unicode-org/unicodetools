<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Unicode Property List</title>
<link rel="stylesheet" type="text/css" href="index.css">
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.*" %> 
<%@ page import="java.lang.*" %> 
<%@ page import="com.ibm.icu.text.*" %>
<%@ page import="com.ibm.icu.lang.*" %> 
<%@ page import="com.ibm.icu.util.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="jsp.*" %>
<%@ page import="org.unicode.cldr.icu.*" %>

<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Property List</h1>
<p>
	<a target="character" href="character.jsp">character</a> |
	<a target="properties" href="properties.jsp">properties</a> |
	<a target="list" href="list-unicodeset.jsp">unicode-set</a> |
	<a target="compare" href="unicodeset.jsp">compare-sets</a> |
	<a target="help" href="index.jsp">help</a>
</p>
<%
		request.setCharacterEncoding("UTF-8");

		Set<String> unicodeProps = UnicodeUtilities.showPropsTable(out);
%>
<p>® = Regex Property (<a target="_blank" href="http://www.unicode.org/reports/tr18/">UTS #18</a>): 
not formal Unicode property<br>
© = ICU-Only Property (not Unicode or Regex)<br>
<b>Not explicitly in ICU: </b><%=unicodeProps%></p>
<p>Version 3<br>
ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %><br>
Unicode version: <%= com.ibm.icu.lang.UCharacter.getUnicodeVersion().toString() %><br>
</b></b>

</body>

</html>
