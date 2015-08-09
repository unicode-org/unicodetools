package org.unicode.test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
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
import com.ibm.icu.util.ICUException;

public class CheckWholeScript {
    static final Normalizer NFD = Default.nfd();
    static final Map<Script_Values, Map<Script_Values, CodepointToConfusables>> SCRIPT_SCRIPT_UNICODESET2 
    = TestSecurity.CONFUSABLES.getScriptToScriptToCodepointToUnicodeSet(); 
    static final CodepointToConfusables COMMON_COMMON_UNICODESET2 
    = SCRIPT_SCRIPT_UNICODESET2.get(Script_Values.Common).get(Script_Values.Common); 

    public enum Status {
        SAME, COMMON, OTHER
    }
    
    private final UnicodeSet includeOnly;
    
    /**
     * @return the includeOnly
     */
    public UnicodeSet getIncludeOnly() {
        return includeOnly;
    }

    /**
     * @param includeOnly the includeOnly to set
     */
    public CheckWholeScript(UnicodeSet includeOnly) {
        this.includeOnly = includeOnly == null ? null : includeOnly.freeze();
    }

    private ScriptDetector scriptDetector;

    public Set<Status> getConfusables(String source, EnumMap<Script_Values, String> examples) {
        String nfd = NFD.normalize(source);
        if (examples != null) {
            examples.clear();
        }
        scriptDetector = new ScriptDetector();
        ScriptDetector sd = scriptDetector.set(nfd);
        Set<Script_Values> set = sd.getSingleSetOrNull();
        if (set == null) { // not a valid single set
            return Collections.<Status>emptySet();
        }
        Set<Status> result = EnumSet.noneOf(Status.class);
        for (Script_Values sourceScript : set) {
            hasRestricted(nfd, sourceScript, examples, result);
        }
        return result;
    }

//    private static Set<Script_Values> getSingletonSet(ScriptDetector sd) {
//        Set<Script_Values> set = sd.singleScripts.isEmpty() ? sd.combinations.iterator().next() : sd.singleScripts;
//        return set;
//    }

    private void hasRestricted(String nfd, Script_Values sourceScript, EnumMap<Script_Values, String> examples, Set<Status> result) {
        Map<Script_Values, CodepointToConfusables> submap = SCRIPT_SCRIPT_UNICODESET2.get(sourceScript);
        if (submap == null) {
            return;
        }
        StringBuilder example = new StringBuilder();

        boolean hasCommon = false;
        CodepointToConfusables cpToCommonSet = submap.get(Script_Values.Common);
        if (hasRestricted(Script_Values.Common, cpToCommonSet, nfd, example, cpToCommonSet)) {
            result.add(Status.COMMON);
            if (examples == null) {
                return;
            } else {
                examples.put(Script_Values.Common, example.toString());
            }
            hasCommon = true;
        }
        CodepointToConfusables cpToSourceSet = submap.get(sourceScript);
        if (hasRestricted(sourceScript, cpToSourceSet, nfd, example, cpToCommonSet)) {
            result.add(Status.SAME);
            if (examples == null) {
                return;
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
                if (hasRestricted(targetScript, cpToIdentifierSet, nfd, example, cpToCommonSet)) {
                    result.add(Status.OTHER);
                    if (examples == null) {
                        return;
                    } else {
                        examples.put(targetScript, example.toString());
                    }
                }
            }
        }
    }

    private boolean hasRestricted(final Script_Values targetScript, CodepointToConfusables cpToIdentifierSet, String nfd, 
            StringBuilder example, CodepointToConfusables cpToCommonSet) {
        example.setLength(0);
        for (int cp : CharSequences.codePoints(nfd)) { // for all chars in the givenSet
            if (ScriptDetector.getScriptExtensions(cp).equals(ScriptDetector.COMMON_SET) && (includeOnly == null || includeOnly.contains(cp))) {
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
        //   Record the number of characters that are the same, using diff. 
        //   If == number of codepoints, we fail.
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
        UnicodeSet intersect = a == null ? b : new UnicodeSet(a).retainAll(b);
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
        if (a == null || a.containsSome(b)) {
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
