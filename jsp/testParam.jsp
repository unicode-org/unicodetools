<%@page contentType="text/html;charset=UTF-8" pageEncoding="ISO-8859-1"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.net.URLDecoder"%>
<%
request.setCharacterEncoding("UTF-8");
response.setContentType("text/html;charset=UTF-8"); //this is redundant
URLDecoder decoder = new URLDecoder();

String qParam = request.getParameter("q");
String queryString = request.getQueryString();
String queryString2 = decoder.decode(queryString);

%>
<HTML>
<BODY>
Query is: <%=queryString%><br>
Query2 is: <%=queryString2%><br>
Parameter q is: <%=qParam%><br>
Parameter q URL encoded: <%=URLEncoder.encode(qParam, "UTF-8")%>
<form action="queryParameterTest.jsp" method="get">
<input type="text" name="q" value="<%=qParam%>">
<input type="submit">
</form>
<font size="-2">To keep this test simple, proper handling for
&amp; &lt; &gt; and &quot; is not included in this program,
so avoid typing those characters into the box above</font>
</body>