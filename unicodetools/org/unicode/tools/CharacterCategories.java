package org.unicode.tools;

import java.text.CollationKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.ULocale;

public class CharacterCategories {
    static final Comparator<String> COLLATOR;
    static {
        Collator COLLATOR1 = Collator.getInstance(ULocale.forLanguageTag("und-u-co-emoji"));
        COLLATOR1.setStrength(Collator.IDENTICAL);
        Comparator<String> stringComparator = new UTF16.StringComparator(true, false, 0);
        COLLATOR = new MultiComparator<String>(
                (Comparator) COLLATOR1, 
                (Comparator<String>) stringComparator);
    }
    private static final int LINE_WIDTH = 50;
    private static final Map<String, UnicodeSet> data = new TreeMap<>(COLLATOR);
    static final UnicodeSet nonspacing = new UnicodeSet("[[:Mn:][:Me:]]").freeze();

    static {
        String currentLabel = null;
        UnicodeSet currentSet = null;
        UnicodeSet ASCII_ID = new UnicodeSet("[_:A-Za-z0-9]").freeze();
        UnicodeSet UNASSIGNED = new UnicodeSet("[:cn:]").freeze();
        
        for (String line : FileUtilities.in(CharacterCategories.class, "characterCategories.txt")) {
            if (line.isEmpty()) {
                continue;
            }
            int idLen = ASCII_ID.span(line, SpanCondition.SIMPLE);
            if (idLen > 0) {
                currentLabel = line.substring(0, idLen);
                currentSet = data.get(currentLabel);
                if (currentSet == null) {
                    data.put(currentLabel, currentSet = new UnicodeSet());
                }
                line = line.substring(idLen);
            }
            line = line.trim();
            if (line.startsWith("#")) {
                //comment
            } else if (UnicodeSet.resemblesPattern(line, 0)) {
                currentSet.addAll(new UnicodeSet(line));
            } else if (line.startsWith("-") && UnicodeSet.resemblesPattern(line, 1)) {
                currentSet.removeAll(new UnicodeSet(line.substring(1)));
            } else {
                currentSet.addAll(line.replace(" ", ""));
            }
        }
        data.put("Symbols:Whitespace", new UnicodeSet("[:Whitespace:]"));
        data.put("Symbols:Format", new UnicodeSet("[[:Cf:][:Variation_Selector:][:block=Ideographic_Description_Characters:]]"));
        data.put("Symbols:Emoji", new UnicodeSet("[:emoji:]"));
        data.put("Symbols:Currency", new UnicodeSet(FixedProps.FixedGeneralCategory.getSet(General_Category_Values.Currency_Symbol))); // "[:sc:]"
        data.put("Symbols:Non-Spacing", new UnicodeSet(nonspacing));

        UnicodeSet common = FixedProps.FixedScriptExceptions.getSet(Collections.singleton(Script_Values.Common));
        UnicodeSet inherited = FixedProps.FixedScriptExceptions.getSet(Collections.singleton(Script_Values.Inherited));
        UnicodeSet control = FixedProps.FixedGeneralCategory.getSet(PropertyValueSets.CONTROL);
        UnicodeSet punctuation = FixedProps.FixedGeneralCategory.getSet(PropertyValueSets.PUNCTUATION);
        UnicodeSet missing = new UnicodeSet(common).addAll(inherited).removeAll(control).removeAll(punctuation);
        
        CollationKey x;

//                "["
//                + "[:scx=common:]"
//                + "[:scx=inherited:]"
//                + "-[:C:]"
//                + "-[:p:]"
//                + "]");
        UnicodeSet punc = new UnicodeSet(punctuation);
        for (Entry<String, UnicodeSet> entry : data.entrySet()) {
            UnicodeSet us = entry.getValue();
            us.removeAll(UNASSIGNED);
            us.freeze();
            missing.removeAll(us);
            punc.removeAll(us);
        }
        data.put("ZSymbol_missing", missing.freeze());
        data.put("Punctuation:Other", punc.freeze());
    }
    
    public static void main(String[] args) {
        UnicodeSet invisible = new UnicodeSet("[[:c:][:z:][:whitespace:][:di:][:Variation_Selector:]]").freeze();
        StringBuilder b = new StringBuilder();
        TreeSet<String> sorted = new TreeSet(COLLATOR);
        for (Entry<String, UnicodeSet> entry : data.entrySet()) {
            UnicodeSet us = entry.getValue();
            System.out.println(entry.getKey() + "\t# " + us.size());
            sorted.clear();
            us.addAllTo(sorted);
            int count = 0;
            b.setLength(0);
            for (String cp : sorted) {
                if (count++ > LINE_WIDTH) {
                    System.out.println("\t" + b);
                    b.setLength(0);
                    count = 0;
                }
                if (invisible.contains(cp)) {
                    b.append(" \\x{").append(Utility.hex(cp,1)).append("}");
                    count+=8;
                } else if (nonspacing.contains(cp)) {
                    b.append(" ").append(cp);
                    count++;
                } else {
                    b.append(cp);
                    count++;
                }
            }
            System.out.println("\t" + b);
        }
        for (Entry<String, UnicodeSet> entry : data.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue().size());
        }
    }
}
