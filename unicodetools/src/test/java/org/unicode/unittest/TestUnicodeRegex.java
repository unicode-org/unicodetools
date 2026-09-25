package org.unicode.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeRegex;

public class TestUnicodeRegex {
    @Test
    public void TestNumberSignInCharacterClasses() {
        String[] patterns = {
            "[\\#]", "[!-\\#]", "[\\#-%]", "\\p{Po}", "\\p{P}", "[\\p{Po}&\\p{Ascii}]"
        };
        for (String pattern : patterns) {
            for (int flags : new int[] {0, Pattern.COMMENTS}) {
                Pattern compiled = UnicodeRegex.compile(pattern, flags);
                assertTrue(compiled.matcher("#").matches(), pattern);
                assertFalse(compiled.matcher("a").matches(), pattern);
            }
            Pattern compiled = UnicodeRegex.compile("(?x)" + pattern);
            assertTrue(compiled.matcher("#").matches(), pattern);
            assertFalse(compiled.matcher("a").matches(), pattern);
        }
    }

    @Test
    public void TestCommentsOutsideCharacterClasses() {
        Pattern compiled = UnicodeRegex.compile("[\\#] # a comment\n [!]", Pattern.COMMENTS);
        assertTrue(compiled.matcher("#!").matches());
        assertFalse(compiled.matcher("#a").matches());
    }

    @Test
    public void TestUnescapedNumberSign() {
        for (String regex : new String[] {"[#]", "[!-#]", "[#-%]", "[\\\\#]"}) {
            assertTrue(UnicodeRegex.compile(regex).matcher("#").matches(), regex);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> UnicodeRegex.compile(regex, Pattern.COMMENTS),
                    regex);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> UnicodeRegex.compile("(?x)" + regex),
                    regex);
        }
    }

    @Test
    public void TestCommentsAndWhitespace() {
        for (String regex :
                new String[] {
                    "[a # comment\n b]",
                    "[a# ] (?x) \\p{NotAProperty}\n b]",
                    "# [\\p{NotAProperty}(?-x)\n [\\#]",
                    "[a b]",
                    "[a\t\n\r\u000B\f b]",
                    "[\\#\\ ]",
                    "[\\\\] # [\\p{NotAProperty}\n [!]",
                    "[\\x{23}\\x{20}]",
                    "[\\u0023\\u0020]",
                    "[a\u0085\u200E\u2028\u2029 b]",
                    "[\\N{LATIN CAPITAL LETTER A}]"
                }) {
            assertMatchesLikeJava(regex, Pattern.COMMENTS, "\\!");
        }
        assertMatchesLikeJava("[a b#]", 0);
        // UnicodeSet intersection and subtraction still work around comments.
        Pattern compiled =
                UnicodeRegex.compile(
                        "[[a-c] # include a through c\n - [b] # exclude b\n]", Pattern.COMMENTS);
        assertTrue(compiled.matcher("a").matches());
        assertTrue(compiled.matcher("c").matches());
        assertFalse(compiled.matcher("b").matches());
        assertFalse(compiled.matcher("#").matches());
        assertFalse(compiled.matcher(" ").matches());
    }

    @Test
    public void TestInlineFlags() {
        for (String regex :
                new String[] {
                    "(?x)[a # comment\n b]",
                    "(?x)[a # comment\n b](?-x)[# ]",
                    "(?x:[a # comment\n b])(?-x:[# ])",
                    "(?x)(?-x:[# ]) [a # comment\n b]",
                    "(?-x)((?x)[a # comment\n b])[# ]",
                    "(?-x)(?:(?x:[a # comment\n b])|[# ])[# ]",
                    "(?x:(?<letter>[a # comment\n b]))(?-x:[# ])",
                    "(?x)(?= [a # comment\n b]) [ab]",
                    "(?x)[(?-x)] [a # comment\n b]",
                    "(?x # [\\p{NotAProperty}\n :[a b])(?-x:[# ])",
                    "(?x-x)[# ]",
                    "(?x)](?-x)[# ]"
                }) {
            for (int flags : new int[] {0, Pattern.COMMENTS}) {
                assertMatchesLikeJava(
                        regex, flags, "a#", "b ", "#a", " b", "c#", "##", "a ", "  ", "xa", "]#");
            }
        }
    }

    @Test
    public void TestQuotedText() {
        for (String regex :
                new String[] {
                    "\\Q[ (?x) # \\p{Po}\\E[!]",
                    "[\\Q #()[]\\E]",
                    "\\Q# \\E # comment\n\\Q[\\E",
                    "\\Qx\\\\E[ab]",
                    "\\Q[ #",
                    "# \\Q comment\n[ab]\\E\n[c]"
                }) {
            for (int flags : new int[] {0, Pattern.COMMENTS}) {
                assertMatchesLikeJava(
                        regex, flags, "[ (?x) # \\p{Po}!", "# [", "x\\a", "[ #", "[ab]c");
            }
        }
    }

    @Test
    public void TestCommentLineEndings() {
        for (String end :
                new String[] {"\n", "\r", "\r\n", "\u0085", "\u2028", "\u2029", "\u0000"}) {
            String regex = "[a # comment" + end + "b\n]";
            for (int flags : new int[] {Pattern.COMMENTS, Pattern.COMMENTS | Pattern.UNIX_LINES}) {
                assertMatchesLikeJava(regex, flags);
            }
            assertMatchesLikeJava("(?dx)" + regex, 0);
            assertMatchesLikeJava(
                    "(?x)(?d:" + regex + ")(?-d:" + regex + ")", 0, "aa", "ab", "ba", "bb");
        }
    }

    @Test
    public void TestLiteralAndFix() {
        String regex = "(?x)[#]\\p{Po}";
        assertEquals(regex, UnicodeRegex.fix(regex, Pattern.LITERAL));
        assertTrue(
                UnicodeRegex.compile(regex, Pattern.LITERAL | Pattern.COMMENTS)
                        .matcher(regex)
                        .matches());

        String source = "[a # comment\n b]";
        Pattern compiled =
                Pattern.compile(UnicodeRegex.fix(source, Pattern.COMMENTS), Pattern.COMMENTS);
        assertTrue(compiled.matcher("a").matches());
        assertFalse(compiled.matcher("#").matches());
        assertFalse(compiled.matcher(" ").matches());
    }

    private static void assertMatchesLikeJava(String regex, int flags, String... strings) {
        Pattern expected = Pattern.compile(regex, flags);
        Pattern actual = UnicodeRegex.compile(regex, flags);
        for (int cp = 0; cp < 128; ++cp) {
            String s = Character.toString(cp);
            assertEquals(
                    expected.matcher(s).matches(),
                    actual.matcher(s).matches(),
                    () -> regex + ", flags=" + flags + ", U+" + Integer.toHexString(s.charAt(0)));
        }
        for (String s : strings) {
            assertEquals(
                    expected.matcher(s).matches(),
                    actual.matcher(s).matches(),
                    () -> regex + ", flags=" + flags + ", input=" + s);
        }
        for (String s : new String[] {"\u0085", "\u200E", "\u2028", "\u2029"}) {
            assertEquals(expected.matcher(s).matches(), actual.matcher(s).matches(), regex);
        }
    }
}
