package org.unicode.idna;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.idna.Idna2008.Idna2008Type;

public class Idna2008t extends Idna {

    public static Idna2008t SINGLETON = new Idna2008t();

    private static UnicodeMap<Idna2008Type> oldTypes;

    private Idna2008t() {
        oldTypes = new UnicodeMap<Idna2008Type>();

        initData();

        for (final Idna2008Type oldType : oldTypes.values()) {
            final UnicodeSet uset = oldTypes.getSet(oldType);
            switch (oldType) {
                case UNASSIGNED:
                case DISALLOWED:
                    types.putAll(uset, Idna.IdnaType.disallowed);
                    break;
                case PVALID:
                case CONTEXTJ:
                case CONTEXTO:
                    types.putAll(uset, Idna.IdnaType.valid);
                    break;
            }
        }
        types.put('.', IdnaType.valid);
        types.freeze();
        mappings.freeze();
        mappings_display.freeze();
        validSet = validSet_transitional = types.getSet(IdnaType.valid).freeze();
    }

    public static UnicodeMap<Idna2008Type> getTypeMapping() {
        return oldTypes;
    }

    // **** privates ****

    private static void initData() {

        final Matcher DATALINE =
                Pattern.compile(
                                "([0-9a-fA-F]{4,6})"
                                        + "(?:\\.\\.([0-9a-fA-F]{4,6}))?"
                                        + "\\s*;\\s*"
                                        + "(PVALID|DISALLOWED|UNASSIGNED|CONTEXTJ|CONTEXTO)"
                                        + "\\s*#\\s*"
                                        + "(.*)")
                        .matcher("");

        try {
            final BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    Idna2008.class.getResourceAsStream("tables.txt")));
            // FileUtilities.openReader(Utility.DATA_DIRECTORY + "/IDN/",
            // "draft-faltstrom-idnabis-tables-05.txt", "ascii");
            boolean inTable = false;

            final int count = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                // if ((count++ % 100) == 0) {
                // System.out.println(count + " " + line);
                // }
                line = line.trim();

                if (line.startsWith("Appendix B.1.")) {
                    inTable = true;
                    continue;
                }
                if (line.startsWith("Author's Address")) {
                    break;
                }
                if (!inTable) {
                    continue;
                }
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
                        DATALINE.group(2) == null
                                ? startChar
                                : Integer.parseInt(DATALINE.group(2), 16);
                final Idna2008Type idnaType = Idna2008Type.valueOf(DATALINE.group(3));
                oldTypes.putAll(startChar, endChar, idnaType);
            }
            in.close();
            oldTypes.freeze();
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
