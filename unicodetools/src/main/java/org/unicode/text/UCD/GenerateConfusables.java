/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateConfusables.java,v $
 * $Date: 2010-06-19 00:29:21 $
 * $Revision: 1.32 $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XEquivalenceClass;
import org.unicode.cldr.util.XEquivalenceClass.Linkage;
import org.unicode.cldr.util.props.BagFormatter;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Uts46;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.ScriptInfo;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.NFKD_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeTransform;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Confusables;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;


public class GenerateConfusables {
    static {
        System.setProperty("SCRIPT_UNICODE_VERSION", Default.ucdVersion());
    }
    private static final String REFERENCE_VERSION = Settings.lastVersion;

    static final Normalizer NFKD = Default.nfkd();
    private static final ToolUnicodeTransformFactory TOOL_FACTORY = new ToolUnicodeTransformFactory();
    // Align these three normally.
    private static final String version = Settings.latestVersion;
    private static final String REVISION = Settings.latestVersion;
    static final String VERSION_PROP_VALUE = REVISION; // "V7_0";

    static final String reformatedInternal = Settings.UnicodeTools.UNICODETOOLS_DIR + "data/security/" + REVISION + "/data/";
    public static final String GEN_SECURITY_DIR = Settings.Output.GEN_DIR + "security/" + REVISION + "/";

    //    static final XIDModifications REFERENCE_VALUES = new XIDModifications(Settings.UNICODETOOLS_DIRECTORY + "data/security/"
    //            + REFERENCE_VERSION
    //            + "/xidmodifications.txt");

    //static final UnicodeSet OLD_CONFUSABLE_TARGETS = new UnicodeSet();
    static final Counter<String> LAST_COUNT = new Counter<String>();

    static final boolean DEBUG = false;

    static {
        Confusables REFERENCE_VALUES = new Confusables(Settings.UnicodeTools.UNICODETOOLS_DIR + "data/security/"
            + REFERENCE_VERSION);
        for (EntryRange<String> entry : REFERENCE_VALUES.getRawMapToRepresentative(Confusables.Style.MA).entryRanges()) {
            if (entry.string != null) {
                LAST_COUNT.add(entry.value, 1);
            } else {
                LAST_COUNT.add(entry.value, entry.codepointEnd - entry.codepoint + 1);
            }
        }
//        OLD_CONFUSABLE_TARGETS.addAll(REFERENCE_VALUES.getStyle2map().get(Confusables.Style.MA).values()).freeze();
//        System.out.println(OLD_CONFUSABLE_TARGETS.toPattern(false));
        for (String s : LAST_COUNT.getKeysetSortedByCount(false)) {
            if (DEBUG) System.out.println(LAST_COUNT.get(s) + "\t" + s + "\tU+" + Utility.hex(s) + "\t" + Default.ucd().getName(s));
        }
    }

    static final String indir = reformatedInternal + "source/";
    static final UCD DEFAULT_UCD = Default.ucd();
    static final UnicodeProperty.Factory ups = ToolUnicodePropertySource.make(version); // ICUPropertyFactory.make();
    static {
        // USE the tool unicode set instead of ICU, which may not be using the latest version.
        UnicodeSet.setDefaultXSymbolTable(ups.getXSymbolTable());
        UnicodeTransform.setFactory(TOOL_FACTORY);
    }
    static final UnicodeSet COMMON_OR_INHERITED;
    static final UnicodeSet CASED;
    static final UnicodeSet COMMON_OR_INHERITED_NFKD;
    static final UnicodeSet CASED_NFKD;
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(version);
    static final UnicodeMap<Set<Script_Values>> scriptExtensions = iup.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
    static final UnicodeSet notNFKD = iup.loadEnum(UcdProperty.NFKD_Quick_Check, NFKD_Quick_Check_Values.class).getSet(NFKD_Quick_Check_Values.No);
    static {
        UnicodeSet common = scriptExtensions.getSet(Collections.singleton(Script_Values.Common));
        UnicodeSet inherited = scriptExtensions.getSet(Collections.singleton(Script_Values.Inherited));
        COMMON_OR_INHERITED = new UnicodeSet(common).addAll(inherited).freeze();
        CASED = iup.loadEnum(UcdProperty.Changes_When_Casefolded, Binary.class).getSet(Binary.Yes);
        COMMON_OR_INHERITED_NFKD = new UnicodeSet(COMMON_OR_INHERITED);
        CASED_NFKD = new UnicodeSet(CASED);
        for (String s: notNFKD) {
            if (s.equals("𝐉")) {
                int debug = 0;
            }
            String nfkd = NFKD.normalize(s);
            if (!COMMON_OR_INHERITED_NFKD.containsAll(nfkd)) {
                COMMON_OR_INHERITED_NFKD.remove(s);
            }
            if (CASED_NFKD.containsSome(nfkd)) {
                CASED_NFKD.add(s);
            }
        }
        COMMON_OR_INHERITED_NFKD.freeze();
        CASED_NFKD.freeze();
    }

    private static final UnicodeProperty SCRIPT_PROPERTY = ups.getProperty("sc");
    static final UnicodeProperty AGE = ups.getProperty("age");

    private static final String EXCAPE_FUNNY_RULE = 
            ":: [[:C:]-[:cn:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]] hex/unicode ; ";

    static final Transliterator EXCAPE_FUNNY = Transliterator.createFromRules(
            "any-html", EXCAPE_FUNNY_RULE, Transliterator.FORWARD);

    static BagFormatter makeFormatter() {
        return new BagFormatter(ups)
        .setLineSeparator("\n");
    }

    private static final boolean SHOW_SUPPRESS = false;
    //    private static boolean EXCLUDE_CONFUSABLE_COMPAT = true;
    static String recommended_scripts = "recommended";

    //    private static final UnicodeSet SPECIAL = new UnicodeSet("[\u01DD\u0259]").freeze();

