package org.unicode.text.tools;

import java.util.HashMap;
import java.util.List;

import org.unicode.text.UCD.ToolUnicodePropertySource;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class CompareProperties {
    public static void main(String[] args) {
        final ToolUnicodePropertySource u61 = ToolUnicodePropertySource.make("6.1.0");
        final ToolUnicodePropertySource u60 = ToolUnicodePropertySource.make("6.0.0");
        final UnicodeSet toTest = new UnicodeSet(0,0x10FFFF)
        .removeAll(u60.getSet("gc=Cn"))
        .removeAll(u60.getSet("gc=Cs"))
        .removeAll(u60.getSet("gc=Co"))
        .removeAll(u60.getSet("gc=Cc"))
        .freeze();

        final UnicodeMap<R2<String,String>> diff = new UnicodeMap();
        final Interner<R2<String,String>> interner = new Interner();

        for (final String prop : (List<String>) u61.getAvailableNames()) {
            //            if (!prop.equals("General_Category")) {
            //                continue;
            //            }
            final UnicodeProperty prop61 = u61.getProperty(prop);
            final int type = prop61.getType();
            if ((type & 1) != 0) {
                continue;
            }
            final UnicodeProperty prop60 = u60.getProperty(prop);
            diff.clear();
            for (final String s : toTest) {
                final int cp = s.codePointAt(0);
                final String v61 = prop61.getValue(cp, true);
                final String v60 = prop60.getValue(cp, true);
                if (UnicodeProperty.equals(v61, v60)) {
                    continue;
                }
                final R2<String,String> info = Row.of(v60,v61);
                diff.put(s, interner.get(info));
            }

            if (diff.size() != 0) {
                System.out.println(prop);
                for (final R2<String, String> i : diff.getAvailableValues()) {
                    final UnicodeSet set = diff.getSet(i);
                    System.out.println("\t" + i.get0() + " → " + i.get1()
                            + "\t" + set.size()
                            + "\t" + set
                            );
                }
            }
        }
    }

    static class Interner<T> {
        HashMap<T,T> items = new HashMap<T,T>();
        T get(T item) {
            T result = items.get(item);
            if (result == null) {
                items.put(item, item);
                result = item;
            }
            return result;
        }
    }
}
