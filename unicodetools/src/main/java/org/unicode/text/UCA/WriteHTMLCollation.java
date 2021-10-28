/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/WriteHTMLCollation.java,v $
 *
 *******************************************************************************
 */

//WARNING: OLD FILE. DON"T COMPILE.

package org.unicode.text.UCA;

import org.unicode.text.UCD.UCD_Types;

public class WriteHTMLCollation implements UCD_Types {

    /*    public static final String copyright =
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    static final boolean EXCLUDE_UNSUPPORTED = true;
    static final boolean GENERATED_NFC_MISMATCHES = true;
    static final boolean DO_CHARTS = true;
    static final boolean WRITE_NAME_IN_CONFORMANCE = true;


    static UCA collator;
    static char unique = '\u0000';
    static TreeMap sortedD = new TreeMap();
    static TreeMap sortedN = new TreeMap();
    static HashMap backD = new HashMap();
    static HashMap backN = new HashMap();
    static TreeMap duplicates = new TreeMap();
    static int duplicateCount = 0;
    static PrintWriter log;

    static UCD ucd;
    static Normalizer nfc, nfd, nfkd, nfkc;

    public static void main(String args[]) throws IOException {

        checkImplicit();
        checkFixes();

        String unicodeVersion = "";

        System.out.println("Building UCA");
        collator = new UCA(null, unicodeVersion);
        nfc = new Normalizer(NFC, unicodeVersion);
        nfkc = new Normalizer(NFKC, unicodeVersion);
        nfd = new Normalizer(NFD, unicodeVersion);
        nfkd = new Normalizer(NFKD, unicodeVersion);

        System.out.println("Building UCD data (old)");
        //UInfo.init();
        ucd = UCD.make("");


        Normalizer foo = new Normalizer(Normalizer.NFKD);
        char x = '\u1EE2';
        System.out.println(UCA.hex(x) + " " + ucd.getName(x));
        String nx = foo.normalize(x);
        for (int i = 0; i < nx.length(); ++i) {
            char c = nx.charAt(i);
            System.out.println(ucd.getCanonicalClass(c));
        }
        System.out.println(UCA.hex(nx, " ") + " " + ucd.getName(nx));


        // DO FOLLOWING
        //writeConformance("CollationTest_NON_IGNORABLE.txt", UCA.NON_IGNORABLE);
        //writeConformance("CollationTest_SHIFTED.txt", UCA.SHIFTED);

        // SKIP BELOW
        if (true) return;

        writeFractionalUCA("FractionalUCA.txt");
        writeRules(WITH_NAMES);
        writeRules(WITHOUT_NAMES);

        testCompatibilityCharacters();

        String s = collator.getSortKey("\u1025\u102E", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0595\u0325") + ", " + collator.toString(s));
        String t = collator.getSortKey("\u0596\u0325", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0596\u0325") + ", " + collator.toString(t));



        writeCollationValidityLog();

        writeCaseExceptions();
        writeCaseFolding();
        System.out.println("Done");
    }

    private static final String DIR = "c:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.0.1/";
    private static final String DIR31 = "c:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.1/";

    static public void writeCaseExceptions() {
        System.err.println("Writing Case Exceptions");
        //Normalizer NFKC = new Normalizer(Normalizer.NFKC);
        for (char a = 0; a < 0xFFFF; ++a) {
            if (!ucd.isRepresented(a)) continue;
            //if (0xA000 <= a && a <= 0xA48F) continue; // skip YI

            String b = Case.fold(a);
            String c = nfkc.normalize(b);
            String d = Case.fold(c);
            String e = nfkc.normalize(d);
            if (!e.equals(c)) {
                System.out.println(Utility.hex(a) + "; " + Utility.hex(d, " ") + " # " + ucd.getName(a));

                System.out.println(Utility.hex(a)
                + ", " + Utility.hex(b, " ")
                + ", " + Utility.hex(c, " ")
                + ", " + Utility.hex(d, " ")
                + ", " + Utility.hex(e, " "));

                System.out.println(ucd.getName(a)
                + ", " + ucd.getName(b)
                + ", " + ucd.getName(c)
                + ", " + ucd.getName(d)
                + ", " + ucd.getName(e));

            }
            String f = Case.fold(e);
            String g = nfkc.normalize(f);
            if (!f.equals(d) || !g.equals(e)) System.out.println("!!!!!!SKY IS FALLING!!!!!!");
        }
    }

    static public void writeCaseFolding() throws IOException {
        System.err.println("Writing Javascript data");
        BufferedReader in = new BufferedReader(new FileReader(DIR31 + "CaseFolding-3.d3.alpha.txt"), 64*1024);
        log = new PrintWriter(new FileOutputStream("CaseFolding_data.js"));
        log.println("var CF = new Object();");
        int count = 0;
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            int comment = line.indexOf('#');                    // strip comments
            if (comment != -1) line = line.substring(0,comment);
            if (line.length() == 0) continue;
            int semi1 = line.indexOf(';');
            int semi2 = line.indexOf(';', semi1+1);
            int semi3 = line.indexOf(';', semi2+1);
            char type = line.substring(semi1+1,semi2).trim().charAt(0);
            if (type == 'C' || type == 'F' || type == 'T') {
                String code = line.substring(0,semi1).trim();
                String result = " " + line.substring(semi2+1,semi3).trim();
                result = replace(result, ' ', "\\u");
                log.println("\t CF[0x" + code + "]='" + result + "';");
                count++;
            }
        }
        log.println("// " + count + " case foldings total");

        in.close();
        log.close();
    }

    static public String replace(String source, char toBeReplaced, String toReplace) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == toBeReplaced) {
                result.append(toReplace);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    static void writeConformance(String filename, byte option)  throws IOException {
        PrintWriter log = Utility.openPrintWriter(filename);

        log.write('\uFEFF');

        System.out.println("Sorting");

        for (int i = 0; i <= 0xFFFF; ++i) {
            char c = (char)i;
            if (!ucd.isRepresented(c)) continue;
            //if (0xA000 <= c && c <= 0xA48F) continue; // skip YI
            addStringX(c, option);
        }


        Hashtable multiTable = collator.getContracting();
        Enumeration enum = multiTable.keys();
        while (enum.hasMoreElements()) {
            addStringX((String)enum.nextElement(), option);
        }

        for (int i = 0; i < extras.length; ++i) { // put in sample non-characters
            addStringX(extras[i], option);
        }

        for (int i = 0; i < extraRanges.length; ++i) {
            char start = extraRanges[i][0];
            char end = extraRanges[i][1];
            int increment = ((end - start + 1) / 303) + 1;
            //System.out.println("Range: " + start + ", " + end + ", " + increment);
            addStringX(start, option);
            for (char j = (char)(start+1); j < end-1; j += increment) {
                addStringX(j, option);
                addStringX(j+1, option);
            }
            addStringX(end-1, option);
            addStringX(end, option);
        }

        System.out.println("Total: " + sortedD.size());
        Iterator it;

        System.out.println("Writing");
        //String version = collator.getVersion();

        it = sortedD.keySet().iterator();

        String lastKey = "";

        while (it.hasNext()) {
            String key = (String) it.next();
            String source = (String) sortedD.get(key);
            key = key.substring(0, key.length()-2);
            //String status = key.equals(lastKey) ? "*" : "";
            //lastKey = key;
            //log.println(source);
            String clipped = source.substring(0, source.length()-1);
            String stren = source.substring(source.length()-1);
            log.print(source + "\t" + Utility.hex(clipped));
            if (WRITE_NAME_IN_CONFORMANCE) {
                log.print(
                    "\t" + ucd.getName(clipped)+ "\t" + UCA.toString(key));
            }
            log.println();
        }

        log.close();
        sortedD.clear();
        System.out.println("Done");
    }


    static void addStringX(int x, byte option) {
        addStringX(String.valueOf((char)x), option);
    }

    static void addStringX(String s, byte option) {
        addStringY(s + 'a', option);
        addStringY(s + 'A', option);
        addStringY(s + '�', option);
        addStringY(s + 'b', option);
        addStringY(s + '\u0325', option);
        addStringY(s + '_', option);
    }

    static char counter;

    static void addStringY(String s, byte option) {
        String colDbase = collator.getSortKey(s, option, true) + "\u0000" + s.charAt(0);
        sortedD.put(colDbase, s);
    }

     *//**
     * Check that the primaries are the same as the compatibility decomposition.
     *//*
    static void checkBadDecomps(int strength, boolean decomposition) {
        int oldStrength = collator.getStrength();
        collator.setStrength(strength);
        //Normalizer nfkd = new Normalizer(Normalizer.NFKD);
        if (strength == 1) {
            log.println("<h2>3. Primaries Incompatible with Decompositions</h2><table border='1'>");
        } else {
            log.println("<h2>4. Secondaries Incompatible with Decompositions</h2><table border='1'>");
        }
        log.println("<tr><th>Code</td><th>Sort Key</th><th>Decomposed Sort Key</th><th>Name</th></tr>");
        for (char ch = 0; ch < 0xFFFF; ++ch) {
            if (nfkd.isNormalized(ch)) continue;
            if (ch > 0xAC00 && ch < 0xD7A3) continue; // skip most of Hangul
            String sortKey = collator.getSortKey(String.valueOf(ch), UCA.NON_IGNORABLE, decomposition);
            String decompSortKey = collator.getSortKey(nfkd.normalize(ch), UCA.NON_IGNORABLE, decomposition);
            if (false && strength == 2) {
                sortKey = remove(sortKey, '\u0020');
                decompSortKey = remove(decompSortKey, '\u0020');
            }
            if (!sortKey.equals(decompSortKey)) {
                log.println("<tr><td>" + Utility.hex(ch)
                    + "</td><td>" + UCA.toString(sortKey)
                    + "</td><td>" + UCA.toString(decompSortKey)
                    + "</td><td>" + ucd.getName(ch)
                    + "</td></tr>"
                    );
            }
        }
        log.println("</table>");
        collator.setStrength(oldStrength);
    }

    static final String remove (String s, char ch) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ch) continue;
            buf.append(c);
        }
        return buf.toString();
    }


        log = new PrintWriter(new FileOutputStream("Frequencies.html"));
        log.println("<html><body>");
        MessageFormat mf = new MessageFormat("<tr><td><tt>{0}</tt></td><td><tt>{1}</tt></td><td align='right'><tt>{2}</tt></td><td align='right'><tt>{3}</tt></td></tr>");
        MessageFormat mf2 = new MessageFormat("<tr><td><tt>{0}</tt></td><td align='right'><tt>{1}</tt></td></tr>");
        String header = mf.format(new String[] {"Start", "End", "Count", "Subtotal"});
        int count;

        log.println("<h2>Writing Used Weights</h2>");
        log.println("<p>Primaries</p><table border='1'>" + mf.format(new String[] {"Start", "End", "Count", "Subtotal"}));
        count = collator.writeUsedWeights(log, 1, mf);
        log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>", new Object[] {new Integer(count)}));
        log.println("</table>");

        log.println("<p>Secondaries</p><table border='1'>" + mf2.format(new String[] {"Code", "Frequency"}));
        count = collator.writeUsedWeights(log, 2, mf2);
        log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>", new Object[] {new Integer(count)}));
        log.println("</table>");

        log.println("<p>Tertiaries</p><table border='1'>" + mf2.format(new String[] {"Code", "Frequency"}));
        count = collator.writeUsedWeights(log, 3, mf2);
        log.println(MessageFormat.format("<tr><td>Count:</td><td>{0}</td></tr>", new Object[] {new Integer(count)}));
        log.println("</table>");
        log.println("</body></html>");
        log.close();


    static int[] compactSecondary;

    static void checkEquivalents() {
        Normalizer nfkd = new Normalizer(Normalizer.NFKC);
        Normalizer nfd = new Normalizer(Normalizer.NFKD);
        for (char c = 0; c < 0xFFFF; ++c) {

    }

    static void testCompatibilityCharacters() throws IOException {
        log = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(GEN_DIR + "UCA_CompatComparison.txt"), "UTF8"), 32*1024));

        int[] kenCes = new int[50];
        int[] markCes = new int[50];
        int[] kenComp = new int[50];
        Map forLater = new TreeMap();
        int count = 0;
        int typeLimit = UCD_Types.CANONICAL;
        boolean decompType = false;
        if (false) {
            typeLimit = UCD_Types.COMPATIBILITY;
            decompType = true;
        }

        // first find all the characters that cannot be generated "correctly"

        for (int i = 0; i < 0xFFFF; ++i) {
            int type = ucd.getDecompositionType(i);
            if (type < typeLimit) continue;
            int ceType = collator.getCEType((char)i);
            if (ceType >= collator.FIXED_CE) continue;
            // fix type
            type = getDecompType(i);

            String s = String.valueOf((char)i);
            int kenLen = collator.getCEs(s, decompType, kenCes); // true
            int markLen = fixCompatibilityCE(s, true, markCes, false);

            if (!arraysMatch(kenCes, kenLen, markCes, markLen)) {
                int kenCLen = fixCompatibilityCE(s, true, kenComp, true);
                String comp = CEList.toString(kenComp, kenCLen);

                if (arraysMatch(kenCes, kenLen, kenComp, kenCLen)) {
                    forLater.put((char)(COMPRESSED | type) + s, comp);
                    continue;
                }
                if (type == ucd.CANONICAL && multipleZeroPrimaries(markCes, markLen)) {
                    forLater.put((char)(MULTIPLES | type) + s, comp);
                    continue;
                }
                forLater.put((char)type + s, comp);
            }
        }

        Iterator it = forLater.keySet().iterator();
        byte oldType = (byte)0xFF; // anything unique
        int caseCount = 0;
        log.println("Generated: " + getNormalDate());
        while (it.hasNext()) {
            String key = (String) it.next();
            byte type = (byte)key.charAt(0);
            if (type != oldType) {
                oldType = type;
                log.println("===============================================================");
                log.print("CASE " + (caseCount++) + ": ");
                byte rType = (byte)(type & OTHER_MASK);
                log.println("    Decomposition Type = " + ucd.getDecompositionTypeID_fromIndex(rType));
                if ((type & COMPRESSED) != 0) {
                    log.println("    Successfully Compressed a la Ken");
                    log.println("    [XXXX.0020.YYYY][0000.ZZZZ.0002] => [XXXX.ZZZZ.YYYY]");
                } else if ((type & MULTIPLES) != 0) {
                    log.println("    PLURAL ACCENTS");
                }
                log.println("===============================================================");
                log.println();
            }
            String s = key.substring(1);
            String comp = (String)forLater.get(key);

            int kenLen = collator.getCEs(s, decompType, kenCes);
            String kenStr = CEList.toString(kenCes, kenLen);

            int markLen = fixCompatibilityCE(s, true, markCes, false);
            String markStr = CEList.toString(markCes, markLen);

            if ((type & COMPRESSED) != 0) {
                log.println("COMPRESSED #" + (++count) + ": " + ucd.getCodeAndName(s));
                log.println("         : " + comp);
            } else {
                log.println("DIFFERENCE #" + (++count) + ": " + ucd.getCodeAndName(s));
                log.println("generated : " + markStr);
                if (!markStr.equals(comp)) {
                    log.println("compressed: " + comp);
                }
                log.println("Ken's     : " + kenStr);
                String nfkdstr = nfkd.normalize(s);
                log.println("NFKD      : " + ucd.getCodeAndName(nfkdstr));
                String nfdstr = nfd.normalize(s);
                if (!nfdstr.equals(nfkdstr)) {
                    log.println("NFD       : " + ucd.getCodeAndName(nfdstr));
                }
                //kenCLen = collator.getCEs(decomp, true, kenComp);
                //log.println("decomp ce: " + CEList.toString(kenComp, kenCLen));
            }
            log.println();
        }
        log.println("===============================================================");
        log.println();
        log.println("Compressible Secondaries");
        for (int i = 0; i < compressSet.size(); ++i) {
            if ((i & 0xF) == 0) log.println();
            if (!compressSet.get(i)) log.print("-  ");
            else log.print(Utility.hex(i, 3) + ", ");
        }
        log.close();
    }

    static final byte getDecompType(int cp) {
        byte result = ucd.getDecompositionType(cp);
        if (result == ucd.CANONICAL) {
            String d = nfd.normalize((char)cp); // TODO
            for (int i = 0; i < d.length(); ++i) {
                byte t = ucd.getDecompositionType(d.charAt(i));
                if (t > ucd.CANONICAL) return t;
            }
        }
        return result;
    }

    static final boolean multipleZeroPrimaries(int[] a, int aLen) {
        int count = 0;
        for (int i = 0; i < aLen; ++i) {
            if (UCA.getPrimary(a[i]) == 0) {
                if (count == 1) return true;
                count++;
            } else {
                count = 0;
            }
        }
        return false;
    }

    static final byte MULTIPLES = 0x20, COMPRESSED = 0x40, OTHER_MASK = 0x1F;
    static final BitSet compressSet = new BitSet();

    static int kenCompress(int[] markCes, int markLen) {
        if (markLen == 0) return 0;
        int out = 1;
        for (int i = 1; i < markLen; ++i) {
            int next = markCes[i];
            int prev = markCes[out-1];
            if (UCA.getPrimary(next) == 0
              && UCA.getSecondary(prev) == 0x20
              && UCA.getTertiary(next) == 0x2) {
                markCes[out-1] = UCA.makeKey(
                  UCA.getPrimary(prev),
                  UCA.getSecondary(next),
                  UCA.getTertiary(prev));
                compressSet.set(UCA.getSecondary(next));
            } else {
                markCes[out++] = next;
            }
        }
        return out;
    }


    static boolean arraysMatch(int[] a, int aLen, int[] b, int bLen) {
        if (aLen != bLen) return false;
        for (int i = 0; i < aLen; ++i) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    static int[] markCes = new int[50];

    static int fixCompatibilityCE(String s, boolean decompose, int[] output, boolean compress) {
        int type = getDecompType(s.charAt(0));
        char ch = s.charAt(0);

        String decomp = nfkd.normalize(s);
        int len = 0;
        int markLen = collator.getCEs(decomp, true, markCes);
        if (compress) markLen = kenCompress(markCes, markLen);

        //for (int j = 0; j < decomp.length(); ++j) {
            for (int k = 0; k < markLen; ++k) {
                int t = UCA.getTertiary(markCes[k]);
                if (type != CANONICAL) {
                    if (0x3041 <= ch && ch <= 0x3094) t = 0xE; // hiragana
                    else if (0x30A1 <= ch && ch <= 0x30FA) t = 0x11; // katakana
                }
                switch (type) {
                    case COMPATIBILITY: t = (t == 8) ? 0xA : 4; break;
                    case COMPAT_FONT:  t = (t == 8) ? 0xB : 5; break;
                    case COMPAT_NOBREAK: t = 0x1B; break;
                    case COMPAT_INITIAL: t = 0x17; break;
                    case COMPAT_MEDIAL: t = 0x18; break;
                    case COMPAT_FINAL: t = 0x19; break;
                    case COMPAT_ISOLATED: t = 0x1A; break;
                    case COMPAT_CIRCLE: t = (t == 0x11) ? 0x13 : (t == 8) ? 0xC : 6; break;
                    case COMPAT_SUPER: t = 0x14; break;
                    case COMPAT_SUB: t = 0x15; break;
                    case COMPAT_VERTICAL: t = 0x16; break;
                    case COMPAT_WIDE: t= (t == 8) ? 9 : 3; break;
                    case COMPAT_NARROW: t = (0xFF67 <= ch && ch <= 0xFF6F) ? 0x10 : 0x12; break;
                    case COMPAT_SMALL: t = (t == 0xE) ? 0xE : 0xF; break;
                    case COMPAT_SQUARE: t = (t == 8) ? 0x1D : 0x1C; break;
                    case COMPAT_FRACTION: t = 0x1E; break;
                }
                output[len++] = UCA.makeKey(
                    UCA.getPrimary(markCes[k]),
                    UCA.getSecondary(markCes[k]),
                    t);
            //}
        }
        return len;
    }

    static final byte WITHOUT_NAMES = 0, WITH_NAMES = 1;

    static void writeRules (byte option) throws IOException {
        int[] ces = new int[50];

        {
        int len2 = collator.getCEs("\u2474", true, ces);
        System.out.println(CEList.toString(ces, len2));

        String a = collator.getSortKey("a");
        String b = collator.getSortKey("A");
        System.out.println(collator.strengthDifference(a, b));
        }

        System.out.println("Sorting");
        Map backMap = new HashMap();
        Map ordered = new TreeMap();

        for (char ch = 0; ch < 0xFFFF; ++ch) {
            byte type = collator.getCEType(ch);
            if (type >= UCA.FIXED_CE) continue;
            String s = String.valueOf(ch);
            int len = collator.getCEs(s, true, ces);
            if (len == 1) backMap.put(new Integer(ces[0]), s);
            ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }
        Hashtable multiTable = collator.getContracting();
        Enumeration enum = multiTable.keys();
        while (enum.hasMoreElements()) {
            String s = (String)enum.nextElement();
            ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }
        System.out.println("Writing");

        String filename = "UCA_Rules.txt";
        if (option == WITH_NAMES) filename = "UCA_Rules_With_Names.txt";
        log = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(GEN_DIR + filename), "UTF8"), 32*1024));
        log.write('\uFEFF'); // BOM
        log.println("& [top]"); // RESET

        Iterator it = ordered.keySet().iterator();
        int oldFirstPrimary = UCA.getPrimary(UCA.TERMINATOR);
        boolean wasVariable = false;

        //String lastSortKey = collator.getSortKey("\u0000");;
        // 12161004
        int lastCE = 0;

        while (it.hasNext()) {
            String sortKey = (String) it.next();
            String chr = (String)ordered.get(sortKey);
            String relation = "";
            int len = collator.getCEs(chr, true, ces);
            int ce = ces[0];

            // special case for relations to fixed items

            String reset = "";
            char primary = collator.getPrimary(ce);
            if (primary >= 0x3400) {
                if (primary == 0x9FA6) {
                    primary = '\u9FA5';
                }
                if (primary < 0x9FA6) {
                    lastCE = collator.makeKey(primary, collator.NEUTRAL_SECONDARY, collator.NEUTRAL_TERTIARY);
                    reset = "& " + primary + " ";
                }
            }

            // get relation'

            if (ce == lastCE) relation = "        =";
            else if (collator.getPrimary(ce) != collator.getPrimary(lastCE)) relation = "<";
            else if (collator.getSecondary(ce) != collator.getSecondary(lastCE)) relation = "  <<";
            else if (collator.getTertiary(ce) != collator.getTertiary(lastCE)) relation = "    <<<";
            lastCE = ce;
            if (chr.equals("\u2474")) {
                System.out.println(CEList.toString(ces, len));
            }

            // check expansions

            String expansion = "";
            if (len > 1) {
                int tert0 = ces[0] & 0xFF;
                boolean isCompat = tert0 != 2 && tert0 != 8;
                for (int i = 1; i < len; ++i) {
                    int probe = ces[i];
                    String s = getFromBackMap(backMap, probe);
                    if (s == null) {
                        System.out.println("No back map for " + CEList.toString(ces[i])
                            + ": " + ucd.getCodeAndName(chr));
                        expansion += "[" + Utility.hex(ces[i]) + "]";
                    } else {
                        expansion += s;
                    }
                }
            }

            // print results

            log.print(reset + relation + " " + quoteOperand(chr));
            if (len > 1) log.print(" / " + expansion);
            if (option == WITH_NAMES) {
                log.print(" # " + ucd.getCodeAndName(chr));
                if (len > 1) log.print(" / " + Utility.hex(expansion));
            }
            log.println();
        }
        log.println("& [top]"); // RESET
        log.close();
    }

    static final String getFromBackMap(Map backMap, int probe) {
        String s = (String)backMap.get(new Integer(probe));
        if (s != null) return s;

        char primary = collator.getPrimary(probe);
        char secondary = collator.getSecondary(probe);
        char tertiary = collator.getTertiary(probe);

        if (primary > 0x3400) {
            return String.valueOf(primary);
        } else {
            int tert = tertiary;
            switch (tert) {
            case 8: case 9: case 0xA: case 0xB: case 0xC: case 0x1D:
                tert = 8;
                break;
            case 0xD: case 0x10: case 0x11: case 0x12: case 0x13: case 0x1C:
                tert = 0xE;
                break;
            default:
                tert = 2;
                break;
            }
            probe = collator.makeKey(primary, secondary, tert);
            s = (String)backMap.get(new Integer(probe));
            if (s != null) return s;

            probe = collator.makeKey(primary, secondary, collator.NEUTRAL_TERTIARY);
            s = (String)backMap.get(new Integer(probe));
        }
        if (s != null) return s;

        if (primary != 0 && secondary != collator.NEUTRAL_SECONDARY) {
            String first = getFromBackMap(backMap,
                collator.makeKey(primary, collator.NEUTRAL_SECONDARY, tertiary));
            String second = getFromBackMap(backMap,
                collator.makeKey(0, secondary, collator.NEUTRAL_TERTIARY));
            if (first != null && second != null) {
                s = first + second;
            }
        }
        return s;
    }

    static final String[] RELATION = {
        "<", " << ", "  <<<  ", "    =    ", "    =    ", "    =    ", "  >>>  ", " >> ", ">"
    };

    static final String quoteOperand(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
              case '<':  case '>':  case '#': case '=': case '&': case '/':
                result.append('\'').append(c).append('\'');
                break;
              case '\'':
                result.append("''");
                break;
              default:
                if (0 <= c && c < 0x20 || 0x7F <= c && c < 0xA0) {
                    result.append("\\u").append(Utility.hex(c));
                    break;
                }
                result.append(c);
                break;
            }
        }
        return result.toString();
    }

    //static Normalizer NFKD = new Normalizer(Normalizer.NFKD);
    //static Normalizer NFD = new Normalizer(Normalizer.NFD);

    static int variableHigh = 0;
    static final int COMMON = 5;

    static int gapForA = 0;

    static void writeFractionalUCA(String filename) throws IOException {

        variableHigh = collator.getVariableHigh() >> 16;
        BitSet secondarySet = collator.getWeightUsage(2);

        // HACK for CJK
        secondarySet.set(0x0040);

        int subtotal = 0;
        System.out.println("Fixing Secondaries");
        compactSecondary = new int[secondarySet.size()];
        for (int secondary = 0; secondary < compactSecondary.length; ++secondary) {
            if (secondarySet.get(secondary)) {
                compactSecondary[secondary] = subtotal++;
                System.out.println("compact[" + Utility.hex(secondary)
                    + "]=" + Utility.hex(compactSecondary[secondary])
                    + ", " + Utility.hex(fixSecondary(secondary)));
            }
        }
        System.out.println();

        //TO DO: find secondaries that don't overlap, and reassign

        System.out.println("Finding Bumps");
        char[] representatives = new char[65536];
        findBumps(representatives);

        System.out.println("Fixing Primaries");
        BitSet primarySet = collator.getWeightUsage(1);
        int[] primaryDelta = new int[65536];
        // start at 1 so zero stays zero.
        for (int primary = 1; primary < 0xFFFF; ++primary) {
            if (primarySet.get(primary)) primaryDelta[primary] = 2;
            else if (primary == 0x1299) {
                System.out.println("WHOOPS! Missing weight");
            }
        }

        int bumpNextToo = 0;

        subtotal = (COMMON << 8) + COMMON; // skip forbidden bytes, leave gap

        // start at 1 so zero stays zero.
        for (int primary = 1; primary < 0xFFFF; ++primary) {
            if (primaryDelta[primary] != 0) {

                subtotal += primaryDelta[primary];  // we will convert from relative to absolute
                if (singles.get(primary)) {
                    subtotal = (subtotal & 0xFF00) + 0x100;
                    if (primary == gapForA) subtotal += 0x200;
                    if (bumpNextToo == 0x40) subtotal += 0x100; // make sure of gap between singles!!!
                    bumpNextToo = 0x40;
                } else if (primary > variableHigh) {
                    variableHigh = 0xFFFF; // never do again!
                    subtotal = (subtotal & 0xFF00) + 0x320 + bumpNextToo;
                    bumpNextToo = 0;
                } else if (bumpNextToo > 0 || bumps.get(primary)) {
                    subtotal = ((subtotal + 0x20) & 0xFF00) + 0x120 + bumpNextToo;
                    bumpNextToo = 0;
                } else {
                    int lastByte = subtotal & 0xFF;
                    // skip all values of FF, 00, 01, 02,
                    if (0 <= lastByte && lastByte < COMMON || lastByte == 0xFF) {
                        subtotal = ((subtotal + 1) & 0xFFFFFF00) + COMMON; // skip
                    }
                }
                primaryDelta[primary] = subtotal;
            }
            // fixup for Kanji
            if (primary >= 0x3400) {
                int CE = getImplicitPrimary(primary);

                primaryDelta[primary] = CE >>> 8;
            }
            if ((primary & 0xFF) == 0) System.out.println(Utility.hex(primary) + " => " + hexBytes(primaryDelta[primary]));
        }


        // now translate!!

        System.out.println("Sorting");
        Map ordered = new TreeMap();

        for (char ch = 0; ch < 0xFFFF; ++ch) {
            byte type = collator.getCEType(ch);
            if (type >= UCA.FIXED_CE) continue;
            String s = String.valueOf(ch);
            ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }
        Hashtable multiTable = collator.getContracting();
        Enumeration enum = multiTable.keys();
        while (enum.hasMoreElements()) {
            String s = (String)enum.nextElement();
            ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
        }
        // JUST FOR TESTING
        if (false) {
            String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
            for (int i = 0; i < sample.length(); ++i) {
                String s = sample.substring(i, i+1);
                ordered.put(collator.getSortKey(s, UCA.NON_IGNORABLE) + '\u0000' + s, s);
            }
        }

        System.out.println("Writing");
        log = new PrintWriter(new BufferedWriter(new FileWriter(GEN_DIR + filename), 32*1024));
        PrintWriter summary = new PrintWriter(new BufferedWriter(new FileWriter(GEN_DIR + "log-" + filename), 32*1024));
        //log.println("[Variable Low = " + UCA.toString(collator.getVariableLow()) + "]");
        //log.println("[Variable High = " + UCA.toString(collator.getVariableHigh()) + "]");

        int[] ces = new int[100];

        StringBuffer newPrimary = new StringBuffer();
        StringBuffer newSecondary = new StringBuffer();
        StringBuffer newTertiary = new StringBuffer();
        StringBuffer oldStr = new StringBuffer();

        EquivalenceClass secEq = new EquivalenceClass("\n#", 4, true);
        EquivalenceClass terEq = new EquivalenceClass("\n#", 4, true);
        String[] sampleEq = new String[500];

        Iterator it = ordered.keySet().iterator();
        int oldFirstPrimary = UCA.getPrimary(UCA.TERMINATOR);
        boolean wasVariable = false;

        log.println("# Fractional UCA Table, Generated from UCA");
        log.println("# M. Davis, " + new Date());
        log.println("# Generated processed version, as described in design document.");
        log.println("# Notes");
        log.println("#  - Bugs in UCA data are NOT FIXED, except for the following problems:");
        log.println("#    - canonical equivalents are decomposed directly (some beta UCA are wrong).");
        log.println("#    - overlapping variable ranges are fixed.");
        log.println("#  - Format is as follows:");
        log.println("#      <codepoint> (' ' <codepoint>)* ';' ('L' | 'S') ';' <fractionalCE>+ ' # ' <UCA_CE> '# ' <name> ");
        log.println("#    - zero weights are not printed");
        log.println("#    - S: contains at least one lowercase or SMALL kana");
        log.println("#    - L: otherwise");
        log.println("#    - Different primaries are separated by a blank line.");
        log.println();

        String lastChr = "";
        int lastNp = 0;
        boolean doVariable = false;

        while (it.hasNext()) {
            Object sortKey = it.next();
            String chr = (String)ordered.get(sortKey);

            // get CEs and fix
            int len = collator.getCEs(chr, true, ces);
            int firstPrimary = UCA.getPrimary(ces[0]);
            if (firstPrimary != oldFirstPrimary) {
                log.println();
                oldFirstPrimary = firstPrimary;
                boolean isVariable = collator.isVariable(ces[0]);
                if (isVariable != wasVariable) {
                    if (isVariable) {
                        log.println("# START OF VARIABLE SECTION!!!");
                        summary.println("# START OF VARIABLE SECTION!!!");
                    } else {
                        log.println("[variable top = " + Utility.hex(primaryDelta[firstPrimary]) + "] # END OF VARIABLE SECTION!!!");
                        doVariable = true;
                    }
                    log.println();
                }
                wasVariable = isVariable;
            }
            oldStr.setLength(0);
            log.print(Utility.hex(chr, " ") + "; " + (needsCaseBit(chr) ? '1' : '0') + "; ");
            boolean nonePrinted = true;
            for (int q = 0; q < len; ++q) {
                nonePrinted = false;
                newPrimary.setLength(0);
                newSecondary.setLength(0);
                newTertiary.setLength(0);

                int pri = UCA.getPrimary(ces[q]);
                int sec = UCA.getSecondary(ces[q]);
                int ter = UCA.getTertiary(ces[q]);

                if (sec != 0x20) {
                    boolean changed = secEq.add(new Integer(sec), new Integer(pri));
                }
                if (ter != 0x2) {
                    boolean changed = terEq.add(new Integer(ter), new Integer((pri << 16) | sec));
                }
                if (sampleEq[sec] == null) sampleEq[sec] = chr;
                if (sampleEq[ter] == null) sampleEq[ter] = chr;
                oldStr.append(CEList.toString(ces[q]));// + "," + Integer.toString(ces[q],16);
                int np = primaryDelta[UCA.getPrimary(ces[q])];
                hexBytes(np, newPrimary);
                hexBytes(fixSecondary(UCA.getSecondary(ces[q])), newSecondary);
                hexBytes(fixTertiary(UCA.getTertiary(ces[q])), newTertiary);
                if (q == 0) {
                    if (!sameTopByte(np, lastNp)) {
                        summary.println("Last:  " + Utility.hex(lastNp) + " " + ucd.getName(lastChr.charAt(0)));
                        summary.println();
                        if (doVariable) {
                            doVariable = false;
                            summary.println("[variable top = " + Utility.hex(primaryDelta[firstPrimary]) + "] # END OF VARIABLE SECTION!!!");
                            summary.println();
                        }
                        summary.println("First: " + Utility.hex(np) + " " + ucd.getName(chr.charAt(0)));
                    }
                    lastNp = np;
                }
                log.print("[" + newPrimary
                    + ", " + newSecondary
                    + ", " + newTertiary
                    + "]");
            }
            if (nonePrinted) {
                log.print("[,,]");
                oldStr.append(CEList.toString(0));
            }
            log.println("    # " + oldStr + " # " + ucd.getName(chr.charAt(0)));
            lastChr = chr;
        }
        summary.println("Last:  " + Utility.hex(lastNp) + " " + ucd.getName(lastChr.charAt(0)));


        String sample = "\u3400\u3401\u4DB4\u4DB5\u4E00\u4E01\u9FA4\u9FA5\uAC00\uAC01\uD7A2\uD7A3";
        for (int i = 0; i < sample.length(); ++i) {
            char ch = sample.charAt(i);
            log.println(Utility.hex(ch) + " => " + Utility.hex(fixHan(ch))
                    + "          " + ucd.getName(ch));
        }

        summary.println();
        summary.println("# First Implicit: " + Utility.hex(0xFFFFFFFFL & getImplicitPrimary(0)));
        summary.println("# Last Implicit: " + Utility.hex(0xFFFFFFFFL & getImplicitPrimary(0x10FFFF)));
        summary.println("Compact Secondary 153: " + compactSecondary[0x153]);
        summary.println("Compact Secondary 157: " + compactSecondary[0x157]);


        summary.println();
        summary.println("# Disjoint classes for Secondaries");
        summary.println("#" + secEq.toString());

        summary.println();
        summary.println("# Disjoint classes for Tertiaries");
        summary.println("#" + terEq.toString());

        summary.println();
        summary.println("# Example characters for each TERTIARY value");
        summary.println();
        summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
        summary.println();
        for (int i = 0; i < sampleEq.length; ++i) {
            if (sampleEq[i] == null) continue;
            if (i == 0x20) {
                summary.println();
                summary.println("# Example characters for each SECONDARY value");
                summary.println();
                summary.println("# UCA : (FRAC) CODE [    UCA CE    ] Name");
                summary.println();
            }
            int len = collator.getCEs(sampleEq[i], true, ces);
            int newval = i < 0x20 ? fixTertiary(i) : fixSecondary(i);
            summary.print("# " + Utility.hex(i) + ": (" + Utility.hex(newval) + ") "
                + Utility.hex(sampleEq[i]) + " ");
            for (int q = 0; q < len; ++q) {
                summary.print(CEList.toString(ces[q]));
            }
            summary.println(" " + ucd.getName(sampleEq[i]));
        }
        log.close();
        summary.close();
    }

    // CONSTANTS

    static final int
        HAN_START = 0x3400,
        HAN_LIMIT = 0xA000,
        SUPPLEMENTARY_COUNT = 0x100000,
        BYTES_TO_AVOID = 3,
        OTHER_COUNT = 256 - BYTES_TO_AVOID,
        LAST_COUNT = OTHER_COUNT / 2,
        LAST_COUNT2 = (SUPPLEMENTARY_COUNT - 1) / (OTHER_COUNT * OTHER_COUNT) + 1, // last byte
        HAN_SHIFT = LAST_COUNT * OTHER_COUNT - HAN_START,
        BOUNDARY = 2 * OTHER_COUNT * LAST_COUNT + HAN_START,
        LAST2_MULTIPLIER = OTHER_COUNT / LAST_COUNT2;

    // GET IMPLICIT PRIMARY WEIGHTS
    // Return value is left justified primary key

    static int getImplicitPrimary(int cp) {
        // we must skip all 00, 01, 02 bytes, so most bytes have 253 values
        // we must leave a gap of 01 between all values of the last byte, so the last byte has 126 values (3 byte case)
        // we shift so that HAN all has the same first primary, for compression.
        // for the 4 byte case, we make the gap as large as we can fit.
        // Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
        // Four byte forms (most supplementaries) are EF xx xx xx (with a gap of LAST2_MULTIPLIER == 14)

        int last0 = cp - BOUNDARY;
        if (last0 < 0) {
            cp += HAN_SHIFT; // shift so HAN shares single block
            int last1 = cp / LAST_COUNT;
            last0 = cp % LAST_COUNT;
            int last2 = last1 / OTHER_COUNT;
            last1 %= OTHER_COUNT;
            return 0xEC030300 + (last2 << 24) + (last1 << 16) + (last0 << 9);
        } else {
            int last1 = last0 / LAST_COUNT2;
            last0 %= LAST_COUNT2;
            int last2 = last1 / OTHER_COUNT;
            last1 %= OTHER_COUNT;
            return 0xEF030303 + (last2 << 16) + (last1 << 8) + (last0 * LAST2_MULTIPLIER);
        }
    }

    // TEST PROGRAM

    static void checkImplicit() {
        long oldPrimary = 0;
        System.out.println("Starting Implicit Check");
        for (int i = 0; i <= 0x10FFFF; ++i) {
            long newPrimary = 0xFFFFFFFFL & getImplicitPrimary(i);

            // test correct values

            if (newPrimary < oldPrimary) {
                throw new IllegalArgumentException(Utility.hex(i) + ": overlap: " + Utility.hex(oldPrimary) + " > " + Utility.hex(newPrimary));
            }

            long b0 = (newPrimary >> 24) & 0xFF;
            long b1 = (newPrimary >> 16) & 0xFF;
            long b2 = (newPrimary >> 8) & 0xFF;
            long b3 = newPrimary & 0xFF;

            if (b0 < 0xEC || b0 > 0xEF || b1 < 3 || b2 < 3 || b3 == 1 || b3 == 2) {
                throw new IllegalArgumentException(Utility.hex(i) + ": illegal byte value: " + Utility.hex(newPrimary)
                    + ", " + Utility.hex(b1) + ", " + Utility.hex(b2) + ", " + Utility.hex(b3));
            }

            // print range to look at

            if (false) {
                int b = i & 0xFF;
                if (b == 255 || b == 0 || b == 1) {
                    System.out.println(Utility.hex(i) + " => " + Utility.hex(newPrimary));
                }
            }
            oldPrimary = newPrimary;
        }
        System.out.println("Successful Implicit Check!!");
    }

    static boolean sameTopByte(int x, int y) {
        int x1 = x & 0xFF0000;
        int y1 = y & 0xFF0000;
        if (x1 != 0 || y1 != 0) return x1 == y1;
        x1 = x & 0xFF00;
        y1 = y & 0xFF00;
        return x1 == y1;
    }

        // return true if either:
        // a. toLower(NFKD(x)) != x (using FULL case mappings), OR
        // b. toSmallKana(NFKD(x)) != x.

    static final boolean needsCaseBit(String x) {
        String s = nfkd.normalize(x);
        if (!ucd.getCase(s, FULL, LOWER).equals(s)) return true;
        if (!toSmallKana(s).equals(s)) return true;
        return false;
    }

    static final StringBuffer toSmallKanaBuffer = new StringBuffer();

    static final String toSmallKana(String s) {
        // note: don't need to do surrogates; none exist
        boolean gotOne = false;
        toSmallKanaBuffer.setLength(0);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if ('\u3042' <= c && c <= '\u30EF') {
                switch(c - 0x3000) {
                  case 0x42: case 0x44: case 0x46: case 0x48: case 0x4A: case 0x64: case 0x84: case 0x86: case 0x8F:
                  case 0xA2: case 0xA4: case 0xA6: case 0xA8: case 0xAA: case 0xC4: case 0xE4: case 0xE6: case 0xEF:
                    --c; // maps to previous char
                    gotOne = true;
                    break;
                  case 0xAB:
                    c = '\u30F5';
                    gotOne = true;
                    break;
                  case 0xB1:
                    c = '\u30F6';
                    gotOne = true;
                    break;
                }
            }
            toSmallKanaBuffer.append(c);
        }
        if (gotOne) return toSmallKanaBuffer.toString();
        return s;
    }


30F5;KATAKANA LETTER SMALL KA;Lo;0;L;;;;;N;;;;;
30AB;KATAKANA LETTER KA;Lo;0;L;;;;;N;;;;;
30F6;KATAKANA LETTER SMALL KE;Lo;0;L;;;;;N;;;;;
30B1;KATAKANA LETTER KE;Lo;0;L;;;;;N;;;;;

30A1;KATAKANA LETTER SMALL A;Lo;0;L;;;;;N;;;;;
30A2;KATAKANA LETTER A;Lo;0;L;;;;;N;;;;;
30A3;KATAKANA LETTER SMALL I;Lo;0;L;;;;;N;;;;;
30A4;KATAKANA LETTER I;Lo;0;L;;;;;N;;;;;
30A5;KATAKANA LETTER SMALL U;Lo;0;L;;;;;N;;;;;
30A6;KATAKANA LETTER U;Lo;0;L;;;;;N;;;;;
30A7;KATAKANA LETTER SMALL E;Lo;0;L;;;;;N;;;;;
30A8;KATAKANA LETTER E;Lo;0;L;;;;;N;;;;;
30A9;KATAKANA LETTER SMALL O;Lo;0;L;;;;;N;;;;;
30AA;KATAKANA LETTER O;Lo;0;L;;;;;N;;;;;
30C3;KATAKANA LETTER SMALL TU;Lo;0;L;;;;;N;;;;;
30C4;KATAKANA LETTER TU;Lo;0;L;;;;;N;;;;;
30E3;KATAKANA LETTER SMALL YA;Lo;0;L;;;;;N;;;;;
30E4;KATAKANA LETTER YA;Lo;0;L;;;;;N;;;;;
30E5;KATAKANA LETTER SMALL YU;Lo;0;L;;;;;N;;;;;
30E6;KATAKANA LETTER YU;Lo;0;L;;;;;N;;;;;
30E7;KATAKANA LETTER SMALL YO;Lo;0;L;;;;;N;;;;;
30E8;KATAKANA LETTER YO;Lo;0;L;;;;;N;;;;;
30EE;KATAKANA LETTER SMALL WA;Lo;0;L;;;;;N;;;;;
30EF;KATAKANA LETTER WA;Lo;0;L;;;;;N;;;;;

3041;HIRAGANA LETTER SMALL A;Lo;0;L;;;;;N;;;;;
3042;HIRAGANA LETTER A;Lo;0;L;;;;;N;;;;;
3043;HIRAGANA LETTER SMALL I;Lo;0;L;;;;;N;;;;;
3044;HIRAGANA LETTER I;Lo;0;L;;;;;N;;;;;
3045;HIRAGANA LETTER SMALL U;Lo;0;L;;;;;N;;;;;
3046;HIRAGANA LETTER U;Lo;0;L;;;;;N;;;;;

3047;HIRAGANA LETTER SMALL E;Lo;0;L;;;;;N;;;;;
3048;HIRAGANA LETTER E;Lo;0;L;;;;;N;;;;;
3049;HIRAGANA LETTER SMALL O;Lo;0;L;;;;;N;;;;;
304A;HIRAGANA LETTER O;Lo;0;L;;;;;N;;;;;
3063;HIRAGANA LETTER SMALL TU;Lo;0;L;;;;;N;;;;;
3064;HIRAGANA LETTER TU;Lo;0;L;;;;;N;;;;;
3083;HIRAGANA LETTER SMALL YA;Lo;0;L;;;;;N;;;;;
3084;HIRAGANA LETTER YA;Lo;0;L;;;;;N;;;;;
3085;HIRAGANA LETTER SMALL YU;Lo;0;L;;;;;N;;;;;
3086;HIRAGANA LETTER YU;Lo;0;L;;;;;N;;;;;
3087;HIRAGANA LETTER SMALL YO;Lo;0;L;;;;;N;;;;;
3088;HIRAGANA LETTER YO;Lo;0;L;;;;;N;;;;;
308E;HIRAGANA LETTER SMALL WA;Lo;0;L;;;;;N;;;;;
308F;HIRAGANA LETTER WA;Lo;0;L;;;;;N;;;;;




    static final int secondaryDoubleStart = 0xD0;

    static int fixSecondary(int x) {
        x = compactSecondary[x];
        return fixSecondary2(x, compactSecondary[0x153], compactSecondary[0x157]);
    }

    static int fixSecondary2(int x, int gap1, int gap2) {
        int top = x;
        int bottom = 0;
        if (top == 0) {
            // ok, zero
        } else if (top == 1) {
            top = COMMON;
        } else {
            top *= 2; // create gap between elements. top is now 4 or more
            top += 0x80 + COMMON - 2; // insert gap to make top at least 87

            // lowest values are singletons. Others are 2 bytes
            if (top > secondaryDoubleStart) {
                top -= secondaryDoubleStart;
                top *= 4; // leave bigger gap just in case
                if (x > gap1) {
                    top += 256; // leave gap after COMBINING ENCLOSING KEYCAP (see below)
                }
                if (x > gap2) {
                    top += 64; // leave gap after RUNIC LETTER SHORT-TWIG-AR A (see below)
                }

                bottom = (top % LAST_COUNT) * 2 + COMMON;
                top = (top / LAST_COUNT) + secondaryDoubleStart;
            }
        }
        return (top << 8) | bottom;
    }


# 0153: (EE3D) 20E3 [0000.0153.0002] COMBINING ENCLOSING KEYCAP
# 0154: (EE41) 0153 [0997.0154.0004][08B1.0020.0004] LATIN SMALL LIGATURE OE
# 0155: (EE45) 017F [09F3.0155.0004] LATIN SMALL LETTER LONG S
# 0157: (EE49) 16C6 [1656.0157.0004] RUNIC LETTER SHORT-TWIG-AR A
# 0158: (EE4D) 2776 [0858.0158.0006] DINGBAT NEGATIVE CIRCLED DIGIT ONE


    static int fixTertiary(int x) {
        if (x == 0) return x;
        if (x == 1) throw new IllegalArgumentException("Tertiary illegal: " + x);
        // 2 => COMMON, 1 is unused
        int result = 2 * (x - 2) + COMMON;
        if (result >= 0x3E) throw new IllegalArgumentException("Tertiary too large: " + Utility.hex(x) + " => " + Utility.hex(result));
        return result;
    }

    static void checkFixes() {
        System.out.println("Checking Secondary/Tertiary Fixes");
        int lastVal = -1;
        for (int i = 0; i <= 0x16E; ++i) {
            if (i == 0x153) {
                System.out.println("debug");
            }
            int val = fixSecondary2(i, 999, 999); // HACK for UCA
            if (val <= lastVal) throw new IllegalArgumentException(
                "Unordered: " + Utility.hex(val) + " => " + Utility.hex(lastVal));
            int top = val >>> 8;
            int bottom = val & 0xFF;
            if (top != 0 && (top < COMMON || top > 0xEF)
                || (top > COMMON && top < 0x87)
                || (bottom != 0 && (isEven(bottom) || bottom < COMMON || bottom > 0xFD))
                || (bottom == 0 && top != 0 && isEven(top))) {
                throw new IllegalArgumentException("Secondary out of range: " + Utility.hex(i) + " => "
                    + Utility.hex(top) + ", " + Utility.hex(bottom));
            }
        }

        lastVal = -1;
        for (int i = 0; i <= 0x1E; ++i) {
            if (i == 1) continue; // never occurs
            int val = fixTertiary(i);
            if (val <= lastVal) throw new IllegalArgumentException(
                "Unordered: " + Utility.hex(val) + " => " + Utility.hex(lastVal));
            if (val != 0 && (isEven(val) || val < COMMON || val > 0x3D)) {
                throw new IllegalArgumentException("Tertiary out of range: " + Utility.hex(i) + " => "
                    + Utility.hex(val));
            }
        }
        System.out.println("END Checking Secondary/Tertiary Fixes");
    }

    static boolean isEven(int x) {
        return (x & 1) == 0;
    }

    static String ceToString(int primary, int secondary, int tertiary) {
        return "[" + hexBytes(primary) + ", "
            + hexBytes(secondary) + ", "
            + hexBytes(tertiary) + "]";
    }


    static String hexBytes(long x) {
        StringBuffer temp = new StringBuffer();
        hexBytes(x, temp);
        return temp.toString();
    }

    static void hexBytes(long x, StringBuffer result) {
        byte lastb = 1;
        for (int shift = 24; shift >= 0; shift -= 8) {
            byte b = (byte)(x >>> shift);
            if (b != 0) {
                if (result.length() != 0) result.append(" ");
                result.append(Utility.hex(b));
                //if (lastb == 0) System.err.println(" bad zero byte: " + result);
            }
            lastb = b;
        }
    }

    static int fixHan(char ch) { // BUMP HANGUL, HAN
        if (ch < 0x3400 || ch > 0xD7A3) return -1;

        char ch2 = ch;
        if (ch >= 0xAC00) ch2 -= (0xAC00 - 0x9FA5 - 1);
        if (ch >= 0x4E00) ch2 -= (0x4E00 - 0x4DB5 - 1);

        return 0x6000 + (ch2-0x3400); // room to interleave
    }

    static BitSet bumps = new BitSet();
    static BitSet singles = new BitSet();

    static void findBumps(char[] representatives) {
        int[] ces = new int[100];
        int[] scripts = new int[100];
        char[] scriptChar = new char[100];

        // find representatives

        for (char ch = 0; ch < 0xFFFF; ++ch) {
            byte type = collator.getCEType(ch);
            if (type < UCA.FIXED_CE) {
                int len = collator.getCEs(String.valueOf(ch), true, ces);
                int primary = UCA.getPrimary(ces[0]);
                if (primary < variableHigh) continue;
                if (ch == 0x1160 || ch == 0x11A8) { // set bumps within Hangul L, V, T
                    bumps.set(primary);
                    continue;
                }
                byte script = ucd.getScript(ch);
                // HACK
                if (ch == 0x0F7E || ch == 0x0F7F) script = TIBETAN_SCRIPT;
                //if (script == ucd.GREEK_SCRIPT) System.out.println(ucd.getName(ch));
                // get least primary for script
                if (scripts[script] == 0 || scripts[script] > primary) {
                    byte cat = ucd.getCategory(ch);
                    // HACK
                    if (ch == 0x0F7E || ch == 0x0F7F) cat = ucd.OTHER_LETTER;
                    if (cat <= ucd.OTHER_LETTER && cat != ucd.Lm) {
                        scripts[script] = primary;
                        scriptChar[script] = ch;
                        if (script == ucd.GREEK_SCRIPT) System.out.println("*" + Utility.hex(primary) + ucd.getName(ch));
                    }
                }
                // get representative char for primary
                if (representatives[primary] == 0 || representatives[primary] > ch) {
                    representatives[primary] = ch;
                }
            }
        }

        // set bumps
        for (int i = 0; i < scripts.length; ++i) {
            if (scripts[i] > 0) {
                bumps.set(scripts[i]);
                System.out.println(Utility.hex(scripts[i]) + " " + UCD.getScriptID_fromIndex((byte)i)
                 + " " + Utility.hex(scriptChar[i]) + " " + ucd.getName(scriptChar[i]));
            }
        }

        char[][] singlePairs = {{'a','z'}, {' ', ' '}}; // , {'\u3041', '\u30F3'}
        for (int j = 0; j < singlePairs.length; ++j) {
            for (char k = singlePairs[j][0]; k <= singlePairs[j][1]; ++k) {
                setSingle(k, ces);
            }
        }
        setSingle('\u0300', ces);
        setSingle('\u0301', ces);
        setSingle('\u0302', ces);
        setSingle('\u0303', ces);
        setSingle('\u0308', ces);
        setSingle('\u030C', ces);


        bumps.set(0x089A); // lowest non-variable
        bumps.set(0x4E00); // lowest Kangxi

    }

    static void setSingle(char ch, int[] ces) {
        collator.getCEs(String.valueOf(ch), true, ces);
        singles.set(UCA.getPrimary(ces[0]));
        if (ch == 'a') gapForA = UCA.getPrimary(ces[0]);
    }


    static void copyFile(PrintWriter log, String fileName) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        while (true) {
           String line = input.readLine();
           if (line == null) break;
           log.println(line);
        }
        input.close();
    }

    static void writeCollationValidityLog() throws IOException {
        log = new PrintWriter(new FileOutputStream("CheckCollationValidity.html"));
        log.println("<html><body>");


        //collator = new UCA(null);
        if (false){
            String key = collator.getSortKey("\u0308\u0301", UCA.SHIFTED, false);
            String look = printableKey(key);
            System.out.println(look);

        }
        System.out.println("Sorting");

        for (int i = 0; i <= 0xFFFF; ++i) {
            char c = (char)i;
            if (EXCLUDE_UNSUPPORTED && !collator.found.contains(c)) continue;
            if (0xD800 <= i && i <= 0xF8FF) continue; // skip surrogates and private use
            //if (0xA000 <= c && c <= 0xA48F) continue; // skip YI
            addString(String.valueOf(c), option);
        }

        Hashtable multiTable = collator.getContracting();
        Enumeration enum = multiTable.keys();
        while (enum.hasMoreElements()) {
            addString((String)enum.nextElement(), option);
        }

        for (int i = 0; i < extras.length; ++i) { // put in sample non-characters
            addString(extras[i], option);
        }

        for (int i = 0; i < extraRanges.length; ++i) {
            char start = extraRanges[i][0];
            char end = extraRanges[i][1];
            int increment = ((end - start + 1) / 303) + 1;
            //System.out.println("Range: " + start + ", " + end + ", " + increment);
            addString(start, option);
            for (char j = (char)(start+1); j < end-1; j += increment) {
                addString(j, option);
                addString(j+1, option);
            }
            addString(end-1, option);
            addString(end, option);
        }

        System.out.println("Total: " + sortedD.size());
        Iterator it;

        //ucd.init();

        if (false) {
            System.out.println("Listing Mismatches");
            it = duplicates.keySet().iterator();
            //String lastSortKey = "";
            //String lastSource = "";
            while (it.hasNext()) {
                String source = (String)it.next();
                String sortKey = (String)duplicates.get(source);
                char endMark = source.charAt(source.length()-1);
                source = source.substring(0,source.length()-1);
                if (endMark == MARK1) {
                    log.println("<br>");
                    log.println("Mismatch: " + Utility.hex(source, " ")
                        + ", " + ucd.getName(source) + "<br>");
                    log.print("  NFD:");
                } else {
                    log.print("  NFC:");
                }
                log.println(UCA.toString(sortKey) + "<br>");

                if (source.equals(lastSource)) {
                    it.remove();
                    --duplicateCount;
                }
                //lastSortKey = sortKey;
                lastSource = lastSource;

            }
            System.out.println("Total: " + sortedD.size());
        }

        System.out.println("Writing");
        String version = collator.getDataVersion();

        if (GENERATED_NFC_MISMATCHES) showMismatches();
        removeAdjacentDuplicates2();
        checkBadDecomps(1, false); // if decomposition is off, all primaries should be identical
        checkBadDecomps(2, true); // if decomposition is ON, all primaries and secondaries should be identical


        if (DO_CHARTS) for (int j = 0; j < 2; ++j) { // with and without key

            String name = "Collation";
            String other = "CollationKey";
            boolean SHOW_CE = false;

            if (j == 1) {
                SHOW_CE = true;
                name = "CollationKey";
                other = "Collation";
            }

            it = sortedD.keySet().iterator();

            int end = sortedD.size() >> 7;

            PrintWriter out = writeHead(0, end, name, other, version, SHOW_CE);

            String lastCol = "";
            String lastChar = "";
            boolean firstRow = true;
            int page = 0;
            for (int count = 0; it.hasNext(); count++) {
                page = count >> 7;
                if (count > 0 && (count & 0xf) == 0) {
                    if ((count & 0x7F) == 0) {
                        writeTail(out, page-1, name, other, SHOW_CE);
                        out = writeHead(page, end, name, other, version, SHOW_CE);
                        System.out.println("Block: " + page);
                        firstRow = true;
                    } else {
                        out.println("</tr><tr>");
                        firstRow = false;
                    }
                }
                String col2 = (String)it.next();
                String ch2 = (String)sortedD.get(col2);

                // remove mark
                col2 = col2.substring(0,col2.length()-1);

                int strength = getStrengthDifference(lastCol, col2);
                lastCol = col2;

                out.print("<td");
                int color = 0xFFFFFF;
                switch (strength) {
                    // case 4: color = 0xFFFFFF; break; // white
                    case 3: color = 0xCCCCFF; break;
                    case 2: color = 0x9999FF; break;
                    case 1: color = 0x6666FF; break;
                    case 0: color = 0x3333FF; break;
                }
                if (mark == MARK2) {
                    color = color & 0xFF00FF;
                }
                if (color != 0xFFFFFF) out.print(" bgcolor='#" + Integer.toString(color,16) + "'");
                //if (firstRow) out.print(" width='6%'");
                out.print(">");

                //log.println(Utility.hex(ch2.charAt(0)));
                boolean ignorable = col2.charAt(0) == 0;
                out.print(HTMLString(ch2) + "<br><tt>"
                    + (ignorable ? "<u>" : "")
                    + Utility.hex(ch2, " ")
                    + (ignorable ? "</u>" : "")
                    );
                if (SHOW_CE) out.print("</tt><br><tt><b>" + UCA.toString(col2) + "</b>");
                out.println("</tt></td>");

                // remember
                lastCol = col2;
                lastChar = ch2;
            }
            writeTail(out, page-1, name, other, SHOW_CE);
        }
        log.println("</body></html>");
        log.close();
        sortedD.clear();
        System.out.println("Done");
    }


3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;
9FA5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;
AC00;<Hangul Syllable, First>;Lo;0;L;;;;;N;;;;;
D7A3;<Hangul Syllable, Last>;Lo;0;L;;;;;N;;;;;
A000;YI SYLLABLE IT;Lo;0;L;;;;;N;;;;;
A001;YI SYLLABLE IX;Lo;0;L;;;;;N;;;;;
A4C4;YI RADICAL ZZIET;So;0;ON;;;;;N;;;;;
A4C6;YI RADICAL KE;So;0;ON;;;;;N;;;;;


    static final char[][] extraRanges = {{0x3400, 0x4DB5}, {0x4E00, 0x9FA5}, {0xAC00, 0xD7A3}, {0xA000, 0xA48C}};

    static final String[] extras = {
            "\uD800\uDC00", "\uDBFF\uDFFD",
            "\u0220", "\uFFF0",
            "\uD800", "\uDFFF",
            "\uFFFE", "\uFFFF", "\uDBFF\uDFFE", "\uDBFF\uDFFF",
        };

    static final int MARK = 1;
    static final char MARK1 = '\u0001';
    static final char MARK2 = '\u0002';
    //Normalizer normalizer = new Normalizer(Normalizer.NFC, true);

    //static Normalizer toC = new Normalizer(Normalizer.NFC);
    //static Normalizer toD = new Normalizer(Normalizer.NFD);
    static TreeMap MismatchedC = new TreeMap();
    static TreeMap MismatchedN = new TreeMap();
    static TreeMap MismatchedD = new TreeMap();

    static final byte option = UCA.NON_IGNORABLE; // SHIFTED

    static void addString(int ch, byte option) {
        addString(String.valueOf((char)ch), option);
    }

    static void addString(String ch, byte option) {
        String colDbase = collator.getSortKey(ch, option, true);
        String colNbase = collator.getSortKey(ch, option, false);
        String colCbase = collator.getSortKey(nfc.normalize(ch), option, false);
        if (!colNbase.equals(colCbase)) {
            System.out.println(Utility.hex(ch));
            System.out.println(printableKey(colNbase));
            System.out.println(printableKey(colNbase));
            System.out.println(printableKey(colNbase));
            MismatchedN.put(ch,colNbase);
            MismatchedC.put(ch,colCbase);
            MismatchedD.put(ch,colDbase);
        }
        String colD = colDbase + "\u0000" + ch; // UCA.NON_IGNORABLE
        String colN = colNbase + "\u0000" + ch;
        String colC = colCbase + "\u0000" + ch;
        sortedD.put(colD, ch);
        backD.put(ch, colD);
        sortedN.put(colN, ch);
        backN.put(ch, colN);

        if (strength > 4) {
            duplicateCount++;
            duplicates.put(ch+MARK1, col);
            duplicates.put(ch+MARK2, col2);
        } else if (strength != 0) {
            sorted.put(col2 + MARK2, ch);
        }
        unique += 2;

    }

   static void removeAdjacentDuplicates() {
        String lastChar = "";
        int countRem = 0;
        int countDups = 0;
        Iterator it1 = sortedD.keySet().iterator();
        Iterator it2 = sortedN.keySet().iterator();
        Differ differ = new Differ(250,3);
        log.println("<h1>2. Differences in Ordering</h1>");
        log.println("<p>Codes and names are in the white rows: bold means that the NO-NFD sort key differs from UCA key.</p>");
        log.println("<p>Keys are in the light blue rows: green is the bad key, blue is UCA, black is where they equal.</p>");
        log.println("<table border='1'>");
        log.println("<tr><th>File Order</th><th>Code and Decomp</th><th>Key and Decomp-Key</th></tr>");

        while (true) {
            boolean gotOne = false;
            if (it1.hasNext()) {
                String col1 = (String)it1.next();
                String ch1 = (String)sortedD.get(col1);
                differ.addA(ch1);
                gotOne = true;
            }

            if (it2.hasNext()) {
                String col2 = (String)it2.next();
                String ch2 = (String)sortedN.get(col2);
                differ.addB(ch2);
                gotOne = true;
            }

            differ.checkMatch(!gotOne);

            if (differ.getACount() != 0 || differ.getBCount() != 0) {
                for (int q = 0; q < 2; ++q) {
                    String cell = "<td valign='top'" + (q!=0 ? "bgcolor='#C0C0C0'" : "") + ">" + (q!=0 ? "<tt>" : "");

                    log.print("<tr>" + cell);
                    for (int i = -1; i < differ.getACount()+1; ++i) {
                        showDiff(q==0, true, differ.getALine(i), differ.getA(i));
                        log.println("<br>");
                        ++countDups;
                    }
                    countDups -= 2; // to make up for extra line above and below
                    if (false) {
                        log.print("</td>" + cell);

                        for (int i = -1; i < differ.getBCount()+1; ++i) {
                            showDiff(q==0, false, differ.getBLine(i), differ.getB(i));
                            log.println("<br>");
                        }
                    }
                    log.println("</td></tr>");
                }
            }
            //differ.flush();

            if (!gotOne) break;
        }

        log.println("</table>");

        //log.println("Removed " + countRem + " adjacent duplicates.<br>");
        System.out.println("Left " + countDups + " conflicts.<br>");
        log.println("Left " + countDups + " conflicts.<br>");
   }

   static void removeAdjacentDuplicates2() {
        String lastChar = "";
        int countRem = 0;
        int countDups = 0;
        Iterator it = sortedD.keySet().iterator();
        log.println("<h1>2. Differences in Ordering</h1>");
        log.println("<p>Codes and names are in the white rows: bold means that the NO-NFD sort key differs from UCA key.</p>");
        log.println("<p>Keys are in the light blue rows: green is the bad key, blue is UCA, black is where they equal.</p>");
        log.println("<table border='1'>");
        log.println("<tr><th>File Order</th><th>Code and Decomp</th><th>Key and Decomp-Key</th></tr>");

        String lastCol = "a";
        String lastColN = "a";
        String lastCh = "";
        boolean showedLast = true;
        int count = 0;
        while (it.hasNext()) {
            count++;
            String col = (String)it.next();
            String ch = (String)sortedD.get(col);
            String colN = (String)backN.get(ch);
            if (colN == null || colN.length() < 1) {
                System.out.println("Missing colN value for " + Utility.hex(ch, " ") + ": " + printableKey(colN));
            }
            if (col == null || col.length() < 1) {
                System.out.println("Missing col value for " + Utility.hex(ch, " ") + ": " + printableKey(col));
            }

            if (compareMinusLast(col, lastCol) == compareMinusLast(colN, lastColN)) {
                showedLast = false;
            } else {
                if (true && count < 200) {
                    System.out.println();
                    System.out.println(Utility.hex(ch, " ") + ", " + Utility.hex(lastCh, " "));
                    System.out.println("      col: " + Utility.hex(col, " "));
                    System.out.println(compareMinusLast(col, lastCol));
                    System.out.println("  lastCol: " + Utility.hex(lastCol, " "));
                    System.out.println();
                    System.out.println("     colN: " + Utility.hex(colN, " "));
                    System.out.println(compareMinusLast(colN, lastColN));
                    System.out.println(" lastColN: " + Utility.hex(lastColN, " "));
                }
                if (!showedLast) {
                    log.println("<tr><td colspan='3'></td><tr>");
                    showLine(count-1, lastCh, lastCol, lastColN);
                }
                showedLast = true;
                showLine(count,ch, col, colN);
            }
            lastCol = col;
            lastColN = colN;
            lastCh = ch;
        }

        log.println("</table>");
   }

    static int compareMinusLast(String a, String b) {
        String am = a.substring(0,a.length()-1);
        String bm = b.substring(0,b.length()-1);
        int result = am.compareTo(b);
        return (result < 0 ? -1 : result > 0 ? 1 : 0);
    }

    static void showLine(int count, String ch, String keyD, String keyN) {
        String decomp = nfd.normalize(ch);
        if (decomp.equals(ch)) decomp = ""; else decomp = "<br><" + Utility.hex(decomp, " ") + "> ";
        log.println("<tr><td>" + count + "</td><td>"
            + Utility.hex(ch, " ")
            + " " + ucd.getName(ch)
            + decomp
            + "</td><td>");

        if (keyD.equals(keyN)) {
            log.println(printableKey(keyN));
        } else {
            log.println("<font color='#009900'>" + printableKey(keyN)
                + "</font><br><font color='#000099'>" + printableKey(keyD) + "</font>"
            );
        }
        log.println("</td></tr>");
    }

    TreeSet foo;

    static final String[] alternateName = {"SHIFTED", "ZEROED", "NON_IGNORABLE", "SHIFTED_TRIMMED"};

    static void showMismatches() {
        MLStreamWriter out = new MLStreamWriter(log);
        out.el("h1").tx("1. Mismatches when NFD is OFF").cl();
        out.el("h2").tx("Date:" + new Date()).cl();
        out.el("h2").tx("File Version:" + UCA.VERSION).cl();
        out.el("p").tx("Alternate Handling = " + alternateName[option]).cl();
        out.el("table").at("border",1);
            out.el("caption").tx("Mismatches in UCA-NOD: Plain vs NFC: ").tx(MismatchedC.size()).cl("caption");
            out.el("tr");
                out.el("th").tx("Code").cl();
                out.el("th").tx("Type").cl();
                out.el("th").tx("CC?").cl();
                out.el("th").tx("Key").cl();
            out.cl("tr");
        Iterator it = MismatchedC.keySet().iterator();
        while (it.hasNext()) {
            String ch = (String)it.next();
            String MN = (String)MismatchedN.get(ch);
            String MC = (String)MismatchedC.get(ch);
            String chInC = nfc.normalize(ch);
            out.el("tr");
              out.el("th").at("rowSpan",2).at("align","right").tx16(ch).tx(' ').tx(ucd.getName(ch));
                out.el("br").cl().tx("NFC=").tx16(chInC).cl();
              out.el("th").tx("Plain").cl();
              out.el("th").tx(containsCombining(ch) ? "y" : "n").cl();
              out.el("td").tx(printableKey(MN)).cl();
            out.cl("tr");
            out.el("tr");
              out.el("th").tx("NFC").cl();
              out.el("th").tx(containsCombining(chInC) ? "Y" : "ERROR").cl();
              out.el("td").tx(printableKey(MC)).cl();
            out.cl("tr");
        }
        out.closeAllElements();
        log.println("<br>");
    }

    static boolean containsCombining(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if ((ucd.getCategoryMask(s.charAt(i)) & ucd.MARK_MASK) != 0) return true;
        }
        return false;
    }


    static void showDiff(boolean showName, boolean firstColumn, int line, Object chobj) {
        String ch = chobj.toString();
        String decomp = nfd.normalize(ch);
        if (showName) {
            if (ch.equals(decomp)) {
                log.println(//title + counter + " "
                    Utility.hex(ch, " ")
                    + " " + ucd.getName(ch)
                );
            } else {
                log.println(//title + counter + " "
                    "<b>" + Utility.hex(ch, " ")
                    + " " + ucd.getName(ch) + "</b>"
                );
            }
        } else {
            String keyD = printableKey(backD.get(chobj));
            String keyN = printableKey(backN.get(chobj));
            if (keyD.equals(keyN)) {
                log.println(//title + counter + " "
                    Utility.hex(ch, " ") + " " + keyN);
            } else {
                log.println(//title + counter + " "
                    "<font color='#009900'>" + Utility.hex(ch, " ") + " " + keyN
                    + "</font><br><font color='#000099'>" + Utility.hex(decomp, " ") + " " + keyD + "</font>"
                );
            }
        }
    }

    static String printableKey(Object keyobj) {
        String sortKey;
        if (keyobj == null) {
            sortKey = "NULL!!";
        } else {
            sortKey = keyobj.toString();
            sortKey = sortKey.substring(0,sortKey.length()-1);
            sortKey = UCA.toString(sortKey);
        }
        return sortKey;
    }


      LINKS</td></tr><tr><td><blockquote>
     CONTENTS



    static void writeTail(PrintWriter out, int counter, String title, String other, boolean show) throws IOException {
        copyFile(out, "HTML-Part2.txt");

        out.println("</tr></table></center></div>");
        out.println("</body></html>");

        out.close();
    }

    static String pad (int number) {
        String num = Integer.toString(number);
        if (num.length() < 2) num = "0" + number;
        return num;
    }

    static PrintWriter writeHead(int counter, int end, String title, String other, String version, boolean show) throws IOException {

        PrintWriter out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(UCA.BASE_DIR+"\\generated\\collation/" + title + pad(counter) + ".html"),
                "UTF8"),
            4*1024));
        copyFile(out, "HTML-Part1.txt");

        out.println("<html><head>");
        out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        out.println("<title>" + HTMLString(title) + "</title>");
        out.println("<style type='text/css'>");
        out.println("<!--");
        //out.println("td           { font-size: 18pt; font-family: Bitstream Cyberbit, Arial Unicode MS; text-align: Center}");
        out.println("td           { font-size: 18pt; text-align: Center}");
        out.println("td.right           { font-size: 12pt; text-align: Right}");
        out.println("td.title           { font-size: 18pt; text-align: Center}");
        out.println("td.left           { font-size: 12pt; text-align: Left}");
        //out.println("th         { background-color: #C0C0C0; font-size: 18pt; font-family: Arial Unicode MS, Bitstream Cyberbit; text-align: Center }");
        out.println("tt         { font-size: 8pt; }");
        //out.println("code           { font-size: 8pt; }");
        out.println("-->");
        out.println("</style></head><body bgcolor='#FFFFFF'>");

        // header
        out.print("<table width='100%'><tr>");
        out.println("<td><p align='left'><font size='3'><a href='index.html'>Instructions</a></font></td>");
        out.println("<td>" + HTMLString(title) + " Version" + version + "</td>");
        out.println("<td><p align='right'><font size='3'><a href='" + other + pad(counter) + ".html'>"
            + (show ? "Hide" : "Show") + " Key</a></td>");
        out.println("</tr></table>");
        /*
        <table width="100%">
  <tr>
    <td.left><a href="Collation.html">
      <font size="3">Instructions</font></a>
    <td>
      <td.title>Collation Version-2.1.9d7
    <td>
      <p align="right"><a href="CollationKey24.html"><font size="3">Show Key</font></a>
  </tr>



        // index
        out.print("<table width='100%'><tr>");
        out.println("<td><p align='left'><font size='3'><a href='index.html'>Instructions</a></font></td>");
        out.println("<td>" + HTMLString(title) + " Version" + version + "</td>");
        out.println("<td><p align='right'><font size='3'><a href='" + other + pad(counter) + ".html'>"
            + (show ? "Hide" : "Show") + " Key</a></td>");
        out.println("</tr></table>");

        out.print("<table width='100%'><tr>");
        out.print("<td width='1%'><p align='left'>");
        if (counter > 0) {
            out.print("<a href='" + title + pad(counter-1) + ".html'>&lt;&lt;</a>");
        } else {
            out.print("<font color='#999999'>&lt;&lt;</font>");
        }
        out.println("</td>");
        out.println("<td><p align='center'>");
        boolean lastFar = false;
        for (int i = 0; i <= end; ++i) {
            boolean far = (i < counter-2 || i > counter+2);
            if (far && ((i % 5) != 0) && (i != end)) continue;
            if (i != 0 && lastFar != far) out.print(" - ");
            lastFar = far;
            if (i != counter) {
                out.print("<a href='" + title + pad(i) + ".html'>" + i + "</a>");
            } else {
                out.print("<font color='#FF0000'>" + i + "</font>");
            }
            out.println();
        }
        out.println("</td>");
        out.println("<td width='1%'><p align='right'>");
        if (counter < end) {
            out.print("<a href='" + title + pad(counter+1) + ".html'>&gt;&gt;</a>");
        } else {
            out.print("<font color='#999999'>&gt;&gt;</font>");
        }
        out.println("</td></tr></table>");
        // standard template!!!
        out.println("</td></tr><tr><td><blockquote>");
        //out.println("<p><div align='center'><center><table border='1'><tr>");
        return out;
    }

    static int getStrengthDifference(String old, String newStr) {
        int result = 5;
        int min = old.length();
        if (newStr.length() < min) min = newStr.length();
        for (int i = 0; i < min; ++i) {
            char ch1 = old.charAt(i);
            char ch2 = newStr.charAt(i);
            if (ch1 != ch2) return result;
            // see if we get difference before we get 0000.
            if (ch1 == 0) --result;
        }
        if (newStr.length() != old.length()) return 1;
        return 0;
    }


    static final boolean needsXMLQuote(String source, boolean quoteApos) {
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch < ' ' || ch == '<' || ch == '&' || ch == '>') return true;
            if (quoteApos & ch == '\'') return true;
            if (ch == '\"') return true;
            if (ch >= '\uD800' && ch <= '\uDFFF') return true;
            if (ch >= '\uFFFE') return true;
        }
        return false;
    }

    public static final String XMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, true);
    }

    public static final String XMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, true);
    }

    public static final String XMLString(String source) {
        return XMLBaseString(source, true);
    }

    public static final String HTMLString(int[] cps) {
        return XMLBaseString(cps, cps.length, false);
    }

    public static final String HTMLString(int[] cps, int len) {
        return XMLBaseString(cps, len, false);
    }

    public static final String HTMLString(String source) {
        return XMLBaseString(source, false);
    }

    public static final String XMLBaseString(int[] cps, int len, boolean quoteApos) {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            temp.append((char)cps[i]);
        }
        return XMLBaseString(temp.toString(), quoteApos);
    }

    public static final String XMLBaseString(String source, boolean quoteApos) {
        if (!needsXMLQuote(source, quoteApos)) return source;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch < ' '
              || ch >= '\u007F' && ch <= '\u009F'
              || ch >= '\uD800' && ch <= '\uDFFF'
              || ch >= '\uFFFE') {
                result.append('\uFFFD');
                result.append("#x");
                result.append(cpName(ch));
                result.append(";");

            } else if (quoteApos && ch == '\'') {
                result.append("&apos;");
            } else if (ch == '\"') {
                result.append("&quot;");
            } else if (ch == '<') {
                result.append("&lt;");
            } else if (ch == '&') {
                result.append("&amp;");
            } else if (ch == '>') {
                result.append("&gt;");
             } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    static int mapToStartOfRange(int ch) {
        if (ch <= 0x3400) return ch;         // CJK Ideograph Extension A
        if (ch <= 0x4DB5) return 0x3400;
        if (ch <= 0x4E00) return ch;         // CJK Ideograph
        if (ch <= 0x9FA5) return 0x4E00;
        if (ch <= 0xAC00) return ch;         // Hangul Syllable
        if (ch <= 0xD7A3) return 0xAC00;
        if (ch <= 0xD800) return ch;         // Non Private Use High Surrogate
        if (ch <= 0xDB7F) return 0xD800;
        if (ch <= 0xDB80) return ch;         // Private Use High Surrogate
        if (ch <= 0xDBFF) return 0xDB80;
        if (ch <= 0xDC00) return ch;         // Low Surrogate
        if (ch <= 0xDFFF) return 0xDC00;
        if (ch <= 0xE000) return ch;         // Private Use
        if (ch <= 0xF8FF) return 0xE000;
        if (ch <= 0xF0000) return ch;       // Plane 15 Private Use
        if (ch <= 0xFFFFD) return 0xF0000;
        if (ch <= 0x100000) return ch;       // Plane 16 Private Use
        return 0x100000;
    }

      */
}