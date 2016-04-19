package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.MapComparator;
//import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.ModifierStatus;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.EmojiData.VariantHandling;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class EmojiOrder {
    private static final boolean DEBUG = false;

    public enum MajorGroup {
        Smileys_and_People,
        Animals_and_Nature,
        Food_and_Drink,
        Travel_and_Places,
        Activities,
        Objects,
        Symbols,
        Flags;
        public String toString() {
            return name().replace("_and_", " & ").replace('_', ' ');
        };
    }

    //static final EmojiData emojiDataLast = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
    public static final StringComparator PLAIN_STRING_COMPARATOR = new UTF16.StringComparator(true, false, 0);
    static final boolean USE_ORDER = true;
    static final ImmutableMap<String,ImmutableList<String>> hack = ImmutableMap.of(
            "👁", ImmutableList.of("👁‍🗨"),
            "💏", ImmutableList.of("👩‍❤️‍💋‍👨", "👨‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩"),
            "💑", ImmutableList.of("👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩"),
            "👪", ImmutableList.of(
                    "👨‍👩‍👦", "👨‍👩‍👧", "👨‍👩‍👧‍👦", "👨‍👩‍👦‍👦", "👨‍👩‍👧‍👧", 
                    "👨‍👨‍👦", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦‍👦", "👨‍👨‍👧‍👧",
                    "👩‍👩‍👦", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧")
            );

    //    static {
    //        for (Entry<String, ImmutableList<String>> entry : hack.entrySet()) {
    //            System.out.println(show(entry.getKey()));
    //            for (String s : entry.getValue()) {
    //                System.out.println("\t" + s + "\t\t" + show(s));
    //            }
    //        }
    //    }

    public static final Comparator<String> UCA_COLLATOR = (Comparator<String>)(Comparator)Collator.getInstance(ULocale.ROOT);
    public static final Comparator<String> FULL_COMPARATOR =
            new MultiComparator<String>(
                    EmojiOrder.UCA_COLLATOR,
                    PLAIN_STRING_COMPARATOR);

    //public static final EmojiOrder ALT_ORDER = new EmojiOrder(Emoji.VERSION_BETA, "altOrder.txt");
    public static final EmojiOrder STD_ORDER = new EmojiOrder(Emoji.VERSION_BETA, "emojiOrdering.txt");

    public final MapComparator<String>     mp;
    public final Relation<String, String>  orderingToCharacters;
    public final UnicodeMap<String>  charactersToOrdering = new UnicodeMap<>();
    public final Comparator<String>        codepointCompare;
    public final UnicodeMap<MajorGroup>  majorGroupings = new UnicodeMap<>(); 
    public final Map<String, Integer>  groupOrder; 
    private final EmojiData emojiData;
    private final Map<String, MajorGroup> categoryToMajor;

    /**
     * @return the categoryToMajor
     */
    public MajorGroup getMajorGroupFromCategory(String group) {
        return categoryToMajor.get(group);
    }

    public EmojiOrder(VersionInfo version, String file) {
        emojiData = EmojiData.of(version);
        mp  = new MapComparator<String>()
                .setErrorOnMissing(false)
                .setSortBeforeOthers(true)
                .setDoFallback(false)
                ;
        HashMap<String, Integer> _groupOrder = new LinkedHashMap<String,Integer>();
        Map<String, MajorGroup> _categoryToMajor = new LinkedHashMap<>();
        orderingToCharacters = getOrdering(version, file, mp, _groupOrder, _categoryToMajor);
        mp.freeze();
        majorGroupings.freeze();
        groupOrder = Collections.unmodifiableMap(_groupOrder);
        categoryToMajor = Collections.unmodifiableMap(_categoryToMajor);
        codepointCompare           =
                new MultiComparator<String>(
                        mp,
                        EmojiOrder.UCA_COLLATOR,
                        PLAIN_STRING_COMPARATOR);
    }

    Relation<String, String> getOrdering(VersionInfo version, String sourceFile, 
            MapComparator<String> mapComparator, 
            Map<String, Integer> _groupOrder, 
            Map<String, MajorGroup> _categoryToMajor) {
        //System.out.println(sourceFile);
        Relation<String, String> result = Relation.of(new LinkedHashMap<String, Set<String>>(), LinkedHashSet.class);
        Set<String> sorted = new LinkedHashSet<>();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        MajorGroup majorGroup = null;
        for (String line : FileUtilities.in(Settings.DATA_DIR + "/emoji/" + version.getVersionString(2, 2) + "/source",
                sourceFile)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (DEBUG) System.out.println(line);
            if (line.startsWith("@")) {
                majorGroup = MajorGroup.valueOf(line.substring(1).trim());
                continue;
            }
            line = Emoji.getLabelFromLine(lastLabel, line);
            for (String item : lastLabel.value) {
                if (!_groupOrder.containsKey(item)) {
                    _groupOrder.put(item, _groupOrder.size());
                }
                MajorGroup major = _categoryToMajor.get(item);
                if (major == null) {
                    _categoryToMajor.put(item, majorGroup);
                } else if (major != majorGroup) {
                    throw new IllegalArgumentException("Conflicting major categories");
                }
            }
            line = Emoji.UNESCAPE.transform(line);
            for (int i = 0; i < line.length();) {
                String string = Emoji.getEmojiSequence(line, i);
                i += string.length();
                if (emojiData.skipEmojiSequence(string)) {
                    continue;
                }
                if (sorted.contains(string)) {
                    continue;
                }
                //System.out.println("Adding: " + Utility.hex(string) + "\t" + string);
                add(result, sorted, majorGroup, lastLabel, string);
                ImmutableList<String> list = hack.get(string);
                if (list != null) {
                    for (String string2 : list) {
                        //System.err.println("Adding " + show(string2));
                        add(result, sorted, majorGroup, lastLabel, string2); 
                    }
                }
                if (emojiData.getModifierBases().contains(string)) {
                    for (String string2 : emojiData.getModifierStatusSet(ModifierStatus.modifier)) {
                        add(result, sorted, majorGroup, lastLabel, string+string2); 
                    }
                }
                if (emojiData.getKeycapBases().contains(string.charAt(0))) { 
                    // add variant form, since it is interior
                    String string2 = emojiData.addEmojiVariants(string, Emoji.EMOJI_VARIANT, VariantHandling.all);
                    if (!string2.equals(string)) {
                        add(result, sorted, majorGroup, lastLabel, string2); 
                    }
                    string2 = emojiData.addEmojiVariants(string, Emoji.TEXT_VARIANT, VariantHandling.all);
                    if (!string2.equals(string)) {
                        add(result, sorted, majorGroup, lastLabel, string2); 
                    }
//                    string2 = string.replace(Emoji.EMOJI_VARIANT_STRING, "");
//                    if (!string2.equals(string)) {
//                        add(result, sorted, majorGroup, lastLabel, string2); 
//                    }
                }
            }
        }

        Set<String> missing = new UnicodeSet(emojiData.getSortingChars())
        .removeAll(emojiData.getModifierSequences())
        .addAllTo(new LinkedHashSet<String>());
        missing.removeAll(sorted);
        if (!missing.isEmpty() && !sourceFile.startsWith("alt")) {
            result.putAll("other", missing);
            System.err.println("Missing some orderings: ");
            for (String s : missing) {
                System.err.print(s + " ");
            }
            System.err.println();

            for (String s : missing) {
                System.err.println("\t" + s + "\t\t" + show(s));
            }
            throw new IllegalArgumentException();
        }
        sorted.addAll(missing);
        mapComparator.add(sorted);
        mapComparator.freeze();
        result.freeze();
        return result;
    }

    private static String show(String key) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(key)) {
            if (b.length() != 0) {
                b.append(' ');
            }
            b.append("U+" + Utility.hex(cp) + " " + UTF16.valueOf(cp));
        }
        return b.toString();
    }

    private void add(Relation<String, String> result, Set<String> sorted, MajorGroup majorGroup, Output<Set<String>> lastLabel, String string) {
        majorGroupings.put(string, majorGroup);
        sorted.add(string);
        for (String item : lastLabel.value) {
            result.put(item, string);
        }
        charactersToOrdering.put(string, lastLabel.value.iterator().next());
    }

    public static void main(String[] args) throws Exception {
        STD_ORDER.showLines(true, null);
//        checkRBC();
        //LinkedHashSet<String> foo = Emoji.FLAGS.addAllTo(new LinkedHashSet());
        //System.out.println(CollectionUtilities.join(foo, " "));
        //        showOrderGroups();
        //        showOrder();
        //        STD_ORDER.show();
        //        ALT_ORDER.show();
    }

