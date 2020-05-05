package org.unicode.text.tools;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class PropertyChanges {
    public static void main(String[] args) {
        checkProperties();
    }

    static void checkProperties() {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make();
        Set<Age_Values> ages = new LinkedHashSet<Age_Values>(Arrays.asList(Age_Values.values()));
        ages.remove(Age_Values.Unassigned);
        ages.remove(Age_Values.V10_0);
        ages.add(Age_Values.V10_0);
        System.out.println(ages);


        UnicodeMap<Age_Values> ageMap = latest.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);
        for (UcdProperty prop : UcdProperty.values()) {
            if (prop == UcdProperty.Age) {
                continue;
            }
            UnicodeMap<String> oldMap = new UnicodeMap<>();
            UnicodeSet oldChars = new UnicodeSet();
            for (Age_Values age : ages) {
                UnicodeSet newChars = ageMap.getSet(age);
                IndexUnicodeProperties iup = IndexUnicodeProperties.make(age);
                UnicodeMap<String> newMap;
                try {
                    newMap = iup.load(prop);
                    Changes changes = Changes.getChanges(oldMap, newMap, oldChars);
                    if (changes.changedItems != 0) {
                        System.out.println(prop + "\t" + age + "\t" + changes.changedItems);
                    }
                    oldMap = newMap;
                } catch (Exception e) {
                }
                oldChars.addAll(newChars);
            }
        }
    }

    static class Changes {
        final int sameItems;
        final int changedItems;

        public Changes(int sameItems, int changedItems) {
            super();
            this.sameItems = sameItems;
            this.changedItems = changedItems;
        }

        static Changes getChanges(UnicodeMap<String> oldMap, UnicodeMap<String> newMap, UnicodeSet oldChars) {
            int sameItems = 0;
            int changedItems = 0;
            for (String c : oldChars) {
                String oldValue = oldMap.get(c);
                String newValue = newMap.get(c);
                if (Objects.equals(oldValue, newValue)) {
                    sameItems++;
                } else {
                    changedItems++;
                }
            }
            return new Changes(sameItems, changedItems);
        }
    }


    //    private static void checkDates() {
    //        long base = new Date(2017-1900, 0, 15).getTime();
    //        for (String locale : Arrays.asList("en", "zh")) {
    //            DateFormat df = DateFormat.getInstanceForSkeleton("hCCC", ULocale.forLanguageTag(locale));
    //            System.out.println("locale: " + locale + "\tpattern: " + ((SimpleDateFormat) df).toPattern());
    //            //DateFormat df = new SimpleDateFormat("h BBBB", ULocale.forLanguageTag(locale));
    //            for (int hour = 0; hour < 24; ++hour) {
    //                String formatted = df.format(base+hour*3600000);
    //                System.out.println(formatted);
    //            }
    //        }
    //    }

}
