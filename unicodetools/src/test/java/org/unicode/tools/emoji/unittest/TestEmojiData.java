package org.unicode.tools.emoji.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiAnnotations;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiData.VariantStatus;
import org.unicode.unittest.TestFmwkMinusMinus;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.tools.emoji.GenerateEmojiData;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class TestEmojiData extends TestFmwkMinusMinus {
    private static final boolean SHOW = false;
    final EmojiData released = EmojiData.of(Emoji.VERSION_TO_TEST_PREVIOUS);
    final EmojiDataSource emojiDataToTest;
    final EmojiOrder emojiOrderToTest;

    /**
     * We structure the test this way so that we can run it with two different sets of data.
     * @param emojiOrder
     */
    public TestEmojiData(EmojiDataSource beta, EmojiOrder emojiOrder) {
        this.emojiDataToTest = beta;
        emojiOrderToTest = emojiOrder;
    }

    public TestEmojiData() {
        this(TestAll.DATA_TO_TEST, TestAll.ORDER_TO_TEST);
    }

    public void TestA() {
        System.out.print(" Version: " + emojiDataToTest.getVersionString()
        + "; class: " + emojiDataToTest.getClass()
                );
    }

    public static final Splitter semi = Splitter.onPattern("[;#]").trimResults();

    public void TestPublicEmojiTest() {
        if (emojiDataToTest instanceof EmojiDataSourceCombined) {
            return; // only test the beta stuff without combining
        }
        UnicodeMap<VariantStatus> tests = new UnicodeMap<>();
        for (String line : FileUtilities.in(GenerateEmojiData.OUTPUT_DIR, "emoji-test.txt")) {
            int hashPos = line.indexOf('#');
            if (hashPos >= 0) {
                line = line.substring(0, hashPos);
            }
            if (line.isEmpty()) continue;
            List<String> list = semi.splitToList(line);
            String source = Utility.fromHex(list.get(0));
            //# subgroup: face-concerned
            // 2639 FE0F                                  ; fully-qualified     # ☹️ frowning face
            VariantStatus variantStatus = VariantStatus.forString(list.get(1));
            tests.put(source, variantStatus);
        }
        tests.freeze();
        assertContains(VariantStatus.full.toString(),
                "emoji-test",
                tests.getSet(VariantStatus.full),
                "EmojiData",
                new UnicodeSet(emojiDataToTest.getBasicSequences())
                .addAll(emojiDataToTest.getKeycapSequences())
                .addAll(emojiDataToTest.getFlagSequences())
                .addAll(emojiDataToTest.getTagSequences())
                .addAll(emojiDataToTest.getModifierSequences())
                .addAll(emojiDataToTest.getZwjSequencesNormal())
                .removeAll(new UnicodeSet("[🇦-🇿🏻-🏿🦰-🦳{#️}{*️}{0️}{1️}{2️}{3️}{4️}{5️}{6️}{7️}{8️}{9️}]"))
                );
        assertEqualsUS(VariantStatus.component.toString(),
                "emoji-test",
                tests.getSet(VariantStatus.component),
                "EmojiData",
                new UnicodeSet(emojiDataToTest.getEmojiComponents())
                .removeAll(new UnicodeSet("[#*0-9‍⃣️🇦-🇿󠀠-󠁿]"))
                );
        //        assertEqualsUS(VariantStatus.other + " = emoji",
        //                "?",
        //                new UnicodeSet(tests.getSet(VariantStatus.other)).add(tests.getSet(VariantStatus.initial)), "?", new UnicodeSet(beta.getAllEmojiWithDefectives()).removeAll(beta.getAllEmojiWithoutDefectives()));
    }

    private void assertEqualsUS(String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        Asserts.assertEqualsUS(this, message, s1Name, s1, s2Name, s2);
    }

    private void assertContains(String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        Asserts.assertContains(this, message, s1Name, s1, s2Name, s2);
    }

    private void assertContains(String message, String s1Name, UnicodeSet s1, String s2Name, String s2char) {
        Asserts.assertContains(this, message, s1Name, s1, s2Name, s2char);
    }

    public void TestHandshake() {
        emojiDataToTest.getName("👩"); // warm up
        assertEquals("👩‍🤝‍👩", "people holding hands", emojiDataToTest.getName("🧑‍🤝‍🧑"));
        assertEquals("👩‍🤝‍👩", "people holding hands: dark skin tone, light skin tone", emojiDataToTest.getName("🧑🏿‍🤝‍🧑🏻"));
        assertEquals("👩‍🤝‍👩", "women holding hands", emojiDataToTest.getName("👩‍🤝‍👩"));
        assertEquals("👩🏿‍🤝‍👩🏻", "women holding hands: dark skin tone, light skin tone", emojiDataToTest.getName("👩🏿‍🤝‍👩🏻"));
        assertEquals("👩🏼‍🤝‍👨🏿", "woman and man holding hands: medium-light skin tone, dark skin tone", emojiDataToTest.getName("👩🏼‍🤝‍👨🏿"));
        assertEquals("👨🏿‍🤝‍👨🏿", "men holding hands: dark skin tone", emojiDataToTest.getName("👨🏿‍🤝‍👨🏿"));
    }

    public void TestCompoundNames() {
        emojiDataToTest.getName("👩"); // warm up
        assertEquals("🚶🏻‍♂️", "man walking: light skin tone", emojiDataToTest.getName("🚶🏻‍♂️"));
        assertEquals("🧍", "person standing", emojiDataToTest.getName("🧍"));
        assertEquals("🧍🏻", "person standing: light skin tone", emojiDataToTest.getName("🧍🏻"));
        assertEquals("🧍🏻‍♂️", "man standing: light skin tone", emojiDataToTest.getName("🧍🏻‍♂️"));
        assertEquals("🧍\u200D♂️", "man standing", emojiDataToTest.getName("🧍\u200D♂️"));
    }

    public void TestDefectives() {
        UnicodeSet excluded = new UnicodeSet("[#*0-9🇦-🇿]");

        for (EmojiDataSource ed : Arrays.asList(released, emojiDataToTest)) {
            if (ed.getAllEmojiWithDefectives().containsSome(Emoji.DEFECTIVE_COMPONENTS)) {
                errln("getChars contains defectives "
                        + new UnicodeSet().addAll(ed.getAllEmojiWithoutDefectives()).retainAll(Emoji.DEFECTIVE_COMPONENTS));
            }
        }
        if (emojiDataToTest.getExtendedPictographic().containsSome(excluded)) {
            UnicodeSet containedDefectives = new UnicodeSet().addAll(emojiDataToTest.getExtendedPictographic()).retainAll(excluded);
            if (!logKnownIssue("ExtendedPictographic().contains defectives: " + containedDefectives, "dropped for now")) {
                errln("getExtendedPictographic contains defectives " + containedDefectives);
            }
        }
    }

    public void TestFlags() {
        UnicodeSet shouldBeFlagEmoji = new UnicodeSet().add(Emoji.getHexFromFlagCode("EU")).add(Emoji.getHexFromFlagCode("UN"));
        Validity validity = Validity.getInstance();
        Map<Status, Set<String>> regionData = validity.getStatusToCodes(LstrType.region);
        for (Entry<Status, Set<String>> e : regionData.entrySet()) {
            switch(e.getKey()) {
            case regular:
                for (String region : e.getValue()) {
                    String flagEmoji = Emoji.getHexFromFlagCode(region);
                    shouldBeFlagEmoji.add(flagEmoji);
                }
            }
        }
        UnicodeSet shouldNOTBeFlagEmoji = new UnicodeSet();
        for (String s : Emoji.REGIONAL_INDICATORS) {
            for (String t : Emoji.REGIONAL_INDICATORS) {
                final String regionalPair = s+t;
                if (!shouldBeFlagEmoji.contains(regionalPair)) {
                    shouldNOTBeFlagEmoji.add(regionalPair);
                }
            }
        }
        logln("Should be flags: " + shouldBeFlagEmoji.toPattern(false));
        assertEquals("Contains all good regions", UnicodeSet.EMPTY, new UnicodeSet(shouldBeFlagEmoji).removeAll(emojiDataToTest.getAllEmojiWithoutDefectives()));
        logln("Should not be flags: " + shouldNOTBeFlagEmoji.toPattern(false));
        assertEquals("Contains no bad regions", UnicodeSet.EMPTY, new UnicodeSet(shouldNOTBeFlagEmoji).retainAll(emojiDataToTest.getAllEmojiWithoutDefectives()));
    }

    /**
     * Not working yet, so blocking for now.
     */
    public void T_estZwjCategories () {
        UnicodeMap<String> chars = new UnicodeMap<>();
        for (String s : emojiDataToTest.getZwjSequencesNormal()) {
            CountEmoji.Category zwjType = CountEmoji.Category.getType(s);
            String grouping = emojiOrderToTest.charactersToOrdering.get(s);
            chars.put(s, zwjType + "\t" + grouping);
        }
        for (String value: chars.values()) {
            final UnicodeSet set = chars.getSet(value);
            System.out.println(value + "\t" + set.size() + "\t" + set.toPattern(false));
        }
        Set<String> testSet = new TreeSet<>(emojiOrderToTest.codepointCompare);
        emojiDataToTest.getAllEmojiWithoutDefectives().addAllTo(testSet);

        CountEmoji.Category oldZwjType = null;
        String last = "";
        for (String s : testSet) {
            CountEmoji.Category zwjType = CountEmoji.Category.getType(s);
            if (zwjType == null) {
                continue;
            }
            if (oldZwjType != null && zwjType.compareTo(oldZwjType) < 0) {
                errln(zwjType + " < " + oldZwjType
                        + ", but they should be ascending"
                        + "\n\t" + oldZwjType + "\t" + last
                        + "\n\t" + zwjType + "\t" + s);
            }
            last = s;
            oldZwjType = zwjType;
        }
    }

    public void TestOrderRulesSimple() {
        RuleBasedCollator ruleBasedCollator;
        try {
            ruleBasedCollator = new RuleBasedCollator("&a <*🍱🍘🍙🍚🍛🍜🍝🍠🍢🍣🍤🍥🍡");
        } catch (Exception e1) {
            throw new ICUException(e1);
        }
    }

    /**
     * Verify that the internal order puts variants next to one another, and handles all code point sequences.
     */
    public void TestOrderVariants() {
        Set<String> sorted = emojiOrderToTest.sort(emojiOrderToTest.codepointCompare, emojiDataToTest.getAllEmojiWithDefectives());
        assertEquals("Sorted handles all different code point sequences: ",
                emojiDataToTest.getAllEmojiWithDefectives().size(), sorted.size());
        // we check that there is no case where variant 1 < non-variant < variant2
        Map<String,Integer> toIntOrder = new HashMap<>();
        int intOrder = 0;
        // add ordering numbers, but use the same for variants
        for (String item : sorted) {
            String itemWO = item.replace(Emoji.EMOJI_VARIANT_STRING,"");
            Integer old = toIntOrder.get(itemWO);
            toIntOrder.put(item, old == null ? intOrder++ : old);
        }
        String lastItem = "";
        int lastOrder = -1;
        for (String item : sorted) {
            int order = toIntOrder.get(item);
            if (order < lastOrder) {
                errln("fail: " + showItem(lastItem, null) + " > " + showItem(item, null));
            }
            lastItem = item;
            lastOrder = order;
        }
    }

    public void TestOrderRulesWithSkin() {
        checkOrder(null);
    }

    public void TestOrderRulesWithoutSkin() {
        checkOrder(EmojiData.MODIFIERS);
    }

    private void checkOrder(UnicodeSet filterOutIfContains) {
        int SKIPTO = 400;
        StringBuilder outText = new StringBuilder();
        emojiOrderToTest.appendCollationRules(outText, emojiDataToTest.getAllEmojiWithDefectives(), EmojiOrder.GENDER_NEUTRALS);
        String rules = outText.toString();
        UnicodeSet modifierBases = emojiDataToTest.getModifierBases();
        UnicodeSet modifiers = new UnicodeSet(EmojiData.getModifiers()).addAll(Emoji.HAIR_BASE).freeze();
        try {
            RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(rules);
            Set<String> testSet = new TreeSet<>(emojiOrderToTest.codepointCompare);
            emojiDataToTest.getAllEmojiWithDefectives().addAllTo(testSet);
            String lastItem = "";
            String highestWithModifierBase = null;
            String lowestWithModifierBase = null;
            for (String item : testSet) {
                if (filterOutIfContains != null && filterOutIfContains.containsSome(item)) {
                    continue;
                }
                if (ruleBasedCollator.compare(lastItem, item) > 0
                        && !modifiers.contains(item)) {
                    errln("RBased Out of order: "
                            //                            + secondToLastItem
                            //                            + " (" + Utility.hex(secondToLastItem) + ": " + beta.getName(secondToLastItem) + ") " + ">"
                            + lastItem
                            + " "
                            + showItem(lastItem, ruleBasedCollator)
                            + " " + "> "
                            + item
                            + " " + showItem(item, ruleBasedCollator) + ") "
                            );
                } else {
                    logln(lastItem + "≤" + item);
                }
                lastItem = item;
                if (modifierBases.containsSome(item)) {
                    if (lowestWithModifierBase == null) {
                        lowestWithModifierBase = item;
                    }
                    highestWithModifierBase = item;
                }
            }
            logln("\nlowestWithModifierBase " + lowestWithModifierBase);
            logln("\nhighestWithModifierBase " + highestWithModifierBase);
        } catch (Exception e) {
            errln("Can't build rules: analysing problem…");
            // figure out where the problem is
            String[] list = rules.split("\n");
            rules = "";
            String oldRules = "";
            int i = 0;
            for (String line : list) {
                ++i;
                logln(i + "\t" + line);
                if (i > 1) {
                    rules += "\n";
                }
                rules += line;
                if (i <= SKIPTO) {
                    continue;
                }
                try {
                    RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(rules);
                } catch (Exception e2) {
                    errln("Fails when adding line " + line);
                    errln(showSorting(oldRules));
                    errln(oldRules);
                    throw new ICUException(e2);
                }
                oldRules = rules;
            }
            throw new ICUException(e);
        }
        logln(showSorting(rules));
        logln(rules);
    }

    private String showItem(String lastItem, RuleBasedCollator ruleBasedCollator) {
        return "(" + Utility.hex(lastItem)
        + "; " + emojiDataToTest.getName(lastItem)
        + (ruleBasedCollator == null ? "" : "; " + showCE(lastItem, ruleBasedCollator))
        + ")";
    }

    private String showCE(String item2, RuleBasedCollator ruleBasedCollator) {
        CollationElementIterator it = ruleBasedCollator.getCollationElementIterator(item2);
        StringBuilder temp = new StringBuilder();
        while (true) {
            int item = it.next();
            if (item == CollationElementIterator.NULLORDER) {
                break;
            }
            if (temp.length() != 0) {
                temp.append(' ');
            }
            temp.append(Utility.hex(item & 0xFFFFFFFFL,8));
        }
        String ce = temp.toString();
        return ce;
    }

    private String showSorting(String oldRules) {
        RuleBasedCollator ruleBasedCollator;
        try {
            ruleBasedCollator = new RuleBasedCollator(oldRules);
        } catch (Exception e1) {
            throw new ICUException(e1);
        }
        UnicodeSet chars = ruleBasedCollator.getTailoredSet();
        StringBuilder buffer = new StringBuilder();
        StringBuilder pbuffer = new StringBuilder();
        StringBuilder sbuffer = new StringBuilder();
        StringBuilder tbuffer = new StringBuilder();
        for (String s : chars) {
            CollationElementIterator it = ruleBasedCollator.getCollationElementIterator(s);
            for (int element = it.next(); element != CollationElementIterator.NULLORDER; element = it.next()) {
                if (element == CollationElementIterator.IGNORABLE) {
                    continue;
                }
                int primary = CollationElementIterator.primaryOrder(element);
                pbuffer.append(Utility.hex(primary,4));
                int secondary = CollationElementIterator.secondaryOrder(element);
                sbuffer.append(Utility.hex(secondary,4));
                int tertiary = CollationElementIterator.tertiaryOrder(element);
                tbuffer.append(Utility.hex(tertiary,4));

            }
            buffer.append(s
                    + "\t0x" + Utility.hex(s, " 0x")
                    + "\t0x" + pbuffer
                    + "\t0x" + sbuffer
                    + "\t0x" + tbuffer
                    + "\n");
            pbuffer.setLength(0);
            sbuffer.setLength(0);
            tbuffer.setLength(0);
        }
        return buffer.toString();
    }

    public void TestAnnotationsCompleteness() {
        EmojiAnnotations em = checkAnnotations("en", null);
        //checkAnnotations("de", em);
    }

    private EmojiAnnotations checkAnnotations(final String localeStr, EmojiAnnotations em2) {
        EmojiAnnotations em = new EmojiAnnotations(localeStr, emojiOrderToTest.codepointCompare);
        Set<String> missing = new LinkedHashSet<>();

        TreeSet<String> sorted = emojiDataToTest.getAllEmojiWithoutDefectives()
                .addAllTo(new TreeSet<>(emojiOrderToTest.codepointCompare));
        int maxLen = 32;

        for (String s : sorted) {
            if (s.equals("🕵‍♂️")) {
                int debug = 0;
            }
            Set<String> keywords = em.getKeys(s);
            String tts = em.getShortName(s);
            EmojiAnnotations.Status status = em.getStatus(s);
            if (status != EmojiAnnotations.Status.missing) {
                if (tts.equals("???") || keywords.contains("???")) {
                    logln(s + "\t" + tts + "\t" + keywords);
                }
                //                if (tts.contains(",")) {
                //                    // do nothing
                //                } else if (tts.length() > maxLen) {
                //                    warnln("Name long:\t" + s + "\t" + tts.length() + "\t" + tts + "\t" + keywords);
                //                }
                //                else if (tts.contains(" and ")) {
                //                    warnln("name:\t" + s + "\t" + tts.length() + "\t" + tts + "\t" + keywords);
                //                }
            }
            if (EmojiData.MODIFIERS.containsSome(s)) {
                if (false && em2 == null && status != EmojiAnnotations.Status.missing) {
                    String rem = EmojiData.MODIFIERS.stripFrom(s, false);
                    String s1 = EmojiData.MODIFIERS.stripFrom(s, true);
                    s1 = emojiDataToTest.addEmojiVariants(s1); // modifiers replace EV characters.
                    Set<String> strippedKeywords = em.getKeys(s1);
                    String strippedTts = em.getShortName(s1);
                    EmojiAnnotations.Status strippedStatus = em.getStatus(s1);
                    if (strippedStatus == EmojiAnnotations.Status.missing) {
                        errln("Modifier removed causing missing: "  + s);
                    } else {
                        if (!keywords.containsAll(strippedKeywords)) {
                            errln("Modifier screwy: "  + s + "\t" + keywords + "\t" + strippedKeywords);
                        }
                        if (!tts.startsWith(strippedTts)) {
                            errln("Modifier screwy: "  + s + "\t" + tts + "\t" + strippedTts);
                        }
                    }
                }
                continue;
            }
            if (status != EmojiAnnotations.Status.found) {
                if (em2 == null) {
                    String oldTts = emojiDataToTest.getName(s);
                    Set<String> oldAnnotations = keywords == null ? new TreeSet<>() : new TreeSet<>(keywords);
                    oldAnnotations.addAll(Arrays.asList(oldTts.split("\\s+")));
                    oldAnnotations = oldAnnotations.isEmpty() ? Collections.singleton("???") : oldAnnotations;
                    missing.add("<annotation cp=\"" + s + "\">" + CollectionUtilities.join(oldAnnotations, " | ") + "</annotation>");
                    missing.add("<annotation cp=\"" + s + "\" type=\"tts\">" + oldTts + "</annotation>");
                } else {
                    tts = tts == null ? "???" : tts;
                    keywords = keywords == null ? Collections.singleton("???") : keywords;
                    Set<String> keys = em2.getKeys(s);
                    missing.add(s
                            + "\t" + Utility.hex(s.replace(Emoji.EMOJI_VARIANT_STRING, ""), "_").toLowerCase(Locale.ENGLISH)
                            + "\t" + em2.getShortName(s)
                            + "\t" + (keys == null ? null : CollectionUtilities.join(keys, " | "))
                            + "\t\t\t" + tts
                            + "\t" + CollectionUtilities.join(keywords, " | "));
                }
            } else {
                boolean fail = false;
                if (tts.contains("|")) {
                    errln("TTS with |\t" + s + "\t" + tts + "\t" + keywords);
                    fail = true;
                }
                if (keywords.size() < 1) {
                    warnln("Keywords without |\t" + s + "\t" + tts + "\t" + keywords);
                    fail = true;
                }
                if (!fail) {
                    logln(s + "\t" + tts + "\t" + keywords);
                }
            }
        }
        if (!missing.isEmpty()) {
            warnln("Missing: " + missing.size());
            if (em2 != null) {
                System.out.println("Constructing text for translating missing items.");
                System.out.println("Chars\tEnglish Name\tEnglish Annotations\tNative Name\tNative Annotations\tName Name (constructed!)\tNative Keywords (constructed!)");
            }
            if (SHOW) {
                if (!missing.isEmpty()) {
                    System.out.println("Not an error if CLDR hasn't caught up. Constructing text for translating missing items:");
                }
                for (String s : missing) {
                    System.out.println(s);
                }
            }
        }
        return em;
    }

    public void TestGroupEmoji() {
        EmojiDataSource source = EmojiData.of(Emoji.VERSION_BETA);

        Asserts.assertContains(this, "", "modifierBases", source.getModifierBases(), "multipersonGroupings", source.getMultiPersonGroupings());
        System.out.print("\n\t(modifierBases: " + Asserts.flat(source.getModifierBases()) + ") ");
        System.out.print("\n\t(multipersonGroupings: " + Asserts.flat(source.getMultiPersonGroupings()) + ") ");
        Asserts.assertContains(this, "", "getGenderBases", source.getGenderBases(), "[👯🤼]", new UnicodeSet("[👯🤼]"));
        System.out.print("\n\t(genderBases: " + Asserts.flat(source.getGenderBases()) + ") ");
    }

    public void TestExplicitGender() {
        assertEqualsUS("",
                "list from UTS 51", new UnicodeSet("[👦-👨 🧔 👩 👴 👵 🤴 👸 👲 🧕 🤵 👰 🤰 🤱 🎅 🤶 💃 🕺 🕴 👫-👭]"),
                "emojiData", emojiDataToTest.getExplicitGender());
    }

    public void TestCombinations() {
        UnicodeSet all = emojiDataToTest.getAllEmojiWithoutDefectives();

        //        if (beta instanceof EmojiDataSourceCombined) { // debugging
        //            boolean gotIt = EmojiDataSourceCombined.candidates.getAllEmojiWithoutDefectives().contains("👩‍🦼");
        //            boolean gotIt2 = all.contains("👩‍🦼");
        //            System.out.println(gotIt);
        //        }

        Asserts.assertContains(this, "", "zwj-sequence", all,
                "woman in motorized wheelchair", "👩‍🦼");
        Asserts.assertContains(this, "", "zwj-sequence", all,
                "woman in motorized wheelchair: light skin tone", "👩🏻‍🦼");
//        if (!logKnownIssue("holding hands in zwj sequences", "dropped for now")) {
//        }
        Asserts.assertContains(this, "", "zwj-sequence", all,
                "women holding hands; medium, dark skin", "👩🏿‍🤝‍👩🏽");
    }

    public void TestBuckets() {
        // just check that there is no exception
        for (String s : emojiDataToTest.getAllEmojiWithDefectives()) {
            try {
                Category category = Category.getBucket(s);
            } catch (Exception e) {
                throw new ICUException("If you get the message «no category available for: [zwj, skin]»"
                        + " you may need to update Emoji.PROFESSION_OBJECT", e);
            }
        }
    }
    public void TestAlphagram() {
	    final EmojiData released = EmojiData.of(Emoji.VERSION_BETA);
	    Multimap<String,String> toRgi = TreeMultimap.create();
	    UnicodeSet discarded = new UnicodeSet();
	    for (String item : released.getAllEmojiWithoutDefectives()) {
		String alphagram = getAlphagram(item, released, discarded);
		toRgi.put(alphagram, item);
	    }
	    int counter = 0;
	    System.out.println();
	    for ( Entry<String, Collection<String>> entry : toRgi.asMap().entrySet()) {
		String key = entry.getKey();
		Collection<String> val = entry.getValue();
		if (val.size() > 1) {
		    System.out.println(++counter + ") " + key + " => " + val);
		}
	    }
	    System.out.println("Discarded: " + discarded);
    }

    static final UnicodeSet TO_DISCARD = new UnicodeSet("[\u200D\uFE0F]");
    private String getAlphagram(String item, EmojiData released2, UnicodeSet discarded) {
	int[] cps = CharSequences.codePoints(item);
	Arrays.sort(cps);
	StringBuilder sb = new StringBuilder();
	for (int cp : cps) {
	    if (TO_DISCARD.contains(cp)) {
		discarded.add(cp);
	    } else {
		sb.appendCodePoint(cp);
	    }
	}
	return sb.toString();
    }
}
