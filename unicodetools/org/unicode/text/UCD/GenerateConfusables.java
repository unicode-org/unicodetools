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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Uts46;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.ArrayComparator;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeLabel;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.XEquivalenceClass;
import com.ibm.icu.dev.util.XEquivalenceClass.Linkage;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;


public class GenerateConfusables {
    private static final String version = "6.3.0";
    private static final String REVISION = "6.3.0";
    private static final String outdir = Settings.UNICODETOOLS_DIRECTORY + "data/security/" + REVISION + "/data/";
    private static final String indir = outdir + "source/";


    private static final boolean SHOW_SUPPRESS = false;
    public static boolean EXCLUDE_CONFUSABLE_COMPAT = true;
    public static String recommended_scripts = "recommended";

    static final UnicodeSet SPECIAL = new UnicodeSet("[\u01DD\u0259]").freeze();

    public static void main(String[] args) throws IOException {
        //quickTest();

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
        }
    }

    private static void generateAsciify() throws IOException {
        BufferedReader in = BagFormatter.openUTF8Reader(indir, "asciify.txt");
        final StringBuilder builder = new StringBuilder();
        System.out.println("String rules = \"\"");
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
            System.out.println(" + \"" + com.ibm.icu.impl.Utility.escape(line) + "\\n\"");
        }
        System.out.println(";");
        in.close();
        final String rules = builder.toString();
        final Transliterator asciify = Transliterator.createFromRules("asciify", rules, Transliterator.FORWARD);
        in = BagFormatter.openUTF8Reader(indir, "asciify_examples.txt");
        System.out.println("String[][] translitTestCases = {");
        System.out.println("//{\"" + "SAMPLE" + "\", \"" + "EXPECTED TRANSFORM" + "\"},");
        while (true) {
            final String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            System.out.println("{\"" + com.ibm.icu.impl.Utility.escape(line) + "\", \"" + asciify.transform(line) + "\"},");
        }
        System.out.println("};");
        in.close();
    }

    static final UnicodeSet LATIN = new UnicodeSet("[:script=latin:]").freeze();
    static final UnicodeSet LATIN_PLUS  = new UnicodeSet("[[:script=latin:][:script=common:][:script=inherited:]]").freeze();
    static final UnicodeSet ASCII = new UnicodeSet("[:ASCII:]").freeze();
    static final UnicodeSet MARKS_AND_ASCII = new UnicodeSet("[[:mark:][:ASCII:]]").freeze();

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
            System.out.println(source + "\t→\t" + target +
                    " ; #" + reason + "\t" + Default.ucd().getCodeAndName(source) + "\t→\t" + Default.ucd().getCodeAndName(target));

        }
    }

    private static void addLatin(Map<String, String> mapping, String fileName) throws IOException {

        final BufferedReader in = BagFormatter.openUTF8Reader(indir, fileName);
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
                    System.out.println("Overriding " + source + "=>" + old + " with " + target);
                }

                // skip NFKC forms
                if (Default.nfkd().normalize(source).equals(Default.nfkd().normalize(target))) {
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

    static Matcher HEX = Pattern.compile(
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
    static String fromHexLenient(String hexOrChars) {
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

    private static void quickTest() {
        int script = getSingleScript("\u0430\u0061");
        script = getSingleScript("\u0061\u0430"); //0323 ;  093C
        final String a = "\u0323";
        final String b = "\u093C";
        final int isLess = betterTargetIsLess.compare(a, b); // ("\u0045", "\u13AC");
        final MyEquivalenceClass test = new MyEquivalenceClass();
        test.add(a, b, "none");
        final Set x = test.getEquivalences(a);
        final String result = (String) CollectionUtilities.getBest(x, betterTargetIsLess, -1);
    }

    /**
     * 
     */
    //  static UnicodeSet _Non_IICore;
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
    //      UnicodeMap um = Default.ucd().getHanValue("kIICore");
    //      System.out.println("IICORE SIZE:\t" + um.keySet().size());
    //      um.put(0x34E4, "2.1");
    //      um.put(0x3007, "2.1");
    //      _Non_IICore.removeAll(um.keySet("2.1"));
    //
    //      // add Chinese?
    //      if (true) {
    //        UnicodeSet cjk_nic = new UnicodeSet();
    //        String line = null;
    //        try {
    //          BufferedReader br = BagFormatter.openUTF8Reader(indir, "cjk_nic.txt");
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
    //    //			System.out.println(value + "\t" + set);
    //    //		}
    //  }

    static PrintWriter log;
    static final String ARROW = "→"; // \u2194
    static final String BACKARROW = "\u2190";
    static UnicodeProperty.Factory ups = ToolUnicodePropertySource.make(""); // ICUPropertyFactory.make();

    static UnicodeSet UNASSIGNED =
            ups.getSet("gc=Cn")
            .addAll(ups.getSet("gc=Co"))
            .addAll(ups.getSet("gc=Cs")).freeze();
    static UnicodeSet SKIP_SET =
            ups.getSet("gc=Cc")
            .addAll(ups.getSet("gc=Cf"))
            .addAll(UNASSIGNED).freeze();
    static UnicodeSet WHITESPACE = ups.getSet("Whitespace=Yes").freeze();
    static UnicodeSet GC_LOWERCASE = ups.getSet("gc=Ll").freeze();
    static UnicodeSet _skipNFKD;
    static UnicodeSet COMBINING =
            ups.getSet("gc=Mn")
            .addAll(ups.getSet("gc=Me"))
            .add(0x3099)
            .add(0x309A).freeze();
    static UnicodeSet INVISIBLES =
            ups.getSet("default-ignorable-codepoint=true").freeze();
    static UnicodeSet XIDContinueSet =
            ups.getSet("XID_Continue=true").freeze();
    static UnicodeSet XID = XIDContinueSet;
    static UnicodeSet RTL = new UnicodeSet("[[:bc=R:][:bc=AL:][:bc=AN:]]").freeze();
    static UnicodeSet CONTROLS = new UnicodeSet("[[:cc:][:Zl:][:Zp:]]").freeze();
    static final char LRM = '\u200E';
    private static UnicodeSet commonAndInherited = new UnicodeSet("[[:script=common:][:script=inherited:]]");


    static Map gatheredNFKD = new TreeMap();
    static UnicodeMap nfcMap;
    static UnicodeMap nfkcMap;

    static Comparator codepointComparator = new UTF16.StringComparator(true,false,0);
    static Comparator UCAComparator = new com.ibm.icu.impl.MultiComparator(new Comparator[] {Collator.getInstance(ULocale.ROOT), codepointComparator});

    static UnicodeSet setsToAbbreviate = new UnicodeSet("[" +
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
    }

    private static class IdentifierInfo {
        static private IdentifierInfo info;

        static IdentifierInfo getIdentifierInfo() {
            try {
                if (info == null) {
                    info = new IdentifierInfo();
                }
                return info;
            } catch (final Exception e) {
                throw (RuntimeException) new IllegalArgumentException("Unable to access data").initCause(e);
            }
        }

        private final boolean mergeRanges = true;

        private UnicodeSet removalSet, remainingOutputSet, inputSet_strict, inputSet_lenient, nonstarting;
        UnicodeSet propNFKCSet, notInXID, xidPlus;

        private final UnicodeMap<String> additions = new UnicodeMap();
        private final UnicodeMap<String> remap = new UnicodeMap();
        private final UnicodeMap<Reason> removals = new UnicodeMap<Reason>();
        private final UnicodeMap<String> recastRemovals = new UnicodeMap<String>();

        private UnicodeMap reviews, removals2, lowerIsBetter;

        private UnicodeSet isCaseFolded;

        private IdentifierInfo() throws IOException {
            isCaseFolded = new UnicodeSet();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                final int cat = Default.ucd().getCategory(cp);
                if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                    continue;
                }
                final String source = UTF16.valueOf(cp);
                final String cf = Default.ucd().getCase(source, UCD_Types.FULL, UCD_Types.FOLD);
                if (cf.equals(source)) {
                    isCaseFolded.add(cp);
                }
            }

            propNFKCSet = ups.getSet("NFKC_QuickCheck=N").complement();
            final UnicodeSet propXIDContinueSet = ups.getSet("XID_Continue=Yes");

            //removals.putAll(propNFKCSet.complement(), PROHIBITED + "compat variant");
            loadFileData();
            xidPlus = new UnicodeSet(propXIDContinueSet).addAll(additions.keySet()).retainAll(propNFKCSet);

            getIdentifierSet();
            //notInXID = new UnicodeSet(IDNOutputSet).removeAll(xidPlus);
            //removals.putAll(notInXID, PROHIBITED + NOT_IN_XID);
            //UnicodeSet notNfkcXid = new UnicodeSet(xidPlus).removeAll(removals.keySet()).removeAll(propNFKCSet);
            //removals.putAll(notNfkcXid, PROHIBITED + "compat variant");
            removalSet = new UnicodeSet();
            for (final Reason value : removals.values()) {
                if (value.isRestricted()) {
                    removalSet.addAll(removals.getSet(value));
                }
            }
            removalSet.freeze();

            remainingOutputSet = new UnicodeSet(IDNOutputSet).removeAll(removalSet);

            final UnicodeSet remainingInputSet1 = new UnicodeSet(IDNInputSet)
            .removeAll(removalSet).removeAll(remainingOutputSet);
            final UnicodeSet remainingInputSet = new UnicodeSet();
            final UnicodeSet specialRemove = new UnicodeSet();
            // remove any others that don't normalize/case fold to something in
            // the output set
            for (final UnicodeSetIterator usi = new UnicodeSetIterator(
                    remainingInputSet1); usi.next();) {
                final String nss = getModifiedNKFC(usi.getString());
                final String cf = Default.ucd().getCase(nss, UCD_Types.FULL, UCD_Types.FOLD);
                final String cf2 = getModifiedNKFC(cf);
                if (remainingOutputSet.containsAll(cf2)) {
                    remainingInputSet.add(usi.codepoint);
                } else {
                    specialRemove.add(usi.codepoint);
                }
            }
            // filter out the items that are case foldings of items in output
            inputSet_strict = new UnicodeSet();
            for (final UnicodeSetIterator usi = new UnicodeSetIterator(
                    remainingInputSet); usi.next();) {
                final String ss = usi.getString();
                final String nss = getModifiedNKFC(ss);
                final String cf = Default.ucd().getCase(ss, UCD_Types.FULL, UCD_Types.FOLD);
                if (usi.codepoint == 0x2126 || usi.codepoint == 0x212B) {
                    System.out.println("check");
                }
                //> > 2126 ; retained-input-only-CF # (?) OHM SIGN
                //> > 212B ; retained-input-only-CF # (?) ANGSTROM SIGN

                if (!remainingOutputSet.containsAll(nss)
                        && remainingOutputSet.containsAll(cf)) {
                    inputSet_strict.add(ss);
                }
            }
            // hack
            inputSet_strict.remove(0x03F4).remove(0x2126).remove(0x212B);
            inputSet_lenient = new UnicodeSet(remainingInputSet)
            .removeAll(inputSet_strict);
            nonstarting = new UnicodeSet(remainingOutputSet).addAll(
                    remainingInputSet).retainAll(new UnicodeSet("[:M:]"));
            reviews = new UnicodeMap();
            //reviews.putAll(removals);
            for (final Reason value : removals.values()) {
                reviews.putAll(removals.getSet(value), value.propertyFileFormat());
            }
            reviews.putAll(remainingOutputSet, "output");
            reviews.putAll(inputSet_strict, "input");
            reviews.putAll(inputSet_lenient, "input-lenient");
            reviews.putAll(specialRemove, PROHIBITED + "output-disallowed");

            lowerIsBetter = new UnicodeMap();

            lowerIsBetter.putAll(propNFKCSet, MARK_NFC); // nfkc is better than the alternative
            lowerIsBetter.putAll(inputSet_lenient, MARK_INPUT_LENIENT);
            lowerIsBetter.putAll(inputSet_strict, MARK_INPUT_STRICT);
            lowerIsBetter.putAll(remainingOutputSet, MARK_OUTPUT);
            lowerIsBetter.putAll(remainingOutputSet, MARK_ASCII);
            lowerIsBetter.setMissing(MARK_NOT_NFC);
            
            // EXCEPTIONAL CASES
            // added to preserve source-target ordering in output.
            lowerIsBetter.put('\u0259', MARK_NFC);

            lowerIsBetter.freeze();
            // add special values:
            //lowerIsBetter.putAll(new UnicodeSet("["), new Integer(0));

            final UnicodeMap nonstartingmap = new UnicodeMap().putAll(nonstarting,
                    "nonstarting");
            final UnicodeMap.Composer composer = new UnicodeMap.Composer() {
                @Override
                public Object compose(int codepoint, String string, Object a, Object b) {
                    if (a == null) {
                        return b;
                    } else if (b == null) {
                        return a;
                    } else {
                        return a.toString() + "-" + b.toString();
                    }
                }
            };
            reviews.composeWith(nonstartingmap, composer);
            reviews.putAll(new UnicodeSet(IDNInputSet).complement(), "");
            final UnicodeMap.Composer composer2 = new UnicodeMap.Composer() {
                @Override
                public Object compose(int codepoint, String string, Object a, Object b) {
                    if (b == null) {
                        return a;
                    }
                    return "remap-to-" + Utility.hex(b.toString());
                }
            };
            //reviews.composeWith(remap, composer2);
            removals2 = new UnicodeMap().putAll(recastRemovals);
            removals2.putAll(ups.getSet("XID_Continue=Yes").complement(),
                    PROHIBITED + NOT_IN_XID);
            removals2.setMissing("future?");

            additions.freeze();
            remap.freeze();

            for (final Reason value : removals.values()) {
                recastRemovals.putAll(removals.getSet(value), value.propertyFileFormat());
            }
            recastRemovals.freeze();
            removals.freeze();
            reviews.freeze();
            removals2.freeze();
        }

        enum Reason {
            default_ignorable,
            not_chars,
            not_NFKC,
            not_xid,
            obsolete,
            technical,
            historic,
            limited_use,
            inclusion,
            recommended;

            public static Reason fromString(String string) {
                String rawReason = string.trim().replace("-","_");
                if (rawReason.equals("allowed")) {
                    rawReason = recommended_scripts;
                }
                return valueOf(rawReason);
            }
            public boolean isRestricted() {
                return this != Reason.inclusion && this != Reason.recommended;
            }
            @Override
            public String toString() {
                return name().replace("_","-");
            }
            public String propertyFileFormat() {
                return (isRestricted() ? PROHIBITED : UNPROHIBITED) + toString();
            }
            public boolean replaceBy(Reason possibleReplacement) {
                return compareTo(possibleReplacement) > 0
                        || this == historic && possibleReplacement == limited_use
                        ; // && this != historic;
            }
        }
        /**
         * 
         */
        private void loadFileData() throws IOException {
            BufferedReader br;
            String line;
            // get all the removals.
            br = BagFormatter.openUTF8Reader(indir, "removals.txt");
            removals.putAll(new UnicodeSet("[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]"),
                    Reason.recommended);

            UnicodeSet sources = new UnicodeSet();
            line = null;
            int counter = 0;
            final Set<String> badLines = new LinkedHashSet<String>();
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) {
                    break;
                }
                ++counter;
                if (line.length() == 0) {
                    continue;
                }
                try {
                    sources.clear();
                    final String[] pieces = Utility.split(line, ';');
                    if (pieces.length < 2) {
                        throw new IllegalArgumentException(counter + " Missing line " + line);
                    }
                    final String codelist = pieces[0].trim();
                    final Reason reasons = Reason.fromString(pieces[1]);
                    if (pieces[0].startsWith("[")) {
                        sources = TestUnicodeInvariants.parseUnicodeSet(codelist); //.retainAll(allocated);
                    } else {
                        final String[] codes = Utility.split(codelist, ' ');
                        for (final String code : codes) {
                            if (code.length() == 0) {
                                continue;
                            }
                            final String[] range = code.split("\\.\\.");
                            final int start = Integer.parseInt(range[0], 16);
                            int end = start;
                            if (range.length > 1) {
                                end = Integer.parseInt(range[1], 16);
                            }
                            sources.add(start, end);
                        }
                    }
                    removals.putAll(sources, reasons);
                    //                    if (reasons == Reason.recommended) {
                    //                        removals.putAll(sources, UNPROHIBITED + recommended_scripts);
                    //                    } else if (reasons.equals("inclusion")) {
                    //                        removals.putAll(sources, UNPROHIBITED + reasons);
                    //                    } else {
                    //                        removals.putAll(sources, PROHIBITED + reasons);
                    //                    }
                } catch (final Exception e) {
                    badLines.add(counter + ")\t" + line + "\t" + e.getMessage());
                }
            }
            if (badLines.size() != 0) {
                throw new RuntimeException(
                        "Failure on lines " + CollectionUtilities.join(badLines, "\t\n"));
            }
            final UnicodeMap<String> removalCollision = new UnicodeMap<String>();
            for (final String script : ScriptMetadata.getScripts()) {
                final Info scriptInfo = ScriptMetadata.getInfo(script);
                final IdUsage idUsage = scriptInfo.idUsage;
                Reason status;
                switch(idUsage) {
                case ASPIRATIONAL: case LIMITED_USE:
                    status = Reason.limited_use;
                    break;
                case EXCLUSION:
                    status = Reason.historic;
                    break;
                default:
                    status = null;
                    break; // do nothing;
                }
                if (status != null) {
                    final UnicodeSet us = new UnicodeSet().applyPropertyAlias("script", script);
                    for (final String s : us) {
                        final Reason old = removals.get(s);
                        if (old == null) {
                            removals.put(s, status);
                        } else if (!old.equals(status)){
                            if (old.replaceBy(status)) {
                                removalCollision.put(s, "REPLACING " + old + "\t!= (script metadata)\t" + status);
                                removals.put(s, status);
                            } else {
                                removalCollision.put(s, "Retaining " + old + "\t!= (script metadata)\t" + status);
                            }
                        }
                    }
                }
            }
            for (final String value : removalCollision.values()) {
                System.out.println("*Removal Collision\t" + value + "\n\t" + removalCollision.getSet(value).toPattern(false));
            }
            removals.freeze();
            //removals.putAll(getNonIICore(), PROHIBITED + "~IICore");
            br.close();

            //      // get the word chars
            //      br = BagFormatter.openUTF8Reader(indir,
            //      "wordchars.txt");
            //      try {
            //        while (true) {
            //          line = Utility.readDataLine(br);
            //          if (line == null)
            //            break;
            //          if (line.length() == 0)
            //            continue;
            //          String[] pieces = Utility.split(line, ';');
            //          int code = Integer.parseInt(pieces[0].trim(), 16);
            //          if (pieces[1].trim().equals("remap-to")) {
            //            remap.put(code, UTF16.valueOf(Integer.parseInt(
            //                    pieces[2].trim(), 16)));
            //          } else {
            //            if (XIDContinueSet.contains(code)) {
            //              System.out.println("Already in XID continue: "
            //                      + line);
            //              continue;
            //            }
            //            additions.put(code, "addition");
            //            removals.put(code, UNPROHIBITED + "inclusion");
            //          }
            //        }
            //      } catch (Exception e) {
            //        throw (RuntimeException) new RuntimeException(
            //                "Failure on line " + line).initCause(e);
            //      }
            //      br.close();

        }

        void printIDNStuff() throws IOException {
            final PrintWriter out;
            printIDModifications();
            writeIDChars();
            writeIDReview();
            generateDecompFile();
        }

        /**
         * 
         */
        private void writeIDReview() throws IOException {
            final BagFormatter bf = new BagFormatter();
            bf.setUnicodePropertyFactory(ups);
            bf.setLabelSource(null);
            bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
            bf.setMergeRanges(true);

            final PrintWriter out = openAndWriteHeader(outdir, "review.txt", "Review List for IDN");
            //			PrintWriter out = BagFormatter.openUTF8Writer(outdir, "review.txt");
            //reviews.putAll(UNASSIGNED, "");
            //			out.print("\uFEFF");
            //			out.println("# Review List for IDN");
            //			out.println("# $Revision: 1.32 $");
            //			out.println("# $Date: 2010-06-19 00:29:21 $");
            //			out.println("");

            final UnicodeSet fullSet = reviews.keySet("").complement();

            bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
            }).set(reviews).setMain("Reviews", "GCB",
                    UnicodeProperty.ENUMERATED, "1.0"));
            //bf.setMergeRanges(false);

            final FakeBreak fakeBreak = new FakeBreak();
            bf.setRangeBreakSource(fakeBreak);
            out.println("");
            out.println("# Characters allowed in IDNA");
            out.println("");
            bf.showSetNames(out, new UnicodeSet(fullSet)); // .removeAll(bigSets)
            //bf.setMergeRanges(true);
            //			out.println("");
            //			out.println("# Large Ranges");
            //			out.println("");
            //			bf.showSetNames(out, new UnicodeSet(fullSet).retainAll(bigSets));
            out.println("");
            out.println("# Characters disallowed in IDNA");
            out
            .println("# The IDNA spec doesn't allow any of these characters,");
            out
            .println("# so don't report any of them as being missing from the above list.");
            out
            .println("# Some possible future additions, once IDNA updates to Unicode 4.1, are given.");
            out.println("");
            //bf.setRangeBreakSource(UnicodeLabel.NULL);
            bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
            }).set(removals2).setMain("Removals", "GCB",
                    UnicodeProperty.ENUMERATED, "1.0"));
            //bf.setValueSource(UnicodeLabel.NULL);
            bf.showSetNames(out, new UnicodeSet(IDNInputSet).complement()
                    .removeAll(UNASSIGNED));
            out.close();
        }

        /**
         * 
         */
        private void writeIDChars() throws IOException {
            final BagFormatter bf = new BagFormatter();
            bf.setUnicodePropertyFactory(ups);
            bf.setLabelSource(null);
            bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
            bf.setMergeRanges(true);

            final UnicodeSet letters = new UnicodeSet("[[:Alphabetic:][:Mark:][:Nd:]]");

            final PrintWriter out = openAndWriteHeader(outdir, "idnchars.txt", "Recommended Identifier Profiles for IDN");

            out.println("# Allowed as output characters");
            out.println("");
            bf.setValueSource("output");
            bf.showSetNames(out, remainingOutputSet);
            showExtras(bf, remainingOutputSet, letters);

            /*
			out.println("");

			out.println("");
			out.println("# Input Characters");
			out.println("");
			bf.setValueSource("input");
			bf.showSetNames(out, inputSet_strict);
			showExtras(bf, inputSet_strict, letters);

			out.println("");
			out.println("# Input Characters (lenient)");
			out.println("");
			bf.setValueSource("input-lenient");
			bf.showSetNames(out, inputSet_lenient);
			showExtras(bf, inputSet_lenient, letters);
             */

            out.println("");
            out.println("# Not allowed at start of identifier");
            out.println("");
            bf.setValueSource("nonstarting");
            bf.showSetNames(out, nonstarting);

            //out.println("");

            //showRemapped(out, "Characters remapped on input in GUIs -- Not required by profile!", remap);

            out.close();
        }


        /**
         * 
         */
        private void showExtras(BagFormatter bf, UnicodeSet source, UnicodeSet letters) {
            final UnicodeSet extra = new UnicodeSet(source).removeAll(letters);
            if (extra.size() != 0) {
                final UnicodeSet fixed = new UnicodeSet();
                for (final UnicodeSetIterator it = new UnicodeSetIterator(extra); it.next();) {
                    if (!letters.containsAll(Default.nfkd().normalize(it.getString()))) {
                        fixed.add(it.codepoint);
                    }
                }
                System.out.println(bf.showSetNames(fixed));
            }
        }

        /**
         * 
         */
        private void printIDModifications() throws IOException {
            final BagFormatter bf = new BagFormatter();
            bf.setUnicodePropertyFactory(ups);
            bf.setLabelSource(null);
            bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
            bf.setMergeRanges(true);

            PrintWriter out = openAndWriteHeader(outdir + "../", "xidmodifications.txt", "Security Profile for General Identifiers");
            /* PrintWriter out = BagFormatter.openUTF8Writer(outdir, "xidmodifications.txt");

			out.println("# Security Profile for General Identifiers");
			out.println("# $Revision: 1.32 $");
			out.println("# $Date: 2010-06-19 00:29:21 $");
             */

            //String skipping = "[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]";
            //UnicodeSet skippingSet = new UnicodeSet(skipping);

            out.println("#  All code points not explicitly listed ");
            out.println("#  have the values: restricted; not-chars");
            out.println("# @missing: 0000..10FFFF; restricted ; not-chars");
            out.println("");
            /*
             * for (Iterator it = values.iterator(); it.hasNext();) { String
             * reason1 = (String)it.next(); bf.setValueSource(reason1);
             * out.println(""); bf.showSetNames(out, removals.getSet(reason1)); }
             */
            bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
            }).set(recastRemovals).setMain("Removals", "GCB",
                    UnicodeProperty.ENUMERATED, "1.0"));

            final Set<String> fullListing = new HashSet<String>(Arrays.asList("technical limited-use historic discouraged obsolete".split("\\s+")));
            final Set<String> sortedValues = new TreeSet<String>(Collator.getInstance(ULocale.ENGLISH));
            sortedValues.addAll(recastRemovals.values());
            System.out.println("Restriction Values: " + sortedValues);
            for (final String value : sortedValues) {
                if (value.contains("not-char")) {
                    continue;
                }
                final UnicodeSet uset = recastRemovals.getSet(value);
                out.println("");
                out.println("#\tStatus/Type:\t" + value);
                out.println("");
                //bf.setMergeRanges(Collections.disjoint(fullListing, Arrays.asList(value.split("[\\s;]+"))));
                //bf.setMergeRanges(value.propertyFileFormat());
                bf.showSetNames(out, uset);
            }

            //      out.println("");
            //      out.println("# Characters added");
            //      out.println("");
            //      bf.setMergeRanges(false);
            //      bf.setValueSource("addition");
            //      bf.showSetNames(out, additions.keySet());

            //showRemapped(out, "Characters remapped on input", remap);

            out.close();

            out = openAndWriteHeader(outdir, "xidAllowed.txt", "Security Profile for General Identifiers");
            final UnicodeSet allowed = new UnicodeSet(xidPlus).removeAll(removals.keySet());
            final UnicodeSet cfAllowed = new UnicodeSet().addAll(allowed).retainAll(isCaseFolded).retainAll(propNFKCSet);
            allowed.removeAll(cfAllowed);
            bf.setValueSource("case_folded");
            out.println("# XID characters allowed (no uppercase)");
            out.println("");
            bf.showSetNames(out, cfAllowed);
            bf.setValueSource("not_case_folded");
            out.println("");
            out.println("# XID characters allowed (uppercase)");
            out.println("");
            bf.showSetNames(out, allowed);
            out.close();

            final UnicodeMap someRemovals = new UnicodeMap();
            final UnicodeMap.Composer myComposer = new UnicodeMap.Composer() {
                @Override
                public Object compose(int codePoint, String string, Object a, Object b) {
                    if (b == null) {
                        return null;
                    }
                    String x = (String)b;
                    if (false) {
                        if (!IDNOutputSet.contains(codePoint)) {
                            return "~IDNA";
                        }
                        if (!xidPlus.contains(codePoint)) {
                            return "~Unicode Identifier";
                        }
                    }
                    if (x.startsWith(PROHIBITED)) {
                        x = x.substring(PROHIBITED.length());
                    }
                    //if (!propNFKCSet.contains(codePoint)) x += "*";
                    if (GC_LOWERCASE.contains(codePoint)) {
                        final String upper = Default.ucd().getCase(codePoint, UCD_Types.FULL, UCD_Types.UPPER);
                        if (upper.equals(UTF16.valueOf(codePoint))
                                && x.equals("technical symbol (phonetic)")) {
                            x = "technical symbol (phonetic with no uppercase)";
                        }
                    }
                    return x;
                }
            };
            someRemovals.composeWith(recastRemovals, myComposer);
            final UnicodeSet nonIDNA = new UnicodeSet(IDNOutputSet).addAll(IDNInputSet).complement();
            someRemovals.putAll(nonIDNA, "~IDNA");
            someRemovals.putAll(new UnicodeSet(xidPlus).complement(), "~Unicode Identifier");
            someRemovals.putAll(UNASSIGNED, null); // clear extras
            //someRemovals = removals;
            out = BagFormatter.openUTF8Writer(outdir, "draft-restrictions.txt");
            out.println("# Characters restricted in domain names");
            out.println("# $Revision: 1.32 $");
            out.println("# $Date: 2010-06-19 00:29:21 $");
            out.println("#");
            out.println("# This file contains a draft list of characters for use in");
            out.println("#     UTR #36: Unicode Security Considerations");
            out.println("#     http://unicode.org/draft/reports/tr36/tr36.html");
            out.println("# According to the recommendations in that document, these characters");
            out.println("# would be restricted in domain names: people would only be able to use them");
            out.println("# by using lenient security settings.");
            out.println("#");
            out.println("# If you have any feedback on this list, please use the submission form at:");
            out.println("#     http://unicode.org/reporting.html.");
            out.println("#");
            out.println("# Notes:");
            out.println("# - Characters are listed along with a reason for their removal.");
            out.println("# - Characters listed as ~IDNA are excluded at this point in domain names,");
            out.println("#   in many cases because the international domain name specification does not contain");
            out.println("#   characters beyond Unicode 3.2. At this point in time, feedback on those characters");
            out.println("#   is not relevant.");
            out.println("# - Characters listed as ~Unicode Identifiers are restricted because they");
            out.println("#   do not fit the specification of identifiers given in");
            out.println("#      UAX #31: Identifier and Pattern Syntax");
            out.println("#      http://unicode.org/reports/tr31/");
            out.println("# - Characters listed as ~IICore are restricted because they are Ideographic,");
            out.println("#   but not part of the IICore set defined by the IRG as the minimal set");
            out.println("#   of required ideographs for East Asian use.");
            out.println("# - The files in this directory are 'live', and may change at any time.");
            out.println("#   Please include the above Revision number in your feedback.");

            bf.setRangeBreakSource(new FakeBreak2());
            if (true) {
                final Set values = new TreeSet(someRemovals.getAvailableValues());
                for (final Iterator it = values.iterator(); it.hasNext();) {
                    final String reason1 = (String) it.next();
                    bf.setValueSource(reason1);
                    out.println("");
                    bf.showSetNames(out, someRemovals.keySet(reason1));
                }
            } else {
                bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
                }).set(someRemovals).setMain("Removals", "GCB",
                        UnicodeProperty.ENUMERATED, "1.0"));
                bf.showSetNames(out, someRemovals.keySet());
            }
            out.close();
        }
    }

    static final String PROHIBITED = "restricted ; ";
    static final String UNPROHIBITED = "allowed ; ";
    static final String NOT_IN_XID = "not in XID+";
    public static final boolean suppress_NFKC = true;
    /**
     * 
     */


    /**
     * 
     */
    private static void generateDecompFile() throws IOException {
        final PrintWriter out = BagFormatter.openUTF8Writer(outdir, "decomps.txt");
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

    static class FakeBreak extends UnicodeLabel {
        UnicodeSet nobreakSet = setsToAbbreviate;
        @Override
        public String getValue(int codepoint, boolean isShort) {
            return nobreakSet.contains(codepoint) ? ""
                    : (codepoint & 1) == 0 ? "O"
                            : "E";
        }
    }

    static class FakeBreak2 extends UnicodeLabel {
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
    private static void showRemapped(PrintWriter out, String title, UnicodeMap remap) {
        out.println("");
        out.println("# " + title);
        out.println("");
        int count = 0;
        for (final UnicodeSetIterator usi = new UnicodeSetIterator(remap.keySet()); usi.next();) {
            writeSourceTargetLine(out, usi.getString(), "remap-to", (String)remap.getValue(usi.codepoint), null, ARROW);
            count++;
        }
        out.println("");
        out.println("# Total code points: " + count);
    }
    /**
     * 
     */
    private static UnicodeSet IDNOutputSet, IDNInputSet, _preferredIDSet;

    static UnicodeSet getIdentifierSet() {
        if (_preferredIDSet == null) {
            IDNOutputSet = new UnicodeSet();
            IDNInputSet = new UnicodeSet();
            IDNOutputSet.add('-'); // HACK
            IDNInputSet.add('-');
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                Utility.dot(cp);
                final int cat = Default.ucd().getCategory(cp);
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

    static private UnicodeSet SKIP_EXCEPTIONS = new UnicodeSet().add(0x1E9A).add('ſ').add('ﬅ').add('ẛ').add("Ϲ").add("ϲ").freeze();

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
                final int cat = Default.ucd().getCategory(cp);
                if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                    continue;
                }
                final int decompType = Default.ucd().getDecompositionType(cp);
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
                        ) {
                    _skipNFKD.add(cp);
                    continue;
                }
                final String source = UTF16.valueOf(cp);
                final String mapped = Default.nfkd().normalize(cp);
                String kmapped = getModifiedNKFC(source);
                if (!kmapped.equals(source) && !kmapped.equals(nfc)) {
                    if (kmapped.startsWith(" ") || kmapped.startsWith("\u0640")) {
                        System.out.println("?? " + Default.ucd().getCodeAndName(cp));
                        System.out.println("\t" + Default.ucd().getCodeAndName(kmapped));
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

    private static boolean isMixedScript(String source) {
        return getSingleScript(source) == UScript.INVALID_CODE;
    }

    /**
     * Returns the script of the input text. Script values of COMMON and INHERITED are ignored.
     * @param source Input text.
     * @return Script value found in the text.
     * If more than one script values are found, then UScript.INVALID_CODE is returned.
     * If no script value is found (other than COMMON or INHERITED), then UScript.COMMON is returned.
     */
    public static int getSingleScript(String source) {
        if (source.length() == 0) {
            return UScript.COMMON;
        }
        int lastScript = UScript.COMMON; // temporary value
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            final int script = UScript.getScript(cp);
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                continue;
            }
            if (lastScript == UScript.COMMON) {
                lastScript = script;
            } else if (script != lastScript) {
                return UScript.INVALID_CODE;
            }
        }
        return lastScript;
    }

    /**
     * 
     */
    private static void generateConfusables() throws IOException {
        log = BagFormatter.openUTF8Writer(outdir, "log.txt");
        //fixMichel(indir, outdir);
        generateConfusables(indir, outdir);
        log.close();
        if (false) {
            for (final Iterator it = gatheredNFKD.keySet().iterator(); it.hasNext();) {
                final String source = (String)it.next();
                System.out.println(Default.ucd().getCodeAndName(source)
                        + " => " + Default.ucd().getCodeAndName((String)gatheredNFKD.get(source)));
            }
        }
    }

    /*	static class Data2 {
		String source;
		String target;
		int count;
		Data2(String target, int count) {
			this.target = target;
			this.count = count;
		}
	}
     */
    /*	static class Data implements Comparable {
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
    static void writeSourceTargetLine(PrintWriter out, String source, String tag, String target, String reason, String relation) {
        out.print(
                Utility.hex(source)
                + " ;\t" + Utility.hex(target)
                + (tag == null ? "" : " ;\t" + tag)
                //+ " ;\t" + (preferredID.contains(source) ? "ID" : "")
                + "\t#"
                + (isXid(source) ? "" : "*")
                + arrowLiterals(source, target, relation)
                + Default.ucd().getName(source) + " " + relation + " "
                + Default.ucd().getName(target)
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

    static class MyEquivalenceClass extends XEquivalenceClass<String,String> {
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
                                + Default.ucd().getCodeAndName(x) + " contains "
                                + Default.ucd().getCodeAndName(y) + " because "
                                + Default.ucd().getCodeAndName(a) + " ~ "
                                + Default.ucd().getCodeAndName(b) + " because of "
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
                            + Default.ucd().getCodeAndName(a)
                            + " overlaps "
                            + Default.ucd().getCodeAndName(b2)
                            + "\n\tfrom "
                            + Default.ucd().getCodeAndName(b)
                            + "\n\twith reason "
                            + reason + " plus "
                            + getReasons(b2, b));
                    return true;
                }
            }
            return false;
        }

        public XEquivalenceClass add(Object a1, Object b1, String reason) {
            final String a = (String)a1;
            final String b = (String)b1;
            try {
                addCheck(a, b, reason);
                return this;
            } catch (final RuntimeException e) {
                throw (RuntimeException) new RuntimeException("Failure adding "
                        + Default.ucd().getCodeAndName(a) + "; "
                        + Default.ucd().getCodeAndName(b)
                        + "; " + reason).initCause(e);
            }
        }
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
                for (final Iterator it = cloneForSafety.iterator(); it.hasNext();) {
                    final String item = (String) it.next();
                    if (!UTF16.hasMoreCodePointsThan(item,1))
                    {
                        continue; // just for speed
                    }
                    reasons.setLength(0);
                    final String mapped = mapString(item, reasons);
                    if (!isEquivalent(item, mapped)) {
                        if (addCheck(item, mapped, reasons.toString())) {
                            // System.out.println("Closing: " + Default.ucd().getCodeAndName(item) + " => " + Default.ucd().getCodeAndName(mapped));
                            addedItem = true;
                        }
                    }
                }
            } while (addedItem);
        }

        /**
         * 
         */
        private String mapString(String item, StringBuffer reasons) {
            if (false && item.startsWith("\u03D2")) {
                System.out.println("foo");
            }
            final StringBuffer result = new StringBuffer();
            int cp;
            for (int i = 0; i < item.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(item, i);
                final String cps = UTF16.valueOf(cp);
                final String mapped = getParadigm(cps, false, false);
                if (mapped.indexOf(cps) >= 0) {
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
            Set filteredSet;
            if (onlyLowercase == false && onlySameScript == false) {
                filteredSet = getEquivalences(item);
            } else {
                filteredSet = new HashSet();
                for (final Object element : getEquivalences(item)) {
                    final String other = (String) element;
                    final String combined = item + other;
                    if (onlyLowercase) {
                        final boolean isLowercase = combined.equals(Default.ucd().getCase(combined, UCD_Types.FULL, UCD_Types.FOLD));
                        if (!isLowercase) {
                            continue;
                        }
                    }
                    if (onlySameScript) {
                        final boolean isMixed = isMixedScript(combined);
                        if (isMixed) {
                            continue;
                        }
                    }
                    filteredSet.add(other);
                }
            }
            return (String) CollectionUtilities.getBest(filteredSet, betterTargetIsLess, -1);
        }

        public Set getOrderedExplicitItems() {
            final Set cloneForSafety = new TreeSet(codepointComparator);
            cloneForSafety.addAll(getExplicitItems());
            return cloneForSafety;
        }
        /**
         * 
         */
        public void writeSource(PrintWriter out) {
            final Set items = getOrderedExplicitItems();
            for (final Iterator it = items.iterator(); it.hasNext();) {
                final String item = (String) it.next();
                final String paradigm = CollectionUtilities.getBest(getEquivalences(item), betterTargetIsLess, -1);
                if (item.equals(paradigm)) {
                    continue;
                }
                writeSourceTargetLine(out, item, null, paradigm, null, ARROW);
            }
        }
    }

    static class RawData {
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

    static class DataSet {
        MyEquivalenceClass dataMixedLowercase = new MyEquivalenceClass();
        MyEquivalenceClass dataMixedAnycase = new MyEquivalenceClass();
        MyEquivalenceClass dataSingleLowercase = new MyEquivalenceClass();
        MyEquivalenceClass dataSingleAnycase = new MyEquivalenceClass();
        RawData raw = new RawData();

        static String testChar = UTF16.valueOf(0x10A3A);

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
            if (combined.indexOf("\u0430") >= 0) {
                System.out.println(Default.ucd().getCodeAndName(combined));
            }
            final boolean isLowercase = combined.equals(Default.ucd().getCase(combined, UCD_Types.FULL, UCD_Types.FOLD));
            final boolean isMixed = isMixedScript(combined);
            raw.add(source,target,type);
            dataMixedAnycase.add(source, target, type);
            if (isLowercase) {
                dataMixedLowercase.add(source, target, type);
            }
            if (!isMixed) {
                dataSingleAnycase.add(source, target, type);
            }
            if (!isMixed && isLowercase) {
                dataSingleLowercase.add(source, target, type);
            }
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
        static final int NORMAL = 0, FOLDING = 1, OLD = 2;
        static final UnicodeSet NSM = new UnicodeSet("[[:Mn:][:Me:]]").freeze();

        public DataSet addFile(String directory, String filename) throws IOException {
            String line = null;
            int count = 0;
            try {
                final BufferedReader in = BagFormatter.openUTF8Reader(directory, filename);
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
                        System.out.println("Error on: (" + count + ")\t" + line);
                        continue;
                    }
                    final String type = filename;
                    final String sourceString = INVISIBLES.stripFrom(pieces[0].trim(), true);
                    final String targetString = INVISIBLES.stripFrom(pieces[1].trim(), true);

                    if (!targetString.equals(pieces[1].trim())) {
                        System.out.println("**\t" + Utility.hex(pieces[0].trim()) + ";\t" + Utility.hex(targetString));
                    }
                    if (kind==FOLDING) {
                        final String target = fromHexOld(targetString);
                        final String source = fromHexOld(sourceString);
                        final String nsource = Default.nfkd().normalize(source);
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
                            System.err.println("ERROR: paren on " + count + "\t" + line);
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
            final String nfkdSource = Default.nfkd().normalize(source);
            final String nfkdTarget = Default.nfkd().normalize(target);
            if (NSM.containsAll(source) && NSM.containsNone(target)
                    || NSM.containsAll(target) && NSM.containsNone(source)) {
                if (SHOW_SUPPRESS) {
                    System.out.println("*** SUPPRESSING NSM Difference\t"
                            + count + "\t" + Default.ucd().getCodeAndName(source) + ";\t" + Default.ucd().getCodeAndName(target) + ";\t" + line);
                }
            } else if (suppress_NFKC && nfkdSource.equals(nfkdTarget)) {
                if (SHOW_SUPPRESS) {
                    System.out.println("*** Suppressing nfkc for:\t"
                            + count + "\t" + Default.ucd().getCodeAndName(source) + ";\t" + Default.ucd().getCodeAndName(target) + ";\t" + line);
                }
            } else {
                add(source, target, type, count, line);
            }
        }

        public void writeSource(String directory, String filename) throws IOException {
            final PrintWriter out = openAndWriteHeader(directory, filename, "Source File for IDN Confusables");
            //			PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
            //			out.println("# Source File for IDN Confusables");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");
            raw.writeSource(out);
            out.close();
        }

        public void writeSourceOrder(String directory, String filename, boolean appendFile, boolean skipNFKEquivs) throws IOException {
            final PrintWriter out = openAndWriteHeader(directory, filename, "Recommended confusable mapping for IDN");
            //            PrintWriter out = BagFormatter.openUTF8Writer(directory, filename);
            //			out.println("# Recommended confusable mapping for IDN");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");

            if (appendFile) {
                final String[] replacements = {"%date%", Default.getDate()};
                Utility.appendFile(Settings.SRC_UCD_DIR + "confusablesHeader.txt",
                        Utility.UTF8_WINDOWS, out, replacements);
            }
            if (true) {
                writeSourceOrder(out, dataMixedAnycase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, true, true);
                writeSourceOrder(out, dataMixedAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, false, true);
                writeSourceOrder(out, dataMixedAnycase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, true, false);
                writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, false, false);
            } else {
                writeSourceOrder(out, dataSingleLowercase, "SL", "Single-Script, Lowercase Confusables", skipNFKEquivs, false, false);
                writeSourceOrder(out, dataSingleAnycase, "SA", "Single-Script, Anycase Confusables", skipNFKEquivs, false, false);
                writeSourceOrder(out, dataMixedLowercase, "ML", "Mixed-Script, Lowercase Confusables", skipNFKEquivs, false, false);
                writeSourceOrder(out, dataMixedAnycase, "MA", "Mixed-Script, Anycase Confusables", skipNFKEquivs, false, false);
            }
            out.close();
        }
        /**
         * @param skipNFKEquivs TODO
         * @param onlyLowercase TODO
         * @param onlySingleScript TODO
         * 
         */
        private void writeSourceOrder(PrintWriter out, MyEquivalenceClass data, String tag, String title, boolean skipNFKEquivs, boolean onlyLowercase, boolean onlySingleScript) {
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
            out.println();
            out.println("# " + title);
            out.println();
            int count = 0;
            final UnicodeSet preferredID = getIdentifierSet();
            final ArrayComparator ac = new ArrayComparator(new Comparator[] {UCAComparator, UCAComparator});
            final Set orderedPairs = new TreeSet(ac);
            for (final Iterator it = items.iterator(); it.hasNext();) {
                final String source = (String) it.next();
                if (UTF16.hasMoreCodePointsThan(source,1)) {
                    continue;
                }
                final String target = data.getParadigm(source, onlyLowercase, onlySingleScript);
                if (target == null) {
                    continue;
                }
                if (source.equals(target)) {
                    continue;
                }
                if (skipNFKEquivs) {
                    if (!Default.nfkd().normalize(source).equals(source)) {
                        continue;
                    }
                }
                orderedPairs.add(new String[] {target, source});
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
            out.println("# total for (" + tag + "): " + count);
            out.println();
        }

        /**
         * 
         */
        private String fixReason(List reasons) {
            final List first = (List)reasons.get(0);
            String result = "";
            for (int i = 0; i < first.size(); ++i) {
                if (i != 0) {
                    result += " ";
                }
                final Object item = first.get(i);
                if (item instanceof String) {
                    result += item;
                } else {
                    String temp = "";
                    for (final Iterator it = ((Set)item).iterator(); it.hasNext();) {
                        if (temp.length() != 0) {
                            temp += "|";
                        }
                        temp += it.next();
                    }
                    result += "{" + temp + "}";
                }
            }
            return result.toString();
        }

        public void addAll(DataSet ds) {
            dataMixedAnycase.addAll(ds.dataMixedAnycase);
            dataMixedLowercase.addAll(ds.dataMixedLowercase);
            dataSingleAnycase.addAll(ds.dataSingleAnycase);
            dataSingleLowercase.addAll(ds.dataSingleLowercase);
        }

        private void checkChar(String string) {
            // debug
            final Set<String> test = getEquivalences(string);
            System.out.println(test);
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
            dataMixedLowercase.close(reason);
            dataSingleAnycase.close(reason);
            dataSingleLowercase.close(reason);
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

        static class MyFilter implements XEquivalenceClass.Filter {
            UnicodeSet output;
            @Override
            public boolean matches(Object o) {
                return output.containsAll((String)o);
            }
        }

        static class MyCollectionFilter implements CollectionUtilities.ObjectMatcher {
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
            //			PrintWriter out = BagFormatter.openUTF8Writer(outdir, filename);
            //			out.print('\uFEFF');
            //			out.println("# Summary: Recommended confusable mapping for IDN");
            //			out.println("# $ Revision: 1.32 $");
            //			out.println("# $ Date: 2010-06-19 00:29:21 $");
            //			out.println("");
            final UnicodeSet representable = new UnicodeSet();
            final MyEquivalenceClass data = dataMixedAnycase;
            final Set items = data.getOrderedExplicitItems();
            //			for (Iterator it = items.iterator(); it.hasNext();) {
            //				System.out.println(Default.ucd().getCodeAndName((String)it.next()));
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
                out.println(status + "\t" + "(\u200E " + target + " \u200E)\t" + Utility.hex(target) + "\t " + Default.ucd().getName(target));
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

                    out.println(BACKARROW + status + "\t" + "(\u200E " + source + " \u200E)\t" + Utility.hex(source) + "\t " + Default.ucd().getName(source)
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
                final BagFormatter bf = new BagFormatter();
                bf.setValueSource(ups.getProperty("script"));
                bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
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
            //			PrintWriter out = BagFormatter.openUTF8Writer(outdir, filename);
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
        private String getStatus(String source) {
            // TODO Auto-generated method stub
            final int val = betterTargetIsLess.getValue(source);
            if (val == MARK_NOT_NFC.intValue()) {
                return "[x]";
            }
            if (val == MARK_NFC.intValue()) {
                return "[x]";
            }
            if (val == MARK_INPUT_LENIENT.intValue()) {
                return "[L]";
            }
            if (val == MARK_INPUT_STRICT.intValue()) {
                return "[I]";
            }
            if (val == MARK_OUTPUT.intValue()) {
                return "[O]";
            }
            if (val == MARK_ASCII.intValue()) {
                return "[A]";
            }

            return "?";
        }

        public void writeData(String string, String string2) {
            // TODO Auto-generated method stub

        }
    }

    static class WholeScript {
        private final UnicodeSet filterSet;
        private final UnicodeSet[] script_representables = new UnicodeSet[UScript.CODE_LIMIT];
        private final UnicodeSet[] script_set = new UnicodeSet[UScript.CODE_LIMIT];
        private final BagFormatter bf = new BagFormatter();
        private final String label;
        {
            for (int i = 0; i < UScript.CODE_LIMIT; ++i) {
                script_representables[i] = new UnicodeSet();
                script_set[i] = new UnicodeSet("[:script=" + UScript.getName(i) + ":]"); // ugly hack
            }
            bf.setValueSource(ups.getProperty("script"));
            bf.setShowLiteral(TransliteratorUtilities.toHTMLControl);
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
                if (script1 == UScript.INVALID_CODE) {
                    continue;
                }
                for (final Iterator it2 = set.iterator(); it2.hasNext();) {
                    final String item2 = (String)it2.next();
                    if (!filterSet.containsAll(item2)) {
                        continue;
                    }
                    final int script2 = getSingleScript(item2);
                    if (script2 == UScript.INVALID_CODE || script2 == script1) {
                        continue;
                    }
                    script_representables[script1].addAll(item2).removeAll(commonAndInherited);
                }
            }
        }

        public static class UnicodeSetToScript {
            public int getScript() {
                return script;
            }
            public UnicodeSetToScript setScript(int script) {
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
            private int script;
        }

        UnicodeSetToScript[][] scriptToUnicodeSetToScript = new UnicodeSetToScript[UScript.CODE_LIMIT][];
        UnicodeSet[] fastReject = new UnicodeSet[UScript.CODE_LIMIT];
        boolean finished = false;

        void finish() {
            if (finished) {
                return;
            }
            for (int j = 0; j < UScript.CODE_LIMIT; ++j) {
                if (j == UScript.COMMON || j == UScript.INHERITED) {
                    continue;
                }
                if (script_representables[j].size() == 0) {
                    continue;
                }
                final UnicodeSet accept = new UnicodeSet();
                final List curr = new ArrayList();
                for (int k = 0; k < UScript.CODE_LIMIT; ++k) {
                    if (k == UScript.COMMON || k == UScript.INHERITED) {
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
            for (int j = 0; j < UScript.CODE_LIMIT; ++j) {
                if (scriptToUnicodeSetToScript[j] == null) {
                    continue;
                }
                for (int q = 0; q < scriptToUnicodeSetToScript[j].length; ++q) {
                    final UnicodeSetToScript uss = scriptToUnicodeSetToScript[j][q];
                    final int k = uss.getScript();
                    final UnicodeSet items = uss.getSet();
                    final String sname = UScript.getShortName(j) + "; " + UScript.getShortName(k) + "; " + label;
                    final String name = UScript.getName(j) + "; " + UScript.getName(k);
                    out.println("# " + name + ": " + items.toPattern(false));
                    out.println("");
                    bf.setValueSource(sname);
                    bf.showSetNames(out, items);
                    out.println("");
                }
            }
        }

    }

    /**
     * @throws IOException
     * 
     */
    private static void fixMichel(String indir, String outdir) throws IOException {
        final BufferedReader in = BagFormatter.openUTF8Reader(indir + "michel/", "tr36comments-annex.txt");
        final PrintWriter out = BagFormatter.openUTF8Writer(outdir, "new-tr36comments-annex.txt");
        while (true) {
            final String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            final String[] pieces = Utility.split(line,'\t');
            if (pieces.length < 2) {
                out.println(line);
                continue;
            }
            final String source = Utility.fromHex(pieces[0].trim());
            if (Default.nfkd().isNormalized(source)) {
                out.println(line);
            }
        }
        in.close();
        out.close();
    }
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
            System.out.println(names[i]);
            final BufferedReader in = BagFormatter.openUTF8Reader(indir, names[i]);
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
                    System.out.println("Error on: " + line);
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
        final PrintWriter out = BagFormatter.openUTF8Writer(outdir, "confusableSource.txt");
        for (final Iterator it = sources.iterator(); it.hasNext();) {
            final String[] sourceItem = (String[]) it.next();
            writeSourceTargetLine(out, sourceItem[0], null, sourceItem[1], null, ARROW);
        }
        out.close();
    }

    private static void generateConfusables(String indir, String outdir) throws IOException {
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
            System.out.println(names[i]);
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
                newDir = outdir + "../";
            } else {
                newName = "formatted-" + stem;
                newDir = outdir + "/source/";
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
        System.out.println(nfkcMap.get('ſ'));

        ds.addUnicodeMap(nfkcMap, "nfkc", "nfkc");
        //System.out.println(ds);
        ds.checkChar("ſ");
        ds.close("*");
        ds.checkChar("ſ");
        //ds.write(outdir, "new-decomp.txt", false, false);
        total.addAll(ds);
        ds.checkChar("ſ");
        total.close("*");
        ds.checkChar("ſ");

        total.writeData(outdir + "/source/", "confusablesRaw.txt");
        total.writeSummary(outdir + "../", "confusablesSummary.txt", false, null);
        total.writeSummary(outdir, "confusablesSummaryIdentifier.txt", true, null);
        //total.writeSummary(outdir, "confusablesSummaryCyrillic.txt", true,
        //		new UnicodeSet("[[:script=Cyrillic:][:script=common:][:script=inherited:]]"));
        total.writeWholeScripts(outdir + "../", "confusablesWholeScript.txt");
        total.writeSourceOrder(outdir + "../", "confusables.txt", false, false);
        //DataSet clean = total.clean();
        //clean.write(outdir, "confusables.txt", true);
    }
    /*
		BufferedReader in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "DiacriticFolding.txt");
		Set set = new TreeSet(new ArrayComparator(new Comparator[] {new UTF16.StringComparator(),
				new UTF16.StringComparator()}));
		while (true) {
			String line = Utility.readDataLine(in);
			if (line == null) break;
			if (line.length() == 0) continue;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 2) {
				System.out.println("Error on: " + line);
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
	public static void gen() throws IOException {
		Map m = new TreeMap();
		BufferedReader in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables.txt");
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			String[] pieces = Utility.split(line,';');
			if (pieces.length < 3) {
				System.out.println("Error on: " + line);
				continue;
			}
			int codepoint = Integer.parseInt(pieces[1], 16);
			int cat = Default.ucd().getCategory(codepoint);
			if (cat == UCD_Types.Co || cat == UCD_Types.Cn) continue; // skip private use
			if (!Default.nfkd().isNormalized(codepoint)) continue; //skip non NFKC
			String result = Utility.fromHex(pieces[0]);
			if (!Default.nfkd().isNormalized(result)) continue; //skip non NFKC
			int count = Integer.parseInt(pieces[2]);
			String source = UTF16.valueOf(codepoint);
			add(m, source, result, count);
		}
		in.close();

		in = BagFormatter.openUTF8Reader(Utility.BASE_DIR + "confusables/", "confusables2.txt");
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
				System.out.println("Error on: " + line);
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
		PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIR, "confusables.txt");
		String[] replacements = {"%date%", Default.getDate()};
		Utility.appendFile(Settings.SRC_UCD_DIR + "confusablesHeader.txt",
				Utility.UTF8_WINDOWS, out, replacements);
		for (Iterator it = s.iterator(); it.hasNext();) {
			Data d = (Data) it.next();
			if (d == null) continue;
			out.println(formatLine(d.source, d.target, d.count));
		}

		out.close();
		System.out.println("Done");
	}
	/**
     * 
     */
    private static String formatLine(String source, String target, int count) {
        return Utility.hex(source) + " ; " + Utility.hex(target," ")
                + " ; " + count
                + " # "
                + arrowLiterals(source, target, ARROW)
                + Default.ucd().getName(source)
                + " " + ARROW + " " + Default.ucd().getName(target);
    }
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
			System.out.println("conflict");
			System.out.println(formatLine(source, target, count));
			System.out.println(formatLine(source, other.target, other.count));
			// skip adding this, and instead add result -> other.target
			add(m, target, other.target, count);
		} else {
			m.put(source, new Data2(target, count));
		}
	};
     */

    static Integer
    MARK_NOT_NFC = new Integer(50),
    MARK_NFC = new Integer(40),
    MARK_INPUT_LENIENT = new Integer(30),
    MARK_INPUT_STRICT = new Integer(20),
    MARK_OUTPUT = new Integer(10),
    MARK_ASCII = new Integer(10);

    static _BetterTargetIsLess betterTargetIsLess = new _BetterTargetIsLess();

    static boolean isXid(String x) {
        return  XID.containsAll(x);
    }

    static class _BetterTargetIsLess implements Comparator<String> {
        IdentifierInfo info = IdentifierInfo.getIdentifierInfo();

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
        static final int BAD = 1000;

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

    static int compare(boolean a, boolean b) {
        return a == b ? 0 : a ? 1 : -1;
    }

    static int compareTrueLess(boolean a, boolean b) {
        return a == b ? 0 : a ? -1 : 1;
    }

    /*	static private boolean preferSecondAsSource(String a, String b) {
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
    static String getCodeCharName(String a) {
        Default.ucd();
        return UCD.getCode(a) + "(  " + a + "  ) " + Default.ucd().getName(a);
    }
    /**
     * Returns the part between - and .
     */
    public static String getReasonFromFilename(String type) {
        int period = type.lastIndexOf('.');
        if (period < 0) {
            period = type.length();
        }
        final int dash = type.lastIndexOf('-', period);
        return type.substring(dash+1,period);
    }

    static Normalizer modNFKC ;

    private static String getModifiedNKFC(String cf) {
        if (modNFKC == null) {
            modNFKC =  new Normalizer(UCD_Types.NFKC, Default.ucdVersion());
            modNFKC.setSpacingSubstitute();
        }
        return modNFKC.normalize(cf);
    }

    private static PrintWriter openAndWriteHeader(String dir, String filename, String title) throws IOException {
        final PrintWriter out = BagFormatter.openUTF8Writer(dir, filename);
        out.print('\uFEFF');
        out.println("# " + title);
        out.println("# File: " + filename);
        out.println("# Version: " + version);
        out.println("# Generated: " + Default.getDate());
        out.println("# Checkin: $Revision: 1.32 $");
        out.println("#");
        out.println("# For documentation and usage, see http://www.unicode.org/reports/tr39/");
        out.println("#");
        return out;
    }

    public static String fromHexOld(String targetString) {
        String result = Utility.fromHex(targetString.trim(),true);
        final String result2 = fromHexLenient(targetString);
        if (!result.equals(result2)) {
            System.out.println("Changing hex\t" + targetString + "\t=>old\t" + result + "\t=>new\t" + result2);
            result = result2;
        }
        return result;
    }

    static String fromHex(String hexOrChars) {
        hexOrChars = hexOrChars.trim();
        String result;
        try {
            result = Utility.fromHex(hexOrChars);
        } catch (final Exception e) {
            result = hexOrChars;
        }
        final String result2 = fromHexLenient(hexOrChars);
        if (!result.equals(result2)) {
            System.out.println("Changing hex\t" + hexOrChars + "\t=>old\t" + result + "\t=>new\t" + result2);
            result = result2;
        }
        return result;
    }

    static Transform<Linkage<String, String>, String> myLinkageTransform = new Transform<Linkage<String, String>, String>() {
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