//    private static void checkRBC() throws Exception {
//        UnicodeSet APPLE_COMBOS = emojiData.getZwjSequencesNormal();
//        UnicodeSet APPLE_COMBOS_WITHOUT_VS = emojiData.getZwjSequencesAll();
//
//        String rules = EmojiOrder.STD_ORDER.appendCollationRules(new StringBuilder(), 
//                new UnicodeSet(emojiData.getChars()).removeAll(Emoji.DEFECTIVE), 
//                APPLE_COMBOS, 
//                APPLE_COMBOS_WITHOUT_VS)
//                .toString();
//        final RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(rules);
//        ruleBasedCollator.setStrength(Collator.IDENTICAL);
//        ruleBasedCollator.freeze();
//        Comparator<String> EMOJI_COMPARATOR = (Comparator<String>) (Comparator) ruleBasedCollator;
//        int x = EMOJI_COMPARATOR.compare("#️⃣","☺️");
//    }


//    private void show() {
//        for (Entry<String, Set<String>> labelToSet : orderingToCharacters.keyValuesSet()) {
//            String label = labelToSet.getKey();
//            System.out.print(label + "\t");
//            int count = label.length();
//            for (String cp : labelToSet.getValue()) {
//                if (++count > 32) {
//                    count = 0;
//                    System.out.println();
//                }
//                System.out.print(cp + " ");
//            }
//            System.out.println();
//        }
//    }

    private void showLines(boolean spreadsheet, Source source) {
        Set<String> filter = source == null ? null : Collections.unmodifiableSet(
                EmojiStats.totalMissingData.get(source).addAllTo(new HashSet<String>()));
        MajorGroup lastMajorGroup = null;
        int i = 0;
        System.out.println("#Main group\tCharacters in order\tInternal subgroup");
        for (Entry<String, Set<String>> labelToSet : orderingToCharacters.keyValuesSet()) {
            boolean isFirst = true;
            final String label = labelToSet.getKey();
            final Set<String> list = labelToSet.getValue();
            MajorGroup majorGroup = getMajorGroup(list); // majorGroupings.get(list.iterator().next());
            if (spreadsheet) {
                LinkedHashSet<String> filtered = new LinkedHashSet<>(list);
                if (filter != null) {
                    filtered.retainAll(filter);
                }
                if (!filtered.isEmpty()) {
                    System.out.println(majorGroup + "\t" + CollectionUtilities.join(filtered, " ") + "\t" + label);
                }
                continue;
            }
            for (String cp : list) {
                if (emojiData.getModifierSequences().contains(cp)) {
                    continue;
                }
                ++i;
                if (majorGroup != lastMajorGroup) {
                    if (lastMajorGroup != null) {
                        System.out.println("# " + lastMajorGroup + " count:\t" + i);
                        i = 0;
                    }
                    System.out.println("####################");
                    System.out.println("# " + majorGroup);
                    System.out.println("####################");
                    lastMajorGroup = majorGroup;
                }
                if (isFirst) {
                    System.out.println("# " + label);
                    isFirst = false;
                }
                System.out.println(Utility.hex(cp) 
                        + " ; " + Emoji.getNewest(cp).getShortName() 
                        + " # " + cp 
                        + " " + Emoji.getName(cp, false, null));
            }
        }
        if (!spreadsheet) {
            System.out.println("# " + lastMajorGroup + " count:\t" + i);
        }
    }

    public <T extends Appendable> T appendCollationRules(T outText, UnicodeSet... characters) {
        try {
            boolean needRelation = true;
            boolean haveFlags = false;
            boolean isFirst = true;
            Set<String> temp = sort(codepointCompare, characters);

            String lastGroup = null;
            for (String s : temp) {
                String group = charactersToOrdering.get(s);
                if (!Objects.equal(group,lastGroup)) {
                    needRelation = true;
                    lastGroup = group;
                }
                boolean multiCodePoint = s.codePointCount(0, s.length()) > 1;
                if (isFirst) {
                    if (multiCodePoint) {
                        throw new IllegalArgumentException("Cannot have first item with > 1 codepoint: " + s);
                    }
                    outText.append("&").append(s);
                    isFirst = false;
                    continue;
                }
                if (multiCodePoint) { // flags and keycaps
                    if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                        if (!haveFlags) {
                            // put all the 26 regional indicators in order at
                            // this point
                            StringBuilder b = new StringBuilder("\n<*");
                            for (int i = Emoji.FIRST_REGIONAL; i <= Emoji.LAST_REGIONAL; ++i) {
                                b.appendCodePoint(i);
                            }
                            outText.append(b);
                            haveFlags = true;
                        }
                        continue;
                    }
                    // keycaps, zwj sequences, can't use <* syntax
                    String quoted = quoteSyntax(s);
                    String quoted2 = quoted.replaceAll(Emoji.EMOJI_VARIANT_STRING, "");
                    if (!quoted2.equals(quoted)) {
                        outText.append("\n<").append(quoted);
                        outText.append(" = ").append(quoted2);
                    } else {
                        outText.append("\n<").append(quoted);
                    }
                    needRelation = true;
                } else {
                    if (needRelation) {
                        outText.append("\n<*");
                        needRelation = false;
                    }
                    outText.append(s);
                    //                    // break arbitrarily (but predictably)
                    //                    int bottomBits = s.codePointAt(0) & 0xF;
                    //                    needRelation = bottomBits == 0;
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error",e);
        }
        return outText;
    }

    private String quoteSyntax(String source) {
        for (String s : Arrays.asList("*", "#", "\u20E3", "\u20E0")) {
            if (source.contains(s)) {
                source = source.replace(s, "\\u" + Utility.hex(s));
            }
        }
        return source;
    }

    public static Set<String> sort(Comparator<String> comparator, UnicodeSet... characters) {
        TreeSet<String> temp = new TreeSet<String>(comparator);
        for (UnicodeSet uset : characters) {
            uset.addAllTo(temp);
        }
        return Collections.unmodifiableSortedSet(temp);
    }

    public MajorGroup getMajorGroup(Iterable<String> values) {
        for (String s : values) {
            MajorGroup result = majorGroupings.get(s);
            if (result != null) {
                return result;
            }
        }
        throw new IllegalArgumentException();
    }

    public int getGroupOrder(String cat1) {
        return groupOrder.get(cat1);
    }
}