package org.unicode.propstest;

import java.util.EnumSet;
import java.util.Set;

import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Joining_Group_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class ShowDuplicatePropertyValues {
    static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<Joining_Group_Values> group = latest.loadEnum(UcdProperty.Joining_Group, Joining_Group_Values.class);
    static final UnicodeMap<Age_Values> age = latest.loadEnum(UcdProperty.Age, Age_Values.class);

    public static void main(String[] args) {
        Counter<Joining_Group_Values> counter = new Counter<>();
        for (Joining_Group_Values key : group.values()) {
            UnicodeSet set = group.getSet(key);
            counter.add(key, set.size());
        }
        int lastCount = -1;
        Set<Joining_Group_Values> remaining = EnumSet.allOf(Joining_Group_Values.class);

        for (R2<Long, Joining_Group_Values> entry : counter.getEntrySetSortedByCount(false, null)) {
            Joining_Group_Values key = entry.get1();
            remaining.remove(key);
            if (key == Joining_Group_Values.No_Joining_Group) {
                continue;
            }
            UnicodeSet set = group.getSet(key);
            int count = set.size();
            if (count != lastCount) {
                //System.out.println("\n# " + count);
                lastCount = count;
            }
            for (String cp : set) {
                System.out.println(count 
                        + "\t" + age.get(cp).getShortName() 
                        + "\tU+" + Utility.hex(cp) 
                        + "\t" + key 
                        + "\t" + latest.getName(cp, ", "));
            }
        }
        if (remaining.size() != 0) {
            //System.out.println("\n# " + 0);
            for (Joining_Group_Values key : remaining) {
                System.out.println("?" + ";\t" + key + "\t# ");
            }
        }
    }
}
