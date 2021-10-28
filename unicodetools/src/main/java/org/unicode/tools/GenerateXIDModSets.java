package org.unicode.tools;

import java.util.Locale;

import org.unicode.jsp.XIDModifications;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;


public class GenerateXIDModSets {
    public static void main(String[] args) {
        final UnicodeSet allowed = XIDModifications.getAllowed();
        final UnicodeMap<String> reasons = XIDModifications.getReasons();
        final UnicodeSet recommended = reasons.getSet("recommended").freeze();
        showSet("allowed", allowed);
        for (final String value : reasons.getAvailableValues()) {
            showSet(value, reasons.getSet(value));
        }
    }

    private static void showSet(String title, UnicodeSet set) {
        title = title.toUpperCase(Locale.ENGLISH).replace('-', '_');
        final String possibleBridge = "[[:Cn:][:nfkcqc=n:][:XIDC=n:]]";
        final String compact = getCompact(set, possibleBridge, true, 60); // "[[:Cn:][:nfkcqc=n:][:XID_Continue=n:]]");
        System.out.println("public static final UnicodeSet " + title + " = new UnicodeSet(" + compact + ");");
    }

    private static String getCompact(UnicodeSet original, String possibleBridge, boolean escape, int width) {
        final String originalString = original.toPattern(escape);
        String s = originalString;
        if (!possibleBridge.isEmpty()) {
            final UnicodeSet dontCare = new UnicodeSet(possibleBridge);
            if (dontCare.containsNone(original)) {
                final UnicodeSet compact = new UnicodeSet(original).addBridges(dontCare);
                final String compactString = "[" + compact.toPattern(escape) + "-" + possibleBridge + "]";

                if (compactString.length() < originalString.length()) {

                    final UnicodeSet roundTrip = new UnicodeSet(compactString);
                    if (!roundTrip.equals(original)) {
                        throw new IllegalArgumentException();
                    }
                    s = compactString;
                }
            }
        }

        s = s.substring(1, s.length() - 1);
        s = s.replace("\\", "\\\\");
        final StringBuilder b = new StringBuilder("\"[");

        for (int pos = 0;;) {
            int nextBreakPoint = pos + width;
            if (s.length() < nextBreakPoint) {
                if (b.length() > 0) {
                    b.append("\"\n+ \"");
                }
                b.append(s.substring(pos)).append("]\"");
                break;
            }
            nextBreakPoint = s.lastIndexOf("\\\\", nextBreakPoint);
            if (nextBreakPoint <= pos) {
                // should never happen
                throw new IllegalArgumentException();
            }
            if (b.length() > 1) {
                b.append("\"\n+ \"");
            }
            b.append(s.substring(pos,nextBreakPoint));
            pos = nextBreakPoint;
        }
        return b.toString();
    }

}
