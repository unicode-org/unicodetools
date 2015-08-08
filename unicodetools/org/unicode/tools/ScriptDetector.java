package org.unicode.tools;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Script_Values;

import com.google.common.base.Joiner;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public final class ScriptDetector {
    public static final Joiner JOINER_COMMA_SPACE = Joiner.on(", ");
    public static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    private static final UnicodeMap<Set<Script_Values>> _CODEPOINT_TO_SCRIPTS = ScriptDetector.IUP.loadEnumSet(UcdProperty.Script_Extensions, UcdPropertyValues.Script_Values.class);
    public static final Set<Script_Values> INHERITED_SET = Collections.singleton(Script_Values.Inherited);
    public static final Set<Script_Values> COMMON_SET = Collections.singleton(Script_Values.Common);
    public static final UnicodeSet COMMON_OR_INHERITED
    = new UnicodeSet(ScriptDetector._CODEPOINT_TO_SCRIPTS.getSet(ScriptDetector.COMMON_SET))
    .addAll(ScriptDetector._CODEPOINT_TO_SCRIPTS.getSet(ScriptDetector.INHERITED_SET))
    .freeze();
    
    
    private boolean isCommon;
    private final EnumSet<Script_Values> singleScripts = EnumSet.noneOf(Script_Values.class);
    private final HashSet<Set<Script_Values>> combinations = new HashSet<Set<Script_Values>>();
    private final HashSet<Set<Script_Values>> toRemove = new HashSet<Set<Script_Values>>();

    /**
     * Return the script of the string, (Common if all characters are Common or Inherited)
     * or null if there is not a unique one
     * Only uses Script property for now.
     * @param source
     * @return
     */
    @SuppressWarnings("deprecation")
    public ScriptDetector set(String source) {
        singleScripts.clear();
        getCombinations().clear();
        isCommon = false;

        boolean haveCommon = false;
        for (int codepoint : CharSequences.codePoints(source)) {
            Set<Script_Values> current = ScriptDetector.getScriptExtensions(codepoint);
            if (current.size() > 1) {
                getCombinations().add(current); // we know there are not Common or Inherited
            } else {
                if (current.equals(ScriptDetector.COMMON_SET)) {
                    haveCommon = true;
                } else {
                    singleScripts.addAll(current);
                }
            }
        }
        // Remove redundant combinations
        if (getCombinations().size() > 0) {
            toRemove.clear();
            // TODO if the combinations are ordered by shortest set first, then we can optimize this
            // but such cases will be rare...
            for (Set<Script_Values> combo : getCombinations()) {
                if (!Collections.disjoint(combo, singleScripts)) {
                    toRemove.add(combo);
                } else {
                    for (Set<Script_Values> combo2 : getCombinations()) {
                        if (combo2 != combo) {
                            if (combo.containsAll(combo2)) {
                                toRemove.add(combo);
                                break; // inner loop
                            }
                        }
                    }
                }
            }
            getCombinations().removeAll(toRemove);
        }
        if (haveCommon && singleScripts.isEmpty() && getCombinations().isEmpty()) {
            singleScripts.add(Script_Values.Common);
            isCommon = true;
        }
        return this;
    }

    public int size() {
        return singleScripts.size() + getCombinations().size();
    }

    public Set<Script_Values> getSingleSetOrNull() {
        return getCombinations().isEmpty() ? singleScripts
                : !singleScripts.isEmpty() ? null
                        : getCombinations().size() > 1 ? null
                                : getCombinations().iterator().next();
    }
    @Override
    public String toString() {
        return singleScripts.isEmpty() ? JOINER_COMMA_SPACE.join(getCombinations())
                : getCombinations().isEmpty() ? JOINER_COMMA_SPACE.join(singleScripts)
                        : JOINER_COMMA_SPACE.join(singleScripts) + ", " + JOINER_COMMA_SPACE.join(getCombinations());
    }

    public Set<Set<Script_Values>> getAll() {
        if (singleScripts.isEmpty()) {
            return getCombinations();
        }
        Set<Set<Script_Values>> result = new LinkedHashSet<>();
        for (Script_Values script : singleScripts) {
            result.add(Collections.singleton(script));
        }
        result.addAll(getCombinations());
        return result;
    }

    public static UnicodeSet getCharactersForScriptExtensions(Set<Script_Values> scriptValueSet) {
        if (scriptValueSet.equals(ScriptDetector.COMMON_SET)) {
            return ScriptDetector.COMMON_OR_INHERITED;
        } else if (scriptValueSet.equals(ScriptDetector.INHERITED_SET)) {
            throw new IllegalArgumentException("Internal Error");
        } else {
            UnicodeSet result = ScriptDetector._CODEPOINT_TO_SCRIPTS.getSet(scriptValueSet);
            return result;
        }
    }

    /**
     * TODO 
     * scx={...Han...} : add Jpan, Kore, eg => {...Han, Jpan, Kore...}
     * scx={...Hiragana...} : add Jpan, eg => {...Hiragana, Jpan...}
     * scx={...Katakana...} : add Jpan, eg => {...Katakana, Jpan...}
     * scx={...Hangul...} : add Kore, eg => {...Han, Kore...}
     * 
     * @param codepoint
     * @return
     */
    public static Set<Script_Values> getScriptExtensions(int codepoint) {
        Set<Script_Values> result = ScriptDetector._CODEPOINT_TO_SCRIPTS.get(codepoint);
        if (result.equals(ScriptDetector.INHERITED_SET)) {
            result = ScriptDetector.COMMON_SET;
        }
        return result;
    }

    /**
     * @return the isCommon
     */
    public boolean isCommon() {
        return isCommon;
    }

    /**
     * @return the singleScripts
     */
    public EnumSet<Script_Values> getSingleScripts() {
        return singleScripts;
    }

    /**
     * @return the combinations
     */
    public HashSet<Set<Script_Values>> getCombinations() {
        return combinations;
    }
}