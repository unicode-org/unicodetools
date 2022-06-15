package org.unicode.tools;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
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
import org.unicode.cldr.util.With;

public class CollatorEquivalences {
    private static final Normalizer2 nfc = Normalizer2.getNFCInstance();
    private static final Normalizer2 nfkccf = Normalizer2.getNFKCCasefoldInstance();

    public static void main(String[] args) throws IOException {
        showMappings(equiv);
        // System.out.println(remapped.toPattern(false));
    }

    static final class RawKey implements Comparable<RawKey> {
        final List<Integer> key;

        public RawKey(Integer single) {
            key = Collections.singletonList(single);
        }

        public RawKey(RuleBasedCollator uca, String s) {
            String norm = nfkccf.normalize(s);
            List<Integer> key = new ArrayList<>();
            CollationElementIterator it = uca.getCollationElementIterator(norm);
            for (int ce = it.next(); ce != CollationElementIterator.NULLORDER; ce = it.next()) {
                int cePS = ce >>> 8; // only primary/secondary differences
                if (cePS != 0) {
                    key.add(cePS);
                }
            }
            this.key = key;
        }

        public boolean isPrefixOf(RawKey first) {
            throw new IllegalArgumentException();
        }

        public int size() {
            return key.size();
        }

        @Override
        public int compareTo(RawKey o) {
            Iterator<Integer> it1 = key.iterator();
            Iterator<Integer> it2 = o.key.iterator();
            while (true) {
                if (!it1.hasNext()) {
                    return !it2.hasNext() ? 0 : -1;
                } else if (!it2.hasNext()) {
                    return 1;
                }
                int ce1 = it1.next();
                int ce2 = it2.next();
                if (ce1 != ce2) {
                    return ce1 > ce2 ? 1 : -1;
                }
            }
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return key.equals(((RawKey) obj).key);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("[");
            for (int i : key) {
                if (b.length() > 1) {
                    b.append(", ");
                }
                b.append(Integer.toHexString(i).toUpperCase(Locale.ROOT));
            }
            return b.append("]").toString();
        }
    }

    static final RuleBasedCollator UCA_SECONDARY_ONLY;
    static final Comparator<String> BEST_IS_LEAST;
    static final UnicodeSet LETTER = new UnicodeSet("[:L:]").freeze();
    static final UnicodeSet KATAKANA = new UnicodeSet("[:sc=Katakana:]").freeze();
    static final UnicodeSet KATAKANA_SMALL = new UnicodeSet("[ァィゥェォッャュョヮヵヶㇰ-ㇿｧ-ｯ]").freeze();
    static final UnicodeSet ChangesWithNfkccf =
            new UnicodeSet("[:Changes_When_NFKC_Casefolded:]").freeze();

