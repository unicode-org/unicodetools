package org.unicode.propstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.props.UnicodeProperty;

public class TestUnicodeProperty {
    private static UnicodeProperty.UnicodeMapProperty property(
            String name, int type, UnicodeMap<String> map) {
        UnicodeProperty.UnicodeMapProperty result =
                new UnicodeProperty.UnicodeMapProperty() {
                    @Override
                    protected boolean hasStrings() {
                        return unicodeMap.stringKeys() != null
                                && !unicodeMap.stringKeys().isEmpty();
                    }

                    @Override
                    public boolean hasUniformUnassigned() {
                        return false;
                    }
                };
        result.set(map).setMain(name, name, type, "test");
        return result;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Yes", "No"})
    void testBinaryLookupDuringClassInitialization(String firstValue, @TempDir Path tempDir)
            throws Exception {
        // A fresh JVM is required: other tests may already have initialized UnicodeProperty.
        Path output = tempDir.resolve("initialization.log");
        Process process =
                new ProcessBuilder(
                                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                                "-cp",
                                System.getProperty(
                                        "surefire.test.class.path",
                                        System.getProperty("java.class.path")),
                                InitializationProbe.class.getName(),
                                firstValue)
                        .redirectErrorStream(true)
                        .redirectOutput(output.toFile())
                        .start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Class initialization timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            process.destroyForcibly();
        }
    }

    public static class InitializationProbe {
        public static void main(String[] args) {
            // Model GenerateIdnaTest's startup: install a symbol table before UnicodeProperty
            // initializes NONCHARACTERS. Resolving that set reenters getSet during initialization.
            UnicodeSet.setDefaultXSymbolTable(
                    new UnicodeSet.XSymbolTable() {
                        @Override
                        public boolean applyPropertyAlias(
                                String propertyName, String propertyValue, UnicodeSet result) {
                            if (!propertyName.equals("noncharactercodepoint")) {
                                return false;
                            }
                            UnicodeProperty p =
                                    property(
                                            "DuringInitialization",
                                            UnicodeProperty.BINARY,
                                            new UnicodeMap<String>()
                                                    .put('A', "Yes")
                                                    .put('B', "No"));
                            // Exercise both caches, varying which one is initialized first.
                            for (String value :
                                    new String[] {args[0], args[0].equals("Yes") ? "No" : "Yes"}) {
                                assertEquals(
                                        new UnicodeSet().add(value.equals("Yes") ? 'A' : 'B'),
                                        p.getSet(value));
                            }
                            result.clear().add('A');
                            return true;
                        }
                    });
            assertEquals(new UnicodeSet("[A]"), UnicodeProperty.NONCHARACTERS);
        }
    }

    @Test
    void testBinaryAliasesAndMutableResults() {
        UnicodeProperty p =
                property(
                        "Binary",
                        UnicodeProperty.BINARY,
                        new UnicodeMap<String>().putAll(0, 0x10FFFF, "No").put('A', "Yes"));
        UnicodeSet yes = new UnicodeSet("[A]");
        UnicodeSet no = new UnicodeSet(yes).complement();
        String[][] aliases = {
            {"Yes", "Y", "T", "True", "y_E-s", "isYES"},
            {"No", "N", "F", "False", "n_O", "isNO"}
        };
        UnicodeSet[] expected = {yes, no};
        // Repeat after modifying both returned sets and caller-supplied destination sets.
        for (int pass = 0; pass < 2; ++pass) {
            for (int i = 0; i < aliases.length; ++i) {
                for (String alias : aliases[i]) {
                    UnicodeSet actual = p.getSet(alias);
                    assertEquals(expected[i], actual, alias);
                    assertFalse(actual.isFrozen());
                    actual.clear().add("changed");
                    UnicodeSet destination = new UnicodeSet().add("kept");
                    assertSame(destination, p.getSet(alias, destination));
                    assertEquals(new UnicodeSet(expected[i]).add("kept"), destination);
                    destination.clear();
                }
            }
        }
        assertTrue(p.getSet("invalid").isEmpty());
        assertTrue(p.getSet((String) null).isEmpty());
    }

    @Test
    void testBinaryStringsAndMissingValues() {
        UnicodeProperty p =
                property(
                        "Partial",
                        UnicodeProperty.BINARY,
                        new UnicodeMap<String>()
                                .put('A', "Yes")
                                .put('B', "No")
                                .put("aa", "Yes")
                                .put("bb", "No"));
        UnicodeSet yes = new UnicodeSet("[A{aa}]");
        UnicodeSet no = new UnicodeSet("[B{bb}]");
        // Query No first, then again after the Yes lookup, including cached aliases.
        for (int pass = 0; pass < 2; ++pass) {
            assertEquals(no, p.getSet("No"));
            assertEquals(yes, p.getSet("Yes"));
            assertEquals(no, p.getSet("False"));
            assertEquals(yes, p.getSet("True"));
        }
    }

    @Test
    void testBinaryMissingValues() {
        UnicodeProperty p =
                property(
                        "Partial",
                        UnicodeProperty.BINARY,
                        new UnicodeMap<String>().put('A', "Yes").put('B', "No"));
        for (int pass = 0; pass < 2; ++pass) {
            assertEquals(new UnicodeSet("[A]"), p.getSet("Yes"));
            assertEquals(new UnicodeSet("[B]"), p.getSet("No"));
            assertEquals(new UnicodeSet("[AB]").complement(), p.getSet((String) null));
        }
    }

