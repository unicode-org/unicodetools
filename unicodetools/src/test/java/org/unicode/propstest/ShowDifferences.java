package org.unicode.propstest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class ShowDifferences {

    static class DPair<T,U> {
        final T first;
        final U second;
        public DPair(T first, U second) {
            this.first = first;
            this.second = second;
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(first, second);
        }
        @Override
        public boolean equals(Object obj) {
            return obj != null 
                    && obj.getClass() == DPair.class 
                    && Objects.equal(first, ((DPair<T,U>) obj).first) 
                    && Objects.equal(second, ((DPair<T,U>) obj).second);
        }
        @Override
        public String toString() {
            return "[" + first + ", " + second + "]";
        }
    }

    /**
     * Computes differences between two versions. The args should either be [old] [new] or empty 
     * (for the most recent two versions).
     * @param args
     */
    public static void main(String[] args) {
        final String OLD_VERSION = args.length > 0 ? args[0] : Settings.lastVersion;
        final String NEW_VERSION = args.length > 1 ? args[1] : Settings.latestVersion;
        
        final IndexUnicodeProperties latestVersion = IndexUnicodeProperties.make(NEW_VERSION);
        final IndexUnicodeProperties lastVersion = IndexUnicodeProperties.make(OLD_VERSION);

        int changeCount = 0;
        System.out.println("№\tProperty\tValue in "
                + OLD_VERSION
                + "\t⇒\tValue in "
                + NEW_VERSION
                + "\tCharacters affected\tLink");
        UnicodeSet oldChars = lastVersion.getSet("gc=Cn").complement().freeze();
        UnicodeSet newChars = latestVersion.getSet("gc=Cn").complement().freeze();
        int count = 0;
        List<UcdProperty> noDiff = new ArrayList<>();
        Map<UcdProperty,UnicodeMap<DPair<String,String>>> newDiffs = new LinkedHashMap<>();
        final UnicodeMap<String> empty = new UnicodeMap<String>().freeze();
        for (UcdProperty prop : UcdProperty.values()) {
            if (!prop.name().startsWith("k")) {
                continue;
            }
            UnicodeMap<String> lastMap = empty;
            UnicodeMap<String> latestMap = empty;
            try {
                lastMap = lastVersion.load(prop);
            } catch (Exception e) {}
            try {
                latestMap = latestVersion.load(prop);
            } catch (Exception e) {}
            
            UnicodeMap<DPair<String,String>> diff = new UnicodeMap<>();
            UnicodeMap<DPair<String,String>> newDiff = new UnicodeMap<>();

            for (EntryRange<String> entry : lastMap.entryRanges()) {
                String lastValue = entry.value;

                if (entry.codepoint == -1) {
                    String latestValue = latestMap.get(entry.string);
                    if (!Objects.equal(latestValue,lastValue)) {
                        if (oldChars.containsAll(entry.string)) {
                            diff.put(entry.string, new DPair<>(lastValue, latestValue));
                        } else {
                            newDiff.put(entry.string, new DPair<>(lastValue, latestValue));
                        }
                    }
                    continue;
                }
                if (newChars.containsNone(entry.codepoint, entry.codepointEnd)) {
                    continue;
                }
                for (int i = entry.codepoint; i <= entry.codepointEnd; ++i) {
                    String latestValue = latestMap.get(i);
                    if (!Objects.equal(latestValue,lastValue)) {
                        if (oldChars.contains(i)) {
                            diff.put(i, new DPair<>(lastValue, latestValue));
                        } else {
                            newDiff.put(i, new DPair<>(lastValue, latestValue));
                        }
                    }
                }
            }
            if (diff.isEmpty()) {
                noDiff.add(prop);
            } else {
                changeCount++;
                count = displayDiff(count, prop, diff);
            }
            if (!newDiff.isEmpty()) {
                newDiffs.put(prop, newDiff);
            }
        }

        for (UcdProperty prop : noDiff) {
            System.out.println(++count + "\t" + prop + "\tNo Differences");
        }
        System.out.println("\tTOTAL Properties with No Differences: " + noDiff.size());
        System.out.println("\tTOTAL Properties with Differences: " + changeCount);
        System.out.println("\nNewDiffs");
        count = 0;
        for (Entry<UcdProperty, UnicodeMap<DPair<String, String>>> newDiff : newDiffs.entrySet()) {
            count = displayDiff(count, newDiff.getKey(), newDiff.getValue());
        }
    }

    private static int displayDiff(int count, UcdProperty prop, UnicodeMap<DPair<String, String>> diff) {
        for (DPair<String,String> value : diff.values()) {
            System.out.println(++count + "\t" + prop + "\t " + value.first + "\t⇒\t " + value.second + "\t" + diff.getSet(value)
            + "\t=\"https://unicode.org/cldr/utility/list-unicodeset.jsp?a=\"&F"+(count+1)
            + "&\"&i=\"&B"+(count+1)
            + "&\"+\"&B"+(count+1)
            + "&\"β\"");
        }
        return count;
    }

//    public void TestProp() {
//        boolean skipIt = false;
//        String skipTo = "Lowercase_Mapping";
//        for (String s : iup.getAvailableNames()) {
//            if (s.equals(skipTo)) {
//                skipIt = false;
//            }
//            if (skipIt) {
//                System.out.println("Skipping\t" + s);
//            } else {
//                checkProp(s);
//            }
//        }
//        //        for (UcdProperty up : iup.getAvailableUcdProperties()) {
//        //            System.out.println(up.getType() + "\t" + up.getNames());
//        //            Set<Enum> enums = up.getEnums();
//        //            if (enums != null) {
//        //                for (Enum enumValue : enums) {
//        //                    System.out.println("\t" + ((PropertyNames.Named)enumValue).getNames());
//        //                }
//        //            }
//        //        }
//    }
//    public void checkProp(String s) {
//        UnicodeProperty prop = iup.getProperty(s);
//        List<String> availableValues;
//        try {
//            availableValues = prop.getAvailableValues();
//        } catch (Exception e) {
//            System.err.println("Can't load properties for " + s + "\t" + e.getMessage());
//            return;
//        }
//        UnicodeProperty oldProp = lastVersion.getProperty(s);
//
//        UnicodeMap<String> propUmOld = oldProp.getUnicodeMap();
//        UnicodeMap<String> propUmNew = prop.getUnicodeMap();
//        String message = UnicodeProperty.getTypeName(prop.getType()) 
//                + "\t" + CollectionUtilities.join(prop.getNameAliases(), ", ") 
//                + "\tvalues: " + availableValues.size();
//        if (propUmOld.equals(propUmNew)) {
//            System.out.println(message);
//            return;
//        }
//        System.out.println(message + "\t!DIFF!");
//        UnicodeSet diffs = findDifferences(propUmOld, propUmNew);
//        if (CHECK_DIFFS) {
//            UnicodeSet diffs2 = findDifferences2(propUmOld, propUmNew);
//            if (!diffs.equals(diffs2)) {
//                throw new IllegalArgumentException(); 
//            }
//        }
//        showDiffs(propUmOld, propUmNew, diffs);
//    }
//    public void showDiffs(UnicodeMap<String> propUmOld,
//            UnicodeMap<String> propUmNew, UnicodeSet diffs) {
//        for (UnicodeSetIterator it = new UnicodeSetIterator(diffs); it.nextRange();) {
//            int start = it.codepoint;
//            String oldValue = propUmOld.get(start);
//            String newValue = propUmNew.get(start);
//            for (int i = start+1; i < it.codepointEnd; ++i) {
//                String oldValue2 = propUmOld.get(i);
//                String newValue2 = propUmNew.get(i);
//                if (!UnicodeMap.areEqual(oldValue, oldValue2) || !UnicodeMap.areEqual(newValue, newValue2)) {
//                    logCharValues(start, i-1, oldValue, newValue);                 
//                    start = i;
//                    oldValue = oldValue2;
//                    newValue = newValue2;
//                }
//            }
//            logCharValues(start, it.codepointEnd, oldValue, newValue);   
//
//            //            UnicodeSet newSet = prop.getSet(value);
//            //            UnicodeSet oldSet = oldProp.getSet(value);
//            //            //                System.out.println("\t" + value + "\t" 
//            //            //                        + CollectionUtilities.join(prop.getValueAliases(value), ", "));
//            //            if (!newSet.equals(oldSet)) {
//            //                System.out.println("\t\t" + value
//            //                        + "\told:\t" + new UnicodeSet(oldSet).removeAll(newSet)
//            //                        + "\tnew:\t" + new UnicodeSet(newSet).removeAll(oldSet)
//            //                        );
//            //            }
//        }
//    }
//
//    public void logCharValues(int start, int end, String oldValue, String newValue) {
//        System.out.println("\t\t" + Utility.hex(start)
//        + (start == end ? "" : ".." + Utility.hex(end))
//        + "\told:\t" + oldValue
//        + "\tnew:\t" + newValue
//        + "\t" + newName.get(start)
//        + (start == end ? "" : ".." + newName.get(end))
//                );
//    }
//
//    private <T> UnicodeSet findDifferences2(UnicodeMap<T> m1,
//            UnicodeMap<T> m2) {
//        UnicodeSet result = new UnicodeSet();
//        for (int i = 0; i < 0x10FFFF; ++i) {
//            if (!UnicodeMap.areEqual(m1.get(i), m2.get(i))) {
//                result.add(i);
//            }
//        }
//        return result;
//    }
//    // TODO call method on UnicodeMap.
//    private <T> UnicodeSet findDifferences(UnicodeMap<T> m1,
//            UnicodeMap<T> m2) {
//        if (m1.size() == 0) {
//            return m2.keySet();
//        } else if (m2.size() == 0) {
//            return m1.keySet();
//        }
//        UnicodeSet result = new UnicodeSet();
//        Iterator<EntryRange<T>> it1 = m1.entryRanges().iterator();
//        Iterator<EntryRange<T>> it2 = m2.entryRanges().iterator();
//        EntryRange<T> er1 = it1.next();
//        EntryRange<T> er2 = it2.next();
//        while (true) {
//            if (er1.string != null || er2.string != null) {
//                throw new UnsupportedOperationException(); // don't handle yet
//            } else if (er1.codepoint == er2.codepoint 
//                    && er1.codepointEnd == er2.codepointEnd) { // fast path for equals
//                if (!CollectionUtilities.equals(er1.value, er2.value)) {
//                    result.add(er1.codepoint, er1.codepointEnd);
//                }
//                if (it1.hasNext() && it2.hasNext()) {
//                    er1 = it1.next();
//                    er2 = it2.next();
//                } else {
//                    break; // add remainders there
//                }
//            } else if (er1.codepointEnd < er2.codepoint) { // fast path for no overlap
//                if (er1.value != null) {
//                    result.add(er1.codepoint, er1.codepointEnd);
//                }
//                if (it1.hasNext()) {
//                    er1 = it1.next();
//                } else {
//                    break; // add remainders there
//                }
//            } else if (er2.codepointEnd < er1.codepoint) { // fast path for no overlap
//                if (er2.value != null) {
//                    result.add(er2.codepoint, er2.codepointEnd);
//                }
//                if (it2.hasNext()) {
//                    er2 = it2.next();
//                } else {
//                    break; // add remainders there
//                }
//            } else { // not the same, and there is overlap
//                int min = Math.min(er1.codepoint, er2.codepoint);
//                int minOverlap = Math.max(er1.codepoint, er2.codepoint);
//                int maxOverlap = Math.min(er1.codepointEnd, er2.codepointEnd);
//                int max = Math.min(er1.codepointEnd, er2.codepointEnd);
//                // add the bits
//                if (min < minOverlap 
//                        && (er1.codepoint == min && er1.value != null
//                        || er2.codepoint == min && er2.value != null)) {
//                    result.add(min, minOverlap-1);
//                }
//                if (!CollectionUtilities.equals(er1.value, er2.value)) {
//                    result.add(minOverlap, maxOverlap);
//                }
//                // now the shorter one is done, and the longer one gets shortened
//                if (er1.codepointEnd < er1.codepointEnd) {
//                    er2.codepoint = er1.codepointEnd + 1;
//                    if (it1.hasNext()) {
//                        er1 = it1.next();
//                    } else {
//                        result.add(er2.codepoint, er2.codepointEnd); // shortened bit
//                        break; // add remainders there
//                    }
//                } else {
//                    er1.codepoint = er2.codepointEnd + 1;
//                    if (it2.hasNext()) {
//                        er2 = it2.next();
//                    } else {
//                        result.add(er1.codepoint, er1.codepointEnd); // shortened bit
//                        break; // add remainders there
//                    }
//                } 
//            }
//        }
//        // now add the remainders. 
//        Iterator<EntryRange<T>> remainder = it2.hasNext() ? it2 : it1;
//        while (remainder.hasNext()) {
//            EntryRange<T> er = remainder.next();
//            result.add(er.codepoint, er.codepointEnd);
//        }
//        return result;
//    }
//
//    public void showDiffs(UnicodeMap<String> generalCategory,
//            UnicodeMap<String> oldGeneralCategory) {
//        showDiffs(oldGeneralCategory, generalCategory, findDifferences2(oldGeneralCategory, generalCategory));
//    }
//

}
