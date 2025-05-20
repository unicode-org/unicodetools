package org.unicode.text.UCA;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Counter;
import org.unicode.text.UCA.MappingsForFractionalUCA.MappingWithSortKey;
import org.unicode.text.UCA.PrimariesToFractional.PrimaryToFractional;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.OldEquivalenceClass;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.Utility;

public class FractionalUCA {
    private static boolean DEBUG = false;

    private static class HighByteToReorderingToken {
        private final PrimariesToFractional ps2f;
        private final ReorderingTokens[] highByteToReorderingToken = new ReorderingTokens[256];
        private boolean mergedScripts = false;

        HighByteToReorderingToken(PrimariesToFractional ps2f) {
            this.ps2f = ps2f;

            for (int i = 0; i < highByteToReorderingToken.length; ++i) {
                highByteToReorderingToken[i] = new ReorderingTokens();
            }
            // set special values
            highByteToReorderingToken[0].reorderingToken.add("TERMINATOR", 1);
            highByteToReorderingToken[1].reorderingToken.add("LEVEL-SEPARATOR", 1);
            highByteToReorderingToken[2].reorderingToken.add("FIELD-SEPARATOR", 1);
            highByteToReorderingToken[3].reorderingToken.add("SPACE", 1);
            for (int i = Fractional.IMPLICIT_BASE_BYTE; i <= Fractional.IMPLICIT_MAX_BYTE; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("IMPLICIT", 1);
            }
            for (int i = Fractional.IMPLICIT_MAX_BYTE + 1; i < Fractional.SPECIAL_BASE; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("TRAILING", 1);
            }
            for (int i = Fractional.SPECIAL_BASE; i <= 0xFF; ++i) {
                highByteToReorderingToken[i].reorderingToken.add("SPECIAL", 1);
            }
        }

        void addScriptsIn(long fractionalPrimary, String value) {
            // don't add to 0
            final int leadByte = Fractional.getLeadByte(fractionalPrimary);
            if (leadByte != 0) {
                highByteToReorderingToken[leadByte].addInfoFrom(fractionalPrimary, value);
            }
        }

        public String getInfo(boolean doScripts) {
            if (!mergedScripts) {
                throw new IllegalArgumentException();
            }
            final StringBuilder builder = new StringBuilder();
            final Map<String, Counter<Integer>> map = new TreeMap<String, Counter<Integer>>();
            for (int i = 0; i < highByteToReorderingToken.length; ++i) {
                addScriptCats(
                        map,
                        i,
                        doScripts
                                ? highByteToReorderingToken[i].reorderingToken
                                : highByteToReorderingToken[i].types);
            }
            appendReorderingTokenCatLine(builder, map, doScripts);
            return builder.toString();
        }

        private void addScriptCats(
                Map<String, Counter<Integer>> map, int i, Counter<String> scripts) {
            for (final String script : scripts) {
                final long count = scripts.get(script);
                Counter<Integer> scriptCounter = map.get(script);
                if (scriptCounter == null) {
                    map.put(script, scriptCounter = new Counter<Integer>());
                }
                scriptCounter.add(i, count);
            }
        }

