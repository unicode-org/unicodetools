<html>

<head>
<meta http-equiv="Content-Language" content="en-us">
<meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
<title>Unicode Utilities: Description and Index</title>
<link rel="stylesheet" type="text/css" href="index.css">
</head>

<body>

<h1>Unicode Utilities: Description and Index</h1>
<%@ include file="others.jsp" %>
<table><tr><td width='33%'>
<h2>Boundaries</h2>
<ul>
	<li><b><a target="breaks" href="breaks.jsp">Breaks</a></b> Demonstrates different boundaries within text.
	<ul>
		<li>Enter the sample text.</li>
		<li>Pick the kind of boundaries, or hit <b>Test</b>.</li>
	</ul>
	</li>
	   <li><b><a target="regex" href="regex.jsp">Regex</a></b> Shows transformation of (Java) Regex pattern to support Unicode.
        <ul>
            <li>Enter the regex pattern</li>
            <li>Change the sample text if desired.</li>
            <li>Click <b>Show Modified Regex Pattern</b></li>
        </ul>You'll then see the modified pattern.
            It will often be much larger, but any reasonable Regex engine will compile character classes reasonably.
            Below that, you'll see a sample of how the expression works, using it to find substrings of the sample text and underline them.
    </li>
</ul>
</td><td width='33%'>
<h2><a target="properties" onclick="return top.js.OpenExtLink(window,event,this)" href="properties.html">Properties</a></h2>
<ul>
	<li><b>Unicode Property Demo</b> window<ul>
		<li>Enter a character code in the right side, and hit <b>Show</b>. You&#39;ll see the properties 
		for that character (where they have non-default values).</li>
		<li>If you click on any property (like
		<a target="properties" href="properties.jsp#Age">Age</a>), you&#39;ll see 
		a list of all the properties and their values in the <b>Unicode Property List</b> window</li>
		<li>If you click on any property value in either of these two windows, like
		<a target="list" href="list-unicodeset.jsp?a=%5B:Age=4.0.0.0:%5D">
		4.0.0.0</a> for Age, you&#39;ll see the characters with that property in the <b> <a href="#UnicodeSet">UnicodeSets</a></b>
		<b>Demo</b> 
		window</li>
	</ul>
	</li>
	<li><b>UnicodeSet Demo</b> window<ul>
		<li>You can put in arbitrary <a href="#UnicodeSet">UnicodeSets</a>, allowing boolean combinations 
		of any of the property+value combinations in the <b>Unicode Property List</b> window</li>
		<li>If you click on Compare at the top, you can compare any two <a href="#UnicodeSet">UnicodeSets</a>.</li>
	</ul>
	</li>
</ul>
</td><td width='33%'>
<h2>Transforms</h2>
<ul>
    <li><b><a target="transform" href="transform.jsp">Transform</a></b> Demonstrates different boundaries within text.
        <ul>
            <li>Enter the Transform Rules</li>
            <li>Enter Sample Text</li>
            <li>Hit Show Transform</li>
            <li>Examples:
                <ul>
                    <li><a target="transform" href="transform.jsp?a=lower&b=The+Quick+Brown+Fox.">lowercase sample</li>
                    <li><a target="transform" href="transform.jsp?a=any-greek&b=The+Quick+Brown+Fox.">Latin-Greek sample</li>
                    <li><a target="transform" href="transform.jsp?a=casefold&b=\p{sc%3DGreek}">casefold Greek</li>
                    <li><a target="transform" href="transform.jsp?a=a+%3E+Huh%27%3F%27%3B%0D%0A%3A%3A[%3Ascript%3Dlatin%3A]+lower%3B%0D%0A%0D%0A&b=\p{ascii}">sample rules</a></li>
                </ul>
            </li>
        </ul>
        <p>The rules can either be <a href="http://userguide.icu-project.org/transforms/general#TOC-Transliterator-Identifiers">IDs (simple or compound)</a>
        or <a href="http://userguide.icu-project.org/transforms/general/rules">general rules</a>. To see a list of all the IDs, see 
        <a target="transform" href="transform.jsp?a=list">ID List</a>.</p>
        <p>The sample can either be a piece of text or a <a href="#UnicodeSet">UnicodeSet</a>. In the latter case, only characters that are affected by the
        transform are shown. They are listed alphabetically by the <i>result</i> of the transform, with multiple entries shown in a <a href="#UnicodeSet">UnicodeSet</a>.</p>
        </li>
    </ul>
