/**
 * 
 */
package org.unicode.text.UCA;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;

class ReorderingTokens {

    Counter<String> reorderingToken = new Counter<String>();
    Counter<String> types = new Counter<String>();
    private final Set<Long> primaryCount = new TreeSet<Long>();
    private final UnicodeSet chars = new UnicodeSet();

    public ReorderingTokens() {
    }

    public ReorderingTokens(ReorderingTokens scriptSet) {
        or(scriptSet);
    }

    public boolean intersects(ReorderingTokens set) {
        return !Collections.disjoint(reorderingToken.keySet(), set.reorderingToken.keySet());
    }

    public void or(ReorderingTokens set) {
        reorderingToken.addAll(set.reorderingToken);
        types.addAll(set.types);
        primaryCount.addAll(set.primaryCount);
        chars.addAll(set.chars);
    }

    public int cardinality() {
        return reorderingToken.size()
                + types.size();
    }

    void addInfoFrom(long primary, String source) {
        primaryCount.add(primary);
        chars.add(source);
        final int cp = Default.nfkd().normalize(source).codePointAt(0);
        //for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        //cp = source.codePointAt(i);
        final byte cat = Fractional.getFixedCategory(cp);
        final int script = Fractional.getFixedScript(cp);

        if (!(script == UCD_Types.Unknown_Script || script == UCD_Types.COMMON_SCRIPT)
                && (cat == UCD_Types.OTHER_LETTER || cat == UCD_Types.UPPERCASE_LETTER || cat == UCD_Types.LOWERCASE_LETTER || cat == UCD_Types.TITLECASE_LETTER)) {
            // Add script aliases Hira & Hrkt before adding Kana.
            if (script == UCD_Types.KATAKANA_SCRIPT && !reorderingToken.containsKey("Hira")) {
                reorderingToken.add("Hira", 1);
                reorderingToken.add("Hrkt", 1);
            }
            reorderingToken.add(UCD.getScriptID_fromIndex((short) script, UCD_Types.SHORT), 1);
            // Add script aliases Hans & Hant after Hani.
            if (script == UCD_Types.HAN_SCRIPT && !reorderingToken.containsKey("Hans")) {
                reorderingToken.add("Hans", 1);
                reorderingToken.add("Hant", 1);
            }
        } else {
            types.add(UCD.getCategoryID_fromIndex(cat, UCD_Types.SHORT), 1);
        }
        //}
    }

    static String getTypesCombined(String chr) {
        String typeKD = ReorderingTokens.getTypes(Default.nfkd().normalize(chr));

        final String type = ReorderingTokens.getTypes(chr);
        if (!type.equals(typeKD)) {
            typeKD = typeKD + "/" + type;
        }
        return typeKD;
    }


    public static String getTypes(String source) {
        //StringBuilder result = new StringBuilder();
        String result;
        final int cp = source.codePointAt(0);
        //for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        //cp = source.codePointAt(i);
        final byte cat = Fractional.getFixedCategory(cp);
        final int script = Fractional.getFixedScript(cp);
        //        if (result.length() != 0) {
        //          result.append(' ');
        //        }
        if (!(script == UCD_Types.Unknown_Script || script == UCD_Types.COMMON_SCRIPT)
                && (cat == UCD_Types.OTHER_LETTER || cat == UCD_Types.UPPERCASE_LETTER || cat == UCD_Types.LOWERCASE_LETTER || cat == UCD_Types.TITLECASE_LETTER)) {
            result = (UCD.getScriptID_fromIndex((short) script, UCD_Types.SHORT));
        } else {
            result = (UCD.getCategoryID_fromIndex(cat, UCD_Types.SHORT));
        }
        // }
        return result;
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    static Set<String> common = new TreeSet<String>();
    static {
        common.add(UCD.getScriptID_fromIndex(UCD_Types.COMMON_SCRIPT, UCD_Types.SHORT));
        common.add(UCD.getScriptID_fromIndex(UCD_Types.Unknown_Script, UCD_Types.SHORT));
        common.add(UCD.getScriptID_fromIndex(UCD_Types.INHERITED_SCRIPT, UCD_Types.SHORT));
    }

    <T extends Appendable> T  appendTo(T result, boolean categoriesAlso) {
        try {
            if (reorderingToken.size() == 0 && types.size() == 0) {
                return result;
            }
            if (categoriesAlso) {
                result.append("[").append(primaryCount.size() + "").append("]\t");
            }
            if (!categoriesAlso) {
                final String scriptNames = reorderingToken.size() != 0 ? CollectionUtilities.join(reorderingToken.keySet(), " ") : "Zyyy";
                result.append(scriptNames);
            } else {
                final String scriptNames = reorderingToken.size() != 0 ? joinCounter(reorderingToken) : "";
                result.append(scriptNames);
                if (types.size() != 0) {
                    final String catNames = joinCounter(types);
                    if (reorderingToken.size() != 0) {
                        result.append(' ');
                    }
                    result.append(catNames);
                }
                //result.append("\t").append(chars.toPattern(false));
            }
            return result;
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getRawReorderingTokens() {
        return reorderingToken.toString();
    }

    private String joinCounter(Counter<String> counter2) {
        final StringBuilder b = new StringBuilder();
        boolean first = true;
        for (final String item : counter2.getKeysetSortedByKey()) {
            if (first) {
                first = false;
            } else {
                b.append(" ");
            }
            b.append(item).append("=").append(counter2.get(item));
        }
        return b.toString();
    }

    public void setScripts(String tag) {
        reorderingToken.clear();
        reorderingToken.add(tag, 1);
    }
}
