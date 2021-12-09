package org.unicode.unittest;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Identifier_Type_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Type;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class TestIdentifierInfo extends TestFmwkMinusMinus {
    static final IndexUnicodeProperties uip = IndexUnicodeProperties.make("12.0");
    static final UnicodeSet NotCharacter = uip.loadEnumSet(
            UcdProperty.General_Category, General_Category_Values.Unassigned);

    static final UnicodeSet SpecialExclusions = new UnicodeSet(
            "[[\\p{Extender=True}&\\p{Joining_Type=Join_Causing}]"
                    + "\\p{Default_Ignorable_Code_Point}"
                    + "\\p{block=Combining_Diacritical_Marks_for_Symbols}"
                    + "\\p{block=Musical_Symbols}"
                    + "\\p{block=Ancient_Greek_Musical_Notation}"
                    + "\\p{block=Phaistos_Disc}]");

    static final UnicodeSet SpecialExclusions2 = new UnicodeSet("[\\U00011301\\U00011303\\U0001133C]");

        @Test
    public void testUax31Overlap() {
        UnicodeMap<Identifier_Type_Values> scriptUsage = getScriptUsage();
        Set<Identifier_Type_Values> inclusion = ImmutableSet.of(Identifier_Type_Values.Inclusion);
        Set<Identifier_Type_Values> notRec = ImmutableSet.of(
                Identifier_Type_Values.Exclusion, Identifier_Type_Values.Limited_Use);

        UnicodeMap<Set<Identifier_Type_Values>> idType = uip.loadEnumSet(UcdProperty.Identifier_Type,Identifier_Type_Values.class);

        UnicodeMap<String> mismatch = new UnicodeMap<>();

        for (EntryRange<Identifier_Type_Values> entry : scriptUsage.entryRanges()) {
            main:
                for (int cp = entry.codepoint; cp <= entry.codepointEnd; ++cp) {
                    Set<Identifier_Type_Values> idTypes = idType.get(cp);
                    if (inclusion.equals(idTypes)) { // skip testing inclusion
                        continue;
                    }
                    Identifier_Type_Values value = entry.value;
                    switch (value) {
                    case Recommended:
                        if (Collections.disjoint(idTypes, notRec)) {
                            continue main;
                        }
                        break;
                    case Exclusion:
                        if (NotCharacter.contains(cp)) {
                            continue main;
                        }
                    case Limited_Use:
                        if (idTypes.contains(value)) {
                            continue main;
                        }
                        break;
                    }
                    mismatch.put(cp, value + " ∉ " + idTypes);
                }
        }
        System.out.println();
        TreeSet<String> sorted = new TreeSet<>(mismatch.values());
        for (String value : sorted) {
                errln(value + ": " + mismatch.getSet(value));
        }
    }


    private UnicodeMap<Identifier_Type_Values> getScriptUsage() {
        UnicodeMap<Identifier_Type_Values> results = new UnicodeMap<>();
        UnicodeMap<Set<Script_Values>> scriptExtensions = uip.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
        for (Set<Script_Values> scriptSet : scriptExtensions.values()) {
            Identifier_Type_Values u31 = getValues(scriptSet);
            if (u31 != null) {
                UnicodeSet uset = scriptExtensions.getSet(scriptSet);
                if (uset.contains(0x09E6)) {
                    int debug = 0;
                    getValues(scriptSet);
                }
                results.putAll(uset, u31);
            }
        }
        results.putAll(SpecialExclusions, Identifier_Type_Values.Exclusion);
        results.putAll(SpecialExclusions2, Identifier_Type_Values.Exclusion);

        return results;
    }

    private Identifier_Type_Values getValues(Set<Script_Values> scriptSet) {
        Identifier_Type_Values best = null;
        for (Script_Values script : scriptSet) {
            if (script == Script_Values.Unknown || script == Script_Values.Inherited || script == Script_Values.Common) {
                continue;
            }
            Info info = ScriptMetadata.getInfo(script.getShortName());
            switch(info.idUsage) {
            case RECOMMENDED:
                return Identifier_Type_Values.Recommended;
            case LIMITED_USE:
                if (best != Identifier_Type_Values.Limited_Use) {
                    best = Identifier_Type_Values.Limited_Use;
                }
                break;
            case EXCLUSION:
                if (best == null) {
                    best = Identifier_Type_Values.Exclusion;
                }
                break;
            }
        }
        return best;
    }

}
