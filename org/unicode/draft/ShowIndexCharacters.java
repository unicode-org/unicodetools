package org.unicode.draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IndexCharacters;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.ULocale;

public class ShowIndexCharacters {

    public static void main(String[] args) {
        // get sample data
        String[] test = { "$", "£", "12", "2", "Edgar", "edgar", "Abbot", "Effron", "Zach", "Ƶ", "Þor", "Åberg", "Östlund",
                "Ἥρα", "Ἀθηνᾶ", "Ζεύς", "Ποσειδὣν", "Ἅιδης", "Δημήτηρ", "Ἑστιά", "Ἀπόλλων", "Ἄρτεμις", "Ἑρμἣς", "Ἄρης", "Ἀφροδίτη",
                "Ἥφαιστος", "Διόνυσος",
                "斉藤", "佐藤", "鈴木", "高橋", "田中", "渡辺", "伊藤", "山本", "中村", "小林", "斎藤", "加藤",
                "吉田", "山田", "佐々木", "山口", "松本", "井上", "木村", "林", "清水"
                };
        ULocale[] testLocales = { new ULocale("ru"), ULocale.ENGLISH, new ULocale("sv"), ULocale.JAPAN };
        for (ULocale testLocale : testLocales) {
            IndexCharacters indexCharacters = new IndexCharacters(testLocale);
            List<Bucket> buckets = getIndexBuckets(indexCharacters, Arrays.asList(test), new UnicodeSet('A', 'Z'));

            // show index at top. We can skip or gray out empty buckets
            System.out.println();
            System.out.println("Locale: " + testLocale.getDisplayName(testLocale));
            boolean showAll = true;
            for (Bucket entry : buckets) {
                String label = entry.getLabel();
                if (showAll || entry.getValues().size() != 0) {
                    System.out.print(label + " ");
                }
            }
            System.out.println();
            System.out.println("========================");

            // show buckets with contents
            for (Bucket entry : buckets) {
                if (entry.getValues().size() != 0) {
                    System.out.println(entry.getLabel());
                    for (String item : entry.getValues()) {
                        System.out.println("\t" + item);
                    }
                }
            }
        }
    }

    /**
     * Associates an label with a set of values V in the bucket for that label.
     * Used for the return value from getIndexBucketCharacters. Used in a list
     * because labels may not be unique, so a Map wouldn't work.
     */
    public static class Bucket {
        private String       label;
        private List<String> values = new ArrayList<String>();

        /**
         * Set up the bucket.
         * 
         * @param label
         */
        public Bucket(String label) {
            this.label = label;
        }

        /**
         * Get the label
         * 
         * @return
         */
        public String getLabel() {
            return label;
        }

        /**
         * Add a value to a bucket.
         * 
         * @param value
         */
        public void add(String value) {
            getValues().add(value);
        }

        /**
         * Get the values.
         * 
         * @return
         */
        public List<String> getValues() {
            return values;
        }
    }

    private static final UnicodeSet IGNORE_SCRIPTS = new UnicodeSet("[[:sc=Common:][:sc=inherited:]]").freeze();

