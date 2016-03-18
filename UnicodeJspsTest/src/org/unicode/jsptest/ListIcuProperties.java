package org.unicode.jsptest;
import java.util.Arrays;
import java.util.List;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;

public class ListIcuProperties {
    public static void main(String[] args) {
        int nameChoice = NameChoice.LONG;
        List<Integer> propRanges = Arrays.asList(
                UProperty.BINARY_START, UProperty.BINARY_LIMIT, 
                UProperty.INT_START, UProperty.INT_LIMIT, 
                UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT, 
                UProperty.STRING_START, UProperty.STRING_LIMIT);
        for (int range = 0; range < propRanges.size(); range += 2) {
            final int rangeStart = propRanges.get(range);
            final int rangeLimit = propRanges.get(range+1);
            for (int property = rangeStart; property < rangeLimit; ++property) {
                String name = UCharacter.getPropertyName(property, nameChoice);
                System.out.print(property + "\t" + name);
                if (rangeStart == UProperty.INT_START) {
                    String gap = "\t ";
                    for (int i = UCharacter.getIntPropertyMinValue(property); i <= UCharacter.getIntPropertyMaxValue(property); ++i) {
                        String propertyValueName = UCharacter.getPropertyValueName(property, i, nameChoice);
                        System.out.print(gap + propertyValueName);
                        gap = ", ";
                    }
                }
                System.out.println();
            }
        }
    }
}
