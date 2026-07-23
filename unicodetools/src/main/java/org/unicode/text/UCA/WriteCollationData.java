/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/WriteCollationData.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCA;

import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.IntStack;
import org.unicode.text.utility.Utility;

public class WriteCollationData {
    private static PrintWriter log;

    static void writeVersionAndDate(PrintWriter log, String filename, boolean auxiliary) {
        log.println(Utility.getDataHeader(filename));
        String version = UCA.getDucetCollator().getDataVersion();
        log.println("# UCA Version: " + version);
        log.println("# UCD Version: " + version);
        if (auxiliary) {
            log.println(
                    "# For a description of the format and usage, see\n"
                            + "# http://www.unicode.org/reports/tr35/tr35-collation.html#Root_Data_Files");
        } else {
            log.println(
                    "# For a description of the format and usage, see\n"
                            + "# https://www.unicode.org/reports/tr10/#Conformance_Tests");
        }
        log.println();
    }

    private static final boolean SKIP_CANONICAL_DECOMPOSIBLES = true;

    private static int getFirstCELen(CEList ces) {
        final int len = ces.length();
        if (len < 2) {
            return len;
        }
        int expansionStart = 1;
        if (UCA.isImplicitLeadCE(ces.at(0))) {
            expansionStart = 2; // move up if first is double-ce
        }
        if (len > expansionStart
                && UCA.getDucetCollator()
                        .getHomelessSecondaries()
                        .contains(CEList.getSecondary(ces.at(expansionStart)))) {
            if (log2 != null) {
                log2.println("Homeless: " + ces);
            }
            ++expansionStart; // move up if *second* is homeless ignoreable
        }
        return expansionStart;
    }

    private static PrintWriter log2 = null;

