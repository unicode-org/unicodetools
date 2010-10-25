package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.jsp.CharEncoder;
import org.unicode.jsp.FileUtilities;
import org.unicode.jsp.FileUtilities.SemiFileReader;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.ULocale;

public class FindHanSizes {
    static final int        SHOW_LIMIT   = 100;
    static Normalizer2      nfkd         = Normalizer2.getInstance(null, "nfkc", Mode.DECOMPOSE);
    static UnicodeSet       NONCANONICAL = new UnicodeSet("[:nfd_qc=n:]");
    static final UnicodeSet HAN;
    static {
        // make sure we include the characters that contain HAN
        HAN = new UnicodeSet("[[:sc=han:][:ideographic:]]");
        for (String s : new UnicodeSet("[:nfkd_qc=n:]")) {
            if (HAN.containsSome(nfkd.normalize(s))) {
                HAN.add(s);
            }
        }
        HAN.removeAll(NONCANONICAL);
        HAN.freeze();
    }

    enum NamedHanSet {
        zh, zh_Hant, GB2312, GBK, Big5, Big5_HKSCS, Stroke, Pinyin, NewStroke, NewPinyin;

        public static String toString(EnumSet<NamedHanSet> set) {
            StringBuilder result = new StringBuilder();
            for (NamedHanSet item : values()) {
                result.append('\t');
                if (set.contains(item)) {
                    result.append(item.toString());
                }
            }
            return result.toString();
        }
    }

