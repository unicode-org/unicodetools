package org.unicode.draft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.Tabber;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.IterableComparator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;


public class ComparePinyin {

    private static final UnicodeSet UNIHAN = new UnicodeSet("[:script=han:]").freeze();
    static Collator pinyinSort = Collator.getInstance(new ULocale("zh@collator=pinyin"));
    static Collator radicalStrokeSort = Collator.getInstance(new ULocale("zh@collator=unihan"));
    static Transliterator toPinyin = Transliterator.getInstance("Han-Latin;nfc");
    static final Comparator<String> codepointComparator = new UTF16.StringComparator(true, false,0);


    public static void main(String[] args) throws IOException {


        showElements("", toPinyin);
        final UnihanPinyin unihanPinyin = new UnihanPinyin();


        final Relation<String, String> notUnihan = new Relation(new TreeMap(pinyinSort), TreeSet.class);

        for (final String s : new UnicodeSet(HAN).removeAll(unihanPinyin.keySet()).removeAll(new UnicodeSet("[:nfkcqc=n:]"))) {
            final String pinyin = toPinyin.transform(s);
            if (!s.equals(pinyin)) {
                notUnihan.put(pinyin, s);
            }
        }

        System.out.println("Characters without Unihan Pinyin but with CLDR");
        for (final String pinyin : notUnihan.keySet()) {
            final Set<String> s = notUnihan.getAll(pinyin);
            System.out.println(pinyin + "\t" + showSet(s));
        }


        final ArrayList<String> foo;
        final IterableComparator<Object> arrayComp = new IterableComparator<Object>(pinyinSort);
        final Relation<Collection<String>, String> notFirst = new Relation(new TreeMap(arrayComp), TreeSet.class);
        final Relation<Collection<String>, String> notIn = new Relation(new TreeMap(arrayComp), TreeSet.class);
        final Relation<Collection<String>, String> noTranslit = new Relation(new TreeMap(arrayComp), TreeSet.class);
        for (final String s : unihanPinyin.keySet()) {
            final String pinyin = toPinyin.transform(s);
            final Set<String> unihanPinyinSet = unihanPinyin.getPinyinSet(s);
            if (!pinyin.equals(s)) {
                if (!unihanPinyinSet.contains(pinyin)) {
                    final ArrayList val = new ArrayList();
                    val.add(pinyin);
                    val.addAll(unihanPinyinSet);
                    notIn.put(val,s);
                } else if (!pinyin.equals(unihanPinyinSet.iterator().next())) {
                    final ArrayList val = new ArrayList();
                    val.add(pinyin);
                    val.add(unihanPinyinSet.iterator().next());
                    notFirst.put(val,s);
                }
                continue;
            }
            noTranslit.put(unihanPinyinSet, s);
        }

        System.out.println("Characters with Unihan Pinyin but no CLDR: " + noTranslit.size());
        for (final Collection<String> unihanPinyinSet : noTranslit.keySet()) {
            final Set<String> s = noTranslit.getAll(unihanPinyinSet);
            System.out.println(unihanPinyinSet + "\t" + showSet(s));
        }

        System.out.println("Characters with Unihan Pinyin and CLDR, but CLDR not in Unihan: " + notIn.size());
        for (final Collection<String> unihanPinyinSet : notIn.keySet()) {
            final Set<String> s = notIn.getAll(unihanPinyinSet);
            final ArrayList<String> val = new ArrayList<String>(unihanPinyinSet);
            final String pinyin = val.get(0);
            val.remove(0);
            System.out.println(pinyin + "\t" + val + "\t" + showSet(s));
        }

        System.out.println("Characters with Unihan Pinyin and CLDR, but CLDR not first in Unihan: " + notFirst.size());
        for (final Collection<String> unihanPinyinSet : notFirst.keySet()) {
            final Set<String> s = notFirst.getAll(unihanPinyinSet);
            final ArrayList<String> val = new ArrayList<String>(unihanPinyinSet);
            final String pinyin = val.get(0);
            val.remove(0);
            System.out.println(pinyin + "\t" + val + "\t" + showSet(s));
        }

        String oldPinyin = "";
        HanInfo list = null;

        final int bad = 0;
        final UnicodeSet tailored = pinyinSort.getTailoredSet().retainAll(UNIHAN);

        final UnicodeSet inSortNotUnihan = new UnicodeSet(tailored).removeAll(unihanPinyin.keySet());
        final UnicodeSet inUnihanNotSort = new UnicodeSet(unihanPinyin.keySet()).removeAll(tailored);
        final UnicodeSet inUnihanAndSort = new UnicodeSet(unihanPinyin.keySet()).retainAll(tailored);
        System.out.println("Extras in pinyinSort - Unihan: " + inSortNotUnihan.size() + "\t" + inSortNotUnihan.toPattern(false));
        System.out.println("Extras in Unihan - pinyinSort: " + inUnihanNotSort.size() + "\t" + inUnihanNotSort.toPattern(false));
        System.out.println("In both Unihan and pinyinSort: " + inUnihanAndSort.size());

        final Set<String> sorted1 = new TreeSet<String>(pinyinSort);
        tailored.addAllTo(sorted1);
        final List<HanInfo> buckets = new ArrayList<HanInfo>();

        for (final String s : sorted1) {
            String pinyin = unihanPinyin.getPinyin(s);
            if (pinyin == null) {
                pinyin = "#";
            }
            if (!pinyin.equals(oldPinyin)) {
                list = new HanInfo();
                list.rank = unihanPinyin.getPinyinOrder(pinyin);
                list.pinyin = (pinyin);
                list.hanList.add(s);
                buckets.add(list);
                oldPinyin = pinyin;
            } else {
                list.hanList.add(s);
            }
        }
        System.out.println("$$CLDR Sorting Comparison");

        excludeItems2(buckets, 1024);
        excludeItems2(buckets, 512);
        excludeItems2(buckets, 256);
        excludeItems2(buckets, 128);
        excludeItems2(buckets, 64);
        excludeItems2(buckets, 32);
        excludeItems2(buckets, 16);
        excludeItems2(buckets, 8);

        printItems(buckets, unihanPinyin);

        final Relation<String,String> sorted = new Relation(new TreeMap(pinyinSort), TreeSet.class, radicalStrokeSort);
        for (final String han : unihanPinyin.keySet()) {
            sorted.put(unihanPinyin.getPinyin(han), han);
        }
        final Relation<String,String> sorted2 = new Relation(new TreeMap(), TreeSet.class, radicalStrokeSort);

        final Tabber tabber = new Tabber.HTMLTabber();

        final PrintWriter out = Utility.openPrintWriterGenDir("pinyinTable.html", null);
        final PrintWriter pinyinCollation = Utility.openPrintWriterGenDir("pinyinCollation.txt", null);
        pinyinCollation.println("\uFEFF# Unihan Pinyin Collation\n" +
                "&[last regular]");

        final PrintWriter pinyinCollationInterleaved = Utility.openPrintWriterGenDir("pinyinCollationInterleaved.txt", null);
        pinyinCollationInterleaved.println("\uFEFF# Unihan Pinyin Interleaved Collation\n" +
                "&[last regular]");

        out.println("<html>\n" +
                "<head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table border='1' style='border-collapse:collapse'>");
        for (final String pinyin : sorted.keySet()) {
            final Set<String> hanSet = sorted.getAll(pinyin);
            pinyinCollationInterleaved.print("<" + pinyin + "\t");
            sorted2.clear();
            for (final String han : hanSet) {
                final Map<String, EnumSet<PinyinSource>> pinyinToSource = unihanPinyin.getPinyinMap(han);
                sorted2.put("" + showPinyinToSource(pinyinToSource), han);
                pinyinCollation.print("<" + han);
                pinyinCollationInterleaved.print("<" + han);
            }
            pinyinCollation.println();
            pinyinCollationInterleaved.println();
            for (final String line : sorted2.keySet()) {
                final Set<String> set = sorted2.getAll(line);
                String setStr = set.toString().replace(",", "");
                setStr = setStr.substring(1,setStr.length()-1);
                out.println(tabber.process(pinyin + "\t" + setStr + "\t" + line));
            }
        }
        out.println("</table></body></html>");
        pinyinCollation.close();
        pinyinCollationInterleaved.close();
        out.close();

        System.out.println("\nbad " + bad);
    }

