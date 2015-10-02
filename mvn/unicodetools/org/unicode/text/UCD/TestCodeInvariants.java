package org.unicode.text.UCD;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class TestCodeInvariants {

    private static final boolean VERBOSE = true;

    static final Set<Script_Values> IMPLICIT = Collections.unmodifiableSet(
            EnumSet.of(Script_Values.Unknown, Script_Values.Common, Script_Values.Inherited));

    static final Age_Values SCX_FIRST_DEFINED = Age_Values.V6_0;

    static final UnicodeMap<String> NAME = IndexUnicodeProperties.make(Default.ucdVersion()).load(UcdProperty.Name);

    public static void main(String[] args) {
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
                        continue;
                    } else if (!Collections.disjoint(extensions, IMPLICIT)) { // more than one element, so
                        int firstCodePoint = scriptExtension.getSet(extensions).getRangeStart(0);
                        Script_Values value = script.get(firstCodePoint);
                        System.out.println("FAIL: Script Extensions with >1 element contains implicit value " + age
                                + ": " + showInfo(firstCodePoint, value, extensions));
                    }
                }


                System.out.println("Script Extensions invariant works for version " + age + "\n");
            }
    }

    private static String showInfo(int codePoint, Script_Values value, Set<Script_Values> extensions) {
        return "sc: " + value
                + "\tscx: " + extensions
                + "\t" + Utility.hex(codePoint) + " ( " + UTF16.valueOf(codePoint) + " ) " + NAME.get(codePoint);

    }
}
