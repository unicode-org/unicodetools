package org.unicode.draft;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.text.UCA.CEList;
import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Settings;

import com.ibm.icu.text.UTF16;


import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterEnums.ECharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;

public class ScriptCount {
    private static final double LOG2 = Math.log(2);

    final static Options myOptions = new Options();
    enum MyOptions {
        ranked("(true|false)", "true", "Use ranked frequencies"),
        language(".*", "mul", "Language code (mul for all)."),
        nfd(null, null, "stats on nfd"),
        filter(".*", null, "only particular category"),
        secondary(null, null, "record secondary weights"),
        ;
        // boilerplate
        final Option option;
        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static class SecondaryInfo implements Comparable<SecondaryInfo>{
        final int secondary;
        long frequency;
        int codePointCount;
        int sampleCodePoint;
        long sampleCount;
        int sampleLength = Integer.MAX_VALUE;
        SecondaryInfo(int secondary) {
            this.secondary = secondary;
        }
        public void add(int sampleCodePoint2, int length, long count2) {
            frequency += count2;
            codePointCount++;
            if (sampleLength < length) {
                return;
            }
            if (sampleLength > length || sampleCount < count2) {
                sampleCodePoint = sampleCodePoint2;
                sampleCount = count2;
                sampleLength = length;
            }
        }
        @Override
        public int compareTo(SecondaryInfo arg0) {
            if (frequency != arg0.frequency){
                return frequency > arg0.frequency ? -1 : 1;
            }
            return arg0.secondary - secondary;
        }
    }

    static class SecondaryCounts {
        private final UCA uca = UCA.buildCollator(null);
        private final Map<Integer, SecondaryInfo> counter = new HashMap<Integer, SecondaryInfo>();

        void add(int sourceString, long count) {
            final CEList celist = uca.getCEList(UTF16.valueOf(sourceString), true);
            final int length = celist.length();
            for (int i = 0; i < length; ++i)  {
                final int ce = celist.at(i);
                final int secondary = UCA.getSecondary(ce);
                SecondaryInfo info = counter.get(secondary);
                if (info == null) {
                    counter.put(secondary, info = new SecondaryInfo(secondary));
                }
                info.add(sourceString, length, count);
            }
        }
        Set<SecondaryInfo> getSorted() {
            return new TreeSet<SecondaryInfo>(counter.values());
        }
    }

