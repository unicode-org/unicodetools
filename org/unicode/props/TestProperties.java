package org.unicode.props;

import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Numeric_Type_Values;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;

public class TestProperties {
    public static void main(String[] args) {
        checkNumbers();
        for (UcdProperty prop : UcdProperty.values()) {
            System.out.println(prop + "\t" + prop.getNames() + "\t" + prop.getEnums());
//            Collection<Enum> values = PropertyValues.valuesOf(prop);
//            System.out.println("values: " + values);
        }
        for (UcdPropertyValues.General_Category_Values prop : UcdPropertyValues.General_Category_Values.values()) {
            System.out.println(prop + "\t" + prop.getNames());
//            Collection<Enum> values = PropertyValues.valuesOf(prop);
//            System.out.println("values: " + values);
        }

        UcdPropertyValues.General_Category_Values q = UcdPropertyValues.General_Category_Values.Unassigned;
        System.out.println(q.getNames());

//        Enum x = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
//        //Bidi_Mirrored_Values y = Properties.Bidi_Mirrored_Values.No;
//        Enum z = PropertyValues.forValueName(UcdProperty.Bidi_Mirrored, "N");
//        Enum w = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
//        System.out.println(x + " " + z + " " + w);
    }

    private static void checkNumbers() {
        for (Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned) { //  || age.compareTo(Age_Values.V4_0) < 0
                continue;
            }
            PropertyNames<Age_Values> names = age.getNames();
            //System.out.println(names.getShortName());
            IndexUnicodeProperties props = IndexUnicodeProperties.make(names.getShortName());
            UnicodeMap<String> gc = props.load(UcdProperty.General_Category);
            UnicodeMap<String> nt = props.load(UcdProperty.Numeric_Type);
            UnicodeSet gcNum = new UnicodeSet()
            .addAll(gc.getSet(General_Category_Values.Decimal_Number.toString()))
            .addAll(gc.getSet(General_Category_Values.Letter_Number.toString()))
            .addAll(gc.getSet(General_Category_Values.Other_Number.toString()))
            ;
            UnicodeSet ntNum = new UnicodeSet()
            .addAll(nt.getSet(Numeric_Type_Values.Decimal.toString()))
            .addAll(nt.getSet(Numeric_Type_Values.Digit.toString()))
            .addAll(nt.getSet(Numeric_Type_Values.Numeric.toString()))
            ;
            UnicodeSet diff;
//            diff = new UnicodeSet(ntNum).removeAll(gcNum);
//            System.out.println(age + ", nt-gc:N" + diff);
            diff = new UnicodeSet(gcNum).removeAll(ntNum);
            System.out.println(age + ", gc:N-nt" + diff);
        }
        
    }

    
}
