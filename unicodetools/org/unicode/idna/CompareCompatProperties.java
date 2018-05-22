package org.unicode.idna;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.draft.FileUtilities;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CompareCompatProperties {
    /*
# Column 1: source -          The source string to be tested
# Column 2: toUnicode -       The result of applying toUnicode to the source, with Transitional_Processing=false.
#                             A blank value means the same as the source value.
# Column 3: toUnicodeStatus - A set of status codes, each corresponding to a particular test.
#                             A blank value means [] (no errors).
# Column 4: toAsciiN -        The result of applying toASCII to the source, with Transitional_Processing=false.
#                             A blank value means the same as the toUnicode value.
# Column 5: toAsciiNStatus -  A set of status codes, each corresponding to a particular test.
#                             A blank value means the same as the toUnicodeStatus value. An explicit [] means no errors.
# Column 6: toAsciiT -        The result of applying toASCII to the source, with Transitional_Processing=true.
#                             A blank value means the same as the toAsciiN value.
# Column 7: toAsciiTStatus -  A set of status codes, each corresponding to a particular test.
#                             A blank value means the same as the toAsciiNStatus value. An explicit [] means no errors.
     */

    enum Column {
        source,
        toUnicode(source),
        toUnicodeStatus,
        toAsciiN(toUnicode),
        toAsciiNStatus(toUnicodeStatus),
        toAsciiT(toAsciiN),
        toAsciiTStatus(toAsciiNStatus),
        EOL;

        Column getFrom;

        private Column() {
            this(null);
        }
        private Column(Column getFrom) {
            this.getFrom = getFrom;
        }
        public Column next() {
            return values()[ordinal()+1];
        }
        public Column addFixedAndNext(String item, List<String> lineParts) {
            if (item.isEmpty()) {
                if (getFrom != null) {
                    item = lineParts.get(getFrom.ordinal());
                } else if (this == toUnicodeStatus) {
                    item ="[]";
                }
            }
            lineParts.add(item);
            return next();
        }
        static boolean equalsIgnoringErrorDiffs(List<String> a, List<String> b) {
            if (a == b) {
                return true;
            } else if (a == null || b == null) {
                return false;
            }
            for (Column col : Column.values()) {
                if (col == EOL) {
                    continue;
                }
                String itemA = a.get(col.ordinal());
                String itemB = b.get(col.ordinal());
                if (!itemA.equals(itemB)) {
                    switch(col) {
                    case toUnicodeStatus:
                    case toAsciiNStatus:
                    case toAsciiTStatus:
                        return a.equals("[]") == b.equals("[]");
                    default: 
                        return false;
                    }
                }
            }
            return true;
        }
    }

    static final Splitter semiSplitter = Splitter.on(';').trimResults(CharMatcher.anyOf(" \t"));
    static final String NEW_FILE_NAME = "IdnaTestV2.txt";

    public static void main(String[] args) {
        Map<String, List<String>> oldFile = fleshOut("internal-" + NEW_FILE_NAME);
        Map<String, List<String>> newFile = fleshOut(NEW_FILE_NAME);
        LinkedHashSet<String> keys = new LinkedHashSet<>(oldFile.keySet());
        keys.addAll(newFile.keySet());
        for (String item : keys) {
            List<String> oldItem = oldFile.get(item);
            List<String> newItem = newFile.get(item);
            if (!Column.equalsIgnoringErrorDiffs(oldItem, newItem)) {
                System.out.println("old:\t" + oldItem + "\nnew:\t" + newItem);
            }
        }
    }

    private static boolean equalsIgnoringExactErrors(List<String> oldItem, List<String> newItem) {
        return Objects.equal(oldItem, newItem);
    }

    private static Map<String, List<String>> fleshOut(String fileName) {
        Map<String, List<String>> fleshedOut = new LinkedHashMap<>();
        for (String line : FileUtilities.in(GenerateIdna.DIR, fileName)) {
            if (line.contains("X3")) {
                int debug = 0;
            }
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            int lastHash = line.lastIndexOf('#');
            if (lastHash >= 0) {
                line = line.substring(0, lastHash);
            }
            List<String> lineParts = new ArrayList<>();
            Column column = Column.source;
            for (String item : semiSplitter.split(line)) {
                column = column.addFixedAndNext(item, lineParts);
            }
            fleshedOut.put(lineParts.get(0), ImmutableList.copyOf(lineParts));
        }
        return ImmutableMap.copyOf(fleshedOut);
    }
}
