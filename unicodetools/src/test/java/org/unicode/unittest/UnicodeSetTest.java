package org.unicode.unittest;

import com.ibm.icu.text.UnicodeSet;
import java.text.ParsePosition;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class UnicodeSetTest extends TestFmwkMinusMinus {
    @Test
    void testLatest() {
        final var iup = IndexUnicodeProperties.make();
        final var symbolTable = VersionedSymbolTable.forDevelopment();
        String path =
                org.unicode.text.utility.Utility.getMostRecentUnicodeDataFile(
                        "unicodeset/*/UnicodeSetTest", Settings.latestVersion, true, false);
        for (final String line : FileUtilities.in("", path)) {
            final int commentPosition = line.indexOf('#');
            final String contents =
                    commentPosition >= 0 ? line.substring(0, commentPosition) : line;
            if (contents.isEmpty()) {
                continue;
            }
            final String[] fields =
                    Arrays.stream(contents.split(";")).map(String::strip).toArray(String[]::new);
            final var scope = fields[0];
            final var general = fields[1];
            final var properties =
                    Arrays.stream(fields[2].split(" "))
                            .map(iup::getProperty)
                            .toArray(UnicodeProperty[]::new);
            final var elements =
                    fields[3].isEmpty()
                            ? new String[] {}
                            : Arrays.stream(
                                            fields[3]
                                                    .replaceFirst("^ *<", "")
                                                    .replaceFirst("> *$", "")
                                                    .split("> <"))
                                    .map(Utility::fromHex)
                                    .toArray(String[]::new);
            final var nonElements =
                    fields[4].isEmpty()
                            ? new String[] {}
                            : Arrays.stream(
                                            fields[4]
                                                    .replaceFirst("^ *<", "")
                                                    .replaceFirst("> *$", "")
                                                    .split("> <"))
                                    .map(Utility::fromHex)
                                    .toArray(String[]::new);
            final Integer size = fields[5].isBlank() ? null : Integer.parseInt(fields[5]);
            final var expression = fields[6];
            final var pp = new ParsePosition(0);
            if (scope.equals("Ill_Formed")) {
                assertEquals(
                        "Ill-formed test must not expect elements:\n" + line, 0, elements.length);
                assertEquals(
                        "Ill-formed test must not expect non-elements:\n" + line,
                        0,
                        nonElements.length);
                assertEquals("Ill-formed test must not expect size:\n" + line, null, size);
            }
            UnicodeSet setUnderTest = null;
            try {
                setUnderTest = new UnicodeSet(expression, pp, symbolTable);
                if (scope.equals("Ill_Formed")) {
                    System.out.println(
                            "EXTENSION: "
                                    + expression
                                    + " = "
                                    + setUnderTest.complement().complement()
                                    + " for\n"
                                    + line);
                }
            } catch (Exception e) {
                if (e.getMessage().contains("doubly negated property-query")
                        || e.getMessage()
                                .contains(
                                        "Unescaped Pattern_White_Space in UnicodeSet string literals is prohibited until ICU 81")) {
                    System.out.println("RESTRICTION: " + e.getMessage() + " for\n" + line);
                } else if (!scope.equals("Ill_Formed")) {
                    errln("Parse error " + e.getMessage() + " for " + line);
                }
                continue;
            }
            for (final String element : elements) {
                if (!setUnderTest.contains(element)) {
                    errln("element <" + Utility.hex(element) + "> " + element + " for\n" + line);
                }
            }
            for (final String element : nonElements) {
                if (setUnderTest.contains(element)) {
                    errln(
                            "non-element <"
                                    + Utility.hex(element)
                                    + "> "
                                    + element
                                    + " for\n"
                                    + line);
                }
            }
            if (size != null) {
                if (setUnderTest.size() != size) {
                    errln("size is " + setUnderTest.size() + " for\n" + line);
                }
            }
        }
    }
}
