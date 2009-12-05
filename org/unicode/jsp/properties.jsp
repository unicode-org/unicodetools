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

		UnicodeJsp.showPropsTable(out);
%>
<p>The list includes both Unicode Character Properties and some additions (like idna2003 or subhead)</p>
<%@ include file="footer.jsp" %>
</body>
</html>