    public static void main(String[] args) throws IOException {
        System.setProperty("line.separator", "\n");
        //quickTest();
        if (args.length == 0) {
            args = new String[] {"-b", "-c"};
        }
        try {
            for (final String arg : args) {
                // use -b -c for the normal data files
                if (arg.equalsIgnoreCase("-b")) {
                    generateIDN();
                } else if (arg.equalsIgnoreCase("-c")) {
                    generateConfusables();
                } else if (arg.equalsIgnoreCase("-d")) {
                    generateDecompFile();
                } else if (arg.equalsIgnoreCase("-s")) {
                    generateSource();
                } else if (arg.equalsIgnoreCase("-l")) {
                    generateLatin();
                } else if (arg.equalsIgnoreCase("-a")) {
                    generateAsciify();
                } else {
                    throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Done");
            System.out.println("!!! Remember to run TestSecurity.java, after refreshing the generated files in Eclipse at "
                    + GEN_SECURITY_DIR
                    + " !!!");
        }
    }

    private static void generateAsciify() throws IOException {
        BufferedReader in = FileUtilities.openUTF8Reader(indir, "asciify.txt");
        final StringBuilder builder = new StringBuilder();
        if (DEBUG) System.out.println("String rules = \"\"");
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            builder.append(line).append('\n');
            if (DEBUG) System.out.println(" + \"" + com.ibm.icu.impl.Utility.escape(line) + "\\n\"");
        }
        if (DEBUG) System.out.println(";");
        in.close();
        final String rules = builder.toString();
        final Transliterator asciify = Transliterator.createFromRules("asciify", rules, Transliterator.FORWARD);
        in = FileUtilities.openUTF8Reader(indir, "asciify_examples.txt");
        if (DEBUG) System.out.println("String[][] translitTestCases = {");
        if (DEBUG) System.out.println("//{\"" + "SAMPLE" + "\", \"" + "EXPECTED TRANSFORM" + "\"},");
        while (true) {
            final String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            if (DEBUG) System.out.println("{\"" + com.ibm.icu.impl.Utility.escape(line) + "\", \"" + asciify.transform(line) + "\"},");
        }
        if (DEBUG) System.out.println("};");
        in.close();
    }

    private static final UnicodeSet LATIN = new UnicodeSet("[:script=latin:]").freeze();
    private static final UnicodeSet LATIN_PLUS  = new UnicodeSet("[[:script=latin:][:script=common:][:script=inherited:]]").freeze();
    private static final UnicodeSet ASCII = new UnicodeSet("[:ASCII:]").freeze();
    private static final UnicodeSet MARKS_AND_ASCII = new UnicodeSet("[[:mark:][:ASCII:]]").freeze();

    private static void generateLatin() throws IOException {
        // pick out only those items where the source and target both have some latin, and no non-latin
        final Map<String,String> mapping = new TreeMap<String,String>(UCAComparator);
        addLatin(mapping, "confusables-source.txt");
        addLatin(mapping, "confusables-intentional.txt");
        final Set<String> sorted = new TreeSet(UCAComparator);
        final UnicodeSet sourceSet = new UnicodeSet();
        for (final ULocale locale : ULocale.getAvailableLocales()) {
            sourceSet.addAll(LocaleData.getExemplarSet(locale, 0));
        }
        sourceSet.retainAll(LATIN).removeAll(ASCII);
        sourceSet.closeOver(UnicodeSet.CASE);
        sourceSet.addAllTo(sorted);
        for (final String source : sorted) {
            if (source.length() > 1 || !Default.nfkc().isNormalized(source)) {
                continue;
            }
            String reason = "CON";
            String target = mapping.get(source);
            if (target == null) {
                reason = "NFC";
                target = Default.nfc().normalize(source);
                if (target.equals(source)) {
                    reason = "!CH";
                }
            }
            if (new UnicodeSet().addAll(Default.nfd().normalize(target)).removeAll(MARKS_AND_ASCII).size() > 0) {
                reason += " XXX";
            }
            if (DEBUG) System.out.println(source + "\t→\t" + target +
                    " ; #" + reason + "\t" + DEFAULT_UCD.getCodeAndName(source) + "\t→\t" + DEFAULT_UCD.getCodeAndName(target));

        }
    }

    private static void addLatin(Map<String, String> mapping, String fileName) throws IOException {

        final BufferedReader in = FileUtilities.openUTF8Reader(indir, fileName);
        while (true) {
            String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            final String oldLine = line = line.trim();
            try {
                if (line.length() == 0) {
                    continue;
                }
                final String[] pieces = line.split(";");
                final String source = fromHex(pieces[0]);
                final String target = fromHex(pieces[1]);
                // must contain some latin
                if (!LATIN.containsSome(source) || !LATIN.containsSome(target)) {
                    continue;
                }
                // mustn't contain anything else
                if (!LATIN_PLUS.containsAll(source) && !LATIN_PLUS.containsAll(target)) {
                    continue;
                }

                final String old = mapping.get(source);
                if (old!=null) {
                    if (DEBUG) System.out.println("Overriding " + source + "=>" + old + " with " + target);
                }

                // skip NFKC forms
                if (NFKD.normalize(source).equals(NFKD.normalize(target))) {
                    if (old != null) {
                        mapping.remove(source);
                    }
                    continue;
                }


                mapping.put(source, target);
            } catch (final Exception e) {
                throw (RuntimeException) new IllegalArgumentException("Can't process <" + oldLine + ">").initCause(e);
            }
        }
        in.close();
    }

    private static Matcher HEX = Pattern.compile(
            "\\b([A-F0-9]{4,6})\\b" +
                    "|U+([a-fA-F0-9]{4,6})\\b" +
                    "|\\\\u([a-fA-F0-9]{4})" +
                    "|\\\\U([a-fA-F0-9]{6})" +
            "|\\\\u\\{([a-fA-F0-9]{1,6})\\}").matcher("");
    /**
     * Convert a string with a mixture of hex and normal characters.
     * Anything like the following is converted from hex to chars
     * and all spaces are removed
     * hexChar = \b[A-F0-9]{4,6}\b
     * | U+[a-fA-F0-9]{4,6}
     * | \\u[a-fA-F0-9]{4}
     * | \\U[a-fA-F0-9]{6}
     * | \\u{[a-fA-F0-9]{1,6}
     * @param hexOrChars
     * @return
     */
    private static String fromHexLenient(String hexOrChars) {
        final StringBuilder result = new StringBuilder();
        HEX.reset(hexOrChars);
        int start = 0;
        while (HEX.find(start)) {
            final int end = HEX.start();
            result.append(hexOrChars.substring(start, end).replace(" ", ""));
            for (int i = 1; i <= HEX.groupCount(); ++i) {
                final String group = HEX.group(i);
                if (group != null) {
                    result.appendCodePoint(Integer.parseInt(group, 16));
                    break;
                }
            }
            start = HEX.end();
        }
        result.append(hexOrChars.substring(start).replace(" ", ""));
        return result.toString();
    }

    //    private static void quickTest() {
    //        int script = getSingleScript("\u0430\u0061");
    //        script = getSingleScript("\u0061\u0430"); //0323 ;  093C
    //        final String a = "\u0323";
    //        final String b = "\u093C";
    //        final int isLess = betterTargetIsLess.compare(a, b); // ("\u0045", "\u13AC");
    //        final MyEquivalenceClass test = new MyEquivalenceClass();
    //        test.add(a, b, "none");
    //        final Set x = test.getEquivalences(a);
    //        final String result = (String) CollectionUtilities.getBest(x, betterTargetIsLess, -1);
    //    }

    /**
     * 
     */
    //  private static UnicodeSet _Non_IICore;
    //
    //  private static UnicodeSet getNonIICore() {
    //    //Main + IICore + (Ext-A intersect Chinese)
    //    //blk; n/a       ; CJK_Unified_Ideographs
    //    //blk; n/a       ; CJK_Unified_Ideographs_Extension_A
    //    //blk; n/a       ; CJK_Unified_Ideographs_Extension_B
    //
    //    if (_Non_IICore == null) {
    //      // stuff to remove
    //      _Non_IICore = ups.getSet("block=CJK_Unified_Ideographs_Extension_A");
    //      _Non_IICore.addAll(ups.getSet("block=CJK_Unified_Ideographs_Extension_B"));
    //      _Non_IICore.removeAll(UNASSIGNED); // remove unassigned
    //      // stuff to restore
    //      UnicodeMap um = DEFAULT_UCD.getHanValue("kIICore");
    //      if (DEBUG) System.out.println("IICORE SIZE:\t" + um.keySet().size());
    //      um.put(0x34E4, "2.1");
    //      um.put(0x3007, "2.1");
    //      _Non_IICore.removeAll(um.keySet("2.1"));
    //
    //      // add Chinese?
    //      if (true) {
    //        UnicodeSet cjk_nic = new UnicodeSet();
    //        String line = null;
    //        try {
    //          BufferedReader br = FileUtilities.openUTF8Reader(indir, "cjk_nic.txt");
    //          while (true) {
    //            line = Utility.readDataLine(br);
    //            if (line == null) break;
    //            if (line.length() == 0) continue;
    //            String[] pieces = Utility.split(line, ';');
    //            // part 0 is range
    //            String range = pieces[0].trim();
    //            int rangeDivider = range.indexOf("..");
    //            int start, end;
    //            if (rangeDivider < 0) {
    //              start = end = Integer.parseInt(range, 16);
    //            } else {
    //              start = Integer.parseInt(range.substring(0, rangeDivider), 16);
    //              end = Integer.parseInt(range.substring(rangeDivider+2), 16);
    //            }
    //            cjk_nic.add(start, end);
    //          }
    //          br.close();
    //        } catch (Exception e) {
    //          throw (RuntimeException) new RuntimeException("Failure on line " + line).initCause(e);
    //        }
    //        _Non_IICore.removeAll(cjk_nic);
    //      }
    //    }
    //    return _Non_IICore;
    //    //		for (Iterator it = um.getAvailableValues().iterator(); it.hasNext();) {
    //    //			Object value = it.next();
    //    //			UnicodeSet set = um.getSet(value);
    //    //			if (DEBUG) System.out.println(value + "\t" + set);
    //    //		}
    //  }

    private static PrintWriter log;
    private static final String ARROW = "→"; // \u2194
    private static final String BACKARROW = "\u2190";

    static UnicodeSet UNASSIGNED =
            ups.getSet("gc=Cn")
            .addAll(ups.getSet("gc=Co"))
            .addAll(ups.getSet("gc=Cs")).freeze();
    private static UnicodeSet SKIP_SET =
            ups.getSet("gc=Cc")
            .addAll(ups.getSet("gc=Cf"))
            .addAll(UNASSIGNED).freeze();
    private static UnicodeSet WHITESPACE = ups.getSet("Whitespace=Yes").freeze();
    static UnicodeSet GC_LOWERCASE = ups.getSet("gc=Ll").freeze();
    private static UnicodeSet _skipNFKD;
    private static UnicodeSet COMBINING =
            ups.getSet("gc=Mn")
            .addAll(ups.getSet("gc=Me"))
            .add(0x3099)
            .add(0x309A).freeze();
    private static UnicodeSet INVISIBLES =
            ups.getSet("default-ignorable-codepoint=true").freeze();
    private static UnicodeSet XIDContinueSet =
            ups.getSet("XID_Continue=true").freeze();
    private static UnicodeSet XID = XIDContinueSet;
    private static UnicodeSet RTL = new UnicodeSet("[[:bc=R:][:bc=AL:][:bc=AN:]]").freeze();
    private static UnicodeSet CONTROLS = new UnicodeSet("[[:cc:][:Zl:][:Zp:]]").freeze();
    private static final char LRM = '\u200E';
    private static UnicodeSet commonAndInherited = new UnicodeSet("[[:script=common:][:script=inherited:]]");


    private static Map gatheredNFKD = new TreeMap();
    private static UnicodeMap nfcMap;
    private static UnicodeMap nfkcMap;

    private static Comparator codepointComparator = new UTF16.StringComparator(true,false,0);
    static Comparator UCAComparator = new org.unicode.cldr.util.MultiComparator(new Comparator[] {
            Collator.getInstance(ULocale.ROOT), 
            //UCA.buildCollator(null),
            codepointComparator});

    private static UnicodeSet setsToAbbreviate = new UnicodeSet("[" +
            "\\u3400-\\u4DB5" +
            "\\u4E00-\\u9FA5" +
            "\\uA000-\\uA48C" +
            "\\uAC00-\\uD7A3" +
            "\\u1100-\\u11FF" +
            "\\uFB00-\\uFEFC" +
            "\\u2460-\\u24FF" +
            "\\u3251-\\u33FF" +
            "\\u4DC0-\\u4DFF" +
            "\\u3165-\\u318E" +
            "\\uA490-\\uA4C6" +
            "\\U00010140-\\U00010174" +
            "\\U0001D300-\\U0001D356" +
            "\\U0001D000-\\U0001D1DD" +
            "\\U00020000-\\U0002A6D6" +
            "\\U0001D400-\\U0001D7FF" +
            "[:script=Canadian_Aboriginal:]" +
            "[:script=ETHIOPIC:]" +
            "[:script=Tagalog:]" +
            "[:script=Hanunoo:]" +
            "[:script=Buhid:]" +
            "[:script=Tagbanwa:]" +
            "[:script=Deseret:]" +
            "[:script=Shavian:]" +
            "[:script=Ogham:]" +
            "[:script=Old Italic:]" +
            "[:script=Runic:]" +
            "[:script=Gothic:]" +
            "[:script=Ugaritic:]" +
            "[:script=Linear B:]" +
            "[:script=Cypriot:]" +
            "[:script=Coptic:]" +
            "[:script=Syriac:]" +
            "[:script=Glagolitic:]" +
            "[:script=Glagolitic:]" +
            "[:script=Old Persian:]" +
            "[:script=Kharoshthi:]" +
            "[:script=Osmanya:]" +
            "[:default ignorable code point:]" +
            "]").freeze();

    /**
     * @throws IOException
     * 
     */
    private static void generateIDN() throws IOException {
        final IdentifierInfo info = IdentifierInfo.getIdentifierInfo();
        info.printIDNStuff();
	generateDecompFile();
    }

    //    static final String PROHIBITED = "Restricted ; ";
    //    static final String UNPROHIBITED = "Allowed ; ";
    private static final boolean suppress_NFKC = true;
    /**
     * 
     */


    /**
     * 
     */
    static void generateDecompFile() throws IOException {
        final PrintWriter out = FileUtilities.openUTF8Writer(reformatedInternal, "decomps.txt");
        final UnicodeProperty dt = ups.getProperty("Decomposition_Type");
        for (final Iterator it = dt.getAvailableValues().iterator(); it.hasNext();) {
            final String value = (String) it.next();
            if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("canonical")) {
                continue;
            }
            final UnicodeSet s = dt.getSet(value);
            out.println("");
            out.println("# Decomposition_Type = " + value);
            out.println("");
            for (final UnicodeSetIterator usi = new UnicodeSetIterator(s); usi.next();) {
                final String source = usi.getString();
                final String target = getModifiedNKFC(source);
                writeSourceTargetLine(out, source, "N", target, value, ARROW);
            }
            //bf.showSetNames(out, s);
            out.flush();
        }
        out.close();
    }

    public static class FakeBreak extends UnicodeLabel {
        UnicodeSet nobreakSet = setsToAbbreviate;
        @Override
        public String getValue(int codepoint, boolean isShort) {
            return nobreakSet.contains(codepoint) ? ""
                    : (codepoint & 1) == 0 ? "O"
                            : "E";
        }
    }

    public static class FakeBreak2 extends UnicodeLabel {
        UnicodeSet nobreakSet = new UnicodeSet(setsToAbbreviate)
        .addAll(new UnicodeSet(IDNOutputSet).complement())
        .addAll(new UnicodeSet(IdentifierInfo.getIdentifierInfo().xidPlus).complement());

        @Override
        public String getValue(int codepoint, boolean isShort) {
            return nobreakSet.contains(codepoint) ? ""
                    : (codepoint & 1) == 0 ? "O"
                            : "E";
        }
    }

    /**
     * 
     */
    //    private static void showRemapped(PrintWriter out, String title, UnicodeMap remap) {
    //        out.println("");
    //        out.println("# " + title);
    //        out.println("");
    //        int count = 0;
    //        for (final UnicodeSetIterator usi = new UnicodeSetIterator(remap.keySet()); usi.next();) {
    //            writeSourceTargetLine(out, usi.getString(), "remap-to", (String)remap.getValue(usi.codepoint), null, ARROW);
    //            count++;
    //        }
    //        out.println("");
    //        out.println("# Total code points: " + count);
    //    }
    /**
     * 
     */
    static UnicodeSet IDNOutputSet;
    static UnicodeSet IDNInputSet;
    private static UnicodeSet _preferredIDSet;

