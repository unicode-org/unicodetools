package org.unicode.unittest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.UnicodeSet;

import org.junit.jupiter.api.Test;
import org.unicode.text.tools.RegexBuilder;
import org.unicode.text.tools.RegexBuilder.Style;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiData.VariantFactory;

public class TestRegexBuilder extends TestFmwkMinusMinus {
    private static final char ENCLOSING_CIRCLE_BACKSLASH = '\u20E0'; //  ⃠
    private static final String U20E0Q = RegexBuilder.showChar(0x20e0, new StringBuilder()) + "?";


	@Test
    public void TestSimple() {
	EmojiData edata = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
	logger.info("Version: " + Emoji.VERSION_LAST_RELEASED);
//	final UnicodeSet fullSet = new UnicodeSet()
//		.addAll(edata.getChars())
//		.addAll(edata.getModifierSequences())
//		.addAll(edata.getZwjSequencesNormal())
//		.freeze();

	final UnicodeSet withDefectives = new UnicodeSet();
	VariantFactory vf = edata.new VariantFactory();
	for (String cp_raw : edata.getAllEmojiWithDefectives()) {
	    vf.set(cp_raw);
	    for (String s : vf.getCombinations()) {
		switch(edata.getVariantStatus(s)) {
		case other:
		    // do nothing;
		    break;
		default:
		    withDefectives.add(s);
		    break;
		}
	    }
	}
	withDefectives.freeze();
	final UnicodeSet fullSet = withDefectives;

	final UnicodeSet flatSet = new UnicodeSet(fullSet).removeAll(fullSet.strings());
	for (String s : fullSet.strings()) {
	    flatSet.addAll(s);
	}
	flatSet.add(0x20E0);
	flatSet.freeze();
	for (Style style : Style.values()) {
	    System.out.println("\n" + style);
	    for (int i = 0; i < 2; ++i) {
		UnicodeSet setToDisplay;
		if (i == 0) {
		    if (true) continue; // skip for now
		    logger.fine("\nChars Only");
		    setToDisplay = flatSet;
		} else {
		    logger.fine("\nSequences");
		    setToDisplay = fullSet;
		}
		RegexBuilder b = new RegexBuilder(style);
		b.addAll(setToDisplay);
		b.finish();

		String result = b.toString(true);
		if (i == 1) result += U20E0Q;
		logger.fine(result);
		// check values
		if (style != Style.CODEPOINT_REGEX) {
		    continue;
		}
		result = b.toString(false);
		if (i == 1) result += U20E0Q;

		Pattern check = Pattern.compile(result);
		Matcher checkMatcher = check.matcher("");
		UnicodeSet others = new UnicodeSet(0,0x10FFFF);
		for (String s : setToDisplay) {
		    if (!checkMatcher.reset(s).matches()) {
			errln("FAILS: " + s);
		    }
		    if (checkMatcher.reset(s+"A").matches()) {
			errln("FAILS x + A: " + s);
		    }
		    others.remove(s);
		}
		for (String s : others) {
		    if (checkMatcher.reset(s).matches()) {
			errln("Doesn't FAIL (but should): " + s);
		    }
		}

		//        b = new RegexBuilder(RegexBuilder.Style.CHAR_REGEX)
		//        .addAll(edata.getChars())
		//        .addAll(edata.getModifierSequences())
		//        .addAll(edata.getZwjSequencesNormal())
		//        .finish();
		//        System.out.println(b.toString(true));
	    }
	}
    }
}
