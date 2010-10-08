package org.unicode.text.UCA;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class WriteConformanceTest {
    
    static final UnicodeSet RTL = new UnicodeSet("[[:bc=r:][:bc=al:][:bc=an:]]").freeze();

    static void writeConformance(String filename, byte option, boolean shortPrint, CollatorType collatorType) throws IOException {
        // UCD ucd30 = UCD.make("3.0.0");
    
        /*
         * U+01D5 LATIN CAPITAL LETTER U WITH DIAERESIS AND MACRON => U+00DC
         * LATIN CAPITAL LETTER U WITH DIAERESIS, U+0304 COMBINING MACRON
         */
        if (WriteCollationData.DEBUG) {
            String[] testList = { "\u3192", "\u3220", "\u0344", "\u0385", "\uF934", "U", "U\u0308", "\u00DC", "\u00DC\u0304", "U\u0308\u0304" };
            for (int jj = 0; jj < testList.length; ++jj) {
                String t = testList[jj];
                System.out.println(Default.ucd().getCodeAndName(t));
    
                CEList ces = WriteCollationData.getCollator(collatorType).getCEList(t, true);
                System.out.println("CEs:    " + ces);
    
                String test = WriteCollationData.getCollator(collatorType).getSortKey(t, option);
                System.out.println("Decomp: " + WriteCollationData.getCollator(collatorType).toString(test));
    
                test = WriteCollationData.getCollator(collatorType).getSortKey(t, option, false);
                System.out.println("No Dec: " + WriteCollationData.getCollator(collatorType).toString(test));
            }
        }
    
        String fullFileName = "CollationTest" 
        + (collatorType==CollatorType.cldr ? "_CLDR" : "") 
        + (option == UCA.NON_IGNORABLE ? "_NON_IGNORABLE" : "_SHIFTED")
        + (shortPrint ? "_SHORT" : "") + ".txt";
        
        PrintWriter log = Utility.openPrintWriter(WriteCollationData.getCollator(collatorType).getUCA_GEN_DIR(), fullFileName, Utility.UTF8_WINDOWS);
        // if (!shortPrint) log.write('\uFEFF');
        WriteCollationData.writeVersionAndDate(log, fullFileName);
    
        System.out.println("Sorting");
        int counter = 0;
    
        UCA.UCAContents cc = WriteCollationData.getCollator(collatorType).getContents(UCA.FIXED_CE, null);
        cc.setDoEnableSamples(true);
        UnicodeSet found2 = new UnicodeSet();
    
        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
    
            found2.addAll(s);
    
            if (WriteCollationData.DEBUG_SHOW_ITERATION) {
                int cp = UTF16.charAt(s, 0);
                if (cp == 0x1CD0 || !Default.ucd().isAssigned(cp) || UCD.isCJK_BASE(cp)) {
                    System.out.println(Default.ucd().getCodeAndName(s));
                }
            }
            Utility.dot(counter++);
            WriteCollationData.addStringX(s, option);
        }
    
        // Add special examples
        /*
         * addStringX("\u2024\u2024", option); addStringX("\u2024\u2024\u2024",
         * option); addStringX("\u2032\u2032", option);
         * addStringX("\u2032\u2032\u2032", option);
         * addStringX("\u2033\u2033\u2033", option); addStringX("\u2034\u2034",
         * option);
         */
    
        UnicodeSet found = WriteCollationData.getCollator(collatorType).getStatistics().found;
        if (!found2.containsAll(found2)) {
            System.out.println("In both: " + new UnicodeSet(found).retainAll(found2).toPattern(true));
            System.out.println("In UCA but not iteration: " + new UnicodeSet(found).removeAll(found2).toPattern(true));
            System.out.println("In iteration but not UCA: " + new UnicodeSet(found2).removeAll(found).toPattern(true));
            throw new IllegalArgumentException("Inconsistent data");
    
        }
    
        /*
         * for (int i = 0; i <= 0x10FFFF; ++i) { if (!ucd.isAssigned(i))
         * continue; addStringX(UTF32.valueOf32(i), option); }
         * 
         * Hashtable multiTable = collator.getContracting(); Enumeration enum =
         * multiTable.keys(); while (enum.hasMoreElements()) {
         * Utility.dot(counter++); addStringX((String)enum.nextElement(),
         * option); }
         * 
         * for (int i = 0; i < extraConformanceTests.length; ++i) { // put in
         * sample non-characters Utility.dot(counter++); String s =
         * UTF32.valueOf32(extraConformanceTests[i]); Utility.fixDot();
         * System.out.println("Adding: " + Utility.hex(s)); addStringX(s,
         * option); }
         * 
         * 
         * 
         * for (int i = 0; i < extraConformanceRanges.length; ++i) {
         * Utility.dot(counter++); int start = extraConformanceRanges[i][0]; int
         * end = extraConformanceRanges[i][1]; int increment = ((end - start +
         * 1) / 303) + 1; //System.out.println("Range: " + start + ", " + end +
         * ", " + increment); addStringX(start, option); for (int j = start+1; j
         * < end-1; j += increment) { addStringX(j, option); addStringX(j+1,
         * option); } addStringX(end-1, option); addStringX(end, option); }
         */
    
        Utility.fixDot();
        System.out.println("Total: " + WriteCollationData.sortedD.size());
        Iterator it;
    
        System.out.println("Writing");
        // String version = collator.getVersion();
    
        it = WriteCollationData.sortedD.keySet().iterator();
    
        String lastKey = "";
    
        while (it.hasNext()) {
            Utility.dot(counter);
            String key = (String) it.next();
            String source = (String) WriteCollationData.sortedD.get(key);
            int fluff = key.charAt(key.length() - 1);
            key = key.substring(0, key.length() - fluff - 2);
            // String status = key.equals(lastKey) ? "*" : "";
            // lastKey = key;
            // log.println(source);
            char extra = source.charAt(source.length() - 1);
            String clipped = source.substring(0, source.length() - 1);
            if (clipped.charAt(0) == WriteCollationData.LOW_ACCENT && extra != WriteCollationData.LOW_ACCENT) {
                extra = WriteCollationData.LOW_ACCENT;
                clipped = source.substring(1);
            }
            if (!shortPrint) {
                log.print(Utility.hex(source));
                String name = Default.ucd().getName(clipped);
                if (name == null) {
                    name = Default.ucd().getName(clipped);
                    System.out.println("Null name for " + Utility.hex(source));
                }
                String quoteOperand = WriteCollationData.quoteOperand(clipped);
                if (RTL.containsSome(quoteOperand)) {
                    quoteOperand = '\u200E' + quoteOperand + '\u200E';
                }
                log.print(
                        ";\t# (" + quoteOperand + ") "
                        + name
                        + "\t" 
                        + UCA.toString(key));
            } else {
                log.print(Utility.hex(source));
            }
            log.println();
        }
    
        log.close();
        WriteCollationData.sortedD.clear();
        System.out.println("Done");
    }

}
