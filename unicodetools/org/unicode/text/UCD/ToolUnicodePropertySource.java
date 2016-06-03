package org.unicode.text.UCD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.UnicodeProperty;
import org.unicode.cldr.util.UnicodeProperty.AliasAddAction;
import org.unicode.cldr.util.UnicodeProperty.BaseProperty;
import org.unicode.cldr.util.UnicodeProperty.SimpleProperty;
import org.unicode.cldr.util.UnicodeProperty.UnicodeMapProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.StringPrep;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * Class that provides all of the properties for formatting in the Unicode
 * standard data files. Note that many of these are generated directly from UCD,
 * and many from {@link DerivedProperty}. So fixes to some will go there.
 * 
 * @author markdavis
 * 
 */
public class ToolUnicodePropertySource extends UnicodeProperty.Factory {

    private static final String[] MAYBE_VALUES = {"M", "Maybe", "U", "Undetermined"};

    private static final String[] NO_VALUES = {"N", "No", "F", "False"};

    private static final String[] YES_VALUES = {"Y", "Yes", "T", "True"};

    static final boolean   DEBUG        = false;

    private static boolean needAgeCache = true;

    private static UCD[]   ucdCache     = new UCD[UCD_Types.LIMIT_AGE];

    private static HashMap<String, ToolUnicodePropertySource> factoryCache =
            new HashMap<String, ToolUnicodePropertySource>();

    private UCD            ucd;

    private Normalizer     nfc, nfd, nfkd, nfkc;

