package org.unicode.propstest;

import java.util.EnumMap;
import java.util.EnumSet;

import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class ShowStats {
    static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> age = latest.load(UcdProperty.Age);
    static final UnicodeMap<String> cat = latest.load(UcdProperty.General_Category);

    static EnumMap<General_Category_Values, General_Category_Values> CAT = new EnumMap(General_Category_Values.class);
    static {
        for (General_Category_Values cat : UcdPropertyValues.General_Category_Values.values()) {
            switch (cat) {
            case Modifier_Letter:
            case Lowercase_Letter:
            case Uppercase_Letter:
            case Titlecase_Letter:
            case Other_Letter:
                CAT.put(cat, General_Category_Values.Letter); break;

            case Other_Punctuation:
            case Close_Punctuation:
            case Dash_Punctuation:
            case Connector_Punctuation:
            case Final_Punctuation:
            case Initial_Punctuation:
            case Open_Punctuation:
                CAT.put(cat, General_Category_Values.Punctuation); break;

            case Math_Symbol:
            case Currency_Symbol:
            case Modifier_Symbol:
            case Other_Symbol:
                CAT.put(cat, General_Category_Values.Symbol); break;

            case Control:
            case Format:
                CAT.put(cat, General_Category_Values.Format); break;

            case Decimal_Number:
            case Letter_Number:
            case Other_Number:
                CAT.put(cat, General_Category_Values.Number); break;

            case Nonspacing_Mark:
            case Enclosing_Mark:
            case Spacing_Mark:
                CAT.put(cat, General_Category_Values.Mark); break;

            case Space_Separator:
            case Paragraph_Separator:
            case Line_Separator:
                CAT.put(cat, General_Category_Values.Separator); break;

            case Unassigned:
            case Surrogate: // we don't care about these.
            case Private_Use:
                CAT.put(cat, General_Category_Values.Unassigned); break;

            case Symbol:
            case Separator:
            case Other:
            case Number:
            case Mark:
            case Cased_Letter:
            case Punctuation:
            case Letter:
                break;
            default: throw new IllegalAccessError();
            }
        } 
    }

    public static void main(String[] args) {
        Counter c = new Counter<Row.R2<UcdPropertyValues.General_Category_Values, UcdPropertyValues.Age_Values>>();
        Counter catCounter = new Counter<UcdPropertyValues.General_Category_Values>();
        
        for (int i = 0; i <= 0x10ffff; ++i) {
            General_Category_Values rawCat = UcdPropertyValues.General_Category_Values.valueOf(cat.get(i));
            General_Category_Values catGroup = CAT.get(rawCat);
            R2<General_Category_Values, Age_Values> pair = Row.of(
                    catGroup,
                    UcdPropertyValues.Age_Values.valueOf(age.get(i)));
            c.add(pair, 1);
            catCounter.add(catGroup,1);
        }
        for (Age_Values age : UcdPropertyValues.Age_Values.values()) {
            System.out.print("\t'" + age.getNames().getShortName());
        }
        System.out.print("\n");
        for (General_Category_Values cat : UcdPropertyValues.General_Category_Values.values()) {
            if (cat == General_Category_Values.Unassigned || catCounter.get(cat) == 0) {
                continue;
            }
            System.out.print(cat == General_Category_Values.Format ? "Format/Control" : cat.toString());
            long total = 0;
            for (Age_Values age : UcdPropertyValues.Age_Values.values()) {
                if (age == Age_Values.Unassigned) {
                    continue;
                }
                total += c.get(Row.of(cat,age));
                System.out.print("\t" + total);
            }
            System.out.print("\n");
        }
    }
}