</td><tr><table>
<hr>
<h2><a name="UnicodeSet">UnicodeSet</a></h2>
<p>UnicodeSets use regular-expression syntax to allow for arbitrary set operations (Union, Intersection, 
Difference) on sets of Unicode characters. The base sets can be specified explicitly, such as <code>
[a-m w-z]</code>, or using Unicode Properties like <code>[[:script=arabic:]&amp;[:decomposit<wbr>iontype=canonical:]]</code>. 
The latter set gets the Arabic script characters that have a canonical decomposition. The properties 
can be specified either with Perl-style notation (<code>\p{script=arabic}</code>) or with POSIX-style 
notation (<code>[:script=arabic:]</code>). For more information, see
<a href="http://icu-project.org/userguide/unicodeSet.html">ICU UnicodeSet Documentation</a>.</p>
<p>In the online demo, the implementation of UnicodeSet is customized in the following ways.</p>
<ol>
	<li><b>Query Use. </b>The UnicodeSet can be typed in, or used as a URL query parameter, such as 
	the following. Note that in that case, &quot;&amp;&quot; needs to be replaced by &quot;%26&quot;.<ul>
		<li>&nbsp;<a target="list" href="list-unicodeset.jsp?a=[:whitespace:]"><code>list-unicodeset.jsp?a=[:whitespace:]</code></a></li>
	</ul>
	</li>
	<li><b>Regular Expressions. </b>For the <i>name</i> property, regular expressions can be used for 
	the value, enclosed in /.../. For example in the following expression, the first term will select 
	all those Unicode characters whose names contain &quot;CJK&quot;. The rest of the expression will then subtract 
	the ideographic characters, showing that these can be used in arbitrary combinations.<ul>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[[:name=/CJK/:]-[:ideographic:]]">[[:name=/CJK/:]-[:ideographic:]]</a></code> - the set of all characters with names that contain CJK 
		that are not Ideographic</li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:name=/\bDOT$/:]">[:name=/\bDOT$/:]</a></code> - the set of all characters with names that end with the word DOT</li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:block=/(?i)arab/:]">[:block=/(?i)arab/:]</a></code> - the set of all characters in blocks that contain the sequence of 
		letters &quot;arab&quot; (case-insensitive)</li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:toNFKC=/\./:]">[:toNFKC=/\./:]</a></code> - the set of all characters with toNFKC values that contain a literal 
		period</li>
	</ul>
	<p>Some particularly useful regex features are:<ul>
		<li>\b means a word break, ^ means front of the string, and $ means end. So /^DOT\b/ means 
		the word DOT at the start.</li>
		<li>(?i) means case-insensitive matching.</li>
	</ul>
	<p><i><b>Caveats:</b></i><ol>
	<li>The regex uses the standard
	<a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html">Java Pattern</a>. 
	In particular, it does not have the extended functions in UnicodeSet, nor is it up-to-date with 
	the latest Unicode. So be aware that you shouldn't depend on properties inside of the /.../ 
	pattern.</li>
	<li>The Unassigned, Surrogate, and Private Use code points are skipped in the Regex comparison, 
	so [:Block=/Aegean_Numbers/:] returns a different number of characters than [:Block=Aegean_Numbers:], 
	because it skips Unassigned code points.</li>
	<li>None of the normal &quot;loose matching&quot; is enabled. So [:Block=aegeannumbers:] works, but 
	[:Block=/aegeannumbers/:] fails -- you have to use [:Block=/Aegean_Numbers/:] or [:Block=/(?i)aegean_numbers/:].</li>
</ol>

	</li>
	<li><b>Casing Properties. </b>Unicode defines a number of string casing functions in <i>Section 
	3.13 Default Case Algorithms</i>. These string functions can also be applied to single characters.
	<i><b>Warning:</b> </i>the first three sets may be somewhat misleading: isLowercase means that 
		the character is the same as its lowercase version, which includes <i>all uncased</i> characters. To 
		get those characters that are <i>cased</i> characters and lowercase, use
		<a target="list" href="list-unicodeset.jsp&a=[[:isLowercase:]-[:^isCased:]]">
		<code>[[:isLowercase:]&amp;[:isCased:]]</code></a><ol>
	<li>The binary testing operations take no argument:<ul>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:isLowercase:]">[:isLowercase:]</a></code></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:isUppercase:]"><code>[:isUppercase:]</code></a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:isTitlecase:]"><code>[:isTitlecase:]</code></a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:isCaseFolded:]"><code>[:isCaseFolded:]</code></a></li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:isCased:]">[:isCased:]</a>.</code></li>
	</ul>
	</li>
	<li>The string functions are also provided, and require an argument. For example:<ul>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:toLowercase=a:]">
		[:toLowercase=a:]</a> </code>- the set of all characters X such that toLowercase(X) = a</li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:toCaseFold=a:]">
		[:toCaseFold=a:]</a> </code></li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:toUppercase=A:]">
		[:toUppercase=A:]</a> </code></li>
		<li><code><a target="list" href="list-unicodeset.jsp?a=[:toTitlecase=A:]">
		[:toTitlecase=A:]</a></code></li>
	</ul>
	<p>Note: The Unassigned, Surrogate, and Private Use code points are skipped in generation of the 
	sets.</li>
