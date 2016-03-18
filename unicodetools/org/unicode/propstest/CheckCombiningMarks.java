package org.unicode.propstest;

import org.unicode.cldr.util.With;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.General_Category_Values;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckCombiningMarks {
    
    static final PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
            .setCompressRanges(true);
    
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        UnicodeMap<String> rawDecomp = iup.load(UcdProperty.Decomposition_Mapping);
        UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
        
        UnicodeMap<String> decomp = new UnicodeMap<>();

        for (EntryRange<String> entry : rawDecomp.entryRanges()) {
            if (entry.string != null) {
                throw new IllegalArgumentException(); // should never happen
            }
            String lastResult = entry.value;
            while (true) {
                String result = rawDecomp.transform(lastResult);
                if (result.equals(lastResult)) {
                    break;
                }
                lastResult = result;
            }
            decomp.putAll(entry.codepoint, entry.codepointEnd, lastResult);
        }
        decomp.freeze();

        UnicodeSet leading = new UnicodeSet();
        UnicodeSet trailing = new UnicodeSet();

        for (EntryRange<String> entry : decomp.entryRanges()) {
            boolean first = true;
            int[] chars = With.codePointArray(entry.value);
            if (chars.length == 1) {
                continue;
            }
            for (int cp : chars) {
                if (first) {
                    first = false;
                    leading.add(cp);
                } else {
                    trailing.add(cp);
                }
            }
        }
        show("Leading characters in decomps: ", leading);
        show("Trailing characters in decomps: ", trailing);
        UnicodeSet marks = new UnicodeSet()
//        .addAll(gc.getSet(General_Category_Values.Enclosing_Mark))
        .addAll(gc.getSet(General_Category_Values.Nonspacing_Mark))
//        .addAll(gc.getSet(General_Category_Values.Spacing_Mark))
        .freeze();
        show("not in Non-Spacing Marks, but in Trailing: ", new UnicodeSet(trailing).removeAll(marks));
        show("in Non-Spacing Marks, and in Trailing: ", new UnicodeSet(marks).retainAll(trailing));
        show("in Non-Spacing Marks, not in Trailing: ", new UnicodeSet(marks).removeAll(trailing));
    }

    private static void show(String title, UnicodeSet leading) {
        System.out.println(title + leading.size() + "\n\t" + pp.format(leading));
    }
}
