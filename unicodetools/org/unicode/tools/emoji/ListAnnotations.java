package org.unicode.tools.emoji;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.ChartAnnotations;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.props.IndexUnicodeProperties;

import com.ibm.icu.dev.util.UnicodeMap;

public class ListAnnotations {
	public static void main(String[] args) {
		AnnotationSet eng = Annotations.getDataSet("en");
		final UnicodeMap<Annotations> map = eng.getUnresolvedExplicitValues();
		IndexUnicodeProperties iup = IndexUnicodeProperties.make();
		Set<String> keys = new TreeSet<>(ChartAnnotations.RBC);
		map.keySet().addAllTo(keys);
		int i = 0;
		for (String emoji : keys) {
			System.out.println(emoji + "\t" + ++i + "\t" + eng.getShortName(emoji) + "\t" + iup.getName(emoji, " + "));
		}

	}
}
