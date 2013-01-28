package org.unicode.idna;

import com.ibm.icu.dev.util.UnicodeTransform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;

public class FilteredUnicodeTransform extends UnicodeTransform {
	final UnicodeSet transformOnly;
	final UnicodeTransform baseTransform;
	public FilteredUnicodeTransform(UnicodeTransform transform, UnicodeSet unicodeSet) {
		baseTransform = transform;
		transformOnly = unicodeSet;
	}
	@Override
	public String transform(String source) {
		final StringBuilder builder = new StringBuilder();
		int start = 0;

		while (true) {
			int end = transformOnly.span(source, start, SpanCondition.CONTAINED);
			if (end > start) {
				builder.append(baseTransform.transform(source.substring(start, end)));
			}
			if (end == source.length()) {
				break;
			}
			start = end;
			end = transformOnly.span(source, start, SpanCondition.NOT_CONTAINED);
			if (end > start) {
				builder.append(source.substring(start, end));
			}
			if (end == source.length()) {
				break;
			}

		}
		return builder.toString();
	}
}
