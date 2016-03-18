package org.unicode.propstest;

import java.util.Iterator;
import java.util.List;

import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class ShowDifferences {
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    private static final UnicodeMap<String> newName = iup.load(UcdProperty.Name);
    private static final IndexUnicodeProperties lastVersion = IndexUnicodeProperties.make("6.3");
    private static final boolean CHECK_DIFFS = false;

    public void TestProp() {
        boolean skipIt = false;
        String skipTo = "Lowercase_Mapping";
        for (String s : iup.getAvailableNames()) {
            if (s.equals(skipTo)) {
                skipIt = false;
            }
            if (skipIt) {
                System.out.println("Skipping\t" + s);
            } else {
                checkProp(s);
            }
        }
        //        for (UcdProperty up : iup.getAvailableUcdProperties()) {
        //            System.out.println(up.getType() + "\t" + up.getNames());
        //            Set<Enum> enums = up.getEnums();
        //            if (enums != null) {
        //                for (Enum enumValue : enums) {
        //                    System.out.println("\t" + ((PropertyNames.Named)enumValue).getNames());
        //                }
        //            }
        //        }
    }
    public void checkProp(String s) {
        UnicodeProperty prop = iup.getProperty(s);
        List<String> availableValues;
        try {
            availableValues = prop.getAvailableValues();
        } catch (Exception e) {
            System.err.println("Can't load properties for " + s + "\t" + e.getMessage());
            return;
        }
        UnicodeProperty oldProp = lastVersion.getProperty(s);

        UnicodeMap<String> propUmOld = oldProp.getUnicodeMap();
        UnicodeMap<String> propUmNew = prop.getUnicodeMap();
        String message = UnicodeProperty.getTypeName(prop.getType()) 
                + "\t" + CollectionUtilities.join(prop.getNameAliases(), ", ") 
                + "\tvalues: " + availableValues.size();
        if (propUmOld.equals(propUmNew)) {
            System.out.println(message);
            return;
        }
        System.out.println(message + "\t!DIFF!");
        UnicodeSet diffs = findDifferences(propUmOld, propUmNew);
        if (CHECK_DIFFS) {
            UnicodeSet diffs2 = findDifferences2(propUmOld, propUmNew);
            if (!diffs.equals(diffs2)) {
                throw new IllegalArgumentException(); 
            }
        }
        showDiffs(propUmOld, propUmNew, diffs);
    }
    public void showDiffs(UnicodeMap<String> propUmOld,
            UnicodeMap<String> propUmNew, UnicodeSet diffs) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(diffs); it.nextRange();) {
            int start = it.codepoint;
            String oldValue = propUmOld.get(start);
            String newValue = propUmNew.get(start);
            for (int i = start+1; i < it.codepointEnd; ++i) {
                String oldValue2 = propUmOld.get(i);
                String newValue2 = propUmNew.get(i);
                if (!UnicodeMap.areEqual(oldValue, oldValue2) || !UnicodeMap.areEqual(newValue, newValue2)) {
                    logCharValues(start, i-1, oldValue, newValue);                 
                    start = i;
                    oldValue = oldValue2;
                    newValue = newValue2;
                }
            }
            logCharValues(start, it.codepointEnd, oldValue, newValue);   

            //            UnicodeSet newSet = prop.getSet(value);
            //            UnicodeSet oldSet = oldProp.getSet(value);
            //            //                System.out.println("\t" + value + "\t" 
            //            //                        + CollectionUtilities.join(prop.getValueAliases(value), ", "));
            //            if (!newSet.equals(oldSet)) {
            //                System.out.println("\t\t" + value
            //                        + "\told:\t" + new UnicodeSet(oldSet).removeAll(newSet)
            //                        + "\tnew:\t" + new UnicodeSet(newSet).removeAll(oldSet)
            //                        );
            //            }
        }
    }

    public void logCharValues(int start, int end, String oldValue, String newValue) {
        System.out.println("\t\t" + Utility.hex(start)
                + (start == end ? "" : ".." + Utility.hex(end))
                + "\told:\t" + oldValue
                + "\tnew:\t" + newValue
                + "\t" + newName.get(start)
                + (start == end ? "" : ".." + newName.get(end))
                );
    }

    private <T> UnicodeSet findDifferences2(UnicodeMap<T> m1,
            UnicodeMap<T> m2) {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!UnicodeMap.areEqual(m1.get(i), m2.get(i))) {
                result.add(i);
            }
        }
        return result;
    }
    // TODO call method on UnicodeMap.
    private <T> UnicodeSet findDifferences(UnicodeMap<T> m1,
            UnicodeMap<T> m2) {
        if (m1.size() == 0) {
            return m2.keySet();
        } else if (m2.size() == 0) {
            return m1.keySet();
        }
        UnicodeSet result = new UnicodeSet();
        Iterator<EntryRange<T>> it1 = m1.entryRanges().iterator();
        Iterator<EntryRange<T>> it2 = m2.entryRanges().iterator();
        EntryRange<T> er1 = it1.next();
        EntryRange<T> er2 = it2.next();
        while (true) {
            if (er1.string != null || er2.string != null) {
                throw new UnsupportedOperationException(); // don't handle yet
            } else if (er1.codepoint == er2.codepoint 
                    && er1.codepointEnd == er2.codepointEnd) { // fast path for equals
                if (!CollectionUtilities.equals(er1.value, er2.value)) {
                    result.add(er1.codepoint, er1.codepointEnd);
                }
                if (it1.hasNext() && it2.hasNext()) {
                    er1 = it1.next();
                    er2 = it2.next();
                } else {
                    break; // add remainders there
                }
            } else if (er1.codepointEnd < er2.codepoint) { // fast path for no overlap
                if (er1.value != null) {
                    result.add(er1.codepoint, er1.codepointEnd);
                }
                if (it1.hasNext()) {
                    er1 = it1.next();
                } else {
                    break; // add remainders there
                }
            } else if (er2.codepointEnd < er1.codepoint) { // fast path for no overlap
                if (er2.value != null) {
                    result.add(er2.codepoint, er2.codepointEnd);
                }
                if (it2.hasNext()) {
                    er2 = it2.next();
                } else {
                    break; // add remainders there
                }
            } else { // not the same, and there is overlap
                int min = Math.min(er1.codepoint, er2.codepoint);
                int minOverlap = Math.max(er1.codepoint, er2.codepoint);
                int maxOverlap = Math.min(er1.codepointEnd, er2.codepointEnd);
                int max = Math.min(er1.codepointEnd, er2.codepointEnd);
                // add the bits
                if (min < minOverlap 
                        && (er1.codepoint == min && er1.value != null
                        || er2.codepoint == min && er2.value != null)) {
                    result.add(min, minOverlap-1);
                }
                if (!CollectionUtilities.equals(er1.value, er2.value)) {
                    result.add(minOverlap, maxOverlap);
                }
                // now the shorter one is done, and the longer one gets shortened
                if (er1.codepointEnd < er1.codepointEnd) {
                    er2.codepoint = er1.codepointEnd + 1;
                    if (it1.hasNext()) {
                        er1 = it1.next();
                    } else {
                        result.add(er2.codepoint, er2.codepointEnd); // shortened bit
                        break; // add remainders there
                    }
                } else {
                    er1.codepoint = er2.codepointEnd + 1;
                    if (it2.hasNext()) {
                        er2 = it2.next();
                    } else {
                        result.add(er1.codepoint, er1.codepointEnd); // shortened bit
                        break; // add remainders there
                    }
                } 
            }
        }
        // now add the remainders. 
        Iterator<EntryRange<T>> remainder = it2.hasNext() ? it2 : it1;
        while (remainder.hasNext()) {
            EntryRange<T> er = remainder.next();
            result.add(er.codepoint, er.codepointEnd);
        }
        return result;
    }
    
    public void showDiffs(UnicodeMap<String> generalCategory,
            UnicodeMap<String> oldGeneralCategory) {
        showDiffs(oldGeneralCategory, generalCategory, findDifferences2(oldGeneralCategory, generalCategory));
    }


}