    private static String showPinyinToSource(Map<String, EnumSet<PinyinSource>> pinyinToSource) {
        final StringBuilder buffer = new StringBuilder();
        for (final String pinyin : pinyinToSource.keySet()) {
            if (buffer.length() != 0) {
                buffer.append("\t");
            }
            final EnumSet<PinyinSource> set = pinyinToSource.get(pinyin);
            buffer.append(pinyin).append("\t");
            for (final PinyinSource x : PinyinSource.values()) {
                buffer.append(set.contains(x) ? x : "â€‘");
            }
            buffer.append("");
        }
        return buffer.toString();
    }

    private static void showElements(String indent, Transliterator toPinyin2) {
        System.out.println(indent + toPinyin2.getID() + "\t" + toPinyin2.getClass().getName() + "\tFilter: " + toPinyin2.getFilter() + "\tSource: " + toPinyin2.getSourceSet().toPattern(false));
        final Transliterator[] elements = toPinyin2.getElements();
        for (final Transliterator element : elements) {
            if (element == toPinyin2) {
                continue;
            }
            showElements(indent+"\t", element);
        }
    }

    private static UnicodeSet getSource(Transliterator toPinyin2, UnicodeSet target) {
        target.addAll(toPinyin2.getSourceSet());
        final Transliterator[] elements = toPinyin2.getElements();
        for (final Transliterator element : elements) {
            if (element == toPinyin2) {
                continue;
            }
            getSource(element, target);
        }
        return target;
    }


