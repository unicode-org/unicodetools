package org.unicode.tools.emoji.unittest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CandidateData;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiDataSource;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.unittest.TestFmwkMinusMinus;

@Disabled("Broken? data load issue")
public class TestEmojiOrder extends TestFmwkMinusMinus {

    //    private EmojiOrder order = EmojiOrder.BETA_ORDER;
    //    private CandidateData CANDIDATES = CandidateData.getInstance();
    //
    //    public void TestInclusion() {
    //        Asserts.assertContains(this, "", "charactersToOrdering.keySet", order.charactersToOrdering.keySet(),
    //                "candidates", CANDIDATES.getAllEmojiWithDefectives());
    //    }

	@Test
    public void TestForMissing() {
	check("Released", TestAll.getOrderToTest(), TestAll.getDataToTestPrevious());
	check("Candidates", TestAll.getOrderToTest(), CandidateData.getInstance());
	check("Beta", EmojiOrder.BETA_ORDER, EmojiData.EMOJI_DATA_BETA);
    }

    private void check(String message, EmojiOrder stdOrder, EmojiDataSource emojiDataReleased) {
	UnicodeSet orderSet = stdOrder.charactersToOrdering.keySet();
	UnicodeSet dataSet = emojiDataReleased.getAllEmojiWithDefectives();
	Asserts.assertContains(this, message, "emojiOrderChars", orderSet, "emojiDataChars", dataSet);
    }

    @Test
    public void TestVariant() {
	final String testWV = "⛹️‍♀️";
	final String testNV = "⛹️‍♀️".replace(Emoji.EMOJI_VARIANT_STRING,"");
	final String testBase = "\u26F9";

	assertTrue("emojiData.getGenderBase().contains " + testBase + " " + Utility.hex(testBase), EmojiData.EMOJI_DATA_BETA.getGenderBase().contains(testBase));

	Set<String> variantsBase = EmojiData.EMOJI_DATA_BETA.new VariantFactory().set(testBase).getCombinations();
	Set<String> variantsWV = EmojiData.EMOJI_DATA_BETA.new VariantFactory().set(testWV).getCombinations();
	Set<String> variantsNV = EmojiData.EMOJI_DATA_BETA.new VariantFactory().set(testNV).getCombinations();

	assertTrue("Variants " + testBase + " " + Utility.hex(testBase) + " contains " + testBase + " " + Utility.hex(testBase),
		variantsBase.contains(testBase));

	for (String test : Arrays.asList(testWV, testNV)) {

	    assertNotNull("Ordering to category? " + test + " " + Utility.hex(test), EmojiOrder.BETA_ORDER.getCategory(test));

	    String containsX = " contains " + test + " " + Utility.hex(test);

	    assertTrue("Variants " + testWV + " " + Utility.hex(testWV) + containsX, variantsWV.contains(test));

	    assertTrue("Variants " + testNV + " " + Utility.hex(testNV) + containsX, variantsNV.contains(test));
	}
    }

    public void TestRuleBasedCollator() throws Exception {
	UnicodeSet APPLE_COMBOS = TestAll.getDataToTest().getZwjSequencesNormal();
	UnicodeSet APPLE_COMBOS_WITHOUT_VS = TestAll.getDataToTest().getZwjSequencesAll();

	String rules = TestAll.getOrderToTest().appendCollationRules(new StringBuilder(),
		new UnicodeSet(TestAll.getDataToTest().getChars()).removeAll(Emoji.DEFECTIVE),
		APPLE_COMBOS,
		APPLE_COMBOS_WITHOUT_VS)
		.toString();
	final RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(rules);
	ruleBasedCollator.setStrength(Collator.IDENTICAL);
	ruleBasedCollator.freeze();
	Comparator<String> EMOJI_COMPARATOR = (Comparator<String>) (Comparator) ruleBasedCollator;
	int x = EMOJI_COMPARATOR.compare("#️⃣","☺️");

	String lastCat = "";
	Set<String> seen = new LinkedHashSet<>();
	TreeSet<String> sorted = new TreeSet<>(ruleBasedCollator);
	TestAll.getDataToTest().getAllEmojiWithDefectives().addAllTo(sorted);
	for (String s : sorted) {
	    String cat = EmojiOrder.BETA_ORDER.getCategory(s);
	    if (!cat.equals(lastCat)) {
		assertFalse(cat, seen.contains(cat));
		lastCat = cat;
		logln(cat);
	    }
	}
    }

    @Test
    public void TestOrderRulesWithSkin() {
	checkOrder(null);
    }

    @Test
    public void TestOrderRulesWithoutSkin() {
	checkOrder(EmojiData.MODIFIERS);
    }

    /**
     * Compares the EmojiOrder with the rules it generates
     * @param filterOutIfContains - don't test strings that this set contains some characters from (ignored if null)
     */
    private void checkOrder(UnicodeSet filterOutIfContains) {
	EmojiOrder emojiOrderToTest = TestAll.getOrderToTest();
	EmojiData emojiDataToTest = TestAll.getDataToTest();
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
	+ "; " + TestAll.getDataToTest().getName(lastItem)
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

}
