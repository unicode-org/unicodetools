<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: Unicode Language Identifers and BCP47</title>
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
        
        String options = UnicodeJsp.getLanguageOptions(choice); 
        
        String table = UnicodeJsp.validateLanguageID(languageCode, choice); 
%>
<h1>Unicode Utilities: Unicode Language Identifers and BCP47</h1>
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
  
  &nbsp;&nbsp;Localization:
  <select size="1" name="l" onchange="document.myform.submit();">
      <%=options%>
  </select>
</form>
<h3>Status</h3>
<%=table%>
<h3>Samples</h3>
<ul>
<li><a href='languageid.jsp?a=en' target='languageid'>en</a></li>
<li><a href='languageid.jsp?a=eng-840' target='languageid'>eng-840</a></li>
<li><a href='languageid.jsp?a=pt_PT' target='languageid'>pt_PT</a></li>
<li><a href='languageid.jsp?a=AZ-arab-Ir' target='languageid'>AZ-arab-Ir</a></li>
<li><a href='languageid.jsp?a=zh-Hant-HK' target='languageid'>zh-Hant-HK</a></li>
<li><a href='languageid.jsp?a=en-cmn-Hant-HK' target='languageid'>en-cmn-Hant-HK</a></li>
<li><a href='languageid.jsp?a=sl-Cyrl-YU-rozaj-solba-1994-b-1234-a-Foobar-x-b-1234-a-Foobar' target='languageid'>
sl-Cyrl-YU-rozaj-solba-1994-b-1234-a-Foobar-x-b-1234-a-Foobar</a></li>
<li><a href='sample_subtags.html' target='samples'>Other Samples</a></li>
</ul>

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
