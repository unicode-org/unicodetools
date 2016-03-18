<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: UnicodeSet Comparison</title>
</head>
<body>
<%
		request.setCharacterEncoding("UTF-8"); 
		boolean abbreviate = request.getParameter("abb") != null;

		String setA = request.getParameter("a");
		if (setA == null) {
			setA = "[:Lowercase:]";
		}
		String setB = request.getParameter("b");
		if (setB == null) {
			setB = "[:Lowercase_Letter:]";
		}
		
		String[] abResults = new String[3];
		String[] abLinks = new String[3];
		int[] abSizes = new int[3];
		UnicodeJsp.getDifferences(setA, setB, abbreviate, abResults, abSizes, abLinks);
		
		NumberFormat nf = NumberFormat.getIntegerInstance();
%>
<h1>Unicode Utilities: UnicodeSet Comparison</h1>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/unicodeset"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<form name="myform" action="unicodeset.jsp" method="POST">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input A</th>
      <th style="width: 50%">Input B</th>
    </tr>
    <tr>
      <td ><textarea name="a" rows="8" cols="10" style="width: 100%"><%=setA%></textarea></td>
      <td ><textarea name="b" rows="8" cols="10" style="width: 100%"><%=setB%></textarea></td>
    </tr>
    <tr>
      <td colspan="2"><input type="submit" value="Compare" />
      <input type="checkbox" <%=abbreviate ? "checked" : ""%> name="abb"><label for="abb">abbreviate</label></td>
    </tr>
    <tr>
      <th align="center" ><a target="list" href='<%=abLinks[0]%>'>Only in A</a>: <%=nf.format(abSizes[0])%> Code Points</th>
      <th align="center" ><a target="list" href='<%=abLinks[1]%>'>Only in B</a>: <%=nf.format(abSizes[1])%> Code Points</th>
    </tr>
    <tr>
      <td ><%=abResults[0]%>&nbsp;</td>
      <td ><%=abResults[1]%>&nbsp;</td>
    </tr>
    <tr>
      <th colspan="2"><a target="list" href='<%=abLinks[2]%>'>In both A and B</a>: <%=nf.format(abSizes[2])%> Code Points</th>
    </tr>
    <tr>
      <td colspan="2"><%=abResults[2]%>&nbsp;</td>
    </tr>
  </table>
</form>
<%@ include file="footer.jsp" %>
</body>

</html>
