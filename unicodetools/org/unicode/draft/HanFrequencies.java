package org.unicode.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class HanFrequencies {

    public static final UnicodeSet HAN = new UnicodeSet("[[:ideographic:][:sc=han:]]");

    public static void main(String[] args) throws IOException {
        for (final String s : args) {
            if ("-f".equals(s)) {
                generateFrequencies();
            } else if ("-r".equals(s)) {
                generateReadings();
            }
        }
    }

    private static void generateReadings() throws IOException {
        final BufferedReader freq = FileUtilities.openUTF8Reader(Settings.Output.GEN_DIR + "/hanfrequency", "unifiedZh.txt");
        final UnicodeMap<Integer> rank = new UnicodeMap<Integer>();
        int count = 0;
        while (true) {
            final String line = freq.readLine();
            if (line == null) {
                break;
            }
            final String[] parts = line.split("\t");
            // 的    s   1   t   1
            rank.put(parts[0], ++count);
        }
        freq.close();
        final BufferedReader readings = FileUtilities.openUTF8Reader(Settings.UnicodeTools.DATA_DIR + "/frequency", "han-reading-diff.txt");
        final Set<R2<Integer, Map<ReadingRows,String>>> ordered = new TreeSet<R2<Integer,Map<ReadingRows,String>>>();
        while (true) {
            String line = readings.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            line = line; // just to make spreadsheets happy with the hex.
            final String[] parts = line.split("\t");
            //4F3D    伽   jiā;    gā; jiā x   # std=old
            try {
                Integer rankValue = rank.get(parts[1]);
                if (rankValue == null) {
                    rankValue = ++count; // next value
                    System.out.println("Missing rank: " + line);
                }
                if (parts[1].codePointAt(0) != Integer.parseInt(parts[0],16) || parts.length != 7) {
                    throw new IllegalArgumentException();
                }
                final Map<ReadingRows,String> values = new TreeMap<ReadingRows,String>();
                values.put(ReadingRows.hex, "x" + parts[0]);
                values.put(ReadingRows.character, parts[1]);
                values.put(ReadingRows.oldVal, parts[2]);
                values.put(ReadingRows.newVal, parts[3]);
                values.put(ReadingRows.CN, parts[4]);
                final String[] alt = parts[5].split(":");

                final ReadingAlt type = alt[0].equals("?") ? ReadingAlt.x : ReadingAlt.valueOf(alt[0]);
                values.put(ReadingRows.TW, type == ReadingAlt.tw ? alt[1] : "");
                values.put(ReadingRows.name, type == ReadingAlt.nm ? alt[1] : "");
                values.put(ReadingRows.combo, type == ReadingAlt.co ? alt[1] : "");
                String comment = parts[6];
                if (comment.startsWith("# ")) {
                    comment = comment.substring(2);
                } else if (comment.equals("#")) {
                    comment = "";
                }
                values.put(ReadingRows.comment, comment);

                final R2<Integer, Map<ReadingRows,String>> row = Row.of(rankValue, values);
                ordered.add(row);
            } catch (final Exception e) {
                throw new IllegalArgumentException(line, e);
            }
        }
        readings.close();

        final PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "/hanfrequency", "han-reading-diff.html");
        //PrintStream out = System.out;
        final ReadingRows[] values = ReadingRows.values();
        out.println("<html><body><table>");
        for (final R2<Integer, Map<ReadingRows,String>> entry : ordered) {
            out.print("<tr><td>");
            out.print(entry.get0());
            for (final ReadingRows heading : values) {
                out.print("</td><td>");
                String column = entry.get1().get(heading);
                column = column.replace(";","");
                out.print(column);
            }
            out.println("</td></tr>");
        }
        out.println("</table></body></html>");
        out.close();
    }

    enum ReadingRows {hex, character, oldVal, newVal, CN, TW, name, combo, comment}
    enum ReadingAlt {x, co, nm, tw}

    //    #   tw: - alt reading for TW (not Hant; in HK,MO this reading is not relevant)
    //    #   nm: - alt reading for names (personal or geographic)
    //    #   co: - alt non-name reading only used in combinations


    private static void generateFrequencies() {
        final Set<String> languages = CharacterFrequency.getLanguagesWithCounter();
        showInterleaved();
        //System.out.println(languages);
        show("zh");
        show("zh-Hant");
        show("ja");
        show("ko");
        show("mul");
    }

    private static void showInterleaved() {
        final PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(Settings.Output.GEN_DIR + "/hanfrequency",
                "unifiedZh.txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);

        final LinkedHashMap<String, Integer> rank1 = getFilteredList("zh");
        final Iterator<Entry<String, Integer>> it1 = rank1.entrySet().iterator();
        final LinkedHashMap<String, Integer> rank2 = getFilteredList("zh-Hant");
        final Iterator<Entry<String, Integer>> it2 = rank2.entrySet().iterator();

        final HashSet<String> alreadyDone = new HashSet<String>();

        while (true) {
            final Entry<String, Integer> item1 = writeItem(out, it1, rank2, alreadyDone, "st");
            final Entry<String, Integer> item2 = writeItem(out, it2, rank1, alreadyDone, "ts");
            if (item1 == null && item2 == null) {
                break;
            }
        }
        out.close();
    }

    private static Entry<String, Integer> writeItem(PrintWriter out, Iterator<Entry<String, Integer>> it1, LinkedHashMap<String, Integer> otherRank, HashSet<String> alreadyDone, String titles) {
        final Entry<String, Integer> entry1 = it1.hasNext() ? it1.next() : null;

        if (entry1 != null) {
            final String item1 = entry1.getKey();
            if (!alreadyDone.contains(item1)) {
                final Integer otherValue = otherRank.get(item1);
                out.println(item1
                        + "\t" + titles.charAt(0) + "\t" + entry1.getValue()
                        + "\t" + titles.charAt(1) + "\t" + (otherValue == null ? "-" : otherValue.toString()));
                alreadyDone.add(item1);
            }
        }
        return entry1;
    }

    private static LinkedHashMap<String, Integer> getFilteredList(String locale) {
        final Counter<Integer> counter1 = CharacterFrequency.getCodePointCounter(locale, true);
        final LinkedHashMap<String,Integer> list1 = new LinkedHashMap<String,Integer>();
        int rank = 0;
        for (final Integer item : counter1.getKeysetSortedByCount(false)) {
            if (HAN.contains(item)) {
                list1.put(UTF16.valueOf(item), ++rank);
            }
        }
        return list1;
    }

    private static void show(String locale) {
        System.out.println("Writing:\t" + locale);
        final PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(Settings.Output.GEN_DIR + "/hanfrequency",
                locale + ".txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
        final Counter<Integer> counter = CharacterFrequency.getCodePointCounter(locale, true);
        long total = 0;
        for (final Integer item : counter) {
            if (!HAN.contains(item)) {
                continue;
            }
            total += counter.get(item);
        }
        final long countLimit = (long)(total * 0.999995d);
        final UnicodeSet currentSet = new UnicodeSet();
        int setCount = 0;
        long runningTotal = 0;
        final int chunkLimit = 1000;
        for (final Integer item : counter.getKeysetSortedByCount(false)) {
            if (!HAN.contains(item)) {
                continue;
            }
            final long count = counter.get(item);
            runningTotal += count;
            currentSet.add(item);
            if (currentSet.size() >= chunkLimit) {
                setCount += currentSet.size();
                out.println(setCount + "\t" + (runningTotal/(double)total) + "\t" + currentSet.toPattern(false));
                out.flush();
                System.out.print(".");
                currentSet.clear();
            }
            if (runningTotal > countLimit) {
                break;
            }
        }
        System.out.println();
        out.close();
    }
}
