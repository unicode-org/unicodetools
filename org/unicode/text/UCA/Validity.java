package org.unicode.text.UCA;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.util.With;
import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCA.UCA.UCAContents;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Pair;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

final class Validity {
    private static final boolean DEBUG = false;
    private static final boolean GENERATED_NFC_MISMATCHES = true;
    private static UnicodeSet compatibilityExceptions = new UnicodeSet("[\u0CCB\u0DDD\u017F\u1E9B\uFB05]");
    private static TreeMap<String, String>  sortedD     = new TreeMap<String, String>();
    private static TreeMap<String, String>  sortedN     = new TreeMap<String, String>();
    private static HashMap<String, String>  backD       = new HashMap<String, String>();
    private static HashMap<String, String>  backN       = new HashMap<String, String>();
    private static TreeMap<String, String>  duplicates  = new TreeMap<String, String>();
    private static PrintWriter              log;
    private static UCA                      uca;

    private static String ERROR_STRING = "<b style=color:red>ERROR</b>";
    private static String KNOWN_ISSUE_STRING = "<b style=color:orange>Known Issue</b>";

    // Called by UCA.Main.
    static void writeCollationValidityLog() throws IOException {
        log = Utility.openPrintWriter(UCA.getUCA_GEN_DIR(), "CheckCollationValidity.html", Utility.UTF8_WINDOWS);
        uca = WriteCollationData.getCollator(CollatorType.ducet);

        log.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>\n" +
                "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        log.println("<title>UCA Validity Log</title>");
        log.println("<style type='text/css'>");
        log.println("table { border-collapse: collapse; }");
        log.println(".bottom { border-bottom-style: solid; border-bottom-color: #0000FF; }\n");
        log.println(".bad { color:red; font-weight:bold; }");
        log.println(".warn { color:orange; font-weight:bold; }");
        log.println("</style>");
        log.println("</head><body bgcolor='#FFFFFF'>");

        System.out.println("Sorting");

        UCAContents cc = uca.getContents(null);
        // cc.setDoEnableSamples(true);
        UnicodeSet coverage = new UnicodeSet();

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            addString(s, option, CollatorType.ducet);
            coverage.add(s);
        }

        System.out.println("Total: " + sortedD.size());

        Iterator<String> it;

        // ucd.init();

        if (false) {
            System.out.println("Listing Mismatches");
            it = duplicates.keySet().iterator();
            // String lastSortKey = "";
            // String lastSource = "";
            while (it.hasNext()) {
                String source = it.next();
                String sortKey = duplicates.get(source);
                char endMark = source.charAt(source.length() - 1);
                source = source.substring(0, source.length() - 1);
                if (endMark == MARK1) {
                    log.println("<br>");
                    log.println("Mismatch: " + Utility.hex(source, " ")
                            + ", " + Default.ucd().getName(source) + "<br>");
                    log.print("  NFD:");
                } else {
                    log.print("  NFC:");
                }
                log.println(UCA.toString(sortKey) + "<br>");

                /*
                 * if (source.equals(lastSource)) { it.remove();
                 * --duplicateCount; } //lastSortKey = sortKey; lastSource =
                 * lastSource;
                 */
            }
            System.out.println("Total: " + sortedD.size());
        }

        System.out.println("Writing");

        log.println("<h1>Collation Validity Checks</h1>");
        log.println("<table><tr><td>Generated: </td><td>" + WriteCollationData.getNormalDate() + "</td></tr>");
        log.println("<tr><td>Unicode  Version: </td><td>" + uca.getUCDVersion());
        log.println("<tr><td>UCA Data Version (@version in file): </td><td>" + uca.getDataVersion());
        log.println("<tr><td>UCA File Name: </td><td>" + uca.getFileVersion());
        log.println("</td></tr></table>");

        if (uca.getDataVersion() == UCA.BADVERSION) {
            log.println(SERIOUS_ERROR);
        }

        checkScripts();

        if (GENERATED_NFC_MISMATCHES) {
            showMismatches();
        }
        removeAdjacentDuplicates2();

        UnicodeSet alreadySeen = new UnicodeSet(compatibilityExceptions);

