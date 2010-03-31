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
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/properties"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<%
        request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String propForValues = utfParameters.getParameter("a");
		UnicodeJsp.showPropsTable(out, propForValues, "properties.jsp");
%>
<p>The list includes both Unicode Character Properties and some additions (like idna2003 or subhead)</p>
<%@ include file="footer.jsp" %>
</body>
</html>