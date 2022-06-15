package org.unicode.propstest;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;

public class CheckEmojiCollation {
    static IndexUnicodeProperties IUP = IndexUnicodeProperties.make();
    static UnicodeSet ASSIGNED =
            IUP.loadEnum(UcdProperty.General_Category, General_Category_Values.class)
                    .getSet(General_Category_Values.Unassigned)
                    .complement();
    static UnicodeSet EMOJI = IUP.loadEnum(UcdProperty.Emoji, Binary.class).getSet(Binary.Yes);

    public static void main(String[] args) throws Exception {
        if (false) checkGaps();
        if (true) showOrder();
    }

    private static void showOrder() throws Exception {
        String[] locales = {
            "raw",
            "en",
            "da",
            "el",
            "ja",
            "en-u-co-emoji",
            "da-u-co-emoji",
            "ja-u-co-emoji",
            "zh-u-co-emoji",
            "da+und-u-co-emoji",
            "und-u-co-emoji+da",
            "el+und-u-co-emoji",
            "und-u-co-emoji+el",
            "ja+und-u-co-emoji",
            "und-u-co-emoji+ja",
            "zh+und-u-co-emoji",
            "und-u-co-emoji+zh",
        };
        String[] chars = {
            ",", "🂾", "€", "1", "a", "y", "ü", "Z", "β", "😀", "✈\uFE0F️", "\u2639\uFE0F", "글", "字"
        };
        int aPosition = 8;
        for (String locale : locales) {
            show(locale, aPosition, chars);
        }
    }

    private static void checkGaps() {
        Collator root = Collator.getInstance(ULocale.ROOT);
        TreeSet<String> sorted = ASSIGNED.addAllTo(new TreeSet<>(root));
        boolean wasEmoji = false;
        UnicodeSet current = new UnicodeSet();
        for (String s : sorted) {
            boolean isEmoji = EMOJI.contains(s);
            if (isEmoji != wasEmoji) {
                showRange(wasEmoji, current);
                current.clear();
                wasEmoji = isEmoji;
            }
            current.add(s);
        }
        showRange(wasEmoji, current);
    }

    private static void showRange(boolean wasEmoji, UnicodeSet current) {
        System.out.println(
                (wasEmoji ? "Emoji" : "Not")
                        + "\t"
                        + current.size()
                        + "\t"
                        + current.toPattern(false));
    }

    private static void show(String locale, int aPosition, String... items) throws Exception {
        Comparator collator =
                locale.equals("raw")
                        ? new UTF16.StringComparator(true, false, 0)
                        : locale.contains("+")
                                ? combine(locale.split("\\+"))
                                : Collator.getInstance(ULocale.forLanguageTag(locale));
        List<String> list = new ArrayList(Arrays.asList(items));
        list.sort(collator);
        while (list.indexOf("a") < aPosition) { // make things easier to see by aligning on 'a'
            list.add(1, " ");
        }
        System.out.println(
                "|| " + locale + " || " + CollectionUtilities.join(list, " || ") + " ||");
    }

    private static Collator combine(String... locales) throws Exception {
        StringBuilder buffer = new StringBuilder();
        for (String locale : locales) {
            String rules =
                    ((RuleBasedCollator) Collator.getInstance(ULocale.forLanguageTag(locale)))
                            .getRules();
            buffer.append(rules);
        }
        RuleBasedCollator collator = new RuleBasedCollator(buffer.toString());
        return collator;
    }
}
