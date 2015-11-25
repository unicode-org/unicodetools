package org.unicode.tools;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class AacCheck {
    private static final UnicodeSet FAIL = new UnicodeSet("[:c:]").freeze();
    private static final UnicodeSet IGNORE = new UnicodeSet("[[:z:][:di:]]").freeze();
    private static final UnicodeSet INITIAL_EXCLUSIONS = new UnicodeSet("[࿕-࿘ 卍 卐]").freeze();
    /**
     * input is a space-delimited list of hex values
     * @param args
     * @return
     */
    public static void main(String[] args) {
        StringBuilder filtered = new StringBuilder();
        // process the input to make sure the code points are ok
        for (String arg : args) {
            int cp = -1;
            try {
                cp = Integer.parseInt(arg, 16);            
            } catch (Exception e) {} // fall through with -1

            if (cp < 0 || cp > 0x10FFFF) {
                System.out.println(";Error: bad codepoint: " + arg);
                System.exit(8);
            } else if (FAIL.contains(cp)) {
                System.out.println(";Not registerable: " + arg);
                System.exit(8);
            } else if (!IGNORE.contains(cp)) {
                filtered.appendCodePoint(cp);
            }
        }
        if (filtered.length() == 0) {
            System.out.println(";Error: need 1 argument");
            System.exit(1);
        }
        String filteredString = filtered.toString();
        String name = UCharacter.getName(filteredString, " + ");
        System.out.println(com.ibm.icu.impl.Utility.hex(filteredString) + ";" + name);
        System.exit(0);
    }
}
