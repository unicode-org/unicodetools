package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.ComparisonNormalizer;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Common;
import org.unicode.tools.IdsFileData;
import org.unicode.tools.RadicalEnum;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateRadicals {
    public static final Normalizer2 NFKC = Normalizer2.getNFKCInstance();
    public static final Normalizer2 NFC = Normalizer2.getNFKCInstance();
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);

    interface Collector<T,S> {
        void add(T item);
        S get();
    }

    static class StringCollector implements Collector<String,String> {
        private StringBuilder b = new StringBuilder();
        private String separator;

        StringCollector(String separator) {
            this.separator = separator;
        }
        @Override
        public void add(String item) {
            if (b.length() != 0) {
                b.append(separator);
            }
            b.append(item);
        }

        @Override
        public String get() {
            String result = b.toString();
            b.setLength(0);
            return result;
        }        
    }

    public static <T,S> S transform(String s, UnicodeMap<T> map, Collector<T,S> collector) {
        for (int cp : CharSequences.codePoints(s)) {
            T value = map.get(cp);
            collector.add(value);
        }
        return collector.get();
    }

    static class Data extends R5<RadicalEnum, Integer, String, String, String> {

        public Data(RadicalEnum a, Integer b, String c, String d, String e) {
            super(a, b, c, d, e);
        }
    }

    public static void main(String[] args) {
        UnicodeMap<String> names = iup.load(UcdProperty.Name);
        UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeMap<String> cjkRadicals = iup.load(UcdProperty.CJK_Radical);
        UnicodeSet radical = iup.loadEnum(UcdProperty.Radical, Binary.class).getSet(Binary.Yes);
        UnicodeMap<Block_Values> blocks = iup.loadEnum(UcdProperty.Block, UcdPropertyValues.Block_Values.class);
        UnicodeSet interest = new UnicodeSet()
        .addAll(blocks.getSet(Block_Values.CJK_Strokes))
        .removeAll(gc.getSet(General_Category_Values.Unassigned)
                );

        Relation<RadicalEnum,String> cjkRadicalToIdeograph = Relation.of(new HashMap(), LinkedHashSet.class);
        for (Entry<String, String> entry : cjkRadicals.entrySet()) {
            cjkRadicalToIdeograph.put(RadicalEnum.fromString(entry.getValue()), entry.getKey());
        }
        Map<String,String> radicalToUnified = new HashMap<>();
        for (Entry<RadicalEnum, Set<String>> entry : cjkRadicalToIdeograph.keyValuesSet()) {
            List<String> values = new ArrayList<>(entry.getValue());
            String item0 = values.get(0);
            String item1 = values.get(1);
            if (radical.contains(item0)) {
                radicalToUnified.put(item0, item1);
            } else {
                radicalToUnified.put(item1, item0);
            }
        }

        StringCollector sc = new StringCollector("+");
        ComparisonNormalizer cnorm = ComparisonNormalizer.getSimple();


        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setNumericCollation(true);
        col.freeze();

        Set<Data> sorted = new TreeSet<>();

        for (String cp : radical) {
            Set<RadicalEnum> radicals = new LinkedHashSet<>();
            String rad = cjkRadicals.get(cp);
            if (rad != null) {
                radicals = Collections.singleton(RadicalEnum.fromString(rad));
            } else {
                final Set<Integer> otherSet = IdsFileData.cjkRadToRad.get(cp);
                if (otherSet != null) {
                    for (int x : otherSet) {
                        radicals.add(RadicalEnum.fromInt(x));
                    }
                }
            }

            for (RadicalEnum radical1 : radicals) {
                Relation<String,String> otherToCause = Relation.of(new HashMap(), LinkedHashSet.class);
                addOther(cp, radicalToUnified.get(cp), "CJKRadicals.txt", otherToCause);
                addOther(cp, NFKC.normalize(cp), "NFKC", otherToCause);
                addOther(cp, cnorm.get(cp), "UCA", otherToCause);
                addOther(cp, IdsFileData.cjkRadSupToIdeo.get(cp), "NamesList", otherToCause);

                if (otherToCause.isEmpty()) {
                    sorted.add(new Data(radical1, cp.charAt(0) >= 0x2F00 ? 0 : 1, cp, "", ""));

                } else {
                    for (Entry<String, Set<String>> entry : otherToCause.keyValuesSet()) {
                        sorted.add(new Data(radical1, cp.charAt(0) >= 0x2F00 ? 0 : 1, cp, entry.getKey(), Common.COMMA_JOINER.join(entry.getValue())));
                    }
                }
            }
        }

        RadicalEnum last = null;
        Set<String> soFar = new HashSet<>();
        for (Data entry : sorted) {
            RadicalEnum radNum = entry.get0();
            if (radNum != last) {
                if (last != null) {
                    doFinal(last, soFar);
                    soFar.clear();
                    System.out.println();
                }
                last = radNum;
            }
            String cp = entry.get2();
            String other = entry.get3();
            soFar.add(other);
            String cause = entry.get4();
            
            System.out.println(Utility.hex(cp) + ";"
                    + "\t" + Common.COMMA_JOINER.join(getTotalStrokes(other)) + ";" 
                    + "\t" + radNum + ";" 
                    + "\t" + Utility.hex(other)
                    + ";\t#\t" + cp + "\t→\t" + other
                    + "\t" + names.get(cp)
                    + ";\t" + cause
                    );
        }
        doFinal(last, soFar);
    }

    private static List<Integer> getTotalStrokes(String other) {
        List<Integer> totalStrokes = CldrUtility.ifNull(
                other.isEmpty() ? null : IdsFileData.TOTAL_STROKES.get(other), 
                        Collections.singletonList(0));
        return totalStrokes;
    }

    private static void doFinal(RadicalEnum radical1, Set<String> soFar) {
        Set<String> unicodeItems1 = normalizeSet(new LinkedHashSet<>(CldrUtility.ifNull(UNICODE_RADICALS.get(radical1), Collections.<String>emptySet())));
        unicodeItems1.removeAll(soFar);
        
        Set<String> unicodeItems = unicodeItems1;
        showOthers(radical1, unicodeItems, "kRSUnicode");

        Set<String> adobeItems = normalizeSet(new LinkedHashSet<>(CldrUtility.ifNull(ADOBE_RADICALS.get(radical1), Collections.<String>emptySet())));
        adobeItems.removeAll(soFar);
        adobeItems.removeAll(unicodeItems);
        showOthers(radical1, adobeItems, "kRSAdobe");
    }

    private static void showOthers(RadicalEnum radical1, Set<String> unicodeItems, String cause) {
        for (String other : unicodeItems) {
            System.out.println("#?   "
                    + "\t" + Common.COMMA_JOINER.join(getTotalStrokes(other)) + ";" 
                    + "\t" + radical1 + ";" 
                    + "\t" + Utility.hex(other)
                    + ";\t#\t" + "?" + "\t→\t" + other
                    + "\t" + "?"
                    + ";\t" + cause
                    );
        }
    }

    private static Set<String> normalizeSet(LinkedHashSet<String> linkedHashSet) {
        Set<String> result = new LinkedHashSet<>();
        for (String s : linkedHashSet) {
            result.add(NFC.normalize(s));
        }
        return result;
    }

    private static String show(Set<String> unicodeItems) {
        StringBuilder b = new StringBuilder();
        for (String s : unicodeItems) {
            if (b.length() != 0) {
                b.append(", ");
            }
            List<Integer> totalStrokes = CldrUtility.ifNull(IdsFileData.TOTAL_STROKES.get(s), Collections.singletonList(0));

            b.append(Utility.hex(s) 
                    + " (" + s
                    + "/" + Common.COMMA_JOINER.join(totalStrokes) + ")");
        };
        return b.toString();
    }

    private static void addOther(String cp, Set<String> mapped, String string, Relation<String, String> otherToCause) {
        if (mapped != null) {
            for (String other : mapped) {
                addOther(cp, other, string, otherToCause);
            }
        }
    }

    private static String addOther(String cp, String mapped, String cause, Relation<String, String> otherToCause) {
        String other = mapped;
        if (other == null) {
            return other;
        }
        other = NFC.normalize(other);
        if (!cp.equals(other)) {
            otherToCause.put(other,cause);
        }
        return other;
    }

    static final Relation<RadicalEnum, String> ADOBE_RADICALS = Relation.of(new HashMap(), TreeSet.class);
    static final Relation<RadicalEnum, String> UNICODE_RADICALS = Relation.of(new HashMap(), TreeSet.class);
    static {
        Matcher m = Common.ADOBE_RS_MATCHER.matcher("");
        UnicodeMap<Set<String>> adobeRadicalStroke = iup.loadSet(UcdProperty.kRSAdobe_Japan1_6);
        for (Entry<String, Set<String>> entry : adobeRadicalStroke.entrySet()) {
            String source = entry.getKey();
            for (String s : entry.getValue()) {
                if (!m.reset(s).matches()) {
                    throw new IllegalArgumentException();
                }
                int remaining = Integer.parseInt(m.group(3));
                if (remaining == 0) {
                    ADOBE_RADICALS.put(RadicalEnum.fromString(m.group(1)), source);
                }
            }
        }
        ADOBE_RADICALS.freeze();

        UnicodeMap<List<String>> radicalStroke = iup.loadList(UcdProperty.kRSUnicode);
        for (Entry<String, List<String>> entry : radicalStroke.entrySet()) {
            List<String> items = entry.getValue();
            List<String> parts = Common.DOT_SPLITTER.splitToList(items.get(0));
            final int remStrokes = Integer.parseInt(parts.get(1));
            if (remStrokes == 0) {
                UNICODE_RADICALS.put(RadicalEnum.fromString(parts.get(0)), entry.getKey());
            }
        }
        UNICODE_RADICALS.freeze();
    }
}