    // Called by UCA.Main.
    static void writeRules(boolean shortPrint, boolean noCE, CollatorType collatorType)
            throws IOException {
        System.out.println("Sorting");
        final Map<ArrayWrapper, String> backMap = new HashMap<ArrayWrapper, String>();
        final Map<String, String> ordered = new TreeMap<String, String>();

        final UCA uca = UCA.getCollator(collatorType);
        final UCA.UCAContents cc =
                uca.getContents(SKIP_CANONICAL_DECOMPOSIBLES ? Default.nfd() : null);

        final Set<String> alreadyDone = new HashSet<String>();

        log2 =
                Utility.openPrintWriter(
                        UCA.getOutputDir() + File.separator + "log",
                        "UCARules-log.txt",
                        Utility.UTF8_WINDOWS);

        while (true) {
            final String s = cc.next();
            if (s == null) {
                break;
            }
            final CEList ces = cc.getCEs();

            if (s.equals("\uD800")) {
                System.out.println("Check: " + ces);
            }

            final String safeString = s.replace("\u0000", "\\u0000");
            log2.println(
                    safeString
                            + "\t"
                            + bidiBracket(ces.toString())
                            + "\t"
                            + Default.ucd().getCodeAndName(s));

            addToBackMap(backMap, ces, s, false);

            int ce2 = 0;
            int ce3 = 0;
            final int logicalFirstLen = getFirstCELen(ces);
            if (logicalFirstLen > 1) {
                ce2 = ces.at(1);
                if (logicalFirstLen > 2) {
                    ce3 = ces.at(2);
                }
            }

            final String key =
                    String.valueOf(CEList.getPrimary(ces.at(0)))
                            + String.valueOf(CEList.getPrimary(ce2))
                            + String.valueOf(CEList.getPrimary(ce3))
                            + String.valueOf(CEList.getSecondary(ces.at(0)))
                            + String.valueOf(CEList.getSecondary(ce2))
                            + String.valueOf(CEList.getSecondary(ce3))
                            + String.valueOf(CEList.getTertiary(ces.at(0)))
                            + String.valueOf(CEList.getTertiary(ce2))
                            + String.valueOf(CEList.getTertiary(ce3))
                            + uca.getSortKey(s, UCA_Types.Alternate.NON_IGNORABLE)
                            + '\u0000'
                            + UCA.codePointOrder(s);

            // String.valueOf((char)(ces.at(0]>>>16)) +
            // String.valueOf((char)(ces.at(0] & 0xFFFF))
            // + String.valueOf((char)(ce2>>>16)) + String.valueOf((char)(ce2 &
            // 0xFFFF))

            if (s.equals("\u0660") || s.equals("\u2080")) {
                System.out.println(Default.ucd().getCodeAndName(s) + "\t" + Utility.hex(key));
            }

            ordered.put(key, s);
            alreadyDone.add(s);

            final String result = ordered.get(key);
            if (result == null) {
                System.out.println("BAD SORT: " + Utility.hex(key) + ", " + Utility.hex(s));
            }
        }

        System.out.println("Checking CJK");

        // Check for characters that are ARE explicitly mapped in the CJK ranges
        final UnicodeSet CJK = new UnicodeSet(0x2E80, 0x2EFF);
        CJK.add(0x2F00, 0x2EFF);
        CJK.add(0x2F00, 0x2FDF);
        CJK.add(0x3400, 0x9FFF);
        CJK.add(0xF900, 0xFAFF);
        CJK.add(0x20000, 0x2A6DF);
        CJK.add(0x2F800, 0x2FA1F);
        CJK.removeAll(new UnicodeSet("[:Cn:]")); // remove unassigned

        // make set with canonical decomposibles
        final UnicodeSet composites = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd().isAllocated(i)) {
                continue;
            }
            if (Default.nfd().isNormalized(i)) {
                continue;
            }
            composites.add(i);
        }
        final UnicodeSet CJKcomposites = new UnicodeSet(CJK).retainAll(composites);
        System.out.println("CJK composites " + CJKcomposites.toPattern(true));
        System.out.println(
                "CJK NONcomposites " + new UnicodeSet(CJK).removeAll(composites).toPattern(true));

        final UnicodeSet mapped = new UnicodeSet();
        Iterator<String> it = alreadyDone.iterator();
        while (it.hasNext()) {
            final String member = it.next();
            mapped.add(member);
        }
        final UnicodeSet CJKmapped = new UnicodeSet(CJK).retainAll(mapped);
        System.out.println("Mapped CJK: " + CJKmapped.toPattern(true));
        System.out.println(
                "UNMapped CJK: " + new UnicodeSet(CJK).removeAll(mapped).toPattern(true));
        System.out.println(
                "Neither Mapped nor Composite CJK: "
                        + new UnicodeSet(CJK)
                                .removeAll(CJKcomposites)
                                .removeAll(CJKmapped)
                                .toPattern(true));

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
            final String decomp = Default.nfkd().normalize(i);
            int cp;
            for (int j = 0; j < decomp.length(); j += Character.charCount(cp)) {
                cp = decomp.codePointAt(j);
                final String s = Character.toString(cp);
                if (alreadyDone.contains(s)) {
                    continue;
                }

                alreadyDone.add(s);
                final CEList ces = uca.getCEList(s, true);

                log2.println(
                        s
                                + "\t"
                                + ces
                                + "\t"
                                + Default.ucd().getCodeAndName(s)
                                + " from "
                                + Default.ucd().getCodeAndName(i));

                addToBackMap(backMap, ces, s, false);
            }
        }

        System.out.println("Find Exact Equivalents");

        final Set<String> removals = new HashSet<String>();
        final Map<String, String> equivalentsMap =
                findExactEquivalents(backMap, ordered, collatorType, removals);
        for (final String s : removals) {
            ordered.remove(s);
        }

        System.out.println("Writing");

        String filename = "UCA_Rules";
        if (collatorType == CollatorType.ducet) {
            filename += "_DUCET";
        }
        if (shortPrint) {
            filename += "_SHORT";
        }
        if (noCE) {
            filename += "_NoCE";
        }
        filename += ".txt";

        final String directory =
                UCA.getOutputDir()
                        + File.separator
                        + (collatorType == CollatorType.cldr ? "CollationAuxiliary" : "Ducet");

        log = Utility.openPrintWriter(directory, filename, Utility.UTF8_WINDOWS);

        //        String[] commentText = {
        //                filename,
        //                "This file contains the UCA tables for the given version, but transformed
        // into rule syntax.",
        //                "Generated:   " + getNormalDate(),
        //                "NOTE: Since UCA handles canonical equivalents, no composites are
        // necessary",
        //                "(except in extensions).",
        //                "For syntax description, see:
        // http://oss.software.ibm.com/icu/userguide/Collate_Intro.html"
        //        };

        log.write('\uFEFF'); // BOM
        WriteCollationData.writeVersionAndDate(log, filename, collatorType == CollatorType.cldr);

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
        CEList nextCes = bogusCes; // bogusCes signals that we need to skip!!

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
                final String nextSortKey = it.next();
                nextChr = ordered.get(nextSortKey);
                final int result = nextSortKey.compareTo(lastSortKey);
                if (result < 0) {
                    System.out.println();
                    System.out.println("DANGER: Sort Key Unordered!");
                    System.out.println(
                            (loopCounter - 1)
                                    + " "
                                    + Utility.hex(lastSortKey)
                                    + ", "
                                    + Default.ucd()
                                            .getCodeAndName(
                                                    lastSortKey.charAt(lastSortKey.length() - 1)));
                    System.out.println(
                            loopCounter
                                    + " "
                                    + Utility.hex(nextSortKey)
                                    + ", "
                                    + Default.ucd()
                                            .getCodeAndName(
                                                    nextSortKey.charAt(nextSortKey.length() - 1)));
                }
                if (nextChr == null) {
                    Utility.fixDot();
                    if (!showNext) {
                        System.out.println();
                        System.out.println(
                                (loopCounter - 1)
                                        + "   Last = "
                                        + Utility.hex(lastSortKey)
                                        + ", "
                                        + Default.ucd()
                                                .getCodeAndName(
                                                        lastSortKey.charAt(
                                                                lastSortKey.length() - 1)));
                    }
                    System.out.println(
                            lastSortKey.compareTo(nextSortKey)
                                    + ", "
                                    + nextSortKey.compareTo(lastSortKey));
                    System.out.println(
                            loopCounter
                                    + " NULL AT  "
                                    + Utility.hex(nextSortKey)
                                    + ", "
                                    + Default.ucd()
                                            .getCodeAndName(
                                                    nextSortKey.charAt(nextSortKey.length() - 1)));
                    nextChr = "??";
                    showNext = true;
                } else if (showNext) {
                    showNext = false;
                    System.out.println(
                            lastSortKey.compareTo(nextSortKey)
                                    + ", "
                                    + nextSortKey.compareTo(lastSortKey));
                    System.out.println(
                            loopCounter
                                    + "   Next = "
                                    + Utility.hex(nextSortKey)
                                    + ", "
                                    + Default.ucd().getCodeAndName(nextChr));
                }
                lastSortKey = nextSortKey;
            } else {
                nextChr = "??";
                done = true; // make one more pass!!!
            }

            nextCes = uca.getCEList(nextChr, true);
            nextCE = nextCes.isEmpty() ? 0 : nextCes.at(0);

            // skip first (fake) element

            if (ces == bogusCes) {
                continue;
            }

            // for debugging

            if (loopCounter < 5) {
                System.out.println(loopCounter);
                System.out.println(
                        lastCes.toString() + ", " + Default.ucd().getCodeAndName(lastChr));
                System.out.println(ces.toString() + ", " + Default.ucd().getCodeAndName(chr));
                System.out.println(
                        nextCes.toString() + ", " + Default.ucd().getCodeAndName(nextChr));
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
                final int relation2 =
                        getStrengthDifference(ces, ces.length(), lastCes, lastCes.length());
                if (relation2 != QUARTERNARY_DIFF) {
                    relation = TERTIARY_DIFF;
                }
            }

            // RESETs: do special case for relations to fixed items

            String reset = "";
            String resetComment = "";
            boolean insertVariableTop = false;
            boolean resetToParameter = false;

            final int ceLayout = getCELayout(ce, uca);
            if (ceLayout == IMPLICIT) {
                if (relation == PRIMARY_DIFF) {
                    final int primary = CEList.getPrimary(ce);
                    final int resetCp =
                            uca.implicit.codePointForPrimaryPair(
                                    primary, CEList.getPrimary(ces.at(1)));

                    final CEList ces2 = uca.getCEList(Character.toString(resetCp), true);
                    relation = getStrengthDifference(ces, ces.length(), ces2, ces2.length());

                    reset = quoteOperand(Character.toString(resetCp));
                    if (!shortPrint) {
                        resetComment = Default.ucd().getCodeAndName(resetCp);
                    }
                    // lastCE = UCA.makeKey(primary, UCA.NEUTRAL_SECONDARY,
                    // UCA.NEUTRAL_TERTIARY);
                }
                // lastCJKPrimary = primary;
            } else if (ceLayout != getCELayout(lastCE, uca) || firstTime) {
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
                log2.println(
                        "Exp: "
                                + Default.ucd().getCodeAndName(chr)
                                + ", "
                                + ces
                                + ", start: "
                                + expansionStart);
                final int[] rel = {relation};
                expansion = getFromBackMap(backMap, ces, expansionStart, ces.length(), chr, rel);
                // relation = rel[0];

                // The relation needs to be fixed differently. Since it is an
                // expansion, it should be compared to
                // the first CE
                // ONLY reset if the sort keys are not equal
                if (false && (relation == PRIMARY_DIFF || relation == SECONDARY_DIFF)) {
                    final int relation2 =
                            getStrengthDifference(ces, expansionStart, lastCes, lastExpansionStart);
                    if (relation2 != relation) {
                        System.out.println();
                        System.out.println(
                                "Resetting: "
                                        + RELATION_NAMES[relation]
                                        + " to "
                                        + RELATION_NAMES[relation2]);
                        System.out.println(
                                "LCes: "
                                        + lastCes
                                        + ", "
                                        + lastExpansionStart
                                        + ", "
                                        + Default.ucd().getCodeAndName(lastChr));
                        System.out.println(
                                "Ces:  "
                                        + ces
                                        + ", "
                                        + expansionStart
                                        + ", "
                                        + Default.ucd().getCodeAndName(chr));
                        relation = relation2;
                    }
                }
            }

            // print results
            // skip printing if it ends with a half-surrogate
            final char lastChar = chr.charAt(chr.length() - 1);
            if (Character.isHighSurrogate(lastChar)) {
                System.out.println("Skipping trailing surrogate: " + chr + "\t" + Utility.hex(chr));
            } else {
                if (insertVariableTop) {
                    log.println(RELATION_NAMES[0] + " [variable top]");
                }
                if (reset.length() != 0) {
                    log.println(
                            "& "
                                    + (resetToParameter ? "[" : "")
                                    + reset
                                    + (resetToParameter ? "]" : "")
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
                    final String typeKD = ReorderingTokens.getTypesCombined(chr);
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
            firstTime = false;
        }
        for (final Entry<String, String> sourceReplacement : equivalentsMap.entrySet()) {
            // note: we set the reset to the value we want, then have
            // = X for the item whose value is to be changed
            final String valueToSetTo = sourceReplacement.getValue();
            final String stringToSet = sourceReplacement.getKey();
            log.print("& " + quoteOperand(valueToSetTo) + " = " + quoteOperand(stringToSet));
            if (!shortPrint) {
                log.print("\t# ");
                log.print(latestAge(stringToSet) + " [");
                final String typeKD = ReorderingTokens.getTypesCombined(stringToSet);
                log.print(typeKD + "] ");
                log.print(
                        Default.ucd().getCodeAndName(stringToSet)
                                + "\t→\t"
                                + Default.ucd().getCodeAndName(valueToSetTo));
            }
            log.println();
        }
        // log.println("& [top]"); // RESET
        log2.close();
        log.close();
        Utility.fixDot();
    }

    private static final UnicodeSet SKIP_TIBETAN_EQUIVALENTS =
            new UnicodeSet("[ྲཱི  ྲཱི ྲཱུ  ྲཱུ ླཱི  ླཱི ླཱུ  ླཱུ]").freeze();

    private static Map<String, String> findExactEquivalents(
            Map<ArrayWrapper, String> backMap,
            Map<String, String> ordered,
            CollatorType collatorType,
            Set<String> removals) {
        final UCA collator = UCA.getCollator(collatorType);
        final Map<String, String> equivalentsStrings = new LinkedHashMap<String, String>();
        final IntStack nextCes = new IntStack(10);
        final int[] startBuffer = new int[100];
        final int[] endBuffer = new int[100];
        final ArrayWrapper start = new ArrayWrapper(startBuffer, 0, 0);
        final ArrayWrapper end = new ArrayWrapper(endBuffer, 0, 0);
        for (final Entry<String, String> entry : ordered.entrySet()) {
            final String sortKey = entry.getKey();
            final String string = entry.getValue();
            if (Character.codePointCount(string, 0, string.length()) < 2) {
                continue;
            } else if (SKIP_TIBETAN_EQUIVALENTS.containsSome(string)) {
                continue;
            }
            nextCes.clear();
            collator.getCEs(string, true, nextCes);
            final int len = nextCes.length();
            if (len < 2) {
                continue;
            }
            // just look for pairs
            for (int i = 1; i < len; ++i) {
                start.limit = nextCes.extractInto(0, i, startBuffer, 0);
                final String string1 = backMap.get(start);
                if (string1 == null) {
                    continue;
                }
                end.limit = nextCes.extractInto(i, len, endBuffer, 0);
                final String string2 = backMap.get(end);
                if (string2 == null) {
                    continue;
                }
                final String replacement = string1 + string2;
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

    private static ToolUnicodePropertySource ups;

    private static ToolUnicodePropertySource getToolUnicodeSource() {
        if (ups == null) {
            ups = ToolUnicodePropertySource.make(Default.ucdVersion());
        }
        return ups;
    }

    private static final UnicodeProperty bidiProp = getToolUnicodeSource().getProperty("bc");
    private static final UnicodeSet BIDI =
            new UnicodeSet(bidiProp.getSet("AL")).addAll(bidiProp.getSet("R")).freeze();
    private static final String LRM = "\u200E";

    private static String latestAge(String chr) {
        int cp;
        String latestAge = "";
        for (int i = 0; i < chr.length(); i += Character.charCount(cp)) {
            final String age = getAge(cp = chr.codePointAt(i));
            if (Utility.NumericComparator.INSTANCE.compare(latestAge, age) < 0) {
                latestAge = age;
            }
        }
        return latestAge;
    }

    private static UnicodeProperty ageProp;

    private static String getAge(int cp) {
        if (ageProp == null) {
            ageProp = getToolUnicodeSource().getProperty("age");
        }
        return ageProp.getValue(cp, true);
    }

    private static final int T_IGNORE = 1,
            S_IGNORE = 2,
            P_IGNORE = 3,
            VARIABLE = 4,
            NON_IGNORE = 5,
            IMPLICIT = 6,
            TRAILING = 7;

    private static int getCELayout(int ce, UCA collator) {
        final int primary = CEList.getPrimary(ce);
        final int secondary = CEList.getSecondary(ce);
        final int tertiary = CEList.getSecondary(ce);
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
        if (primary < Implicit.START) {
            return NON_IGNORE;
        }
        if (primary < Implicit.LIMIT) {
            return IMPLICIT;
        }
        return TRAILING;
    }

    private static final int PRIMARY_DIFF = 0,
            SECONDARY_DIFF = 1,
            TERTIARY_DIFF = 2,
            QUARTERNARY_DIFF = 3,
            DONE = -1;

    private static class CE_Iterator {
        CEList ces;
        int len;
        int current;
        int level;

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
                final int ce = ces.at(current++);
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
                final int weight1 = ceit1.next();
                final int weight2 = ceit2.next();
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

    private static final String[] RELATION_NAMES = {" <\t", "  <<\t", "   <<<\t", "    =\t"};

    private static class ArrayWrapper {
        int[] array;
        int start;
        int limit;

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

        @Override
        public boolean equals(Object other) {
            final ArrayWrapper that = (ArrayWrapper) other;
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

        @Override
        public int hashCode() {
            int result = limit - start;
            for (int i = start; i < limit; ++i) {
                result = result * 37 + array[i];
            }
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
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

    private static final void addToBackMap(
            Map<ArrayWrapper, String> backMap, CEList ces, String s, boolean show) {
        if (show
                || contains(testCase, 0, testCase.length, ces.at(0))
                || testString.indexOf(s) > 0) {
            System.out.println("Test case: " + Utility.hex(s) + ", " + ces);
        }
        // NOTE: we add the back map based on the string value; the smallest
        // (UTF-16 order) string wins
        final int[] cesArray = new int[ces.length()];
        final ArrayWrapper key = new ArrayWrapper(cesArray, 0, ces.appendTo(cesArray, 0));
        if (false) {
            final String value = backMap.get(key);
            if (value == null) {
                return;
            }
            if (s.compareTo(value) >= 0) {
                return;
            }
        }
        backMap.put(key, s);
    }

    private static final String getFromBackMap(
            Map<ArrayWrapper, String> backMap,
            CEList originalces,
            int expansionStart,
            int len,
            String chr,
            int[] rel) {
        final UCA ducet = UCA.getDucetCollator();
        final int[] ces = new int[originalces.length()];
        originalces.appendTo(ces, 0);

        String expansion = "";

        // process ces to neutralize tertiary

        for (int i = expansionStart; i < len; ++i) {
            final int probe = ces[i];
            final char primary = CEList.getPrimary(probe);
            final char secondary = CEList.getSecondary(probe);
            final char tertiary = CEList.getTertiary(probe);

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

        for (int i = expansionStart; i < len; ) {
            int limit;
            String s = null;
            for (limit = len; limit > i; --limit) {
                final ArrayWrapper wrapper = new ArrayWrapper(ces, i, limit);
                s = backMap.get(wrapper);
                if (s != null) {
                    break;
                }
            }
            if (s == null) {
                do {
                    if (ducet.getHomelessSecondaries().contains(CEList.getSecondary(ces[i]))) {
                        s = "";
                        if (rel[0] > 1) {
                            rel[0] = 1; // HACK
                        }
                        break;
                    }

                    // Try stomping the value to different tertiaries

                    final int probe = ces[i];
                    if (UCA.isImplicitLeadCE(probe)) {
                        int nextCE = ces[i + 1];
                        int c =
                                ducet.implicit.codePointForPrimaryPair(
                                        CEList.getPrimary(probe), CEList.getPrimary(nextCE));
                        s = Character.toString(c);
                        ++i; // skip over trail primary
                        break;
                    }

                    final char primary = CEList.getPrimary(probe);
                    final char secondary = CEList.getSecondary(probe);

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

                    System.out.println(
                            "Fix Homeless! No back map for "
                                    + CEList.toString(ces[i])
                                    + " from "
                                    + CEList.toString(ces, len));
                    System.out.println(
                            "\t"
                                    + Default.ucd().getCodeAndName(chr)
                                    + " => "
                                    + Default.ucd().getCodeAndName(Default.nfkd().normalize(chr)));
                    s = "[" + Utility.hex(ces[i]) + "]";
                } while (false); // exactly one time, just for breaking
                limit = i + 1;
            }
            expansion += s;
            i = limit;
        }
        return expansion;
    }

    private static UnicodeSet needsQuoting = null;
    private static UnicodeSet needsUnicodeForm = null;

    static final String quoteOperand(String s) {
        if (needsQuoting == null) {
            final ToolUnicodePropertySource ups = getToolUnicodeSource();
            final UnicodeProperty cat = ups.getProperty("gc");
            final UnicodeSet cn = cat.getSet("Cn");
            /*
             * c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <=
             * '9' || (c >= 0xA0 && !UCharacterProperty.isRuleWhiteSpace(c))
             */
            needsQuoting =
                    new UnicodeSet("[[:whitespace:][:z:][:c:][:Block=ASCII:]-[a-zA-Z0-9]-[:cn:]]")
                            .addAll(cn); //
            // "[[:Block=ASCII:]-[a-zA-Z0-9]-[:c:]-[:z:]]"); //
            // [:whitespace:][:c:][:z:]
            // for (int i = 0; i <= 0x10FFFF; ++i) {
            // if (UCharacterProperty.isRuleWhiteSpace(i)) needsQuoting.add(i);
            // }
            // needsQuoting.remove();
            needsUnicodeForm =
                    new UnicodeSet("[\\u000d\\u000a[:zl:][:zp:][:c:][:di:]-[:cn:]]").addAll(cn);
        }
        s = Default.nfc().normalize(s);
        StringBuilder quoteOperandBuffer = new StringBuilder();
        boolean noQuotes = true;
        boolean inQuote = false;
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            if (!needsQuoting.contains(cp)) {
                if (inQuote) {
                    quoteOperandBuffer.append('\'');
                    inQuote = false;
                }
                quoteOperandBuffer.append(Character.toString(cp));
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
                        quoteOperandBuffer.append(Character.toString(cp)); // cp !=
                        // 0x2028
                    } else if (cp > 0xFFFF) {
                        quoteOperandBuffer.append("\\U").append(Utility.hex(cp, 8));
                    } else if (cp <= 0x20 || cp > 0x7E) {
                        quoteOperandBuffer.append("\\u").append(Utility.hex(cp));
                    } else {
                        quoteOperandBuffer.append(Character.toString(cp));
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
        final String noDate = System.getProperty("NODATE");
        if (noDate != null) {
            return "(date omitted)";
        }
        final String date = myDateFormat.format(new Date());
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
}
