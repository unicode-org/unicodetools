<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Mapping Demo</title>
</head>
<body>
<%
        request.setCharacterEncoding("UTF-8");
        //response.setContentType("text/html;charset=UTF-8"); //this is redundant
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String mapping = utfParameters.getParameter("a");

        if (mapping == null) {
            mapping = "[:ASCII:]";
        }

%>
<h1>Unicode Mapping Demo</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=mapping%></textarea></td>
    </tr>
    <tr>
      <td><input id='main' type="submit" value="Show Mapping" onClick="window.location.href='mapping.jsp?a='+document.getElementById('main').value"/>&nbsp;
    </tr>
</table>
  <hr>
  <%= UnicodeUtilities.showMapping(mapping)%>
</form>
<%@ include file="footer.jsp" %>
</body>

</html>
