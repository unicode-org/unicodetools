package org.unicode.tools.emoji;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.MultiComparator;
//import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class EmojiOrder {
    private static final UnicodeSet MODIFIER_BASES = EmojiData.EMOJI_DATA.getModifierBases();
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
//    static final ImmutableMap<String,ImmutableList<String>> hack = ImmutableMap.of(
//            "👁", ImmutableList.of("👁️‍🗨️ "),
//            "💏", ImmutableList.of("👩‍❤️‍💋‍👨", "👨‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩"),
//            "💑", ImmutableList.of("👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩"),
//            "👪", ImmutableList.of(
//                    "👨‍👩‍👦", "👨‍👩‍👧", "👨‍👩‍👧‍👦", "👨‍👩‍👦‍👦", "👨‍👩‍👧‍👧", 
//                    "👨‍👨‍👦", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦‍👦", "👨‍👨‍👧‍👧",
//                    "👩‍👩‍👦", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧")
//            );

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
    public static final EmojiOrder STD_ORDER = new EmojiOrder(Emoji.VERSION_TO_GENERATE, "emojiOrdering.txt");

    public final MapComparator<String>     mp;
    public final Relation<String, String>  orderingToCharacters;
    public final UnicodeMap<String>  charactersToOrdering = new UnicodeMap<>();
    public final Comparator<String>        codepointCompare;
    public final UnicodeMap<MajorGroup>  majorGroupings = new UnicodeMap<>(); 
    public final Map<String, Integer>  groupOrder; 
    final EmojiData emojiData;
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
                .setSortBeforeOthers(false)
                .setDoFallback(false)
                ;
        HashMap<String, Integer> _groupOrder = new LinkedHashMap<String,Integer>();
        Map<String, MajorGroup> _categoryToMajor = new LinkedHashMap<>();
        orderingToCharacters = loadOrdering(version, file, mp, _groupOrder, _categoryToMajor);
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

    Relation<String, String> loadOrdering(VersionInfo version, String sourceFile, 
            MapComparator<String> mapComparator, 
            Map<String, Integer> _groupOrder, 
            Map<String, MajorGroup> _categoryToMajor) {
        //System.out.println(sourceFile);
        Relation<String, String> result = Relation.of(new LinkedHashMap<String, Set<String>>(), LinkedHashSet.class);
        Set<String> sorted = new LinkedHashSet<>();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        MajorGroup majorGroup = null;
        EmojiIterator ei = new EmojiIterator(emojiData, false);
        for (String line : FileUtilities.in(Settings.DATA_DIR + "/emoji/" + version.getVersionString(2, 2) + "/source",
                sourceFile)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.contains("\u20E3")) {
                int debug = 0;
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
            line = line.replace(Emoji.TEXT_VARIANT_STRING, "").replace(Emoji.EMOJI_VARIANT_STRING, "");
            for (String string : ei.set(line)) {
                // NOTE: all emoji variant selectors have been removed at this point
                if (sorted.contains(string)) {
                    continue;
                }
                //System.out.println("Adding: " + Utility.hex(string) + "\t" + string);
                add(result, sorted, majorGroup, lastLabel, string);
                addVariants(result, sorted, majorGroup, lastLabel, string); 

                //                ImmutableList<String> list = hack.get(string);
                //                if (list != null) {
                //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                //                    for (String string2 : list) {
                //                        //System.err.println("Adding " + show(string2));
                //                        add(result, sorted, majorGroup, lastLabel, string2); 
                //                        addVariants(result, sorted, majorGroup, lastLabel, string2);
                //                    }
                //                }

                if (Emoji.GENDER_BASE.contains(string)) {
                    addVariants(result, sorted, majorGroup, lastLabel, string + "\u200d\u2642"); 
                    addVariants(result, sorted, majorGroup, lastLabel, string + "\u200d\u2640"); 
                }
                //                // add/remove all variant strings
                //                if (string.contains(Emoji.JOINER_STRING) || emojiData.getKeycapBases().contains(string.charAt(0))) { 
                //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                //                }
            }
        }

        Set<String> missing = new UnicodeSet(emojiData.getSortingChars())
        .removeAll(emojiData.getModifierSequences())
        .addAllTo(new LinkedHashSet<String>());
        missing.removeAll(sorted);
        for (Iterator<String> it = missing.iterator(); it.hasNext();) {
            String s = it.next();
            if (s.endsWith(Emoji.EMOJI_VARIANT_STRING)) {
                it.remove();
            }
        }
        if (!missing.isEmpty() && !sourceFile.startsWith("alt")) {
            result.putAll("other", missing);
            System.err.println("Missing some orderings: ");
            for (String s : missing) {
                System.err.print(s + " ");
            }
            System.err.println();

            for (String s : missing) {
                System.err.println("\t" + s + "\t\t" + Emoji.show(s));
            }
            throw new IllegalArgumentException();
        }
        sorted.addAll(missing);
        mapComparator.add(sorted);
        mapComparator.setErrorOnMissing(true);
        mapComparator.freeze();
        result.freeze();
        return result;
    }

    private void addAllModifiers(Relation<String, String> result, Set<String> sorted, Output<Set<String>> lastLabel, MajorGroup majorGroup, String... strings) {
        for (String mod : emojiData.MODIFIERS) {
            for (String string : strings) {
                final String addedModifier = emojiData.addModifier(string, mod);
                if (addedModifier != null) {
                    add(result, sorted, majorGroup, lastLabel, addedModifier); 
                }
            }
        }
    }

    /**
     * Add the string, with all emoji variants either present or missing.
     * @param result
     * @param sorted
     * @param majorGroup
     * @param lastLabel
     * @param string
     */
    private void addVariants(Relation<String, String> result, Set<String> sorted, MajorGroup majorGroup, Output<Set<String>> lastLabel, String string) {
        // add variant form, since it is interior
        String withEmojiVariant = emojiData.addEmojiVariants(string);
        add(result, sorted, majorGroup, lastLabel, withEmojiVariant);

        String withoutFinal = null;
        if (withEmojiVariant.endsWith(Emoji.EMOJI_VARIANT_STRING)) {
            withoutFinal = withEmojiVariant.substring(0, withEmojiVariant.length()-Emoji.EMOJI_VARIANT_STRING.length());
            add(result, sorted, majorGroup, lastLabel, withoutFinal); 
        }

        String noVariant = string.replace(Emoji.EMOJI_VARIANT_STRING,"");
        add(result, sorted, majorGroup, lastLabel, noVariant);

        //Now add the modifier sequences
        addAllModifiers(result, sorted, lastLabel, majorGroup, string, withEmojiVariant, withoutFinal, noVariant);
    }

    private void add(Relation<String, String> result, Set<String> sorted, MajorGroup majorGroup, Output<Set<String>> lastLabel, String string) {
        if (sorted.contains(string)) {
            return; // already done.
        }
        //        if (string.contains("⚕")) {
        //            System.out.println("\t" + string);
        //        }
        // System.out.println(string + "\t" + Utility.hex(string));
        if (string.equals("👁‍🗨")) {
            int debug = 0;
        }
        majorGroupings.put(string, majorGroup);
        String full = emojiData.addEmojiVariants(string);
        boolean didAdd = sorted.add(string);
        for (String item : lastLabel.value) {
            result.put(item, string);
        }
        final String first = lastLabel.value.iterator().next();
        charactersToOrdering.put(string, first);
        if (!full.equals(string)) {
            charactersToOrdering.put(full, first);
        }
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

    public <T extends Appendable> T appendCollationRules(T outText, UnicodeSet... characters) {
        try {
            boolean needRelation = true;
            boolean haveFlags = false;
            boolean isFirst = true;
            Set<String> temp = sort(codepointCompare, characters);

            String lastGroup = null;
            // HACK
            outText.append("& [first trailing]");
            isFirst = false;
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
                    String withoutTrail = withoutTrailingVariant(s);
                    String quoted = quoteSyntax(withoutTrail);
                    String quoted2 = quoteSyntax(s.replaceAll(Emoji.EMOJI_VARIANT_STRING, ""));
                    outText.append("\n<").append(quoted);
                    if (!quoted2.equals(quoted)) {
                        outText.append(" = ").append(quoted2);
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

    private String withoutTrailingVariant(String s) {
        return s.endsWith(Emoji.EMOJI_VARIANT_STRING) ? s.substring(0, s.length()-1) : s;
    }

    private String quoteSyntax(String source) {
        for (String s : Arrays.asList("*", "#", "\u20E3", "\u20E0", Emoji.EMOJI_VARIANT_STRING)) {
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