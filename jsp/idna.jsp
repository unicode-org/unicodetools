<html>
<head>
<%@ include file="header.jsp" %>
<title>IDNA Demo</title>
</head>

<body>
<%
        request.setCharacterEncoding("UTF-8");
        //response.setContentType("text/html;charset=UTF-8"); //this is redundant
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String IDNA2008 = utfParameters.getParameter("a");

        if (IDNA2008 == null) {
IDNA2008 = "\u00D6BB\n"
        + "O\u0308BB\n"
        + "Sch\u00E4ffer\n"
        + "\uFF21\uFF22\uFF23\u30FB\u65E5\u672C\n"
        + "\u0221og\n"
        + "x\u0301\u0327\n"
        + "I\u2665NY\n"
        + "fa\u00DF\n"
        + "\u0392\u03CC\u03BB\u03BF\u03C2\n"
        + "\uFECB\uFEAE\uFE91\uFEF2";
        }
        String filter = utfParameters.getParameter("f", "[\u00DF \u03C2 [:Join_C:]]");
        String fixedRegex = UnicodeUtilities.testIdnaLines(IDNA2008, filter);
%>
<h1>Unicode IDNA Demo</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input Labels: (See also <a target="picker" href="http://macchiato.com/picker/MyApplication.html">Picker</a>)</th>
      <th style="width: 50%">Exclude from NFKC-CF-RDI mapping:</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="12" cols="10" style="width: 100%"><%=IDNA2008%></textarea></td>
      <td><textarea name="f" rows="12" cols="10" style="width: 100%"><%=filter%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show IDNA Status" onClick="window.location.href='idna.jsp?a='+document.getElementById('main').value"/>
<a target="rules" href="idnaContextRules.txt">Context Rules</a>
</form>
  <hr>
  <h2>Modified IDNA2008 Pattern</h2>
  <p><%=fixedRegex%></p>
<p>* = Putative labels. If there are accents or invisible characters they are shown on a second line with \u escapes,
to show the difference between cases like &#x00D6; and O +  &#x0308;</p>
<%@ include file="footer.jsp" %>
</body>
</html>
