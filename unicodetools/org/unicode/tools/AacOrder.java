package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CandidateData;
import org.unicode.tools.emoji.CandidateData.Status;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.EmojiOrder;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.VersionInfo;

public class AacOrder {

    private static final VersionInfo VERSION = Emoji.VERSION11;
    private static final VersionInfo UCD_VERSION = Emoji.UCD10;

    private static final CandidateData CANDIDATE_DATA = CandidateData.getInstance();

    private static final EmojiDataSource EMOJI_DATA = new EmojiDataSourceCombined();
    private static final EmojiOrder ORDER = EmojiOrder.of(VERSION);

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(UCD_VERSION);
    static final UnicodeMap<String> names = iup.load(UcdProperty.Name);
    static final UnicodeMap<General_Category_Values> gencat = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    static final UnicodeSet DI = iup.loadEnum(UcdProperty.Default_Ignorable_Code_Point, Binary.class).getSet(Binary.Yes);
    static final UnicodeMap<Age_Values> AGE = iup.loadEnum(UcdProperty.Age, Age_Values.class);


    static final UnicodeSet EMOJI = new UnicodeSet();
    static {
        UnicodeSet temp = EMOJI_DATA.getAllEmojiWithoutDefectives();
        //        new UnicodeSet()
        //        .addAll(emojiData.getSingletonsWithoutDefectives())
        //        .addAll(emojiData.getZwjSequencesNormal())
        //        .addAll(emojiData.getFlagSequences())
        //        .addAll(emojiData.getModifierSequences())
        //        .addAll(emojiData.getKeycapSequences());
        for (String s : temp) {
            Status status = CANDIDATE_DATA.getStatus(s);
            if (status == Status.Draft_Candidate || status == Status.Provisional_Candidate) {
                continue;
            }
            if (s.contains(Emoji.EMOJI_VARIANT_STRING)) {
                s = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
            }
            EMOJI.add(s);
            System.out.println(Utility.hex(s) + "\t" + s);
        }
        EMOJI.freeze();
    }

    static final UnicodeSet ALLOWED = new UnicodeSet(0,0x10FFFF);
    static {
        //      + "[^[:c:][:z:][:di:][࿕-࿘ 卍 卐]]" + emoji_sequences
        for (General_Category_Values v : ImmutableSet.<General_Category_Values>builder()
                .addAll(PropertyValueSets.CONTROL)
                .addAll(PropertyValueSets.SEPARATOR)
                .build()) {
            ALLOWED.removeAll(gencat.getSet(v));
        }
        ALLOWED
        .removeAll(DI)
        .removeAll(new UnicodeSet("[࿕-࿘ 卍 卐]")) // special exceptions
        .addAll(EMOJI)
        .freeze();
    }

    static final Set<String> SORTED_ALL_CHARS_SET
    = EmojiOrder.sort(ORDER.codepointCompare, ALLOWED);