    /**
     * Convenience routine to bucket a list of input strings according to the
     * index.
     * 
     * @param additions
     *            Characters to be added to the index. An example might be A-Z
     *            added for non-Latin alphabets.
     * @param inputList
     *            List of strings to be sorted and bucketed according to the
     *            index characters.
     * @return List of buckets, where each bucket has a label (typically an
     *         index character) and the strings in order in that bucket.
     */
    public static <T extends CharSequence> List<Bucket> getIndexBuckets(IndexCharacters indexCharacters, Collection<T> inputList, UnicodeSet additions) {
        RuleBasedCollator collator = getCollator(indexCharacters);
        // Note: A key issue is that deciding what goes into a bucket is
        // different
        // than sorting within that bucket.

        // fix up the list, adding underflow, additions, overflow
        List<String> characters = getIndexCharacters(indexCharacters);
        if (additions != null && additions.size() != 0) {
            collator.setStrength(Collator.TERTIARY);
            TreeSet<String> sortedIndex = new TreeSet<String>(collator);
            sortedIndex.addAll(characters);
            additions.addAllTo(sortedIndex);
            characters = new ArrayList<String>(sortedIndex);
            collator.setStrength(Collator.PRIMARY); // used for bucketing
            // insert infix labels as needed, using \uFFFF.
            String last = characters.get(0);
            UnicodeSet lastSet = getScriptSet(last).removeAll(IGNORE_SCRIPTS);
            for (int i = 1; i < characters.size(); ++i) {
                String current = characters.get(i);
                UnicodeSet set = getScriptSet(current).removeAll(IGNORE_SCRIPTS);
                if (lastSet.containsNone(set)) {
                    // check for adjacent
                    String overflowComparisonString = getOverflowComparisonString(indexCharacters, last);
                    if (collator.compare(overflowComparisonString, current) < 0) {
                        characters.add(i, "\uFFFD" + overflowComparisonString);
                        i++;
                        lastSet = set;
                    }
                }
                last = current;
                lastSet = set;
            }
        } else {
            characters = new ArrayList<String>(characters);
        }
        String beforeMarker = getUnderflowLabel(indexCharacters);
        String afterMarker = getOverflowLabel(indexCharacters);
        String inMarker = getInflowLabel(indexCharacters);
        String limitString = getOverflowComparisonString(indexCharacters, characters.get(characters.size() - 1));
        characters.add("\uFFFF" + limitString); // final, overflow bucket

        // Set up an array of sorted elements
        String[] sortedInput = inputList.toArray(new String[inputList.size()]);
        collator.setStrength(Collator.TERTIARY);
        Arrays.sort(sortedInput, 0, sortedInput.length, collator);
        collator.setStrength(Collator.PRIMARY); // used for bucketing
        List<Bucket> buckets = new ArrayList<Bucket>(); // Can't use Map,
        // because keys might
        // not be unique

        Bucket currentBucket;
        buckets.add(currentBucket = new Bucket(beforeMarker));
        Iterator<String> characterIterator = characters.iterator();
        String nextChar = characterIterator.next(); // there is always at least
        String nextLabel = nextChar;
        // one
        boolean atEnd = false;
        for (String s : sortedInput) {
            while (!atEnd && collator.compare(s, nextChar) >= 0) {
                buckets.add(currentBucket = new Bucket(nextLabel));
                // now reset nextChar
                if (characterIterator.hasNext()) {
                    nextLabel = nextChar = characterIterator.next();
                    switch (nextChar.charAt(0)) {
                    case 0xFFFD:
                        nextChar = nextChar.substring(0);
                        nextLabel = inMarker;
                        break;
                    case 0xFFFF:
                        nextChar = nextChar.substring(0);
                        nextLabel = afterMarker;
                        break;
                    }
                } else {
                    atEnd = true;
                }
            }
            currentBucket.add(s);
        }
        return buckets;
    }

    /**
     * Just change current API to return a list
     */
    public static List<String> getIndexCharacters(IndexCharacters indexCharacters) {
        return new ArrayList<String>(indexCharacters.getIndexCharacters());
    }

    private static ConcurrentHashMap<ULocale, List<String>> LIMIT_STRING_CACHE = new ConcurrentHashMap<ULocale, List<String>>();

    /**
     * Get the Unicode character (or tailored string) that defines the overflow
     * bucket; that is anything greater than or equal to that string should go
     * there, instead of with the last character. Normally that is the first
     * character of the script after lowerLimit. Thus in X Y Z ...
     * <i>Devanagari-ka</i>, the overflow character for Z would be the
     * <i>Greek-alpha</i>.
     * 
     * @param indexCharacters
     * @param lowerLimit
     *            The character below the overflow (or inflow) bucket
     * @return string that defines top of the overflow buck for lowerLimit
     */
    public static String getOverflowComparisonString(IndexCharacters indexCharacters, String lowerLimit) {
        RuleBasedCollator collator = getCollator(indexCharacters);
        List<String> list = LIMIT_STRING_CACHE.get(indexCharacters.getLocale());
        if (list == null) {
            list = firstStringsInScript(collator);
            LIMIT_STRING_CACHE.put(indexCharacters.getLocale(), list);
        }
        for (String s : list) {
            if (collator.compare(s, lowerLimit) > 0) {
                return s;
            }
        }
        return null;
    }