        private void appendReorderingTokenCatLine(
                StringBuilder builder, Map<String, Counter<Integer>> map, boolean doScripts) {
            for (final String item : map.keySet()) {
                builder.append("[" + (doScripts ? "reorderingTokens" : "categories") + "\t")
                        .append(item)
                        .append('\t');
                final Counter<Integer> counter2 = map.get(item);
                boolean first = true;
                for (final Integer i : counter2) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(' ');
                    }

                    builder.append(Utility.hex(i, 2));
                    if (!doScripts) {
                        final String stringScripts =
                                CollectionUtilities.join(
                                        highByteToReorderingToken[i].reorderingToken.keySet(), " ");
                        if (stringScripts.length() != 0) {
                            builder.append('{').append(stringScripts).append("}");
                        }
                    }
                    builder.append('=').append(counter2.get(i));
                }
                builder.append(" ]\n");
            }
        }

        @Override
        public String toString() {
            if (!mergedScripts) {
                throw new IllegalArgumentException();
            }
            final StringBuilder result = new StringBuilder();
            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                final ReorderingTokens tokens = highByteToReorderingToken[k];
                result.append("[top_byte\t" + Utility.hex(k, 2) + "\t");
                tokens.appendTo(result, false);
                if (ps2f.isCompressibleLeadByte(k)) {
                    result.append("\tCOMPRESS");
                }
                result.append(" ]");
                // No need to output the number of characters per top_byte,
                // only causes gratuitous diffs.
                // If anything, the number of *primary weights* per top_byte might be interesting.
                // result.append("\t#\t");
                // tokens.appendTo(result, true);
                result.append("\n");
            }
            return result.toString();
        }

        private void cleanup(Map<Integer, String> overrides) {
            mergedScripts = true;
            final ReorderingTokens[] mergedTokens = new ReorderingTokens[256];

            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                mergedTokens[k] = new ReorderingTokens(highByteToReorderingToken[k]);
            }
            // Any bytes that share scripts need to be combined
            // brute force, because we don't care about speed
            boolean fixedOne = true;
            while (fixedOne) {
                fixedOne = false;
                for (int k = 1; k < mergedTokens.length; ++k) {
                    if (mergedTokens[k - 1].intersects(mergedTokens[k])
                            && !mergedTokens[k - 1].equals(mergedTokens[k])) {
                        mergedTokens[k - 1].or(mergedTokens[k]);
                        mergedTokens[k] = mergedTokens[k - 1];
                        fixedOne = true;
                    }
                }
            }
            for (final Entry<Integer, String> override : overrides.entrySet()) {
                final int value = override.getKey();
                final String tag = override.getValue();
                mergedTokens[value].setScripts(tag);
            }
            for (int k = 0; k < highByteToReorderingToken.length; ++k) {
                if (mergedTokens[k].reorderingToken.isEmpty()) {
                    mergedTokens[k].setScripts("Hani");
                    mergedTokens[k].reorderingToken.add("Hans", 1);
                    mergedTokens[k].reorderingToken.add("Hant", 1);
                }
                highByteToReorderingToken[k] = mergedTokens[k];
            }
        }
    }

    /**
     * Finds the minimum or maximum fractional collation element among those added via {@link
     * MinMaxFCE#setValue(int, int, int, String)}.
     */
    private static class MinMaxFCE {
        static final long UNDEFINED_MAX = Long.MAX_VALUE;
        static final long UNDEFINED_MIN = Long.MIN_VALUE;
        long[] key;
        boolean max;
        boolean debugShow = false;
        String source;
        String title;

        MinMaxFCE(boolean max, String title) {
            this.max = max;
            this.title = title;
            if (max) {
                key = new long[] {UNDEFINED_MIN, UNDEFINED_MIN, UNDEFINED_MIN}; // make small!
            } else {
                key = new long[] {UNDEFINED_MAX, UNDEFINED_MAX, UNDEFINED_MAX};
            }
        }

        boolean isUnset() {
            return key[0] == UNDEFINED_MIN || key[0] == UNDEFINED_MAX;
        }

        /**
         * Left-justifies the weight. If the weight is not zero, then the lead byte is moved to bits
         * 31..24.
         */
        static long fixWeight(int weight) {
            long result = weight & 0xFFFFFFFFL;
            if (result == 0) {
                return result;
            }
            while ((result & 0xFF000000) == 0) {
                result <<= 8; // shift to top
            }
            return result;
        }

        String formatFCE() {
            return formatFCE(false, WeightStyle.DEFAULT);
        }

        String formatFCE(boolean showEmpty, WeightStyle option) {
            String b0 = formatWeight(key[0], false, option, "pp");
            final boolean key0Defined = key[0] != UNDEFINED_MIN && key[0] != UNDEFINED_MAX;
            if (showEmpty && b0.length() == 0) {
                b0 = "X";
            }

            String b1 = formatWeight(key[1], key0Defined, option, "ss");
            final boolean key1Defined = key[1] != UNDEFINED_MIN && key[1] != UNDEFINED_MAX;
            if (b1.length() != 0) {
                b1 = " " + b1;
            } else if (showEmpty) {
                b1 = " X";
            }

            String b2 = formatWeight(key[2], key0Defined || key1Defined, option, "tt");
            if (b2.length() != 0) {
                b2 = " " + b2;
            } else if (showEmpty) {
                b2 = " X";
            }

            return "[" + b0 + "," + b1 + "," + b2 + "]";
        }

        String formatWeight(long val, boolean haveHigher, WeightStyle option, String blanked) {
            if (val == UNDEFINED_MIN) {
                return "?";
            }
            if (val == UNDEFINED_MAX) {
                if (haveHigher) {
                    val = Fractional.COMMON_SEC << 24;
                } else {
                    return "?";
                }
            }
            return option == WeightStyle.DEFAULT
                    ? Fractional.hexBytes(val)
                    : Fractional.blankedHexBytes(val, blanked, null).toString();
        }

        long getValue(int zeroBasedLevel) {
            return key[zeroBasedLevel];
        }

        enum WeightStyle {
            DEFAULT,
            BLANKED
        }

        @Override
        public String toString() {
            return toString(false, WeightStyle.DEFAULT);
        }

        String toString(boolean showEmpty, WeightStyle option) {
            final String src =
                    source.length() == 0 ? "CONSTRUCTED" : Default.ucd().getCodeAndName(source);
            return "["
                    + (max ? "last " : "first ")
                    + title
                    + " "
                    + formatFCE(showEmpty, option)
                    + "] # "
                    + src;
        }

        void setValue(int npInt, int nsInt, int ntInt, String source) {
            if (debugShow) {
                System.out.println(
                        "Setting FCE: "
                                + Utility.hex(npInt)
                                + ", "
                                + Utility.hex(nsInt)
                                + ", "
                                + Utility.hex(ntInt));
            }
            // to get the sign right!
            final long np = fixWeight(npInt);
            final long ns = fixWeight(nsInt);
            final long nt = fixWeight(ntInt);
            if (max) {
                // return if the key is LEQ
                if (np < key[0]) {
                    return;
                }
                if (np == key[0]) {
                    if (ns < key[1]) {
                        return;
                    }
                    if (ns == key[1]) {
                        if (nt <= key[2]) {
                            return;
                        }
                    }
                }
            } else {
                // return if the key is GEQ
                if (np > key[0]) {
                    return;
                }
                if (np == key[0]) {
                    if (ns > key[1]) {
                        return;
                    }
                    if (ns == key[1]) {
                        if (nt >= key[2]) {
                            return;
                        }
                    }
                }
            }
            // we didn't bail, so reset!
            key[0] = np;
            key[1] = ns;
            key[2] = nt;
            this.source = source;
        }
    }

    public static class FractionalStatistics {
        private final PrimariesToFractional ps2f;
        private final Map<Long, UnicodeSet> primaries = new TreeMap<Long, UnicodeSet>();
        private final Map<Long, UnicodeSet> secondaries = new TreeMap<Long, UnicodeSet>();
        private final Map<Long, UnicodeSet> tertiaries = new TreeMap<Long, UnicodeSet>();
        private final Output output;

        public FractionalStatistics(PrimariesToFractional ps2f, Output output) {
            this.ps2f = ps2f;
            this.output = output;
        }

        private void addToSet(Map<Long, UnicodeSet> map, int key0, String codepoints) {
            // shift up
            key0 = leftJustify(key0);
            final Long key = (long) key0;
            UnicodeSet secondarySet = map.get(key);
            if (secondarySet == null) {
                map.put(key, secondarySet = new UnicodeSet());
            }
            secondarySet.add(codepoints);
        }

        private int leftJustify(int key0) {
            if (key0 != 0) {
                while ((key0 & 0xFF000000) == 0) {
                    key0 <<= 8;
                }
            }
            return key0;
        }

        public void show(Appendable summary) {
            try {
                show("primary", primaries, summary);
                show("secondary", secondaries, summary);
                show("tertiary", tertiaries, summary);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private static final UnicodeSet AVOID_CODE_POINTS = // doesn't depend on version
                new UnicodeSet("[[:C:][:Noncharacter_Code_Point:]]").freeze();

        private static String safeString(String s, int maxLength) {
            if (AVOID_CODE_POINTS.containsSome(s)) {
                final StringBuilder sb = new StringBuilder(s);
                for (int i = 0; i < sb.length(); ) {
                    final int c = sb.codePointAt(i);
                    int next = i + Character.charCount(c);
                    if (AVOID_CODE_POINTS.contains(c)) {
                        String replacement;
                        if (c <= 0xffff) {
                            replacement = String.format("\\u%04X", c);
                        } else {
                            replacement = String.format("\\U%08X", c);
                        }
                        sb.replace(i, next, replacement);
                        next = i + replacement.length();
                    }
                    i = next;
                }
                s = sb.toString();
            }

            if (s.length() > maxLength) {
                if (Character.isHighSurrogate(s.charAt(maxLength - 1))) {
                    --maxLength;
                }
                s = s.substring(0, maxLength) + "...";
            }
            return s;
        }

        private void show(String type, Map<Long, UnicodeSet> map, Appendable summary)
                throws IOException {
            summary.append("\n# Start " + type + " statistics \n");
            for (final Entry<Long, UnicodeSet> entry : map.entrySet()) {
                final long weight = entry.getKey();
                final UnicodeSet uset = entry.getValue();
                final String pattern = uset.toPattern(false);
                summary.append(Fractional.hexBytes(weight))
                        .append('\t')
                        .append(String.valueOf(uset.size()))
                        .append('\t')
                        .append(safeString(pattern, 100))
                        .append('\n');
            }
            summary.append("# End " + type + "  statistics \n");
        }

        private final StringBuilder sb = new StringBuilder();
        private final StringBuilder blanked = new StringBuilder();

        private void printAndRecord(
                boolean show, String chr, int np, int ns, int nt, String comment) {
            try {
                addToSet(primaries, np, chr);
                addToSet(secondaries, ns, chr);
                addToSet(tertiaries, nt, chr);

                sb.setLength(0);
                blanked.setLength(0);
                if (show) {
                    sb.append(Utility.hex(chr)).append(";\t");
                    blanked.append(sb);
                }

                sb.append('[');
                Fractional.hexBytes(np, sb);
                sb.append(", ");
                Fractional.hexBytes(ns, sb);
                sb.append(", ");
                Fractional.hexBytes(nt, sb);
                sb.append(']');

                blanked.append('[');
                Fractional.blankedHexBytes(np, "pp", blanked);
                blanked.append(", ");
                Fractional.blankedHexBytes(ns, "ss", blanked);
                blanked.append(", ");
                Fractional.blankedHexBytes(nt, "tt", blanked);
                blanked.append(']');

                if (comment != null) {
                    sb.append('\t').append(comment).append('\n');
                    blanked.append('\t').append(comment).append('\n');
                }

                output.frac.print(sb);
                output.fracShort.print(sb);
                output.fracBlanked.print(blanked);
            } catch (final Exception e) {
                throw new ChainException("Character is {0}", new String[] {Utility.hex(chr)}, e);
            }
        }

        private void printAndRecordCodePoint(
                boolean show, String chr, int cp, int ns, int nt, String comment) {
            try {
                addToSet(secondaries, ns, chr);
                addToSet(tertiaries, nt, chr);

                sb.setLength(0);
                blanked.setLength(0);
                if (show) {
                    sb.append(Utility.hex(chr)).append(";\t");
                }

                sb.append("[U+").append(Utility.hex(cp));
                blanked.append(sb);
                if (ns != 5 && ns != 0x500) {
                    sb.append(", ");
                    blanked.append(", ");
                    Fractional.hexBytes(ns, sb);
                    Fractional.blankedHexBytes(ns, "ss", blanked);
                }
                if ((ns != 5 && ns != 0x500) || nt != 5) {
                    sb.append(", ");
                    blanked.append(", ");
                    Fractional.hexBytes(nt, sb);
                    Fractional.blankedHexBytes(nt, "tt", blanked);
                }
                sb.append(']');
                blanked.append(']');

                if (comment != null) {
                    sb.append('\t').append(comment).append('\n');
                    blanked.append('\t').append(comment).append('\n');
                }

                output.frac.print(sb);
                output.fracShort.print(sb);
                output.fracBlanked.print(blanked);
            } catch (final Exception e) {
                throw new ChainException("Character is {0}", new String[] {Utility.hex(chr)}, e);
            }
        }

        /**
         * Inserts the script-first primary. It only exists in FractionalUCA, there is no UCA
         * primary for it.
         */
        private void printAndRecordScriptFirstPrimary(
                boolean show, int reorderCode, boolean newByte, int firstPrimary) {
            String comment = String.format("# %s first primary", ReorderCodes.getName(reorderCode));
            if (newByte) {
                comment = comment + " starts new lead byte";
            }
            if (ps2f.isCompressibleFractionalPrimary(firstPrimary)) {
                comment = comment + " (compressible)";
            }
            final String scriptStartString = ReorderCodes.getScriptStartString(reorderCode);
            printAndRecord(true, scriptStartString, firstPrimary, 0x500, 5, comment);
            output.fracPrintln();
        }
    }

    private static final class Output {
        final String directory;
        final PrintWriter frac;
        final PrintWriter fracShort;
        final PrintWriter fracBlanked;
        final PrintWriter summary;
        final PrintWriter log;

        Output() {
            directory = UCA.getOutputDir() + "CollationAuxiliary" + File.separator;
            frac = Utility.openPrintWriter(directory, "FractionalUCA.txt", Utility.UTF8_WINDOWS);
            fracShort =
                    Utility.openPrintWriter(
                            directory, "FractionalUCA_SHORT.txt", Utility.UTF8_WINDOWS);
            fracBlanked =
                    Utility.openPrintWriter(
                            directory, "FractionalUCA_blanked.txt", Utility.UTF8_WINDOWS);
            summary =
                    Utility.openPrintWriter(
                            directory, "FractionalUCA_summary.txt", Utility.UTF8_WINDOWS);
            log =
                    Utility.openPrintWriter(
                            UCA.getOutputDir() + "log" + File.separator,
                            "FractionalUCA_log.txt",
                            Utility.UTF8_WINDOWS);
        }

        void close() {
            frac.close();
            fracShort.close();
            fracBlanked.close();
            summary.close();
            log.close();
        }

        void fracPrintln() {
            frac.println();
            fracShort.println();
            fracBlanked.println();
        }

        void fracPrintln(Object o) {
            String s = String.valueOf(o);
            frac.println(s);
            fracShort.println(s);
            fracBlanked.println(s);
        }

        void fracPrint(Object o) {
            String s = String.valueOf(o);
            frac.print(s);
            fracShort.print(s);
            fracBlanked.print(s);
        }

        void printRadicalStrokeOrder(RadicalStroke radicalStroke) throws IOException {
            radicalStroke.printRadicalStrokeOrder(frac);
            radicalStroke.printRadicalStrokeOrder(fracShort);
            radicalStroke.printRadicalStrokeOrder(fracBlanked);
            fracPrintln();
        }

        void fracPrintComment(String s, CEList ces) {
            int first = s.codePointAt(0);
            String name = Default.ucd().getName(first);
            if (s.length() > Character.charCount(first)) {
                name = name + " ...";
            }

            String scriptInfo = mapCodePoints(s, "/", FractionalUCA::cpToScript);
            String gcInfo = mapCodePoints(s, "/", FractionalUCA::cpToGeneralCategory);
            frac.print(
                    "\t# "
                            + scriptInfo
                            + " "
                            + gcInfo
                            + "\t"
                            + ces.toString(CEList.WeightStyle.NO_SPACES)
                            + "\t* "
                            + name);
            fracBlanked.print(
                    "\t# "
                            + scriptInfo
                            + " "
                            + gcInfo
                            + "\t"
                            + ces.toString(CEList.WeightStyle.BLANKED)
                            + "\t* "
                            + name);
        }

        void fracPrintSampleComment(String s, CEList ces) {
            frac.print(
                    "\t# "
                            + ces.toString(CEList.WeightStyle.NO_SPACES)
                            + "\t* "
                            + Default.ucd().getCodeAndName(s));
            fracBlanked.print(
                    "\t# "
                            + ces.toString(CEList.WeightStyle.BLANKED)
                            + "\t* "
                            + Default.ucd().getCodeAndName(s));
        }

        void fracPrintln(MinMaxFCE mm) {
            fracPrintln(mm, false);
        }

        void fracPrintln(MinMaxFCE mm, boolean showEmpty) {
            String withWeights = mm.toString(showEmpty, MinMaxFCE.WeightStyle.DEFAULT);
            frac.println(withWeights);
            fracShort.println(withWeights);
            fracBlanked.println(mm.toString(showEmpty, MinMaxFCE.WeightStyle.BLANKED));
        }
    }

    static void writeFractionalUCA() throws IOException {
        final UCA uca = getCollator();
        final PrimariesToFractional ps2f = new PrimariesToFractional(uca);
        final SortedSet<MappingWithSortKey> ordered =
                new MappingsForFractionalUCA(uca).getMappings();
        secondaryAndTertiaryWeightsToFractional(ordered, ps2f);
        final StringBuilder topByteInfo = new StringBuilder();
        ps2f.assignFractionalPrimaries(topByteInfo);

        final FractionalUCA.HighByteToReorderingToken highByteToScripts =
                new FractionalUCA.HighByteToReorderingToken(ps2f);
        final Map<Integer, String> reorderingTokenOverrides = new TreeMap<Integer, String>();

        // now translate!!

        // add special reordering tokens
        for (int reorderCode = ReorderCodes.FIRST;
                reorderCode < ReorderCodes.LIMIT;
                ++reorderCode) {
            final int nextCode =
                    reorderCode == ReorderCodes.DIGIT ? UCD_Types.LATIN_SCRIPT : reorderCode + 1;
            final int start = Fractional.getLeadByte(ps2f.getFirstFractionalPrimary(reorderCode));
            final int limit = Fractional.getLeadByte(ps2f.getFirstFractionalPrimary(nextCode));
            final String token = ReorderCodes.getNameForSpecial(reorderCode);
            for (int i = start; i < limit; ++i) {
                reorderingTokenOverrides.put(i, token);
            }
        }

        Utility.fixDot();
        System.out.println("Writing");
        Output output = new Output();
        final FractionalStatistics fractionalStatistics = new FractionalStatistics(ps2f, output);

        output.summary.println("# Summary of Fractional UCA Table, generated from the UCA DUCET");
        WriteCollationData.writeVersionAndDate(output.summary, "FractionalUCA_summary.txt", true);
        output.summary.println("# Primary Ranges");

        final OldEquivalenceClass secEq = new OldEquivalenceClass("\n#", 2, true);
        final OldEquivalenceClass terEq = new OldEquivalenceClass("\n#", 2, true);
        final String[] sampleEq = new String[0x200];
        final int[] sampleLen = new int[0x200];

        int oldFirstPrimary = 0;

        output.fracPrintln("# Fractional UCA Table, generated from the UCA DUCET");
        output.fracPrintln("# " + WriteCollationData.getNormalDate());
        output.fracPrintln(
                "# VERSION: UCA="
                        + getCollator().getDataVersion()
                        + ", UCD="
                        + getCollator().getUCDVersion());
        output.fracPrintln("# © 2016 and later: Unicode, Inc. and others.");
        output.fracPrintln("# License & terms of use: http://www.unicode.org/copyright.html");
        output.fracPrintln("# For a description of the format and usage, see");
        output.fracPrintln("#   http://www.unicode.org/reports/tr35/tr35-collation.html");
        output.fracBlanked.print(
                "\n"
                        + "# This file shows “blanked weights” for most non-zero collation weights.\n"
                        + "# It is useful for simple diffing between versions of the data, showing\n"
                        + "# changes in the sort order and in the number of bytes in fractional weights.\n");
        output.fracPrintln();
        output.fracPrintln("[UCA version = " + getCollator().getDataVersion() + "]");

        printUnifiedIdeographRanges(output);
        output.fracPrintln();
        RadicalStroke radicalStroke = new RadicalStroke(getCollator().getUCDVersion());
        output.printRadicalStrokeOrder(radicalStroke);

        try (PrintWriter unihanIndexWriter =
                Utility.openPrintWriter(
                        output.directory, "unihan-index.txt", Utility.UTF8_WINDOWS)) {
            radicalStroke.printUnihanIndex(unihanIndexWriter);
        }

        // Print the [top_byte] information before any of the mappings
        // so that parsers can use this data while working with the fractional primary weights,
        // in particular the COMPRESS bits.
        output.fracPrintln("# Top Byte => Reordering Tokens");
        output.fracPrint(topByteInfo);
        output.fracPrintln();

        String lastChr = "";
        int lastNp = 0;

        final FractionalUCA.MinMaxFCE firstTertiaryIgnorable =
                new FractionalUCA.MinMaxFCE(false, "tertiary ignorable");
        final FractionalUCA.MinMaxFCE lastTertiaryIgnorable =
                new FractionalUCA.MinMaxFCE(true, "tertiary ignorable");

        final FractionalUCA.MinMaxFCE firstSecondaryIgnorable =
                new FractionalUCA.MinMaxFCE(false, "secondary ignorable");
        final FractionalUCA.MinMaxFCE lastSecondaryIgnorable =
                new FractionalUCA.MinMaxFCE(true, "secondary ignorable");

        final FractionalUCA.MinMaxFCE firstTertiaryInSecondaryNonIgnorable =
                new FractionalUCA.MinMaxFCE(false, "tertiary in secondary non-ignorable");
        final FractionalUCA.MinMaxFCE lastTertiaryInSecondaryNonIgnorable =
                new FractionalUCA.MinMaxFCE(true, "tertiary in secondary non-ignorable");

        final FractionalUCA.MinMaxFCE firstPrimaryIgnorable =
                new FractionalUCA.MinMaxFCE(false, "primary ignorable");
        final FractionalUCA.MinMaxFCE lastPrimaryIgnorable =
                new FractionalUCA.MinMaxFCE(true, "primary ignorable");

        final FractionalUCA.MinMaxFCE firstSecondaryInPrimaryNonIgnorable =
                new FractionalUCA.MinMaxFCE(false, "secondary in primary non-ignorable");
        final FractionalUCA.MinMaxFCE lastSecondaryInPrimaryNonIgnorable =
                new FractionalUCA.MinMaxFCE(true, "secondary in primary non-ignorable");

        final FractionalUCA.MinMaxFCE firstVariable =
                new FractionalUCA.MinMaxFCE(false, "variable");
        final FractionalUCA.MinMaxFCE lastVariable = new FractionalUCA.MinMaxFCE(true, "variable");

        final FractionalUCA.MinMaxFCE firstNonIgnorable =
                new FractionalUCA.MinMaxFCE(false, "regular");
        final FractionalUCA.MinMaxFCE lastNonIgnorable =
                new FractionalUCA.MinMaxFCE(true, "regular");

        final FractionalUCA.MinMaxFCE firstImplicitFCE =
                new FractionalUCA.MinMaxFCE(false, "implicit");
        final FractionalUCA.MinMaxFCE lastImplicitFCE =
                new FractionalUCA.MinMaxFCE(true, "implicit");

        final FractionalUCA.MinMaxFCE firstTrailing =
                new FractionalUCA.MinMaxFCE(false, "trailing");
        final FractionalUCA.MinMaxFCE lastTrailing = new FractionalUCA.MinMaxFCE(true, "trailing");

        final Map<Integer, Pair> fractBackMap = new TreeMap<Integer, Pair>();

        // int topVariable = -1;

        for (final MappingWithSortKey mapping : ordered) {
            final String chr = mapping.getString();
            final CEList ces = mapping.getCEs();
            final CEList originalCEs = mapping.getOriginalCEs();
            PrimaryToFractional props = null;

            if (!ces.isEmpty()) {
                final int firstPrimary = CEList.getPrimary(ces.at(0));
                // String message = null;
                if (firstPrimary != oldFirstPrimary) {
                    output.fracPrintln();
                    oldFirstPrimary = firstPrimary;
                }

                props = ps2f.getProps(firstPrimary);
                final int firstFractional = props.getAndResetScriptFirstFractionalPrimary();
                if (firstFractional != 0) {
                    final int reorderCode = props.getReorderCode();
                    int reservedFirstFractional = props.getReservedBeforeFractionalPrimary();
                    if (reservedFirstFractional != 0) {
                        fractionalStatistics.printAndRecordScriptFirstPrimary(
                                true,
                                props.getReservedBeforeReorderCode(),
                                true,
                                reservedFirstFractional);
                    }

                    fractionalStatistics.printAndRecordScriptFirstPrimary(
                            true, reorderCode, props.beginsByte(), firstFractional);

                    // Record script-first primaries with the scripts of their sample characters.
                    final String sampleChar = ReorderCodes.getSampleCharacter(reorderCode);
                    highByteToScripts.addScriptsIn(firstFractional, sampleChar);

                    // Print script aliases that have distinct sample characters.
                    if (reorderCode == UCD_Types.Meroitic_Cursive) {
                        // Mero = Merc
                        fractionalStatistics.printAndRecordScriptFirstPrimary(
                                true, UCD_Types.Meroitic_Hieroglyphs, false, firstFractional);
                    } else if (reorderCode == UCD_Types.HIRAGANA_SCRIPT) {
                        // Kana = Hrkt = Hira
                        fractionalStatistics.printAndRecordScriptFirstPrimary(
                                true, UCD_Types.KATAKANA_SCRIPT, false, firstFractional);
                        // Note: Hrkt = Hira but there is no sample character for Hrkt
                        // in CLDR scriptMetadata.txt.
                    }
                    // Note: Hans = Hant = Hani but we do not print Hans/Hant here because
                    // they have the same script sample character as Hani,
                    // and we cannot write multiple mappings for the same string.

                    if (reorderCode == ReorderCodes.DIGIT) {
                        fractionalStatistics.printAndRecord(
                                true,
                                "\uFDD04",
                                ps2f.getNumericFractionalPrimary(),
                                0x500,
                                5,
                                "# lead byte for numeric sorting");
                        output.fracPrintln();
                        highByteToScripts.addScriptsIn(ps2f.getNumericFractionalPrimary(), "4");
                    }
                }
            }

            if (mapping.hasPrefix()) {
                output.fracPrint(Utility.hex(mapping.getPrefix()) + " | ");
            }
            output.fracPrint(Utility.hex(chr) + "; ");

            // In order to support continuation CEs (as for implicit primaries),
            // we need a flag for the first variable-length CE rather than just testing q==0.
            boolean isFirst = true;

            for (int q = 0; ; ++q) {
                if (q == ces.length()) {
                    if (q == 0) {
                        // chr maps to nothing.
                        output.fracPrint("[,,]");
                    }
                    break;
                }

                final int ce = ces.at(q);
                final int pri = CEList.getPrimary(ce);
                final int sec = CEList.getSecondary(ce);
                final int ter = CEList.getTertiary(ce);

                if (sec != 0x20) {
                    /* boolean changed = */ secEq.add(new Integer(sec), new Integer(pri));
                }
                if (ter != 0x2) {
                    /* boolean changed = */ terEq.add(
                            new Integer(ter), new Integer((pri << 16) | sec));
                }

                if (sampleEq[sec] == null || sampleLen[sec] > originalCEs.length()) {
                    sampleEq[sec] = chr;
                    sampleLen[sec] = originalCEs.length();
                }
                if (sampleEq[ter] == null || sampleLen[ter] > originalCEs.length()) {
                    sampleEq[ter] = chr;
                    sampleLen[ter] = originalCEs.length();
                }

                // props was fetched for the firstPrimary before the loop.
                if (q != 0) {
                    props = ps2f.getProps(pri);
                }
                int np = props.getFractionalPrimary();
                int implicitCodePoint = -1;

                // special treatment for implicit weights
                if (Implicit.isImplicitLeadPrimary(pri)) {
                    if (DEBUG) {
                        System.out.println(
                                "DEBUG: "
                                        + ces
                                        + ", Current: "
                                        + q
                                        + ", "
                                        + Default.ucd().getCodeAndName(chr));
                    }
                    ++q;

                    final int pri2 = CEList.getPrimary(ces.at(q));
                    implicitCodePoint = uca.implicit.codePointForPrimaryPair(pri, pri2);
                    if (DEBUG) {
                        System.out.println(
                                "Implicit weight to code point: "
                                        + Utility.hex(pri)
                                        + ", "
                                        + Utility.hex(pri2)
                                        + " => "
                                        + Utility.hex(implicitCodePoint));
                    }

                    if (pri < Implicit.CJK_BASE) {
                        // siniform ideograph:
                        // There is only one props object for the whole range.
                        // Look up the fractional primary for the specific code point and
                        // continue like for regular primaries.
                        np = props.getSiniformRangeFractionalPrimary(implicitCodePoint);
                        implicitCodePoint = -1;
                    }
                } else {
                    // pri is a non-implicit UCA primary.
                    if (isFirst) { // only look at first one
                        highByteToScripts.addScriptsIn(np, chr);
                    }

                    if (pri == 0) {
                        if (chr.equals("\u01C6")) {
                            System.out.println("At dz-caron");
                        }
                        final Integer key = new Integer(ce);
                        final Pair value = fractBackMap.get(key);
                        if (value == null
                                || (ces.length() < ((Integer) (value.first)).intValue())) {
                            fractBackMap.put(key, new Pair(ces.length(), chr));
                        }
                    }
                }

                final int secTer = getFractionalSecAndTer(props, sec, ter);
                final int ns = secTer >>> 16;
                final int nt = secTer & 0xffff;

                if (implicitCodePoint >= 0) {
                    // Han implicit or unassigned implicit
                    fractionalStatistics.printAndRecordCodePoint(
                            false, chr, implicitCodePoint, ns, nt, null);
                } else {
                    // TODO: add the prefix to the stats?
                    fractionalStatistics.printAndRecord(false, chr, np, ns, nt, null);

                    // RECORD STATS
                    // but ONLY if we are not part of an implicit

                    if (np != 0) {
                        firstSecondaryInPrimaryNonIgnorable.setValue(0, ns, 0, chr);
                        lastSecondaryInPrimaryNonIgnorable.setValue(0, ns, 0, chr);
                    }
                    if (ns != 0) {
                        firstTertiaryInSecondaryNonIgnorable.setValue(0, 0, nt & 0x3F, chr);
                        lastTertiaryInSecondaryNonIgnorable.setValue(0, 0, nt & 0x3F, chr);
                    }
                    if (np == 0 && ns == 0) {
                        firstSecondaryIgnorable.setValue(np, ns, nt, chr);
                        lastSecondaryIgnorable.setValue(np, ns, nt, chr);
                    } else if (np == 0) {
                        firstPrimaryIgnorable.setValue(np, ns, nt, chr);
                        lastPrimaryIgnorable.setValue(np, ns, nt, chr);
                    } else if (getCollator().isVariable(ce)) {
                        firstVariable.setValue(np, ns, nt, chr);
                        lastVariable.setValue(np, ns, nt, chr);
                    } else if (pri >= Implicit.LIMIT) { // Trailing (none currently)
                        System.out.println(
                                "Trailing: "
                                        + Default.ucd().getCodeAndName(chr)
                                        + ", "
                                        + CEList.toString(ce)
                                        + ", "
                                        + Utility.hex(pri)
                                        + ", "
                                        + Utility.hex(Implicit.LIMIT));
                        firstTrailing.setValue(np, ns, nt, chr);
                        lastTrailing.setValue(np, ns, nt, chr);
                    } else {
                        firstNonIgnorable.setValue(np, ns, nt, chr);
                        lastNonIgnorable.setValue(np, ns, nt, chr);
                    }
                }

                if (isFirst) {
                    if (!FractionalUCA.sameTopByte(np, lastNp)) {
                        if (lastNp != 0) {
                            showRange("Last", output.summary, lastChr, lastNp);
                        }
                        output.summary.println();
                        showRange("First", output.summary, chr, np);
                    }
                    lastNp = np;
                    isFirst = false;
                }
            }
            output.fracPrintComment(chr, originalCEs);
            output.fracPrintln();
            lastChr = chr;
        }

        output.fracPrintln();
        fractionalStatistics.printAndRecord(
                true,
                "\uFDD1" + ReorderCodes.getSampleCharacter(UCD_Types.Unknown_Script), // Zzzz
                Fractional.IMPLICIT_MAX_BYTE << 16,
                5,
                5,
                "# unassigned first primary");
        output.fracPrintln();
        output.fracPrintln("# SPECIAL MAX/MIN COLLATION ELEMENTS");
        output.fracPrintln();

        fractionalStatistics.printAndRecord(
                true, "\uFFFE", 0x020000, 5, 5, "# Special LOWEST primary, for merge/interleaving");
        fractionalStatistics.printAndRecord(
                true, "\uFFFF", 0xEFFF00, 5, 5, "# Special HIGHEST primary, for ranges");
        lastTrailing.setValue(0xEFFF00, 5, 5, "\uFFFF");

        output.fracPrintln();

        // ADD HOMELESS COLLATION ELEMENTS
        output.fracPrintln();
        output.fracPrintln("# HOMELESS COLLATION ELEMENTS");

        final FakeString fakeString = new FakeString();
        final Iterator<Integer> it3 = fractBackMap.keySet().iterator();
        while (it3.hasNext()) {
            final Integer key = it3.next();
            final Pair pair = fractBackMap.get(key);
            if (((Integer) pair.first).intValue() < 2) {
                continue;
            }
            final String sample = (String) pair.second;

            final int ce = key.intValue();

            final PrimaryToFractional props = ps2f.getProps(CEList.getPrimary(ce));
            final int np = props.getFractionalPrimary();
            final int secTer =
                    getFractionalSecAndTer(props, CEList.getSecondary(ce), CEList.getTertiary(ce));
            final int ns = secTer >>> 16;
            final int nt = secTer & 0xffff;

            highByteToScripts.addScriptsIn(np, sample);
            fractionalStatistics.printAndRecord(true, fakeString.next(), np, ns, nt, null);

            output.fracPrintSampleComment(sample, getCollator().getCEList(sample, true));
            output.fracPrintln();
        }

        // Since the UCA doesn't have secondary ignorables, fake them.
        final int fakeTertiary = (Fractional.FIRST_IGNORABLE_TER_ASSIGNED << 8) | 2;
        if (firstSecondaryIgnorable.isUnset()) {
            System.out.println(
                    "No first/last secondary ignorable: resetting to HARD CODED, adding homeless");
            // long bound = lastTertiaryInSecondaryNonIgnorable.getValue(2);
            firstSecondaryIgnorable.setValue(0, 0, fakeTertiary, "");
            lastSecondaryIgnorable.setValue(0, 0, fakeTertiary, "");
            System.out.println(firstSecondaryIgnorable.formatFCE());
            // also add homeless
            fractionalStatistics.printAndRecord(true, fakeString.next(), 0, 0, fakeTertiary, null);

            output.fracPrintln("\t# CONSTRUCTED FAKE SECONDARY-IGNORABLE");
        }

        final int firstImplicit = Fractional.IMPLICIT_BASE_BYTE; // was
        // FractionalUCA.getImplicitPrimary(UCD_Types.CJK_BASE);
        final int lastImplicit =
                Fractional.IMPLICIT_MAX_BYTE; // was FractionalUCA.getImplicitPrimary(0x10FFFD);

        output.fracPrintln();
        output.fracPrintln("# VALUES BASED ON UCA");

        if (firstTertiaryIgnorable.isUnset()) {
            firstTertiaryIgnorable.setValue(0, 0, 0, "");
            lastTertiaryIgnorable.setValue(0, 0, 0, "");
            System.out.println(firstSecondaryIgnorable.formatFCE());
        }

        output.fracPrintln(firstTertiaryIgnorable);
        output.fracPrintln(lastTertiaryIgnorable);

        output.fracPrintln("# Warning: Case bits are masked in the following");

        output.fracPrintln(firstTertiaryInSecondaryNonIgnorable, true);
        output.fracPrintln(lastTertiaryInSecondaryNonIgnorable, true);

        output.fracPrintln(firstSecondaryIgnorable);
        output.fracPrintln(lastSecondaryIgnorable);

        if (lastTertiaryInSecondaryNonIgnorable.getValue(2)
                >= firstSecondaryIgnorable.getValue(2)) {
            output.fracPrintln("# FAILURE: Overlap of tertiaries");
        }

        output.fracPrintln(firstSecondaryInPrimaryNonIgnorable, true);
        output.fracPrintln(lastSecondaryInPrimaryNonIgnorable, true);

        output.fracPrintln(firstPrimaryIgnorable);
        output.fracPrintln(lastPrimaryIgnorable);

        if (lastSecondaryInPrimaryNonIgnorable.getValue(1) >= firstPrimaryIgnorable.getValue(1)) {
            output.fracPrintln("# FAILURE: Overlap of secondaries");
        }

        output.fracPrintln(firstVariable);
        output.fracPrintln(lastVariable);

        final long firstSymbolPrimary =
                MinMaxFCE.fixWeight(ps2f.getFirstFractionalPrimary(ReorderCodes.SYMBOL));
        long varTop = (firstSymbolPrimary & 0xff000000) - 1;
        output.frac.println("[variable top = " + Fractional.hexBytes(varTop) + "]");
        output.fracShort.println("[variable top = " + Fractional.hexBytes(varTop) + "]");
        output.fracBlanked.println(
                "[variable top = " + Fractional.blankedHexBytes(varTop, "pp", null) + "]");

        output.fracPrintln(firstNonIgnorable);
        output.fracPrintln(lastNonIgnorable);

        firstImplicitFCE.setValue(firstImplicit, Fractional.COMMON_SEC, Fractional.COMMON_TER, "");
        lastImplicitFCE.setValue(lastImplicit, Fractional.COMMON_SEC, Fractional.COMMON_TER, "");

        output.fracPrintln(firstImplicitFCE);
        output.fracPrintln(lastImplicitFCE);

        if (firstTrailing.isUnset()) {
            System.out.println("No first/last trailing: resetting");
            firstTrailing.setValue(
                    Fractional.IMPLICIT_MAX_BYTE + 1,
                    Fractional.COMMON_SEC,
                    Fractional.COMMON_TER,
                    "");
            lastTrailing.setValue(
                    Fractional.IMPLICIT_MAX_BYTE + 1,
                    Fractional.COMMON_SEC,
                    Fractional.COMMON_TER,
                    "");
            System.out.println(firstTrailing.formatFCE());
        }

        output.fracPrintln(firstTrailing);
        output.fracPrintln(lastTrailing);
        output.fracPrintln();

        highByteToScripts.cleanup(reorderingTokenOverrides);

        output.fracPrintln("# Reordering Tokens => Top Bytes");
        output.fracPrintln(highByteToScripts.getInfo(true));
        output.fracPrintln();

        output.fracPrintln("# General Categories => Top Byte");
        output.fracPrintln(highByteToScripts.getInfo(false));
        output.fracPrintln();

        output.fracPrintln();
        output.fracPrintln("# FIXED VALUES");

        output.fracPrintln(
                "[fixed first implicit byte "
                        + Utility.hex(Fractional.IMPLICIT_BASE_BYTE, 2)
                        + "]");
        output.fracPrintln(
                "[fixed last implicit byte " + Utility.hex(Fractional.IMPLICIT_MAX_BYTE, 2) + "]");
        output.fracPrintln(
                "[fixed first trail byte "
                        + Utility.hex(Fractional.IMPLICIT_MAX_BYTE + 1, 2)
                        + "]");
        output.fracPrintln(
                "[fixed last trail byte " + Utility.hex(Fractional.SPECIAL_BASE - 1, 2) + "]");
        output.fracPrintln(
                "[fixed first special byte " + Utility.hex(Fractional.SPECIAL_BASE, 2) + "]");
        output.fracPrintln("[fixed last special byte " + Utility.hex(0xFF, 2) + "]");
        output.fracPrintln();
        output.fracPrintln(
                "[fixed secondary common byte " + Utility.hex(Fractional.COMMON_SEC, 2) + "]");
        output.fracPrintln(
                "[fixed last secondary common byte "
                        + Utility.hex(Fractional.COMMON_SEC_TOP, 2)
                        + "]");
        output.fracPrintln(
                "[fixed first ignorable secondary byte "
                        + Utility.hex(Fractional.FIRST_IGNORABLE_SEC, 2)
                        + "]");
        output.fracPrintln();
        output.fracPrintln(
                "[fixed tertiary common byte " + Utility.hex(Fractional.COMMON_TER, 2) + "]");
        output.fracPrintln(
                "[fixed first ignorable tertiary byte "
                        + Utility.hex(Fractional.FIRST_IGNORABLE_TER, 2)
                        + "]");

        showRange("Last", output.summary, lastChr, lastNp);
        // output.summary.println("Last:  " + Utility.hex(lastNp) + ", " +
        // WriteCollationData.ucd.getCodeAndName(UTF16.charAt(lastChr, 0)));

        /*
           String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
           for (int i = 0; i < sample.length(); ++i) {
               char ch = sample.charAt(i);
               log.println(Utility.hex(ch) + " => " + Utility.hex(fixHan(ch))
                       + "          " + ucd.getName(ch));
           }
        */
        output.summary.println();

        output.summary.println();
        output.summary.println("# Disjoint classes for Secondaries");
        output.summary.println("#" + secEq.toString());

        output.summary.println();
        output.summary.println("# Disjoint classes for Tertiaries");
        output.summary.println("#" + terEq.toString());

        output.summary.println();
        output.summary.println("# Example characters for each TERTIARY value");
        output.summary.println();
        // TODO: reenable printing of (FRAC)?
        // output.summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
        output.summary.println("# UCA : CODE [    UCA CE    ] Name");
        output.summary.println();

        for (int i = 0; i < sampleEq.length; ++i) {
            if (sampleEq[i] == null) {
                continue;
            }
            if (i == UCA_Types.NEUTRAL_SECONDARY) {
                output.summary.println();
                output.summary.println("# Example characters for each SECONDARY value");
                output.summary.println();
                // TODO: reenable printing of (FRAC)?
                // output.summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
                output.summary.println("# UCA : CODE [    UCA CE    ] Name");
                output.summary.println();
            }
            final CEList ces = getCollator().getCEList(sampleEq[i], true);
            // TODO: reenable printing of (FRAC)?
            // Now that we "fix" secondary and tertiary weights per primary weight
            // rather than with a constant distribution,
            // we would have to store the fractional weight with the sample,
            // near where we do the conversion.
            // However, it is also less meaningful because the samples for a particular UCA weight
            // do not correspond to the samples for a particular fractional weight any more.
            // It might make more sense to collect and print samples separately.
            // int newval = i < 0x20 ? FractionalUCA.fixTertiary(i,sampleEq[i]) :
            // FractionalUCA.fixSecondary(i);
            output.summary.print(
                    "# "
                            + Utility.hex(i)
                            + ": " // TODO: reenable printing of (FRAC)?  + "(" +
                            // Utility.hex(newval) + ") "
                            + Utility.hex(sampleEq[i])
                            + " ");
            for (int q = 0; q < ces.length(); ++q) {
                output.summary.print(CEList.toString(ces.at(q)));
            }
            output.summary.println(" " + Default.ucd().getName(sampleEq[i]));
        }
        fractionalStatistics.show(output.log);
        output.close();
    }

    private static void secondaryAndTertiaryWeightsToFractional(
            SortedSet<MappingWithSortKey> ordered, PrimariesToFractional ps2f) {
        // Collect all secondary weights per primary, and all tertiary weights per
        // primary+secondary.
        final List<SecTerToFractional> allSecTerToFractional = new LinkedList<SecTerToFractional>();
        for (final MappingWithSortKey mapping : ordered) {
            final CEList ces = mapping.getCEs();
            for (int i = 0; i < ces.length(); ++i) {
                final int ce = ces.at(i);
                final int pri = CEList.getPrimary(ce);
                final int sec = CEList.getSecondary(ce);
                final int ter = CEList.getTertiary(ce);
                final boolean isNeutralSecTer =
                        (sec == 0 || sec == UCA_Types.NEUTRAL_SECONDARY)
                                && (ter == 0 || ter == UCA_Types.NEUTRAL_TERTIARY);
                final PrimaryToFractional p2f = ps2f.getOrCreateProps(pri);
                if (isNeutralSecTer) {
                    assert p2f.neutralSec < 0 || sec == p2f.neutralSec;
                    assert p2f.neutralTer < 0 || ter == p2f.neutralTer;
                    p2f.neutralSec = sec;
                    p2f.neutralTer = ter;
                    if (p2f.secTerToFractional != null) {
                        p2f.secTerToFractional.addUCASecondaryAndTertiary(sec, ter);
                    }
                } else {
                    SecTerToFractional st2f = p2f.secTerToFractional;
                    if (st2f == null) {
                        st2f = p2f.secTerToFractional = new SecTerToFractional(pri == 0);
                        allSecTerToFractional.add(st2f);
                        if (p2f.neutralSec >= 0) {
                            st2f.addUCASecondaryAndTertiary(p2f.neutralSec, p2f.neutralTer);
                        }
                    }
                    st2f.addUCASecondaryAndTertiary(sec, ter);
                }
                if (Implicit.isImplicitLeadPrimary(pri)) {
                    // Skip trailing implicit-weight CEs. See
                    // http://www.unicode.org/reports/tr10/#Implicit_Weights
                    ++i;
                }
            }
        }

        // Assign fractional weights accordingly.
        for (final SecTerToFractional st2f : allSecTerToFractional) {
            st2f.assignFractionalWeights();
        }
    }

    /**
     * Converts the UCA secondary & tertiary weights to fractional weights. Returns an int with the
     * fractional secondary in the upper 16 bits and the fractional tertiary in the lower 16 bits.
     */
    private static int getFractionalSecAndTer(PrimaryToFractional p2f, int sec, int ter) {
        if (sec == 0) {
            if (ter == 0) {
                return 0;
            }
            if (ter == UCA_Types.NEUTRAL_TERTIARY) {
                return Fractional.COMMON_TER;
            }
        } else if (sec == UCA_Types.NEUTRAL_SECONDARY && ter == UCA_Types.NEUTRAL_TERTIARY) {
            return (Fractional.COMMON_SEC << 16) | Fractional.COMMON_TER;
        }
        int secTer = p2f.secTerToFractional.getFractionalSecAndTer(sec, ter);

        if (UCA_Types.uppercaseTertiaries.get(ter)) {
            if ((secTer & 0xff00) != 0) {
                // The tertiary lead byte is in bits 15..8, set the uppercase bit there too.
                secTer |= 0x8000;
            } else {
                secTer |= 0x80;
            }
        }
        return secTer;
    }

    private static class FakeString {
        char[] buffer = {'\uFDD0', '@'};

        String next() {
            buffer[1]++;
            return new String(buffer);
        }
    }

    private static UCA getCollator() {
        return WriteCollationData.getCollator(CollatorType.cldrWithoutFFFx);
    }

    private static String cpToScript(int codePoint) {
        return Default.ucd().getScriptID(codePoint, UCD_Types.SHORT);
    }

    private static String cpToGeneralCategory(int codePoint) {
        return Default.ucd().getCategoryID(codePoint, UCD_Types.SHORT);
    }

    private static String mapCodePoints(
            CharSequence s, CharSequence separator, IntFunction<String> fn) {
        return s.codePoints().mapToObj(fn).collect(Collectors.joining(separator));
    }

    private static void showRange(String title, PrintWriter summary, String lastChr, int lastNp) {
        final int ch = lastChr.codePointAt(0);
        summary.println(
                "#\t"
                        + title
                        + "\t"
                        + padHexBytes(lastNp)
                        + "\t"
                        + cpToScript(ch)
                        + "\t"
                        + cpToGeneralCategory(ch)
                        + "\t"
                        + Default.ucd().getCodeAndName(ch));
    }

    private static String padHexBytes(int lastNp) {
        final String result = Fractional.hexBytes(lastNp & 0xFFFFFFFFL);
        return result + Utility.repeat(" ", 11 - result.length());
        // E3 9B 5F C8
    }

    private static boolean sameTopByte(int x, int y) {
        int x1 = x & 0xFF0000;
        int y1 = y & 0xFF0000;
        if (x1 != 0 || y1 != 0) {
            return x1 == y1;
        }
        x1 = x & 0xFF00;
        y1 = y & 0xFF00;
        return x1 == y1;
    }

    //    static final boolean needsCaseBit(String x) {
    //        String s = Default.nfkd().normalize(x);
    //        if (!Default.ucd().getCase(s, FULL, LOWER).equals(s)) {
    //            return true;
    //        }
    //        if (!CaseBit.toSmallKana(s).equals(s)) {
    //            return true;
    //        }
    //        return false;
    //    }

    /**
     * Prints the Unified_Ideograph ranges in collation order. As a result, a parser need not have
     * this data available for the current Unicode version.
     */
    private static void printUnifiedIdeographRanges(Output output) {
        final UnicodeSet hanSet =
                ToolUnicodePropertySource.make(getCollator().getUCDVersion())
                        .getProperty("Unified_Ideograph")
                        .getSet("True");
        output.fracPrintln("# Unified_Ideograph: " + hanSet.size() + " characters");
        final UnicodeSet coreHanSet = (UnicodeSet) hanSet.clone();
        coreHanSet.retain(
                0x4e00, 0xffff); // BlockCJK_Unified_Ideograph or CJK_Compatibility_Ideographs
        final StringBuilder hanSB = new StringBuilder("[Unified_Ideograph");
        final UnicodeSetIterator hanIter = new UnicodeSetIterator(coreHanSet);
        while (hanIter.nextRange()) {
            final int start = hanIter.codepoint;
            final int end = hanIter.codepointEnd;
            hanSB.append(' ').append(Utility.hex(start));
            if (start < end) {
                hanSB.append("..").append(Utility.hex(end));
            }
        }
        hanSet.remove(0x4e00, 0xffff);
        hanIter.reset(hanSet);
        while (hanIter.nextRange()) {
            final int start = hanIter.codepoint;
            final int end = hanIter.codepointEnd;
            hanSB.append(' ').append(Utility.hex(start));
            if (start < end) {
                hanSB.append("..").append(Utility.hex(end));
            }
        }
        hanSB.append("]");
        output.fracPrintln(hanSB);
    }
}
