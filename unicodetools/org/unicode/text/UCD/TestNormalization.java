/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/TestNormalization.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.props.BagFormatter;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public final class TestNormalization {
    static final String DIR = Settings.UnicodeTools.UCD_DIR + "Update 3.0.1/";
    static final boolean SKIP_FILE = true;

    static PrintWriter out = null;
    static BufferedReader in = null;

    static BitSet charsListed = new BitSet(0x110000);
    static int errorCount = 0;
    static int lineErrorCount = 0;
    static String originalLine = "";
    static String lastLine = "";

    public static void main(String[] args)  throws java.io.IOException {
        System.out.println("Creating Normalizers");


        final String[] testSet = {"a\u0304\u0328", "a\u0328\u0304"};
        for (final String s : testSet) {
            final boolean test = Default.nfc().isFCD(s);
            System.out.println(test + ": " + Default.ucd().getCodeAndName(s));
        }


        final String x = UTF32.valueOf32(0x10000);
        check("NFC", Default.nfc(), x);
        check("NFD", Default.nfd(), x);
        check("NFKC", Default.nfkc(), x);
        check("NFKD", Default.nfkd(), x);


        out = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream("NormalizationTestLog.txt"),
                                "UTF8"),
                                32*1024));

        in = new BufferedReader (
                new FileReader (DIR + "NormalizationTest.txt"),
                32*1024);

        try {
            final String[] parts = new String[10];

            System.out.println("Checking files");

            int count = 0;

            while (true) {
                String line = in.readLine();
                if ((count++ & 0x3FF) == 0) {
                    System.out.println("#LINE: " + line);
                }
                if (line == null) {
                    break;
                }
                originalLine = line;
                final int pos = line.indexOf('#');
                if (pos >= 0) {
                    line = line.substring(0,pos);
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }


                final int splitCount = Utility.split(line, ';', parts);
                // FIX check splitCount
                for (int i = 0; i < splitCount; ++i) {
                    parts[i] = Utility.fromHex(parts[i]);
                }
                if (UTF16.countCodePoint(parts[0]) == 1) {
                    final int code = parts[0].codePointAt(0);
                    charsListed.set(code);
                    if ((code & 0x3FF) == 0) {
                        System.out.println("# " + Utility.hex(code));
                    }
                }

                // c2 == NFC(c1) == NFC(c2) == NFC(c3)
                errorCount += check("NFCa", Default.nfc(), parts[1], parts[0]);
                errorCount += check("NFCb", Default.nfc(), parts[1], parts[1]);
                errorCount += check("NFCc", Default.nfc(), parts[1], parts[2]);

                // c4 == NFC(c4) == NFC(c5)
                errorCount += check("NFCd", Default.nfc(), parts[3], parts[3]);
                errorCount += check("NFCe", Default.nfc(), parts[3], parts[4]);

                // c3 == NFD(c1) == NFD(c2) == NFD(c3)
                errorCount += check("NFDa", Default.nfd(), parts[2], parts[0]);
                errorCount += check("NFDb", Default.nfd(), parts[2], parts[1]);
                errorCount += check("NFDc", Default.nfd(), parts[2], parts[2]);

                // c5 == NFD(c4) == NFD(c5)
                errorCount += check("NFDd", Default.nfd(), parts[4], parts[3]);
                errorCount += check("NFDe", Default.nfd(), parts[4], parts[4]);

                // c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)
                errorCount += check("NFKCa", Default.nfkc(), parts[3], parts[0]);
                errorCount += check("NFKCb", Default.nfkc(), parts[3], parts[1]);
                errorCount += check("NFKCc", Default.nfkc(), parts[3], parts[2]);
                errorCount += check("NFKCd", Default.nfkc(), parts[3], parts[3]);
                errorCount += check("NFKCe", Default.nfkc(), parts[3], parts[4]);

                // c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)
                errorCount += check("NFKDa", Default.nfkd(), parts[4], parts[0]);
                errorCount += check("NFKDb", Default.nfkd(), parts[4], parts[1]);
                errorCount += check("NFKDc", Default.nfkd(), parts[4], parts[2]);
                errorCount += check("NFKDd", Default.nfkd(), parts[4], parts[3]);
                errorCount += check("NFKDe", Default.nfkd(), parts[4], parts[4]);
            }
            System.out.println("Total errors in file: " + errorCount
                    + ", lines: " + lineErrorCount);
            errorCount = lineErrorCount = 0;

            System.out.println("Checking Missing");
            checkMissing();
            System.out.println("Total errors in unlisted items: " + errorCount
                    + ", lines: " + lineErrorCount);

        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    static String lastBase = "";

    public static int check(String type, Normalizer n, String base, String other) {
        try {
            final String trans = n.normalize(other);
            if (!trans.equals(base)) {
                String temp = "";
                if (!lastLine.equals(originalLine)) {
                    temp = "// " + originalLine;
                    lastLine = originalLine;
                }
                if (!base.equals(lastBase)) {
                    lastBase = base;
                    lineErrorCount++;
                }
                String otherList = "";
                if (!base.equals(other)) {
                    otherList = "(" + Default.ucd().getCodeAndName(other) + ")";
                }
                out.println("DIFF " + type + ": "
                        + Default.ucd().getCodeAndName(base) + " != "
                        + type
                        + otherList
                        + " == " + Default.ucd().getCodeAndName(trans)
                        + temp
                        );
                return 1;
            }
        } catch (final Exception e) {
            throw new ChainException("DIFF " + type + ": "
                    + Default.ucd().getCodeAndName(base) + " != "
                    + type + "(" + Default.ucd().getCodeAndName(other) + ")", new Object[]{}, e);
        }
        return 0;
    }

    public static int check(String type, Normalizer n, String base) {
        return check(type, n, base, base);
    }

    static void checkMissing() {
        for (int missing = 0; missing < 0x100000; ++missing) {
            if ((missing & 0xFFF) == 0) {
                System.out.println("# " + Utility.hex(missing));
            }
            if (charsListed.get(missing)) {
                continue;
            }
            final String x = UTF32.valueOf32(missing);
            errorCount += check("NFC", Default.nfc(), x);
            errorCount += check("NFD", Default.nfd(), x);
            errorCount += check("NFKC", Default.nfkc(), x);
            errorCount += check("NFKD", Default.nfkd(), x);
        }
    }

    public static void checkStarters () {
        System.out.println("Checking Starters");
        final UnicodeSet leading = new UnicodeSet();
        final UnicodeSet trailing = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (Default.nfc().isLeading(i)) {
                leading.add(i);
            }
            if (Default.ucd().getCombiningClass(i) != 0) {
                continue;
            }
            if (Default.nfc().isTrailing(i)) {
                trailing.add(i);
            }
        }
        System.out.println("Leading: " + leading.size());
        System.out.println("Trailing Starters: " + trailing.size());
        final UnicodeSetIterator lead = new UnicodeSetIterator(leading);
        final UnicodeSetIterator trail = new UnicodeSetIterator(trailing);
        final UnicodeSet followers = new UnicodeSet();
        final Map map = new TreeMap(new CompareProperties.UnicodeSetComparator());
        while (lead.next()) {
            trail.reset();
            followers.clear();
            while (trail.next()) {
                if (Default.nfc().getComposition(lead.codepoint, trail.codepoint) != 0xFFFF) {
                    followers.add(trail.codepoint);
                }
            }
            if (followers.size() == 0) {
                continue;
            }
            System.out.println(Default.ucd().getCode(lead.codepoint)
                    + "\t" + followers.toPattern(true));
            UnicodeSet possLead = (UnicodeSet) map.get(followers);
            if (possLead == null) {
                possLead = new UnicodeSet();
                map.put(followers.clone(), possLead);
            }
            possLead.add(lead.codepoint);
        }
        final Iterator it = map.keySet().iterator();
        final BagFormatter bf = new BagFormatter();
        bf.setLineSeparator("<br>");
        bf.setLabelSource(null);
        bf.setAbbreviated(true);
        while (it.hasNext()) {
            final UnicodeSet t = (UnicodeSet) it.next();
            final UnicodeSet l = (UnicodeSet) map.get(t);
            System.out.println("<tr><td>"
                    + bf.showSetNames(l)
                    + "</td><td>"
                    + bf.showSetNames(t)
                    + "</td></tr>");
        }
    }
}
