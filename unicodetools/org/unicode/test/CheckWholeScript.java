package org.unicode.test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.tools.Confusables;
import org.unicode.tools.Confusables.CodepointToConfusables;
import org.unicode.tools.Confusables.ScriptDetector;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class CheckWholeScript {
    static final Normalizer NFD = Default.nfd();
    static final UnicodeSet COMMON_OR_INHERITED
    = new UnicodeSet(Confusables.CODEPOINT_TO_SCRIPTS.getSet(Collections.singleton(Script_Values.Common)))
    .addAll(Confusables.CODEPOINT_TO_SCRIPTS.getSet(Collections.singleton(Script_Values.Inherited)))
    .freeze();
    static final Map<Script_Values, Map<Script_Values, UnicodeSet>> SCRIPT_SCRIPT_UNICODESET = TestSecurity.CONFUSABLES.getScriptToScriptToUnicodeSet(); 
    static final Map<Script_Values, Map<Script_Values, CodepointToConfusables>> SCRIPT_SCRIPT_UNICODESET2 = TestSecurity.CONFUSABLES.getScriptToScriptToCodepointToUnicodeSet(); 
    static final CodepointToConfusables COMMON_COMMON_UNICODESET2 = SCRIPT_SCRIPT_UNICODESET2.get(Script_Values.Common).get(Script_Values.Common); 

    public enum Status {NONE, OTHER, COMMON, SAME}

    public static Status hasWholeScriptConfusable(String source, UnicodeSet includeOnly, EnumMap<Script_Values, String> examples) {
        String nfd = NFD.normalize(source);
        //        UnicodeSet givenSet = new UnicodeSet().addAll(nfd);
        //        givenSet.removeAll(COMMON_OR_INHERITED);
        if (examples != null) {
            examples.clear();
        }
        ScriptDetector sd = new Confusables.ScriptDetector().set(nfd);
        Status result = Status.NONE;
        if (sd.size() > 1) { // the original is not a single script
            return result;
        }
        Set<Script_Values> set = sd.singleScripts.isEmpty() ? sd.combinations.iterator().next() : sd.singleScripts;
        for (Script_Values sourceScript : set) {
            Status temp;
            if (includeOnly == null) {
                temp = hasSimple(nfd, sourceScript);
            } else {
                temp = hasRestricted(nfd, sourceScript, includeOnly, examples);
            }
            if (temp.compareTo(result) > 0) {
                result = temp;
            }
        }
        return result;
    }

    private static Status hasSimple(String nfd, Script_Values sourceScript) {
        UnicodeSet givenSet = new UnicodeSet().addAll(nfd);
        Map<Script_Values, UnicodeSet> submap = SCRIPT_SCRIPT_UNICODESET.get(sourceScript);
        if (submap == null) {
            return Status.NONE; // fix later
        }
        for (Entry<Script_Values, UnicodeSet> entry : submap.entrySet()) {
            if (entry.getValue().containsAll(givenSet)) {
                return Status.OTHER; // fix later
            }
        }
        return Status.NONE;
    }

    private static Status hasRestricted(String nfd, Script_Values sourceScript, UnicodeSet includeOnly, EnumMap<Script_Values, String> examples) {
        Map<Script_Values, CodepointToConfusables> submap = SCRIPT_SCRIPT_UNICODESET2.get(sourceScript);
        if (submap == null) {
            return Status.NONE;
        }
        StringBuilder example = new StringBuilder();
        Status result = Status.NONE;

        boolean hasCommon = false;
        CodepointToConfusables cpToCommonSet = submap.get(Script_Values.Common);
        if (hasRestricted(Script_Values.Common, cpToCommonSet, nfd, includeOnly, example, cpToCommonSet)) {
            result = Status.COMMON;
            if (examples == null) {
                return result;
            } else {
                examples.put(Script_Values.Common, example.toString());
            }
            hasCommon = true;
        }
        CodepointToConfusables cpToSourceSet = submap.get(sourceScript);
        if (hasRestricted(sourceScript, cpToSourceSet, nfd, includeOnly, example, cpToCommonSet)) {
            result = Status.SAME;
            if (examples == null) {
                return result;
            } else {
                examples.put(sourceScript, example.toString());
            }
        }
        if (!hasCommon) {
            for (Entry<Script_Values, CodepointToConfusables> entry : submap.entrySet()) {
                final Script_Values targetScript = entry.getKey();
                if (targetScript == Script_Values.Common || targetScript == sourceScript) { // we did these first
                    continue;
                }
                CodepointToConfusables cpToIdentifierSet = entry.getValue();
                if (hasRestricted(targetScript, cpToIdentifierSet, nfd, includeOnly, example, cpToCommonSet)) {
                    result = Status.OTHER;
                    if (examples == null) {
                        return result;
                    } else {
                        examples.put(targetScript, example.toString());
                    }
                }
            }
        }
        return result;
    }

    private static boolean hasRestricted(final Script_Values targetScript, CodepointToConfusables cpToIdentifierSet, String nfd, UnicodeSet includeOnly, 
            StringBuilder example, CodepointToConfusables cpToCommonSet) {
        example.setLength(0);
        for (int cp : CharSequences.codePoints(nfd)) { // for all chars in the givenSet
            if (COMMON_OR_INHERITED.contains(cp) && includeOnly.contains(cp)) {
                // see if there is an alternate
                boolean diff = appendDifferentIfPossible(cp, example);
                continue;
            }
            UnicodeSet idSet = cpToIdentifierSet.get(cp);
            if (idSet == null || !containsSomeFlattened(includeOnly, idSet)) { // either no idSet or doesn't include
                if (targetScript != Script_Values.Common) {
                    return false;
                }
                idSet = cpToCommonSet.get(cp); // try Common
                if (idSet == null || !containsSomeFlattened(includeOnly, idSet)) { // either no idSet or doesn't include
                    return false;
                }
            }
            final String sample = getSampleFlattened(includeOnly, idSet);
            example.append(sample);
        }
        // we try to get a different string if at all possible. If there is no other, then we fail.
        // Could optimize to avoid the saving the characters in example:
        //   Record the number of characters that are the same, using diff. If == number of codepoints, we fail.
        if (CharSequences.equalsChars(example, nfd)) {
            return false;
        }
        return true;
    }

    /** Append, and return true if different 
     * @return **/
    private static boolean appendDifferentIfPossible(int cp, StringBuilder example) {
        UnicodeSet alts = COMMON_COMMON_UNICODESET2.get(cp);
        if (alts != null && alts.size() > 1) {
            example.append(alts.iterator().next());
            return true;
        } else {
            example.appendCodePoint(cp);
            return false;
        }
    }

    /** The containsSomeFlattened is true, and get a sample 
     * @param avoidIfPossible **/
    private static String getSampleFlattened(UnicodeSet a, UnicodeSet b) {
        UnicodeSet intersect = new UnicodeSet(a).retainAll(b);
        if (!intersect.isEmpty()) {
            final Iterator<String> iterator = intersect.iterator();
            return iterator.next();
        }
        for (String item : b.strings()) {
            if (a.containsAll(item)) { // it has to contain the whole string!
                return item;
            }
        }
        throw new ICUException("Internal error.");
    }

    private static boolean containsSomeFlattened(UnicodeSet a, UnicodeSet b) {
        if (a.containsSome(b)) {
            return true;
        }
        // if fails, and b has some strings, try those
        for (String item : b.strings()) {
            if (a.containsAll(item)) { // it has to contain the whole string!
                return true;
            }
        }
        return false;
    }
}
