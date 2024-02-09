package org.unicode.jsp;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.unicode.props.UnicodeProperty;

/**
 * Class for testing whether strings have allowed combinations of multiple scripts.
 *
 * @author markdavis
 */
public class ScriptTester {
    static Logger logger = Logger.getLogger(ScriptTester.class.getName());
    private final UnicodeMap<BitSet> character_compatibleScripts;

    public enum CompatibilityLevel {
        Highly_Restrictive,
        Moderately_Restrictive
    }

    public enum ScriptSpecials {
        on,
        off
    }

    /** Space reserved for script codes not in ICU */
    public static final int EXTRA_COUNT = 16; // should be enough, hard working as UTC is!

    public static final Map<String, Integer> extraScripts = new ConcurrentHashMap<>(EXTRA_COUNT);
    /** Extended scripts; note that they do not have stable numbers, and should not be persisted. */
    public static final int
            // HANT = UScript.CODE_LIMIT,
            // HANS = HANT + 1,
            LIMIT = UScript.CODE_LIMIT + EXTRA_COUNT; // HANS + 1;

    private static String[][] EXTENDED_NAME = {
        // Scripts without stable numbers
        {"Hant", "Han Traditional"}, {"Hans", "Han Simplified"},
    };

    static AtomicInteger scriptCounter = new AtomicInteger(UScript.CODE_LIMIT);

    static int getScriptCode(String script) {
        try {
            // If ICU has it, great
            return UCharacter.getPropertyValueEnum(UProperty.SCRIPT, script);
        } catch (com.ibm.icu.impl.IllegalIcuArgumentException iiae) {
            // Make something up
            int newCode =
                    extraScripts.computeIfAbsent(
                            script,
                            script2 -> {
                                int i = scriptCounter.getAndIncrement();
                                logger.warning(
                                        "Synthesized scriptCode "
                                                + i
                                                + " for unrecognized script extension '"
                                                + script
                                                + "'");
                                return i;
                            });
            // Verify we didn't run over
            if (newCode >= LIMIT) {
                throw new RuntimeException(
                        "computed script code of "
                                + newCode
                                + " for '"
                                + script
                                + "' overflows: have "
                                + extraScripts.size()
                                + " scripts but EXTRA_COUNT="
                                + EXTRA_COUNT);
            }
            return newCode;
        }
    }

    public static String getScriptName(int extendedScriptCode, int choice) {
        if (extendedScriptCode >= UScript.CODE_LIMIT) {
            if (extendedScriptCode >= LIMIT) {
                return EXTENDED_NAME[extendedScriptCode - LIMIT][choice];
            } else {
                for (Map.Entry<String, Integer> e : extraScripts.entrySet()) {
                    if (e.getValue() == extendedScriptCode) {
                        if (choice == 0) {
                            return e.getKey();
                        } else {
                            return "New Script '" + e.getKey() + "'";
                        }
                    }
                }
                throw new IllegalArgumentException(
                        "Unknown extended script code " + extendedScriptCode);
            }
        }
        return UCharacter.getPropertyValueName(UProperty.SCRIPT, extendedScriptCode, choice);
    }

    private static final BitSet ALL =
            new BitSet(LIMIT); // be careful when using this; can't freeze it!

    static {
        ALL.set(0, LIMIT, true);
    }

    /**
     * Build a ScriptTester
     *
     * @return
     */
    public static Builder start(CompatibilityLevel level, ScriptSpecials specials) {
        return new Builder(level, specials);
    }

    public static Builder start() {
        return new Builder(CompatibilityLevel.Highly_Restrictive, ScriptSpecials.on);
    }

    public static Builder start(CompatibilityLevel level) {
        return new Builder(level, ScriptSpecials.on);
    }

