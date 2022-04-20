package org.unicode.text.tools;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.Counter;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

public class CharsByAgeAndCategory {
    public static void main(String[] args) {
        final ToolUnicodePropertySource source = ToolUnicodePropertySource.make(Default.ucd().getVersion());
        final UnicodeProperty ageProp = source.getProperty("age");
        final Map<String,Counter<Integer>> data = new TreeMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            String age = ageProp.getValue(i);
            if (age.equals("unassigned")) {
                age = "na";
            }
            Counter<Integer> counter = data.get(age);
            if (counter == null) {
                counter = new Counter<Integer>();
                data.put(age, counter);
            }
            final int cat = 0xFF & Default.ucd().getCategory(i);
            final int script = 0xFF & Default.ucd().getScript(i);
            int combined = (cat << 8) | script;
            if (i > 0xFFFF) {
                combined |= 0x10000;
            }
            counter.add(combined, 1);
        }
        for (final String age : data.keySet()) {
            final Counter<Integer> counter = data.get(age);
            for (final int item : counter.getKeysetSortedByKey()) {
                final boolean bmp = (item & 0x10000) == 0;
                Default.ucd();
                final String cat = UCD.getCategoryID_fromIndex((byte)((item >>> 8)&0xFF),UCD_Types.SHORT);
                Default.ucd();
                final String script = UCD.getScriptID_fromIndex((byte)(item),UCD_Types.SHORT);
                System.out.println(age + "\t" + (bmp ? "B" : "S") + "\t" + cat + "\t" + script + "\t" + counter.get(item));
            }
        }
    }
}
