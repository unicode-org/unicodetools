package org.unicode.props;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

/**
 * This class analyzes a possible identifier for script and identifier status.
 * Use it by calling setIdentifierProfile then setIdentifier.
 * At this point:
 * <ol>
 * <li>call getScripts for the specific scripts in the identifier. The identifier contains at least one character in
 * each of these.
 * <li>call getAlternates to get cases where a character is not limited to a single script. For example, it could be
 * either Katakana or Hiragana.
 * <li>call getCommonAmongAlternates to find out if any scripts are common to all the alternates.
 * <li>call getNumerics to get a representative character (with value zero) for each of the decimal number systems in
 * the identifier.
 * <li>call getRestrictionLevel to see what the UTS36 restriction level is. (This has some proposed changes from the
 * current one, however.)
 * </ol>
 * 
 * @author markdavis
 * @internal
 */
public class ScriptInfo {

    public enum IdentifierStatus {
        /** Only ASCII characters: U+0000..U+007F **/
        ASCII,
        /**
         * All characters in each identifier must be from a single script, or
         * from the combinations: Latin + Han + Hiragana + Katakana; Latin + Han
         * + Bopomofo; or Latin + Han + Hangul. Note that this level will satisfy
         * the vast majority of Latin-script users; also that TR36 has ASCII instead of Latin.
         **/
        HIGHLY_RESTRICTIVE,
        /**
         * Allow Latin with other scripts except Cyrillic, Greek, Cherokee
         * Otherwise, the same as Highly Restrictive
         **/
        MODERATELY_RESTRICTIVE,
        /**
         * Allow arbitrary mixtures of scripts, such as Ωmega, Teχ, HλLF-LIFE,
         * Toys-Я-Us. Otherwise, the same as Moderately Restrictive
         **/
        MINIMALLY_RESTRICTIVE,
        /**
         * Any valid identifiers, including characters outside of the Identifier
         * Profile, such as I♥NY.org
         **/
        UNRESTRICTIVE
    }

    private static final UnicodeSet ASCII = new UnicodeSet(0, 0x7F).freeze();

    private String identifier;
    private final EnumSet<Script_Values> requiredScripts = EnumSet.noneOf(Script_Values.class);
    private final EnumSet<Script_Values> explicitScripts = EnumSet.noneOf(Script_Values.class);
    private final Set<Set<Script_Values>> scriptSetSet = new HashSet<>();
    private final EnumSet<Script_Values> commonAmongAlternates = EnumSet.noneOf(Script_Values.class);
    private final UnicodeSet numerics = new UnicodeSet();
    private final UnicodeSet identifierProfile = new UnicodeSet(0, 0x10FFFF);
    private final IdentifierVersionInfo ivi;


    private static final EnumSet<Script_Values> ALL_SCRIPTS = EnumSet.allOf(Script_Values.class);

    public static class IdentifierVersionInfo {
        private final UnicodeMap<General_Category_Values> generalCategory;
        private final UnicodeMap<Double> numericValue;
        private final UnicodeMap<Set<Script_Values>> scriptExtensions;
        private final Map<Script_Values, UnicodeSet> scriptExtensionsFlattened;
        //        private final UnicodeMap<Script_Values> script;

        public IdentifierVersionInfo(String version) {
            IndexUnicodeProperties iup = IndexUnicodeProperties.make(version);
            generalCategory = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
            numericValue = iup.loadDouble(UcdProperty.Numeric_Value);
            scriptExtensions = iup.loadSet(UcdProperty.Script_Extensions, Script_Values.class, UcdProperty.Script);
            scriptExtensionsFlattened = IndexUnicodeProperties.freeze(
                    IndexUnicodeProperties.invertSet(scriptExtensions, new EnumMap<Script_Values, UnicodeSet>(Script_Values.class)));
            //            script = iup.loadEnum(UcdProperty.Script);
        }

        public static IdentifierVersionInfo getInstance(String version) {
            return new IdentifierVersionInfo(version);
        }
    }

    public ScriptInfo(String version) {
        ivi = IdentifierVersionInfo.getInstance(version);
    }

    private ScriptInfo clear() {
        requiredScripts.clear();
        explicitScripts.clear();
        scriptSetSet.clear();
        numerics.clear();
        commonAmongAlternates.clear();
        return this;
    }

    public ScriptInfo setIdentifierProfile(UnicodeSet identifierProfile) {
        this.numerics.set(numerics);
        return this;
    }

    public UnicodeSet getIdentifierProfile() {
        return new UnicodeSet(identifierProfile);
    }
    
    public UnicodeSet getUnicodeSetContaining(Script_Values value) {
        return ivi.scriptExtensionsFlattened.get(value);
    }