    static UnicodeSet getIdentifierSet() {
        if (_preferredIDSet == null) {
            IDNOutputSet = new UnicodeSet();
            IDNInputSet = new UnicodeSet();
            IDNOutputSet.add('-'); // HACK
            IDNInputSet.add('-');
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                final int cat = DEFAULT_UCD.getCategory(cp);
                if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                    continue;
                }
                // get IDNA
                //int idnaType = GenerateStringPrep.getIDNAType(cp);
                //if (idnaType == GenerateStringPrep.OK) IDNOutputSet.add(cp);
                //if (idnaType != GenerateStringPrep.ILLEGAL) IDNInputSet.add(cp);
                final IdnaType idnaType = Uts46.SINGLETON.getType(cp);
                switch (idnaType) {
                case valid: case deviation:
                    IDNOutputSet.add(cp);
                    // fall thru!
                case mapped: case ignored:
                    IDNInputSet.add(cp);
                    break;
                case disallowed:
                    // no action
                }
            }
            _preferredIDSet = new UnicodeSet(IDNOutputSet).addAll(XIDContinueSet);
            _preferredIDSet.add(0x2018).add(0x2019).freeze();
        }
        return _preferredIDSet;
    }

    private static UnicodeSet SKIP_EXCEPTIONS = new UnicodeSet().add(0x1E9A).add('ſ').add('ﬅ').add('ẛ').add("Ϲ").add("ϲ").freeze();

    private static UnicodeSet getSkipNFKD() {
        nfcMap = new UnicodeMap();
        nfkcMap = new UnicodeMap();
        if (_skipNFKD == null) {
            _skipNFKD = new UnicodeSet();

            // General exceptions
            final UnicodeSet idSet = getIdentifierSet();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (SKIP_EXCEPTIONS.contains(cp)) {
                    _skipNFKD.add(cp);
                    continue;
                }
                final int cat = DEFAULT_UCD.getCategory(cp);
                if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                    continue;
                }
                final int decompType = DEFAULT_UCD.getDecompositionType(cp);
                final String nfc = Default.nfc().normalize(cp);
                if (decompType == UCD_Types.CANONICAL) {
                    nfcMap.put(cp, nfc);
                }
                if (decompType == UCD_Types.COMPAT_CIRCLE
                        || decompType == UCD_Types.COMPAT_SUPER
                        || decompType == UCD_Types.COMPAT_SUB
                        || decompType == UCD_Types.COMPAT_VERTICAL
                        || decompType == UCD_Types.COMPAT_SMALL
                        || decompType == UCD_Types.COMPAT_SQUARE
                        || decompType == UCD_Types.COMPAT_FRACTION
                        || decompType == UCD_Types.COMPAT_NARROW
                        || decompType == UCD_Types.COMPAT_WIDE
                        || decompType == UCD_Types.COMPAT_WIDE
                        || cp == '﬩'
                        || cp == '︒'
                        ) {
                    _skipNFKD.add(cp);
                    continue;
                }
                final String source = UTF16.valueOf(cp);
                final String mapped = NFKD.normalize(cp);
                String kmapped = getModifiedNKFC(source);
                if (!kmapped.equals(source) && !kmapped.equals(nfc)) {
                    if (kmapped.startsWith(" ") || kmapped.startsWith("\u0640")) {
                        if (DEBUG) System.out.println("?? " + DEFAULT_UCD.getCodeAndName(cp));
                        if (DEBUG) System.out.println("\t" + DEFAULT_UCD.getCodeAndName(kmapped));
                        kmapped = getModifiedNKFC(source); // for debugging
                    }
                    nfkcMap.put(cp,kmapped);
                }
                if (mapped.equals(source)) {
                    continue;
                }
                if (idSet.contains(cp) && !idSet.contains(mapped)) {
                    _skipNFKD.add(cp);
                } else if (!WHITESPACE.contains(cp) && WHITESPACE.containsSome(mapped)) {
                    _skipNFKD.add(cp);
                }
            }
        }
        nfcMap.setMissing("");
        nfcMap.freeze();
        nfkcMap.setMissing("");
        nfkcMap.freeze();
        return _skipNFKD;
    }

    /**
     * Returns the script of the input text. Script values of COMMON and INHERITED are ignored.
     * @param source Input text.
     * @return Script value found in the text.
     * If more than one script values are found, then UCD_Types.UNUSED_SCRIPT is returned.
     * If no script value is found (other than COMMON or INHERITED), then UCD_Types.COMMON_SCRIPT is returned.
     */
    public static int getSingleScript(String source) {
        if (source.length() == 0) {
            return UCD_Types.COMMON_SCRIPT;
        }
        int lastScript = UCD_Types.COMMON_SCRIPT; // temporary value
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            final int script = DEFAULT_UCD.getScript(cp);
            if (script == UCD_Types.COMMON_SCRIPT || script == UCD_Types.INHERITED_SCRIPT) {
                continue;
            }
            if (lastScript == UCD_Types.COMMON_SCRIPT) {
                lastScript = script;
            } else if (script != lastScript) {
                return UCD_Types.UNUSED_SCRIPT;
            }
        }
        return lastScript;
    }

    /**
     * 
     */
    private static void generateConfusables() throws IOException {
        log = FileUtilities.openUTF8Writer(reformatedInternal, "log.txt");
        //fixMichel(indir, outdir);
        generateConfusables(indir, reformatedInternal, GEN_SECURITY_DIR);
        log.close();
        if (false) {
            for (final Iterator it = gatheredNFKD.keySet().iterator(); it.hasNext();) {
                final String source = (String)it.next();
                if (DEBUG) System.out.println(DEFAULT_UCD.getCodeAndName(source)
                        + " => " + DEFAULT_UCD.getCodeAndName((String)gatheredNFKD.get(source)));
            }
        }
    }

    /*	private static class Data2 {
		String source;
		String target;
		int count;
		Data2(String target, int count) {
			this.target = target;
			this.count = count;
		}
	}
     */
    /*	private static class Data implements Comparable {
		String source;
		String target;
		String type;
		Data(String source, String target, String type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
		public int compareTo(Object o) {
			int result;
			Data that = (Data)o;
			if (0 != (result = target.compareTo(that.target))) return result;
			if (0 != (result = source.compareTo(that.source))) return result;
			if (0 != (result = type.compareTo(that.type))) return result;
			return 0;
		}
	}
     */

    /**
     * @param relation TODO
     * 
     */
    private static void writeSourceTargetLine(PrintWriter out, String source, String tag, String target, String reason, String relation) {
        out.print(
                Utility.hex(source)
                + " ;\t" + Utility.hex(target)
                + (tag == null ? "" : " ;\t" + tag)
                //+ " ;\t" + (preferredID.contains(source) ? "ID" : "")
                + "\t#"
                + (isXid(source) ? "" : "*")
                + arrowLiterals(source, target, relation)
                + DEFAULT_UCD.getName(source) + " " + relation + " "
                + DEFAULT_UCD.getName(target)
                );
        if (reason != null) {
            out.print("\t# " + reason);
        }
        out.println();
    }

    private static String arrowLiterals(String source, String target, String relation) {
        return (" ( " + rtlProtect(source) + " " + relation + " " + rtlProtect(target) + " ) ");
    }

    private static String rtlProtect(String source) {
        if (CONTROLS.containsSome(source)) {
            source = "";
        } else if (INVISIBLES.containsSome(source)) {
            source = "";
        } else if (RTL.containsSome(source)) {
            source = LRM + source + LRM;
        }
        return source;
    }

    private static class MyEquivalenceClass extends XEquivalenceClass<String,String> {
        public MyEquivalenceClass() {
            super("NONE");
        }
        public boolean addCheck(String a, String b, String reason) {
            // quick check for illegal containment, before changing object
            if (checkForBad(a, b, reason) || checkForBad(b, a, reason)) {
                return false;
            }
            super.add(a, b, reason);
            // full check for any resulting illegal containment.
            // illegal if for any x, y, x is a proper superstring of y
            final Set equivalences = getEquivalences(a);
            for (final Iterator it = equivalences.iterator(); it.hasNext();) {
                final String x = (String)it.next();
                if (!UTF16.hasMoreCodePointsThan(x,1)) {
                    continue;
                }
                for (final Iterator it2 = equivalences.iterator(); it2.hasNext();) {
                    final String y = (String)it2.next();
                    if (x.equals(y)) {
                        continue;
                    }
                    if (x.indexOf(y) >= 0) {
                        throw new RuntimeException("Illegal containment: "
                                + DEFAULT_UCD.getCodeAndName(x) + " contains "
                                + DEFAULT_UCD.getCodeAndName(y) + " because "
                                + DEFAULT_UCD.getCodeAndName(a) + " ~ "
                                + DEFAULT_UCD.getCodeAndName(b) + " because of "
                                + reason);
                    }
                }
            }
            return true;
        }

        /**
         * 
         */
        private boolean checkForBad(String a, String b, String reason) {
            final Set equivalences = getEquivalences(b);
            for (final Iterator it = equivalences.iterator(); it.hasNext();) {
                final String b2 = (String)it.next();
                if (a.equals(b2)) {
                    continue;
                }
                if (b2.indexOf(a) >= 0 || a.indexOf(b2) >= 0) {
                    log.println("Illegal containment: "
                            + DEFAULT_UCD.getCodeAndName(a)
                            + " overlaps "
                            + DEFAULT_UCD.getCodeAndName(b2)
                            + "\n\tfrom "
                            + DEFAULT_UCD.getCodeAndName(b)
                            + "\n\twith reason "
                            + reason + " plus "
                            + getReasons(b2, b));
                    return true;
                }
            }
            return false;
        }

        //        public XEquivalenceClass add(Object a1, Object b1, String reason) {
        //            final String a = (String)a1;
        //            final String b = (String)b1;
        //            try {
        //                addCheck(a, b, reason);
        //                return this;
        //            } catch (final RuntimeException e) {
        //                throw (RuntimeException) new RuntimeException("Failure adding "
        //                        + DEFAULT_UCD.getCodeAndName(a) + "; "
        //                        + DEFAULT_UCD.getCodeAndName(b)
        //                        + "; " + reason).initCause(e);
        //            }
        //        }
        /**
         * Only NFKD if the result doesn't cross from ID set to nonID set, and space is not added
         */
        //		private String specialNFKD(String item) {
        //			UnicodeSet skipSet = getSkipNFKD();
        //			StringBuffer result = new StringBuffer();
        //			int cp;
        //			for (int i = 0; i < item.length(); i += UTF16.getCharCount(cp)) {
        //				cp = UTF16.charAt(item, i);
        //				if (skipSet.contains(cp)) {
        //					UTF16.append(result, cp);
        //					continue;
        //				}
        //				String cps = UTF16.valueOf(cp);
        //				String mapped = Default.nfkd().normalize(cps);
        //				if (cps.equals(mapped)) {
        //					UTF16.append(result, cp);
        //					continue;
        //				}
        //				result.append(mapped);
        //				gatheredNFKD.put(cps, mapped);
        //			}
        //			return result.toString();
        //		}

        public void close(String reason) {
            boolean addedItem;
            final StringBuffer reasons = new StringBuffer();
            do {
                addedItem = false;
                final Set cloneForSafety = getOrderedExplicitItems();
                // do all the combinations for all the paradigms
                for (int lower = 0; lower < 2; ++lower) {
                    for (int sameScript = 0; sameScript < 2; ++sameScript) {
                        for (final Iterator it = cloneForSafety.iterator(); it.hasNext();) {
                            final String item = (String) it.next();
                            if (!UTF16.hasMoreCodePointsThan(item,1))
                            {
                                continue; // just for speed
                            }
                            reasons.setLength(0);
                            final String mapped = mapString(item, reasons, lower == 1, sameScript == 1);
                            if (!isEquivalent(item, mapped)) {
                                if (addCheck(item, mapped, reasons.toString())) {
                                    // if (DEBUG) System.out.println("Closing: " + DEFAULT_UCD.getCodeAndName(item) + " => " + DEFAULT_UCD.getCodeAndName(mapped));
                                    addedItem = true;
                                }
                            }
                        }
                    }
                }
            } while (addedItem);
        }

        /**
         * 
         */
        private String mapString(String item, StringBuffer reasons, boolean onlyLowercase, boolean onlySameScript) {
            if (DEBUG && item.startsWith("\u03D2")) {
                System.out.println("foo");
            }
            final StringBuffer result = new StringBuffer();
            int cp;
            for (int i = 0; i < item.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(item, i);
                final String cps = UTF16.valueOf(cp);
                final String mapped = getParadigm(cps, onlyLowercase, onlySameScript);
                if (mapped == null || mapped.indexOf(cps) >= 0) {
                    result.append(cps);
                } else {
                    result.append(mapped);
                    final List x = getReasons(cps, mapped);
                    reasons.append(getBestForm(x));
                }
            }
            return result.toString();
        }

        private Object getBestForm(Collection x) {
            if (x.size() != 1) {
                return "[" +  x + "]";
            }
            final Object item = x.iterator().next();
            if (!(item instanceof Collection)) {
                return x.toString();
            }
            return getBestForm((Collection)item);
        }

        public String getParadigm(String item, boolean onlyLowercase, boolean onlySameScript) {
            //            0049 ;    006C ;    MA    # ( I → l ) LATIN CAPITAL LETTER I → LATIN SMALL LETTER L    #
            //            042E ;    0049 004F ;    MA    # ( Ю → IO ) CYRILLIC CAPITAL LETTER YU → LATIN CAPITAL LETTER I, LATIN CAPITAL LETTER O    #
            // fails, since 0049 should not occur in the target
            Set filteredSet = new HashSet();
            final Set<String> equivalences = getEquivalences(item);
            //            if (equivalences.contains("\u2CFE")) {
            //                int debug = 0;
            //            }

            main:
                for (final Object element : equivalences) {
                    final String other = (String) element;
                    if (item.equals("\u2CFE") && (other.equals("\u22D7") || other.equals("\u00B7\u1433"))) {
                        int debug = 0;
                    }

                    final String combined = item + other;

                    if (onlyLowercase) {
                        final boolean isLowercase = combined.equals(DEFAULT_UCD.getCase(combined, UCD_Types.FULL, UCD_Types.FOLD));
                        if (!isLowercase) {
                            continue;
                        }
                    }
                    if (onlySameScript) {
                        final boolean isMixed = ScriptInfo.isMixedScript(combined);
                        if (isMixed) {
                            continue;
                        }
                    }

                    // verify idempotence
                    final int[] codePointArray = With.codePointArray(other);
                    if (codePointArray.length == 1) {
                        //                        String otherParadigm = getParadigm(other, onlyLowercase, onlySameScript);
                        //                        if (otherParadigm != null && !item.equals(otherParadigm)) {
                        //                            continue main;
                        //                        }
                    } else {
                        for (int codepoint : codePointArray) {
                            final String codePointString = UTF16.valueOf(codepoint);
                            String otherParadigm = getParadigm(codePointString, onlyLowercase, onlySameScript);
                            if (otherParadigm != null && !codePointString.equals(otherParadigm)) {
                                continue main;
                            }
                        }
                    }

                    filteredSet.add(other);
                }
            //            }
            return CollectionUtilities.getBest(filteredSet, 
                    // onlyLowercase || onlySameScript ? betterTargetIsLessFavorNeutral : 
                        betterTargetIsLess, -1);
        }

        public Set getOrderedExplicitItems() {
            final Set cloneForSafety = new TreeSet(codepointComparator);
            cloneForSafety.addAll(getExplicitItems());
            return cloneForSafety;
        }
        /**
         * 
         */
        //        public void writeSource(PrintWriter out) {
        //            final Set items = getOrderedExplicitItems();
        //            for (final Iterator it = items.iterator(); it.hasNext();) {
        //                final String item = (String) it.next();
        //                final String paradigm = CollectionUtilities.getBest(getEquivalences(item), betterTargetIsLess, -1);
        //                if (item.equals(paradigm)) {
        //                    continue;
        //                }
        //                writeSourceTargetLine(out, item, null, paradigm, null, ARROW);
        //            }
        //        }
    }

    private static class RawData {
        Map<String,Set<String>> data = new TreeMap<String,Set<String>>();

        public void add(String source, String target, String type) {
            if (betterTargetIsLess.compare(source, target) < 0) {
                add2(source,target,type);
            } else {
                add2(target,source,type);
            }
        }

        private void add2(String source, String target, String type) {
            Set<String> set = data.get(source);
            if (set == null) {
                data.put(source, set = new TreeSet<String>(betterTargetIsLess));
            }
            set.add(target);
        }

        public void writeSource(PrintWriter out) {
            for (final String source : data.keySet()) {
                for (final String target : data.get(source)) {
                    writeSourceTargetLine(out, source, null, target, null, "~");
                }
                out.println();
            }
        }
    }

    private static class DataSet {
        MyEquivalenceClass dataMixedAnycase = new MyEquivalenceClass();
        //        MyEquivalenceClass dataMixedLowercase = new MyEquivalenceClass();
        //        MyEquivalenceClass dataSingleLowercase = new MyEquivalenceClass();
        //        MyEquivalenceClass dataSingleAnycase = new MyEquivalenceClass();
        RawData raw = new RawData();

        private static String testChar = UTF16.valueOf(0x10A3A);

        public DataSet add(String source, String target, String type, int lineCount, String errorLine) {
            if (SKIP_SET.containsAll(source) || SKIP_SET.containsAll(target)) {
                return this;
            }
            final String nsource = Default.nfd().normalize(source);
            final String ntarget = Default.nfd().normalize(target);

            if (COMBINING.containsAll(nsource) != COMBINING.containsAll(ntarget)) {
                if (nsource.contains(testChar)) {
                    COMBINING.containsAll(nsource);
                    COMBINING.containsAll(ntarget);
                }
                System.err.println("ERROR: Mixed combining classes: " + lineCount + "\t" + errorLine +  "\t" + Utility.hex(nsource) +  "\t" + Utility.hex(ntarget));
            }

            // if it is just a compatibility match, return
            //if (nsource.equals(ntarget)) return this;
            if (type.indexOf("skip") >= 0) {
                return this;
            }
            if (target.indexOf('\u203D') >= 0) {
                return this;
            }

            type = getReasonFromFilename(type);

            // if it is base + combining sequence => base2 + same combining sequence, do just the base
            final int nsourceFirst = UTF16.charAt(nsource,0);
            final String nsourceRest = nsource.substring(UTF16.getCharCount(nsourceFirst));
            final int ntargetFirst = UTF16.charAt(ntarget,0);
            final String ntargetRest = ntarget.substring(UTF16.getCharCount(ntargetFirst));

            if (nsourceRest.length() != 0 && nsourceRest.equals(ntargetRest)) {
                source = UTF16.valueOf(nsourceFirst);
                target = UTF16.valueOf(ntargetFirst);
                type += "-base";
            }
            //type += ":" + lineCount;

            final String combined = source + target;
            if (DEBUG && combined.indexOf("\u0430") >= 0) {
                System.out.println(DEFAULT_UCD.getCodeAndName(combined));
            }
            final boolean isLowercase = combined.equals(DEFAULT_UCD.getCase(combined, UCD_Types.FULL, UCD_Types.FOLD));
            final boolean isMixed = ScriptInfo.isMixedScript(combined);
            // Here's where we add data, if you need to debug
            raw.add(source,target,type);
            dataMixedAnycase.add(source, target, type);
            //            if (isLowercase) {
            //                dataMixedLowercase.add(source, target, type);
            //            }
            //            if (!isMixed) {
            //                dataSingleAnycase.add(source, target, type);
            //            }
            //            if (!isMixed && isLowercase) {
            //                dataSingleLowercase.add(source, target, type);
            //            }
            return this;
        }

        @Override
        public String toString() {
            return dataMixedAnycase.toString();
        }

        /*		*//**
         * @param errorLine TODO
         * 
         *//*
		private DataSet add(Data newData, String errorLine) {
			if (controls.containsSome(newData.source) || controls.containsSome(newData.target)) {
				System.out.println("Problem with " + errorLine);
				System.out.println(getCodeCharName(newData.source) + " => " + getCodeCharName(newData.target));
			}
			String[] key = {newData.source, newData.target};
			Data old = (Data) dataMap.get(key);
			if (old == null) {
				dataSet.add(newData);
				dataMap.put(key, newData);
			}else {
				old.type = old.type + "/" + newData.type;
			}
			return this;
		}
          */		// Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt"
        private static final int NORMAL = 0, FOLDING = 1, OLD = 2;
        private static final UnicodeSet NSM = new UnicodeSet("[[:Mn:][:Me:]]").freeze();

        public DataSet addFile(String directory, String filename) throws IOException {
            String line = null;
            int count = 0;
            try {
                final BufferedReader in = FileUtilities.openUTF8Reader(directory, filename);
                int kind = NORMAL;
                if (filename.indexOf("Folding") >= 0) {
                    kind = FOLDING;
                } else if (false && filename.indexOf("-old") >= 0) {
                    kind = OLD;
                }
                while (true) {
                    count++;
                    line = Utility.readDataLine(in);
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    boolean isFont = false;
                    if (line.equals("@font")) {
                        isFont = true;
                        continue;
                    }
                    final String[] pieces = Utility.split(line,';');
                    if (pieces.length < 2) {
                        System.err.println("Error on: (" + count + ")\t" + line);
                        continue;
                    }
                    final String type = filename;
                    final String sourceString = INVISIBLES.stripFrom(pieces[0].trim(), true);
                    final String targetString = INVISIBLES.stripFrom(pieces[1].trim(), true);

                    if (!targetString.equals(pieces[1].trim())) {
                        if (DEBUG) System.out.println("**\t" + Utility.hex(pieces[0].trim()) + ";\t" + Utility.hex(targetString));
                    }
                    if (kind==FOLDING) {
                        final String target = fromHexOld(targetString);
                        final String source = fromHexOld(sourceString);
                        final String nsource = NFKD.normalize(source);
                        final String first = UTF16.valueOf(UTF16.charAt(nsource, 0));
                        if (!first.equals(target)) {
                            add(source, target, type, count, line);
                        }
                    } else if (kind == OLD) {
                        final String target = sourceString.trim();
                        for (int i = 1; i < pieces.length; ++i) {
                            add(pieces[i].trim(), target, type, count, line);
                        }
                    } else {
                        if (targetString.contains("(")) {
                            System.err.println("WARNING: paren on " + count + "\t" + line);
                        }
                        final String target = fromHexOld(targetString);
                        if (UnicodeSet.resemblesPattern(sourceString, 0)) {
                            final UnicodeSet sourceSet = new UnicodeSet(sourceString);
                            for (final String s : sourceSet) {
                                add2(s, target, type, count, line);
                            }
                        } else {
                            final String source = fromHexOld(sourceString);
                            add2(source, target, type, count, line);
                        }
                    }
                }
                in.close();
                return this;
            } catch (final Exception e) {
                throw (RuntimeException) new RuntimeException("Failure with file: "
                        + directory + filename + " on line: " + count
                        + ": " + line).initCause(e);
            }
        }

        private void add2(String source, String target, String type, int count, String line) {
            //if (pieces.length > 2) type = pieces[2].trim();
            final String nfkdSource = NFKD.normalize(source);
            final String nfkdTarget = NFKD.normalize(target);
            if (NSM.containsAll(source) && NSM.containsNone(target)
                    || NSM.containsAll(target) && NSM.containsNone(source)) {
                if (SHOW_SUPPRESS) {
                    System.out.println("*** SUPPRESSING NSM Difference\t"
                            + count + "\t" + DEFAULT_UCD.getCodeAndName(source) + ";\t" + DEFAULT_UCD.getCodeAndName(target) + ";\t" + line);
                }
            } else if (suppress_NFKC && nfkdSource.equals(nfkdTarget)) {
                if (SHOW_SUPPRESS) {
                    System.out.println("*** Suppressing nfkc for:\t"
                            + count + "\t" + DEFAULT_UCD.getCodeAndName(source) + ";\t" + DEFAULT_UCD.getCodeAndName(target) + ";\t" + line);
                }
            } else {
                add(source, target, type, count, line);
            }
        }

        public void writeSource(String directory, String filename) throws IOException {
            final PrintWriter out = openAndWriteHeader(directory, filename, "Source File for IDN Confusables");
            //			PrintWriter out = FileUtilities.openUTF8Writer(directory, filename);
            //			out.println("# Source File for IDN Confusables");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");
            raw.writeSource(out);
            out.close();
        }

        public void writeSourceOrder(String directory, String filename, boolean appendFile, boolean skipNFKEquivs) throws IOException {
            final PrintWriter out = openAndWriteHeader(directory, filename, "Recommended confusable mapping for IDN");
            //            PrintWriter out = FileUtilities.openUTF8Writer(directory, filename);
            //			out.println("# Recommended confusable mapping for IDN");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");

            if (appendFile) {
                final String[] replacements = {"%date%", Default.getDate()};
                Utility.appendFile(Settings.SRC_UCD_DIR + "confusablesHeader.txt",
                        Utility.UTF8_WINDOWS, out, replacements);
            }
            Relation<Pair<String,String>, String> confusableMap 
            = Relation.of(new TreeMap(MyPairComparator), TreeSet.class); 
            if (true) {
                //                writeSourceOrder(out, dataMixedAnycase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, 
                //                        true, true, confusableMap);
                //                writeSourceOrder(out, dataMixedAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, 
                //                        false, true, confusableMap);
                //                writeSourceOrder(out, dataMixedAnycase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, 
                //                        true, false, confusableMap);
                writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, 
                        false, false, confusableMap);
                Counter<Set<String>> counter = new Counter();
                Map<Set<String>, Pair<String, String>> examples = new HashMap<Set<String>, Pair<String, String>>();
                for (Entry<Pair<String, String>, Set<String>> entry : confusableMap.keyValuesSet()) {
                    final Set<String> set = entry.getValue();
                    counter.add(set, 1);
                    if (!examples.containsKey(set)) {
                        examples.put(set, entry.getKey());
                    }
                }
                for (Set<String> entry : counter) {
                    if (DEBUG) System.out.println(counter.get(entry) + "\t" + entry + "\t" + examples.get(entry));
                }
                //            } else {
                //                writeSourceOrder(out, dataSingleLowercase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, false, false);
                //                writeSourceOrder(out, dataSingleAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, false, false);
                //                writeSourceOrder(out, dataMixedLowercase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, false, false);
                //                writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, false, false);
            }
            out.close();
        }
        private static Comparator<Pair<String,String>> MyPairComparator = new Comparator<Pair<String,String>>() {
            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                int result = UCAComparator.compare(o1.getFirst(), o2.getFirst());
                return result != 0 ? result : UCAComparator.compare(o1.getSecond(), o2.getSecond());
            }

        };
        /**
         * @param skipNFKEquivs TODO
         * @param onlyLowercase TODO
         * @param onlySingleScript TODO
         * @param confusableMap 
         * 
         */
        private void writeSourceOrder(PrintWriter out, MyEquivalenceClass data, String tag, String title,
                boolean skipNFKEquivs, boolean onlyLowercase, boolean onlySingleScript, 
                Relation<Pair<String, String>, String> confusableMap) {
            // first get all the sets. Then get the best paradigm from each. Then sort.
            //			Set setOfSets = data.getEquivalenceSets();
            //			Map orderedResults = new TreeMap(betterTargetIsLess);
            //			for (Iterator it = setOfSets.iterator(); it.hasNext();) {
            //				Set setOfEquivs = (Set) it.next();
            //				Object item = CollectionUtilities.getBest(setOfEquivs, betterTargetIsLess, -1);
            //
            //			}
            //int c = codepointComparator.compare("\uFFFF", "\uD800\uDC00");
            //System.out.println("Code Point Compare: " + c);
            final Set items = data.getOrderedExplicitItems();
            //            out.println();
            //            out.println("# " + title);
            //            out.println();
            int count = 0;
            final UnicodeSet preferredID = getIdentifierSet();
            final ArrayComparator ac = new ArrayComparator(new Comparator[] {UCAComparator, UCAComparator});
            final Set orderedPairs = new TreeSet(ac);
            for (final Iterator it = items.iterator(); it.hasNext();) {
                final String source = (String) it.next();
                if (UTF16.hasMoreCodePointsThan(source,1)) {
                    continue;
                }
                if (source.equals("\u2CFE") || source.equals("\u22D7")) {
                    int debug = 0;
                }
                final String target = data.getParadigm(source, onlyLowercase, onlySingleScript);
                if (target == null) {
                    continue;
                }
                if (source.equals(target)) {
                    continue;
                }
                if (skipNFKEquivs) {
                    if (!NFKD.normalize(source).equals(source)) {
                        continue;
                    }
                }
                orderedPairs.add(new String[] {target, source});
                Pair<String,String> pair = new Pair<String,String>(target, source);
                confusableMap.put(pair, tag);
            }
            String lastTarget = null;
            for (final Iterator it = orderedPairs.iterator(); it.hasNext();) {
                final String[] pair = (String[]) it.next();
                final String source = pair[1];
                final String target = pair[0];
                final List<Linkage<String,String>> reasons = data.getReasons(source, target);
                final String reason = XEquivalenceClass.toString(reasons, myLinkageTransform); // fixReason(reasons);
                if (lastTarget != null && !lastTarget.equals(target)) {
                    out.println();
                }
                writeSourceTargetLine(out, source, tag, target, reason, ARROW);
                lastTarget = target;
                count++;
            }
            out.println();
            //             out.println("# total for (" + tag + "): " + count);
            out.println("# total: " + count);
            out.println();
        }

        /**
         * 
         */
        //        private String fixReason(List reasons) {
        //            final List first = (List)reasons.get(0);
        //            String result = "";
        //            for (int i = 0; i < first.size(); ++i) {
        //                if (i != 0) {
        //                    result += " ";
        //                }
        //                final Object item = first.get(i);
        //                if (item instanceof String) {
        //                    result += item;
        //                } else {
        //                    String temp = "";
        //                    for (final Iterator it = ((Set)item).iterator(); it.hasNext();) {
        //                        if (temp.length() != 0) {
        //                            temp += "|";
        //                        }
        //                        temp += it.next();
        //                    }
        //                    result += "{" + temp + "}";
        //                }
        //            }
        //            return result.toString();
        //        }

        public void addAll(DataSet ds) {
            dataMixedAnycase.addAll(ds.dataMixedAnycase);
            //            dataMixedLowercase.addAll(ds.dataMixedLowercase);
            //            dataSingleAnycase.addAll(ds.dataSingleAnycase);
            //            dataSingleLowercase.addAll(ds.dataSingleLowercase);
        }

        private void checkChar(String string) {
            // debug
            final Set<String> test = getEquivalences(string);
            if (DEBUG) System.out.println(test);
        }

        public Set<String> getEquivalences(String string) {
            return dataMixedAnycase.getEquivalences(string);
        }
        /*		*//**
         * 
         *//*
		public DataSet clean() {
			// remove all skips
			DataSet tempSet = new DataSet();
			Map m = new HashMap();
			for (Iterator it = dataSet.iterator(); it.hasNext();) {
				Data d = (Data) it.next();
				if (d.type.indexOf("skip") >= 0) continue;
				String newTarget = Default.nfkd().normalize(d.target);
				String newSource = Default.nfkd().normalize(d.source);
				String type = d.type;
				if (!d.target.equals(newTarget) || !d.source.equals(newSource)) {
					type += "-nf";
					log.println("Norm:\t" + getCodeCharName(d.source) + " " + ARROW + " " + getCodeCharName(newSource));
					log.println("\t" + getCodeCharName(d.target) + " " + ARROW + " " + getCodeCharName(newTarget) + " \t" + type);
					continue;
				}
				// swap order
				if (preferSecondAsSource(newSource, newTarget)) {
					String temp = newTarget;
					newTarget = newSource;
					newSource = temp;
				}

				Data already = (Data) m.get(newSource);
				if (already != null && !newTarget.equals(already.target)) {
					log.println("X " + getCodeCharName(newSource) + " " + ARROW);
					log.println("\t" + getCodeCharName(newTarget) + " \t" + type);
					log.println("\t" + getCodeCharName(already.target) + " \t" + already.type);
					if (preferSecondAsSource(already.target, newTarget)) {
						// just fix new guy
						type += "[" + newSource + "]" + already.type;
						newSource = newTarget;
						newTarget = already.target;
					} else {
						// need to fix new guy, AND fix old guy.
						tempSet.remove(already);
						type += "[" + newSource + "]" + already.type;
						newSource = already.target;
						already.type += "[" + already.target + "]" + type;
						already.target = newTarget;
						tempSet.add(already, "");
					}
				}
				Data newData = new Data(newSource, newTarget, type);
				m.put(newSource, newData);
				tempSet.add(newData, "");
			}
			// now recursively apply
			DataSet s = new DataSet();
			for (Iterator it = tempSet.dataSet.iterator(); it.hasNext();) {
				Data d = (Data) it.next();
				int cp = 0;
				StringBuffer result = new StringBuffer();
				for (int i = 0; i < d.target.length(); i += UTF16.getCharCount(cp)) {
					cp = UTF16.charAt(d.target, i);
					String src = UTF16.valueOf(cp);
					while (true) {
						Data rep = (Data) m.get(src);
						if (rep == null) break;
						src = rep.target;
					}
					result.append(src);
				}
				String newTarget = result.toString();
				newTarget = Default.nfkd().normalize(newTarget);
				s.add(d.source, newTarget, d.type + (newTarget.equals(newTarget) ? "" : "-rec"), "");
			}
			return s;
		}
          *//**
          * 
          *//*
		private void remove(Data already) {
			String[] key = {already.source, already.target};
			dataMap.remove(key);
			dataSet.remove(already);
		}*/
        /**
         * 
         */
        public void close(String reason) {
            dataMixedAnycase.close(reason);
            //            dataMixedLowercase.close(reason);
            //            dataSingleAnycase.close(reason);
            //            dataSingleLowercase.close(reason);
        }
        /**
         * 
         */
        public void addUnicodeMap(UnicodeMap decompMap, String type, String errorLine) {
            int count = 0;
            for (final UnicodeSetIterator it = new UnicodeSetIterator(decompMap.keySet()); it.next(); ) {
                add(it.getString(), (String)decompMap.getValue(it.codepoint), type, ++count, errorLine);
            }
        }

        //        private static class MyFilter implements XEquivalenceClass.Filter {
        //            UnicodeSet output;
        //            @Override
        //            public boolean matches(Object o) {
        //                return output.containsAll((String)o);
        //            }
        //        }

        private static class MyCollectionFilter implements CollectionUtilities.ObjectMatcher {
            UnicodeSet outputAllowed;
            int minLength;
            @Override
            public boolean matches(Object o) {
                final String item = (String)o;
                if (!outputAllowed.containsAll(item)) {
                    return false;
                }
                final int len = UTF16.countCodePoint(item);
                if (len < minLength) {
                    minLength = len;
                }
                return true;
            }
        };
        /**
         * @param script TODO
         * @throws IOException
         * 
         */
        public void writeSummary(String outdir, String filename, boolean outputOnly, UnicodeSet script) throws IOException {
            final PrintWriter out = openAndWriteHeader(outdir, filename, "Summary: Recommended confusable mapping for IDN");
            //			PrintWriter out = FileUtilities.openUTF8Writer(outdir, filename);
            //			out.print('\uFEFF');
            //			out.println("# Summary: Recommended confusable mapping for IDN");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");
            final UnicodeSet representable = new UnicodeSet();
            final MyEquivalenceClass data = dataMixedAnycase;
            final Set items = data.getOrderedExplicitItems();
            //			for (Iterator it = items.iterator(); it.hasNext();) {
            //				if (DEBUG) System.out.println(DEFAULT_UCD.getCodeAndName((String)it.next()));
            //			}
            int count = 0;
            final UnicodeSet preferredID = getIdentifierSet();
            final String lastTarget = "";
            final Set itemsSeen = new HashSet();
            final Set equivalents = new TreeSet(betterTargetIsLess);
            final MyCollectionFilter myFilter = new MyCollectionFilter();
            myFilter.outputAllowed= new UnicodeSet("[[\u0021-\u007E]-[:letter:]]")
            .addAll(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
            .addAll(IdentifierInfo.getIdentifierInfo().inputSet_strict);

            for (final Iterator it = items.iterator(); it.hasNext();) {
                String target = (String) it.next();
                if (itemsSeen.contains(target)) {
                    continue;
                }
                equivalents.clear();
                equivalents.addAll(data.getEquivalences(target));
                itemsSeen.addAll(equivalents);
                if (outputOnly) { // remove non-output
                    myFilter.minLength = 1000;
                    CollectionUtilities.retainAll(equivalents, myFilter);
                    if (equivalents.size() <= 1) {
                        continue;
                    }
                    if (myFilter.minLength > 1) {
                        continue;
                    }
                    if (!equivalents.contains(target)) { // select new target if needed
                        target = (String) equivalents.iterator().next();
                    }
                }
                scriptTest:
                    if (script != null) {
                        // see if at least one item contains the target script
                        for (final Iterator it2 = equivalents.iterator(); it2.hasNext();) {
                            final String item = (String) it2.next();
                            if (script.containsAll(item)) {
                                target = item;
                                for (final Iterator it3 = equivalents.iterator(); it3.hasNext();) {
                                    representable.addAll((String)it3.next());
                                }
                                break scriptTest;
                            }
                        }
                        continue; // skip this one
                    }
                out.println();
                out.println("#\t" + CollectionUtilities.join(equivalents, "\t"));
                String status = ""; // getStatus(target);
                out.println(status + "\t" + "(\u200E " + target + " \u200E)\t" + Utility.hex(target) + "\t " + DEFAULT_UCD.getName(target));
                //if (UTF16.hasMoreCodePointsThan(source,1)) continue;
                for (final Iterator it2 = equivalents.iterator(); it2.hasNext();) {
                    final String source = (String) it2.next();
                    if (source.equals(target)) {
                        continue;
                    }
                    //boolean compatEqual = Default.nfkd().normalize(source).equals(Default.nfkd().normalize(target));
                    //if (EXCLUDE_CONFUSABLE_COMPAT && compatEqual) continue;
                    final String reason = XEquivalenceClass.toString(data.getReasons(source, target), myLinkageTransform); // fixReason(data.getReasons(source, target));
                    //if (!outputAllowed.containsAll(source)) continue;
                    //					if (compatEqual) {
                    //						out.print("\u21D0");
                    //					} else {
                    //						out.print("\u2190");
                    //					}
                    final String reasonOrEmpty = reason.length() == 0 ? "" : "\t# " + reason;
                    status = ""; // getStatus(source);

                    out.println(BACKARROW + status + "\t" + "(\u200E " + source + " \u200E)\t" + Utility.hex(source) + "\t " + DEFAULT_UCD.getName(source)
                            + reasonOrEmpty);
                    count++;
                }
            }
            out.println();
            out.println("# total : " + count);
            out.println();
            if (script != null) {
                out.println();
                out.println("# Base Letters Representable with Script");
                out.println();
                representable.removeAll(script);
                final BagFormatter bf = makeFormatter();
                bf.setValueSource(ups.getProperty("script"));
                bf.setShowLiteral(EXCAPE_FUNNY);
                bf.showSetNames(out, representable);
            }
            out.close();
        }



        public void writeWholeScripts(String outdir, String filename) throws IOException {
            final UnicodeSet commonAndInherited = new UnicodeSet(
                    "[[:script=common:][:script=inherited:]]");

            final WholeScript wsLower = new WholeScript(
                    new UnicodeSet(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
                    .removeAll(new UnicodeSet("[A-Z]")), "L");
            final WholeScript wsAny = new WholeScript(
                    new UnicodeSet(IdentifierInfo.getIdentifierInfo().remainingOutputSet)
                    .addAll(IdentifierInfo.getIdentifierInfo().inputSet_strict), "A");

            final MyEquivalenceClass data = new MyEquivalenceClass();
            for (final Object element : dataMixedAnycase.getSamples()) {
                String target = (String) element;
                final Set equivalents = getEquivalences(target);
                boolean first = true;
                for (final Iterator it2 = equivalents.iterator(); it2.hasNext();) {
                    final String cleaned = CollectionUtilities.remove((String)it2.next(), commonAndInherited);
                    if (cleaned.length() == 0) {
                        continue;
                    }
                    if (first) {
                        target = cleaned;
                        first = false;
                    } else {
                        data.add(target, cleaned);
                    }
                }
            }
            final Set itemsSeen = new HashSet();
            for (final Iterator it = data.getOrderedExplicitItems().iterator(); it.hasNext();) {
                final String target = (String) it.next();
                if (itemsSeen.contains(target)) {
                    continue;
                }
                final Set equivalents = data.getEquivalences(target);
                itemsSeen.addAll(equivalents);
                wsAny.addEquivalents(equivalents);
                wsLower.addEquivalents(equivalents);
            }
            final PrintWriter out = openAndWriteHeader(outdir, filename, "Summary: Whole-Script Confusables");
            //			PrintWriter out = FileUtilities.openUTF8Writer(outdir, filename);
            //			out.print('\uFEFF');
            //			out.println("# Summary: Whole-Script Confusables");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            out.println("# This data is used for determining whether a strings is a");
            out.println("# whole-script or mixed-script confusable.");
            out.println("# The mappings here ignore common and inherited script characters,");
            out.println("# such as accents.");
            out.println("");
            out.println("# Lowercase Only");
            out.println("");
            wsLower.write(out);
            out.println("");
            out.println("# Any-Case");
            out.println("");
            wsAny.write(out);
            out.close();
        }
        /**
         * 
         */
        //        private String getStatus(String source) {
        //            // TODO Auto-generated method stub
        //            final int val = betterTargetIsLess.getValue(source);
        //            if (val == MARK_NOT_NFC.intValue()) {
        //                return "[x]";
        //            }
        //            if (val == MARK_NFC.intValue()) {
        //                return "[x]";
        //            }
        //            if (val == MARK_INPUT_LENIENT.intValue()) {
        //                return "[L]";
        //            }
        //            if (val == MARK_INPUT_STRICT.intValue()) {
        //                return "[I]";
        //            }
        //            if (val == MARK_OUTPUT.intValue()) {
        //                return "[O]";
        //            }
        //            if (val == MARK_ASCII.intValue()) {
        //                return "[A]";
        //            }
        //
        //            return "?";
        //        }

        public void writeData(String string, String string2) {
            // TODO Auto-generated method stub

        }
    }

    static class WholeScript {
        private final UnicodeSet filterSet;
        private final UnicodeSet[] script_representables = new UnicodeSet[UCD_Types.LIMIT_SCRIPT];
        private final UnicodeSet[] script_set = new UnicodeSet[UCD_Types.LIMIT_SCRIPT];
        private final BagFormatter bf = makeFormatter();
        private final String label;
        {
            for (short i = 0; i < UCD_Types.LIMIT_SCRIPT; ++i) {
                script_representables[i] = new UnicodeSet();
                //script_set[i] = new UnicodeSet("[:script=" + DEFAULT_UCD.getScriptID(i, UCD_Types.LONG) + ":]"); // ugly hack
                script_set[i] = SCRIPT_PROPERTY.getSet(UCD.getScriptID_fromIndex(i)); // ugly hack
            }
            bf.setValueSource(ups.getProperty("script"));
            bf.setShowLiteral(EXCAPE_FUNNY);
            bf.setLabelSource(UnicodeLabel.NULL);
        }

        WholeScript(UnicodeSet filterSet, String label) {
            this.filterSet = filterSet;
            this.label = label;
            finished = false;
        }

        void addEquivalents(Set set) {
            finished = false;
            // if we have y ~ x, and both are single scripts
            // that means that x can be represented in script(y),
            // and y can be represented in script(x).
            for (final Iterator it = set.iterator(); it.hasNext();) {
                final String item1 = (String)it.next();
                if (!filterSet.containsAll(item1)) {
                    continue;
                }
                final int script1 = getSingleScript(item1);
                if (script1 == UCD_Types.UNUSED_SCRIPT) {
                    continue;
                }
                for (final Iterator it2 = set.iterator(); it2.hasNext();) {
                    final String item2 = (String)it2.next();
                    if (!filterSet.containsAll(item2)) {
                        continue;
                    }
                    final int script2 = getSingleScript(item2);
                    if (script2 == UCD_Types.UNUSED_SCRIPT || script2 == script1) {
                        continue;
                    }
                    script_representables[script1].addAll(item2).removeAll(commonAndInherited);
                }
            }
        }

        public static class UnicodeSetToScript {
            public short getScript() {
                return script;
            }
            public UnicodeSetToScript setScript(short script) {
                this.script = script;
                return this;
            }
            public UnicodeSet getSet() {
                return set;
            }
            public UnicodeSetToScript setSet(UnicodeSet set) {
                this.set = set;
                return this;
            }
            private UnicodeSet set;
            private short script;
        }

        UnicodeSetToScript[][] scriptToUnicodeSetToScript = new UnicodeSetToScript[UCD_Types.LIMIT_SCRIPT][];
        UnicodeSet[] fastReject = new UnicodeSet[UCD_Types.LIMIT_SCRIPT];
        boolean finished = false;

        void finish() {
            if (finished) {
                return;
            }
            for (int j = 0; j < UCD_Types.LIMIT_SCRIPT; ++j) {
                if (j == UCD_Types.COMMON_SCRIPT || j == UCD_Types.INHERITED_SCRIPT) {
                    continue;
                }
                if (script_representables[j].size() == 0) {
                    continue;
                }
                final UnicodeSet accept = new UnicodeSet();
                final List curr = new ArrayList();
                for (short k = 0; k < UCD_Types.LIMIT_SCRIPT; ++k) {
                    if (k == UCD_Types.COMMON_SCRIPT || k == UCD_Types.INHERITED_SCRIPT) {
                        continue;
                    }
                    if (script_representables[k].size() == 0) {
                        continue;
                    }

                    if (script_set[j].containsNone(script_representables[k])) {
                        continue;
                    }
                    final UnicodeSet items = new UnicodeSet(script_set[j]).retainAll(script_representables[k]);
                    final UnicodeSetToScript uss = new UnicodeSetToScript().setScript(k).setSet(items);
                    curr.add(uss);
                }
                scriptToUnicodeSetToScript[j] = (UnicodeSetToScript[]) curr.toArray(new UnicodeSetToScript[curr.size()]);
                fastReject[j] = accept.complement();
            }
            finished = true;
        }

        void write(PrintWriter out) throws IOException {
            finish();

            Map<Pair<String,String>, String> reorder = new TreeMap<>(); // reorder alphabetically

            for (short j = 0; j < UCD_Types.LIMIT_SCRIPT; ++j) {
                final UnicodeSetToScript[] unicodeSetToScripts = scriptToUnicodeSetToScript[j];
                if (unicodeSetToScripts == null) {
                    continue;
                }

                for (int q = 0; q < unicodeSetToScripts.length; ++q) {
                    final UnicodeSetToScript uss = unicodeSetToScripts[q];
                    final short k = uss.getScript();
                    final UnicodeSet items = uss.getSet();

                    // get other side
                    UnicodeSet items2 = UnicodeSet.EMPTY;
                    final UnicodeSetToScript[] unicodeSetToScripts2 = scriptToUnicodeSetToScript[k];
                    for (int qq = 0; qq <  unicodeSetToScripts2.length; ++qq) {
                        final UnicodeSetToScript uss2 = unicodeSetToScripts2[qq];
                        if (uss2.getScript() == j) {
                            items2 = uss2.getSet();
                            break;
                        }
                    }

                    final String sname = UCD.getScriptID_fromIndex(j, UCD_Types.SHORT) + "; " 
                            + UCD.getScriptID_fromIndex(k, UCD_Types.SHORT) + "; " + label;
                    final String name = getScriptIndexName(j, UCD_Types.LONG) 
                            + "; " + getScriptIndexName(k, UCD_Types.LONG);
                    StringWriter b = new StringWriter();
                    PrintWriter out2 = new PrintWriter(b);
                    out2.println("# " + name + ": "
                            + items.toPattern(false) + "; " + items2.toPattern(false) + "\n");
                    bf.setValueSource(sname);
                    bf.showSetNames(out2, items);
                    out2.println("");
                    out2.flush();
                    reorder.put(Pair.of(getScriptIndexName(j, UCD_Types.LONG), getScriptIndexName(k, UCD_Types.LONG)), b.toString());
                    out2.close();
                }
            }
            for (Entry<Pair<String, String>, String> s : reorder.entrySet()) {
                out.print(s.getValue());
            }
        }
        public String getScriptIndexName(short scriptIndex, byte length) {
            return UCharacter.toTitleCase(Locale.ENGLISH, UCD.getScriptID_fromIndex(scriptIndex, length), null);
        }

    }

    /**
     * @throws IOException
     * 
     */
    //    private static void fixMichel(String indir, String outdir) throws IOException {
    //        final BufferedReader in = FileUtilities.openUTF8Reader(indir + "michel/", "tr36comments-annex.txt");
    //        final PrintWriter out = FileUtilities.openUTF8Writer(outdir, "new-tr36comments-annex.txt");
    //        while (true) {
    //            final String line = Utility.readDataLine(in);
    //            if (line == null) {
    //                break;
    //            }
    //            final String[] pieces = Utility.split(line,'\t');
    //            if (pieces.length < 2) {
    //                out.println(line);
    //                continue;
    //            }
    //            final String source = Utility.fromHex(pieces[0].trim());
    //            if (Default.nfkd().isNormalized(source)) {
    //                out.println(line);
    //            }
    //        }
    //        in.close();
    //        out.close();
    //    }
    /**
     * 
     */

    private static void generateSource() throws IOException {
        final File dir = new File(indir);
        final String[] names = dir.list();
        final Set sources = new TreeSet(new ArrayComparator(
                new Comparator[] {codepointComparator, codepointComparator}));

        final int[] count = new int[1];
        for (int i = 0; i < names.length; ++i) {
            if (new File(indir, names[i]).isDirectory()) {
                continue;
            }
            if (!names[i].startsWith("confusables")) {
                continue;
            }
            final String reason = getReasonFromFilename(names[i]);
            if (DEBUG) System.out.println(names[i]);
            final BufferedReader in = FileUtilities.openUTF8Reader(indir, names[i]);
            String line;
            count[0] = 0;
            while (true) {
                line = Utility.readDataLine(in, count);
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                final String[] pieces = Utility.split(line,';');
                if (pieces.length < 2) {
                    System.err.println("Error on: " + line);
                    continue;
                }
                String source = fromHexOld(pieces[0]);
                String target = fromHexOld(pieces[1]);

                if (source.length() == 0 || target.length() == 0) {
                    throw new IllegalArgumentException("zero-length item: " + count[0] + ":\t" + line);
                }

                // check for identical combining sequences
                final String nsource = Default.nfc().normalize(source);
                final String ntarget = Default.nfc().normalize(target);
                if (nsource.equals(ntarget)) {
                    continue;
                }

                if (true) {
                    final int nsourceFirst = UTF16.charAt(nsource,0);
                    final String nsourceRest = nsource.substring(UTF16.getCharCount(nsourceFirst));
                    final int ntargetFirst = UTF16.charAt(ntarget,0);
                    final String ntargetRest = ntarget.substring(UTF16.getCharCount(ntargetFirst));
                    if (nsourceRest.equals(ntargetRest)) {
                        source = UTF16.valueOf(nsourceFirst);
                        target = UTF16.valueOf(ntargetFirst);
                    }
                }

                if (betterTargetIsLess.compare(source, target) < 0) {
                    final String temp = source;
                    source = target;
                    target = temp;
                }
                sources.add(new String[] {source, target});
            }
            in.close();
        }
        final PrintWriter out = FileUtilities.openUTF8Writer(reformatedInternal, "confusableSource.txt");
        for (final Iterator it = sources.iterator(); it.hasNext();) {
            final String[] sourceItem = (String[]) it.next();
            writeSourceTargetLine(out, sourceItem[0], null, sourceItem[1], null, ARROW);
        }
        out.close();
    }

    private static void generateConfusables(String indir, String reformatedInternal, String draftDir) throws IOException {
        final File dir = new File(indir);
        final String[] names = dir.list();
        final DataSet total = new DataSet();
        for (int i = 0; i < names.length; ++i) {
            if (new File(indir, names[i]).isDirectory()) {
                continue;
            }
            if (!names[i].startsWith("confusables-")) {
                continue;
            }
            if (DEBUG) System.out.println(names[i]);
            final DataSet ds = new DataSet();
            ds.addFile(indir, names[i]);
            String newName = null;
            String newDir = null;
            final String stem = names[i].substring("confusables-".length());

            //            if (names[i].equals("confusables-source.txt")) {
            //              newName = "formatted-source.txt";
            //              newDir = outdir + "/source/";
            //            } else
            if (stem.equals("intentional.txt")) {
                newName = stem;
                newDir = draftDir;
            } else {
                newName = "formatted-" + stem;
                newDir = reformatedInternal + "/source/";
            }
            ds.writeSource(newDir, newName);
            ds.close("*");
            total.addAll(ds);
            total.close("t*" + names[i]);
        }
        // add normalized data
        //        for (int i = 0; i <= 0x10FFFF; ++i) {
        //            if (Default.nfkc().isNormalized(i)) continue;
        //            String result = getModifiedNKFC(UTF16.valueOf(i));
        //            ds.foo();
        //        }
        getSkipNFKD();
        DataSet ds = new DataSet();
        ds.addUnicodeMap(nfcMap, "nfc", "nfc");
        ds.close("*");
        total.addAll(ds);
        total.close("*");

        total.checkChar("ſ");
        ds = new DataSet();
        if (DEBUG) System.out.println(nfkcMap.get('ſ'));

        ds.addUnicodeMap(nfkcMap, "nfkc", "nfkc");
        //if (DEBUG) System.out.println(ds);
        ds.checkChar("ſ");
        ds.close("*");
        ds.checkChar("ſ");
        //ds.write(outdir, "new-decomp.txt", false, false);
        total.addAll(ds);
        ds.checkChar("ſ");
        total.close("*");
        ds.checkChar("ſ");

        total.writeData(reformatedInternal + "/source/", "confusablesRaw.txt");
        total.writeSummary(draftDir, "confusablesSummary.txt", false, null);
        total.writeSummary(reformatedInternal, "confusablesSummaryIdentifier.txt", true, null);
        //total.writeSummary(outdir, "confusablesSummaryCyrillic.txt", true,
        //		new UnicodeSet("[[:script=Cyrillic:][:script=common:][:script=inherited:]]"));
        //total.writeWholeScripts(draftDir, "confusablesWholeScript.txt");
        total.writeSourceOrder(draftDir, "confusables.txt", false, false);
        //DataSet clean = total.clean();
        //clean.write(outdir, "confusables.txt", true);
    }
    /*
		BufferedReader in = FileUtilities.openUTF8Reader(Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt");
		Set set = new TreeSet(new ArrayComparator(new Comparator[] {new UTF16.StringComparator(),
				new UTF16.StringComparator()}));
		while (true) {
			String line = Utility.readDataLine(in);
			if (line == null) break;
			if (line.length() == 0) continue;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 2) {
				if (DEBUG) System.out.println("Error on: " + line);
				continue;
			}
			String source = Utility.fromHex(pieces[0].trim());
			String target = Utility.fromHex(pieces[1].trim());
			String nsource = Default.nfkd().normalize(source);
			String first = UTF16.valueOf(UTF16.charAt(nsource, 0));
			if (!first.equals(target)) {
				set.add(new String[]{source, target});
			}
		}
		in.close();

	}
	private static void gen() throws IOException {
		Map m = new TreeMap();
		BufferedReader in = FileUtilities.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables.txt");
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 3) {
				if (DEBUG) System.out.println("Error on: " + line);
				continue;
			}
			int codepoint = Integer.parseInt(pieces[1], 16);
			int cat = DEFAULT_UCD.getCategory(codepoint);
			if (cat == UCD_Types.Co || cat == UCD_Types.Cn) continue; // skip private use
			if (!Default.nfkd().isNormalized(codepoint)) continue; //skip non NFKC
			String result = Utility.fromHex(pieces[0]);
			if (!Default.nfkd().isNormalized(result)) continue; //skip non NFKC
			int count = Integer.parseInt(pieces[2]);
			String source = UTF16.valueOf(codepoint);
			add(m, source, result, count);
		}
		in.close();

		in = FileUtilities.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables2.txt");
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			line = line.trim();
			int pos = line.indexOf("#");
			if (pos >= 0) line = line.substring(0,pos).trim();
			if (line.length() == 0) continue;
			if (line.startsWith("@")) continue;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 2) {
				if (DEBUG) System.out.println("Error on: " + line);
				continue;
			}
			String source = pieces[0].trim();
			for (int i = 1; i < pieces.length; ++i) {
				add(m, source, pieces[i].trim(), -1);
			}
		}
		in.close();

		boolean gotOne;
		// close the set
		do {
			gotOne = false;
			for (Iterator it = m.keySet().iterator(); it.hasNext();) {
				String source = (String) it.next();
				Data2 data = (Data2) m.get(source);
				Data2 data2 = (Data2) m.get(data.target);
				if (data2 == null) continue;
				data.target = data2.target;
				gotOne = true;
				break;
			}
		} while (gotOne);
		// put into different sorting order
		Set s = new TreeSet();
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String source = (String) it.next();
			Data2 data = (Data2) m.get(source);
			s.add(new Data(source, data.target, data.count));
		}
		// write it out
		PrintWriter out = FileUtilities.openUTF8Writer(Utility.GEN_DIR, "confusables.txt");
		String[] replacements = {"%date%", Default.getDate()};
		Utility.appendFile(Settings.SRC_UCD_DIR + "confusablesHeader.txt",
				Utility.UTF8_WINDOWS, out, replacements);
		for (Iterator it = s.iterator(); it.hasNext();) {
			Data d = (Data) it.next();
			if (d == null) continue;
			out.println(formatLine(d.source, d.target, d.count));
		}

		out.close();
		if (DEBUG) System.out.println("Done");
	}
	/**
     * 
     */
    //    private static String formatLine(String source, String target, int count) {
    //        return Utility.hex(source) + " ; " + Utility.hex(target," ")
    //                + " ; " + count
    //                + " # "
    //                + arrowLiterals(source, target, ARROW)
    //                + DEFAULT_UCD.getName(source)
    //                + " " + ARROW + " " + DEFAULT_UCD.getName(target);
    //    }
    /**
     * 
     */
    /*	private static void add(Map m, String source, String target, int count) {
		if (source.length() == 0 || target.length() == 0) return;
		if (preferSecondAsSource(source, target)) {
			String temp = target;
			target = source;
			source = temp;
		}
		Data2 other = (Data2) m.get(source);
		if (other != null) {
			if (target.equals(other.target)) return;
			if (DEBUG) System.out.println("conflict");
			if (DEBUG) System.out.println(formatLine(source, target, count));
			if (DEBUG) System.out.println(formatLine(source, other.target, other.count));
			// skip adding this, and instead add result -> other.target
			add(m, target, other.target, count);
		} else {
			m.put(source, new Data2(target, count));
		}
	};
     */

    private static _BetterTargetIsLess betterTargetIsLess = new _BetterTargetIsLess(false);
    //private static _BetterTargetIsLess betterTargetIsLessFavorNeutral = new _BetterTargetIsLess(true);

    private static boolean isXid(String x) {
        return  XID.containsAll(x);
    }
    
    private static class _BetterTargetIsLess implements Comparator<String> {
        IdentifierInfo info = IdentifierInfo.getIdentifierInfo();
        private boolean favorNeutral;

        _BetterTargetIsLess(boolean favorNeutral) {
            this.favorNeutral = favorNeutral;
        }
        @Override
        public int compare(String a, String b) {
            if (a.equals(b)) {
                return 0;
            }
            int diff;

            // longer is better (less)
            final int ca = UTF16.countCodePoint(a);
            final int cb = UTF16.countCodePoint(b);
            if (ca != cb)  {
                return ca > cb ? -1 : 1;
            }
            
            // favor item with higher old last value, if there is one.
            
            long lasta = LAST_COUNT.get(a);
            long lastb = LAST_COUNT.get(b);
            long ldiff = lasta - lastb;
            if (ldiff != 0) {
                return ldiff < 0 ? 1 : -1; // bigger count is less!!
            };

            if (favorNeutral) {
                boolean isCommonA = COMMON_OR_INHERITED.containsAll(a);
                boolean isCommonB = COMMON_OR_INHERITED.containsAll(b);
                if (isCommonA != isCommonB) {
                    return isCommonA ? -1 : 1;
                }
                boolean isUncasedA = !CASED.containsAll(a);
                boolean isUncasedB = !CASED.containsAll(b);
                if (isUncasedA != isUncasedB) {
                    return isCommonA ? -1 : 1;
                }
            }

            // favor NFKD
            boolean isNfkdA = !notNFKD.containsSome(a);
            boolean isNfkdB = !notNFKD.containsSome(b);
            if (isNfkdA != isNfkdB) {
                return isNfkdA ? -1 : 1;
            }

            String latestA = getMostRecentAge(a);
            String latestB = getMostRecentAge(b);
            if (0 != (diff = latestA.compareTo(latestB))) {
                return diff;
            }

            final boolean asciiA = ASCII.containsAll(a);
            final boolean asciiB = ASCII.containsAll(b);
            if (asciiA != asciiB) {
                return asciiA ? -1 : 1;
            }

            // is Identifier is better
            final boolean ba = isXid(a);
            final boolean bb = isXid(b);
            if (ba != bb) {
                return ba ? -1 : 1;
            }

            if (0 != (diff = compareTrueLess(LATIN.containsSome(a), LATIN.containsSome(b)))) {
                return diff;
            }

            final int aok = getValue(a);
            final int bok = getValue(b);
            if (aok != bok) {
                return aok < bok ? -1 : 1;
            }
            return codepointComparator.compare(a, b);
        }
        //        private static final int BAD = 1000;

        private int getValue(String a) { // lower is better
            //            if (SPECIAL.contains(a)) {
            //                int debug = 0;
            //            }

            int cp;
            int lastValue = 0;
            for (int i = 0; i < a.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(a, i);
                final Object objValue = info.lowerIsBetter.getValue(cp);
                final int value = ((Integer) objValue).intValue();
                if (value > lastValue) {
                    lastValue = value;
                }
            }
            return lastValue;
        }
    };

    //    private static int compare(boolean a, boolean b) {
    //        return a == b ? 0 : a ? 1 : -1;
    //    }

    private static int compareTrueLess(boolean a, boolean b) {
        return a == b ? 0 : a ? -1 : 1;
    }

    static final IndexUnicodeProperties LATEST_IUP = IndexUnicodeProperties.make(version);
    static final UnicodeMap<String> AGE_MAP = LATEST_IUP.load(UcdProperty.Age);

    public static String getMostRecentAge(String a) {
        String latest = "V6_3"; // we don't care about earlier differences
        for (int cp : With.codePointArray(a)) {
            if (cp == '\uA7F7') {
                int debug = 0;
            }
            String age = AGE_MAP.get(cp);
            if (latest == null || Utility.NumericComparator.INSTANCE.compare(age, latest) > 0) {
                latest = age;
            }
        }
        return latest;
    }

    /*	private static boolean preferSecondAsSource(String a, String b) {
		// if first is longer, prefer second
		int ca = UTF16.countCodePoint(a);
		int cb = UTF16.countCodePoint(b);
		if (ca != cb) {
			return ca > cb;
		}
		// if first is lower, prefer second
		return a.compareTo(b) < 0;
	}
     */
    //    private static String getCodeCharName(String a) {
    //        return UCD.getCode(a) + "(  " + a + "  ) " + DEFAULT_UCD.getName(a);
    //    }
    /**
     * Returns the part between - and .
     */
    private static String getReasonFromFilename(String type) {
        int period = type.lastIndexOf('.');
        if (period < 0) {
            period = type.length();
        }
        final int dash = type.lastIndexOf('-', period);
        return type.substring(dash+1,period);
    }

    private static Normalizer modNFKC ;

    static String getModifiedNKFC(String cf) {
        if (modNFKC == null) {
            modNFKC =  new Normalizer(UCD_Types.NFKC, Default.ucdVersion());
            modNFKC.setSpacingSubstitute();
        }
        return modNFKC.normalize(cf);
    }

    static PrintWriter openAndWriteHeader(String dir, String filename, String title) throws IOException {
        final PrintWriter out = FileUtilities.openUTF8Writer(dir, filename);
        out.print('\uFEFF');
        //int trNumber, String title, String filename, String version
        out.println(Utility.getBaseDataHeader(filename, 39, "Unicode Security Mechanisms", version));
//        out.println("# " + title);
//        out.println("# File: " + filename);
//        out.println("# Version: " + version);
//        out.println("# Generated: " + Default.getDate());
//        out.println("# Checkin: $Revision: 1.32 $");
//        out.println("#");
//        out.println("# For documentation and usage, see http://www.unicode.org/reports/tr39/");
//        out.println("#");
        return out;
    }

    private static String fromHexOld(String targetString) {
        String result = Utility.fromHex(targetString.trim(),true);
        final String result2 = fromHexLenient(targetString);
        if (!result.equals(result2)) {
            if (DEBUG) System.out.println("Changing hex\t" + targetString + "\t=>old\t" + result + "\t=>new\t" + result2);
            result = result2;
        }
        return result;
    }

    private static String fromHex(String hexOrChars) {
        hexOrChars = hexOrChars.trim();
        String result;
        try {
            result = Utility.fromHex(hexOrChars);
        } catch (final Exception e) {
            result = hexOrChars;
        }
        final String result2 = fromHexLenient(hexOrChars);
        if (!result.equals(result2)) {
            if (DEBUG) System.out.println("Changing hex\t" + hexOrChars + "\t=>old\t" + result + "\t=>new\t" + result2);
            result = result2;
        }
        return result;
    }

    private static Transform<Linkage<String, String>, String> myLinkageTransform = new Transform<Linkage<String, String>, String>() {
        @Override
        public String transform(Linkage<String, String> source) {
            String sourceString = source.reasons.toString();
            sourceString = sourceString.substring(1,sourceString.length()-1);
            return source.result == null ? "" :
                source.result.length() == 0 ? "\u21d2" :
                    ARROW + rtlProtect(source.result) + ARROW;
        }

    };

}
