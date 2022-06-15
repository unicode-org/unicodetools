package org.unicode.draft;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeCompressor;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.Counter;

public class CheckResources {

    public static void main(String[] args) throws UnsupportedEncodingException {
        for (final String baseName : getBaseNames()) {
            gatherData(baseName);
        }
        System.out.println("***DATA COUNTER");
        printData(counter, false);
        System.out.println("***Data COUNTER - SHARING");
        printDataCompressed(counter, false);
        System.out.println("***KEY COUNTER");
        printData(keyCounter, false);
        System.out.println("***KEY COUNTER - SHARING");
        printDataCompressed(keyCounter, false);
    }

    // Ugly hack to get base names
    static Collection<String> getBaseNames() {
        return new LinkedHashSet<String>(
                Arrays.asList(
                        new String[] {
                            ICUData.ICU_BASE_NAME,
                            ICUData.ICU_BRKITR_BASE_NAME,
                            ICUData.ICU_COLLATION_BASE_NAME,
                            ICUData.ICU_RBNF_BASE_NAME,
                            ICUData.ICU_TRANSLIT_BASE_NAME
                        }));
    }

    private static void gatherData(String baseName) {
        ULocale[] availableULocales;
        try {
            availableULocales =
                    ICUResourceBundle.getAvailableULocales(
                            baseName, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        } catch (final Exception e) {
            e.printStackTrace();
            System.out.println("*** Unable to load " + baseName);
            return;
        }
        System.out.println("Gathering data for: " + baseName);
        for (final ULocale locale : availableULocales) {
            final UResourceBundle rs = UResourceBundle.getBundleInstance(baseName, locale);
            addStrings(rs);
        }
    }

    private static void addStrings(UResourceBundle rs) {
        final String key = rs.getKey();
        if (key != null) {
            keyCounter.add(key, 1);
        }
        switch (rs.getType()) {
            case UResourceBundle.STRING:
                counter.add(rs.getString(), 1);
                break;
            case UResourceBundle.ARRAY:
            case UResourceBundle.TABLE:
                for (int i = 0; i < rs.getSize(); ++i) {
                    final UResourceBundle rs2 = rs.get(i);
                    addStrings(rs2);
                }
                break;
            case UResourceBundle.BINARY:
            case UResourceBundle.INT:
            case UResourceBundle.INT_VECTOR: // skip
                break;
            default:
                throw new IllegalArgumentException("Unknown Option: " + rs.getType());
        }
    }

    private static void printData(Counter<String> counter2, boolean showKeys)
            throws UnsupportedEncodingException {
        long totalUtf16Size = 0;
        long totalUtf8Size = 0;
        long totalScsuSize = 0;
        long uniqueUtf16Size = 0;
        long uniqueUtf8Size = 0;
        long uniqueScsuSize = 0;

        for (final String key : counter2.getKeysetSortedByCount(false)) {
            final long count = counter2.getCount(key);
            if (showKeys) {
                final String trunc = key.length() < 20 ? key : key.substring(0, 19) + "...";
                System.out.println(count + "\t" + trunc);
            }
            final long utf16Length = (key.length() + 1) * 2;
            uniqueUtf16Size += utf16Length;
            totalUtf16Size += utf16Length * count;

            final long utf8Length = key.getBytes("utf-8").length + 1;
            totalUtf8Size += utf8Length * count;
            uniqueUtf8Size += utf8Length;

            final long scsuLength = UnicodeCompressor.compress(key).length + 1;
            totalScsuSize += scsuLength * count;
            uniqueScsuSize += scsuLength;
        }

        System.out.println("Total Count:\t" + nf.format(counter2.getItemCount()));

        System.out.println("Total Size (UTF-16):\t" + nf.format(totalUtf16Size) + " bytes");
        System.out.println("Total Size (UTF-8):\t" + nf.format(totalUtf8Size) + " bytes");
        System.out.println("Total Size (SCSU):\t" + nf.format(totalScsuSize) + " bytes");

        System.out.println("Total Unique Size (UTF-16):\t" + nf.format(uniqueUtf16Size) + " bytes");
        System.out.println("Total Unique Size (UTF-8):\t" + nf.format(uniqueUtf8Size) + " bytes");
        System.out.println("Total Unique Size (SCSU):\t" + nf.format(uniqueScsuSize) + " bytes");
    }

    private static void printDataCompressed(Counter<String> counter2, boolean show)
            throws UnsupportedEncodingException {
        long uniqueUtf8Size = 0;
        long savedUtf8Size = 0;
        long savedUtf8SingleSize = 0;

        final Counter<Character> charCount = new Counter<Character>();

        final Set<String> count_key = new TreeSet<String>(REVERSE);
        count_key.addAll(counter2.keySet());
        String lastKey = "";
        for (final String key : count_key) {
            final long count = counter2.getCount(key);
            final String trunc = key.length() < 20 ? key : key.substring(0, 19) + "...";
            if (show) {
                System.out.print(count + "\t" + trunc);
            }

            countChars(charCount, key, count);

            final long utf8Length = key.getBytes("utf-8").length + 1;
            uniqueUtf8Size += utf8Length;
            if (false && key.length() == 1 && key.charAt(0) < 0x7F) {
                savedUtf8SingleSize += utf8Length;
                if (show) {
                    System.out.print("\tSKIP SINGLE");
                }
            } else if (key.regionMatches(
                    0, lastKey, lastKey.length() - key.length(), key.length())) {
                savedUtf8Size += utf8Length;
                if (show) {
                    System.out.print("\tSKIP");
                }
            } else {
                lastKey = key;
            }
            if (show) {
                System.out.println();
            }
        }
        System.out.println("Total Unique Size:\t" + nf.format(uniqueUtf8Size) + " bytes");
        System.out.println(
                "Total Saved Shared Suffix Size:\t"
                        + nf.format(savedUtf8Size)
                        + " bytes\t"
                        + savedUtf8Size / (double) (uniqueUtf8Size));
        System.out.println(
                "Total Saved Singleton Size:\t"
                        + nf.format(savedUtf8SingleSize)
                        + " bytes\t"
                        + savedUtf8SingleSize / (double) (uniqueUtf8Size));

        System.out.println("Character Frequencies");
        int i = 0;
        for (final Character key : charCount.getKeysetSortedByCount(false)) {
            final long count = charCount.getCount(key);
            System.out.println(
                    ++i
                            + "\t"
                            + count
                            + "\t"
                            + key
                            + "\t"
                            + Utility.hex(key)
                            + (alphanum.contains(key) ? "" : "\t!Alphanum"));
            if (i > 99) {
                break;
            }
        }
    }

    static UnicodeSet alphanum = new UnicodeSet("[[:L:][:Nd:]]");

    private static void countChars(Counter<Character> charCount, String key, long value) {
        for (int i = 0; i < key.length(); ++i) {
            charCount.add(key.charAt(i), value);
        }
    }

    static Counter<String> counter = new Counter<String>();
    static Counter<String> keyCounter = new Counter<String>();
    static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);

    static {
        nf.setGroupingUsed(true);
    }

    static class SortReverseLengthFirst implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            int p1 = o1.length();
            int p2 = o2.length();
            while (true) {
                final int ch1 = p1 <= 0 ? 0x10000 : o1.charAt(--p1);
                final int ch2 = p2 <= 0 ? 0x10000 : o2.charAt(--p2);
                final int diff = ch1 - ch2;
                if (diff != 0) {
                    return diff;
                }
                if (ch1 == 0x10000) {
                    return 0;
                }
            }
        }
    }

    static Comparator<String> REVERSE = new SortReverseLengthFirst();
}
