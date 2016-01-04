/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateStandardizedVariants.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public final class GenerateStandardizedVariants implements UCD_Types {

    static final boolean DEBUG = false;

    static public String showVarGlyphs(String code0, String code1, String shape, String description) {
        if (DEBUG) {
            System.out.println(code0 + ", " + code1 + ", [" + shape + "]");
        }

        String abbShape = "";
        if (shape.length() != 0) {
            abbShape = '-' + shape.substring(0,4);
            if (description.indexOf("feminine") >= 0) {
                abbShape += "fem";
            }
        }

        return "<img alt='U+" + code0 + "+U+" + code1 + "/" + shape
                + "' src='http://www.unicode.org/cgi-bin/varglyph?24-" +code0 + "-" + code1 + abbShape + "'>";
    }

    /*
#   Field 0: the variation sequence
#   Field 1: the description of the desired appearance
#   Field 2: where the appearance is only different in in particular shaping environments
#	this field lists them. The possible values are: isolated, initial, medial, final.
#	If more than one is present, there are spaces between them.
     */
    static public void generate() throws IOException {
        if (Default.ucdVersionInfo().compareTo(VersionInfo.getInstance(9)) >= 0) {
            // StandardizedVariants.html is obsolete since Unicode 9.0.
            return;
        }

        // read the data and compose the table

        String table = "<table><tr><th>Rep Glyph</th><th>Character Sequence</th><th>Context</th><th width='10%'>Alt Glyph</th><th>Description of variant appearance</th></tr>";

        final String[] splits = new String[4];
        final String[] codes = new String[2];
        final String[] shapes = new String[4];
        
        ToolUnicodePropertySource tups = ToolUnicodePropertySource.make(Default.ucdVersion());
        final UnicodeProperty ui = tups.getProperty("Unified_Ideograph");
        UnicodeSet uiSet = ui.getSet("Yes");

        final BufferedReader in = Utility.openUnicodeFile("StandardizedVariants", Default.ucdVersion(), true, Utility.LATIN1);
        while (true) {
            final String line = Utility.readDataLine(in);
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }

            //nal int count = Utility.split(line, ';', splits);
            //nal int codeCount = Utility.split(splits[0], ' ', codes);
            final int code = Utility.codePointFromHex(codes[0]);
            if (uiSet.contains(code)) {
                continue;
            }

            // <img alt="03E2" src="http://www.unicode.org/cgi-bin/refglyph?24-03E2" style="vertical-align:middle">

            final String glyphPart = "refglyph?24-" + codes[0];
            final String substitute = FIX_GLYPH_PART.get(glyphPart);
            table += "<tr><td><img alt='U+" + codes[0] + "' src='http://www.unicode.org/cgi-bin/" +
                    (substitute != null ? substitute : glyphPart) +
                    "'></td>\n";
            table += "<td>" + splits[0] + "</td>\n";

            String shape = splits[2].trim();
            if (shape.equals("all")) {
                shape = "";
            }

            table += "<td>" + Utility.replace(shape, " ", "<br>") + "</td>\n";

            // http://www.unicode.org/cgi-bin/varglyph?24-1820-180B-fina
            // http://www.unicode.org/cgi-bin/varglyph?24-222A-FE00

            table += "<td>";
            if (shape.length() == 0) {
                table += showVarGlyphs(codes[0], codes[1], "", "");
            } else {
                final int shapeCount = Utility.split(shape, ' ', shapes);
                for (int i = 0; i < shapeCount; ++i) {
                    if (i != 0) {
                        table += " ";
                    }
                    table += showVarGlyphs(codes[0], codes[1], shapes[i], splits[1]);
                }
            }
            table += "</td>\n";

            table += "<td>" + Default.ucd().getName(code) + " " + splits[1] + "</td>\n";
            table += "</tr>";
        }
        in.close();
        table += "</table>";

        // now write out the results

        final UnicodeDataFile outfile = UnicodeDataFile.openHTMLAndWriteHeader(MakeUnicodeFiles.MAIN_OUTPUT_DIRECTORY, "StandardizedVariants")
                .setSkipCopyright(Settings.SKIP_COPYRIGHT);

        final PrintWriter out = outfile.out;

        final String version = Default.ucd().getVersion();
        final String lastVersion = Utility.getPreviousUcdVersion(version);
        final int lastDot = version.lastIndexOf('.');
        String updateDirectory;
        String lastDirectory;
        String partialFilename;
        if (version.compareTo("4.1.0") < 0) {
            updateDirectory = version.substring(0,lastDot) + "-Update";
            final int updateV = version.charAt(version.length()-1) - '0';
            if (updateV != 0) {
                updateDirectory += (char)('1' + updateV);
            }
            if (DEBUG) {
                System.out.println("updateDirectory: " + updateDirectory);
            }
            partialFilename = "StandardizedVariants-" + Default.ucd().getVersion();
        } else if (version.compareTo("4.1.0") == 0) {
            updateDirectory = version.substring(0,lastDot) + "/ucd";
            partialFilename = "StandardizedVariants";
        } else {
            updateDirectory = version + "/ucd";
            partialFilename = "StandardizedVariants";
        }
        lastDirectory = lastVersion + "/ucd";


        final String[] replacementList = {
                "@revision@", Default.ucd().getVersion(),
                "@updateDirectory@", updateDirectory,
                "@lastDirectory@", lastDirectory,
                "@filename@", partialFilename,
                "@date@", Default.getDate(),
                "@table@", table};

        Utility.appendFile(Settings.SRC_UCD_DIR + "StandardizedVariants-Template.html", Utility.UTF8, out, replacementList);

        outfile.close();
    }

    static Map<String,String> FIX_GLYPH_PART = new HashMap<String,String>();
    static {
        final String[][] HACKS = {
                {"refglyph?24-25FB", "varglyph?24-25FB-FE0E"},
                {"refglyph?24-25FB", "varglyph?24-25FB-FE0E"},
                {"refglyph?24-25FC", "varglyph?24-25FC-FE0E"},
                {"refglyph?24-25FC", "varglyph?24-25FC-FE0E"},
                {"refglyph?24-25FD", "varglyph?24-25FD-FE0E"},
                {"refglyph?24-25FD", "varglyph?24-25FD-FE0E"},
                {"refglyph?24-25FE", "varglyph?24-25FE-FE0E"},
                {"refglyph?24-25FE", "varglyph?24-25FE-FE0E"},
                {"refglyph?24-2614", "varglyph?24-2614-FE0E"},
                {"refglyph?24-2614", "varglyph?24-2614-FE0E"},
                {"refglyph?24-2615", "varglyph?24-2615-FE0E"},
                {"refglyph?24-2615", "varglyph?24-2615-FE0E"},
                {"refglyph?24-267B", "varglyph?24-267B-FE0E"},
                {"refglyph?24-267B", "varglyph?24-267B-FE0E"},
                {"refglyph?24-267F", "varglyph?24-267F-FE0E"},
                {"refglyph?24-267F", "varglyph?24-267F-FE0E"},
                {"refglyph?24-2693", "varglyph?24-2693-FE0E"},
                {"refglyph?24-2693", "varglyph?24-2693-FE0E"},
                {"refglyph?24-26A0", "varglyph?24-26A0-FE0E"},
                {"refglyph?24-26A0", "varglyph?24-26A0-FE0E"},
                {"refglyph?24-26A1", "varglyph?24-26A1-FE0E"},
                {"refglyph?24-26A1", "varglyph?24-26A1-FE0E"},
                {"refglyph?24-26AA", "varglyph?24-26AA-FE0E"},
                {"refglyph?24-26AA", "varglyph?24-26AA-FE0E"},
                {"refglyph?24-26AB", "varglyph?24-26AB-FE0E"},
                {"refglyph?24-26AB", "varglyph?24-26AB-FE0E"},
                {"refglyph?24-26BD", "varglyph?24-26BD-FE0E"},
                {"refglyph?24-26BD", "varglyph?24-26BD-FE0E"},
                {"refglyph?24-26BE", "varglyph?24-26BE-FE0E"},
                {"refglyph?24-26BE", "varglyph?24-26BE-FE0E"},
                {"refglyph?24-26C4", "varglyph?24-26C4-FE0E"},
                {"refglyph?24-26C4", "varglyph?24-26C4-FE0E"},
                {"refglyph?24-26C5", "varglyph?24-26C5-FE0E"},
                {"refglyph?24-26C5", "varglyph?24-26C5-FE0E"},
                {"refglyph?24-26D4", "varglyph?24-26D4-FE0E"},
                {"refglyph?24-26D4", "varglyph?24-26D4-FE0E"},
                {"refglyph?24-26EA", "varglyph?24-26EA-FE0E"},
                {"refglyph?24-26EA", "varglyph?24-26EA-FE0E"},
                {"refglyph?24-26F2", "varglyph?24-26F2-FE0E"},
                {"refglyph?24-26F2", "varglyph?24-26F2-FE0E"},
                {"refglyph?24-26F3", "varglyph?24-26F3-FE0E"},
                {"refglyph?24-26F3", "varglyph?24-26F3-FE0E"},
                {"refglyph?24-26F5", "varglyph?24-26F5-FE0E"},
                {"refglyph?24-26F5", "varglyph?24-26F5-FE0E"},
                {"refglyph?24-26FA", "varglyph?24-26FA-FE0E"},
                {"refglyph?24-26FA", "varglyph?24-26FA-FE0E"},
                {"refglyph?24-26FD", "varglyph?24-26FD-FE0E"},
                {"refglyph?24-26FD", "varglyph?24-26FD-FE0E"},
                {"refglyph?24-2757", "varglyph?24-2757-FE0E"},
                {"refglyph?24-2757", "varglyph?24-2757-FE0E"},
                {"refglyph?24-2934", "varglyph?24-2934-FE0E"},
                {"refglyph?24-2934", "varglyph?24-2934-FE0E"},
                {"refglyph?24-2935", "varglyph?24-2935-FE0E"},
                {"refglyph?24-2935", "varglyph?24-2935-FE0E"},
                {"refglyph?24-2B05", "varglyph?24-2B05-FE0E"},
                {"refglyph?24-2B05", "varglyph?24-2B05-FE0E"},
                {"refglyph?24-2B06", "varglyph?24-2B06-FE0E"},
                {"refglyph?24-2B06", "varglyph?24-2B06-FE0E"},
                {"refglyph?24-2B07", "varglyph?24-2B07-FE0E"},
                {"refglyph?24-2B07", "varglyph?24-2B07-FE0E"},
                {"refglyph?24-2B1B", "varglyph?24-2B1B-FE0E"},
                {"refglyph?24-2B1B", "varglyph?24-2B1B-FE0E"},
                {"refglyph?24-2B1C", "varglyph?24-2B1C-FE0E"},
                {"refglyph?24-2B1C", "varglyph?24-2B1C-FE0E"},
                {"refglyph?24-2B50", "varglyph?24-2B50-FE0E"},
                {"refglyph?24-2B50", "varglyph?24-2B50-FE0E"},
                {"refglyph?24-2B55", "varglyph?24-2B55-FE0E"},
                {"refglyph?24-2B55", "varglyph?24-2B55-FE0E"},
                {"refglyph?24-303D", "varglyph?24-303D-FE0E"},
                {"refglyph?24-303D", "varglyph?24-303D-FE0E"},
                {"refglyph?24-3297", "varglyph?24-3297-FE0E"},
                {"refglyph?24-3297", "varglyph?24-3297-FE0E"},
                {"refglyph?24-3299", "varglyph?24-3299-FE0E"},
                {"refglyph?24-3299", "varglyph?24-3299-FE0E"},
                {"refglyph?24-1F004", "varglyph?24-1F004-FE0E"},
                {"refglyph?24-1F004", "varglyph?24-1F004-FE0E"},
                {"refglyph?24-1F17F", "varglyph?24-1F17F-FE0E"},
                {"refglyph?24-1F17F", "varglyph?24-1F17F-FE0E"},
                {"refglyph?24-1F21A", "varglyph?24-1F21A-FE0E"},
                {"refglyph?24-1F21A", "varglyph?24-1F21A-FE0E"},
                {"refglyph?24-1F22F", "varglyph?24-1F22F-FE0E"},
                {"refglyph?24-1F22F", "varglyph?24-1F22F-FE0E"},
        };
        for (final String[] pair : HACKS) {
            FIX_GLYPH_PART.put(pair[0], pair[1]);
        }
    }
}