</ol>

	</li>
	<li><b>Normalization Properties. </b>Unicode defines a number of string normalization functions 
	UAX #15. These string functions can also be applied to single characters.<ol>
	<li>The binary testing operations have somewhat odd constructions:<ul>
		<li><a target="list" href="list-unicodeset.jsp?a=[:^NFCquickcheck=N:]">
		[:^NFCquickcheck=N:]</a> (use for [:isNFC:], and so on).</li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:^NFKCquickcheck=N:]">
		[:^NFKCquickcheck=N:]</a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:^NFDquickcheck=N:]">
		[:^NFDquickcheck=N:]</a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:^NFKDquickcheck=N:]">
		[:^NFKDquickcheck=N:]</a></li>
	</ul>
	</li>
	<li>The string functions are also provided, and require an argument. For example:<ul>
		<li><a target="list" href="list-unicodeset.jsp?a=[:toNFC=a:]">[:toNFC=a:]</a><code> </code>
		- the set of all characters X such that toNFC(X) = a</li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:toNFD=A\u0300:]">[:toNFD=A\u0300:]</a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:toNFKC=A:]">[:toNFKC=A:]</a></li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:toNFKD=A\u0300:]">[:toNFKD=A\u0300:]</a></li>
	</ul>
	<p>Note: The Unassigned, Surrogate, and Private Use code points are skipped in the generation of 
	the sets.</li>
</ol>

	</li>
	<li><b>IDNA Properties.</b> The status of characters with respect to IDNA (internationalized domain 
	names) can also be determined. The available properties are listed below.<ol>
		<li><a target="list" href="list-unicodeset.jsp?a=[:idna=output:]"><code>[:idna=output:]</code></a> The set of all characters 
		allowed in the output of IDNA. An example is<ul>
			<li><code><a target="character" href="character.jsp?a=00E0">
			U+00E0</a></code> ( &#x00E0; ) LATIN SMALL LETTER A WITH GRAVE</li>
		</ul>
		</li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:idna=ignored:]"><code>[:idna=ignored:]</code></a> The set of all characters 
		ignored by IDNA on input. That is, these characters are mapped to nothing -- removed -- by NamePrep. An example is:<ul>
			<li><code><a target="character" href="character.jsp?a=00AD">U+00AD</a></code> 
			( &#x00AD; ) SOFT HYPHEN.</li>
		</ul>
		</li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:idna=remapped:]"><code>[:idna=remapped:]</code></a> The set of characters 
		remapped to other characters by IDNA (NamePrep). Examples are:<ul>
			<li><code><a target="character" href="character.jsp?a=00C0">U+00C0</a></code> 
			( &#x00C0; ) LATIN CAPITAL LETTER A WITH GRAVE (remapped to the lowercase version).</li>
			<li><code><a target="c" href="character.jsp?a=FF21">
			U+FF21</a></code> ( &#65313; ) FULLWIDTH LATIN CAPITAL LETTER A</li>
		</ul>
		</li>
		<li><a target="list" href="list-unicodeset.jsp?a=[:idna=disallowed:]"><code>[:idna=disallowed:]</code></a> These are characters 
		disallowed (on the registry side) by IDNA. An example is:<ul>
			<li><code><a target="character" href="character.jsp?a=002C">U+002C</a></code> 
			( , ) COMMA</li>
		</ul>
		<p>Note: The client side adds characters unassigned in Unicode 3.2, for compatibility. To 
		see just the characters disallowed in Unicode 3.2, you can use <code>
		<a target="list" href="list-unicodeset.jsp?a=[[:idna=disallowed:]%26[:age=3.2:]]">[[:idna=disallowed:]&amp;[:age=3.2:]]</a></code>. 
		To also remove <i>private-use, unassigned, surrogates, </i>and<i> controls</i>, use <code>
		<a target="list" href="list-unicodeset.jsp?a=[[:idna=disallowed:]%26[:age=3.2:]-[:c:]]">[[:idna=disallowed:]&amp;[:age=3.2:]-[:c:]]</a></code>.</li>
	</ol>
	</li>
</ol>
<%@ include file="footer.jsp" %>
</body>
</html>