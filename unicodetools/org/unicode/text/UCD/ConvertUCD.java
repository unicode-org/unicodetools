/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/ConvertUCD.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.Utility;


/** Simple program to merge UCD files into XML. Not yet documented!!
 * @author Mark Davis
 */

public final class ConvertUCD implements UCD_Types {
    public static final boolean SHOW = false;
    public static final boolean DEBUG = false;
    static final boolean SHOW_SAMPLE = false;
    private static final boolean DEBUG_SCRIPT = false;


    int major;
    int minor;
    int update;

    String version;

    // varies by version
    /*
    public static final String BASE_DIR11 = DATA_DIR + "\\Versions\\";
    public static final String BASE_DIR20 = DATA_DIR + "\\Versions\\";
    public static final String BASE_DIR21 = DATA_DIR + "\\Versions\\";
    public static final String BASE_DIR30 = DATA_DIR + "\\Update 3.0.1\\";
    public static final String BASE_DIR31 = DATA_DIR + "\\3.1-Update\\";
     */

    //public static final String blocksnamePlain = "Blocks.txt";
    //public static final String blocksname31 = "Blocks-4d2.beta";

    /** First item is file name, rest are field names (skipping character).
     *  "OMIT" is special -- means don't record
     */

    static String[][] labelList = {
        // Labels for the incoming files. Labels MUST match field order in file.
        // IMPORTANT - defaults of form y-=x must occur after x is encountered!
        // The one exception is "st", which is handled specially.
        // So file order is important.
        //*
        // 01CA;LATIN CAPITAL LETTER NJ;Lu;0; L; <compat> 004E 004A;  ;  ;  ;N ;LATIN CAPITAL LETTER N J;    ;  ;01CC;01CB
        //      n                       gc cc bc dm                 dd dv nv bm on                       cm,  uc lc   tc
        {"UnicodeData", "n", "gc", "ccc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
        //{"ExtraProperties", "xp"},
        {"PropList", "binary"},

        //{"ExtraProperties", "xp"},

        {"EastAsianWidth", "ea", "OMIT"},
        {"LineBreak", "lb", "OMIT"},
        {"SpecialCasing", "*sl", "*st", "*su", "sc"},
        {"CompositionExclusions", "ce"},
        {"CaseFolding", "OMIT", "*fc"},
        {"ArabicShaping", "OMIT", "jt", "jg"},
        {"BidiMirroring", "*bg"},
        {"Scripts", "sn"},
        {"BidiBrackets", "bpb", "bpt"}, // 0028; 0029; o # LEFT PARENTHESIS Bidi_Paired_Bracket, Bidi_Paired_Bracket_Type
        //{"Jamo", "jn"},
        //{"Scripts-1d4", "RANGE", "sn"},
        //{"Age", "*sn"},
        //*/
        /*
        //*/
    };
    static HashMap isHex = new HashMap();
    static HashMap defaults = new HashMap();

    static {
        for (final String[] element : labelList) {
            final String[] labels = element;

            for (int i = 1; i < labels.length; ++i) {
                boolean hex = false;
                final String def = null;
                //char appendChar = '\u0000';

                // pull off "*": hex interpretation
                if (labels[i].charAt(0) == '*') { // HEX value
                    hex = true;
                    labels[i] = labels[i].substring(1);
                }

                /*
                // pull off "$": append duplicates
                if (labels[i].charAt(0) == '$') { // HEX value
                    appendChar = labels[i].charAt(1);
                    labels[i] = labels[i].substring(2);
                }

                // pull off default values
                int pos = labels[i].indexOf('-');
                if (pos >= 0) {
                    def = labels[i].substring(pos+1);
                    labels[i] = labels[i].substring(0,pos);
                }
                 */
                // store results
                // we do this after all processing, so that the label is clean!!

                if (hex) {
                    isHex.put(labels[i], "");
                }
                //if (appendChar != 0) appendDuplicates.put(labels[i], String.valueOf(appendChar));
                defaults.put(labels[i], def);
            }
        }
    }
    /*
    static String[][] labelList31 = {
        // Labels for the incoming files. Labels MUST match field order in file.
        // IMPORTANT - defaults of form y-=x must occur after x is encountered!
        // The one exception is "st", which is handled specially.
        // So file order is important.
        //*
        // 01CA;LATIN CAPITAL LETTER NJ;Lu;0; L; <compat> 004E 004A;  ;  ;  ;N ;LATIN CAPITAL LETTER N J;    ;  ;01CC;01CB
        //      n                       gc cc bc dm                 dd dv nv bm on                       cm,  uc lc   tc
        {"UnicodeData-3.1.0d8.beta", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
        {"PropList-3.1.0d5.beta", "binary"},

        {"ExtraProperties", "xp"},

        {"EastAsianWidth-4d7.beta", "ea", "OMIT"},
        {"LineBreak-6d6.beta", "lb", "OMIT"},
        {"SpecialCasing-4d1.beta", "*sl", "*st", "*su", "sc"},
        {"CompositionExclusions-3d6.beta", "ce"},
        {"CaseFolding-3d4.beta", "OMIT", "*fc"},
        {"ArabicShaping", "OMIT", "jt", "jg"},
        {"BidiMirroring", "*bg"},
        {"Scripts-3.1.0d4.beta", "sn"},
        //{"Scripts-1d4", "RANGE", "sn"},
        //{"Age", "*sn"},
         //*/
    /*
        {"Jamo", "jn"},
        //
    };
    /*
        {"UnicodeData-3.1.0d8.beta", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
        {"ExtraProperties", "xp"},

        {"EastAsianWidth-4d7.beta", "ea", "OMIT"},
        {"LineBreak-6d6.beta", "lb", "OMIT"},
        {"SpecialCasing-4d1.beta", "*sl", "*st", "*su", "sc"},
        {"CompositionExclusions-3d6.beta", "ce"},
        {"CaseFolding-3d4.beta", "OMIT", "*fc"},
        {"PropList-3.1.0d2.beta", "PROP", "OMIT"},
        {"ArabicShaping", "OMIT", "jt", "jg"},
        {"BidiMirroring", "*bg"},
        {"Scripts-1d4", "sn"},
        //{"Scripts-1d4", "RANGE", "sn"},
        //{"Age", "*sn"},
         //*/
    /*
        {"Jamo", "jn"},
        //

    //"NamesList-3.1.0d1.beta"

    static String[][] labelList30 = {
        // Labels for the incoming files. Labels MUST match field order in file.
        // IMPORTANT - defaults of form y-=x must occur after x is encountered!
        // The one exception is "st", which is handled specially.
        // So file order is important.
        //*
        {"UnicodeData", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
        {"CompositionExclusions", "ce"},
        {"EastAsianWidth", "ea", "OMIT"},
        {"LineBreak", "lb", "OMIT"},
        {"SpecialCasing", "*sl", "*st", "*su", "sc"},
        {"CaseFolding", "OMIT", "*fc"},
        {"ArabicShaping", "OMIT", "jt", "jg"},
        {"BidiMirroring", "*bg"},
        /*
        {"Jamo", "jn"},
        {"PropList.alpha", "RANGE", "OMIT"},
        //
    };

    static String[][] labelList11 = {
        {"UnicodeData-1.1", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
    };

    static String[][] labelList20 = {
        {"UnicodeData-2.0", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
    };

    static String[][] labelList21 = {
        {"UnicodeData-2.1", "n", "gc", "cc", "bc", "dm", "dd", "dv", "nv", "bm", "on", "OMIT", "*uc", "*lc", "*tc"},
    };
     */

    // handles
    public static final String blocksname = "Blocks";
    //public static final String[][] labelList;
    public static final boolean NEWPROPS = true;

    /*
    static {
        switch (major*10 + minor) {
        case 31:
            blocksname = blocksname31;
            labelList = labelList31;
            break;
        case 30:
            blocksname = blocksnamePlain;
            labelList = labelList30;
            break;
        case 21:
            blocksname = blocksnamePlain;
            labelList = labelList21;
            break;
        case 20:
            blocksname = blocksnamePlain;
            labelList = labelList20;
            break;
        default:
            blocksname = blocksnamePlain;
            labelList = labelList11;
            break;
        }
    }

     */
    static final String dataFilePrefix = "UCD_Data";


    // MAIN!!

    public static void main (String[] args) throws Exception {
        System.out.println("Building binary version of UCD");

        log = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(Settings.GEN_DIR + "UCD-log.txt"),
                        "UTF8"),
                        32*1024));
        log.write("\uFEFF"); // BOM

        try {
            for (final String arg : args) {
                String version = arg;
                if (version.length() == 0) {
                    version = Settings.latestVersion;
                }

                new ConvertUCD().toJava(version);
            }
        } finally {
            log.close();
        }
    }

    /*
    static void toXML() throws Exception {
        // Blocks is special
        // Unihan is special
        // collect all the other .txt files in the directory
        if (false) readBlocks();
        if (true) for (int i = 0; i < labelList.length; ++i) {
            readSemi(labelList[i]);
        } else {
            readSemi(labelList[0]); // TESTING ONLY
        }
        writeXML();
    }
     */

    void toJava(String version) throws Exception {
        this.version = version;
        final String[] parts = new String[3];
        Utility.split(version, '.', parts);
        major = Integer.parseInt(parts[0]);
        minor = Integer.parseInt(parts[1]);
        update = Integer.parseInt(parts[2]);
        System.out.println("Building " + version);
        // Blocks is special
        // Unihan is special
        // collect all the other .txt files in the directory
        if (false) {
            readBlocks();
        }
        if (true) {
            for (final String[] element : labelList) {
                readSemi(element);
            }
        } else {
            readSemi(labelList[0]); // TESTING ONLY
        }

        final Iterator it = charData.keySet().iterator();
        while (it.hasNext()) {
            final Object key = it.next();
            final UData value = (UData) charData.get(key);
            value.compact();
        }

        /*
        UData ud;
        ud = getEntry(0x5e);
        System.out.println("SPOT-CHECK: 5e: " + ud);

        ud = getEntry(0x130);
        System.out.println("SPOT-CHECK: 130: " + ud);

        ud = getEntry(0x1f6);
        System.out.println("SPOT-CHECK: 1f6: " + ud);

        ud = getEntry(0x2A6D6);
        System.out.println("SPOT-CHECK: 2A6D6: " + ud);

        ud = getEntry(0xFFFF);
        System.out.println("SPOT-CHECK: FFFF: " + ud);
         */

        writeJavaData();
    }

    static PrintWriter log;
    //static String directory = BASE_DIR;
    //static Map appendDuplicates = new HashMap();

    /** First item in labels is file name, rest are field names (skipping character).
     *  "OMIT" is special -- means don't record
     */


    List blockData = new LinkedList();

    void readBlocks() throws Exception {
        System.out.println("Reading 'Blocks'");
        final BufferedReader input = Utility.openUnicodeFile(blocksname, version, true, Utility.LATIN1);
        String line = "";
        try {
            final String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                line = input.readLine();
                if (line == null) {
                    break;
                }
                if (SHOW && (lineNumber % 500) == 0) {
                    System.out.println("//" + lineNumber + ": '" + line + "'");
                }

                //String original = line;
                String comment = "";
                final int commentPos = line.indexOf('#');
                if (commentPos >= 0) {
                    comment = line.substring(commentPos+1).trim();
                    line = line.substring(0, commentPos);
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                final int count = Utility.split(line,';',parts);
                if (count != 3) {
                    throw new ChainException("Bad count in Blocks", null);
                }
                blockData.add(new String[] {Utility.fromHex(parts[0]), Utility.fromHex(parts[1]), parts[2].trim()});
            }

        } catch (final Exception e) {
            System.out.println("Exception at: " + line);
            throw e;
        } finally {
            input.close();
        }
    }

    Set properties = new TreeSet();

    void readSemi(String[] labels) throws Exception {
        System.out.println();
        System.out.println("Reading '" + labels[0] + "'");
        if (major < 3 || (major == 3 && minor < 1)) {
            if (labels[0] == "PropList") {
                System.out.println("SKIPPING old format of Proplist for " + version);
                return;
            }
        }
        String tempVersion = version;
        if (version.equals(Settings.latestVersion)) {
            tempVersion = "";
        }
        final BufferedReader input = Utility.openUnicodeFile(labels[0], tempVersion, true, Utility.LATIN1);
        if (input == null) {
            System.out.println("COULDN'T OPEN: " + labels[0]);
            return;
        }
        boolean showedSemi = false;
        boolean showedShort = false;
        int minNumFields = labels.length;
        while (minNumFields > 0 && labels[minNumFields - 1].equals("OMIT")) {
            --minNumFields;
        }
        String line = "";

        try {
            final String[] parts = new String[20];
            for (int lineNumber = 1; ; ++lineNumber) {
                try {
                    line = input.readLine();
                    if (line == null) {
                        break;
                    }
                    if (SHOW && (lineNumber % 500) == 0) {
                        System.out.println("//" + lineNumber + ": '" + line + "'");
                    }

                    final String original = line;
                    String comment = "";
                    final int commentPos = line.indexOf('#');
                    if (commentPos >= 0) {
                        comment = line.substring(commentPos+1).trim();
                        line = line.substring(0, commentPos);
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }

                    int count = Utility.split(line,';',parts);

                    // fix malformed or simple lists.

                    if (count < labels.length) {
                        if (count == 1) { // fix simple list
                            ++count;
                            parts[1] = "Y";
                        } else if (count >= minNumFields) {
                            if (!showedShort) {
                                System.out.println("Line shorter than labels: " + original);
                            }
                            showedShort = true;
                            for (int i = count; i < labels.length; ++i) {
                                parts[i] = "";
                            }
                        } else {
                            System.out.println("Too few fields: " + original);
                            throw new ChainException("too few fields: {0}",
                                    new Object[] {new Integer(line), new Integer(count)});
                        }
                    } else if (count > labels.length) {
                        if (count == labels.length + 1 && parts[count-1].equals("")) {
                            if (!showedSemi) {
                                System.out.println("Extra semicolon in: " + original);
                            }
                            showedSemi = true;
                        } else {
                            System.out.println("Too many fields: " + original);
                            throw new ChainException("too many fields: {0}",
                                    new Object[] {new Integer(line), new Integer(count)});
                        }
                    }

                    // store char
                    // first field is always character OR range. May be UTF-32
                    int cpTop;
                    int cpStart;
                    final int ddot = parts[0].indexOf(".");
                    if (ddot >= 0) {
                        cpStart = UTF32.char32At(Utility.fromHex(parts[0].substring(0,ddot)),0);
                        cpTop = UTF32.char32At(Utility.fromHex(parts[0].substring(ddot+2)),0);
                        // System.out.println(Utility.hex(cpStart) + " ... " + Utility.hex(cpTop));
                    } else {
                        cpStart = UTF32.char32At(Utility.fromHex(parts[0]),0);
                        cpTop = cpStart;
                        if (labels[1].equals("RANGE")) {
                            UTF32.char32At(Utility.fromHex(parts[1]),0);
                        }
                    }

                    // properties first
                    if (labels[1].equals("PROP")) {
                        String prop = parts[2].trim();
                        // FIX!!
                        boolean skipLetters = false;
                        if (prop.equals("Alphabetic")) {
                            prop = "Other_Alphabetic";
                            skipLetters = true;
                        }
                        // END FIX!!
                        properties.add(prop);
                        if (Utility.find(prop, UCD_Names.DeletedProperties, true) == -1) { // only undeleted
                            int end = UTF32.char32At(Utility.fromHex(parts[1]),0);
                            if (end == 0) {
                                end = cpStart;
                            }

                            for (int j = cpStart; j <= end; ++j) {
                                if (j != UCD.mapToRepresentative(j, Integer.MAX_VALUE)) {
                                    continue;
                                }
                                if (skipLetters && getEntry(cpStart).isLetter()) {
                                    continue;
                                }
                                appendCharProperties(j, prop);
                            }
                        }
                    } else { // not range!
                        String val = "";
                        String lastVal;

                        for (int i = 1; i < labels.length; ++i) {
                            final String key = labels[i];
                            lastVal = val;
                            if (isHex.get(key) != null) {
                                val = Utility.fromHex(parts[i]);
                            } else {
                                val = parts[i].trim();
                            }
                            if (key.equals("OMIT"))
                            {
                                continue; // do after val, so lastVal is correct
                            }
                            if (key.equals("RANGE"))
                            {
                                continue; // do after val, so lastVal is correct
                            }
                            if (val.equals(""))
                            {
                                continue; // skip empty values, they mean default
                            }

                            for (int cps = cpStart; cps <= cpTop; ++cps) {
                                if (UCD.mapToRepresentative(cps, Integer.MAX_VALUE) != cps)
                                {
                                    continue;    // skip condensed ranges
                                }

                                if (key.equals("binary")) {
                                    appendCharProperties(cps, val);
                                } else if (key.equals("fc")) {
                                    final UData data = getEntry(cps);
                                    final String type = parts[i-1].trim();
                                    if (type.equals("F") || type.equals("C") || type.equals("E") || type.equals("L")) {
                                        data.fullCaseFolding = val;
                                        //System.out.println("*<" + parts[i-1] + "> Setting " + Utility.hex(cps) + ": " + Utility.hex(val));
                                    }
                                    if (type.equals("S") || type.equals("C") || type.equals("L")) {
                                        data.simpleCaseFolding = val;
                                        //System.out.println("<" + parts[i-1] + "> Setting " + Utility.hex(cps) + ": " + Utility.hex(val));
                                    }
                                    if (type.equals("I")) {
                                        data.simpleCaseFolding = val;
                                        setBinaryProperty(cps, CaseFoldTurkishI);
                                        if (DEBUG) {
                                            System.out.println("SPOT-CHECK: <" + parts[i-1] + "> Setting "
                                                    + Utility.hex(cps) + ": " + Utility.hex(val));
                                        }
                                    }
                                } else if (labels[0].equals("SpecialCasing")   // special handling for special casing
                                        && labels[4].equals("sc")
                                        && parts[4].trim().length() > 0) {
                                    if (i < 4) {
                                        if (DEBUG) {
                                            System.out.println("Got special: " + Utility.hex(cps) + ", "
                                                    + Utility.hex(key) + ":" + Utility.hex(val));
                                        }
                                        addCharData(cps, "sc", parts[4].trim() + ":" + key + ":" + val);
                                    }
                                } else {
                                    /*if (key.equals("sn")) { // SKIP UNDEFINED!!
					                    UData data = getEntryIfExists(cps);
					                    if (data == null || data.generalCategory == Cn) continue;
					                }
                                     */
                                    addCharData(cps, key, val);
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    System.err.println("*Exception at: " + line + ", " + e.getMessage());
                    throw e;
                    //System.err.println(e.getMessage());
                }
            }
        } catch (final Exception e) {
            System.out.println("Exception at: " + line + ", " + e.getMessage());
            throw e;
        } finally {
            input.close();
        }
        //printValues("JOINING_TYPE", jtSet);
        //printValues("JOINING_GROUP", jgSet);
    }

    static void printValues(String title, Set s) {
        Iterator it = s.iterator();
        System.out.println("public static String[] " + title + " = {");
        while (it.hasNext()) {
            final String value = (String) it.next();
            System.out.println("    \"" + value + "\",");
        }
        System.out.println("};");
        it = s.iterator();
        System.out.println("public static byte ");
        int count = 0;
        while (it.hasNext()) {
            final String value = (String) it.next();
            System.out.println("    " + value.replace(' ', '-').toUpperCase() + " = " + (count++) + ",");
        }
        System.out.println("    LIMIT_" + title + " = " + count);
        System.out.println(";");
    }

    Map charData = new TreeMap();

    /*
    static void writeXML() throws IOException {
        System.out.println("Writing 'UCD-Main.xml'");
        BufferedWriter output = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(UCD.BIN_DIR + "UCD_Data.xml"),
                "UTF8"),
            32*1024);

        try {
            // write header

            output.write("<?xml version='1.0' encoding='utf-8'?>\n");
            output.write("<UnicodeCharacterDatabase>\n");
            output.write(" <!-- IMPORTANT: see UCD-Notes.html for information on the format. This file CANNOT be read correctly without that information. -->\n");
            output.write(" <unicode version='" + major + "' minor='" + minor + "' update='" + update + "'/>\n");
            output.write(" <fileVersion status='DRAFT' date='" + new Date() + "'/>\n");

            // write blocks

            Iterator it = blockData.iterator();
            while (it.hasNext()) {
                String[] block = (String[]) it.next();
                output.write(" <block start='" + Utility.quoteXML(block[0])
                    + "' end='" + Utility.quoteXML(block[1])
                    + "' name='" + Utility.quoteXML(block[2])
                    + "'/>\n" );
            }

            // write char data

            it = charData.keySet().iterator();
            while (it.hasNext()) {
                Integer cc = (Integer) it.next();
                output.write(" <e c='" + Utility.quoteXML(cc.intValue()) + "'");
                /*
                UData data = (UData) charData.get(cc);
                Iterator dataIt = data.keySet().iterator();
                while (dataIt.hasNext()) {
                    String label = (String) dataIt.next();
                    if (label.equals("c")) continue; // already wrote it.
                    if (label.equals("fc")) {
                        String fc = getResolved(data, "fc");
                        String lc = getResolved(data, "lc");
                        if (!fc.equals(lc) && !lc.equals(cc)) log.println("FC " + fc.length() + ": " + toString(cc));
                    }
                    String value = Utility.quoteXML((String) data.get(label));
                    output.write(" " + label + "='" + value + "'");
                }
     *//*
                output.write("/>\n");
            }

            // write footer

            output.write("</UnicodeCharacterDatabase>\n");
        } finally {
            output.close();
        }
    }
      */

    void writeJavaData() throws IOException {
        final Iterator it = charData.keySet().iterator();
        final int codePoint = -1;
        System.out.println("Writing " + dataFilePrefix + version);
        final DataOutputStream dataOut = new DataOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(Settings.BIN_DIR +  dataFilePrefix + version + ".bin"),
                        128*1024));

        // write header
        dataOut.writeByte(BINARY_FORMAT);
        dataOut.writeByte(major);
        dataOut.writeByte(minor);
        dataOut.writeByte(update);
        final long millis = System.currentTimeMillis();
        dataOut.writeLong(millis);
        dataOut.writeInt(charData.size());
        System.out.println("Data Size: " + NumberFormat.getInstance().format(charData.size()));
        int count = 0;

        // write records
        try {
            // write char data

            while (it.hasNext()) {
                final Object cc = it.next();
                //codePoint = UTF32.char32At(cc,0);
                if (DEBUG) {
                    System.out.println(Utility.hex(cc));
                }

                final UData uData = (UData) charData.get(cc);
                if (false && uData.name == null) {
                    System.out.println("Warning: NULL name\n" + uData);
                    System.out.println();
                }
                if (false && uData.codePoint == 0x2801) {
                    System.out.println("SPOT-CHECK: " + uData);
                }
                uData.cleanup();
                uData.writeBytes(dataOut);
                count++;
                if (DEBUG) {
                    System.out.println("Setting2");
                }
            }
            System.out.println("Wrote Data " + count);
        } catch (final Exception e) {
            throw new ChainException("Bad data write {0}", new Object [] {Utility.hex(codePoint)}, e);
        } finally {
            dataOut.close();
        }
    }

    //static String[] xsSplit = new String[40];

    // Cache a little bit for speed
    int getEntryCodePoint = -1;
    UData getEntryUData = null;

    UData getEntryIfExists(int cp) {
        if (cp == getEntryCodePoint) {
            return getEntryUData;
        }
        final Integer cc = new Integer(cp);
        final UData charEntry = (UData) charData.get(cc);
        if (charEntry == null) {
            return null;
        }
        getEntryCodePoint = cp;
        getEntryUData = charEntry;
        return charEntry;
    }

    /* Get entry in table for cc
     */
    UData getEntry(int cp) {
        if (cp == getEntryCodePoint) {
            return getEntryUData;
        }
        final Integer cc = new Integer(cp);
        UData charEntry = (UData) charData.get(cc);
        if (charEntry == null) {
            charEntry = new UData(cp);
            charData.put(cc, charEntry);
            //charEntry.put("c", cc);
        }
        getEntryCodePoint = cp;
        getEntryUData = charEntry;
        return charEntry;
    }
    /** Adds the character data. Signals duplicates with an exception
     */

    void setBinaryProperty(int cp, int binProp) {
        final UData charEntry = getEntry(cp);
        charEntry.binaryProperties |= (1L << binProp);
    }

    void appendCharProperties(int cp, String key) {
        int ind;
        //if (true || NEWPROPS) {
        ind = Utility.lookup(key, UCD_Names.BP, true);
        /*} else {
            ind = Utility.lookup(key, UCD_Names.BP_OLD);
        }
         */
        //charEntry.binaryProperties |= (1 << ind);
        setBinaryProperty(cp, ind);
    }

    Set jtSet = new TreeSet();
    Set jgSet = new TreeSet();

    /** Adds the character data. Signals duplicates with an exception
     */
    void addCharData(int cp, String key, String value) {
        //if (cp < 10) System.out.println("A: " + Utility.hex(cp) + ", " + key + ", " + Utility.quoteJavaString(value));
        final UData charEntry = getEntry(cp);
        //if (cp < 10) System.out.println("   " + charEntry);

        if (SHOW_SAMPLE && cp == 0x221) {
            System.out.println("Sample: " + cp + ", " + key + ", " + value);
            System.out.println(charEntry);
        }

        if (key.equals("bm")) {
            if (value.equals("Y")) {
                charEntry.binaryProperties |= 1;
            }
        } else if (key.equals("ce")) {
            charEntry.binaryProperties |= 2;
        } else if (key.equals("on")) {
            if (charEntry.name.charAt(0) == '<') {
                charEntry.name = '<' + value + '>';
            }
        } else if (key.equals("dm")) {
            charEntry.decompositionType = CANONICAL;
            if (value.charAt(0) == '<') {
                final int pos = value.indexOf('>');
                String dType = value.substring(1,pos);
                if (major < 2) {
                    if (dType.charAt(0) == '+') {
                        dType = dType.substring(1);
                    }
                }
                value = value.substring(pos+1);
                setField(charEntry, "dt", dType);
            }
            // FIX OLD
            if (major < 2) {
                int oldStyle = value.indexOf('<');
                if (oldStyle > 0) {
                    value = value.substring(0,oldStyle);
                }
                oldStyle = value.indexOf('{');
                if (oldStyle > 0) {
                    value = value.substring(0,oldStyle);
                }
            }
            setField(charEntry, key, Utility.fromHex(value));

            // fix the numeric fields to be more sensible
        } else if (key.equals("dd")) {
            if (charEntry.numericType < UCD_Types.DECIMAL) {
                charEntry.numericType = UCD_Types.DECIMAL;
            }
            setField(charEntry, "nv", value);
        } else if (key.equals("dv")) {
            if (charEntry.numericType < UCD_Types.DIGIT) {
                charEntry.numericType = UCD_Types.DIGIT;
            }
            setField(charEntry, "nv", value);
        } else if (key.equals("nv")) {
            if (charEntry.numericType < UCD_Types.NUMERIC) {
                charEntry.numericType = UCD_Types.NUMERIC;
            }
            setField(charEntry, "nv", value);
            /*} else if (key.equals("jt")) {
            jtSet.add(value);
        } else if (key.equals("jg")) {
            jgSet.add(value);
             */
        } else {
            setField(charEntry, key, value);
        }
        if (SHOW_SAMPLE && cp == 0x221) {
            System.out.println("Sample Result:");
            System.out.println(charEntry);
        }

    }

    public void setField(UData uData, String fieldName, String fieldValue) {
        try {
            if (fieldName.equals("n")) {
                uData.name = fieldValue;
            } else if (fieldName.equals("dm")) {
                uData.decompositionMapping = fieldValue;
            } else if (fieldName.equals("bg")) {
                uData.bidiMirror = fieldValue;
            } else if (fieldName.equals("uc")) {
                uData.simpleUppercase = fieldValue;
            } else if (fieldName.equals("lc")) {
                uData.simpleLowercase = fieldValue;
            } else if (fieldName.equals("tc")) {
                uData.simpleTitlecase = fieldValue;

            } else if (fieldName.equals("su")) {
                uData.fullUppercase = fieldValue;
            } else if (fieldName.equals("sl")) {
                if (DEBUG) {
                    System.out.println("Setting full lowercase to " + Utility.hex(fieldValue) + uData);
                }
                uData.fullLowercase = fieldValue;
            } else if (fieldName.equals("st")) {
                uData.fullTitlecase = fieldValue;

            } else if (fieldName.equals("sc")) {
                if (uData.specialCasing.length() > 0) {
                    uData.specialCasing += ";";
                }
                uData.specialCasing += fieldValue;

            } else if (fieldName.equals("xp")) {
                uData.binaryProperties |= 1L << Utility.lookup(fieldValue, UCD_Names.BP, true);
                //UCD_Names.BP_OLD

            } else if (fieldName.equals("gc")) {
                uData.generalCategory = Utility.lookup(fieldValue, UCD_Names.GENERAL_CATEGORY, true);
                //                if (major >= 5 && uData.script == Unknown_Script
                //                		&& uData.generalCategory != Cn
                //                		&& uData.generalCategory != Cs
                //                		&& uData.generalCategory != Co) {
                //                	uData.script = COMMON_SCRIPT;
                //                	System.out.println("Resetting to Common Script: " + Utility.hex(uData.codePoint));
                //                }
            } else if (fieldName.equals("bc")) {
                uData.bidiClass = Utility.lookup(fieldValue, UCD_Names.BIDI_CLASS, true);
            } else if (fieldName.equals("dt")) {
                if (major < 2) {
                    if (fieldValue.equals("no-break")) {
                        fieldValue = "noBreak";
                    } else if (fieldValue.equals("circled")) {
                        fieldValue = "circle";
                    } else if (fieldValue.equals("sup")) {
                        fieldValue = "super";
                    } else if (fieldValue.equals("break")) {
                        fieldValue = "compat";
                    } else if (fieldValue.equals("font variant")) {
                        fieldValue = "font";
                    } else if (fieldValue.equals("no-join")) {
                        fieldValue = "compat";
                    } else if (fieldValue.equals("join")) {
                        fieldValue = "compat";
                    }
                }
                uData.decompositionType = Utility.lookup(fieldValue, UCD_Names.LONG_DECOMPOSITION_TYPE, true);
            } else if (fieldName.equals("nt")) {
                uData.numericType = Utility.lookup(fieldValue, UCD_Names.LONG_NUMERIC_TYPE, true);

            } else if (fieldName.equals("ea")) {
                uData.eastAsianWidth = Utility.lookup(fieldValue, UCD_Names.EAST_ASIAN_WIDTH, true);
            } else if (fieldName.equals("lb")) {
                uData.lineBreak = Utility.lookup(fieldValue, UCD_Names.LINE_BREAK, true);
            } else if (fieldName.equals("sn")) {
                uData.script = Utility.lookupShort(fieldValue, UCD_Names.LONG_SCRIPT, true);
            } else if (fieldName.equals("jt")) {
                uData.joiningType = Utility.lookup(fieldValue, UCD_Names.JOINING_TYPE, true);
            } else if (fieldName.equals("jg")) {
                final byte temp = (byte)Utility.find(fieldValue, UCD_Names.OLD_JOINING_GROUP, true);
                if (temp != -1) {
                    uData.joiningGroup = temp;
                } else {
                    uData.joiningGroup = Utility.lookup(fieldValue, UCD_Names.JOINING_GROUP, true);
                }

            } else if (fieldName.equals("nv")) {
                if (major < 2) {
                    if (fieldValue.equals("-")) {
                        return;
                    }
                }
                uData.numericValue = Utility.doubleFrom(fieldValue);
            } else if (fieldName.equals("ccc")) {
                uData.combiningClass = (byte)Utility.intFrom(fieldValue);
                if (uData.combiningClass == 9 && major >= 5) {
                    if (DEBUG) {
                        System.out.println("setting Grapheme_Link " + Utility.hex(uData.codePoint) + "\t" + uData.name);
                    }
                    uData.binaryProperties |= (1<<GraphemeLink);
                    if (DEBUG) {
                        System.out.println(uData);
                    }
                }
            } else if (fieldName.equals("bp")) {
                uData.binaryProperties = (byte)Utility.longFrom(fieldValue);
                //                if (major >= 5 && (uData.binaryProperties & 1<<Noncharacter_Code_Point) != 0) {
                //                  uData.script = Unknown_Script;
                //                }
                System.out.println("Resetting: " + uData);
            } else if (fieldName.equals("bpb")) {
                uData.Bidi_Paired_Bracket = Integer.parseInt(fieldValue, 16);
            } else if (fieldName.equals("bpt")) {
                uData.Bidi_Paired_Bracket_Type = Utility.lookup(fieldValue, UCD_Names.Bidi_Paired_Bracket_Type_SHORT, true);;
            } else {
                throw new IllegalArgumentException("Unknown fieldName");
            }
        } catch (final Exception e) {
            throw new ChainException(
                    "Bad field name= \"{0}\", value= \"{1}\"", new Object[] {fieldName, fieldValue}, e);
        }
    }

}