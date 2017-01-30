package org.unicode.tools.emoji.unittest;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiAnnotations;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.tools.emoji.GenerateEmojiData;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;

public class TestEmojiData extends TestFmwkPlus {

    public static void main(String[] args) {
    	System.out.println("Version: " + Emoji.VERSION_TO_GENERATE + "; isBeta: " + Emoji.IS_BETA);
        new TestEmojiData().run(args);
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
        assertEquals("Contains all good regions", UnicodeSet.EMPTY, new UnicodeSet(shouldBeFlagEmoji).removeAll(EmojiData.EMOJI_DATA.getChars()));
        logln("Should not be flags: " + shouldNOTBeFlagEmoji.toPattern(false));
        assertEquals("Contains no bad regions", UnicodeSet.EMPTY, new UnicodeSet(shouldNOTBeFlagEmoji).retainAll(EmojiData.EMOJI_DATA.getChars()));
    }

    public void TestZwjCategories () {
        UnicodeMap<String> chars = new UnicodeMap<>();
        for (String s : EmojiData.EMOJI_DATA.getZwjSequencesNormal()) {
            GenerateEmojiData.ZwjType zwjType = ZwjType.getType(s);
            String grouping = EmojiOrder.STD_ORDER.charactersToOrdering.get(s);
            chars.put(s, zwjType + "\t" + grouping);
        }
        for (String value: chars.values()) {
            final UnicodeSet set = chars.getSet(value);
            System.out.println(value + "\t" + set.size() + "\t" + set.toPattern(false));
        }
        Set<String> testSet = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().addAllTo(testSet);
        
        ZwjType oldZwjType = ZwjType.na; 
        String last = "";
        for (String s : testSet) {
            ZwjType zwjType = ZwjType.getType(s);
            if (zwjType == ZwjType.na) {
            	continue;
            }
            if (zwjType.compareTo(oldZwjType) < 0 && oldZwjType != ZwjType.na) {
            	errln(zwjType + " < " + oldZwjType 
            			+ ", but they should be ascending"
            			+ "\n\t" + oldZwjType + "\t" + last 
            			+ "\n\t" + zwjType + "\t" + s);
            }
            last = s;
            oldZwjType = zwjType;
        }
    }

    public void TestOrderRules() throws Exception {
        int SKIPTO = 400;
        RuleBasedCollator ruleBasedCollator;
        ruleBasedCollator = new RuleBasedCollator("&a <*🍱🍘🍙🍚🍛🍜🍝🍠🍢🍣🍤🍥🍡");
        //        UnicodeSet ruleSet = new UnicodeSet();
        //        for (String s : EmojiData.EMOJI_DATA.getEmojiForSortRules()) {
        //            // skip modifiers not in zwj, as hack
        //            if (true || s.contains(Emoji.JOINER_STR) || EmojiData.MODIFIERS.containsNone(s)) {
        //                ruleSet.add(s);
        //            }
        //        }
        StringBuilder outText = new StringBuilder();
        EmojiOrder.STD_ORDER.appendCollationRules(outText, EmojiData.EMOJI_DATA.getEmojiForSortRules(), EmojiOrder.GENDER_NEUTRALS);
        String rules = outText.toString();
        UnicodeSet modifierBases = EmojiData.EMOJI_DATA.getModifierBases();
        UnicodeSet modifiers = EmojiData.EMOJI_DATA.getModifiers();
        try {
            ruleBasedCollator = new RuleBasedCollator(rules);
            Set<String> testSet = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
            EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().addAllTo(testSet);
            String lastItem = "";
            String highestWithModifierBase = null;
            String lowestWithModifierBase = null;
            for (String item : testSet) {
                if (ruleBasedCollator.compare(lastItem, item) > 0 
                		&& !modifiers.contains(item)) {
                    errln("Out of order: " + lastItem + ">" + item);
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
                    throw (e2);
                }
                oldRules = rules;
            }
            throw (e);
        }
        logln(showSorting(rules));
        logln(rules);
    }

    private String showSorting(String oldRules) throws Exception {
        RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(oldRules);
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

        TreeSet<String> sorted = EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()
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
                    s1 = EmojiData.EMOJI_DATA.addEmojiVariants(s1); // modifiers replace EV characters.
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
                    String oldTts = EmojiData.EMOJI_DATA.getName(s, true, null);
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
}