    /**
     * If the scripts in the string are compatible, then returns a list of them. Otherwise returns
     * an empty bitset. The input must be in NFD.
     *
     * @param input
     * @return bitset of scripts found
     */
    public boolean isOk(CharSequence input) {
        input = Normalizer.normalize(input.toString(), Normalizer.NFD);
        // We make one pass forward and one backward, finding if each characters scripts
        // are compatible with the ones before and after
        // We save the value that we collect on the first pass.
        int cp;
        int maxSize = input.length();
        int base = -1;
        BitSet[] actual = new BitSet[maxSize];
        BitSet[] compat = new BitSet[maxSize];
        int codePointCount = 0;
        BitSet compatBefore = new BitSet(LIMIT);
        compatBefore.or(ALL);
        int lastCp = -1;
        for (int i = 0; i < maxSize; i += Character.charCount(cp)) {
            cp = Character.codePointAt(input, i);
            // check for mixed numbers
            int type = UCharacter.getType(cp);
            if (type == UCharacter.DECIMAL_DIGIT_NUMBER) {
                int newBase = cp & 0xFFFFF0;
                if (base < 0) {
                    base = newBase;
                } else if (base != newBase) {
                    return false;
                }
            }
            // check for multiple combining marks
            if (type == UCharacter.NON_SPACING_MARK || type == UCharacter.ENCLOSING_MARK) {
                if (lastCp == cp) {
                    return false;
                }
            }
            // check scripts
            compat[codePointCount] = character_compatibleScripts.get(cp);
            actual[codePointCount] = getActualScripts(cp);
            if (!actual[codePointCount].intersects(compatBefore)) {
                return false;
            }
            compatBefore.and(compat[codePointCount]);
            codePointCount++;
            lastCp = cp;
        }
        compatBefore.or(ALL);
        for (int i = codePointCount - 1; i >= 0; --i) {
            if (!actual[i].intersects(compatBefore)) {
                return false;
            }
            compatBefore.and(compat[i]);
        }
        // check numbers
        return true;
    }

    // TODO, cache results
    private BitSet getActualScripts(int cp) {
        BitSet actualScripts = getScriptSpecials().get(cp);
        if (actualScripts == null) {
            actualScripts = new BitSet(LIMIT);
            int script = UCharacter.getIntPropertyValue(cp, UProperty.SCRIPT);
            actualScripts.set(script);
        }
        return actualScripts;
    }

    public boolean filterTable(List<Set<String>> table) {

        // We make one pass forward and one backward, finding if each characters scripts
        // are compatible with the ones before.
        // We then make a second pass for the ones after.
        // Could be optimized if needed
        int maxSize = table.size();
        BitSet compatBefore = new BitSet(LIMIT);
        compatBefore.or(ALL);
        BitSet anyCompatAt = new BitSet(LIMIT);

        HashSet<String> toRemove = new HashSet<String>();
        for (int i = 0; i < maxSize; ++i) {
            toRemove.clear();
            anyCompatAt.clear();
            Set<String> column = table.get(i);
            for (String item : column) {
                BitSet compatibleScripts = getCompatibleScripts(item); // ANDed
                anyCompatAt.or(compatibleScripts);
                BitSet actualScripts = getActualScripts(item); // ORed
                if (!actualScripts.intersects(compatBefore)) {
                    toRemove.add(item);
                }
            }
            column.removeAll(toRemove);
            if (column.size() == 0) {
                return false;
            }
            compatBefore.and(anyCompatAt);
        }
        // now reverse order
        compatBefore.or(ALL);
        for (int i = maxSize - 1; i >= 0; --i) {
            toRemove.clear();
            anyCompatAt.clear();
            Set<String> column = table.get(i);
            for (String item : column) {
                BitSet compatibleScripts = getCompatibleScripts(item); // ANDed
                anyCompatAt.or(compatibleScripts);
                BitSet actualScripts = getActualScripts(item); // ORed
                if (!actualScripts.intersects(compatBefore)) {
                    toRemove.add(item);
                }
            }
            column.removeAll(toRemove);
            if (column.size() == 0) {
                return false;
            }
            compatBefore.and(anyCompatAt);
        }
        return true;
    }

    private BitSet getActualScripts(String item) {
        BitSet toOrWith = new BitSet(LIMIT);
        int cp;
        for (int i = 0; i < item.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(item, i);
            toOrWith.or(getActualScripts(cp));
        }
        return toOrWith;
    }

    private BitSet getCompatibleScripts(String item) {
        BitSet toAndWith = new BitSet(LIMIT);
        toAndWith.or(ALL);
        int cp;
        for (int i = 0; i < item.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(item, i);
            toAndWith.and(character_compatibleScripts.get(cp));
        }
        return toAndWith;
    }

    /**
     * Each character in item has a compatible set that intersects overall.
     *
     * @param item
     * @param overallCompatible
     * @return
     */
    private boolean isCompatible(String input, BitSet overallCompatible) {
        int cp;
        for (int i = 0; i < input.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(input, i);
            BitSet scripts = character_compatibleScripts.get(cp); // will never fail
            if (!scripts.intersects(overallCompatible)) {
                return false;
            }
        }
        return true;
    }

    // Ugly hack, because BitSet doesn't have the method.
    private boolean contains(BitSet set1, BitSet set2) {
        // quick check to verify intersecting
        if (!set1.intersects(set2)) {
            return false;
        }
        BitSet temp = new BitSet();
        temp.or(set2);
        temp.and(set1);
        // we now have the intersection. It must be equal to set2
        return temp.equals(set2);
    }

    public static class ScriptExtensions {

