<%@ page import="org.unicode.jsp.*,java.time.Year" %>
<footer>
    <p><b><a name="fonts">Fonts and Display.</a></b> If you don't have a good set of Unicode fonts (and modern browser),
you may not be able to read some of the characters.
Some suggested fonts that you can add for coverage are:
<a href="https://www.google.com/get/noto/" target="_blank">Noto Fonts site</a>,
<a href="http://greekfonts.teilar.gr/" target="_blank">Unicode Fonts for Ancient Scripts</a>,
<a href="http://www.alanwood.net/unicode/fonts.html" target="_blank">Large, multi-script Unicode fonts</a>.
See also: <a href="http://www.unicode.org/help/display_problems.html" target="_blank">Unicode Display Problems</a>.</p>
<p><%= UnicodeJsp.getVersions() %> &#x2022; <a href="https://github.com/unicode-org/unicodetools">source</a></p>
    <p>
<!-- yes, UTF-8 issue here, so using &copy; -->
&copy; 2001-<%= Year.now() %> Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in
    the U.S. and other countries. See <a href="https://www.unicode.org/copyright.html">Terms of Use</a>.
    </p>
</footer>