    private static void excludeItems2(List<HanInfo> buckets, int threshold) {
        for (int i = 0; i < buckets.size(); ++i) {
            final HanInfo row = buckets.get(i);
            if (row.exclude) {
                continue;
            }
            final double localDistance = getLocalDistance(buckets, i);
            if (localDistance > threshold) {
                row.exclude = true;
            }
        }
    }

    private static void printItems(List<HanInfo> buckets, UnihanPinyin unihanPinyin) {
        int bad = 0;
        String bestPinyin = "Ä�";
        for (int i = 0; i < buckets.size(); ++i) {
            final HanInfo row = buckets.get(i);
            final int localDistance = getLocalDistance(buckets, i);
            boolean ok = true;
            if (row.exclude) {
                ok = false;
            } else {
                final int before = findNonexcludedRank(buckets, i, -1);
                final int after = findNonexcludedRank(buckets, i, 1);
                if (before >= 0 && buckets.get(before).rank > row.rank && getLocalDistance(buckets, before) <= localDistance) {
                    ok = false;
                } else if (after >= 0 && buckets.get(after).rank < row.rank && getLocalDistance(buckets, after) <= localDistance) {
                    ok = false;
                }
            }
            if (ok) {
                bestPinyin = row.pinyin;
            }
            final String status = ok ? "" : "??";
            if (!ok) {
                ++bad;
            }
            System.out.println(status + "\t" + localDistance + "\t" + (ok? "": "[" + bestPinyin + "]") + row);
            if (!ok && row.hanList.size() > 1){
                for (final String han : row.hanList) {
                    System.out.println("\t--\t" + han + "\t" + unihanPinyin.getPinyinMap(han));
                }
            }
            for (final String han : row.hanList) {
                unihanPinyin.addAll(han,"?", PinyinSource.s, bestPinyin);
            }
        }
        System.out.println("Bad:\t" + bad);
    }

