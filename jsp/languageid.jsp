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
        
        String options = UnicodeUtilities.getLanguageOptions(choice); 
        
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
      <td><input type="text" name="a" rows="8" cols="10" style="width: 100%" value="<%=languageCode%>"/></td>
    </tr>
  </table>
  
  <input id='main' type="submit" value="Show Status" onClick="window.location.href='languageid.jsp?a='+document.getElementById('main').value"/>
  
  Localization:
  <select size="1" name="l" onchange="document.myform.submit();">
      <%=options%>
  </select>
</form>
  <h2>Status</h2>
  <p><%=table%></p>
  <h3>Notes</h3>
     <ul><li>Unicode language ids are based on BCP 47,
       but <a href='http://www.unicode.org/reports/tr35/#Unicode_Language_and_Locale_Identifiers' target="uts35">differ in a few ways</a>.</li>
     <li>The names are localized with <a href='http://cldr.unicode.org/'>Unicode CLDR data</a>: names with '*' are fallbacks to English;
       names with '**' are fallbacks to the <a href='http://tools.ietf.org/html/draft-ietf-ltru-4645bis' target="bcp47bis">latest draft registry</a> names.</li>
     <li>Replacements are for invalid subtags (zho &rarr; zh, 248 &rarr; AX), or preferred replacements (iw &rarr; he), or 
     <a href='http://cldr.unicode.org/development/design-proposals/languages-to-show-for-translation#TOC-Macrolanguage-Table' target='cldr'>predominant languages</a> (arb &rarr; ar).</li>
   </ul>
<%@ include file="footer.jsp" %>
</body>
</html>
