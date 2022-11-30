<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Character Properties</title>
<link rel="stylesheet" type="text/css" href="index.css">
<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Utilities: Character Properties</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="https://unicode-org.github.io/unicodetools/help/character"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<div style='text-align:center'>
<form name="myform" action="character.jsp" method="get">
  <%
		request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();

        UtfParameters utfParameters = new UtfParameters(queryString);
		String text = utfParameters.getParameter("a", "\u2615", "\u2615");

		int[] codePoints = UnicodeJsp.parseCode(text,null,null);
		int cp = codePoints[0];
    String nextHex = "character.jsp?a=" + Utility.hex(cp < 0x110000 ? cp+1 : 0, 4);
		String prevHex = "character.jsp?a=" + Utility.hex(cp > 0 ? cp-1 : 0x10FFFF, 4);
    if (codePoints.length > 1) {
      %>
        <p class="error">
          Multiple code points or unrecognized hexadecimal syntax.
        </p>
        <p>
          This tool shows the properties of individual code points.
          Code points may be entered directly (<code>&#x2615;</code>), as bare hexadecimal
          digits (<code>2615</code>), in U+ notation (<code>U+2615</code>), or in a variety
          of hexadecimal escape sequence and numeric literal syntaxes
          (<code>\u{2615}</code>, <code>&amp;#x2615;</code>, <code>0x2615</code>, etc.).
        </p>
        <p>
          Showing the properties of the first code point. See the properties of
      <%
      for (int i = 1; i < codePoints.length; ++i) {
        String digits = String.format("%04X", codePoints[i]);
        String escaped = UnicodeUtilities.toHTML(UTF16.valueOf(codePoints[i]));
        String codePoint = "U+" + digits;
        if (escaped != "") {
          codePoint += " (" + escaped + ")";
        }
        %>
        <a href="?a=<%=digits%>"><%=codePoint%></a><%=i==codePoints.length - 1 ? "." : "," %>
        <%
      }
      %>
        </p>
      <%
    }
%>
  <p>
  <input name="B3" type="button" value="-" onClick="window.location.href='<%=prevHex%>'">
  <input name="a" type="text" style='text-align:center; font-size:150%' size="10" value="<%=UnicodeUtilities.toHTMLInput(text)%>">
  <input name="B2" type="button" value="+" onClick="window.location.href='<%=nextHex%>'"><br>
  <input name="B1" type="submit" value="Show">
  </p>
</form>
<%
	UnicodeJsp.showProperties(cp, out);
%>
</div>
<p>The list includes both Unicode Character Properties and some additions (like idna2003 or subhead)</p>
<%@ include file="footer.jsp" %>
</body>

</html>