    public ScriptInfo setIdentifier(String identifier) {
        this.identifier = identifier;
        clear();
        int cp;
        for (int i = 0; i < identifier.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(identifier, i);
            // Store a representative character for each kind of decimal digit
            if (ivi.generalCategory.getValue(cp) == General_Category_Values.Decimal_Number) {
                // Just store the zero character as a representative for comparison. Unicode guarantees it is cp - value
                numerics.add(cp - ivi.numericValue.getValue(cp).intValue());
            }
            Set<Script_Values> scripts = ivi.scriptExtensions.getValue(cp);
            //            temp.remove(Script_Values.Common);
            //            temp.remove(Script_Values.Inherited);
            if (scripts.size() == 1) {
                // Single script, record it.
                requiredScripts.addAll(scripts);
            } else if (Collections.disjoint(requiredScripts, scripts)) {
                scriptSetSet.add(scripts);
            }
        }
        // Now make a final pass through to remove alternates that came before singles.
        // [Kana], [Kana Hira] => [Kana]
        // This is relatively infrequent, so doesn't have to be optimized.
        if (scriptSetSet.size() == 0) {
            commonAmongAlternates.clear();
        } else {
            commonAmongAlternates.addAll(ALL_SCRIPTS);
            for (Iterator<Set<Script_Values>> it = scriptSetSet.iterator(); it.hasNext();) {
                Set<Script_Values> next = it.next();
                if (!Collections.disjoint(requiredScripts, next)) {
                    it.remove();
                } else {
                    // [[Arab Syrc Thaa]; [Arab Syrc]] => [[Arab Syrc]]
                    for (Set<Script_Values> other : scriptSetSet) {
                        if (next != other && next.containsAll(other)) {
                            it.remove();
                            break;
                        }
                    }
                }
                commonAmongAlternates.retainAll(next); // get the intersection.
            }
            //            if (commonAmongAlternates.size() == 0) {
            //                commonAmongAlternates.clear();
            //            }
        }
        explicitScripts.addAll(requiredScripts);
        explicitScripts.remove(Script_Values.Common);
        explicitScripts.remove(Script_Values.Inherited);
        if (explicitScripts.contains(Script_Values.Unknown)) {
            throw new IllegalArgumentException();
        }
        // Note that the above code doesn't minimize alternatives. That is, it does not collapse
        // [[Arab Syrc Thaa]; [Arab Syrc]] to [[Arab Syrc]]
        // That would be a possible optimization, but is probably not worth the extra processing
        return this;
    }

    static final EnumSet<Script_Values> COMMON_AND_INHERITED = EnumSet.of(Script_Values.Common, Script_Values.Inherited);

    public boolean isMultiScript() {
        return scriptSetSet.size() + explicitScripts.size() > 1;
    }

    public int getScriptSetCount() {
        return scriptSetSet.size() + explicitScripts.size();
    }

