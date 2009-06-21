<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Language ID Demo</title>
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
            
        String languageCode = utfParameters.getParameter("a");
        if (languageCode == null) {
            languageCode = "gsw-Arab-AQ";
        }
        
        String choice = request.getParameter("l");
        if (choice == null) choice = "en";
        
        String table = UnicodeUtilities.validateLanguageID(languageCode, choice); 
%>
<h1>Unicode Language Identifier Demo</h1>
<%@ include file="others.jsp" %>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th style="width: 50%">Input</th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=languageCode%></textarea></td>
    </tr>
  </table>
  <select size="1" name="l" onchange="document.myform.submit();">
      <option <%= (choice.equals("en") ? "selected" : "")%>>en</option>
      <option <%= (choice.equals("de") ? "selected" : "")%>>de</option>
      <option <%= (choice.equals("fr") ? "selected" : "")%>>fr</option>
      <option <%= (choice.equals("it") ? "selected" : "")%>>it</option>
      <option <%= (choice.equals("es") ? "selected" : "")%>>es</option>
      <option <%= (choice.equals("pt") ? "selected" : "")%>>pt</option>
      <option <%= (choice.equals("zh") ? "selected" : "")%>>zh</option>
      <option <%= (choice.equals("ja") ? "selected" : "")%>>ja</option>
      <option <%= (choice.equals("hi") ? "selected" : "")%>>hi</option>
  </select>
  
  <input id='main' type="submit" value="Show Status" onClick="window.location.href='languageid.jsp?a='+document.getElementById('main').value"/>
</form>
  <h2>Status</h2>
  <p><%=table%></p>
  <p><a href='http://www.unicode.org/reports/tr35/#Unicode_Language_and_Locale_Identifiers'>Unicode language ids</a>
   are based on BCP 47, but not exactly the same.
<%@ include file="footer.jsp" %>
</body>
</html>
