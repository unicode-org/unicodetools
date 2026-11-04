<%@ page import="org.unicode.jsp.*,java.time.Year" %>
<footer>
<p><%= UnicodeJsp.getVersions() %> &#x2022; <a href="https://github.com/unicode-org/unicodetools">source</a></p>
    <p>
<!-- yes, UTF-8 issue here, so using &copy; -->
&copy; 2001-<%= Year.now() %> Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in
    the U.S. and other countries. See <a href="https://www.unicode.org/copyright.html">Terms of Use</a>.
    </p>
</footer>
