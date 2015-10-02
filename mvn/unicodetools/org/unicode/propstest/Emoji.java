package org.unicode.propstest;

import java.util.EnumSet;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.Default;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

public class Emoji {

    static final Set<General_Category_Values> LETTER = EnumSet.of(
            General_Category_Values.Modifier_Letter,
            General_Category_Values.Lowercase_Letter,
            General_Category_Values.Uppercase_Letter,
            General_Category_Values.Titlecase_Letter,
            General_Category_Values.Other_Letter);

    static final Set<General_Category_Values> PUNCTUATION = EnumSet.of(
            General_Category_Values.Other_Punctuation,
            General_Category_Values.Close_Punctuation,
            General_Category_Values.Dash_Punctuation,
            General_Category_Values.Connector_Punctuation,
            General_Category_Values.Final_Punctuation,
            General_Category_Values.Initial_Punctuation,
            General_Category_Values.Open_Punctuation);

    static final Set<General_Category_Values> SYMBOL = EnumSet.of(
            General_Category_Values.Math_Symbol,
            General_Category_Values.Currency_Symbol,
            General_Category_Values.Modifier_Symbol,
            General_Category_Values.Other_Symbol);

    static final Set<General_Category_Values> SEPARATOR = EnumSet.of(
            General_Category_Values.Space_Separator,
            General_Category_Values.Paragraph_Separator,
            General_Category_Values.Line_Separator);

    static final Set<General_Category_Values> OTHER = EnumSet.of(
            General_Category_Values.Control,
            General_Category_Values.Format,
            General_Category_Values.Unassigned,
            General_Category_Values.Surrogate, // we don't care about these.
            General_Category_Values.Private_Use);

    static final Set<General_Category_Values> NUMBER = EnumSet.of(
            General_Category_Values.Decimal_Number,
            General_Category_Values.Letter_Number,
            General_Category_Values.Other_Number);

    static final Set<General_Category_Values> MARK = EnumSet.of(
            General_Category_Values.Nonspacing_Mark,
            General_Category_Values.Enclosing_Mark,
            General_Category_Values.Spacing_Mark);

    private static <E extends Enum<?>> UnicodeSet getSet(UnicodeMap<String> source, E item) {
        return source.getSet(item.toString());
    }

    private static <E extends Enum<?>> UnicodeSet getSet(UnicodeMap<String> source, Set<E> itemSet) {
        UnicodeSet result = new UnicodeSet();
        for (E s : itemSet) {
            result.addAll(source.getSet(s.toString()));
        }
        return result.freeze();
    }

    public static void main(String[] args) {
        final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        final UnicodeMap<String> generalCategory = latest.load(UcdProperty.General_Category);
        final UnicodeMap<String> age = latest.load(UcdProperty.Age);
        final UnicodeMap<String> name = latest.load(UcdProperty.Name);
        final UnicodeMap<String> block = latest.load(UcdProperty.Block);
        UnicodeSet v70 = getSet(age, Age_Values.V7_0);
        for (General_Category_Values symbolSet : SYMBOL) {
            UnicodeSet symbols = getSet(generalCategory, symbolSet);
            UnicodeSet u70Symbols = new UnicodeSet(symbols).retainAll(v70);
            for (String s : u70Symbols) {
                System.out.println(
                        "U+" + Utility.hex(s) 
                        + "\temoji\t\t\t" + Age_Values.V7_0
                        + "\t" + s 
                        + "\t\t\t" + name.get(s)
                        + "\t" + symbolSet + " — " + block.get(s)
                        );
            }
        }
    }
}
