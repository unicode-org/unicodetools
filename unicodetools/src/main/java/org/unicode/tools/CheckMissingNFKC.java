package org.unicode.tools;

import com.google.common.base.Objects;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.unicode.cldr.util.Pair;
import org.unicode.text.utility.Settings.UnicodeTools;
import org.unicode.text.utility.Utility;

public class CheckMissingNFKC {
    public static void main(String[] args) throws IOException {
        Normalizer2 nfkc_cf = Normalizer2.getNFKCCasefoldInstance();

        UnicodeMap<String> n4m = new UnicodeMap<>();
        System.out.println(UnicodeTools.UNICODETOOLS_RSRC_DIR);
        // "/unicodetools/src/main/resources/org/unicode/tools/nfkc-extended.txt"
        Path filePath =
                Paths.get(
                        UnicodeTools.UNICODETOOLS_RSRC_DIR, "org/unicode/tools/nfkc-extended.txt");

        // Unfortunately the internal tools in ICU aren't accessible, so parse it ourselves
        // https://unicode-org.github.io/icu/userguide/transforms/normalization/#data-file-syntax

        Files.lines(filePath)
                .forEach(
                        line -> {
                            if (line.isBlank() || line.startsWith("*")) {
                                return;
                            }
                            int greaterPos = line.indexOf('>');
                            if (greaterPos < 0) {
                                greaterPos =
                                        line.indexOf('='); // for our purposed, = is the same as >
                                if (greaterPos < 0) {
                                    int colonPos = line.indexOf(':');
                                    if (colonPos < 0) {
                                        throw new IllegalArgumentException("line: " + line);
                                    }
                                }
                                return;
                            }
                            String rawSource = line.substring(0, greaterPos);
                            String target = Utility.fromHex(line.substring(greaterPos + 1));

                            int rangePos = rawSource.indexOf("..");
                            if (rangePos < 0) {
                                String source = Utility.fromHex(rawSource);
                                n4m.put(source, target);
                            } else {
                                int sourceStart =
                                        Utility.fromHex(rawSource.substring(0, rangePos))
                                                .codePointAt(0);
                                int sourceEnd =
                                        Utility.fromHex(rawSource.substring(rangePos + 2))
                                                .codePointAt(0);
                                n4m.putAll(sourceStart, sourceEnd, target);
                            }
                        });

        Map<String, Pair<String, String>> diff = new LinkedHashMap<>();
        UnicodeSet toCheck = new UnicodeSet("[[\\P{C}]-\\p{cf}]");
        System.out.println("Checking: " + toCheck.size() + " \t" + toCheck);
        for (int cp : toCheck.codePoints()) {
            String string = Character.toString(cp); // wish there were a code point interface
            String nfc_cfString = nfkc_cf.normalize(string);
            String n4mString = n4m.get(cp);
            if (n4mString == null) {
                n4mString = string;
            }
            if (Objects.equal(nfc_cfString, n4mString)) {
                continue;
            }
            diff.put(string, Pair.of(n4mString, nfc_cfString));
        }
        System.out.println("Differences:\t" + diff.size());
        System.out.println("Source" + "\t" + "N4M" + "\t" + "nfkc_cf");

        for (Entry<String, Pair<String, String>> entry : diff.entrySet()) {
            System.out.println(
                    Utility.hex(entry.getKey())
                            + "\t"
                            + Utility.hex(entry.getValue().getFirst())
                            + "\t"
                            + Utility.hex(entry.getValue().getSecond()));
        }
    }
}