    public static synchronized ToolUnicodePropertySource make(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("call with explicit version, or Default.ucdVersion()");
        }
        ToolUnicodePropertySource result = factoryCache.get(version);
        if (result != null) {
            return result;
        }
        result = new ToolUnicodePropertySource(version);
        factoryCache.put(version, result);
        return result;
    }

    private ToolUnicodePropertySource(String version) {
        // make sure we have enough unassigned characters
        ucd = UCD.make(version);
        final UnicodeSet unassigned = new UnicodeSet();
        for (int i = 0; i < 0x110000; ++i) {
            if (ucd.getCategory(i) == UCD_Types.UNASSIGNED) {
                unassigned.add(i);
            }
        }
        UnicodeProperty.contractUNASSIGNED(unassigned);

        nfc = new Normalizer(UCD_Types.NFC, ucd.getVersion());
        nfd = new Normalizer(UCD_Types.NFD, ucd.getVersion());
        nfkc = new Normalizer(UCD_Types.NFKC, ucd.getVersion());
        nfkd = new Normalizer(UCD_Types.NFKD, ucd.getVersion());

        // emoji support

        UnicodeSet tags = new UnicodeSet(0xE0020,0xE007f).freeze();
        VersionInfo versionInfo = VersionInfo.getInstance(version);

        EmojiData emojiData = EmojiData.forUcd(versionInfo);
        final UnicodeSet E_Modifier = emojiData.getModifiers();

        UnicodeSet _E_Base = emojiData.getModifierBases();
        UnicodeSet _Glue_After_Zwj = emojiData.getAfterZwj();

        // break apart overlaps
        final UnicodeSet E_Base_GAZ = new UnicodeSet(_Glue_After_Zwj).retainAll(_E_Base).freeze();
        final UnicodeSet Glue_After_Zwj = new UnicodeSet(_Glue_After_Zwj).removeAll(E_Base_GAZ).freeze();
        final UnicodeSet E_Base = new UnicodeSet(_E_Base).removeAll(E_Base_GAZ).freeze();

        UnicodeSet Zwj = new UnicodeSet(0x200D,0x200D).freeze();

        version = ucd.getVersion(); // regularize

        // first the special cases
        if (DEBUG) {
            System.out.println("Adding Simple Cases");
        }

        //    add(new UnicodeProperty.SimpleProperty() {
        //      public String _getValue(int codepoint) {
        //        if (!nfc.isNormalized(codepoint))
        //          return "No";
        //        else if (nfc.isTrailing(codepoint))
        //          return "Maybe";
        //        else
        //          return "Yes";
        //      }
        //
        //      public int getMaxWidth(boolean isShort) {
        //        return 15;
        //      }
        //    }.setMain("NFC", null, UnicodeProperty.STRING, version));


        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                final int catMask = ucd.getCategoryMask(codepoint);
                if (((1 << UCD_Types.Cc) & catMask) != 0) {
                    return "<control-" + Utility.hex(codepoint, 4) + ">";
                    // return "<control>";
                }
                if ((ODD_BALLS & catMask) != 0) {
                    return null;
                }
                return ucd.getName(codepoint);
            }
        }.setValues("<string>").setMain("Name", "na", UnicodeProperty.MISC, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return UCharacter.getName1_0(codepoint);
            }
        }.setValues("<string>").setMain("Unicode_1_Name", "na1", UnicodeProperty.MISC, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return "";
            }
        }.setValues("<string>").setMain("ISO_Comment", "isc", UnicodeProperty.MISC, version));

        // add(new UnicodeProperty.SimpleProperty() {
        // public String _getValue(int codepoint) {
        // return "";
        // }
        // }.setValues("<string>").setMain("Jamo_Short_Name", "JSN",
        // UnicodeProperty.MISC, version));

        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "Name_Alias", "Name_Alias");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkRSUnicode", "cjkRSUnicode", "kRSUnicode", "Unicode_Radical_Stroke", "URS");
        addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkAccountingNumeric", "cjkAccountingNumeric", "kAccountingNumeric");
        addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkOtherNumeric", "cjkOtherNumeric", "kOtherNumeric");
        addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkPrimaryNumeric", "cjkPrimaryNumeric", "kPrimaryNumeric");
        addFakeProperty(version, UnicodeProperty.STRING, "<none>", "cjkCompatibilityVariant", "cjkCompatibilityVariant", "kCompatibilityVariant");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIICore", "cjkIICore", "kIICore");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_GSource", "cjkIRG_GSource", "kIRG_GSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_HSource", "cjkIRG_HSource", "kIRG_HSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_JSource", "cjkIRG_JSource", "kIRG_JSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_KPSource", "cjkIRG_KPSource", "kIRG_KPSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_KSource", "cjkIRG_KSource", "kIRG_KSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_TSource", "cjkIRG_TSource", "kIRG_TSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_USource", "cjkIRG_USource", "kIRG_USource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_VSource", "cjkIRG_VSource", "kIRG_VSource");
        addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_MSource", "cjkIRG_MSource", "kIRG_MSource");

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getDecompositionMapping(codepoint);
            }
        }.setValues("<string>").setMain("Decomposition_Mapping", "dm", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.LOWER);
            }
        }.setValues("<string>").setMain("Lowercase_Mapping", "lc", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.LOWER);
            }
        }.setValues("<string>").setMain("Lowercase_Mapping", "lc", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.SIMPLE, UCD_Types.LOWER);
            }
        }.setValues("<string>").setMain("Simple_Lowercase_Mapping", "slc", UnicodeProperty.STRING,
                version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.UPPER);
            }
        }.setValues("<string>").setMain("Uppercase_Mapping", "uc", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.SIMPLE, UCD_Types.UPPER);
            }
        }.setValues("<string>").setMain("Simple_Uppercase_Mapping", "suc", UnicodeProperty.STRING,
                version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.TITLE);
            }
        }.setValues("<string>").setMain("Titlecase_Mapping", "tc", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.SIMPLE, UCD_Types.TITLE);
            }
        }.setValues("<string>").setMain("Simple_Titlecase_Mapping", "stc", UnicodeProperty.STRING,
                version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.FOLD);
            }
        }.setValues("<string>").setMain("Case_Folding", "cf", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getCase(codepoint, UCD_Types.SIMPLE, UCD_Types.FOLD);
            }
        }.setValues("<string>").setMain("Simple_Case_Folding", "scf", UnicodeProperty.STRING, version)
        .addName("sfc"));

        /*
         * cp=00FD, isc=<> != <MISSING> cp=00FD, lc=<> != <MISSING> cp=00FD, slc=<>
         * != <MISSING> cp=00FD, stc=<00DD> != <MISSING> cp=00FD, suc=<00DD> !=
         * <MISSING> cp=00FD, tc=<> != <MISSING> cp=00FD, uc=<> != <MISSING>
         */

        final String[][] blockNames = ucd.getBlockNameLists();

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                if (DEBUG && codepoint == 0x1D100) {
                    System.out.println("here");
                }
                // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBlock(codepoint);
            }

            @Override
            protected UnicodeMap<String> _getUnicodeMap() {
                return ucd.blockData;
            }
        }
        .setValues(blockNames[0], blockNames[1])
        .swapFirst2ValueAliases()
        .setMain("Block", "blk", UnicodeProperty.CATALOG, version)
        .addValueAliases(
                new String[][] {
                        { "Basic_Latin", "ASCII" },
                        { "Latin_1_Supplement", "Latin_1" },
                        { "Unified_Canadian_Aboriginal_Syllabics", "Canadian_Syllabics" },
                        { "Greek_And_Coptic", "Greek" },
                        { "Private_Use_Area", "Private_Use" },
                        { "Combining_Diacritical_Marks_For_Symbols", "Combining_Marks_For_Symbols" },
                        { "Arabic_Presentation_Forms_A", "Arabic_Presentation_Forms-A" },
                }, AliasAddAction.REQUIRE_MAIN_ALIAS)
                );

        // add(new UnicodeProperty.SimpleProperty() {
        // public String _getValue(int codepoint) {
        // return "";
        // }
        // }.setValues("<string>").setMain("Jamo_Short_Name", "JSN",
        // UnicodeProperty.MISC, version));
        // UCD_Names.JAMO_L_TABLE[LIndex] + UCD_Names.JAMO_V_TABLE[VIndex] +
        // UCD_Names.JAMO_T_TABLE[TIndex]
        // LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7
        final Set<String> tempValues = new LinkedHashSet<String>();
        tempValues.addAll(Arrays.asList(UCD_Names.JAMO_L_TABLE));
        tempValues.addAll(Arrays.asList(UCD_Names.JAMO_V_TABLE));
        tempValues.addAll(Arrays.asList(UCD_Names.JAMO_T_TABLE));
        tempValues.remove("");
        // tempValues.add("none");

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                int temp;
                temp = codepoint - UCD.TBase;
                if (temp > 0) { // skip first
                    return temp >= UCD_Names.JAMO_T_TABLE.length ? null : UCD_Names.JAMO_T_TABLE[temp];
                }
                temp = codepoint - UCD.VBase;
                if (temp >= 0) {
                    return temp >= UCD_Names.JAMO_V_TABLE.length ? null : UCD_Names.JAMO_V_TABLE[temp];
                }
                temp = codepoint - UCD.LBase;
                if (temp >= 0) {
                    return temp >= UCD_Names.JAMO_L_TABLE.length ? null : UCD_Names.JAMO_L_TABLE[temp];
                }
                return null;
            }
        }.setValues(new ArrayList<String>(tempValues)).setMain("Jamo_Short_Name", "JSN", UnicodeProperty.MISC,
                version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBidiMirror(codepoint);
            }
        }.setValues("<string>").setMain("Bidi_Mirroring_Glyph", "bmg", UnicodeProperty.MISC, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return UTF16.valueOf(ucd.getBidi_Paired_Bracket(codepoint));
            }
        }.setValues("<string>").setMain("Bidi_Paired_Bracket", "bpb", UnicodeProperty.MISC, version));

        BaseProperty bpt = new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return ucd.getBidi_Paired_Bracket_TypeID(codepoint);
            }
        }.setValues(UCD_Names.Bidi_Paired_Bracket_Type, UCD_Names.Bidi_Paired_Bracket_Type_SHORT)
        .swapFirst2ValueAliases()
        .setMain("Bidi_Paired_Bracket_Type", "bpt", UnicodeProperty.ENUMERATED, version);
        add(bpt);


        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.FOLD);
            }
        }.setValues("<string>").setMain("Case_Folding", "cf", UnicodeProperty.STRING, version));

        add(new UnicodeProperty.SimpleProperty() {
            IdnaInfo info;
            {
                try {
                    info = new IdnaInfo();
                } catch (final IOException e) {
                    throw new IllegalArgumentException("Can't find data");
                }
            }

            @Override
            public String _getValue(int codepoint) {
                // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return info.getIDNAType(codepoint) == IdnaInfo.IdnaType.OK ? UCD_Names.YES : UCD_Names.NO;
            }
        }.setMain("IdnOutput", "idnOut", UnicodeProperty.EXTENDED_BINARY, version));

        add(new UnicodeProperty.SimpleProperty() {
            NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
            {
                nf.setGroupingUsed(false);
                nf.setMaximumFractionDigits(8);
                nf.setMinimumFractionDigits(1);
            }

            @Override
            public String _getValue(int codepoint) {

                final double num = ucd.getNumericValue(codepoint);
                if (Double.isNaN(num)) {
                    return null;
                }
                return nf.format(num);
            }
        }.setMain("Numeric_Value", "nv", UnicodeProperty.NUMERIC, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int cp) {
                if (!ucd.isRepresented(cp)) {
                    return null;
                }
                if (cp == '\u1E9E') {
                    System.out.println("@#$ debug");
                }
                final String b = nfkc.normalize(ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD));
                final String c = nfkc.normalize(ucd.getCase(b, UCD_Types.FULL, UCD_Types.FOLD));
                if (c.equals(b)) {
                    return null;
                }
                final String d = nfkc.normalize(ucd.getCase(b, UCD_Types.FULL, UCD_Types.FOLD));
                if (!d.equals(c)) {
                    throw new IllegalArgumentException("Serious failure in FC_NFKC!!!");
                }
                return c;
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 14;
            }
        }.setMain("FC_NFKC_Closure", "FC_NFKC", UnicodeProperty.STRING, version)
        // .addName("FNC")
                );

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int cp) {
                if (!ucd.isRepresented(cp)) {
                    return null;
                }
                final String b = nfd.normalize(cp);
                if (b.codePointAt(0) == cp && b.length() == Character.charCount(cp)) {
                    return null;
                }
                return b;
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 5;
            }
        }.setMain("toNFD", "toNFD", UnicodeProperty.EXTENDED_STRING, version)
                );

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int cp) {
                if (!ucd.isRepresented(cp)) {
                    return null;
                }
                final String b = nfc.normalize(cp);
                if (b.codePointAt(0) == cp && b.length() == Character.charCount(cp)) {
                    return null;
                }
                return b;
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 5;
            }
        }.setMain("toNFC", "toNFC", UnicodeProperty.EXTENDED_STRING, version)
                );

        add(new SimpleIsProperty("isNFD", "isNFD", version, getProperty("toNFD"), false).setExtended());
        add(new SimpleIsProperty("isNFC", "isNFC", version, getProperty("toNFC"), false).setExtended());



        add(new UnicodeProperty.SimpleProperty() {
            UnicodeSet ignorable = null;
            @Override
            public String _getValue(final int cp) {
                if (!ucd.isRepresented(cp)) {
                    return null;
                }
                boolean debug = false;
                if (cp == -1) {  // change to a real code point for debugging
                    debug = true;
                }
                // lazy eval
                if (ignorable == null) {
                    ignorable = getProperty("DefaultIgnorableCodePoint").getSet(UCD_Names.YES);
                }
                if (ignorable.contains(cp)) {
                    return "";
                }
                final String case1 = ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD);
                final String b = nfkc.normalize(case1);
                if (equals(cp,b)) {
                    return null;
                }
                if (debug) {
                    System.out.println("NFKC_CF:"
                            + "\n\tsource:\tU+" + Utility.hex(cp) + "\t" + Default.ucd().getName(cp)
                            + "\n\tcase1:\tU+" + Utility.hex(case1) + "\t" + Default.ucd().getName(case1));
                }
                final String c = trans(b);
                if (c.equals(b)) {
                    return c;
                }
                if (debug) {
                    System.out.println("NFKC_CF:"
                            + "\n\tsource:\tU+" + Utility.hex(cp) + "\t" + Default.ucd().getName(cp)
                            + "\n\tcase1:\tU+" + Utility.hex(case1) + "\t" + Default.ucd().getName(case1)
                            + "\n\ttrans1:\tU+" + Utility.hex(c) + "\t" + Default.ucd().getName(c));
                }
                final String d = trans(c);
                if (d.equals(c)) {
                    return d;
                }
                throw new IllegalArgumentException(
                        "NFKC_CF requires THREE passes:"
                                + "\n\tsource:\tU+" + Utility.hex(cp) + "\t" + Default.ucd().getName(cp)
                                + "\n\tcase1:\tU+" + Utility.hex(case1) + "\t" + Default.ucd().getName(case1)
                                + "\n\ttrans1:\tU+" + Utility.hex(c) + "\t" + Default.ucd().getName(c)
                                + "\n\ttrans2:\tU+" + Utility.hex(d) + "\t" + Default.ucd().getName(d)
                        );
            }
            private String trans(String b) {
                final String bb = removeFrom(b, ignorable);
                final String case2 = ucd.getCase(bb, UCD_Types.FULL, UCD_Types.FOLD);
                final String c = nfkc.normalize(case2);
                return c;
            }
            @Override
            public int getMaxWidth(boolean isShort) {
                return 14;
            }
        }.setMain("NFKC_Casefold", "NFKC_CF", UnicodeProperty.STRING, version));

        add(new SimpleIsProperty("Changes_When_NFKC_Casefolded", "CWKCF", version, getProperty("NFKC_Casefold"), false).setCheckUnassigned());

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                if (!nfd.isNormalized(codepoint)) {
                    return UCD_Names.NO;
                } else if (nfd.isTrailing(codepoint)) {
                    throw new IllegalArgumentException("Internal Error!");
                } else {
                    return UCD_Names.YES;
                }
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases().setMain("NFD_Quick_Check", "NFD_QC",
                UnicodeProperty.ENUMERATED, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                if (!nfc.isNormalized(codepoint)) {
                    return "No";
                } else if (nfc.isTrailing(codepoint)) {
                    return "Maybe";
                } else {
                    return "Yes";
                }
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases().setMain(
                "NFC_Quick_Check", "NFC_QC", UnicodeProperty.ENUMERATED, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                if (!nfkd.isNormalized(codepoint)) {
                    return UCD_Names.NO;
                } else if (nfkd.isTrailing(codepoint)) {
                    throw new IllegalArgumentException("Internal Error!");
                } else {
                    return UCD_Names.YES;
                }
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases().setMain("NFKD_Quick_Check",
                "NFKD_QC", UnicodeProperty.ENUMERATED, version));

        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                if (!nfkc.isNormalized(codepoint)) {
                    return "No";
                } else if (nfkc.isTrailing(codepoint)) {
                    return "Maybe";
                } else {
                    return "Yes";
                }
            }

            @Override
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases().setMain(
                "NFKC_Quick_Check", "NFKC_QC", UnicodeProperty.ENUMERATED, version));

        /*
         * add(new UnicodeProperty.SimpleProperty() { public String _getValue(int
         * codepoint) { if (!nfx.isNormalized(codepoint)) return NO; else if
         * (nfx.isTrailing(codepoint)) return MAYBE; else return ""; }
         * }.setMain("NFD_QuickCheck", "nv", UnicodeProperty.NUMERIC, version)
         * .setValues("<number>"));
         */

        // Now the derived properties
        if (DEBUG) {
            System.out.println("Derived Properties");
        }
        for (int i = 0; i < UCD_Types.DERIVED_PROPERTY_LIMIT; ++i) {
            final UCDProperty prop = DerivedProperty.make(i, ucd);
            if (prop == null) {
                continue;
            }
            if (!prop.isStandard()) {
                continue;
            }
            final String name = prop.getName();
            if (getProperty(name) != null) {
                if (DEBUG) {
                    System.out.println("Iterated Names: " + name + ", ALREADY PRESENT*");
                }
                continue; // skip if already there
            }
            int type = prop.getValueType();
            if (i == UCD_Types.FC_NFKC_Closure) {
                type = UnicodeProperty.STRING;
            } else if (i == UCD_Types.FullCompExclusion) {
                type = UnicodeProperty.BINARY;
            } else {
                type = remapUCDType(type);
            }

            if (DEBUG) {
                System.out.println(prop.getName());
            }
            add(new UCDPropertyWrapper(prop, type, false));
        }

        // then the general stuff

        if (DEBUG) {
            System.out.println("Other Properties");
        }
        final List<String> names = new ArrayList<String>();
        UnifiedProperty.getAvailablePropertiesAliases(names, ucd);
        final Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            final String name = it.next();
            if (getProperty(name) != null) {
                if (DEBUG) {
                    System.out.println("Iterated Names: " + name + ", ALREADY PRESENT");
                }
                continue; // skip if already there
            }
            if (DEBUG) {
                System.out.println("Iterated Names: " + name);
            }
            add(new ToolUnicodeProperty(name));
        }

        final int compositeVersion = ucd.getCompositeVersion();
        if (compositeVersion >= 0x040000) {
            final UnicodeMap<String> unicodeMap = new UnicodeMap<String>();
            unicodeMap.setErrorOnReset(true); // will cause exception if we try assigning 2 different values

            final UnicodeSet prepend = new UnicodeSet("[\\u0600-\\u0605\\u06DD\\u08E2\\u070F\\U000110BD \\u0D4E \\U000111C2 \\U000111C3]");
            unicodeMap.putAll(prepend, "Prepend");
            unicodeMap.put(0xD, "CR");
            unicodeMap.put(0xA, "LF");
            final UnicodeProperty cat = getProperty("General_Category");
            final UnicodeProperty di = getProperty("di");

            final UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet(UCD_Names.YES);

            //String x = cat.getValue(0x17B4);
            final UnicodeSet catCn = cat.getSet("Cn");
            final UnicodeSet di2 = di.getSet("Yes");
            final UnicodeSet unassignedDi = new UnicodeSet(catCn).retainAll(di2);

            final UnicodeSet temp = cat.getSet("Line_Separator")
                    .addAll(cat.getSet("Paragraph_Separator"))
                    .addAll(cat.getSet("Control"))
                    .addAll(cat.getSet("Format"))
                    .addAll(cat.getSet("Cs"))
                    .addAll(unassignedDi)
                    .remove(0xD)
                    .remove(0xA)
                    .remove(0x200C)
                    .remove(0x200D)
                    .removeAll(tags)
                    .removeAll(prepend)
                    ;
            final UnicodeSet diff = new UnicodeSet(temp).retainAll(graphemeExtend);
            if (diff.size() != 0) {
                temp.removeAll(diff);
                System.err.println("ERROR: Problem in generating Grapheme_Cluster_Break for "
                        + ucd.getVersion() + ",\t" + diff);
            }
            unicodeMap.putAll(temp, "Control");

            unicodeMap.putAll(new UnicodeSet(graphemeExtend).remove(0x200d), "Extend");
            unicodeMap.putAll(new UnicodeSet("[[\u0E31 \u0E34-\u0E3A \u0EB1 \u0EB4-\u0EB9 \u0EBB \u0EBA]-[:cn:]]"), "Extend");

            unicodeMap.putAll(0x1F1E6, 0x1F1FF, "Regional_Indicator");

            // (Currently there are no characters with this value)
            //UnicodeSet graphemePrepend = getProperty("Logical_Order_Exception").getSet(UCD_Names.YES);
            //            unicodeMap.setErrorOnReset(false);
            //            unicodeMap.put(0, "Prepend");
            //            unicodeMap.setErrorOnReset(true);

            unicodeMap.putAll(
                    cat.getSet("Spacing_Mark")
                    //.addAll(new UnicodeSet("[\u0E30 \u0E32 \u0E33 \u0E45 \u0EB0 \u0EB2 \u0EB3]"))
                    .addAll(new UnicodeSet("[\u0E33 \u0EB3]"))
                    .removeAll(new UnicodeSet("[\u102B\u102C\u1038\u1062-\u1064\u1067-\u106D\u1083\u1087-\u108C\u108F\u109A-\u109C\u19B0-\u19B4\u19B8\u19B9\u19BB-\u19C0\u19C8\u19C9\u1A61\u1A63\u1A64\uAA7B]"))
                    .removeAll(unicodeMap.keySet("Extend"))
                    .remove(0xAA7D)
                    , "SpacingMark");

            final UnicodeProperty hangul = getProperty("Hangul_Syllable_Type");
            unicodeMap.putAll(hangul.getSet("L"), "L");
            unicodeMap.putAll(hangul.getSet("V"), "V");
            unicodeMap.putAll(hangul.getSet("T"), "T");
            unicodeMap.putAll(hangul.getSet("LV"), "LV");
            unicodeMap.putAll(hangul.getSet("LVT"), "LVT");

            // emoji support
            unicodeMap.putAll(tags, "Extend");

            unicodeMap.putAll(E_Base, "E_Base");
            unicodeMap.putAll(E_Modifier, "E_Modifier");

            unicodeMap.putAll(Zwj, "ZWJ");
            unicodeMap.putAll(Glue_After_Zwj, "Glue_After_Zwj");
            unicodeMap.putAll(E_Base_GAZ, "E_Base_GAZ");

            // note: during development it is easier to put new properties at the top.
            // that way you find out which other values overlap.

            unicodeMap.setMissing("Other");
            add(new UnicodeProperty.UnicodeMapProperty()
            .set(unicodeMap)
            .setMain("Grapheme_Cluster_Break", "GCB", UnicodeProperty.ENUMERATED, version)
            .addValueAliases(
                    new String[][] {
                            { "Prepend", "PP" }, { "Control", "CN" }, { "Extend", "EX" },
                            { "Other", "XX" }, { "SpacingMark", "SM" },
                            { "Regional_Indicator", "RI" },
                            { "E_Base", "EB" },
                            { "E_Modifier", "EM" },
                            { "Glue_After_Zwj", "GAZ" },
                            { "E_Base_GAZ", "EBG" },
                            { "ZWJ", "ZWJ" }
                    }, AliasAddAction.ADD_MAIN_ALIAS)
                    .swapFirst2ValueAliases())
                    ;
            // HACK. Property value aliases can only be added if there is a property value on some character. So we added null above.
            //            unicodeMap.setErrorOnReset(false);
            //            unicodeMap.put(0,oldValue);
            //            unicodeMap.setErrorOnReset(true);
        }

        if (compositeVersion >= 0x040000) {
            final UnicodeMap<String> unicodeMap = new UnicodeMap<String>();
            unicodeMap.setErrorOnReset(true); // disallow multiple values for code point

            final UnicodeProperty cat = getProperty("General_Category");
            final UnicodeProperty script = getProperty("Script");
            //unicodeMap.put(0x200B, "Other");

            unicodeMap.putAll(new UnicodeSet("[\"]"), "Double_Quote");
            unicodeMap.putAll(new UnicodeSet("[']"), "Single_Quote");
            unicodeMap.putAll(
                    new UnicodeSet(cat.getSet("Other_Letter")
                            .retainAll(script.getSet("Hebrew"))),
                    "Hebrew_Letter");

            unicodeMap.putAll(new UnicodeSet("[\\u000D]"), "CR");
            unicodeMap.putAll(new UnicodeSet("[\\u000A]"), "LF");
            unicodeMap.putAll(new UnicodeSet("[\\u0085\\u000B\\u000C\\u000C\\u2028\\u2029]"),
                    "Newline");
            unicodeMap.putAll(getProperty("Grapheme_Extend")
                    .getSet(UCD_Names.YES)
                    .addAll(cat.getSet("Spacing_Mark"))
                    .removeAll(Zwj), "Extend");

            unicodeMap.putAll(0x1F1E6, 0x1F1FF, "Regional_Indicator");

            unicodeMap.putAll(cat.getSet("Format")
                    .remove(0x200C)
                    .remove(0x200D)
                    .remove(0x200B)
                    .removeAll(tags), "Format");
            unicodeMap
            .putAll(
                    script
                    .getSet("Katakana")
                    .addAll(
                            new UnicodeSet(
                                    "[\u3031\u3032\u3033\u3034\u3035\u309B\u309C\u30A0\u30FC\uFF70]")),
                    "Katakana"); // \uFF9E\uFF9F
            // final Object foo = unicodeMap.keySet("Katakana");
            // UnicodeSet graphemeExtend =
            // getProperty("Grapheme_Extend").getSet(UCD_Names.YES).remove(0xFF9E,0xFF9F);
            final UnicodeProperty lineBreak = getProperty("Line_Break");
            unicodeMap.putAll(getProperty("Alphabetic").getSet(UCD_Names.YES).add(0x05F3).removeAll(
                    getProperty("Ideographic").getSet(UCD_Names.YES))
                    .removeAll(unicodeMap.keySet("Katakana"))
                    // .removeAll(script.getSet("Thai"))
                    // .removeAll(script.getSet("Lao"))
                    .removeAll(lineBreak.getSet("SA")).removeAll(script.getSet("Hiragana"))
                    .removeAll(unicodeMap.keySet("Extend"))
                    .removeAll(unicodeMap.keySet("Hebrew_Letter")),
                    "ALetter");
            unicodeMap
            .putAll(new UnicodeSet(
                    "[\\u00B7\\u0387\\u05F4\\u2027\\u003A\\uFE13\\uFE55\\uFF1A\\u02D7]"),
                    "MidLetter");
            /*
             * ? \\u02D7 U+02D7 ( ˗ ) MODIFIER LETTER MINUS SIGN
U+00B7 ( · ) MIDDLE DOT
U+0387 ( · ) GREEK ANO TELEIA
U+05F4 ( ״ ) HEBREW PUNCTUATION GERSHAYIM
U+2027 ( ‧ ) HYPHENATION POINT

U+003A ( : ) COLON (used in Swedish)
U+FE13 ( ︓ ) PRESENTATION FORM FOR VERTICAL COLON
U+FE55 ( ﹕ ) SMALL COLON
U+FF1A ( ： ) FULLWIDTH COLON
             */
            /*
             * 0387 ( · ) GREEK ANO TELEIA FE13 ( ︓ ) PRESENTATION FORM FOR
             * VERTICAL COLON FE55 ( ﹕ ) SMALL COLON FF1A ( ： ) FULLWIDTH COLON
             */
            unicodeMap.putAll(lineBreak.getSet("Infix_Numeric").add(0x066C).add(0xFE50).add(0xFE54)
                    .add(0xFF0C).add(0xFF1B).remove(0x002E).remove(0x003A).remove(0xFE13), "MidNum");
            /*
             * 066C ( ٬ ) ARABIC THOUSANDS SEPARATOR
             * 
             * FE50 ( ﹐ ) SMALL COMMA FE54 ( ﹔ ) SMALL SEMICOLON FF0C ( ， )
             * FULLWIDTH COMMA FF1B ( ； ) FULLWIDTH SEMICOLON
             */
            unicodeMap.putAll(new UnicodeSet(
                    "[\\u002E\\u2018\\u2019\\u2024\\uFE52\\uFF07\\uFF0E]"), "MidNumLet");

            unicodeMap.putAll(new UnicodeSet(lineBreak.getSet("Numeric")).remove(0x066C), "Numeric"); // .remove(0x387)
            unicodeMap.putAll(cat.getSet("Connector_Punctuation")
                    .remove(0x30FB).remove(0xFF65)
                    .add(0x202F), // action 144a067
                    "ExtendNumLet");
            // unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none
            // of the above touch it.

            // emoji support
            unicodeMap.putAll(tags, "Extend");

            unicodeMap.putAll(E_Base, "E_Base");
            unicodeMap.putAll(E_Modifier, "E_Modifier");

            unicodeMap.putAll(Zwj, "ZWJ");
            unicodeMap.putAll(Glue_After_Zwj, "Glue_After_Zwj");
            unicodeMap.putAll(E_Base_GAZ, "E_Base_GAZ");

            // note: during development it is easier to put new properties at the top.
            // that way you find out which other values overlap.

            unicodeMap.setMissing("Other");
            // 0387 Wordbreak = Other → MidLetter

            add(new UnicodeProperty.UnicodeMapProperty()
            .set(unicodeMap)
            .setMain("Word_Break", "WB", UnicodeProperty.ENUMERATED, version)
            .addValueAliases(
                    new String[][] { { "Format", "FO" }, { "Katakana", "KA" }, { "ALetter", "LE" },
                            { "MidLetter", "ML" }, { "MidNum", "MN" }, { "MidNumLet", "MB" },
                            { "MidNumLet", "MB" }, { "Numeric", "NU" }, { "ExtendNumLet", "EX" },
                            { "Other", "XX" }, { "Newline", "NL" },
                            { "Regional_Indicator", "RI" },
                            { "Double_Quote", "DQ" },
                            { "Single_Quote", "SQ" },
                            { "Hebrew_Letter", "HL" },
                            { "E_Base", "EB" },
                            { "E_Modifier", "EM" },
                            { "Glue_After_Zwj", "GAZ" },
                            { "E_Base_GAZ", "EBG" },
                            { "ZWJ", "ZWJ" }
                    }, AliasAddAction.ADD_MAIN_ALIAS).swapFirst2ValueAliases());
        }

        if (compositeVersion >= 0x040000) {
            final UnicodeMap<String> unicodeMap = new UnicodeMap<String>();
            unicodeMap.setErrorOnReset(true);
            unicodeMap.putAll(new UnicodeSet("[\\u000D]"), "CR");
            unicodeMap.putAll(new UnicodeSet("[\\u000A]"), "LF");
            final UnicodeProperty cat = getProperty("General_Category");
            unicodeMap.putAll(getProperty("Grapheme_Extend").getSet(UCD_Names.YES)
                    .addAll(cat.getSet("Spacing_Mark"))
                    .add(0x200D)
                    .add(0xE0020,0xE007F), "Extend");
            unicodeMap.putAll(new UnicodeSet("[\\u0085\\u2028\\u2029]"), "Sep");
            unicodeMap.putAll(cat.getSet("Format")
                    .remove(0x200C)
                    .remove(0x200D)
                    .remove(0xE0020,0xE007F), "Format");
            unicodeMap.putAll(getProperty("Whitespace").getSet(UCD_Names.YES)
                    .removeAll(unicodeMap.keySet("Sep"))
                    .removeAll(unicodeMap.keySet("CR"))
                    .removeAll(unicodeMap.keySet("LF"))
                    //.remove(0x202F) // action 144a067, reversed by 147A027
                    , "Sp");
            final UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet(UCD_Names.YES);
            unicodeMap.putAll(getProperty("Lowercase").getSet(UCD_Names.YES)
                    .removeAll(graphemeExtend), "Lower");
            unicodeMap.putAll(getProperty("Uppercase").getSet(UCD_Names.YES).addAll(
                    cat.getSet("Titlecase_Letter")), "Upper");
            final UnicodeSet temp = getProperty("Alphabetic").getSet(UCD_Names.YES)
                    // .add(0x00A0)
                    .add(0x05F3).removeAll(unicodeMap.keySet("Lower")).removeAll(
                            unicodeMap.keySet("Upper")).removeAll(unicodeMap.keySet("Extend"));
            unicodeMap.putAll(temp, "OLetter");
            final UnicodeProperty lineBreak = getProperty("Line_Break");
            unicodeMap.putAll(lineBreak.getSet("Numeric"), "Numeric");
            unicodeMap.putAll(new UnicodeSet("[\\u002E\\u2024\\uFE52\\uFF0E]"), "ATerm");
            unicodeMap.putAll(getProperty("STerm").getSet(UCD_Names.YES).removeAll(
                    unicodeMap.keySet("ATerm")), "STerm");
            unicodeMap.putAll(cat.getSet("Open_Punctuation").addAll(cat.getSet("Close_Punctuation"))
                    .addAll(lineBreak.getSet("Quotation")).remove(0x05F3).removeAll(
                            unicodeMap.keySet("ATerm")).removeAll(unicodeMap.keySet("STerm")),
                    "Close");
            unicodeMap.putAll(new UnicodeSet("[\\u002C\\u3001\\uFE10\\uFE11\\uFF0C"
                    + "\\uFE50\\uFF64\\uFE51\\uFE51\\u055D\\u060C\\u060D\\u07F8\\u1802\\u1808" + // new
                    // from
                    // L2/08-029
                    "\\u003A\\uFE13\\uFF1A" + "\\uFE55" + // new from L2/08-029
                    // "\\u003B\\uFE14\\uFF1B" +
                    "\\u2014\\uFE31\\u002D\\uFF0D" + "\\u2013\\uFE32\\uFE58\\uFE63" + // new
                    // from
                    // L2/08-029
                    "]"), "SContinue");
            // unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none
            // of the above touch it.
            unicodeMap.setMissing("Other");

            add(new UnicodeProperty.UnicodeMapProperty()
            .set(unicodeMap)
            .setMain("Sentence_Break", "SB", UnicodeProperty.ENUMERATED, version)
            .addValueAliases(
                    new String[][] { { "Sep", "SE" }, { "Format", "FO" }, { "Sp", "SP" },
                            { "Lower", "LO" }, { "Upper", "UP" }, { "OLetter", "LE" }, { "Numeric", "NU" },
                            { "ATerm", "AT" }, { "STerm", "ST" }, { "Extend", "EX" }, { "SContinue", "SC" },
                            { "Close", "CL" }, { "Other", "XX" }, }, AliasAddAction.IGNORE_IF_MISSING).swapFirst2ValueAliases());
        }

        // ========================

        /*
         * # As defined by Unicode Standard Definition D120 # C has the Lowercase or
         * Uppercase property or has a General_Category value of Titlecase_Letter.
         */
        add(new SimpleBinaryProperty("Cased", "Cased", version, new UnicodeSet()
        .addAll(getProperty("Lowercase").getSet(UCD_Names.YES))
        .addAll(getProperty("Uppercase").getSet(UCD_Names.YES))
        .addAll(getProperty("GeneralCategory").getSet("Lt"))));

        /*
         * # As defined by Unicode Standard Definition
         * D121 # C is defined to be case-ignorable if C has the value MidLetter or
         * the value MidNumLet # for the Word_Break property or its General_Category
         * is one of # Nonspacing_Mark (Mn), Enclosing_Mark (Me), Format (Cf),
         * Modifier_Letter (Lm), or Modifier_Symbol (Sk).
         */
        add(new SimpleBinaryProperty("Case_Ignorable", "CI", version, new UnicodeSet()
        .addAll(getProperty("WordBreak").getSet("MidNumLet"))
        .addAll(getProperty("WordBreak").getSet("MidLetter"))
        .addAll(getProperty("WordBreak").getSet("SQ"))
        .addAll(getProperty("GeneralCategory").getSet("Mn"))
        .addAll(getProperty("GeneralCategory").getSet("Me"))
        .addAll(getProperty("GeneralCategory").getSet("Cf"))
        .addAll(getProperty("GeneralCategory").getSet("Lm"))
        .addAll(getProperty("GeneralCategory").getSet("Sk"))));

        /*
         * Property: Is_Lowercased # As defined by Unicode Standard
         * Definition D124
         */
        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return changesWhenCased(codepoint, UCD_Types.LOWER);
            }
        }
        .setMain("Changes_When_Lowercased", "CWL", UnicodeProperty.BINARY, version)
        .swapFirst2ValueAliases());


        /* Property: Is_Uppercased # As defined by Unicode Standard
         * Definition D125
         */
        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return changesWhenCased(codepoint, UCD_Types.UPPER);
            }
        }
        .setMain("Changes_When_Uppercased", "CWU", UnicodeProperty.BINARY, version)
        .swapFirst2ValueAliases());


        /* Property: Is_Titlecased # As defined by Unicode Standard
         * Definition D126
         */
        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return changesWhenCased(codepoint, UCD_Types.TITLE);
            }
        }
        .setMain("Changes_When_Titlecased", "CWT", UnicodeProperty.BINARY, version)
        .swapFirst2ValueAliases());


        /* Property: Is_Casefolded # As defined by Unicode Standard
         * Definition D127
         */
        add(new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return changesWhenCased(codepoint, UCD_Types.FOLD);
            }
        }
        .setMain("Changes_When_Casefolded", "CWCF", UnicodeProperty.BINARY, version)
        .swapFirst2ValueAliases());


        /* Property: Is_Cased # As defined by Unicode Standard Definition
         * D128
         * isCased(X) when isLowercase(X) is false, or isUppercase(X) is false, or
isTitlecase(X) is false.
         */
        add(new SimpleBinaryProperty("Changes_When_Casemapped", "CWCM", version, new UnicodeSet()
        .addAll(getProperty("Changes_When_Lowercased").getSet(UCD_Names.YES))
        .addAll(getProperty("Changes_When_Uppercased").getSet(UCD_Names.YES))
        .addAll(getProperty("Changes_When_Titlecased").getSet(UCD_Names.YES))));

        // ========================

        add(new UnicodeProperty.UnicodeSetProperty().set("[\\u0000-\\u007F]").setMain("ASCII", "ASCII", UnicodeProperty.EXTENDED_BINARY, ""));

        final String x = Utility.getMostRecentUnicodeDataFile("ScriptExtensions", Settings.latestVersion, true, true);
        if (x == null) {
            System.out.println("ScriptExtensions not available for version");
        } else {
            final File f = new File(x);
            final ScriptExtensions extensions = ScriptExtensions.make(f.getParent(), f.getName());
            final Collection<BitSet> values = extensions.getAvailableValues();
            final TreeSet<BitSet> sortedValues = new TreeSet<BitSet>(ScriptExtensions.COMPARATOR);
            sortedValues.addAll(values);
            final UnicodeMap<String> umap = new UnicodeMap<String>();
            for (final BitSet set : sortedValues) {
                final UnicodeSet uset = extensions.getSet(set);
                umap.putAll(uset, ScriptExtensions.getNames(set, UCD_Types.SHORT, " "));
            }
            final UnicodeMapProperty prop2 = new UnicodeMapProperty()
            .set(umap);
            prop2.setMain("Script_Extensions", "scx", UnicodeProperty.MISC, version);
            prop2.addValueAliases(new String[][] {}, AliasAddAction.IGNORE_IF_MISSING); // hack
            //      for (BitSet set : sortedValues) {
            //        prop2.addValueAlias(ScriptExtensions.getNames(set, UProperty.NameChoice.SHORT, " "),
            //                ScriptExtensions.getNames(set, UProperty.NameChoice.LONG, " "),
            //                false);
            //      }
            add(prop2);
        }

        // Indic gorp

        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());

        UnicodeMap<String> map = latest.load(UcdProperty.Indic_Positional_Category);
        //String defaultValue = IndexUnicodeProperties.getDefaultValue(UcdProperty.Indic_Matra_Category);
        add(new UnicodeProperty.UnicodeMapProperty().set(map).setMain("Indic_Positional_Category", "InPC", UnicodeProperty.ENUMERATED, ""));

        map = latest.load(UcdProperty.Indic_Syllabic_Category);
        //defaultValue = IndexUnicodeProperties.getDefaultValue(UcdProperty.Indic_Matra_Category);
        add(new UnicodeProperty.UnicodeMapProperty().set(map).setMain("Indic_Syllabic_Category", "InSC", UnicodeProperty.ENUMERATED, ""));

    }

    private void addFakeProperty(String version, int unicodePropertyType, String defaultValue, String name, String abbr, String... alts) {
        final SimpleProperty item = new UnicodeProperty.SimpleProperty() {
            @Override
            public String _getValue(int codepoint) {
                return "";
            }
        };
        item.setValues(defaultValue);
        item.setMain(name, abbr, unicodePropertyType, version);
        for (final String alt : alts) {
            item.addName(alt);
        }
        add(item);
    }

    static String[] YES_NO_MAYBE      = { "N", "M", "Y" };

    static String[] LONG_YES_NO_MAYBE = { "No", "Maybe", "Yes" };

    static String[] YES_NO            = { "N", "Y" };

    static String[] LONG_YES_NO       = { "No", "Yes" };

    /*
     * "Bidi_Mirroring_Glyph", "Block", "Case_Folding", "Case_Sensitive",
     * "ISO_Comment", "Lowercase_Mapping", "Name", "Numeric_Value",
     * "Simple_Case_Folding", "Simple_Lowercase_Mapping",
     * "Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping",
     * "Titlecase_Mapping", "Unicode_1_Name", "Uppercase_Mapping", "isCased",
     * "isCasefolded", "isLowercase", "isNFC", "isNFD", "isNFKC", "isNFKD",
     * "isTitlecase", "isUppercase", "toNFC", "toNFD", "toNFKC", "toNKFD" });
     */

    /*
     * private class NameProperty extends UnicodeProperty.SimpleProperty {
     * {set("Name", "na", "<string>", UnicodeProperty.STRING);} public String
     * getPropertyValue(int codepoint) { if ((ODD_BALLS &
     * ucd.getCategoryMask(codepoint)) != 0) return null; return
     * ucd.getName(codepoint); } }
     */

    static class UCDPropertyWrapper extends UnicodeProperty {
        UCDProperty ucdProperty;

        boolean     yes_no_maybe;

        UCDPropertyWrapper(UCDProperty ucdProperty, int type, boolean yes_no_maybe) {
            this.ucdProperty = ucdProperty;
            setType(type);
            final String name = ucdProperty.getName(UCD_Types.LONG);
            if (name == null) {
                ucdProperty.getName(UCD_Types.SHORT);
            }
            setName(name);
            this.yes_no_maybe = yes_no_maybe;
            setUniformUnassigned(false);
        }

        @Override
        protected String _getVersion() {
            return ucdProperty.getUCD().getVersion();
        }

        @Override
        protected String _getValue(int codepoint) {
            final String result = ucdProperty.getValue(codepoint, UCD_Types.LONG);
            if (result.length() == 0) {
                return UCD_Names.NO;
            }
            return result;
        }

        @Override
        protected List<String> _getNameAliases(List<String> result) {
            addUnique(ucdProperty.getName(UCD_Types.SHORT), result);
            final String name = getName();
            addUnique(name, result);
            if (name.equals("White_Space")) {
                addUnique("space", result);
            }
            return result;
        }

        @Override
        protected List<String> _getValueAliases(String valueAlias, List<String> result) {
            if (isType(BINARY_MASK)) {
                lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
                // if (valueAlias.equals(UCD_Names.YES)) {
                // addUnique(UCD_Names.Y, result);
                // addUnique("True", result);
                // addUnique("T", result);
                // }
                // else if (valueAlias.equals(UCD_Names.NO)) {
                // addUnique(UCD_Names.N, result);
                // addUnique("False", result);
                // addUnique("F", result);
                // }
                addUnique(valueAlias, result);
            }
            if (yes_no_maybe) {
                lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
                // if (valueAlias.equals("Yes"))
                // addUnique("Y", result);
                // else if (valueAlias.equals("No"))
                // addUnique("N", result);
                // else if (valueAlias.equals("Maybe"))
                // addUnique("M", result);
                // addUnique(valueAlias, result);
            }
            return result;
        }

        @Override
        protected List<String> _getAvailableValues(List<String> result) {
            if (isType(BINARY_MASK)) {
                addUnique(UCD_Names.YES, result);
                addUnique(UCD_Names.NO, result);
            }
            if (yes_no_maybe) {
                addUnique("No", result);
                addUnique("Maybe", result);
                addUnique("Yes", result);
            }
            return result;
        }
    }

    static final int ODD_BALLS = (1 << UCD_Types.Cn) | (1 << UCD_Types.Cs) | (1 << UCD_Types.Co); // |
    // (1
    // <<
    // UCD.Cc)

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyAliases(java
     * .util.Collection)
     */

    private static final Relation<String, String> ALIAS_JOINING_GROUP
    = new Relation<String, String>(new HashMap<String, Set<String>>(), LinkedHashSet.class);
    static {
        ALIAS_JOINING_GROUP.put("Teh_Marbuta_Goal", "Hamza_On_Heh_Goal");
        ALIAS_JOINING_GROUP.freeze();
    }

    private class ToolUnicodeProperty extends UnicodeProperty {
        org.unicode.text.UCD.UCDProperty up;
        int                              propMask;

        private ToolUnicodeProperty(String propertyAlias) {
            propMask = UnifiedProperty.getPropmask(propertyAlias, ucd);
            up = UnifiedProperty.make(propMask, ucd);
            if (up == null) {
                throw new IllegalArgumentException("Not found: " + propertyAlias);
            }
            if (propertyAlias.equals("Case_Fold_Turkish_I")) {
                System.out.println(propertyAlias + " " + getTypeName(getType()));
            }
            setType(getPropertyTypeInternal());
            setName(propertyAlias);
            //if (up.hasUnassigned || (propMask >> 8) == (UCD_Types.AGE >> 8) ) {
            // always skip
            setUniformUnassigned(false);
            //}
        }

        @Override
        public List<String> _getAvailableValues(List<String> result) {
            if (result == null) {
                result = new ArrayList<String>();
            }
            final int type = getType() & CORE_MASK;
            if (type == STRING || type == MISC) {
                result.add("<string>");
            } else if (type == NUMERIC) {
                result.add("<number>");
            } else if (type == BINARY) {
                result.add(UCD_Names.YES);
                result.add(UCD_Names.NO);
            } else if (type == ENUMERATED || type == CATALOG) {
                final byte style = UCD_Types.LONG;
                final int prop = propMask >> 8;
    String temp = null;
    boolean titlecase = false;
    for (short i = 0; i < 256; ++i) {
        final boolean check = false;
        try {
            switch (prop) {
            case UCD_Types.CATEGORY >> 8:
                temp = (UCD.getCategoryID_fromIndex(i, style));
    break;
    case UCD_Types.COMBINING_CLASS >> 8:
        temp = (UCD.getCombiningClassID_fromIndex(i, style));
    break;
    case UCD_Types.BIDI_CLASS >> 8:
        temp = (UCD.getBidiClassID_fromIndex(i, style));
    break;
    case UCD_Types.DECOMPOSITION_TYPE >> 8:
        temp = (UCD.getDecompositionTypeID_fromIndex(i, style));
    // check = temp != null;
    break;
    case UCD_Types.NUMERIC_TYPE >> 8:
        temp = (UCD.getNumericTypeID_fromIndex(i, style));
    titlecase = true;
    break;
    case UCD_Types.EAST_ASIAN_WIDTH >> 8:
        temp = (UCD.getEastAsianWidthID_fromIndex(i, style));
    break;
    case UCD_Types.LINE_BREAK >> 8:
        temp = (UCD.getLineBreakID_fromIndex(i, style));
    break;
    case UCD_Types.JOINING_TYPE >> 8:
        temp = (UCD.getJoiningTypeID_fromIndex(i, style));
    break;
    case UCD_Types.JOINING_GROUP >> 8:
        temp = (UCD.getJoiningGroupID_fromIndex(i, style));
    break;
    case UCD_Types.SCRIPT >> 8:
        temp = (UCD.getScriptID_fromIndex(i, style));
    titlecase = true;
    if (UnicodeProperty.UNUSED.equals(temp)) {
        continue;
    }
    if (temp != null) {
        temp = UCharacter.toTitleCase(Locale.ENGLISH, temp, null);
    }
    break;
    case UCD_Types.AGE >> 8:
        temp = (UCD.getAgeID_fromIndex(i, style));
    break;
    case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
        temp = (UCD.getHangulSyllableTypeID_fromIndex(i, style));
    break;
    default:
        throw new IllegalArgumentException("Internal Error: " + prop);
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            continue;
        }
        if (check) {
            System.out.println("Value: " + temp);
        }
        if (temp != null && temp.length() != 0 && !temp.equals(UNUSED)) {
            result.add(Utility.getUnskeleton(temp, titlecase));
        }
        if (check) {
            System.out.println("Value2: " + temp);
        }
    }
    // if (prop == (UCD_Types.DECOMPOSITION_TYPE>>8)) result.add("none");
    // if (prop == (UCD_Types.JOINING_TYPE>>8)) result.add("Non_Joining");
    // if (prop == (UCD_Types.NUMERIC_TYPE>>8)) result.add("None");
            }
            return result;
        }

        @Override
        public List<String> _getNameAliases(List<String> result) {
            if (result == null) {
                result = new ArrayList<String>();
            }
            addUnique(Utility.getUnskeleton(up.getName(UCD_Types.SHORT), false), result);
            final String longName = up.getName(UCD_Types.LONG);
            addUnique(Utility.getUnskeleton(longName, true), result);
            // hack
            if (longName.equals("White_Space")) {
                addUnique("space", result);
            }
            return result;
        }

        @Override
        public List<String> _getValueAliases(String valueAlias, List<String> result) {
            if (result == null) {
                result = new ArrayList<String>();
            }
            final int type = getType() & CORE_MASK;
            if (type == STRING || type == MISC || type == NUMERIC) {
                UnicodeProperty.addUnique(valueAlias, result);
                return result;
            } else if (type == BINARY) {
                // UnicodeProperty.addUnique(valueAlias, result);
                return lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
            } else if (type == ENUMERATED || type == CATALOG) {
                final int prop = propMask >> 8;
                for (int i = 0; i < 256; ++i) {
                    try {
                        switch (prop) {
                        case UCD_Types.CATEGORY >> 8:
                            return lookup(valueAlias, UCD_Names.LONG_GENERAL_CATEGORY,
                                    UCD_Names.GENERAL_CATEGORY, UCD_Names.EXTRA_GENERAL_CATEGORY, result);
                case UCD_Types.COMBINING_CLASS >> 8:
                    addUnique(String.valueOf(Utility.lookupShort(valueAlias,
                            UCD_Names.LONG_COMBINING_CLASS, true)), result);
                return lookup(valueAlias, UCD_Names.LONG_COMBINING_CLASS,
                        UCD_Names.COMBINING_CLASS, null, result);
                case UCD_Types.BIDI_CLASS >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_BIDI_CLASS, UCD_Names.BIDI_CLASS, null,
                            result);
                case UCD_Types.DECOMPOSITION_TYPE >> 8:
                    lookup(valueAlias, UCD_Names.LONG_DECOMPOSITION_TYPE, FIXED_DECOMPOSITION_TYPE,
                            null, result);
                return lookup(valueAlias, UCD_Names.LONG_DECOMPOSITION_TYPE,
                        UCD_Names.DECOMPOSITION_TYPE, null, result);
                case UCD_Types.NUMERIC_TYPE >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_NUMERIC_TYPE, UCD_Names.NUMERIC_TYPE,
                            null, result);
                case UCD_Types.EAST_ASIAN_WIDTH >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_EAST_ASIAN_WIDTH,
                            UCD_Names.EAST_ASIAN_WIDTH, null, result);
                case UCD_Types.LINE_BREAK >> 8:
                    lookup(valueAlias, UCD_Names.LONG_LINE_BREAK, UCD_Names.LINE_BREAK, null, result);
                if (valueAlias.equals("Inseparable")) {
                    addUnique("Inseperable", result);
                }
                // Inseparable; Inseperable
                return result;
                case UCD_Types.JOINING_TYPE >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_JOINING_TYPE, UCD_Names.JOINING_TYPE,
                            null, result);
                case UCD_Types.JOINING_GROUP >> 8:
                    return lookup(valueAlias, UCD_Names.JOINING_GROUP, UCD_Names.JOINING_GROUP, ALIAS_JOINING_GROUP, result);
                case UCD_Types.SCRIPT >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_SCRIPT, UCD_Names.SCRIPT,
                            UCD_Names.EXTRA_SCRIPT, result);
                case UCD_Types.AGE >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_AGE, UCD_Names.SHORT_AGE, null, result);
                case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
                    return lookup(valueAlias, UCD_Names.LONG_HANGUL_SYLLABLE_TYPE,
                            UCD_Names.HANGUL_SYLLABLE_TYPE, null, result);
                default:
                    throw new IllegalArgumentException("Internal Error: " + prop);
                        }
                    } catch (final ArrayIndexOutOfBoundsException e) {
                        continue;
                    }
                }
            }
            throw new ArrayIndexOutOfBoundsException("not supported yet");
        }

        @Override
        public String _getValue(int codepoint) {
            final byte style = UCD_Types.LONG;
            String temp = null;
            boolean titlecase = false;
            switch (propMask >> 8) {
            case UCD_Types.CATEGORY >> 8:
                temp = (UCD.getCategoryID_fromIndex(ucd.getCategory(codepoint), style));
            break;
            case UCD_Types.COMBINING_CLASS >> 8:
                temp = (UCD.getCombiningClassID_fromIndex(ucd.getCombiningClass(codepoint), style));
            // if (temp.startsWith("Fixed_")) temp = temp.substring(6);
            break;
            case UCD_Types.BIDI_CLASS >> 8:
                temp = (UCD.getBidiClassID_fromIndex(ucd.getBidiClass(codepoint), style));
            break;
            case UCD_Types.DECOMPOSITION_TYPE >> 8:
                temp = (UCD.getDecompositionTypeID_fromIndex(ucd.getDecompositionType(codepoint), style));
            if (temp == null || temp.length() == 0) {
                temp = "none";
            }
            break;
            case UCD_Types.NUMERIC_TYPE >> 8:
                temp = (UCD.getNumericTypeID_fromIndex(ucd.getNumericType(codepoint), style));
            titlecase = true;
            if (temp == null || temp.length() == 0) {
                temp = "None";
            }
            break;
            case UCD_Types.EAST_ASIAN_WIDTH >> 8:
                temp = (UCD.getEastAsianWidthID_fromIndex(ucd.getEastAsianWidth(codepoint), style));
            break;
            case UCD_Types.LINE_BREAK >> 8:
                temp = (UCD.getLineBreakID_fromIndex(ucd.getLineBreak(codepoint), style));
            break;
            case UCD_Types.JOINING_TYPE >> 8:
                temp = (UCD.getJoiningTypeID_fromIndex(ucd.getJoiningType(codepoint), style));
            if (temp == null || temp.length() == 0) {
                temp = "Non_Joining";
            }
            break;
            case UCD_Types.JOINING_GROUP >> 8:
                temp = (UCD.getJoiningGroupID_fromIndex(ucd.getJoiningGroup(codepoint), style));
            break;
            case UCD_Types.SCRIPT >> 8:
                temp = (UCD.getScriptID_fromIndex(ucd.getScript(codepoint), style));
            if (temp != null) {
                temp = UCharacter.toTitleCase(Locale.ENGLISH, temp, null);
            }
            titlecase = true;
            break;
            case UCD_Types.AGE >> 8:
                temp = getAge(codepoint);
            break;
            case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
                temp = (UCD.getHangulSyllableTypeID_fromIndex(ucd.getHangulSyllableType(codepoint), style));
            break;
            }
            if (temp != null) {
                return Utility.getUnskeleton(temp, titlecase);
            }
            if (isType(BINARY_MASK)) {
                return up.hasValue(codepoint) ? "Yes" : "No";
            }
            throw new IllegalArgumentException("Failed to find value for " + Utility.hex(codepoint));
        }

        public String getAge(int codePoint) {
            if (codePoint == 0x1FFFE) {
                System.out.println("debug point");
            }
            if (needAgeCache) {
                for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
                    final String version = UCD_Types.AGE_VERSIONS[i];
                    if (version.compareTo(ucd.getVersion()) > 0) {
                        break;
                    }
                    ucdCache[i] = UCD.make(version);
                }
                needAgeCache = false;
            }
            for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
                if (ucdCache[i] == null) {
                    break;
                }
                if (ucdCache[i].isAllocated(codePoint)) {
                    return UCD_Names.LONG_AGE[i];
                }
            }
            return UCD_Names.LONG_AGE[UCD_Types.UNKNOWN];
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyType()
         */
        private int getPropertyTypeInternal() {

            switch (propMask) {
            case UCD_Types.BINARY_PROPERTIES | UCD_Types.CaseFoldTurkishI:
            case UCD_Types.BINARY_PROPERTIES | UCD_Types.Non_break:
                return EXTENDED_BINARY;
            }

            switch (propMask >> 8) {
            case UCD_Types.SCRIPT >> 8:
            case UCD_Types.AGE >> 8:
                return CATALOG;
            }
            int mask = 0;
            if (!up.isStandard()) {
                mask = EXTENDED_MASK;
            }
            return remapUCDType(up.getValueType()) | mask;
        }

        @Override
        public String _getVersion() {
            return up.ucd.getVersion();
        }

    }

    private static int remapUCDType(int result) {
        switch (result) {
        case UCD_Types.NUMERIC_PROP:
            result = UnicodeProperty.NUMERIC;
            break;
        case UCD_Types.STRING_PROP:
            result = UnicodeProperty.STRING;
            break;
        case UCD_Types.MISC_PROP:
            result = UnicodeProperty.STRING;
            break;
        case UCD_Types.CATALOG_PROP:
            result = UnicodeProperty.ENUMERATED;
            break;
        case UCD_Types.FLATTENED_BINARY_PROP:
        case UCD_Types.ENUMERATED_PROP:
            result = UnicodeProperty.ENUMERATED;
            break;
        case UCD_Types.BINARY_PROP:
            result = UnicodeProperty.BINARY;
            break;
        case UCD_Types.UNKNOWN_PROP:
        default:
            result = UnicodeProperty.STRING;
            // throw new IllegalArgumentException("Type: UNKNOWN_PROP");
        }
        return result;
    }

    static public boolean equals(int codepoint, String string) {
        return UTF16.valueOf(codepoint).equals(string);
    }

    static List<String> lookup(String valueAlias, String[] main, String[] aux,
            Relation<String, String> aux2, List<String> result) {
        // System.out.println(valueAlias + "=>");
        // System.out.println("=>" + aux[pos]);
        if (aux != null) {
            final int pos = Utility.lookupShort(valueAlias, main, true);
            UnicodeProperty.addUnique(aux[pos], result);
        }
        UnicodeProperty.addUnique(valueAlias, result);
        if (aux2 != null) {
            final Set<String> xtra = aux2.getAll(valueAlias);
            if (xtra != null) {
                for (final String extraItem : xtra) {
                    UnicodeProperty.addUnique(extraItem, result);
                }
            }
        }
        return result;
    }

    /*
     * static class DerivedPropertyWrapper extends UnicodeProperty { UCDProperty
     * derivedProperty; UCD ucd;
     * 
     * DerivedPropertyWrapper(int derivedPropertyID, UCD ucd) { this.ucd = ucd;
     * derivedProperty = DerivedProperty.make(derivedPropertyID, ucd); } protected
     * String _getVersion() { return ucd.getVersion(); }
     * 
     * protected String _getValue(int codepoint) { return
     * derivedProperty.getValue(codepoint, UCD_Types.LONG); } protected List
     * _getNameAliases(List result) { if (result != null) result = new
     * ArrayList(1); addUnique(derivedProperty.getName(UCD_Types.SHORT), result);
     * addUnique(derivedProperty.getName(UCD_Types.LONG), result); return null; }
     * 
     * protected List _getValueAliases(String valueAlias, List result) { // TODO
     * Auto-generated method stub return null; } protected List
     * _getAvailableValues(List result) { // TODO Auto-generated method stub
     * return null; }
     * 
     * }
     */

    public static class IdnaInfo {
        public enum IdnaType {
            OK, DELETED, ILLEGAL, REMAPPED
        };

        private final UCD                      ucdIdna = UCD.makeLatestVersion();
        private final StringPrep               namePrep;

        IdnaInfo() throws IOException {
            namePrep = StringPrep.getInstance(StringPrep.RFC3491_NAMEPREP);
            // InputStream stream =
            // ICUData.getRequiredStream(ICUResourceBundle.ICU_BUNDLE+"/uidna.spp");
            // namePrep = new StringPrep(stream);
            // stream.close();
        }

        public IdnaType getIDNAType(int cp) {
            if (ucdIdna.isPUA(cp) || !ucdIdna.isAllocated(cp)) {
                return IdnaType.ILLEGAL;
            }
            if (cp == '-') {
                return IdnaType.OK;
            }
            final String source = UTF16.valueOf(cp);
            try {
                String result = namePrep.prepare(source, IDNA.DEFAULT);
                if (!source.equals(result)) {
                    return IdnaType.REMAPPED;
                }
            } catch (final StringPrepParseException e) {
                return IdnaType.ILLEGAL;
            } catch (final Exception e) {
                System.out.println("Failure at: " + Utility.hex(cp));
                return IdnaType.ILLEGAL;
            }
            return IdnaType.OK;
        }
    }

    static final Pattern WELL_FORMED_LANGUAGE_TAG = Pattern.compile("..."); // ...
    // is
    // ugly
    // mess
    // that
    // someone
    // supplies

    static boolean isWellFormedLanguageTag(String tag) {
        return WELL_FORMED_LANGUAGE_TAG.matcher(tag).matches();
    }

    public static final Relation<String, String> YNTF = new Relation<String, String>(
            new TreeMap<String, Set<String>>(),
            LinkedHashSet.class);
    static {
        YNTF.putAll("Yes", Arrays.asList(YES_VALUES));
        YNTF.putAll("No", Arrays.asList(NO_VALUES));
        YNTF.putAll("Maybe", Arrays.asList(MAYBE_VALUES));
    }

    private static final String[]                FIXED_DECOMPOSITION_TYPE = new String[UCD_Names.DECOMPOSITION_TYPE.length];
    static {
        for (int i = 0; i < UCD_Names.DECOMPOSITION_TYPE.length; ++i) {
            FIXED_DECOMPOSITION_TYPE[i] = Utility.getUnskeleton(UCD_Names.DECOMPOSITION_TYPE[i], true);
        }
    }

    static class SimpleBinaryProperty extends UnicodeProperty.SimpleProperty {
        UnicodeSet items;

        SimpleBinaryProperty(String name, String shortName, String version, UnicodeSet items) {
            this.items = items;
            setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases();
            setMain(name, shortName, UnicodeProperty.BINARY, version);
        }

        @Override
        protected String _getValue(int codepoint) {
            return items.contains(codepoint) ? UCD_Names.YES : UCD_Names.NO;
        }
    }

    static class SimpleIsProperty extends UnicodeProperty.SimpleProperty {
        private final UnicodeProperty property;
        private final boolean samePolarity;

        SimpleIsProperty(String name, String shortName, String version, UnicodeProperty property, boolean samePolarity) {
            this.property = property;
            setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases();
            setMain(name, shortName, UnicodeProperty.BINARY, version);
            this.samePolarity = samePolarity;
        }

        @Override
        protected String _getValue(int codepoint) {
            final String value = property.getValue(codepoint);
            return value == null ? samePolarity ? UCD_Names.YES : UCD_Names.NO // null means same value, by convention
                    : equals(codepoint, value) == samePolarity ? UCD_Names.YES : UCD_Names.NO;
        }

        SimpleIsProperty setExtended() {
            setType(UnicodeProperty.EXTENDED_BINARY);
            return this;
        }

        SimpleIsProperty setCheckUnassigned() {
            setUniformUnassigned(false);
            return this;
        }
    }

    /**
     * Removes set elements from the string.
     * Consider using UnicodeSetSpanner(set).deleteFrom(b).
     */
    public String removeFrom(String b, UnicodeSet set) {
        if (set.containsNone(b)) {
            return b;
        }
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length();) {
            final int end = set.matchesAt(b,i);
            if (end > i) {
                i = end;
            } else {
                result.append(b.charAt(i));
                ++i;
            }
        }
        return result.toString();
    }

    private String changesWhenCased(int codepoint, byte caseType) {
        final String nfdCodepoint = nfd.normalize(codepoint);
        return !nfdCodepoint.equals(ucd.getCase(nfdCodepoint, UCD_Types.FULL, caseType))
                ? UCD_Names.YES
                        : UCD_Names.NO;
    }
}
