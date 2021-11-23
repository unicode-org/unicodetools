package org.unicode.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.parse.EBNF;
import org.unicode.parse.EBNF.Position;
import org.unicode.text.utility.Settings;

import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeBnf {
    static final boolean DEBUG = true;
    
    static void checkRegexBnf() {
        List<String> lines = new ArrayList<>();
        for (String line : FileUtilities.in(TestUnicodeBnf.class, "SegmentBnfGraphemeBreak.txt")) {
            line.trim();
            if (line.startsWith("//") || line.isEmpty()) {
                continue;
            }
            lines.add(line);
        }

        UnicodeRegex regexMaker = new UnicodeRegex();
        String pattern = regexMaker.compileBnf(lines);
        System.out.println(pattern);
        String fixed = regexMaker.transform(pattern.replace(" ", ""));
        fixed = BreakTest.fixControls(fixed);
        //System.out.println(fixed);
        Pattern gcbRegex = Pattern.compile(fixed);
        int lineCount = 0;
        // test
        for (String line : FileUtilities.in(Settings.UnicodeTools.DATA_DIR,
                "ucd/11.0.0-Update/auxiliary/GraphemeBreakTest.txt")) {
            ++lineCount;
            BreakTest breakTest = BreakTest.forLine(line);
            if (breakTest == null) {
                continue;
            }
            String source = breakTest.source;
            BitSet breaksActual = BreakTest.breaksFrom(gcbRegex, source);
            if (!breakTest.expectedBreaks.equals(breaksActual)) {
                BitSet breaksActual2 = BreakTest.breaksFrom(gcbRegex, source);
                System.out.println(lineCount + " Fails «" + BreakTest.fixControls(breakTest.source) 
                + "» expected: " + breakTest.expectedBreaks 
                + " ≠ actual: " + breaksActual
                + "# " + breakTest.comment);
            }
        }
    }
    
    // ÷ 0020 ÷ 0020 ÷  #  ÷ [0.2] SPACE (Other) ÷ [999.0] SPACE (Other) ÷ [0.3]
    // ÷ 0020 × 0308 ÷ 0020 ÷   #  ÷ [0.2] SPACE (Other) × [9.0] COMBINING DIAERESIS (Extend_ExtCccZwj) ÷ [999.0] SPACE (Other) ÷ [0.3]

    static class BreakTest {
        final String source;
        final String comment;
        final BitSet expectedBreaks = new BitSet(); // wish we had immutable bitset
        
        @Override
        public String toString() {
            return "{source=" + source + ";expectedBreaks=" + expectedBreaks + ";comment=" + comment + "}";
        }

        public static BreakTest forLine(String line) {
            int hashPos = line.indexOf('#');
            String comment = "";
            if (hashPos >= 0) {
                comment = line.substring(hashPos+1).trim();
                line = line.substring(0, hashPos);
            }
            line = line.trim();
            if (line.isEmpty()) {
                return null;
            }
            return new BreakTest(line, comment);
        }
        
        private BreakTest(String source, String comment) {
            this.comment = comment;
            String[] items = source.split(" ");
            StringBuilder result = new StringBuilder();
            for (String item : items) {
                switch (item) {
                case "×": 
                    break;
                case "÷": 
                    expectedBreaks.set(result.length()); 
                    break;
                default: 
                    result.appendCodePoint(Integer.parseInt(item, 16));
                    break;
                }
            }
            this.source = result.toString();
        }
        public static BitSet breaksFrom(Pattern regex, String source) {
            BitSet result = new BitSet();
            int len = source.length();
            if (len == 0) {
                return result;
            }
            result.set(0);
            Matcher gcpMatcher = regex.matcher(source);
            while (true) {
                if (!gcpMatcher.lookingAt()) { // only valid at end
                    throw new IllegalArgumentException("doesn't advance");
                }
                result.set(gcpMatcher.end());
                if (len == gcpMatcher.end()) {
                    break;
                }
                gcpMatcher.region(gcpMatcher.end(), len);
            }
            return result;
        }
        
        private static String fixControls(String fixed) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < fixed.length(); ++i) { // can do charAt because only controls...
                char cu = fixed.charAt(i);
                if (Character.getType(cu) == Character.CONTROL) {
                    result.append("\\x" + (cu < 16 ? "0" : "") + Integer.toHexString(cu));
                } else {
                    result.append(cu);
                }
            }
            return result.toString();
        }
    }
    
    public static void main(String[] args) {
        checkRegexBnf();
        //checkBnf();
    }

    private static void checkBnf() {
        EBNF bnf = new EBNF();
        // SegmentBnfGraphemeBreak.txt
        for (String line : FileUtilities.in(TestUnicodeBnf.class, "SegmentBnfGraphemeBreak.txt")) {
            if (line.startsWith("//")) {
                continue;
            }
            bnf.addRules(line);
        }
        bnf.build();

        System.out.println(bnf.getInternal());
        
        String status = "";
        Position p = new Position();

        int lineCount = 0;
        for (String line : FileUtilities.in(TestUnicodeBnf.class, "xxx.txt")) {
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
