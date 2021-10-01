package org.unicode.text.utility;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.MultiComparator;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ComparisonNormalizer {
    public static final Normalizer2 nfkc_cf = Normalizer2.getNFKCCasefoldInstance();
    private UnicodeMap<String> map = new UnicodeMap<>();

    public <T> ComparisonNormalizer(Transform<String,T> transform, Comparator<String> bestFirst) {
        UnicodeSet chars = new UnicodeSet("[^[:Cn:][:Co:][:Cs:]]");
        Relation<T,String> mapping = Relation.of(new HashMap<T,Set<String>>(), TreeSet.class, bestFirst);
        for (String cp : chars) {
            mapping.put(transform.transform(cp), cp);
            String cpN = nfkc_cf.normalize(cp);
            if (!cp.equals(cpN)) {
                mapping.put(transform.transform(cpN), cpN);
            }
        }

        // TODO we should do this for all strings that might have an equivalent
        mapping.put(transform.transform("ss"), "ss");

        // we have collected the items, now make a mapping
        // drop the ones that are NFKC_CF equivalents
        for (Entry<T, Set<String>> entry : mapping.keyValuesSet()) {
            String target = null;
            String targetN = null;
            for (String value : entry.getValue()) {
                if (target == null) {
                    target = value;
                    targetN = nfkc_cf.normalize(target);
                } else {
                    String valueN = nfkc_cf.normalize(value);
                    if (!valueN.equals(targetN)) {
                        map.put(value, target);
                    }
                }
            }
        }
        map.freeze();
    }

    public static class CEList {
        int[] items = new int[20];
        int size = 0;
        public CEList(CollationElementIterator it, String text) {
            it.setText(text);
            for (int ce = it.next(); ce != CollationElementIterator.NULLORDER; ce = it.next()) {
                int primary = CollationElementIterator.primaryOrder(ce);
                int secondary = CollationElementIterator.secondaryOrder(ce);
                items[size++] = (primary << 8) + secondary;
            }
        }
    }

    static final UnicodeSet secondaryData = new UnicodeSet("[:radical:]").freeze();

    public static class RawCollationKeyTransform implements Transform<String, RawCollationKey> {
        private final RuleBasedCollator uca_raw;
        public RawCollationKeyTransform(RuleBasedCollator uca_raw) {
            this.uca_raw = uca_raw;
        }
        @Override
        public RawCollationKey transform(String source) {
            RawCollationKey result = uca_raw.getRawCollationKey(source, new RawCollationKey());
            if (result.size != result.bytes.length) {
                int size = result.size;
                byte[] bytes = result.releaseBytes();
                result.append(bytes, 0, size); // make the capacity = size, to work around hashCode bug.
            }
            return result;
        }

    }

    public static final Comparator<String> CODEPOINT = new StringComparator(true, false, StringComparator.FOLD_CASE_DEFAULT);
    public static final RuleBasedCollator UCA_SECONDARY_DECOMPOSING = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
    static {
        UCA_SECONDARY_DECOMPOSING.setStrength(Collator.SECONDARY);
        UCA_SECONDARY_DECOMPOSING.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        UCA_SECONDARY_DECOMPOSING.freeze();
    }



    public static ComparisonNormalizer getSimple() {
        Comparator<String> combiningLast = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean c1 = !o1.isEmpty() && isCombining(o1.codePointAt(0));
                boolean c2 = !o2.isEmpty() && isCombining(o2.codePointAt(0));
                return c1 == c2 ? 0 
                        : c1 ? 1 : -1; // is NOT combining is better
            }

            private boolean isCombining(int cp) {
                int t1 = UCharacter.getType(cp);
                return t1 == UCharacter.NON_SPACING_MARK || t1 == UCharacter.ENCLOSING_MARK || t1 == UCharacter.COMBINING_SPACING_MARK;
            }
        };

        Comparator<String> isNFKC_CF = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean c1 = nfkc_cf.isNormalized(o1);
                boolean c2 = nfkc_cf.isNormalized(o2);
                return c1 == c2 ? 0 
                        : c1 ? -1 : 1; // isNormalized is better
            }
        };


        Comparator<String> secondarySet  = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean c1 = !o1.isEmpty() && secondaryData.containsAll(o1);
                boolean c2 = !o1.isEmpty() && secondaryData.containsAll(o2);
                return c1 == c2 ? 0 
                        : c1 ? 1 : -1; // NOT secondary is better
            }
        };


        Comparator<String> firstIsBetterTarget = new MultiComparator<String>(
                isNFKC_CF, 
                combiningLast, 
                secondarySet,
                (Comparator<String>)(Comparator<?>)UCA_SECONDARY_DECOMPOSING, 
                CODEPOINT);
        Transform<String,RawCollationKey> transform = new RawCollationKeyTransform(UCA_SECONDARY_DECOMPOSING);

        ComparisonNormalizer norm = new ComparisonNormalizer(transform, firstIsBetterTarget);
        return norm;
    }

    //  private static void checkProblem(final RuleBasedCollator uca_raw, Comparator<String> ucaFull) {
    //    RawCollationKey a = uca_raw.getRawCollationKey("०", new RawCollationKey());
    //    RawCollationKey b = uca_raw.getRawCollationKey("𐒠", new RawCollationKey());
    //    boolean eq = a.equals(b);
    //    int comp = a.compareTo(b);
    //    Relation<RawCollationKey,String> mapping = Relation.of(new HashMap<RawCollationKey,Set<String>>(), TreeSet.class, ucaFull);
    //    mapping.put(a, "०");
    //    mapping.put(b, "𐒠");
    //    System.out.println(a.hashCode());
    //    System.out.println(b.hashCode());
    //    Set<RawCollationKey> keys = mapping.keySet();
    //  }

    /**
     * @return
     * @see com.ibm.icu.dev.util.UnicodeMap#values()
     */
    public Collection<String> values() {
        return map.values();
    }

    /**
     * @param codepoint
     * @return
     * @see com.ibm.icu.dev.util.UnicodeMap#get(int)
     */
    public String get(int codepoint) {
        return map.get(codepoint);
    }

    /**
     * @param value
     * @return
     * @see com.ibm.icu.dev.util.UnicodeMap#get(java.lang.String)
     */
    public String get(String value) {
        return map.get(value);
    }

    /**
     * @param value
     * @return
     * @see com.ibm.icu.dev.util.UnicodeMap#getSet(java.lang.Object)
     */
    public UnicodeSet getSet(String value) {
        return map.getSet(value);
    }

    public static void main(String[] args) {
        ComparisonNormalizer norm = getSimple();

        Comparator<String> ucaFull = new MultiComparator<String>((Comparator<String>)(Comparator<?>)UCA_SECONDARY_DECOMPOSING, CODEPOINT);

        TreeSet<String> sorted = new TreeSet<>(ucaFull);
        sorted.addAll(norm.values());

//        String z1 = norm.map.get('०');
//        String z2 = norm.map.get("𐒠");

        StringBuilder key = new StringBuilder();
        for (String target : sorted) {
            UnicodeSet sources = norm.getSet(target);
            CollationElementIterator it = UCA_SECONDARY_DECOMPOSING.getCollationElementIterator(target);
            key.setLength(0);
            for (int ce = it.next(); ce != CollationElementIterator.NULLORDER; ce = it.next()) {
                if (key.length() != 0) {
                    key.append(' ');
                }
                int primary = CollationElementIterator.primaryOrder(ce);
                int secondary = CollationElementIterator.secondaryOrder(ce);
                key.append(Utility.hex(primary) + ":" + Utility.hex(secondary,2));
            }

            System.out.println(
                    "\n#" + key
                    + "\t" + Utility.hex(target) 
                    + ";\t# ( " + target + " )" 
                    + "\t" + (target.isEmpty() ? "" : UCharacter.getExtendedName(target.codePointAt(0)))
                    );
            for (String source : sources) {
                System.out.println(
                        Utility.hex(source) 
                        + ";\t" + Utility.hex(target)
                        + ";\t# ( " + source + " )"
                        + "\t" + UCharacter.getExtendedName(source.codePointAt(0))
                        );
            }
        }
    }
}
