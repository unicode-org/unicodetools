<html>

<head>
<%@ include file="header.jsp" %>
<title>Unicode Utilities: BIDI (UBA) C Reference</title>
<script type="text/javascript">
function detectIE() {
    var ua = window.navigator.userAgent;
    if (ua.indexOf("Trident/") > 0 || ua.indexOf("MSIE ") > 0) {
        return true;
    }
    return false;
}

function setUbaInputFromInsert(elemId, val) {
    if (!detectIE()) {
        elemId.focus();
    }
    if (elemId.selectionStart || elemId.selectionStart == 0) {
        var startPos = elemId.selectionStart;
        var endPos = elemId.selectionEnd;
        elemId.value =
            elemId.value.substring(0, startPos)
            + val
            + elemId.value.substring(endPos, elemId.value.length);
        startPos += val.length;
        elemId.selectionStart = elemId.selectionEnd = startPos;
    } else {
        elemId.value += val;
    }
    setUbaInput(elemId.value);
}

function setUbaInputFromEdit(ev) {
    setUbaInput(ev.target.value);
}

function setUbaInput(str) {
    var strLen = str.length;
    var cpPos = "", cpSeq = "";
    var strSourcePos = "<th>Memory pos.</th>";
    var strSourceChars = "<th id=\"idSourceCharsTh\"; style=\"font-size:medium; vertical-align:middle\">Character</th>";
    var strSourceCodes = "<th>Code point</th>";
    var i, j = 0;
    var cu1, cu2;
    var truncated = false;

    if (strLen == 0) {
        truncated = true;
    }

    for (i = 0; i < strLen; ++i) {
        var sup = 0;
        cp = cu1 = str.charCodeAt(i);
        if (cu1 == 0x000A || cu1 == 0x000D || cu1 == 0x001C || cu1 == 0x001D
                || cu1 == 0x001E || cu1 == 0x0085 || cu1 == 0x2029) {
            truncated = true;
            break;
        }
        if (0xD800 <= cu1 && cu1 <= 0xDBFF) {
            if (i + 1 < strLen) {
                cu2 = str.charCodeAt(i + 1);
                if (0xDC00 <= cu2 && cu2 <= 0xDFFF) {
                    cp = 0x10000 + ((cu1 - 0xD800) << 10) + (cu2 - 0xDC00);
                    ++i;
                    sup = 1;
                    if (cp >= 0x100000) {
                        sup = 2;
                    }
                }
            }
        }
        cpSeq += " " + ("0000" + cp.toString(16)).substr(-(4 + sup)).toUpperCase();
        strSourceChars += "<td>&#x" + ("0000" + cp.toString(16)).substr(-(4 + sup)).toUpperCase() + "</td>";
        strSourceCodes += "<td><pre>" + ("0000" + cp.toString(16)).substr(-(4 + sup)).toUpperCase() + "</pre></td>";
        var k;
        var s = "";
        for (k = 5 + sup - j.toString().length; k > 0; --k) {
            s += " ";
        }
        cpPos += s + j.toString();
        strSourcePos += "<td><pre>" + j.toString() + "</pre></td>";
        ++j;
    }
    document.getElementById("idInputCpSeq").innerHTML = cpSeq;
    document.getElementById("idSourcePos").innerHTML = strSourcePos;
    document.getElementById("idSourceChars").innerHTML = strSourceChars;
    document.getElementById("idSourceCharsTh").style.height = "40";
    document.getElementById("idSourceCodes").innerHTML = strSourceCodes;

    if (!truncated) {
        document.getElementById("idBtRun").disabled = false;
        document.getElementById("idInputCpSeq").style.color = "";
        document.getElementById("idSource").style.color = "";
    }
    else {
        document.getElementById("idBtRun").disabled = true;
        document.getElementById("idInputCpSeq").style.color = "darkred";
        document.getElementById("idSource").style.color = "darkred";
    }
    document.getElementById("idLevels").style.color = "gray";
    document.getElementById("idDisplay").style.color = "gray";
    document.getElementById("idAnalysis").style.color = "gray";
}
</script>
</head>

