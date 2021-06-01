package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class Cmudict {
    static final String BASE_DIR = Settings.UnicodeTools.DATA_DIR + "/translit/";
    static final Collator col = Collator.getInstance(ULocale.ROOT);
    //static final StressFixer stressFixer = new StressFixer();
    static final Transliterator arpabet = getTransliteratorFromFile("arpabet-ipa", BASE_DIR, "arpabet-ipa.txt");
    static final Transliterator respell = getTransliteratorFromFile("ipa-en", BASE_DIR, "respell.txt");

    public static void main(String[] args) throws IOException {

        final UnicodeSet SKIP_START = new UnicodeSet("[^a-z’]").freeze();
        final UnicodeSet WORD_OK = new UnicodeSet("[-.a-z’¹²³\\u0020]").freeze();
        final Set<String> funnyWords = new TreeSet<String>();

        final Relation<String,String> toIPA = new Relation(new TreeMap<String,String>(col), LinkedHashSet.class);
        final Relation<String,String> fromIpa = new Relation(new TreeMap(col), TreeSet.class);
        final Set<String> ipaWithoutStress = new TreeSet<String>(col);
        final Relation<String,String> ipaDifferingByStress = new Relation(new TreeMap(col), TreeSet.class);

        final BufferedReader in = FileUtilities.openUTF8Reader(BASE_DIR, "cmudict.0.7a.txt");
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1); // remove BOM
            }
            if (line.startsWith(";;;")) {
                continue;
            }
            //            if (SKIP_START.contains(line.codePointAt(0))) {
            //                System.out.println("*SKIPPING: " + line);
            //                continue;
            //            }
            final int wordEnd = line.indexOf(' ');
            final String word = line.substring(0,wordEnd).toLowerCase(Locale.ENGLISH)
                    .replace('\'', '’')
                    .replace('_', ' ')
                    .replace("(1)","") // ¹
                    .replace("(2)","") // ²
                    .replace("(3)","") // ³
                    ;
            if (!WORD_OK.containsAll(word) || SKIP_START.contains(word.codePointAt(0))
                    || word.startsWith("’") && word.contains("quote")) {
                funnyWords.add(word);
                continue;
            }
            final String pronunciation = line.substring(wordEnd+1);
            final String ipa = getIpa(pronunciation);
            fromIpa.put(ipa, word);
            toIPA.put(word, ipa);
            if (ipa.contains("\uFFFD")) {
                System.out.print("");
            }
            if (!ipa.contains("ˈ")) {
                ipaWithoutStress.add(ipa);
            }
            final String stresslessIpa = ipa.replace("ˈ","").replace("ˌ","");
            ipaDifferingByStress.put(stresslessIpa, ipa);
        }
        in.close();
        System.out.println("*Skipped Words:\t" + funnyWords);

        //        System.out.println("*No Stress");
        //
        //        for (String ipa : ipaWithoutStress) {
        //            System.out.println(ipa + "\t" + homonyms.get(ipa));
        //        }
        //
        //        System.out.println("*Only by Stress");
        //
        //        for (Entry<String, Set<String>> ipaWoAndW : ipaDifferingByStress.entrySet()) {
        //            Set<String> values = ipaWoAndW.getValue();
        //            if (values.size() == 1) {
        //                continue;
        //            }
        //            for (String ipa : values) {
        //                System.out.print(ipa + "\t" + homonyms.get(ipa) + "\t;\t");
        //            }
        //            System.out.println();
        //        }
        //
        //        for (Entry<String, Set<String>> ipaAndWords : homonyms.entrySet()) {
        //            String ipa = ipaAndWords.getKey();
        //            String result = stressFixer.fix(ipa);
        //            if (!ipa.equals(result)) {
        //                System.out.println("IPA simplified:\t" + ipa + "\t" + result);
        //            }
        //        }


        System.out.println("Post-processing");
        final Set<String> removals = new HashSet<String>();
        final Relation<String,String> specials = new Relation(new TreeMap(col), LinkedHashSet.class);

        for (final Entry<String, Set<String>> entry : toIPA.keyValuesSet()) {
            final String word = entry.getKey();
            final boolean startsWith = word.startsWith("’");
            final boolean endsWith = word.endsWith("’");
            if (!startsWith && !endsWith) {
                continue;
            }
            final Set<String> values = entry.getValue();
            String newWord = word;
            if (startsWith) {
                newWord = newWord.substring(1);
            }
            if (endsWith) {
                newWord = newWord.substring(0, newWord.length()-1);
            }
            final Collection<String> values2 = toIPA.get(newWord);
            if (values2 == null) {
                specials.putAll(newWord, values);
            } else if (values.equals(values2)) {
                // System.out.println("Values Match:\t" + word + "\t" + values + "\t" + values2);
                removals.add(word);
            } else {
                System.out.println("Values Differ:\t" + word + "\t" + values + "\t" + newWord + "\t" + values2);
            }
        }
        toIPA.removeAll(removals);
        for (final Entry<String, Set<String>> entry : specials.keyValuesSet()) {
            System.out.println("Missing?\t" + entry);
        }

        PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "/translit/", "cmudict.txt") ;
        for (final Entry<String, Set<String>> entry : toIPA.keyValuesSet()) {
            final String word = entry.getKey();
            final Set<String> values = entry.getValue();
            out.println(word + "\t→\t" + showIpa(values));
        }
        out.close();

        out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "/translit/", "homonyms.txt") ;
        final Set<String> temp = new TreeSet(col);
        for (final Entry<String, Set<String>> entry : fromIpa.keyValuesSet()) {
            final Set<String> values = entry.getValue();
            if (values.size() == 1) {
                continue;
            }
            temp.clear();
            for (String value : values) {
                value = value.replace("’","");
                temp.add(value);
            }
            if (temp.size() == 1) {
                continue;
            }
            out.println(respell.transform(entry.getKey()) + "\t" + values);
        }
        out.close();
        final Map<String, String> reverseIpa = new TreeMap<String, String>(col);
        final Map<String, String> checkRespellRoundtrip = new TreeMap<String, String>(col);

        for (final Entry<String, Set<String>> entry : fromIpa.keyValuesSet()) {
            final String ipa = entry.getKey();
            final String respelled = respell.transform(ipa);
            final String old = checkRespellRoundtrip.get(respelled);
            if (old != null) {
                System.out.println("*Collision:\t" + respelled + "\t" + ipa + "\t" + old);
            } else {
                checkRespellRoundtrip.put(respelled, ipa);
            }
            final String respelledKey = respell.transform(ipa);
            final String reversedRespelledKey = reverse(respelledKey, RESPELL_UNITS);
            if (reverseIpa.containsKey(reversedRespelledKey)) {
                final String otherIpa = reverseIpa.get(reversedRespelledKey);
                final String respelledOtherIpa = respell.transform(otherIpa);
                final String reversedRespelledOtherIpa = respell.transform(respelledOtherIpa);
                System.out.println("Collision:"
                        + "\t" + ipa
                        + "\t" + respelledKey
                        + "\t" + reversedRespelledKey
                        + "\t" + otherIpa
                        + "\t" + respelledOtherIpa
                        + "\t" + reversedRespelledOtherIpa
                        );
            }
            reverseIpa.put(reversedRespelledKey, ipa);
        }

        out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "/translit/", "reversed.txt");
        for (final Entry<String, String> reversed_normal : reverseIpa.entrySet()) {
            final String original = reversed_normal.getValue();
            out.println(CollectionUtilities.join(fromIpa.get(original), ", ") + "\t{"
                    // + reversed_normal.getKey() + ", "
                    + original + "}");
        }
        out.close();

    }

    static UnicodeSet IPA_UNITS = new UnicodeSet("[{aɪ}{aʊ}{ɔɪ}{tʃ}{dʒ}]").freeze();
    static UnicodeSet RESPELL_UNITS = new UnicodeSet("[{ùr}{òu}{òi}{cħ}{dʒ}{tħ}{ţħ}{sħ}{nġ}]").freeze();

    private static String reverse(String sourceString, UnicodeSet units) {
        final StringBuilder result = new StringBuilder();
        final StringBuilder temp = new StringBuilder(sourceString);
        for (int i = 0; i < temp.length(); ++i) {
            final int matchValue = units.matchesAt(temp, i);
            if (matchValue > i) {
                final char ch1 = temp.charAt(i);
                final char ch2 = temp.charAt(i+1);
                temp.setCharAt(i, ch2);
                temp.setCharAt(i+1, ch1);
            }
        }
        // pass through and reverse duals
        // chars-only ok
        for (int i = temp.length() - 1; i >= 0; --i) {
            final char ch = temp.charAt(i);
            result.append(ch);
        }
        return result.toString();
    }

    private static String getIpa(String pronunciation) {
        String ipa = arpabet.transform(pronunciation);
        ipa = ipa.replace(""+PRIMARY_STRESS, "").replace(""+SECONDARY_STRESS, "");
        return ipa;
    }

    private static String showIpa(Set<String> values) {
        if (values.size() == 1) {
            final String value = values.iterator().next();
            final String respelled = respell.transform(value);
            return value + " (" + respelled + ")";
        }
        String result = "{";
        for (final String value : values) {
            if (result.length() > 1) {
                result += ", ";
            }
            final String respelled = respell.transform(value);
            result += value + " (" + respelled + ")";
        }
        return result + "}";
    }

    public static Transliterator getTransliteratorFromFile(String ID, String dir, String file) {
        try {
            final BufferedReader br = FileUtilities.openUTF8Reader(dir, file);
            final StringBuffer input = new StringBuffer();
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("\uFEFF"))
                {
                    line = line.substring(1); // remove BOM
                }
                input.append(line);
                input.append('\n');
            }
            return Transliterator.createFromRules(ID, input.toString(), Transliterator.FORWARD);
        } catch (final IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't open transliterator file " + file).initCause(e);
        }
    }

    static final UnicodeSet VOWELS = new UnicodeSet("[æ {aɪ} {aʊ} ɑ e ə ɛ ɚ i ɪ o {ɔɪ} u ʊ]");
    private static final char SECONDARY_STRESS = 'ˌ';
    private static final char PRIMARY_STRESS = 'ˈ';
}
//    static final class StressFixer {
//        int vowelCount;
//        int primaryCount;
//        int secondaryCount;
//        int firstSecondary;
//        int firstVowel;
//        String input;
//
//        String fix(String input) {
//            // with one syllable, there should be no stress marks
//            // with two, exactly one primary stress mark
//            // with more, exactly one primary stress mark, optionally one secondary stress mark
//
//            vowelCount = 0;
//            primaryCount = 0;
//            secondaryCount = 0;
//            firstSecondary = -1;
//            firstVowel = -1;
//            this.input = input;
//
//            for (int offset = 0; offset < input.length(); ++offset) {
//                char ch = input.charAt(offset); // chars ok, only BMP
//                if (ch == PRIMARY_STRESS) {
//                    if (primaryCount == 1) {
//                        System.out.println("* Too many primaries\t" + input);
//                        ch = 'ˌ'; // remap
//                        input = input.substring(0,offset) + ch + input.substring(offset+1);
//                    } else {
//                        primaryCount++;
//                        continue;
//                    }
//                }
//                if (ch == SECONDARY_STRESS) {
//                    if (firstSecondary < 0) {
//                        firstSecondary = offset;
//                    }
//                    secondaryCount++;
//                    continue;
//                }
//                int endOffset = VOWELS.matchesAt(input, offset);
//                if (endOffset > offset) {
//                    if (firstVowel < 0) {
//                        firstVowel = offset;
//                    }
//                    vowelCount++;
//                    offset = endOffset - 1;
//                }
//            }
//            String result = input;
//            switch (vowelCount) {
//            case 0:
//                result = input;
//                System.out.println("* No vowels\t" + input);
//                break;
//            case 1:
//                result = input.replace("ˈ","").replace("ˌ", "");
//                break;
//            case 2:
//                if (primaryCount < 1) {
//                    addPrimary();
//                }
//                result = input.replace("ˌ", "");
//                break;
//            case 3:
//                if (primaryCount < 1) {
//                    addPrimary();
//                }
//                // reduce secondary count to 1
//                if (secondaryCount > 1) {
//                    result = input;
//                    boolean haveOne = false;
//                    for (int offset = 0; offset < input.length(); ++offset) {
//                        char ch = input.charAt(offset); // chars ok, only BMP
//                        if (ch == 'ˌ') {
//                            if (!haveOne) {
//
//                                haveOne = true;
//                            }
//                        } else {
//                            result += ch;
//                        }
//                    }
//                }
//                return result;
//            }
//
//            private void addPrimary() {
//                System.out.println("* Too few primaries\t" + input);
//                if (firstSecondary >= 0) {
//                    --secondaryCount;
//                    input = input.substring(0,firstSecondary) + 'ˌ' + input.substring(firstSecondary+1);
//                } else {
//                    input = input.substring(0,firstVowel) + 'ˌ' + input.substring(firstVowel);
//                }
//                ++primaryCount;
//            }
//        }
//    }
