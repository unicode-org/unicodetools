/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateNamedSequences.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;

public final class GenerateNamedSequences implements UCD_Types {

    static final boolean DEBUG = false;

    public static String showVarGlyphs(
            String code0, String code1, String shape, String description) {
        if (DEBUG) {
            System.out.println(code0 + ", " + code1 + ", [" + shape + "]");
        }

        String abbShape = "";
        if (shape.length() != 0) {
            abbShape = '-' + shape.substring(0, 4);
            if (description.indexOf("feminine") >= 0) {
                abbShape += "fem";
            }
        }

        return "<img alt='U+"
                + code0
                + "+U+"
                + code1
                + "/"
                + shape
                + "' src='http://www.unicode.org/cgi-bin/varglyph?24-"
                + code0
                + "-"
                + code1
                + abbShape
                + "'>";
    }

    /*
    #   Field 0: the variation sequence
    #   Field 1: the description of the desired appearance
    #   Field 2: where the appearance is only different in in particular shaping environments
    #    this field lists them. The possible values are: isolated, initial, medial, final.
    #    If more than one is present, there are spaces between them.
         */
    public static void generate(String filename2) throws IOException {

        // read the data and compose the table

        String table =
                "<table><tr><th width='10%'>Rep Glyph</th><th>Hex Sequence</th><th>Name</th><th>Copyable</th></tr>";

        final String[] splits = new String[4];
        final String[] codes = new String[20];
        final String[] shapes = new String[4];

        final BufferedReader in =
                Utility.openUnicodeFile(filename2, Default.ucdVersion(), true, Utility.LATIN1);
        final Transliterator unicodexml = Transliterator.getInstance("hex/xml");
        while (true) {
            String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            final int count = Utility.split(line, ';', splits);
            final String name = splits[0];
            final int codeCount = Utility.split(splits[1].trim(), ' ', codes);
            final StringBuffer codeBuffer = new StringBuffer();
            for (int i = 0; i < codeCount; ++i) {
                if (codes[i].length() == 0) {
                    continue;
                }
                UTF16.append(codeBuffer, Integer.parseInt(codes[i], 16));
            }
            final String codeWithHyphens = splits[1].replaceAll("\\s+", "-");
            final String codeAlt = "U+" + splits[1].replaceAll("\\s", " U+");
            final String codeString = unicodexml.transliterate(codeBuffer.toString());

            // <img alt="03E2" src="http://www.unicode.org/cgi-bin/refglyph?24-03E2"
            // style="vertical-align:middle">

            // table += "<tr><td><img alt='U+" + codes[0] + "'
            // src='http://www.unicode.org/cgi-bin/refglyph?24-" + codes[0] + "'></td>\n";
            String imageName = "images/U" + codeWithHyphens + ".gif";
            if (splits[1].compareTo("1780") >= 0 && splits[1].compareTo("1800") < 0) {
                final String codeNoSpaces2 = splits[1].replaceAll("\\s", "");
                imageName = "http://www.unicode.org/reports/tr28/images/" + codeNoSpaces2 + ".gif";
            }
            table +=
                    "<tr>"
                            + "<td class='copy'><img alt='("
                            + codeAlt
                            + ")' src='"
                            + imageName
                            + "'><br><tt>"
                            + splits[1]
                            + "</tt></td>"
                            + "<td>"
                            + splits[1]
                            + "</td>"
                            + "</td><td>"
                            + name
                            + "</td>"
                            + "<td class='copy'>"
                            + codeString
                            + "</td>"
                            + "</tr>\n";
            if (DEBUG) {
                System.out.println(splits[1] + "\t" + codeString);
            }
        }
        in.close();
        table += "</table>";

        // now write out the results

        final String directory = "UCD/" + Default.ucd().getVersion() + "/extra/";
        final UnicodeDataFile outfile =
                UnicodeDataFile.openHTMLAndWriteHeader(directory, filename2)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);

        final PrintWriter out =
                outfile.out; // Utility.openPrintWriter(filename, Utility.LATIN1_UNIX);
        /*
        String[] batName = {""};
        String mostRecent = UnicodeDataFile.generateBat(directory, filename, UnicodeDataFile.getFileSuffix(true), batName);

        String version = Default.ucd().getVersion();
        int lastDot = version.lastIndexOf('.');
        String updateDirectory = version.substring(0,lastDot) + "-Update";
        int updateV = version.charAt(version.length()-1) - '0';
        if (updateV != 0) updateDirectory += (char)('1' + updateV);
        if (DEBUG) System.out.println("updateDirectory: " + updateDirectory);
         */

        final String[] replacementList = {
            "@revision@", Default.ucd().getVersion(),
            // "@updateDirectory@", updateDirectory,
            "@date@", Default.getDate(),
            "@table@", table
        };

        Utility.appendFile(
                Settings.SRC_UCD_DIR + "NamedSequences-Template.html",
                Utility.UTF8,
                out,
                replacementList);

        outfile.close();
        // Utility.renameIdentical(mostRecent, Utility.getOutputName(filename), batName[0]);
    }
}
