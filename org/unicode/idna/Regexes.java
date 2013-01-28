package org.unicode.idna;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexes {

	/**
	 * Split a string correctly. That is, empty values between matches are always returned, wherever they are.
	 * Splitting "..A..B.." with [.] returns ["", "", "A", "B", "", ""].
	 * @param pattern
	 * @param input
	 * @return split array.
	 */
	public static String[] split(Pattern pattern, String input) {
		final Matcher m = pattern.matcher(input);
		return split(m, input, false);
	}

	public static String[] split(Matcher m, String input) {
		m.reset(input);
		return split(m, input, false);
	}

	/**
	 * Split correctly. The matcher must be set to the input before calling.
	 * @param m
	 * @param input
	 * @param includeMatches determines whether or not the matched substrings are included or not
	 * @return
	 */
	public static String[] split(Matcher m, String input, boolean includeMatches) {
		final ArrayList<String> matchList = new ArrayList<String>();
		int lastPos = 0;
		while (true) {
			final boolean found = m.find();
			final int start = found ? m.start() : input.length();
			matchList.add(input.substring(lastPos, start));
			if (!found) {
				break;
			}
			if (includeMatches) {
				matchList.add(input.substring(start, m.end()));
			}
			lastPos = m.end();
		}
		return matchList.toArray(new String[matchList.size()]);
	}

}
