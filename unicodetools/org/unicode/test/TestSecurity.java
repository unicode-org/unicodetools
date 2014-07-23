package org.unicode.test;

import java.util.EnumMap;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;

public class TestSecurity extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestSecurity().run(args);
    }

    enum Style {SL, SA, ML, MA}
    
    static class Data {
        final Style style;
        final String result;
        public Data(Style style, String result) {
            this.style = style;
            this.result = result;
        }
    }

    EnumMap<Style, UnicodeMap<String>> style2map = new EnumMap<Style,UnicodeMap<String>>(Style.class);
    UnicodeMap<EnumMap<Style,String>> char2data = new UnicodeMap<EnumMap<Style,String>>();

    @Override
    protected void init() throws Exception {
        for (String line : FileUtilities.in("/Users/markdavis/workspace/unicodetools/data/security/"
                + Settings.latestVersion, "confusables.txt")) {
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int hashPos = line.indexOf('#');
            line = line.substring(0,hashPos).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s*;\\s*");
            String source = Utility.fromHex(parts[0]);
            String target = Utility.fromHex(parts[1]);
            Style style = Style.valueOf(parts[2]);
            UnicodeMap<String> map = style2map.get(style);
            if (map == null) {
                style2map.put(style, map = new UnicodeMap());
            }
            map.put(source, target);
            EnumMap<Style, String> map2 = char2data.get(source);
            if (map2 == null) {
                char2data.put(source, map2 = new EnumMap(Style.class));
            }
            map2.put(style, target);
        }
    }

    public void TestIdempotence() {
        for (Entry<String, EnumMap<Style, String>> entry : char2data.entrySet()) {
            String code = entry.getKey();
            EnumMap<Style, String> map = entry.getValue();
            for (Entry<Style, String> codeToValue : map.entrySet()) {
                Style style = codeToValue.getKey();
                boolean warningOnly = style != Style.MA;

                UnicodeMap<String> transformMap = style2map.get(style);
                String value = codeToValue.getValue();
                String value2 = transformMap.transform(value);
                if (!value2.equals(value)) {
                    final String message = style
                            + "\tU+" + Utility.hex(code)+ " ( " + code + " ) "  + Default.ucd().getName(code)
                            + "\t\texpect:\tU+" + Utility.hex(value2, "U+") + " ( " + value2 + " ) " + Default.ucd().getName(value2)
                            + "\t\tactual:\tU+" + Utility.hex(value, "U+") + " ( " + value + " ) " + " " + Default.ucd().getName(value)
                            ;
                    if (warningOnly) {
                        warnln(message);
                    } else { 
                        errln(message); 
                    }
                }
            }
        }
    }
}

