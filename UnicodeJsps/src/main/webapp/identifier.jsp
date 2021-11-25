<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Identifier</title>
</head>
<body>
<%
		request.setCharacterEncoding("UTF-8");
		//response.setContentType("text/html;charset=UTF-8"); //this is redundant
		String queryString = request.getQueryString();

		UtfParameters utfParameters = new UtfParameters(queryString);

		String test = utfParameters.getParameter("a", "Latin");

		String a_out = UnicodeJsp.getIdentifier(test);

%>
<h1>Unicode Utilities: Identifier</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="https://unicode-org.github.io/unicodetools/help/identifier"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
      <td rowSpan='3'><i>This demo shows the identifier information for characters in a script: identifier restrictions and confusable characters. <b>To suggest additions or modifications, see <a href='http://www.unicode.org/reports/tr39/suggestions.html'>Suggestions</a>.</b></i>
      </td>
    </tr>
    <tr>
      <td>
      <input name="a" type="text" value="<%=test%>" style="width: 100%">
      </td>
    </tr>
    <tr>
      <td>
       <input id='main' type="submit" value="Show" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;&nbsp;
      </td>
    </tr>
  </table>
</form>
<div style='margin:1em'><%=a_out%></div>
<%@ include file="footer.jsp" %>
</body>
</html>