    private static UnicodeSet getScriptSet(String codePoint) {
        return new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, UScript.getScript(codePoint.codePointAt(0)));
    }

    /**
     * Get a clone of the collator used for the index characters.
     * 
     * @param indexCharacters
     * @return clone of collator
     */
    public static RuleBasedCollator getCollator(IndexCharacters indexCharacters) {
        RuleBasedCollator comparator = (RuleBasedCollator) Collator.getInstance(indexCharacters.getLocale());
        comparator.setStrength(Collator.PRIMARY);
        comparator.setNumericCollation(true);
        return comparator;
    }

    /**
     * Get the default label used in the IndexCharacters' locale for overflow,
     * eg the first item in: … A B C
     * 
     * @param indexCharacters
     * @return overflow label
     */
    public static String getOverflowLabel(IndexCharacters indexCharacters) {
        return "\u2026";
    }

    /**
     * Get the default label used in the IndexCharacters' locale for underflow,
     * eg the last item in: X Y Z …
     * 
     * @param indexCharacters
     * @return underflow label
     */
    public static String getUnderflowLabel(IndexCharacters indexCharacters) {
        return "\u2026";
    }

    /**
     * Get the default label used for abbreviated buckets <i>between</i> other
     * index characters. For example, consider the index characters for Latin
     * and Greek are used: X Y Z … &#x0391; &#x0392; &#x0393;.
     * 
     * @param indexCharacters
     * @return inflow label
     */
    public static String getInflowLabel(IndexCharacters indexCharacters) {
        return "\u2026";
    }

    public static final UnicodeSet TO_TRY = new UnicodeSet("[^[:cn:][:co:][:cs:]]").addAll(IGNORE_SCRIPTS).freeze();

    /**
     * Returns a list of all the "First" characters of scripts, according to the
     * collation, and sorted according to the collation.
     * 
     * @param comparator
     * @param lowerLimit
     * @param testScript
     * @return
     */

    private static List<String> firstStringsInScript(RuleBasedCollator comparator) {
        comparator.setStrength(Collator.TERTIARY);
        String[] results = new String[UScript.CODE_LIMIT];
        Normalizer2 normalizer = Normalizer2.getInstance(null, "nfkc", Mode.COMPOSE);
        for (String current : TO_TRY) {
            if (!normalizer.isNormalized(current) || comparator.compare(current, "a") < 0) {
                continue;
            }
            int script = UScript.getScript(current.codePointAt(0));
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                continue;
            }
            String bestSoFar = results[script];
            if (bestSoFar == null || comparator.compare(current, bestSoFar) < 0) {
                results[script] = current;
            }
        }

        UnicodeSet extras = new UnicodeSet();
        UnicodeSet expansions = new UnicodeSet();
        try {
            comparator.getContractionsAndExpansions(extras, expansions, true);
        } catch (Exception e) {
        } // why have a checked exception???

        extras.addAll(expansions).removeAll(TO_TRY);
        for (String current : extras) {
            if (!normalizer.isNormalized(current) || comparator.compare(current, "a") < 0) {
                continue;
            }
            int script = UScript.getScript(current.codePointAt(0));
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                continue;
            }
            String bestSoFar = results[script];
            if (bestSoFar == null || comparator.compare(current, bestSoFar) < 0) {
                results[script] = current;
            }
        }

        TreeSet<String> sorted = new TreeSet<String>(comparator);
        for (int i = 0; i < results.length; ++i) {
            if (results[i] != null) {
                sorted.add(results[i]);
            }
        }
        return Collections.unmodifiableList(new ArrayList<String>(sorted));
    }
}
