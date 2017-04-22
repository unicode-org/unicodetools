<html>
<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: BIDI (UBA)</title>
<script type="text/javascript">

function displaymessage() {
    alert("Hello World!");
}

function insert(myField, myValue) {
    if (document.selection) { //IE support
        myField.focus();
        sel = document.selection.createRange();
        sel.text = myValue;
    } else if (myField.selectionStart || myField.selectionStart == 0) { //MOZILLA/NETSCAPE support
        var startPos = myField.selectionStart;
        var endPos = myField.selectionEnd;
        myField.value = myField.value.substring(0, startPos) + myValue
        + myField.value.substring(endPos, myField.value.length);
        startPos += myValue.length;
        myField.selectionStart = myField.selectionEnd = startPos;
    } else {
        myField.value += myValue;
    }
    myField.focus();
}

</script>
</head>
<body>
<%
        request.setCharacterEncoding("UTF-8");
        String queryString = request.getQueryString();
        
        UtfParameters utfParameters = new UtfParameters(queryString);
        
        String sample = utfParameters.getParameter("a", "mark 3.1% \u0645\u0627\u0631\u0652\u0643 2.0.");
        boolean hack = !"off".equals(utfParameters.getParameter("hack", "off"));
        String p = utfParameters.getParameter("p", "Auto");
        int p2 = p.equals("LTR") ? 0 : p.equals("RTL") ? 1 : -1;
        // 
%>
<h1>Unicode Utilities: BIDI (UBA)</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/bidi"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<p>Shows processing by the UBA (<a target='doc' href='http://unicode.org/reports/tr9/'>Unicode Bidi Algorithm</a>), which is used to display all Unicode Arabic and Hebrew text.
For more information, see <a target='doc' href='http://cldr.unicode.org/unicode-utilities/bidi'>bidi info</a>.</p>
<p><b><i>Not yet updated for the changes in 
<a href="http://unicode-inc.blogspot.ch/2013/09/announcing-unicode-standard-version-63.html">Unicode 6.3</a>!</i></b></p>
<form name="myform">
  <table border="1" cellpadding="0" cellspacing="0" style="border-collapse: collapse; width:100%">
    <tr>
      <th>Sample &nbsp; &nbsp; &nbsp;
      <input id='tab' type="button" value="TAB" onClick="insert(document.myform.a, '\u0009')"/>
      <input id='tab' type="button" value="LRM" onClick="insert(document.myform.a, '\u200E')"/>
      <input id='tab' type="button" value="RLM" onClick="insert(document.myform.a, '\u200F')"/>
      <input id='tab' type="button" value="LRE" onClick="insert(document.myform.a, '\u202A')"/>
      <input id='tab' type="button" value="RLE" onClick="insert(document.myform.a, '\u202B')"/>
      <input id='tab' type="button" value="PDF" onClick="insert(document.myform.a, '\u202C')"/>
      <input id='tab' type="button" value="LRO" onClick="insert(document.myform.a, '\u202D')"/>
      <input id='tab' type="button" value="RLO" onClick="insert(document.myform.a, '\u202E')"/>
      </th>
    </tr>
    <tr>
      <td><textarea name="a" rows="8" cols="10" style="width: 100%"><%=sample%></textarea></td>
    </tr>
</table>
<select name="p">
<option<%= p2==-1 ? " selected" : ""%>>Auto</option>
<option<%= p2==0 ? " selected" : ""%>>LTR</option>
<option<%= p2==1 ? " selected" : ""%>>RTL</option>
</select> <label for="p">Paragraph Direction</label>
<input type="checkbox" <%=hack ? "checked" : ""%> name="hack"><label for="hack">ASCII Hack?</label>
<input id='main' type="submit" value="Show Bidi" onClick="window.location.href='transform.jsp?a='+document.getElementById('main').value"/>
  <hr>
  <%= UnicodeJsp.showBidi(sample, p2, hack)%>
</form>
<%@ include file="footer.jsp" %>
</body>
</html>
