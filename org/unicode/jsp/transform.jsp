<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Transforms</title>
</head>
<body>
<%
        request.setCharacterEncoding("UTF-8");
        //response.setContentType("text/html;charset=UTF-8"); //this is redundant
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String transform = utfParameters.getParameter("a", "nfkc; Greek");
        String sample = utfParameters.getParameter("b", "The Quick Brown Fox?\n\u24B6\u24D1\u24D2\u24D3");
        boolean show = !"off".equals(utfParameters.getParameter("show", "off"));
%>
<h1>Unicode Utilities: Transforms</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Transform Rules</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=transform%></textarea></td>
    </tr>
    <tr>
      <th style="width: 50%">Sample</th>
    </tr>
    <tr>
      <td><textarea name="b" rows="8" cols="10" style="width: 100%"><%=sample%></textarea></td>
    </tr>
</table>
<input id='main' type="submit" value="Show Transform" onClick="window.location.href='transform.jsp?a='+document.getElementById('main').value"/>
<input type="checkbox" <%=show ? "checked" : ""%> name="show"><label for="show">Show Transforms</label></td>
  <hr>
  <h2>Result</h2>
  <%= UnicodeJsp.showTransform(transform, sample)%>
  <%= show ? UnicodeJsp.listTransforms() : "" %>
</form>
<%@ include file="footer.jsp" %>
</body>

</html>
