<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 5.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Unicode Break Demo</title>
<%@ page import="com.ibm.icu.text.*" %> <%@ page import="java.util.regex.*" %>
<style>
<!--
td {vertical-align: top}
span.break   { border-right: 1px solid red;}
-->
</style>
</head>

<body>

<%
		//request.setCharacterEncoding("UTF-8");

		String text = request.getParameter("a");
		if (text == null) text = "Sample Text.";
		String choice = request.getParameter("D1");
		if (choice == null) choice = "Word";
%>
<h1>Unicode Break Demo</h1>
<form name="myform" action="http://unicode.org/cldr/utility/breaks.jsp" method="POST">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse;">
    <tr>
      <td style="width:50%"><b>Input </b></td>
      <td style="width:50%"><select size="1" name="D1" onchange="document.myform.submit();">
      <option <%= (choice.equals("User Character") ? "selected" : "")%>>User Character</option>
      <option <%= (choice.equals("Word") ? "selected" : "")%>>Word</option>
      <option <%= (choice.equals("Line") ? "selected" : "")%>>Line</option>
      <option <%= (choice.equals("Sentence") ? "selected" : "")%>>Sentence</option>
      </select> <input type="submit" value="Test" /></td>
    </tr>
    <tr>
      <td><textarea name="a" rows="10" cols="10" style="width:100%; height:100%"><%=text%></textarea></td>
      <td>
      <%	
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
"([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]]) > &hex/xml($1) ; ";

Transliterator toHTML = Transliterator.createFromRules(
        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
		
RuleBasedBreakIterator b;
if (choice.equals("Word")) b = (RuleBasedBreakIterator) BreakIterator.getWordInstance();
else if (choice.equals("Line")) b = (RuleBasedBreakIterator) BreakIterator.getLineInstance();
else if (choice.equals("Sentence")) b = (RuleBasedBreakIterator) BreakIterator.getSentenceInstance();
else b = (RuleBasedBreakIterator) BreakIterator.getCharacterInstance();

Matcher decimalEscapes = Pattern.compile("&#(x)?([0-9]+);").matcher(text);
// quick hack, since hex-any doesn't do decimal escapes
int start = 0;
StringBuffer result2 = new StringBuffer();
while (decimalEscapes.find(start)) {
	int radix = 10;
	int code = Integer.parseInt(decimalEscapes.group(2), radix);
	result2.append(text.substring(start,decimalEscapes.start()) + UTF16.valueOf(code));
	start = decimalEscapes.end();
}
result2.append(text.substring(start));
text = result2.toString();

int lastBreak = 0;
StringBuffer result = new StringBuffer();
b.setText(text);
b.first();
for (int nextBreak = b.next(); nextBreak != b.DONE; nextBreak = b.next()) {
	int status = b.getRuleStatus();
	String piece = text.substring(lastBreak, nextBreak);
	piece = toHTML.transliterate(piece);
	piece = piece.replaceAll("&#xA;","<br>");
	result.append("<span class='break'>").append(piece).append("</span>");
	lastBreak = nextBreak;
}

out.println(result.toString());
%>&nbsp;</td>
    </tr>
  </table>
</form>
<p>Built with ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>

</body>

</html>
