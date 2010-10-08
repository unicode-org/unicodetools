package org.unicode.draft;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.FieldPosition;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Counter;

import sun.text.normalizer.UTF16;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;


public class Test2 {
    public static void main(String[] args) throws Exception {
        getCollationClasses();
        if (true) return;
        LinkedHashMap<Integer, Integer> s = getMatchingBraces(new LinkedHashMap<Integer, Integer>());
        for (int startChar : s.keySet()) {
            System.out.println(UTF16.valueOf(startChar) + "\t" + UTF16.valueOf(s.get(startChar)) + "\t" + UCharacter.getName(startChar));
        }
        verifyUtf8();
        getISOAliases();
        checkCharsets();

        trySortedList();
        if (true) return;
        System.out.println(new Date(2010-1900,12-1,10).getTime());
        tryPlural();

        ULocale myLocale = ULocale.FRANCE;
        Currency theCurrency = Currency.getInstance("USD");
        double number = 1234.56;

        NumberFormat formatter = NumberFormat.getCurrencyInstance(myLocale);
        formatter.setCurrency(theCurrency);
        System.out.println(formatter.format(number) + "\t" + number);

        Character.isLowSurrogate('a');
        testEnsurePlus();
        Font f;
        if (true) return;
        foo(Enum2.b);
        timeStrings();
        StringTransform t = Transliterator.getInstance("any-publishing");
        String result = t.transform("\"he said 'I won't, and can't!' as he stormed out--and slammed the door.\"");
        System.out.println(result);
    }

