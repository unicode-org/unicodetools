package org.unicode.text.tools;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;

import com.ibm.icu.dev.util.UnicodeProperty;

public class CharsByAgeAndCategory {
    public static void main(String[] args) {
        ToolUnicodePropertySource source = ToolUnicodePropertySource.make(Default.ucd().getVersion());
        UnicodeProperty ageProp = source.getProperty("age");
        Map<String,Counter<Integer>> data = new TreeMap();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            String age = ageProp.getValue(i);
            if (age.equals("unassigned")) age = "na";
            Counter<Integer> counter = data.get(age); 
            if (counter == null) {
                counter = new Counter<Integer>();
                data.put(age, counter);
            }
            int cat = 0xFF & Default.ucd().getCategory(i);
            int script = 0xFF & Default.ucd().getScript(i);
            int combined = (cat << 8) | script;
            if (i > 0xFFFF) {
                combined |= 0x10000;
            }
            counter.add(combined, 1);
        }
        for (String age : data.keySet()) {
            Counter<Integer> counter = data.get(age); 
            for (int item : counter.getKeysetSortedByKey()) {
                boolean bmp = (item & 0x10000) == 0;
                String cat = Default.ucd().getCategoryID_fromIndex((byte)((item >>> 8)&0xFF),UCD.SHORT);
                String script = Default.ucd().getScriptID_fromIndex((byte)(item),UCD.SHORT);
                System.out.println(age + "\t" + (bmp ? "B" : "S") + "\t" + cat + "\t" + script + "\t" + counter.get(item));
            }
        }
    }
}
