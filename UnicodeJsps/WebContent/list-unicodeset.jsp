<html>
<body>
<p>This page has been disabled, pending resolution of unusual load problems.</p> 
</body>
</html>
<!--  
<--%@ include file="header.jsp" %>
<title>Unicode Utilities: UnicodeSet</title>
</head>
<body>
<--%
		request.setCharacterEncoding("UTF-8");
		//response.setContentType("text/html;charset=UTF-8"); //this is redundant
		String queryString = request.getQueryString();
		
		UtfParameters utfParameters = new UtfParameters(queryString);
		
		String setA = utfParameters.getParameter("a", "[:ASCII:]");
		String group = utfParameters.getParameter("g", "");
		String info = utfParameters.getParameter("i", "");
		
		boolean abbreviate = request.getParameter("abb") != null;
		boolean collate = request.getParameter("c") != null;
        boolean ucdFormat = request.getParameter("ucd") != null;
        boolean escape = request.getParameter("esc") != null;
		
		UnicodeSet a = new UnicodeSet();
		String a_out = UnicodeJsp.getSimpleSet(setA, a, abbreviate, escape);
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
		String sizeStr = nf.format(a.size());
		//   action="http://unicode.org/cldr/utility/list-unicodeset.jsp" method="POST"
%>
<h1>Unicode Utilities: UnicodeSet </h1>
<--%@ include file="subtitle.jsp" %>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/list-unicodeset"><b>help</b></a> | <--%@ include file="others.jsp" %></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><--%=setA%></textarea></td>
    </tr>
    <tr>
      <td>
      <input id='main' type="submit" value="Show Set" onClick="window.location.href='list-unicodeset.jsp?a='+document.getElementById('main').value"/>&nbsp;&nbsp;
      <input type="checkbox" <--%=abbreviate ? "checked" : ""%> name="abb"><label for="abb">Abbreviate</label>&nbsp;&nbsp;
      <input type="checkbox" <--%=collate ? "checked" : ""%> name="c"><label for="c">Collate</label>&nbsp;&nbsp;
      <input type="checkbox" <--%=ucdFormat ? "checked" : ""%> name="ucd"><label for="ucd">UCD format</label>&nbsp;&nbsp;
      <input type="checkbox" <--%=escape ? "checked" : ""%> name="esc"><label for="esc">Escape</label>&nbsp;&nbsp;
      <label for="g">Group by:</label>
      <input type="text" <--%=escape ? "checked" : ""%> name="g" size="25" value="<--%=group%>">
      <label for="i">Info:</label>
      <input type="text" <--%=escape ? "checked" : ""%> name="i" size="25" value="<--%=info%>">
      </td>
    </tr>
</table>
  <p><--%= sizeStr %> Code Points</p>
  <hr>
  <p><--%=a_out%></p>
  <hr>
  <--% UnicodeJsp.showSet(group, info, a, abbreviate, ucdFormat, collate, out); %>
</form>
<--%@ include file="footer.jsp" %>
</body>
</html>
-->
