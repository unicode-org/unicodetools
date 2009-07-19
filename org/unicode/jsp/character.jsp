<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Property Demo</title>
<link rel="stylesheet" type="text/css" href="index.css">
<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Property Demo</h1>
<%@ include file="others.jsp" %>
<form name="myform" action="http://unicode.org/cldr/utility/character.jsp" method="POST">
  <%
		request.setCharacterEncoding("UTF-8");
		String text = request.getParameter("a");
		if (text == null || text.length() == 0) text = "a";
%> <%
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
		"([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]-[\\u0020]]) > &hex/xml($1) ; ";


Transliterator toHTML = Transliterator.createFromRules(
        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
        
		String HTML_INPUT = "::hex-any/xml10; ::hex-any/unicode; ::hex-any/java;";
		Transliterator fromHTML = Transliterator.createFromRules(
				"any-xml", HTML_INPUT, Transliterator.FORWARD);
		
		text = fromHTML.transliterate(text);

		if (text.length() > 2) {
		    try {
			  text = UTF16.valueOf(Integer.parseInt(text,16));
			} catch (Exception e) {}
		}
		int cp = UTF16.charAt(text, 0);
		String nextHex = "character.jsp?a=" + Utility.hex(cp < 0x110000 ? cp+1 : 0, 4);
		String prevHex = "character.jsp?a=" + Utility.hex(cp > 0 ? cp-1 : 0x10FFFF, 4);
%>
  <p><input type="button" value="Previous" name="B3" onClick="window.location.href='<%=prevHex%>'">
  <input type="text" name="a" size="10" value="<%=text%>">
  <input type="submit" value="Show" name="B1">
  <input type="button" value="Next" name="B2" onClick="window.location.href='<%=nextHex%>'"></p>
</form>
<%
	UnicodeUtilities.showProperties(text, out); 
%>
<p><i>(only includes properties with non-default values)<br>
</i>® = Regex Property (<a href="http://www.unicode.org/reports/tr18/">UTS #18</a>): not formal 
Unicode property<br>
© = ICU-Only Property (not Unicode or Regex)<br>
<%@ include file="footer.jsp" %>
</body>

</html>
