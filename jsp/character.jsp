<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Unicode Property Demo</title>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.*" %> <%@ page import="java.lang.*" %> <%@ page import="com.ibm.icu.text.*" %>
<%@ page import="com.ibm.icu.lang.*" %> <%@ page import="com.ibm.icu.util.*" %>
<%@ page import="java.util.regex.*" %> <%@ page import="com.ibm.icu.dev.demo.translit.*" %>
<%@ page import="com.ibm.icu.impl.*" %>
<style>
<!--
table, td {border:1px solid magenta; padding:2; margin:0; vertical-align: top}
table {border:0px solid blue; border-collapse: collapse}
-->
</style>
</head>

<body>

<h1>Unicode Property Demo (<a target="c" href="properties.jsp">List</a>)</h1>
<form name="myform" action="http://unicode.org/cldr/utility/character.jsp" method="POST">
  <%
		String text = request.getParameter("a");
		if (text == null || text.length() == 0) text = "�";
%> <%
String BASE_RULES =
  	"'<' > '&lt;' ;" +
    "'<' < '&'[lL][Tt]';' ;" +
    "'&' > '&amp;' ;" +
    "'&' < '&'[aA][mM][pP]';' ;" +
    "'>' < '&'[gG][tT]';' ;" +
    "'\"' < '&'[qQ][uU][oO][tT]';' ; " +
    "'' < '&'[aA][pP][oO][sS]';' ; ";

String CONTENT_RULES =
    "'>' > '&gt;' ;";

String HTML_RULES = BASE_RULES + CONTENT_RULES + 
"'\"' > '&quot;' ; ";

String HTML_RULES_CONTROLS = HTML_RULES + 
		"([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:][\\u0080-\\U0010FFFF]-[\\u0020]]) > &hex/xml($1) ; ";


