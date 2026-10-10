package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Utility;

public class TestCodeInvariants {

    private static final boolean VERBOSE = false;
    private static final int TEST_PASS = 0;
    private static final int TEST_FAIL = -1;

    static final Set<Script_Values> IMPLICIT =
            Collections.unmodifiableSet(
                    EnumSet.of(
                            Script_Values.Unknown, Script_Values.Common, Script_Values.Inherited));

    static final Age_Values SCX_FIRST_DEFINED = Age_Values.V6_0;

    static final IndexUnicodeProperties IUP =
            IndexUnicodeProperties.make(Default.ucdVersion()); // Settings.latestVersion
    static final UnicodeMap<String> NAME = IUP.load(UcdProperty.Name);

    /**
     * This test checks the numbers in the big note under
     * https://www.unicode.org/reports/tr31/#R2-2. When the test fails, the expectations should be
     * updated to make it pass, and the table in UAX #31 should be updated. The name of the test
     * refers to an early name for XML 1.1 in 2001, see
     * https://www.w3.org/TR/2001/WD-xml11-20011213/, https://www.w3.org/TR/xml-blueberry-req/, and
     * the properties@ thread dated 2026-08-31 titled Blueberry. The identifier definition from XML
     * 1.1 was incorporated into XML 1.0 (Fifth Edition) in 2008.
     */
    @Test
    void compareBlueberry() {
        // The following two lines are verbatim from https://www.w3.org/TR/xml/#sec-common-syn,
        // except for some line wrapping.
        final String nameStartCharEBNF =
                """
                  ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D]
                | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF]
                | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
                """;
        final String nameCharEBNF =
                """
                NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
                """;

        final var xmlNameChar =
                new UnicodeSet(
                                "["
                                        + nameCharEBNF
                                                .replaceAll("NameStartChar", nameStartCharEBNF)
                                                .replaceAll("\"(.)\"", "{$1}")
                                                .replaceAll("\\|", "")
                                                .replaceAll("#x([0-9A-F]+)", "\\\\x{$1}")
                                        + "]")
                        .freeze();
        final var defaultIdentifiers =
                new UnicodeSet("[:XID_Continue:]", null, VersionedSymbolTable.forDevelopment())
                        .freeze();
        // See https://www.unicode.org/reports/tr31/#R2-1.
        final var immutableIdentifiers =
                new UnicodeSet(
                                "[^[:Pattern_White_Space:][:Pattern_Syntax:][:Private_Use:][:Surrogate:][:Control:][:Noncharacter_Code_Point:]]",
                                null,
                                VersionedSymbolTable.forDevelopment())
                        .freeze();

        // By the stability policy, this number cannot change.
        assertEquals(0, defaultIdentifiers.cloneAsThawed().removeAll(immutableIdentifiers).size());
        assertEquals(4, defaultIdentifiers.cloneAsThawed().removeAll(xmlNameChar).size());
        assertEquals(
                809_619, immutableIdentifiers.cloneAsThawed().removeAll(defaultIdentifiers).size());
        // By the stability policy, this number cannot change.
        assertEquals(259, immutableIdentifiers.cloneAsThawed().removeAll(xmlNameChar).size());
        assertEquals(809_556, xmlNameChar.cloneAsThawed().removeAll(defaultIdentifiers).size());
        // By the stability policy, this number cannot change.
        assertEquals(192, xmlNameChar.cloneAsThawed().removeAll(immutableIdentifiers).size());
    }

    @Test
    void testBlockRanges() {
        // https://github.com/unicode-org/unicodetools/issues/987
        UnicodeProperty blocks = IUP.getProperty(UcdProperty.Block);
        for (Block_Values block : Block_Values.values()) {
            if (block == Block_Values.No_Block) {
                continue;
            }
            UnicodeSet range = blocks.getSet(block);
            assertEquals(1, range.getRangeCount(), "Block must be one contiguous range: " + block);
            assertEquals(0x0, range.getRangeStart(0) % 16, "Block start must end in 0: " + block);
            assertEquals(0xF, range.getRangeEnd(0) % 16, "Block end must end in F: " + block);
        }
    }

