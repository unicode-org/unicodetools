package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.MultiComparator;
//import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiData.VariantFactory;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.text.UnicodeSetSpanner.TrimOption;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class EmojiOrder {
    private static final UnicodeSet MODIFIER_BASES = EmojiData.EMOJI_DATA.getModifierBases();
    private static final ConcurrentHashMap<VersionInfo, EmojiOrder> VERSION_TO_DATA = new ConcurrentHashMap<>();

    private static final boolean DEBUG = false;

    public enum MajorGroup {
        Smileys,
        People,
        Component,
        Animals_and_Nature,
        Food_and_Drink,
        Travel_and_Places,
        Activities,
        Objects,
        Symbols,
        Flags,
        Other;
        static final MajorGroup Smileys_and_People = Smileys;
        public String toString() {
            throw new ICUException();
            //return name().replace("_and_", " & ").replace('_', ' ');
        };
        public String toPlainString() {
            return name().replace("_and_", " & ").replace('_', ' ');
        };
        public String toHTMLString() {
            return toPlainString().replace("&", "&amp;");
        }
        public static MajorGroup fromString(String source) {
            source = source.trim();
            if (source.equals("Smileys_and_People")) {
                return Smileys;
            }
            return valueOf(source);
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

    public static final EmojiOrder BETA_ORDER = EmojiOrder.of(Emoji.VERSION_BETA);
    public static final EmojiOrder STD_ORDER = EmojiOrder.of(Emoji.VERSION_TO_GENERATE);

    public final MapComparator<String>     mp;
    public final Relation<String, String>  orderingToCharacters;
    public final UnicodeMap<String>  charactersToOrdering = new UnicodeMap<>();
    public final Comparator<String>        codepointCompare;
    public final Comparator<String>        codepointCompareSeparateDefects;
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


    public static EmojiOrder of(VersionInfo version) {
        EmojiOrder result = VERSION_TO_DATA.get(version);
        if (result == null) {
            VERSION_TO_DATA.put(version, result = new EmojiOrder(version, "emojiOrdering.txt"));
        }
        return result;
    }

    private EmojiOrder(VersionInfo version, String file) {
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
        codepointCompareSeparateDefects           =
                new MultiComparator<String>(
                        mp,
                        EmojiOrder.UCA_COLLATOR,
                        PLAIN_STRING_COMPARATOR,
                        new UTF16.StringComparator(true,false,0));
        if(DEBUG) {
            String last = "";
            for (String s : Arrays.asList("\u2017", "\u002D", "\uFF0D")) {
                System.out.println(
                        Utility.hex(last) + "/" + last 
                        + " vs " 
                        + Utility.hex(s) + "/" + s 
                        + ": " + codepointCompare.compare(last, s)
                        + " map: " + mp.getNumericOrder(s));
                last = s;
            }
        }
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
        final String directory = Settings.DATA_DIR + "/emoji/" + version.getVersionString(2, 2) + "/source";
        try (PrintWriter reformatted = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, sourceFile)) {
            for (String line : FileUtilities.in(EmojiOrder.class,
                    sourceFile)) {
                if (line.isEmpty() || line.startsWith("#") && !line.startsWith("#⃣") && !line.startsWith("#️⃣")) {
                    continue;
                }
                if (DEBUG) System.out.println(line);

                line = Emoji.UNESCAPE.transform(line);
                line = line.replace(Emoji.TEXT_VARIANT_STRING, "").replace(Emoji.EMOJI_VARIANT_STRING, "");

                if (line.contains("keycap")) {
                    int debug = 0;
                }

                if (line.startsWith("@@")) {
                    majorGroup = MajorGroup.fromString(line.substring(2).trim());
                    reformatted.println(line);
                    continue;
                }
                if (line.startsWith("@")) {
                    String item = line.substring(1).trim();
                    if (!_groupOrder.containsKey(item)) {
                        _groupOrder.put(item, _groupOrder.size());
                    }
                    MajorGroup major = _categoryToMajor.get(item);
                    if (major == null) {
                        _categoryToMajor.put(item, majorGroup);
                    } else if (major != majorGroup) {
                        throw new IllegalArgumentException("Conflicting major categories");
                    }
                    lastLabel.value.clear();
                    lastLabel.value.add(item);
                    reformatted.println("@" + item);
                    continue;
                }
                //                String oldLine = line;
                //                line = Emoji.getLabelFromLine(lastLabel, line);
                //                for (String item : lastLabel.value) {
                //                    if (!_groupOrder.containsKey(item)) {
                //                        _groupOrder.put(item, _groupOrder.size());
                //                    }
                //                    MajorGroup major = _categoryToMajor.get(item);
                //                    if (major == null) {
                //                        _categoryToMajor.put(item, majorGroup);
                //                    } else if (major != majorGroup) {
                //                        throw new IllegalArgumentException("Conflicting major categories");
                //                    }
                //                    // hack for now
                //                    if (oldLine.contains("\t")) {
                //                        reformatted.println("@" + item);
                //                    }
                //                }
                if (line.indexOf("🤝") >= 0) {
                    int debug = 0;
                }
                boolean isFirst = true;
                for (String string : ei.set(line)) {
                    // NOTE: all emoji variant selectors have been removed at this point
                    if (sorted.contains(string)) {
                        continue;
                    }
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        reformatted.print(' ');
                    }
                    reformatted.print(EmojiData.EMOJI_DATA.addEmojiVariants(string));
                    //System.out.println("Adding: " + Utility.hex(string) + "\t" + string);
                    add(result, sorted, majorGroup, lastLabel, string);
                    addVariants(result, sorted, majorGroup, lastLabel, string); 
                    
                    switch (string) {
                    case "👭": 
                        addVariants(result, sorted, majorGroup, lastLabel, "👩‍🤝‍👩"); 
                        break;
                    case "👫": 
                        addVariants(result, sorted, majorGroup, lastLabel, "👩‍🤝‍👨"); 
                        break;
                    case "👬": 
                        addVariants(result, sorted, majorGroup, lastLabel, "👨‍🤝‍👨");
                        break;
                    }

                    //                ImmutableList<String> list = hack.get(string);
                    //                if (list != null) {
                    //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                    //                    for (String string2 : list) {
                    //                        //System.err.println("Adding " + show(string2));
                    //                        add(result, sorted, majorGroup, lastLabel, string2); 
                    //                        addVariants(result, sorted, majorGroup, lastLabel, string2);
                    //                    }
                    //                }

                    // We have a hack for blond person, and add them explicity.
                    if (emojiData.getGenderBase().contains(string) && !string.equals("👱")) {
                        addVariants(result, sorted, majorGroup, lastLabel, string + "\u200d\u2642"); 
                        addVariants(result, sorted, majorGroup, lastLabel, string + "\u200d\u2640"); 
                    }
                    //                // add/remove all variant strings
                    //                if (string.contains(Emoji.JOINER_STRING) || emojiData.getKeycapBases().contains(string.charAt(0))) { 
                    //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                    //                }
                }
                if (!isFirst) { // skip empty lines
                    reformatted.println();
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
                System.err.println(directory);
            }
            sorted.addAll(missing);
            mapComparator.add(sorted);
            //mapComparator.setErrorOnMissing(true);
            mapComparator.freeze();
            result.freeze();
            return result;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private void addAllModifiers(Relation<String, String> result, Set<String> sorted, Output<Set<String>> lastLabel, MajorGroup majorGroup, String... strings) {
        for (String string : strings) {
            Set<String> results = emojiData.addModifiers(string);
            if (results.isEmpty()) {
                continue;
            }
            // System.out.println(string + "=>" + results);
            for (String item : results) {
                add(result, sorted, majorGroup, lastLabel, item);
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
        for (String variant : emojiData.new VariantFactory().set(string).getCombinations()) {
            add(result, sorted, majorGroup, lastLabel, variant);
        }

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
        if (string.equals("-")) {
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

            outText.append("& [last primary ignorable]<<*");

            for (String m : Arrays.asList("🦰", "🦱", "🦳", "🦲")) { // not cp order, so not Emoji.HAIR_STYLES
                outText.append(m);
            }
            for (String m : EmojiData.MODIFIERS) {
                outText.append(m);
            }
            UnicodeSet hairSkin = new UnicodeSet(EmojiData.MODIFIERS).addAll(Emoji.HAIR_STYLES_WITH_JOINERS).freeze();
            UnicodeSetSpanner HairSkinSpanner = new UnicodeSetSpanner(hairSkin);

            String lastGroup = null;
            // HACK
            outText.append("\n& [before 1]\uFDD1€");
            isFirst = false;
            Set<String> haveSeen = new HashSet<>();
            for (String s : temp) {
                if (s.startsWith(Emoji.ADULT)) {
                    int debug = 0;
                }
                String group = getCategory(s);
                if (!Objects.equal(group,lastGroup)) {
                    needRelation = true;
                    lastGroup = group;
                }
                int trimmed = hairSkin.spanBack(s, SpanCondition.SIMPLE);
                if (trimmed < s.length()) {
                    continue;
                }
                if (haveSeen.contains(s)) {
                    continue;
                }

                boolean multiCodePoint = s.codePointCount(0, s.length()) > 1;
                boolean containsModifier = EmojiData.MODIFIERS.containsSome(s);
                if (containsModifier) {
                    if (!multiCodePoint) {
                        continue; // skip single modifiers, they are ignorable
                    }
                    int last = Character.codePointBefore(s, s.length());
                    if (EmojiData.MODIFIERS.contains(last)) {
                        String oldS = s;
                        s = s.substring(0, s.length()-Character.charCount(last)); // remove last
                        if (temp.contains(s)) {
                            continue; // skip if present
                        }
                        containsModifier = EmojiData.MODIFIERS.containsSome(s);
                    }
                }
                // at this point, the only Modifiers can be medial.
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
                            needRelation = true;
                            haveFlags = true;
                        }
                        continue;
                    }

                    // keycaps, zwj sequences, can't use <* syntax

                    final String withoutAnyVS = s.replaceAll(Emoji.EMOJI_VARIANT_STRING, "");
                    if (haveSeen.contains(withoutAnyVS)) {
                        continue;
                    }
                    String quoted2 = quoteSyntax(withoutAnyVS);
                    outText.append(containsModifier ? " << " : "\n< ");
                    outText.append(quoted2);
                    haveSeen.add(withoutAnyVS);
                    needRelation = true;

                    String allVariants = EmojiData.EMOJI_DATA.addEmojiVariants(s);
                    String withoutTrail = withoutTrailingVariant(allVariants);
                    if (haveSeen.contains(withoutTrail)) {
                        continue;
                    }
                    String quoted = quoteSyntax(withoutTrail);
                    outText.append(" = ").append(quoted);
                    haveSeen.add(withoutTrail);
                } else if (Emoji.HAIR_EXPLICIT.contains(s)) { // HACK the blond person to make secondary
                    outText.append(" <<");
                    outText.append(s);
                    haveSeen.add(s);
                    needRelation = true;
                } else {
                    if (needRelation) {
                        outText.append("\n<*");
                        needRelation = false;
                    }
                    outText.append(s);
                    haveSeen.add(s);
                    //                    needRelation = true;
                    //                                        // break arbitrarily (but predictably)
                    //                                        int bottomBits = s.codePointAt(0) & 0x7;
                    //                                        needRelation = bottomBits == 0;
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error",e);
        }
        return outText;
    }

    public String getCategory(String emoji) {
        return charactersToOrdering.get(emoji);
    }

    public UnicodeSet getCharsWithCategory(String category) {
        return charactersToOrdering.getSet(category);
    }

    private String withoutTrailingVariant(String s) {
        return s.endsWith(Emoji.EMOJI_VARIANT_STRING) ? s.substring(0, s.length()-1) : s;
    }

    static final UnicodeSet NEEDS_QUOTE = new UnicodeSet("[[:Pattern_White_Space:][\\&\\[\\]#@!<;,=*]]").freeze();

    private String quoteSyntax(String source) {
        return NEEDS_QUOTE.containsNone(source) ? source :
            "'" + source.replace("'", "''") + "'";
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

    public static final UnicodeSet GENDER_OBJECTS = new UnicodeSet();
    public static final UnicodeSet GENDER_NEUTRALS = new UnicodeSet();
    static {
        for (String s : EmojiData.EMOJI_DATA.getEmojiForSortRules()) {
            CountEmoji.ZwjType type = CountEmoji.ZwjType.getType(s);
            if (type != CountEmoji.ZwjType.roleWithObject) {
                continue;
            }
            GENDER_OBJECTS.add(s.codePointBefore(s.length()));
            if (s.startsWith(Emoji.WOMAN_STR)) {
                GENDER_NEUTRALS.add(s.replace(Emoji.WOMAN_STR, Emoji.ADULT));
            }
        }
        GENDER_OBJECTS.freeze();
        GENDER_NEUTRALS.freeze();
    }

    public static void main(String[] args) {
        EmojiData EMOJI_DATA_RELEASE = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
        UnicodeSet roleBase = new UnicodeSet();
        UnicodeSet roleObject = new UnicodeSet();
        UnicodeSet temp = new UnicodeSet();
        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);

        for (String s : EMOJI_DATA_RELEASE.getAllEmojiWithDefectives()) {
            switch (CountEmoji.Category.getBucket(s)) {
            case zwj_seq_role: 
                int first = s.codePointAt(0);
                roleBase.add(first);
                temp.clear().addAll(s).remove(first)
                .remove(Emoji.JOINER)
                .remove(Emoji.EMOJI_VARIANT)
                .removeAll(EmojiData.MODIFIERS)
                .removeAll(EMOJI_DATA_RELEASE.getEmojiComponents());
                roleObject.addAll(temp);
                break;
            }
        }
        System.out.println("roleObject: " + roleObject.toPattern(false));

        UnicodeSet all = new UnicodeSet()
                .addAll(EMOJI_DATA_RELEASE.getGenderBases())
                .addAll(EMOJI_DATA_RELEASE.getHairBases())
                .addAll(EMOJI_DATA_RELEASE.getModifierBases())
                .addAll(EMOJI_DATA_RELEASE.getExplicitGender())
                .addAll(EMOJI_DATA_RELEASE.getExplicitHair())
                .addAll(roleBase)
                ;
        UnicodeSet missing = new UnicodeSet();
        Set<String> categories = new TreeSet<>();
        for (String s : all) {
            categories.add(order.getCategory(s));
        }
        for (String category : categories) {
            missing.addAll(new UnicodeSet(order.getCharsWithCategory(category)).removeAll(all));
        }
        Set<String> skip = new HashSet<>(missing.strings());
        missing.removeAll(skip).removeAll(EmojiData.EMOJI_DATA.getEmojiComponents());
        all.addAll(missing).freeze();
        System.out.println("Missing: " + missing);

        UnicodeSet multiple = new UnicodeSet("[👯🤼👫👬👭💏💑👪🤝]");

        UnicodeMap<String> data = new UnicodeMap<>();
        for (String s : all) {
            data.put(s, (EMOJI_DATA_RELEASE.getModifierBases().contains(s) ? "skin" : multiple.contains(s) ? "multiple" : "")
                    + "\t" + (EMOJI_DATA_RELEASE.getExplicitGender().contains(s) ? "xgender" : EMOJI_DATA_RELEASE.getGenderBases().contains(s) ? "gender" : "")
                    + "\t" + (EMOJI_DATA_RELEASE.getExplicitHair().contains(s) ? "xhair" : EMOJI_DATA_RELEASE.getHairBases().contains(s) ? "hair" : "")
                    + "\t" + (roleBase.contains(s) ? "role" : "")
                    );

        }
        show(data);
        System.out.println();

        System.out.println("# Generating for repertoire of E" + Emoji.VERSION_LAST_RELEASED_STRING 
                + ", based on ordering for E" + Emoji.VERSION_BETA_STRING);
        EmojiOrder STD_ORDER_NEW = EmojiOrder.of(Emoji.VERSION_BETA);


        System.out.println("# START AUTOGENERATED EMOJI ORDER");
        StringBuilder rules = new StringBuilder();
        STD_ORDER_NEW.appendCollationRules(rules, EMOJI_DATA_RELEASE.getEmojiForSortRules(), GENDER_NEUTRALS);
        System.out.println(rules);
        System.out.println("# END AUTOGENERATED EMOJI ORDER");
    }


    private static void show(UnicodeMap<String> data) {
        Set<String> temp = sort(EmojiOrder.of(Emoji.VERSION_LAST_RELEASED).codepointCompare, data.keySet());
        for (String s : temp) {
            showEmoji(data.get(s), s);
        }
    }

    private static void show(String title, UnicodeSet unicodeSet) {
        Set<String> temp = sort(EmojiOrder.of(Emoji.VERSION_LAST_RELEASED).codepointCompare, unicodeSet);
        for (String s : temp) {
            showEmoji(title, s);
        }
    }

    private static void showEmoji(String info, String s) {
        String name = "n/a";
        try {
            name = EmojiData.EMOJI_DATA.getName(s);
        } catch (Exception e) {
            return;
        }
        System.out.println("\\x{" + Utility.hex(s,2," ") + "}"
                + "\t" + s
                + "\t" + name
                + "\t" + info
                );
    }
}