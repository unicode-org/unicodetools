package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ICUUncheckedIOException;

public class Unilex {
    static final Splitter TAB = Splitter.on('\t');
    static final String DATADIR = "/Users/markdavis/Google Drive/workspace/DATA/unilex/"; 
    static final String GENDIR = "/Users/markdavis/Google Drive/workspace/Generated/unilex/"; 

    public static class Frequency {
        private static final Long ZERO = new Long(0);
        private final Map<String,Long> data;
        private final Multimap<Long, String> value2keys;

        public Long get(String key) {
            Long result = data.get(key);
            return result == null ? ZERO : result;
        }
        public int size() {
            return data.size();
        }
        public Set<String> keySet() {
            return data.keySet();
        }
        public Set<Long> valueSet() {
            return value2keys.keySet();
        }
        public Set<String> getKeys(Long value) {
            return (Set<String>) value2keys.get(value);
        }

        private Frequency(Map<String,Long> data) {
            this.data = data;
            TreeMultimap<Long, String> inverted = Multimaps.invertFrom(Multimaps.forMap(data), TreeMultimap.<Long,String>create());
            value2keys = ImmutableSetMultimap.copyOf(inverted);
        }
        public static Frequency create(String locale) {
            Map<String,Long> temp = new TreeMap<>();
            processFields(DATADIR + "frequency", locale+".txt", parts -> {
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("Wrong number of items: " + parts);
                }
                temp.put(parts.get(0), Long.parseLong(parts.get(1)));
                }
            );
            return new Frequency(ImmutableMap.copyOf(temp));
        }
    }

    public static class Pronunciation {
        private final Map<String,String> data;
        private final Multimap<String, String> value2keys;

        public int size() {
            return data.size();
        }
        public String get(Object key) {
            return data.get(key);
        }
        public Set<String> keySet() {
            return data.keySet();
        }
        public Set<String> valueSet() {
            return value2keys.keySet();
        }
        public Set<String> getKeys(String value) {
            return (Set<String>) value2keys.get(value);
        }

        private Pronunciation(Map<String,String> data) {
            this.data = data;
            TreeMultimap<String, String> inverted = Multimaps.invertFrom(Multimaps.forMap(data), TreeMultimap.<String,String>create());
            value2keys = ImmutableSetMultimap.copyOf(inverted);
        }
        public static Pronunciation create(String locale) {
            Map<String,String> temp = new TreeMap<>();
            processFields(DATADIR + "pronunciation", locale+".txt", parts -> {
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("Wrong number of items: " + parts);
                }
                temp.put(parts.get(0), parts.get(1));
                }
            );
            return new Pronunciation(ImmutableMap.copyOf(temp));
        }
    }
    
    private static <T> void processFields(String directory, String file, Consumer<List<String>> processor) {
        boolean firstNonEmpty = true;
        for (String line : FileUtilities.in(directory, file)) {
            if (line.isEmpty() || line.startsWith("# ")) {
                continue;
            }
            if (firstNonEmpty) {
                firstNonEmpty = false;
                continue;
            }
            List<String> parts = TAB.splitToList(line);
            processor.accept(parts);
        }
    }

    public static void main(String[] args) {
        getData("fr");
        getData("de");
    }

    private static void getData(String locale) {
        Pronunciation dep = Pronunciation.create(locale);
        Frequency def = Frequency.create(locale);
        try (PrintWriter out = FileUtilities.openUTF8Writer(GENDIR, locale + ".log.txt")) {
            int i = 0;

            GraphemeCount pronGraphemes = new GraphemeCount();
            GraphemeCount termGraphemes = new GraphemeCount();
            out.println("\n#1. Identical pronunciations");
            out.println("\n#Count\tPronunciations\tTerm");
            for (String pron : dep.valueSet()) {
                Set<String> set = dep.getKeys(pron);
                if (set.size() > 1) {
                    set = reduceCase(set);
                    if (set.size() < 2) {
                        continue;
                    }
                    if (0 == (i & 0xFF)) {
                        int debug = 0;
                    }
                    out.println(i + "\t" + pron + "\t" + CollectionUtilities.join(set, "\t"));
                    ++i;
                }
                for (String term : set) {
                    Long freq = def.get(term);
                    pronGraphemes.add(freq, pron, term);
                    termGraphemes.add(freq, term, pron);
                }
            }
            out.println("\n2. Pronunciation Graphemes");
            pronGraphemes.print(out);
            
            out.println("\n3. Pronunciation Term Graphemes");
            termGraphemes.print(out);

            // show the pronunciations in descending frequency order
            i = 0;
            GraphemeCount freqGraphemes = new GraphemeCount();
            ArrayList<Long> frequencies = new ArrayList<>(def.valueSet());
            ArrayList<String> lines = new ArrayList<>();
            for (Long freq : Lists.reverse(frequencies)) {
                for (String fkey : def.getKeys(freq)) {
                    freqGraphemes.add(freq, fkey);
                    String p = dep.get(fkey);
                    //                if (p == null) {
                    //                    continue;
                    //                }
                    if (0 == (i & 0xFF)) {
                        int debug = 0;
                    }
                    lines.add(freq + "\t" + fkey + "\t" + p);
                    i++;
                }
            }
            out.println("\n4. Frequency Term Graphemes");
            freqGraphemes.print(out);

            out.println("\n5. Frequency of Terms with Pronunciation");
            out.println("\n#Frequency\tTerm\tPronunciation");
            for (String line : lines) {
                out.println(line);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static Set<String> reduceCase(Set<String> set) {
        Set<String> result = new LinkedHashSet<>(set);
        for (Iterator<String> it = result.iterator(); it.hasNext();) {
            String item = it.next();
            String lc = UCharacter.toLowerCase(item); // TODO locale sensitive
            if (!lc.equals(item) && set.contains(lc)) {
                it.remove();
            }
        }
        return result;
    }

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make();
    static final UnicodeMap<General_Category_Values> cat = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);

    static public final class GraphemeCount {
        private Counter<String> target = new Counter<>();
        private Map<String,String> sourceToSamples = new HashMap<>();
        private Map<String,Long> sourceToFrequency = new HashMap<>();
        private transient StringBuilder item = new StringBuilder();

        void add(Long frequency, String source, String... samples) {
            for (int cp : With.codePointArray(source)) {
                switch (cat.get(cp)) {
                default:
                    //            case Uppercase_Letter:
                    //            case Lowercase_Letter:
                    //            case Other_Letter:
                    //            case Titlecase_Letter:
                    if (item.length() != 0 && item.charAt(item.length()-1) == '͡') {
                        // special
                    } else {
                        flush(frequency, source, samples);
                    }
                    item.appendCodePoint(cp);
                    break;
                case Enclosing_Mark:
                case Nonspacing_Mark:
                case Spacing_Mark:
                    item.appendCodePoint(cp);
                    break;
                case Modifier_Letter:
                case Modifier_Symbol:
                    if (cp == 'ˈ' || cp == 'ˌ') { // singleton
                        flush(frequency, source, samples);
                        item.appendCodePoint(cp);
                        flush(frequency, source, samples);
                    } else {
                        item.appendCodePoint(cp);
                    }
                    break;
                }
            }
            flush(frequency, source, samples);
        }

        private void flush(Long frequency, String source, String... samples) {
            if (item.length() != 0) {
                String string = item.toString();
                Long oldFreq = sourceToFrequency.get(string);
                if (oldFreq  == null || oldFreq <= frequency) {
                    sourceToFrequency.put(string, frequency);
                    sourceToSamples.put(string, source + (samples.length == 0 ? "" : "\t" + Arrays.asList(samples)));
                }
                target.add(string, frequency);
                item.setLength(0);
            }
        }

        Set<R2<Long, String>> entries() {
            return target.getEntrySetSortedByCount(false, null);
        }

        String getSample(String grapheme) {
            return sourceToSamples.get(grapheme);
        }

        void print(PrintWriter out) {
            out.println("\n#Frequency\tGrapheme\tHex\tSample");
            for (R2<Long, String> item : entries()) {
                out.println(item.get0() 
                        + "\t" + item.get1() 
                        + "\t" + Utility.hex(item.get1(), " ") 
                        + "\t" + getSample(item.get1()));
            }
        }

    }
}
