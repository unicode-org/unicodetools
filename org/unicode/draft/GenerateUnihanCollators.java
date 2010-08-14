package org.unicode.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.draft.ComparePinyin.PinyinSource;
import org.unicode.jsp.FileUtilities.SemiFileReader;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.ULocale;

public class GenerateUnihanCollators {
    static final UnicodeSet         UNIHAN              = new UnicodeSet("[:script=han:]").freeze();
    static final Collator           pinyinSort          = Collator.getInstance(new ULocale("zh@collator=pinyin"));
    static final Collator           radicalStrokeSort   = Collator.getInstance(new ULocale("zh@collator=unihan"));
    static final Transliterator     toPinyin            = Transliterator.getInstance("Han-Latin;nfc");
    static final Comparator<String> codepointComparator = new UTF16.StringComparator(true, false, 0);
    static final UnicodeMap<String> kRSUnicode          = Default.ucd().getHanValue("kRSUnicode");
    static final Normalizer2 nfkd = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
    static final Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Mode.DECOMPOSE);

    static final Matcher            RSUnicodeMatcher    = Pattern.compile("^([1-9][0-9]{0,2})('?)\\.([0-9]{1,2})$")
    .matcher("");
    static Map<String, Integer>     cache               = new HashMap<String, Integer>();
    static UnicodeMap<String> radicalMap = new UnicodeMap<String>();
    static {
        new MyFileReader().process(GenerateUnihanCollators.class, "CJK_Radicals.csv");
        for (String s : radicalMap.values()) {
            System.out.println(s + "\t" + radicalMap.getSet(s).toPattern(false));
        }
        addRadicals(kRSUnicode);
    }

    public static void main(String[] args) throws IOException {
        showSorting(RSComparator, kRSUnicode, "radicalStrokeCollation.txt",false);
        showSorting(PinyinComparator, unihanPinyin, "pinyinCollation.txt",false);
        showSorting(PinyinComparator, unihanPinyin, "pinyinCollationInterleaved.txt", true);
        showTranslit("pinyinTranslit.txt");
        System.out.println("TODO: test the conversions");
    }

    private static void showTranslit(String filename) throws IOException {
        PrintWriter out = Utility.openPrintWriter(filename, null);
        TreeSet<String> s = new TreeSet(pinyinSort);
        s.addAll(unihanPinyin.getAvailableValues());
        for (String value : s) {
            UnicodeSet uset = unihanPinyin.getSet(value);
            out.println(uset.toPattern(false) + "→"+value + ";");
        }
        out.close();
    }

    public static int getSortOrder(int codepoint) {
        String valuesString = kRSUnicode.get(codepoint);
        if (valuesString == null) {
            return Integer.MAX_VALUE / 2;
        }
        Integer result = cache.get(valuesString);
        if (result != null) {
            return result;
        }
        int x;
        String[] values = valuesString.split(" ");

        String primaryValue = values[0];
        if (!RSUnicodeMatcher.reset(primaryValue).matches()) {
            throw new RuntimeException("Strange value for: " + valuesString);
        }
        int radical = Integer.parseInt(RSUnicodeMatcher.group(1));
        int alternate = RSUnicodeMatcher.group(2).length() == 0 ? 0 : 1;
        int stroke = Integer.parseInt(RSUnicodeMatcher.group(3));
        result = radical * 1000 + alternate * 100 + stroke;
        cache.put(valuesString, result);
        return result;
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

    static Comparator<String> PinyinComparator = new Comparator<String>() {

        public int compare(String o1, String o2) {
            int cp1 = o1.codePointAt(0);
            int cp2 = o2.codePointAt(0);
            String s1 = unihanPinyin.get(cp1);
            String s2 = unihanPinyin.get(cp2);
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
            return RSComparator.compare(o1, o2);
        }
    };

    private static void showSorting(Comparator<String> comparator, UnicodeMap<String> unicodeMap, String filename, boolean interleave) throws IOException {
        PrintWriter out = Utility.openPrintWriter(filename, null);
        TreeSet<String> rsSorted = new TreeSet<String>(comparator);
        StringBuilder buffer = new StringBuilder();
        for (String s : UNIHAN) {
            rsSorted.add(s);
        }
        if (interleave) {
            out.println("#[import XXX]");
            out.println("# 1. need to decide whether Latin pinyin comes before or after Han characters with that pinyin.");
            out.println("#\tEg, with <* 叭吧巴𣬶𣬷笆罢罷 <* 㓦𢛞挀掰擘𨃅䪹 #bāi, do we have 罷 < bāi or have 䪹 < bāi");
            out.println("# 2. need to also do single latin characters, and case variants");
        } else {
            out.println("&[last regular]");
        }
        String oldValue = null;
        String lastS = null;
        for (String s : rsSorted) {
            String newValue = unicodeMap.get(s);
            if (!equals(newValue, oldValue)) {
                if (oldValue == null) {
                    // do nothing
                } else if (interleave) {
                    out.println("&" + lastS + "<" + oldValue);
                } else {
                    out.println("<*" + buffer + "\t#" + oldValue);
                }
                buffer.setLength(0);
                oldValue = newValue;
            }
            buffer.append(s);
            lastS = s;
        }

        if (oldValue != null) {
            if (interleave) {
                out.println("&" + lastS + "<" + oldValue);
            } else {
                out.println("<*" + buffer + "\t#" + oldValue);
            }
        } else if (!interleave) {
            UnicodeSet missing = new UnicodeSet().addAll(buffer.toString());
            System.out.println("MISSING" + "\t" + missing.toPattern(false));
            UnicodeSet tailored = new UnicodeSet(UNIHAN).removeAll(missing);
            
            for (String s : new UnicodeSet("[:nfkd_qc=n:]").removeAll(tailored)) { // decomposable, but not tailored
                String kd = nfkd.getDecomposition(s.codePointAt(0));
                if (!tailored.containsSome(kd)) continue; // the decomp has to contain at least one tailored character
                String d = nfd.getDecomposition(s.codePointAt(0));
                if (kd.equals(d)) continue; // the NFD characters are already handled by UCA
                out.println("&" + kd + "<<<" + s);
            }
        }
        out.close();
    }

    private static boolean equals(Object newValue, Object oldValue) {
        return newValue == null ? oldValue == null
                : oldValue == null ? false
                        : newValue.equals(oldValue);
    }

    // kHanyuPinyin, space, 10297.260: qīn,qìn,qǐn,
    // [a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]+(,[a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]
    // kMandarin, space, [A-Z\x{308}]+[1-5] // 3475=HAN4 JI2 JIE2 ZHA3 ZI2
    // kHanyuPinlu, space, [a-z\x{308}]+[1-5]\([0-9]+\) 4E0A=shang4(12308)
    // shang5(392)
    static UnicodeMap<String>               unihanPinyin = new UnicodeMap();
    static Transform<String, String> noaccents    = Transliterator.getInstance("nfkd; [[:m:]-[\u0308]] remove; nfc");
    static Transform<String, String> accents      = Transliterator.getInstance("nfkd; [^[:m:]-[\u0308]] remove; nfc");

    static UnicodeSet                INITIALS     = new UnicodeSet("[b c {ch} d f g h j k l m n p q r s {sh} t w x y z {zh}]").freeze();
    static UnicodeSet                FINALS       = new UnicodeSet(
    "[a {ai} {an} {ang} {ao} e {ei} {en} {eng} {er} i {ia} {ian} {iang} {iao} {ie} {in} {ing} {iong} {iu} o {ong} {ou} u {ua} {uai} {uan} {uang} {ue} {ui} {un} {uo} ü {üe}]")
    .freeze();


    static {
        Transform<String, String> pinyinNumeric = Transliterator.getInstance("NumericPinyin-Latin;nfc");

        // all kHanyuPinlu readings first; then take all kXHC1983; then
        // kHanyuPinyin.

        UnicodeMap<String> kHanyuPinlu = Default.ucd().getHanValue("kHanyuPinlu");
        for (String s : kHanyuPinlu.keySet()) {
            String original = kHanyuPinlu.get(s);
            String source = original.replaceAll("\\([0-9]+\\)", "");
            source = pinyinNumeric.transform(source);
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
            source = pinyinNumeric.transform(source);
            addAll(s, original, PinyinSource.m, source.split(" "));
        }
        
        addRadicals(unihanPinyin);
    }

    private static void addRadicals(UnicodeMap<String> unicodeMap) {
        for (String s : radicalMap.keySet()) {
            if (unicodeMap.get(s) != null) continue;
            String main = radicalMap.get(s);
            String newValue = unicodeMap.get(main);
            unicodeMap.put(s, newValue);
        }
    }

    static boolean validPinyin(String pinyin) {
        String base = noaccents.transform(pinyin);
        int initialEnd = INITIALS.findIn(base, 0, true);
        if (initialEnd == 0 && (pinyin.startsWith("i") || pinyin.startsWith("u"))) {
            return false;
        }
        String finalSegment = base.substring(initialEnd);
        boolean result = finalSegment.length() == 0 ? true : FINALS.contains(finalSegment);
        return result;
    }

    static void addAll(String han, String original, PinyinSource pinyin, String... pinyinList) {
        String item = unihanPinyin.get(han);
        if (item != null)
            return; // just do first

        if (pinyinList.length == 0) {
            throw new IllegalArgumentException();
        }
        for (String source : pinyinList) {
            if (source.length() == 0) {
                throw new IllegalArgumentException();
            }
            if (!validPinyin(source)) {
                System.out.println("***Invalid Pinyin: " + han + "\t" + pinyin + "\t" + source + "\t" + Utility.hex(han) + "\t" + original);
            }
            unihanPinyin.put(han, source.intern());
            return;
        }
    }
    
    static final class MyFileReader extends SemiFileReader {
        public final Pattern SPLIT = Pattern.compile("\\s*,\\s*");
        String last = "";
        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }
        protected boolean isCodePoint() {
            return false;
        };

        /**
         * ;Radical Number,Status,Unified Ideo,Hex,Radical,Hex,Name,Conf.Char,Hex,Unified Ideo. has NO RemainingStrokes in Unihan
         * 1,Main,一,U+4E00,⼀,U+2F00,ONE
         */
        protected boolean handleLine(int start, int end, String[] items) {
            if (items[0].startsWith(";")) {
                return true;
            }
            if (items[2].length() != 0) {
                last = items[2];
            }
            radicalMap.put(items[4], last);
            return true;
        } 
    };
}
