/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/FileLineIterator.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Opens a file, and iterates through the lines in the file.
 * Options allow trimming and comment handling, and splitting
 */
public class FileLineIterator {
    static public final char NOTCHAR = '\uFFFF';

    // public writable
    public boolean doCounter = true;
    public int lineLimit = Integer.MAX_VALUE;
    public char commentChar = '#';  // NOTCHAR if no comments
    public boolean showFilename = true;

    // public readable
    public String originalLine = "";
    public String cleanedLine = "";
    public int counter = 0;

    private BufferedReader br = null;
    private Utility.Encoding encoding = Utility.UTF8;

    /**
     * Open the file for reading. If useGenDir is set, use the normal generation directory
     */
    public void open(String filename, Utility.Encoding encoding) throws IOException {
        if (showFilename) {
            Utility.fixDot();
            System.out.println("Reading File: " + new File(filename).getCanonicalPath());
        }
        br = Utility.openReadFile(filename, encoding);
        this.encoding = encoding;
    }

    /**
     * Fetch a non-zero-length line from the file, stripping comments & using counter, according to settings.
     */
    public String read() throws IOException {
        while (true) {
            if (counter >= lineLimit) {
                return null;
            }
            cleanedLine = originalLine = br.readLine();
            if (doCounter) {
                Utility.dot(counter++);
            }
            if (cleanedLine == null) {
                return null;
            }

            // drop BOM
            if (encoding == Utility.UTF8 && counter == 0 && cleanedLine.length() > 0 && cleanedLine.charAt(0) == 0xFEFF) {
                cleanedLine = cleanedLine.substring(1);
            }

            // drop comment
            if (commentChar != NOTCHAR) {
                final int commentPos = cleanedLine.indexOf(commentChar);
                if (commentPos >= 0) {
                    cleanedLine = cleanedLine.substring(0, commentPos);
                }
            }
            cleanedLine = cleanedLine.trim();
            if (cleanedLine.length() != 0) {
                return cleanedLine;
            }
        }
    }

    public int readSplit(String[] results, char delimiter) throws IOException {
        final String line = read();
        if (line == null) {
            return 0;
        }
        return Utility.split(line, delimiter, results);
    }

    public void close() throws IOException {
        Utility.fixDot();
        br.close();
    }
}
