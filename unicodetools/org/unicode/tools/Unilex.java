package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
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
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

public class Unilex {
    static final Splitter TAB = Splitter.on('\t');
    static final String DATADIR = "/Users/markdavis/Google Drive/workspace/DATA/unilex/"; 
    static final String GENDIR = "/Users/markdavis/Google Drive/workspace/Generated/unilex/"; 
    static final UnicodeSet IPA_VOWELS = new UnicodeSet("[a æ ɐ-ɒ e ə ɛ ɘ ɜ ɞ ɤ i ɪ ɨ oø œ ɶ ɔ ɵ u ʉ ɯ ʊ ʌ y ʏ]").freeze();
    static final UnicodeSet VOWELS = new UnicodeSet("[AEIOUYaeiouyÀ-ÆÈ-ÏÒ-Ö Ø-Ýà-æè-ïò-öø-ýÿ-ąĒ-ě Ĩ-İŌ-œŨ-ųŶ-ŸƆƏƐƗƜƟ-ơƯ-Ʊ Ǎ-ǜǞ-ǣǪ-ǭǺ-ȏȔ-ȗȦ-ȳɄɅɐ-ɒ ɔɘəɛɜɞɤɨɪɯɵɶʉʊʌʏḀḁḔ-ḝ Ḭ-ḯṌ-ṓṲ-ṻẎẏẙẠ-ỹÅⱭⱯⱰꞫꞮ]");
    private static final Long ZERO = new Long(0);
    private static final Long ONE = new Long(1);
    private static Normalizer2 NFC = Normalizer2.getNFCInstance();

    public static String cleanTerm(String source) {
        return source.replace('\'', '’').replace('‘', '’').replace('´', '’');
    }

    // TODO clean IPA də.ˈˈpɥi => dəˈpɥi, etc.

    public static class Frequency {
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
            locale = locale.replace("-fonxsamp","");