    @Test
    public void testScriptExtensions() {
        int testResult = TEST_PASS;

        main:
        for (Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned
                    || age.compareTo(SCX_FIRST_DEFINED) < 0
                    || age == Age_Values.V13_1) { // skip irrelevants
                continue;
            }

            IndexUnicodeProperties current = IndexUnicodeProperties.make(age);
            UnicodeMap<Script_Values> script =
                    current.loadEnum(UcdProperty.Script, UcdPropertyValues.Script_Values.class);
            UnicodeMap<Set<Script_Values>> scriptExtension =
                    current.loadEnumSet(
                            UcdProperty.Script_Extensions, UcdPropertyValues.Script_Values.class);

            // Now test for each explicit value.

            for (Script_Values value : script.values()) {
                if (IMPLICIT.contains(value)) {
                    continue;
                }
                for (EntryRange range : script.getSet(value).ranges()) {
                    for (int codePoint = range.codepoint;
                            codePoint <= range.codepointEnd;
                            ++codePoint) {
                        Set<Script_Values> extensions = scriptExtension.get(codePoint);
                        if (!extensions.contains(value)) {
                            System.out.println(
                                    "FAIL: Script Extensions invariant doesn't work for version "
                                            + age
                                            + ": "
                                            + showInfo(codePoint, value, extensions));
                            testResult = TEST_FAIL;
                            break main;
                        } else if (VERBOSE && extensions.size() != 1) {
                            System.out.println("OK: " + showInfo(codePoint, value, extensions));
                        }
                    }
                    // don't need to test for strings in the set; there won't be any
                }
            }

            // We also have the invariants for implicit values, though not captured on the
            // stability_policy page, that
            // 1. BAD: scx={Common} and sc=Arabic.
            //    If a character has a script extensions value with 1 implicit element, then it must
            // be the script value for the character
            // 2. BAD: scx={Common, Arabic}
            //    NO script extensions value set with more than one element can contain an implicit
            // value

            for (Set<Script_Values> extensions : scriptExtension.values()) {
                if (extensions.size() == 1) {
                    Script_Values singleton = extensions.iterator().next();
                    if (!IMPLICIT.contains(singleton)) {
                        continue;
                    }
                    UnicodeSet setWithExtensions = scriptExtension.getSet(extensions);
                    UnicodeSet setWithSingleton = script.getSet(singleton);
                    if (setWithSingleton.containsAll(setWithExtensions)) {
                        continue;
                    }
                    // failure!
                    UnicodeSet diff = new UnicodeSet(setWithSingleton).removeAll(setWithExtensions);
                    int firstCodePoint = diff.getRangeStart(0);
                    Script_Values value = script.get(firstCodePoint);
                    System.out.println(
                            "FAIL: characters with implicit script value don't "
                                    + "contain those with that script extensions value "
                                    + age
                                    + ": "
                                    + showInfo(firstCodePoint, value, extensions));
                    testResult = TEST_FAIL;
                    continue;
                } else if (!Collections.disjoint(
                        extensions, IMPLICIT)) { // more than one element, so
                    int firstCodePoint = scriptExtension.getSet(extensions).getRangeStart(0);
                    Script_Values value = script.get(firstCodePoint);
                    System.out.println(
                            "FAIL: Script Extensions with >1 element contains implicit value "
                                    + age
                                    + ": "
                                    + showInfo(firstCodePoint, value, extensions));
                    testResult = TEST_FAIL;
                }
            }

            System.out.println("Script Extensions invariant works for version " + age + "\n");
        }