    public static void main(String[] args) {
        myOptions.parse(MyOptions.ranked, args, true);
        final boolean ranked = MyOptions.ranked.option.getValue().equals("true");
        final String language = MyOptions.language.option.getValue();
        final String filter = MyOptions.filter.option.getValue();
        final boolean nfd = MyOptions.nfd.option.doesOccur();
        final boolean secondary = MyOptions.secondary.option.doesOccur();

        Counter<Integer> langCounter = CharacterFrequency.getCodePointCounter(language, ranked);
        //System.out.println(langCounter.getItemCount());
        final Normalizer2 nfkc = Normalizer2.getNFKCInstance();
        final Normalizer2 toNfd = Normalizer2.getNFDInstance();
        final Map<String, Counter<Integer>> keyCounter = new TreeMap<String,Counter<Integer>>();
        final BitSet bitset = new BitSet();
        SecondaryCounts secondaryCounts = null;
        if (secondary) {
            secondaryCounts = new SecondaryCounts();
        }

        if (nfd) {
            final Counter<Integer> langCounter2 = new Counter<Integer>();
            for (final Integer cp : langCounter) {
                final long count = langCounter.getCount(cp);
                final String nfdString = toNfd.getDecomposition(cp);
                if (nfdString != null) {
                    for (final int cp2 : With.codePointArray(nfdString)) {
                        langCounter2.add(cp2, count);
                    }
                    continue;
                }
                langCounter2.add(cp, count);
            }
            langCounter = langCounter2;
        }
        for (final Integer cp : langCounter) {
            final long count = langCounter.getCount(cp);
            addCharacter(nfkc, keyCounter, bitset, cp, count);
            if (secondary) {
                secondaryCounts.add(cp, count);
            }
        }
        for (final Entry<String, Counter<Integer>> entry : keyCounter.entrySet()) {
            final String key = entry.getKey();
            final Counter<Integer> counter = entry.getValue();
            System.out.println(key + "\t" + Math.log(counter.getTotal()) + "\t" + counter.getItemCount() + "\t" + getTop(32, counter, langCounter));
        }
        // Only supplementary characters
        for (final Entry<String, Counter<Integer>> entry : keyCounter.entrySet()) {
            final String key = entry.getKey();
            final Counter<Integer> counter0 = entry.getValue();
            final Counter<Integer> counter = filterCounter(counter0);
            System.out.println(key + "\t" + Math.log(counter.getTotal()) + "\t" + counter.getItemCount() + "\t" + getTop(32, counter, langCounter));
        }

        final DecimalFormat pf = (DecimalFormat) NumberFormat.getInstance();
        pf.setMaximumFractionDigits(6);
        pf.setMinimumFractionDigits(6);
        //        pf.setMinimumSignificantDigits(3);
        //        pf.setMaximumSignificantDigits(3);
        final int counter = 0;
        final double max = langCounter.getTotal();
        PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(Settings.GEN_DIR + "/frequency-text",
                language
                + (nfd ? "-nfd" : "")
                + (filter != null ? "-" + filter : "") +
                ".txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
        final Matcher m = filter == null ? null : Pattern.compile(filter).matcher("");
        for (final int ch : langCounter.getKeysetSortedByCount(false)) {
            final long count = langCounter.get(ch);
            // 0%   忌   U+5FCC  Lo  Hani    CJK UNIFIED IDEOGRAPH-5FCC
            final String catString = propValue(ch, UProperty.GENERAL_CATEGORY, UProperty.NameChoice.SHORT);
            final String scriptString = propValue(ch, UProperty.SCRIPT, UProperty.NameChoice.SHORT);
            if (m != null && !m.reset(catString).matches() && !m.reset(scriptString).matches()) {
                continue;
            }
            out.println(pf.format(Math.log(count/max)/LOG2)
                    + "\t" + show(ch)
                    + "\tU+" + Utility.hex(ch, 4)
                    + "\t" + catString
                    + "\t" + scriptString
                    + "\t" + UCharacter.getExtendedName(ch));
            //if (count < 10000) break;
        }
        out.close();
        if (secondary) {
            out = org.unicode.text.utility.Utility.openPrintWriter(Settings.GEN_DIR + "/frequency-text",
                    language + "-sec"
                            + (nfd ? "-nfd" : "")
                            + (filter != null ? "-" + filter : "") +
                            ".txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
            for (final SecondaryInfo secondaryInfo : secondaryCounts.getSorted()) {
                if (secondaryInfo.secondary == 0) {
                    continue;
                }
                final int ch = secondaryInfo.sampleCodePoint;
                final String catString = propValue(ch, UProperty.GENERAL_CATEGORY, UProperty.NameChoice.SHORT);
                final String scriptString = propValue(ch, UProperty.SCRIPT, UProperty.NameChoice.SHORT);
                if (m != null && !m.reset(catString).matches() && !m.reset(scriptString).matches()) {
                    continue;
                }
                out.println(pf.format(Math.log(secondaryInfo.frequency/max)/LOG2)
                        + "\t0x" + Utility.hex(secondaryInfo.secondary)
                        + "\t" + secondaryInfo.sampleLength
                        + "\t" + secondaryInfo.codePointCount
                        + "\t" + show(ch)
                        + "\tU+" + Utility.hex(secondaryInfo.sampleCodePoint, 4)
                        + "\t" + catString
                        + "\t" + scriptString
                        + "\t" + UCharacter.getExtendedName(ch));
            }
            out.close();
        }
    }

    private static Counter<Integer> filterCounter(Counter<Integer> counter0) {
        final Counter<Integer> result = new Counter<Integer>();
        for (final Integer i : counter0.keySet()) {
            if (i > 0xFFFF) {
                result.add(i, counter0.get(i));
            }
        }
        return result;
    }

    public static Integer addCharacter(Normalizer2 nfkc,
            Map<String, Counter<Integer>> keyCounter, BitSet bitset,
            Integer cp, long count) {
        int cat = UCharacter.getType(cp);
        if (cat == ECharacterCategory.ENCLOSING_MARK) {
            cat = ECharacterCategory.NON_SPACING_MARK;
        }
        String key = null;
        if (cp > 0xFFFF) {
            addCount(keyCounter, cp, count, "!BMP\tSupplementary");
        }
        if (isLetter(cat)) {
            cp = UCharacter.toLowerCase(cp);
            final String norm = nfkc.getDecomposition(cp);
            if (norm == null) {
                addScript(keyCounter, cp, bitset, count);
            } else {
                for (final int cp2 : With.codePointArray(norm)) {
                    final int cat2 = UCharacter.getType(cp2);
                    if (isLetter(cat2)) {
                        addScript(keyCounter, cp2, bitset, count);
                    }
                }
            }
        } else if (UCharacter.isWhitespace(cp) || cat == ECharacterCategory.SPACE_SEPARATOR) {
            key = "*WS\tWhitespace";
            addCount(keyCounter, cp, count, key);
        } else {
            key = "*" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, cat, NameChoice.SHORT)
                    + "\t" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, cat, NameChoice.LONG);
            addCount(keyCounter, cp, count, key);
        }
        return cp;
    }

