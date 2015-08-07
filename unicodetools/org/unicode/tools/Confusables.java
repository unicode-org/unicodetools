package org.unicode.tools;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames;
import org.unicode.props.PropertyNames.NameMatcher;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Confusables.CodepointToConfusables;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;

public class Confusables {
    public static final Splitter SEMI = Splitter.on(';').trimResults();

    public enum Style {SL, SA, ML, MA}

    public static class Data {
        final Style style;
        final String result;
        public Data(Style style, String result) {
            this.style = style;
            this.result = result;
        }
    }

    /**
     * @return the style2map
     */
    public EnumMap<Style, UnicodeMap<String>> getStyle2map() {
        return style2map;
    }
    /**
     * @return the char2data
     */
    public UnicodeMap<EnumMap<Style, String>> getChar2data() {
        return char2data;
    }
    /**
     * @return the scriptToScriptToUnicodeSet
     */
    public Map<Script_Values, Map<Script_Values, UnicodeSet>> getScriptToScriptToUnicodeSet() {
        return scriptToScriptToUnicodeSet;
    }

    final private EnumMap<Style, UnicodeMap<String>> style2map;
    final private UnicodeSet hasConfusable = new UnicodeSet();
    final private UnicodeMap<EnumMap<Style,String>> char2data = new UnicodeMap<EnumMap<Style,String>>();
    final private Map<Script_Values, Map<Script_Values, UnicodeSet>> scriptToScriptToUnicodeSet;
    final private Map<Script_Values, Map<Script_Values, CodepointToConfusables>> scriptToScriptToCodepointToUnicodeSet;

    public static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    public static final UnicodeMap<Script_Values> CODEPOINT_TO_SCRIPT = IUP.loadEnum(UcdProperty.Script, UcdPropertyValues.Script_Values.class);
    public static final UnicodeMap<Set<Script_Values>> CODEPOINT_TO_SCRIPTS = IUP.loadEnumSet(UcdProperty.Script_Extensions, UcdPropertyValues.Script_Values.class);
    public static final UnicodeMap<String> CODEPOINT_TO_NAME = IUP.load(UcdProperty.Name);

