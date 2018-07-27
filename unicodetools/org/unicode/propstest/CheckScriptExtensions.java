package org.unicode.propstest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class CheckScriptExtensions {
    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make();
    public static void main(String[] args) {
        UnicodeMap<Set<Script_Values>> scx = IUP.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
        
        Map<Script_Values,UnicodeSet> scriptToSet = new EnumMap<>(Script_Values.class);
        
        for (EntryRange<Set<Script_Values>> entry : scx.entryRanges()) {
            for (Script_Values script : entry.value) {
                UnicodeSet uset = scriptToSet.get(script);
                if (uset == null) {
                    scriptToSet.put(script, uset = new UnicodeSet());
                }
                uset.addAll(entry.codepoint, entry.codepointEnd);
            }
        }
        // print by script
        for (Entry<Script_Values, UnicodeSet> scriptEntry : scriptToSet.entrySet()) {
            UnicodeSet uset = scriptEntry.getValue().freeze();
            // clear everything else
            UnicodeMap<Set<Script_Values>> tempMap = new UnicodeMap<Set<Script_Values>>(scx).putAll(new UnicodeSet(uset).complement(), null);
            for (EntryRange<Set<Script_Values>> entry : tempMap.entryRanges()) {
                System.out.print(Utility.hex(entry.codepoint));
                if (entry.codepointEnd != entry.codepoint) {
                    System.out.print(".." + Utility.hex(entry.codepoint));
                }
                System.out.println(" ; \t" + CollectionUtilities.join(entry.value, " "));
            }
        }
    }
}
