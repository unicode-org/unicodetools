package org.unicode.text.tools;

import com.ibm.icu.text.UnicodeSet;

public class CompareIdentifiers {
    public static void main(String[] args) {

        final UnicodeSet javaStart = new UnicodeSet();
        final UnicodeSet javaPart = new UnicodeSet();
        final UnicodeSet javaUnicodeStart = new UnicodeSet();
        final UnicodeSet javaUnicodePart = new UnicodeSet();

        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            if (Character.isJavaIdentifierStart(cp)) {
                javaStart.add(cp);
            }
            if (Character.isJavaIdentifierPart(cp)) {
                javaPart.add(cp);
            }
            if (Character.isUnicodeIdentifierStart(cp)) {
                javaUnicodeStart.add(cp);
            }
            if (Character.isUnicodeIdentifierPart(cp)) {
                javaUnicodePart.add(cp);
            }
        }

        final UnicodeSet unicodeStart = new UnicodeSet("[:id_start:]").freeze();
        final UnicodeSet unicodePart = new UnicodeSet("[:id_continue:]").freeze();

        final UnicodeSet simulatedJavaStart = new UnicodeSet(unicodeStart)
        .addAll("[:gc=Pc:]")
        .addAll("[:gc=Sc:]").freeze();

        final UnicodeSet simulatedJavaPart = new UnicodeSet(unicodeStart)
        .addAll("[\\u0000-\\u0008 \\u000E-\\u001B \\u007F-\\u009F ]")
        .addAll("[:gc=Cf:]").freeze();

        showDiff("javaStart", javaStart, "simulatedJavaStart", simulatedJavaStart);
        showDiff("javaPart", javaPart, "simulatedJavaPart", simulatedJavaPart);
        showDiff("javaUnicodePart", javaUnicodePart, "unicodePart", unicodePart);
    }

    private static void showDiff(String title1, UnicodeSet set1, String title2, UnicodeSet set2) {
        System.out.printf("%s - %s\t%s\n", title1, title2, new UnicodeSet(set1).removeAll(set2));
    }
}
