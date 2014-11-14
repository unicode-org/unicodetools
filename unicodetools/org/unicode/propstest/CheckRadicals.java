package org.unicode.propstest;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.With;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class CheckRadicals {
    public static void main(String[] args) {
        getRadicals();
    }
    static void getRadicals() {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        UnicodeMap<String> radicals = iup.load(UcdProperty.CJK_Radical);
        Relation<String,String> ordered = Relation.of(new TreeMap(), TreeSet.class);
        for (Entry<String, String> entry : radicals.entrySet()) {
            String source = entry.getKey();
            String radicalNumber = entry.getValue();
            ordered.put(radicalNumber, source);
        }
        UnicodeSet remapped = new UnicodeSet();
        UnicodeSet CJKIdeograph = new UnicodeSet("[:Unified_Ideograph:]");
        for (Entry<String, Set<String>> entry : ordered.keyValuesSet()) {
            Set<String> values = entry.getValue();
            UnicodeSet us = new UnicodeSet().addAll(values).retainAll(CJKIdeograph);
            //String best = us.size() > 0 ? us.iterator().next() : values.iterator().next();
            String best = us.iterator().next();
            for (String s : values) {
                if (best.equals(s)) continue;
                showMapping(s, best);
                remapped.add(s);
            }
        }
        System.out.println(remapped);
    }
    private static void showMapping(String source, String target) {
        System.out.println(Utility.hex(source) + " ; " + Utility.hex(target) + " # "
                + "(" + source + "→" + target + ") "
                + getName(source," + ") + " → " + getName(target," + "));
    }

    private static String getName(String best, String separator) {
        StringBuilder b = new StringBuilder();
        for (int cp : With.codePointArray(best)) {
            if (b.length() > 0) {
                b.append(separator);
            }
            b.append(UCharacter.getExtendedName(cp));
        }
        return b.toString();
    }

}
