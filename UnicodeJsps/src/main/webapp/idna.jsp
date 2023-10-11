<%@ page import="org.owasp.encoder.Encode" %>
<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Internationalized Domain Names (IDN)</title>
</head>

<body>
<%
        request.setCharacterEncoding("UTF-8");
        //response.setContentType("text/html;charset=UTF-8"); //this is redundant
        String queryString = request.getQueryString();

        UtfParameters utfParameters = new UtfParameters(queryString);

        String IDNA2008 = utfParameters.getParameter("a");

        if (IDNA2008 == null) {
            IDNA2008 = UnicodeJsp.getDefaultIdnaInput();
        }
        String fixedRegex = UnicodeJsp.testIdnaLines(IDNA2008, "");
%>
<h1>Unicode Utilities: Internationalized Domain Names (IDN)</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="https://unicode-org.github.io/unicodetools/help/idna"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th>Enter International Domain Names:</th>
      <th class='r'>For special characters, you can use <a target="picker" href="http://macchiato.com/picker/MyApplication.html">Picker</a></th>
    </tr>
    <tr>
      <td colSpan='2'><textarea name="a" rows="12" cols="10" style="width: 100%"><%=Encode.forHtmlContent(IDNA2008)%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show IDNA Status" onClick="window.location.href='idna.jsp?a='+document.getElementById('main').value"/>
</form>
  <hr>
  <h2>Results (see <a href='#notes'>Notes</a>)</h2>
  <p><%=fixedRegex%></p>
<hr>
<h2 id='notes'>Notes</h2>
<ol>
<li><a href='http://unicode.org/reports/tr46'>Unicode UTS #46: Unicode IDNA Compatibility Processing</a>
is designed to allow implementations to support both IDNA2008 and IDNA2003,
without the compatibility problems resulting from the conflicts between them.</li>
<li>Errors in labels are shown with red; the results may show &#xFFFD; if they are not determinant.</li>
<li>The input can have hex Unicode, using the \u convention. For example, &#x2665; can be supplied as \u2665.
If there are accents or invisible characters they are shown on a second line with \u escapes,
to show the difference between cases like &#x00D6; and O +  &#x0308;</li>
<li>The Punycode shown in the Input column is raw - without any mapping or transformation,
but breaking at dots (full stops and ideographic full stops), but not those in characters like 'U+2490 ( &#x2490; ) DIGIT NINE FULL STOP')</li>
<li>The behavior with of browsers with composed single characters like 'U+2490 ( &#x2490; ) DIGIT NINE FULL STOP'
(in <a target='_blank' href='http://lab&#x2490;be'>http://lab&#x2490;be</a>) varies:
<ol>
<li>Chrome, Safari, and IE will fail to resolve http://lab&#x2490;be.</li>
<li>Firefox maps '&#x2490;' to "9" + "." <i>before</i> separating labels; thus resolving http://lab&#x2490;be.</li>
</ol></li>
</ol>
<%@ include file="footer.jsp" %>
</body>
</html>
