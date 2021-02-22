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
		String testEscaped = UnicodeUtilities.toHTML(test);
		
		String p = utfParameters.getParameter("r", "None");
        int p2 = p.equals("None") ? 0 
            : p.equals("IDNA2003") ? 1
            : p.equals("IDNA2008") ? 2
            : 3;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeJsp.getConfusables(test, p2);
		
%>
<h1>Unicode Utilities: Confusables</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/confusables"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
      <td rowSpan='3'><i>With this demo, you can supply an Input string and see the combinations that are confusable with it,
        using data collected by the Unicode consortium. You can also try different restrictions, using characters valid in
        different approaches to international domain names. For more info, see <a href='#data'>Data</a> below.</i>
      </td>
    </tr>
    <tr>
      <td>
      <input name="a" type="text" value="<%=testEscaped%>" style="width: 100%">
      <!-- <textarea name="a" rows="8" cols="5" style="width: 100%; height: 100%"><%=testEscaped%></textarea> -->
      </td>
    </tr>
    <tr>
      <td>
       <label for="r">Restriction</label>
      <select name="r">
        <option<%= p2==0 ? " selected" : ""%>>None</option>
        <option<%= p2==1 ? " selected" : ""%>>IDNA2003</option>
        <option<%= p2==2 ? " selected" : ""%>>IDNA2008</option>
        <option<%= p2==3 ? " selected" : ""%>>UTS46+UTS39</option>
       </select>
       <input id='main' type="submit" value="Show" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;&nbsp;
      </td>
    </tr>
  </table>
</form>
<div style='margin:1em; font-family:sans-serif,"Arial Unicode MS"'><%=a_out%></div>
<hr>
<h2><a name="data">Data</a></h2>
<p>
 Confusable characters are those that may be confused with others (in some common UI fonts),
        such as the Latin letter "o" and the Greek letter omicron "&#x03BF;". Fonts make a difference: for example,
        the Hebrew character "&#x05E1;" looks confusingly similar to "o" in some fonts (such as Arial Hebrew), but not in others.
        See also unaccented <a target="list" href='list-unicodeset.jsp?\p{latin}-\p{nfkdqc%3Dn}'>Latin Characters.</a>.</i>
        </p>
<p>The data for confusables and restrictions is from UTS39. You can <a href='http://www.macchiato.com/unicode/security-mechanisms' target="_blank">suggest
 additions or changes</a> to the Unicode data for future versions of that standard.</p>
<ul>
<li>The confusables data is in the <a href='http://www.unicode.org/Public/security/revision-03/confusablesSummary.txt' target="_blank">confusablesSummary</a> file.</li>
<li>The restricted ID data is the <a href='http://www.unicode.org/Public/security/revision-03/xidmodifications.txt' target="_blank">xidmodifications</a> file.</li>
<li>The "UTS46+UTS39" test checks mixed scripts according to the levels in 
<a href='http://www.unicode.org/reports/tr36/tr36-8.html#Security_Levels_and_Alerts' target="_blank">Unicode Security Considerations (proposed update)</a>,
such as "a&#x3B1;", "a&#x660;b", or "&#x13A0;a1". 
It also checks for Hans vs Hant (&#x4E07;&#x4E1F;); for different number systems(1&#x666;, &#x666;&#x6F6;); and for duplicate non-spacing marks (&#xE4;&#x0308;).

</ul>
<p>For more information on the use of the data,
see proposed updates <a href='http://www.unicode.org/reports/tr39/tr39-3.html' target="_blank">Unicode Security Mechanisms</a> and
<a href='http://www.unicode.org/reports/tr36/tr36-8.html' target="_blank">Unicode Security Considerations</a>.
</p>
<p>The restrictions are purely on a character level. For a more detailed view, see <a target="idna" href="idna.jsp">idna</a>.
<h2>Caveats</h2> 
<p>The Unicode data is designed for testing, not enumerating, so not all combinations are generated in this demo;
In particular, where a character is confusable with a sequence, not all combinations are generated.
</p>
<br>
<%@ include file="footer.jsp" %>
</body>
</html>