    @Test
    void testExtendedBinaryAliasesRemainDistinct() {
        // Unlike BINARY, EXTENDED_BINARY does not automatically add all standard aliases.
        UnicodeProperty.UnicodeMapProperty p =
                property(
                        "Extended",
                        UnicodeProperty.EXTENDED_BINARY,
                        new UnicodeMap<String>().put('A', "Yes").put('B', "No"));
        p.addValueAliases(
                new String[][] {{"Yes", "Enabled"}},
                UnicodeProperty.AliasAddAction.REQUIRE_MAIN_ALIAS);
        for (int pass = 0; pass < 2; ++pass) {
            assertEquals(new UnicodeSet("[A]"), p.getSet("Yes"));
            assertEquals(new UnicodeSet("[B]"), p.getSet("No"));
            assertEquals(new UnicodeSet("[A]"), p.getSet("Enabled"));
            for (String absent : new String[] {"Y", "T", "True", "N", "F", "False"}) {
                assertTrue(p.getSet(absent).isEmpty(), absent);
            }
        }
    }

    @Test
    void testBinaryLookupUsesSubclassResult() {
        // Model an overlay such as IndexUnicodeProperty's version-delta lookup. Its effective
        // values differ from its raw map, and No is not the code-point complement of Yes.
        UnicodeProperty p =
                new UnicodeProperty.UnicodeMapProperty() {
                    @Override
                    public UnicodeSet getSet(PatternMatcher matcher, UnicodeSet result) {
                        if (result == null) result = new UnicodeSet();
                        if (matcher.test("Yes")) result.add('A').add("aa");
                        if (matcher.test("No")) result.add('B').add("bb");
                        return result;
                    }
                }.set(new UnicodeMap<String>().put('A', "No").put('B', "Yes"))
                        .setMain("Overlay", "Overlay", UnicodeProperty.BINARY, "test");
        for (int pass = 0; pass < 2; ++pass) {
            assertEquals(new UnicodeSet("[A{aa}]"), p.getSet("Yes"));
            assertEquals(new UnicodeSet("[B{bb}]"), p.getSet("No"));
        }
    }

    @Test
    void testMultivaluedMatching() {
        for (int type : new int[] {UnicodeProperty.ENUMERATED, UnicodeProperty.CATALOG}) {
            UnicodeProperty.UnicodeMapProperty p =
                    property(
                            "Scripts",
                            type,
                            new UnicodeMap<String>()
                                    .put('A', "Latin Greek")
                                    .put('B', "Latin")
                                    .put('C', "Greek"));
            p.setMultivalued(true).setDelimiter(" ");
            p.addValueAliases(
                    new String[][] {{"Latin", "Latn"}},
                    UnicodeProperty.AliasAddAction.REQUIRE_MAIN_ALIAS);
            assertEquals(new UnicodeSet("[AB]"), p.getSet("Latin"));
            assertEquals(new UnicodeSet("[AB]"), p.getSet("Latn"));
            assertEquals(new UnicodeSet("[AC]"), p.getSet("Greek"));
            assertEquals(
                    new UnicodeSet("[AB]"),
                    p.getSet(new UnicodeProperty.RegexMatcher().set("^Lat")));
            assertTrue(p.getSet(new UnicodeProperty.RegexMatcher().set("Latin Greek")).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> p.getSet("Latin Greek"));
        }
    }

    @Test
    void testSimpleMatcherResetAndNull() {
        UnicodeProperty.PatternMatcher matcher =
                new UnicodeProperty.SimpleMatcher(
                        "upper case-letter", UnicodeProperty.PROPERTY_COMPARATOR);
        assertTrue(matcher.test("Uppercase_Letter"));
        assertTrue(matcher.test("IS_UPPERCASE_LETTER"));
        assertFalse(matcher.test(null));
        matcher.set("Lowercase_Letter");
        assertFalse(matcher.test("Uppercase_Letter"));
        assertTrue(matcher.test("lowercase letter"));
        matcher.set(null);
        assertTrue(matcher.test(null));
        assertFalse(matcher.test("Lowercase_Letter"));
    }

    @Test
    void testNumericMatching() {
        UnicodeProperty p =
                property(
                        "Numeric_Value",
                        UnicodeProperty.NUMERIC,
                        new UnicodeMap<String>().put('A', "1/2").put('B', "NaN"));
        for (String value : new String[] {"1/2", "2/4", "0.5"}) {
            assertEquals(new UnicodeSet("[A]"), p.getSet(value), value);
        }
        assertEquals(new UnicodeSet("[B]"), p.getSet("nan"));
        assertEquals(new UnicodeSet("[AB]").complement(), p.getSet((String) null));
    }

    @Test
    void testCharacterNameMatching() {
        UnicodeMap<String> names =
                new UnicodeMap<String>()
                        .put(0x1180, "HANGUL JUNGSEONG O-E")
                        .put(0x116C, "HANGUL JUNGSEONG OE");
        for (String propertyName : new String[] {"Name", "Name_Alias"}) {
            UnicodeProperty p = property(propertyName, UnicodeProperty.STRING, names);
            assertEquals(new UnicodeSet().add(0x1180), p.getSet("hangul jungseong o-e"));
            assertEquals(new UnicodeSet().add(0x116C), p.getSet("hangul jungseong oe"));
        }
    }

    @Test
    void testStringMatchingIsExact() {
        UnicodeProperty p =
                property(
                        "String",
                        UnicodeProperty.STRING,
                        new UnicodeMap<String>().put('A', "Uppercase_Letter"));
        assertEquals(new UnicodeSet("[A]"), p.getSet("Uppercase_Letter"));
        assertTrue(p.getSet("uppercase letter").isEmpty());
    }
}
