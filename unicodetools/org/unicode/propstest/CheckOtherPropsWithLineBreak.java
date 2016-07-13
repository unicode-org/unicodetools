package org.unicode.propstest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CheckOtherPropsWithLineBreak {
    static IndexUnicodeProperties UnicodeProperties = IndexUnicodeProperties.make(Age_Values.V9_0);
    public static void main(String[] args) {
        UnicodeMap<Line_Break_Values> lineBreakProperty = UnicodeProperties.loadEnum(UcdProperty.Line_Break, Line_Break_Values.class);
        UnicodeSet hebrewScript = UnicodeProperties.loadEnum(UcdProperty.Script, Script_Values.class).getSet(Script_Values.Hebrew);
        UnicodeSet testCharacters = new UnicodeSet();
        String otherPropName = "Hebr";

for (Line_Break_Values lineBreakValue : Line_Break_Values.values()) {
    UnicodeSet lineBreakPropertyValueSet = lineBreakProperty.getSet(lineBreakValue);
    String lbvName = lineBreakValue.getShortName();

    UnicodeSet difference = new UnicodeSet(lineBreakPropertyValueSet).removeAll(hebrewScript);
    UnicodeSet intersection = new UnicodeSet(lineBreakPropertyValueSet).retainAll(hebrewScript);
    if (difference.isEmpty() || intersection.isEmpty()) {
        addFirstCharacter(lbvName + "\t " + otherPropName, lineBreakPropertyValueSet, testCharacters);                
    } else {
        addFirstCharacter(lbvName + "\t-" + otherPropName, difference, testCharacters);
        addFirstCharacter(lbvName + "\t+" + otherPropName, intersection, testCharacters);
    }
}
    }
    private static void addFirstCharacter(String message, UnicodeSet intersection, UnicodeSet testCharacters) {
        if (intersection.isEmpty()) {
            return;
        }
        String testChar = intersection.iterator().next();
        System.out.println(message + "\t" + Utility.hex(testChar) + "\t" + UnicodeProperties.getName(testChar, ", "));
        testCharacters.add(testChar);
    }
}