    private static int findNonexcludedRank(List<HanInfo> buckets, int i, int direction) {
        try {
            while (true) {
                i += direction;
                final HanInfo row = buckets.get(i);
                if (!row.exclude) {
                    return i;
                }
            }
        } catch (final Exception e) {
            return -1;
        }
    }

    static class HanInfo {
        boolean exclude;
        int rank;
        String pinyin;
        List<String> hanList = new ArrayList<String>();
        @Override
        public String toString() {
            return pinyin + " (" + rank + ")\t" + showSet(hanList); // (exclude ? "*" : "") + "\t" +
        }
    }

    private static int getLocalDistance(List<HanInfo> buckets, int i) {
        final HanInfo myBucket = buckets.get(i);
        final int core = myBucket.rank;
        double distance = 0;
        int count = 0;
        for (int j = i-1; j >= 0 && count < 10; --j) {
            final HanInfo bucket = buckets.get(j);
            if (bucket.exclude) {
                continue;
            }
            ++count;
            distance += Math.abs(bucket.rank + count - core);
        }

        int count2 = 0;
        for (int j = i+1; j < buckets.size() && count2 < 10; ++j) {
            final HanInfo bucket = buckets.get(j);
            if (bucket.exclude) {
                continue;
            }
            ++count2;
            distance += Math.abs(bucket.rank - count2 - core);
        }
        return (int) (distance / (count + count2) / myBucket.hanList.size());
    }

    private static String showSet(Iterable<String> set) {
        final StringBuilder buff = new StringBuilder();
        boolean first = true;
        for (final String s : set) {
            if (first) {
                first = false;
            } else {
                buff.append(' ');
            }
            buff.append(s);
        }
        return buff.toString();
    }

    private static final UnicodeSet HAN = new UnicodeSet("[:script=han:]");

    enum PinyinSource {l, x, p, m, t, s};

    static class UnihanPinyin {
        // kHanyuPinyin, space, 10297.260: qÄ«n,qÃ¬n,qÇ�n, [a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]+(,[a-z\x{FC}\x{300}-\x{302}\x{304}\x{308}\x{30C}]
        // kMandarin, space,  [A-Z\x{308}]+[1-5] // 3475=HAN4 JI2 JIE2 ZHA3 ZI2
        // kHanyuPinlu, space, [a-z\x{308}]+[1-5]\([0-9]+\) 4E0A=shang4(12308) shang5(392)

        UnicodeMap<Map<String,EnumSet<PinyinSource>>> unihanPinyin = new UnicodeMap();
        Map<String,Integer> pinyinToOrder = new HashMap();
        TreeSet<String> pinyinSet = new TreeSet<String>(pinyinSort);