            Map<String,Long> temp = new TreeMap<>();
            processFields(DATADIR + "frequency", locale+".txt", parts -> {
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("Wrong number of items: " + parts);
                }
                temp.put(cleanTerm(parts.get(0)), Long.parseLong(parts.get(1)));
            }
                    );
            return new Frequency(ImmutableMap.copyOf(temp));
        }
    }

    static final Transliterator XSampa_IPA = Transliterator.getInstance("XSampa-IPA");

    public static class Pronunciation {
        private final Map<String,String> data;
        private final Map<String,String> rawToIpa;
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

        private Pronunciation(Map<String,String> data, Map<String,String> rawToIpa) {
            this.data = data;
            this.rawToIpa = rawToIpa;
            
            TreeMultimap<String, String> inverted = Multimaps.invertFrom(Multimaps.forMap(data), TreeMultimap.<String,String>create());
            value2keys = ImmutableSetMultimap.copyOf(inverted);
        }
        public static Pronunciation create(String locale) {
            Map<String,String> temp = new TreeMap<>();
            Map<String,String> _rawToIpa = new TreeMap<>();
            boolean isXsampa = locale.contains("fonxsamp");
            processFields(DATADIR + "pronunciation", locale+".txt", parts -> {
                String source = cleanTerm(parts.get(0));
                switch(parts.size()) {
                case 2:
                    String target = parts.get(1);
                    _rawToIpa.put(source, target);
                    if (isXsampa) {
                        target = XSampa_IPA.transform(target
                                .replace('\'', 'ˈ')
                                .replace('-', '.')
                                .replace(',', 'ˌ')
                                );
                    }
                    target = target.replace(".ˈ", "ˈ").replace(".ˌ", "ˌ");
                    temp.put(source, target);
                    break;
                case 0:
                case 1:
                    break;
                default:
                    throw new IllegalArgumentException("Wrong number of items: " + parts);
                }
            }
                    );
            return new Pronunciation(ImmutableMap.copyOf(temp), ImmutableMap.copyOf(_rawToIpa));
        }
    }

    private static <T> void processFields(String directory, String file, Consumer<List<String>> processor) {
        boolean firstNonEmpty = true;
        for (String line : FileUtilities.in(directory, file)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (firstNonEmpty) {
                firstNonEmpty = false;
                continue;
            }
            List<String> parts = TAB.splitToList(NFC.normalize(line));
            processor.accept(parts);
        }
    }

    public static void main(String[] args) {
        Pronunciation xsamp = getData("de-fonxsamp");
        Pronunciation plain = getData("de");
        
        Set<String> combined = new LinkedHashSet<>(xsamp.data.keySet());
        combined.addAll(plain.keySet());
        Set<String> extra = new LinkedHashSet<>();
        for (String key : combined) {
            String xsampValue = xsamp.get(key);
            String plainValue = plain.get(key);
            if (Objects.equal(xsampValue, plainValue)) {
                continue;
            }
            String plainValue2 = plainValue == null ? null : plainValue
            .replace("ɐ̯", "ɐ")
            .replace("ʊ̯", "ʊ")
            .replace("ʏ̯", "ʏ")
            .replace("ɪ̯", "ɪ")
            //.replace("ʊ̯̯", "ʊ")
            .replace("t͡s", "ts")
            .replace("t͡ʃ", "tʃ")
            .replace("p͡f", "pf")
            // 
            ;
            if (plainValue != null && !plainValue2.contains("ˈ")) {
                plainValue2 = "ˈ" + plainValue2;
            }
            if (Objects.equal(xsampValue, plainValue2)) {
                continue;
            }
            if (plainValue == null) {
                extra.add(key + "\t" + xsampValue);
            } else {
                System.out.println(key + "\t" + xsampValue + "\t" + plainValue2);
            }
        }
        int i = 0;
        for (String s : extra) {
            System.out.println(++i + "\t" + s);
        }
        getData("fr");
    }

    private static Pronunciation getData(String locale) {
        Collator collator = Collator.getInstance(ULocale.forLanguageTag(locale));
        Pronunciation dep = Pronunciation.create(locale);
        Frequency def = Frequency.create(locale);
        try (PrintWriter out = FileUtilities.openUTF8Writer(GENDIR, locale + ".log.txt")) {
            int i = 0;
            TreeSet<String> sorted = new TreeSet<>(collator);

            GraphemeCount pronGraphemes = new GraphemeCount(collator);
            //            GraphemeCount termGraphemes = new GraphemeCount();
            out.println("\n#1. Identical pronunciations");
            out.println("#Count\tPronunciations\tTerm");
            sorted.addAll(dep.valueSet());
            Set<String> set = new TreeSet<>(collator);
            for (String pron : sorted) {
                set.clear();
                set.addAll(dep.getKeys(pron));
                for (String term : set) {
                    Long freq = def.get(term);
                    pronGraphemes.add(freq, pron, false, term);
                    //                  termGraphemes.add(freq, term, true, pron);
                }
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
            }
            pronGraphemes.print(out, 2, "Pronunciation Graphemes");

            //termGraphemes.print(out, 3, "Pronunciation Term Graphemes");

            // show the pronunciations in descending frequency order
            i = 0;
            sorted.clear();
            GraphemeCount freqGraphemes = new GraphemeCount(collator);
            ArrayList<Long> frequencies = new ArrayList<>(def.valueSet());
            ArrayList<String> lines = new ArrayList<>();
            for (Long freq : Lists.reverse(frequencies)) {
                sorted.addAll(def.getKeys(freq));
                for (String fkey : sorted) {
                    String p = dep.get(fkey);
                    freqGraphemes.add(freq, fkey, true, p);
                    lines.add(freq + "\t" + fkey + "\t" + p);
                    i++;
                }
                sorted.clear();
            }
            // add missing from pronunciation
            sorted.addAll(dep.keySet());
            sorted.removeAll(def.keySet());
            for (String fkey : sorted) {
                String p = dep.get(fkey);
                freqGraphemes.add(0L, fkey, true, p);
                lines.add(0 + "\t" + fkey + "\t" + p);
                i++;
            }

            freqGraphemes.print(out, 3, "Term Graphemes");

            out.println("\n#4. Frequency of Terms with Pronunciation");
            out.println("#Frequency\tTerm\tPronunciation");
            for (String line : lines) {
                out.println(line);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return dep;
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

    static public final class GraphemeList implements Iterable<String> {
        private List<String> graphemes = new ArrayList<>();
        private transient StringBuilder item = new StringBuilder();

        public void fill(String source) {
            graphemes.clear();
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
                        flush(source);
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
                    if (cp == 'ˈ' || cp == 'ˌ' || cp == '.') { // singleton
                        flush(source);
                        item.appendCodePoint(cp);
                        flush(source);
                    } else {
                        item.appendCodePoint(cp);
                    }
                    break;
                }
            }
            flush(source);
        }
        private void flush(String source) {
            if (item.length() != 0) {
                String string = item.toString();
                graphemes.add(string);
                item.setLength(0);
            }
        }
        @Override
        public Iterator<String> iterator() {
            return graphemes.iterator();
        }
    }

    static class FrequencyAndSamples {
        private Counter<String> targetMap = new Counter<>();
        private Map<String,String> sampleMap = new HashMap<>();

        private void add(String string, Long frequency, String sampleString) {
            Long oldFreq = targetMap.get(string);
            if (oldFreq  == null || oldFreq <= frequency) {
                targetMap.add(string, frequency);
                sampleMap.put(string, sampleString);
            }
        }
    }

    static public final class GraphemeCount {
        FrequencyAndSamples source = new FrequencyAndSamples();

        private Counter<String> target = new Counter<>();

        FrequencyAndSamples termOnsets = new FrequencyAndSamples();
        FrequencyAndSamples termCodas = new FrequencyAndSamples();
        FrequencyAndSamples termRimes = new FrequencyAndSamples();

        private transient GraphemeList graphemeList = new GraphemeList();
        private transient StringBuilder termOnset = new StringBuilder();
        private transient StringBuilder termRime = new StringBuilder();
        private transient StringBuilder termCoda = new StringBuilder();

        private final Comparator<String> collator;

        public GraphemeCount(Comparator collator) {
            this.collator = (Comparator<String>) collator;
        }

        void add(Long frequency, String source, boolean toLower, String... samples) {
            //            if (frequency.equals(ZERO)) {
            //                frequency = ONE;
            //            }
            String sampleString = sampleString(source, samples);

            graphemeList.fill(toLower ? UCharacter.toLowerCase(source) : source);
            termOnset.setLength(0);
            termCoda.setLength(0);
            boolean start = true;
            boolean wasVowel = false;
            boolean hasVowel = false;
            for (String grapheme : graphemeList) {
                boolean isVowel = VOWELS.containsSome(grapheme);
                hasVowel |= isVowel;
                // capture the onset until we hit the first vowel
                if (start) {
                    if (isVowel) {
                        termOnsets.add(clean(termOnset), frequency, sampleString);
                        start = false;
                    } else {
                        termOnset.append(grapheme);
                    }
                }
                // capture the rime (vowel & everything after), but flush whenever we transition from consonant to vowel
                if (isVowel) {
                    termCoda.setLength(0);
                    if (!wasVowel) {
                        termRime.setLength(0);
                    }
                } else {
                    termCoda.append(grapheme);
                }
                termRime.append(grapheme);
                flush(grapheme, frequency, sampleString);
                wasVowel = isVowel;
            }
            if (hasVowel) {
                termCodas.add(clean(termCoda), frequency, sampleString);
                termRimes.add(clean(termRime), frequency, sampleString);
            }
        }

        private String clean(CharSequence source) {
            return source.toString().replace("ˈ","").replace(".","");
        }

        private void flush(String grapheme, Long frequency, String sampleString) {
            source.add(grapheme, frequency, sampleString);
            target.add(grapheme, frequency);
        }

        private String sampleString(String source, String... samples) {
            return source + (samples.length == 0 ? "" : "\t" + Arrays.asList(samples));
        }

        Set<R2<Long, String>> entries() {
            return target.getEntrySetSortedByCount(false, null);
        }

        String getSample(String grapheme) {
            return source.sampleMap.get(grapheme);
        }

        void print(PrintWriter out, int num, String title) {
            out.println("\n#" + num + "a." + title);
            out.println("#Frequency\tGrapheme\tHex\tSample");
            for (R2<Long, String> item : entries()) {
                out.println(item.get0() 
                        + "\t" + item.get1() 
                        + "\t" + Utility.hex(item.get1(), " ") 
                        + "\t" + getSample(item.get1()));
            }
            out.println("\n#" + num + "b." + title);
            out.println("#Frequency\tOnset\tHex");
            for (R2<Long, String> item : termOnsets.targetMap.getEntrySetSortedByCount(false, collator)) {
                out.println(item.get0() 
                        + "\t" + item.get1() 
                        + "\t" + Utility.hex(item.get1(), " ")
                        + "\t" + termOnsets.sampleMap.get(item.get1()));
            }
            out.println("\n#" + num + "c." + title);
            out.println("#Frequency\tCoda\tHex");
            for (R2<Long, String> item : termCodas.targetMap.getEntrySetSortedByCount(false, collator)) {
                out.println(item.get0() 
                        + "\t" + item.get1() 
                        + "\t" + Utility.hex(item.get1(), " ")
                        + "\t" + termCodas.sampleMap.get(item.get1()));
            }
        }

    }
}
