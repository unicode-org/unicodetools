package org.unicode.text.tools;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.Locale;
import org.unicode.cldr.util.Counter;
import org.unicode.draft.CharacterFrequency;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;

public class ShowCharacterFrequency {
    static final Counter<Integer> freq = CharacterFrequency.getCodePointCounter("mul", true);
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make();
    static final UnicodeMap<General_Category_Values> cat =
            iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);

    public static void main(String[] args) {
        UnicodeSet proposal =
                new UnicodeSet(
                        "[– — · « » § † • ° ℗ ← → ↑ ↓ ⇅ ⇆ ∆-∈ × √ ∞ ∩ ∪ ≡ ⊂ ▲ ▼ ◊ ○ ● ◯ ↕ ↔ ▶ ◀ © ® ™ £ ¥ € ₹ ₽ ² ³ µ"
                                + "‘ ’ “ ” (-* ∋ ⁻ ∖ ⊃ ⊆ ⊇ ⁰ ¹"
                                + "]");
        UnicodeSet punctSym =
                new UnicodeSet(proposal)
                        .addAll(cat.getSet(General_Category_Values.Close_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Connector_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Open_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Final_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Dash_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Initial_Punctuation))
                        .addAll(cat.getSet(General_Category_Values.Currency_Symbol))
                        .addAll(cat.getSet(General_Category_Values.Math_Symbol))
                        .addAll(cat.getSet(General_Category_Values.Modifier_Symbol))
                        .addAll(cat.getSet(General_Category_Values.Math_Symbol))
                        .removeAll(new UnicodeSet(0, 0x7F));

        double factor = show("broad-ASCII", punctSym, 100, proposal);

        // show("original", proposal, 100, factor);
    }

    private static double show(String title, UnicodeSet proposal, int maxCount, UnicodeSet keep) {
        System.out.println("\n" + title);
        Counter<Integer> nfreq = new Counter<>();
        for (String s : proposal) {
            int cp = s.codePointAt(0);
            nfreq.add(cp, freq.getCount(cp));
        }
        double factor = 100d / nfreq.getTotal();

        for (R2<Long, Integer> s : nfreq.getEntrySetSortedByCount(false, null)) {
            final Integer cp = s.get1();
            if (!keep.contains(cp)) {
                if (--maxCount <= 0) {
                    continue;
                }
            }
            double count = s.get0() * factor;
            System.out.println(
                    UTF16.valueOf(cp)
                            + "\t"
                            + count
                            + "%"
                            + "\t"
                            + cat.get(cp)
                            + "\t"
                            + iup.getName(cp).toLowerCase(Locale.ROOT));
        }
        return factor;
    }
}
