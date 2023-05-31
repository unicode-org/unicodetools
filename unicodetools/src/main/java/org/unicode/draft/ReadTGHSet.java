package org.unicode.draft;

import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

// See https://unicode-org.atlassian.net/browse/CLDR-16571
public class ReadTGHSet {
    public static void main(String[] args) throws FileNotFoundException {
        UnicodeSet set = new UnicodeSet();
        Scanner sc =
                new Scanner(new File("/usr/local/google/home/mscherer/Downloads/tgh2013-8105.txt"));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int tabPos = line.indexOf('\t');
            if (!line.startsWith("U+") || tabPos < 0 || 7 < tabPos) {
                throw new IllegalArgumentException(line);
            }
            String hex = line.substring(2, tabPos);
            int cp = Integer.parseInt(hex, 16);
            set.add(cp);
        }
        System.out.println("TGH 2013 code points as a UnicodeSet pattern string:");
        String pattern = set.toPattern(false);
        System.out.println(pattern);
        System.out.println("TGH 2013 number of code points: " + set.size());
        System.out.println("TGH 2013 UnicodeSet pattern Java string literal:");
        int length = pattern.length();
        for (int start = 0, limit = 0; limit < length; start = limit) {
            if (start > 0) {
                System.out.print("+ ");
            }
            // Advance by up to 60 code points.
            if ((start + 120) <= length) {
                limit = pattern.offsetByCodePoints(start, 60);
            } else {
                for (int i = 0; i < 60 && limit < length; ++i) {
                    limit = pattern.offsetByCodePoints(limit, 1);
                }
            }
            // Nice to have: Keep an A-Z range together.
            // Move the last character to the next segment if that would start with '-'.
            // Move the last two to the next segment if this one would end with '-'.
            // (In this pattern of a set of non-ASCII characters, the dash only occurs between
            // two Han characters.)
            if (pattern.charAt(limit - 1) == '-') {
                limit = pattern.offsetByCodePoints(limit - 1, -1);
            } else if (limit < length && pattern.charAt(limit) == '-') {
                limit = pattern.offsetByCodePoints(limit, -1);
            }
            System.out.println("\"" + pattern.substring(start, limit) + '"');
        }
    }
}