    public boolean hasMixedNumberSystems(String identifier) {
        return numerics.size() > 1;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Set<Script_Values> getScripts() {
        return requiredScripts;
    }

    public Set<Script_Values> getExplicitScripts() {
        return explicitScripts;
    }

    public Set<Set<Script_Values>> getAlternates() {
        return scriptSetSet;
    }

    public UnicodeSet getNumerics() {
        return new UnicodeSet(numerics);
    }

    public Set<Script_Values> getCommonAmongAlternates() {
        return commonAmongAlternates;
    }

    //    // EnumSet<Script_Values> doesn't support "contains(...)", so we have inverted constants
    //    // They are private; they can't be made immutable in Java.
    //    private final static EnumSet<Script_Values> JAPANESE = EnumSet.of(Script_Values.Latin, Script_Values.Han, Script_Values.Hiragana,
    //            Script_Values.Katakana);
    //    private final static EnumSet<Script_Values> CHINESE = EnumSet.of(Script_Values.Latin, Script_Values.Han, Script_Values.Bopomofo);
    //    private final static EnumSet<Script_Values> KOREAN = EnumSet.of(Script_Values.Latin, Script_Values.Han, Script_Values.Hangul);
    //    private final static EnumSet<Script_Values> CONFUSABLE_WITH_LATIN = EnumSet.of(Script_Values.Cyrillic, Script_Values.Greek,
    //            Script_Values.Cherokee);
    //
    //    public IdentifierStatus getRestrictionLevel() {
    //        if (!identifierProfile.containsAll(identifier) || getNumerics().size() > 1) {
    //            return IdentifierStatus.UNRESTRICTIVE;
    //        }
    //        if (ASCII.containsAll(identifier)) {
    //            return IdentifierStatus.ASCII;
    //        }
    //        EnumSet<Script_Values> temp = EnumSet.noneOf(Script_Values.class);
    //        temp.addAll(requiredScripts);
    //        temp.clear(Script_Values.Common);
    //        temp.clear(Script_Values.Inherited);
    //        // This is a bit tricky. We look at a number of factors.
    //        // The number of scripts in the text.
    //        // Plus 1 if there is some commonality among the alternates (eg [Arab Thaa]; [Arab Syrc])
    //        // Plus number of alternates otherwise (this only works because we only test cardinality up to 2.)
    //        final int cardinalityPlus = temp.size() + (commonAmongAlternates.isEmpty() ? scriptSetSet.size() : 1);
    //        if (cardinalityPlus < 2) {
    //            return IdentifierStatus.HIGHLY_RESTRICTIVE;
    //        }
    //        if (containsWithAlternates(JAPANESE, temp)
    //                || containsWithAlternates(CHINESE, temp)
    //                || containsWithAlternates(KOREAN, temp)) {
    //            return IdentifierStatus.HIGHLY_RESTRICTIVE;
    //        }
    //        if (cardinalityPlus == 2
    //                && temp.get(Script_Values.LATIN)
    //                && !temp.intersects(CONFUSABLE_WITH_LATIN)) {
    //            return IdentifierStatus.MODERATELY_RESTRICTIVE;
    //        }
    //        return IdentifierStatus.MINIMALLY_RESTRICTIVE;
    //    }

    @Override
    public String toString() {
        return identifier + ", " + identifierProfile.toPattern(false)
                //                + ", " + getRestrictionLevel()
                + ", " + requiredScripts
                + ", " + scriptSetSet
                + ", " + numerics;
    }

    public static EnumSet<Script_Values> parseScripts(String scriptsString) {
        EnumSet<Script_Values> result = EnumSet.noneOf(Script_Values.class);
        if (scriptsString.isEmpty()) {
            return result;
        }

        for (String s : scriptsString.split(",\\s*|\\s+")) {
            result.add((Script_Values) UcdProperty.Script.getEnum(s));
        }
        return result;
    }

    public static Set<EnumSet<Script_Values>> parseAlternates(String multiscriptsString) {
        if (multiscriptsString.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set<EnumSet<Script_Values>> result = new LinkedHashSet<>();
        for (String s : multiscriptsString.split("\\s*;\\s+")) {
            result.add(parseScripts(s));
        }
        return result;
    }

    public UnicodeSet getSetWith(String script) {
        return getUnicodeSetContaining((Script_Values) UcdProperty.Script.getEnum(script));
    }

    //    private boolean containsWithAlternates(EnumSet<Script_Values> container, EnumSet<Script_Values> containee) {
    //        if (!contains(container, containee)) {
    //            return false;
    //        }
    //        for (EnumSet<Script_Values> alternatives : scriptSetSet) {
    //            if (!container.intersects(alternatives)) {
    //                return false;
    //            }
    //        }
    //        return true;
    //    }
    //
    //    public static String displayAlternates(Collection<EnumSet<Script_Values>> alternates) {
    //        StringBuilder result = new StringBuilder();
    //        for (EnumSet<Script_Values> item : alternates) {
    //            if (result.length() != 0) {
    //                result.append("; ");
    //            }
    //            result.append(displayScripts(item));
    //        }
    //        return result.toString();
    //    }
    //
    //    public static String displayScripts(EnumSet<Script_Values> scripts) {
    //        StringBuilder result = new StringBuilder("[");
    //        for (int i = scripts.nextSetBit(0); i >= 0; i = scripts.nextSetBit(i + 1)) {
    //            if (result.length() != 1) {
    //                result.append(' ');
    //            }
    //            result.append(Script_Values.getShortName(i));
    //        }
    //        return result.append("]").toString();
    //    }
    //
    //    public static EnumSet<Script_Values> parseScripts(String scriptsString) {
    //        EnumSet<Script_Values> result = EnumSet.noneOf(Script_Values.class);
    //        for (String item : scriptsString.trim().split(",?\\s+")) {
    //            if (!item.isEmpty()) {
    //                result.set(Script_Values.getCodeFromName(item));
    //            }
    //        }
    //        return result;
    //    }
    //
    //    public static Set<EnumSet<Script_Values>> parseAlternates(String scriptsSetString) {
    //        Set<EnumSet<Script_Values>> result = new HashSet<EnumSet<Script_Values>>();
    //        for (String item : scriptsSetString.trim().split("\\s*;\\s*")) {
    //            if (!item.isEmpty()) {
    //                result.add(parseScripts(item));
    //            }
    //        }
    //        return result;
    //    }
    //
}