<body>
<%
    request.setCharacterEncoding("UTF-8");
    String queryStr = request.getQueryString();
    UtfParameters utfParams = new UtfParameters(queryStr);

    String ubaPara = utfParams.getParameter("b", "2");
    String ubaVersion = utfParams.getParameter("u", "100");
    String ubaDetail = utfParams.getParameter("d", "2");
    boolean ubaShowVacuous = !"off".equals(utfParams.getParameter("y", "off"));
    String valInputCharSeq = utfParams.getParameter("s", "\u0645\u0627\u0631\u0652\u0643 \u2066\u0031\u2013\u0033%\u2069 mark (\u0366v.2)\u0368!");
    ArrayList<String> arrInputCpSeq = new ArrayList<String>();
    String[] resLevArray = {};
    String outResLevels = "\"\"";
    String outResOrder = "\"\"";
    String s;
    StringBuilder sbInputCpSeq = new StringBuilder();
    StringBuilder sbSourcePos = new StringBuilder();
    StringBuilder sbSourceChars = new StringBuilder();
    StringBuilder sbSourceCodes = new StringBuilder();
    int j = 0;
    for (int i = 0; i < valInputCharSeq.length(); ++i) {
        int sup = 0;
        int cp = valInputCharSeq.codePointAt(i);
        if (cp >= 0x10000) {
            ++i;
            sup = 1;
            if (cp >= 0x100000) {
                sup = 2;
            }
        }
        s = "0000" + Integer.toHexString(cp);
        s = s.substring(s.length() - (4 + sup)).toUpperCase();
        arrInputCpSeq.add(s);
        sbInputCpSeq.append(' ').append(s);
        sbSourceChars.append("<td>&#x").append(s).append("</td>");
        sbSourceCodes.append("<td><pre>").append(s).append("</pre></td>");
        sbSourcePos.append("<td><pre>").append(Integer.toString(j++)).append("</pre></td>");
    }
    String valInputCpSeq = sbInputCpSeq.toString();
    String valSourcePos = sbSourcePos.toString();
    String valSourceChars = sbSourceChars.toString();
    String valSourceCodes = sbSourceCodes.toString().toUpperCase();
    String valLevelsLevs = "";
    String valDisplayDispPos = "";
    String valDisplayMemPos = "";
    String valDisplayChars = "";
    String valDisplayCodes = "";

    ////Process process = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/C", "cd").start();
    Process process = new ProcessBuilder(
        "bidiref/bidiref1",
        "-b" + ubaPara, "-u" + ubaVersion, "-d" + ubaDetail, "-y" + (ubaShowVacuous ? "0" : "1"), "-s" + valInputCpSeq)
        .start();
    java.io.InputStream is = process.getInputStream();
    java.io.InputStreamReader isr = new java.io.InputStreamReader(is);
    java.io.BufferedReader br = new java.io.BufferedReader(isr);
    String line;
    String valAnalysis = "";

    ////valAnalysis += java.nio.file.Paths.get(".").toAbsolutePath().normalize().toString() + "<br>";

    while ((line = br.readLine()) != null) {
        if (line.equals("--------------------------------------------------------------------------------")) {
            valAnalysis += "<hr>";
        }
        else if (line.startsWith("Resolved Levels:")) {
            valAnalysis += line + "\n";
            String resLevels = line.substring(18, line.length() - 1);
            outResLevels = "\"" + resLevels + "\"";
            resLevArray = resLevels.split(" ");
            for (int i = 0; i < resLevArray.length; ++i) {
                valLevelsLevs += "<td><pre>" + resLevArray[i] + "</pre></td>";
            }
        }
        else if (line.startsWith("Resolved Order:")) {
            valAnalysis += line + "\n";
            String resOrder = line.substring(18, line.length() - 1);
            outResOrder = "\"" + resOrder + "\"";
            if (resOrder.length() > 0) {
                String[] resOrdArray = resOrder.split(" ");
                for (int i = 0; i < resOrdArray.length; ++i) {
                    valDisplayDispPos += "<td><pre>" + i + "</pre></td>";
                    valDisplayMemPos += "<td><pre>" + resOrdArray[i] + "</pre></td>";
                    int resIdx = Integer.parseInt(resOrdArray[i]);
                    String cp = arrInputCpSeq.get(resIdx);
                    valDisplayChars += "<td>&#x" + cp + "</td>";
                    valDisplayCodes += "<td><pre>" + cp + "</pre></td>";
                }
            }
        }
        else {
            valAnalysis += line.replace("<", "&lt;").replace(">", "&gt;") + "\n";
        }
    }