        {
            final Transform<String,String> pinyinNumeric = Transliterator.getInstance("NumericPinyin-Latin;nfc");

            // all kHanyuPinlu readings first; then take all kXHC1983; then kHanyuPinyin.

            final UnicodeMap<String> kHanyuPinlu = Default.ucd().getHanValue("kHanyuPinlu");
            for (final String s : kHanyuPinlu.keySet()) {
                final String original = kHanyuPinlu.get(s);
                String source = original.replaceAll("\\([0-9]+\\)", "");
                source = pinyinNumeric.transform(source);
                addAll(s, original, PinyinSource.l, source.split(" "));
            }

            //kXHC1983
            //^[0-9,.*]+:*[a-zx{FC}x{300}x{301}x{304}x{308}x{30C}]+$
            final UnicodeMap<String> kXHC1983 = Default.ucd().getHanValue("kXHC1983");
            for (final String s : kXHC1983.keySet()) {
                final String original = kXHC1983.get(s);
                String source = Normalizer.normalize(original, Normalizer.NFC);
                source = source.replaceAll("([0-9,.*]+:)*", "");
                addAll(s, original, PinyinSource.x, source.split(" "));
            }

            final UnicodeMap<String> kHanyuPinyin = Default.ucd().getHanValue("kHanyuPinyin");
            for (final String s : kHanyuPinyin.keySet()) {
                final String original = kHanyuPinyin.get(s);
                String source = Normalizer.normalize(original, Normalizer.NFC);
                source = source.replaceAll("^\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ""); // , only for medial
                source = source.replaceAll("\\s*(\\d{5}\\.\\d{2}0,)*\\d{5}\\.\\d{2}0:", ",");
                addAll(s, original, PinyinSource.p, source.split(","));
            }

            final UnicodeMap<String> kMandarin = Default.ucd().getHanValue("kMandarin");
            for (final String s : kMandarin.keySet()) {
                final String original = kMandarin.get(s);
                String source = original.toLowerCase();
                source = pinyinNumeric.transform(source);
                addAll(s, original, PinyinSource.m, source.split(" "));
            }

            for (final String s : getSource(toPinyin, new UnicodeSet()).retainAll(HAN)) {
                final String target = toPinyin.transform(s);
                if (!HAN.contains(target)) {
                    addAll(s, "?", PinyinSource.t, target);
                }
            }

            collectPinyin();

            int i = 0;
            for (final String s : pinyinSet) {
                pinyinToOrder.put(s,i++);
            }
            pinyinToOrder.put("#", i + 1000);
            pinyinSet = null;

            for (final String han : keySet()) {
                final Map<String, EnumSet<PinyinSource>> stuff = unihanPinyin.get(han);
                for (final String pinyin : stuff.keySet()) {
                    final EnumSet<PinyinSource> set = stuff.get(pinyin);
                    if (set.size() != 1) {
                        System.out.println("**\t" + han + "\t" + stuff);
                        break;
                    }
                }
            }
        }

        private void collectPinyin() {
            final PrintWriter out = Utility.openPrintWriterGenDir("pinyin/pinyins.txt", null);
            final String[] line = {"", "", "", "", "", "", "", ""};
            final Map<String,Integer> groupToIndex = new HashMap();
            int k = 3;
            for (final String item : "zuÅ� zuÃ³ zuÇ’ zuÃ² zuo".split("\\s+")) {
                groupToIndex.put(accents.transform(item), k++);
            }

            final Set<String> initials = new TreeSet(pinyinSort);
            final Set<String> finals = new TreeSet(pinyinSort);
            final Set<String> accentedFinals = new TreeSet(pinyinSort);
            final BitSet collectedAccents = new BitSet();
            String oldBase = "";
            for (final String s : pinyinSet) {
                final String base = noaccents.transform(s);
                final String group = accents.transform(s);
                if (!base.equals(oldBase)) {
                    out.println(showPinyinLine(line, collectedAccents));
                    for (int j = 1; j < line.length; ++j) {
                        line[j] = "";
                    }
                    line[0] = oldBase = base;
                    final int initialEnd = INITIALS.findIn(base, 0, true);
                    final String initialSegment = base.substring(0,initialEnd);
                    final String finalSegment = base.substring(initialEnd);
                    line[1] = initialSegment;
                    initials.add(initialSegment);
                    line[2] = finalSegment;
                    finals.add(finalSegment);
                    collectedAccents.clear();
                }
                final int initialEnd = INITIALS.findIn(s, 0, true);
                final String finalSegment = s.substring(initialEnd);
                accentedFinals.add(finalSegment);

                try {
                    final int groupIndex = groupToIndex.get(group);
                    collectedAccents.set(groupIndex-3);
                    if (line[groupIndex].length() != 0) {
                        System.out.println("***Multiple pinyins: " + s + "\t" + line[groupIndex]);
                    }
                    line[groupIndex] = s;
                } catch (final Exception e) {
                    System.out.println("***Illegal pinyin: " + s);
                }
            }
            out.println(showPinyinLine(line, collectedAccents));
            out.close();
            System.out.println(initials);
            System.out.println(finals);
            System.out.println(accentedFinals);
        }

        private String showPinyinLine(String[] line, BitSet collectedAccents) {
            return Arrays.asList(line).toString().replaceAll(",\\s*","\t").replaceAll("\\[|\\]", "")
                    + "\t" + collectedAccents.cardinality() + "\t" + collectedAccents;
        }

        public Integer getPinyinOrder(String pinyin) {
            Integer result = pinyinToOrder.get(pinyin);
            if (result == null) {
                result = pinyinToOrder.get("#");
            }
            return result;
        }

        static Transform<String,String> noaccents = Transliterator.getInstance("nfkd; [[:m:]-[\u0308]] remove; nfc");
        static Transform<String,String> accents = Transliterator.getInstance("nfkd; [^[:m:]-[\u0308]] remove; nfc");

        static UnicodeSet INITIALS = new UnicodeSet("[b c {ch} d f g h j k l m n p q r s {sh} t w x y z {zh}]").freeze();
        static UnicodeSet FINALS = new UnicodeSet("[a {ai} {an} {ang} {ao} e {ei} {en} {eng} {er} i {ia} {ian} {iang} {iao} {ie} {in} {ing} {iong} {iu} o {ong} {ou} u {ua} {uai} {uan} {uang} {ue} {ui} {un} {uo} Ã¼ {Ã¼e}]").freeze();

        boolean validPinyin(String pinyin) {
            final String base = noaccents.transform(pinyin);
            final int initialEnd = INITIALS.findIn(base, 0, true);
            if (initialEnd == 0 && (pinyin.startsWith("i") || pinyin.startsWith("u"))) {
                return false;
            }
            final String finalSegment = base.substring(initialEnd);
            final boolean result = finalSegment.length() == 0 ? true : FINALS.contains(finalSegment);
            return result;
        }

        void addAll(String han, String original, PinyinSource pinyin, String... pinyinList) {
            // ð ®½
            if (pinyinList.length == 0) {
                throw new IllegalArgumentException();
            }
            final Map<String, EnumSet<PinyinSource>> pinyinToSources = getPinyinToSources(han, true);
            for (final String source : pinyinList) {
                if (source.length() == 0) {
                    throw new IllegalArgumentException();
                }
                if (!validPinyin(source)) {
                    System.out.println("***Invalid Pinyin: " + han + "\t" + pinyin + "\t" + source + "\t" + Utility.hex(han) + "\t" + original);
                }
                if (pinyinSet != null) {
                    pinyinSet.add(source);
                }
                final EnumSet<PinyinSource> enumSet = getEnumSet(pinyinToSources, source, true);
                if (pinyin == PinyinSource.t && enumSet.contains(PinyinSource.m)) {
                    continue;
                }
                enumSet.add(pinyin);
            }
            if (pinyinToSources.size() == 0) {
                throw new IllegalArgumentException();
            }
        }
        private EnumSet<PinyinSource> getEnumSet(Map<String, EnumSet<PinyinSource>> pinyinToSources, String pinyin, boolean createSet) {
            EnumSet<PinyinSource> set = pinyinToSources.get(pinyin);
            if (createSet && set == null) {
                set = EnumSet.noneOf(PinyinSource.class);
                pinyinToSources.put(pinyin, set);
            }
            return set;
        }

        private Map<String,EnumSet<PinyinSource>> getPinyinToSources(String han, boolean createSet) {
            Map<String,EnumSet<PinyinSource>> set = unihanPinyin.get(han);
            if (createSet && set == null) {
                set = new LinkedHashMap<String,EnumSet<PinyinSource>>();
                unihanPinyin.put(han, set);
            }
            return set;
        }

        public UnicodeSet keySet() {
            return unihanPinyin.keySet();
        }

        private Set<String> getPinyinSet(String han) {
            final Map<String, EnumSet<PinyinSource>> values = unihanPinyin.get(han);
            if (values == null) {
                return null;
            }
            return values.keySet();
        }

        private Map<String, EnumSet<PinyinSource>> getPinyinMap(String han) {
            return unihanPinyin.get(han);
        }

        private String getPinyin(String han) {
            final Set<String> set = getPinyinSet(han);
            if (set == null) {
                return han;
            }
            return set.iterator().next();
        }

    }


}
