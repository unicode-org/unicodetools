<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>UnicodeSet Demo</title>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="com.ibm.icu.text.*" %> <%@ page import="com.ibm.icu.lang.*" %>
<%@ page import="com.ibm.icu.impl.*" %> <%@ page import="java.util.regex.*" %>
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

String BASE_RULES =
  	"'<' > '&lt;' ;" +
    "'<' < '&'[lL][Tt]';' ;" +
    "'&' > '&amp;' ;" +
    "'&' < '&'[aA][mM][pP]';' ;" +
    "'>' < '&'[gG][tT]';' ;" +
    "'\"' < '&'[qQ][uU][oO][tT]';' ; " +
    "'' < '&'[aA][pP][oO][sS]';' ; ";

String CONTENT_RULES =
    "'>' > '&gt;' ;";

String HTML_RULES = BASE_RULES + CONTENT_RULES + 
"'\"' > '&quot;' ; ";

String HTML_RULES_CONTROLS = HTML_RULES + 
"([[:C:][:Default_Ignorable_Code_Point:]]) > \u25a0 ; "
+ 
":: [\\u0080-\\U0010FFFF] hex/xml ; ";

Transliterator toHTML = Transliterator.createFromRules(
        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
        

		String setA = request.getParameter("a");
		if (setA == null) 
			setA = "[:ASCII:]";

		UnicodeSet a = new UnicodeSet();
		String a_out;
		try {
		     setA = Normalizer.normalize(setA, Normalizer.NFC);
			a = new UnicodeSet(setA);
			if (a.size() < 100000) {
		     	PrettyPrinter pp = new PrettyPrinter();
				a_out = toHTML.transliterate(pp.toPattern(a));
			} else {
				a_out = toHTML.transliterate(a.toString());
			}
		} catch (Exception e) {
			a_out = e.getMessage();
		}
			NumberFormat nf = NumberFormat.getIntegerInstance();
			String sizeStr = nf.format(a.size());
%>
<h1>UnicodeSet Demo (<a target="u" href="unicodeset.jsp">Compare</a>)</h1>
<form name="myform" action="http://unicode.org/cldr/utility/list-unicodeset.jsp" method="POST">
  <p><textarea name="a" rows="3" cols="10" style="width: 100%"><%=setA%></textarea></p>
  <p><input type="submit" value="Show Set" /> <%= sizeStr %> Code Points</p>
  <hr>
  <p><%=a_out%></p>
  <hr>
  <%
          if (a.size() > 50000) out.println("<i>Too many to list individually</i>");
 		else if (a != null) for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.next();) {
			int s = it.codepoint;
			String literal = toHTML.transliterate(UTF16.valueOf(s));
			String hex = com.ibm.icu.impl.Utility.hex(s,4);
			String name = UCharacter.getName(s);
			if (name == null) name = "<i>no name</i>";
			out.println("<code><a target='c' href='character.jsp?a=" + hex + "'>" + hex + "</a></code> ( " + literal
					+ " ) " + name + "<br>");
		}
 %>
</form>
<p>Built with ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>
<p></p>

</body>

</html>
