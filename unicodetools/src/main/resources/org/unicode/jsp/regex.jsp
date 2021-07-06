<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Regex</title>
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
            
        String regex = utfParameters.getParameter("a");
        if (regex == null) {
            regex = "\\p{Nd}+([[:WB=MB:][:WB=MN:]]\\p{Nd}+)?";
        }
        
        String fixedRegex;
        try {       
            fixedRegex = org.unicode.jsp.UnicodeRegex.fix(regex);
            org.unicode.jsp.UnicodeRegex.compile(regex); // just to get the error message
            testPattern = UnicodeJsp.showRegexFind(fixedRegex, test);        
        } catch (Exception e) {
            fixedRegex = e.getMessage();
        }
%>
<h1>Unicode Utilities: Regex</h1>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/regex"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=regex%></textarea></td>
    </tr>
    <tr>
      <th style="width: 50%">TestText</th>
    </tr>
    <tr>
      <td><textarea name="b" rows="8" cols="10" style="width: 100%"><%=test%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show Modified Regex Pattern" onClick="window.location.href='regex.jsp?a='+document.getElementById('main').value"/>
</form>
  <hr>
  <h2>Modified Regex Pattern</h2>
  <p><%=fixedRegex%></p>
  <hr>
  <h2>Underlined Find Values</h2>
  <p><%=testPattern%></p>
<%@ include file="footer.jsp" %>
</body>
</html>
