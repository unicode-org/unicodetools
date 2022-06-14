package org.unicode.propstest;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;

public class ShowTirhuta {
    public static void main(String[] args) {

        Transform<String, String> t =
                Transliterator.createFromRules(
                        "id", "([:di:]) > &hex($1);", Transliterator.FORWARD);
        String source = "abc\u00ADd\u034Fe";
        String formatted = t.transform(source);
        System.out.println(source + " => " + formatted);

        UnicodeSet us = new UnicodeSet("[:di:]").freeze();
        UnicodeSet x = new UnicodeSet().addAll(source).retainAll(us);
        StringBuilder b = new StringBuilder();
        for (String s : x) {
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append("U+")
                    .append(Utility.hex(s.codePointAt(0)))
                    .append(' ')
                    .append(UCharacter.getName(s.codePointAt(0)));
        }
        System.out.println(b);
        // abc­d => abc\u00ADd

        if (true) return;
        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        UnicodeProperty scriptProp = latest.getProperty("sc");
        UnicodeProperty nameProp = latest.getProperty("name");
        UnicodeSet bengali = scriptProp.getSet("bengali");
        UnicodeSet tirhuta = scriptProp.getSet("Tirhuta");
        Map<String, Integer> bengaliToName = getMap(nameProp, bengali, "BENGALI ");
        Map<String, Integer> tirhutaToName = getMap(nameProp, tirhuta, "TIRHUTA ");
        Map<Integer, String> results = new TreeMap();
        for (Entry<String, Integer> s : tirhutaToName.entrySet()) {
            final String name = s.getKey();
            Integer bcp = bengaliToName.get(name);
            if (bcp == null) {
                continue;
            }
            results.put(s.getValue(), ";\t" + UTF16.valueOf(bcp) + "\t# " + name);
        }
        for (Entry<Integer, String> s : results.entrySet()) {
            System.out.println(UTF16.valueOf(s.getKey()) + s.getValue());
        }
    }

    public static Map<String, Integer> getMap(
            UnicodeProperty nameProp, UnicodeSet tirhuta, String scriptPrefix) {
        Map<String, Integer> tirhutaToName = new HashMap();
        for (String s : tirhuta) {
            final int codePoint = s.codePointAt(0);
            String name = nameProp.getValue(codePoint);
            name = removeAtStart(name, scriptPrefix);
            name = removeAtStart(name, "LETTER ");
            tirhutaToName.put(name, codePoint);
        }
        return tirhutaToName;
    }

    public static String removeAtStart(String name, String start) {
        if (name.startsWith(start)) {
            name = name.substring(start.length());
        }
        return name;
    }
}