    private static void getCollationClasses() {
        Collator c = Collator.getInstance(ULocale.ROOT);
        c.setStrength(c.IDENTICAL);
        TreeSet<String> sorted = new TreeSet<String>(c);
        Normalizer2 nfd = Normalizer2.getInstance(null, "nfc", Mode.DECOMPOSE);
        for (int i = 1; i <= 0x10FFFF; ++i) {
            int type = UCharacter.getType(i);
            
            if (type == UCharacterCategory.UNASSIGNED || type == UCharacterCategory.PRIVATE_USE || type == UCharacterCategory.SURROGATE) {
                continue;
            }
            String s = UTF16.valueOf(i);
            if (!nfd.isNormalized(s)) {
                continue;
            }
            sorted.add(s);
        }
        int oldType = -1;
        UnicodeSet chars = new UnicodeSet();
        int count = 0;
        StringBuffer rules = new StringBuffer("& [last tertiary ignorable]\n");
        addChars(rules, "=", "ߺ");
        addChars(rules, "=", "ـ");
        addChars(rules, "&", "‖");
        addChars(rules, "<", "᧞");
        addChars(rules, "<", "᧟");
        addChars(rules, "&", "₸");
        addChars(rules, "<", "₨");
        addChars(rules, "<", "﷼");
        addChars(rules, "&", " ");
        rules.append("\t# gc=P, ordered by DUCET\n");
        final String punctStart = "<*\t\u200e'";
        final String punctEnd = "'\u200e";
        
        
        StringBuffer punctuation = new StringBuffer();
        int oldPunctuation = -1;
        for (String s : sorted) {
            int type = UCharacter.getType(s.codePointAt(0));
            switch (type) {
            case UCharacterCategory.UPPERCASE_LETTER:
            case UCharacterCategory.LOWERCASE_LETTER:
                type = UCharacterCategory.OTHER_LETTER;
                break;
            case UCharacterCategory.START_PUNCTUATION:
            case UCharacterCategory.END_PUNCTUATION:
            case UCharacterCategory.INITIAL_PUNCTUATION:
            case UCharacterCategory.FINAL_PUNCTUATION:
                type = UCharacterCategory.START_PUNCTUATION;
            case UCharacterCategory.OTHER_PUNCTUATION:
            case UCharacterCategory.CONNECTOR_PUNCTUATION:
            case UCharacterCategory.DASH_PUNCTUATION:
                if (punctuation.length() >= 50 || oldPunctuation != type && oldPunctuation != -1) {
                    rules.append(punctStart);
                    rules.append(punctuation);
                    rules.append(punctEnd);
                    if (oldPunctuation != -1) {
                        String punctLabel = type == UCharacterCategory.START_PUNCTUATION ? "Poc" 
                                : UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, oldPunctuation, NameChoice.SHORT);
                        rules.append(" \t# " + punctLabel);
                    }
                    rules.append("\n");
                    punctuation.setLength(0);
                }
                oldPunctuation = type;
                punctuation.append(s);
                break;
            }

            if (type != oldType) {
                if (oldType != -1) {
                    count += chars.size();
                    System.out.println(count + "\t"
                            + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, oldType, UProperty.NameChoice.SHORT)
                + "\t" + chars.size()
                            + "\t" + chars.toPattern(false));
                    chars.clear();
                }
                oldType = type;
            }
            chars.add(s);
        }
        count += chars.size();
        System.out.println(count + "\t"
                + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, oldType, UProperty.NameChoice.SHORT)
                + "\t" + chars.size()
                + "\t" + chars.toPattern(false));
        if (punctuation.length() > 0) {
            rules.append(punctStart);
            rules.append(punctuation);
            rules.append(punctEnd);
        }
        System.out.println(rules);
    }

    private static void addChars(StringBuffer punctuationChars, String relation, String s) {
        punctuationChars.append(relation +
        		"\t\u200E'" 
            + s 
            + "'\u200E" 
            + "\t# " 
            + UCharacter.getName(s, " + ")
            + "\n");
    }

    static final UnicodeSet INITIAL_PUNCTUATION = new UnicodeSet("[[:Ps:][:Pi:]-[༺༼᚛‘‚‛“„‟〝]]").freeze();
    private static <T extends Map<Integer, Integer>> T getMatchingBraces(T output) {
        for (String start : INITIAL_PUNCTUATION) {
            int startChar = start.codePointAt(0);
            String end = UCharacter.getStringPropertyValue(UProperty.BIDI_MIRRORING_GLYPH, startChar,
                    UProperty.NameChoice.SHORT);
            if (end == null) {
                continue;
            }
            output.put(startChar, end.codePointAt(0));
        }
        return output;
    }

    private static void verifyUtf8() {
        Charset utf8 = Charset.forName("UTF-8");
        // exhaustive brute force test of all sequences of 4 bytes
        for (long i = 0; i < 0xFFFFFFFFL; ++i) {
            if ((i % 1000000) == 0) {
                System.out.println(i);
            }
            for (int j = 1; j <= 4; ++j) {
                byte[] bytes = new byte[j];
                int values = (int)i;
                for (int k = 0; k < bytes.length; ++k) {
                    bytes[k] = (byte)values;
                    values >>= 8;
                }
                String s = new String(bytes, utf8);
                byte[] bytes2 = s.getBytes(utf8);
                boolean roundtrips = areEqual(bytes, bytes2);
                ByteString byteString = ByteString.copyFrom(bytes);
                if (isValidUtf8(byteString) != roundtrips) {
                    isValidUtf8(byteString);
                    System.out.println("Failed at " + Integer.toHexString((int)i));
                    return;
                }
            }
        }
        // TODO negatives
    }

    private static boolean areEqual(byte[] bytes, byte[] bytes2) {
        if (bytes.length != bytes2.length) return false;
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] != bytes2[i]) return false;
        }
        return true;
    }

    static class ByteString {
        byte[] bytes;
        
        public ByteString(byte[] bytes2) {
            bytes = bytes2;
        }

        public static ByteString copyFrom(final byte[] bytes) {
            return new ByteString(bytes);
        }

        public int size() {
            return bytes.length;
        }

        public byte byteAt(int index) {
            return bytes[index];
        }
        public boolean equals(Object other) {
            ByteString that = (ByteString) other;
            if (bytes.length != that.bytes.length) return false;
            for (int i = 0; i < bytes.length; ++i) {
                if (bytes[i] != that.bytes[i]) return false;
            }
            return true;
        }
        
        public int hashCode() {
            int value = bytes.length;
            for (int i = 0; i < bytes.length; ++i) {
                value *= 37;
                value += bytes[i];
            }
            return value;
        }
    }
    /**
     * Verifies correct UTF-8. Uses Table 3-7 of the Unicode Standard.
     * @param byteString
     * @return
     */
    private static boolean isValidUtf8(ByteString byteString) {
        int index = 0;
        int size = byteString.size();
        // To avoid the masking, we could change this to use bytes;
        // Then X > 0xC2 gets turned into X < -0xC2; X < 0x80 gets turned into X >= 0, etc.
        while (index < size) {
            int byte1 = byteString.byteAt(index++) & 0xFF;
            if (byte1 < 0x80) { // fast loop for single bytes
                continue;
                // we know from this point on that we have 2-4 byte forms
            } else if (byte1 < 0xC2 || byte1 > 0xF4) { // catch illegal first bytes: < C2 or > F4
                return false;
            }
            if (index >= size) { // fail if we run out of bytes
                return false;
            }
            int byte2 = byteString.byteAt(index++) & 0xFF;
            if (byte2 < 0x80 || byte2 > 0xBF) { // general trail-byte test
                return false;
            }
            if (byte1 <= 0xDF) { // two-byte form; general trail-byte test is sufficient
                continue;
            }
            // we know from this point on that we have 3 or 4 byte forms
            if (index >= size) { // fail if we run out of bytes
                return false;
            }
            int byte3 = byteString.byteAt(index++) & 0xFF;
            if (byte3 < 0x80 || byte3 > 0xBF) { // general trail-byte test
                return false;
            }
            if (byte1 <= 0xEF) { // three-byte form. Vastly more frequent than four-byte forms
                // The following has an extra test, but not worth restructuring  
                if (byte1 == 0xE0 && byte2 < 0xA0
                        || byte1 == 0xED && byte2 > 0x9F) { // check special cases of byte2
                    return false;
                }
            } else { // four-byte form
                if (index >= size) { // fail if we run out of bytes
                    return false;
                }
                int byte4 = byteString.byteAt(index++) & 0xFF;
                if (byte4 < 0x80 || byte4 > 0xBF) { // general trail-byte test
                    return false;
                }
                // The following has an extra test, but not worth restructuring  
                if (byte1 == 0xF0 && byte2 < 0x90
                        || byte1 == 0xF4 && byte2 > 0x8F) { // check special cases of byte2
                    return false;
                }
            }
        }
        return true;
    }

    private static void getISOAliases() throws IOException {
        InputStream input = Test2.class.getResourceAsStream("iana_character-sets.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        Map<String,Set<String>> data = new TreeMap(Collator.getInstance());
        Set<String> currentSet = new LinkedHashSet();
        String currentName = null;
        Matcher nameMatcher = Pattern.compile("(Alias|Name):\\s+(\\S+).*").matcher("");
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            if (line.startsWith("Name:")) {
                if (!nameMatcher.reset(line).matches()) {
                    throw new IllegalArgumentException(line);
                }
                if (currentName != null) {
                    currentSet.remove(currentName);
                    data.put(currentName, currentSet);
                    currentSet = new LinkedHashSet();
                }
                currentName = nameMatcher.group(2);
                currentSet.add(currentName);
            } else if (line.startsWith("Alias:")) {
                if (!nameMatcher.reset(line).matches()) {
                    throw new IllegalArgumentException(line);
                }
                String alias = nameMatcher.group(2);
                if (!alias.equals("None")) {
                    currentSet.add(alias);
                    if (line.contains("preferred")) {
                        currentName = alias;
                    }
                }
            }
        }
        for (String name : data.keySet()) {
            System.out.print(name);
            for (String alias : data.get(name)) {
                System.out.print("\t" + alias);
            }
            System.out.println("\t");
        }
    }

    private static void checkCharsets() {
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.limit(1);
        char[] returnChars = new char[10];
        CharBuffer returnCharBuffer = CharBuffer.wrap(returnChars);
        int[][] iso8859Results = new int[16][256];
        Counter<String>[] counter = new Counter[16];
        Map<String,String>[] samples = new HashMap[16];

        for (String iso8859 : charsets.keySet()) {
            if (!iso8859.startsWith("ISO-8859-")) {
                continue;
            }
            int numb = Integer.parseInt(iso8859.substring(9));
            iso8859Results[numb] = new int[256];
            counter[numb] = new Counter<String>();
            samples[numb] = new HashMap();
            fillArray(charsets, iso8859, byteBuffer, returnCharBuffer, returnChars, iso8859Results[numb]);
        }
        int[] array256 = new int[256];
        Set<String> seen = new HashSet();

        for (String name : charsets.keySet()) {
            if (!name.startsWith("windows-") || seen.contains(name)) {
                continue;
            }
            Charset charset = charsets.get(name);
            seen.addAll(charset.aliases());
            fillArray(charsets, name, byteBuffer, returnCharBuffer, returnChars, array256);
            StringBuilder sample = new StringBuilder();
            for (int numb = 0; numb < iso8859Results.length; ++numb) {
                if (counter[numb] == null) continue;
                int score = 0;
                sample.setLength(0);
                for (int j = 0; j < 256; ++j) {
                    if (iso8859Results[numb][j] == array256[j] || iso8859Results[numb][j] == -1) {
                        score++;
                    } else {
                        sample.append(Integer.toHexString(j).toUpperCase())
                        .append("-")
                        .appendCodePoint(iso8859Results[numb][j])
                        .append("-")
                        .appendCodePoint(array256[j])
                        .append(" ");
                    }
                }
                samples[numb].put(name, sample.toString());
                counter[numb].add(name, score);
            }
        }
        for (int numb = 0; numb < iso8859Results.length; ++numb) {
            if (counter[numb] == null) continue;
            for (String name : counter[numb].getKeysetSortedByCount(false)) {
                long score = counter[numb].get(name);
                if (score > 200) {
                    Charset charset = charsets.get(name);
                    System.out.println("iso-8859-" + numb + "\t" + score + "\t" + name + "\t" + charset.aliases() + "\t" + samples[numb].get(name));
                }
            }
            System.out.println();
        }
    }

    private static void fillArray(SortedMap<String, Charset> charsets, String iso8859, ByteBuffer byteBuffer, CharBuffer returnCharBuffer, char[] returnChars, int[] results) {
        int[] array256 = results;
        CharsetDecoder iso8859Decoder = getDecoder(charsets, iso8859);
        for (int i = 0x20; i < 256; ++i) {
            if (0x7E < i && i < 0xA0) continue;
            byteBuffer.clear();
            byteBuffer.put((byte)i);
            byteBuffer.flip();
            returnCharBuffer.clear();
            try {
                CoderResult decodeResult = iso8859Decoder.decode(byteBuffer, returnCharBuffer, true);
                array256[i] = decodeResult.isError() ? 0xFFFD : Character.codePointAt(returnChars, 0);
            } catch (Exception e) {
                array256[i] = -1;
            }
        }
    }

    static CharsetDecoder getDecoder(SortedMap<String, Charset> charsets, String name) {
        Charset charset = charsets.get(name);
        CharsetDecoder decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder;
    }



    private static void trySortedList() {
        ULocale[] list = {ULocale.ENGLISH, ULocale.FRENCH, ULocale.GERMAN};
        for (ULocale usersLocale : list) {

            System.out.println("\n*** User's Locale = " + usersLocale.getDisplayName(ULocale.ENGLISH) + "\n");

            TreeMap<String, String> mapNameToCode = new TreeMap<String, String>(com.ibm.icu.text.Collator.getInstance(usersLocale));

            for (String countryCode : ULocale.getISOCountries()) {
                countryCode = countryCode.toLowerCase(Locale.ENGLISH);
                String name = ULocale.getDisplayCountry("und-" + countryCode, usersLocale);
                mapNameToCode.put(name, countryCode);
            }

            assert(list.length == mapNameToCode.size());

            for (String name : mapNameToCode.keySet()) {
                String countryCode = mapNameToCode.get(name);
                String googleDomain = "http://www.google." + countryCode;
                // the above line is just a stand-in for getting the actual domain, such as google.co.uk
                System.out.println(" <a href='" + googleDomain + "/'>\n  " +
                        "<span class='asm'></span>" + googleDomain +
                        "<br>" + makeHtmlSafe(name) + "</a>");
            }
        }
    }

    private static String makeHtmlSafe(String name) {
        return name;
    }

    private static void tryPlural() {
        //MessageFormat mf = new MessageFormat("{0, plural, one {{1} rated this place} other {{1} and # others rated this place}}}");
        String[] patterns = {
                "{NUM_PEOPLE,choice,"
                + "0#nobody likes"
                + "|1#{PERSON} likes"
                + "|2#{PERSON} and {PERSON2} like"
                + "|3#{NUM_OTHERS, plural, one {{PERSON}} other {{PERSON}, {PERSON2} and # others}}"
                + "} this place.",
                "{NUM_OTHERS, plural, one {{PERSON}} other {{PERSON} and # others}} rated this place.",
                "There {NUM_PEOPLE,choice,0#are no files|1#is one file|1&lt;are {0,number,integer} files}.",

        };
        for (String pattern : patterns) {
            MessageFormat mf = new MessageFormat(pattern);
            mf.setLocale(ULocale.ENGLISH);
            Map<String,Object> arguments = new HashMap<String,Object>();
            arguments.put("PERSON", "John");
            arguments.put("PERSON2", "Mary");
            StringBuffer result = new StringBuffer();
            FieldPosition pos = new FieldPosition(0);// what is FP for?
            for (int i = 0; i < 5; ++i) {
                arguments.put("NUM_PEOPLE", i);
                arguments.put("NUM_OTHERS", 3);
                arguments.put("0", i);
                result.setLength(0);
                StringBuffer formatted = mf.format(arguments, result, null);
                System.out.println(i + "\t" + formatted);
            }

        }
        for (ULocale locale : PluralRules.getAvailableULocales()) {
            PluralRules pr = PluralRules.forLocale(locale);
            if (!locale.equals(pr.getFunctionalEquivalent(locale, null))) {
                continue;
            }
            Set<String> keywords = pr.getKeywords();
            System.out.println(locale + "\t" + keywords);
        }
    }

    private static void testEnsurePlus() {
        for (ULocale locale : NumberFormat.getAvailableULocales()) {
            if (locale.getCountry().length() > 0) {
                continue; // skip country locales
            }
            NumberFormat nf = NumberFormat.getPercentInstance(locale);
            nf.setMinimumFractionDigits(2);
            String oldPositive = nf.format(0.1234);
            String oldNegative = nf.format(-0.1234);
            nf = ensurePlus(nf);
            String newPositive = nf.format(0.1234);
            System.out.println(locale + "\t" + locale.getDisplayName(ULocale.ENGLISH)
                    + "\t" + newPositive
                    + "\t" + oldPositive
                    + "\t" + oldNegative
            );
        }
    }

    private static NumberFormat ensurePlus(NumberFormat numberFormat) {
        DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
        String positivePrefix = decimalFormat.getPositivePrefix();
        String positiveSuffix = decimalFormat.getPositiveSuffix();
        if (!positivePrefix.contains("+") && !positiveSuffix.contains("+")) {
            decimalFormat.setPositivePrefix("+" + positivePrefix);
        }
        return decimalFormat;
    }

    static class Foo extends AbstractList<String> {

        @Override
        public String get(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }


    }

    public LanguageCode fromOther(ULocale uLocale) {
        // for invalid codes, get the best fit.
        while (true) {
            try {
                return LanguageCode.forString(uLocale.toLanguageTag());
            } catch (InvalidLanguageCode e) {
                uLocale = uLocale.getFallback();
                // keep going until we fail completely
                if (uLocale == null) {
                    throw e; 
                }
            }
        }
    }

    public LanguageCode fromOther(Locale locale) {
        // for invalid codes, get the best fit.
        String localeString = locale.toString();
        while (true) {
            try {
                return LanguageCode.forString(localeString);
            } catch (InvalidLanguageCode e) {
                // keep going until we fail completely
                int lastUnder = localeString.lastIndexOf('_');
                if (lastUnder < 0) {
                    throw e; 
                }
                localeString = localeString.substring(0,lastUnder);
            }
        }
    }

    static void timeStrings() {
        List<CharSequence> data1 = new ArrayList<CharSequence>();
        List<CharSequence> data2 = new ArrayList<CharSequence>();
        for (String lang : ULocale.getISOLanguages()) {
            data1.add(ULocale.getDisplayName(lang, ULocale.ENGLISH));
            for (String reg : ULocale.getISOCountries()) {
                data1.add(ULocale.getDisplayName(lang + "_" + reg, ULocale.ENGLISH));
            }
        }
        for (CharSequence a : data1) {
            data2.add(new Hasher(a));
        }
        HashSet<CharSequence> s1 = new HashSet<CharSequence>();

        double time1 = time(s1, data1, 100);
        System.out.println("Hash\t" + time1);
        double time2 = time(s1, data2, 100);
        System.out.println("Hash\t" + time2 + "\t\t" + 100*time2/time1 + "%");
    }

    static class Hasher implements CharSequence, Comparable<CharSequence> {
        CharSequence s;
        int hashCode;
        Hasher(CharSequence s) {
            this.s = s;
            hashCode = s.hashCode();
        }
        public char charAt(int index) {
            return s.charAt(index);
        }
        public int compareTo(CharSequence other) {
            int length = s.length();
            int diff = length - other.length();
            if (diff != 0) return diff;
            for (int i = 0; i < length; ++i) {
                diff = s.charAt(i) - other.charAt(i);
                if (diff != 0) return diff;
            }
            return 0;
        }

        public int compareTo(Hasher other) {
            return compareTo(other.s);
        }

        public boolean equals(CharSequence anObject) {
            return s.equals(anObject);
        }
        public int hashCode() {
            return s.hashCode();
        }
        public int length() {
            return s.length();
        }
        public CharSequence subSequence(int start, int end) {
            return s.subSequence(start, end);
        }
    };

    private static double time(Set<CharSequence> s1, List<CharSequence> data, int count) {
        double start = System.currentTimeMillis();
        boolean in = false;
        for (int i = count; i > 0; --i) {
            s1.clear();
            s1.addAll(data);
            for (CharSequence item : data) {
                in ^= s1.contains(item);
            }
        }
        return (System.currentTimeMillis() - start)/count;
    }
    static class LanguageCode {

        public static LanguageCode forString(String string) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    enum Enum1 {a, b, c}
    enum Enum2 {b, c, d}
    static void foo(Enum x) {
        System.out.println(x.compareTo(Enum2.b));
        System.out.println(x.name());
        System.out.println(x.ordinal());
        System.out.println(Enum1.valueOf(Enum1.a.name()));
        System.out.println(Arrays.asList(Enum1.values()));
        System.out.println(Enum.valueOf(Enum2.class, "b"));
    }

    static class InvalidLanguageCode extends RuntimeException {

    }

}
