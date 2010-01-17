<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Confusables</title>
</head>
<body>
<%
		request.setCharacterEncoding("UTF-8");
		//response.setContentType("text/html;charset=UTF-8"); //this is redundant
		String queryString = request.getQueryString();
		
		UtfParameters utfParameters = new UtfParameters(queryString);
		
		String test = utfParameters.getParameter("a", "paypal");
		boolean nfkcCheck = utfParameters.getParameter("n") != null;
        boolean scriptCheck = request.getParameter("s") != null;
        boolean idCheck = request.getParameter("i") != null;
        boolean xidCheck = request.getParameter("x") != null;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeJsp.getConfusables(test, nfkcCheck, scriptCheck, idCheck, xidCheck);
		
%>
<h1>Unicode Utilities: Confusables</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
      <td rowSpan='2'><i>This demo takes an Input string and produces combinations that are confusable with it,
        using data collected by the Unicode consortium. Confusable characters are those that may be confused with others (in some common UI fonts),
        such as the Latin letter "o" and the Greek letter omicron "&#x03BF;". Fonts make a difference: for example,
        the Hebrew character "&#x05E1;" looks confusingly similar to "o" in some fonts (such as Arial Hebrew), but not in others.
        See also unaccented <a target="list" href='list-unicodeset.jsp?\p{latin}-\p{nfkdqc%3Dn}'>Latin Characters.</a>.</i>
      </td>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%; height: 100%"><%=test%></textarea></td>
    </tr>
    <tr>
      <td>
      <input id='main' type="submit" value="Show" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;&nbsp;
      <input type="checkbox" <%=nfkcCheck ? "checked" : ""%> name="n"><label for="n" title='no compatibility characters'>NFKC Only</label>&nbsp;&nbsp;
      <!--<input type="checkbox" <%=idCheck ? "checked" : ""%> name="i"><label for="i" title='only Unicode identifiers'>Id Chars Only</label>&nbsp;&nbsp;
      -->
      <input type="checkbox" <%=xidCheck ? "checked" : ""%> name="x"><label for="x" title='only restricted Unicode identifiers'>Restricted Identifiers Only</label>&nbsp;&nbsp;
      <input type="checkbox" <%=scriptCheck ? "checked" : ""%> name="s"><label for="s" title='no mixed scripts'>No Mixed Scripts</label>&nbsp;&nbsp;
      </td>
      <td><i>You can try different combinations of the checkboxes to see the results: filtering most compatibility characters,
      non-restricted-identifiers,
      and/or mixed scripts. For more info, see <a href='#data'>data</a></i>.
      </td>
    </tr>
  </table>
</form>
<div style='margin:1em; font-family:sans-serif,"Arial Unicode MS"'><%=a_out%></div>
<hr>
<h2><a name="data">Data</a></h2>
<ul>
<li><b>NFKC Only</b> removes Unicode compatibility characters that are variants of other characters.</li>
<li><b>Restricted Identifiers Only</b> limits characters to those in the
 <a href='http://www.unicode.org/Public/security/revision-03/xidmodifications.txt' target="_blank">xidmodifications</a> file, 
 which are modifications to the regular Unicode Identifier recommendations. They exclude historic scripts and characters, as well as technical characters, symbols, and others.
</li>
<li><b>No Mixed Scripts</b> disallows any resulting string that contains mixed scripts, with the exception of Common and Inherited.
</li>
</ul>
<p>The Unicode data is draft. You can <a href='http://www.macchiato.com/unicode/security-mechanisms' target="_blank">suggest additions to the Unicode data</a>.</p>
<ul>
<li>The confusables data is in the <a href='http://www.unicode.org/Public/security/revision-03/confusablesSummary.txt' target="_blank">confusablesSummary</a> file.</li>
<li>The restricted ID data is the <a href='http://www.unicode.org/Public/security/revision-03/xidmodifications.txt' target="_blank">xidmodifications</a> file.</li>
</ul>
<p>For more information on the use of the data,
see proposed updates <a href='http://www.unicode.org/reports/tr39/tr39-3.html' target="_blank">Unicode Security Mechanisms</a> and
<a href='http://www.unicode.org/reports/tr36/tr36-8.html' target="_blank">Unicode Security Considerations</a>.
</p>
<h2>Caveats</h2> 
<p>The Unicode data is designed for testing, not enumerating, so not all combinations are generated in this demo.
In particular, the results are in canonical form (<a href='http://unicode.org/glossary/#normalization_form_c'>NFC</a>), so canonical variants do not show up.
Also, where a character is confusable with a sequence, not all combinations are generated.
The "mixed script" test needs to be changed to allow the levels in 
<a href='http://www.unicode.org/reports/tr36/tr36-8.html#Security_Levels_and_Alerts' target="_blank">Unicode Security Considerations (proposed update)</a>.
An additional checkbox needs to be added to restrict to IDNA2008 characters (although in general those are a superset of the Restricted Identifiers).
</p>
<br>
<%@ include file="footer.jsp" %>
</body>
</html>
