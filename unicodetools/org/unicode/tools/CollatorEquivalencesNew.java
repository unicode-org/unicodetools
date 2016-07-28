package org.unicode.tools;

import java.io.IOException;
import java.rmi.server.Skeleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.MultiComparator;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.UCA.UCA;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
//import com.ibm.icu.text.CollationElementIterator;
//import com.ibm.icu.text.Collator;
//import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CollatorEquivalencesNew {
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Default.ucdVersion());
    static UnicodeSet.XSymbolTable NO_PROPS = new UnicodeSet.XSymbolTable() {
        @Override
        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            throw new IllegalArgumentException("Don't use any ICU Unicode Properties! " + propertyName + "=" + propertyValue);
        };
    };
    static {
        UnicodeSet.setDefaultXSymbolTable(NO_PROPS);
    }
    static interface BaseCollatorKey extends Comparable<BaseCollatorKey> {

    }
    static interface BaseCollator extends Comparator<String> {
        BaseCollatorKey getSortKey(String c);
    }

    private static final UnicodeMap<Script_Values> SC = iup.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final UnicodeMap<General_Category_Values> GC = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    private static final UnicodeSet CN_CS_CO = PropertyValueSets.getSet(GC,
            General_Category_Values.Unassigned,
            General_Category_Values.Surrogate,
            General_Category_Values.Private_Use);
    static final UnicodeSet NOT_BEST = iup.loadEnumSet(UcdProperty.Block, Block_Values.CJK_Radicals_Supplement);
    static final UnicodeSet LETTER = PropertyValueSets.getSet(GC, PropertyValueSets.LETTER);
    static final UnicodeSet KATAKANA = SC.getSet(Script_Values.Katakana);
    static final UnicodeSet KATAKANA_SMALL = new UnicodeSet("[ァィゥェォッャュョヮヵヶㇰ-ㇿｧ-ｯ]").freeze();

    private static final Normalizer nfc = Default.nfc();
    private static final Normalizer3 nfkccf = Normalizer3.NFKCCF;

    public static void main(String[] args) throws IOException {
        showMappings(equiv);
        //System.out.println(remapped.toPattern(false));
    }

    private static final org.unicode.text.UCA.UCA uca_raw = org.unicode.text.UCA.UCA.buildCollator(null);
    private static final org.unicode.text.UCA.UCA uca_level2Only = org.unicode.text.UCA.UCA.buildCollator(null);
    static {
        uca_level2Only.setStrength(2);
    }

    static final BaseCollator MyCollator = new BaseCollator() {
        final class MyBaseCollatorKey implements BaseCollatorKey {
            final String sortKey;
            MyBaseCollatorKey(String a) {
                sortKey = uca_level2Only.getSortKey(a);
            }
            @Override
            public int compareTo(BaseCollatorKey o) {
                return sortKey.compareTo(((MyBaseCollatorKey) o).sortKey);
            }
        }
        @Override
        public int compare(String a, String b) {
            return getSortKey(a).compareTo(getSortKey(b));
        }

        @Override
        public BaseCollatorKey getSortKey(String c) {
            return new MyBaseCollatorKey(c);
        }
    };

    //    static final class RawKey implements BaseCollatorKey {
    //        final List<Integer> key;
    //
    //        public RawKey(Integer single) {
    //            key = Collections.singletonList(single);
    //        }
    //
    //        public RawKey(UCA uca, String s) {
    //            String norm = nfkccf.normalize(s);
    //            List<Integer> key = new ArrayList<>();
    //            CollationElementIterator it = uca.getCollationElementIterator(norm);
    //            for (int ce = it.next(); ce != CollationElementIterator.NULLORDER; ce = it.next()) {
    //                int cePS = ce >>> 8; // only primary/secondary differences
    //                if (cePS != 0) {
    //                    key.add(cePS);
    //                }
    //            }
    //            this.key = key;
    //        }
    //
    //        public boolean isPrefixOf(RawKey first) {
    //            throw new IllegalArgumentException();
    //        }
    //
    //        public int size() {
    //            return key.size();
    //        }
    //
    //        @Override
    //        public int compareTo(RawKey o) {
    //            Iterator<Integer> it1 = key.iterator();
    //            Iterator<Integer> it2 = o.key.iterator();
    //            while (true) {
    //                if (!it1.hasNext()) {
    //                    return !it2.hasNext() ? 0 : -1;
    //                } else if (!it2.hasNext()) {
    //                    return 1;
    //                }
    //                int ce1 = it1.next();
    //                int ce2 = it2.next();
    //                if (ce1 != ce2) {
    //                    return ce1 > ce2 ? 1 : -1;
    //                }
    //            }
    //        }
    //        @Override
    //        public int hashCode() {
    //            return key.hashCode();
    //        }
    //        @Override
    //        public boolean equals(Object obj) {
    //            return key.equals(((RawKey)obj).key);
    //        }
    //        @Override
    //        public String toString() {
    //            StringBuilder b = new StringBuilder("[");
    //            for (int i : key) {
    //                if (b.length() > 1) {
    //                    b.append(", ");
    //                }
    //                b.append(Integer.toHexString(i).toUpperCase(Locale.ROOT));
    //            }
    //            return b.append("]").toString();
    //        }
    //    }

    static final UCA UCA_SECONDARY_ONLY = uca_level2Only;
    static final Comparator<String> BEST_IS_LEAST;

    static {
        Comparator<String> LONGER_FIRST = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.equals(o2) ? 0
                        : o1.isEmpty() ? -1
                                :o2.isEmpty() ? 1
                                        : o2.codePointCount(0, o2.length()) - o1.codePointCount(0, o1.length());
            }
        };
        Comparator<String> REGULAR_FIRST = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (LETTER.containsAll(o1) ? 0 : 1) - (LETTER.containsAll(o2) ? 0 : 1);
            }
        };
        Comparator<String> KANA_FIRST = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int order1 = (KATAKANA_SMALL.containsAll(o1) ? 1 : KATAKANA.containsAll(o1) ? 0 : 2);
                int order2 = (KATAKANA_SMALL.containsAll(o2) ? 1 : KATAKANA.containsAll(o2) ? 0 : 2);
                if (order1 != order2) {
                    int debug = 0;
                }
                return order1 - order2;
            }
        };

        Comparator<String> CODEPOINT = new StringComparator(true, false, StringComparator.FOLD_CASE_DEFAULT);
        BEST_IS_LEAST = new MultiComparator<String>(
                LONGER_FIRST, 
                KANA_FIRST,
                uca_raw, 
                REGULAR_FIRST, 
                CODEPOINT);
    }

    public static UnicodeMap<String> COLLATION_MAP = new UnicodeMap<>();
    static Relation<BaseCollatorKey, String> equiv = Relation.of(new TreeMap<BaseCollatorKey, Set<String>>(), TreeSet.class, BEST_IS_LEAST);
    static Set<BaseCollatorKey> failed = new LinkedHashSet<>();

    static  {
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (CN_CS_CO.contains(i)) {
                continue;
            }
            String s = UTF16.valueOf(i);
            equiv.put(MyCollator.getSortKey(s), s);
            String t = nfkccf.normalize(s);
            equiv.put(MyCollator.getSortKey(t), t);
        }
        // Find combinations
        StringBuilder b = new StringBuilder();
        Set<String> moreStrings = new HashSet<>();
        combos:
            for (BaseCollatorKey rawKey : equiv.keySet()) {
                //                if (rawKey.size() < 2) continue;
                b.setLength(0);
                // TODO
//                for (Integer temp : rawKey.key){
//                    RawKey singleKey = new RawKey(temp);
//                    Set<String> items = equiv.get(singleKey);
//                    if (items == null) {
//                        failed.add(singleKey);
//                        //System.out.println("Failed to map " + rawKey + "\t" + equiv.get(rawKey));
//                        continue combos;
//                    }
//                    b.append(items.iterator().next());
//                }
                moreStrings.add(nfc.normalize(b.toString()));
            }

        // we found something, try adding
        // might be redundant, but we don't care.
        for (String s : moreStrings) {
            equiv.put(MyCollator.getSortKey(s), s);
            String t = nfkccf.normalize(s);
            equiv.put(MyCollator.getSortKey(t), t);
        }

        for (Entry<BaseCollatorKey, Set<String>> entry : equiv.keyValuesSet()) {
            Set<String> equivalentItems = entry.getValue();
            if (equivalentItems.size() > 1) {
                String target = null;
                for (String s : equivalentItems) {
                    if (target == null) {
                        target = s;
                    } else if (!s.isEmpty()) {
                        COLLATION_MAP.put(s, target);
                    }
                }
            }
        }

        COLLATION_MAP.freeze();
    }

    private static void showMappings(Relation<BaseCollatorKey, String> equiv) {
        UnicodeSet remapped = new UnicodeSet();
        System.out.println("Source Hex\tTarget Hex\t(Source→Target)\tCollation Key (P/S)\tSource Name → Target Name");
        for (Entry<BaseCollatorKey, Set<String>> entry : equiv.keyValuesSet()) {
            BaseCollatorKey rawKey = entry.getKey();
            Set<String> equivalentItems = entry.getValue();
            if (equivalentItems.size() > 1) {
                showItems(rawKey, equivalentItems, remapped);
            }
        }
    }

    //  private static final UnicodeSet DIGIT = new UnicodeSet("[0-9]").freeze();
    //  private static final UnicodeSet NSM = new UnicodeSet("[[:Mn:][:Me:]]").freeze();
    //  private static final UnicodeSet COMMON = new UnicodeSet("[[:scx=Common:]-[:Block=Counting Rod Numerals:]]").freeze();
    //  private static final UnicodeSet SKIP = new UnicodeSet("[\\u0C01\\u0020 ः\u20DD\u0982\\p{Block=Musical Symbols}"
    //      + "[:sc=Hiragana:]"
    //      + "[:sc=Katakana:]"
    //      + "]").freeze();

    static String showItems(BaseCollatorKey rawKey, Set<String> equivalentItems, UnicodeSet remapped) {
        if (equivalentItems.size() > 1) {
            String best = getBest(equivalentItems);
            for (String s : equivalentItems) {
                if (s.equals(best) || s.codePointCount(0, s.length()) > 1) continue;
                String norm = nfkccf.normalize(s);
                if (norm.equals(best)) {
                    continue; // same as nfkccf, skip
                }
                remapped.add(s);
                showMapping(rawKey, s, best);
            }
            return best;
        }
        return null;
    }

    private static void showMapping(BaseCollatorKey rawKey, String source, String target) {
        System.out.println(Utility.hex(source) + " ;\t" + Utility.hex(target,4," ") 
                + " #\t(" + source + "→" + target + ")\t"
                + rawKey + "\t"
                + iup.getName(source, " + ") + " → " + iup.getName(target, " + "));
    }

    private static String getBest(Set<String> fixed) {
        if (fixed.contains("")) {
            return "";
        }
        if (fixed.contains("ꜵ")) {
            String last = "";
            for (String s : fixed) {
                BEST_IS_LEAST.compare(last, s);
                last = s;
            }
        }
        return fixed.iterator().next();
    }

}
