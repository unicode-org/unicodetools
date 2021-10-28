package org.unicode.text.UCD;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class ScriptTimeline {
    public static void main(String[] args) {
        final String[] versions = { "2.0.0", "2.1.2", "3.0.0", "3.1.0", "3.2.0", "4.0.0", "4.1.0", "5.0.0", "5.2.0", "6.0.0", "6.1.0" };
        for (int s = 0; s < UScript.CODE_LIMIT; ++s) {
            final String scriptName = UScript.getName(s);
            final UnicodeSet chars = new UnicodeSet().applyPropertyAlias("script", scriptName);
            if (chars.size() == 0) {
                continue;
            }
            System.out.print(scriptName);
            for (final String version : versions) {
                final UnicodeSet age = new UnicodeSet();
                age.applyPropertyAlias("age", version);
                System.out.print("\t" + new UnicodeSet(chars).retainAll(age).size());
            }
            System.out.println();
        }


    }
}