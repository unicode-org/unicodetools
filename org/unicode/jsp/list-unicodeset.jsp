<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: UnicodeSet</title>
</head>
<body>
<%
		request.setCharacterEncoding("UTF-8");
		//response.setContentType("text/html;charset=UTF-8"); //this is redundant
		String queryString = request.getQueryString();
		
		UtfParameters utfParameters = new UtfParameters(queryString);
		
		String setA = utfParameters.getParameter("a");

		if (setA == null) {
			setA = "[:ASCII:]";
		}
		boolean abbreviate = request.getParameter("abb") != null;
        boolean ucdFormat = request.getParameter("ucd") != null;
        boolean escape = request.getParameter("esc") != null;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeUtilities.getSimpleSet(setA, a, abbreviate, escape);
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		String sizeStr = nf.format(a.size());
		//   action="http://unicode.org/cldr/utility/list-unicodeset.jsp" method="POST"
%>
<h1>Unicode Utilities: UnicodeSet</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=setA%></textarea></td>
    </tr>
    <tr>
      <td><input id='main' type="submit" value="Show Set" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;
      <input type="checkbox" <%=abbreviate ? "checked" : ""%> name="abb"><label for="abb">Abbreviate</label>
      <input type="checkbox" <%=ucdFormat ? "checked" : ""%> name="ucd"><label for="ucd">UCD format</label>
      <input type="checkbox" <%=escape ? "checked" : ""%> name="esc"><label for="esc">Escape</label></td>
    </tr>
</table>
  <p><%= sizeStr %> Code Points</p>
  <hr>
  <p><%=a_out%></p>
  <hr>
  <% UnicodeUtilities.showSet(a, abbreviate, ucdFormat, out); %>
</form>
<%@ include file="footer.jsp" %>
</body>
</html>
