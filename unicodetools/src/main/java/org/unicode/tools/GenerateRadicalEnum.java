package org.unicode.tools;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public class GenerateRadicalEnum {
    public static void main(String[] args) {
        final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
        UnicodeMap<List<String>> unicodeToRadicalRaw = iup.loadList(UcdProperty.CJK_Radical);
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        col.setNumericCollation(true);
        col.freeze();
        TreeSet<String> sorted = new TreeSet<>(col);
        for (Entry<String, List<String>> entry : unicodeToRadicalRaw.entrySet()) {
            for (String item : entry.getValue()) {
                sorted.add(item);
            }
        }
        for (String item : sorted) {
            boolean prime = item.endsWith("'");
            if (prime) {
                item = item.substring(0,item.length()-1);
            }
            System.out.println("R" + item + (prime ? "a" : "") + ",");
        }
    }
}
