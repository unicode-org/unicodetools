package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUException;

/**
 * Class that encapsulates the data from confusables.txt. It currently generates its own whole-script confusable data,
 * because of omissions in the Unicode data file.
 * @author markdavis
 *
 */
public class Confusables {
    public static final Splitter SEMI = Splitter.on(';').trimResults();

    /** type of confusable data. Only the MA data is used; the rest is deprecated. 
     * @author markdavis
     */
    public enum Style {
//        SL, 
//        SA, 
//        ML, 
        MA}

//    private static class Data {
//        final Style style;
//        final String result;
//        public Data(Style style, String result) {
//            this.style = style;
//            this.result = result;
//        }
//    }

    
    /**
     * @return the style2map
     */
    public UnicodeMap<String> getRawMapToRepresentative(Style style) {
        return style2RawMapToRepresentative.get(style);
    }

    /**
     * Get the mapping from character to representative confusable.
     * @return the char2data
     */
    public UnicodeMap<EnumMap<Style, String>> getChar2data() {
        return char2data;
    }

    final private EnumMap<Style, UnicodeMap<String>> style2RawMapToRepresentative;
    final private UnicodeSet hasConfusable = new UnicodeSet();
    final private UnicodeMap<EnumMap<Style,String>> char2data = new UnicodeMap<EnumMap<Style,String>>();

    final private Map<Script_Values, Map<Script_Values, CodepointToConfusables>> scriptToScriptToCodepointToUnicodeSet;

    /**
     * Mapping from codepoint to name.
     */
    public static final UnicodeMap<String> CODEPOINT_TO_NAME = ScriptDetector.IUP.load(UcdProperty.Name);

    /**
     * Create confusables data from a directory—not cached!
     * @param directory
     */
    public Confusables (String directory) {
        try {
            EnumMap<Style, UnicodeMap<String>> _style2RawMapToRepresentative = new EnumMap<Style,UnicodeMap<String>>(Style.class);
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
                addConfusable(style, source, target, _style2RawMapToRepresentative);

//                if (CharSequences.getSingleCodePoint(target) != Integer.MAX_VALUE) {
//                    addConfusable(style, target, source, _style2RawMapToRepresentative);
//                }
            }
            style2RawMapToRepresentative = CldrUtility.protectCollection(_style2RawMapToRepresentative);
            char2data.freeze();
            hasConfusable.freeze();

            // patch, because the file doesn't contain X => common/inherited or the targetSet
            
            UnicodeMap<String> codepointToRepresentativeConfusable = style2RawMapToRepresentative.get(Style.MA);
            Map<Script_Values, Map<Script_Values, CodepointToConfusables>> _scriptToScriptToCodepointToUnicodeSet = new EnumMap<>(Script_Values.class);

            // get the equivalence classes
            final ScriptDetector scriptDetector = new ScriptDetector();
            for (String representative : codepointToRepresentativeConfusable.values()) {
                UnicodeSet equivalents = new UnicodeSet(codepointToRepresentativeConfusable.getSet(representative))
                .add(representative);
                for (String a : equivalents) {
                    for (String b : equivalents) {
                        if (a.equals(b)) {
                            continue; // skip a => a
                        }
                        // only if the source is a single code point
                        int aSingle = getSingleCodePoint(a);
                        if (aSingle < 0) { // not valid singleton?
                            continue;
                        }
                        Set<Script_Values> aScripts = ScriptDetector.getScriptExtensions(aSingle);
                        for (Script_Values aScript : aScripts) {
                            Set<Script_Values> bScripts = scriptDetector.set(b).getSingleSetOrNull();
                            if (bScripts == null) {
                                continue; // not a single set of scripts
                            }
                            for (Script_Values bScript : bScripts) {
                                addToMap(aScript, bScript, aSingle, b, _scriptToScriptToCodepointToUnicodeSet);
                            }
                        }
                    }
                }
            }
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

    private void addToMap(Script_Values sourceScript, 
            Script_Values targetScript, int aSingle, String b,
            Map<Script_Values, Map<Script_Values, CodepointToConfusables>> map) {
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

    /**
     * Return whole-script confusables data. Augments the Unicode data by adding the set of characters mapped to.
     * @return null if no match for script1+script2
     */
    public CodepointToConfusables getCharsToConfusables(Script_Values sourceScript, Script_Values targetScript) {
        Map<Script_Values, CodepointToConfusables> map1 = scriptToScriptToCodepointToUnicodeSet.get(sourceScript);
        if (map1 == null) {
            return null;
        }
        CodepointToConfusables map2 = map1.get(targetScript);
        return map2;
    }

    /**
     * A map from codepoints to sets of characters. Encapsulated to make it easier to manage.
     * @author markdavis
     */
    public static class CodepointToConfusables implements Iterable<Entry<Integer,UnicodeSet>>, Freezable<CodepointToConfusables> {
        boolean isFrozen;
        Map<Integer, UnicodeSet> data = new TreeMap<>();

        @Override
        public Iterator<Entry<Integer,UnicodeSet>> iterator() {
            return data.entrySet().iterator();
        }

        private void add(int aSingle, String b) {
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

    /**
     * Returns all the characters that have some confusable.
     * @return
     */
    public UnicodeSet getCharsWithConfusables() {
        return hasConfusable;
    }

    /**
     * Prints out the whole-script confusable data.
     * @param out
     */
    public void print(Appendable out) {
        try {
            for (Entry<Script_Values, Map<Script_Values, CodepointToConfusables>> scriptToCodepointToUnicodeSet 
                    : scriptToScriptToCodepointToUnicodeSet.entrySet()) {
                String sourceScript = scriptToCodepointToUnicodeSet.getKey().getShortName();
                for (Entry<Script_Values, CodepointToConfusables> codepointToUnicodeSet 
                        : scriptToCodepointToUnicodeSet.getValue().entrySet()) {
                    String targetScript = codepointToUnicodeSet.getKey().getShortName();
                    UnicodeMap<UnicodeSet> temp = new UnicodeMap<UnicodeSet>();
                    for (Entry<Integer, UnicodeSet> value : codepointToUnicodeSet.getValue()) {
                        temp.put(value.getKey(), value.getValue());
                    }
                    for (UnicodeSet values : temp.values()) {
                        UnicodeSet keys = temp.getSet(values);
                        for (UnicodeSet.EntryRange range : keys.ranges()) {
                            final boolean single = range.codepointEnd == range.codepoint;
                            out.append(Utility.hex(range.codepoint) 
                                    + (single ? "\t\t" : ".." + Utility.hex(range.codepointEnd))
                                    + ";\t" + sourceScript
                                    + ";\t" + targetScript
                                    + "; A; " + values.toPattern(false)
                                    + "\t# ( " + UTF16.valueOf(range.codepoint) + " ) " + CODEPOINT_TO_NAME.get(range.codepoint)
                                    + (single ? "" : "...")
                                    + "\n"
                                    );
                        }
                    }
                }
                out.append("\n");
            }
        } catch (IOException e) {
            throw new ICUException(e);
        }
    }

    /**
     * Write out the whole-script confusables data.
     */
    public static void main(String[] args) throws IOException {
        final String SECURITY_PUBLIC = Settings.UNICODE_DRAFT_PUBLIC + "security/";
        final Confusables CONFUSABLES = new Confusables(SECURITY_PUBLIC + Settings.latestVersion);
        try (PrintWriter pw = FileUtilities.openUTF8Writer(Settings.GEN_UCD_DIR, "confusablesWholeScript.txt")) {
            CONFUSABLES.print(pw);
            pw.flush();
        }
    }
}