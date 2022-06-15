/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateCaseTest.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.text.UTF16;
import java.io.IOException;
import java.io.PrintWriter;
import org.unicode.text.utility.Utility;

public abstract class GenerateCaseTest implements UCD_Types {

    public static void main(String[] args) throws IOException {
        System.out.println(
                "Remember to add length marks (half & full) and other punctuation for sentence, with FF61");

        final PrintWriter out =
                Utility.openPrintWriterGenDir("log/CaseTest.txt", Utility.UTF8_WINDOWS);

        out.println("# CaseTest");
        out.println("# Generated: " + Default.getDate() + ", MED");
        Utility.appendFile("CaseTestHeader.txt", Utility.LATIN1, out);

        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            Utility.dot(cp);
            if (!Default.ucd().isAllocated(cp)) {
                continue;
            }
            Default.ucd();
            if (UCD.isHangulSyllable(cp)) {
                continue;
            }
            final byte cat = Default.ucd().getCategory(cp);
            if (cp == PRIVATE_USE) {
                continue;
            }

            final String lower = Default.ucd().getCase(cp, FULL, LOWER);
            final String upper = Default.ucd().getCase(cp, FULL, UPPER);
            final String title = Default.ucd().getCase(cp, FULL, TITLE);
            final String fold = Default.ucd().getCase(cp, FULL, FOLD);
            if (lower.equals(upper) && lower.equals(title) && lower.equals(fold)) {
                continue;
            }

            String s = UTF16.valueOf(cp);
            write(out, s, true);

            // if (cp == '\u0345') continue; // don't add combining for this special case

            s = s + testChar;

            final String s2 = Default.nfd().normalize(s);

            final String lower1 = Default.nfc().normalize(Default.ucd().getCase(s2, FULL, LOWER));
            final String upper1 = Default.nfc().normalize(Default.ucd().getCase(s2, FULL, UPPER));
            final String title1 = Default.nfc().normalize(Default.ucd().getCase(s2, FULL, TITLE));
            final String fold1 = Default.nfc().normalize(Default.ucd().getCase(s2, FULL, FOLD));

            if (lower1.equals(Default.nfc().normalize(lower + testChar))
                    && upper1.equals(Default.nfc().normalize(upper + testChar))
                    && title1.equals(Default.nfc().normalize(title + testChar))
                    && fold1.equals(Default.nfc().normalize(fold + testChar))) {
                continue;
            }

            write(out, s, true);
        }
        out.println("# total lines: " + counter);
        out.close();
    }

    static final char testChar = '\u0316';
    static int counter = 0;

    static void write(PrintWriter out, String ss, boolean doComment) {
        final String s = Default.nfd().normalize(ss);
        final String lower = Default.nfc().normalize(Default.ucd().getCase(s, FULL, LOWER));
        final String upper = Default.nfc().normalize(Default.ucd().getCase(s, FULL, UPPER));
        final String title = Default.nfc().normalize(Default.ucd().getCase(s, FULL, TITLE));
        final String fold = Default.nfc().normalize(Default.ucd().getCase(s, FULL, FOLD));
        out.println(
                Utility.hex(ss)
                        + "; "
                        + Utility.hex(lower)
                        + "; "
                        + Utility.hex(upper)
                        + "; "
                        + Utility.hex(title)
                        + "; "
                        + Utility.hex(fold)
                        + (doComment ? "\t# " + Default.ucd().getName(ss) : ""));
        counter++;
    }
}