        public static final Comparator<BitSet> COMPARATOR =
                new Comparator<BitSet>() {

                    @Override
                    public int compare(BitSet o1, BitSet o2) {
                        int diff = o1.cardinality() - o2.cardinality();
                        if (diff != 0) return diff;
                        if (o1.equals(o2)) return 0;
                        String n1 = getNames(o1, UProperty.NameChoice.LONG, " ");
                        String n2 = getNames(o2, UProperty.NameChoice.LONG, " ");
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

        private static class MyHandler extends FileUtilities.SemiFileReader {
            public static final Pattern SPACES = Pattern.compile("\\s+");

            UnicodeMap<BitSet> map = new UnicodeMap<BitSet>();

            @Override
            public boolean handleLine(int start, int end, String[] items) {
                BitSet bitSet = new BitSet(LIMIT);
                for (String script : SPACES.split(items[1])) {
                    int scriptCode = getScriptCode(script);
                    bitSet.set(scriptCode);
                }
                map.putAll(start, end, bitSet);
                return true;
            }
        }

        public static ScriptExtensions make(String directory, String filename) {
            ScriptExtensions result = new ScriptExtensions();
            result.scriptSpecials =
                    ((MyHandler) new MyHandler().process(directory, filename)).map.freeze();
            return result;
        }

        public static ScriptExtensions make(Class aClass, String filename) {
            ScriptExtensions result = new ScriptExtensions();
            result.scriptSpecials =
                    ((MyHandler) new MyHandler().process(aClass, filename)).map.freeze();
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

        public static String getNames(
                BitSet value, int choice, String separator, Set<String> names) {
            names.clear();
            for (int i = value.nextSetBit(0); i >= 0; i = value.nextSetBit(i + 1)) {
                names.add(ScriptTester.getScriptName(i, choice));
            }
            return CollectionUtilities.join(names, separator).toString();
        }
    }

    static final class ScriptExtensionsHelper {
        ScriptExtensions scriptSpecials;

        ScriptExtensionsHelper() {
            scriptSpecials = ScriptExtensions.make(ScriptExtensions.class, "ScriptExtensions.txt");
        }

        static ScriptExtensionsHelper INSTANCE = new ScriptExtensionsHelper();
    }

    static final ScriptExtensions getScriptSpecials() {
        return ScriptExtensionsHelper.INSTANCE.scriptSpecials;
    }

    public static BitSet getScriptSpecials(int codepoint) {
        BitSet output = new BitSet(LIMIT);
        BitSet actualScripts = getScriptSpecials().get(codepoint);
        if (actualScripts != null) {
            output.or(actualScripts);
        } else {
            int script = UCharacter.getIntPropertyValue(codepoint, UProperty.SCRIPT);
            output.set(script);
        }
        return output;
    }

    public static UnicodeMap<String> getScriptSpecialsNames() {
        UnicodeMap<String> result = new UnicodeMap<String>();
        Set<String> names = new TreeSet<String>(); // to alphabetize

        for (BitSet value : getScriptSpecials().getAvailableValues()) {
            result.putAll(
                    getScriptSpecials().getSet(value),
                    ScriptExtensions.getNames(value, UProperty.NameChoice.LONG, ",", names));
        }
        return result;
    }

    public static String[][] getScriptSpecialsAlternates(UnicodeProperty scriptProp) {
        Collection<BitSet> availableValues = getScriptSpecials().getAvailableValues();
        List<String[]> result = new ArrayList<>();
        Set<String> names = new TreeSet<String>(); // to alphabetize

        for (BitSet value : availableValues) {
            String baseName =
                    ScriptExtensions.getNames(value, UProperty.NameChoice.LONG, ",", names);
            String altName =
                    ScriptExtensions.getNames(value, UProperty.NameChoice.SHORT, ",", names);
            String[] row = {baseName, altName};
            result.add(row);
        }

        // Get the single values, and build alternate values for the property, for isValidValue
        // of a single script (eg Arab)
        List<String> values = scriptProp.getAvailableValues();
        for (String value : values) {
            List<String> row = new ArrayList<>();
            row.add(value);
            for (String alias : scriptProp.getValueAliases(value)) {
                if (!alias.equals(value)) {
                    row.add(alias);
                }
            }
            // duplicate it whenever singular, because the tooling expects at least 2 values (ugg)
            if (row.size() == 1) {
                row.add(value);
            }
            result.add(row.toArray(new String[row.size()]));
        }

        return result.toArray(new String[result.size()][]);
    }

    private ScriptTester(UnicodeMap<BitSet> character_scripts) {
        this.character_compatibleScripts = character_scripts;
    }

    public static class Builder {

        private final Map<Integer, BitSet> compatible = new TreeMap<Integer, BitSet>();
        private final UnicodeMap<BitSet> char2scripts = new UnicodeMap<BitSet>();

        private Builder(CompatibilityLevel level, ScriptSpecials specials) {
            // make everything compatible with itself
            for (int i = 0; i < LIMIT; ++i) {
                BitSet itself = new BitSet(LIMIT);
                itself.set(i);
                compatible.put(i, itself);
            }
            // first do levels
            switch (level) {
                case Moderately_Restrictive:
                    for (int i = 0; i < LIMIT; ++i) {
                        if (i == UScript.CYRILLIC || i == UScript.GREEK || i == UScript.CHEROKEE) {
                            continue;
                        }
                        addCompatible(UScript.LATIN, i);
                    }
                    // FALL THRU!
                case Highly_Restrictive:
                    addCompatible(UScript.LATIN, UScript.HAN, UScript.HIRAGANA, UScript.KATAKANA);
                    // addCompatible(UScript.LATIN, HANT, UScript.HIRAGANA, UScript.KATAKANA);
                    // addCompatible(UScript.LATIN, HANS, UScript.HIRAGANA, UScript.KATAKANA);

                    addCompatible(UScript.LATIN, UScript.HAN, UScript.HANGUL);
                    // addCompatible(UScript.LATIN, HANT, UScript.HANGUL);
                    // addCompatible(UScript.LATIN, HANS, UScript.HANGUL);

                    addCompatible(UScript.LATIN, UScript.HAN, UScript.BOPOMOFO);
                    addCompatible(UScript.LATIN, UScript.HAN);
                    // ?? Asomtavruli, Nuskhuri, and Mkhedruli (georgian)
                    // FALL THRU!
                default:
                    // addCompatible(UScript.HAN, HANT);
                    // addCompatible(UScript.HAN, HANS);
                    // Common and Inherited are compatible with everything!
                    for (int i = 0; i < LIMIT; ++i) {
                        addCompatible(UScript.COMMON, i);
                        addCompatible(UScript.INHERITED, i);
                    }
            }
            // then specials
            // fix the char2scripts mapping

            if (specials == ScriptSpecials.on) {
                getScriptSpecials().putAllInto(char2scripts);
            }
        }

        public ScriptTester get() {
            UnicodeMap<BitSet> character_scripts = new UnicodeMap<BitSet>();
            // first set all the simple cases: character => script => scripts
            for (int script = 0; script < UScript.CODE_LIMIT; ++script) {
                UnicodeSet uset = new UnicodeSet();
                uset.applyIntPropertyValue(UProperty.SCRIPT, script);
                if (uset.size() != 0) {
                    BitSet scripts = compatible.get(script);
                    character_scripts.putAll(uset, scripts);
                }
            }
            // now override these (as necessary) with the charScriptMapping
            for (BitSet scripts : char2scripts.values()) {
                // The scripts need fluffing up according to the acceptableTogether sets
                // We have to create new Bitsets!
                BitSet fluffed = new BitSet(LIMIT);
                fluffed.or(scripts);
                for (int unfluffedScript = scripts.nextSetBit(0);
                        unfluffedScript >= 0;
                        unfluffedScript = scripts.nextSetBit(unfluffedScript + 1)) {
                    BitSet acceptable = compatible.get(unfluffedScript);
                    fluffed.or(acceptable);
                }
                UnicodeSet uset = char2scripts.getSet(scripts);
                character_scripts.putAll(uset, fluffed);
            }
            return new ScriptTester(character_scripts);
        }
        /**
         * Add list of scripts that are acceptable in combination together.
         *
         * <p>Example: st.addAcceptable(UScript.LATIN, USCRIPT.HANGUL);
         *
         * @param scripts
         */
        public Builder addCompatible(int... scripts) {
            // set all the scripts on each of the other scripts
            for (int script : scripts) {
                BitSet items = compatible.get(script);
                for (int script2 : scripts) {
                    items.set(script2);
                }
            }
            return this;
        }

        /**
         * Add mapping from code point to scripts
         *
         * <p>Example: st.addMapping(0x, USCRIPT.HIRAGANA, USCRIPT.KATAKANA); // U+30FC
         * KATAKANA-HIRAGANA PROLONGED SOUND MARK
         */
        public Builder addMapping(int codePoint, int... scripts) {
            BitSet newScripts = new BitSet(LIMIT);
            BitSet oldScripts = char2scripts.get(codePoint);
            if (oldScripts != null) {
                newScripts.or(oldScripts);
            }
            for (int script : scripts) {
                newScripts.set(script);
            }
            char2scripts.put(codePoint, newScripts);
            return this;
        }
    }
}
