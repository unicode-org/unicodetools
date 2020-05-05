<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: BNF-Regex</title>
</head>

<body>
<%
        request.setCharacterEncoding("UTF-8");
        //response.setContentType("text/html;charset=UTF-8"); //this is redundant
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String test = utfParameters.getParameter("b");
        if (test == null) {
          test = "The 35 quick brown fox jumped over 1.234 lazy dogs: 1:234.";
        }
        String testPattern = test;
        
        String bnf = utfParameters.getParameter("a");
        if (bnf == null) {
            bnf = "number = digits (separator digits)?;\n"
            + "digits = \\p{Nd}+;\n"
            + "separator = [[:WB=MB:][:WB=MN:]];";
        }

        String fixedbnf;
        String random = "<i>Error In Pattern</i>";
        try {
            fixedbnf = new org.unicode.jsp.UnicodeRegex().compileBnf(bnf);
            String fixedbnf2 = org.unicode.jsp.UnicodeRegex.fix(fixedbnf);
            String fixedbnfNoPercent = fixedbnf2.replaceAll("[0-9]+%", "");
            testPattern = UnicodeJsp.showRegexFind(fixedbnfNoPercent, test);
            try {
                random = UnicodeJsp.getBnf(fixedbnf, 100, 10);
            } catch (Exception e) {
                random = e.getMessage();
            }
        } catch (Exception e) {
            fixedbnf = e.getMessage();
        }
%>
<h1>Unicode Utilities: BNF-Regex</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/bnf"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=bnf%></textarea></td>
    </tr>
    <tr>
      <th style="width: 50%">TestText</th>
    </tr>
    <tr>
      <td><textarea name="b" rows="8" cols="10" style="width: 100%"><%=test%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show Modified BNF Pattern" onClick="window.location.href='bnf.jsp?a='+document.getElementById('main').value"/>
</form>
  <hr>
  <h2>Modified BNF Pattern</h2>
  <p><%=fixedbnf%></p>
  <hr>
  <h2>Underlined Find Values</h2>
  <p><%=testPattern%></p>
  <hr>
  <h2>Random Generation</h2>
  <%=random%>
<%@ include file="footer.jsp" %>
</body>
</html>
