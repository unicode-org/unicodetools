package org.unicode.tools.emoji.unittest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiAnnotations;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiData.VariantStatus;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.tools.emoji.GenerateEmojiData;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class TestEmojiData extends TestFmwkPlus {
    final EmojiData released = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
    final EmojiDataSource beta;

    public static void main(String[] args) {
        new TestEmojiData().run(args);
    }
    
    /**
     * We structure the test this way so that we can run it with two different sets of data.
     */
    public TestEmojiData(EmojiDataSource beta) {
        this.beta = beta;
    }

    public TestEmojiData() {
        this(EmojiData.of(Emoji.VERSION_BETA));
    }
    
    public void TestA() {
        System.out.print(" Version: " + beta.getVersionString()
                + "; class: " + beta.getClass()
                );
    }

    public static final Splitter semi = Splitter.onPattern("[;#]").trimResults();

    public void TestPublicEmojiTest() {
        if (beta instanceof EmojiDataSourceCombined) {
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
        assertEqualsUS(VariantStatus.full.toString(), 
                "emoji-test", 
                tests.getSet(VariantStatus.full), 
                "EmojiData", 
                new UnicodeSet(beta.getBasicSequences())
                .addAll(beta.getKeycapSequences())
                .addAll(beta.getFlagSequences())
                .addAll(beta.getTagSequences())
                .addAll(beta.getModifierSequences())
                .addAll(beta.getZwjSequencesNormal())
                .removeAll(new UnicodeSet("[🇦-🇿🏻-🏿🦰-🦳{#️}{*️}{0️}{1️}{2️}{3️}{4️}{5️}{6️}{7️}{8️}{9️}]"))
                );
        assertEqualsUS(VariantStatus.component.toString(), 
                "emoji-test", 
                tests.getSet(VariantStatus.component), 
                "EmojiData", 
                new UnicodeSet(beta.getEmojiComponents())
                .removeAll(new UnicodeSet("[#*0-9‍⃣️🇦-🇿󠀠-󠁿]"))
                );
//        assertEqualsUS(VariantStatus.other + " = emoji", 
//                "?", 
//                new UnicodeSet(tests.getSet(VariantStatus.other)).add(tests.getSet(VariantStatus.initial)), "?", new UnicodeSet(beta.getAllEmojiWithDefectives()).removeAll(beta.getAllEmojiWithoutDefectives()));
    }
    
    private void assertEqualsUS(String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        if (s1.equals(s2)) {
            return;
        }
        assertContains(message, s1Name, s1, s2Name, s2);
        assertContains(message, s2Name, s2, s1Name, s1);
    }

    private void assertContains(String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        UnicodeSet s2minuss1 = new UnicodeSet(s2).removeAll(s1);
        if (!s2minuss1.isEmpty()) {
            errln(message + ", " + s2Name + " - " + s1Name + " ≠ ∅: " + s2minuss1.toPattern(false));
        }
    }

    public void TestHandshake() {
        beta.getName("👩"); // warm up
        assertEquals("👩‍🤝‍👩", "two women holding hands", beta.getName("👩‍🤝‍👩"));
        assertEquals("👩🏿‍🤝‍👩🏻", "two women holding hands: dark skin tone, light skin tone", beta.getName("👩🏿‍🤝‍👩🏻"));
        assertEquals("👩🏼‍🤝‍👨🏿", "man and woman holding hands: medium-light skin tone, dark skin tone", beta.getName("👩🏼‍🤝‍👨🏿"));
        assertEquals("👨🏿‍🤝‍👨🏿", "two men holding hands: dark skin tone", beta.getName("👨🏿‍🤝‍👨🏿"));
    }
    
    public void TestCompoundNames() {
        beta.getName("👩"); // warm up
        assertEquals("🚶🏻‍♂️", "man walking: light skin tone", beta.getName("🚶🏻‍♂️"));
        assertEquals("🧍", "person standing", beta.getName("🧍"));
        assertEquals("🧍🏻", "person standing: light skin tone", beta.getName("🧍🏻"));
        assertEquals("🧍🏻‍♂️", "man standing: light skin tone", beta.getName("🧍🏻‍♂️"));
        assertEquals("🧍\u200D♂️", "man standing", beta.getName("🧍\u200D♂️"));
    }

    public void TestDefectives() {
        UnicodeSet excluded = new UnicodeSet("[#*0-9🇦-🇿]");

        for (EmojiDataSource ed : Arrays.asList(released, beta)) {
            if (ed.getAllEmojiWithDefectives().containsSome(Emoji.DEFECTIVE_COMPONENTS)) {
                errln("getChars contains defectives " 
                        + new UnicodeSet().addAll(ed.getAllEmojiWithoutDefectives()).retainAll(Emoji.DEFECTIVE_COMPONENTS));
            }
        }
        if (beta.getExtendedPictographic().containsSome(excluded)) {
            errln("getExtendedPictographic contains defectives " 
                    + new UnicodeSet().addAll(beta.getExtendedPictographic()).retainAll(excluded));
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
        assertEquals("Contains all good regions", UnicodeSet.EMPTY, new UnicodeSet(shouldBeFlagEmoji).removeAll(beta.getAllEmojiWithoutDefectives()));
        logln("Should not be flags: " + shouldNOTBeFlagEmoji.toPattern(false));
        assertEquals("Contains no bad regions", UnicodeSet.EMPTY, new UnicodeSet(shouldNOTBeFlagEmoji).retainAll(beta.getAllEmojiWithoutDefectives()));
    }

    /**
     * Not working yet, so blocking for now.
     */
    public void T_estZwjCategories () {
        UnicodeMap<String> chars = new UnicodeMap<>();
        for (String s : beta.getZwjSequencesNormal()) {
            CountEmoji.Category zwjType = CountEmoji.Category.getType(s);
            String grouping = EmojiOrder.STD_ORDER.charactersToOrdering.get(s);
            chars.put(s, zwjType + "\t" + grouping);
        }
        for (String value: chars.values()) {
            final UnicodeSet set = chars.getSet(value);
            System.out.println(value + "\t" + set.size() + "\t" + set.toPattern(false));
        }
        Set<String> testSet = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        beta.getAllEmojiWithoutDefectives().addAllTo(testSet);

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

    public void TestOrderRules() {
        int SKIPTO = 400;
        RuleBasedCollator ruleBasedCollator;
        try {
            ruleBasedCollator = new RuleBasedCollator("&a <*🍱🍘🍙🍚🍛🍜🍝🍠🍢🍣🍤🍥🍡");
        } catch (Exception e1) {
            throw new ICUException(e1);
        }
        //        UnicodeSet ruleSet = new UnicodeSet();
        //        for (String s : beta.getEmojiForSortRules()) {
        //            // skip modifiers not in zwj, as hack
        //            if (true || s.contains(Emoji.JOINER_STR) || EmojiData.MODIFIERS.containsNone(s)) {
        //                ruleSet.add(s);
        //            }
        //        }
        StringBuilder outText = new StringBuilder();
        EmojiOrder.STD_ORDER.appendCollationRules(outText, beta.getEmojiForSortRules(), EmojiOrder.GENDER_NEUTRALS);
        String rules = outText.toString();
        UnicodeSet modifierBases = beta.getModifierBases();
        UnicodeSet modifiers = new UnicodeSet(EmojiData.getModifiers()).addAll(Emoji.HAIR_BASE).freeze();
        try {
            ruleBasedCollator = new RuleBasedCollator(rules);
            Set<String> testSet = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
            beta.getAllEmojiWithDefectives().addAllTo(testSet);
            String secondToLastItem = "";
            String lastItem = "";
            String highestWithModifierBase = null;
            String lowestWithModifierBase = null;
            for (String item : testSet) {
                if (ruleBasedCollator.compare(lastItem, item) > 0 
                        && !modifiers.contains(item)) {
                    errln("Out of order: " + secondToLastItem + ">" + lastItem + ">" + item);
                } else {
                    logln(lastItem + "≤" + item);
                }
                secondToLastItem = lastItem;
                lastItem = item;
                if (modifierBases.containsSome(item)) {
                    if (lowestWithModifierBase == null) {
                        lowestWithModifierBase = item;
                    }
                    highestWithModifierBase = item;
                }
            }
            System.out.println("\nlowestWithModifierBase " + lowestWithModifierBase);
            System.out.println("\nhighestWithModifierBase " + highestWithModifierBase);
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
                    ruleBasedCollator = new RuleBasedCollator(rules);
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
        EmojiAnnotations em = new EmojiAnnotations(localeStr, EmojiOrder.STD_ORDER.codepointCompare);
        Set<String> missing = new LinkedHashSet<>();

        TreeSet<String> sorted = beta.getAllEmojiWithoutDefectives()
                .addAllTo(new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare));
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
                    s1 = beta.addEmojiVariants(s1); // modifiers replace EV characters.
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
                    String oldTts = beta.getName(s);
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
            errln("Missing: " + missing.size());
            if (em2 != null) {
                System.out.println("Constructing text for translating missing items.");
                System.out.println("Chars\tEnglish Name\tEnglish Annotations\tNative Name\tNative Annotations\tName Name (constructed!)\tNative Keywords (constructed!)");
            }
            for (String s : missing) {
                System.out.println(s);
            }
        }
        return em;
    }
    
    public void TestGroupEmoji() {
        assertContains("", "modifierBases", beta.getModifierBases(), "multipersonGroupings", beta.getMultiPersonGroupings());
        assertContains("", "👯🤼", beta.getGenderBases(), "multipersonGroupings", new UnicodeSet("[👯🤼]"));
        for (String s : beta.getExplicitGender()) {
            System.out.print(s);
        }
    }
    
    public void TestExplicitGender() {
        assertEqualsUS("", 
                "list from UTS 51", new UnicodeSet("[👦-👨 🧔 👩 👴 👵 🤴 👸 👲 🧕 🤵 👰 🤰 🤱 🎅 🤶 💃 🕺 🕴 👫-👭]"), 
                "emojiData", beta.getExplicitGender());
    }
    
    public void TestCombinations() {
        assertContains("", "zwj-sequences", beta.getZwjSequencesNormal(), 
                "woman with probing cane", new UnicodeSet("[{\\x{1F469}\u200D\\x{1F9AF}\uFE0F}]"));
        assertContains("", "zwj-sequences", beta.getZwjSequencesNormal(), 
                "woman with probing cane; light skin", new UnicodeSet("[{\\x{1F469}\\x{1F3FB}\u200D\\x{1F9AF}\uFE0F}]"));
        // 1F469 200D 1F9AF FE0F
    }
}
