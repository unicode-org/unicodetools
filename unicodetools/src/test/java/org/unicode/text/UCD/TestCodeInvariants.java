package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Grapheme_Cluster_Break_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class TestCodeInvariants {

    private static final boolean VERBOSE = false;
    private static final int TEST_PASS = 0;
    private static final int TEST_FAIL = -1;

    static final Set<Script_Values> IMPLICIT = Collections.unmodifiableSet(
            EnumSet.of(Script_Values.Unknown, Script_Values.Common, Script_Values.Inherited));

    static final Age_Values SCX_FIRST_DEFINED = Age_Values.V6_0;

    static final Normalizer2 NORM2_NFD = Normalizer2.getNFDInstance();
    static final UCD UCD_LATEST = UCD.makeLatestVersion();
    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Default.ucdVersion());    // Settings.latestVersion
    static final UnicodeMap<String> NAME = IUP.load(UcdProperty.Name);
    static final UnicodeMap<Grapheme_Cluster_Break_Values> GCB =
        IUP.loadEnum(UcdProperty.Grapheme_Cluster_Break, UcdPropertyValues.Grapheme_Cluster_Break_Values.class);

    @Test
    public void testScriptExtensions() {
        int testResult = TEST_PASS;

        main:
            for (Age_Values age : Age_Values.values()) {
                if (age == Age_Values.Unassigned
                        || age.compareTo(SCX_FIRST_DEFINED) < 0) { // skip irrelevants
                    continue;
                }

                IndexUnicodeProperties current = IndexUnicodeProperties.make(age);
                UnicodeMap<Script_Values> script = current.loadEnum(UcdProperty.Script, UcdPropertyValues.Script_Values.class);
                UnicodeMap<Set<Script_Values>> scriptExtension = current.loadEnumSet(UcdProperty.Script_Extensions, UcdPropertyValues.Script_Values.class);

                // Now test for each explicit value.

                for (Script_Values value : script.values()) {
                    if (IMPLICIT.contains(value)) {
                        continue;
                    }
                    for (EntryRange range : script.getSet(value).ranges()) {
                        for (int codePoint = range.codepoint; codePoint <= range.codepointEnd; ++codePoint) {
                            Set<Script_Values> extensions = scriptExtension.get(codePoint);
                            if (!extensions.contains(value)) {
                                System.out.println("FAIL: Script Extensions invariant doesn't work for version " + age
                                        + ": " + showInfo(codePoint, value, extensions));
                                testResult = TEST_FAIL;
                                break main;
                            } else if (VERBOSE && extensions.size() != 1){
                                System.out.println("OK: " + showInfo(codePoint, value, extensions));
                            }
                        }
                        // don't need to test for strings in the set; there won't be any
                    }
                }

                // We also have the invariants for implicit values, though not captured on the stability_policy page, that
                // 1. BAD: scx={Common} and sc=Arabic.
                //    If a character has a script extensions value with 1 implicit element, then it must be the script value for the character
                // 2. BAD: scx={Common, Arabic}
                //    NO script extensions value set with more than one element can contain an implicit value

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
                        System.out.println("FAIL: characters with implicit script value don't "
                                + "contain those with that script extensions value " + age
                                + ": " + showInfo(firstCodePoint, value, extensions));
                        testResult = TEST_FAIL;
                        continue;
                    } else if (!Collections.disjoint(extensions, IMPLICIT)) { // more than one element, so
                        int firstCodePoint = scriptExtension.getSet(extensions).getRangeStart(0);
                        Script_Values value = script.get(firstCodePoint);
                        System.out.println("FAIL: Script Extensions with >1 element contains implicit value " + age
                                + ": " + showInfo(firstCodePoint, value, extensions));
                        testResult = TEST_FAIL;
                    }
                }

                System.out.println("Script Extensions invariant works for version " + age + "\n");
            }

        assertEquals(TEST_PASS, testResult, "Invariant test for Script_Extensions failed!");
    }

    public void testGcbInDecompositions() {
        int testResult = TEST_PASS;

        final String gcbPropShortName = UcdProperty.Grapheme_Cluster_Break.getShortName();
        int count = 0;
        for (int cp = 0x0000; cp <= 0x10FFFF; ++cp) {

            if ((0xAC00 <= cp && cp <= 0xD7AF) || (0xF900 <= cp && cp <= 0xFAFF) || (0x2F800 <= cp && cp <= 0x2FA1F)) {
                continue;
            }

            final int cat = UCD_LATEST.getCategory(cp);
            if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                continue;
            }

            final String nfdOrNull = NORM2_NFD.getDecomposition(cp);
            if (nfdOrNull == null || nfdOrNull.length() <= 1) {
                continue;
            }

            int ch;
            boolean flagged = false;
            for (int i = 0; i < nfdOrNull.length(); i += Character.charCount(ch)) {
                ch = UTF16.charAt(nfdOrNull, i);
                if ((i > 0) && (GCB.get(ch) != UcdPropertyValues.Grapheme_Cluster_Break_Values.Extend)) {
                    flagged = true;
                    testResult = TEST_FAIL;
                }
            }

            if (VERBOSE || flagged) {
                System.out.print(Utility.hex(cp));
                System.out.print(" (" + gcbPropShortName + "=" + GCB.get(cp).getShortName() + ")");
                System.out.print("  ≡  " + Utility.hex(nfdOrNull) + " ( ");

                for (int i = 0; i < nfdOrNull.length(); i += Character.charCount(ch)) {
                    ch = UTF16.charAt(nfdOrNull, i);
                    System.out.print(gcbPropShortName + "=" + GCB.get(ch).getShortName() + " ");
                }

                System.out.print(")");
                System.out.print("  " + UTF16.valueOf(cp));
                System.out.print("  \"" + NAME.get(cp) + "\"");

                if (flagged) {
                    System.out.print("  ←");
                    ++count;
                }

                System.out.println();
            }
        }

        System.out.println("Count: " + count
            + " characters have non-singleton canonical decompositions whose any non-first characters are GCB≠EX (marked with \'←\').");

        assertEquals(TEST_PASS, testResult, "Invariant test for GCB in canonical decompositions failed!");
    }

    private static String showInfo(int codePoint, Script_Values value, Set<Script_Values> extensions) {
        return "sc: " + value
                + "\tscx: " + extensions
                + "\t" + Utility.hex(codePoint) + " ( " + UTF16.valueOf(codePoint) + " ) " + NAME.get(codePoint);
    }
}
