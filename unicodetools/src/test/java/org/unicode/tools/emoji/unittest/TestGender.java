package org.unicode.tools.emoji.unittest;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.unittest.TestFmwkMinusMinus;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

import com.ibm.icu.text.AlphabeticIndex.Bucket;

public class TestGender extends TestFmwkMinusMinus {

	@Test
    public void testGender() {
	Set<String> sorted = new TreeSet(EmojiOrder.STD_ORDER.BETA_ORDER.codepointCompare);
	EmojiData.EMOJI_DATA_BETA.getAllEmojiWithoutDefectivesOrModifiers().addAllTo(sorted);
	for (String woman_role : sorted) {
	    if (woman_role.startsWith(Emoji.WOMAN_STR)) {
		Category bucket = Category.getBucket(woman_role);
		switch (bucket) {
		case zwj_seq_role:
		case zwj_seq_hair:
		    String mod = woman_role.replace(Emoji.WOMAN_STR, Emoji.ADULT);
		    String womanName = EmojiData.EMOJI_DATA_BETA.getName(woman_role);
		    String personName = womanName
			    .replace("woman in ", "person in ")
			    .replace("woman with ", "person with ")
			    .replace("woman: ", "person: ")
			    .replace("woman ", "");
		    System.out.println("Proposal=L2/19-078\n"
			    + "After=" + woman_role + "\n"
			    + "U+" + Utility.hex(mod, " U+")+ "\n"
			    + "Name=" + personName+ "\n"
			    + "Keywords=TBD\n"
			    );
		    break;
		};
	    }
	}
    }
}
