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
        return split(m, input, new ArrayList<>(), null);
    }

    public static String[] split(Matcher m, String input) {
        m.reset(input);
        return split(m, input, new ArrayList<>(), null);
    }

    /**
     * Split correctly. The matcher must be set to the input before calling.
     *
     * @param m
     * @param input
     * @param matchList String list which gets values appended. Make new or clear first.
     * @param destArray Destination array for reuse if possible. Can be null.
     * @return array of String values between matching delimiters
     */
    public static String[] split(Matcher m, String input,
            ArrayList<String> matchList, String[] destArray) {
        int lastPos = 0;
        while (true) {
            final boolean found = m.find();
            final int start = found ? m.start() : input.length();
            matchList.add(input.substring(lastPos, start));
            if (!found) {
                break;
            }
            lastPos = m.end();
        }
        if (destArray == null || destArray.length != matchList.size()) {
            destArray = new String[matchList.size()];
        }
        return matchList.toArray(destArray);
    }

}
