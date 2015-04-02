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
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class TestCodeInvariants {

    private static final boolean VERBOSE = true;

    static final Set<Script_Values> IMPLICIT = Collections.unmodifiableSet(
            EnumSet.of(Script_Values.Unknown, Script_Values.Common, Script_Values.Inherited));
    
    static final Age_Values SCX_FIRST_DEFINED = Age_Values.V6_0;

    static final UnicodeMap<String> NAME = IndexUnicodeProperties.make(Default.ucdVersion()).load(UcdProperty.Name);

    public static void main(String[] args) {

        for (Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned 
                    || age.compareTo(SCX_FIRST_DEFINED) < 0) { // skip irrelevants
                continue;
            }
            
            IndexUnicodeProperties current = IndexUnicodeProperties.make(age);
            UnicodeMap<Script_Values> script = current.loadEnum(UcdProperty.Script, UcdPropertyValues.Script_Values.class);
            UnicodeMap<Set<Script_Values>> scriptExtension = current.loadSet(UcdProperty.Script_Extensions, 
                    UcdPropertyValues.Script_Values.class, UcdProperty.Script);
            
            // Now test for each explicit value.

            for (Script_Values value : script.values()) {
                if (IMPLICIT.contains(value)) {
                    continue;
                }
                for (EntryRange range : script.getSet(value).ranges()) {
                    for (int codePoint = range.codepoint; codePoint <= range.codepointEnd; ++codePoint) {
                        Set<Script_Values> extensions = scriptExtension.get(codePoint);
                        if (!extensions.contains(value)) {
                            System.out.println("Script Extensions invariant doesn't work for version " + age
                                    + ": " + showInfo(codePoint, value, extensions));
                        } else if (VERBOSE && extensions.size() != 1){
                            System.out.println("OK: " + showInfo(codePoint, value, extensions));
                        }
                    }
                    // don't need to test for strings in the set; there won't be any
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