%>

<h1>Unicode Utilities: BIDI (UBA) C Reference</h1>
<%@ include file="subtitle.jsp" %>
<p><a target="help" href="http://cldr.unicode.org/unicode-utilities/breaks"><b>help</b></a> | <%@ include file="others.jsp" %></p>
<p>Shows processing of a single paragraph of text by the Unicode Bidirectional Algorithm (UBA),
    Versions <a target="doc" href="http://www.unicode.org/reports/tr9/tr9-27.html">6.2</a> through <a target="doc" href="http://www.unicode.org/reports/tr9/tr9-37.html">10.0</a>, 
    using the C Reference Implementation, Version <a target="doc" href="http://www.unicode.org/Public/PROGRAMS/BidiReferenceC/10.0.0/">10.0</a>.</p>

<h3>Source</h3>
<form name="naInputForm">
    <table border="1" style="border-collapse:collapse; width:10120">
        <tbody>
        <tr>
            <td>
                Insert:
                &ensp;
                <input id="idBtTab" type="button" value="Tab" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u0009')"/>
                &ensp;
                <input id="idBtLrm" type="button" value="LRM" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u200E')"/>
                <input id="idBtRlm" type="button" value="RLM" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u200F')"/>
                <input id="idBtAlm" type="button" value="ALM" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u061C')"/>
                &ensp;
                <input id="idBtLre" type="button" value="LRE" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u202A')"/>
                <input id="idBtRle" type="button" value="RLE" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u202B')"/>
                <input id="idBtLro" type="button" value="LRO" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u202D')"/>
                <input id="idBtRlo" type="button" value="RLO" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u202E')"/>
                <input id="idBtPdf" type="button" value="PDF" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u202C')"/>
                &ensp;
                <input id="idBtLri" type="button" value="LRI" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u2066')"/>
                <input id="idBtRli" type="button" value="RLI" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u2067')"/>
                <input id="idBtFsi" type="button" value="FSI" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u2068')"/>
                <input id="idBtPdi" type="button" value="PDI" onClick="setUbaInputFromInsert(document.naInputForm.idInputCharSeq, '\u2069')"/>
            </td>
        </tr>
        <tr>
            <td>
                <textarea id="idInputCharSeq" name="s" rows="2" cols="100" maxlength="200" oninput="setUbaInputFromEdit(event)"><%= valInputCharSeq %></textarea>
            </td>
        </tr>
        <tr style="display:none">
            <td>
                <pre style="background-color:#EEFFEE">Text:         <span id="idInputCpSeq"><%= valInputCpSeq %></span></pre>
            </td>
        </tr>
        <tr><td style="padding:0">
            <table id="idSource" style="border-collapse:collapse; text-align:center; table-layout:fixed">
                <colgroup>
                    <col style="width:120">
                    <col span="200" style="width:50">
                </colgroup>
                <tr id="idSourcePos">
                    <th style="white-space:nowrap">Memory pos.</th>
                    <%= valSourcePos %>
                </tr>
                <tr id="idSourceChars" style="font-size:28">
                    <th style="height:40; font-size:medium; vertical-align:middle">Character</th>
                    <%= valSourceChars %>
                </tr>
                <tr id="idSourceCodes">
                    <th>Code point</th>
                    <%= valSourceCodes %>
                </tr>
            </table>
        </td></tr>
        <tr>
            <td>
                Paragraph:
                <select name="b" size="1">
                    <option value="2" <%= (ubaPara.equals("2") ? "selected" : "") %>>Auto</option>
                    <option value="0" <%= (ubaPara.equals("0") ? "selected" : "") %>>LTR</option>
                    <option value="1" <%= (ubaPara.equals("1") ? "selected" : "") %>>RTL</option>
                </select>
                &ensp;
                UBA Version:
                <select name="u" size="1">
                    <option value="62" <%= (ubaVersion.equals("62") ? "selected" : "") %>>6.2</option>
                    <option value="63" <%= (ubaVersion.equals("63") ? "selected" : "") %>>6.3</option>
                    <option value="70" <%= (ubaVersion.equals("70") ? "selected" : "") %>>7.0</option>
                    <option value="80" <%= (ubaVersion.equals("80") ? "selected" : "") %>>8.0</option>
                    <option value="90" <%= (ubaVersion.equals("90") ? "selected" : "") %>>9.0</option>
                    <option value="100" <%= (ubaVersion.equals("100") ? "selected" : "") %>>10.0</option>
                </select>
                &ensp;
                Detail:
                <select name="d" size="1">
                    <option value="2" <%= (ubaDetail.equals("2") ? "selected" : "") %>>Low</option>
                    <option value="3" <%= (ubaDetail.equals("3") ? "selected" : "") %>>High</option>
                    <option value="4" <%= (ubaDetail.equals("4") ? "selected" : "") %>>Full</option>
                </select>
                &ensp;
                <input name="y" type="checkbox" <%= (ubaShowVacuous ? "checked" : "") %>/>
                <label for="y">Show vacuous rules</label>
                &ensp;
                <input id="idBtRun" type="submit" value="Run UBA"
                    onClick="window.location.href='bidic.jsp?b='+document.getElementById('b').value + '&u='+document.getElementById('u').value + '&d='+document.getElementById('d').value + (document.getElementById('y').checked?'&y':'') + '&s=' + document.getElementById('s').value"/>
            </td>
        </tr>
        </tbody>
    </table>
