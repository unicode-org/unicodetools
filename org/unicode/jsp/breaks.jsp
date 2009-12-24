<html>

<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Breaks (Segmentation)</title>
<style>
<!--
td {vertical-align: top}
span.break   { border-right: 1px solid red;}
-->
</style>
</head>

<body>

<%
		request.setCharacterEncoding("UTF-8");

		String text = request.getParameter("a");
		if (text == null) text = "Sample Text.";
		String choice = request.getParameter("D1");
		if (choice == null) choice = "Word";
%>
<h1>Unicode Utilities: Breaks (Segmentation)</h1>
<%@ include file="others.jsp" %>
<form name="myform" action="<%= request.getContextPath() + request.getServletPath() %>" method="POST">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <td style="width:50%"><b>Input </b></td>
      <td style="width:50%">
      <select size="1" name="D1" onchange="document.myform.submit();">
      <option <%= (choice.equals("User Character") ? "selected" : "")%>>User Character</option>
      <option <%= (choice.equals("Word") ? "selected" : "")%>>Word</option>
      <option <%= (choice.equals("Line") ? "selected" : "")%>>Line</option>
      <option <%= (choice.equals("Sentence") ? "selected" : "")%>>Sentence</option>
      </select>
      <input type="submit" value="Test" /></td>
    </tr>
    <tr>
      <td><textarea name="a" rows="30" cols="30" style="width:100%; height:100%"><%=text%></textarea></td>
      <td>
      <%=UnicodeJsp.showBreaks(text, choice)%>&nbsp;</td>
    </tr>
  </table>
</form>
<%@ include file="footer.jsp" %>
</body>

</html>
