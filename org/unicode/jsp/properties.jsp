<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Character Property Index</title>
<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Utilities: Character Property Index</h1>
<%@ include file="others.jsp" %>
<%
		request.setCharacterEncoding("UTF-8");

		Set<String> unicodeProps = UnicodeJsp.showPropsTable(out);
%>
<p>® = Regex Property (<a target="_blank" href="http://www.unicode.org/reports/tr18/">UTS #18</a>): 
not formal Unicode property<br>
© = ICU-Only Property (not Unicode or Regex)<br>
<b>Not explicitly in ICU: </b><%=unicodeProps%></p>
<%@ include file="footer.jsp" %>
</body>
</html>