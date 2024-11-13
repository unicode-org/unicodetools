package org.unicode.tools;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.PropertyStatus.PropertyScope;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.ValueCardinality;
import org.unicode.text.utility.Settings;

public class ListProps {

    private static final String BIN_PROPS = Settings.Output.BIN_DIR;
    private static final UcdProperty DEBUG_LIST_VALUES = null; // UcdProperty.Confusable_MA;
    private static String ONLY_PROP = null; // "Emoji";
    static final boolean ONLY_JSP = true;

    public static final Set<PropertyStatus> SKIP_JSP_STATUS =
            ImmutableSet.of(
                    PropertyStatus.Deprecated,
                    PropertyStatus.Obsolete,
                    PropertyStatus.Stabilized,
                    PropertyStatus.Contributory,
                    PropertyStatus.Internal);

    public static void main(String[] args) {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make();
        System.out.println(ListProps.class.getName() + ": UCD: " + latest.getUcdVersion());
        new File(BIN_PROPS).mkdirs();
        //        if (true) {
        //             Set<Script_Values> sc = EnumSet.copyOf(latest.loadEnum(UcdProperty.Script,
        // Script_Values.class).values());
        //             sc.remove(Script_Values.Inherited);
        //             sc.remove(Script_Values.Common);
        //             sc.remove(Script_Values.Unknown);
        //            System.out.println(sc.size());
        //            return;
        //        }
        //        if (true) {
        //            UnicodeSet ep = latest.loadEnum(UcdProperty.Extended_Pictographic,
        // UcdPropertyValues.Binary.class).getSet(Binary.Yes);
        //            UnicodeSet em = latest.loadEnum(UcdProperty.Emoji,
        // UcdPropertyValues.Binary.class).getSet(Binary.Yes);
        //            UnicodeSet combined = new UnicodeSet(ep).addAll(em).freeze();
        //            PropertyLister pl = new PropertyLister(latest);
        //            System.out.println(
        //                    pl.listSet(combined,
        //                            UcdProperty.Extended_Pictographic.toString(),
        //                            new StringBuilder()));
        //            return;
        //        }
        PropertyType lastType = null;
        Set<String> skipped = new LinkedHashSet<>();
        Set<String> failures = new LinkedHashSet<>();
        Set<String> unknownScope = new LinkedHashSet<>();
        main:
        for (UcdProperty item : UcdProperty.values()) {
            String propName = item.toString();
            if (ONLY_PROP != null && !propName.contains(ONLY_PROP)) {
                continue;
            }
            PropertyType type = item.getType();
            ValueCardinality cardinality = item.getCardinality();
            if (type != lastType) {
                // System.out.println("\n" + type + "\n");
                lastType = type;
            }
            EnumSet<PropertyStatus> status = PropertyStatus.getPropertyStatusSet(item);

            UnicodeMap<String> map = latest.load(item);
            Set<String> values = map.values();

            PropertyScope scope = PropertyStatus.getScope(propName);
            String itemInfo =
                    item
                            + "\tType:\t"
                            + type
                            + "\tStatus:\t"
                            + CollectionUtilities.join(status, ", ")
                            + "\tCard:\t"
                            + cardinality
                            + "\tDefVal:\t"
                            + IndexUnicodeProperties.getDefaultValue(
                                    item, Settings.LATEST_VERSION_INFO)
                            + "\tScope:\t"
                            + scope
                            + "\tOrigin:\t"
                            + PropertyStatus.getOrigin(propName)
                            + "\tValues:\t"
                            + clip(values);
            if (scope == PropertyScope.Unknown) {
                unknownScope.add(propName);
            }
            if (item == DEBUG_LIST_VALUES) {
                for (String value : map.values()) {
                    UnicodeSet uset = map.getSet(value);
                    System.out.println(
                            value + "\t" + Utility.hex(value) + "\t" + uset.toPattern(false));
                }
            }

            if (ONLY_JSP) {
                if (!Collections.disjoint(status, SKIP_JSP_STATUS)) {
                    skipped.add(itemInfo);
                    continue main;
                }
                if (propName.startsWith("k")) {
                    switch (type) {
                        case Miscellaneous:
                        case String:
                            if (item == UcdProperty.kSimplifiedVariant
                                    || item == UcdProperty.kTraditionalVariant) {
                                break;
                            }
                            skipped.add(itemInfo);
                            continue main;
                        default:
                            break;
                    }
                }
            }
            System.out.println("➕\t" + itemInfo);
            try {

                UnicodeProperty uprop = latest.getProperty(propName);

                Set<Enum> enums = item.getEnums();
                if (enums != null) {
                    Set<String> flatValues = flattenValues(values);
                    Set<String> enumStrings = getStrings(enums);
                    Collection<String> exceptions = propExceptions.get(propName);
                    if (exceptions != null) {
                        enumStrings.removeAll(exceptions);
                    }
                    if (!enumStrings.equals(flatValues)) {
                        System.out.println(
                                "\t"
                                        + "≠ VALUES!!!\t"
                                        + showDiff("enums", enumStrings, "values", flatValues));
                        failures.add(propName);
                    }
                    for (String pval : uprop.getAvailableValues()) {
                        uprop.getValueAliases(pval);
                    }
                }
                latest.internalStoreCachedMap(BIN_PROPS, item, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String skip : skipped) {
            System.out.println("➖\t" + skip);
        }
        if (!failures.isEmpty()) {
            System.err.println("FAILURES: " + failures);
        }
        if (!unknownScope.isEmpty()) {
            System.err.println("UNKNOWN SCOPE: " + unknownScope);
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

    static Multimap<String, String> propExceptions =
            ImmutableMultimap.<String, String>builder()
                    .putAll(
                            "Script",
                            "Katakana_Or_Hiragana, Japanese, Korean, Han_with_Bopomofo, Math_Symbols, Emoji_Symbols, Other_Symbols, Unwritten"
                                    .split(", "))
                    .putAll(
                            "Script_Extensions",
                            "Katakana_Or_Hiragana, Japanese, Korean, Han_with_Bopomofo, Math_Symbols, Emoji_Symbols, Other_Symbols, Unwritten"
                                    .split(", "))
                    .putAll("Canonical_Combining_Class", "CCC133, Attached_Below_Left".split(", "))
                    .putAll(
                            "General_Category",
                            "Other, Letter, Cased_Letter, Mark, Number, Punctuation, Symbol, Separator"
                                    .split(", "))
                    .putAll("Identifier_Type", "Aspirational".split(", "))
                    .putAll(
                            "Word_Break",
                            "E_Base_GAZ, Glue_After_Zwj, E_Base, E_Modifier".split(", "))
                    .putAll(
                            "Grapheme_Cluster_Break",
                            "E_Base_GAZ, Glue_After_Zwj, E_Base, E_Modifier".split(", "))
                    .build();

    private static String showDiff(String as, Set<String> a, String bs, Set<String> b) {
        // TODO Auto-generated method stub
        return as + " - " + bs + ": " + diff(a, b) + "; " + bs + " - " + as + ": " + diff(b, a);
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
        return availableValues.size() > 24
                ? new ArrayList(availableValues).subList(0, 23) + ", …"
                : availableValues.toString();
    }
}