    static {
        RuleBasedCollator temp = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        temp.setStrength(Collator.SECONDARY);
        temp.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        temp.freeze();
        UCA_SECONDARY_ONLY = temp;

        Comparator<String> LONGER_FIRST =
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.equals(o2)
                                ? 0
                                : o1.isEmpty()
                                        ? -1
                                        : o2.isEmpty()
                                                ? 1
                                                : o2.codePointCount(0, o2.length())
                                                        - o1.codePointCount(0, o1.length());
                    }
                };
        Comparator<String> REGULAR_FIRST =
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return (LETTER.containsAll(o1) ? 0 : 1) - (LETTER.containsAll(o2) ? 0 : 1);
                    }
                };

        Comparator<String> NFKCCF_FIRST =
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return (ChangesWithNfkccf.containsNone(o1) ? 0 : 1)
                                - (ChangesWithNfkccf.containsNone(o2) ? 0 : 1);
                    }
                };

        Comparator<String> KANA_FIRST =
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int order1 =
                                (KATAKANA_SMALL.containsAll(o1)
                                        ? 1
                                        : KATAKANA.containsAll(o1) ? 0 : 2);
                        int order2 =
                                (KATAKANA_SMALL.containsAll(o2)
                                        ? 1
                                        : KATAKANA.containsAll(o2) ? 0 : 2);
                        if (order1 != order2) {
                            int debug = 0;
                        }
                        return order1 - order2;
                    }
                };

        RuleBasedCollator temp2 = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        temp2.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        temp2.freeze();
        Comparator<String> CODEPOINT =
                new StringComparator(true, false, StringComparator.FOLD_CASE_DEFAULT);
        BEST_IS_LEAST =
                new MultiComparator<String>(
                        LONGER_FIRST,
                        KANA_FIRST,
                        // NFKCCF_FIRST,
                        (Comparator<String>) (Comparator<?>) temp2,
                        REGULAR_FIRST,
                        CODEPOINT);
    }

    public static UnicodeMap<String> COLLATION_MAP = new UnicodeMap();
    static Relation<RawKey, String> equiv =
            Relation.of(new TreeMap<RawKey, Set<String>>(), TreeSet.class, BEST_IS_LEAST);
    static Set<RawKey> failed = new LinkedHashSet<>();

    static {
        for (int i = 0; i <= 0x10FFFF; ++i) {
            int gc = UCharacter.getIntPropertyValue(i, UProperty.GENERAL_CATEGORY);
            if (gc == UCharacter.UNASSIGNED
                    || gc == UCharacter.PRIVATE_USE
                    || gc == UCharacter.SURROGATE) {
                continue;
            }
            String s = UTF16.valueOf(i);
            equiv.put(new RawKey(UCA_SECONDARY_ONLY, s), s);
            String t = nfkccf.normalize(s);
            equiv.put(new RawKey(UCA_SECONDARY_ONLY, t), t);
        }
        // Find combinations
        StringBuilder b = new StringBuilder();
        Set<String> moreStrings = new HashSet<>();
        combos:
        for (RawKey rawKey : equiv.keySet()) {
            if (rawKey.size() < 2) continue;
            b.setLength(0);
            for (Integer temp : rawKey.key) {
                RawKey singleKey = new RawKey(temp);
                Set<String> items = equiv.get(singleKey);
                if (items == null) {
                    failed.add(singleKey);
                    // System.out.println("Failed to map " + rawKey + "\t" + equiv.get(rawKey));
                    continue combos;
                }
                b.append(items.iterator().next());
            }
            moreStrings.add(nfc.normalize(b.toString()));
        }

        // we found something, try adding
        // might be redundant, but we don't care.
        for (String s : moreStrings) {
            equiv.put(new RawKey(UCA_SECONDARY_ONLY, s), s);
            String t = nfkccf.normalize(s);
            equiv.put(new RawKey(UCA_SECONDARY_ONLY, t), t);
        }

        for (Entry<RawKey, Set<String>> entry : equiv.keyValuesSet()) {
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

    private static void showMappings(Relation<RawKey, String> equiv) {
        UnicodeSet remapped = new UnicodeSet();
        System.out.println(
                "Source Hex\tTarget Hex\t(Source→Target)\tCollation Key (P/S)\tSource Name → Target Name");
        for (Entry<RawKey, Set<String>> entry : equiv.keyValuesSet()) {
            RawKey rawKey = entry.getKey();
            Set<String> equivalentItems = entry.getValue();
            if (equivalentItems.size() > 1) {
                showItems(rawKey, equivalentItems, remapped);
            }
        }
    }

    //  private static final UnicodeSet DIGIT = new UnicodeSet("[0-9]").freeze();
    //  private static final UnicodeSet NSM = new UnicodeSet("[[:Mn:][:Me:]]").freeze();
    //  private static final UnicodeSet COMMON = new UnicodeSet("[[:scx=Common:]-[:Block=Counting
    // Rod Numerals:]]").freeze();
    //  private static final UnicodeSet SKIP = new UnicodeSet("[\\u0C01\\u0020
    // ः\u20DD\u0982\\p{Block=Musical Symbols}"
    //      + "[:sc=Hiragana:]"
    //      + "[:sc=Katakana:]"
    //      + "]").freeze();

    static String showItems(RawKey rawKey, Set<String> equivalentItems, UnicodeSet remapped) {
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

    private static void showMapping(RawKey rawKey, String source, String target) {
        System.out.println(
                Utility.hex(source)
                        + " ;\t"
                        + Utility.hex(target, 4, " ")
                        + " #\t("
                        + source
                        + "→"
                        + target
                        + ")\t"
                        + rawKey
                        + "\t"
                        + getName(source, " + ")
                        + " → "
                        + getName(target, " + "));
    }

    private static String getName(String best, String separator) {
        StringBuilder b = new StringBuilder();
        for (int cp : With.codePointArray(best)) {
            if (b.length() > 0) {
                b.append(separator);
            }
            b.append(UCharacter.getExtendedName(cp));
        }
        return b.toString();
    }

    static final UnicodeSet NOT_BEST = new UnicodeSet("[:Block=CJK_Radicals_Supplement:]").freeze();

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
