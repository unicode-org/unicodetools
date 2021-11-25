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
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="https://unicode-org.github.io/unicodetools/help/properties"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<%
        request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();
        UtfParameters utfParameters = new UtfParameters(queryString);

        String propForValues = utfParameters.getParameter("a");
		UnicodeJsp.showPropsTable(out, propForValues, "properties.jsp");
%>
<h2>Key</h2>
<p>The Categories are from <a target='_blank' href='http://www.unicode.org/reports/tr44#Property_Index'>UCD
Table 8. Property Summary Table</a>, with some extended categories:
Emoji, IDNA, Regex, Security, and UCA.</p>
<p>The Datatypes are from <a target='_blank' href='http://www.unicode.org/reports/tr44#Type_Key_Table'>UCD Table 5. Property Type Key</a>.</p>
<p>The Sources are:
<ul>
<li>UCD - <a target='_blank' href='http://www.unicode.org/reports/tr44/'>Unicode Character Database</a></li>
<li>Unicode - The Unicode Standard. For example, <a target='_blank' href='http://www.unicode.org/versions/Unicode5.2.0/ch03.pdf#G47728'>isLowercase</a>
or <a target='_blank' href='http://www.unicode.org/reports/tr15/#Notation'>toNFC</a></li>
<li>UTS - a Unicode Technical Standard. For example, identifier-restriction is from UTS #39, and alnum from UTS #18.</li>
<li>X-ICU - from <a target='_blank' href='http://site.icu-project.org/'>ICU</a>:
Typically a derived property, such as <a target='_blank' href='http://icu-project.org/apiref/icu4j/index.html?com/ibm/icu/lang/UProperty.html'>Case Sensitive</a>.</li>
<li>X-Demo - properties purely for demonstration purposes.</li>
</ul>
</p>
<%@ include file="footer.jsp" %>
</body>
</html>
