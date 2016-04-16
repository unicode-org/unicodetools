package org.unicode.text.tools;

import java.util.TreeSet;

import org.unicode.cldr.util.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class CompareScriptExtensions {
    static final ToolUnicodePropertySource tups = ToolUnicodePropertySource.make(Default.ucdVersion());
    static final UnicodeProperty scriptProp = tups.getProperty("sc");
    static final UnicodeProperty scriptXProp = tups.getProperty("scx");

    public static void main(String[] args) {
        final UnicodeMap<String> diffs = new UnicodeMap();

        for (int i = 0; i <= 0x10FFFF; ++i) {
            final String spx = scriptXProp.getValue(i);
            if (spx == null) {
                continue;
            }
            final String sp = scriptProp.getValue(i);
            if (!sp.equals(spx)) {
                diffs.put(i, sp + "\t" + getLongForm(spx));
            }
        }
        final TreeSet<String> s = new TreeSet<String>(diffs.getAvailableValues());
        for (final String value : s) {
            UnicodeSet set = diffs.getSet(value);
            System.out.println(value + "\t" + set.toPattern(true));
            for (String c : set) {
                System.out.println("U+" + Utility.hex(c) + " (" + c + ") " + Default.ucd().getName(c));
            }
            System.out.println();
        }
    }

    private static String getLongForm(String spx) {
        final String[] items = spx.split(" ");
        final StringBuffer result = new StringBuffer();
        for (final String item : items) {
            if (result.length() != 0) {
                result.append(" ");
            }
            result.append(UScript.getName(UScript.getCodeFromName(item)));
        }
        return result.toString();
    }
}