    public static boolean isLetter(int cat) {
        return cat == ECharacterCategory.UPPERCASE_LETTER || cat == ECharacterCategory.LOWERCASE_LETTER || cat == ECharacterCategory.MODIFIER_LETTER || cat == ECharacterCategory.TITLECASE_LETTER
                || cat == ECharacterCategory.OTHER_LETTER || cat == ECharacterCategory.COMBINING_SPACING_MARK;
    }

    public static void addScript(Map<String, Counter<Integer>> keyCounter,
            int cp2, BitSet bitset, long count) {
        UScript.getScriptExtensions(cp2, bitset);
        for (int script = bitset.nextSetBit(0); script >= 0; script = bitset.nextSetBit(script+1)) {
            final String key = UScript.getShortName(script) + "\t" + UScript.getName(script);
            addCount(keyCounter, cp2, count, key);
        }
    }

    public static Counter<Integer> addCount(
            Map<String, Counter<Integer>> keyCounter, Integer cp, long count,
            String key) {
        Counter<Integer> counter = keyCounter.get(key);
        if (counter == null) {
            keyCounter.put(key, counter = new Counter<Integer>());
        }
        counter.add(cp, count);
        return counter;
    }

    private static String getTop(int max, Counter<Integer> counter, Counter<Integer> languageCounter) {
        final StringBuilder b = new StringBuilder();
        for (final int cp : counter.getKeysetSortedByCount(false)) {
            if (--max < 0) {
                break;
            }
            if (b.length() != 0) {
                b.append("\t");
            }
            //b.append('“');
            b.append(show(cp));
            //b.append('”').append("(").append((int)Math.round(100*Math.log(langCounter.get(0x20)/langCounter.get(cp)))).append(")");
        }
        return b.toString();
    }

    private static String getExtendedName(String s, String separator) {
        final StringBuilder result = new StringBuilder();
        boolean first = true;
        for (final int cp : With.codePointArray(s)) {
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(UCharacter.getExtendedName(cp));
        }
        return result.toString();
    }

    private static String propValue(int ch, int propEnum, int nameChoice) {
        return UCharacter.getPropertyValueName(propEnum, UCharacter.getIntPropertyValue(ch, propEnum), nameChoice);
    }

    private static String show(int s) {
        final int cat = UCharacter.getType(s);
        if (cat == ECharacterCategory.FORMAT || cat == ECharacterCategory.CONTROL || cat == ECharacterCategory.PRIVATE_USE
                || cat == ECharacterCategory.SPACE_SEPARATOR || cat == ECharacterCategory.LINE_SEPARATOR || cat == ECharacterCategory.PARAGRAPH_SEPARATOR) {
            return "U+" + Utility.hex(s);
        }
        if (s == '\'' || s == '"' || s == '=') {
            return "'" + UTF16.valueOf(s);
        }
        return UTF16.valueOf(s);
    }

    private static int getScript(String norm) {
        int cp;
        int result = UScript.INHERITED;
        for (int i = 0; i < norm.length(); i += Character.charCount(cp)) {
            cp = norm.codePointAt(i);
            int script = UScript.getScript(cp);
            if (script == UScript.UNKNOWN) {
                final int type = UCharacter.getType(cp);
                if (type == ECharacterCategory.PRIVATE_USE) {
                    script = UScript.BLISSYMBOLS;
                }
            }
            if (script == UScript.INHERITED || script == result) {
                continue;
            }
            if (script == UScript.COMMON) {
                if (result == UScript.INHERITED) {
                    result = script;
                }
                continue;
            }
            if (result == UScript.COMMON || result == UScript.INHERITED) {
                result = script;
                continue;
            }
            // at this point both are different explicit scripts
            return UScript.COMMON;
        }
        return result;
    }
}