        checkBadDecomps(1, false, alreadySeen); // if decomposition is off, all
        // primaries should be identical
        checkBadDecomps(2, false, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries
        // should be identical
        checkBadDecomps(3, false, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries
        // should be identical
        // checkBadDecomps(2, true, alreadySeen); // if decomposition is ON, all
        // primaries and secondaries should be identical

        log.println("<p>Note: characters with decompositions to space + X, and tatweel + X are excluded,"
                + " as are a few special characters: " + compatibilityExceptions.toPattern(true) + "</p>");

        checkDefaultIgnorables();
        checkUnassigned();
        checkWellformedTable();
        addClosure();
        writeDuplicates();
        checkMissingPrefixContractions();

        log.println("<h2>11. Coverage</h2>");
        BagFormatter bf = new BagFormatter();
        bf.setLineSeparator("<br>\r\n");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        bf.setUnicodePropertyFactory(ups);
        bf.setShowLiteral(TransliteratorUtilities.toHTML);
        bf.setFixName(TransliteratorUtilities.toHTML);
        UnicodeProperty cat = ups.getProperty("gc");
        UnicodeSet ucdCharacters = cat.getSet("Cn")
                .addAll(cat.getSet("Co"))
                .addAll(cat.getSet("Cs"))
                .complement()
                // .addAll(ups.getSet("Noncharactercodepoint=true"))
                // .addAll(ups.getSet("Default_Ignorable_Code_Point=true"))
                ;
        bf.showSetDifferences(log, "UCD" + Default.ucdVersion(), ucdCharacters, uca.getFileVersion(), coverage, 3);

        log.println("</body></html>");
        log.close();
        sortedD.clear();
        System.out.println("Done");
    }

    private static CanonicalIterator canIt = null;

    private static UCD ucd_uca_base = null;

    private static String remapSortKey(int cp, boolean decomposition) {
        if (Default.nfd().isNormalized(cp)) {
            return remapCanSortKey(cp, decomposition);
        }

        // we know that it is not NFKD.
        String canDecomp = Default.nfd().normalize(cp);
        String result = "";
        int ch;
        for (int j = 0; j < canDecomp.length(); j += UTF16.getCharCount(ch)) {
            ch = UTF16.charAt(canDecomp, j);
            System.out.println("* " + Default.ucd().getCodeAndName(ch));
            String newSortKey = remapCanSortKey(ch, decomposition);
            System.out.println("* " + UCA.toString(newSortKey));
            result = mergeSortKeys(result, newSortKey);
            System.out.println("= " + UCA.toString(result));
        }
        return result;
    }

    private static String remapCanSortKey(int ch, boolean decomposition) {
        String compatDecomp = Default.nfkd().normalize(ch);
        String decompSortKey = uca.getSortKey(compatDecomp, UCA.NON_IGNORABLE, decomposition, AppendToCe.none);

        byte type = Default.ucd().getDecompositionType(ch);
        int pos = decompSortKey.indexOf(UCA.LEVEL_SEPARATOR) + 1; // after first
        // separator
        pos = decompSortKey.indexOf(UCA.LEVEL_SEPARATOR, pos) + 1; // after
        // second
        // separator
        String newSortKey = decompSortKey.substring(0, pos);
        for (int i = pos; i < decompSortKey.length(); ++i) {
            int weight = decompSortKey.charAt(i);
            int newWeight = CEList.remap(ch, type, weight);
            newSortKey += (char) newWeight;
        }
        return newSortKey;
    }

    // keys must be of the same strength
    private static String mergeSortKeys(String key1, String key2) {
        StringBuffer result = new StringBuffer();
        int end1 = 0, end2 = 0;
        while (true) {
            int pos1 = key1.indexOf(UCA.LEVEL_SEPARATOR, end1);
            int pos2 = key2.indexOf(UCA.LEVEL_SEPARATOR, end2);
            if (pos1 < 0) {
                result.append(key1.substring(end1)).append(key2.substring(end2));
                return result.toString();
            }
            if (pos2 < 0) {
                result.append(key1.substring(end1, pos1)).append(key2.substring(end2)).append(key1.substring(pos1));
                return result.toString();
            }
            result.append(key1.substring(end1, pos1)).append(key2.substring(end2, pos2)).append(UCA.LEVEL_SEPARATOR);
            end1 = pos1 + 1;
            end2 = pos2 + 1;
        }
    }

    private static final String remove(String s, char ch) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ch) {
                continue;
            }
            buf.append(c);
        }
        return buf.toString();
    }

    private static void writeDuplicates() {
        log.println("<h2>9. Checking characters that are not canonical equivalents, but have same CE</h2>");
        log.println("<p>These are not necessarily errors, but should be examined for <i>possible</i> errors</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");

        UCAContents cc = uca.getContents(Default.nfd());

        Map<CEList,Set<String>> map = new TreeMap<CEList,Set<String>>();

        while (true) {
            String s = cc.next();
            if (s == null) {
                break;
            }
            if (!Default.nfd().isNormalized(s)) {
                continue; // only unnormalized stuff
            }
            if (UTF16.countCodePoint(s) == 1) {
                int cat = Default.ucd().getCategory(UTF16.charAt(s, 0));
                if (cat == UCD_Types.Cn || cat == UCD_Types.Cc || cat == UCD_Types.Cs) {
                    continue;
                }
            }

            CEList celist = uca.getCEList(s, true);
            Utility.addToSet(map, celist, s);
        }

        Iterator<CEList> it = map.keySet().iterator();
        while (it.hasNext()) {
            CEList celist = it.next();
            Set<String> s = map.get(celist);
            String name = celist.toString();
            if (name.length() == 0) {
                name = "<i>ignore</i>";
            }
            if (s.size() > 1) {
                log.println("<tr><td>" + name
                        + "</td><td>" + getHTML_NameSet(s, null, true)
                        + "</td></tr>");
            }
        }
        log.println("</table>");
        log.flush();
    }

    /**
     * Checks for well-formedness condition 5:
     *
     * <p>If a table contains a contraction consisting of a sequence of N code points,
     * with N > 2 and the last code point being a non-starter,
     * then the table must also contain a contraction
     * consisting of the sequence of the first N-1 code points.
     *
     * <p>For example, if "ae+umlaut" is a contraction, then "ae" must be a contraction as well.
     */
    private static void checkMissingPrefixContractions() {
        log.println("<h2>10. Checking for missing prefix contractions (WF5)</h2>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Severity</th><th>missing prefix</th><th>of contraction</th></tr>");
        Set<String> contractions = uca.getContractions();
        for (String contraction : contractions) {
            if (!UTF16.hasMoreCodePointsThan(contraction, 2)) {
                continue;
            }
            int lastCodePoint = contraction.codePointBefore(contraction.length());
            int cc = uca.getNFDNormalizer().getCanonicalClass(lastCodePoint);
            if (cc != 0) {
                int prefixLength = contraction.length() - Character.charCount(lastCodePoint);
                String prefixContraction = contraction.substring(0, prefixLength);
                if (!contractions.contains(prefixContraction)) {
                    String severity;
                    if (prefixContraction.equals("\u0FB2\u0F71") ||
                            prefixContraction.equals("\u0FB3\u0F71")) {
                        // see http://www.unicode.org/reports/tr10/#Well_Formed_DUCET
                        severity = KNOWN_ISSUE_STRING;
                    } else {
                        severity = ERROR_STRING;
                    }
                    log.println("<tr><td>" + severity
                            + "</td><td>" + Default.ucd().getCodeAndName(prefixContraction)
                            + "</td><td>" + Default.ucd().getCodeAndName(contraction)
                            + "</td></tr>");
                }
            }
        }
        log.println("</table>");
        log.flush();
    }

    // if m exists, then it is a mapping to strings. Use it.
    // otherwise just print what is in set
    private static <K, V> String getHTML_NameSet(Collection<K> set, Map<K,V> m, boolean useName) {
        StringBuffer result = new StringBuffer();
        Iterator<K> it = set.iterator();
        while (it.hasNext()) {
            if (result.length() != 0) {
                result.append(";<br>");
            }
            K item = it.next();
            String name = null;
            if (m != null) {
                V name0 = m.get(item);
                if (name0 == null) {
                    System.out.println("Missing Item: " + item);
                    name = item.toString();
                } else if (useName) {
                    name = Default.ucd().getCodeAndName(name0.toString());
                } else {
                    name = name0.toString();
                }
            } else {
                name = item.toString();
            }
            result.append(name);
        }
        return result.toString();
    }

    /**
     * Check that the primaries are the same as the compatibility decomposition.
     */
    private static void checkBadDecomps(int strength, boolean decomposition, UnicodeSet alreadySeen) {
        if (ucd_uca_base == null) {
            ucd_uca_base = UCD.make(uca.getUCDVersion());
        }
        int oldStrength = uca.getStrength();
        uca.setStrength(strength);
        switch (strength) {
        case 1:
            log.println("<h2>3. Primaries Incompatible with NFKD</h2>");
            break;
        case 2:
            log.println("<h2>4. Secondaries Incompatible with NFKD</h2>");
            break;
        case 3:
            log.println("<h2>5. Tertiaries Incompatible with NFKD</h2>");
            break;
        default:
            throw new IllegalArgumentException("bad strength: " + strength);
        }
        log.println("<p>Note: Differences are not really errors; but they should be checked over for inadvertant problems</p>");
        log.println("<p>Warning: only checking characters defined in base: " + ucd_uca_base.getVersion() + "</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Code</td><th>Sort Key</th><th>NFKD Sort Key</th><th>Name</th></tr>");

        int errorCount = 0;

        UnicodeSet skipSet = new UnicodeSet();

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            if (!ucd_uca_base.isAllocated(ch)) {
                continue;
            }
            if (Default.nfkd().isNormalized(ch)) {
                continue;
            }
            if (ch > 0xAC00 && ch < 0xD7A3) {
                continue; // skip most of Hangul
            }
            if (alreadySeen.contains(ch)) {
                continue;
            }
            Utility.dot(ch);

            String decomp = Default.nfkd().normalize(ch);
            if (ch != ' ' && decomp.charAt(0) == ' ') {
                skipSet.add(ch);
                continue; // skip weird decomps
            }
            if (ch != '\u0640' && decomp.charAt(0) == '\u0640') {
                skipSet.add(ch);
                continue; // skip weird decomps
            }

            String sortKey = uca.getSortKey(UTF16.valueOf(ch), UCA.NON_IGNORABLE, decomposition, AppendToCe.none);
            String decompSortKey = uca.getSortKey(decomp, UCA.NON_IGNORABLE, decomposition, AppendToCe.none);
            if (false && strength == 2) {
                sortKey = remove(sortKey, '\u0020');
                decompSortKey = remove(decompSortKey, '\u0020');
            }

            if (sortKey.equals(decompSortKey)) {
                continue; // no problem!
            }

            // fix key in the case of strength 3

            if (strength == 3) {
                String newSortKey = remapSortKey(ch, decomposition);
                if (!sortKey.equals(newSortKey)) {
                    System.out.println("Fixing: " + Default.ucd().getCodeAndName(ch));
                    System.out.println("  Old:" + UCA.toString(decompSortKey));
                    System.out.println("  New: " + UCA.toString(newSortKey));
                    System.out.println("  Tgt: " + UCA.toString(sortKey));
                }
                decompSortKey = newSortKey;
            }

            if (sortKey.equals(decompSortKey)) {
                continue; // no problem!
            }

            log.println("<tr><td>" + Utility.hex(ch)
                    + "</td><td>" + UCA.toString(sortKey)
                    + "</td><td>" + UCA.toString(decompSortKey)
                    + "</td><td>" + Default.ucd().getName(ch)
                    + "</td></tr>"
                    );
            alreadySeen.add(ch);
            errorCount++;
        }
        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        log.println("<p>Space/Tatweel exceptions: " + skipSet.toPattern(true) + "</p>");
        log.flush();
        uca.setStrength(oldStrength);
        Utility.fixDot();
    }

    private static void checkUnassigned() {
        System.out.println("Checking that Unassigned characters have implicits");

        log.println("<h2>5b. Checking that Unassigned characters have implicit weights</h2>");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        UnicodeSet di = ups.getSet("gc=cn");
        UnicodeSet bad = new UnicodeSet();
        int[] output = new int[2];

        // the invariants are that there must be exactly 2 ces; the ces will
        // match codepointToImplicit
        for (String diChar : di) {
            CEList ceList = uca.getCEList(diChar, true);
            if (ceList.length() != 2) {
                bad.add(diChar);
            } else {
                UCA.UnassignedToImplicit(diChar.codePointAt(0), output);
                int ce0 = ceList.at(0);
                int ce1 = ceList.at(1);
                if (CEList.getPrimary(ce0) != output[0] || CEList.getPrimary(ce1) != output[1]) {
                    bad.add(diChar);
                }
            }
        }
        if (bad.size() == 0) {
            log.println("<h3>No Bad Characters</h3>");
        } else {
            log.println(SERIOUS_ERROR);
            log.println("<h3>Bad Unassigned Characters: " + bad.size() + ": " + bad + "</h3>");
        }
        log.flush();

    }

    private static void checkDefaultIgnorables() {
        UnicodeSet exceptions = new UnicodeSet("[\u115F\u1160\u17B4\u17B5\u3164\uFFA0]");
        System.out.println("Checking for defaultIgnorables");

        log.println("<h2>5a. Checking for Default Ignorables</h2>");
        log.println("<p>Checking that all Default Ignorables except " + exceptions + " should be secondary ignorables (L1 = L2 = 0)</p>");
        ToolUnicodePropertySource ups = getToolUnicodeSource();
        UnicodeSet di = new UnicodeSet(ups.getSet("default_ignorable_code_point=true")).removeAll(ups.getSet("gc=cn")).removeAll(exceptions);
        UnicodeSet bad = new UnicodeSet();
        for (String diChar : di) {
            CEList ceList = uca.getCEList(diChar, true);
            for (int i = 0; i < ceList.length(); ++i) {
                int ce = ceList.at(i);
                if (CEList.getPrimary(ce) != 0 || CEList.getSecondary(ce) != 0) {
                    bad.add(diChar);
                }
            }
        }
        if (bad.size() == 0) {
            log.println("<h3>No Bad Characters</h3>");
        } else {
            log.println(SERIOUS_ERROR);
            UnicodeSet badUnassigned = ups.getSet("gc=cn").retainAll(bad);
            UnicodeSet badAssigned = bad.removeAll(badUnassigned);
            log.println("<h3>Bad Assigned Characters: " + badAssigned.size() + ": " + badAssigned +
                    "</h3>");
            for (String diChar : badAssigned) {
                log.println("<p>" + Default.ucd().getCodeAndName(diChar) + "</p>");
            }
            log.println("<h3>Bad Unassigned Characters: " + badUnassigned.size() + ": " + badUnassigned +
                    "</h3>");
        }
        log.flush();
    }

    private static ToolUnicodePropertySource ups;

    private static ToolUnicodePropertySource getToolUnicodeSource() {
        if (ups == null) {
            ups = ToolUnicodePropertySource.make(Default.ucdVersion());
        }
        return ups;
    }

    private static void addClosure() {
        int canCount = 0;
        System.out.println("Add missing decomposibles");
        log.println("<h2>7. Comparing Other Equivalents</h2>");
        log.println("<p>These are usually problems with contractions.</p>");
        log.println("<p>Each of the three strings is canonically equivalent, but has different sort keys</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>Count</th><th>Type</th><th>Name</th><th>Code</th><th>Sort Keys</th></tr>");

        Set<String> contentsForCanonicalIteration = new TreeSet<String>();
        UCAContents ucac = uca.getContents(null); // NFD
        int ccounter = 0;
        while (true) {
            Utility.dot(ccounter++);
            String s = ucac.next();
            if (s == null) {
                break;
            }
            contentsForCanonicalIteration.add(s);
        }

        Set<String> additionalSet = new HashSet<String>();

        System.out.println("Loading canonical iterator");
        if (canIt == null) {
            canIt = new CanonicalIterator(".");
        }
        Iterator<String> it2 = contentsForCanonicalIteration.iterator();
        System.out.println("Adding any FCD equivalents that have different sort keys");
        while (it2.hasNext()) {
            String key = it2.next();
            if (key == null) {
                System.out.println("Null Key");
                continue;
            }
            canIt.setSource(key);
            String nfdKey = Default.nfd().normalize(key);

            boolean first = true;
            while (true) {
                String s = canIt.next();
                if (s == null) {
                    break;
                }
                if (s.equals(key)) {
                    continue;
                }
                if (contentsForCanonicalIteration.contains(s)) {
                    continue;
                }
                if (additionalSet.contains(s)) {
                    continue;
                }

                // Skip anything that is not FCD.
                if (!Default.nfd().isFCD(s)) {
                    continue;
                }

                // We ONLY add if the sort key would be different
                // Than what we would get if we didn't decompose!!
                String sortKey = uca.getSortKey(s, UCA.NON_IGNORABLE);
                String nonDecompSortKey = uca.getSortKey(s, UCA.NON_IGNORABLE, false, AppendToCe.none);
                if (sortKey.equals(nonDecompSortKey)) {
                    continue;
                }

                if (DEBUG && first) {
                    System.out.println(" " + Default.ucd().getCodeAndName(key));
                    first = false;
                }
                log.println("<tr><td rowspan='3'>" + (++canCount) +
                        "</td><td>Orig.</td><td>" + Utility.replace(Default.ucd().getName(key), ", ", ",<br>") + "</td>");
                log.println("<td>" + Utility.hex(key) + "</td>");
                log.println("<td>" + UCA.toString(sortKey) + "</td></tr>");
                log.println("<tr><td>NFD</td><td>" + Utility.replace(Default.ucd().getName(nfdKey), ", ", ",<br>") + "</td>");
                log.println("<td>" + Utility.hex(nfdKey) + "</td>");
                log.println("<td>" + UCA.toString(sortKey) + "</td></tr>");
                log.println("<tr><td>Equiv.</td><td class='bottom'>" + Utility.replace(Default.ucd().getName(s), ", ", ",<br>") + "</td>");
                log.println("<td class='bottom'>" + Utility.hex(s) + "</td>");
                log.println("<td class='bottom'>" + UCA.toString(nonDecompSortKey) + "</td></tr>");
                additionalSet.add(s);
            }
        }
        log.println("</table>");
        log.println("<p>Errors: " + canCount + "</p>");
        if (canCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.flush();
    }

    private static void checkWellformedTable() throws IOException {
        int errorCount = 0;
        System.out.println("Checking for well-formedness");

        log.println("<h2>6. Checking for well-formedness</h2>");
        if (uca.haveVariableWarning) {
            log.println("<p><b>Ill-formed: alternate values overlap!</b></p>");
            errorCount++;
        }

        if (uca.haveZeroVariableWarning) {
            log.println("<p><b>Ill-formed: alternate values on zero primaries!</b></p>");
            errorCount++;
        }

        UCAContents cc = uca.getContents(Default.nfd());

        int minps = Integer.MAX_VALUE;
        int minpst = Integer.MAX_VALUE;
        String minpsSample = "", minpstSample = "";

        while (true) {
            String str = cc.next();
            if (str == null) {
                break;
            }
            CEList ces = cc.getCEs();
            int len = ces.length();

            for (int i = 0; i < len; ++i) {
                int ce = ces.at(i);
                int p = CEList.getPrimary(ce);
                int s = CEList.getSecondary(ce);
                int t = CEList.getTertiary(ce);

                // Gather data for WF#2 check

                if (p == 0) {
                    if (s > 0) {
                        if (s < minps) {
                            minps = s;
                            minpsSample = str;
                        }
                    } else {
                        if (t > 0 && t < minpst) {
                            minpst = t;
                            minpstSample = str;
                        }
                    }
                }
            }
        }

        cc = uca.getContents(Default.nfd());
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        int lastPrimary = 0;

        while (true) {
            String str = cc.next();
            if (str == null) {
                break;
            }
            CEList ces = cc.getCEs();
            int len = ces.length();

            for (int i = 0; i < len; ++i) {
                int ce = ces.at(i);
                int p = CEList.getPrimary(ce);
                int s = CEList.getSecondary(ce);
                int t = CEList.getTertiary(ce);

                // IF we are at the start of an implicit, then just check that
                // the implicit is in range
                // CHECK implicit
                if (UCA.isImplicitLeadPrimary(lastPrimary)) {
                    try {
                        if (s != 0 || t != 0) {
                            throw new Exception("Second implicit must be [X,0,0]");
                        }
                        UCA.ImplicitToCodePoint(lastPrimary, p); // throws
                        // exception
                        // if bad
                    } catch (Exception e) {
                        log.println("<tr><td>" + (++errorCount) + ". BAD IMPLICIT: " + e.getMessage()
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                    // zap the primary, since we worry about the last REAL
                    // primary:
                    lastPrimary = 0;
                    continue;
                }

                // IF we are in the trailing range, something is wrong.
                // UCA 6.3+ sets aside primary weights FFFD..FFFF as specials, so those are ok.
                // See http://www.unicode.org/draft/reports/tr10/tr10.html#Trailing_Weights
                if (UCA_Types.UNSUPPORTED_LIMIT <= p && p < 0xfffd) {
                    log.println("<tr><td>" + (++errorCount) + ". Unexpected trailing-weight primary"
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    lastPrimary = p;
                    continue;
                }

                // Check WF#1

                if (p != 0 && s == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.1"
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }
                if (s != 0 && t == 0) {
                    log.println("<tr><td>" + (++errorCount) + ". WF1.2"
                            + "</td><td>" + ces
                            + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                }

                // Check WF#2

                if (p != 0) {
                    if (s > minps) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.2"
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                }
                if (s != 0) {
                    if (t > minpst) {
                        log.println("<tr><td>" + (++errorCount) + ". WF2.3"
                                + "</td><td>" + ces
                                + "</td><td>" + Default.ucd().getCodeAndName(str) + "</td></tr>");
                    }
                } else {
                }

                lastPrimary = p;

            }
        }
        log.println("</table>");
        log.println("<p>Minimum Secondary in Primary Ignorable = " + Utility.hex(minps)
                + " from \t" + uca.getCEList(minpsSample, true)
                + "\t" + Default.ucd().getCodeAndName(minpsSample) + "</p>");
        if (minpst < Integer.MAX_VALUE) {
            log.println("<p>Minimum Tertiary in Secondary Ignorable =" + Utility.hex(minpst)
                    + " from \t" + uca.getCEList(minpstSample, true)
                    + "\t" + Default.ucd().getCodeAndName(minpstSample) + "</p>");
        }

        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(SERIOUS_ERROR);
        }
        log.flush();
    }

    private static final String  SERIOUS_ERROR          = "<p><b><font color='#FF0000'>SERIOUS_ERROR!</font></b></p>";
    private static final String  IMPORTANT_ERROR        = "<p><b><font color='#FF0000'>IMPORTANT_ERROR!</font></b></p>";

    private static final char    MARK1                  = '\u0001';

    private static TreeMap<String, String> MismatchedC = new TreeMap<String, String>();
    private static TreeMap<String, String> MismatchedN = new TreeMap<String, String>();
    private static TreeMap<String, String> MismatchedD = new TreeMap<String, String>();

    private static final byte    option                 = UCA.NON_IGNORABLE;                                            // SHIFTED

    private static void addString(String ch, byte option, CollatorType collatorType) {
        String colDbase = WriteCollationData.getCollator(collatorType).getSortKey(ch, option, true, AppendToCe.none);
        String colNbase = WriteCollationData.getCollator(collatorType).getSortKey(ch, option, false, AppendToCe.none);
        String colCbase = WriteCollationData.getCollator(collatorType).getSortKey(Default.nfc().normalize(ch), option, false, AppendToCe.none);
        if (!colNbase.equals(colCbase) || !colNbase.equals(colDbase)) {
            /*
             * System.out.println(Utility.hex(ch));
             * System.out.println(printableKey(colNbase));
             * System.out.println(printableKey(colNbase));
             * System.out.println(printableKey(colNbase));
             */
            MismatchedN.put(ch, colNbase);
            MismatchedC.put(ch, colCbase);
            MismatchedD.put(ch, colDbase);
        }
        String colD = colDbase + "\u0000" + ch; // UCA.NON_IGNORABLE
        String colN = colNbase + "\u0000" + ch;
        sortedD.put(colD, ch);
        backD.put(ch, colD);
        sortedN.put(colN, ch);
        backN.put(ch, colN);
        /*
         * if (strength > 4) { duplicateCount++; duplicates.put(ch+MARK1, col);
         * duplicates.put(ch+MARK2, col2); } else if (strength != 0) {
         * sorted.put(col2 + MARK2, ch); } unique += 2;
         */
    }

    private static void removeAdjacentDuplicates2() {
        int errorCount = 0;
        Iterator<String> it = sortedD.keySet().iterator();
        log.println("<h1>2. Differences in Ordering</h1>");
        log.println("<p>Codes and names are in the white rows: bold means that the NO-NFD sort key differs from UCA key.</p>");
        log.println("<p>Keys are in the light blue rows: green is the bad key, blue is UCA, black is where they equal.</p>");
        log.println("<p>Note: so black lines are generally ok.</p>");
        log.println("<table border='1' cellspacing='0' cellpadding='2'>");
        log.println("<tr><th>File Order</th><th>Code and Decomp</th><th>Key and Decomp-Key</th></tr>");

        String lastCol = "a";
        String lastColN = "a";
        String lastCh = "";
        boolean showedLast = true;
        int count = 0;
        while (it.hasNext()) {
            count++;
            String col = it.next();
            String ch = sortedD.get(col);
            String colN = backN.get(ch);
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
                    showLine(count - 1, lastCh, lastCol, lastColN);
                    errorCount++;
                }
                showedLast = true;
                showLine(count, ch, col, colN);
                errorCount++;
            }
            lastCol = col;
            lastColN = colN;
            lastCh = ch;
        }

        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        log.flush();
    }

    private static int compareMinusLast(String a, String b) {
        String am = a.substring(0, a.length() - 1);
        String bm = b.substring(0, b.length() - 1);
        int result = am.compareTo(bm);
        return (result < 0 ? -1 : result > 0 ? 1 : 0);
    }

    private static void showLine(int count, String ch, String keyD, String keyN) {
        String decomp = Default.nfd().normalize(ch);
        if (decomp.equals(ch)) {
            decomp = "";
        } else {
            decomp = "<br><" + Utility.hex(decomp, " ") + "> ";
        }
        log.println("<tr><td>" + count + "</td><td>"
                + Utility.hex(ch, " ")
                + " " + Default.ucd().getName(ch)
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

    private static final String[] alternateName = { "SHIFTED", "ZEROED", "NON_IGNORABLE", "SHIFTED_TRIMMED" };

    private static final ToolUnicodePropertySource propertySource = ToolUnicodePropertySource.make(null);
    private static final UnicodeProperty gc = propertySource.getProperty("gc");

    private static final UnicodeSet WHITESPACE = propertySource.getSet("whitespace=true")
            .freeze();

    private static final UnicodeSet IGNORABLE = new UnicodeSet()
    .addAll(gc.getSet("Cf"))
    .addAll(gc.getSet("Cc"))
    .addAll(gc.getSet("Mn"))
    .addAll(gc.getSet("Me"))
    .addAll(propertySource.getSet("Default_Ignorable_Code_Point=Yes"))
    .freeze();

    private static final UnicodeSet PUNCTUATION = new UnicodeSet()
    .addAll(gc.getSet("pc"))
    .addAll(gc.getSet("pd"))
    .addAll(gc.getSet("ps"))
    .addAll(gc.getSet("pe"))
    .addAll(gc.getSet("pi"))
    .addAll(gc.getSet("pf"))
    .addAll(gc.getSet("po"))
    .freeze();

    private static final UnicodeSet GENERAL_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sm"))
    .addAll(gc.getSet("Sk"))
    .addAll(gc.getSet("So"))
    .freeze();

    private static final UnicodeSet CURRENCY_SYMBOLS = new UnicodeSet()
    .addAll(gc.getSet("Sc"))
    .freeze();

    private static final UnicodeSet NUMBERS = new UnicodeSet()
    .addAll(gc.getSet("Nd"))
    .addAll(gc.getSet("No"))
    .addAll(gc.getSet("Nl"))
    .freeze();

    private static final UnicodeSet OTHERS = new UnicodeSet(0,0x10FFFF)
    .removeAll(IGNORABLE)
    .removeAll(WHITESPACE)
    .removeAll(PUNCTUATION)
    .removeAll(GENERAL_SYMBOLS)
    .removeAll(CURRENCY_SYMBOLS)
    .removeAll(NUMBERS)
    .removeAll(gc.getSet("Cn"))
    .removeAll(gc.getSet("Cs"))
    .removeAll(gc.getSet("Co"))
    .freeze();

    private static final UnicodeSet SPECIALS = new UnicodeSet(0xFFFD, 0xFFFD).freeze();

    private static final UnicodeMap<String> EMPTY = new UnicodeMap<String>().freeze();
    private static final UnicodeMap<String> IGNORABLE_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ࠤࠨःংঃਃઃଂଃఁ-ఃಂಃംഃංඃཿ းះៈᬄᮂ᳡ᳲ�ꢀꢁꦃ꯬�𑀀𑀂𑂂��𝅥𝅦𝅭-𝅲]"), "Unknown why these are ignored")
            .putAll(new UnicodeSet("[ـߺﱞ-ﱣﳲ-ﳴﹰ-ﹴﹶ-ﹿ]"), 
                    "Tatweel and related characters are ignorable; isolated vowels have screwy NFKD values")
                    .freeze();
    private static final UnicodeMap<String> GENERAL_SYMBOL_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[ៗ ː ˑ ॱ ๆ ໆ ᪧ ꧏ ꩰ ꫝ 々 〻 〱-〵 ゝ ゞ ーｰ ヽ ヾ \uAAF3 \uAAF4]"), "regular (not ignorable) symbols - significant modifiers")
            .putAll(new UnicodeSet("[৴-৹ ୲-୷ ꠰-꠵ ௰-௲ ൰-൵ ፲-፼ ↀ-ↂ ↆ-ↈ 𐹩-𐹾 ⳽ 𐌢 𐌣 𐄐-𐄳 𐅀 𐅁 𐅄-𐅇 𐅉-𐅎 𐅐-𐅗 𐅠-𐅲 𐅴-𐅸 𐏓-𐏕 𐩾 𐤗-𐤙 𐡛-𐡟 𐭜-𐭟 𐭼-𐭿 𑁛-𑁥 𐩄-𐩇 𒐲 𒐳 𒑖 𒑗 𒑚-𒑢 𝍩-𝍱]"), 
                    "Unknown why these are treated differently than numbers with scripts")
                    .freeze();
    private static final UnicodeMap<String> SCRIPT_REASONS = new UnicodeMap<String>()
            .putAll(new UnicodeSet("[⺀-⺙⺛-⺞⺠-⻲]"), "CJK radicals sort like they had toNFKD values")
            .putAll(new UnicodeSet("[ ᅟᅠㅤﾠ]"), "Hangul fillers are Default Ignorables, but sort as primaries")
            .putAll(gc.getSet("Mn"), "Indic (or Indic-like) non-spacing marks sort as primaries")
            .putAll(new UnicodeSet("[🅐-🅩🅰-🆏🆑-🆚🇦-🇿]"), "Characters that should have toNFKD values")
            .putAll(new UnicodeSet("[ᛮ-ᛰꛦ-ꛯ𐍁𐍊]"), "Unknown why these numbers are treated differently than numbers with symbols.")
            .freeze();

    private enum UcaBucket {
        ignorable('\u0000', IGNORABLE, IGNORABLE_REASONS),
        whitespace('\u0009', WHITESPACE, EMPTY), 
        punctuation('\u203E', PUNCTUATION, EMPTY), 
        general_symbols('\u0060', GENERAL_SYMBOLS, GENERAL_SYMBOL_REASONS),
        currency_symbols('\u00A4', CURRENCY_SYMBOLS, EMPTY),
        numbers('\u0030', NUMBERS, EMPTY), 
        scripts('a', OTHERS, SCRIPT_REASONS),
        specials('\uFFFD', SPECIALS, EMPTY),
        fail(-1, UnicodeSet.EMPTY, EMPTY);
        final int least;
        final UnicodeSet expected;
        final UnicodeSet exceptionsAllowed;
        final UnicodeMap<String> exceptionReasons;
        final UnicodeSet charOk = new UnicodeSet();
        final UnicodeSet bothOk = new UnicodeSet();
        final UnicodeSet nfkdOk = new UnicodeSet();
        final UnicodeSet warn = new UnicodeSet();
        final UnicodeSet failure = new UnicodeSet();

        UcaBucket(int least, UnicodeSet expected, UnicodeMap<String> toWarn) {
            this.least = least;
            this.expected = expected.freeze();
            this.exceptionsAllowed = toWarn.keySet().freeze();
            this.exceptionReasons = toWarn;
        }
        public UcaBucket next() {
            return values()[ordinal()+1];
        }

        public void add(int codepoint) {
            String nfkd = Default.nfkd().normalize(codepoint);
            int nfkdCh = nfkd.codePointAt(0);

            if (expected.contains(codepoint)) {
                if (expected.contains(nfkdCh)) {
                    bothOk.add(codepoint);
                } else {
                    charOk.add(codepoint);
                }
                return;
            }

            if (expected.contains(nfkdCh)) {
                nfkdOk.add(codepoint);
                return;
            }

            if (exceptionsAllowed.contains(codepoint)) {
                warn.add(codepoint);
                return;
            }

            failure.add(codepoint);
        }
    }

    private static void checkScripts() {
        log.println("<h2>0. Check UCA ‘Bucket’ Assignment</h2>");
        log.println("<p>Alternate Handling = " + alternateName[option] + "</p>");
        //        log.println("<table border='1'>");
        //log.println("<tr><th>Status</th><th>Type</th><th>GC</th><th>Script</th><th>Ch</th><th>Code</th><th>CE</th><th>Name</th></tr>");

        //        UnicodeSet ignore = new UnicodeSet()
        //        .addAll(propertySource.getSet("dt=none"))
        //        .complement()
        //        .freeze();
        //        
        //        UnicodeSet ignoreAboveA = new UnicodeSet()
        //        .addAll(propertySource.getSet("sc=Common"))
        //        .addAll(propertySource.getSet("sc=Inherited"))
        //        .complement()
        //        .freeze();
        //

        UnicodeSet mustSkip = new UnicodeSet()
        .addAll(gc.getSet("cn"))
        .addAll(gc.getSet("co"))
        .addAll(gc.getSet("cs"))
        .freeze();

        UnicodeSet shouldHave = new UnicodeSet(0,0x10FFFF)
        .removeAll(mustSkip)
        .removeAll(propertySource.getSet("Ideographic=true"))
        .removeAll(propertySource.getSet("Hangul_Syllable_Type=LVT"))
        .removeAll(propertySource.getSet("Hangul_Syllable_Type=LV"))
        .freeze();


        //UnicodeMap<UcaBucket> expectedOrder = new UnicodeMap();

        UnicodeSet lm = gc.getSet("Lm");

        UnicodeSet generalSymbols = new UnicodeSet()
        .addAll(gc.getSet("Sm"))
        .addAll(gc.getSet("Sk"))
        .addAll(gc.getSet("So"))
        .addAll(lm)
        .freeze();

        UnicodeSet currencySymbols = new UnicodeSet()
        .addAll(gc.getSet("Sc"))
        .freeze();

        UnicodeSet decimalNumber = new UnicodeSet()
        .addAll(gc.getSet("Nd"))
        .addAll(gc.getSet("No"))
        .addAll(gc.getSet("Nl"))
        .freeze();

        //        expectedOrder.putAll(WHITESPACE, UcaBucket.whitespace);
        //        expectedOrder.putAll(PUNCTUATION, UcaBucket.punctuation);
        //        expectedOrder.putAll(generalSymbols, UcaBucket.general_symbols);
        //        expectedOrder.putAll(currencySymbols, UcaBucket.currency_symbols);
        //        expectedOrder.putAll(decimalNumber, UcaBucket.numbers);

        UCAContents contents = uca.getContents(null);
        UnicodeSet covered = new UnicodeSet();
        UnicodeSet illegalCharacters = new UnicodeSet();
        int errorCount = 0;
        UcaBucket lastOrder = UcaBucket.whitespace;
        CEList lastCe = null;
        String lastString = "";
        String lastShown = "";

        TreeSet<Pair> sorted = new TreeSet<Pair>();
        String s;
        while ((s = contents.next()) != null) {
            sorted.add(new Pair(contents.getCEs(), s));
        }

        UnicodeSet funnyAboveA = new UnicodeSet();
        UnicodeSet ignorable = new UnicodeSet();
        UnicodeSet lmInSymbols = new UnicodeSet();
        UnicodeSet nInSymbols = new UnicodeSet();
        boolean aboveA = false;
        int count = 0;

        UcaBucket currentOrder = UcaBucket.ignorable;
        UcaBucket nextOrder = currentOrder.next();
        int currentOkCount = 0;

        for (Pair ceAndString : sorted) {
            final CEList ce = (CEList) ceAndString.first;
            final String string = (String)ceAndString.second;

            if (mustSkip.containsSome(string)) {
                illegalCharacters.addAll(string);
            }
            //            covered.addAll(string);
            //            if (ce.isPrimaryIgnorable()) {
            //                ignorable.add(string);
            //                continue;
            //            }

            //            boolean okOverride = false;
            // only look at isNFKD
            //            if (nfkdCh == '(' || nfkdCh == '〔' || nfkdCh == ':') {
            //                okOverride = true;
            //            }

            // skip multi-codepoint cases for now
            //            if (nfkd.codePointCount(0, nfkd.length()) > 1) {
            //                continue;
            //            }

            int ch = string.codePointAt(0);
            if (ch == nextOrder.least) {
                currentOrder = nextOrder;
                nextOrder = currentOrder.next();
            }
            currentOrder.add(ch);


            //            if (ignore.contains(ch)) {
            //                continue;
            //            }
            //            if (aboveA && ignoreAboveA.contains(ch)) {
            //                continue;
            //            }

            //            UcaBucket actualOrder = expectedOrder.get(ch);
            //            UcaBucket nfkdOrder = expectedOrder.get(nfkdCh);
            //            if (nfkdOrder == UcaBucket.numbers) {
            //                actualOrder = nfkdOrder;
            //            }
            //            if (actualOrder == null) {
            //                actualOrder = UcaBucket.scripts;
            //            }
            //            
            //            // add the actual value
            //            
            //            if (actualOrder.equals(currentOrder)) {
            //                actualOrder.add(ch);
            //                continue;
            //            }
            //            
            //            if (okOverride || actualOrder.equals(currentOrder)) {
            //                currentOkCount++;
            //                if (actualOrder == UcaBucket.general_symbols && lm.containsAll(string)) {
            //                    lmInSymbols.add(string);
            //                }
            //            } else if (currentOrder == UcaBucket.general_symbols && actualOrder==UcaBucket.numbers) {
            //                currentOkCount++;
            //                nInSymbols.add(string);
            //            } else {
            //                // above 'a' are just warnings.
            //                if (currentOrder == UcaBucket.scripts) {
            //                    funnyAboveA.add(string);
            //                    continue;
            //                }
            //
            //                errorCount++;
            //                String messageAttr = " class='bad'";
            //                String message = "(" + errorCount + ") expected " + currentOrder;
            //
            //                boolean newError = !lastShown.equals(lastString);
            //                // out of order, message
            //                if (newError) {
            //                    showTypeLine("", "OK Count: " + currentOkCount, lastOrder, lastString, lastCe);
            //                }
            //                showTypeLine(messageAttr, message, actualOrder, string, ce);
            //                lastShown = string;
            //                currentOkCount = 0;
            //            }
            //            lastOrder = actualOrder;
            //            lastString = string;
            //            lastCe = ce;
        }
        //        log.println("</table>");
        UnicodeSet missing = new UnicodeSet(shouldHave).removeAll(covered);
        illegalCharacters.retainAll(mustSkip);
        ignorable.removeAll(IGNORABLE);

        for (UcaBucket eo : UcaBucket.values()) {
            showCharSummary(eo + ": Ok char & 1st NFKD", eo.bothOk);
            showCharSummary(eo + ": Ok char, not 1st NFKD", eo.charOk);
            showCharSummary(eo + ": Ok 1st NFKD, not char", eo.nfkdOk);
            for (String value : eo.exceptionReasons.getAvailableValues()) {
                UnicodeSet filter = new UnicodeSet(eo.warn).retainAll(eo.exceptionReasons.getSet(value));
                showCharDetails(eo + ": WARNING - exceptions allowed «" + value + "»", filter, true);
            }
            errorCount += showCharDetails(eo + ": ERROR - unexpected characters", eo.failure, false);
        }
        log.println("<p>TODO: show missing</p>");

        //        showBadChars("N in general_symbols: not error, but check", nInSymbols);
        //        showBadChars("Lm in general_symbols: not error, but check", lmInSymbols);
        //        showBadChars("Unusual GC in ignorable: not error, but check", ignorable);
        //        showBadChars("Unusual GC above 'a': not error, but check", funnyAboveA);
        //        errorCount += showBadChars("Illegal character", illegalCharacters);
        //        errorCount += showBadChars("Missing CEs", missing);

        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.println("<br>");
        log.flush();
    }

    private static void showCharSummary(String title, UnicodeSet chars) {
        if (chars.size() == 0) return;
        log.println("<h3>" + title + ", Count: " + chars.size() + "</h3>");
        showCharSummaryTable(chars);
    }

    private static void showCharSummaryTable(UnicodeSet chars) {
        log.println("<table border='1'>");
        UnicodeMap<String> counter = new UnicodeMap<String>();
        for (String value : (List<String>) gc.getAvailableValues()) {
            List<String> aliases = (List<String>) gc.getValueAliases(value);
            String shortValue = aliases.get(0);
            UnicodeSet current = new UnicodeSet(chars).retainAll(gc.getSet(value));
            counter.putAll(current, shortValue);
        }
        TreeSet<String> sorted = new TreeSet<String>();
        sorted.addAll(counter.getAvailableValues());
        for (String value : sorted) {
            UnicodeSet set = counter.getSet(value);
            String pattern = set.toPattern(false).replace("\u0000", "\\u0000");
            int maxLength = 120;
            if (pattern.length() > maxLength) {
                // Do not truncate in the middle of a surrogate pair.
                if (Character.isHighSurrogate(pattern.charAt(maxLength - 1))) {
                    --maxLength;
                }
                pattern = pattern.substring(0, maxLength) + "…";
            }
            log.println("<tr>"
                    + "<td>" + value + "</td>"
                    + "<td>" + set.size() +  "</td>"
                    + "<td>" + pattern +  "</td>"
                    + "</tr>");
        }
        log.println("</table>");
    }

    private static int showCharDetails(String title, UnicodeSet illegalCharacters, boolean notError) {
        if (illegalCharacters.size() == 0) {
            return 0;
        }
        Map<String, UnicodeSet> sorted = new TreeMap<String, UnicodeSet>();

        for (String s: illegalCharacters) {
            final String type = getType(s);
            UnicodeSet us = sorted.get(type);
            if (us == null) {
                sorted.put(type, us = new UnicodeSet());
            }
            us.add(s);
        }
        String rtitle = "<span class='" + (notError ? "warn" : "bad") + "'>" + title + "<span>";
        System.out.println("***" + rtitle);
        log.println("<h3>" + rtitle + "; Count: " + illegalCharacters.size() + "</h3>");
        showCharSummaryTable(illegalCharacters);
        log.println("<p>Details:</p>");
        log.println("<table border='1'>");
        for (Entry<String, UnicodeSet> typeAndString : sorted.entrySet()) {
            UnicodeSet us = typeAndString.getValue();
            for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.nextRange();) {
                if (it.codepointEnd != it.codepoint) {
                    log.println("<tr>"
                            + "<td>" + typeAndString.getKey() + "</td>"
                            + "<td>" + UTF16.valueOf(it.codepoint) 
                            + "…" + UTF16.valueOf(it.codepointEnd) +  "</td>"
                            + "<td>" + Utility.hex(it.codepoint) 
                            + "…" + Utility.hex(it.codepointEnd) +  "</td>" 
                            + "<td>" + Default.ucd().getName(it.codepoint) 
                            + "…" + Default.ucd().getName(it.codepointEnd) + "</td>"
                            + "</tr>");
                } else {
                    log.println("<tr>"
                            + "<td>" + typeAndString.getKey() + "</td>"
                            + "<td>" + UTF16.valueOf(it.codepoint) + "</td>"
                            + "<td>" + Utility.hex(it.codepoint) + "</td>" 
                            + "<td>" + Default.ucd().getName(it.codepoint) + "</td>"
                            + "</tr>");
                }
            }
        }
        log.println("</table>");
        return illegalCharacters.size();
    }

    private static String getType(String s) {
        Set<String> set = getTypes(s, new LinkedHashSet<String>());

        String nfkd = Default.nfkd().normalize(s);
        Set<String> set2 = getTypes(nfkd, new LinkedHashSet<String>());
        set2.removeAll(set);

        String result = CollectionUtilities.join(set, "/");
        return set2.size() == 0 ? result : result + "(" + CollectionUtilities.join(set2, "/") + ")";
    }

    private static Set<String> getTypes(String s, Set<String> set) {
        for (int cp : With.codePointArray(s)) {
            set.add(Default.ucd().getCategoryID(cp));
        }
        for (int cp : With.codePointArray(s)) {
            set.add(Default.ucd().getScriptID(cp, UCD_Types.SHORT));
        }
        return set;
    }

    private static void showMismatches() {
        log.println("<h2>1. Mismatches when NFD is OFF</h2>");
        log.println("<p>Alternate Handling = " + alternateName[option] + "</p>");
        log.println("<p>NOTE: NFD form is used by UCA,"
                + "so if other forms are different there are <i>ignored</i>. This may indicate a problem, e.g. missing contraction.</p>");
        log.println("<table border='1'>");
        log.println("<tr><th>Name</th><th>Type</th><th>Unicode</th><th>Key</th></tr>");
        Iterator<String> it = MismatchedC.keySet().iterator();
        int errorCount = 0;
        while (it.hasNext()) {
            String ch = it.next();
            String MN = MismatchedN.get(ch);
            String MC = MismatchedC.get(ch);
            String MD = MismatchedD.get(ch);
            String chInC = Default.nfc().normalize(ch);
            String chInD = Default.nfd().normalize(ch);

            log.println("<tr><td rowSpan='3' class='bottom'>" + Utility.replace(Default.ucd().getName(ch), ", ", ",<br>")
                    + "</td><td>NFD</td><td>" + Utility.hex(chInD)
                    + "</td><td>" + printableKey(MD) + "</td></tr>");

            log.println("<tr><td>NFC</td><td>" + Utility.hex(chInC)
                    + "</td><td>" + printableKey(MC) + "</td></tr>");

            log.println("<tr><td class='bottom'>Plain</td><td class='bottom'>" + Utility.hex(ch)
                    + "</td><td class='bottom'>" + printableKey(MN) + "</td></tr>");

            errorCount++;
        }
        log.println("</table>");
        log.println("<p>Errors: " + errorCount + "</p>");
        if (errorCount != 0) {
            log.println(IMPORTANT_ERROR);
        }
        log.println("<br>");
        log.flush();
    }

    private static String printableKey(Object keyobj) {
        String sortKey;
        if (keyobj == null) {
            sortKey = "NULL!!";
        } else {
            sortKey = keyobj.toString();
            sortKey = sortKey.substring(0, sortKey.length() - 1);
            sortKey = UCA.toString(sortKey);
        }
        return sortKey;
    }
}