        assertEquals(TEST_PASS, testResult, "Invariant test for Script_Extensions failed!");
    }

    private static String showInfo(
            int codePoint, Script_Values value, Set<Script_Values> extensions) {
        return "sc: "
                + value
                + "\tscx: "
                + extensions
                + "\t"
                + Utility.hex(codePoint)
                + " ( "
                + Character.toString(codePoint)
                + " ) "
                + NAME.get(codePoint);
    }

    @Test
    void testPropertyAliasUniqueness() {
        // All property aliases constitute a single namespace. Property aliases are
        // guaranteed to be unique within this namespace.
        testLM3NamespaceUniqueness(
                Arrays.asList(UcdProperty.values()),
                property -> property.getNames().getAllNames(),
                Set.of("Age"),
                "!!Stability policy violation!! (Property Alias Uniqueness)");
        Set<Object> propertyNamespace = new HashSet<>();
        propertyNamespace.addAll(Arrays.asList(UcdProperty.values()));
        propertyNamespace.add("code point");
        propertyNamespace.add("none");
        testLM3NamespaceUniqueness(
                propertyNamespace,
                x ->
                        x instanceof String
                                ? List.of((String) x)
                                : ((UcdProperty) x).getNames().getAllNames(),
                Set.of("Age"),
                "Violation of UnicodeSet requirements: A property alias matches <code point> or <none>");
        for (var property : UcdProperty.values()) {
            if (IndexUnicodeProperties.make()
                    .getProperty(property)
                    .isType(UnicodeProperty.BINARY_OR_ENUMERATED_OR_CATALOG_MASK)) {
                Set<String> expectedRedundant;
                switch (property) {
                    case Block:
                        expectedRedundant = Set.of("Arabic_Presentation_Forms-A");
                        break;
                    case Decomposition_Type:
                        expectedRedundant =
                                Set.of(
                                        "can", "com", "enc", "fin", "font", "fra", "init", "iso",
                                        "med", "nar", "nb", "none", "sml", "sqr", "sub", "sup",
                                        "vert", "wide");
                        break;
                    case Sentence_Break:
                        expectedRedundant = Set.of("Sp");
                        break;
                    case IDNA2008_Category:
                        expectedRedundant = Set.of("Disallowed", "Unassigned");
                        break;
                    default:
                        expectedRedundant = Set.of();
                        break;
                }
                // For each property, all of its property value aliases constitute a separate
                // namespace, one per property. Within each of these property value alias
                // namespaces, property value aliases are guaranteed to be unique.
                testLM3NamespaceUniqueness(
                        property.getEnums(),
                        value -> ((Named) value).getNames().getAllNames(),
                        expectedRedundant,
                        "!!Stability policy violation!! (Property Alias Uniqueness for value aliases of "
                                + property
                                + ")");
            }
        }
        Set<Object> unicodeSetUnaryQueryNames =
                Arrays.stream(UcdProperty.values())
                        .filter(p -> p.getType() == PropertyType.Binary)
                        .collect(Collectors.toCollection(() -> new HashSet<>()));
        unicodeSetUnaryQueryNames.addAll(
                Arrays.asList(UcdPropertyValues.General_Category_Values.values()));
        unicodeSetUnaryQueryNames.addAll(Arrays.asList(UcdPropertyValues.Script_Values.values()));
        testLM3NamespaceUniqueness(
                unicodeSetUnaryQueryNames,
                x ->
                        x instanceof UcdProperty
                                ? ((UcdProperty) x).getNames().getAllNames()
                                : ((Named) x).getNames().getAllNames(),
                Set.of("Age"),
                "Violation of UnicodeSet requirements: gc-sc-binary property namespace collision");
        Set<Object> nonCollidingProperties = new HashSet<>();
        nonCollidingProperties.addAll(Arrays.asList(UcdProperty.values()));
        nonCollidingProperties.addAll(
                Arrays.asList(UcdPropertyValues.General_Category_Values.values()));
        nonCollidingProperties.addAll(Arrays.asList(UcdPropertyValues.Script_Values.values()));
        nonCollidingProperties.remove(UcdProperty.ISO_Comment); // Collides with gc Other.
        nonCollidingProperties.remove(UcdProperty.Case_Folding); // Collides with gc Format.
        nonCollidingProperties.remove(
                UcdProperty.Lowercase_Mapping); // Collides with gc Cased_Letter.
        nonCollidingProperties.remove(UcdProperty.Script); // Collides with gc Currency_Symbol.
        testLM3NamespaceUniqueness(
                nonCollidingProperties,
                x ->
                        x instanceof UcdProperty
                                ? ((UcdProperty) x).getNames().getAllNames()
                                : ((Named) x).getNames().getAllNames(),
                Set.of("Age"),
                "Unusual (not a violation of UnicodeSet requirement): New gc-sc-non-binary property namespace collision");
    }

    <T> void testLM3NamespaceUniqueness(
            Iterable<T> namespace,
            Function<T, List<String>> getNames,
            Set<String> expectedRedundant,
            String message) {
        final Map<String, T> entitiesByAlias = new TreeMap<>(UnicodeProperty.PROPERTY_COMPARATOR);
        final Map<String, String> aliasesByLM3Skeleton = new HashMap<>();
        for (T entity : namespace) {
            for (String alias : getNames.apply(entity)) {
                final var matchingEntity = entitiesByAlias.get(alias);
                final var lm3Skeleton = UnicodeProperty.toSkeleton(alias);
                final var matchingAlias = aliasesByLM3Skeleton.get(lm3Skeleton);
                assertTrue(
                        matchingEntity == null || entity.equals(matchingEntity),
                        message
                                + ": alias "
                                + alias
                                + " for "
                                + entity
                                + " matches alias "
                                + matchingAlias
                                + " for "
                                + matchingEntity);
                if (matchingEntity != null && !expectedRedundant.contains(alias)) {
                    assertEquals(
                            matchingAlias,
                            alias,
                            "Unusual (not a stability policy violation): distinct aliases for "
                                    + entity
                                    + " match each other");
                }
                entitiesByAlias.putIfAbsent(alias, entity);
                aliasesByLM3Skeleton.putIfAbsent(lm3Skeleton, alias);
            }
        }
    }
}
