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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.UCA.Remap;
import org.unicode.text.UCA.UCA.UCAContents;
import org.unicode.text.UCA.UCA_Statistics.RoBitSet;
import org.unicode.text.UCA.WriteCollationData.UcaBucket;
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

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.impl.Differ;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class WriteCollationData implements UCD_Types, UCA_Types {

    private static final boolean SHOW_NON_MAPPED = false;

    // may require fixing

    static final boolean       DEBUG                    = false;
    static final boolean       DEBUG_SHOW_ITERATION     = true;

    public static final String copyright                =
            "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    static final boolean       EXCLUDE_UNSUPPORTED      = true;
    static final boolean       GENERATED_NFC_MISMATCHES = true;
    static final boolean       DO_CHARTS                = false;

    static final String        UNICODE_VERSION          = UCD.latestVersion;

    private static UCA                 ducetCollator;
    private static UCA                 cldrCollator;
    private static UCA                 cldrWithoutFFFxCollator;

    static char                unique                   = '\u0000';
    static TreeMap             sortedD                  = new TreeMap();
    static TreeMap             sortedN                  = new TreeMap();
    static HashMap             backD                    = new HashMap();
    static HashMap             backN                    = new HashMap();
    static TreeMap             duplicates               = new TreeMap();
    static int                 duplicateCount           = 0;
    static PrintWriter         log;

    static void javatest() throws Exception {
        checkJavaRules("& J , K / B & K , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
        checkJavaRules("& J , K / B , M", new String[] { "JA", "MA", "KA", "KC", "JC", "MC" });
    }

    static void checkJavaRules(String rules, String[] tests) throws Exception {
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

    static String showJavaCollationKey(RuleBasedCollator col, String test) {
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

    // static final String DIR =
    // "c:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.0.1/";
    // static final String DIR31 =
    // "c:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.1/";

    static void writeCaseExceptions() {
        System.err.println("Writing Case Exceptions");
        // Normalizer NFKC = new Normalizer(Normalizer.NFKC, UNICODE_VERSION);
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

    static void writeCaseFolding() throws IOException {
        System.err.println("Writing Javascript data");
        BufferedReader in = Utility.openUnicodeFile("CaseFolding", UNICODE_VERSION, true, Utility.LATIN1);
        // new BufferedReader(new FileReader(DIR31 +
        // "CaseFolding-3.d3.alpha.txt"), 64*1024);
        // log = new PrintWriter(new FileOutputStream("CaseFolding_data.js"));
        log = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), "CaseFolding_data.js", Utility.UTF8_WINDOWS);
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

    static String replace(String source, char toBeReplaced, String toReplace) {
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

    static void writeJavascriptInfo() throws IOException {
        System.err.println("Writing Javascript data");
        // Normalizer normKD = new Normalizer(Normalizer.NFKD, UNICODE_VERSION);
        // Normalizer normD = new Normalizer(Normalizer.NFD, UNICODE_VERSION);
        // log = new PrintWriter(new FileOutputStream("Normalization_data.js"));
        log = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), "Normalization_data.js", Utility.LATIN1_WINDOWS);

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

    static void addStringX(int x, byte option, CollatorType collatorType, AppendToCe appendToCe) {
        addStringX(UTF32.valueOf32(x), option, collatorType, appendToCe);
    }

    static final char     LOW_ACCENT                   = '\u0334';
    static final String   SUPPLEMENTARY_ACCENT         = UTF16.valueOf(0x1D165);
    static final String   COMPLETELY_IGNOREABLE        = "\u0001";
    static final String   COMPLETELY_IGNOREABLE_ACCENT = "\u0591";
    static final String[] CONTRACTION_TEST             = { SUPPLEMENTARY_ACCENT, COMPLETELY_IGNOREABLE, COMPLETELY_IGNOREABLE_ACCENT };

    static int            addCounter                   = 0;

    static void addStringX(String s, byte option, CollatorType collatorType, AppendToCe appendToCe) {
        int firstChar = UTF16.charAt(s, 0);
        addStringY(s + 'a', option, collatorType, appendToCe);
        addStringY(s + 'b', option, collatorType, appendToCe);
        addStringY(s + '?', option, collatorType, appendToCe);
        addStringY(s + 'A', option, collatorType, appendToCe);
        addStringY(s + '!', option, collatorType, appendToCe);
        if (option == SHIFTED && getCollator(collatorType).isVariable(firstChar)) {
            addStringY(s + LOW_ACCENT, option, collatorType, appendToCe);
        }

        // NOW, if the character decomposes, or is a combining mark (non-zero),
        // try combinations

        if (Default.ucd().getCombiningClass(firstChar) > 0
                || !Default.nfd().isNormalized(s) && !Default.ucd().isHangulSyllable(firstChar)) {
            // if it ends with a non-starter, try the decompositions.
            String decomp = Default.nfd().normalize(s);
            if (Default.ucd().getCombiningClass(UTF16.charAt(decomp, decomp.length() - 1)) > 0) {
                if (canIt == null) {
                    canIt = new CanonicalIterator(".");
                }
                canIt.setSource(s + LOW_ACCENT);
                int limit = 4;
                for (String can = canIt.next(); can != null; can = canIt.next()) {
                    if (s.equals(can)) {
                        continue;
                    }
                    if (--limit < 0) {
                        continue; // just include a sampling
                    }
                    addStringY(can, option, collatorType, appendToCe);
                    // System.out.println(addCounter++ + " Adding " +
                    // Default.ucd.getCodeAndName(can));
                }
            }
        }
        if (UTF16.countCodePoint(s) > 1) {
            for (int i = 1; i < s.length(); ++i) {
                if (UTF16.isLeadSurrogate(s.charAt(i - 1))) {
                    continue; // skip if in middle of supplementary
                }

                for (int j = 0; j < CONTRACTION_TEST.length; ++j) {
                    String extra = s.substring(0, i) + CONTRACTION_TEST[j] + s.substring(i);
                    addStringY(extra + 'a', option, collatorType, appendToCe);
                    if (DEBUG) {
                        System.out.println(addCounter++ + " Adding " + Default.ucd().getCodeAndName(extra));
                    }
                }
            }
        }
    }

    static CanonicalIterator canIt = null;

    static char              counter;

    static void addStringY(String s, byte option, CollatorType collatorType, AppendToCe appendToCe) {
        if (DEBUG && s.contains("\uA6F0")) {
            System.out.println("Test BAMUM COMBINING MARK");
        }
        //String cpo = UCA.codePointOrder(s);
        String colDbase = getCollator(collatorType).getSortKey(s, option, true, appendToCe);
        sortedD.put(colDbase, s);
    }

    static UCD ucd_uca_base = null;

    /**
     * Check that the primaries are the same as the compatibility decomposition.
     */
    static void checkBadDecomps(int strength, boolean decomposition, UnicodeSet alreadySeen) {
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
                    System.out.println("  Old:" + getCollator(CollatorType.ducet).toString(decompSortKey));
                    System.out.println("  New: " + getCollator(CollatorType.ducet).toString(newSortKey));
                    System.out.println("  Tgt: " + getCollator(CollatorType.ducet).toString(sortKey));
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

    static String remapSortKey(int cp, boolean decomposition) {
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

    static String remapCanSortKey(int ch, boolean decomposition) {
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
    static String mergeSortKeys(String key1, String key2) {
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

    static final String remove(String s, char ch) {
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

    /*
     * log = new PrintWriter(new FileOutputStream("Frequencies.html"));
     * log.println("<html><body>"); MessageFormat mf = newMessageFormat(
     * "<tr><td><tt>{0}</tt></td><td><tt>{1}</tt></td><td align='right'><tt>{2}</tt></td><td align='right'><tt>{3}</tt></td></tr>"
     * ); MessageFormat mf2 = newMessageFormat(
     * "<tr><td><tt>{0}</tt></td><td align='right'><tt>{1}</tt></td></tr>");
     * String header = mf.format(new String[] {"Start", "End", "Count",
     * "Subtotal"}); int count;
     * 
     * log.println("<h2>Writing Used Weights</h2>");
     * log.println("<p>Primaries</p><table border='1'>" + mf.format(new String[]
     * {"Start", "End", "Count", "Subtotal"})); count =
     * collator.writeUsedWeights(log, 1, mf);
     * log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>",
     * new Object[] {new Integer(count)})); log.println("</table>");
     * 
     * log.println("<p>Secondaries</p><table border='1'>" + mf2.format(new
     * String[] {"Code", "Frequency"})); count = collator.writeUsedWeights(log,
     * 2, mf2);
     * log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>",
     * new Object[] {new Integer(count)})); log.println("</table>");
     * 
     * log.println("<p>Tertiaries</p><table border='1'>" + mf2.format(new
     * String[] {"Code", "Frequency"})); count = collator.writeUsedWeights(log,
     * 3, mf2);
     * log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>",
     * new Object[] {new Integer(count)})); log.println("</table>");
     * log.println("</body></html>"); log.close();
     */

    static final byte getDecompType(int cp) {
        byte result = Default.ucd().getDecompositionType(cp);
        if (result == Default.ucd().CANONICAL) {
            String d = Default.nfd().normalize(cp); // TODO
            int cp1;
            for (int i = 0; i < d.length(); i += UTF16.getCharCount(cp1)) {
                cp1 = UTF16.charAt(d, i);
                byte t = Default.ucd().getDecompositionType(cp1);
                if (t > Default.ucd().CANONICAL) {
                    return t;
                }
            }
        }
        return result;
    }

    static final boolean multipleZeroPrimaries(int[] a, int aLen) {
        int count = 0;
        for (int i = 0; i < aLen; ++i) {
            if (UCA.getPrimary(a[i]) == 0) {
                if (count == 1) {
                    return true;
                }
                count++;
            } else {
                count = 0;
            }
        }
        return false;
    }

    static int kenCompress(int[] markCes, int markLen) {
        if (markLen == 0) {
            return 0;
        }
        int out = 1;
        for (int i = 1; i < markLen; ++i) {
            int next = markCes[i];
            int prev = markCes[out - 1];
            if (UCA.getPrimary(next) == 0
                    && UCA.getSecondary(prev) == 0x20
                    && UCA.getTertiary(next) == 0x2) {
                markCes[out - 1] = UCA.makeKey(
                        UCA.getPrimary(prev),
                        UCA.getSecondary(next),
                        UCA.getTertiary(prev));
                FractionalUCA.Variables.compressSet.set(UCA.getSecondary(next));
            } else {
                markCes[out++] = next;
            }
        }
        return out;
    }

    static boolean arraysMatch(int[] a, int aLen, int[] b, int bLen) {
        if (aLen != bLen) {
            return false;
        }
        for (int i = 0; i < aLen; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    static int[] markCes = new int[50];

    static int getStrengthDiff(CEList celist) {
        int result = QUARTERNARY_DIFF;
        for (int j = 0; j < celist.length(); ++j) {
            int ce = celist.at(j);
            if (getCollator(CollatorType.ducet).getPrimary(ce) != 0) {
                return PRIMARY_DIFF;
            } else if (getCollator(CollatorType.ducet).getSecondary(ce) != 0) {
                result = SECONDARY_DIFF;
            } else if (getCollator(CollatorType.ducet).getTertiary(ce) != 0) {
                result = TERTIARY_DIFF;
            }
        }
        return result;
    }

    static String[] strengthName = { "XYZ", "0YZ", "00Z", "000" };

    static void writeCategoryCheck() throws IOException {
        /*
         * PrintWriter diLog = new PrintWriter( new BufferedWriter( new
         * OutputStreamWriter( new FileOutputStream(UCA_GEN_DIR +
         * "UCA_Nonspacing.txt"), "UTF8"), 32*1024));
         */
        log.println("<h2>8. Checking against categories</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        Set sorted = new TreeSet();

        for (int i = 0; i < 0x10FFFF; ++i) {
            Utility.dot(i);
            if (!Default.ucd().isRepresented(i)) {
                continue;
            }
            CEList celist = getCollator(CollatorType.ducet).getCEList(UTF32.valueOf32(i), true);
            int real = getStrengthDiff(celist);

            int desired = PRIMARY_DIFF;
            byte cat = Default.ucd().getCategory(i);
            if (cat == Cc || cat == Cs || cat == Cf || Default.ucd().isNoncharacter(i)) {
                desired = QUARTERNARY_DIFF;
            } else if (cat == Mn || cat == Me) {
                desired = SECONDARY_DIFF;
            }

            String listName = celist.toString();
            if (listName.length() == 0) {
                listName = "<i>ignore</i>";
            }
            if (real != desired) {
                sorted.add("<tr><td>" + strengthName[real]
                        + "</td><td>" + strengthName[desired]
                                + "</td><td>" + Default.ucd().getCategoryID(i)
                                + "</td><td>" + listName
                                + "</td><td>" + Default.ucd().getCodeAndName(i)
                                + "</td></tr>");
            }
        }

        Utility.print(log, sorted, "\r\n");
        log.println("</table>");
        log.flush();
    }

    static void writeDuplicates() {
        log.println("<h2>9. Checking characters that are not canonical equivalents, but have same CE</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());

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

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            CEList celist = (CEList) it.next();
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

    static void writeOverlap() {
        log.println("<h2>10. Checking overlaps</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());

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

    static void reverse(List ell) {
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

    static final boolean DEBUG_SHOW_OVERLAP = false;

    static boolean overlaps(Map map, Map tails, CEList celist, int depth, List me, List other) {
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
    static <K, V> String getHTML_NameSet(Collection<K> set, Map<K,V> m, boolean useName) {
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

    static void writeContractions() throws IOException {
        /*
         * PrintWriter diLog = new PrintWriter( new BufferedWriter( new
         * OutputStreamWriter( new FileOutputStream(UCA_GEN_DIR +
         * "UCA_Contractions.txt"), "UTF8"), 32*1024));
         */
        String fullFileName = "UCA_Contractions.txt";
        PrintWriter diLog = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), fullFileName, Utility.UTF8_WINDOWS);

        diLog.write('\uFEFF');

        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        int[] ces = new int[50];

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());
        int[] lenArray = new int[1];

        diLog.println("# Contractions");
        writeVersionAndDate(diLog, fullFileName, true);
        // diLog.println("# Generated " + getNormalDate());
        // diLog.println("# UCA Version: " + collator.getDataVersion() + "/" +
        // collator.getUCDVersion());
        while (true) {
            String s = cc.next(ces, lenArray);
            if (s == null) {
                break;
            }
            int len = lenArray[0];

            if (s.length() > 1) {
                diLog.println(Utility.hex(s, " ")
                        + ";\t #" + CEList.toString(ces, len)
                        + " ( " + s + " )"
                        + " " + Default.ucd().getName(s));
            }
        }
        diLog.close();
    }

    static void checkDisjointIgnorables() throws IOException {
        /*
         * PrintWriter diLog = new PrintWriter( new BufferedWriter( new
         * OutputStreamWriter( new FileOutputStream(UCA_GEN_DIR +
         * "DisjointIgnorables.txt"), "UTF8"), 32*1024));
         */
        PrintWriter diLog = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), "DisjointIgnorables.js", Utility.UTF8_WINDOWS);

        diLog.write('\uFEFF');

        /*
         * PrintWriter diLog = new PrintWriter( // try new one new
         * UTF8StreamWriter(new FileOutputStream(UCA_GEN_DIR +
         * "DisjointIgnorables.txt"), 32*1024)); diLog.write('\uFEFF');
         */

        // diLog = new PrintWriter(new FileOutputStream(UCA_GEN_DIR +
        // "DisjointIgnorables.txt"));

        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        int[] ces = new int[50];
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
        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());
        int[] lenArray = new int[1];

        Set sortedCodes = new TreeSet();
        Set mixedCEs = new TreeSet();

        while (true) {
            String s = cc.next(ces, lenArray);
            if (s == null) {
                break;
            }

            // process all CEs. Look for controls, and for mixed
            // ignorable/non-ignorables

            int ccc;
            for (int kk = 0; kk < s.length(); kk += UTF32.count16(ccc)) {
                ccc = UTF32.char32At(s, kk);
                byte cat = Default.ucd().getCategory(ccc);
                if (cat == Cf || cat == Cc || cat == Zs || cat == Zl || cat == Zp) {
                    sortedCodes.add(CEList.toString(ces, lenArray[0]) + "\t" + Default.ucd().getCodeAndName(s));
                    break;
                }
            }

            int len = lenArray[0];

            int haveMixture = 0;
            for (int j = 0; j < len; ++j) {
                int ce = ces[j];
                int pri = getCollator(CollatorType.ducet).getPrimary(ce);
                int sec = getCollator(CollatorType.ducet).getSecondary(ce);
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
                    mixedCEs.add(CEList.toString(ces, len) + "\t" + Default.ucd().getCodeAndName(s));
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
                + Utility.hex(UCA.getPrimary(getCollator(CollatorType.ducet).getVariableLowCE())) + " to "
                + Utility.hex(UCA.getPrimary(getCollator(CollatorType.ducet).getVariableHighCE())));
        diLog.println();

        Iterator it;
        it = mixedCEs.iterator();
        while (it.hasNext()) {
            Object key = it.next();
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

    static void checkCE_overlap() throws IOException {
        /*
         * PrintWriter diLog = new PrintWriter( new BufferedWriter( new
         * OutputStreamWriter( new FileOutputStream(UCA_GEN_DIR +
         * "DisjointIgnorables.txt"), "UTF8"), 32*1024));
         */
        PrintWriter diLog = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), "DisjointIgnorables2.js", Utility.UTF8_WINDOWS);

        diLog.write('\uFEFF');

        // diLog = new PrintWriter(new FileOutputStream(UCA_GEN_DIR +
        // "DisjointIgnorables.txt"));

        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

        int[] ces = new int[50];
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
        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());
        int[] lenArray = new int[1];

        Set sortedCodes = new TreeSet();
        Set mixedCEs = new TreeSet();

        while (true) {
            String s = cc.next(ces, lenArray);
            if (s == null) {
                break;
            }

            // process all CEs. Look for controls, and for mixed
            // ignorable/non-ignorables

            int ccc;
            for (int kk = 0; kk < s.length(); kk += UTF32.count16(ccc)) {
                ccc = UTF32.char32At(s, kk);
                byte cat = Default.ucd().getCategory(ccc);
                if (cat == Cf || cat == Cc || cat == Zs || cat == Zl || cat == Zp) {
                    sortedCodes.add(CEList.toString(ces, lenArray[0]) + "\t" + Default.ucd().getCodeAndName(s));
                    break;
                }
            }

            int len = lenArray[0];

            int haveMixture = 0;
            for (int j = 0; j < len; ++j) {
                int ce = ces[j];
                int pri = getCollator(CollatorType.ducet).getPrimary(ce);
                int sec = getCollator(CollatorType.ducet).getSecondary(ce);
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
                    mixedCEs.add(CEList.toString(ces, len) + "\t" + Default.ucd().getCodeAndName(s));
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

        diLog.close();
    }

    static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, Vector v) {
        for (int i = 0; i < v.size(); ++i) {
            showSampleOverlap(diLog, doNew, head, (String) v.get(i));
        }
    }

    static void showSampleOverlap(PrintWriter diLog, boolean doNew, String head, String src) {
        int[] ces = new int[30];
        int len = getCollator(CollatorType.ducet).getCEs(src, true, ces);
        int[] newCes = null;
        int newLen = 0;
        if (doNew) {
            newCes = new int[30];
            for (int i = 0; i < len; ++i) {
                int ce = ces[i];
                int p = UCA.getPrimary(ce);
                int s = UCA.getSecondary(ce);
                int t = UCA.getTertiary(ce);
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

    static final byte    WITHOUT_NAMES                = 0, WITH_NAMES = 1, IN_XML = 2;

    static final boolean SKIP_CANONICAL_DECOMPOSIBLES = true;

    static final int     TRANSITIVITY_COUNT           = 8000;
    static final int     TRANSITIVITY_ITERATIONS      = 1000000;

    static void testTransitivity() {
        char[] tests = new char[TRANSITIVITY_COUNT];
        String[] keys = new String[TRANSITIVITY_COUNT];

        int i = 0;
        System.out.println("Loading");
        for (char ch = 0; i < tests.length; ++ch) {
            byte type = getCollator(CollatorType.ducet).getCEType(ch);
            if (type >= UCA.FIXED_CE) {
                continue;
            }
            Utility.dot(ch);
            tests[i] = ch;
            keys[i] = getCollator(CollatorType.ducet).getSortKey(String.valueOf(ch));
            ++i;
        }

        java.util.Comparator cm = new RuleComparator();

        i = 0;
        Utility.fixDot();
        System.out.println("Comparing");

        while (i++ < TRANSITIVITY_ITERATIONS) {
            Utility.dot(i);
            int a = (int) Math.random() * TRANSITIVITY_COUNT;
            int b = (int) Math.random() * TRANSITIVITY_COUNT;
            int c = (int) Math.random() * TRANSITIVITY_COUNT;
            int ab = cm.compare(keys[a], keys[b]);
            int bc = cm.compare(keys[b], keys[c]);
            int ca = cm.compare(keys[c], keys[a]);

            if (ab < 0 && bc < 0 && ca < 0 || ab > 0 && bc > 0 && ca > 0) {
                System.out.println("Transitivity broken for "
                        + Utility.hex(a)
                        + ", " + Utility.hex(b)
                        + ", " + Utility.hex(c));
            }
        }
    }

    // static Normalizer nfdNew = new Normalizer(Normalizer.NFD, "");
    // static Normalizer NFC = new Normalizer(Normalizer.NFC, "");
    // static Normalizer nfkdNew = new Normalizer(Normalizer.NFKD, "");

    static int getFirstCELen(int[] ces, int len) {
        if (len < 2) {
            return len;
        }
        int expansionStart = 1;
        if (UCA.isImplicitLeadCE(ces[0])) {
            expansionStart = 2; // move up if first is double-ce
        }
        if (len > expansionStart && getCollator(CollatorType.ducet).getHomelessSecondaries().contains(UCA.getSecondary(ces[expansionStart]))) {
            if (log2 != null) {
                log2.println("Homeless: " + CEList.toString(ces, len));
            }
            ++expansionStart; // move up if *second* is homeless ignoreable
        }
        return expansionStart;
    }

    static PrintWriter log2 = null;

    static void writeRules(byte option, boolean shortPrint, boolean noCE, CollatorType collatorType2) throws IOException {

        // testTransitivity();
        // if (true) return;

        int[] ces = new int[50];
        // Normalizer nfd = new Normalizer(Normalizer.NFD, UNICODE_VERSION);
        // Normalizer nfkd = new Normalizer(Normalizer.NFKD, UNICODE_VERSION);

        if (false) {
            int len2 = getCollator(collatorType2).getCEs("\u2474", true, ces);
            System.out.println(CEList.toString(ces, len2));

            String a = getCollator(collatorType2).getSortKey("a");
            String b = getCollator(collatorType2).getSortKey("A");
            System.out.println(getCollator(collatorType2).strengthDifference(a, b));
        }

        System.out.println("Sorting");
        Map backMap = new HashMap();
        java.util.Comparator cm = new RuleComparator();
        Map ordered = new TreeMap(cm);

        UCA.UCAContents cc = getCollator(collatorType2).getContents(UCA.FIXED_CE,
                SKIP_CANONICAL_DECOMPOSIBLES ? Default.nfd() : null);
        int[] lenArray = new int[1];

        Set alreadyDone = new HashSet();

        log2 = Utility.openPrintWriter(UCA.getUCA_GEN_DIR() + File.separator + "log", "UCARules-log.txt", Utility.UTF8_WINDOWS);

        while (true) {
            String s = cc.next(ces, lenArray);
            if (s == null) {
                break;
            }
            int len = lenArray[0];

            if (s.equals("\uD800")) {
                System.out.println("Check: " + CEList.toString(ces, len));
            }

            log2.println(s + "\t" + bidiBracket(CEList.toString(ces, len)) + "\t" + Default.ucd().getCodeAndName(s));

            addToBackMap(backMap, ces, len, s, false);

            int ce2 = 0;
            int ce3 = 0;
            int logicalFirstLen = getFirstCELen(ces, len);
            if (logicalFirstLen > 1) {
                ce2 = ces[1];
                if (logicalFirstLen > 2) {
                    ce3 = ces[2];
                }
            }

            String key = String.valueOf(UCA.getPrimary(ces[0])) + String.valueOf(UCA.getPrimary(ce2)) + String.valueOf(UCA.getPrimary(ce3))
                    + String.valueOf(UCA.getSecondary(ces[0])) + String.valueOf(UCA.getSecondary(ce2)) + String.valueOf(UCA.getSecondary(ce3))
                    + String.valueOf(UCA.getTertiary(ces[0])) + String.valueOf(UCA.getTertiary(ce2)) + String.valueOf(UCA.getTertiary(ce3))
                    + getCollator(collatorType2).getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + UCA.codePointOrder(s);

            // String.valueOf((char)(ces[0]>>>16)) +
            // String.valueOf((char)(ces[0] & 0xFFFF))
            // + String.valueOf((char)(ce2>>>16)) + String.valueOf((char)(ce2 &
            // 0xFFFF))

            if (s.equals("\u0660") || s.equals("\u2080")) {
                System.out.println(Default.ucd().getCodeAndName(s) + "\t" + Utility.hex(key));
            }

            ordered.put(key, s);
            alreadyDone.add(s);

            Object result = ordered.get(key);
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
        Iterator it = alreadyDone.iterator();
        while (it.hasNext()) {
            String member = (String) it.next();
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
                int len = getCollator(collatorType2).getCEs(s, true, ces);

                log2.println(s + "\t" + CEList.toString(ces, len)
                        + "\t" + Default.ucd().getCodeAndName(s) + " from " + Default.ucd().getCodeAndName(i));

                addToBackMap(backMap, ces, len, s, false);
            }
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

        String directory = WriteCollationData.getCollator(collatorType2).getUCA_GEN_DIR() + File.separator
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
        int oldFirstPrimary = UCA.getPrimary(UCA.TERMINATOR);
        boolean wasVariable = false;

        // String lastSortKey = collator.getSortKey("\u0000");;
        // 12161004
        int lastCE = 0;
        int ce = 0;
        int nextCE = 0;
        int lastCJKPrimary = 0;

        boolean firstTime = true;

        boolean done = false;

        String chr = "";
        int len = -1;

        String nextChr = "";
        int nextLen = -1; // -1 signals that we need to skip!!
        int[] nextCes = new int[50];

        String lastChr = "";
        int lastLen = -1;
        int[] lastCes = new int[50];
        int lastExpansionStart = 0;
        int expansionStart = 0;

        long variableTop = getCollator(collatorType2).getVariableHighCE() & FractionalUCA.Variables.INT_MASK;

        // for debugging ordering
        String lastSortKey = "";
        boolean showNext = false;

        for (int loopCounter = 0; !done; loopCounter++) {
            Utility.dot(loopCounter);

            lastCE = ce;
            lastLen = len;
            lastChr = chr;
            lastExpansionStart = expansionStart;
            if (len > 0) {
                System.arraycopy(ces, 0, lastCes, 0, lastLen);
            }

            // copy the current from Next

            ce = nextCE;
            len = nextLen;
            chr = nextChr;
            if (nextLen > 0) {
                System.arraycopy(nextCes, 0, ces, 0, nextLen);
            }

            // We need to look ahead one, to be able to reset properly

            if (it.hasNext()) {
                String nextSortKey = (String) it.next();
                nextChr = (String) ordered.get(nextSortKey);
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

            nextLen = getCollator(collatorType2).getCEs(nextChr, true, nextCes);
            nextCE = nextCes[0];

            // skip first (fake) element

            if (len == -1) {
                continue;
            }

            // for debugging

            if (loopCounter < 5) {
                System.out.println(loopCounter);
                System.out.println(CEList.toString(lastCes, lastLen) + ", " + Default.ucd().getCodeAndName(lastChr));
                System.out.println(CEList.toString(ces, len) + ", " + Default.ucd().getCodeAndName(chr));
                System.out.println(CEList.toString(nextCes, nextLen) + ", " + Default.ucd().getCodeAndName(nextChr));
            }

            // get relation

            /*
             * if (chr.charAt(0) == 0xFFFB) { System.out.println("DEBUG"); }
             */

            if (chr.equals("\u0966")) {
                System.out.println(CEList.toString(ces, len));
            }

            expansionStart = getFirstCELen(ces, len);

            // int relation = getStrengthDifference(ces, len, lastCes, lastLen);
            int relation = getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);

            if (relation == QUARTERNARY_DIFF) {
                int relation2 = getStrengthDifference(ces, len, lastCes, lastLen);
                if (relation2 != QUARTERNARY_DIFF) {
                    relation = TERTIARY_DIFF;
                }
            }

            // RESETs: do special case for relations to fixed items

            String reset = "";
            String resetComment = "";
            int xmlReset = 0;
            boolean insertVariableTop = false;
            boolean resetToParameter = false;

            int ceLayout = getCELayout(ce, getCollator(collatorType2));
            if (ceLayout == IMPLICIT) {
                if (relation == PRIMARY_DIFF) {
                    int primary = UCA.getPrimary(ce);
                    int resetCp = UCA.ImplicitToCodePoint(primary, UCA.getPrimary(ces[1]));

                    int[] ces2 = new int[50];
                    int len2 = getCollator(collatorType2).getCEs(UTF16.valueOf(resetCp), true, ces2);
                    relation = getStrengthDifference(ces, len, ces2, len2);

                    reset = quoteOperand(UTF16.valueOf(resetCp));
                    if (!shortPrint) {
                        resetComment = Default.ucd().getCodeAndName(resetCp);
                    }
                    // lastCE = UCA.makeKey(primary, UCA.NEUTRAL_SECONDARY,
                    // UCA.NEUTRAL_TERTIARY);
                    xmlReset = 2;
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
            if (len > expansionStart) {
                // int tert0 = ces[0] & 0xFF;
                // boolean isCompat = tert0 != 2 && tert0 != 8;
                log2.println("Exp: " + Default.ucd().getCodeAndName(chr) + ", " + CEList.toString(ces, len) + ", start: " + expansionStart);
                int[] rel = { relation };
                expansion = getFromBackMap(backMap, ces, expansionStart, len, chr, rel);
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
                        System.out.println("LCes: " + CEList.toString(lastCes, lastLen) + ", " + lastExpansionStart
                                + ", " + Default.ucd().getCodeAndName(lastChr));
                        System.out.println("Ces:  " + CEList.toString(ces, len) + ", " + expansionStart
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
                            log.print(CEList.toString(ces, len) + " ");
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
                            log.print(CEList.toString(ces, len) + " ");
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
        // log.println("& [top]"); // RESET
        if (option == IN_XML) {
            log.println("</rules></collation>");
        }
        log2.close();
        log.close();
        Utility.fixDot();
    }

    static String bidiBracket(String string) {
        if (BIDI.containsSome(string)) {
            return LRM + string + LRM;
        }
        return string;
    }

    static final UnicodeProperty bidiProp = getToolUnicodeSource().getProperty("bc");
    static final UnicodeSet      BIDI     = new UnicodeSet(bidiProp.getSet("AL")).addAll(bidiProp.getSet("R")).freeze();
    static final String          LRM      = "\u200E";

    static String latestAge(String chr) {
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

    static UnicodeProperty ageProp;

    static String getAge(int cp) {
        if (ageProp == null)
            ageProp = getToolUnicodeSource().getProperty("age");
        return ageProp.getValue(cp, true);
    }

    static UnicodeSet oldCharacters = new UnicodeSet("[:assigned:]");

    static boolean containsNew(String chr) {
        return (!oldCharacters.containsAll(chr));
    }

    static final int NONE = 0, T_IGNORE = 1, S_IGNORE = 2, P_IGNORE = 3, VARIABLE = 4, NON_IGNORE = 5, IMPLICIT = 6, TRAILING = 7;

    static int getCELayout(int ce, UCA collator) {
        int primary = collator.getPrimary(ce);
        int secondary = collator.getSecondary(ce);
        int tertiary = collator.getSecondary(ce);
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

    static long getPrimary(int[] ces, int len) {
        for (int i = 0; i < len; ++i) {
            int result = UCA.getPrimary(ces[i]);
            if (result == 0) {
                continue;
            }
            if (UCA.isImplicitLeadPrimary(result)) {
                return (result << 16) + UCA.getPrimary(ces[i + 1]);
            } else {
                return result;
            }
        }
        return 0;
    }

    static long getSecondary(int[] ces, int len) {
        for (int i = 0; i < len; ++i) {
            int result = UCA.getSecondary(ces[i]);
            if (result == 0) {
                continue;
            }
            return result;
        }
        return 0;
    }

    static long getTertiary(int[] ces, int len) {
        for (int i = 0; i < len; ++i) {
            int result = UCA.getTertiary(ces[i]);
            if (result == 0) {
                continue;
            }
            return result;
        }
        return 0;
    }

    static final int
    PRIMARY_DIFF = 0,
    SECONDARY_DIFF = 1,
    TERTIARY_DIFF = 2,
    QUARTERNARY_DIFF = 3,
    DONE = -1;

    static class CE_Iterator {
        int[] ces;
        int   len;
        int   current;
        int   level;

        void reset(int[] ces, int len) {
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
                int ce = ces[current++];
                switch (level) {
                case PRIMARY_DIFF:
                    val = UCA.getPrimary(ce);
                    break;
                case SECONDARY_DIFF:
                    val = UCA.getSecondary(ce);
                    break;
                case TERTIARY_DIFF:
                    val = UCA.getTertiary(ce);
                    break;
                }
                if (val != 0) {
                    return val;
                }
            }
            return DONE;
        }
    }

    static CE_Iterator ceit1 = new CE_Iterator();
    static CE_Iterator ceit2 = new CE_Iterator();

    // WARNING, Never Recursive!

    static int getStrengthDifference(int[] ces, int len, int[] lastCes, int lastLen) {
        if (false && lastLen > 0 && lastCes[0] > 0) {
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

        /*
         * 
         * int relation = QUARTERNARY_DIFF; if (getPrimary(ces, len) !=
         * getPrimary(lastCes, lastLen)) { relation = PRIMARY_DIFF; } else if
         * (getSecondary(ces, len) != getSecondary(lastCes, lastLen)) { relation
         * = SECONDARY_DIFF; } else if (getTertiary(ces, len) !=
         * getTertiary(lastCes, lastLen)) { relation = TERTIARY_DIFF; } else if
         * (len > lastLen) { relation = TERTIARY_DIFF; // HACK } else { int
         * minLen = len < lastLen ? len : lastLen; int start =
         * UCA.isImplicitLeadCE(ces[0]) ? 2 : 1; for (int kk = start; kk <
         * minLen; ++kk) { int lc = lastCes[kk]; int c = ces[kk]; if
         * (collator.getPrimary(c) != collator.getPrimary(lc) ||
         * collator.getSecondary(c) != collator.getSecondary(lc)) { relation =
         * QUARTERNARY_DIFF; // reset relation on FIRST char, since differ
         * anyway break; } else if (collator.getTertiary(c) >
         * collator.getTertiary(lc)) { relation = TERTIARY_DIFF; // reset to
         * tertiary (but later ce's might override!) } } } return relation;
         */
    }

    // static final String[] RELATION_NAMES = {" <", "   <<", "     <<<",
    // "         ="};
    static final String[] RELATION_NAMES     = { " <\t", "  <<\t", "   <<<\t", "    =\t" };
    static final String[] XML_RELATION_NAMES = { "p", "s", "t", "i" };

    static class ArrayWrapper {
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
    }

    static int    testCase[] = {
        // collator.makeKey(0xFF40, 0x0020,
        // 0x0002),
        getCollator(CollatorType.ducet).makeKey(0x0255, 0x0020, 0x000E),
    };

    static String testString = "\u33C2\u002E";

    static boolean contains(int[] array, int start, int limit, int key) {
        for (int i = start; i < limit; ++i) {
            if (array[i] == key) {
                return true;
            }
        }
        return false;
    }

    static final void addToBackMap(Map backMap, int[] ces, int len, String s, boolean show) {
        if (show || contains(testCase, 0, testCase.length, ces[0]) || testString.indexOf(s) > 0) {
            System.out.println("Test case: " + Utility.hex(s) + ", " + CEList.toString(ces, len));
        }
        // NOTE: we add the back map based on the string value; the smallest
        // (UTF-16 order) string wins
        Object key = new ArrayWrapper((int[]) (ces.clone()), 0, len);
        if (false) {
            String value = (String) backMap.get(key);
            if (value == null) {
                return;
            }
            if (s.compareTo(value) >= 0) {
                return;
            }
        }
        backMap.put(key, s);
        /*
         * // HACK until Ken fixes for (int i = 0; i < len; ++i) { int ce =
         * ces[i]; if (collator.isImplicitLeadCE(ce)) { ++i; ce = ces[i]; if
         * (DEBUG && (UCA.getPrimary(ce) == 0 || UCA.getSecondary(ce) != 0 ||
         * UCA.getTertiary(ce) != 0)) {
         * System.out.println("WEIRD 2nd IMPLICIT: " + CEList.toString(ces, len)
         * + ", " + ucd.getCodeAndName(s)); } ces[i] =
         * UCA.makeKey(UCA.getPrimary(ce), NEUTRAL_SECONDARY, NEUTRAL_TERTIARY);
         * } } backMap.put(new ArrayWrapper((int[])(ces.clone()), 0, len), s);
         */
    }

    /*
     * static int[] ignorableList = new int[homelessSecondaries.size()];
     * 
     * static { UnicodeSetIterator ui = new
     * UnicodeSetIterator(homelessSecondaries); int counter = 0; while
     * (ui.next()) { ignorableList. UCA.makeKey(0x0000, 0x0153, 0x0002),
     * UCA.makeKey(0x0000, 0x0154, 0x0002), UCA.makeKey(0x0000, 0x0155, 0x0002),
     * UCA.makeKey(0x0000, 0x0156, 0x0002), UCA.makeKey(0x0000, 0x0157, 0x0002),
     * UCA.makeKey(0x0000, 0x0158, 0x0002), UCA.makeKey(0x0000, 0x0159, 0x0002),
     * UCA.makeKey(0x0000, 0x015A, 0x0002), UCA.makeKey(0x0000, 0x015B, 0x0002),
     * UCA.makeKey(0x0000, 0x015C, 0x0002), UCA.makeKey(0x0000, 0x015D, 0x0002),
     * UCA.makeKey(0x0000, 0x015E, 0x0002), UCA.makeKey(0x0000, 0x015F, 0x0002),
     * UCA.makeKey(0x0000, 0x0160, 0x0002), UCA.makeKey(0x0000, 0x0161, 0x0002),
     * UCA.makeKey(0x0000, 0x0162, 0x0002), UCA.makeKey(0x0000, 0x0163, 0x0002),
     * UCA.makeKey(0x0000, 0x0164, 0x0002), UCA.makeKey(0x0000, 0x0165, 0x0002),
     * UCA.makeKey(0x0000, 0x0166, 0x0002), UCA.makeKey(0x0000, 0x0167, 0x0002),
     * UCA.makeKey(0x0000, 0x0168, 0x0002), UCA.makeKey(0x0000, 0x0169, 0x0002),
     * UCA.makeKey(0x0000, 0x016A, 0x0002), UCA.makeKey(0x0000, 0x016B, 0x0002),
     * UCA.makeKey(0x0000, 0x016C, 0x0002), UCA.makeKey(0x0000, 0x016D, 0x0002),
     * UCA.makeKey(0x0000, 0x016E, 0x0002), UCA.makeKey(0x0000, 0x016F, 0x0002),
     * UCA.makeKey(0x0000, 0x0170, 0x0002), };
     */

    static final String getFromBackMap(Map backMap, int[] originalces, int expansionStart, int len, String chr, int[] rel) {
        int[] ces = (int[]) (originalces.clone());

        String expansion = "";

        // process ces to neutralize tertiary

        for (int i = expansionStart; i < len; ++i) {
            int probe = ces[i];
            char primary = getCollator(CollatorType.ducet).getPrimary(probe);
            char secondary = getCollator(CollatorType.ducet).getSecondary(probe);
            char tertiary = getCollator(CollatorType.ducet).getTertiary(probe);

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
            ces[i] = getCollator(CollatorType.ducet).makeKey(primary, secondary, tert);
        }

        for (int i = expansionStart; i < len;) {
            int limit;
            String s = null;
            for (limit = len; limit > i; --limit) {
                ArrayWrapper wrapper = new ArrayWrapper(ces, i, limit);
                s = (String) backMap.get(wrapper);
                if (s != null) {
                    break;
                }
            }
            if (s == null) {
                do {
                    if (getCollator(CollatorType.ducet).getHomelessSecondaries().contains(UCA.getSecondary(ces[i]))) {
                        s = "";
                        if (rel[0] > 1) {
                            rel[0] = 1; // HACK
                        }
                        break;
                    }

                    // Try stomping the value to different tertiaries

                    int probe = ces[i];
                    if (UCA.isImplicitLeadCE(probe)) {
                        s = UTF16.valueOf(UCA.ImplicitToCodePoint(UCA.getPrimary(probe), UCA.getPrimary(ces[i + 1])));
                        ++i; // skip over next item!!
                        break;
                    }

                    char primary = getCollator(CollatorType.ducet).getPrimary(probe);
                    char secondary = getCollator(CollatorType.ducet).getSecondary(probe);

                    ces[i] = getCollator(CollatorType.ducet).makeKey(primary, secondary, 2);
                    ArrayWrapper wrapper = new ArrayWrapper(ces, i, i + 1);
                    s = (String) backMap.get(wrapper);
                    if (s != null) {
                        break;
                    }

                    ces[i] = getCollator(CollatorType.ducet).makeKey(primary, secondary, 0xE);
                    wrapper = new ArrayWrapper(ces, i, i + 1);
                    s = (String) backMap.get(wrapper);
                    if (s != null) {
                        break;
                    }

                    /*
                     * int meHack = UCA.makeKey(0x1795,0x0020,0x0004); if
                     * (ces[i] == meHack) { s = "\u3081"; break; }
                     */

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

    /*
     * 
     * static final String getFromBackMap(Map backMap, int[] ces, int index, int
     * limit) { ArrayWrapper wrapper = new ArrayWrapper(ces, index, limit);
     * 
     * int probe = ces[index]; wrapperContents[0] = probe; String s =
     * (String)backMap.get(wrapper);
     * 
     * outputLen[0] = 1; if (s != null) return s;
     * 
     * char primary = collator.getPrimary(probe); char secondary =
     * collator.getSecondary(probe); char tertiary =
     * collator.getTertiary(probe);
     * 
     * if (isFixedIdeograph(remapUCA_CompatibilityIdeographToCp(primary))) {
     * return String.valueOf(primary); } else { int tert = tertiary; switch
     * (tert) { case 8: case 9: case 0xA: case 0xB: case 0xC: case 0x1D: tert =
     * 8; break; case 0xD: case 0x10: case 0x11: case 0x12: case 0x13: case
     * 0x1C: tert = 0xE; break; default: tert = 2; break; } probe =
     * collator.makeKey(primary, secondary, tert); wrapperContents[0] = probe; s
     * = (String)backMap.get(wrapper); if (s != null) return s;
     * 
     * probe = collator.makeKey(primary, secondary, collator.NEUTRAL_TERTIARY);
     * wrapperContents[0] = probe; s = (String)backMap.get(wrapper); } if (s !=
     * null) return s;
     * 
     * if (primary != 0 && secondary != collator.NEUTRAL_SECONDARY) { int[]
     * dummyArray = new int[1]; dummyArray[0] = collator.makeKey(primary,
     * collator.NEUTRAL_SECONDARY, tertiary); String first =
     * getFromBackMap(backMap, dummyArray, 0, outputLen);
     * 
     * dummyArray[0] = collator.makeKey(0, secondary,
     * collator.NEUTRAL_TERTIARY); String second = getFromBackMap(backMap,
     * dummyArray, 0, outputLen);
     * 
     * if (first != null && second != null) { s = first + second; } } return s;
     * }
     */

    static final String[] RELATION                     = {
        "<", " << ", "  <<<  ", "    =    ", "    =    ", "    =    ", "  >>>  ", " >> ", ">"
    };

    static StringBuffer   quoteOperandBuffer           = new StringBuffer();                            // faster

    static UnicodeSet     needsQuoting                 = null;
    static UnicodeSet     needsUnicodeForm             = null;

    // copied from RBBI
    static String         gRuleSet_rule_char_pattern   = "[^[\\p{Z}\\u0020-\\u007f]-[\\p{L}]-[\\p{N}]]";
    static String         gRuleSet_white_space_pattern = "[\\p{Pattern_White_Space}]";

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

    // static Normalizer NFKD = new Normalizer(Normalizer.NFKD,
    // UNICODE_VERSION);
    // static Normalizer NFD = new Normalizer(Normalizer.NFD, UNICODE_VERSION);

    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd','HH:mm:ss' GMT'");

    static String getNormalDate() {
        return Default.getDate() + " [MD]";
    }

    static void copyFile(PrintWriter log, String fileName) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            log.println(line);
        }
        input.close();
    }

    static UnicodeSet compatibilityExceptions = new UnicodeSet("[\u0CCB\u0DDD\u017F\u1E9B\uFB05]");

    static void writeCollationValidityLog() throws IOException {

        // log = new PrintWriter(new
        // FileOutputStream("CheckCollationValidity.html"));
        log = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), "CheckCollationValidity.html", Utility.UTF8_WINDOWS);

        log.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>UCA Validity Log</title>");
        log.println("<style>");
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
        /*
         * for (int i = 0; i <= 0x10FFFF; ++i) { if (EXCLUDE_UNSUPPORTED &&
         * !collator.found.contains(i)) continue; if (0xD800 <= i && i <=
         * 0xF8FF) continue; // skip surrogates and private use //if (0xA000 <=
         * c && c <= 0xA48F) continue; // skip YI addString(UTF32.valueOf32(i),
         * option); }
         */

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, null);
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

    static void checkUnassigned() {

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
            if (ceList.count != 2) {
                bad.add(diChar);
            } else {
                UCA.UnassignedToImplicit(diChar.codePointAt(0), output);
                int ce0 = ceList.at(0);
                int ce1 = ceList.at(1);
                if (UCA.getPrimary(ce0) != output[0] || UCA.getPrimary(ce1) != output[1]) {
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

    static void checkDefaultIgnorables() {
        UnicodeSet exceptions = new UnicodeSet("[\u115F\u1160\u17B4\u17B5\u3164\uFFA0]");
        int errorCount = 0;
        System.out.println("Checking for defaultIgnorables");

        log.println("<h2>5a. Checking for Default Ignorables</h2>");
        log.println("<p>Checking that all Default Ignorables except " + exceptions + " should be secondary ignorables (L1 = L2 = 0)</p>");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        UnicodeSet di = new UnicodeSet(ups.getSet("default_ignorable_code_point=true")).removeAll(ups.getSet("gc=cn")).removeAll(exceptions);
        UnicodeSet bad = new UnicodeSet();
        for (String diChar : di) {
            CEList ceList = getCollator(CollatorType.ducet).getCEList(diChar, true);
            for (int i = 0; i < ceList.count; ++i) {
                int ce = ceList.at(i);
                if (UCA.getPrimary(ce) != 0 || UCA.getSecondary(ce) != 0) {
                    bad.add(diChar);
                }
            }
        }
        errorCount = bad.size();
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

    static ToolUnicodePropertySource ups;

    static ToolUnicodePropertySource getToolUnicodeSource() {
        if (ups == null)
            ups = ToolUnicodePropertySource.make(Default.ucdVersion());
        return ups;
    }

    static void addClosure() {
        int canCount = 0;
        System.out.println("Add missing decomposibles");
        log.println("<h2>7. Comparing Other Equivalents</h2>");
        log.println("<p>These are usually problems with contractions.</p>");
        log.println("<p>Each of the three strings is canonically equivalent, but has different sort keys</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Count</th><th>Type</th><th>Name</th><th>Code</th><th>Sort Keys</th></tr>");

        Set contentsForCanonicalIteration = new TreeSet();
        UCA.UCAContents ucac = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, null); // NFD
        int ccounter = 0;
        while (true) {
            Utility.dot(ccounter++);
            String s = ucac.next();
            if (s == null) {
                break;
            }
            contentsForCanonicalIteration.add(s);
        }

        Set additionalSet = new HashSet();

        System.out.println("Loading canonical iterator");
        if (canIt == null) {
            canIt = new CanonicalIterator(".");
        }
        Iterator it2 = contentsForCanonicalIteration.iterator();
        System.out.println("Adding any FCD equivalents that have different sort keys");
        while (it2.hasNext()) {
            String key = (String) it2.next();
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
                log.println("<td>" + getCollator(CollatorType.ducet).toString(sortKey) + "</td></tr>");
                log.println("<tr><td>NFD</td><td>" + Utility.replace(Default.ucd().getName(nfdKey), ", ", ",<br>") + "</td>");
                log.println("<td>" + Utility.hex(nfdKey) + "</td>");
                log.println("<td>" + getCollator(CollatorType.ducet).toString(sortKey) + "</td></tr>");
                log.println("<tr><td>Equiv.</td><td class='bottom'>" + Utility.replace(Default.ucd().getName(s), ", ", ",<br>") + "</td>");
                log.println("<td class='bottom'>" + Utility.hex(s) + "</td>");
                log.println("<td class='bottom'>" + getCollator(CollatorType.ducet).toString(nonDecompSortKey) + "</td></tr>");
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

    static void checkWellformedTable() throws IOException {
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

        int[] ces = new int[50];

        UCA.UCAContents cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());
        int[] lenArray = new int[1];

        int minps = Integer.MAX_VALUE;
        int minpst = Integer.MAX_VALUE;
        String minpsSample = "", minpstSample = "";

        while (true) {
            String str = cc.next(ces, lenArray);
            if (str == null) {
                break;
            }
            int len = lenArray[0];

            for (int i = 0; i < len; ++i) {
                int ce = ces[i];
                int p = UCA.getPrimary(ce);
                int s = UCA.getSecondary(ce);
                int t = UCA.getTertiary(ce);

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

        cc = getCollator(CollatorType.ducet).getContents(UCA.FIXED_CE, Default.nfd());
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        int lastPrimary = 0;

        while (true) {
            String str = cc.next(ces, lenArray);
            if (str == null) {
                break;
            }
            int len = lenArray[0];

            for (int i = 0; i < len; ++i) {
                int ce = ces[i];
                int p = UCA.getPrimary(ce);
                int s = UCA.getSecondary(ce);
                int t = UCA.getTertiary(ce);

                // IF we are at the start of an implicit, then just check that
                // the implicit is in range
                // CHECK implicit
                if (getCollator(CollatorType.ducet).isImplicitLeadPrimary(lastPrimary)) {
                    try {
                        if (s != 0 || t != 0) {
                            throw new Exception("Second implicit must be [X,0,0]");
                        }
                        getCollator(CollatorType.ducet).ImplicitToCodePoint(lastPrimary, p); // throws
                        // exception
                        // if bad
                    } catch (Exception e) {
                        log.println("<tr><td>" + (++errorCount) + ". BAD IMPLICIT: " + e.getMessage()
                                + "</td><td>" + CEList.toString(ces, len)
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
                            + "</td><td>" + CEList.toString(ces, len)
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    lastPrimary = p;
                    continue;
                }

                // Check WF#1

                if (p != 0 && s == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.1"
                            + "</td><td>" + CEList.toString(ces, len)
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }
                if (s != 0 && t == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.2"
                            + "</td><td>" + CEList.toString(ces, len)
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }

                // Check WF#2

                if (p != 0) {
                    if (s > minps) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.2"
                                + "</td><td>" + CEList.toString(ces, len)
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                }
                if (s != 0) {
                    if (t > minpst) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.3"
                                + "</td><td>" + CEList.toString(ces, len)
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

    static final String  SERIOUS_ERROR          = "<p><b><font color='#FF0000'>SERIOUS_ERROR!</font></b></p>";
    static final String  IMPORTANT_ERROR        = "<p><b><font color='#FF0000'>IMPORTANT_ERROR!</font></b></p>";

    /*
     * 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;; 4DB5;<CJK
     * Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;; 4E00;<CJK Ideograph,
     * First>;Lo;0;L;;;;;N;;;;; 9FA5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
     * AC00;<Hangul Syllable, First>;Lo;0;L;;;;;N;;;;; D7A3;<Hangul Syllable,
     * Last>;Lo;0;L;;;;;N;;;;; A000;YI SYLLABLE IT;Lo;0;L;;;;;N;;;;; A001;YI
     * SYLLABLE IX;Lo;0;L;;;;;N;;;;; A4C4;YI RADICAL ZZIET;So;0;ON;;;;;N;;;;;
     * A4C6;YI RADICAL KE;So;0;ON;;;;;N;;;;;
     */

    static final int[][] extraConformanceRanges = {
        { 0x3400, 0x4DB5 }, { 0x4E00, 0x9FA5 }, { 0xAC00, 0xD7A3 }, { 0xA000, 0xA48C }, { 0xE000, 0xF8FF },
        { 0xFDD0, 0xFDEF },
        { 0x20000, 0x2A6D6 },
        { 0x2F800, 0x2FA1D },
    };

    static final int[]   extraConformanceTests  = {
        // 0xD800, 0xDBFF, 0xDC00,
        // 0xDFFF,
        0xFDD0, 0xFDEF, 0xFFF8,
        0xFFFE, 0xFFFF,
        0x10000, 0x1FFFD, 0x1FFFE, 0x1FFFF,
        0x20000, 0x2FFFD, 0x2FFFE, 0x2FFFF,
        0xE0000, 0xEFFFD, 0xEFFFE, 0xEFFFF,
        0xF0000, 0xFFFFD, 0xFFFFE, 0xFFFFF,
        0x100000, 0x10FFFD, 0x10FFFE, 0x10FFFF,
        FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY, FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY - 1,
        FractionalUCA.Variables.IMPLICIT_4BYTE_BOUNDARY + 1,
    };

    static final int     MARK                   = 1;
    static final char    MARK1                  = '\u0001';
    static final char    MARK2                  = '\u0002';
    // Normalizer normalizer = new Normalizer(Normalizer.NFC, true);

    // static Normalizer toC = new Normalizer(Normalizer.NFC, UNICODE_VERSION);
    // static Normalizer toD = new Normalizer(Normalizer.NFD, UNICODE_VERSION);
    static TreeMap       MismatchedC            = new TreeMap();
    static TreeMap       MismatchedN            = new TreeMap();
    static TreeMap       MismatchedD            = new TreeMap();

    static final byte    option                 = UCA.NON_IGNORABLE;                                            // SHIFTED

    static void addString(int ch, byte option) {
        addString(UTF32.valueOf32(ch), option, CollatorType.ducet);
    }

    static void addString(String ch, byte option, CollatorType collatorType) {
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
        String colC = colCbase + "\u0000" + ch;
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

    static void removeAdjacentDuplicates() {
        String lastChar = "";
        int countRem = 0;
        int countDups = 0;
        int errorCount = 0;
        Iterator it1 = sortedD.keySet().iterator();
        Iterator it2 = sortedN.keySet().iterator();
        Differ differ = new Differ(250, 3);
        log.println("<h2>2. Differences in Ordering</h2>");
        log.println("<p>Codes and names are in the white rows: bold means that the NO-NFD sort key differs from UCA key.</p>");
        log.println("<p>Keys are in the light blue rows: green is the bad key, blue is UCA, black is where they equal.</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>File Order</th><th>Code and Decomp</th><th>Key and Decomp-Key</th></tr>");

        while (true) {
            boolean gotOne = false;
            if (it1.hasNext()) {
                String col1 = (String) it1.next();
                String ch1 = (String) sortedD.get(col1);
                differ.addA(ch1);
                gotOne = true;
            }

            if (it2.hasNext()) {
                String col2 = (String) it2.next();
                String ch2 = (String) sortedN.get(col2);
                differ.addB(ch2);
                gotOne = true;
            }

            differ.checkMatch(!gotOne);

            if (differ.getACount() != 0 || differ.getBCount() != 0) {
                for (int q = 0; q < 2; ++q) {
                    String cell = "<td valign='top'" + (q != 0 ? "bgcolor='#C0C0C0'" : "") + ">" + (q != 0 ? "<tt>" : "");

                    log.print("<tr>" + cell);
                    for (int i = -1; i < differ.getACount() + 1; ++i) {
                        showDiff(q == 0, true, differ.getALine(i), differ.getA(i));
                        log.println("<br>");
                        ++countDups;
                    }
                    countDups -= 2; // to make up for extra line above and below
                    if (false) {
                        log.print("</td>" + cell);

                        for (int i = -1; i < differ.getBCount() + 1; ++i) {
                            showDiff(q == 0, false, differ.getBLine(i), differ.getB(i));
                            log.println("<br>");
                        }
                    }
                    log.println("</td></tr>");
                }
                errorCount++;
            }
            // differ.flush();

            if (!gotOne) {
                break;
            }
        }

        log.println("</table>");

        log.println("<p>Errors: " + errorCount + "</p>");

        // log.println("Removed " + countRem + " adjacent duplicates.<br>");
        System.out.println("Left " + countDups + " conflicts.<br>");
        log.println("Left " + countDups + " conflicts.<br>");
        log.flush();
    }

    static void removeAdjacentDuplicates2() {
        String lastChar = "";
        int countRem = 0;
        int countDups = 0;
        int errorCount = 0;
        Iterator it = sortedD.keySet().iterator();
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
            String col = (String) it.next();
            String ch = (String) sortedD.get(col);
            String colN = (String) backN.get(ch);
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

    static int compareMinusLast(String a, String b) {
        String am = a.substring(0, a.length() - 1);
        String bm = b.substring(0, b.length() - 1);
        int result = am.compareTo(b);
        return (result < 0 ? -1 : result > 0 ? 1 : 0);
    }

    static void showLine(int count, String ch, String keyD, String keyN) {
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

    TreeSet               foo;

    static final String[] alternateName = { "SHIFTED", "ZEROED", "NON_IGNORABLE", "SHIFTED_TRIMMED" };

    static final ToolUnicodePropertySource propertySource = ToolUnicodePropertySource.make(null);
    static final UnicodeProperty gc = propertySource.getProperty("gc");
    static final UnicodeProperty sc = propertySource.getProperty("sc");

    static final UnicodeSet WHITESPACE = propertySource.getSet("whitespace=true")
            .freeze();

    static final UnicodeSet IGNORABLE = new UnicodeSet()
    .addAll(gc.getSet("Cf"))
    .addAll(gc.getSet("Cc"))
    .addAll(gc.getSet("Mn"))
    .addAll(gc.getSet("Me"))
    .addAll(propertySource.getSet("Default_Ignorable_Code_Point=Yes"))
    .freeze();

    static final UnicodeSet PUNCTUATION = new UnicodeSet()
    .addAll(gc.getSet("pc"))
    .addAll(gc.getSet("pd"))
    .addAll(gc.getSet("ps"))
    .addAll(gc.getSet("pe"))
    .addAll(gc.getSet("pi"))
    .addAll(gc.getSet("pf"))
    .addAll(gc.getSet("po"))
    .freeze();

    static final UnicodeSet GENERAL_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sm"))
    .addAll(gc.getSet("Sk"))
    .addAll(gc.getSet("So"))
    .freeze();

    static final UnicodeSet CURRENCY_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sc"))
    .freeze();

    static final UnicodeSet NUMBERS = new UnicodeSet()
    .addAll(gc.getSet("Nd"))
    .addAll(gc.getSet("No"))
    .addAll(gc.getSet("Nl"))
    .freeze();

    static final UnicodeSet OTHERS = new UnicodeSet(0,0x10FFFF)
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

    static final UnicodeMap<String> EMPTY = new UnicodeMap<String>().freeze();
    static final UnicodeMap<String> IGNORABLE_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ࠤࠨःংঃਃઃଂଃఁ-ఃಂಃംഃංඃཿ းះៈᬄᮂ᳡ᳲ�ꢀꢁꦃ꯬�𑀀𑀂𑂂��𝅥𝅦𝅭-𝅲]"), "Unknown why these are ignored")
            .putAll(new UnicodeSet("[ـߺﱞ-ﱣﳲ-ﳴﹰ-ﹴﹶ-ﹿ]"), 
                    "Tatweel and related characters are ignorable; isolated vowels have screwy NFKD values")
                    .freeze();
    static final UnicodeMap<String> GENERAL_SYMBOL_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ៗ ː ˑ ॱ ๆ ໆ ᪧ ꧏ ꩰ ꫝ 々 〻 〱-〵 ゝ ゞ ーｰ ヽ ヾ \uAAF3 \uAAF4]"), "regular (not ignorable) symbols - significant modifiers")
            .putAll(new UnicodeSet("[৴-৹ ୲-୷ ꠰-꠵ ௰-௲ ൰-൵ ፲-፼ ↀ-ↂ ↆ-ↈ 𐹩-𐹾 ⳽ 𐌢 𐌣 𐄐-𐄳 𐅀 𐅁 𐅄-𐅇 𐅉-𐅎 𐅐-𐅗 𐅠-𐅲 𐅴-𐅸 𐏓-𐏕 𐩾 𐤗-𐤙 𐡛-𐡟 𐭜-𐭟 𐭼-𐭿 𑁛-𑁥 𐩄-𐩇 𒐲 𒐳 𒑖 𒑗 𒑚-𒑢 𝍩-𝍱]"), 
                    "Unknown why these are treated differently than numbers with scripts")
                    .freeze();
    static final UnicodeMap<String> SCRIPT_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[⺀-⺙⺛-⺞⺠-⻲]"), "CJK radicals sort like they had toNFKD values")
            .putAll(new UnicodeSet("[ ᅟᅠㅤﾠ]"), "Hangul fillers are Default Ignorables, but sort as primaries")
            .putAll(gc.getSet("Mn"), "Indic (or Indic-like) non-spacing marks sort as primaries")
            .putAll(new UnicodeSet("[🅐-🅩🅰-🆏🆑-🆚🇦-🇿]"), "Characters that should have toNFKD values")
            .putAll(new UnicodeSet("[ᛮ-ᛰꛦ-ꛯ𐍁𐍊]"), "Unknown why these numbers are treated differently than numbers with symbols.")
            .freeze();

    enum UcaBucket {
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

        UcaBucket(int least, UnicodeSet expected, UnicodeMap toWarn) {
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

    static void checkScripts() {
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

        UCAContents contents = collator.getContents(UCA.FIXED_CE, null);
        UnicodeSet covered = new UnicodeSet();
        UnicodeSet illegalCharacters = new UnicodeSet();
        int errorCount = 0;
        UcaBucket lastOrder = UcaBucket.whitespace;
        CEList lastCe = null;
        String lastString = "";
        String lastShown = "";

        Pair ceAndString1 = new Pair(null, null);
        TreeSet<Pair> sorted = new TreeSet<Pair>();
        while (contents.next(ceAndString1)) {
            sorted.add(ceAndString1);
            ceAndString1 = new Pair(null, null);
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
            String pattern = set.toPattern(false);
            if (pattern.length() > 120) {
                pattern = pattern.substring(0,120) + "…";
            }
            log.println("<tr>"
                    + "<td>" + value + "</td>"
                    + "<td>" + set.size() +  "</td>"
                    + "<td>" + pattern +  "</td>"
                    + "</tr>");
        }
        log.println("</table>");
    }

    private static void showTypeLine(String messageAttr, String message, UcaBucket actualOrder, final String string, final CEList ce) {
        log.println("<tr>"
                + "<td" + messageAttr + ">" + message + "</td>"
                + "<td>" + actualOrder + "</td>"
                + "<td>" + Default.ucd().getCategoryID(string.codePointAt(0)) + "</td>"
                + "<td>" + Default.ucd().getScriptID(string.codePointAt(0), SHORT) + "</td>"
                + "<td>" + string + "</td>"
                + "<td>" + Utility.hex(string) + "</td>"
                + "<td>" + ce + "</td>"
                + "<td>" + Default.ucd().getName(string) + "</td>"
                + "</tr>");
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

    static void showMismatches() {
        log.println("<h2>1. Mismatches when NFD is OFF</h2>");
        log.println("<p>Alternate Handling = " + alternateName[option] + "</p>");
        log.println("<p>NOTE: NFD form is used by UCA,"
                + "so if other forms are different there are <i>ignored</i>. This may indicate a problem, e.g. missing contraction.</p>");
        log.println("<table border='1'>");
        log.println("<tr><th>Name</th><th>Type</th><th>Unicode</th><th>Key</th></tr>");
        Iterator it = MismatchedC.keySet().iterator();
        int errorCount = 0;
        while (it.hasNext()) {
            String ch = (String) it.next();
            String MN = (String) MismatchedN.get(ch);
            String MC = (String) MismatchedC.get(ch);
            String MD = (String) MismatchedD.get(ch);
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

    static boolean containsCombining(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if ((Default.ucd().getCategoryMask(s.charAt(i)) & Default.ucd().MARK_MASK) != 0) {
                return true;
            }
        }
        return false;
    }

    static void showDiff(boolean showName, boolean firstColumn, int line, Object chobj) {
        String ch = chobj.toString();
        String decomp = Default.nfd().normalize(ch);
        if (showName) {
            if (ch.equals(decomp)) {
                log.println(// title + counter + " "
                        Utility.hex(ch, " ")
                        + " " + Default.ucd().getName(ch)
                        );
            } else {
                log.println(// title + counter + " "
                        "<b>" + Utility.hex(ch, " ")
                        + " " + Default.ucd().getName(ch) + "</b>"
                        );
            }
        } else {
            String keyD = printableKey(backD.get(chobj));
            String keyN = printableKey(backN.get(chobj));
            if (keyD.equals(keyN)) {
                log.println(// title + counter + " "
                        Utility.hex(ch, " ") + " " + keyN);
            } else {
                log.println(// title + counter + " "
                        "<font color='#009900'>" + Utility.hex(ch, " ") + " " + keyN
                        + "</font><br><font color='#000099'>" + Utility.hex(decomp, " ") + " " + keyD + "</font>"
                        );
            }
        }
    }

    static String printableKey(Object keyobj) {
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

    /*
     * LINKS</td></tr><tr><td><blockquote> CONTENTS
     */

    static void writeTail(PrintWriter out, int counter, String title, String other, boolean show) throws IOException {
        copyFile(out, "HTML-Part2.txt");
        /*
         * out.println("</tr></table></center></div>");
         * out.println("</body></html>");
         */
        out.close();
    }

    static String pad(int number) {
        String num = Integer.toString(number);
        if (num.length() < 2) {
            num = "0" + number;
        }
        return num;
    }

    static PrintWriter writeHead(int counter, int end, String title, String other, String version, boolean show) throws IOException {

        PrintWriter out = Utility.openPrintWriter(getCollator(CollatorType.ducet).getUCA_GEN_DIR(), title + pad(counter) + ".html", Utility.UTF8_WINDOWS);

        copyFile(out, "HTML-Part1.txt");
        /*
         * out.println("<html><head>");out.println(
         * "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>"
         * ); out.println("<title>" + HTMLString(title) + "</title>");
         * out.println("<style>"); out.println("<!--");//out.println(
         * "td           { font-size: 18pt; font-family: Bitstream Cyberbit, Arial Unicode MS; text-align: Center}"
         * );
         * out.println("td           { font-size: 18pt; text-align: Center}");
         * out
         * .println("td.right           { font-size: 12pt; text-align: Right}");
         * out
         * .println("td.title           { font-size: 18pt; text-align: Center}"
         * );
         * out.println("td.left           { font-size: 12pt; text-align: Left}"
         * );//out.println(
         * "th         { background-color: #C0C0C0; font-size: 18pt; font-family: Arial Unicode MS, Bitstream Cyberbit; text-align: Center }"
         * ); out.println("tt         { font-size: 8pt; }");
         * //out.println("code           { font-size: 8pt; }");
         * out.println("-->");
         * out.println("</style></head><body bgcolor='#FFFFFF'>");
         * 
         * // header out.print("<table width='100%'><tr>");out.println(
         * "<td><p align='left'><font size='3'><a href='index.html'>Instructions</a></font></td>"
         * ); out.println("<td>" + HTMLString(title) + " Version" + version +
         * "</td>"); out.println("<td><p align='right'><font size='3'><a href='"
         * + other + pad(counter) + ".html'>" + (show ? "Hide" : "Show") +
         * " Key</a></td>"); out.println("</tr></table>"); /* <table
         * width="100%"> <tr> <td.left><a href="Collation.html"> <font
         * size="3">Instructions</font></a> <td> <td.title>Collation
         * Version-2.1.9d7 <td> <p align="right"><a
         * href="CollationKey24.html"><font size="3">Show Key</font></a> </tr>
         */

        // index
        out.print("<table width='100%'><tr>");
        out.println("<td><p align='left'><font size='3'><a href='index.html'>Instructions</a></font></td>");
        out.println("<td>" + HTMLString(title) + " Version" + version + "</td>");
        out.println("<td><p align='right'><font size='3'><a href='" + other + pad(counter) + ".html'>"
                + (show ? "Hide" : "Show") + " Key</a></td>");
        out.println("</tr></table>");

        out.print("<table width='100%'><tr>");
        out.print("<td width='1%'><p align='left'>");
        if (counter > 0) {
            out.print("<a href='" + title + pad(counter - 1) + ".html'>&lt;&lt;</a>");
        } else {
            out.print("<font color='#999999'>&lt;&lt;</font>");
        }
        out.println("</td>");
        out.println("<td><p align='center'>");
        boolean lastFar = false;
        for (int i = 0; i <= end; ++i) {
            boolean far = (i < counter - 2 || i > counter + 2);
            if (far && ((i % 5) != 0) && (i != end)) {
                continue;
            }
            if (i != 0 && lastFar != far) {
                out.print(" - ");
            }
            lastFar = far;
            if (i != counter) {
                out.print("<a href='" + title + pad(i) + ".html'>" + i + "</a>");
            } else {
                out.print("<font color='#FF0000'>" + i + "</font>");
            }
            out.println();
        }
        out.println("</td>");
        out.println("<td width='1%'><p align='right'>");
        if (counter < end) {
            out.print("<a href='" + title + pad(counter + 1) + ".html'>&gt;&gt;</a>");
        } else {
            out.print("<font color='#999999'>&gt;&gt;</font>");
        }
        out.println("</td></tr></table>");
        // standard template!!!
        out.println("</td></tr><tr><td><blockquote>");
        // out.println("<p><div align='center'><center><table border='1'><tr>");
        return out;
    }

    static int getStrengthDifference(String old, String newStr) {
        int result = 5;
        int min = old.length();
        if (newStr.length() < min) {
            min = newStr.length();
        }
        for (int i = 0; i < min; ++i) {
            char ch1 = old.charAt(i);
            char ch2 = newStr.charAt(i);
            if (ch1 != ch2) {
                return result;
            }
            // see if we get difference before we get 0000.
            if (ch1 == 0) {
                --result;
            }
        }
        if (newStr.length() != old.length()) {
            return 1;
        }
        return 0;
    }

    static final boolean needsXMLQuote(String source, boolean quoteApos) {
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

    public static final String XMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, true);
    }

    public static final String XMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, true);
    }

    public static final String XMLString(String source) {
        return XMLBaseString(source, true);
    }

    public static final String HTMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, false);
    }

    public static final String HTMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, false);
    }

    public static final String HTMLString(String source) {
        return XMLBaseString(source, false);
    }

    public static final String XMLBaseString(int[] cps, int len, boolean quoteApos) {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            temp.append((char) cps[i]);
        }
        return XMLBaseString(temp.toString(), quoteApos);
    }

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

    static int mapToStartOfRange(int ch) {
        if (ch <= 0x3400) {
            return ch; // CJK Ideograph Extension A
        }
        if (ch <= 0x4DB5) {
            return 0x3400;
        }
        if (ch <= 0x4E00) {
            return ch; // CJK Ideograph
        }
        if (ch <= 0x9FA5) {
            return 0x4E00;
        }
        if (ch <= 0xAC00) {
            return ch; // Hangul Syllable
        }
        if (ch <= 0xD7A3) {
            return 0xAC00;
        }
        if (ch <= 0xD800) {
            return ch; // Non Private Use High Surrogate
        }
        if (ch <= 0xDB7F) {
            return 0xD800;
        }
        if (ch <= 0xDB80) {
            return ch; // Private Use High Surrogate
        }
        if (ch <= 0xDBFF) {
            return 0xDB80;
        }
        if (ch <= 0xDC00) {
            return ch; // Low Surrogate
        }
        if (ch <= 0xDFFF) {
            return 0xDC00;
        }
        if (ch <= 0xE000) {
            return ch; // Private Use
        }
        if (ch <= 0xF8FF) {
            return 0xE000;
        }
        if (ch <= 0xF0000) {
            return ch; // Plane 15 Private Use
        }
        if (ch <= 0xFFFFD) {
            return 0xF0000;
        }
        if (ch <= 0x100000) {
            return ch; // Plane 16 Private Use
        }
        return 0x100000;
    }

    static void setCollator(UCA collator) {
        WriteCollationData.ducetCollator = collator;
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
        int firstScriptPrimary = UCA.getPrimary(firstForA);
        Remap primaryRemap = new Remap();
        int counter = 1;
        RoBitSet primarySet = oldCollator.getStatistics().getPrimarySet();
        // gather the data
        UnicodeSet spaces = new UnicodeSet();
        UnicodeSet punctuation = new UnicodeSet();
        UnicodeSet generalSymbols = new UnicodeSet();
        UnicodeSet currencySymbols = new UnicodeSet();
        UnicodeSet numbers = new UnicodeSet();

        int oldVariableHigh = UCA.getPrimary(oldCollator.getVariableHighCE());
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

        // verify results
        int[] output = new int[30];
        StringBuilder failures = new StringBuilder();
        for (int i = 0; i <= 0x10FFFF; ++i) {            
            byte type2 = result.getCEType(i);
            if (type2 >= FIXED_CE) {
                continue;
            }
            if (!Default.ucd().isAllocated(i)) continue;
            int ceCount = result.getCEs(UTF16.valueOf(i), true, output);
            int primary = ceCount < 1 ? 0 : UCA.getPrimary(output[0]);
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
                // no actoin
            }
        }
        if (failures.length() > 0) {
            throw new IllegalArgumentException("Failures:\n" + failures);
        }
        return result;
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