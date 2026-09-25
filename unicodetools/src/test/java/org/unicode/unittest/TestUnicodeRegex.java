package org.unicode.unittest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeRegex;

public class TestUnicodeRegex {
    @Test
    public void TestNumberSignInCharacterClasses() {
        String[] patterns = {
            "[#]", "[\\#]", "[!-#]", "[#-%]", "\\p{Po}", "\\p{P}", "[\\p{Po}&\\p{Ascii}]"
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
        Pattern compiled = UnicodeRegex.compile("[#] # a comment\n [!]", Pattern.COMMENTS);
        assertTrue(compiled.matcher("#!").matches());
        assertFalse(compiled.matcher("#a").matches());
    }
}
