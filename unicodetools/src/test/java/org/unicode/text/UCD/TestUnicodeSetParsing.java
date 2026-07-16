package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.text.ParsePosition;
import org.junit.jupiter.api.Test;
import org.unicode.text.UCD.TestUnicodeInvariants.BackwardParseException;

public class TestUnicodeSetParsing {
    @Test
    void testUnicodeSetParsing() throws ParseException {
        assertEquals(
                26,
                TestUnicodeInvariants.parseUnicodeSet(
                                "TEST [\\N{LATIN SMALL LETTER A}-\\N{LATIN SMALL LETTER Z}]",
                                new ParsePosition(5))
                        .size());
        ParseException thrown =
                assertThrows(
                        ParseException.class,
                        () ->
                                TestUnicodeInvariants.parseUnicodeSet(
                                        "TEST [\\N{MEOW}]", new ParsePosition(5)));
        assertEquals("No character name nor name alias matches MEOW", thrown.getMessage());
        assertEquals("TEST [".length(), thrown.getErrorOffset());
        thrown =
                assertThrows(
                        BackwardParseException.class,
                        () ->
                                TestUnicodeInvariants.parseUnicodeSet(
                                        "TEST [[a-z]-\\N{LATIN SMALL LETTER Z}]",
                                        new ParsePosition(5)));
        assertEquals("Error: Set expected after operator", thrown.getMessage());
        assertEquals("TEST [[a-z]-.N{LATIN SMALL LETTER Z}".length(), thrown.getErrorOffset());
    }
}
