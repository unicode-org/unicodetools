package org.unicode.tools.emoji;

import java.util.function.Predicate;

import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

public class DebugUtilities {
    
    public static String composeStringsWhen(String title, Iterable<String> strings, Predicate<String> predicate) {
        StringBuilder b = new StringBuilder();
        for (String s : strings) {
            if (predicate.test(s)) {
                if (b.length() == 0) {
                    b.append('\n');
                }
                b.append(title + "\t" + hexAndName(s));
            }
        }
        return b.toString();
    }

    public static void debugStringsWhen(String title, Iterable<String> strings, Predicate<String> predicate) {
        System.out.println(composeStringsWhen(title, strings, predicate));
    }

    public static String hexAndName(String s) {
        return s + "\t" + Utility.hex(s) + "\t" + Default.ucd().getName(s);
    }
}