    public Confusables (String directory) {
        try {
            EnumMap<Style, UnicodeMap<String>> _style2map = new EnumMap<Style,UnicodeMap<String>>(Style.class);
            for (String line : FileUtilities.in(directory, "confusables.txt")) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int hashPos = line.indexOf('#');
                line = line.substring(0,hashPos).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s*;\\s*");
                String source = Utility.fromHex(parts[0]);
                String target = Utility.fromHex(parts[1]);
                hasConfusable.add(source);
                hasConfusable.add(target);
                
                Style style = Style.valueOf(parts[2]);
                addConfusable(style, source, target, _style2map);
                
                if (CharSequences.getSingleCodePoint(target) != Integer.MAX_VALUE) {
                    addConfusable(style, target, source, _style2map);
                }
            }
            style2map = CldrUtility.protectCollection(_style2map);
            char2data.freeze();
            hasConfusable.freeze();

            Map<Script_Values, Map<Script_Values, UnicodeSet>> _scriptToScriptToUnicodeSet = new TreeMap<>();
            // 05E1          ; Hebr; Telu; A #      (ס)  HEBREW LETTER SAMEKH

            //            NameMatcher<Script_Values> matcher = PropertyNames.getNameToEnums(Script_Values.class);
            //            for (String line : FileUtilities.in(directory, "confusablesWholeScript.txt")) {
            //                if (line.startsWith("\uFEFF")) {
            //                    line = line.substring(1);
            //                }
            //                line = line.trim();
            //                if (line.isEmpty()) {
            //                    continue;
            //                }
            //                int hashPos = line.indexOf('#');
            //                line = line.substring(0,hashPos).trim();
            //                if (line.isEmpty()) {
            //                    continue;
            //                }
            //                String[] parts = line.split("\\s*;\\s*");
            //                if ("L".equals(parts[3])) {
            //                    continue; // the L case isn't very useful;
            //                }
            //                Script_Values sourceScript = matcher.get(parts[1]);
            //                Script_Values targetScript = matcher.get(parts[2]);
            //                UnicodeSet uset = getUnicodeSetIn(_scriptToScriptToUnicodeSet, sourceScript, targetScript);
            //                String[] sourceRange = parts[0].split("\\.\\.");
            //                int sourceCharStart = Integer.parseInt(sourceRange[0], 16);
            //                int sourceCharEnd = sourceRange.length < 2 ? sourceCharStart : Integer.parseInt(sourceRange[1], 16);
            //                uset.add(sourceCharStart, sourceCharEnd);
            //            }

            // patch, because the file doesn't contain X => common/inherited or the targetSet
            UnicodeMap<String> map = style2map.get(Style.MA);
            Map<Script_Values, Map<Script_Values, CodepointToConfusables>> _scriptToScriptToCodepointToUnicodeSet = new EnumMap<>(Script_Values.class);

            // get the equivalence classes
            for (String representative : map.values()) {
                UnicodeSet equivalents = new UnicodeSet(map.getSet(representative)).add(representative);
                for (String a : equivalents) {
                    for (String b : equivalents) {
                        if (a.compareTo(b) >= 0) {
                            continue; // only consider pair once, and skip a,a
                        }
                        if ("l".equals(a) || "l".equals(b)) {
                            @SuppressWarnings("unused")
                            int debug = 0;
                        }
                        // try adding both directions
                        // but only if the source is a single code point
                        int aSingle = getSingleCodePoint(a);
                        int bSingle = getSingleCodePoint(b);
                        if (aSingle >= 0) {
                            tryAdding(aSingle, b, _scriptToScriptToUnicodeSet, _scriptToScriptToCodepointToUnicodeSet);
                        }
                        if (bSingle >= 0) {
                            tryAdding(bSingle, a, _scriptToScriptToUnicodeSet, _scriptToScriptToCodepointToUnicodeSet);
                        }
                    }
                }
            }
            if (false) for (Entry<Script_Values, Map<Script_Values, CodepointToConfusables>> scriptToCodepointToUnicodeSet : _scriptToScriptToCodepointToUnicodeSet.entrySet()) {
                String sourceScript = scriptToCodepointToUnicodeSet.getKey().getShortName();
                for (Entry<Script_Values, CodepointToConfusables> codepointToUnicodeSet : scriptToCodepointToUnicodeSet.getValue().entrySet()) {
                    String targetScript = codepointToUnicodeSet.getKey().getShortName();
                    for (Entry<Integer, UnicodeSet> value : codepointToUnicodeSet.getValue()) {
                        int codePoint = value.getKey();
                        UnicodeSet set = value.getValue();
                        System.out.println(Utility.hex(codePoint) 
                                + ";\t" + sourceScript
                                + ";\t" + targetScript
                                + "; A; " + set.toPattern(false)
                                + "\t# ( " + UTF16.valueOf(codePoint) + " ) " + CODEPOINT_TO_NAME.get(codePoint)
                                );
                    }
                }
            }

            scriptToScriptToUnicodeSet = CldrUtility.protectCollection(_scriptToScriptToUnicodeSet);
            scriptToScriptToCodepointToUnicodeSet = CldrUtility.protectCollection(_scriptToScriptToCodepointToUnicodeSet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void addConfusable(Style style, String source, String target, EnumMap<Style, UnicodeMap<String>> _style2map) {
        UnicodeMap<String> map = _style2map.get(style);
        if (map == null) {
            _style2map.put(style, map = new UnicodeMap<>());
        }
        map.put(source, target);
        EnumMap<Style, String> map2 = char2data.get(source);
        if (map2 == null) {
            char2data.put(source, map2 = new EnumMap<>(Style.class));
        }
        map2.put(style, target);
    }

    /**
     * @return the scriptToScriptToCodepointToUnicodeSet
     */
    public Map<Script_Values, Map<Script_Values, CodepointToConfusables>> getScriptToScriptToCodepointToUnicodeSet() {
        return scriptToScriptToCodepointToUnicodeSet;
    }

    static final Set<Script_Values> COMMON_SET = Collections.singleton(Script_Values.Common);
    static final Set<Script_Values> INHERITED_SET = Collections.singleton(Script_Values.Inherited);

    private void tryAdding(int aSingle, String b, Map<Script_Values, 
            Map<Script_Values, UnicodeSet>> _scriptToScriptToUnicodeSet,
            Map<Script_Values, Map<Script_Values, CodepointToConfusables>> _scriptToScriptToCodepointToUnicodeSet) {
        final ScriptDetector scriptDetector = new ScriptDetector();
        Set<Script_Values> aScripts = CODEPOINT_TO_SCRIPTS.get(aSingle);
        for (Script_Values aScript : aScripts) {
            if (aScript == Script_Values.Inherited) {
                aScript = Script_Values.Common;
            }
            Set<Script_Values> bScripts = scriptDetector.set(b).getSingleSet();
            if (bScripts == null) {
                continue; // not single set of scripts
            }
            for (Script_Values bScript : bScripts) {
                addToMap(_scriptToScriptToUnicodeSet, aSingle, aScript, bScript);
                addToMap(_scriptToScriptToCodepointToUnicodeSet, aSingle, b, aScript, bScript);
            }
        }
    }

    /**
     * Return single code point if a is one; otherwise -1;
     * @param a
     * @return
     */
    private int getSingleCodePoint(String a) {
        return a.length() == 1 || a.length() == 2 && Character.isHighSurrogate(a.charAt(0)) ? a.codePointAt(0) : -1;
    }

    private void addToMap(Map<Script_Values, Map<Script_Values, UnicodeSet>> _scriptToScriptToUnicodeSet, 
            int aSingle,
            Script_Values sourceScript,
            Script_Values targetScript) {
        UnicodeSet uset;
        Map<Script_Values, UnicodeSet> map = _scriptToScriptToUnicodeSet.get(sourceScript);
        if (map == null) {
            _scriptToScriptToUnicodeSet.put(sourceScript, map = new EnumMap<>(Script_Values.class));
        }
        uset = map.get(targetScript);
        if (uset == null) {
            map.put(targetScript, uset = new UnicodeSet());
        }
        uset.add(aSingle);
    }

    private void addToMap(Map<Script_Values, Map<Script_Values, CodepointToConfusables>> map, 
            int aSingle, String b, Script_Values sourceScript,
            Script_Values targetScript) {
        Map<Script_Values, CodepointToConfusables> map2 = map.get(sourceScript);
        if (map2 == null) {
            map.put(sourceScript, map2 = new EnumMap<>(Script_Values.class));
        }
        CodepointToConfusables map3 = map2.get(targetScript);
        if (map3 == null) {
            map2.put(targetScript, map3 = new CodepointToConfusables());
        }
        map3.add(aSingle, b);
    }


    //    /**
    //     * Return the script of the string, or null if there is not a unique one.
    //     * Only uses Script property for now.
    //     * @param source
    //     * @return
    //     */
    //    public static Script_Values getSingleScript(UnicodeSet source) {
    //        Script_Values result = null;
    //        boolean haveCommon = false;
    //        for (UnicodeSet.EntryRange range : source.ranges()) {
    //            for (int codepoint = range.codepoint; codepoint <= range.codepointEnd; ++codepoint) {
    //                Script_Values current = Confusables.CODEPOINT_TO_SCRIPT.get(codepoint);
    //                if (current == Script_Values.Common || current == Script_Values.Inherited) {
    //                    haveCommon = true;
    //                } else if (result == null) {
    //                    result = current;
    //                } else if (result != current) {
    //                    return null;
    //                }
    //            }
    //        }
    //        return result != null ? result : haveCommon ? Script_Values.Common : null;
    //    }

    public static final class ScriptDetector {
        public static final Joiner JOINER_COMMA_SPACE = Joiner.on(", ");
        public boolean isCommon;
        public final EnumSet<Script_Values> singleScripts = EnumSet.noneOf(Script_Values.class);
        public final HashSet<Set<Script_Values>> combinations = new HashSet<Set<Script_Values>>();

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
            combinations.clear();
            isCommon = false;

            boolean haveCommon = false;
            for (int codepoint : CharSequences.codePoints(source)) {
                Set<Script_Values> current = Confusables.CODEPOINT_TO_SCRIPTS.get(codepoint);
                if (current.size() > 1) {
                    combinations.add(current); // we know there are not Common or Inherited
                } else {
                    Script_Values only = current.iterator().next();
                    if (only == Script_Values.Common || only == Script_Values.Inherited) {
                        haveCommon = true;
                    } else {
                        singleScripts.add(only);
                    }
                }
            }
            // Remove redundant combinations
            if (combinations.size() > 0) {
                toRemove.clear();
                for (Set<Script_Values> combo : combinations) {
                    if (!Collections.disjoint(combo, singleScripts)) {
                        toRemove.add(combo);
                    }
                }
                combinations.removeAll(toRemove);
            }
            if (haveCommon && singleScripts.isEmpty() && combinations.isEmpty()) {
                singleScripts.add(Script_Values.Common);
                isCommon = true;
            }
            return this;
        }

        public int size() {
            return singleScripts.size() + combinations.size();
        }

        public Set<Script_Values> getSingleSet() {
            return combinations.isEmpty() ? singleScripts
                    : !singleScripts.isEmpty() ? null
                            : combinations.size() > 1 ? null
                                    : combinations.iterator().next();
        }
        @Override
        public String toString() {
            return singleScripts.isEmpty() ? JOINER_COMMA_SPACE.join(combinations)
                    : combinations.isEmpty() ? JOINER_COMMA_SPACE.join(singleScripts)
                            : JOINER_COMMA_SPACE.join(singleScripts) + ", " + JOINER_COMMA_SPACE.join(combinations);
        }
    }

    public CodepointToConfusables getCharsToConfusables(Script_Values sourceScript, Script_Values targetScript) {
        Map<Script_Values, CodepointToConfusables> map1 = scriptToScriptToCodepointToUnicodeSet.get(sourceScript);
        if (map1 == null) {
            return null;
        }
        CodepointToConfusables map2 = map1.get(targetScript);
        return map2;
    }
    
    public static class CodepointToConfusables implements Iterable<Entry<Integer,UnicodeSet>>, Freezable<CodepointToConfusables> {
        boolean isFrozen;
        Map<Integer, UnicodeSet> data = new TreeMap<>();

        @Override
        public Iterator<Entry<Integer,UnicodeSet>> iterator() {
            return data.entrySet().iterator();
        }

        public void add(int aSingle, String b) {
            UnicodeSet uset = data.get(aSingle);
            if (uset == null) {
                data.put(aSingle, uset = new UnicodeSet());
            }
            uset.add(b);
        }

        @Override
        public boolean isFrozen() {
            return isFrozen;
        }

        @Override
        public CodepointToConfusables freeze() {
            if (!isFrozen) {
                data = CldrUtility.protectCollection(data);
                isFrozen = true;
            }
            return this;
        }

        @Override
        public CodepointToConfusables cloneAsThawed() {
            throw new UnsupportedOperationException();
        }

        public UnicodeSet get(int cp) {
            return data.get(cp);
        }

        public UnicodeSet keySet() {
            return new UnicodeSet().addAll(data.keySet());
        }
    }

    public UnicodeSet getCharsWithConfusables() {
        return hasConfusable;
    }
}