    /**
     * First arg is output directory.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        UnicodeSet Not10 = AGE.getSet(Age_Values.Unassigned);
        UnicodeSet Not9 = new UnicodeSet(Not10).addAll(AGE.getSet(Age_Values.V9_0)).freeze();

        UnicodeSet extra10 = new UnicodeSet();
        UnicodeSet extra9 = new UnicodeSet();

        if (SORTED_ALL_CHARS_SET.size() != ALLOWED.size()) {
            throw new IllegalArgumentException("Bad size");
        }
        String outputDir = args.length == 1 ? args[0] : Settings.UNICODE_DRAFT_DIRECTORY + "consortium/";
        try (PrintWriter outRanges = FileUtilities.openUTF8Writer(outputDir, "aac-order-ranges.txt");
                PrintWriter outEach = FileUtilities.openUTF8Writer(outputDir, "aac-order.txt")
                ) {
            outRanges.println("# Format: codepoint/range/string ; index ; name (if emoji)");
            outEach.println("# Format: codepoint/string ; name (if emoji)"
                    + "\n# Compute the index while reading the file:"
                    + "\n#  For each single codepoint or string, add one; "
                    + "\n#  For each range, add the number of items in the range");
            Range range = new Range(outRanges, true);
            Range rangeNone = new Range(outEach, false);
            for (String s : SORTED_ALL_CHARS_SET) {
                range.add(s);
                rangeNone.add(s);
                if (Not9.containsSome(s)) {
                    extra9.add(s);
                    if (Not10.contains(s)) {
                        extra10.add(s);
                    }
                }
            }
            range.flush();
            rangeNone.flush();
        }
        writeUs(outputDir, ALLOWED, "aac-order-us.txt");
        //        writeUs(outputDir, new UnicodeSet(ALLOWED)
        ////        .addAll(EMOJI_DATA.getKeycapSequencesAll())
        ////        .addAll(EMOJI_DATA.getZwjSequencesAll())
        //        , "aac-order-us-full.txt");
        // writeUs(outputDir, EMOJI_DATA.getAllEmojiWithoutDefectives(), "emoji-us.txt");
        //        writeUs(outputDir, extra10, "aac-extra-emoji-U10.txt");
        //        writeUs(outputDir, extra9, "aac-extra-emoji-U9.txt");
    }

    private static void writeUs(String outputDir, UnicodeSet unicodeSet, String filename) throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename)) {
            out.print("UnicodeSet EMOJI_ALLOWED = new UnicodeSet(");
            int totalCodePoints = 0;
            int totalStrings = 0;
            String separator = "";
            for (EntryRange entry : unicodeSet.ranges()) {
                out.print(separator);
                separator = ",";
                out.println("\n// (" + UTF16.valueOf(entry.codepoint) + ") " + getName(entry.codepoint) + 
                        (entry.codepointEnd == entry.codepoint ? "" : 
                            "\t..\t(" + UTF16.valueOf(entry.codepointEnd) + ") " + getName(entry.codepointEnd)));
                out.print("0x" + Integer.toHexString(entry.codepoint) + ",0x" + Integer.toHexString(entry.codepointEnd));
                totalCodePoints += (entry.codepointEnd - entry.codepoint) + 1;
            }
            out.println(")");
            for (String string : unicodeSet.strings()) {
                out.println("// (" + string + ") " + getName(string));
                out.println(".add(\"" + string + "\")");
                ++totalStrings;
            }
            out.println(".freeze();");
            out.println("// Total code points: " + totalCodePoints);
            out.println("// Total strings: " + totalStrings);
            out.println("// Total: " + (totalCodePoints + totalStrings));
        }
    }

    private static String getName(String string) {
        return EMOJI_DATA.getName(string);
    }

    private static String getName(int codepoint) {
        try {
            return getName(UTF16.valueOf(codepoint));
        } catch (Exception e) {
            return UCharacter.getExtendedName(codepoint);
        }
    }

    private static class Range {
        int first;
        int last = -2;
        int firstIndex;
        int currentIndex;
        private final boolean doIndexes;
        private final PrintWriter out;

        public Range(PrintWriter out, boolean doIndexes) {
            this.out = out;
            this.doIndexes = doIndexes;
        }
        public void flush() {
            if (last >= 0) {
                if (first == last) {
                    out.println(Utility.hex(first) 
                            + (doIndexes ? " ; " + firstIndex : "")
                            + (EMOJI.contains(first) ? "; \t" + getName(first) : ""));
                } else if (EMOJI.containsSome(first,last)) {
                    for (int cp = first; cp <= last; ++cp) {
                        out.println(Utility.hex(cp) 
                                + (doIndexes ? " ; " + firstIndex : "")
                                + "; \t" + getName(cp));
                        ++firstIndex;
                    }
                } else {
                    out.println(Utility.hex(first) + ".." + Utility.hex(last) 
                    + (doIndexes ? " ; " + firstIndex : ""));
                }
            }
            last = -2;
        }
        private String getName(String s) {
            String name = EMOJI_DATA.getName(s);
            return name != null ? name : UCharacter.getName(s,"+");
        }
        private String getName(int s) {
            String name = null;
            try {
                name = EMOJI_DATA.getName(UTF16.valueOf(s));
            } catch (Exception e) {
            }
            return name != null ? name : UCharacter.getName(s);
        }
        public void add(String s) {
            ++currentIndex;
            int current = s.codePointAt(0);
            if (UCharacter.charCount(current) != s.length()) {
                flush();
                out.println(Utility.hex(s) 
                        + (doIndexes ? " ; " + currentIndex : "")
                        + (EMOJI.contains(first) ? " ;\t" + getName(s) : ""));
            } else {
                if (current == last+1) {
                    last = current;
                } else {
                    flush();
                    first = last = current;
                    firstIndex = currentIndex;
                }
            }
        }
    }
    //
    //    static final UnicodeSet EMOJI_ALLOWED = new UnicodeSet(
    //            0x21,0x7e,
    //            0xa1,0xac,
    //            0xae,0x34e,
    //            0x350,0x377,
    //            0x37a,0x37f,
    //            0x384,0x38a,
    //            0x38c,0x38c,
    //            0x38e,0x3a1,
    //            0x3a3,0x52f,
    //            0x531,0x556,
    //            0x559,0x55f,
    //            0x561,0x587,
    //            0x589,0x58a,
    //            0x58d,0x58f,
    //            0x591,0x5c7,
    //            0x5d0,0x5ea,
    //            0x5f0,0x5f4,
    //            0x606,0x61b,
    //            0x61e,0x6dc,
    //            0x6de,0x70d,
    //            0x710,0x74a,
    //            0x74d,0x7b1,
    //            0x7c0,0x7fa,
    //            0x800,0x82d,
    //            0x830,0x83e,
    //            0x840,0x85b,
    //            0x85e,0x85e,
    //            0x8a0,0x8b4,
    //            0x8b6,0x8bd,
    //            0x8d4,0x8e1,
    //            0x8e3,0x983,
    //            0x985,0x98c,
    //            0x98f,0x990,
    //            0x993,0x9a8,
    //            0x9aa,0x9b0,
    //            0x9b2,0x9b2,
    //            0x9b6,0x9b9,
    //            0x9bc,0x9c4,
    //            0x9c7,0x9c8,
    //            0x9cb,0x9ce,
    //            0x9d7,0x9d7,
    //            0x9dc,0x9dd,
    //            0x9df,0x9e3,
    //            0x9e6,0x9fb,
    //            0xa01,0xa03,
    //            0xa05,0xa0a,
    //            0xa0f,0xa10,
    //            0xa13,0xa28,
    //            0xa2a,0xa30,
    //            0xa32,0xa33,
    //            0xa35,0xa36,
    //            0xa38,0xa39,
    //            0xa3c,0xa3c,
    //            0xa3e,0xa42,
    //            0xa47,0xa48,
    //            0xa4b,0xa4d,
    //            0xa51,0xa51,
    //            0xa59,0xa5c,
    //            0xa5e,0xa5e,
    //            0xa66,0xa75,
    //            0xa81,0xa83,
    //            0xa85,0xa8d,
    //            0xa8f,0xa91,
    //            0xa93,0xaa8,
    //            0xaaa,0xab0,
    //            0xab2,0xab3,
    //            0xab5,0xab9,
    //            0xabc,0xac5,
    //            0xac7,0xac9,
    //            0xacb,0xacd,
    //            0xad0,0xad0,
    //            0xae0,0xae3,
    //            0xae6,0xaf1,
    //            0xaf9,0xaf9,
    //            0xb01,0xb03,
    //            0xb05,0xb0c,
    //            0xb0f,0xb10,
    //            0xb13,0xb28,
    //            0xb2a,0xb30,
    //            0xb32,0xb33,
    //            0xb35,0xb39,
    //            0xb3c,0xb44,
    //            0xb47,0xb48,
    //            0xb4b,0xb4d,
    //            0xb56,0xb57,
    //            0xb5c,0xb5d,
    //            0xb5f,0xb63,
    //            0xb66,0xb77,
    //            0xb82,0xb83,
    //            0xb85,0xb8a,
    //            0xb8e,0xb90,
    //            0xb92,0xb95,
    //            0xb99,0xb9a,
    //            0xb9c,0xb9c,
    //            0xb9e,0xb9f,
    //            0xba3,0xba4,
    //            0xba8,0xbaa,
    //            0xbae,0xbb9,
    //            0xbbe,0xbc2,
    //            0xbc6,0xbc8,
    //            0xbca,0xbcd,
    //            0xbd0,0xbd0,
    //            0xbd7,0xbd7,
    //            0xbe6,0xbfa,
    //            0xc00,0xc03,
    //            0xc05,0xc0c,
    //            0xc0e,0xc10,
    //            0xc12,0xc28,
    //            0xc2a,0xc39,
    //            0xc3d,0xc44,
    //            0xc46,0xc48,
    //            0xc4a,0xc4d,
    //            0xc55,0xc56,
    //            0xc58,0xc5a,
    //            0xc60,0xc63,
    //            0xc66,0xc6f,
    //            0xc78,0xc83,
    //            0xc85,0xc8c,
    //            0xc8e,0xc90,
    //            0xc92,0xca8,
    //            0xcaa,0xcb3,
    //            0xcb5,0xcb9,
    //            0xcbc,0xcc4,
    //            0xcc6,0xcc8,
    //            0xcca,0xccd,
    //            0xcd5,0xcd6,
    //            0xcde,0xcde,
    //            0xce0,0xce3,
    //            0xce6,0xcef,
    //            0xcf1,0xcf2,
    //            0xd01,0xd03,
    //            0xd05,0xd0c,
    //            0xd0e,0xd10,
    //            0xd12,0xd3a,
    //            0xd3d,0xd44,
    //            0xd46,0xd48,
    //            0xd4a,0xd4f,
    //            0xd54,0xd63,
    //            0xd66,0xd7f,
    //            0xd82,0xd83,
    //            0xd85,0xd96,
    //            0xd9a,0xdb1,
    //            0xdb3,0xdbb,
    //            0xdbd,0xdbd,
    //            0xdc0,0xdc6,
    //            0xdca,0xdca,
    //            0xdcf,0xdd4,
    //            0xdd6,0xdd6,
    //            0xdd8,0xddf,
    //            0xde6,0xdef,
    //            0xdf2,0xdf4,
    //            0xe01,0xe3a,
    //            0xe3f,0xe5b,
    //            0xe81,0xe82,
    //            0xe84,0xe84,
    //            0xe87,0xe88,
    //            0xe8a,0xe8a,
    //            0xe8d,0xe8d,
    //            0xe94,0xe97,
    //            0xe99,0xe9f,
    //            0xea1,0xea3,
    //            0xea5,0xea5,
    //            0xea7,0xea7,
    //            0xeaa,0xeab,
    //            0xead,0xeb9,
    //            0xebb,0xebd,
    //            0xec0,0xec4,
    //            0xec6,0xec6,
    //            0xec8,0xecd,
    //            0xed0,0xed9,
    //            0xedc,0xedf,
    //            0xf00,0xf47,
    //            0xf49,0xf6c,
    //            0xf71,0xf97,
    //            0xf99,0xfbc,
    //            0xfbe,0xfcc,
    //            0xfce,0xfd4,
    //            0xfd9,0xfda,
    //            0x1000,0x10c5,
    //            0x10c7,0x10c7,
    //            0x10cd,0x10cd,
    //            0x10d0,0x115e,
    //            0x1161,0x1248,
    //            0x124a,0x124d,
    //            0x1250,0x1256,
    //            0x1258,0x1258,
    //            0x125a,0x125d,
    //            0x1260,0x1288,
    //            0x128a,0x128d,
    //            0x1290,0x12b0,
    //            0x12b2,0x12b5,
    //            0x12b8,0x12be,
    //            0x12c0,0x12c0,
    //            0x12c2,0x12c5,
    //            0x12c8,0x12d6,
    //            0x12d8,0x1310,
    //            0x1312,0x1315,
    //            0x1318,0x135a,
    //            0x135d,0x137c,
    //            0x1380,0x1399,
    //            0x13a0,0x13f5,
    //            0x13f8,0x13fd,
    //            0x1400,0x167f,
    //            0x1681,0x169c,
    //            0x16a0,0x16f8,
    //            0x1700,0x170c,
    //            0x170e,0x1714,
    //            0x1720,0x1736,
    //            0x1740,0x1753,
    //            0x1760,0x176c,
    //            0x176e,0x1770,
    //            0x1772,0x1773,
    //            0x1780,0x17b3,
    //            0x17b6,0x17dd,
    //            0x17e0,0x17e9,
    //            0x17f0,0x17f9,
    //            0x1800,0x180a,
    //            0x1810,0x1819,
    //            0x1820,0x1877,
    //            0x1880,0x18aa,
    //            0x18b0,0x18f5,
    //            0x1900,0x191e,
    //            0x1920,0x192b,
    //            0x1930,0x193b,
    //            0x1940,0x1940,
    //            0x1944,0x196d,
    //            0x1970,0x1974,
    //            0x1980,0x19ab,
    //            0x19b0,0x19c9,
    //            0x19d0,0x19da,
    //            0x19de,0x1a1b,
    //            0x1a1e,0x1a5e,
    //            0x1a60,0x1a7c,
    //            0x1a7f,0x1a89,
    //            0x1a90,0x1a99,
    //            0x1aa0,0x1aad,
    //            0x1ab0,0x1abe,
    //            0x1b00,0x1b4b,
    //            0x1b50,0x1b7c,
    //            0x1b80,0x1bf3,
    //            0x1bfc,0x1c37,
    //            0x1c3b,0x1c49,
    //            0x1c4d,0x1c88,
    //            0x1cc0,0x1cc7,
    //            0x1cd0,0x1cf6,
    //            0x1cf8,0x1cf9,
    //            0x1d00,0x1df5,
    //            0x1dfb,0x1f15,
    //            0x1f18,0x1f1d,
    //            0x1f20,0x1f45,
    //            0x1f48,0x1f4d,
    //            0x1f50,0x1f57,
    //            0x1f59,0x1f59,
    //            0x1f5b,0x1f5b,
    //            0x1f5d,0x1f5d,
    //            0x1f5f,0x1f7d,
    //            0x1f80,0x1fb4,
    //            0x1fb6,0x1fc4,
    //            0x1fc6,0x1fd3,
    //            0x1fd6,0x1fdb,
    //            0x1fdd,0x1fef,
    //            0x1ff2,0x1ff4,
    //            0x1ff6,0x1ffe,
    //            0x2010,0x2027,
    //            0x2030,0x205e,
    //            0x2070,0x2071,
    //            0x2074,0x208e,
    //            0x2090,0x209c,
    //            0x20a0,0x20be,
    //            0x20d0,0x20f0,
    //            0x2100,0x218b,
    //            0x2190,0x23fe,
    //            0x2400,0x2426,
    //            0x2440,0x244a,
    //            0x2460,0x2b73,
    //            0x2b76,0x2b95,
    //            0x2b98,0x2bb9,
    //            0x2bbd,0x2bc8,
    //            0x2bca,0x2bd1,
    //            0x2bec,0x2bef,
    //            0x2c00,0x2c2e,
    //            0x2c30,0x2c5e,
    //            0x2c60,0x2cf3,
    //            0x2cf9,0x2d25,
    //            0x2d27,0x2d27,
    //            0x2d2d,0x2d2d,
    //            0x2d30,0x2d67,
    //            0x2d6f,0x2d70,
    //            0x2d7f,0x2d96,
    //            0x2da0,0x2da6,
    //            0x2da8,0x2dae,
    //            0x2db0,0x2db6,
    //            0x2db8,0x2dbe,
    //            0x2dc0,0x2dc6,
    //            0x2dc8,0x2dce,
    //            0x2dd0,0x2dd6,
    //            0x2dd8,0x2dde,
    //            0x2de0,0x2e44,
    //            0x2e80,0x2e99,
    //            0x2e9b,0x2ef3,
    //            0x2f00,0x2fd5,
    //            0x2ff0,0x2ffb,
    //            0x3001,0x303f,
    //            0x3041,0x3096,
    //            0x3099,0x30ff,
    //            0x3105,0x312d,
    //            0x3131,0x3163,
    //            0x3165,0x318e,
    //            0x3190,0x31ba,
    //            0x31c0,0x31e3,
    //            0x31f0,0x321e,
    //            0x3220,0x32fe,
    //            0x3300,0x4db5,
    //            0x4dc0,0x534c,
    //            0x534e,0x534f,
    //            0x5351,0x9fd5,
    //            0xa000,0xa48c,
    //            0xa490,0xa4c6,
    //            0xa4d0,0xa62b,
    //            0xa640,0xa6f7,
    //            0xa700,0xa7ae,
    //            0xa7b0,0xa7b7,
    //            0xa7f7,0xa82b,
    //            0xa830,0xa839,
    //            0xa840,0xa877,
    //            0xa880,0xa8c5,
    //            0xa8ce,0xa8d9,
    //            0xa8e0,0xa8fd,
    //            0xa900,0xa953,
    //            0xa95f,0xa97c,
    //            0xa980,0xa9cd,
    //            0xa9cf,0xa9d9,
    //            0xa9de,0xa9fe,
    //            0xaa00,0xaa36,
    //            0xaa40,0xaa4d,
    //            0xaa50,0xaa59,
    //            0xaa5c,0xaac2,
    //            0xaadb,0xaaf6,
    //            0xab01,0xab06,
    //            0xab09,0xab0e,
    //            0xab11,0xab16,
    //            0xab20,0xab26,
    //            0xab28,0xab2e,
    //            0xab30,0xab65,
    //            0xab70,0xabed,
    //            0xabf0,0xabf9,
    //            0xac00,0xd7a3,
    //            0xd7b0,0xd7c6,
    //            0xd7cb,0xd7fb,
    //            0xf900,0xfa6d,
    //            0xfa70,0xfad9,
    //            0xfb00,0xfb06,
    //            0xfb13,0xfb17,
    //            0xfb1d,0xfb36,
    //            0xfb38,0xfb3c,
    //            0xfb3e,0xfb3e,
    //            0xfb40,0xfb41,
    //            0xfb43,0xfb44,
    //            0xfb46,0xfbc1,
    //            0xfbd3,0xfd3f,
    //            0xfd50,0xfd8f,
    //            0xfd92,0xfdc7,
    //            0xfdf0,0xfdfd,
    //            0xfe10,0xfe19,
    //            0xfe20,0xfe52,
    //            0xfe54,0xfe66,
    //            0xfe68,0xfe6b,
    //            0xfe70,0xfe74,
    //            0xfe76,0xfefc,
    //            0xff01,0xff9f,
    //            0xffa1,0xffbe,
    //            0xffc2,0xffc7,
    //            0xffca,0xffcf,
    //            0xffd2,0xffd7,
    //            0xffda,0xffdc,
    //            0xffe0,0xffe6,
    //            0xffe8,0xffee,
    //            0xfffc,0xfffd,
    //            0x10000,0x1000b,
    //            0x1000d,0x10026,
    //            0x10028,0x1003a,
    //            0x1003c,0x1003d,
    //            0x1003f,0x1004d,
    //            0x10050,0x1005d,
    //            0x10080,0x100fa,
    //            0x10100,0x10102,
    //            0x10107,0x10133,
    //            0x10137,0x1018e,
    //            0x10190,0x1019b,
    //            0x101a0,0x101a0,
    //            0x101d0,0x101fd,
    //            0x10280,0x1029c,
    //            0x102a0,0x102d0,
    //            0x102e0,0x102fb,
    //            0x10300,0x10323,
    //            0x10330,0x1034a,
    //            0x10350,0x1037a,
    //            0x10380,0x1039d,
    //            0x1039f,0x103c3,
    //            0x103c8,0x103d5,
    //            0x10400,0x1049d,
    //            0x104a0,0x104a9,
    //            0x104b0,0x104d3,
    //            0x104d8,0x104fb,
    //            0x10500,0x10527,
    //            0x10530,0x10563,
    //            0x1056f,0x1056f,
    //            0x10600,0x10736,
    //            0x10740,0x10755,
    //            0x10760,0x10767,
    //            0x10800,0x10805,
    //            0x10808,0x10808,
    //            0x1080a,0x10835,
    //            0x10837,0x10838,
    //            0x1083c,0x1083c,
    //            0x1083f,0x10855,
    //            0x10857,0x1089e,
    //            0x108a7,0x108af,
    //            0x108e0,0x108f2,
    //            0x108f4,0x108f5,
    //            0x108fb,0x1091b,
    //            0x1091f,0x10939,
    //            0x1093f,0x1093f,
    //            0x10980,0x109b7,
    //            0x109bc,0x109cf,
    //            0x109d2,0x10a03,
    //            0x10a05,0x10a06,
    //            0x10a0c,0x10a13,
    //            0x10a15,0x10a17,
    //            0x10a19,0x10a33,
    //            0x10a38,0x10a3a,
    //            0x10a3f,0x10a47,
    //            0x10a50,0x10a58,
    //            0x10a60,0x10a9f,
    //            0x10ac0,0x10ae6,
    //            0x10aeb,0x10af6,
    //            0x10b00,0x10b35,
    //            0x10b39,0x10b55,
    //            0x10b58,0x10b72,
    //            0x10b78,0x10b91,
    //            0x10b99,0x10b9c,
    //            0x10ba9,0x10baf,
    //            0x10c00,0x10c48,
    //            0x10c80,0x10cb2,
    //            0x10cc0,0x10cf2,
    //            0x10cfa,0x10cff,
    //            0x10e60,0x10e7e,
    //            0x11000,0x1104d,
    //            0x11052,0x1106f,
    //            0x1107f,0x110bc,
    //            0x110be,0x110c1,
    //            0x110d0,0x110e8,
    //            0x110f0,0x110f9,
    //            0x11100,0x11134,
    //            0x11136,0x11143,
    //            0x11150,0x11176,
    //            0x11180,0x111cd,
    //            0x111d0,0x111df,
    //            0x111e1,0x111f4,
    //            0x11200,0x11211,
    //            0x11213,0x1123e,
    //            0x11280,0x11286,
    //            0x11288,0x11288,
    //            0x1128a,0x1128d,
    //            0x1128f,0x1129d,
    //            0x1129f,0x112a9,
    //            0x112b0,0x112ea,
    //            0x112f0,0x112f9,
    //            0x11300,0x11303,
    //            0x11305,0x1130c,
    //            0x1130f,0x11310,
    //            0x11313,0x11328,
    //            0x1132a,0x11330,
    //            0x11332,0x11333,
    //            0x11335,0x11339,
    //            0x1133c,0x11344,
    //            0x11347,0x11348,
    //            0x1134b,0x1134d,
    //            0x11350,0x11350,
    //            0x11357,0x11357,
    //            0x1135d,0x11363,
    //            0x11366,0x1136c,
    //            0x11370,0x11374,
    //            0x11400,0x11459,
    //            0x1145b,0x1145b,
    //            0x1145d,0x1145d,
    //            0x11480,0x114c7,
    //            0x114d0,0x114d9,
    //            0x11580,0x115b5,
    //            0x115b8,0x115dd,
    //            0x11600,0x11644,
    //            0x11650,0x11659,
    //            0x11660,0x1166c,
    //            0x11680,0x116b7,
    //            0x116c0,0x116c9,
    //            0x11700,0x11719,
    //            0x1171d,0x1172b,
    //            0x11730,0x1173f,
    //            0x118a0,0x118f2,
    //            0x118ff,0x118ff,
    //            0x11ac0,0x11af8,
    //            0x11c00,0x11c08,
    //            0x11c0a,0x11c36,
    //            0x11c38,0x11c45,
    //            0x11c50,0x11c6c,
    //            0x11c70,0x11c8f,
    //            0x11c92,0x11ca7,
    //            0x11ca9,0x11cb6,
    //            0x12000,0x12399,
    //            0x12400,0x1246e,
    //            0x12470,0x12474,
    //            0x12480,0x12543,
    //            0x13000,0x1342e,
    //            0x14400,0x14646,
    //            0x16800,0x16a38,
    //            0x16a40,0x16a5e,
    //            0x16a60,0x16a69,
    //            0x16a6e,0x16a6f,
    //            0x16ad0,0x16aed,
    //            0x16af0,0x16af5,
    //            0x16b00,0x16b45,
    //            0x16b50,0x16b59,
    //            0x16b5b,0x16b61,
    //            0x16b63,0x16b77,
    //            0x16b7d,0x16b8f,
    //            0x16f00,0x16f44,
    //            0x16f50,0x16f7e,
    //            0x16f8f,0x16f9f,
    //            0x16fe0,0x16fe0,
    //            0x17000,0x187ec,
    //            0x18800,0x18af2,
    //            0x1b000,0x1b001,
    //            0x1bc00,0x1bc6a,
    //            0x1bc70,0x1bc7c,
    //            0x1bc80,0x1bc88,
    //            0x1bc90,0x1bc99,
    //            0x1bc9c,0x1bc9f,
    //            0x1d000,0x1d0f5,
    //            0x1d100,0x1d126,
    //            0x1d129,0x1d172,
    //            0x1d17b,0x1d1e8,
    //            0x1d200,0x1d245,
    //            0x1d300,0x1d356,
    //            0x1d360,0x1d371,
    //            0x1d400,0x1d454,
    //            0x1d456,0x1d49c,
    //            0x1d49e,0x1d49f,
    //            0x1d4a2,0x1d4a2,
    //            0x1d4a5,0x1d4a6,
    //            0x1d4a9,0x1d4ac,
    //            0x1d4ae,0x1d4b9,
    //            0x1d4bb,0x1d4bb,
    //            0x1d4bd,0x1d4c3,
    //            0x1d4c5,0x1d505,
    //            0x1d507,0x1d50a,
    //            0x1d50d,0x1d514,
    //            0x1d516,0x1d51c,
    //            0x1d51e,0x1d539,
    //            0x1d53b,0x1d53e,
    //            0x1d540,0x1d544,
    //            0x1d546,0x1d546,
    //            0x1d54a,0x1d550,
    //            0x1d552,0x1d6a5,
    //            0x1d6a8,0x1d7cb,
    //            0x1d7ce,0x1da8b,
    //            0x1da9b,0x1da9f,
    //            0x1daa1,0x1daaf,
    //            0x1e000,0x1e006,
    //            0x1e008,0x1e018,
    //            0x1e01b,0x1e021,
    //            0x1e023,0x1e024,
    //            0x1e026,0x1e02a,
    //            0x1e800,0x1e8c4,
    //            0x1e8c7,0x1e8d6,
    //            0x1e900,0x1e94a,
    //            0x1e950,0x1e959,
    //            0x1e95e,0x1e95f,
    //            0x1ee00,0x1ee03,
    //            0x1ee05,0x1ee1f,
    //            0x1ee21,0x1ee22,
    //            0x1ee24,0x1ee24,
    //            0x1ee27,0x1ee27,
    //            0x1ee29,0x1ee32,
    //            0x1ee34,0x1ee37,
    //            0x1ee39,0x1ee39,
    //            0x1ee3b,0x1ee3b,
    //            0x1ee42,0x1ee42,
    //            0x1ee47,0x1ee47,
    //            0x1ee49,0x1ee49,
    //            0x1ee4b,0x1ee4b,
    //            0x1ee4d,0x1ee4f,
    //            0x1ee51,0x1ee52,
    //            0x1ee54,0x1ee54,
    //            0x1ee57,0x1ee57,
    //            0x1ee59,0x1ee59,
    //            0x1ee5b,0x1ee5b,
    //            0x1ee5d,0x1ee5d,
    //            0x1ee5f,0x1ee5f,
    //            0x1ee61,0x1ee62,
    //            0x1ee64,0x1ee64,
    //            0x1ee67,0x1ee6a,
    //            0x1ee6c,0x1ee72,
    //            0x1ee74,0x1ee77,
    //            0x1ee79,0x1ee7c,
    //            0x1ee7e,0x1ee7e,
    //            0x1ee80,0x1ee89,
    //            0x1ee8b,0x1ee9b,
    //            0x1eea1,0x1eea3,
    //            0x1eea5,0x1eea9,
    //            0x1eeab,0x1eebb,
    //            0x1eef0,0x1eef1,
    //            0x1f000,0x1f02b,
    //            0x1f030,0x1f093,
    //            0x1f0a0,0x1f0ae,
    //            0x1f0b1,0x1f0bf,
    //            0x1f0c1,0x1f0cf,
    //            0x1f0d1,0x1f0f5,
    //            0x1f100,0x1f10c,
    //            0x1f110,0x1f12e,
    //            0x1f130,0x1f16b,
    //            0x1f170,0x1f1ac,
    //            0x1f1e6,0x1f202,
    //            0x1f210,0x1f23b,
    //            0x1f240,0x1f248,
    //            0x1f250,0x1f251,
    //            0x1f300,0x1f6d2,
    //            0x1f6e0,0x1f6ec,
    //            0x1f6f0,0x1f6f6,
    //            0x1f700,0x1f773,
    //            0x1f780,0x1f7d4,
    //            0x1f800,0x1f80b,
    //            0x1f810,0x1f847,
    //            0x1f850,0x1f859,
    //            0x1f860,0x1f887,
    //            0x1f890,0x1f8ad,
    //            0x1f910,0x1f91e,
    //            0x1f920,0x1f927,
    //            0x1f930,0x1f930,
    //            0x1f933,0x1f93e,
    //            0x1f940,0x1f94b,
    //            0x1f950,0x1f95e,
    //            0x1f980,0x1f991,
    //            0x1f9c0,0x1f9c0,
    //            0x20000,0x2a6d6,
    //            0x2a700,0x2b734,
    //            0x2b740,0x2b81d,
    //            0x2b820,0x2cea1,
    //            0x2f800,0x2fa1d)
    //    .add("#️⃣")
    //    .add("*️⃣")
    //    .add("0️⃣")
    //    .add("1️⃣")
    //    .add("2️⃣")
    //    .add("3️⃣")
    //    .add("4️⃣")
    //    .add("5️⃣")
    //    .add("6️⃣")
    //    .add("7️⃣")
    //    .add("8️⃣")
    //    .add("9️⃣")
    //    .add("☝🏻")
    //    .add("☝🏼")
    //    .add("☝🏽")
    //    .add("☝🏾")
    //    .add("☝🏿")
    //    .add("⛹🏻")
    //    .add("⛹🏼")
    //    .add("⛹🏽")
    //    .add("⛹🏾")
    //    .add("⛹🏿")
    //    .add("✊🏻")
    //    .add("✊🏼")
    //    .add("✊🏽")
    //    .add("✊🏾")
    //    .add("✊🏿")
    //    .add("✋🏻")
    //    .add("✋🏼")
    //    .add("✋🏽")
    //    .add("✋🏾")
    //    .add("✋🏿")
    //    .add("✌🏻")
    //    .add("✌🏼")
    //    .add("✌🏽")
    //    .add("✌🏾")
    //    .add("✌🏿")
    //    .add("✍🏻")
    //    .add("✍🏼")
    //    .add("✍🏽")
    //    .add("✍🏾")
    //    .add("✍🏿")
    //    .add("🇦🇨")
    //    .add("🇦🇩")
    //    .add("🇦🇪")
    //    .add("🇦🇫")
    //    .add("🇦🇬")
    //    .add("🇦🇮")
    //    .add("🇦🇱")
    //    .add("🇦🇲")
    //    .add("🇦🇴")
    //    .add("🇦🇶")
    //    .add("🇦🇷")
    //    .add("🇦🇸")
    //    .add("🇦🇹")
    //    .add("🇦🇺")
    //    .add("🇦🇼")
    //    .add("🇦🇽")
    //    .add("🇦🇿")
    //    .add("🇧🇦")
    //    .add("🇧🇧")
    //    .add("🇧🇩")
    //    .add("🇧🇪")
    //    .add("🇧🇫")
    //    .add("🇧🇬")
    //    .add("🇧🇭")
    //    .add("🇧🇮")
    //    .add("🇧🇯")
    //    .add("🇧🇱")
    //    .add("🇧🇲")
    //    .add("🇧🇳")
    //    .add("🇧🇴")
    //    .add("🇧🇶")
    //    .add("🇧🇷")
    //    .add("🇧🇸")
    //    .add("🇧🇹")
    //    .add("🇧🇻")
    //    .add("🇧🇼")
    //    .add("🇧🇾")
    //    .add("🇧🇿")
    //    .add("🇨🇦")
    //    .add("🇨🇨")
    //    .add("🇨🇩")
    //    .add("🇨🇫")
    //    .add("🇨🇬")
    //    .add("🇨🇭")
    //    .add("🇨🇮")
    //    .add("🇨🇰")
    //    .add("🇨🇱")
    //    .add("🇨🇲")
    //    .add("🇨🇳")
    //    .add("🇨🇴")
    //    .add("🇨🇵")
    //    .add("🇨🇷")
    //    .add("🇨🇺")
    //    .add("🇨🇻")
    //    .add("🇨🇼")
    //    .add("🇨🇽")
    //    .add("🇨🇾")
    //    .add("🇨🇿")
    //    .add("🇩🇪")
    //    .add("🇩🇬")
    //    .add("🇩🇯")
    //    .add("🇩🇰")
    //    .add("🇩🇲")
    //    .add("🇩🇴")
    //    .add("🇩🇿")
    //    .add("🇪🇦")
    //    .add("🇪🇨")
    //    .add("🇪🇪")
    //    .add("🇪🇬")
    //    .add("🇪🇭")
    //    .add("🇪🇷")
    //    .add("🇪🇸")
    //    .add("🇪🇹")
    //    .add("🇪🇺")
    //    .add("🇫🇮")
    //    .add("🇫🇯")
    //    .add("🇫🇰")
    //    .add("🇫🇲")
    //    .add("🇫🇴")
    //    .add("🇫🇷")
    //    .add("🇬🇦")
    //    .add("🇬🇧")
    //    .add("🇬🇩")
    //    .add("🇬🇪")
    //    .add("🇬🇫")
    //    .add("🇬🇬")
    //    .add("🇬🇭")
    //    .add("🇬🇮")
    //    .add("🇬🇱")
    //    .add("🇬🇲")
    //    .add("🇬🇳")
    //    .add("🇬🇵")
    //    .add("🇬🇶")
    //    .add("🇬🇷")
    //    .add("🇬🇸")
    //    .add("🇬🇹")
    //    .add("🇬🇺")
    //    .add("🇬🇼")
    //    .add("🇬🇾")
    //    .add("🇭🇰")
    //    .add("🇭🇲")
    //    .add("🇭🇳")
    //    .add("🇭🇷")
    //    .add("🇭🇹")
    //    .add("🇭🇺")
    //    .add("🇮🇨")
    //    .add("🇮🇩")
    //    .add("🇮🇪")
    //    .add("🇮🇱")
    //    .add("🇮🇲")
    //    .add("🇮🇳")
    //    .add("🇮🇴")
    //    .add("🇮🇶")
    //    .add("🇮🇷")
    //    .add("🇮🇸")
    //    .add("🇮🇹")
    //    .add("🇯🇪")
    //    .add("🇯🇲")
    //    .add("🇯🇴")
    //    .add("🇯🇵")
    //    .add("🇰🇪")
    //    .add("🇰🇬")
    //    .add("🇰🇭")
    //    .add("🇰🇮")
    //    .add("🇰🇲")
    //    .add("🇰🇳")
    //    .add("🇰🇵")
    //    .add("🇰🇷")
    //    .add("🇰🇼")
    //    .add("🇰🇾")
    //    .add("🇰🇿")
    //    .add("🇱🇦")
    //    .add("🇱🇧")
    //    .add("🇱🇨")
    //    .add("🇱🇮")
    //    .add("🇱🇰")
    //    .add("🇱🇷")
    //    .add("🇱🇸")
    //    .add("🇱🇹")
    //    .add("🇱🇺")
    //    .add("🇱🇻")
    //    .add("🇱🇾")
    //    .add("🇲🇦")
    //    .add("🇲🇨")
    //    .add("🇲🇩")
    //    .add("🇲🇪")
    //    .add("🇲🇫")
    //    .add("🇲🇬")
    //    .add("🇲🇭")
    //    .add("🇲🇰")
    //    .add("🇲🇱")
    //    .add("🇲🇲")
    //    .add("🇲🇳")
    //    .add("🇲🇴")
    //    .add("🇲🇵")
    //    .add("🇲🇶")
    //    .add("🇲🇷")
    //    .add("🇲🇸")
    //    .add("🇲🇹")
    //    .add("🇲🇺")
    //    .add("🇲🇻")
    //    .add("🇲🇼")
    //    .add("🇲🇽")
    //    .add("🇲🇾")
    //    .add("🇲🇿")
    //    .add("🇳🇦")
    //    .add("🇳🇨")
    //    .add("🇳🇪")
    //    .add("🇳🇫")
    //    .add("🇳🇬")
    //    .add("🇳🇮")
    //    .add("🇳🇱")
    //    .add("🇳🇴")
    //    .add("🇳🇵")
    //    .add("🇳🇷")
    //    .add("🇳🇺")
    //    .add("🇳🇿")
    //    .add("🇴🇲")
    //    .add("🇵🇦")
    //    .add("🇵🇪")
    //    .add("🇵🇫")
    //    .add("🇵🇬")
    //    .add("🇵🇭")
    //    .add("🇵🇰")
    //    .add("🇵🇱")
    //    .add("🇵🇲")
    //    .add("🇵🇳")
    //    .add("🇵🇷")
    //    .add("🇵🇸")
    //    .add("🇵🇹")
    //    .add("🇵🇼")
    //    .add("🇵🇾")
    //    .add("🇶🇦")
    //    .add("🇷🇪")
    //    .add("🇷🇴")
    //    .add("🇷🇸")
    //    .add("🇷🇺")
    //    .add("🇷🇼")
    //    .add("🇸🇦")
    //    .add("🇸🇧")
    //    .add("🇸🇨")
    //    .add("🇸🇩")
    //    .add("🇸🇪")
    //    .add("🇸🇬")
    //    .add("🇸🇭")
    //    .add("🇸🇮")
    //    .add("🇸🇯")
    //    .add("🇸🇰")
    //    .add("🇸🇱")
    //    .add("🇸🇲")
    //    .add("🇸🇳")
    //    .add("🇸🇴")
    //    .add("🇸🇷")
    //    .add("🇸🇸")
    //    .add("🇸🇹")
    //    .add("🇸🇻")
    //    .add("🇸🇽")
    //    .add("🇸🇾")
    //    .add("🇸🇿")
    //    .add("🇹🇦")
    //    .add("🇹🇨")
    //    .add("🇹🇩")
    //    .add("🇹🇫")
    //    .add("🇹🇬")
    //    .add("🇹🇭")
    //    .add("🇹🇯")
    //    .add("🇹🇰")
    //    .add("🇹🇱")
    //    .add("🇹🇲")
    //    .add("🇹🇳")
    //    .add("🇹🇴")
    //    .add("🇹🇷")
    //    .add("🇹🇹")
    //    .add("🇹🇻")
    //    .add("🇹🇼")
    //    .add("🇹🇿")
    //    .add("🇺🇦")
    //    .add("🇺🇬")
    //    .add("🇺🇲")
    //    .add("🇺🇸")
    //    .add("🇺🇾")
    //    .add("🇺🇿")
    //    .add("🇻🇦")
    //    .add("🇻🇨")
    //    .add("🇻🇪")
    //    .add("🇻🇬")
    //    .add("🇻🇮")
    //    .add("🇻🇳")
    //    .add("🇻🇺")
    //    .add("🇼🇫")
    //    .add("🇼🇸")
    //    .add("🇽🇰")
    //    .add("🇾🇪")
    //    .add("🇾🇹")
    //    .add("🇿🇦")
    //    .add("🇿🇲")
    //    .add("🇿🇼")
    //    .add("🎅🏻")
    //    .add("🎅🏼")
    //    .add("🎅🏽")
    //    .add("🎅🏾")
    //    .add("🎅🏿")
    //    .add("🏃🏻")
    //    .add("🏃🏼")
    //    .add("🏃🏽")
    //    .add("🏃🏾")
    //    .add("🏃🏿")
    //    .add("🏄🏻")
    //    .add("🏄🏼")
    //    .add("🏄🏽")
    //    .add("🏄🏾")
    //    .add("🏄🏿")
    //    .add("🏊🏻")
    //    .add("🏊🏼")
    //    .add("🏊🏽")
    //    .add("🏊🏾")
    //    .add("🏊🏿")
    //    .add("🏋🏻")
    //    .add("🏋🏼")
    //    .add("🏋🏽")
    //    .add("🏋🏾")
    //    .add("🏋🏿")
    //    .add("👁‍🗨")
    //    .add("👂🏻")
    //    .add("👂🏼")
    //    .add("👂🏽")
    //    .add("👂🏾")
    //    .add("👂🏿")
    //    .add("👃🏻")
    //    .add("👃🏼")
    //    .add("👃🏽")
    //    .add("👃🏾")
    //    .add("👃🏿")
    //    .add("👆🏻")
    //    .add("👆🏼")
    //    .add("👆🏽")
    //    .add("👆🏾")
    //    .add("👆🏿")
    //    .add("👇🏻")
    //    .add("👇🏼")
    //    .add("👇🏽")
    //    .add("👇🏾")
    //    .add("👇🏿")
    //    .add("👈🏻")
    //    .add("👈🏼")
    //    .add("👈🏽")
    //    .add("👈🏾")
    //    .add("👈🏿")
    //    .add("👉🏻")
    //    .add("👉🏼")
    //    .add("👉🏽")
    //    .add("👉🏾")
    //    .add("👉🏿")
    //    .add("👊🏻")
    //    .add("👊🏼")
    //    .add("👊🏽")
    //    .add("👊🏾")
    //    .add("👊🏿")
    //    .add("👋🏻")
    //    .add("👋🏼")
    //    .add("👋🏽")
    //    .add("👋🏾")
    //    .add("👋🏿")
    //    .add("👌🏻")
    //    .add("👌🏼")
    //    .add("👌🏽")
    //    .add("👌🏾")
    //    .add("👌🏿")
    //    .add("👍🏻")
    //    .add("👍🏼")
    //    .add("👍🏽")
    //    .add("👍🏾")
    //    .add("👍🏿")
    //    .add("👎🏻")
    //    .add("👎🏼")
    //    .add("👎🏽")
    //    .add("👎🏾")
    //    .add("👎🏿")
    //    .add("👏🏻")
    //    .add("👏🏼")
    //    .add("👏🏽")
    //    .add("👏🏾")
    //    .add("👏🏿")
    //    .add("👐🏻")
    //    .add("👐🏼")
    //    .add("👐🏽")
    //    .add("👐🏾")
    //    .add("👐🏿")
    //    .add("👦🏻")
    //    .add("👦🏼")
    //    .add("👦🏽")
    //    .add("👦🏾")
    //    .add("👦🏿")
    //    .add("👧🏻")
    //    .add("👧🏼")
    //    .add("👧🏽")
    //    .add("👧🏾")
    //    .add("👧🏿")
    //    .add("👨‍❤️‍👨")
    //    .add("👨‍❤️‍💋‍👨")
    //    .add("👨‍👨‍👦")
    //    .add("👨‍👨‍👦‍👦")
    //    .add("👨‍👨‍👧")
    //    .add("👨‍👨‍👧‍👦")
    //    .add("👨‍👨‍👧‍👧")
    //    .add("👨‍👩‍👦")
    //    .add("👨‍👩‍👦‍👦")
    //    .add("👨‍👩‍👧")
    //    .add("👨‍👩‍👧‍👦")
    //    .add("👨‍👩‍👧‍👧")
    //    .add("👨🏻")
    //    .add("👨🏼")
    //    .add("👨🏽")
    //    .add("👨🏾")
    //    .add("👨🏿")
    //    .add("👩‍❤️‍👨")
    //    .add("👩‍❤️‍👩")
    //    .add("👩‍❤️‍💋‍👨")
    //    .add("👩‍❤️‍💋‍👩")
    //    .add("👩‍👩‍👦")
    //    .add("👩‍👩‍👦‍👦")
    //    .add("👩‍👩‍👧")
    //    .add("👩‍👩‍👧‍👦")
    //    .add("👩‍👩‍👧‍👧")
    //    .add("👩🏻")
    //    .add("👩🏼")
    //    .add("👩🏽")
    //    .add("👩🏾")
    //    .add("👩🏿")
    //    .add("👮🏻")
    //    .add("👮🏼")
    //    .add("👮🏽")
    //    .add("👮🏾")
    //    .add("👮🏿")
    //    .add("👰🏻")
    //    .add("👰🏼")
    //    .add("👰🏽")
    //    .add("👰🏾")
    //    .add("👰🏿")
    //    .add("👱🏻")
    //    .add("👱🏼")
    //    .add("👱🏽")
    //    .add("👱🏾")
    //    .add("👱🏿")
    //    .add("👲🏻")
    //    .add("👲🏼")
    //    .add("👲🏽")
    //    .add("👲🏾")
    //    .add("👲🏿")
    //    .add("👳🏻")
    //    .add("👳🏼")
    //    .add("👳🏽")
    //    .add("👳🏾")
    //    .add("👳🏿")
    //    .add("👴🏻")
    //    .add("👴🏼")
    //    .add("👴🏽")
    //    .add("👴🏾")
    //    .add("👴🏿")
    //    .add("👵🏻")
    //    .add("👵🏼")
    //    .add("👵🏽")
    //    .add("👵🏾")
    //    .add("👵🏿")
    //    .add("👶🏻")
    //    .add("👶🏼")
    //    .add("👶🏽")
    //    .add("👶🏾")
    //    .add("👶🏿")
    //    .add("👷🏻")
    //    .add("👷🏼")
    //    .add("👷🏽")
    //    .add("👷🏾")
    //    .add("👷🏿")
    //    .add("👸🏻")
    //    .add("👸🏼")
    //    .add("👸🏽")
    //    .add("👸🏾")
    //    .add("👸🏿")
    //    .add("👼🏻")
    //    .add("👼🏼")
    //    .add("👼🏽")
    //    .add("👼🏾")
    //    .add("👼🏿")
    //    .add("💁🏻")
    //    .add("💁🏼")
    //    .add("💁🏽")
    //    .add("💁🏾")
    //    .add("💁🏿")
    //    .add("💂🏻")
    //    .add("💂🏼")
    //    .add("💂🏽")
    //    .add("💂🏾")
    //    .add("💂🏿")
    //    .add("💃🏻")
    //    .add("💃🏼")
    //    .add("💃🏽")
    //    .add("💃🏾")
    //    .add("💃🏿")
    //    .add("💅🏻")
    //    .add("💅🏼")
    //    .add("💅🏽")
    //    .add("💅🏾")
    //    .add("💅🏿")
    //    .add("💆🏻")
    //    .add("💆🏼")
    //    .add("💆🏽")
    //    .add("💆🏾")
    //    .add("💆🏿")
    //    .add("💇🏻")
    //    .add("💇🏼")
    //    .add("💇🏽")
    //    .add("💇🏾")
    //    .add("💇🏿")
    //    .add("💪🏻")
    //    .add("💪🏼")
    //    .add("💪🏽")
    //    .add("💪🏾")
    //    .add("💪🏿")
    //    .add("🕵🏻")
    //    .add("🕵🏼")
    //    .add("🕵🏽")
    //    .add("🕵🏾")
    //    .add("🕵🏿")
    //    .add("🖐🏻")
    //    .add("🖐🏼")
    //    .add("🖐🏽")
    //    .add("🖐🏾")
    //    .add("🖐🏿")
    //    .add("🖕🏻")
    //    .add("🖕🏼")
    //    .add("🖕🏽")
    //    .add("🖕🏾")
    //    .add("🖕🏿")
    //    .add("🖖🏻")
    //    .add("🖖🏼")
    //    .add("🖖🏽")
    //    .add("🖖🏾")
    //    .add("🖖🏿")
    //    .add("🙅🏻")
    //    .add("🙅🏼")
    //    .add("🙅🏽")
    //    .add("🙅🏾")
    //    .add("🙅🏿")
    //    .add("🙆🏻")
    //    .add("🙆🏼")
    //    .add("🙆🏽")
    //    .add("🙆🏾")
    //    .add("🙆🏿")
    //    .add("🙇🏻")
    //    .add("🙇🏼")
    //    .add("🙇🏽")
    //    .add("🙇🏾")
    //    .add("🙇🏿")
    //    .add("🙋🏻")
    //    .add("🙋🏼")
    //    .add("🙋🏽")
    //    .add("🙋🏾")
    //    .add("🙋🏿")
    //    .add("🙌🏻")
    //    .add("🙌🏼")
    //    .add("🙌🏽")
    //    .add("🙌🏾")
    //    .add("🙌🏿")
    //    .add("🙍🏻")
    //    .add("🙍🏼")
    //    .add("🙍🏽")
    //    .add("🙍🏾")
    //    .add("🙍🏿")
    //    .add("🙎🏻")
    //    .add("🙎🏼")
    //    .add("🙎🏽")
    //    .add("🙎🏾")
    //    .add("🙎🏿")
    //    .add("🙏🏻")
    //    .add("🙏🏼")
    //    .add("🙏🏽")
    //    .add("🙏🏾")
    //    .add("🙏🏿")
    //    .add("🚣🏻")
    //    .add("🚣🏼")
    //    .add("🚣🏽")
    //    .add("🚣🏾")
    //    .add("🚣🏿")
    //    .add("🚴🏻")
    //    .add("🚴🏼")
    //    .add("🚴🏽")
    //    .add("🚴🏾")
    //    .add("🚴🏿")
    //    .add("🚵🏻")
    //    .add("🚵🏼")
    //    .add("🚵🏽")
    //    .add("🚵🏾")
    //    .add("🚵🏿")
    //    .add("🚶🏻")
    //    .add("🚶🏼")
    //    .add("🚶🏽")
    //    .add("🚶🏾")
    //    .add("🚶🏿")
    //    .add("🛀🏻")
    //    .add("🛀🏼")
    //    .add("🛀🏽")
    //    .add("🛀🏾")
    //    .add("🛀🏿")
    //    .add("🤘🏻")
    //    .add("🤘🏼")
    //    .add("🤘🏽")
    //    .add("🤘🏾")
    //    .add("🤘🏿")
    //    .freeze();
    //    static {
    //        if (!ALLOWED.equals(EMOJI_ALLOWED)) {
    //            throw new IllegalArgumentException();
    //        }
    //    }
}
