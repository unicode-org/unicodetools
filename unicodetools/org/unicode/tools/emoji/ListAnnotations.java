package org.unicode.tools.emoji;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.ChartAnnotations;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;

public class ListAnnotations {
	public static void main(String[] args) {
		AnnotationSet eng = Annotations.getDataSet("en");
		UnicodeMap<Annotations> annotationsMap = eng.getUnresolvedExplicitValues();
		IndexUnicodeProperties iup = IndexUnicodeProperties.make();
		EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
		Set<String> keys = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare); // ChartAnnotations.RBC
		annotationsMap.keySet().addAllTo(keys);
		int i = 0;
        UnicodeMap<Integer> years = EmojiData.getYears();

		for (String emoji : keys) {
            ;
			String category = order.getCategory(emoji);
            System.out.println(years.get(emoji) 
			        + "\t" + emoji 
			        + "\t" + ++i 
                    + "\t" + order.getMajorGroupFromCategory(category).toPlainString()
                    + "\t" + category
                    + "\t" + eng.getShortName(emoji) 
                    + "\t"
			        + "\t" + CldrUtility.join(eng.getKeywords(emoji), " | ")
                    + "\t"
                    + "\t" + Utility.hex(emoji, " + ")
                    + "\t" + iup.getName(emoji, " + ")
			        );
		}
	}
}
