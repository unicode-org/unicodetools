package org.unicode.tools.emoji;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.MultiComparator;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class EmojiOrder {
    private static final ImmutableList<String> HAIR_ORDER =
            ImmutableList.of("🦰", "🦱", "🦳", "🦲");
    // private static final UnicodeSet MODIFIER_BASES = EmojiData.EMOJI_DATA.getModifierBases();
    private static final ConcurrentHashMap<VersionInfo, EmojiOrder> VERSION_TO_DATA =
            new ConcurrentHashMap<>();

    private static final boolean DEBUG = false;
    static final UnicodeSet DEBUG_SET = new UnicodeSet("[\u200D \\U0001F9D1 \\U0001F9AF]").freeze();

    public enum MajorGroup {
        Smileys_and_Emotion("Smileys_and_People", "Smileys"),
        People_and_Body("People", "People_and_Body"),
        Component,
        Animals_and_Nature,
        Food_and_Drink,
        Travel_and_Places,
        Activities,
        Objects,
        Symbols,
        Flags,
        Other;
        final Set<String> alternateInput;

        MajorGroup(String... alternateInput) {
            this.alternateInput = ImmutableSet.copyOf(alternateInput);
        }

        static final MajorGroup Smileys_and_People = Smileys_and_Emotion,
                Smileys = Smileys_and_Emotion,
                People = People_and_Body;

        public String toString() {
            return name();
            //            throw new ICUException("Disabling this because it is too easy to use the
            // wrong choice.");
            // return name().replace("_and_", " & ").replace('_', ' ');
        }
        ;

        public String toPlainString() {
            return name().replace("_and_", " & ").replace('_', ' ');
        }
        ;

        public String toSourceString() {
            return name();
        }
        ;

        public String toHTMLString() {
            return toPlainString().replace("&", "&amp;");
        }

        public static MajorGroup fromString(String source) {
            source = source.trim();
            for (MajorGroup trial : values()) {
                if (trial.alternateInput.contains(source)) {
                    return trial;
                }
            }
            return valueOf(source);
        }
        ;
    }

    // static final EmojiData emojiDataLast = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
    public static final StringComparator CODE_POINT_COMPARATOR =
            new UTF16.StringComparator(true, false, 0);
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

    public static final Comparator<String> UCA_COLLATOR =
            (Comparator<String>) (Comparator) Collator.getInstance(ULocale.ROOT);
    public static final Comparator<String> UCA_PLUS_CODEPOINT =
            new MultiComparator<String>(EmojiOrder.UCA_COLLATOR, CODE_POINT_COMPARATOR);

    public static final EmojiOrder STD_ORDER = EmojiOrder.of(Emoji.VERSION_TO_GENERATE);
    public static final EmojiOrder ORDER_RELEASED = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
    public static final EmojiOrder BETA_ORDER =
            Emoji.IS_BETA ? EmojiOrder.of(Emoji.VERSION_BETA) : STD_ORDER;

    public final EmojiData emojiData;

    public final MapComparator<String> mapCollator;
    public final Comparator<String> codepointCompare;

    public final Multimap<String, String> orderingToCharacters;
    public final UnicodeMap<String> charactersToOrdering = new UnicodeMap<>();
    public final UnicodeMap<MajorGroup> majorGroupings = new UnicodeMap<>();
    public final Map<String, Integer> categoryToOrder;
    private final Map<String, MajorGroup> categoryToMajor;
    private final UnicodeSet firstInLine;

    // TODO — remove
    private final String reformatted;
    private ImmutableSet<String> sorted;

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
        mapCollator =
                new MapComparator<String>()
                        .setErrorOnMissing(false)
                        .setSortBeforeOthers(false)
                        .setDoFallback(false);
        Map<String, Integer> _groupOrder = new LinkedHashMap<String, Integer>();
        Map<String, MajorGroup> _categoryToMajor = new LinkedHashMap<>();
        StringBuilder _reformatted = new StringBuilder();
        firstInLine = new UnicodeSet();
        LinkedHashSet<String> _sorted = new LinkedHashSet<String>();
        orderingToCharacters =
                loadOrdering(
                        version,
                        file,
                        mapCollator,
                        _groupOrder,
                        _categoryToMajor,
                        _reformatted,
                        _sorted);
        reformatted = _reformatted.toString();
        sorted = ImmutableSet.copyOf(_sorted);
        firstInLine.freeze();
        mapCollator.freeze();
        majorGroupings.freeze();
        categoryToOrder = Collections.unmodifiableMap(_groupOrder);
        categoryToMajor = Collections.unmodifiableMap(_categoryToMajor);
        codepointCompare =
                new MultiComparator<String>(
                        mapCollator, EmojiOrder.UCA_COLLATOR, CODE_POINT_COMPARATOR);
        //                new MultiComparator<String>(
        //                        mp,
        //                        EmojiOrder.UCA_COLLATOR,
        //                        CODE_POINT_COMPARATOR,
        //                        new UTF16.StringComparator(true,false,0));
        if (DEBUG) {
            String last = "";
            for (String s : Arrays.asList("\u2017", "\u002D", "\uFF0D")) {
                System.out.println(
                        Utility.hex(last)
                                + "/"
                                + last
                                + " vs "
                                + Utility.hex(s)
                                + "/"
                                + s
                                + ": "
                                + codepointCompare.compare(last, s)
                                + " map: "
                                + mapCollator.getNumericOrder(s));
                last = s;
            }
        }
        // check consistency

        Set<String> orderedStrings = new LinkedHashSet<>(mapCollator.getOrder());
        check("orderedStrings, sorted", orderedStrings, sorted, true);

        Set<String> sourceData =
                emojiData
                        .getAllEmojiWithoutDefectives()
                        .addAllTo(new LinkedHashSet<>(mapCollator.getOrder()));
        check("orderedStrings vs sourceData", orderedStrings, sourceData, true);

        Set<String> orderingToCharactersValues = new LinkedHashSet<>(orderingToCharacters.values());
        check(
                "edStrings vs orderingToCharactersValues",
                orderedStrings,
                orderingToCharactersValues,
                true);

        Set<String> charactersToOrderingKeys =
                charactersToOrdering.keySet().addAllTo(new LinkedHashSet<>());
        check(
                "orderedStrings vs charactersToOrderingKeys",
                orderedStrings,
                charactersToOrderingKeys,
                false);

        check(
                "orderedStrings vs majorGroupings.keySet",
                orderedStrings,
                majorGroupings.keySet().addAllTo(new LinkedHashSet<>()),
                false);

        check(
                "categoryToOrder.keySet vs categoryToMajor.keySet",
                categoryToOrder.keySet(),
                categoryToMajor.keySet(),
                false);
    }

    public static <T, U extends Collection<T>> void check(
            String title, U a, U b, boolean checkOrder) {
        if (!Objects.equal(a, b)) {
            LinkedHashSet<T> aMinusB = new LinkedHashSet<>(a);
            aMinusB.removeAll(b);
            LinkedHashSet<T> bMinusA = new LinkedHashSet<>(b);
            bMinusA.removeAll(a);
            if (!aMinusB.isEmpty() || !bMinusA.isEmpty()) {
                throw new ICUException(
                        (title.isEmpty() ? "" : title + ": ")
                                + "mismatch:\n\t(a-b)="
                                + aMinusB
                                + ";\n\t(b-a)="
                                + bMinusA);
            }
        }
        if (!checkOrder) {
            return;
        }
        Iterator<T> ita = a.iterator();
        Iterator<T> itb = b.iterator();
        int counter = 0;
        while (ita.hasNext()) {
            T aItem = ita.next();
            T bItem = itb.next();
            if (!aItem.equals(bItem)) {
                throw new ICUException(
                        counter + ") ordering mismatch: a=" + aItem + "; b-a=" + bItem);
            }
            ++counter;
        }
    }

    private Multimap<String, String> loadOrdering(
            VersionInfo version,
            String sourceFile,
            MapComparator<String> mapComparator,
            Map<String, Integer> _groupOrder,
            Map<String, MajorGroup> _categoryToMajor,
            StringBuilder reformatted,
            LinkedHashSet<String> sorted) {
        // System.out.println(sourceFile);
        Multimap<String, String> _orderingToCharacters = LinkedHashMultimap.create();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        MajorGroup majorGroup = null;
        EmojiIterator ei = new EmojiIterator(emojiData, false);
        final String directory =
                Settings.UnicodeTools.getDataPathString("emoji", version.getVersionString(2, 2));
        int lineCounter = 0;

        for (String line : FileUtilities.in(EmojiOrder.class, sourceFile)) {
            ++lineCounter;
            if (line.isEmpty()
                    || line.startsWith("#") && !line.startsWith("#⃣") && !line.startsWith("#️⃣")) {
                continue;
            }
            if (DEBUG) System.out.println(line);

            line = Emoji.UNESCAPE.transform(line);
            line =
                    line.replace(Emoji.TEXT_VARIANT_STRING, "")
                            .replace(Emoji.EMOJI_VARIANT_STRING, "");

            if (line.contains("keycap")) {
                int debug = 0;
            }

            if (line.startsWith("@@")) {
                majorGroup = MajorGroup.fromString(line.substring(2).trim());
                reformatted.append(line).append('\n');
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
                reformatted.append("@" + item).append('\n');
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
            //                        throw new IllegalArgumentException("Conflicting major
            // categories");
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
                if (string.contains(UTF16.valueOf(0x1F90C))) {
                    int debug = 0;
                }
                { // just for formatting
                    String cleanString = EmojiData.EMOJI_DATA.addEmojiVariants(string);
                    if (isFirst) {
                        isFirst = false;
                        firstInLine.add(cleanString);
                    } else {
                        reformatted.append(' ');
                    }
                    reformatted.append(cleanString);
                }
                // System.out.println("Adding: " + Utility.hex(string) + "\t" + string);
                add(_orderingToCharacters, sorted, majorGroup, lastLabel, string);
                if (!sorted.contains(string)) {
                    throw new ICUException();
                }
                String handshakeVersion = EmojiData.COUPLES_TO_HANDSHAKE_VERSION.get(string);
                if (handshakeVersion == null) {
                    // Now add the modifier sequences
                    if (string.contains("👨‍❤‍👨")) {
                        int debug = 0;
                    }
                    addAllModifiers(
                            _orderingToCharacters,
                            sorted,
                            lastLabel,
                            majorGroup,
                            string); // , withEmojiVariant, withoutFinal, noVariant);
                } else {
                    // Now add the modifier sequences
                    addAllModifiers(
                            _orderingToCharacters,
                            sorted,
                            lastLabel,
                            majorGroup,
                            handshakeVersion); // , withEmojiVariant, withoutFinal, noVariant);
                }

                //                ImmutableList<String> list = hack.get(string);
                //                if (list != null) {
                //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                //                    for (String string2 : list) {
                //                        //System.err.println("Adding " + show(string2));
                //                        add(result, sorted, majorGroup, lastLabel, string2);
                //                        addVariants(result, sorted, majorGroup, lastLabel,
                // string2);
                //                    }
                //                }

                // We have a hack for blond person, and add them explicitly.
                if (emojiData.getGenderBase().contains(string) && !string.equals("👱")) {
                    String stringWithMale = string + "\u200d\u2642";
                    add(_orderingToCharacters, sorted, majorGroup, lastLabel, stringWithMale);
                    // Now add the modifier sequences
                    addAllModifiers(
                            _orderingToCharacters,
                            sorted,
                            lastLabel,
                            majorGroup,
                            stringWithMale); // , withEmojiVariant, withoutFinal, noVariant); //  +
                    // Emoji.EMOJI_VARIANT_STRING
                    if (!sorted.contains(stringWithMale)) {
                        throw new ICUException();
                    }
                    String stringWithFemale = string + "\u200d\u2640";
                    add(_orderingToCharacters, sorted, majorGroup, lastLabel, stringWithFemale);
                    // Now add the modifier sequences
                    addAllModifiers(
                            _orderingToCharacters,
                            sorted,
                            lastLabel,
                            majorGroup,
                            stringWithFemale); // , withEmojiVariant, withoutFinal, noVariant); //
                    // + Emoji.EMOJI_VARIANT_STRING
                    if (!sorted.contains(stringWithFemale)) {
                        throw new ICUException();
                    }
                }
                //                // add/remove all variant strings
                //                if (string.contains(Emoji.JOINER_STRING) ||
                // emojiData.getKeycapBases().contains(string.charAt(0))) {
                //                    addVariants(result, sorted, majorGroup, lastLabel, string);
                //                }
            }
            if (!isFirst) { // skip empty lines
                reformatted.append('\n');
            }
        }

        if (DEBUG)
            for (String s : sorted) {
                if (DEBUG_SET.containsAll(s)) {
                    System.out.println("Debug: " + Utility.hex(s) + "\t" + s);
                }
            }
        mapComparator.add(sorted);
        // mapComparator.setErrorOnMissing(true);
        mapComparator.freeze();

        check("sorted, mp.getOrder", sorted, mapCollator.getOrder(), false);

        return ImmutableMultimap.copyOf(_orderingToCharacters);
    }

    private void addAllModifiers(
            Multimap<String, String> result,
            Set<String> sorted,
            Output<Set<String>> lastLabel,
            MajorGroup majorGroup,
            String... strings) {
        HashSet<String> seen = new HashSet<>();
        for (String string : strings) {
            if (string == null || seen.contains(string)) {
                continue;
            }
            seen.add(string);
            Set<String> results = EmojiData.EMOJI_DATA_BETA.addModifiers(string, true);
            if (results.isEmpty()) {
                continue;
            }
            if (DEBUG) {
                System.out.println(string + "=>" + results);
            }
            boolean isHoldingHands =
                    string.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)
                            && !string.equals(EmojiData.NEUTRAL_HOLDING);
            boolean isHandshake = EmojiData.EMOJI_DATA_BETA.isHandshake(string);
            UnicodeSet temp = isHoldingHands || isHandshake ? new UnicodeSet() : null;

            for (String item : results) {
                if (isHoldingHands || isHandshake) {
                    // substitute the single character IFF the modifiers are the same
                    temp.clear().addAll(item).retainAll(EmojiData.MODIFIERS);
                    String oldItem = item;
                    if (temp.size() == 1) {
                        String base = EmojiData.EMOJI_DATA_BETA.getBaseRemovingModsGender(item);
                        item = base + temp.iterator().next(); // add the modifier in temp
                        if (DEBUG) System.out.println(oldItem + " ==> " + item);
                    }
                }
                if (DEBUG
                        && (EmojiData.COUPLES.containsSome(item)
                                || item.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ))) {
                    System.out.println("**Adding: " + item);
                }
                add(result, sorted, majorGroup, lastLabel, item);
            }
        }
    }

    private void add(
            Multimap<String, String> _orderingToCharacters,
            Set<String> sorted,
            MajorGroup majorGroup,
            Output<Set<String>> lastLabel,
            String string) {
        if (string.contains(Emoji.EMOJI_VARIANT_STRING)) {
            throw new IllegalArgumentException(
                    "String shouldn't contain variants at this point: " + string);
        }
        if (sorted.contains(string)) {
            throw new IllegalArgumentException("Duplicate entry for: " + string);
            // return; // already done.
        }
        if (DEBUG)
            if (DEBUG_SET.containsAll(string)) {
                System.out.println("Debug: " + Utility.hex(string) + "\t" + string);
                int debug = 0;
            }
        //        if (string.contains("⚕")) {
        //            System.out.println("\t" + string);
        //        }
        // System.out.println(string + "\t" + Utility.hex(string));
        if (string.equals("-")) {
            int debug = 0;
        }
        final String first = lastLabel.value.iterator().next();
        sorted.add(string);
        for (String item : lastLabel.value) {
            _orderingToCharacters.put(item, string);
        }
        charactersToOrdering.put(string, first);
        majorGroupings.put(string, majorGroup);
        for (String variant : emojiData.new VariantFactory().set(string).getCombinations()) {
            sorted.add(variant);
            for (String item : lastLabel.value) {
                _orderingToCharacters.put(item, variant);
            }
            charactersToOrdering.put(variant, first);
            majorGroupings.put(variant, majorGroup);
        }
        //        String full = emojiData.addEmojiVariants(string);
        //        if (full.endsWith(Emoji.EMOJI_VARIANT_STRING)) {
        //            String noVariantAtEnd = full.substring(0, full.length() -
        // Emoji.EMOJI_VARIANT_STRING.length());
        //            sorted.add(noVariantAtEnd);
        //        }
        //        sorted.add(full);
        //        for (String item : lastLabel.value) {
        //            _orderingToCharacters.put(item, string);
        //        }
        //        final String first = lastLabel.value.iterator().next();
        //        charactersToOrdering.put(string, first);
        //        if (!full.equals(string)) {
        //            charactersToOrdering.put(full, first);
        //        }
    }

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

            for (String m : HAIR_ORDER) { // not cp order, so not Emoji.HAIR_STYLES
                outText.append(m);
            }
            for (String m : EmojiData.MODIFIERS) {
                outText.append(m);
            }
            UnicodeSet hairSkin =
                    new UnicodeSet(EmojiData.MODIFIERS)
                            .addAll(Emoji.HAIR_STYLES_WITH_JOINERS)
                            .freeze();
            UnicodeSetSpanner HairSkinSpanner = new UnicodeSetSpanner(hairSkin);

            String lastGroup = null;
            // HACK
            outText.append("\n& [before 1]\uFDD1€");
            isFirst = false;
            Set<String> haveSeen = new HashSet<>();
            Set<String> forLater = new LinkedHashSet<>();
            Set<String> checkHolding = new HashSet<>();

            for (String s : temp) {
                if (s.startsWith(Emoji.ADULT)) {
                    int debug = 0;
                }
                String group = getCategory(s);
                if (!Objects.equal(group, lastGroup)) {
                    needRelation = true;
                    lastGroup = group;
                }

                // we can skip anything that ends with skin or zwj-hair, since we make those
                // ignorable
                int trimmed = hairSkin.spanBack(s, SpanCondition.SIMPLE);
                if (trimmed < s.length()) {
                    continue;
                }
                if (haveSeen.contains(s)) {
                    continue;
                }
                for (String explicitHair : Emoji.HAIR_EXPLICIT) {
                    // HACK the blond & bearded person to make secondary and at end
                    if (s.contains(explicitHair)) {
                        forLater.add(stripFinalVariant(s));
                        continue;
                    }
                }
                if (s.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)
                        || EmojiData.HOLDING_HANDS_COMPOSITES.contains(s)) {
                    if (s.equals(EmojiData.NEUTRAL_HOLDING)) {
                        addMultipersonSkinTones(outText, haveSeen, "🧑‍🤝‍🧑", "👭", "👫", "👬");
                        needRelation =
                                true; // after this, we will need the next item to have the relation
                    } else {
                        checkHolding.add(s);
                    }
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
                        s = s.substring(0, s.length() - Character.charCount(last)); // remove last
                        if (temp.contains(s)) {
                            continue; // skip if present
                        }
                        containsModifier = EmojiData.MODIFIERS.containsSome(s);
                    }
                }
                // at this point, the only Modifiers can be medial.
                if (isFirst) {
                    if (multiCodePoint) {
                        throw new IllegalArgumentException(
                                "Cannot have first item with > 1 codepoint: " + s);
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
                } else {
                    if (needRelation) {
                        outText.append("\n<*");
                        needRelation = false;
                    }
                    outText.append(s);
                    haveSeen.add(s);
                    //                    needRelation = true;
                    //                                        // break arbitrarily (but predictably)
                    //                                        int bottomBits = s.codePointAt(0) &
                    // 0x7;
                    //                                        needRelation = bottomBits == 0;
                }
            }
            // Hack to sort blond, bearded like hair color
            if (DEBUG) System.out.println("\nFor later: " + forLater);
            outText.append("\n& 🧑 << 👱");
            outText.append("\n& 👨 << 🧔");
            for (String item : forLater) {
                if (item.contains(Emoji.MALE)) {
                    outText.append(" <<").append(item);
                }
            }
            outText.append("\n& 👩");
            for (String item : forLater) {
                if (item.contains(Emoji.FEMALE)) {
                    outText.append(" << ").append(item);
                }
            }
            // OLD Hack to sort handshake items
            // addHandshakes(outText);
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error", e);
        }
        return outText;
    }

    /**
     * Add all the characters, based on the bases and their order: NN, WW, WM, MM (🧑‍🤝‍🧑, 👭, 👫,
     * 👬) Start with each base, and add the ZWJ (equivalents) skin on the first item. (Need not add
     * for last, since the skin components have the right order.) Exceptions! When the skin tone is
     * identical, it is applied to the combined base for WW, WM, MM. The easiest way to do that is
     * to store these for later and do resets. TODO Generalize for other skin tones, generalizing
     * EmojiData.COUPLES_TO_HANDSHAKE_VERSION
     *
     * @param outText output
     * @param haveSeen for tracking the items added
     * @param bases the base characters
     * @throws IOException
     */
    private <T extends Appendable> void addMultipersonSkinTones(
            T outText, Set<String> haveSeen, String... bases) throws IOException {
        Map<String, String> expansion = new LinkedHashMap<>();
        for (String base : bases) {
            outText.append("\n< ").append(quoteSyntax(base));
            haveSeen.remove(base);
            boolean compositeBase = base.length() == 2;
            String handshake =
                    compositeBase ? EmojiData.COUPLES_TO_HANDSHAKE_VERSION.get(base) : base;
            int firstCp = handshake.codePointAt(0);
            String lead = UTF16.valueOf(firstCp);
            String trail = handshake.substring(Character.charCount(firstCp));
            for (String skin : EmojiData.MODIFIERS) {
                String combo = lead + skin + trail;
                outText.append(" << ").append(quoteSyntax(combo));
                haveSeen.remove(combo);
                if (compositeBase) {
                    expansion.put(combo + skin, base + skin);
                }
            }
        }
        for (Entry<String, String> entry : expansion.entrySet()) {
            outText.append("\n& ")
                    .append(quoteSyntax(entry.getKey()))
                    .append('=')
                    .append(quoteSyntax(entry.getValue()));
        }
    }

    // HACK the handshakes to add at end
    private <T extends Appendable> void addHandshakes(T outText) throws IOException {
        String handshake = null;
        Set<String> sorted = sort(codepointCompare, emojiData.getAllEmojiWithoutDefectives());
        for (String emoji : sorted) {
            if (EmojiData.COUPLES.containsSome(emoji)) {
                handshake = emoji; // save in case we have one after
            } else if (emoji.contains(EmojiData.ZWJ_HANDSHAKE_ZWJ)) {
                if (handshake != null) {
                    outText.append("\n& " + handshake);
                    handshake = null;
                }
                outText.append(" << " + emoji);
            }
        }
    }

    private String stripFinalVariant(String s) {
        if (s.endsWith(Emoji.EMOJI_VARIANT_STRING)) {
            s = s.substring(0, s.length() - Emoji.EMOJI_VARIANT_STRING.length());
        }
        return s;
    }

    public String getCategory(String emoji) {
        return emoji.isEmpty() ? null : charactersToOrdering.get(emoji);
    }

    public UnicodeSet getCharsWithCategory(String category) {
        return charactersToOrdering.getSet(category);
    }

    private String withoutTrailingVariant(String s) {
        return s.endsWith(Emoji.EMOJI_VARIANT_STRING) ? s.substring(0, s.length() - 1) : s;
    }

    static final UnicodeSet NEEDS_QUOTE =
            new UnicodeSet("[[:Pattern_White_Space:][\\&\\[\\]#@!<;,=*]]").freeze();

    private String quoteSyntax(String source) {
        return NEEDS_QUOTE.containsNone(source) ? source : "'" + source.replace("'", "''") + "'";
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
        throw new ICUException("Value not found: " + CollectionUtilities.join(values, ", "));
    }

    public int getGroupOrder(String cat1) {
        return categoryToOrder.get(cat1);
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

        //        EmojiData EMOJI_DATA_RELEASE = EmojiData.of(Emoji.VERSION_BETA);
        //        UnicodeSet roleBase = new UnicodeSet();
        //        UnicodeSet roleObject = new UnicodeSet();
        //        UnicodeSet temp = new UnicodeSet();
        //        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
        //
        //        for (String s : EMOJI_DATA_RELEASE.getAllEmojiWithDefectives()) {
        //            switch (CountEmoji.Category.getBucket(s)) {
        //            case zwj_seq_role:
        //                int first = s.codePointAt(0);
        //                roleBase.add(first);
        //                temp.clear().addAll(s).remove(first)
        //                .remove(Emoji.JOINER)
        //                .remove(Emoji.EMOJI_VARIANT)
        //                .removeAll(EmojiData.MODIFIERS)
        //                .removeAll(EMOJI_DATA_RELEASE.getEmojiComponents());
        //                roleObject.addAll(temp);
        //                break;
        //            }
        //        }
        //        if (DEBUG) System.out.println("roleObject: " + roleObject.toPattern(false));
        //
        //        UnicodeSet all = new UnicodeSet()
        //                .addAll(EMOJI_DATA_RELEASE.getGenderBases())
        //                .addAll(EMOJI_DATA_RELEASE.getHairBases())
        //                .addAll(EMOJI_DATA_RELEASE.getModifierBases())
        //                .addAll(EMOJI_DATA_RELEASE.getExplicitGender())
        //                .addAll(EMOJI_DATA_RELEASE.getExplicitHair())
        //                .addAll(roleBase)
        //                ;
        //        UnicodeSet missing = new UnicodeSet();
        //        Set<String> categories = new TreeSet<>();
        //        for (String s : all) {
        //            categories.add(order.getCategory(s));
        //        }
        //        for (String category : categories) {
        //            missing.addAll(new
        // UnicodeSet(order.getCharsWithCategory(category)).removeAll(all));
        //        }
        //        Set<String> skip = new HashSet<>(missing.strings());
        //        missing.removeAll(skip).removeAll(EmojiData.EMOJI_DATA.getEmojiComponents());
        //        all.addAll(missing).freeze();
        //        if (DEBUG) System.out.println("Missing: " + missing);
        //
        //        UnicodeSet multiple = new UnicodeSet("[👯🤼👫👬👭💏💑👪🤝]");
        //
        //        UnicodeMap<String> data = new UnicodeMap<>();
        //        for (String s : all) {
        //            data.put(s, (EMOJI_DATA_RELEASE.getModifierBases().contains(s) ? "skin" :
        // multiple.contains(s) ? "multiple" : "")
        //                    + "\t" + (EMOJI_DATA_RELEASE.getExplicitGender().contains(s) ?
        // "xgender" : EMOJI_DATA_RELEASE.getGenderBases().contains(s) ? "gender" : "")
        //                    + "\t" + (EMOJI_DATA_RELEASE.getExplicitHair().contains(s) ? "xhair" :
        // EMOJI_DATA_RELEASE.getHairBases().contains(s) ? "hair" : "")
        //                    + "\t" + (roleBase.contains(s) ? "role" : "")
        //                    );
        //
        //        }
        //        show(data);
        //        System.out.println();

        //        System.out.println("# Generating for repertoire of E" +
        // Emoji.VERSION_LAST_RELEASED_STRING
        //                + ", based on ordering for E" + Emoji.VERSION_BETA_STRING);

        check(
                "BETA_ORDER.sorted, BETA_ORDER.mp.getOrder",
                BETA_ORDER.sorted,
                BETA_ORDER.mapCollator.getOrder(),
                false);

        Set<String> temp =
                sort(
                        BETA_ORDER.codepointCompare,
                        BETA_ORDER.emojiData.getEmojiForSortRules(),
                        GENDER_NEUTRALS);

        // check(BETA_ORDER.sorted, temp, false);

        int counter = 0;
        for (String s : temp) {
            System.out.println(
                    ++counter
                            + ")\t"
                            + BETA_ORDER.mapCollator.getNumericOrder(s)
                            + "\t"
                            + s
                            + "\t"
                            + getName(s));
            if (counter > 50) break;
        }

        counter = 0;
        for (String s : BETA_ORDER.sorted) {
            System.out.println(
                    ++counter
                            + ")\t"
                            + BETA_ORDER.mapCollator.getNumericOrder(s)
                            + "\t"
                            + s
                            + "\t"
                            + getName(s));
            if (counter > 50) break;
        }

        System.out.println(
                "# START AUTOGENERATED EMOJI ORDER — " + BETA_ORDER.emojiData.getVersionString());
        StringBuilder rules = new StringBuilder();
        BETA_ORDER.appendCollationRules(
                rules, BETA_ORDER.emojiData.getEmojiForSortRules(), GENDER_NEUTRALS);
        System.out.println(rules);
        System.out.println("# END AUTOGENERATED EMOJI ORDER");

        PrintWriter out = new PrintWriter(System.out);
        BETA_ORDER.showCategories(out);
        out.flush();
        out.close();
    }

    private static String getName(String s) {
        try {
            return EmojiData.EMOJI_DATA_BETA.getName(s);
        } catch (Exception e) {
            return "<name n/a>";
        }
    }

    private static void show(UnicodeMap<String> data) {
        Set<String> temp =
                sort(EmojiOrder.of(Emoji.VERSION_LAST_RELEASED).codepointCompare, data.keySet());
        for (String s : temp) {
            showEmoji(data.get(s), s);
        }
    }

    private static void show(String title, UnicodeSet unicodeSet) {
        Set<String> temp =
                sort(EmojiOrder.of(Emoji.VERSION_LAST_RELEASED).codepointCompare, unicodeSet);
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
        System.out.println(
                "\\x{" + Utility.hex(s, 2, " ") + "}" + "\t" + s + "\t" + name + "\t" + info);
    }

    public void showCategories(Appendable out) {
        try {
            Map<MajorGroup, Multimap<String, String>> majorToMinorToEmoji =
                    new EnumMap<>(MajorGroup.class);
            Set<String> sorted =
                    sort(codepointCompare, emojiData.getAllEmojiWithoutDefectivesOrModifiers());
            for (String emoji : sorted) {
                String cat = getCategory(emoji);
                MajorGroup major = getMajorGroupFromCategory(cat);
                Multimap<String, String> sub = majorToMinorToEmoji.get(major);
                if (sub == null) {
                    majorToMinorToEmoji.put(major, sub = LinkedHashMultimap.create());
                }
                sub.put(cat, emoji);
            }
            int line = 0;
            for (Entry<MajorGroup, Multimap<String, String>> entry :
                    majorToMinorToEmoji.entrySet()) {
                MajorGroup major = entry.getKey();
                for (Entry<String, Collection<String>> entry2 :
                        entry.getValue().asMap().entrySet()) {
                    Collection<String> items = entry2.getValue();
                    out.append(
                            ++line
                                    + "\t"
                                    + major.toPlainString()
                                    + "\t"
                                    + entry2.getKey()
                                    + "\t"
                                    + items.size()
                                    + "\t"
                                    + CollectionUtilities.join(items, " ")
                                    + "\n");
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public String getReformatted() {
        return reformatted;
    }

    public boolean isFirstInLine(String s) {
        return firstInLine.contains(s);
    }
}
