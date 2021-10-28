package org.unicode.propstest;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Joining_Group_Values;
import org.unicode.props.UcdPropertyValues.Joining_Type_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class PropertyDemo {
    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        final UnicodeMap<Joining_Group_Values> joiningGroup = latest.loadEnum(UcdProperty.Joining_Group, Joining_Group_Values.class);
        final UnicodeMap<Joining_Type_Values> joiningType = latest.loadEnum(UcdProperty.Joining_Type, Joining_Type_Values.class);
        UnicodeSet items = new UnicodeSet()
                .addAll(joiningGroup.getSet(Joining_Group_Values.No_Joining_Group))
                .retainAll(joiningType.getSet(Joining_Type_Values.Non_Joining))
                .complement()
                .freeze();

        Set<String> words = new LinkedHashSet<>();
        for (String s : items) {
            int cp = s.codePointAt(0);
            String name = latest.getName(cp);
            String word = name.split(" ")[0];
            words.add(word);
        }

        for (String wordMatch : words) {
            // # Syriac Characters
            System.out.println("\n # " + UCharacter.toTitleCase(wordMatch, null) + " Characters\n");
            for (String s : items) {
                int cp = s.codePointAt(0);
                String name = latest.getName(cp);
                String word = name.split(" ")[0];
                if (!word.equals(wordMatch)) {
                    continue;
                }
                // 0600; ARABIC NUMBER SIGN; U; No_Joining_Group
                System.out.println(Utility.hex(cp) + "; " + latest.getName(cp) + "; " + joiningType.get(cp).getShortName() + "; " + joiningGroup.get(cp).getShortName());
            }
        }

        //        for (Joining_Group_Values value : UcdPropertyValues.Joining_Group_Values.values()) {
        //            System.out.println("jg ; " + );
        //            // jg ; African_Feh                      ; African_Feh
        //            
        //        }
    }
}
