package org.unicode.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Differ;
import org.unicode.cldr.util.MultiComparator;
import org.unicode.cldr.util.XEquivalenceClass;
import org.unicode.draft.ComparePinyin.PinyinSource;
import org.unicode.jsp.FileUtilities.SemiFileReader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCA.RadicalStroke;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class GenerateUnihanCollators {
    private static final boolean DEBUG = false;

    private static String version = CldrUtility.getProperty("UVERSION");
    static {
        System.out.println("To make files for a different version of unicode, use -DUVERSION=x.y.z");
        if (version == null) {
            version = Settings.latestVersion;
        } else {
            System.out.println("Resetting default version to: " + version);
            Default.setUCD(version);
        }
    }
    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(version);
    private static final RadicalStroke radicalStroke = new RadicalStroke(version);
    private static final char INDEX_ITEM_BASE = '\u2800';

    private enum FileType {txt, xml}
    private enum InfoType {
        radicalStroke("\uFDD2"), stroke("\uFDD1"), pinyin("\uFDD0");
        final String base = "\uFDD0";
        InfoType(String base) {
            //this.base = base;
        }
    }

    private enum OverrideItems {keepOld, keepNew}

    private static final Transform<String, String>    fromNumericPinyin   = Transliterator.getInstance("NumericPinyin-Latin;nfc");
    private static final Transliterator               toNumericPinyin     = Transliterator.getInstance("Latin-NumericPinyin;nfc");

    private static final Normalizer                  nfkd                = Default.nfkd();
    private static final Normalizer                  nfd                 = Default.nfd();
    private static final Normalizer                  nfc                 = Default.nfc();

    private static final UnicodeSet                   PINYIN_LETTERS      = new UnicodeSet("['a-uw-zàáèéìíòóùúüāēěīōūǎǐǒǔǖǘǚǜ]").freeze();

    // these should be ok, eve if we are not on an old version

    private static final UnicodeSet                   NOT_NFC             = new UnicodeSet("[:nfc_qc=no:]").freeze();
    private static final UnicodeSet                   NOT_NFD             = new UnicodeSet("[:nfd_qc=no:]").freeze();
    private static final UnicodeSet                   NOT_NFKD             = new UnicodeSet("[:nfkd_qc=no:]").freeze();

    // specifically restrict this to the set version. Theoretically there could be some variance in ideographic, but it isn't worth worrying about

    private static final UnicodeSet                   UNIHAN_LATEST         = new UnicodeSet("[[:ideographic:][:script=han:]]")
            .removeAll(NOT_NFC)
            .freeze();
    private static final UnicodeSet                   UNIHAN                = version == null ? UNIHAN_LATEST
            : new UnicodeSet("[:age=" + version + ":]")
            .retainAll(UNIHAN_LATEST)
            .freeze();
    static {
        if (!UNIHAN.contains(0x2B820)) {
            throw new ICUException(Utility.hex(0x2B820) + " not supported");
        }
    }

    private static Matcher                            unicodeCp           = Pattern.compile("^U\\+(2?[0-9A-F]{4})$").matcher("");
    private static final HashMap<String, Boolean>     validPinyin         = new HashMap<String, Boolean>();
    private static final Collator                     pinyinSort          = Collator.getInstance(new ULocale("zh@collator=pinyin"));
    private static final Collator                     strokeSort          = Collator.getInstance(new ULocale("zh@collator=stroke"));

    private static final Comparator<String>           codepointComparator = new UTF16.StringComparator(true, false, 0);
    private static final Comparator<String>           nfkdComparator = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            if (!nfkd.isNormalized(o1) || !nfkd.isNormalized(o2)) {
                final String s1 = nfkd.normalize(o1);
                final String s2 = nfkd.normalize(o2);
                final int result = codepointComparator.compare(s1, s2);
                if (result != 0) {
                    return result; // otherwise fall through to codepoint comparator
                }
            }
            return codepointComparator.compare(o1, o2);
        }
    };

    private static final UnicodeMap<Integer>          bestStrokesS        = new UnicodeMap<Integer>();
    private static final UnicodeMap<Integer>          bestStrokesT         = new UnicodeMap<Integer>();
    private static final Splitter ONBAR = Splitter.on('|').trimResults();
    private static final Splitter ONSPACE = Splitter.on(' ').trimResults();
    private static final Splitter ONCOMMA = Splitter.on(',').trimResults();

    static {
        UnicodeMap<String> kTotalStrokes = IUP.load(UcdProperty.kTotalStrokes);
        for (EntryRange<String> s : kTotalStrokes.entryRanges()) {
            List<String> parts = ONBAR.splitToList(s.value);
            Integer sValue = Integer.parseInt(parts.get(0));
            Integer tValue = parts.size() == 1 ? sValue : Integer.parseInt(parts.get(1));
            if (s.string != null) {
                bestStrokesS.put(s.string, sValue);
                bestStrokesT.put(s.string, tValue);
            } else {
                bestStrokesS.putAll(s.codepoint, s.codepointEnd, sValue);
                bestStrokesT.putAll(s.codepoint, s.codepointEnd, tValue);
            }
        }
    }

    private static UnicodeMap<Row.R2<String, String>> bihuaData           = new UnicodeMap<Row.R2<String, String>>();

    private static final UnicodeMap<String>           kRSUnicode          = IUP.load(UcdProperty.kRSUnicode).cloneAsThawed();
    private static final UnicodeMap<String>           kSimplifiedVariant  = IUP.load(UcdProperty.kSimplifiedVariant);
    private static final UnicodeMap<String>           kTraditionalVariant = IUP.load(UcdProperty.kTraditionalVariant);

    private static final UnicodeMap<Set<String>>      mergedPinyin        = new UnicodeMap<Set<String>>();
    private static final UnicodeSet                   originalPinyin;

    private static final boolean only19 = System.getProperty("only19") != null;
    private static UnicodeMap<String>                 radicalMap          = new UnicodeMap<String>();

    // kHanyuPinyin, space, 10297.260: qīn,qìn,qǐn,
    // [a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]+(,[a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]
    // kMandarin, space, [A-Z\x{308}]+[1-5] // 3475=HAN4 JI2 JIE2 ZHA3 ZI2
    // kHanyuPinlu, space, [a-z\x{308}]+[1-5]\([0-9]+\) 4E0A=shang4(12308)
    // shang5(392)
    private static UnicodeMap<String>        bestPinyin = new UnicodeMap<>();

    // while these use NFKD, for the repertoire they apply to it should work.
    private static Transform<String, String> noaccents  = Transliterator.getInstance("nfkd; [[:m:]-[\u0308]] remove; nfc");

    private static UnicodeSet                INITIALS   = new UnicodeSet("[b c {ch} d f g h j k l m n p q r s {sh} t w x y z {zh}]").freeze();
    private static UnicodeSet                FINALS     = new UnicodeSet(
            "[a {ai} {an} {ang} {ao} e {ei} {en} {eng} {er} i {ia} {ian} {iang} {iao} {ie} {in} {ing} {iong} {iu} o {ong} {ou} u {ua} {uai} {uan} {uang} {ue} {ui} {un} {uo} ü {üe}]")
            .freeze();
    private static final int NO_STROKE_INFO = Integer.MAX_VALUE;

    // We need to quote at least the collation syntax characters, see
    // http://www.unicode.org/reports/tr35/tr35-collation.html#Rules
    private static UnicodeSet NEEDSQUOTE = new UnicodeSet("[_[:pattern_syntax:][:pattern_whitespace:]]").freeze();

    private static final XEquivalenceClass<Integer, Integer> variantEquivalents = new XEquivalenceClass<Integer, Integer>();
    private static final String INDENT = "               ";


    static {
        System.out.println("kRSUnicode " + kRSUnicode.size());

        new BihuaReader().process(GenerateUnihanCollators.class, "bihua-chinese-sorting.txt");
        getBestStrokes();
        new PatchStrokeReader(bestStrokesS, Pattern.compile("\\t\\s*")).process(GenerateUnihanCollators.class, "ucs-strokes-ext-e.txt");
        RsInfo.addToStrokeInfo(bestStrokesS, true);

        bestStrokesT.putAll(bestStrokesS);
        // patch the values for the T strokes
        new PatchStrokeReader(bestStrokesT, SemiFileReader.SPLIT).process(GenerateUnihanCollators.class, "patchStrokeT.txt");
        RsInfo.addToStrokeInfo(bestStrokesT, false);

        new MyFileReader().process(GenerateUnihanCollators.class, "CJK_Radicals.csv");
        if (false) {
            for (final String s : radicalMap.values()) {
                System.out.println(s + "\t" + radicalMap.getSet(s).toPattern(false));
            }
        }
        addRadicals(kRSUnicode);
        closeUnderNFKD("Unihan", kRSUnicode);

        final UnicodeMap<String> kMandarin = IUP.load(UcdProperty.kMandarin);
        System.out.println("UcdProperty.kMandarin " + kMandarin.size());
        for (final String s : kMandarin.keySet()) {
            final String original = kMandarin.get(s);
            String source = original.toLowerCase();
            source = fromNumericPinyin.transform(source);
            addAllKeepingOld(s, original, PinyinSource.m, ONSPACE.split(source));
        }

        // all kHanyuPinlu readings first; then take all kXHC1983; then
        // kHanyuPinyin.

        final UnicodeMap<String> kHanyuPinlu = IUP.load(UcdProperty.kHanyuPinlu);
        for (final String s : kHanyuPinlu.keySet()) {
            final String original = kHanyuPinlu.get(s);
            String source = original.replaceAll("\\([0-9]+\\)", "");
            source = fromNumericPinyin.transform(source);
            addAllKeepingOld(s, original, PinyinSource.l, ONBAR.split(source));
        }

        // kXHC1983
        // ^[0-9,.*]+:*[a-zx{FC}x{300}x{301}x{304}x{308}x{30C}]+$
        final UnicodeMap<String> kXHC1983 = IUP.load(UcdProperty.kXHC1983);
        System.out.println("UcdProperty.kXHC1983 " + kXHC1983.size());
        for (final String s : kXHC1983.keySet()) {
            final String original = kXHC1983.get(s);
            String source = nfc.normalize(original);
            source = source.replaceAll("([0-9,.*]+:)*", "");
            addAllKeepingOld(s, original, PinyinSource.x, ONBAR.split(source));
        }

        Pattern stuffToRemove = Pattern.compile("(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:");
        final UnicodeMap<String> kHanyuPinyin = IUP.load(UcdProperty.kHanyuPinyin);
        System.out.println("UcdProperty.kHanyuPinyin " + kHanyuPinyin.size());
        for (final String s : kHanyuPinyin.keySet()) {
            final String original2 = kHanyuPinyin.get(s);
            for (String original : ONBAR.split(original2)) {
                String source = nfc.normalize(original);
                source = stuffToRemove.matcher(source).replaceAll("");
                // only
                // for
                // medial
                //source = source.replaceAll("\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ",");
                addAllKeepingOld(s, original, PinyinSource.p, ONCOMMA.split(source));
            }

        }

        originalPinyin = mergedPinyin.keySet().freeze();
        int count = mergedPinyin.size();
        System.out.println("unihanPinyin original size " + count);

        addBihua();
        count += showAdded("bihua", count);

        addRadicals();
        count += showAdded("radicals", count);

        addEquivalents(kTraditionalVariant);
        addEquivalents(kSimplifiedVariant);

        count += addPinyinFromVariants("STVariants", count);
        //count += showAdded("kTraditionalVariant", count);

        //addVariants("kSimplifiedVariant", kSimplifiedVariant);
        //count += showAdded("kSimplifiedVariant", count);

        new PatchPinyinReader().process(GenerateUnihanCollators.class, "patchPinyin.txt");

        closeUnderNFKD("Pinyin", bestPinyin);

        printExtraPinyinForUnihan();
    }

    public static void main(String[] args) throws Exception {
        showOldData(pinyinSort, "pinyin1.8.txt", false);
        showOldData(strokeSort, "stroke1.8.txt", false);
        showOldData(Collator.getInstance(new ULocale("ja")), "japanese1.8.txt", true);

        final UnicodeSet zh = FindHanSizes.getMostFrequent("zh", 0.999);
        final UnicodeSet zh_Hant = FindHanSizes.getMostFrequent("zh_Hant", 0.999);

        //Matcher charsetMatcher = Pattern.compile("GB2312|GBK|Big5|Big5-HKSCS").matcher("");
        final UnicodeSet GB2312 = FindHanSizes.getCharsetRepertoire("GB2312");
        final UnicodeSet GBK = FindHanSizes.getCharsetRepertoire("GBK");
        final UnicodeSet Big5 = FindHanSizes.getCharsetRepertoire("Big5");
        final UnicodeSet Big5_HKSCS = FindHanSizes.getCharsetRepertoire("Big5-HKSCS");

        final UnicodeSet shortPinyin = new UnicodeSet(zh).addAll(GB2312).addAll(GBK);
        final UnicodeSet shortStroke = new UnicodeSet(shortPinyin).addAll(zh_Hant).addAll(Big5).addAll(Big5_HKSCS);


        showSorting(RSComparator, kRSUnicode, "unihan", InfoType.radicalStroke);
        testSorting(RSComparator, kRSUnicode, "unihan");

        writeAndTest(shortPinyin, PinyinComparator, bestPinyin, "pinyin", InfoType.pinyin);
        //writeAndTest(shortStroke, SStrokeComparator, bestStrokesS, "stroke", InfoType.stroke);
        writeAndTest(shortStroke, TStrokeComparator, bestStrokesT, "strokeT", InfoType.stroke);

        for (final Entry<InfoType, Set<String>> entry : indexValues.keyValuesSet()) {
            final InfoType infoType = entry.getKey();
            final UnicodeSet sorted = new UnicodeSet();
            sorted.addAll(entry.getValue());
            System.out.println(infoType + "\t" + sorted);
        }

        writeUnihanFields(bestPinyin, bestPinyin, mergedPinyin, PinyinComparator, "kMandarin");
        writeUnihanFields(bestStrokesS, bestStrokesT, null, SStrokeComparator, "kTotalStrokes");

        //            showSorting(PinyinComparator, bestPinyin, "pinyin");
        //            UnicodeMap<String> shortPinyinMap = new UnicodeMap<String>().putAllFiltered(bestPinyin, shortPinyin);
        //            System.out.println("stroke_pinyin base size:\t" + shortPinyinMap.size());
        //            showSorting(PinyinComparator, shortPinyinMap, "pinyin_short");
        //            testSorting(PinyinComparator, bestPinyin, "pinyin");
        //            testSorting(PinyinComparator, shortPinyinMap, "pinyin_short");


        //            showSorting(TStrokeComparator, bestStrokesT, "strokeT");
        //            UnicodeMap<Integer> shortStrokeMapT = new UnicodeMap<Integer>().putAllFiltered(bestStrokesT, shortStroke);
        //            System.out.println("Tstroke_stroke base size:\t" + shortStrokeMapT.size());
        //            showSorting(TStrokeComparator, shortStrokeMapT, "stroke_shortT");
        //            testSorting(TStrokeComparator, bestStrokesT, "strokeT");
        //            testSorting(TStrokeComparator, shortStrokeMapT, "stroke_shortT");


        //showSorting(PinyinComparator, bestPinyin, "pinyinCollationInterleaved", true, FileType.TXT);


        showTranslit("Han-Latin");

        showBackgroundData();


        System.out.println("TODO: test the translit");

        getIndexChars();
    }

    /**
     * U+3400   kMandarin   QIU1
     * U+3400   kTotalStrokes   5
     * @param <U>
     * @param <T>
     * @param simplified
     * @param traditional
     * @param other
     * @param comp
     * @param filename
     */
    private static <U, T> void writeUnihanFields(UnicodeMap<U> simplified, UnicodeMap<U> traditional, UnicodeMap<T> other, Comparator<String> comp, String filename) {
        final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".txt", null);
        final UnicodeSet keys = new UnicodeSet(simplified.keySet()).addAll(traditional.keySet());
        final Set<String> sorted = new TreeSet<String>(comp);
        UnicodeSet.addAllTo(keys, sorted);
        for (final String s : sorted) {
            final U simp = simplified.get(s);
            final U trad = traditional.get(s);
            String item;
            if (simp == null) {
                item = trad.toString();
            } else if (trad != null && !simp.equals(trad)) {
                item = simp + " " + trad;
            } else {
                item = simp.toString();
            }
            final T commentSource = other == null ? null : other.get(s);
            String comments = "";
            if (commentSource == null) {
                // do nothing
            } else if (commentSource instanceof Set) {
                @SuppressWarnings("unchecked")
                final
                LinkedHashSet<String> temp = new LinkedHashSet<String>((Set<String>)commentSource);
                temp.remove(simp);
                temp.remove(trad);
                comments = CollectionUtilities.join(temp, " ");
            } else {
                comments = commentSource.toString();
                if (comments.equals(item)) {
                    comments = "";
                }
            }
            out.println("U+" + Utility.hex(s) + "\t" + filename + "\t" + item + "\t# " + s + (comments.isEmpty() ? "" : "\t" + comments));
        }
        out.close();
    }

    private static <T> void writeAndTest(UnicodeSet shortStroke, Comparator<String> comparator2, UnicodeMap<T> unicodeMap2, String title2, InfoType infoType) throws Exception {
        showSorting(comparator2, unicodeMap2, title2, infoType);
        testSorting(comparator2, unicodeMap2, title2);
        final UnicodeMap<T> shortMap = new UnicodeMap<T>().putAllFiltered(unicodeMap2, shortStroke);
        System.out.println(title2 + " base size:\t" + shortMap.size());
        showSorting(comparator2, shortMap, title2 + "_short", infoType);
        testSorting(comparator2, shortMap, title2 + "_short");
    }

    private static void showOldData(Collator collator, String name, boolean japanese) {
        final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, name, null);
        final UnicodeSet tailored = collator.getTailoredSet();
        final TreeSet<String> sorted = new TreeSet<String>(collator);
        for (final String s : tailored) {
            sorted.add(nfc.normalize(s));
        }
        final UnicodeMap<String>          kJapaneseKun         = IUP.load(UcdProperty.kJapaneseKun);
        final UnicodeMap<String>          kJapaneseOn         = IUP.load(UcdProperty.kJapaneseOn);

        final StringBuilder buffer = new StringBuilder();
        out.println("#char; strokes; radical; rem-strokes; reading");
        for (final String item : sorted) {
            buffer.append("<").append(item).append("\t#");
            final String code = Utility.hex(item);
            buffer.append(pad(code,6)).append(";\t");

            int strokes = CldrUtility.ifNull(bestStrokesS.get(item), 0);
            buffer.append(pad(String.valueOf(strokes),3)).append(";\t");

            int data = getRSShortData(item.codePointAt(0));
            String radical = null;
            String remainingStrokes = null;
            if (data != 0) {
                radical = radicalStroke.getRadicalStringFromShortData(data);
                remainingStrokes = RadicalStroke.getResidualStrokesFromShortData(data) + "";
            }
            buffer.append(pad(radical,4)).append(";\t");
            buffer.append(pad(remainingStrokes,2)).append(";\t");

            if (japanese) {
                final String reading = kJapaneseKun.get(item);
                final String reading2 = kJapaneseOn.get(item);
                buffer.append(pad(reading,1)).append(";\t");
                buffer.append(pad(reading2,1)).append(";\t");
            } else {
                final Set<String> pinyins = mergedPinyin.get(item);
                if (pinyins != null) {
                    boolean first = true;
                    for (final String pinyin : pinyins) {
                        if (first) {
                            first = false;
                        } else {
                            buffer.append(", ");
                        }
                        buffer.append(pinyin);
                    }
                } else {
                    buffer.append("?");
                }
            }
            out.println(buffer);
            buffer.setLength(0);
        }
        out.close();
    }

    private static String pad(String strokes, int padSize) {
        if (strokes == null) {
            strokes = "?";
        } else {
            strokes = strokes.toLowerCase(Locale.ENGLISH).replace(" ", ", ");
        }
        return Utility.repeat(" ", padSize - strokes.length()) + strokes;
    }

    private static void getIndexChars() {
        // TODO Auto-generated method stub
        final UnicodeSet tailored = pinyinSort.getTailoredSet();
        final TreeMap<String, String> sorted = new TreeMap<String, String>(pinyinSort);
        final Counter<String> counter = new Counter<String>();
        for (final String s : tailored) {
            String pinyin = bestPinyin.get(s);
            if (pinyin == null) {
                pinyin = "?";
            } else {
                pinyin = nfd.normalize(pinyin);
                pinyin = pinyin.substring(0, 1).toUpperCase().intern();
            }
            counter.add(pinyin, 1);
            sorted.put(s, pinyin);
        }
        System.out.println(counter);
        String lastPinyin = "";
        int count = 0;
        final Counter<String> progressive = new Counter<String>();

        for (final String s : sorted.keySet()) {
            final String pinyin = sorted.get(s);
            progressive.add(pinyin, 1);
            if (pinyin.equals(lastPinyin)) {
                count++;
            } else {
                if (DEBUG) System.out.println("\t" + count + "\t"
                        + (progressive.get(lastPinyin) / (double) counter.get(lastPinyin)));
                count = 1;
                lastPinyin = pinyin;
                System.out.print(s + "\t" + pinyin + "\t");
            }
        }
        if (DEBUG) System.out.println("\t" + count + "\t"
                + (progressive.get(lastPinyin) / (double) counter.get(lastPinyin)));
    }

    private static void getBestStrokes() {
        final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, "kTotalStrokesReplacements.txt", null);

        out.println("#Code\tkTotalStrokes\tValue\t#\tChar\tUnihan");

        for (final String s : new UnicodeSet(bihuaData.keySet()).addAll(bestStrokesS.keySet())) {
            final int unihanStrokes = CldrUtility.ifNull(bestStrokesS.get(s), NO_STROKE_INFO);
            final R2<String, String> bihua = bihuaData.get(s);
            final int bihuaStrokes = bihua == null ? NO_STROKE_INFO : bihua.get1().length();
            if (bihuaStrokes != NO_STROKE_INFO) {
                bestStrokesS.put(s, bihuaStrokes);
            } else if (unihanStrokes != NO_STROKE_INFO) {
                bestStrokesS.put(s, unihanStrokes);
            }
            if (bihuaStrokes != NO_STROKE_INFO && bihuaStrokes != unihanStrokes) {
                out.println("U+" + Utility.hex(s) + "\tkTotalStrokes\t" + bihuaStrokes + "\t#\t" + s + "\t" + unihanStrokes);
            }
        }
        out.close();

        new PatchStrokeReader(bestStrokesS, SemiFileReader.SPLIT).process(GenerateUnihanCollators.class, "patchStroke.txt");
    }

    private static <T> void closeUnderNFKD(String title, UnicodeMap<T> mapping) {
        //        UnicodeSet possibles = new UnicodeSet(NOT_NFKD).removeAll(NOT_NFD).removeAll(mapping.keySet());
        //        if (!possibles.contains("㊀")) {
        //            System.out.println("??");
        //        }
        //
        //        for (String s : possibles) {
        //            if (s.equals("㊀")) {
        //                System.out.println("??");
        //            }
        //            String kd = nfkd.normalize(s);
        //            T value = mapping.get(kd);
        //            if (value == null) {
        //                continue;
        //            }
        //            mapping.put(s, value);
        //            System.out.println("*** " + title + " Closing " + s + " => " + kd + "; " + value);
        //        }
        final UnicodeSet extras = new UnicodeSet(NOT_NFKD).retainAll(mapping.keySet());
        if (extras.size() != 0) {
            System.out.println("*** " + title + " Removing " + extras.toPattern(false));
            mapping.putAll(extras, null);
        }
        mapping.freeze();
    }

    private static void showBackgroundData() throws IOException {
        final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, "backgroundCjkData.txt", null);
        final UnicodeSet all = new UnicodeSet(bihuaData.keySet()); // .addAll(allPinyin.keySet()).addAll(kRSUnicode.keySet());
        final Comparator<Row.R4<String, String, Integer, String>> comparator = new Comparator<Row.R4<String, String, Integer, String>>() {
            @Override
            public int compare(R4<String, String, Integer, String> o1, R4<String, String, Integer, String> o2) {
                int result = o1.get0().compareTo(o2.get0());
                if (result != 0) {
                    return result;
                }
                result = pinyinSort.compare(o1.get1(), o2.get1());
                if (result != 0) {
                    return result;
                }
                result = o1.get2().compareTo(o2.get2());
                if (result != 0) {
                    return result;
                }
                result = o1.get3().compareTo(o2.get3());
                return result;
            }

        };
        final Set<Row.R4<String, String, Integer, String>> items = new TreeSet<Row.R4<String, String, Integer, String>>(comparator);
        for (final String s : all) {
            final R2<String, String> bihua = bihuaData.get(s);
            final int bihuaStrokes = bihua == null ? 0 : bihua.get1().length();
            final String bihuaPinyin = bihua == null ? "?" : bihua.get0();
            final Set<String> allPinyins = mergedPinyin.get(s);
            final String firstPinyin = allPinyins == null ? "?" : allPinyins.iterator().next();
            final String rs = kRSUnicode.get(s);

            final int totalStrokes = org.unicode.cldr.util.CldrUtility.ifNull(bestStrokesS.get(s),0);
            // for (String rsItem : rs.split(" ")) {
            // RsInfo rsInfo = RsInfo.from(rsItem);
            // int totalStrokes = rsInfo.totalStrokes;
            // totals.set(totalStrokes);
            // if (firstTotal != -1) firstTotal = totalStrokes;
            // int radicalsStokes = bihuaStrokes - rsInfo.remainingStrokes;
            // counter.add(Row.of(rsInfo.radical + (rsInfo.alternate == 1 ? "'"
            // : ""), radicalsStokes), 1);
            // }
            final String status = (bihuaPinyin.equals(firstPinyin) ? "-" : "P") + (bihuaStrokes == totalStrokes ? "-" : "S");
            items.add(Row.of(status, firstPinyin, totalStrokes,
                    status + "\t" + s + "\t" + rs + "\t" + totalStrokes + "\t" + bihuaStrokes + "\t" + bihua + "\t" + mergedPinyin.get(s)));
        }
        for (final R4<String, String, Integer, String> item : items) {
            out.println(item.get3());
        }
        out.close();
    }

    // Markus: Could not figure out how to avoid type safety warnings with
    // Comparator collator = new MultiComparator(coll, codepointComparator);
    // Note that Collator is a Comparator<Object> and it cannot also be a Comparator<something else>.
    private static final class CollatorWithTieBreaker implements Comparator<String> {
        private final Collator coll;
        private final Comparator<String> tieBreaker;
        CollatorWithTieBreaker(Collator c, Comparator<String> tb) {
            coll = c;
            tieBreaker = tb;
        }
        public int compare(String left, String right) {
            int result = coll.compare(left, right);
            if (result != 0) {
                return result;
            }
            return tieBreaker.compare(left, right);
        }
    }

    private static <S> void testSorting(Comparator<String> oldComparator, UnicodeMap<S> krsunicode2, String filename) throws Exception {
        final List<String> temp = krsunicode2.keySet().addAllTo(new ArrayList<String>());
        final String rules = getFileAsString(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY + File.separatorChar + filename + ".txt");

        // The rules contain \uFDD0 and such and must be unescaped for the RuleBasedCollator.
        final Collator coll = new RuleBasedCollator(com.ibm.icu.impl.Utility.unescape(rules));
        final Comparator<String> collator = new CollatorWithTieBreaker(coll, codepointComparator);
        final List<String> ruleSorted = sortList(collator, temp);

        @SuppressWarnings("unchecked")
        final Comparator<String> oldCollator = new MultiComparator<String>(oldComparator, codepointComparator);
        final List<String> originalSorted = sortList(oldCollator, temp);
        int badItems = 0;
        final int min = Math.min(originalSorted.size(), ruleSorted.size());
        final Differ<String> differ = new Differ<String>(100, 2);
        for (int k = 0; k < min; ++k) {
            final String ruleItem = ruleSorted.get(k);
            final String originalItem = originalSorted.get(k);
            if (ruleItem == null || originalItem == null) {
                throw new IllegalArgumentException();
            }
            differ.add(originalItem, ruleItem);

            differ.checkMatch(k == min - 1);

            final int aCount = differ.getACount();
            final int bCount = differ.getBCount();
            if (aCount != 0 || bCount != 0) {
                badItems += aCount + bCount;
                System.out.println(aline(krsunicode2, differ, -1) + "\t" + bline(krsunicode2, differ, -1));

                if (aCount != 0) {
                    for (int i = 0; i < aCount; ++i) {
                        System.out.println(aline(krsunicode2, differ, i));
                    }
                }
                if (bCount != 0) {
                    for (int i = 0; i < bCount; ++i) {
                        System.out.println("\t\t\t\t\t\t" + bline(krsunicode2, differ, i));
                    }
                }
                System.out.println(aline(krsunicode2, differ, aCount) + "\t " + bline(krsunicode2, differ, bCount));
                System.out.println("-----");
            }

            // if (!ruleItem.equals(originalItem)) {
            // badItems += 1;
            // if (badItems < 50) {
            // System.out.println(i + ", " + ruleItem + ", " + originalItem);
            // }
            // }
        }
        System.out.println(badItems + " differences");
    }

    private static <S> String aline(UnicodeMap<S> krsunicode2, Differ<String> differ, int i) {
        final String item = differ.getA(i);
        try {
            return "unihan: " + differ.getALine(i) + " " + item + " [" + Utility.hex(item) + "/" + krsunicode2.get(item) + "]";
        } catch (final RuntimeException e) {
            throw e;
        }
    }

    private static <S> String bline(UnicodeMap<S> krsunicode2, Differ<String> differ, int i) {
        final String item = differ.getB(i);
        return "rules: " + differ.getBLine(i) + " " + item + " [" + Utility.hex(item) + "/" + krsunicode2.get(item) + "]";
    }

    private static List<String> sortList(Comparator<String> collator, List<String> temp) {
        final String[] ruleSorted = temp.toArray(new String[temp.size()]);
        Arrays.sort(ruleSorted, collator);
        return Arrays.asList(ruleSorted);
    }

    // private static String getFileAsString(Class<GenerateUnihanCollators> relativeToClass, String filename) throws IOException {
    //     final BufferedReader in = FileUtilities.openFile(relativeToClass, filename);
    //     ... same as the version below
    // }

    private static String getFileAsString(String filename) throws IOException {
        final InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), FileUtilities.UTF8);
        final BufferedReader in = new BufferedReader(reader, 1024 * 64);
        final StringBuilder builder = new StringBuilder();
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            builder.append(line).append('\n');
        }
        in.close();
        return builder.toString();
    }

    private static void showTranslit(String filename) {
        final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".txt", null);
        final PrintWriter out2 = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + ".xml", null);
        final TreeSet<String> s = new TreeSet<String>(pinyinSort);
        s.addAll(bestPinyin.getAvailableValues());

        for (final String value : s) {
            final UnicodeSet uset = bestPinyin.getSet(value);
            // [专叀塼嫥専專瑼甎砖磚篿耑膞蟤跧鄟顓颛鱄鷒]→zhuān;
            out.println(uset.toPattern(false) + "→" + value + ";");
            out2.println(uset.toPattern(false) + "→" + value + ";");
        }
        out.close();
        out2.close();
    }

    private static class RsInfo {
        public static void addToStrokeInfo(UnicodeMap<Integer> bestStrokesIn, boolean simplified) {
            final int[] mainStrokes      = new int[256];
            final int[] alternateStrokes = new int[256];

            final Counter<Integer> mainStrokesTotal = new Counter<Integer>();
            final Counter<Integer> mainCount = new Counter<Integer>();
            final Counter<Integer> alternateStrokesTotal = new Counter<Integer>();
            final Counter<Integer> alternateCount = new Counter<Integer>();
            for (final String s : bestStrokesIn) {
                final int c = s.codePointAt(0);
                final Integer bestStrokeInfo = bestStrokesIn.get(c);
                int data = getRSShortData(c);
                if (data == 0) {
                    continue;
                }
                int radical = RadicalStroke.getRadicalNumberFromShortData(data);
                final int radicalsStrokes = bestStrokeInfo - RadicalStroke.getResidualStrokesFromShortData(data);
                if (!RadicalStroke.isSimplifiedFromShortData(data)) {
                    mainStrokesTotal.add(radical, radicalsStrokes);
                    mainCount.add(radical, 1);
                } else {
                    alternateStrokesTotal.add(radical, radicalsStrokes);
                    alternateCount.add(radical, 1);
                }
            }
            // compute averages. Lame, but the best we have for now.
            for (final int key : mainStrokesTotal.keySet()) {
                mainStrokes[key] = (int) Math.round(mainStrokesTotal.get(key) / (double) mainCount.get(key));
                if (DEBUG) System.out.println("radical " + key + "\t" + mainStrokes[key]);
            }
            for (final int key : alternateStrokesTotal.keySet()) {
                alternateStrokes[key] = (int) Math.round(alternateStrokesTotal.get(key) / (double) alternateCount.get(key));
                if (DEBUG) System.out.println("radical' " + key + "\t" + alternateStrokes[key]);
            }
            final PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, "imputedStrokes" + (simplified ? "" : "T") +
                    ".txt", null);
            for (final String s : new UnicodeSet(kRSUnicode.keySet()).removeAll(bestStrokesIn.keySet())) {
                int c = s.codePointAt(0);
                int data = getRSShortData(c);
                int radical = RadicalStroke.getRadicalNumberFromShortData(data);
                final int computedStrokes = RadicalStroke.getResidualStrokesFromShortData(data) +
                        (RadicalStroke.isSimplifiedFromShortData(data) ?
                                alternateStrokes[radical] : mainStrokes[radical]);
                bestStrokesIn.put(s, computedStrokes);
                out.println("U+" + Utility.hex(s) + "\tkImputedStrokes\t" + computedStrokes + "\t#\t" + s);
            }
            closeUnderNFKD("Strokes", bestStrokesIn);
            bestStrokesIn.freeze();
            out.close();
        }
    }

    private static int getRSShortData(int c) {
        int data = radicalStroke.getShortData(c);
        if (data != 0) {
            return data;
        }
        if (c < 0x3000) {
            String radical = radicalMap.get(c);
            if (radical == null) {
                return 0;
            }
            c = radical.codePointAt(0);
            assert radical.length() == Character.charCount(c);  // single code point
            data = radicalStroke.getShortData(c);
            assert data != 0;
            return data;
        }
        String decomp = nfd.normalize(c);
        c = decomp.codePointAt(0);
        data = radicalStroke.getShortData(c);
        return data;
    }

    private static long getRSLongOrder(int c) {
        long order = radicalStroke.getLongOrder(c);
        if (order != 0) {
            return order;
        }
        if (c < 0x3000) {
            String radical = radicalMap.get(c);
            if (radical == null) {
                // Not an ideograph, sort higher than any of them.
                return ((long)Integer.MAX_VALUE << 32) | c;
            }
            c = radical.codePointAt(0);
            assert radical.length() == Character.charCount(c);  // single code point
            order = radicalStroke.getLongOrder(c);
            assert order != 0;
            return order;
        }
        String decomp = nfd.normalize(c);
        c = decomp.codePointAt(0);
        order = radicalStroke.getLongOrder(c);
        if (order == 0) {
            // Not an ideograph, sort higher than any of them.
            order = ((long)Integer.MAX_VALUE << 32) | c;
        }
        return order;
    }

    private static <S> void showSorting(Comparator<String> comparator, UnicodeMap<S> unicodeMap, String filename, InfoType infoType) {
        showSorting(comparator, unicodeMap, filename, FileType.txt, infoType);
        showSorting(comparator, unicodeMap, filename, FileType.xml, infoType);
    }

    @SuppressWarnings("resource")
    private static <S> void showSorting(Comparator<String> comparator, UnicodeMap<S> unicodeMap, String filename,
            FileType fileType, InfoType infoType) {
        // special capture for Pinyin buckets
        final boolean isPinyin = filename.startsWith("pinyin") && fileType == FileType.xml;
        int alpha = 'a';
        final StringBuilder pinyinBuffer = new StringBuilder("\"\", // A\n");
        final StringBuilder pinyinIndexBuffer = new StringBuilder("\"\u0101");

        final UnicodeSet accumulated = new UnicodeSet();
        PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + "." + fileType, null);
        final TreeSet<String> rsSorted = new TreeSet<String>(comparator);
        final StringBuilder buffer = new StringBuilder();
        for (final String s : unicodeMap) {
            //            S newValue = unicodeMap.get(s);
            //            if (newValue == null) continue;
            if (UTF16.countCodePoint(s) != 1) {
                throw new IllegalArgumentException("Wrong length!!");
            }
            rsSorted.add(s);
        }
        if (fileType == FileType.txt) {
            out.println(INDENT + "&[last regular]");
        } else {
            //            final String typeAlt = filename.replace("_", "' alt='");
            //            out.print(
            //                    "<?xml version='1.0' encoding='UTF-8' ?>\n"
            //                    +"<!DOCTYPE ldml SYSTEM '.../cldr/common/dtd/ldml.dtd'>\n"
            //                    +"<ldml>\n"
            //                    +"    <identity>\n"
            //                    +"        <version number='$Revision: 1.8 $' />\n"
            //                    +"        <generation date='$Date: 2010/12/14 07:57:17 $' />\n"
            //                    +"        <language type='zh' />\n"
            //                    +"    </identity>\n"
            //                    +"    <collations>\n"
            //                    +"        <collation type='" + typeAlt + "'>\n"
            //            );
            //            FileUtilities.appendFile(GenerateUnihanCollators.class, "pinyinHeader.txt", out);
            out.println("\t\t\t\t<reset><last_non_ignorable /></reset>");
        }
        S oldValue = null;
        String oldIndexValue = null;
        final Output<String> comment = new Output<String>();
        for (final String s : rsSorted) {
            final S newValue = unicodeMap.get(s);
            if (!equals(newValue, oldValue)) {
                if (oldValue == null) {
                    final String indexValue = getIndexValue(infoType, s, comment);
                    showIndexValue(fileType, out, comment, indexValue);
                    oldIndexValue = indexValue;
                } else {
                    // show other characters
                    if (buffer.codePointCount(0, buffer.length()) < 128) {
                        if (fileType == FileType.txt) {
                            out.println(INDENT + "<*" + sortingQuote(buffer.toString(), accumulated) + " # " + sortingQuote(oldValue, accumulated));
                        } else {
                            out.println("               <pc>" + buffer + "</pc><!-- " + oldValue + " -->");
                        }
                    } else {
                        int count = 1;
                        while (buffer.length() > 0) {
                            final String temp = extractFirst(buffer, 128);
                            if (fileType == FileType.txt) {
                                out.println(INDENT + "<*" + sortingQuote(temp.toString(), accumulated) + " # " + sortingQuote(oldValue, accumulated) + " (p" + count++ + ")");
                            } else {
                                out.println("               <pc>" + temp + "</pc><!-- " + oldValue + " (p" + count++ + ") -->");
                            }
                        }
                    }

                    // insert index character
                    final String indexValue = getIndexValue(infoType, s, comment);
                    if (!equals(indexValue, oldIndexValue)) {
                        showIndexValue(fileType, out, comment, indexValue);
                        oldIndexValue = indexValue;
                    }
                }
                buffer.setLength(0);
                oldValue = newValue;
                if (isPinyin) {
                    // OK to just use codepoint order.
                    final String pinyinValue = newValue.toString();
                    final int pinyinFirst = nfd.normalize(pinyinValue).charAt(0);
                    if (alpha < pinyinFirst) {
                        while (alpha < pinyinFirst) {
                            alpha++;
                        }
                        // "\u516B", // B
                        pinyinBuffer.append("\"" + hexConstant(s) + "\", " +
                                "// " + UTF16.valueOf(alpha) + " : " + s + " [" + pinyinValue + "]\n");
                        pinyinIndexBuffer.append(hexConstant(pinyinValue.substring(0,1)));
                    }
                }
            }
            buffer.append(s);
        }

        if (oldValue != null) {
            if (fileType == FileType.txt) {
                out.println(INDENT + "<*" + sortingQuote(buffer.toString(), accumulated) + " # " + sortingQuote(oldValue, accumulated));
            } else {
                out.println("               <pc>" + buffer + "</pc><!-- " + oldValue + " -->");
            }
            buffer.setLength(0);
        }

        final UnicodeSet missing = new UnicodeSet().addAll(buffer.toString());
        if (missing.size() != 0) {
            System.out.println("MISSING" + "\t" + missing.toPattern(false));
        }
        final UnicodeSet tailored = new UnicodeSet(unicodeMap.keySet()).removeAll(missing);

        final TreeSet<String> sorted = new TreeSet<String>(nfkdComparator);
        new UnicodeSet(NOT_NFKD).removeAll(NOT_NFD).removeAll(tailored).addAllTo(sorted);

        for (final String s : sorted) {
            // decomposable, but not tailored
            final String kd = nfkd.normalize(s.codePointAt(0));
            if (!tailored.containsSome(kd))
            {
                continue; // the decomp has to contain at least one tailored
            }
            //            if (tailored.containsAll(kd))
            //                continue; //already have it
            // character
            if (fileType == FileType.txt) {
                out.println(INDENT + "&" + sortingQuote(kd, accumulated) + "<<<" + sortingQuote(s, accumulated));
            } else {
                out.println("               <reset>" + kd + "</reset>");
                out.println("               <t>" + s + "</t>");
            }
        }
        //        if (fileType == FileType.xml) {
        //            out.println(
        //                    "           </rules>\n" +
        //                    "        </collation>\n" +
        //                    "    </collations>\n" +
        //                    "</ldml>"
        //            );
        //        }

        out.close();
        out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + "_repertoire.txt", null);
        out.println(accumulated.toPattern(false));
        out.close();
        if (isPinyin) {
            out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, filename + "_buckets.txt", null);
            pinyinIndexBuffer.append('"');
            out.println(pinyinIndexBuffer);
            out.println(pinyinBuffer);
            out.close();
        }
    }

    private static <T> void showIndexValue(FileType fileType, PrintWriter out, Output<T> comment, String indexValue) {
        if (fileType == FileType.txt) {
            out.println(INDENT + "<'" + hexConstant(indexValue) + "' # INDEX " + comment);
        } else {
            out.println("               <p>" + indexValue + "</p><!-- INDEX " + comment + " -->");
        }
    }

    /**
     * Hex format by code unit.
     * @param s
     * @return
     */
    private static CharSequence hexConstant(String s) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            final char ch = s.charAt(i);
            if (0x20 <= ch && ch < 0x80) {
                result.append(ch);
            } else {
                result.append("\\u").append(Utility.hex(ch, 4));
            }
        }
        return result;
    }

    private static String extractFirst(StringBuilder buffer, int i) {
        String result;
        try {
            final int charEnd = buffer.offsetByCodePoints(0, i);
            result = buffer.substring(0, charEnd);
            buffer.delete(0, charEnd);
        } catch (final Exception e) {
            result = buffer.toString();
            buffer.setLength(0);
        }
        return result;
    }

    private static <T> String sortingQuote(T input, UnicodeSet accumulated) {
        String s = input.toString();
        accumulated.addAll(s);
        s = s.replace("'", "''");
        if (NEEDSQUOTE.containsSome(s)) {
            s = '\'' + s + '\'';
        }
        return s;
    }

    private static boolean equals(Object newValue, Object oldValue) {
        return newValue == null ? oldValue == null
                : oldValue == null ? false
                        : newValue.equals(oldValue);
    }

    private static int showAdded(String title, int difference) {
        difference = mergedPinyin.size() - difference;
        System.out.println("added " + title + " " + difference);
        return difference;
    }

    private static void addBihua() {
        for (final String s : bihuaData.keySet()) {
            final R2<String, String> bihuaRow = bihuaData.get(s);
            final String value = bihuaRow.get0();
            addPinyin("bihua", s, value, OverrideItems.keepOld);
        }
    }

    private static void printExtraPinyinForUnihan() {
        PrintWriter out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, "kMandarinAdditions.txt", null);

        out.println("#Code\tkMandarin\tValue\t#\tChar");
        for (final String s : new UnicodeSet(bestPinyin.keySet()).removeAll(originalPinyin)) {
            if (s.codePointAt(0) < 0x3400) {
                continue;
            }
            final String value = bestPinyin.get(s);
            final String oldValue = toNumericPinyin.transform(value).toUpperCase();
            out.println("U+" + Utility.hex(s) + "\tkMandarin\t" + oldValue + "\t#\t" + s);
        }
        out.close();

        out = Utility.openPrintWriter(GenerateUnihanCollatorFiles.OUTPUT_DIRECTORY, "kPinyinOverride.txt", null);
        out.println("#Code\t'Best'\tValue\t#\tChar\tUnihan");
        for (final String s : new UnicodeSet(bestPinyin.keySet()).retainAll(originalPinyin).retainAll(bihuaData.keySet())) {
            if (s.codePointAt(0) < 0x3400) {
                continue;
            }
            final String value = bestPinyin.get(s);
            final R2<String, String> datum = bihuaData.get(s);
            final String bihua = datum.get0();
            if (value.equals(bihua)) {
                continue;
            }
            out.println("U+" + Utility.hex(s) + "\tkPinyinOverride\t" + value + "\t#\t" + s + "\t" + bihua);
        }
        out.close();

    }

    private static int addPinyinFromVariants(String title, int count) {
        for (final Set<Integer> s : variantEquivalents.getEquivalenceSets()) {
            String hasPinyin = null;
            int countHasPinyin = 0;
            for (final Integer cp : s) {
                final String existing = bestPinyin.get(cp);
                if (existing != null) {
                    hasPinyin = existing; // take last one. Might be better algorithm, but for now...
                    countHasPinyin++;
                }
            }
            // see if at least one has a pinyin, and at least one doesn't.
            if (countHasPinyin != s.size() && hasPinyin != null) {
                for (final Integer cp : s) {
                    if (!bestPinyin.containsKey(cp)) {
                        addPinyin(title, UTF16.valueOf(cp), hasPinyin, OverrideItems.keepOld);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void addEquivalents(UnicodeMap<String> variantMap) {
        for (final String s : variantMap) {
            final String value = variantMap.get(s);
            if (value == null) {
                continue;
            }
            final int baseCp = s.codePointAt(0);
            for (final String part : ONBAR.split(value)) {
                if (!unicodeCp.reset(part).matches()) {
                    throw new IllegalArgumentException();
                } else {
                    final int cp = Integer.parseInt(unicodeCp.group(1), 16);
                    variantEquivalents.add(baseCp, cp);
                }
            }
        }
    }


    private static void addRadicals() {
        for (final String s : radicalMap.keySet()) {
            final String main = radicalMap.get(s);
            final Set<String> newValues = mergedPinyin.get(main);
            if (newValues == null) {
                continue;
            }
            for (final String newValue : newValues) {
                addPinyin("radicals", s, newValue, OverrideItems.keepOld);
            }
        }
    }

    private static void addRadicals(UnicodeMap<String> source) {
        for (final String s : radicalMap.keySet()) {
            final String main = radicalMap.get(s);
            final String newValue = source.get(main);
            if (newValue == null) {
                continue;
            }
            source.put(s, newValue);
        }
    }

    private static boolean validPinyin(String pinyin) {
        final Boolean cacheResult = validPinyin.get(pinyin);
        if (cacheResult != null) {
            return cacheResult;
        }
        boolean result;
        final String base = noaccents.transform(pinyin);
        final int initialEnd = INITIALS.findIn(base, 0, true);
        if (initialEnd == 0 && (pinyin.startsWith("i") || pinyin.startsWith("u"))) {
            result = false;
        } else {
            final String finalSegment = base.substring(initialEnd);
            result = finalSegment.length() == 0 ? true : FINALS.contains(finalSegment);
        }
        validPinyin.put(pinyin, result);
        return result;
    }

    private static void addAllKeepingOld(String han, String original, PinyinSource pinyin, Iterable<String> pinyinList) {
        int count = 0;
        for (final String source : pinyinList) {
            if (source.length() == 0) {
                throw new IllegalArgumentException();
            }
            addPinyin(pinyin.toString(), han, source, OverrideItems.keepOld);
            ++count;
        }
        if (count == 0) {
            throw new IllegalArgumentException();
        }
    }

    private static void addPinyin(String title, String han, String source, OverrideItems override) {
        if (!validPinyin(source)) {
            System.out.println("***Invalid Pinyin - " + title + ": " + han + "\t" + source + "\t" + Utility.hex(han));
            return;
        }
        source = source.intern();
        final String item = bestPinyin.get(han);
        if (item == null || override == OverrideItems.keepNew) {
            if (!source.equals(item)) {
                if (item != null) {
                    System.out.println("Overriding Pinyin " + han + "\told: " + item + "\tnew: " + source);
                }
                bestPinyin.put(han, source);
            }
        }
        Set<String> set = mergedPinyin.get(han);
        if (set == null) {
            mergedPinyin.put(han, set = new LinkedHashSet<String>());
        }
        set.add(source);
    }

    private static final class MyFileReader extends SemiFileReader {
        public final Pattern SPLIT = Pattern.compile("\\s*,\\s*");
        String               last  = "";

        @Override
        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        @Override
        protected boolean isCodePoint() {
            return false;
        };

        /**
         * <pre>;Radical Number,Status,Unified_Ideo,Hex,Radical,Hex,Name,Conf.Char,Hex,Unified Ideo. has NORemainingStrokes in Unihan
         * <br>1,Main,一,U+4E00,⼀,U+2F00,ONE
         * </pre>
         */
        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (items[0].startsWith(";")) {
                return true;
            }
            if (items[2].length() != 0) {
                last = items[2];
            }

            final String radical = items[4];
            if (NOT_NFC.contains(radical)) {
                return true;
            }
            radicalMap.put(radical, last);
            return true;
        }
    };

    // 吖 ; a ; 1 ; 251432 ; 0x5416
    private static final class BihuaReader extends SemiFileReader {
        @Override
        protected boolean isCodePoint() {
            return false;
        };

        Set<String> seen = new HashSet<String>();

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            final String character = items[0];
            String pinyinBase = items[1];
            final String other = pinyinBase.replace("v", "ü");
            if (!other.equals(pinyinBase)) {
                if (!seen.contains(other)) {
                    System.out.println("bihua: " + pinyinBase + " => " + other);
                    seen.add(other);
                }
                pinyinBase = other;
            }
            final String pinyinTone = items[2];
            final String charSequence = items[3];
            final String hex = items[4];
            if (!hex.startsWith("0x")) {
                throw new RuntimeException(hex);
            } else {
                final int hexValue = Integer.parseInt(hex.substring(2), 16);
                if (!character.equals(UTF16.valueOf(hexValue))) {
                    throw new RuntimeException(hex + "!=" + character);
                }
            }
            final String source = fromNumericPinyin.transform(pinyinBase + pinyinTone);
            bihuaData.put(character, Row.of(source, charSequence));
            return true;
        }
    };

    private static final class PatchPinyinReader extends SemiFileReader {
        boolean skip = false;

        @Override
        protected boolean isCodePoint() {
            return false;
        };

        @Override
        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (skip) {
                return true;
            }
            if (items.length > 1) {
                if (!UNIHAN.contains(items[0])) {
                    throw new IllegalArgumentException("Non-Unihan character: " + items[0]);
                }
                if (!PINYIN_LETTERS.containsAll(items[1])) {
                    throw new IllegalArgumentException("Non-Pinyin character: " + items[1]
                            + "; " + new UnicodeSet().addAll(items[1]).removeAll(PINYIN_LETTERS));
                }
                addPinyin("patchPinyin", items[0], items[1], OverrideItems.keepNew);
            }
            return true;
        }
    }

    private static final class PatchStrokeReader extends SemiFileReader {
        final UnicodeMap<Integer> target;
        boolean skip = false;
        private Pattern splitter;

        PatchStrokeReader(UnicodeMap<Integer> target, Pattern splitter) {
            this.target = target;
            this.splitter = splitter;
        }

        protected String[] splitLine(String line) {
            return splitter.split(line);
        }

        @Override
        protected boolean isCodePoint() {
            return false;
        };

        @Override
        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            if (skip) {
                return true;
            }
            String codepoint = items[0];
            if (codepoint.startsWith("U+")) {
                codepoint = UTF16.valueOf(Integer.parseInt(codepoint.substring(2), 16));
            }
            if (!UNIHAN.contains(codepoint)) {
                throw new IllegalArgumentException("Non-Unihan character: " + codepoint + ", " + Utility.hex(codepoint));
            }
            if (items.length > 1) {
                String strokeCount = items[1];
                int comma = strokeCount.indexOf(',');
                if (comma >= 0) {
                    strokeCount = strokeCount.substring(0,comma);
                }
                target.put(codepoint, Integer.parseInt(strokeCount));
            }
            return true;
        }
    }

    private static Comparator<String> RSComparator     = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            int c1 = s1.codePointAt(0);
            assert Character.charCount(c1) == s1.length();
            int c2 = s2.codePointAt(0);
            assert Character.charCount(c2) == s2.length();
            long order1 = getRSLongOrder(c1);
            long order2 = getRSLongOrder(c2);
            return order1 < order2 ? -1 : order1 > order2 ? 1 : 0;
        }
    };

    private static class StrokeComparator implements Comparator<String> {
        final UnicodeMap<Integer> baseMap;
        public StrokeComparator(UnicodeMap<Integer> baseMap) {
            this.baseMap = baseMap;
        }
        @Override
        public int compare(String o1, String o2) {
            final Integer n1 = getStrokeValue(o1, baseMap);
            final Integer n2 = getStrokeValue(o2, baseMap);
            if (n1 == null) {
                if (n2 != null) {
                    return 1;
                }
                // both null, fall through
            } else if (n2 == null) {
                return -1;
            } else { // both not null
                final int result = n1 - n2;
                if (result != 0) {
                    return result;
                }
            }
            return RSComparator.compare(o1, o2);
        }
    }

    private static Integer getStrokeValue(String o1, UnicodeMap<Integer> baseMap) {
        final int cp1 = o1.codePointAt(0);
        return baseMap.get(cp1);
    }

    private static Comparator<String> SStrokeComparator = new StrokeComparator(bestStrokesS);
    private static Comparator<String> TStrokeComparator = new StrokeComparator(bestStrokesT);

    private static Comparator<String> PinyinComparator = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            final String s1 = getPinyin(o1);
            final String s2 = getPinyin(o2);
            if (s1 == null) {
                if (s2 != null) {
                    return 1;
                }
            } else if (s2 == null) {
                return -1;
            }
            final int result = pinyinSort.compare(s1, s2);
            if (result != 0) {
                return result;
            }
            return SStrokeComparator.compare(o1, o2);
        }
    };

    public static String getPinyin(String o1) {
        final int cp1 = o1.codePointAt(0);
        return bestPinyin.get(cp1);
    }

    private static final Relation<InfoType,String> indexValues = Relation.of(new EnumMap<InfoType,Set<String>>(InfoType.class), HashSet.class);

    private static String getIndexValue(InfoType infoType, String s, Output<String> comment) {
        String rest;
        switch (infoType) {
        case pinyin:
            final String str = getPinyin(s).toUpperCase(Locale.ENGLISH); // TODO drop accents
            final int first = str.charAt(0);
            if (first < 0x7F) {
                rest = str.substring(0,1);
            } else {
                rest = nfd.normalize(first).substring(0,1);
            }
            comment.value = rest;
            break;
        case radicalStroke:
            final int codepoint = s.codePointAt(0);
            int data = getRSShortData(codepoint);
            if (data == 0) {
                throw new IllegalArgumentException("Missing R-S data for U+" + Utility.hex(codepoint));
            }
            rest = radicalStroke.getRadicalCharFromShortData(data);
            comment.value = radicalStroke.getRadicalStringFromShortData(data);
            break;
        case stroke:
            final Integer strokeCount = getStrokeValue(s, bestStrokesT);
            rest = String.valueOf((char)(INDEX_ITEM_BASE + strokeCount));
            comment.value = String.valueOf(strokeCount);
            break;
        default:
            throw new IllegalArgumentException();
        }
        final String result = infoType.base + rest;
        indexValues.put(infoType, result);
        return result;
    }
}
