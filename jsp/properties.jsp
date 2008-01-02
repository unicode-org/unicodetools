<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta name="GENERATOR" content="Microsoft FrontPage 6.0">
<meta name="ProgId" content="FrontPage.Editor.Document">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Unicode Property List</title>
<link rel="stylesheet" type="text/css" href="index.css">
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.*" %> <%@ page import="java.lang.*" %> <%@ page import="com.ibm.icu.text.*" %>
<%@ page import="com.ibm.icu.lang.*" %> <%@ page import="com.ibm.icu.util.*" %>
<%@ page import="java.util.regex.*" %>
<style>
<!--
table, td, th {border:1px solid green; padding:1; margin:0; vertical-align: top;}
table {border:0px solid blue; border-collapse: collapse}
span.break   { border-right: 1px solid red;}
-->
</style>
<style>
<!--
th           { text-align: left }
-->
</style>
</head>

<body>

<h1>Unicode Property List (<a target="c" href="character.jsp">Check</a>)</h1>
<%
		request.setCharacterEncoding("UTF-8");
		int[][] ranges = {{UProperty.BINARY_START, UProperty.BINARY_LIMIT},
				{UProperty.INT_START, UProperty.INT_LIMIT},
				{UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT},
				{UProperty.STRING_START, UProperty.STRING_LIMIT},
		};
		Collator col = Collator.getInstance(ULocale.ROOT);
		((RuleBasedCollator)col).setNumericCollation(true);
		Map alpha = new TreeMap(col);

		Set showLink = new HashSet();
		
		for (int range = 0; range < ranges.length; ++range) {
			for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
				String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
				//String shortPropName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.SHORT);
				//propName = getName(propIndex, propName, shortPropName);
				Set valueOrder = new TreeSet(col);
				alpha.put(propName, valueOrder);
				//out.println(propName + "<br>");
				switch (range) {
				default: valueOrder.add("[?]"); break;
				case 0: valueOrder.add("True"); valueOrder.add("False"); showLink.add(propName); break;
				case 2: valueOrder.add("[double]"); break;
				case 3: valueOrder.add("[string]"); break;
				case 1:
				for (int valueIndex = 0; valueIndex < 256; ++valueIndex) {
					try {
						String valueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.LONG);
						//out.println("----" + valueName + "<br>");
						//String shortValueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.SHORT);
						//valueName = getName(valueIndex, valueName, shortValueName);
						if (valueName != null) valueOrder.add(valueName);
						else if (propIndex == UProperty.CANONICAL_COMBINING_CLASS) {
							String posVal = String.valueOf(valueIndex);
							if (new UnicodeSet("[:ccc=" + posVal + ":]").size() != 0) {
								valueOrder.add(posVal);
							}
						}
						showLink.add(propName);
					} catch (RuntimeException e) {
						// just skip
					}
				}
				}
			}
		}
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
		for (Iterator it = alpha.keySet().iterator(); it.hasNext();) {
			String propName = (String) it.next();
			String sPropName = propName;
			Set values = (Set) alpha.get(propName);
			if (unicodeProps.contains(propName)) {
				unicodeProps.remove(propName);
			} else if (regexProps.contains(propName)) {
			     	regexProps.remove(propName);
					sPropName = "<tt>\u00AE\u00A0" + sPropName + "</tt>";
			} else {
				sPropName = "<i>\u00A9\u00A0" + sPropName + "</i>";
			}

			out.println("<tr><th width='1%'><a name='" + propName + "'>" + sPropName + "</a></th>");
			out.println("<td>");
			boolean first = true;
			for (Iterator it2 = values.iterator(); it2.hasNext();) {
				String propValue = (String) it2.next();
				if (first) first = false;
				else out.print(", ");

				
					if (showLink.contains(propName)) {
						propValue = "<a target='u' href='list-unicodeset.jsp?a=[:" + propName
							+ "=" + propValue + ":]'>" + propValue + "</a>";
					}
					
				out.print(propValue);
			}
			out.println("</td></tr>");
		}
		out.println("</table>");
		unicodeProps.addAll(regexProps);
%>
<p>® = Regex Property (<a target="_blank" href="http://www.unicode.org/reports/tr18/">UTS #18</a>): 
not formal Unicode property<br>
© = ICU-Only Property (not Unicode or Regex)<br>
<b>Not explicitly in ICU: </b><%=unicodeProps%></p>
<p>Built with ICU version: <%= com.ibm.icu.util.VersionInfo.ICU_VERSION.toString() %></p>
</b></b>

</body>

</html>
