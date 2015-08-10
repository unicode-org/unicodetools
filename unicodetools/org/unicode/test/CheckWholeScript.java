package org.unicode.test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.tools.Confusables.CodepointToConfusables;
import org.unicode.tools.ScriptDetector;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

/**
 * A class for checking the whole-script confusable status of a string. Also returns examples.
 * @author markdavis
 */
public class CheckWholeScript {
    private static final Normalizer NFD = Default.nfd();
    private static final UnicodeSet ALL_CODEPOINTS = new UnicodeSet("[:any:]").freeze(); 

    static final Map<Script_Values, Map<Script_Values, CodepointToConfusables>> SCRIPT_SCRIPT_UNICODESET2 
    = TestSecurity.CONFUSABLES.getScriptToScriptToCodepointToUnicodeSet(); 
    
    static final CodepointToConfusables COMMON_COMMON_UNICODESET2 
    = SCRIPT_SCRIPT_UNICODESET2.get(Script_Values.Common).get(Script_Values.Common);
    

    /**
     * The status of a whole-script confusable.
     * @author markdavis
     */
    public enum Status {
        /**
         * There is a same-script confusable, like "google" and "goog1e"
         */
        SAME,
        /**
         * There is a common-only confusable, like "l" and "1"
         */
        COMMON, 
        /**
         * There is a different script confusable, like "sex" (Latin) and "ѕех" (Cyrillic)
         */
        OTHER
    }

    private final UnicodeSet includeOnly;

    /**
     * Returns the set of characters allowed in identifiers, passed in on creation.
     */
    public UnicodeSet getIncludeOnly() {
        return includeOnly;
    }

    /**
     * Create a checker, with the set of allowed characters. Null means there are no character limits.
     * @param includeOnly the includeOnly to set. Note that it becomes frozen.
     */
    public CheckWholeScript(UnicodeSet includeOnly) {
        this.includeOnly = includeOnly == null ? ALL_CODEPOINTS : includeOnly.freeze();
    }

    private ScriptDetector sourceScriptDetector = new ScriptDetector();
    private ScriptDetector tempScriptDetector = new ScriptDetector();

    /**
     * Gets the whole-script confusable status, filling in the examples (if not null).
     * Example:
     * <ul>
     * <li>source = sex (Latin) => [SAME, COMMON], with examples= {Latin=ƽꬲⅹ, Common=𝐬℮×}</li>
     * <li>source = NO (Latin) => returns [OTHER], with examples= {Coptic=ⲚⲞ, Elbasan=𐔓𐔖, Greek=ΝΟ, Lisu=ꓠꓳ}</li>
     * </ul>
     * @param source
     * @param examples
     * @return
     */
    public Set<Status> getConfusables(String source, EnumMap<Script_Values, String> examples) {
        String nfd = NFD.normalize(source);
        examples.clear();
        ScriptDetector sd = sourceScriptDetector.set(nfd);
        Set<Script_Values> sourceScriptSet = sd.getSingleSetOrNull();
        if (sourceScriptSet == null) { // not a valid single set
            return Collections.<Status>emptySet();
        }
        for (Script_Values sourceScript : sourceScriptSet) {
            hasRestricted(nfd, sourceScript, examples);
        }
        if (examples.isEmpty()) {
            return Collections.<Status>emptySet();
        }
        // provide a summary result
        Set<Status> result = EnumSet.noneOf(Status.class);
        Set<Script_Values> temp = EnumSet.copyOf(examples.keySet());
        if (temp.removeAll(sourceScriptSet)) {
            result.add(Status.SAME);
        }
        if (temp.remove(Script_Values.Common)) {
            result.add(Status.COMMON);
        }
        if (temp.size() > 0) {
            result.add(Status.OTHER);
        }
        return result;
    }

    private void hasRestricted(String nfd, Script_Values sourceScript, EnumMap<Script_Values, String> examples) {
        Map<Script_Values, CodepointToConfusables> submap = SCRIPT_SCRIPT_UNICODESET2.get(sourceScript);
        if (submap == null) {
            return;
        }
        StringBuilder example = new StringBuilder();
        CodepointToConfusables cpToCommonSet = submap.get(Script_Values.Common);
        for (Entry<Script_Values, CodepointToConfusables> entry : submap.entrySet()) {
            final Script_Values targetScript = entry.getKey();
            final CodepointToConfusables cpToIdentifierSet = entry.getValue();
            if (hasRestricted(targetScript, nfd, cpToIdentifierSet, cpToCommonSet, example)) {
                examples.put(targetScript, example.toString());
            }
        }
    }

    /**
     * Return true if there is a match, and set example correctly.
     */
    private boolean hasRestricted(Script_Values targetScript, String nfd, 
            CodepointToConfusables cpToIdentifierSet, CodepointToConfusables cpToCommonSet, 
            StringBuilder example) {
        example.setLength(0);
        for (int cp : CharSequences.codePoints(nfd)) { // for all chars in the givenSet
            UnicodeSet idSet = cpToIdentifierSet.get(cp);
            String sample = getSampleFlattened(cp, includeOnly, idSet);
            if (sample == null) {
                if (targetScript != Script_Values.Common) { // if we failed, try common
                    idSet = cpToCommonSet.get(cp); // try Common
                    sample = getSampleFlattened(cp, includeOnly, idSet);
                }
                if (sample == null) {
                    Set<Script_Values> scripts = ScriptDetector.getScriptExtensions(cp);
                    if (scripts.contains(targetScript) || scripts.contains(Script_Values.Common)) {
                        example.appendCodePoint(cp);
                        continue;
                    }
                    return false;
                }
            }
            example.append(sample);
        }
        // skip if the target cannot be different than the source, 
        if (CharSequences.equalsChars(example, nfd)) {
            return false;
        }
        // or the targetsScript != common and the example is all Common,
        if (targetScript != Script_Values.Common 
                && tempScriptDetector.set(example.toString()).getSingleScripts().contains(Script_Values.Common)) {
            return false;
        }
        return true;
    }

    /** Get a sample from the intersection, avoiding cp if possible, and null if impossible.
     * @param avoidIfPossible 
     * @param avoidIfPossible 
     **/
    private static String getSampleFlattened(int avoidIfPossible, UnicodeSet a, UnicodeSet b) {
        if (b == null) {
            return null;
        }
        UnicodeSet intersect = a == null ? b : new UnicodeSet(a).retainAll(b);
        if (!intersect.isEmpty()) {
            String possible = null;
            for (String s : intersect) {
                if (CharSequences.equals(avoidIfPossible, s)) {
                    possible = s;
                }
                return s;
            }
            return possible;
        }
        for (String item : b.strings()) {
            if (a.containsAll(item)) { // it has to contain the whole string!
                return item;
            }
        }
        return null;
    }
}
