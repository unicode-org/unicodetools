package org.unicode.text.UCA;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

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
        String fullFileName = filename
            + (collatorType==CollatorType.cldr ? "_CLDR" : "") 
            + ".txt";

        String directory = UCA.getUCA_GEN_DIR() + File.separator
        + (collatorType==CollatorType.cldr ? "CollationAuxiliary" : "Ducet");

        PrintWriter log = Utility.openPrintWriter(directory, fullFileName, Utility.UTF8_WINDOWS);
        // if (!shortPrint) log.write('\uFEFF');
        WriteCollationData.writeVersionAndDate(log, fullFileName, collatorType==CollatorType.cldr);

        if (false) log.println("\n# A reordering of the DUCET allkeys file for CLDR.\n" +
        		"# Guarantees that characters below 'a' are in the order: spaces, punctuation, general-symbols, currency-symbols, numbers.\n" +
        		"# Where general-symbols includes anything in the DUCET below 'a', thus including some Lm characters, for example.\n" +
        		"# Also sets only spaces and punctuation to be variable.\n" +
        		"# The primary values may overlap with secondaries; if non-overlap is required in the implementation, non-zero primaries should be offset by an appropriate amount.\n" +
        		"# Because of the preprocessing, some values may have somewhat different weights, but the results (other than the above changes) should be the same.\n" +
        		"# Unlike the DUCET, the ordering is by non-ignorable sort order.\n"
                );
        
        UCA collator = WriteCollationData.getCollator(collatorType);
        UCA.UCAContents cc = collator.getContents(null);
        log.println("@version " + collator.getDataVersion() + "\n");

        Map<String,String> sorted = new TreeMap();
    
        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
        
            Utility.dot(counter++);
            addString(collator, s, sorted);
        }
        addString(collator, "\uFFFE", sorted);
        addString(collator, "\uFFFF", sorted);

        int variableTop = UCA.getPrimary(collator.getVariableHighCE());
        StringBuilder extraComment = new StringBuilder();
        for (Entry<String, String> entry : sorted.entrySet()) {
            //String key = entry.getKey();
            String value = entry.getValue();
            char lastChar = value.charAt(value.length()-1);
            if (Character.isHighSurrogate(lastChar)) {
                continue;
            }
            String hex = Utility.hex(value, " ");
            if (hex.length() < 5) {
                hex += " ";
            }
            CEList ceList = collator.getCEList(value, true);
            if (ceList == null) {
                String sortkey = collator.getSortKey(value, UCA.NON_IGNORABLE, true, AppendToCe.none);
                // 0000  ; [.0000.0000.0000.0000] # [0000] NULL (in 6429)
                // 0430 0306 ; [.1947.0020.0002.04D1] # CYRILLIC SMALL LETTER A WITH BREVE
                // 1F60  ; [.1904.0020.0002.03C9][.0000.0022.0002.0313] # GREEK SMALL LETTER OMEGA WITH PSILI; QQCM
    
                log.println(hex 
                        + " ; " + UCA.toStringUCA(sortkey, value, variableTop, extraComment)
                        + " # " + Default.ucd().getName(value)
                        + extraComment
                        );
            } else {
                    log.println(hex 
                            + " ; " + UCA.toStringUCA(ceList, value, variableTop, extraComment)
                            + " # " + Default.ucd().getName(value)
                            + extraComment
                            );

            }
        }
        log.close();
    }

    public static String addString(UCA collator, String s,
            Map<String, String> sorted) {
        String colDbase = collator.getSortKey(s, UCA.SHIFTED, true, AppendToCe.nfd);
        sorted.put(colDbase, s);
        return colDbase;
    }
    
    static String dropIdentical(String sortKeyWithIdentical) {
        char extra = sortKeyWithIdentical.charAt(sortKeyWithIdentical.length() - 1);
        String clipped = sortKeyWithIdentical.substring(0, sortKeyWithIdentical.length() - 1);
        return clipped;
    }
}
