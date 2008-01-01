<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>UnicodeSet Demo</title>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="com.ibm.icu.text.*" %> 
<%@ page import="com.ibm.icu.lang.*" %>
<%@ page import="com.ibm.icu.impl.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="jsp.*" %>
<%@ page import="org.unicode.cldr.icu.*" %>
<style>
<!--
td {vertical-align: top}
span.break   { border-right: 1px solid red;}
-->
</style>
</head>

<body>

<%
		request.setCharacterEncoding("UTF-8"); 

		String setA = request.getParameter("a");
		if (setA == null) setA = "[:Lowercase:]";
		String setB = request.getParameter("b");
		if (setB == null) setB = "[:Lowercase_Letter:]";
		String a_b =  "error";
		String b_a =  "error";
		String ab = "error";

		UnicodeSet a = null;
		try {
		     setA = Normalizer.normalize(setA, Normalizer.NFC);
			a = UnicodeUtilities.parseUnicodeSet(setA);
		} catch (Exception e) {
			a_b = e.getMessage();
		}
		UnicodeSet b = null;
		try {
		     setB = Normalizer.normalize(setB, Normalizer.NFC);
			b = UnicodeUtilities.parseUnicodeSet(setB);
		} catch (Exception e) {
			b_a = e.getMessage();
		}
		int a_bSize = 0, b_aSize = 0, abSize = 0;
		if (a != null && b != null) {
		     PrettyPrinter pp = new PrettyPrinter();
		     
			UnicodeSet temp = new UnicodeSet(a).removeAll(b);
			a_bSize = temp.size();
			a_b = UnicodeUtilities.toHTML(pp.toPattern(temp));
			
			temp = new UnicodeSet(b).removeAll(a);
			b_aSize = temp.size();
			b_a = UnicodeUtilities.toHTML(pp.toPattern(temp));
			
			temp = new UnicodeSet(a).retainAll(b);
			abSize = temp.size();
			ab = UnicodeUtilities.toHTML(pp.toPattern(temp));
		}
		NumberFormat nf = NumberFormat.getIntegerInstance();
%>
<h1>UnicodeSet Demo (<a target="u" href="list-unicodeset.jsp">List</a>)</h1>
<form name="myform" action="http://unicode.org/cldr/utility/unicodeset.jsp" method="POST">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
    <tr>
      <td style="width: 45%">
      <p align="center"><b>Input A</b></td>
      <td colspan="2" align="center"><input type="submit" value="Test" /></td>
      <td style="width: 45%">
      <p align="center"><b>Input B</b> </td>
    </tr>
    <tr>
      <td colspan="2"><textarea name="a" rows="3" cols="10" style="width: 100%; height: 100%"><%=setA%></textarea></td>
      <td colspan="2"><textarea name="b" rows="3" cols="10" style="width: 100%; height: 100%"><%=setB%></textarea></td>
    </tr>
    <tr>
      <td align="center" colspan="2"><b>Only in A: <%=nf.format(a_bSize)%> Code Points</b></td>
      <td align="center" colspan="2"><b>Only in B: <%=nf.format(b_aSize)%> Code Points</b></td>
    </tr>
    <tr>
      <td colspan="2"><%=a_b%>&nbsp;</td>
      <td colspan="2"><%=b_a%>&nbsp;</td>
    </tr>
    <tr>
      <td colspan="4">
      <p align="center"><b>In both A and B: <%=nf.format(abSize)%> Code Points</b></td>
    </tr>
    <tr>
      <td colspan="4"><%=ab%>&nbsp;</td>
    </tr>
  </table>
</form>
<p>Built with ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>

</body>

</html>
