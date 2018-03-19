package org.unicode.test;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.parse.EBNF;
import org.unicode.parse.EBNF.Position;

import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeBnf {
    static final boolean DEBUG = true;
    
    public static void main(String[] args) {
        EBNF bnf = new EBNF();

        for (String line : FileUtilities.in(TestUnicodeBnf.class, "UnicodeSetBnf.txt")) {
            bnf.addRules(line);
        }
        bnf.build();

        System.out.println(bnf.getInternal());

        String status = "";
        Position p = new Position();

        int lineCount = 0;
        for (String line : FileUtilities.in(TestUnicodeBnf.class, "UnicodeTest.txt")) {
            ++lineCount;
            if (line.startsWith("@")) {
                status = line;
                continue;
            }

            boolean bnfValue;
            Exception bnfException = null;
            try {
                bnfValue = bnf.match(line, 0, p.clear());
            } catch (Exception e) {
                bnfException = e;
                System.out.println(p);
                bnfValue = false;
            }

            boolean isUnicodeSet;
            Exception usException = null;
            try {
                UnicodeSet s = new UnicodeSet(line);
                isUnicodeSet = true;
            } catch (Exception e) {
                usException = e;
                isUnicodeSet = false;
            }

            if (isUnicodeSet != bnfValue) {
                if (bnfException != null) {
                    bnfException.printStackTrace();
                }
                if (usException != null) {
                    System.out.println("usException: " + usException.getMessage());
                    //usException.printStackTrace();
                }

                System.out.println(lineCount + ") Mismatch: " + line + "\t" + status);
                if (DEBUG) {
                    System.out.println(p);
                    bnfValue = bnf.match(line, 0, p.clear());
                }
            } 
        }
    }
}
