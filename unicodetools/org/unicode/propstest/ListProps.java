package org.unicode.propstest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.ValueCardinality;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;

public class ListProps {
    public static void main(String[] args) {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make();
        PropertyType lastType = null;
        for (UcdProperty item : UcdProperty.values()) {
            PropertyType type = item.getType();
            if (type != lastType) {
                System.out.println("\n" + type + "\n");
                lastType = type;
            }
            //            if (type == PropertyType.Miscellaneous || type == PropertyType.String) {
            //                continue;
            //            }
            String propName = item.toString();
            switch (item) {
            case Identifier_Status:
            case Bidi_Mirroring_Glyph:
            case Bidi_Paired_Bracket:
            case Confusable_MA:
            case Idn_Mapping:
            case kSimplifiedVariant:
            case kTraditionalVariant:
                break;
            default: continue;
            }
            if (item == UcdProperty.Identifier_Status) {
                int debug = 0;
            }
            System.out.println(item + (item.getCardinality() == ValueCardinality.Singleton ? "" : "\t\t**** " + item.getCardinality()));
            try {
                UnicodeMap<String> map = latest.load(item);
                Set<String> values = map.values();
                System.out.println("  values:\t" + clip(values));

                UnicodeProperty uprop = latest.getProperty(propName);

                Set<Enum> enums = item.getEnums();
                if (enums != null) {
                    Set<String> flatValues = flattenValues(values);
                    System.out.println("  enums: \t" + clip(enums));
                    Set<String> enumStrings = getStrings(enums);
                    Collection<String> exceptions = propExceptions.get(propName);
                    if (exceptions != null) {
                        enumStrings.removeAll(exceptions);
                    }
                    if (!enumStrings.equals(flatValues)) {
                        System.out.println("\t" + "≠ VALUES!!!" + showDiff("enums", enumStrings, "values", flatValues));
                    }
                    for (String pval : uprop.getAvailableValues()) {
                        uprop.getValueAliases(pval);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    static final Splitter BAR = Splitter.on('|').omitEmptyStrings().trimResults();

    private static Set<String> flattenValues(Set<String> values) {
        Set<String> result = new LinkedHashSet<String>();
        for (String item : values) {
            result.addAll(BAR.splitToList(item));
        }
        return result;
    }

    static Multimap<String,String> propExceptions = ImmutableMultimap.<String,String>builder()
            .putAll("Script", "Katakana_Or_Hiragana, Japanese, Korean, Han_with_Bopomofo, Math_Symbols, Emoji_Symbols, Other_Symbols, Unwritten".split(", "))
            .putAll("Script_Extensions", "Katakana_Or_Hiragana, Japanese, Korean, Han_with_Bopomofo, Math_Symbols, Emoji_Symbols, Other_Symbols, Unwritten".split(", "))
            .putAll("Canonical_Combining_Class", "CCC133, Attached_Below_Left".split(", "))
            .putAll("General_Category", "Other, Letter, Cased_Letter, Mark, Number, Punctuation, Symbol, Separator".split(", "))
            .build();

    private static String showDiff(String as, Set<String> a, String bs, Set<String> b) {
        // TODO Auto-generated method stub
        return as + "-" + bs + ": " + diff(a,b) + "; " + bs + "-" + as + ": " + diff(b,a);
    }
    private static <T> Set<T> diff(Collection<T> a, Collection<T> b) {
        Set<T> result = new LinkedHashSet<T>();
        result.addAll(a);
        result.removeAll(b);
        return result;
    }
    private static Set<String> getStrings(Collection enums) {
        Set<String> result = new LinkedHashSet<String>();
        for (Object item : enums) {
            result.add(String.valueOf(item));
        }
        return result;
    }
    private static String clip(Collection availableValues) {
        return availableValues.size() > 24 ? new ArrayList(availableValues).subList(0, 23) + ", …" : availableValues.toString();
    }
}
