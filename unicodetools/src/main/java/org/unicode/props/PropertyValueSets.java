package org.unicode.props;

import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.EnumSet;
import java.util.Set;
import org.unicode.props.UcdPropertyValues.General_Category_Values;

public class PropertyValueSets {

    public static final Set<General_Category_Values> CONTROL =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Other,
                            General_Category_Values.Control,
                            General_Category_Values.Format,
                            General_Category_Values.Unassigned,
                            General_Category_Values.Private_Use,
                            General_Category_Values.Surrogate));
    public static final Set<General_Category_Values> LETTER =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Lowercase_Letter,
                            General_Category_Values.Modifier_Letter,
                            General_Category_Values.Other_Letter,
                            General_Category_Values.Titlecase_Letter,
                            General_Category_Values.Uppercase_Letter));
    public static final Set<General_Category_Values> MARK =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Spacing_Mark,
                            General_Category_Values.Enclosing_Mark,
                            General_Category_Values.Nonspacing_Mark));
    public static final Set<General_Category_Values> NUMBER =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Decimal_Number,
                            General_Category_Values.Letter_Number,
                            General_Category_Values.Other_Number));
    public static final Set<General_Category_Values> PUNCTUATION =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Connector_Punctuation,
                            General_Category_Values.Dash_Punctuation,
                            General_Category_Values.Close_Punctuation,
                            General_Category_Values.Final_Punctuation,
                            General_Category_Values.Initial_Punctuation,
                            General_Category_Values.Other_Punctuation,
                            General_Category_Values.Open_Punctuation));
    public static final Set<General_Category_Values> SYMBOL =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Currency_Symbol,
                            General_Category_Values.Modifier_Symbol,
                            General_Category_Values.Math_Symbol,
                            General_Category_Values.Other_Symbol));
    public static final Set<General_Category_Values> SEPARATOR =
            ImmutableSortedSet.copyOf(
                    EnumSet.of(
                            General_Category_Values.Line_Separator,
                            General_Category_Values.Paragraph_Separator,
                            General_Category_Values.Space_Separator));

    public static <T> UnicodeSet getSet(UnicodeMap<T> map, T... values) {
        UnicodeSet result = new UnicodeSet();
        for (T value : values) {
            result.addAll(map.getSet(value));
        }
        return result.freeze();
    }

    public static <T> UnicodeSet getSet(UnicodeMap<T> map, Iterable<T> values) {
        UnicodeSet result = new UnicodeSet();
        for (T value : values) {
            result.addAll(map.getSet(value));
        }
        return result.freeze();
    }
}
