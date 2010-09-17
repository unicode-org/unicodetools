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
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;

class ScriptSet {

    Counter<String> scripts = new Counter<String>();
    Counter<String> types = new Counter<String>();
    private Set<Long> primaryCount = new TreeSet<Long>();
    private UnicodeSet chars = new UnicodeSet();

    public ScriptSet() {
    }

    public ScriptSet(ScriptSet scriptSet) {
        scripts.addAll(scriptSet.scripts);
        types.addAll(scriptSet.types);
    }

    public boolean intersects(ScriptSet set) {
        return !Collections.disjoint(scripts.keySet(), set.scripts.keySet());
    }

    public void or(ScriptSet set) {
        scripts.addAll(set.scripts);
        types.addAll(set.types);
    }

    public int cardinality() {
        return scripts.size()
        + types.size();
    }

    void addScriptsIn(long primary, String source) {
        primaryCount.add(primary);
        chars.add(source);
        int cp = Default.nfkd().normalize(source).codePointAt(0);
        //for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        //cp = source.codePointAt(i);
        byte cat = FractionalUCA.getFixedCategory(cp);
        int script = FractionalUCA.getFixedScript(cp);

        if (!(script == WriteCollationData.ucd.Unknown_Script || script == WriteCollationData.ucd.COMMON_SCRIPT)
                && (cat == WriteCollationData.ucd.OTHER_LETTER || cat == WriteCollationData.ucd.UPPERCASE_LETTER || cat == WriteCollationData.ucd.LOWERCASE_LETTER || cat == WriteCollationData.ucd.TITLECASE_LETTER)) {
            scripts.add(WriteCollationData.ucd.getScriptID_fromIndex((byte)script, UCD_Types.SHORT), 1);
        } else {
            types.add(WriteCollationData.ucd.getCategoryID_fromIndex(cat, UCD_Types.SHORT), 1);
        }
        //}
    }

    static String getTypesCombined(String chr) {
        String typeKD = ScriptSet.getTypes(Default.nfkd().normalize(chr));

        String type = ScriptSet.getTypes(chr);
        if (!type.equals(typeKD)) {
            typeKD = typeKD + "/" + type;
        }
        return typeKD;
    }


    public static String getTypes(String source) {
        //StringBuilder result = new StringBuilder();
        String result;
        int cp = source.codePointAt(0);
        //for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        //cp = source.codePointAt(i);
        byte cat = FractionalUCA.getFixedCategory(cp);
        int script = FractionalUCA.getFixedScript(cp);
        //        if (result.length() != 0) {
        //          result.append(' ');
        //        }
        if (!(script == WriteCollationData.ucd.Unknown_Script || script == WriteCollationData.ucd.COMMON_SCRIPT)
                && (cat == WriteCollationData.ucd.OTHER_LETTER || cat == WriteCollationData.ucd.UPPERCASE_LETTER || cat == WriteCollationData.ucd.LOWERCASE_LETTER || cat == WriteCollationData.ucd.TITLECASE_LETTER)) {
            result = (WriteCollationData.ucd.getScriptID_fromIndex((byte)script, UCD_Types.SHORT));
        } else {
            result = (WriteCollationData.ucd.getCategoryID_fromIndex(cat, UCD_Types.SHORT));
        }
        // }
        return result;
    }

    public String toString() {
        throw new UnsupportedOperationException();
    }

    static Set<String> common = new TreeSet();
    static {
        common.add(WriteCollationData.ucd.getScriptID_fromIndex(UCD_Types.COMMON_SCRIPT, UCD_Types.SHORT));
        common.add(WriteCollationData.ucd.getScriptID_fromIndex(UCD_Types.Unknown_Script, UCD_Types.SHORT));
        common.add(WriteCollationData.ucd.getScriptID_fromIndex(UCD_Types.INHERITED_SCRIPT, UCD_Types.SHORT));
    }

    <T extends Appendable> T  toString(T result, boolean categoriesAlso) {
        try {
            if (scripts.size() == 0 && types.size() == 0) {
                return result;
            }
            if (categoriesAlso) {
                result.append("[").append(primaryCount.size() + "").append("]\t");
            }
            boolean first = true;
            if (!categoriesAlso) {
                String scriptNames = scripts.size() != 0 ? CollectionUtilities.join(scripts.keySet(), " ") : "Zyyy";
                result.append(scriptNames);
            } else {
                String scriptNames = scripts.size() != 0 ? joinCounter(scripts) : "";
                result.append(scriptNames);
                if (types.size() != 0) {
                    String catNames = joinCounter(types);
                    if (scripts.size() != 0) {
                        result.append(' ');
                    }
                    result.append(catNames);
                }
                //result.append("\t").append(chars.toPattern(false));
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String joinCounter(Counter<String> counter2) {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (String item : counter2.getKeysetSortedByKey()) {
            if (first)
                first = false;
            else
                b.append(" ");
            b.append(item).append("=").append(counter2.get(item));
        }
        return b.toString();
    }
}