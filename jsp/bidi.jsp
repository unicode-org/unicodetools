<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Bidi Demo</title>
</head>
<body>
<%
        request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String sample = utfParameters.getParameter("a", "mark 3.1% \u0645\u0627\u0631\u0643\u0652 2.0.");
        boolean hack = !"off".equals(utfParameters.getParameter("hack", "off"));
        boolean dir = !"off".equals(utfParameters.getParameter("dir", "off"));
%>
<h1>Unicode Bidi Demo</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th>Sample</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=sample%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show Bidi" onClick="window.location.href='transform.jsp?a='+document.getElementById('main').value"/>
<input type="checkbox" <%=hack ? "checked" : ""%> name="hack"><label for="hack">ASCII Hack?</label></td>
<input type="checkbox" <%=dir ? "checked" : ""%> name="dir"><label for="dir">LTR Paragraph?</label></td>
  <hr>
  <h2>Result</h2>
  <%= UnicodeUtilities.showBidi(sample, dir ? 0 : 1, hack)%>
</form>
<%@ include file="footer.jsp" %>
</body>

</html>