</form>

<h3>Resolved Levels</h3>
<table border="1" style="border-collapse:collapse; width:10120">
    <tbody>
    <tr><td style="padding:0">
        <table id="idLevels" style="border-collapse:collapse; text-align:center; table-layout:fixed">
            <colgroup">
                <col style="width:120"/>
                <col span="200" style="width:50"/>
            </colgroup>
            <tr id="idLevelsPos">
                <th style="white-space:nowrap">Memory pos.</th>
                <%= valSourcePos %>
            </tr>
            <tr id="idLevelsLevs">
                <th>Level</th>
                <%= valLevelsLevs %>
            </tr>
        </table>
    </td></tr>
    </tbody>
</table>

<h3>Reordered Display</h3>
<table border="1" style="border-collapse:collapse; width:10120">
    <tbody>
    <tr><td style="padding:0">
        <table id="idDisplay" style="border-collapse:collapse; text-align:center; table-layout:fixed">
            <colgroup>
                <col style="width:120"/>
                <col span="200" style="width:50"/>
            </colgroup>
            <tr id="idDisplayDispPos">
                <th style="white-space:nowrap">Display pos.</th>
                <%= valDisplayDispPos %>
            </tr>
            <tr id="idDisplayMemPos">
                <th style="white-space:nowrap">Memory pos.</th>
                <%= valDisplayMemPos %>
            </tr>
            <tr id="idDisplayChars" style="font-size:28">
                <th style="height:40; font-size:medium; vertical-align:middle">Character</th>
                <%= valDisplayChars %>
            </tr>
            <tr id="idDisplayCodes">
                <th>Code point</th>
                <%= valDisplayCodes %>
            </tr>
        </table>
    </td></tr>
    </tbody>
</table>

<h3>Analysis</h3>
<!-- <p><%= outResLevels %></p> -->
<!-- <p><%= outResOrder %></p> -->
<pre id="idAnalysis"><%= valAnalysis %></pre>

<%@ include file="footer.jsp" %>
</body>

</html>
