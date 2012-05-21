package org.unicode.draft;

import java.io.PrintWriter;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;

public class ScriptCount {
    public static void main(String[] args) {
        for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
            UnicodeSet samples = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, script).retainAll(new UnicodeSet("[:l:]"));
            if (samples.size() == 0) continue;
            String first = samples.iterator().next();
            System.out.println(UScript.getName(script) + "\t" + UScript.getShortName(script) + "\t" + first);
        }
        //if (true) return;
        Counter<Integer> scriptCounter = new Counter<Integer>();
        Counter<String> mulCounter = CharacterFrequency.getCharCounter("mul");
        Normalizer2 nfkd = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
        for (String s : mulCounter) {
            long count = mulCounter.getCount(s);
            String norm = nfkd.normalize(s);
            int script = getScript(norm);
            scriptCounter.add(script, count);
        }
        for (Integer script : scriptCounter) {
            System.out.println(UScript.getShortName(script) + "\t" + UScript.getName(script) + "\t" + scriptCounter.get(script));
        }
        DecimalFormat pf = (DecimalFormat) NumberFormat.getPercentInstance();
        pf.setMaximumFractionDigits(6);
//        pf.setMinimumSignificantDigits(3);
//        pf.setMaximumSignificantDigits(3);
        int counter = 0;
        double max = mulCounter.getTotal();
        PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(UCD_Types.GEN_DIR + "/frequency-text", 
                "mul.txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
        for (String s : mulCounter.getKeysetSortedByCount(false)) {
            long count = mulCounter.get(s);
            int ch = s.codePointAt(0);
            // 0%   忌   U+5FCC  Lo  Hani    CJK UNIFIED IDEOGRAPH-5FCC
            out.println(pf.format(count/max) 
                    + "\t" + show(s) 
                    + "\tU+" + Utility.hex(s, 4, "&U+")
                    + "\t" + propValue(ch, UProperty.GENERAL_CATEGORY, UProperty.NameChoice.SHORT)
                    + "\t" + propValue(ch, UProperty.SCRIPT, UProperty.NameChoice.SHORT)
                    + "\t" + UCharacter.getName(s, "&"));
            if (counter++ > 10000) break;
        }
        out.close();
    }

    private static String propValue(int ch, int propEnum, int nameChoice) {
        return UCharacter.getPropertyValueName(propEnum, UCharacter.getIntPropertyValue(ch, propEnum), nameChoice);
    }

    private static String show(String s) {
        if (s.codePointAt(0) < 20) return "";
        if (s.equals("\"")) return "";
        return s;
    }

    private static int getScript(String norm) {
        int cp;
        int result = UScript.INHERITED;
        for (int i = 0; i < norm.length(); i += Character.charCount(cp)) {
            cp = norm.codePointAt(i);
            int script = UScript.getScript(cp);
            if (script == UScript.UNKNOWN) {
                int type = UCharacter.getType(cp);
                if (type == UCharacter.PRIVATE_USE) {
                    script = UScript.BLISSYMBOLS;
                }
            }
            if (script == UScript.INHERITED || script == result) continue;
            if (script == UScript.COMMON) {
                if (result == UScript.INHERITED) {
                    result = script;
                }
                continue;
            }
            if (result == UScript.COMMON || result == UScript.INHERITED) {
                result = script;
                continue;
            }
            // at this point both are different explicit scripts
            return UScript.COMMON;
        }
        return result;
    }
}
