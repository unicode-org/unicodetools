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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.draft.ComparePinyin.PinyinSource;
import org.unicode.jsp.FileUtilities.SemiFileReader;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Differ;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateUnihanCollators {
    enum FileType {txt, xml}
    enum InfoType {radicalStroke, stroke, pinyin}
    enum Override {keepOld, keepNew}

    static final String OUTPUT_DIRECTORY = "/Users/markdavis/Documents/workspace/cldr-tmp/dropbox/han";

    static final Transform<String, String>    fromNumericPinyin   = Transliterator.getInstance("NumericPinyin-Latin;nfc");
    static final Transliterator               toNumericPinyin     = Transliterator.getInstance("Latin-NumericPinyin;nfc");

    static final Normalizer2                  nfkd                = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
    static final Normalizer2                  nfd                 = Normalizer2.getInstance(null, "nfc", Mode.DECOMPOSE);
    static final Normalizer2                  nfc                 = Normalizer2.getInstance(null, "nfc", Mode.COMPOSE);

    static final UnicodeSet                   PINYIN_LETTERS      = new UnicodeSet("['a-uw-zàáèéìíòóùúüāēěīōūǎǐǒǔǖǘǚǜ]").freeze();
    static final UnicodeSet                   NOT_NFC             = new UnicodeSet("[:nfc_qc=no:]").freeze();
    static final UnicodeSet                   NOT_NFD             = new UnicodeSet("[:nfd_qc=no:]").freeze();
    static final UnicodeSet                   NOT_NFKD             = new UnicodeSet("[:nfkd_qc=no:]").freeze();
    static final UnicodeSet                   UNIHAN              = new UnicodeSet("[[:ideographic:][:script=han:]]").removeAll(NOT_NFC).freeze();

    static Matcher                            unicodeCp           = Pattern.compile("^U\\+(2?[0-9A-F]{4})$").matcher("");
    static final HashMap<String, Boolean>     validPinyin         = new HashMap<String, Boolean>();
    static final Collator                     pinyinSort          = Collator.getInstance(new ULocale("zh@collator=pinyin"));
    static final Collator                     strokeSort          = Collator.getInstance(new ULocale("zh@collator=stroke"));
    static final Collator                     radicalStrokeSort   = Collator.getInstance(new ULocale("zh@collator=unihan"));
    static final Transliterator               toPinyin            = Transliterator.getInstance("Han-Latin;nfc");
    
    static final Comparator<String>           codepointComparator = new UTF16.StringComparator(true, false, 0);
    static final Comparator<String>           nfkdComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            if (!nfkd.isNormalized(o1) || !nfkd.isNormalized(o2)) {
                String s1 = nfkd.normalize(o1);
                String s2 = nfkd.normalize(o2);
                int result = codepointComparator.compare(s1, s2);
                if (result != 0) {
                    return result; // otherwise fall through to codepoint comparator
                }
            }
            return codepointComparator.compare(o1, o2);
        }
    };

    static final UnicodeMap<String>           kTotalStrokes       = Default.ucd().getHanValue("kTotalStrokes");
    static final UnicodeMap<Integer>          bestStrokesS         = Default.ucd().getHanValue("kTotalStrokes");
    static final UnicodeMap<Integer>          bestStrokesT         = new UnicodeMap<Integer>();

    static UnicodeMap<Row.R2<String, String>> bihuaData           = new UnicodeMap<Row.R2<String, String>>();

    static final UnicodeMap<String>           kRSUnicode          = Default.ucd().getHanValue("kRSUnicode");
    static final UnicodeMap<String>           kSimplifiedVariant  = Default.ucd().getHanValue("kSimplifiedVariant");
    static final UnicodeMap<String>           kTraditionalVariant = Default.ucd().getHanValue("kTraditionalVariant");

    static final UnicodeMap<String>           kBestStrokes        = new UnicodeMap<String>();
    static final UnicodeMap<Set<String>>      mergedPinyin        = new UnicodeMap<Set<String>>();
    static final UnicodeSet                   originalPinyin;

    static final UnicodeMap<RsInfo>           RS_INFO_CACHE       = new UnicodeMap();
    static final Matcher                      RSUnicodeMatcher    = Pattern.compile("^([1-9][0-9]{0,2})('?)\\.([0-9]{1,2})$")
    .matcher("");

    private static final boolean only19 = System.getProperty("only19") != null;
    static UnicodeMap<String>                 radicalMap          = new UnicodeMap<String>();
    
    // kHanyuPinyin, space, 10297.260: qīn,qìn,qǐn,
    // [a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]+(,[a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]
    // kMandarin, space, [A-Z\x{308}]+[1-5] // 3475=HAN4 JI2 JIE2 ZHA3 ZI2
    // kHanyuPinlu, space, [a-z\x{308}]+[1-5]\([0-9]+\) 4E0A=shang4(12308)
    // shang5(392)
    static UnicodeMap<String>        bestPinyin = new UnicodeMap();
    static Transform<String, String> noaccents  = Transliterator.getInstance("nfkd; [[:m:]-[\u0308]] remove; nfc");
    static Transform<String, String> accents    = Transliterator.getInstance("nfkd; [^[:m:]-[\u0308]] remove; nfc");

    static UnicodeSet                INITIALS   = new UnicodeSet("[b c {ch} d f g h j k l m n p q r s {sh} t w x y z {zh}]").freeze();
    static UnicodeSet                FINALS     = new UnicodeSet(
    "[a {ai} {an} {ang} {ao} e {ei} {en} {eng} {er} i {ia} {ian} {iang} {iao} {ie} {in} {ing} {iong} {iu} o {ong} {ou} u {ua} {uai} {uan} {uang} {ue} {ui} {un} {uo} ü {üe}]")
    .freeze();
    static final int NO_STROKE_INFO = Integer.MAX_VALUE;
    private static UnicodeSet NEEDSQUOTE = new UnicodeSet("[[:pattern_syntax:][:pattern_whitespace:]]").freeze();

    static {
        System.out.println("kRSUnicode " + kRSUnicode.size());

        new BihuaReader().process(GenerateUnihanCollators.class, "bihua-chinese-sorting.txt");
        getBestStrokes();
        RsInfo.addToStrokeInfo(bestStrokesS, true);
        
        bestStrokesT.putAll(bestStrokesS);
        // patch the values for the T strokes
        new PatchStrokeReader(bestStrokesT).process(GenerateUnihanCollators.class, "patchStrokeT.txt");
        RsInfo.addToStrokeInfo(bestStrokesT, false);

        new MyFileReader().process(GenerateUnihanCollators.class, "CJK_Radicals.csv");
        if (false)
            for (String s : radicalMap.values()) {
                System.out.println(s + "\t" + radicalMap.getSet(s).toPattern(false));
            }
        UnicodeMap added = new UnicodeMap();
        addRadicals(kRSUnicode);
        closeUnderNFKD("Unihan", kRSUnicode);

        // all kHanyuPinlu readings first; then take all kXHC1983; then
        // kHanyuPinyin.

        UnicodeMap<String> kHanyuPinlu = Default.ucd().getHanValue("kHanyuPinlu");
        for (String s : kHanyuPinlu.keySet()) {
            String original = kHanyuPinlu.get(s);
            String source = original.replaceAll("\\([0-9]+\\)", "");
            source = fromNumericPinyin.transform(source);
            addAll(s, original, PinyinSource.l, source.split(" "));
        }

        // kXHC1983
        // ^[0-9,.*]+:*[a-zx{FC}x{300}x{301}x{304}x{308}x{30C}]+$
        UnicodeMap<String> kXHC1983 = Default.ucd().getHanValue("kXHC1983");
        for (String s : kXHC1983.keySet()) {
            String original = kXHC1983.get(s);
            String source = Normalizer.normalize(original, Normalizer.NFC);
            source = source.replaceAll("([0-9,.*]+:)*", "");
            addAll(s, original, PinyinSource.x, source.split(" "));
        }

        UnicodeMap<String> kHanyuPinyin = Default.ucd().getHanValue("kHanyuPinyin");
        for (String s : kHanyuPinyin.keySet()) {
            String original = kHanyuPinyin.get(s);
            String source = Normalizer.normalize(original, Normalizer.NFC);
            source = source.replaceAll("^\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ""); // ,
            // only
            // for
            // medial
            source = source.replaceAll("\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ",");
            addAll(s, original, PinyinSource.p, source.split(","));
        }

        UnicodeMap<String> kMandarin = Default.ucd().getHanValue("kMandarin");
        for (String s : kMandarin.keySet()) {
            String original = kMandarin.get(s);
            String source = original.toLowerCase();
            source = fromNumericPinyin.transform(source);
            addAll(s, original, PinyinSource.m, source.split(" "));
        }

        originalPinyin = mergedPinyin.keySet().freeze();
        int count = mergedPinyin.size();
        System.out.println("unihanPinyin original size " + count);

        addBihua();
        count += showAdded("bihua", count);

        addRadicals();
        count += showAdded("radicals", count);

        addVariants("kTraditionalVariant", kTraditionalVariant);
        count += showAdded("kTraditionalVariant", count);

        addVariants("kSimplifiedVariant", kSimplifiedVariant);
        count += showAdded("kSimplifiedVariant", count);

        new PatchPinyinReader().process(GenerateUnihanCollators.class, "patchPinyin.txt");

        closeUnderNFKD("Pinyin", bestPinyin);

        printExtraPinyinForUnihan();
    }


    public static void main(String[] args) throws Exception {
        showOldData(pinyinSort, "pinyin1.8.txt", false);
        showOldData(strokeSort, "stroke1.8.txt", false);
        showOldData(Collator.getInstance(new ULocale("ja")), "japanese1.8.txt", true);

        UnicodeSet zh = FindHanSizes.getMostFrequent("zh", 0.999);
        UnicodeSet zh_Hant = FindHanSizes.getMostFrequent("zh_Hant", 0.999);

        //Matcher charsetMatcher = Pattern.compile("GB2312|GBK|Big5|Big5-HKSCS").matcher("");
        UnicodeSet GB2312 = FindHanSizes.getCharsetRepertoire("GB2312");
        UnicodeSet GBK = FindHanSizes.getCharsetRepertoire("GBK");
        UnicodeSet Big5 = FindHanSizes.getCharsetRepertoire("Big5");
        UnicodeSet Big5_HKSCS = FindHanSizes.getCharsetRepertoire("Big5-HKSCS");

        UnicodeSet shortPinyin = new UnicodeSet(zh).addAll(GB2312).addAll(GBK);
        UnicodeSet shortStroke = new UnicodeSet(shortPinyin).addAll(zh_Hant).addAll(Big5).addAll(Big5_HKSCS);


        showSorting(RSComparator, kRSUnicode, "unihan", InfoType.radicalStroke);
        testSorting(RSComparator, kRSUnicode, "unihan");

        writeAndTest(shortPinyin, PinyinComparator, bestPinyin, "pinyin", InfoType.pinyin);
        writeAndTest(shortStroke, SStrokeComparator, bestStrokesS, "stroke", InfoType.stroke);
        writeAndTest(shortStroke, TStrokeComparator, bestStrokesT, "strokeT", InfoType.stroke);

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

    private static <T> void writeAndTest(UnicodeSet shortStroke, Comparator<String> comparator2, UnicodeMap<T> unicodeMap2, String title2, InfoType infoType) throws Exception {
        showSorting(comparator2, unicodeMap2, title2, infoType);
        testSorting(comparator2, unicodeMap2, title2);
        UnicodeMap<T> shortMap = new UnicodeMap<T>().putAllFiltered(unicodeMap2, shortStroke);
        System.out.println(title2 + " base size:\t" + shortMap.size());
        showSorting(comparator2, shortMap, title2 + "_short", infoType);
        testSorting(comparator2, shortMap, title2 + "_short");
    }

    private static void showOldData(Collator collator, String name, boolean japanese) {
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, name, null);
        UnicodeSet tailored = collator.getTailoredSet();
        TreeSet<String> sorted = new TreeSet<String>(collator);
        for (String s : tailored) {
            sorted.add(nfc.normalize(s));
        }
        final UnicodeMap<String>          kJapaneseKun         = Default.ucd().getHanValue("kJapaneseKun");
        final UnicodeMap<String>          kJapaneseOn         = Default.ucd().getHanValue("kJapaneseOn");

        StringBuilder buffer = new StringBuilder();
        out.println("#char; strokes; radical; rem-strokes; reading");
        for (String item : sorted) {
            buffer.append("<").append(item).append("\t#");
            String code = Utility.hex(item);
            buffer.append(pad(code,6)).append(";\t");

            String strokes = kTotalStrokes.get(item);
            buffer.append(pad(strokes,3)).append(";\t");

            RsInfo rsInfo = RsInfo.from(item.codePointAt(0));
            String radical = null;
            String remainingStrokes = null;
            if (rsInfo != null) {
                radical = rsInfo.radical + (rsInfo.alternate == 0 ? "" : "'");
                remainingStrokes = rsInfo.remainingStrokes + "";
            }
            buffer.append(pad(radical,4)).append(";\t");
            buffer.append(pad(remainingStrokes,2)).append(";\t");

            if (japanese) {
                String reading = kJapaneseKun.get(item);
                String reading2 = kJapaneseOn.get(item);
                buffer.append(pad(reading,1)).append(";\t");
                buffer.append(pad(reading2,1)).append(";\t");
            } else {
                Set<String> pinyins = mergedPinyin.get(item);
                if (pinyins != null) {
                    boolean first = true;
                    for (String pinyin : pinyins) {
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
        UnicodeSet tailored = pinyinSort.getTailoredSet();
        TreeMap<String, String> sorted = new TreeMap<String, String>(pinyinSort);
        Counter<String> counter = new Counter();
        for (String s : tailored) {
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
        Counter<String> progressive = new Counter();

        for (String s : sorted.keySet()) {
            String pinyin = sorted.get(s);
            progressive.add(pinyin, 1);
            if (pinyin.equals(lastPinyin)) {
                count++;
            } else {
                System.out.println("\t" + count + "\t"
                        + (progressive.get(lastPinyin) / (double) counter.get(lastPinyin)));
                count = 1;
                lastPinyin = pinyin;
                System.out.print(s + "\t" + pinyin + "\t");
            }
        }
        System.out.println("\t" + count + "\t"
                + (progressive.get(lastPinyin) / (double) counter.get(lastPinyin)));
    }

    private static void getBestStrokes() {
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, "kTotalStrokesReplacements.txt", null);

        out.println("#Code\tkTotalStrokes\tValue\t#\tChar\tUnihan");

        for (String s : new UnicodeSet(bihuaData.keySet()).addAll(kTotalStrokes.keySet())) {
            String unihanStrokesString = kTotalStrokes.get(s);
            int unihanStrokes = unihanStrokesString == null ? NO_STROKE_INFO : Integer.parseInt(unihanStrokesString);
            R2<String, String> bihua = bihuaData.get(s);
            int bihuaStrokes = bihua == null ? NO_STROKE_INFO : bihua.get1().length();
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

        new PatchStrokeReader(bestStrokesS).process(GenerateUnihanCollators.class, "patchStroke.txt");
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
        UnicodeSet extras = new UnicodeSet(NOT_NFKD).retainAll(mapping.keySet());
        if (extras.size() != 0) {
            System.out.println("*** " + title + " Removing " + extras.toPattern(false));
            mapping.putAll(extras, null);
        }
        mapping.freeze();
    }

    private static void showBackgroundData() throws IOException {
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, "backgroundCjkData.txt", null);
        UnicodeSet all = new UnicodeSet(bihuaData.keySet()); // .addAll(allPinyin.keySet()).addAll(kRSUnicode.keySet());
        Comparator<Row.R4<String, String, Integer, String>> comparator = new Comparator<Row.R4<String, String, Integer, String>>() {
            public int compare(R4<String, String, Integer, String> o1, R4<String, String, Integer, String> o2) {
                int result = o1.get0().compareTo(o2.get0());
                if (result != 0)
                    return result;
                result = pinyinSort.compare(o1.get1(), o2.get1());
                if (result != 0)
                    return result;
                result = o1.get2().compareTo(o2.get2());
                if (result != 0)
                    return result;
                result = o1.get3().compareTo(o2.get3());
                return result;
            }

        };
        Set<Row.R4<String, String, Integer, String>> items = new TreeSet<Row.R4<String, String, Integer, String>>(comparator);
        for (String s : all) {
            R2<String, String> bihua = bihuaData.get(s);
            int bihuaStrokes = bihua == null ? 0 : bihua.get1().length();
            String bihuaPinyin = bihua == null ? "?" : bihua.get0();
            Set<String> allPinyins = mergedPinyin.get(s);
            String firstPinyin = allPinyins == null ? "?" : allPinyins.iterator().next();
            String rs = kRSUnicode.get(s);
            String totalStrokesString = kTotalStrokes.get(s);
            int totalStrokes = totalStrokesString == null ? 0 : Integer.parseInt(totalStrokesString);
            // for (String rsItem : rs.split(" ")) {
            // RsInfo rsInfo = RsInfo.from(rsItem);
            // int totalStrokes = rsInfo.totalStrokes;
            // totals.set(totalStrokes);
            // if (firstTotal != -1) firstTotal = totalStrokes;
            // int radicalsStokes = bihuaStrokes - rsInfo.remainingStrokes;
            // counter.add(Row.of(rsInfo.radical + (rsInfo.alternate == 1 ? "'"
            // : ""), radicalsStokes), 1);
            // }
            String status = (bihuaPinyin.equals(firstPinyin) ? "-" : "P") + (bihuaStrokes == totalStrokes ? "-" : "S");
            items.add(Row.of(status, firstPinyin, totalStrokes,
                    status + "\t" + s + "\t" + rs + "\t" + totalStrokes + "\t" + bihuaStrokes + "\t" + bihua + "\t" + mergedPinyin.get(s)));
        }
        for (R4<String, String, Integer, String> item : items) {
            out.println(item.get3());
        }
        out.close();
    }

    private static <S> void testSorting(Comparator<String> oldComparator, UnicodeMap<S> krsunicode2, String filename) throws Exception {
        List<String> temp = krsunicode2.keySet().addAllTo(new ArrayList<String>());
        String rules = getFileAsString(UCD_Types.GEN_DIR + File.separatorChar + "han" + File.separatorChar + filename + ".txt");

        Comparator collator = new MultiComparator(new RuleBasedCollator(rules), codepointComparator);
        List<String> ruleSorted = sortList(collator, temp);

        Comparator oldCollator = new MultiComparator(oldComparator, codepointComparator);
        List<String> originalSorted = sortList(oldCollator, temp);
        int badItems = 0;
        int min = Math.min(originalSorted.size(), ruleSorted.size());
        Differ<String> differ = new Differ(100, 2);
        for (int k = 0; k < min; ++k) {
            String ruleItem = ruleSorted.get(k);
            String originalItem = originalSorted.get(k);
            if (ruleItem == null || originalItem == null) {
                throw new IllegalArgumentException();
            }
            differ.add(originalItem, ruleItem);

            differ.checkMatch(k == min - 1);

            int aCount = differ.getACount();
            int bCount = differ.getBCount();
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
        String item = differ.getA(i);
        try {
            return "unihan: " + differ.getALine(i) + " " + item + " [" + Utility.hex(item) + "/" + krsunicode2.get(item) + "]";
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static <S> String bline(UnicodeMap<S> krsunicode2, Differ<String> differ, int i) {
        String item = differ.getB(i);
        return "rules: " + differ.getBLine(i) + " " + item + " [" + Utility.hex(item) + "/" + krsunicode2.get(item) + "]";
    }

    private static List<String> sortList(Comparator collator, List<String> temp) {
        String[] ruleSorted = temp.toArray(new String[temp.size()]);
        Arrays.sort(ruleSorted, collator);
        return Arrays.asList(ruleSorted);
    }

    private static String getFileAsString(Class<GenerateUnihanCollators> relativeToClass, String filename) throws IOException {
        BufferedReader in = FileUtilities.openFile(relativeToClass, filename);
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = in.readLine();
            if (line == null)
                break;
            builder.append(line).append('\n');
        }
        in.close();
        return builder.toString();
    }

    private static String getFileAsString(String filename) throws IOException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), FileUtilities.UTF8);
        BufferedReader in = new BufferedReader(reader, 1024 * 64);
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = in.readLine();
            if (line == null)
                break;
            builder.append(line).append('\n');
        }
        in.close();
        return builder.toString();
    }

    private static void showTranslit(String filename) {
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, filename + ".txt", null);
        PrintWriter out2 = Utility.openPrintWriter(OUTPUT_DIRECTORY, filename + ".xml", null);
        TreeSet<String> s = new TreeSet(pinyinSort);
        s.addAll(bestPinyin.getAvailableValues());

        out2.println("<?xml version='1.0' encoding='UTF-8' ?>\n"
                +"<!DOCTYPE supplementalData SYSTEM '../../common/dtd/ldmlSupplemental.dtd'>\n"
                +"<supplementalData>\n"
                +"  <version number='$Revision: 1.8 $'/>\n"
                +"  <generation date='$Date: 2010/12/14 07:57:17 $'/>\n"
                +"  <transforms>\n"
                +"      <transform source='Han' target='Latin' direction='both'>\n"
                +"          <comment># Warning: does not do round-trip mapping!!</comment>\n"
                +"          <comment># Convert CJK characters</comment>\n"
                +"          <tRule>::Han-Spacedhan();</tRule>\n"
                +"          <comment># Start RAW data for converting CJK characters</comment>");

        for (String value : s) {
            UnicodeSet uset = bestPinyin.getSet(value);
            // <tRule>[专叀塼嫥専專瑼甎砖磚篿耑膞蟤跧鄟顓颛鱄鷒]→zhuān;</tRule>
            out.println(uset.toPattern(false) + "→" + value + ";");
            out2.println("           <tRule>" + uset.toPattern(false) + "→" + value + ";</tRule>");
        }

        out2.println("          <comment># End RAW data for converting CJK characters</comment>\n"
                +"          <comment># fallbacks</comment>\n"
                +"          <comment>## | yi ← i;</comment>\n"
                +"          <comment>## | wu ← u;</comment>\n"
                +"          <comment>## | bi ← b;</comment>\n"
                +"          <comment>## | ci ← c;</comment>\n"
                +"          <comment>## | di ← d;</comment>\n"
                +"          <comment>## | fu ← f;</comment>\n"
                +"          <comment>## | gu ← g;</comment>\n"
                +"          <comment>## | he ← h;</comment>\n"
                +"          <comment>## | ji ← j;</comment>\n"
                +"          <comment>## | ku ← k;</comment>\n"
                +"          <comment>## | li ← l;</comment>\n"
                +"          <comment>## | mi ← m;</comment>\n"
                +"          <comment>## | pi ← p;</comment>\n"
                +"          <comment>## | qi ← q;</comment>\n"
                +"          <comment>## | l ← r;</comment>\n"
                +"          <comment>## | si ← s;</comment>\n"
                +"          <comment>## | ti ← t;</comment>\n"
                +"          <comment>## | f ← v;</comment>\n"
                +"          <comment>## | wa ← w;</comment>\n"
                +"          <comment>## | xi ← x;</comment>\n"
                +"          <comment>## | yi ← y;</comment>\n"
                +"          <comment>## | zi ← z;</comment>\n"
                +"          <comment># filter out the half-width hangul</comment>\n"
                +"          <comment># :: [^ﾾ-￮] fullwidth-halfwidth ();</comment>\n"
                +"          <comment>## :: (lower) ;</comment>\n"
                +"      </transform>\n"
                +"  </transforms>\n"
                +"</supplementalData>");
        out.close();
        out2.close();
    }

    public static int getSortOrder(int codepoint) {
        RsInfo rsInfo = RsInfo.from(codepoint);
        if (rsInfo == null) {
            return Integer.MAX_VALUE / 2;
        }
        return rsInfo.getRsOrder();
    }

    public static class RsInfo {
        private int radical;
        private int alternate;
        private int remainingStrokes;

        private RsInfo() {
        }

        private static RsInfo from(int codepoint) {
            String radicalStrokeStrings = kRSUnicode.get(codepoint);
            if (radicalStrokeStrings == null) {
                return null;
            }
            String radicalStrokeString = radicalStrokeStrings.contains(" ") ? radicalStrokeStrings.split(" ")[0] : radicalStrokeStrings;
            RsInfo result = RS_INFO_CACHE.get(radicalStrokeString);
            if (result != null)
                return result;
            if (!RSUnicodeMatcher.reset(radicalStrokeString).matches()) {
                throw new RuntimeException("Strange value for: " + radicalStrokeString);
            }
            result = new RsInfo();
            result.radical = Integer.parseInt(RSUnicodeMatcher.group(1));
            result.alternate = RSUnicodeMatcher.group(2).length() == 0 ? 0 : 1;
            result.remainingStrokes = Integer.parseInt(RSUnicodeMatcher.group(3));
            RS_INFO_CACHE.put(radicalStrokeString, result);
            return result;
        }

        private int getComputedStrokes(int[] mainStrokesIn, int[] alternateStrokesIn) {
            return remainingStrokes + (alternate == 0 ? mainStrokesIn[radical] : alternateStrokesIn[radical]);
        }

        public int getRsOrder() {
            return radical * 1000 + alternate * 100 + remainingStrokes;
        }

        public static void addToStrokeInfo(UnicodeMap<Integer> bestStrokesIn, boolean simplified) {
            int[] mainStrokes      = new int[256];
            int[] alternateStrokes = new int[256];

            Counter<Integer> mainStrokesTotal = new Counter<Integer>();
            Counter<Integer> mainCount = new Counter<Integer>();
            Counter<Integer> alternateStrokesTotal = new Counter<Integer>();
            Counter<Integer> alternateCount = new Counter<Integer>();
            for (String s : bestStrokesIn) {
                int codePointAt = s.codePointAt(0);
                Integer bestStrokeInfo = bestStrokesIn.get(codePointAt);
                RsInfo rsInfo = RsInfo.from(codePointAt);
                if (rsInfo == null) {
                    continue;
                }
                int radicalsStrokes = bestStrokeInfo - rsInfo.remainingStrokes;
                if (rsInfo.alternate == 0) {
                    mainStrokesTotal.add(rsInfo.radical, radicalsStrokes);
                    mainCount.add(rsInfo.radical, 1);
                } else {
                    alternateStrokesTotal.add(rsInfo.radical, radicalsStrokes);
                    alternateCount.add(rsInfo.radical, 1);
                }
            }
            // compute averages. Lame, but the best we have for now.
            for (int key : mainStrokesTotal.keySet()) {
                mainStrokes[key] = (int) Math.round(mainStrokesTotal.get(key) / (double) mainCount.get(key));
                System.out.println("radical " + key + "\t" + mainStrokes[key]);
            }
            for (int key : alternateStrokesTotal.keySet()) {
                alternateStrokes[key] = (int) Math.round(alternateStrokesTotal.get(key) / (double) alternateCount.get(key));
                System.out.println("radical' " + key + "\t" + alternateStrokes[key]);
            }
            PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, "imputedStrokes" + (simplified ? "" : "T") +
            		".txt", null);
            for (String s : new UnicodeSet(kRSUnicode.keySet()).removeAll(bestStrokesIn.keySet())) {
                int computedStrokes = RsInfo.from(s.codePointAt(0)).getComputedStrokes(mainStrokes, alternateStrokes);
                bestStrokesIn.put(s, computedStrokes);
                out.println("U+" + Utility.hex(s) + "\tkImputedStrokes\t" + computedStrokes + "\t#\t" + s);
            }
            closeUnderNFKD("Strokes", bestStrokesIn);
            bestStrokesIn.freeze();      
            out.close();
        }
    }


    private static <S> void showSorting(Comparator<String> comparator, UnicodeMap<S> unicodeMap, String filename, InfoType infoType) {
        showSorting(comparator, unicodeMap, filename, FileType.txt, infoType);
        showSorting(comparator, unicodeMap, filename, FileType.xml, infoType);
    }

    private static <S> void showSorting(Comparator<String> comparator, UnicodeMap<S> unicodeMap, String filename, FileType fileType, InfoType infoType) {
        // special capture for Pinyin buckets
        boolean isPinyin = filename.startsWith("pinyin") && fileType == FileType.xml;
        int alpha = 'a';
        StringBuilder pinyinBuffer = new StringBuilder("\"\", // A\n");
        StringBuilder pinyinIndexBuffer = new StringBuilder("\"\u0101");

        UnicodeSet accumulated = new UnicodeSet();
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, filename + "." + fileType, null);
        TreeSet<String> rsSorted = new TreeSet<String>(comparator);
        StringBuilder buffer = new StringBuilder();
        for (String s : unicodeMap) {
            //            S newValue = unicodeMap.get(s);
            //            if (newValue == null) continue;
            if (s.codePointCount(0, s.length()) != 1) {
                throw new IllegalArgumentException("Wrong length!!");
            }
            rsSorted.add(s);
        }
        if (fileType == FileType.txt) {
            out.println("&[last regular]");
        } else {
            String typeAlt = filename.replace("_", "' alt='");
            out.print(
                    "<?xml version='1.0' encoding='UTF-8' ?>\n"
                    +"<!DOCTYPE ldml SYSTEM '/Users/markdavis/Documents/workspace/cldr/common/dtd/ldml.dtd'>\n"
                    +"<ldml>\n"
                    +"    <identity>\n"
                    +"        <version number='$Revision: 1.8 $' />\n"
                    +"        <generation date='$Date: 2010/12/14 07:57:17 $' />\n"
                    +"        <language type='zh' />\n"
                    +"    </identity>\n"
                    +"    <collations>\n"
                    +"        <collation type='" + typeAlt + "'>\n"
            );
            FileUtilities.appendFile(GenerateUnihanCollators.class, "pinyinHeader.txt", out);
        }
        S oldValue = null;
        String lastS = null;
        for (String s : rsSorted) {
            S newValue = unicodeMap.get(s);
            if (!equals(newValue, oldValue)) {
                if (oldValue == null) {
                    // do nothing
                } else {
                    if (buffer.codePointCount(0, buffer.length()) < 128) {
                        if (fileType == FileType.txt) {
                            out.println("<*" + sortingQuote(buffer.toString(), accumulated) + "\t#" + sortingQuote(oldValue, accumulated));
                        } else {
                            out.println("               <pc>" + buffer + "</pc><!-- " + oldValue + " -->");
                        }
                    } else {
                        int count = 1;
                        while (buffer.length() > 0) {
                            String temp = extractFirst(buffer, 128);
                            if (fileType == FileType.txt) {
                                out.println("<*" + sortingQuote(temp.toString(), accumulated) + "\t#" + sortingQuote(oldValue, accumulated) + " (p" + count++ + ")");
                            } else {
                                out.println("               <pc>" + temp + "</pc><!-- " + oldValue + " (p" + count++ + ") -->");
                            }
                        }
                    }
                }
                buffer.setLength(0);
                oldValue = newValue;
                if (isPinyin) {
                    // OK to just use codepoint order.
                    String pinyinValue = newValue.toString();
                    int pinyinFirst = nfd.normalize(pinyinValue).charAt(0);
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
            lastS = s;
        }

        if (oldValue != null) {
            if (fileType == FileType.txt) {
                out.println("<*" + sortingQuote(buffer.toString(), accumulated) + "\t#" + sortingQuote(oldValue, accumulated));
            } else {
                out.println("               <pc>" + buffer + "</pc><!-- " + oldValue + " -->");
            }
            buffer.setLength(0);
        }

        UnicodeSet missing = new UnicodeSet().addAll(buffer.toString());
        if (missing.size() != 0) {
            System.out.println("MISSING" + "\t" + missing.toPattern(false));
        }
        UnicodeSet tailored = new UnicodeSet(unicodeMap.keySet()).removeAll(missing);

        TreeSet<String> sorted = new TreeSet<String>(nfkdComparator);
        new UnicodeSet(NOT_NFKD).removeAll(NOT_NFD).removeAll(tailored).addAllTo(sorted);

        for (String s : sorted) {
            // decomposable, but not tailored
            String kd = nfkd.getDecomposition(s.codePointAt(0));
            if (!tailored.containsSome(kd))
                continue; // the decomp has to contain at least one tailored
            //            if (tailored.containsAll(kd))
            //                continue; //already have it
            // character
            if (fileType == FileType.txt) {
                out.println("&" + sortingQuote(kd, accumulated) + "<<<" + sortingQuote(s, accumulated));
            } else {
                out.println("               <reset>" + kd + "</reset>");
                out.println("               <t>" + s + "</t>");
            }
        }
        if (fileType == FileType.xml) {
            out.println(
                    "           </rules>\n" +
                    "        </collation>\n" +
                    "    </collations>\n" +
                    "</ldml>"
            );
        }

        out.close();
        out = Utility.openPrintWriter(OUTPUT_DIRECTORY, filename + "_repertoire.txt", null);
        out.println(accumulated.toPattern(false));
        out.close();
        if (isPinyin) {
            out = Utility.openPrintWriter(OUTPUT_DIRECTORY, filename + "_buckets.txt", null);
            pinyinIndexBuffer.append('"');
            out.println(pinyinIndexBuffer);
            out.println(pinyinBuffer);
            out.close();
        }
    }

    /**
     * Hex format by code unit.
     * @param s
     * @return
     */
    private static CharSequence hexConstant(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
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
            int charEnd = buffer.offsetByCodePoints(0, i);
            result = buffer.substring(0, charEnd);
            buffer.delete(0, charEnd);
        } catch (Exception e) {
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
        for (String s : bihuaData.keySet()) {
            R2<String, String> bihuaRow = bihuaData.get(s);
            String value = bihuaRow.get0();
            addPinyin("bihua", s, value, Override.keepOld);
        }
    }

    private static void printExtraPinyinForUnihan() {
        PrintWriter out = Utility.openPrintWriter(OUTPUT_DIRECTORY, "kMandarinAdditions.txt", null);

        out.println("#Code\tkMandarin\tValue\t#\tChar");
        for (String s : new UnicodeSet(bestPinyin.keySet()).removeAll(originalPinyin)) {
            if (s.codePointAt(0) < 0x3400)
                continue;
            String value = bestPinyin.get(s);
            String oldValue = toNumericPinyin.transform(value).toUpperCase();
            out.println("U+" + Utility.hex(s) + "\tkMandarin\t" + oldValue + "\t#\t" + s);
        }
        out.close();

        out = Utility.openPrintWriter(OUTPUT_DIRECTORY, "kPinyinOverride.txt", null);
        out.println("#Code\t'Best'\tValue\t#\tChar\tUnihan");
        for (String s : new UnicodeSet(bestPinyin.keySet()).retainAll(originalPinyin).retainAll(bihuaData.keySet())) {
            if (s.codePointAt(0) < 0x3400)
                continue;
            String value = bestPinyin.get(s);
            R2<String, String> datum = bihuaData.get(s);
            String bihua = datum.get0();
            if (value.equals(bihua))
                continue;
            out.println("U+" + Utility.hex(s) + "\tkPinyinOverride\t" + value + "\t#\t" + s + "\t" + bihua);
        }
        out.close();

    }

    private static void addVariants(String title, UnicodeMap<String> variantMap) {
        for (String s : variantMap) {
            String value = variantMap.get(s);
            if (value == null)
                continue;
            for (String part : value.split(" ")) {
                if (!unicodeCp.reset(part).matches()) {
                    throw new IllegalArgumentException();
                } else {
                    int cp = Integer.parseInt(unicodeCp.group(1), 16);
                    Set<String> others = mergedPinyin.get(cp);
                    if (others != null) {
                        for (String other : others) {
                            addPinyin(title, UTF16.valueOf(cp), other, Override.keepOld);
                        }
                    }
                }
            }
        }
    }

    private static void addRadicals() {
        for (String s : radicalMap.keySet()) {
            String main = radicalMap.get(s);
            Set<String> newValues = mergedPinyin.get(main);
            if (newValues == null)
                continue;
            for (String newValue : newValues) {
                addPinyin("radicals", s, newValue, Override.keepOld);
            }
        }
    }

    private static void addRadicals(UnicodeMap<String> source) {
        for (String s : radicalMap.keySet()) {
            String main = radicalMap.get(s);
            String newValue = source.get(main);
            if (newValue == null)
                continue;
            source.put(s, newValue);
        }
    }

    static boolean validPinyin(String pinyin) {
        Boolean cacheResult = validPinyin.get(pinyin);
        if (cacheResult != null) {
            return cacheResult;
        }
        boolean result;
        String base = noaccents.transform(pinyin);
        int initialEnd = INITIALS.findIn(base, 0, true);
        if (initialEnd == 0 && (pinyin.startsWith("i") || pinyin.startsWith("u"))) {
            result = false;
        } else {
            String finalSegment = base.substring(initialEnd);
            result = finalSegment.length() == 0 ? true : FINALS.contains(finalSegment);
        }
        validPinyin.put(pinyin, result);
        return result;
    }

    static void addAll(String han, String original, PinyinSource pinyin, String... pinyinList) {
        if (pinyinList.length == 0) {
            throw new IllegalArgumentException();
        }
        for (String source : pinyinList) {
            if (source.length() == 0) {
                throw new IllegalArgumentException();
            }
            addPinyin(pinyin.toString(), han, source, Override.keepOld);
        }
    }

    private static void addPinyin(String title, String han, String source, Override override) {
        if (!validPinyin(source)) {
            System.out.println("***Invalid Pinyin - " + title + ": " + han + "\t" + source + "\t" + Utility.hex(han));
            return;
        }
        source = source.intern();
        String item = bestPinyin.get(han);
        if (item == null || override == Override.keepNew) {
            if (item != null) {
                System.out.println("Overriding Pinyin " + han + "\told: " + item + "\tnew: " + source);
            }
            bestPinyin.put(han, source);
        }
        Set<String> set = mergedPinyin.get(han);
        if (set == null) {
            mergedPinyin.put(han, set = new LinkedHashSet<String>());
        }
        set.add(source);
    }

    static final class MyFileReader extends SemiFileReader {
        public final Pattern SPLIT = Pattern.compile("\\s*,\\s*");
        String               last  = "";

        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        protected boolean isCodePoint() {
            return false;
        };

        /**
         * ;Radical Number,Status,Unified
         * Ideo,Hex,Radical,Hex,Name,Conf.Char,Hex,Unified Ideo. has NO
         * RemainingStrokes in Unihan 1,Main,一,U+4E00,⼀,U+2F00,ONE
         */
        protected boolean handleLine(int start, int end, String[] items) {
            if (items[0].startsWith(";")) {
                return true;
            }
            if (items[2].length() != 0) {
                last = items[2];
            }

            String radical = items[4];
            if (NOT_NFC.contains(radical)) {
                return true;
            }
            radicalMap.put(radical, last);
            return true;
        }
    };

    // 吖 ; a ; 1 ; 251432 ; 0x5416
    static final class BihuaReader extends SemiFileReader {
        protected boolean isCodePoint() {
            return false;
        };

        Set<String> seen = new HashSet<String>();

        protected boolean handleLine(int start, int end, String[] items) {
            String character = items[0];
            String pinyinBase = items[1];
            String other = pinyinBase.replace("v", "ü");
            if (!other.equals(pinyinBase)) {
                if (!seen.contains(other)) {
                    System.out.println("bihua: " + pinyinBase + " => " + other);
                    seen.add(other);
                }
                pinyinBase = other;
            }
            String pinyinTone = items[2];
            String charSequence = items[3];
            String hex = items[4];
            if (!hex.startsWith("0x")) {
                throw new RuntimeException(hex);
            } else {
                int hexValue = Integer.parseInt(hex.substring(2), 16);
                if (!character.equals(UTF16.valueOf(hexValue))) {
                    throw new RuntimeException(hex + "!=" + character);
                }
            }
            String source = fromNumericPinyin.transform(pinyinBase + pinyinTone);
            bihuaData.put(character, Row.of(source, charSequence));
            return true;
        }
    };

    static final class PatchPinyinReader extends SemiFileReader {
        boolean skip = false;

        protected boolean isCodePoint() {
            return false;
        };

        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

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
                addPinyin("patchPinyin", items[0], items[1], Override.keepNew);
            }
            return true;
        }
    }

    static final class PatchStrokeReader extends SemiFileReader {
        final UnicodeMap<Integer> target;
        boolean skip = false;

        PatchStrokeReader(UnicodeMap<Integer> target) {
            this.target = target;
        }

        protected boolean isCodePoint() {
            return false;
        };
        
        protected void processComment(String line, int comment) {
            if (only19 && line.substring(comment).contains("1.9.1")) {
                skip = true;
            }
        }

        protected boolean handleLine(int start, int end, String[] items) {
            if (skip) {
                return true;
            }
            if (!UNIHAN.contains(items[0])) {
                throw new IllegalArgumentException("Non-Unihan character: " + items[0]);
            }
            if (items.length > 1) {
                target.put(items[0], Integer.parseInt(items[1]));
            }
            return true;
        }
    }
    
    static Comparator<String> RSComparator     = new Comparator<String>() {

        public int compare(String o1, String o2) {
            int cp1 = o1.codePointAt(0);
            int cp2 = o2.codePointAt(0);
            int s1 = getSortOrder(cp1);
            int s2 = getSortOrder(cp2);
            int result = s1 - s2;
            if (result != 0)
                return result;
            return cp1 - cp2;
        }
    };

    static class StrokeComparator implements Comparator<String> {
        final UnicodeMap<Integer> baseMap;
        public StrokeComparator(UnicodeMap<Integer> baseMap) {
            this.baseMap = baseMap;
        }
        public int compare(String o1, String o2) {
            int cp1 = o1.codePointAt(0);
            int cp2 = o2.codePointAt(0);
            Integer n1 = baseMap.get(cp1);
            Integer n2 = baseMap.get(cp2);
            if (n1 == null) {
                if (n2 != null) {
                    return 1;
                }
                // both null, fall through
            } else if (n2 == null) {
                return -1;
            } else { // both not null
                int result = n1 - n2;
                if (result != 0) {
                    return result;
                }
            }
            return RSComparator.compare(o1, o2);
        }
    }

    static Comparator<String> SStrokeComparator = new StrokeComparator(bestStrokesS);
    static Comparator<String> TStrokeComparator = new StrokeComparator(bestStrokesT);

    static Comparator<String> PinyinComparator = new Comparator<String>() {

        public int compare(String o1, String o2) {
            int cp1 = o1.codePointAt(0);
            int cp2 = o2.codePointAt(0);
            String s1 = bestPinyin.get(cp1);
            String s2 = bestPinyin.get(cp2);
            if (s1 == null) {
                if (s2 != null) {
                    return 1;
                }
            } else if (s2 == null) {
                return -1;
            }
            int result = pinyinSort.compare(s1, s2);
            if (result != 0) {
                return result;
            }
            return SStrokeComparator.compare(o1, o2);
        }
    };
}
