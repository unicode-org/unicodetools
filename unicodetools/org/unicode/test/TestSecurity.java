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

    EnumMap<Style, UnicodeMap<String>> data = new EnumMap<Style,UnicodeMap<String>>(Style.class);

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
            UnicodeMap<String> map = data.get(style);
            if (map == null) {
                data.put(style, map = new UnicodeMap());
            }
            map.put(source, target);
        }
    }

    public void TestIdempotence() {
        for (Entry<Style, UnicodeMap<String>> entry : data.entrySet()) {
            Style key = entry.getKey();
            UnicodeMap<String> map = entry.getValue();
            for (Entry<String, String> codeToValue : map.entrySet()) {
                String code = codeToValue.getKey();
                String value = codeToValue.getValue();
                String value2 = map.transform(value);
                if (!value2.equals(value)) {
                    errln(key
                            + ", " + Utility.hex(code)+ " ( " + code + " )"  + Default.ucd().getName(code)
                            + "\n\t\texpect:\t" + Utility.hex(value2) + " ( " + value2 + " )" + Default.ucd().getName(value2)
                            + "\n\t\tactual:\t" + Utility.hex(value) + " ( " + value + " )" + " " + Default.ucd().getName(value)
                            + "\n"
                            );
                }
            }
        }
    }
}