Transliterator toHTML = Transliterator.createFromRules(
        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
        
		int[][] ranges = {{UProperty.BINARY_START, UProperty.BINARY_LIMIT},
				{UProperty.INT_START, UProperty.INT_LIMIT},
				{UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT},
				{UProperty.STRING_START, UProperty.STRING_LIMIT},
		};
		Collator col = Collator.getInstance(ULocale.ROOT);
		((RuleBasedCollator)col).setNumericCollation(true);
		Map alpha = new TreeMap(col);

		String HTML_INPUT = "::hex-any/xml10; ::hex-any/unicode; ::hex-any/java;";
		Transliterator fromHTML = Transliterator.createFromRules(
				"any-xml", HTML_INPUT, Transliterator.FORWARD);
		
		text = fromHTML.transliterate(text);

		if (text.length() > 2) {
		    try {
			  text = UTF16.valueOf(Integer.parseInt(text,16));
			} catch (Exception e) {}
		}
		int cp = UTF16.charAt(text, 0);
		String nextHex = "character.jsp?a=" + Utility.hex(cp < 0x110000 ? cp+1 : 0, 4);
		String prevHex = "character.jsp?a=" + Utility.hex(cp > 0 ? cp-1 : 0x10FFFF, 4);
%>
  <p><input type="button" value="Previous" name="B3" onClick="window.location.href='<%=prevHex%>'">
  <input type="text" name="a" size="10" value="<%=text%>">
  <input type="submit" value="Show" name="B1">
  <input type="button" value="Next" name="B2" onClick="window.location.href='<%=nextHex%>'"></p>
</form>
<%
		text = UTF16.valueOf(text,0);
		Set showLink = new HashSet();
		for (int range = 0; range < ranges.length; ++range) {
			for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
				String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
				String propValue = null;
				int ival;
				switch (range) {
				default: propValue = "???"; break;
				case 0: ival = UCharacter.getIntPropertyValue(cp, propIndex);
					if (ival != 0) propValue = "True";
					showLink.add(propName);
					break;
				case 2:
					double nval = UCharacter.getNumericValue(cp);
					if (nval != -1) {
						propValue = String.valueOf(nval);
						showLink.add(propName);
					}
					break;
				case 3: 
					propValue = UCharacter.getStringPropertyValue(propIndex, cp, UProperty.NameChoice.LONG); 
					if (text.equals(propValue)) propValue = null;
					break;
				case 1: ival = UCharacter.getIntPropertyValue(cp, propIndex);
					if (ival != 0) {
						propValue = UCharacter.getPropertyValueName(propIndex, ival, UProperty.NameChoice.LONG);
						if (propValue == null) propValue = String.valueOf(ival);
					}
					showLink.add(propName);
					break;					
				}
				if (propValue != null) {
					alpha.put(propName, propValue);
				}
			}
		}
		showLink.add("Age");

		String x;
		String upper = x = UCharacter.toUpperCase(ULocale.ENGLISH,text);
		if (!text.equals(x)) alpha.put("Uppercase", x);
		String lower = x = UCharacter.toLowerCase(ULocale.ENGLISH,text);
		if (!text.equals(x)) alpha.put("Lowercase", x);
		String title = x = UCharacter.toTitleCase(ULocale.ENGLISH,text,null);
		if (!text.equals(x)) alpha.put("Titlecase", x);
		String nfc = x = Normalizer.normalize(text,Normalizer.NFC);
		if (!text.equals(x)) alpha.put("NFC", x);
		String nfd = x = Normalizer.normalize(text,Normalizer.NFD);
		if (!text.equals(x)) alpha.put("NFD", x);
		x = Normalizer.normalize(text,Normalizer.NFKD);
		if (!text.equals(x)) alpha.put("NFKD", x);
		x = Normalizer.normalize(text,Normalizer.NFKC);
		if (!text.equals(x)) alpha.put("NFKC", x);

		
		CanonicalIterator ci = new CanonicalIterator(text);
		int count = 0;
		for (String item = ci.next(); item != null; item = ci.next()) {
			if (item.equals(text)) continue;
			if (item.equals(nfc)) continue;
			if (item.equals(nfd)) continue;
			alpha.put("Other_Canonical_Equivalent#" + (++count), item);
		}

		/*
		CaseIterator cai = new CaseIterator();
		cai.reset(text);
		count = 0;
		for (String item = cai.next(); item != null; item = cai.next()) {
			if (item.equals(text)) continue;
			if (item.equals(upper)) continue;
			if (item.equals(lower)) continue;
			if (item.equals(title)) continue;
			alpha.put("Other_Case_Equivalent#" + (++count), item);
		}
		*/
		
				Set unicodeProps = new TreeSet(Arrays.asList(new String[] {
				"Numeric_Value", "Bidi_Mirroring_Glyph", "Case_Folding",
				"Decomposition_Mapping", "FC_NFKC_Closure",
				"Lowercase_Mapping", "Special_Case_Condition",
				"Simple_Case_Folding", "Simple_Lowercase_Mapping",
				"Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping",
				"Titlecase_Mapping", "Uppercase_Mapping", "ISO_Comment",
				"Name", "Unicode_1_Name", "Unicode_Radical_Stroke", "Age",
				"Block", "Script", "Bidi_Class", "Canonical_Combining_Class",
				"Decomposition_Type", "East_Asian_Width", "General_Category",
				"Grapheme_Cluster_Break", "Hangul_Syllable_Type",
				"Joining_Group", "Joining_Type", "Line_Break",
				"NFC_Quick_Check", "NFD_Quick_Check", "NFKC_Quick_Check",
				"NFKD_Quick_Check", "Numeric_Type", "Sentence_Break",
				"Word_Break", "ASCII_Hex_Digit", "Alphabetic", "Bidi_Control",
				"Bidi_Mirrored", "Composition_Exclusion",
				"Full_Composition_Exclusion", "Dash", "Deprecated",
				"Default_Ignorable_Code_Point", "Diacritic", "Extender",
				"Grapheme_Base", "Grapheme_Extend", "Grapheme_Link",
				"Hex_Digit", "Hyphen", "ID_Continue", "Ideographic",
				"ID_Start", "IDS_Binary_Operator", "IDS_Trinary_Operator",
				"Join_Control", "Logical_Order_Exception", "Lowercase", "Math",
				"Noncharacter_Code_Point", "Other_Alphabetic",
				"Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend",
				"Other_ID_Continue", "Other_ID_Start", "Other_Lowercase",
				"Other_Math", "Other_Uppercase", "Pattern_Syntax",
				"Pattern_White_Space", "Quotation_Mark", "Radical",
				"Soft_Dotted", "STerm", "Terminal_Punctuation",
				"Unified_Ideograph", "Uppercase", "Variation_Selector",
				"White_Space", "XID_Continue", "XID_Start", "Expands_On_NFC",
				"Expands_On_NFD", "Expands_On_NFKC", "Expands_On_NFKD" }));
		
		Set regexProps = new TreeSet(Arrays.asList(new String[] {
				"xdigit", "alnum", "blank", "graph", "print", "word"}));

		out.println("<table>");
		String name = (String)alpha.get("Name");
		if (name != null) name = toHTML.transliterate(name);
		
		out.println("<tr><td><b>" + "Character" + "</b></td><td><b>" + toHTML.transliterate(text) + "</b></td></tr>");
		out.println("<tr><td><b>" + "Code_Point" + "</b></td><td><b>" + com.ibm.icu.impl.Utility.hex(cp,4) + "</b></td></tr>");
		out.println("<tr><td><b>" + "Name" + "</b></td><td><b>" + name + "</b></td></tr>");
		alpha.remove("Name");
		for (Iterator it = alpha.keySet().iterator(); it.hasNext();) {
			String propName = (String) it.next();
			String propValue = (String) alpha.get(propName);
			
			String hValue = toHTML.transliterate(propValue);
			hValue = showLink.contains(propName)
			? "<a target='u' href='list-unicodeset.jsp?a=[:" + propName + "=" + propValue+ ":]'>"
			  + hValue + "</a>" 
			: hValue;
			
			String pName = propName;
			if (unicodeProps.contains(propName)) {
			} else if (regexProps.contains(propName)) {
				pName = "<tt>\u00AE\u00A0" + pName + "</tt>";
			} else {
				pName = "<i>\u00A9\u00A0" + pName + "</i>";
			}

			
			out.println("<tr><td><a target='c' href='properties.jsp#" + propName + "'>" + pName + "</a></td><td>" + hValue + "</td></tr>");
		}
		out.println("</table>");
%>
<p><i>(only includes properties with non-default values)<br>
</i>® = Regex Property (<a href="http://www.unicode.org/reports/tr18/">UTS #18</a>): not formal 
Unicode property<br>
© = ICU-Only Property (not Unicode or Regex)<br>
<i><br>
</i>Built using ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>

</body>

</html>
