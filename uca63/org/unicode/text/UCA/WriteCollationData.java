/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/WriteCollationData.java,v $ 
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.unicode.cldr.util.With;
import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.UCA.Remap;
import org.unicode.text.UCA.UCA.UCAContents;
import org.unicode.text.UCA.UCA_Statistics.RoBitSet;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.CompactByteArray;
import org.unicode.text.utility.CompactShortArray;
import org.unicode.text.utility.IntStack;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class WriteCollationData implements UCD_Types, UCA_Types {

    private static final boolean SHOW_NON_MAPPED = false;
    
    private static final boolean ADD_TIBETAN = true;

    // may require fixing

    private static final boolean DEBUG = false;

    private static final boolean GENERATED_NFC_MISMATCHES = true;

    private static final String UNICODE_VERSION = UCD.latestVersion;

    private static UCA                 ducetCollator;
    private static UCA                 cldrCollator;
    private static UCA                 cldrWithoutFFFxCollator;

    private static TreeMap<String, String>  sortedD     = new TreeMap<String, String>();
    private static TreeMap<String, String>  sortedN     = new TreeMap<String, String>();
    private static HashMap<String, String>  backD       = new HashMap<String, String>();
    private static HashMap<String, String>  backN       = new HashMap<String, String>();
    private static TreeMap<String, String>  duplicates  = new TreeMap<String, String>();
    private static PrintWriter              log;

    // Called by UCA.Main.
    // TODO: Remove? This code tests the Java Collator. Useful?
    static void javatest() throws Exception {
        checkJavaRules("& J , K / B & K , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
        checkJavaRules("& J , K / B , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
    }

    private static void checkJavaRules(String rules, String[] tests) throws Exception {
        System.out.println();
        System.out.println("Rules: " + rules);
        System.out.println();

        // duplicate the effect of ICU 1.8 by grabbing the default rules and
        // appending

        RuleBasedCollator defaultCollator = (RuleBasedCollator) Collator.getInstance(Locale.US);
        RuleBasedCollator col = new RuleBasedCollator(defaultCollator.getRules() + rules);

        // check to make sure each pair is in order

        int i = 1;
        for (; i < tests.length; ++i) {
            System.out.println(tests[i - 1] + "\t=> " + showJavaCollationKey(col, tests[i - 1]));
            if (col.compare(tests[i - 1], tests[i]) > 0) {
                System.out.println("Failure: " + tests[i - 1] + " > " + tests[i]);
            }
        }
        System.out.println(tests[i - 1] + "\t=> " + showJavaCollationKey(col, tests[i - 1]));
    }

    private static String showJavaCollationKey(RuleBasedCollator col, String test) {
        CollationElementIterator it = col.getCollationElementIterator(test);
        String result = "[";
        for (int i = 0;; ++i) {
            int ce = it.next();
            if (ce == CollationElementIterator.NULLORDER) {
                break;
            }
            if (i != 0) {
                result += ", ";
            }
            result += Utility.hex(ce, 8);
        }
        return result + "]";
    }

    // Called by UCA.Main.
    static void writeCaseExceptions() {
        System.err.println("Writing Case Exceptions");
        for (char a = 0; a < 0xFFFF; ++a) {
            if (!Default.ucd().isRepresented(a)) {
                continue;
                // if (0xA000 <= a && a <= 0xA48F) continue; // skip YI
            }

            String b = Case.fold(a);
            String c = Default.nfkc().normalize(b);
            String d = Case.fold(c);
            String e = Default.nfkc().normalize(d);
            if (!e.equals(c)) {
                System.out.println(Utility.hex(a) + "; " + Utility.hex(d, " ") + " # " + Default.ucd().getName(a));
                /*
                 * System.out.println(Utility.hex(a) + ", " + Utility.hex(b,
                 * " ") + ", " + Utility.hex(c, " ") + ", " + Utility.hex(d,
                 * " ") + ", " + Utility.hex(e, " "));
                 * 
                 * System.out.println(ucd.getName(a) + ", " + ucd.getName(b) +
                 * ", " + ucd.getName(c) + ", " + ucd.getName(d) + ", " +
                 * ucd.getName(e));
                 */
            }
            String f = Case.fold(e);
            String g = Default.nfkc().normalize(f);
            if (!f.equals(d) || !g.equals(e)) {
                System.out.println("!!!!!!SKY IS FALLING!!!!!!");
            }
        }
    }

    // Called by UCA.Main.
    static void writeCaseFolding() throws IOException {
        System.err.println("Writing Javascript data");
        BufferedReader in = Utility.openUnicodeFile("CaseFolding", UNICODE_VERSION, true, Utility.LATIN1);
        // new BufferedReader(new FileReader(DIR31 +
        // "CaseFolding-3.d3.alpha.txt"), 64*1024);
        // log = new PrintWriter(new FileOutputStream("CaseFolding_data.js"));
        log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "CaseFolding_data.js", Utility.UTF8_WINDOWS);
        log.println("var CF = new Object();");
        int count = 0;
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            int comment = line.indexOf('#'); // strip comments
            if (comment != -1) {
                line = line.substring(0, comment);
            }
            if (line.length() == 0) {
                continue;
            }
            int semi1 = line.indexOf(';');
            int semi2 = line.indexOf(';', semi1 + 1);
            int semi3 = line.indexOf(';', semi2 + 1);
            char type = line.substring(semi1 + 1, semi2).trim().charAt(0);
            if (type == 'C' || type == 'F' || type == 'T') {
                String code = line.substring(0, semi1).trim();
                String result = " " + line.substring(semi2 + 1, semi3).trim();
                result = replace(result, ' ', "\\u");
                log.println("\t CF[0x" + code + "]='" + result + "';");
                count++;
            }
        }
        log.println("// " + count + " case foldings total");

        in.close();
        log.close();
    }

    private static String replace(String source, char toBeReplaced, String toReplace) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == toBeReplaced) {
                result.append(toReplace);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // Called by UCA.Main.
    static void writeJavascriptInfo() throws IOException {
        System.err.println("Writing Javascript data");
        // Normalizer normKD = new Normalizer(Normalizer.NFKD, UNICODE_VERSION);
        // Normalizer normD = new Normalizer(Normalizer.NFD, UNICODE_VERSION);
        // log = new PrintWriter(new FileOutputStream("Normalization_data.js"));
        log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "Normalization_data.js", Utility.LATIN1_WINDOWS);

        int count = 0;
        int datasize = 0;
        int max = 0;
        int over7 = 0;
        log.println("var KD = new Object(); // NFKD compatibility decomposition mappings");
        log.println("// NOTE: Hangul is done in code!");
        CompactShortArray csa = new CompactShortArray((short) 0);

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) {
                System.err.println(Utility.hex(c));
            }
            if (0xAC00 <= c && c <= 0xD7A3) {
                continue;
            }
            if (!Default.nfkd().isNormalized(c)) {
                ++count;
                String decomp = Default.nfkd().normalize(c);
                datasize += decomp.length();
                if (max < decomp.length()) {
                    max = decomp.length();
                }
                if (decomp.length() > 7) {
                    ++over7;
                }
                csa.setElementAt(c, (short) count);
                log.println("\t KD[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp, "\\u") + "';");
            }
        }
        csa.compact();
        log.println("// " + count + " NFKD mappings total");
        log.println("// " + datasize + " total characters of results");
        log.println("// " + max + " string length, maximum");
        log.println("// " + over7 + " result strings with length > 7");
        log.println("// " + csa.storage() + " trie length (doesn't count string size)");
        log.println();

        count = 0;
        datasize = 0;
        max = 0;
        log.println("var D = new Object();  // NFD canonical decomposition mappings");
        log.println("// NOTE: Hangul is done in code!");
        csa = new CompactShortArray((short) 0);

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) {
                System.err.println(Utility.hex(c));
            }
            if (0xAC00 <= c && c <= 0xD7A3) {
                continue;
            }
            if (!Default.nfd().isNormalized(c)) {
                ++count;
                String decomp = Default.nfd().normalize(c);
                datasize += decomp.length();
                if (max < decomp.length()) {
                    max = decomp.length();
                }
                csa.setElementAt(c, (short) count);
                log.println("\t D[0x" + Utility.hex(c) + "]='\\u" + Utility.hex(decomp, "\\u") + "';");
            }
        }
        csa.compact();

        log.println("// " + count + " NFD mappings total");
        log.println("// " + datasize + " total characters of results");
        log.println("// " + max + " string length, maximum");
        log.println("// " + csa.storage() + " trie length (doesn't count string size)");
        log.println();

        count = 0;
        datasize = 0;
        log.println("var CC = new Object(); // canonical class mappings");
        CompactByteArray cba = new CompactByteArray();

        for (char c = 0; c < 0xFFFF; ++c) {
            if ((c & 0xFFF) == 0) {
                System.err.println(Utility.hex(c));
            }
            int canClass = Default.nfkd().getCanonicalClass(c);
            if (canClass != 0) {
                ++count;

                log.println("\t CC[0x" + Utility.hex(c) + "]=" + canClass + ";");
            }
        }
        cba.compact();
        log.println("// " + count + " canonical class mappings total");
        log.println("// " + cba.storage() + " trie length");
        log.println();

        count = 0;
        datasize = 0;
        log.println("var C = new Object();  // composition mappings");
        log.println("// NOTE: Hangul is done in code!");

        System.out.println("WARNING -- COMPOSITIONS UNFINISHED!!");

        /*
         * 
         * IntHashtable.IntEnumeration enum = Default.nfkd.getComposition();
         * while (enum.hasNext()) { int key = enum.next(); char val = (char)
         * enum.value(); if (0xAC00 <= val && val <= 0xD7A3) continue; ++count;
         * log.println("\tC[0x" + Utility.hex(key) + "]=0x" + Utility.hex(val) +
         * ";"); } log.println("// " + count + " composition mappings total");
         * log.println();
         */

        log.close();
        System.err.println("Done writing Javascript data");
    }

    static void writeVersionAndDate(PrintWriter log, String filename, boolean auxiliary) {
        log.println("# File:        " + filename);
        log.println("# UCA Version: " + getCollator(CollatorType.ducet).getDataVersion());
        log.println("# UCD Version: " + getCollator(CollatorType.ducet).getDataVersion());
        log.println("# Generated:   " + getNormalDate());
        log.println("# For a description of the format and usage, see Collation" +
                (auxiliary ? "Auxiliary" : "Test") +
                ".html");
        log.println();
    }

    private static CanonicalIterator canIt = null;

    private static UCD ucd_uca_base = null;

    /**
     * Check that the primaries are the same as the compatibility decomposition.
     */
    private static void checkBadDecomps(int strength, boolean decomposition, UnicodeSet alreadySeen) {
        if (ucd_uca_base == null) {
            ucd_uca_base = UCD.make(getCollator(CollatorType.ducet).getUCDVersion());
        }
        int oldStrength = getCollator(CollatorType.ducet).getStrength();
        getCollator(CollatorType.ducet).setStrength(strength);
        // Normalizer nfkd = new Normalizer(Normalizer.NFKD, UNICODE_VERSION);
        // Normalizer nfc = new Normalizer(Normalizer.NFC, UNICODE_VERSION);
        switch (strength) {
        case 1:
            log.println("<h2>3. Primaries Incompatible with NFKD</h2>");
            break;
        case 2:
            log.println("<h2>4. Secondaries Incompatible with NFKD</h2>");
            break;
        case 3:
            log.println("<h2>5. Tertiaries Incompatible with NFKD</h2>");
            break;
        default:
            throw new IllegalArgumentException("bad strength: " + strength);
        }
        log.println("<p>Note: Differences are not really errors; but they should be checked over for inadvertant problems</p>");
        log.println("<p>Warning: only checking characters defined in base: " + ucd_uca_base.getVersion() + "</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Code</td><th>Sort Key</th><th>NFKD Sort Key</th><th>Name</th></tr>");

        int errorCount = 0;

        UnicodeSet skipSet = new UnicodeSet();

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            if (!ucd_uca_base.isAllocated(ch)) {
                continue;
            }
            if (Default.nfkd().isNormalized(ch)) {
                continue;
            }
            if (ch > 0xAC00 && ch < 0xD7A3) {
                continue; // skip most of Hangul
            }
            if (alreadySeen.contains(ch)) {
                continue;
            }
            Utility.dot(ch);

            String decomp = Default.nfkd().normalize(ch);
            if (ch != ' ' && decomp.charAt(0) == ' ') {
                skipSet.add(ch);
                continue; // skip wierd decomps
            }
            if (ch != '\u0640' && decomp.charAt(0) == '\u0640') {
                skipSet.add(ch);
                continue; // skip wierd decomps
            }

            String sortKey = getCollator(CollatorType.ducet).getSortKey(UTF16.valueOf(ch), UCA.NON_IGNORABLE, decomposition, AppendToCe.none);
            String decompSortKey = getCollator(CollatorType.ducet).getSortKey(decomp, UCA.NON_IGNORABLE, decomposition, AppendToCe.none);
            if (false && strength == 2) {
                sortKey = remove(sortKey, '\u0020');
                decompSortKey = remove(decompSortKey, '\u0020');
            }

            if (sortKey.equals(decompSortKey)) {
                continue; // no problem!
            }

            // fix key in the case of strength 3

            if (strength == 3) {
                String newSortKey = remapSortKey(ch, decomposition);
                if (!sortKey.equals(newSortKey)) {
                    System.out.println("Fixing: " + Default.ucd().getCodeAndName(ch));
                    System.out.println("  Old:" + UCA.toString(decompSortKey));
                    System.out.println("  New: " + UCA.toString(newSortKey));
                    System.out.println("  Tgt: " + UCA.toString(sortKey));
                }
                decompSortKey = newSortKey;
            }

            if (sortKey.equals(decompSortKey)) {
                continue; // no problem!
            }

            log.println("<tr><td>" + Utility.hex(ch)
                    + "</td><td>" + UCA.toString(sortKey)
                    + "</td><td>" + UCA.toString(decompSortKey)
                    + "</td><td>" + Default.ucd().getName(ch)
                    + "</td></tr>"
                    );
            alreadySeen.add(ch);
            errorCount++;
        }
        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        log.println("<p>Space/Tatweel exceptions: " + skipSet.toPattern(true) + "</p>");
        log.flush();
        getCollator(CollatorType.ducet).setStrength(oldStrength);
        Utility.fixDot();
    }

    private static String remapSortKey(int cp, boolean decomposition) {
        if (Default.nfd().isNormalized(cp)) {
            return remapCanSortKey(cp, decomposition);
        }

        // we know that it is not NFKD.
        String canDecomp = Default.nfd().normalize(cp);
        String result = "";
        int ch;
        for (int j = 0; j < canDecomp.length(); j += UTF16.getCharCount(ch)) {
            ch = UTF16.charAt(canDecomp, j);
            System.out.println("* " + Default.ucd().getCodeAndName(ch));
            String newSortKey = remapCanSortKey(ch, decomposition);
            System.out.println("* " + UCA.toString(newSortKey));
            result = mergeSortKeys(result, newSortKey);
            System.out.println("= " + UCA.toString(result));
        }
        return result;
    }

    private static String remapCanSortKey(int ch, boolean decomposition) {
        String compatDecomp = Default.nfkd().normalize(ch);
        String decompSortKey = getCollator(CollatorType.ducet).getSortKey(compatDecomp, UCA.NON_IGNORABLE, decomposition, AppendToCe.none);

        byte type = Default.ucd().getDecompositionType(ch);
        int pos = decompSortKey.indexOf(UCA.LEVEL_SEPARATOR) + 1; // after first
        // separator
        pos = decompSortKey.indexOf(UCA.LEVEL_SEPARATOR, pos) + 1; // after
        // second
        // separator
        String newSortKey = decompSortKey.substring(0, pos);
        for (int i = pos; i < decompSortKey.length(); ++i) {
            int weight = decompSortKey.charAt(i);
            int newWeight = CEList.remap(ch, type, weight);
            if (i > pos + 1) {
                newWeight = 0x1F;
            }
            newSortKey += (char) newWeight;
        }
        return newSortKey;
    }

    // keys must be of the same strength
    private static String mergeSortKeys(String key1, String key2) {
        StringBuffer result = new StringBuffer();
        int end1 = 0, end2 = 0;
        while (true) {
            int pos1 = key1.indexOf(UCA.LEVEL_SEPARATOR, end1);
            int pos2 = key2.indexOf(UCA.LEVEL_SEPARATOR, end2);
            if (pos1 < 0) {
                result.append(key1.substring(end1)).append(key2.substring(end2));
                return result.toString();
            }
            if (pos2 < 0) {
                result.append(key1.substring(end1, pos1)).append(key2.substring(end2)).append(key1.substring(pos1));
                return result.toString();
            }
            result.append(key1.substring(end1, pos1)).append(key2.substring(end2, pos2)).append(UCA.LEVEL_SEPARATOR);
            end1 = pos1 + 1;
            end2 = pos2 + 1;
        }
    }

    private static final String remove(String s, char ch) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ch) {
                continue;
            }
            buf.append(c);
        }
        return buf.toString();
    }

    private static void writeDuplicates() {
        log.println("<h2>9. Checking characters that are not canonical equivalents, but have same CE</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

        Map<CEList,Set<String>> map = new TreeMap<CEList,Set<String>>();

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            if (!Default.nfd().isNormalized(s)) {
                continue; // only unnormalized stuff
            }
            if (UTF16.countCodePoint(s) == 1) {
                int cat = Default.ucd().getCategory(UTF16.charAt(s, 0));
                if (cat == Cn || cat == Cc || cat == Cs) {
                    continue;
                }
            }

            CEList celist = getCollator(CollatorType.ducet).getCEList(s, true);
            Utility.addToSet(map, celist, s);
        }

        Iterator<CEList> it = map.keySet().iterator();
        while (it.hasNext()) {
            CEList celist = it.next();
            Set<String> s = map.get(celist);
            String name = celist.toString();
            if (name.length() == 0) {
                name = "<i>ignore</i>";
            }
            if (s.size() > 1) {
                log.println("<tr><td>" + name
                        + "</td><td>" + getHTML_NameSet(s, null, true)
                        + "</td></tr>");
            }
        }
        log.println("</table>");
        log.flush();
    }

    private static void writeOverlap() {
        log.println("<h2>10. Checking overlaps</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

        Map<CEList,String> map = new TreeMap<CEList,String>();
        Map<CEList,Set<CEList>> tails = new TreeMap<CEList,Set<CEList>>();

        int counter = 0;
        System.out.println("Collecting items");

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            Utility.dot(counter++);
            if (!Default.nfd().isNormalized(s)) {
                continue; // only normalized stuff
            }
            CEList celist = getCollator(CollatorType.ducet).getCEList(s, true);
            if (!map.containsKey(celist)) {
                map.put(celist, s);
            }
        }

        Utility.fixDot();
        System.out.println("Collecting tails");

        Map<CEList,String> mapNames = new TreeMap<CEList,String>();
        mapNames.putAll(map);

        for (Entry<CEList, String> entry : map.entrySet()) {
            CEList celist = entry.getKey();
            String s = entry.getValue();
            Utility.dot(counter++);
            int len = celist.length();
            if (len < 2) {
                continue;
            }

            for (int i = 1; i < len; ++i) {
                CEList tail = celist.sub(i, len);
                Utility.dot(counter++);
                if (map.get(tail) == null && mapNames.get(tail) == null) { // skip anything in main
                    Utility.addToSet(tails, tail, celist);
                    mapNames.put(tail, s+"//"+i);
                }
            }
        }

        Utility.fixDot();
        System.out.println("Finding overlaps");

        // we now have a set of main maps, and a set of tails
        // the main maps to string, the tails map to set of CELists

        Iterator<CEList> it = map.keySet().iterator();
        List<CEList> first = new ArrayList<CEList>();
        List<CEList> second = new ArrayList<CEList>();

        while (it.hasNext()) {
            CEList celist = (CEList) it.next();
            int len = celist.length();
            if (len < 2) {
                continue;
            }
            Utility.dot(counter++);
            first.clear();
            second.clear();
            if (overlaps(map, tails, celist, 0, first, second)) {
                reverse(first);
                reverse(second);

                log.println("<tr><td>" + getHTML_NameSet(first, null, false)
                        + "</td><td>" + getHTML_NameSet(first, mapNames, true)
                        + "</td><td>" + getHTML_NameSet(second, null, false)
                        + "</td><td>" + getHTML_NameSet(second, mapNames, true)
                        + "</td></tr>");
            }
        }
        log.println("</table>");
        log.flush();
    }

    private static void reverse(List ell) {
        int i = 0;
        int j = ell.size() - 1;
        while (i < j) {
            Object temp = ell.get(i);
            ell.set(i, ell.get(j));
            ell.set(j, temp);
            ++i;
            --j;
        }
    }

    private static final boolean DEBUG_SHOW_OVERLAP = false;

    private static boolean overlaps(Map map, Map tails, CEList celist, int depth, List me, List other) {
        if (depth == 5) {
            return false;
        }
        boolean gotOne = false;
        if (DEBUG_SHOW_OVERLAP && depth > 0) {
            Object foo = map.get(celist);
            System.out.println(Utility.repeat("**", depth) + "Trying:" + celist + ", "
                    + (foo != null ? Default.ucd().getCodeAndName(foo.toString()) : ""));
            gotOne = true;
        }
        int len = celist.length();
        // if the tail of the celist matches something, then ArrayList
        // A. subtract the match and retry
        // B. if that doesn't work, see if the result is the tail of something
        // else.
        for (int i = 1; i < len; ++i) {
            CEList tail = celist.sub(i, len);
            if (map.get(tail) != null) {

                if (DEBUG_SHOW_OVERLAP && !gotOne) {
                    Object foo = map.get(celist);
                    System.out.println(Utility.repeat("**", depth) + "Trying:" + celist + ", "
                            + (foo != null ? Default.ucd().getCodeAndName(foo.toString()) : ""));
                    gotOne = true;
                }

                if (DEBUG_SHOW_OVERLAP) {
                    System.out
                    .println(Utility.repeat("**", depth) + "  Match tail at " + i + ": " + tail + ", " + Default.ucd().getCodeAndName(map.get(tail).toString()));
                }
                // temporarily add tail
                int oldListSize = me.size();
                me.add(tail);

                // the tail matched, try 3 options
                CEList head = celist.sub(0, i);

                // see if the head matches exactly!

                if (map.get(head) != null) {
                    if (DEBUG_SHOW_OVERLAP) {
                        System.out.println(Utility.repeat("**", depth) + "  Match head at "
                                + i + ": " + head + ", " + Default.ucd().getCodeAndName(map.get(head).toString()));
                    }
                    me.add(head);
                    other.add(celist);
                    return true;
                }

                // see if the end of the head matches something (recursively)

                if (DEBUG_SHOW_OVERLAP) {
                    System.out.println(Utility.repeat("**", depth) + "  Checking rest");
                }
                if (overlaps(map, tails, head, depth + 1, me, other)) {
                    return true;
                }

                // otherwise we see if the head is some tail

                Set possibleFulls = (Set) tails.get(head);
                if (possibleFulls != null) {
                    Iterator it = possibleFulls.iterator();
                    while (it.hasNext()) {
                        CEList full = (CEList) it.next();
                        CEList possibleHead = full.sub(0, full.length() - head.length());
                        if (DEBUG_SHOW_OVERLAP) {
                            System.out.println(Utility.repeat("**", depth) + "  Reversing " + full
                                    + ", " + Default.ucd().getCodeAndName(map.get(full).toString())
                                    + ", " + possibleHead);
                        }
                        if (overlaps(map, tails, possibleHead, depth + 1, other, me)) {
                            return true;
                        }
                    }
                }

                // didn't work, so retract!
                me.remove(oldListSize);
            }
        }
        return false;
    }

    // if m exists, then it is a mapping to strings. Use it.
    // otherwise just print what is in set
    private static <K, V> String getHTML_NameSet(Collection<K> set, Map<K,V> m, boolean useName) {
        StringBuffer result = new StringBuffer();
        Iterator<K> it = set.iterator();
        while (it.hasNext()) {
            if (result.length() != 0) {
                result.append(";<br>");
            }
            K item = it.next();
            String name = null;
            if (m != null) {
                V name0 = m.get(item);
                if (name0 == null) {
                    System.out.println("Missing Item: " + item);
                    name = item.toString();
                } else if (useName) {
                    name = Default.ucd().getCodeAndName(name0.toString());
                } else {
                    name = name0.toString();
                }
            } else {
                name = item.toString();
            }
            result.append(name);
        }
        return result.toString();
    }

    // Called by UCA.Main.
    static void writeContractions() throws IOException {
        String fullFileName = "UCA_Contractions.txt";
        PrintWriter diLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), fullFileName, Utility.UTF8_WINDOWS);

        diLog.write('\uFEFF');

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

        diLog.println("# Contractions");
        writeVersionAndDate(diLog, fullFileName, true);
        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            CEList ces = cc.getCEs();

            if (s.length() > 1) {
                diLog.println(Utility.hex(s, " ")
                        + ";\t #" + ces
                        + " ( " + s + " )"
                        + " " + Default.ucd().getName(s));
            }
        }
        diLog.close();
    }

    // Called by UCA.Main.
    static void checkDisjointIgnorables() throws IOException {
        PrintWriter diLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "DisjointIgnorables.js", Utility.UTF8_WINDOWS);

        diLog.write('\uFEFF');

        /*
         * PrintWriter diLog = new PrintWriter( // try new one new
         * UTF8StreamWriter(new FileOutputStream(UCA_GEN_DIR +
         * "DisjointIgnorables.txt"), 32*1024)); diLog.write('\uFEFF');
         */

        // diLog = new PrintWriter(new FileOutputStream(UCA_GEN_DIR +
        // "DisjointIgnorables.txt"));

        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        int[] secondariesZP = new int[400];
        Vector[] secondariesZPsample = new Vector[400];
        int[] remapZP = new int[400];

        int[] secondariesNZP = new int[400];
        Vector[] secondariesNZPsample = new Vector[400];
        int[] remapNZP = new int[400];

        for (int i = 0; i < secondariesZP.length; ++i) {
            secondariesZPsample[i] = new Vector();
            secondariesNZPsample[i] = new Vector();
        }

        int zpCount = 0;
        int nzpCount = 0;

        /*
         * for (char ch = 0; ch < 0xFFFF; ++ch) { byte type =
         * collator.getCEType(ch); if (type >= UCA.FIXED_CE) continue; if
         * (SKIP_CANONICAL_DECOMPOSIBLES && nfd.hasDecomposition(ch)) continue;
         * String s = String.valueOf(ch); int len = collator.getCEs(s, true,
         * ces);
         */
        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

        Set<String> sortedCodes = new TreeSet<String>();
        Set<String> mixedCEs = new TreeSet<String>();

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }

            // process all CEs. Look for controls, and for mixed
            // ignorable/non-ignorables
            CEList ces = cc.getCEs();

            int ccc;
            for (int kk = 0; kk < s.length(); kk += UTF32.count16(ccc)) {
                ccc = UTF32.char32At(s, kk);
                byte cat = Default.ucd().getCategory(ccc);
                if (cat == Cf || cat == Cc || cat == Zs || cat == Zl || cat == Zp) {
                    sortedCodes.add(ces + "\t" + Default.ucd().getCodeAndName(s));
                    break;
                }
            }

            int len = ces.length();

            int haveMixture = 0;
            for (int j = 0; j < len; ++j) {
                int ce = ces.at(j);
                int pri = CEList.getPrimary(ce);
                int sec = CEList.getSecondary(ce);
                if (pri == 0) {
                    secondariesZPsample[sec].add(secondariesZP[sec], s);
                    secondariesZP[sec]++;
                } else {
                    secondariesNZPsample[sec].add(secondariesNZP[sec], s);
                    secondariesNZP[sec]++;
                }
                if (haveMixture == 3) {
                    continue;
                }
                if (getCollator(CollatorType.ducet).isVariable(ce)) {
                    haveMixture |= 1;
                } else {
                    haveMixture |= 2;
                }
                if (haveMixture == 3) {
                    mixedCEs.add(ces + "\t" + Default.ucd().getCodeAndName(s));
                }
            }
        }

        for (int i = 0; i < secondariesZP.length; ++i) {
            if (secondariesZP[i] != 0) {
                remapZP[i] = zpCount;
                zpCount++;
            }
            if (secondariesNZP[i] != 0) {
                remapNZP[i] = nzpCount;
                nzpCount++;
            }
        }

        diLog.println();
        diLog.println("# Proposed Remapping (see doc about Japanese characters)");
        diLog.println();

        int bothCount = 0;
        for (int i = 0; i < secondariesZP.length; ++i) {
            if ((secondariesZP[i] != 0) || (secondariesNZP[i] != 0)) {
                char sign = ' ';
                if (secondariesZP[i] != 0 && secondariesNZP[i] != 0) {
                    sign = '*';
                    bothCount++;
                }
                if (secondariesZP[i] != 0) {
                    showSampleOverlap(diLog, false, sign + "ZP ", secondariesZPsample[i]); // i,
                    // 0x20
                    // +
                    // nzpCount
                    // +
                    // remapZP[i],
                }
                if (secondariesNZP[i] != 0) {
                    if (i == 0x20) {
                        diLog.println("(omitting " + secondariesNZP[i] + " NZP with values 0020 -- values don't change)");
                    } else {
                        showSampleOverlap(diLog, true, sign + "NZP", secondariesNZPsample[i]); // i,
                        // 0x20
                        // +
                        // remapNZP[i],
                    }
                }
                diLog.println();
            }
        }
        diLog.println("ZP Count = " + zpCount + ", NZP Count = " + nzpCount + ", Collisions = " + bothCount);

        /*
         * diLog.println(); diLog.println("OVERLAPS"); diLog.println();
         * 
         * for (int i = 0; i < secondariesZP.length; ++i) { if (secondariesZP[i]
         * != 0 && secondariesNZP[i] != 0) { diLog.println("Overlap at " +
         * Utility.hex(i) + ": " + secondariesZP[i] + " with zero primaries" +
         * ", " + secondariesNZP[i] + " with non-zero primaries" );
         * 
         * showSampleOverlap(" ZP:  ", secondariesZPsample[i], ces);
         * showSampleOverlap(" NZP: ", secondariesNZPsample[i], ces);
         * diLog.println(); } }
         */

        diLog.println();
        diLog.println("# BACKGROUND INFORMATION");
        diLog.println();
        diLog.println("# All characters with 'mixed' CEs: variable and non-variable");
        diLog.println("# Note: variables are in " 
                + Utility.hex(CEList.getPrimary(getCollator(CollatorType.ducet).getVariableLowCE())) + " to "
                + Utility.hex(CEList.getPrimary(getCollator(CollatorType.ducet).getVariableHighCE())));
        diLog.println();

        Iterator<String> it;
        it = mixedCEs.iterator();
        while (it.hasNext()) {
            String key = it.next();
            diLog.println(key);
        }

        diLog.println();
        diLog.println("# All 'controls': Cc, Cf, Zs, Zp, Zl");
        diLog.println();

        it = sortedCodes.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            diLog.println(key);
        }

        diLog.close();
    }

    private static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, Vector v) {
        for (int i = 0; i < v.size(); ++i) {
            showSampleOverlap(diLog, doNew, head, (String) v.get(i));
        }
    }

    private static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, String src) {
        int[] ces = new int[30];
        int len = getCollator(CollatorType.ducet).getCEs(src, true, ces);
        int[] newCes = null;
        int newLen = 0;
        if (doNew) {
            newCes = new int[30];
            for (int i = 0; i < len; ++i) {
                int ce = ces[i];
                int p = CEList.getPrimary(ce);
                int s = CEList.getSecondary(ce);
                int t = CEList.getTertiary(ce);
                if (p != 0 && s != 0x20) {
                    newCes[newLen++] = UCA.makeKey(p, 0x20, t);
                    newCes[newLen++] = UCA.makeKey(0, s, 0x1F);
                } else {
                    newCes[newLen++] = ce;
                }
            }
        }
        diLog.println(
                Default.ucd().getCode(src)
                + "\t" + head
                // + "\t" + Utility.hex(oldWeight)
                // + " => " + Utility.hex(newWeight)
                + "\t" + CEList.toString(ces, len)
                + (doNew ? " => " + CEList.toString(newCes, newLen) : "")
                + "\t( " + src + " )"
                + "\t" + Default.ucd().getName(src)
                );
    }

    // Options for writeRules(byte options, ...), used by UCA.Main.
    static final byte    WITHOUT_NAMES                = 0, WITH_NAMES = 1, IN_XML = 2;

    private static final boolean SKIP_CANONICAL_DECOMPOSIBLES = true;

    private static int getFirstCELen(CEList ces) {
        int len = ces.length();
        if (len < 2) {
            return len;
        }
        int expansionStart = 1;
        if (UCA.isImplicitLeadCE(ces.at(0))) {
            expansionStart = 2; // move up if first is double-ce
        }
        if (len > expansionStart && getCollator(CollatorType.ducet).getHomelessSecondaries().contains(CEList.getSecondary(ces.at(expansionStart)))) {
            if (log2 != null) {
                log2.println("Homeless: " + ces);
            }
            ++expansionStart; // move up if *second* is homeless ignoreable
        }
        return expansionStart;
    }

    private static PrintWriter log2 = null;

    // Called by UCA.Main.
    static void writeRules(byte option, boolean shortPrint, boolean noCE, CollatorType collatorType2) throws IOException {
        System.out.println("Sorting");
        Map<ArrayWrapper, String> backMap = new HashMap<ArrayWrapper, String>();
        java.util.Comparator<String> cm = new RuleComparator();
        Map<String, String> ordered = new TreeMap<String, String>(cm);

        UCA.UCAContents cc = getCollator(collatorType2).getContents(SKIP_CANONICAL_DECOMPOSIBLES ? Default.nfd() : null);

        Set<String> alreadyDone = new HashSet<String>();

        log2 = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", "UCARules-log.txt", Utility.UTF8_WINDOWS);

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            CEList ces = cc.getCEs();

            if (s.equals("\uD800")) {
                System.out.println("Check: " + ces);
            }

            String safeString = s.replace("\u0000", "\\u0000");
            log2.println(safeString + "\t" + bidiBracket(ces.toString()) + "\t" + Default.ucd().getCodeAndName(s));

            addToBackMap(backMap, ces, s, false);

            int ce2 = 0;
            int ce3 = 0;
            int logicalFirstLen = getFirstCELen(ces);
            if (logicalFirstLen > 1) {
                ce2 = ces.at(1);
                if (logicalFirstLen > 2) {
                    ce3 = ces.at(2);
                }
            }

            String key = String.valueOf(CEList.getPrimary(ces.at(0))) + String.valueOf(CEList.getPrimary(ce2)) + String.valueOf(CEList.getPrimary(ce3))
                    + String.valueOf(CEList.getSecondary(ces.at(0))) + String.valueOf(CEList.getSecondary(ce2)) + String.valueOf(CEList.getSecondary(ce3))
                    + String.valueOf(CEList.getTertiary(ces.at(0))) + String.valueOf(CEList.getTertiary(ce2)) + String.valueOf(CEList.getTertiary(ce3))
                    + getCollator(collatorType2).getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + UCA.codePointOrder(s);

            // String.valueOf((char)(ces.at(0]>>>16)) +
            // String.valueOf((char)(ces.at(0] & 0xFFFF))
            // + String.valueOf((char)(ce2>>>16)) + String.valueOf((char)(ce2 &
            // 0xFFFF))

            if (s.equals("\u0660") || s.equals("\u2080")) {
                System.out.println(Default.ucd().getCodeAndName(s) + "\t" + Utility.hex(key));
            }

            ordered.put(key, s);
            alreadyDone.add(s);

            String result = ordered.get(key);
            if (result == null) {
                System.out.println("BAD SORT: " + Utility.hex(key) + ", " + Utility.hex(s));
            }
        }

        System.out.println("Checking CJK");

        // Check for characters that are ARE explicitly mapped in the CJK ranges
        UnicodeSet CJK = new UnicodeSet(0x2E80, 0x2EFF);
        CJK.add(0x2F00, 0x2EFF);
        CJK.add(0x2F00, 0x2FDF);
        CJK.add(0x3400, 0x9FFF);
        CJK.add(0xF900, 0xFAFF);
        CJK.add(0x20000, 0x2A6DF);
        CJK.add(0x2F800, 0x2FA1F);
        CJK.removeAll(new UnicodeSet("[:Cn:]")); // remove unassigned

        // make set with canonical decomposibles
        UnicodeSet composites = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd().isAllocated(i)) {
                continue;
            }
            if (Default.nfd().isNormalized(i)) {
                continue;
            }
            composites.add(i);
        }
        UnicodeSet CJKcomposites = new UnicodeSet(CJK).retainAll(composites);
        System.out.println("CJK composites " + CJKcomposites.toPattern(true));
        System.out.println("CJK NONcomposites " + new UnicodeSet(CJK).removeAll(composites).toPattern(true));

        UnicodeSet mapped = new UnicodeSet();
        Iterator<String> it = alreadyDone.iterator();
        while (it.hasNext()) {
            String member = it.next();
            mapped.add(member);
        }
        UnicodeSet CJKmapped = new UnicodeSet(CJK).retainAll(mapped);
        System.out.println("Mapped CJK: " + CJKmapped.toPattern(true));
        System.out.println("UNMapped CJK: " + new UnicodeSet(CJK).removeAll(mapped).toPattern(true));
        System.out.println("Neither Mapped nor Composite CJK: "
                + new UnicodeSet(CJK).removeAll(CJKcomposites).removeAll(CJKmapped).toPattern(true));

        /*
         * 2E80..2EFF; CJK Radicals Supplement 2F00..2FDF; Kangxi Radicals
         * 
         * 3400..4DBF; CJK Unified Ideographs Extension A 4E00..9FFF; CJK
         * Unified Ideographs F900..FAFF; CJK Compatibility Ideographs
         * 
         * 20000..2A6DF; CJK Unified Ideographs Extension B 2F800..2FA1F; CJK
         * Compatibility Ideographs Supplement
         */

        System.out.println("Adding Kanji");
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd().isAllocated(i)) {
                continue;
            }
            if (Default.nfkd().isNormalized(i)) {
                continue;
            }
            Utility.dot(i);
            String decomp = Default.nfkd().normalize(i);
            int cp;
            for (int j = 0; j < decomp.length(); j += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(decomp, j);
                String s = UTF16.valueOf(cp);
                if (alreadyDone.contains(s)) {
                    continue;
                }

                alreadyDone.add(s);
                CEList ces = getCollator(collatorType2).getCEList(s, true);

                log2.println(s + "\t" + ces
                        + "\t" + Default.ucd().getCodeAndName(s) + " from " + Default.ucd().getCodeAndName(i));

                addToBackMap(backMap, ces, s, false);
            }
        }
        
        System.out.println("Find Exact Equivalents");
        
        Set<String> removals = new HashSet<String>();
        Map<String,String> equivalentsMap = findExactEquivalents(backMap, ordered, collatorType2, removals);
        for (String s : removals) {
            ordered.remove(s);
        }
        
        System.out.println("Writing");

        String filename = "UCA_Rules";
        if (collatorType2 == CollatorType.ducet) {
            filename += "_DUCET";
        }
        if (shortPrint) {
            filename += "_SHORT";
        }
        if (noCE) {
            filename += "_NoCE";
        }
        if (option == IN_XML) {
            filename += ".xml";
        } else {
            filename += ".txt";
        }

        String directory = UCA.getUCA_GEN_DIR() + File.separator
                + (collatorType2==CollatorType.cldr ? "CollationAuxiliary" : "Ducet");

        log = Utility.openPrintWriter(directory, filename, Utility.UTF8_WINDOWS);

        //        String[] commentText = {
        //                filename,
        //                "This file contains the UCA tables for the given version, but transformed into rule syntax.",
        //                "Generated:   " + getNormalDate(),
        //                "NOTE: Since UCA handles canonical equivalents, no composites are necessary",
        //                "(except in extensions).",
        //                "For syntax description, see: http://oss.software.ibm.com/icu/userguide/Collate_Intro.html"
        //        };

        if (option == IN_XML) {
            log.println("<collation>");
            log.println("<!--");
            WriteCollationData.writeVersionAndDate(log, filename, collatorType2==CollatorType.cldr);
            log.println("-->");
            log.println("<base uca='" + getCollator(collatorType2).getDataVersion() + "/" + getCollator(collatorType2).getUCDVersion() + "'/>");
            log.println("<rules>");
        } else {
            log.write('\uFEFF'); // BOM
            WriteCollationData.writeVersionAndDate(log, filename, collatorType2==CollatorType.cldr);
        }

        it = ordered.keySet().iterator();

        // String lastSortKey = collator.getSortKey("\u0000");;
        // 12161004
        int lastCE = 0;
        int ce = 0;
        int nextCE = 0;

        final CEList bogusCes = new CEList(new int[] {});
        boolean firstTime = true;

        boolean done = false;

        String chr = "";
        CEList ces = bogusCes;

        String nextChr = "";
        CEList nextCes = bogusCes;  // bogusCes signals that we need to skip!!

        String lastChr = "";
        CEList lastCes = CEList.EMPTY;
        int lastExpansionStart = 0;
        int expansionStart = 0;

        // for debugging ordering
        String lastSortKey = "";
        boolean showNext = false;

        for (int loopCounter = 0; !done; loopCounter++) {
            Utility.dot(loopCounter);

            lastCE = ce;
            lastChr = chr;
            lastExpansionStart = expansionStart;
            lastCes = ces;

            // copy the current from Next

            ce = nextCE;
            chr = nextChr;
            ces = nextCes;

            // We need to look ahead one, to be able to reset properly

            if (it.hasNext()) {
                String nextSortKey = it.next();
                nextChr = ordered.get(nextSortKey);
                int result = cm.compare(nextSortKey, lastSortKey);
                if (result < 0) {
                    System.out.println();
                    System.out.println("DANGER: Sort Key Unordered!");
                    System.out.println((loopCounter - 1) + " " + Utility.hex(lastSortKey)
                            + ", " + Default.ucd().getCodeAndName(lastSortKey.charAt(lastSortKey.length() - 1)));
                    System.out.println(loopCounter + " " + Utility.hex(nextSortKey)
                            + ", " + Default.ucd().getCodeAndName(nextSortKey.charAt(nextSortKey.length() - 1)));
                }
                if (nextChr == null) {
                    Utility.fixDot();
                    if (!showNext) {
                        System.out.println();
                        System.out.println((loopCounter - 1) + "   Last = " + Utility.hex(lastSortKey)
                                + ", " + Default.ucd().getCodeAndName(lastSortKey.charAt(lastSortKey.length() - 1)));
                    }
                    System.out.println(cm.compare(lastSortKey, nextSortKey)
                            + ", " + cm.compare(nextSortKey, lastSortKey));
                    System.out.println(loopCounter + " NULL AT  " + Utility.hex(nextSortKey)
                            + ", " + Default.ucd().getCodeAndName(nextSortKey.charAt(nextSortKey.length() - 1)));
                    nextChr = "??";
                    showNext = true;
                } else if (showNext) {
                    showNext = false;
                    System.out.println(cm.compare(lastSortKey, nextSortKey)
                            + ", " + cm.compare(nextSortKey, lastSortKey));
                    System.out.println(loopCounter + "   Next = " + Utility.hex(nextSortKey)
                            + ", " + Default.ucd().getCodeAndName(nextChr));
                }
                lastSortKey = nextSortKey;
            } else {
                nextChr = "??";
                done = true; // make one more pass!!!
            }

            nextCes = getCollator(collatorType2).getCEList(nextChr, true);
            nextCE = nextCes.isEmpty() ? 0 : nextCes.at(0);

            // skip first (fake) element

            if (ces == bogusCes) {
                continue;
            }

            // for debugging

            if (loopCounter < 5) {
                System.out.println(loopCounter);
                System.out.println(lastCes.toString() + ", " + Default.ucd().getCodeAndName(lastChr));
                System.out.println(ces.toString() + ", " + Default.ucd().getCodeAndName(chr));
                System.out.println(nextCes.toString() + ", " + Default.ucd().getCodeAndName(nextChr));
            }

            // get relation

            /*
             * if (chr.charAt(0) == 0xFFFB) { System.out.println("DEBUG"); }
             */

            if (chr.equals("\u0966")) {
                System.out.println(ces.toString());
            }

            expansionStart = getFirstCELen(ces);

            int relation = getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);

            if (relation == QUARTERNARY_DIFF) {
                int relation2 = getStrengthDifference(ces, ces.length(), lastCes, lastCes.length());
                if (relation2 != QUARTERNARY_DIFF) {
                    relation = TERTIARY_DIFF;
                }
            }

            // RESETs: do special case for relations to fixed items

            String reset = "";
            String resetComment = "";
            boolean insertVariableTop = false;
            boolean resetToParameter = false;

            int ceLayout = getCELayout(ce, getCollator(collatorType2));
            if (ceLayout == IMPLICIT) {
                if (relation == PRIMARY_DIFF) {
                    int primary = CEList.getPrimary(ce);
                    int resetCp = UCA.ImplicitToCodePoint(primary, CEList.getPrimary(ces.at(1)));

                    CEList ces2 = getCollator(collatorType2).getCEList(UTF16.valueOf(resetCp), true);
                    relation = getStrengthDifference(ces, ces.length(), ces2, ces2.length());

                    reset = quoteOperand(UTF16.valueOf(resetCp));
                    if (!shortPrint) {
                        resetComment = Default.ucd().getCodeAndName(resetCp);
                    }
                    // lastCE = UCA.makeKey(primary, UCA.NEUTRAL_SECONDARY,
                    // UCA.NEUTRAL_TERTIARY);
                }
                // lastCJKPrimary = primary;
            } else if (ceLayout != getCELayout(lastCE, getCollator(collatorType2)) || firstTime) {
                resetToParameter = true;
                switch (ceLayout) {
                case T_IGNORE:
                    reset = "last tertiary ignorable";
                    break;
                case S_IGNORE:
                    reset = "last secondary ignorable";
                    break;
                case P_IGNORE:
                    reset = "last primary ignorable";
                    break;
                case VARIABLE:
                    reset = "last regular";
                    break;
                case NON_IGNORE: /* reset = "top"; */
                    insertVariableTop = true;
                    break;
                case TRAILING:
                    reset = "last trailing";
                    break;
                }
            }

            // There are double-CEs, so we have to know what the length of the
            // first bit is.

            // check expansions

            String expansion = "";
            if (ces.length() > expansionStart) {
                // int tert0 = ces.at(0] & 0xFF;
                // boolean isCompat = tert0 != 2 && tert0 != 8;
                log2.println("Exp: " + Default.ucd().getCodeAndName(chr) + ", " + ces + ", start: " + expansionStart);
                int[] rel = { relation };
                expansion = getFromBackMap(backMap, ces, expansionStart, ces.length(), chr, rel);
                // relation = rel[0];

                // The relation needs to be fixed differently. Since it is an
                // expansion, it should be compared to
                // the first CE
                // ONLY reset if the sort keys are not equal
                if (false && (relation == PRIMARY_DIFF || relation == SECONDARY_DIFF)) {
                    int relation2 = getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);
                    if (relation2 != relation) {
                        System.out.println();
                        System.out.println("Resetting: " + RELATION_NAMES[relation] + " to " + RELATION_NAMES[relation2]);
                        System.out.println("LCes: " + lastCes + ", " + lastExpansionStart
                                + ", " + Default.ucd().getCodeAndName(lastChr));
                        System.out.println("Ces:  " + ces + ", " + expansionStart
                                + ", " + Default.ucd().getCodeAndName(chr));
                        relation = relation2;
                    }
                }

            }

            // print results
            // skip printing if it ends with a half-surrogate
            char lastChar = chr.charAt(chr.length() - 1);
            if (Character.isHighSurrogate(lastChar)) {
                System.out.println("Skipping trailing surrogate: " + chr + "\t" + Utility.hex(chr));
            } else {
                if (option == IN_XML) {
                    if (insertVariableTop) {
                        log.println(XML_RELATION_NAMES[0] + "<variableTop/>");
                    }

                    /*
                     * log.print("  <!--" + ucd.getCodeAndName(chr)); if (len > 1)
                     * log.print(" / " + Utility.hex(expansion));
                     * log.println("-->");
                     */

                    if (reset.length() != 0) {
                        log.println("<reset/>"
                                + (resetToParameter ? "<position at=\"" + reset + "\"/>" : Utility.quoteXML(reset))
                                + (resetComment.length() != 0 ? "<!-- " + resetComment + "-->" : ""));
                    }
                    if (expansion.length() > 0) {
                        log.print("<x>");
                    }
                    if (!firstTime) {
                        log.print("  <" + XML_RELATION_NAMES[relation] + ">");
                        log.print(Utility.quoteXML(chr));
                        log.print("</" + XML_RELATION_NAMES[relation] + ">");
                    }

                    // <x><t>&#x20A8;</t><extend>s</extend></x> <!--U+20A8 RUPEE SIGN / 0073-->

                    if (expansion.length() > 0) {
                        log.print("<extend>" + Utility.quoteXML(expansion) + "</extend></x>");
                    }
                    if (!shortPrint) {
                        log.print("\t<!--");
                        if (!noCE) {
                            log.print(ces.toString() + " ");
                        }
                        log.print(Default.ucd().getCodeAndName(chr));
                        if (expansion.length() > 0) {
                            log.print(" / " + Utility.hex(expansion));
                        }
                        log.print("-->");
                    }
                    log.println();
                } else {
                    if (insertVariableTop) {
                        log.println(RELATION_NAMES[0] + " [variable top]");
                    }
                    if (reset.length() != 0) {
                        log.println("& "
                                + (resetToParameter ? "[" : "") + reset + (resetToParameter ? "]" : "")
                                + (resetComment.length() != 0 ? "\t\t# " + resetComment : ""));
                    }
                    if (!firstTime) {
                        log.print(RELATION_NAMES[relation] + " " + quoteOperand(chr));
                    }
                    if (expansion.length() > 0) {
                        log.print(" / " + quoteOperand(expansion));
                    }
                    if (!shortPrint) {
                        log.print("\t# ");
                        if (false) {
                            if (latestAge(chr).startsWith("5.2")) {
                                log.print("† ");
                            }
                        }

                        log.print(latestAge(chr) + " [");
                        String typeKD = ReorderingTokens.getTypesCombined(chr);
                        log.print(typeKD + "] ");

                        if (!noCE) {
                            log.print(ces.toString() + " ");
                        }
                        log.print(Default.ucd().getCodeAndName(chr));
                        if (expansion.length() > 0) {
                            log.print(" / " + Utility.hex(expansion));
                        }
                    }
                    log.println();
                }
            }
            firstTime = false;
        }
        for (Entry<String, String> sourceReplacement : equivalentsMap.entrySet()) {
            // note: we set the reset to the value we want, then have 
            // = X for the item whose value is to be changed
            String valueToSetTo = sourceReplacement.getValue();
            String stringToSet = sourceReplacement.getKey();
            if (option == IN_XML) {
                log.print("<reset/>" 
                        + Utility.quoteXML(valueToSetTo)
                        + "<i>" 
                        + Utility.quoteXML(stringToSet) 
                        + "</i>");
                if (!shortPrint) {
                    log.print("\t<!--");
                    log.print(Default.ucd().getCodeAndName(stringToSet)
                            + "\t→\t" 
                            + Default.ucd().getCodeAndName(valueToSetTo));
                    log.print("-->");
                }
            } else {
                log.print("& " 
                        + quoteOperand(valueToSetTo)
                        + " = " 
                        + quoteOperand(stringToSet));
                if (!shortPrint) {
                    log.print("\t# ");
                    log.print(latestAge(stringToSet) + " [");
                    String typeKD = ReorderingTokens.getTypesCombined(stringToSet);
                    log.print(typeKD + "] ");
                    log.print(Default.ucd().getCodeAndName(stringToSet) 
                            + "\t→\t" 
                            + Default.ucd().getCodeAndName(valueToSetTo));
                }
            }
            log.println();
        }
        // log.println("& [top]"); // RESET
        if (option == IN_XML) {
            log.println("</rules></collation>");
        }
        log2.close();
        log.close();
        Utility.fixDot();
    }

    private static final UnicodeSet SKIP_TIBETAN_EQUIVALENTS = new UnicodeSet("[ྲཱི  ྲཱི ྲཱུ  ྲཱུ ླཱི  ླཱི ླཱུ  ླཱུ]").freeze();
    private static Map<String, String> findExactEquivalents(
            Map<ArrayWrapper, String> backMap, Map<String, String> ordered, 
            CollatorType collatorType2,
            Set<String> removals) {
        Map<String, String> equivalentsStrings = new LinkedHashMap<String, String>();
        IntStack nextCes = new IntStack(10);
        int[] startBuffer = new int[100];
        int[] endBuffer = new int[100];
        ArrayWrapper start = new ArrayWrapper(startBuffer, 0, 0);
        ArrayWrapper end = new ArrayWrapper(endBuffer, 0, 0);
        for (Entry<String, String> entry : ordered.entrySet()) {
            String sortKey = entry.getKey();
            String string = entry.getValue();
            if (Character.codePointCount(string, 0, string.length()) < 2) {
                continue;
            } else if (SKIP_TIBETAN_EQUIVALENTS.containsSome(string)) {
                continue;
            }
            nextCes.clear();
            getCollator(collatorType2).getCEs(string, true, nextCes);
            int len = nextCes.length();
            if (len < 2) {
                continue;
            }
            // just look for pairs
            for (int i = 1; i < len; ++i) {
                start.limit = nextCes.extractInto(0, i, startBuffer, 0);
                String string1 = backMap.get(start);
                if (string1 == null) {
                    continue;
                }
                end.limit = nextCes.extractInto(i, len, endBuffer, 0);
                String string2 = backMap.get(end);
                if (string2 == null) {
                    continue;
                }
                String replacement = string1 + string2;
                if (string.equals(replacement)) {
                    continue;
                }
                equivalentsStrings.put(string, replacement);
                removals.add(sortKey);
            }
        }
        return equivalentsStrings;
    }

    private static String bidiBracket(String string) {
        if (BIDI.containsSome(string)) {
            return LRM + string + LRM;
        }
        return string;
    }

    private static final UnicodeProperty bidiProp = getToolUnicodeSource().getProperty("bc");
    private static final UnicodeSet      BIDI     = new UnicodeSet(bidiProp.getSet("AL")).addAll(bidiProp.getSet("R")).freeze();
    private static final String          LRM      = "\u200E";

    private static String latestAge(String chr) {
        int cp;
        String latestAge = "";
        for (int i = 0; i < chr.length(); i += Character.charCount(cp)) {
            String age = getAge(cp = chr.codePointAt(i));
            if (latestAge.compareTo(age) < 0) {
                latestAge = age;
            }
        }
        // if (latestAge.endsWith(".0")) {
        // latestAge = latestAge.substring(0, latestAge.length() - 2);
        // }
        return latestAge;
    }

    private static UnicodeProperty ageProp;

    private static String getAge(int cp) {
        if (ageProp == null)
            ageProp = getToolUnicodeSource().getProperty("age");
        return ageProp.getValue(cp, true);
    }

    private static final int T_IGNORE = 1, S_IGNORE = 2, P_IGNORE = 3, VARIABLE = 4, NON_IGNORE = 5, IMPLICIT = 6, TRAILING = 7;

    private static int getCELayout(int ce, UCA collator) {
        int primary = CEList.getPrimary(ce);
        int secondary = CEList.getSecondary(ce);
        int tertiary = CEList.getSecondary(ce);
        if (primary == 0) {
            if (secondary == 0) {
                if (tertiary == 0) {
                    return T_IGNORE;
                }
                return S_IGNORE;
            }
            return P_IGNORE;
        }
        if (collator.isVariable(ce)) {
            return VARIABLE;
        }
        if (primary < UNSUPPORTED_BASE) {
            return NON_IGNORE;
        }
        if (primary < UNSUPPORTED_LIMIT) {
            return IMPLICIT;
        }
        return TRAILING;
    }

    private static final int
    PRIMARY_DIFF = 0,
    SECONDARY_DIFF = 1,
    TERTIARY_DIFF = 2,
    QUARTERNARY_DIFF = 3,
    DONE = -1;

    private static class CE_Iterator {
        CEList ces;
        int   len;
        int   current;
        int   level;

        void reset(CEList ces, int len) {
            this.ces = ces;
            this.len = len;
            current = 0;
            level = PRIMARY_DIFF;
        }

        void setLevel(int level) {
            current = 0;
            this.level = level;
        }

        int next() {
            int val = DONE;
            while (current < len) {
                int ce = ces.at(current++);
                switch (level) {
                case PRIMARY_DIFF:
                    val = CEList.getPrimary(ce);
                    break;
                case SECONDARY_DIFF:
                    val = CEList.getSecondary(ce);
                    break;
                case TERTIARY_DIFF:
                    val = CEList.getTertiary(ce);
                    break;
                }
                if (val != 0) {
                    return val;
                }
            }
            return DONE;
        }
    }

    private static CE_Iterator ceit1 = new CE_Iterator();
    private static CE_Iterator ceit2 = new CE_Iterator();

    // WARNING, Never Recursive!

    private static int getStrengthDifference(CEList ces, int len, CEList lastCes, int lastLen) {
        if (false && lastLen > 0 && lastCes.at(0) > 0) {
            System.out.println("DeBug");
        }
        ceit1.reset(ces, len);
        ceit2.reset(lastCes, lastLen);

        for (int level = PRIMARY_DIFF; level <= TERTIARY_DIFF; ++level) {
            ceit1.setLevel(level);
            ceit2.setLevel(level);
            while (true) {
                int weight1 = ceit1.next();
                int weight2 = ceit2.next();
                if (weight1 != weight2) {
                    return level;
                }
                if (weight1 == DONE) {
                    break;
                }
            }
        }
        return QUARTERNARY_DIFF;
    }

    private static final String[] RELATION_NAMES     = { " <\t", "  <<\t", "   <<<\t", "    =\t" };
    private static final String[] XML_RELATION_NAMES = { "p", "s", "t", "i" };

    private static class ArrayWrapper {
        int[] array;
        int   start;
        int   limit;

        /*
         * public ArrayWrapper(int[] contents) { set(contents, 0,
         * contents.length); }
         */

        public ArrayWrapper(int[] contents, int start, int limit) {
            set(contents, start, limit);
        }

        private void set(int[] contents, int start, int limit) {
            array = contents;
            this.start = start;
            this.limit = limit;
        }

        public boolean equals(Object other) {
            ArrayWrapper that = (ArrayWrapper) other;
            if (that.limit - that.start != limit - start) {
                return false;
            }
            for (int i = start; i < limit; ++i) {
                if (array[i] != that.array[i - start + that.start]) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            int result = limit - start;
            for (int i = start; i < limit; ++i) {
                result = result * 37 + array[i];
            }
            return result;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int i = start; i < limit; ++i) {
                if (result.length() != 0) {
                    result.append(",");
                }
                result.append(Utility.hex(0xFFFFFFFFL & array[i]));
            }
            return result.toString();
        }
    }

    private static int testCase[] = {
        UCA.makeKey(0x0255, 0x0020, 0x000E),
    };

    private static String testString = "\u33C2\u002E";

    private static boolean contains(int[] array, int start, int limit, int key) {
        for (int i = start; i < limit; ++i) {
            if (array[i] == key) {
                return true;
            }
        }
        return false;
    }

    private static final void addToBackMap(Map<ArrayWrapper,String> backMap, CEList ces, String s, boolean show) {
        if (show || contains(testCase, 0, testCase.length, ces.at(0)) || testString.indexOf(s) > 0) {
            System.out.println("Test case: " + Utility.hex(s) + ", " + ces);
        }
        // NOTE: we add the back map based on the string value; the smallest
        // (UTF-16 order) string wins
        int[] cesArray = new int[ces.length()];
        ArrayWrapper key = new ArrayWrapper(cesArray, 0, ces.appendTo(cesArray, 0));
        if (false) {
            String value = backMap.get(key);
            if (value == null) {
                return;
            }
            if (s.compareTo(value) >= 0) {
                return;
            }
        }
        backMap.put(key, s);
    }

    private static final String getFromBackMap(Map<ArrayWrapper, String> backMap, CEList originalces, int expansionStart, int len, String chr, int[] rel) {
        int[] ces = new int[originalces.length()];
        originalces.appendTo(ces, 0);

        String expansion = "";

        // process ces to neutralize tertiary

        for (int i = expansionStart; i < len; ++i) {
            int probe = ces[i];
            char primary = CEList.getPrimary(probe);
            char secondary = CEList.getSecondary(probe);
            char tertiary = CEList.getTertiary(probe);

            int tert = tertiary;
            switch (tert) {
            case 8:
            case 9:
            case 0xA:
            case 0xB:
            case 0xC:
            case 0x1D:
                tert = 8;
                break;
            case 0xD:
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x1C:
                tert = 0xE;
                break;
            default:
                tert = 2;
                break;
            }
            ces[i] = UCA.makeKey(primary, secondary, tert);
        }

        for (int i = expansionStart; i < len;) {
            int limit;
            String s = null;
            for (limit = len; limit > i; --limit) {
                ArrayWrapper wrapper = new ArrayWrapper(ces, i, limit);
                s = backMap.get(wrapper);
                if (s != null) {
                    break;
                }
            }
            if (s == null) {
                do {
                    if (getCollator(CollatorType.ducet).getHomelessSecondaries().contains(CEList.getSecondary(ces[i]))) {
                        s = "";
                        if (rel[0] > 1) {
                            rel[0] = 1; // HACK
                        }
                        break;
                    }

                    // Try stomping the value to different tertiaries

                    int probe = ces[i];
                    if (UCA.isImplicitLeadCE(probe)) {
                        s = UTF16.valueOf(UCA.ImplicitToCodePoint(CEList.getPrimary(probe), CEList.getPrimary(ces[i + 1])));
                        ++i; // skip over next item!!
                        break;
                    }

                    char primary = CEList.getPrimary(probe);
                    char secondary = CEList.getSecondary(probe);

                    ces[i] = UCA.makeKey(primary, secondary, 2);
                    ArrayWrapper wrapper = new ArrayWrapper(ces, i, i + 1);
                    s = backMap.get(wrapper);
                    if (s != null) {
                        break;
                    }

                    ces[i] = UCA.makeKey(primary, secondary, 0xE);
                    wrapper = new ArrayWrapper(ces, i, i + 1);
                    s = backMap.get(wrapper);
                    if (s != null) {
                        break;
                    }

                    // we failed completely. Print error message, and bail

                    System.out.println("Fix Homeless! No back map for " + CEList.toString(ces[i])
                            + " from " + CEList.toString(ces, len));
                    System.out.println("\t" + Default.ucd().getCodeAndName(chr)
                            + " => " + Default.ucd().getCodeAndName(Default.nfkd().normalize(chr))
                            );
                    s = "[" + Utility.hex(ces[i]) + "]";
                } while (false); // exactly one time, just for breaking
                limit = i + 1;
            }
            expansion += s;
            i = limit;
        }
        return expansion;
    }

    private static StringBuffer   quoteOperandBuffer           = new StringBuffer();                            // faster

    private static UnicodeSet     needsQuoting                 = null;
    private static UnicodeSet     needsUnicodeForm             = null;

    static final String quoteOperand(String s) {
        if (needsQuoting == null) {
            ToolUnicodePropertySource ups = getToolUnicodeSource();
            UnicodeProperty cat = ups.getProperty("gc");
            UnicodeSet cn = cat.getSet("Cn");
            /*
             * c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <=
             * '9' || (c >= 0xA0 && !UCharacterProperty.isRuleWhiteSpace(c))
             */
            needsQuoting = new UnicodeSet("[[:whitespace:][:z:][:c:][:ascii:]-[a-zA-Z0-9]-[:cn:]]").addAll(cn); // 
            // "[[:ascii:]-[a-zA-Z0-9]-[:c:]-[:z:]]"); //
            // [:whitespace:][:c:][:z:]
            // for (int i = 0; i <= 0x10FFFF; ++i) {
            // if (UCharacterProperty.isRuleWhiteSpace(i)) needsQuoting.add(i);
            // }
            // needsQuoting.remove();
            needsUnicodeForm = new UnicodeSet("[\\u000d\\u000a[:zl:][:zp:][:c:][:di:]-[:cn:]]").addAll(cn);
        }
        s = Default.nfc().normalize(s);
        quoteOperandBuffer.setLength(0);
        boolean noQuotes = true;
        boolean inQuote = false;
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (!needsQuoting.contains(cp)) {
                if (inQuote) {
                    quoteOperandBuffer.append('\'');
                    inQuote = false;
                }
                quoteOperandBuffer.append(UTF16.valueOf(cp));
            } else {
                noQuotes = false;
                if (cp == '\'') {
                    quoteOperandBuffer.append("''");
                } else {
                    if (!inQuote) {
                        quoteOperandBuffer.append('\'');
                        inQuote = true;
                    }
                    if (!needsUnicodeForm.contains(cp)) {
                        quoteOperandBuffer.append(UTF16.valueOf(cp)); // cp !=
                        // 0x2028
                    } else if (cp > 0xFFFF) {
                        quoteOperandBuffer.append("\\U").append(Utility.hex(cp, 8));
                    } else if (cp <= 0x20 || cp > 0x7E) {
                        quoteOperandBuffer.append("\\u").append(Utility.hex(cp));
                    } else {
                        quoteOperandBuffer.append(UTF16.valueOf(cp));
                    }
                }
            }
            /*
             * switch (c) { case '<': case '>': case '#': case '=': case '&':
             * case '/': quoteOperandBuffer.append('\'').append(c).append('\'');
             * break; case '\'': quoteOperandBuffer.append("''"); break;
             * default: if (0 <= c && c < 0x20 || 0x7F <= c && c < 0xA0) {
             * quoteOperandBuffer.append("\\u").append(Utility.hex(c)); break; }
             * quoteOperandBuffer.append(c); break; }
             */
        }
        if (inQuote) {
            quoteOperandBuffer.append('\'');
        }

        if (noQuotes) {
            return bidiBracket(s); // faster
        }
        return bidiBracket(quoteOperandBuffer.toString());
    }

    // Do not print a full date+time, to reduce gratuitous file changes.
    private static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // was "yyyy-MM-dd','HH:mm:ss' GMT'" in UCA 6.2

    static String getNormalDate() {
        // return Default.getDate() + " [MD]";
        String noDate = System.getProperty("NODATE");
        if (noDate != null) {
            return "(date omitted)";
        }
    	String date = myDateFormat.format(new Date());
    	String author = System.getProperty("AUTHOR");
    	if (author == null) {
    	    author = " [MS]";
    	} else if (author.isEmpty()) {
    	    // empty value in -DAUTHOR= or -DAUTHOR means add no author
    	} else {
    	    author = " [" + author + ']';
    	}
    	return date + author;
    }

    private static UnicodeSet compatibilityExceptions = new UnicodeSet("[\u0CCB\u0DDD\u017F\u1E9B\uFB05]");

    // Called by UCA.Main.
    static void writeCollationValidityLog() throws IOException {
        log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "CheckCollationValidity.html", Utility.UTF8_WINDOWS);

        log.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>\n" +
        		"<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>UCA Validity Log</title>");
        log.println("<style type='text/css'>");
        log.println("table { border-collapse: collapse; }");
        log.println(".bottom { border-bottom-style: solid; border-bottom-color: #0000FF; }\n");
        log.println(".bad { color:red; font-weight:bold; }");
        log.println(".warn { color:orange; font-weight:bold; }");
        log.println("</style>");
        log.println("</head><body bgcolor='#FFFFFF'>");

        // collator = new UCA(null);
        if (false) {
            String key = getCollator(CollatorType.ducet).getSortKey("\u0308\u0301", UCA.SHIFTED, false, AppendToCe.none);
            String look = printableKey(key);
            System.out.println(look);

        }
        System.out.println("Sorting");

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(null);
        // cc.setDoEnableSamples(true);
        UnicodeSet coverage = new UnicodeSet();

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            addString(s, option, CollatorType.ducet);
            coverage.add(s);
        }

        System.out.println("Total: " + sortedD.size());

        Iterator it;

        // ucd.init();

        if (false) {
            System.out.println("Listing Mismatches");
            it = duplicates.keySet().iterator();
            // String lastSortKey = "";
            // String lastSource = "";
            while (it.hasNext()) {
                String source = (String) it.next();
                String sortKey = (String) duplicates.get(source);
                char endMark = source.charAt(source.length() - 1);
                source = source.substring(0, source.length() - 1);
                if (endMark == MARK1) {
                    log.println("<br>");
                    log.println("Mismatch: " + Utility.hex(source, " ")
                            + ", " + Default.ucd().getName(source) + "<br>");
                    log.print("  NFD:");
                } else {
                    log.print("  NFC:");
                }
                log.println(UCA.toString(sortKey) + "<br>");

                /*
                 * if (source.equals(lastSource)) { it.remove();
                 * --duplicateCount; } //lastSortKey = sortKey; lastSource =
                 * lastSource;
                 */
            }
            System.out.println("Total: " + sortedD.size());
        }

        System.out.println("Writing");
        String version = getCollator(CollatorType.ducet).getDataVersion();

        log.println("<h1>Collation Validity Checks</h1>");
        log.println("<table><tr><td>Generated: </td><td>" + getNormalDate() + "</td></tr>");
        log.println("<tr><td>Unicode  Version: </td><td>" + getCollator(CollatorType.ducet).getUCDVersion());
        log.println("<tr><td>UCA Data Version (@version in file): </td><td>" + getCollator(CollatorType.ducet).getDataVersion());
        log.println("<tr><td>UCA File Name: </td><td>" + getCollator(CollatorType.ducet).getFileVersion());
        log.println("</td></tr></table>");

        if (getCollator(CollatorType.ducet).getDataVersion() == UCA.BADVERSION) {
            log.println(SERIOUS_ERROR);
        }

        checkScripts();

        if (GENERATED_NFC_MISMATCHES) {
            showMismatches();
        }
        removeAdjacentDuplicates2();

        UnicodeSet alreadySeen = new UnicodeSet(compatibilityExceptions);

        checkBadDecomps(1, false, alreadySeen); // if decomposition is off, all
        // primaries should be identical
        checkBadDecomps(2, false, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries
        // should be identical
        checkBadDecomps(3, false, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries
        // should be identical
        // checkBadDecomps(2, true, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries should be identical

        log.println("<p>Note: characters with decompositions to space + X, and tatweel + X are excluded,"
                + " as are a few special characters: " + compatibilityExceptions.toPattern(true) + "</p>");

        checkDefaultIgnorables();
        checkUnassigned();
        checkWellformedTable();
        addClosure();
        writeDuplicates();
        writeOverlap();

        log.println("<h2>11. Coverage</h2>");
        BagFormatter bf = new BagFormatter();
        bf.setLineSeparator("<br>\r\n");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        bf.setUnicodePropertyFactory(ups);
        bf.setShowLiteral(TransliteratorUtilities.toHTML);
        bf.setFixName(TransliteratorUtilities.toHTML);
        UCD ucd = Default.ucd();
        UnicodeProperty cat = ups.getProperty("gc");
        UnicodeSet ucdCharacters = cat.getSet("Cn")
                .addAll(cat.getSet("Co"))
                .addAll(cat.getSet("Cs"))
                .complement()
                // .addAll(ups.getSet("Noncharactercodepoint=true"))
                // .addAll(ups.getSet("Default_Ignorable_Code_Point=true"))
                ;
        bf.showSetDifferences(log, "UCD" + Default.ucdVersion(), ucdCharacters, getCollator(CollatorType.ducet).getFileVersion(), coverage, 3);

        log.println("</body></html>");
        log.close();
        sortedD.clear();
        System.out.println("Done");
    }

    private static void checkUnassigned() {
        int errorCount = 0;
        System.out.println("Checking that Unassigned characters have implicits");

        log.println("<h2>5b. Checking that Unassigned characters have implicit weights</h2>");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        UnicodeSet di = ups.getSet("gc=cn");
        UnicodeSet bad = new UnicodeSet();
        int[] output = new int[2];

        // the invariants are that there must be exactly 2 ces; the ces will
        // match codepointToImplicit
        for (String diChar : di) {
            CEList ceList = getCollator(CollatorType.ducet).getCEList(diChar, true);
            if (ceList.length() != 2) {
                bad.add(diChar);
            } else {
                UCA.UnassignedToImplicit(diChar.codePointAt(0), output);
                int ce0 = ceList.at(0);
                int ce1 = ceList.at(1);
                if (CEList.getPrimary(ce0) != output[0] || CEList.getPrimary(ce1) != output[1]) {
                    bad.add(diChar);
                }
            }
        }
        errorCount = bad.size();
        if (bad.size() == 0) {
            log.println("<h3>No Bad Characters</h3>");
        } else {
            log.println(SERIOUS_ERROR);
            log.println("<h3>Bad Unassigned Characters: " + bad.size() + ": " + bad + "</h3>");
        }
        log.flush();

    }

    private static void checkDefaultIgnorables() {
        UnicodeSet exceptions = new UnicodeSet("[\u115F\u1160\u17B4\u17B5\u3164\uFFA0]");
        System.out.println("Checking for defaultIgnorables");

        log.println("<h2>5a. Checking for Default Ignorables</h2>");
        log.println("<p>Checking that all Default Ignorables except " + exceptions + " should be secondary ignorables (L1 = L2 = 0)</p>");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        UnicodeSet di = new UnicodeSet(ups.getSet("default_ignorable_code_point=true")).removeAll(ups.getSet("gc=cn")).removeAll(exceptions);
        UnicodeSet bad = new UnicodeSet();
        for (String diChar : di) {
            CEList ceList = getCollator(CollatorType.ducet).getCEList(diChar, true);
            for (int i = 0; i < ceList.length(); ++i) {
                int ce = ceList.at(i);
                if (CEList.getPrimary(ce) != 0 || CEList.getSecondary(ce) != 0) {
                    bad.add(diChar);
                }
            }
        }
        if (bad.size() == 0) {
            log.println("<h3>No Bad Characters</h3>");
        } else {
            log.println(SERIOUS_ERROR);
            UnicodeSet badUnassigned = ups.getSet("gc=cn").retainAll(bad);
            UnicodeSet badAssigned = bad.removeAll(badUnassigned);
            log.println("<h3>Bad Assigned Characters: " + badAssigned.size() + ": " + badAssigned +
                    "</h3>");
            for (String diChar : badAssigned) {
                log.println("<p>" + Default.ucd().getCodeAndName(diChar) + "</p>");
            }
            log.println("<h3>Bad Unassigned Characters: " + badUnassigned.size() + ": " + badUnassigned +
                    "</h3>");
        }
        log.flush();
    }

    private static ToolUnicodePropertySource ups;

    private static ToolUnicodePropertySource getToolUnicodeSource() {
        if (ups == null)
            ups = ToolUnicodePropertySource.make(Default.ucdVersion());
        return ups;
    }

    private static void addClosure() {
        int canCount = 0;
        System.out.println("Add missing decomposibles");
        log.println("<h2>7. Comparing Other Equivalents</h2>");
        log.println("<p>These are usually problems with contractions.</p>");
        log.println("<p>Each of the three strings is canonically equivalent, but has different sort keys</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Count</th><th>Type</th><th>Name</th><th>Code</th><th>Sort Keys</th></tr>");

        Set<String> contentsForCanonicalIteration = new TreeSet<String>();
        UCA.UCAContents ucac = getCollator(CollatorType.ducet).getContents(null); // NFD
        int ccounter = 0;
        while (true) {
            Utility.dot(ccounter++);
            String s = ucac.next();
            if (s == null) {
                break;
            }
            contentsForCanonicalIteration.add(s);
        }

        Set<String> additionalSet = new HashSet<String>();

        System.out.println("Loading canonical iterator");
        if (canIt == null) {
            canIt = new CanonicalIterator(".");
        }
        Iterator<String> it2 = contentsForCanonicalIteration.iterator();
        System.out.println("Adding any FCD equivalents that have different sort keys");
        while (it2.hasNext()) {
            String key = it2.next();
            if (key == null) {
                System.out.println("Null Key");
                continue;
            }
            canIt.setSource(key);
            String nfdKey = Default.nfd().normalize(key);

            boolean first = true;
            while (true) {
                String s = canIt.next();
                if (s == null) {
                    break;
                }
                if (s.equals(key)) {
                    continue;
                }
                if (contentsForCanonicalIteration.contains(s)) {
                    continue;
                }
                if (additionalSet.contains(s)) {
                    continue;
                }

                // Skip anything that is not FCD.
                if (!Default.nfd().isFCD(s)) {
                    continue;
                }

                // We ONLY add if the sort key would be different
                // Than what we would get if we didn't decompose!!
                String sortKey = getCollator(CollatorType.ducet).getSortKey(s, UCA.NON_IGNORABLE);
                String nonDecompSortKey = getCollator(CollatorType.ducet).getSortKey(s, UCA.NON_IGNORABLE, false, AppendToCe.none);
                if (sortKey.equals(nonDecompSortKey)) {
                    continue;
                }

                if (DEBUG && first) {
                    System.out.println(" " + Default.ucd().getCodeAndName(key));
                    first = false;
                }
                log.println("<tr><td rowspan='3'>" + (++canCount) +
                        "</td><td>Orig.</td><td>" + Utility.replace(Default.ucd().getName(key), ", ", ",<br>") + "</td>");
                log.println("<td>" + Utility.hex(key) + "</td>");
                log.println("<td>" + UCA.toString(sortKey) + "</td></tr>");
                log.println("<tr><td>NFD</td><td>" + Utility.replace(Default.ucd().getName(nfdKey), ", ", ",<br>") + "</td>");
                log.println("<td>" + Utility.hex(nfdKey) + "</td>");
                log.println("<td>" + UCA.toString(sortKey) + "</td></tr>");
                log.println("<tr><td>Equiv.</td><td class='bottom'>" + Utility.replace(Default.ucd().getName(s), ", ", ",<br>") + "</td>");
                log.println("<td class='bottom'>" + Utility.hex(s) + "</td>");
                log.println("<td class='bottom'>" + UCA.toString(nonDecompSortKey) + "</td></tr>");
                additionalSet.add(s);
            }
        }
        log.println("</table>");
        log.println("<p>Errors: " + canCount + "</p>");
        if (canCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.flush();
    }

    private static void checkWellformedTable() throws IOException {
        int errorCount = 0;
        System.out.println("Checking for well-formedness");

        log.println("<h2>6. Checking for well-formedness</h2>");
        if (getCollator(CollatorType.ducet).haveVariableWarning) {
            log.println("<p><b>Ill-formed: alternate values overlap!</b></p>");
            errorCount++;
        }

        if (getCollator(CollatorType.ducet).haveZeroVariableWarning) {
            log.println("<p><b>Ill-formed: alternate values on zero primaries!</b></p>");
            errorCount++;
        }

        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(Default.nfd());

        int minps = Integer.MAX_VALUE;
        int minpst = Integer.MAX_VALUE;
        String minpsSample = "", minpstSample = "";

        while (true) {
            String str = cc.next();
            if (str == null) {
                break;
            }
            CEList ces = cc.getCEs();
            int len = ces.length();

            for (int i = 0; i < len; ++i) {
                int ce = ces.at(i);
                int p = CEList.getPrimary(ce);
                int s = CEList.getSecondary(ce);
                int t = CEList.getTertiary(ce);

                // Gather data for WF#2 check

                if (p == 0) {
                    if (s > 0) {
                        if (s < minps) {
                            minps = s;
                            minpsSample = str;
                        }
                    } else {
                        if (t > 0 && t < minpst) {
                            minpst = t;
                            minpstSample = str;
                        }
                    }
                }
            }
        }

        cc = getCollator(CollatorType.ducet).getContents(Default.nfd());
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        int lastPrimary = 0;

        while (true) {
            String str = cc.next();
            if (str == null) {
                break;
            }
            CEList ces = cc.getCEs();
            int len = ces.length();

            for (int i = 0; i < len; ++i) {
                int ce = ces.at(i);
                int p = CEList.getPrimary(ce);
                int s = CEList.getSecondary(ce);
                int t = CEList.getTertiary(ce);

                // IF we are at the start of an implicit, then just check that
                // the implicit is in range
                // CHECK implicit
                if (UCA.isImplicitLeadPrimary(lastPrimary)) {
                    try {
                        if (s != 0 || t != 0) {
                            throw new Exception("Second implicit must be [X,0,0]");
                        }
                        UCA.ImplicitToCodePoint(lastPrimary, p); // throws
                        // exception
                        // if bad
                    } catch (Exception e) {
                        log.println("<tr><td>" + (++errorCount) + ". BAD IMPLICIT: " + e.getMessage()
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                    // zap the primary, since we worry about the last REAL
                    // primary:
                    lastPrimary = 0;
                    continue;
                }

                // IF we are in the trailing range, something is wrong.
                if (p >= UCA_Types.UNSUPPORTED_LIMIT) {
                    log.println("<tr><td>" + (++errorCount) + ". > " + Utility.hex(UCA_Types.UNSUPPORTED_LIMIT, 4)
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    lastPrimary = p;
                    continue;
                }

                // Check WF#1

                if (p != 0 && s == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.1"
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }
                if (s != 0 && t == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.2"
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }

                // Check WF#2

                if (p != 0) {
                    if (s > minps) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.2"
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                }
                if (s != 0) {
                    if (t > minpst) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.3"
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                } else {
                }

                lastPrimary = p;

            }
        }
        log.println("</table>");
        log.println("<p>Minimum Secondary in Primary Ignorable = " + Utility.hex(minps)
                + " from \t" + getCollator(CollatorType.ducet).getCEList(minpsSample, true)
                + "\t" + Default.ucd().getCodeAndName(minpsSample) + "</p>");
        if (minpst < Integer.MAX_VALUE) {
            log.println("<p>Minimum Tertiary in Secondary Ignorable =" + Utility.hex(minpst)
                    + " from \t" + getCollator(CollatorType.ducet).getCEList(minpstSample, true)
                    + "\t" + Default.ucd().getCodeAndName(minpstSample) + "</p>");
        }

        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(SERIOUS_ERROR);
        }
        log.flush();
    }

    private static final String  SERIOUS_ERROR          = "<p><b><font color='#FF0000'>SERIOUS_ERROR!</font></b></p>";
    private static final String  IMPORTANT_ERROR        = "<p><b><font color='#FF0000'>IMPORTANT_ERROR!</font></b></p>";

    private static final char    MARK1                  = '\u0001';

    private static TreeMap<String, String> MismatchedC = new TreeMap<String, String>();
    private static TreeMap<String, String> MismatchedN = new TreeMap<String, String>();
    private static TreeMap<String, String> MismatchedD = new TreeMap<String, String>();

    private static final byte    option                 = UCA.NON_IGNORABLE;                                            // SHIFTED

    private static void addString(String ch, byte option, CollatorType collatorType) {
        String colDbase = getCollator(collatorType).getSortKey(ch, option, true, AppendToCe.none);
        String colNbase = getCollator(collatorType).getSortKey(ch, option, false, AppendToCe.none);
        String colCbase = getCollator(collatorType).getSortKey(Default.nfc().normalize(ch), option, false, AppendToCe.none);
        if (!colNbase.equals(colCbase) || !colNbase.equals(colDbase)) {
            /*
             * System.out.println(Utility.hex(ch));
             * System.out.println(printableKey(colNbase));
             * System.out.println(printableKey(colNbase));
             * System.out.println(printableKey(colNbase));
             */
            MismatchedN.put(ch, colNbase);
            MismatchedC.put(ch, colCbase);
            MismatchedD.put(ch, colDbase);
        }
        String colD = colDbase + "\u0000" + ch; // UCA.NON_IGNORABLE
        String colN = colNbase + "\u0000" + ch;
        sortedD.put(colD, ch);
        backD.put(ch, colD);
        sortedN.put(colN, ch);
        backN.put(ch, colN);
        /*
         * if (strength > 4) { duplicateCount++; duplicates.put(ch+MARK1, col);
         * duplicates.put(ch+MARK2, col2); } else if (strength != 0) {
         * sorted.put(col2 + MARK2, ch); } unique += 2;
         */
    }

    private static void removeAdjacentDuplicates2() {
        int errorCount = 0;
        Iterator<String> it = sortedD.keySet().iterator();
        log.println("<h1>2. Differences in Ordering</h1>");
        log.println("<p>Codes and names are in the white rows: bold means that the NO-NFD sort key differs from UCA key.</p>");
        log.println("<p>Keys are in the light blue rows: green is the bad key, blue is UCA, black is where they equal.</p>");
        log.println("<p>Note: so black lines are generally ok.</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>File Order</th><th>Code and Decomp</th><th>Key and Decomp-Key</th></tr>");

        String lastCol = "a";
        String lastColN = "a";
        String lastCh = "";
        boolean showedLast = true;
        int count = 0;
        while (it.hasNext()) {
            count++;
            String col = it.next();
            String ch = sortedD.get(col);
            String colN = backN.get(ch);
            if (colN == null || colN.length() < 1) {
                System.out.println("Missing colN value for " + Utility.hex(ch, " ") + ": " + printableKey(colN));
            }
            if (col == null || col.length() < 1) {
                System.out.println("Missing col value for " + Utility.hex(ch, " ") + ": " + printableKey(col));
            }

            if (compareMinusLast(col, lastCol) == compareMinusLast(colN, lastColN)) {
                showedLast = false;
            } else {
                if (true && count < 200) {
                    System.out.println();
                    System.out.println(Utility.hex(ch, " ") + ", " + Utility.hex(lastCh, " "));
                    System.out.println("      col: " + Utility.hex(col, " "));
                    System.out.println(compareMinusLast(col, lastCol));
                    System.out.println("  lastCol: " + Utility.hex(lastCol, " "));
                    System.out.println();
                    System.out.println("     colN: " + Utility.hex(colN, " "));
                    System.out.println(compareMinusLast(colN, lastColN));
                    System.out.println(" lastColN: " + Utility.hex(lastColN, " "));
                }
                if (!showedLast) {
                    log.println("<tr><td colspan='3'></td><tr>");
                    showLine(count - 1, lastCh, lastCol, lastColN);
                    errorCount++;
                }
                showedLast = true;
                showLine(count, ch, col, colN);
                errorCount++;
            }
            lastCol = col;
            lastColN = colN;
            lastCh = ch;
        }

        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        log.flush();
    }

    private static int compareMinusLast(String a, String b) {
        String am = a.substring(0, a.length() - 1);
        String bm = b.substring(0, b.length() - 1);
        int result = am.compareTo(b);
        return (result < 0 ? -1 : result > 0 ? 1 : 0);
    }

    private static void showLine(int count, String ch, String keyD, String keyN) {
        String decomp = Default.nfd().normalize(ch);
        if (decomp.equals(ch)) {
            decomp = "";
        } else {
            decomp = "<br><" + Utility.hex(decomp, " ") + "> ";
        }
        log.println("<tr><td>" + count + "</td><td>"
                + Utility.hex(ch, " ")
                + " " + Default.ucd().getName(ch)
                + decomp
                + "</td><td>");

        if (keyD.equals(keyN)) {
            log.println(printableKey(keyN));
        } else {
            log.println("<font color='#009900'>" + printableKey(keyN)
                    + "</font><br><font color='#000099'>" + printableKey(keyD) + "</font>"
                    );
        }
        log.println("</td></tr>");
    }

    private static final String[] alternateName = { "SHIFTED", "ZEROED", "NON_IGNORABLE", "SHIFTED_TRIMMED" };

    private static final ToolUnicodePropertySource propertySource = ToolUnicodePropertySource.make(null);
    private static final UnicodeProperty gc = propertySource.getProperty("gc");

    private static final UnicodeSet WHITESPACE = propertySource.getSet("whitespace=true")
            .freeze();

    private static final UnicodeSet IGNORABLE = new UnicodeSet()
    .addAll(gc.getSet("Cf"))
    .addAll(gc.getSet("Cc"))
    .addAll(gc.getSet("Mn"))
    .addAll(gc.getSet("Me"))
    .addAll(propertySource.getSet("Default_Ignorable_Code_Point=Yes"))
    .freeze();

    private static final UnicodeSet PUNCTUATION = new UnicodeSet()
    .addAll(gc.getSet("pc"))
    .addAll(gc.getSet("pd"))
    .addAll(gc.getSet("ps"))
    .addAll(gc.getSet("pe"))
    .addAll(gc.getSet("pi"))
    .addAll(gc.getSet("pf"))
    .addAll(gc.getSet("po"))
    .freeze();

    private static final UnicodeSet GENERAL_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sm"))
    .addAll(gc.getSet("Sk"))
    .addAll(gc.getSet("So"))
    .freeze();

    private static final UnicodeSet CURRENCY_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sc"))
    .freeze();

    private static final UnicodeSet NUMBERS = new UnicodeSet()
    .addAll(gc.getSet("Nd"))
    .addAll(gc.getSet("No"))
    .addAll(gc.getSet("Nl"))
    .freeze();

    private static final UnicodeSet OTHERS = new UnicodeSet(0,0x10FFFF)
    .removeAll(IGNORABLE)
    .removeAll(WHITESPACE)
    .removeAll(PUNCTUATION)
    .removeAll(GENERAL_SYMBOLS)
    .removeAll(CURRENCY_SYMBOLS)
    .removeAll(NUMBERS)
    .removeAll(gc.getSet("Cn"))
    .removeAll(gc.getSet("Cs"))
    .removeAll(gc.getSet("Co"))
    .freeze();

    private static final UnicodeMap<String> EMPTY = new UnicodeMap<String>().freeze();
    private static final UnicodeMap<String> IGNORABLE_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ࠤࠨःংঃਃઃଂଃఁ-ఃಂಃംഃංඃཿ းះៈᬄᮂ᳡ᳲ�ꢀꢁꦃ꯬�𑀀𑀂𑂂��𝅥𝅦𝅭-𝅲]"), "Unknown why these are ignored")
            .putAll(new UnicodeSet("[ـߺﱞ-ﱣﳲ-ﳴﹰ-ﹴﹶ-ﹿ]"), 
                    "Tatweel and related characters are ignorable; isolated vowels have screwy NFKD values")
                    .freeze();
    private static final UnicodeMap<String> GENERAL_SYMBOL_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ៗ ː ˑ ॱ ๆ ໆ ᪧ ꧏ ꩰ ꫝ 々 〻 〱-〵 ゝ ゞ ーｰ ヽ ヾ \uAAF3 \uAAF4]"), "regular (not ignorable) symbols - significant modifiers")
            .putAll(new UnicodeSet("[৴-৹ ୲-୷ ꠰-꠵ ௰-௲ ൰-൵ ፲-፼ ↀ-ↂ ↆ-ↈ 𐹩-𐹾 ⳽ 𐌢 𐌣 𐄐-𐄳 𐅀 𐅁 𐅄-𐅇 𐅉-𐅎 𐅐-𐅗 𐅠-𐅲 𐅴-𐅸 𐏓-𐏕 𐩾 𐤗-𐤙 𐡛-𐡟 𐭜-𐭟 𐭼-𐭿 𑁛-𑁥 𐩄-𐩇 𒐲 𒐳 𒑖 𒑗 𒑚-𒑢 𝍩-𝍱]"), 
                    "Unknown why these are treated differently than numbers with scripts")
                    .freeze();
    private static final UnicodeMap<String> SCRIPT_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[⺀-⺙⺛-⺞⺠-⻲]"), "CJK radicals sort like they had toNFKD values")
            .putAll(new UnicodeSet("[ ᅟᅠㅤﾠ]"), "Hangul fillers are Default Ignorables, but sort as primaries")
            .putAll(gc.getSet("Mn"), "Indic (or Indic-like) non-spacing marks sort as primaries")
            .putAll(new UnicodeSet("[🅐-🅩🅰-🆏🆑-🆚🇦-🇿]"), "Characters that should have toNFKD values")
            .putAll(new UnicodeSet("[ᛮ-ᛰꛦ-ꛯ𐍁𐍊]"), "Unknown why these numbers are treated differently than numbers with symbols.")
            .freeze();

    private enum UcaBucket {
        ignorable('\u0000', IGNORABLE, IGNORABLE_REASONS),
        whitespace('\u0009', WHITESPACE, EMPTY), 
        punctuation('\u203E', PUNCTUATION, EMPTY), 
        general_symbols('\u0060', GENERAL_SYMBOLS, GENERAL_SYMBOL_REASONS),
        currency_symbols('\u00A4', CURRENCY_SYMBOLS, EMPTY),
        numbers('\u0030', NUMBERS, EMPTY), 
        scripts('a', OTHERS, SCRIPT_REASONS),
        fail(-1, UnicodeSet.EMPTY, EMPTY);
        final int least;
        final UnicodeSet expected;
        final UnicodeSet exceptionsAllowed;
        final UnicodeMap<String> exceptionReasons;
        final UnicodeSet charOk = new UnicodeSet();
        final UnicodeSet bothOk = new UnicodeSet();
        final UnicodeSet nfkdOk = new UnicodeSet();
        final UnicodeSet warn = new UnicodeSet();
        final UnicodeSet failure = new UnicodeSet();

        UcaBucket(int least, UnicodeSet expected, UnicodeMap<String> toWarn) {
            this.least = least;
            this.expected = expected.freeze();
            this.exceptionsAllowed = toWarn.keySet().freeze();
            this.exceptionReasons = toWarn;
        }
        public UcaBucket next() {
            // TODO Auto-generated method stub
            return values()[ordinal()+1];
        }

        public void add(int codepoint) {
            String nfkd = Default.nfkd().normalize(codepoint);
            int nfkdCh = nfkd.codePointAt(0);

            if (expected.contains(codepoint)) {
                if (expected.contains(nfkdCh)) {
                    bothOk.add(codepoint);
                } else {
                    charOk.add(codepoint);
                }
                return;
            }

            if (expected.contains(nfkdCh)) {
                nfkdOk.add(codepoint);
                return;
            }

            if (exceptionsAllowed.contains(codepoint)) {
                warn.add(codepoint);
                return;
            }

            failure.add(codepoint);
        }
    }

    private static void checkScripts() {
        log.println("<h2>0. Check UCA ‘Bucket’ Assignment</h2>");
        log.println("<p>Alternate Handling = " + alternateName[option] + "</p>");
        //        log.println("<table border='1'>");
        //log.println("<tr><th>Status</th><th>Type</th><th>GC</th><th>Script</th><th>Ch</th><th>Code</th><th>CE</th><th>Name</th></tr>");
        UCA collator = getCollator(CollatorType.ducet);

        //        UnicodeSet ignore = new UnicodeSet()
        //        .addAll(propertySource.getSet("dt=none"))
        //        .complement()
        //        .freeze();
        //        
        //        UnicodeSet ignoreAboveA = new UnicodeSet()
        //        .addAll(propertySource.getSet("sc=Common"))
        //        .addAll(propertySource.getSet("sc=Inherited"))
        //        .complement()
        //        .freeze();
        //

        UnicodeSet mustSkip = new UnicodeSet()
        .addAll(gc.getSet("cn"))
        .addAll(gc.getSet("co"))
        .addAll(gc.getSet("cs"))
        .freeze();

        UnicodeSet shouldHave = new UnicodeSet(0,0x10FFFF)
        .removeAll(mustSkip)
        .removeAll(propertySource.getSet("Ideographic=true"))
        .removeAll(propertySource.getSet("Hangul_Syllable_Type=LVT"))
        .removeAll(propertySource.getSet("Hangul_Syllable_Type=LV"))
        .freeze();


        //UnicodeMap<UcaBucket> expectedOrder = new UnicodeMap();

        UnicodeSet lm = gc.getSet("Lm");

        UnicodeSet generalSymbols = new UnicodeSet()
        .addAll(gc.getSet("Sm"))
        .addAll(gc.getSet("Sk"))
        .addAll(gc.getSet("So"))
        .addAll(lm)
        .freeze();

        UnicodeSet currencySymbols = new UnicodeSet()
        .addAll(gc.getSet("Sc"))
        .freeze();

        UnicodeSet decimalNumber = new UnicodeSet()
        .addAll(gc.getSet("Nd"))
        .addAll(gc.getSet("No"))
        .addAll(gc.getSet("Nl"))
        .freeze();

        //        expectedOrder.putAll(WHITESPACE, UcaBucket.whitespace);
        //        expectedOrder.putAll(PUNCTUATION, UcaBucket.punctuation);
        //        expectedOrder.putAll(generalSymbols, UcaBucket.general_symbols);
        //        expectedOrder.putAll(currencySymbols, UcaBucket.currency_symbols);
        //        expectedOrder.putAll(decimalNumber, UcaBucket.numbers);

        UCAContents contents = collator.getContents(null);
        UnicodeSet covered = new UnicodeSet();
        UnicodeSet illegalCharacters = new UnicodeSet();
        int errorCount = 0;
        UcaBucket lastOrder = UcaBucket.whitespace;
        CEList lastCe = null;
        String lastString = "";
        String lastShown = "";

        TreeSet<Pair> sorted = new TreeSet<Pair>();
        String s;
        while ((s = contents.next()) != null) {
            sorted.add(new Pair(contents.getCEs(), s));
        }

        UnicodeSet funnyAboveA = new UnicodeSet();
        UnicodeSet ignorable = new UnicodeSet();
        UnicodeSet lmInSymbols = new UnicodeSet();
        UnicodeSet nInSymbols = new UnicodeSet();
        boolean aboveA = false;
        int count = 0;

        UcaBucket currentOrder = UcaBucket.ignorable;
        UcaBucket nextOrder = currentOrder.next();
        int currentOkCount = 0;

        for (Pair ceAndString : sorted) {
            final CEList ce = (CEList) ceAndString.first;
            final String string = (String)ceAndString.second;

            if (mustSkip.containsSome(string)) {
                illegalCharacters.addAll(string);
            }
            //            covered.addAll(string);
            //            if (ce.isPrimaryIgnorable()) {
            //                ignorable.add(string);
            //                continue;
            //            }

            //            boolean okOverride = false;
            // only look at isNFKD
            //            if (nfkdCh == '(' || nfkdCh == '〔' || nfkdCh == ':') {
            //                okOverride = true;
            //            }

            // skip multi-codepoint cases for now
            //            if (nfkd.codePointCount(0, nfkd.length()) > 1) {
            //                continue;
            //            }

            int ch = string.codePointAt(0);
            if (ch == nextOrder.least) {
                currentOrder = nextOrder;
                nextOrder = currentOrder.next();
            }
            currentOrder.add(ch);


            //            if (ignore.contains(ch)) {
            //                continue;
            //            }
            //            if (aboveA && ignoreAboveA.contains(ch)) {
            //                continue;
            //            }

            //            UcaBucket actualOrder = expectedOrder.get(ch);
            //            UcaBucket nfkdOrder = expectedOrder.get(nfkdCh);
            //            if (nfkdOrder == UcaBucket.numbers) {
            //                actualOrder = nfkdOrder;
            //            }
            //            if (actualOrder == null) {
            //                actualOrder = UcaBucket.scripts;
            //            }
            //            
            //            // add the actual value
            //            
            //            if (actualOrder.equals(currentOrder)) {
            //                actualOrder.add(ch);
            //                continue;
            //            }
            //            
            //            if (okOverride || actualOrder.equals(currentOrder)) {
            //                currentOkCount++;
            //                if (actualOrder == UcaBucket.general_symbols && lm.containsAll(string)) {
            //                    lmInSymbols.add(string);
            //                }
            //            } else if (currentOrder == UcaBucket.general_symbols && actualOrder==UcaBucket.numbers) {
            //                currentOkCount++;
            //                nInSymbols.add(string);
            //            } else {
            //                // above 'a' are just warnings.
            //                if (currentOrder == UcaBucket.scripts) {
            //                    funnyAboveA.add(string);
            //                    continue;
            //                }
            //
            //                errorCount++;
            //                String messageAttr = " class='bad'";
            //                String message = "(" + errorCount + ") expected " + currentOrder;
            //
            //                boolean newError = !lastShown.equals(lastString);
            //                // out of order, message
            //                if (newError) {
            //                    showTypeLine("", "OK Count: " + currentOkCount, lastOrder, lastString, lastCe);
            //                }
            //                showTypeLine(messageAttr, message, actualOrder, string, ce);
            //                lastShown = string;
            //                currentOkCount = 0;
            //            }
            //            lastOrder = actualOrder;
            //            lastString = string;
            //            lastCe = ce;
        }
        //        log.println("</table>");
        UnicodeSet missing = new UnicodeSet(shouldHave).removeAll(covered);
        illegalCharacters.retainAll(mustSkip);
        ignorable.removeAll(IGNORABLE);

        for (UcaBucket eo : UcaBucket.values()) {
            showCharSummary(eo + ": Ok char & 1st NFKD", eo.bothOk);
            showCharSummary(eo + ": Ok char, not 1st NFKD", eo.charOk);
            showCharSummary(eo + ": Ok 1st NFKD, not char", eo.nfkdOk);
            for (String value : eo.exceptionReasons.getAvailableValues()) {
                UnicodeSet filter = new UnicodeSet(eo.warn).retainAll(eo.exceptionReasons.getSet(value));
                showCharDetails(eo + ": WARNING - exceptions allowed «" + value + "»", filter, true);
            }
            errorCount += showCharDetails(eo + ": ERROR - unexpected characters", eo.failure, false);
        }
        log.println("<p>TODO: show missing</p>");

        //        showBadChars("N in general_symbols: not error, but check", nInSymbols);
        //        showBadChars("Lm in general_symbols: not error, but check", lmInSymbols);
        //        showBadChars("Unusual GC in ignorable: not error, but check", ignorable);
        //        showBadChars("Unusual GC above 'a': not error, but check", funnyAboveA);
        //        errorCount += showBadChars("Illegal character", illegalCharacters);
        //        errorCount += showBadChars("Missing CEs", missing);

        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.println("<br>");
        log.flush();
    }

    private static void showCharSummary(String title, UnicodeSet chars) {
        if (chars.size() == 0) return;
        log.println("<h3>" + title + ", Count: " + chars.size() + "</h3>");
        showCharSummaryTable(chars);
    }

    private static void showCharSummaryTable(UnicodeSet chars) {
        log.println("<table border='1'>");
        UnicodeMap<String> counter = new UnicodeMap<String>();
        for (String value : (List<String>) gc.getAvailableValues()) {
            List<String> aliases = (List<String>) gc.getValueAliases(value);
            String shortValue = aliases.get(0);
            UnicodeSet current = new UnicodeSet(chars).retainAll(gc.getSet(value));
            counter.putAll(current, shortValue);
        }
        TreeSet<String> sorted = new TreeSet<String>();
        sorted.addAll(counter.getAvailableValues());
        for (String value : sorted) {
            UnicodeSet set = counter.getSet(value);
            String pattern = set.toPattern(false).replace("\u0000", "\\u0000");
            int maxLength = 120;
            if (pattern.length() > maxLength) {
                // Do not truncate in the middle of a surrogate pair.
                if (Character.isHighSurrogate(pattern.charAt(maxLength - 1))) {
                    --maxLength;
                }
                pattern = pattern.substring(0, maxLength) + "…";
            }
            log.println("<tr>"
                    + "<td>" + value + "</td>"
                    + "<td>" + set.size() +  "</td>"
                    + "<td>" + pattern +  "</td>"
                    + "</tr>");
        }
        log.println("</table>");
    }

    private static int showCharDetails(String title, UnicodeSet illegalCharacters, boolean notError) {
        if (illegalCharacters.size() == 0) {
            return 0;
        }
        Map<String, UnicodeSet> sorted = new TreeMap();

        for (String s: illegalCharacters) {
            final String type = getType(s);
            UnicodeSet us = sorted.get(type);
            if (us == null) {
                sorted.put(type, us = new UnicodeSet());
            }
            us.add(s);
        }
        String rtitle = "<span class='" + (notError ? "warn" : "bad") + "'>" + title + "<span>";
        System.out.println("***" + rtitle);
        log.println("<h3>" + rtitle + "; Count: " + illegalCharacters.size() + "</h3>");
        showCharSummaryTable(illegalCharacters);
        log.println("<p>Details:</p>");
        log.println("<table border='1'>");
        for (Entry<String, UnicodeSet> typeAndString : sorted.entrySet()) {
            UnicodeSet us = typeAndString.getValue();
            for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.nextRange();) {
                if (it.codepointEnd != it.codepoint) {
                    log.println("<tr>"
                            + "<td>" + typeAndString.getKey() + "</td>"
                            + "<td>" + UTF16.valueOf(it.codepoint) 
                            + "…" + UTF16.valueOf(it.codepointEnd) +  "</td>"
                            + "<td>" + Utility.hex(it.codepoint) 
                            + "…" + Utility.hex(it.codepointEnd) +  "</td>" 
                            + "<td>" + Default.ucd().getName(it.codepoint) 
                            + "…" + Default.ucd().getName(it.codepointEnd) + "</td>"
                            + "</tr>");
                } else {
                    log.println("<tr>"
                            + "<td>" + typeAndString.getKey() + "</td>"
                            + "<td>" + UTF16.valueOf(it.codepoint) + "</td>"
                            + "<td>" + Utility.hex(it.codepoint) + "</td>" 
                            + "<td>" + Default.ucd().getName(it.codepoint) + "</td>"
                            + "</tr>");
                }
            }
        }
        log.println("</table>");
        return illegalCharacters.size();
    }

    private static String getType(String s) {
        Set<String> set = getTypes(s, new LinkedHashSet());

        String nfkd = Default.nfkd().normalize(s);
        Set<String> set2 = getTypes(nfkd, new LinkedHashSet());
        set2.removeAll(set);

        String result = CollectionUtilities.join(set, "/");
        return set2.size() == 0 ? result : result + "(" + CollectionUtilities.join(set2, "/") + ")";
    }

    private static Set<String> getTypes(String s, Set<String> set) {
        for (int cp : With.codePointArray(s)) {
            set.add(Default.ucd().getCategoryID(cp));
        }
        for (int cp : With.codePointArray(s)) {
            set.add(Default.ucd().getScriptID(cp, SHORT));
        }
        return set;
    }

    private static void showMismatches() {
        log.println("<h2>1. Mismatches when NFD is OFF</h2>");
        log.println("<p>Alternate Handling = " + alternateName[option] + "</p>");
        log.println("<p>NOTE: NFD form is used by UCA,"
                + "so if other forms are different there are <i>ignored</i>. This may indicate a problem, e.g. missing contraction.</p>");
        log.println("<table border='1'>");
        log.println("<tr><th>Name</th><th>Type</th><th>Unicode</th><th>Key</th></tr>");
        Iterator<String> it = MismatchedC.keySet().iterator();
        int errorCount = 0;
        while (it.hasNext()) {
            String ch = it.next();
            String MN = MismatchedN.get(ch);
            String MC = MismatchedC.get(ch);
            String MD = MismatchedD.get(ch);
            String chInC = Default.nfc().normalize(ch);
            String chInD = Default.nfd().normalize(ch);

            log.println("<tr><td rowSpan='3' class='bottom'>" + Utility.replace(Default.ucd().getName(ch), ", ", ",<br>")
                    + "</td><td>NFD</td><td>" + Utility.hex(chInD)
                    + "</td><td>" + printableKey(MD) + "</td></tr>");

            log.println("<tr><td>NFC</td><td>" + Utility.hex(chInC)
                    + "</td><td>" + printableKey(MC) + "</td></tr>");

            log.println("<tr><td class='bottom'>Plain</td><td class='bottom'>" + Utility.hex(ch)
                    + "</td><td class='bottom'>" + printableKey(MN) + "</td></tr>");

            errorCount++;
        }
        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.println("<br>");
        log.flush();
    }

    private static String printableKey(Object keyobj) {
        String sortKey;
        if (keyobj == null) {
            sortKey = "NULL!!";
        } else {
            sortKey = keyobj.toString();
            sortKey = sortKey.substring(0, sortKey.length() - 1);
            sortKey = UCA.toString(sortKey);
        }
        return sortKey;
    }

    private static final boolean needsXMLQuote(String source, boolean quoteApos) {
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch < ' ' || ch == '<' || ch == '&' || ch == '>') {
                return true;
            }
            if (quoteApos & ch == '\'') {
                return true;
            }
            if (ch == '\"') {
                return true;
            }
            if (ch >= '\uD800' && ch <= '\uDFFF') {
                return true;
            }
            if (ch >= '\uFFFE') {
                return true;
            }
        }
        return false;
    }

    // TODO: Unused, remove?
    public static final String XMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, true);
    }

    // TODO: Unused, remove?
    public static final String XMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, true);
    }

    // TODO: Unused, remove?
    public static final String XMLString(String source) {
        return XMLBaseString(source, true);
    }

    // TODO: Unused, remove?
    public static final String HTMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, false);
    }

    // TODO: Unused, remove?
    public static final String HTMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, false);
    }

    // TODO: Unused, remove?
    public static final String HTMLString(String source) {
        return XMLBaseString(source, false);
    }

    // TODO: Unused, remove?
    public static final String XMLBaseString(int[] cps, int len, boolean quoteApos) {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            temp.append((char) cps[i]);
        }
        return XMLBaseString(temp.toString(), quoteApos);
    }

    // TODO: Unused, remove?
    public static final String XMLBaseString(String source, boolean quoteApos) {
        if (!needsXMLQuote(source, quoteApos)) {
            return source;
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch < ' '
                    || ch >= '\u007F' && ch <= '\u009F'
                    || ch >= '\uD800' && ch <= '\uDFFF'
                    || ch >= '\uFFFE') {
                result.append('\uFFFD');
                /*
                 * result.append("#x"); result.append(cpName(ch));
                 * result.append(";");
                 */
            } else if (quoteApos && ch == '\'') {
                result.append("&apos;");
            } else if (ch == '\"') {
                result.append("&quot;");
            } else if (ch == '<') {
                result.append("&lt;");
            } else if (ch == '&') {
                result.append("&amp;");
            } else if (ch == '>') {
                result.append("&gt;");
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    static UCA getCollator(CollatorType type) {
        switch(type) {
        case cldr:
            if (cldrCollator == null) {
                //                if (Default.ucdVersion().compareTo("6.1") < 0) { // only reorder if less than v6.1
                //                    cldrCollator = buildCldrCollator(true);
                //                } else 
                {
                    cldrCollator = buildCldrCollator(false);

                    cldrCollator.overrideCE("\uFFFE", 0x1, 0x20, 0x5);
                    cldrCollator.overrideCE("\uFFFF", 0xFFFE, 0x20, 0x5);
                }
            }
            return cldrCollator;                
        case ducet:
            if (ducetCollator == null) {
                ducetCollator = UCA.buildCollator(null);
            }
            return ducetCollator;
        case cldrWithoutFFFx:
            if (cldrWithoutFFFxCollator == null) {
                cldrWithoutFFFxCollator = buildCldrCollator(false);
            }
            return cldrWithoutFFFxCollator;
        default:
            throw new IllegalArgumentException();
        }
    }

    private static UCA buildCldrCollator(boolean addFFFx) {
        PrintWriter fractionalLog = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", "FractionalRemap.txt", Utility.UTF8_WINDOWS);
        // hack to reorder elements
        UCA oldCollator = getCollator(CollatorType.ducet);
        CEList ceListForA = oldCollator.getCEList("a", true);
        int firstForA = ceListForA.at(0);
        int firstScriptPrimary = CEList.getPrimary(firstForA);
        Remap primaryRemap = new Remap();
        RoBitSet primarySet = oldCollator.getStatistics().getPrimarySet();
        // gather the data
        UnicodeSet spaces = new UnicodeSet();
        UnicodeSet punctuation = new UnicodeSet();
        UnicodeSet generalSymbols = new UnicodeSet();
        UnicodeSet currencySymbols = new UnicodeSet();
        UnicodeSet numbers = new UnicodeSet();

        int oldVariableHigh = CEList.getPrimary(oldCollator.getVariableHighCE());
        int firstDucetNonVariable = -1;

        for (int i = primarySet.nextSetBit(0); i >= 0; i = primarySet.nextSetBit(i+1)) {
            if (i == 0) continue; // skip ignorables
            if (i == UCA.TEST_PRIMARY) {
                i = i; // for debugging
            }
            if (firstDucetNonVariable < 0 && i > oldVariableHigh) {
                firstDucetNonVariable = i;
            }

            CharSequence repChar = oldCollator.getRepresentativePrimary(i);
            CharSequence rep2 = filter(repChar);
            if (rep2 == null) {
                rep2 = repChar;
                fractionalLog.println("# Warning - No NFKD primary with:\t" + Utility.hex(i) 
                        + "\t" + repChar
                        + "\t" + Default.ucd().getCodeAndName(repChar.toString()));
                //continue;
            }
            rep2 = repChar;
            int firstChar = Character.codePointAt(rep2, 0);
            int cat = Default.ucd().getCategory(firstChar);
            switch (cat) {
            case SPACE_SEPARATOR: case LINE_SEPARATOR: case PARAGRAPH_SEPARATOR: case CONTROL:
                spaces.add(i);
                break;
            case DASH_PUNCTUATION: case START_PUNCTUATION: case END_PUNCTUATION: case CONNECTOR_PUNCTUATION: 
            case OTHER_PUNCTUATION: case INITIAL_PUNCTUATION: case FINAL_PUNCTUATION:
                punctuation.add(i);
                break;
            case DECIMAL_DIGIT_NUMBER:
                numbers.add(i);
                break;
            case LETTER_NUMBER: case OTHER_NUMBER:
                if (i >= firstScriptPrimary) break;
                numbers.add(i);
                break;
            case CURRENCY_SYMBOL:
                currencySymbols.add(i);
                break;
            case MATH_SYMBOL: case MODIFIER_SYMBOL: case OTHER_SYMBOL:
                generalSymbols.add(i);
                break;
            case UNASSIGNED: case UPPERCASE_LETTER: case LOWERCASE_LETTER: case TITLECASE_LETTER: case MODIFIER_LETTER:
            case OTHER_LETTER: case NON_SPACING_MARK: case ENCLOSING_MARK: case COMBINING_SPACING_MARK:
            case FORMAT:
                if (i >= firstScriptPrimary) break;
                generalSymbols.add(i);
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
        // now reorder
        primaryRemap
        .addItems(spaces)
        .addItems(punctuation)
        .setVariableHigh()
        .addItems(generalSymbols)
        .addItems(currencySymbols)
        .putRemappedCharacters(0x20A8) // U+20A8 RUPEE SIGN
        .putRemappedCharacters(0xFDFC) // U+FDFC RIAL SIGN
        .addItems(numbers);

        primaryRemap.setFirstDucetNonVariable(firstDucetNonVariable);

        LinkedHashSet<String> s = new LinkedHashSet<String>();

        fractionalLog.println("# Remapped primaries");

        for (int i = primarySet.nextSetBit(0); i >= 0; i = primarySet.nextSetBit(i+1)) {
            if (i == UCA.TEST_PRIMARY) {
                i = i; // for debugging
            }
            CharSequence repChar = oldCollator.getRepresentativePrimary(i);
            CharSequence rep2 = repChar;
            //                        filter(repChar);
            //                    if (rep2 == null) {
            //                        rep2 = repChar;
            //                    }
            String gcInfo = FractionalUCA.getStringTransform(rep2, "/", FractionalUCA.GeneralCategoryTransform, s);
            // FractionalUCA.GeneralCategoryTransform.transform(repChar);
            //String scriptInfo = FractionalUCA.ScriptTransform.transform(repChar);

            Integer remap = primaryRemap.getRemappedPrimary(i);
            if (remap == null) {
                if (!SHOW_NON_MAPPED) continue;
            }
            int remap2 = remap == null ? i : remap;
            fractionalLog.println(
                    (remap == null ? "#" : "")
                    + "\t" + i 
                    + "\t" + remap2
                    + "\tx" + Utility.hex(i) 
                    + "\tx" + Utility.hex(remap2)
                    + "\t" + gcInfo
                    + "\t" + excelQuote(rep2)
                    + "\t" + Default.ucd().getCodeAndName(Character.codePointAt(rep2, 0))
                    );
        }
        Map<Integer, IntStack> characterRemap = primaryRemap.getCharacterRemap();
        fractionalLog.println("# Remapped characters");

        for (Entry<Integer, IntStack> x : characterRemap.entrySet()) {
            Integer character = x.getKey();
            fractionalLog.println("#" + Utility.hex(character) 
                    + "\t" + x.getValue() 
                    + "\t" + Default.ucd().getCodeAndName(character));
        }
        fractionalLog.close();

        UCA result = UCA.buildCollator(primaryRemap);

        if (addFFFx) {
            result.overrideCE("\uFFFE", 0x1, 0x20, 0x5);
            result.overrideCE("\uFFFF", 0xFFFE, 0x20, 0x5);
        }
        
        if (ADD_TIBETAN) {
            CEList fb2 = result.getCEList("\u0FB2", true);
            CEList fb3 = result.getCEList("\u0FB3", true);
            CEList f71_f72 = result.getCEList("\u0F71\u0F72", true);
            CEList f71_f74 = result.getCEList("\u0F71\u0F74", true);
            CEList fb2_f71 = result.getCEList("\u0FB2\u0F71", true);
            CEList fb3_f71 = result.getCEList("\u0FB3\u0F71", true);

            addOverride(result, "\u0FB2\u0F71", fb2_f71);               //0FB2 0F71      ;     [.255A.0020.0002.0FB2][.2570.0020.0002.0F71] - concat 0FB2 + 0F71
            addOverride(result, "\u0FB2\u0F71\u0F72", fb2, f71_f72);    //0FB2 0F71 0F72 ;    [.255A.0020.0002.0FB2][.2572.0020.0002.0F73] - concat 0FB2 + (0F71/0F72)
            addOverride(result, "\u0FB2\u0F73", fb2, f71_f72);          //0FB2 0F73      ;        [.255A.0020.0002.0FB2][.2572.0020.0002.0F73] = prev
            addOverride(result, "\u0FB2\u0F71\u0F74", fb2, f71_f74);    //0FB2 0F71 0F74 ;    [.255A.0020.0002.0FB2][.2576.0020.0002.0F75] - concat 0FB2 + (0F71/0F74)
            addOverride(result, "\u0FB2\u0F75", fb2, f71_f74);          //0FB2 0F75      ;        [.255A.0020.0002.0FB2][.2576.0020.0002.0F75]  = prev

            // same as above, but 0FB2 => 0FB3 and fb2 => fb3
            
            addOverride(result, "\u0FB3\u0F71", fb3_f71);               //0FB3 0F71      ;     [.255A.0020.0002.0FB3][.2570.0020.0002.0F71] - concat 0FB3 + 0F71
            addOverride(result, "\u0FB3\u0F71\u0F72", fb3, f71_f72);    //0FB3 0F71 0F72 ;    [.255A.0020.0002.0FB3][.2572.0020.0002.0F73] - concat 0FB3 + (0F71/0F72)
            addOverride(result, "\u0FB3\u0F73", fb3, f71_f72);          //0FB3 0F73      ;        [.255A.0020.0002.0FB3][.2572.0020.0002.0F73] = prev
            addOverride(result, "\u0FB3\u0F71\u0F74", fb3, f71_f74);    //0FB3 0F71 0F74 ;    [.255A.0020.0002.0FB3][.2576.0020.0002.0F75] - concat 0FB3 + (0F71/0F74)
            addOverride(result, "\u0FB3\u0F75", fb3, f71_f74);          //0FB3 0F75      ;        [.255A.0020.0002.0FB3][.2576.0020.0002.0F75]  = prev
        }

        // verify results
        int[] output = new int[30];
        StringBuilder failures = new StringBuilder();
        for (int i = 0; i <= 0x10FFFF; ++i) {            
            if (!result.codePointHasExplicitMappings(i)) {
                continue;
            }
            if (!Default.ucd().isAllocated(i)) continue;
            int ceCount = result.getCEs(UTF16.valueOf(i), true, output);
            int primary = ceCount < 1 ? 0 : CEList.getPrimary(output[0]);
            int cat = Default.ucd().getCategory(i);

            switch (cat) {
            case SPACE_SEPARATOR: case LINE_SEPARATOR: case PARAGRAPH_SEPARATOR: case CONTROL:
            case DASH_PUNCTUATION: case START_PUNCTUATION: case END_PUNCTUATION: case CONNECTOR_PUNCTUATION: 
            case OTHER_PUNCTUATION: case INITIAL_PUNCTUATION: case FINAL_PUNCTUATION:
            case DECIMAL_DIGIT_NUMBER: // case LETTER_NUMBER: case OTHER_NUMBER:
            case CURRENCY_SYMBOL:
            case MATH_SYMBOL: case MODIFIER_SYMBOL: 
                //case OTHER_SYMBOL:
                if (primary > firstScriptPrimary) {
                    failures.append("\t" + Utility.hex(primary)
                            + "\t" + Default.ucd().getCategoryID(i)
                            + "\t" + Default.ucd().getCodeAndName(i)
                            + "\n"
                            );
                }
                break;
            default:
                // no action
            }
        }
        if (failures.length() > 0) {
            throw new IllegalArgumentException("Failures:\n" + failures);
        }
        return result;
    }

    private static void addOverride(UCA result, String string, CEList... ceLists) {
        IntStack tempStack = new IntStack(10);
        for (CEList ceList : ceLists) {
            for (int i = 0; i < ceList.length(); ++i) {
                int ce = ceList.at(i);
                tempStack.append(ce);
            }
        }
        result.overrideCE(string, tempStack);
    }

    private static CharSequence filter(CharSequence repChar) {
        if (Default.nfkd().isNormalized(repChar.toString())) {
            return repChar;
        }
        StringBuilder result = new StringBuilder();
        int cp;
        for (int i = 0; i < repChar.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(repChar, i);
            if (Default.nfkd().isNormalized(cp)) {
                result.appendCodePoint(cp);
            }
        }
        if (result.length() == 0) {
            return null;
        }
        return result.toString();
    }

    private static Pattern EXCEL_QUOTE = Pattern.compile("[\"\\p{Cntrl}\u0085\u2029\u2028]");

    private static String excelQuote(CharSequence input) {
        return EXCEL_QUOTE.matcher(input).replaceAll("\uFFFD");
    }
}