    static class SetComparator<T extends Comparable<T>> implements Comparator<Set<T>> {
        public int compare(Set<T> o1, Set<T> o2) {
            int size1 = o1.size();
            int size2 = o2.size();
            int diff = size1 - size2;
            if (diff != 0) {
                return diff;
            }
            Iterator<T> i1 = o1.iterator();
            Iterator<T> i2 = o2.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                T item1 = i1.next();
                T item2 = i2.next();
                diff = item1.compareTo(item2);
                if (diff != 0) {
                    return diff;
                }
            }
            // we know that they are the same length at this point, so if we
            // make it through the gauntlet, we're done
            return 0;
        }
    }

    static final SetComparator<NamedHanSet> SINGLETON = new SetComparator<NamedHanSet>();
    static Normalizer2                      nfc       = Normalizer2.getInstance(null, "nfc", Mode.COMPOSE);

    static class EncodingInfo {
        Map<Integer, EnumSet<NamedHanSet>> status = new HashMap();

        void add(int cp, NamedHanSet e) {
            String s = nfc.normalize(UTF16.valueOf(cp));
            int newCp = s.codePointAt(0);
            if (cp != newCp) {
                if (s.length() != Character.charCount(newCp)) {
                    throw new IllegalArgumentException("Han growing??");
                }
                cp = newCp;
            }
            EnumSet<NamedHanSet> set = status.get(cp);
            if (set == null) {
                status.put(cp, set = EnumSet.noneOf(NamedHanSet.class));
            }
            set.add(e);
        }

        public TreeSet<EnumSet<NamedHanSet>> getValues() {
            Collection<EnumSet<NamedHanSet>> values = status.values();
            TreeSet<EnumSet<NamedHanSet>> set = new TreeSet(SINGLETON);
            set.addAll(values);
            return set;
        }

        public UnicodeMap<EnumSet<NamedHanSet>> getUnicodeMap() {
            UnicodeMap<EnumSet<NamedHanSet>> result = new UnicodeMap();
            for (Entry<Integer, EnumSet<NamedHanSet>> entry : status.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }

        public void addAll(UnicodeSet tailored, NamedHanSet e) {
            for (UnicodeSetIterator it = new UnicodeSetIterator(tailored); it.next();) {
                add(it.codepoint, e);
            }
        }

        public UnicodeMap<EnumSet<NamedHanSet>> showContents() {
            UnicodeMap<EnumSet<NamedHanSet>> unicodeMap = getUnicodeMap();
            Collection<EnumSet<NamedHanSet>> values = unicodeMap.values();
            TreeSet<EnumSet<NamedHanSet>> set = new TreeSet(SINGLETON);
            set.addAll(values);

            for (EnumSet<NamedHanSet> value : set) {
                UnicodeSet keys = unicodeMap.getSet(value);
                System.out.println(NamedHanSet.toString(value) + "\t" + keys.size() + "\t" + toAbbreviated(SHOW_LIMIT, keys));
            }
            System.out.println("Total:\t" + unicodeMap.size() + "\t" + toAbbreviated(SHOW_LIMIT, unicodeMap.keySet()));
            return unicodeMap;
        }
    }

    /**
     * Shows only chars, and just to the limit
     * 
     * @param input
     * @param limit
     * @return
     */
    static String toAbbreviated(int limit, UnicodeSet input) {
        int ranges = input.getRangeCount();
        if (ranges * 3 < limit) {
            return input.toPattern(false);
        } else {
            UnicodeSet smaller = new UnicodeSet();
            int count = 0;
            for (UnicodeSetIterator it = new UnicodeSetIterator(input); it.nextRange();) {
                count += it.codepoint == it.codepointEnd ? 1 : it.codepoint + 1 == it.codepointEnd ? 2 : 3;
                if (count > limit)
                    break;
                smaller.addAll(it.codepoint, it.codepointEnd);
            }
            return smaller.toPattern(false) + '…';
        }
    }

    public static void main(String[] args) {
        System.out.println("Use GenerateHanCollators for data");
        System.out.println("All Han:\t" + HAN.size() + "\t" + HAN.toPattern(false));
        Set<String> collators = new TreeSet<String>();
        collators.addAll(Arrays.asList(Collator.getKeywordValuesForLocale("collation", ULocale.CHINESE, false)));
        System.out.println("Collators:\t" + collators);
        for (String collatorType : collators) {
            UnicodeSet set = getTailoredHan(collatorType);
            System.out.println(collatorType + "\t" + set.size() + "\t" + set);
        }

        EncodingInfo info = new EncodingInfo();

        System.out.println("Most Frequent (99.9%)");
        addHanMostFrequent(info, NamedHanSet.zh, 0.999);
        addHanMostFrequent(info, NamedHanSet.zh_Hant, 0.999);
        // addHanMostFrequent(info, NamedHanSet.ja, 0.999);
        info.showContents();

        System.out.println("Current Collators");
        addTailoredHan(info, NamedHanSet.Stroke);
        addTailoredHan(info, NamedHanSet.Pinyin);
        info.showContents();

        System.out.println("New Collators");
        addNewCollator(info, NamedHanSet.NewStroke);
        addNewCollator(info, NamedHanSet.NewPinyin);
        info.showContents();

        System.out.println("Comparing Charsets");

        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        System.out.println("All charsets:\t" + charsets.keySet());

        Matcher charsetMatcher = Pattern.compile("GB2312|GBK|Big5|Big5-HKSCS").matcher("");
        for (String name : charsets.keySet()) {
            if (!charsetMatcher.reset(name).matches()) {
                continue;
            }
            UnicodeSet result = getCharsetRepertoire(name);
            NamedHanSet e = NamedHanSet.valueOf(name.replace("-", "_"));
            for (String s: result) {
                int cp = s.codePointAt(0);
                info.add(cp, e);
            }
        }

        info.showContents();
    }

    static UnicodeSet getCharsetRepertoire(String name) {
        UnicodeSet result = new UnicodeSet();
        {
            Charset charset = Charset.forName(name);
            CharEncoder encoder = new CharEncoder(charset, true, true);
            for (String s : HAN) {
                int cp = s.codePointAt(0);
                if (encoder.getValue(cp, null, 0) > 0) {
                    result.add(cp);
                }
            }
        }
        return result;
    }

    private static void addNewCollator(EncodingInfo info, NamedHanSet e) {
        try {
            BufferedReader in = FileUtilities.openFile(FindHanSizes.class, e + "_repertoire.txt");
            String contents = FileUtilities.getFileAsString(in);
            UnicodeSet items = new UnicodeSet(contents);
            items.retainAll(HAN);
            info.addAll(items, e);
            System.out.println(e + "\t" + items.size() + "\t" + toAbbreviated(SHOW_LIMIT, items));
        } catch (IOException e1) {
            throw new IllegalArgumentException(e1);
        }
    }

    private static void addTailoredHan(EncodingInfo info, NamedHanSet e) {
        UnicodeSet tailored = getTailoredHan(e.toString());
        info.addAll(tailored, e);
    }

    private static UnicodeSet getTailoredHan(String type) {
        Collator collator = Collator.getInstance(new ULocale("zh_co_" + type));
        UnicodeSet tailored = new UnicodeSet(collator.getTailoredSet()).retainAll(HAN).removeAll(NONCANONICAL);
        return tailored;
    }

    static void addHanMostFrequent(EncodingInfo info, NamedHanSet e, double limit) {
        UnicodeSet results = getMostFrequent(e.toString(), limit);
        info.addAll(results, e);
    }

    public static UnicodeSet getMostFrequent(String e, double limit) {
        MyReader myReader = new MyReader(limit);
        myReader.process(MyReader.class, e + ".txt");
        UnicodeSet results = myReader.results;
        return results;
    }

    // 1000 0.8716829002327223 [一七三-下不且世並中主久之九也了事二五
    static class MyReader extends SemiFileReader {
        UnicodeSet results = new UnicodeSet();
        double     limit;

        MyReader(double limit) {
            this.limit = limit;
        }

        public final Pattern SPLIT2 = Pattern.compile("\\s+");

        protected String[] splitLine(String line) {
            return SPLIT2.split(line);
        }

        protected boolean isCodePoint() {
            return false;
        }

        protected boolean handleLine(int start, int end, String[] items) {
            double inclusion = Double.parseDouble(items[1]);
            if (inclusion <= limit) {
                UnicodeSet other = new UnicodeSet(items[2]);
                results.addAll(other);
            }
            return true;
        }

    };
}
