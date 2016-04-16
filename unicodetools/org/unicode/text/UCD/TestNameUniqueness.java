/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/TestNameUniqueness.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.UnicodeProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class TestNameUniqueness implements UCD_Types {

    public static void main(String[] args) throws Exception {
        checkNameList();
        // new TestNameUniqueness().checkNames();
    }

    Map names = new HashMap();
    int[] charCount = new int[128];
    int[] samples = new int[128];

    public static class NameIterator {
        int fileCount = -1;
        String line;
        BufferedReader br;
        String[] pieces = new String[3];
        /**
         * @return null when done
         */
        static String[][] files = {
            {Settings.DATA_DIR + "", "pdam1040630.lst"},
            {Settings.UCD_DIR + "4.1.0-Update/", "NamedCompositeEntities-4.1.0d2.txt"}
        };

        public String next() {
            while (true) {
                try {
                    if (br != null) {
                        line = br.readLine();
                    }
                    if (line == null) {
                        fileCount++;
                        br = FileUtilities.openReader(files[fileCount][0], files[fileCount][1], "ISO-8859-1");
                        line = br.readLine();
                    }
                } catch (final IOException e) {}
                if (line == null) {
                    return null;
                }
                if (line.length() == 0) {
                    continue;
                }
                if (fileCount == 0) {
                    final char c = line.charAt(0);
                    // skip if doesn't start with hex digit
                    if (!(('0' <= c && c <= '9') || ('A' <= c && c <= 'F'))) {
                        continue;
                    }
                    Utility.split(line,'\t',pieces,true);
                    Utility.split(pieces[1],'(',pieces,true);
                    Utility.split(pieces[0],'*',pieces,true);
                    return pieces[0];
                } else {
                    Utility.split(line,';',pieces,true);
                    return pieces[1];
                }
                //throw new IllegalArgumentException("Illegal file type");
            }
        }
    }

    public static void checkNameList() throws IOException {
        final Map map = new HashMap();
        final NameIterator nameIterator = new NameIterator();
        int lineCount = 0;
        while (true) {
            final String name = nameIterator.next();
            if (name == null) {
                break;
            }
            String key;
            try {
                if (name.startsWith("<")) {
                    key = name;
                } else {
                    key = UnicodeProperty.toNameSkeleton(name);
                }
            } catch (final RuntimeException e) {
                System.out.println("Error on " + nameIterator.line);
                throw e;
            }
            final Object value = map.get(key);
            if (value != null && !key.startsWith("<")) {
                System.out.println("*!*!*!* Collision at " + key + " between: ");
                System.out.println("\t" + value);
                System.out.println("\t" + nameIterator.line);
                //throw new IllegalArgumentException();
            }
            map.put(key, nameIterator.line);
            if (nameIterator.line.startsWith("116C")
                    || nameIterator.line.startsWith("1180")
                    || name.indexOf('-') >= 0
                    || (lineCount++ % 1000) == 0) {
                System.out.println("[" + lineCount + "]\t" + nameIterator.line + "\t" + name);
                System.out.println("\t" + name);
                System.out.println("\t" + key);
            }
        }
    }

    void checkNames() throws IOException {
        final PrintWriter out = Utility.openPrintWriterGenDir("log/name_uniqueness.txt", Utility.LATIN1_WINDOWS);
        try {
            out.println("Collisions");
            out.println();
            for (int cp = 0; cp < 0x10FFFF; ++cp) {
                Utility.dot(cp);
                if (!Default.ucd().isAllocated(cp)) {
                    continue;
                }
                if (Default.ucd().hasComputableName(cp)) {
                    continue;
                }
                final int cat = Default.ucd().getCategory(cp);
                if (cat == Cc) {
                    continue;
                }

                final String name = Default.ucd().getName(cp);
                final String processedName = processName(cp, name);
                final Integer existing = (Integer) names.get(processedName);
                if (existing != null) {
                    out.println("Collision between: "
                            + Default.ucd().getCodeAndName(existing.intValue())
                            + ", " + Default.ucd().getCodeAndName(cp));
                } else {
                    names.put(processedName, new Integer(cp));
                }
            }
            out.println();
            out.println("Samples");
            out.println();
            for (int i = 0; i < charCount.length; ++i) {
                final int count = charCount[i];
                if (count == 0) {
                    continue;
                }
                final String sampleName = Default.ucd().getCodeAndName(samples[i]);
                out.println(count + "\t'" + ((char)i)
                        + "'\t" + Default.ucd().getCodeAndName(samples[i])
                        + "\t=>\t" + processName(samples[i], Default.ucd().getName(samples[i])));
            }
            out.println();
            out.println("Name Samples");
            out.println();
            for (int i = 0; i < 256; ++i) {
                final int cat = Default.ucd().getCategory(i);
                if (cat == Cc) {
                    continue;
                }
                out.println(Default.ucd().getCodeAndName(i)
                        + "\t=>\t" + processName(i, Default.ucd().getName(i)));
            }
        } finally {
            out.close();
        }
    }

    static final String[][] replacements = {
        //{"SMALL LETTER", ""},
        {"LETTER", ""},
        {"CHARACTER", ""},
        {"DIGIT", ""},
        {"SIGN", ""},
        //{"WITH", ""},
    };

    StringBuffer processNamesBuffer = new StringBuffer();

    String processName(int codePoint, String name) {
        name = Utility.replace(name, replacements);
        processNamesBuffer.setLength(0);
        for (int i = 0; i < name.length(); ++i) {
            final char c = name.charAt(i);
            ++charCount[c];
            if (samples[c] == 0) {
                samples[c] = codePoint;
            }
            if ('A' <= c && c <= 'Z'
                    || '0' <= c && c <= '9') {
                processNamesBuffer.append(c);
            }

        }
        if (processNamesBuffer.length() == name.length()) {
            return name;
        }
        return processNamesBuffer.toString();
    }
}

