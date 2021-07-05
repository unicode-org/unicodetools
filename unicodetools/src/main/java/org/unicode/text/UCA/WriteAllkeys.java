package org.unicode.text.UCA;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

public class WriteAllkeys {
    public static void main(String[] args) throws IOException {
        writeAllkeys("allkeys", CollatorType.cldr);
        System.out.println("Done");
    }

    static void writeAllkeys(String filename, CollatorType collatorType) throws IOException {

        int counter = 0;
        final String fullFileName = filename
                + (collatorType==CollatorType.cldr ? "_CLDR" : "")
                + ".txt";

        final String directory = UCA.getOutputDir() + File.separator
                + (collatorType==CollatorType.cldr ? "CollationAuxiliary" : "Ducet");

        final PrintWriter log = Utility.openPrintWriter(directory, fullFileName, Utility.UTF8_WINDOWS);
        // if (!shortPrint) log.write('\uFEFF');
        WriteCollationData.writeVersionAndDate(log, fullFileName, collatorType==CollatorType.cldr);

        if (false) {
            log.println("\n# A reordering of the DUCET allkeys file for CLDR.\n" +
                    "# Guarantees that characters below 'a' are in the order: spaces, punctuation, general-symbols, currency-symbols, numbers.\n" +
                    "# Where general-symbols includes anything in the DUCET below 'a', thus including some Lm characters, for example.\n" +
                    "# Also sets only spaces and punctuation to be variable.\n" +
                    "# The primary values may overlap with secondaries; if non-overlap is required in the implementation, non-zero primaries should be offset by an appropriate amount.\n" +
                    "# Because of the preprocessing, some values may have somewhat different weights, but the results (other than the above changes) should be the same.\n" +
                    "# Unlike the DUCET, the ordering is by non-ignorable sort order.\n"
                    );
        }

        final UCA collator = WriteCollationData.getCollator(collatorType);
        final UCA.UCAContents cc = collator.getContents(null);
        log.println("@version " + collator.getDataVersion() + "\n");

        final Map<String,String> sorted = new TreeMap<String,String>();

        while (true) {
            final String s = cc.next();
            if (s == null) {
                break;
            }

            Utility.dot(counter++);
            addString(collator, s, sorted);
        }
        addString(collator, "\uFFFE", sorted);
        addString(collator, "\uFFFF", sorted);

        final int variableTop = CEList.getPrimary(collator.getVariableHighCE());
        final StringBuilder extraComment = new StringBuilder();
        for (final Entry<String, String> entry : sorted.entrySet()) {
            //String key = entry.getKey();
            final String value = entry.getValue();
            final char lastChar = value.charAt(value.length()-1);
            if (Character.isHighSurrogate(lastChar)) {
                continue;
            }
            String hex = Utility.hex(value, " ");
            if (hex.length() < 5) {
                hex += " ";
            }
            final CEList ceList = collator.getCEList(value, true);
            log.println(hex
                    + " ; " + UCA.toStringUCA(ceList, variableTop, extraComment)
                    + " # " + Default.ucd().getName(value)
                    + extraComment
                    );
        }
        log.close();
    }

    public static String addString(UCA collator, String s,
            Map<String, String> sorted) {
        final String colDbase = collator.getSortKey(s, UCA_Types.NON_IGNORABLE, true, AppendToCe.tieBreaker);
        sorted.put(colDbase, s);
        return colDbase;
    }

    static String dropIdentical(String sortKeyWithIdentical) {
        final String clipped = sortKeyWithIdentical.substring(0, sortKeyWithIdentical.length() - 1);
        return clipped;
    }
}
