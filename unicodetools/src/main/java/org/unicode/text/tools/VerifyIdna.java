package org.unicode.text.tools;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.BagFormatter;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.Settings;

public class VerifyIdna {
    // 011B ; PVALID # LATIN SMALL LETTER E WITH CARON
    static final Matcher DATALINE =
            Pattern.compile(
                            "([0-9a-fA-F]{4,6})"
                                    + "(?:\\.\\.([0-9a-fA-F]{4,6}))?"
                                    + "\\s*;\\s*"
                                    + "(PVALID|DISALLOWED|UNASSIGNED|CONTEXTJ|CONTEXTO)"
                                    + "\\s*#\\s*"
                                    + "(.*)")
                    .matcher("");

    public enum IdnaType {
        PVALID,
        DISALLOWED,
        UNASSIGNED,
        CONTEXTJ,
        CONTEXTO
    }

    public static void main(String[] args) throws IOException {
        try {
            final UnicodeMap patrik = getPatriksMapping();
            final UnicodeMap alternate = getAlternativeDerivation();

            final Set<IdnaType> patrickValues = new TreeSet<IdnaType>(patrik.getAvailableValues());
            final Set<IdnaType> altValues = new TreeSet<IdnaType>(alternate.getAvailableValues());
            if (!patrickValues.equals(altValues)) {
                System.out.println("Difference in Values");
                System.out.println("\tpat:\t" + patrickValues);
                System.out.println("\talt:\t" + altValues);
                System.out.println();
            }

            final BagFormatter bf = new BagFormatter();

            for (final IdnaType idnaType : patrickValues) {
                System.out.println(idnaType);
                final UnicodeSet patItems = patrik.keySet(idnaType);
                final UnicodeSet altItems = alternate.keySet(idnaType);
                if (patItems.equals(altItems)) {
                    System.out.println("\tequal");
                } else {
                    System.out.println("Difference in Contents");
                    // System.out.println(bf.showSetDifferences("pat", patItems, "alt", altItems));
                    final UnicodeSet patDiffAlt = new UnicodeSet(patItems).removeAll(altItems);
                    System.out.println("\tpat-alt:\n" + bf.showSetNames(patDiffAlt));
                    final UnicodeSet altDiffPat = new UnicodeSet(altItems).removeAll(patItems);
                    System.out.println("\talt-pat:\n" + bf.showSetNames(altDiffPat));
                    // System.out.println("\tpat:\t" + patItems);
                    // System.out.println("\talt:\t" + altItems);
                }
                System.out.println();
            }
        } finally {
            System.out.println("DONE");
        }
    }

    private static UnicodeMap getAlternativeDerivation() {
        final UnicodeMap result = new UnicodeMap();
        final UnicodeSet foo = parseUnicodeSet("[[:gc=cn:]-[:NChar:]]");

        System.out.println(
                "A\t"
                        + parseUnicodeSet(
                                        "["
                                                + "[[:gc=Ll:][:gc=Lt:][:gc=Lu:][:gc=Lo:][:gc=Lm:][:gc=Mn:][:gc=Mc:][:gc=Nd:]]"
                                                + // A - restrict to only letters, marks, numbers
                                                "]")
                                .complement()
                                .complement());

        result.putAll(
                parseUnicodeSet("[\\u0000-\\U0010FFFF]"),
                IdnaType.DISALLOWED); // Assume disallowed unless we set otherwise
        result.putAll(
                parseUnicodeSet("[[:gc=cn:]-[:NChar:]]"),
                IdnaType.UNASSIGNED); // J - unassigned code points // -[:NChar:]
        // parseUnicodeSet("[[:gc=cn:]]");
        result.putAll(
                parseUnicodeSet(
                        "["
                                + "[[:gc=Ll:][:gc=Lt:][:gc=Lu:][:gc=Lo:][:gc=Lm:][:gc=Mn:][:gc=Mc:][:gc=Nd:]]"
                                + // A - restrict to only letters, marks, numbers
                                "-[[:^isCaseFolded:]]"
                                + // B - minus characters unstable under NFKC & casefolding
                                "-[:di:]"
                                + // C - minus default-ignorables
                                "-[[:block=Combining_Diacritical_Marks_for_Symbols:]"
                                + // D minus exceptional block exclusions
                                "[:block=Musical_Symbols:]"
                                + "[:block=Ancient_Greek_Musical_Notation:]"
                                + "[:block=Phaistos_Disc:]]"
                                + // x
                                "[\\u3007]"
                                + // x
                                "]"),
                IdnaType.PVALID);
        result.putAll(
                parseUnicodeSet(
                        "["
                                + "[\u002D\u00B7\u02B9\u0375\u0483\u05F3\u05F4\u3005\u303B\u30FB]"
                                + // F.2 - exceptional contextual characters
                                "[:gc=cf:]"
                                + // I - other Cf characters (should be omitted)
                                "]"),
                IdnaType.CONTEXTO);
        result.putAll(parseUnicodeSet("[:join_control:]"), IdnaType.CONTEXTJ); // H - join controls
        result.freeze();
        return result;
    }

    private static UnicodeMap getPatriksMapping() throws IOException {
        final BufferedReader in =
                FileUtilities.openReader(
                        Settings.UnicodeTools.DATA_DIR + "/IDN/",
                        "draft-faltstrom-idnabis-tables-05.txt",
                        "ascii");
        boolean inTable = false;
        final UnicodeMap patrik = new UnicodeMap();
        int count = 0;
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if ((count++ % 100) == 0) {
                System.out.println(count + " " + line);
            }
            if (line.startsWith("A.1.")) {
                inTable = true;
                continue;
            }
            if (line.startsWith("Author's Address")) {
                break;
            }
            if (!inTable) {
                continue;
            }
            line = line.trim();
            if (line.length() == 0
                    || line.startsWith("Faltstrom")
                    || line.startsWith("Internet-Draft")) {
                continue;
            }
            // we now have real data
            if (!DATALINE.reset(line).matches()) {
                System.out.println("Error: line doesn't match: " + line);
                continue;
            }
            final int startChar = Integer.parseInt(DATALINE.group(1), 16);
            final int endChar =
                    DATALINE.group(2) == null ? startChar : Integer.parseInt(DATALINE.group(2), 16);
            final IdnaType idnaType = IdnaType.valueOf(DATALINE.group(3));
            patrik.putAll(startChar, endChar, idnaType);
        }
        in.close();
        patrik.freeze();
        return patrik;
    }

    public static UnicodeSet parseUnicodeSet(String input) {
        final var myXSymbolTable = VersionedSymbolTable.forDevelopment();
        myXSymbolTable.setUnversionedExtensions(ToolUnicodePropertySource.make(""));
        input = input.trim();
        final ParsePosition parsePosition = new ParsePosition(0);
        final UnicodeSet result = new UnicodeSet(input, parsePosition, myXSymbolTable);
        if (parsePosition.getIndex() != input.length()) {
            throw new IllegalArgumentException(
                    "Additional characters past the end of the set, at "
                            + parsePosition.getIndex()
                            + ", ..."
                            + input.substring(
                                    Math.max(0, parsePosition.getIndex() - 10),
                                    parsePosition.getIndex())
                            + "|"
                            + input.substring(
                                    parsePosition.getIndex(),
                                    Math.min(input.length(), parsePosition.getIndex() + 10)));
        }
        return result;
    }
}
