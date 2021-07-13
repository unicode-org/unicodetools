package org.unicode.text.UCD;

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class ScriptExtensions {

    public static final Comparator<BitSet> COMPARATOR = new Comparator<BitSet>() {

        @Override
        public int compare(BitSet o1, BitSet o2) {
            final int diff = o1.cardinality() - o2.cardinality();
            if (diff != 0) {
                return diff;
            }
            if (o1.equals(o2)) {
                return 0;
            }
            final String n1 = getNames(o1, UCD_Types.LONG, " ");
            final String n2 = getNames(o2, UCD_Types.LONG, " ");
            return n1.compareToIgnoreCase(n2);
        }
    };

    private UnicodeMap<BitSet> scriptSpecials;

    public Collection<BitSet> getAvailableValues() {
        return scriptSpecials.getAvailableValues();
    }

    public UnicodeSet getSet(BitSet value) {
        return scriptSpecials.getSet(value);
    }

    private static class MyHandler extends org.unicode.jsp.FileUtilities.SemiFileReader {
        public final static Pattern SPACES = Pattern.compile("\\s+");

        UnicodeMap<BitSet> map = new UnicodeMap<BitSet>();

        @Override
        public boolean handleLine(int start, int end, String[] items) {
            final BitSet bitSet = new BitSet(UCD_Types.LIMIT_SCRIPT);
            for (final String script : SPACES.split(items[1])) {
                int scriptCode = Utility.lookupShort(script, UCD_Names.SCRIPT, true);
                if (scriptCode < 0) {
                    scriptCode = Utility.lookupShort(script, UCD_Names.LONG_SCRIPT, true);
                }
                bitSet.set(scriptCode);
            }
            map.putAll(start, end, bitSet);
            return true;
        }
    }

    public static String getScriptID_fromIndex(int prop, int length) {
        return prop < 0 || prop >= UCD_Names.SCRIPT.length ? null
                : (length == UCD_Types.EXTRA_ALIAS && prop == UCD_Types.INHERITED_SCRIPT) ? "Qaai"
                        : (length == UCD_Types.SHORT) ? UCD_Names.SCRIPT[prop] : UCD_Names.LONG_SCRIPT[prop];
    }


    public static ScriptExtensions make(String directory, String filename) {
        final ScriptExtensions result = new ScriptExtensions();
        result.scriptSpecials = ((ScriptExtensions.MyHandler) new MyHandler()
        .process(directory, filename)).map.freeze();
        return result;
    }

    public static ScriptExtensions make(Class aClass, String filename) {
        final ScriptExtensions result = new ScriptExtensions();
        result.scriptSpecials = ((ScriptExtensions.MyHandler) new MyHandler()
        .process(aClass, filename)).map.freeze();
        return result;
    }

    public BitSet get(int codepoint) {
        return scriptSpecials.get(codepoint);
    }

    public void putAllInto(UnicodeMap<BitSet> char2scripts) {
        char2scripts.putAll(scriptSpecials);
    }

    public static String getNames(BitSet value, int choice, String separator) {
        return getNames(value, choice, separator, new TreeSet<String>());
    }

    public static String getNames(BitSet value, int choice, String separator, Set<String> names) {
        names.clear();
        for (int i = value.nextSetBit(0); i >= 0; i = value.nextSetBit(i+1)) {
            names.add(getScriptID_fromIndex(i, choice));
        }
        return CollectionUtilities.join(names, separator).toString();
    }
}