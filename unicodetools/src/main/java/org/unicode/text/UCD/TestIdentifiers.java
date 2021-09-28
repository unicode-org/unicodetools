package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.XEquivalenceClass;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestIdentifiers {

    public static void main(String[] args) throws IOException {
        final String[] tests = { "SØS", "façade", "MOPE", "VOP", "scope", "ibm", "vop",
                "toys-я-us", "1iνе", "back", "boгing" };

        final TestIdentifiers ti = new TestIdentifiers("L");
        final TestIdentifiers tiany = new TestIdentifiers("A");
        ti.loadIdentifiers();
        final UnicodeSet idnCharSet = ti.idnChars.keySet("output", new UnicodeSet());
        System.out.println("idnCharSet: " + idnCharSet.size());
        final UnicodeSet idnCharNonStarting = ti.nonstarting;
        System.out.println("idnCharNonStarting: " + idnCharSet);
        if (true) {
            return;
        }

        for (final String test : tests) {
            System.out.print(test);
            final String folded = UCharacter.foldCase(test, true);
            if (folded.equals(test)) {
                ti.testItem(test);
            } else {
                System.out.print("\t");
                tiany.testItem(test);
                System.out.print(folded);
                ti.testItem(folded);
            }
            for (int j = 0; j < test.length(); ++j) {
                final int cp = test.charAt(j);
                final Set s = ti.getConfusables(cp, "MA");
                System.out.println(Default.ucd().getCodeAndName(cp));
                for (final Iterator it = s.iterator(); it.hasNext();) {
                    System.out.println("\t= " + Default.ucd().getCodeAndName((String)it.next()));
                }
            }
        }
    }

    void testItem(String test) {
        test = Normalizer.normalize(test, Normalizer.DECOMP_COMPAT);
        final BitSet scripts = new BitSet();
        System.out.print("\t" + caseType + "\t");
        boolean foundProblem = false;
        if (hasWholeScriptConfusable(test, scripts)) {
            System.out.print("whole-script confusables: ");
            for (int j = 0; j < scripts.length(); ++j) {
                if (scripts.get(j)) {
                    System.out.print(UScript.getName(j) + " ");
                }
            }
            System.out.println();
            foundProblem = true;
        }
        if (hasMixedScriptConfusable(test)) {
            System.out.println("mixed-script confusable");
            foundProblem = true;
        }
        if (!foundProblem) {
            System.out.println("no confusable");
        }
    }

    private static final String indir = "C:\\Unicode-CVS2\\draft\\reports\\tr36\\data/";

    private static UnicodeSet commonAndInherited = new UnicodeSet(
            "[[:script=common:][:script=inherited:]]");

    private static UnicodeSet XIDContinueSet = new UnicodeSet("[:xidcontinue:]")
    .add('-');

    private static final boolean DEBUG = false;
    private final String caseType;

    TestIdentifiers(String caseType) throws IOException {
        this.caseType = caseType;
        loadWholeScriptConfusables(caseType);
    }

    private static class UnicodeSetToScript {
        public int getScript() {
            return script;
        }

        public UnicodeSetToScript setScript(int script) {
            this.script = script;
            return this;
        }

        public UnicodeSet getSet() {
            return set;
        }

        public UnicodeSetToScript setSet(UnicodeSet set) {
            this.set = set;
            return this;
        }

        private UnicodeSet set;

        private int script;
    }

    UnicodeSetToScript[][] scriptToUnicodeSetToScript = new UnicodeSetToScript[UScript.CODE_LIMIT][];
    UnicodeSet[] fastReject = new UnicodeSet[UScript.CODE_LIMIT];

    UnicodeMap idnChars = new UnicodeMap();
    UnicodeSet nonstarting = new UnicodeSet();

    void loadIdentifiers() throws IOException {
        final BufferedReader br = FileUtilities.openUTF8Reader(indir, "idnchars.txt");
        String line = null;
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                final String[] pieces = Utility.split(line, ';');
                // part 0 is range
                final String range = pieces[0].trim();
                final int rangeDivider = range.indexOf("..");
                int start, end;
                if (rangeDivider < 0) {
                    start = end = Integer.parseInt(range, 16);
                } else {
                    start = Integer.parseInt(range.substring(0, rangeDivider),
                            16);
                    end = Integer.parseInt(range.substring(rangeDivider + 2),
                            16);
                }
                // part 1 is script1
                final String type = pieces[1].trim().intern();
                if (type.equals("nonstarting")) {
                    nonstarting.add(start,end);
                } else {
                    idnChars.putAll(start, end, type);
                }
            }
        } catch (final Exception e) {
            throw (RuntimeException) new RuntimeException("Failure on line "
                    + line).initCause(e);
        }
        br.close();
    }

    Map type_equivalences;

    void loadConfusables() throws IOException {
        final BufferedReader br = FileUtilities.openUTF8Reader(indir, "confusables.txt");
        String line = null;
        type_equivalences = new HashMap();
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                final String[] pieces = Utility.split(line, ';');
                // part 0 is source code point
                final String s = Utility.fromHex(pieces[0].trim());
                // part 1 is script1
                final String t = Utility.fromHex(pieces[1].trim());

                final String type = pieces[2].trim();
                XEquivalenceClass ec = (XEquivalenceClass) type_equivalences.get(type);
                if (ec == null) {
                    type_equivalences.put(type, ec = new XEquivalenceClass(""));
                }
                ec.add(s, t);
                //System.out.println(type + ": " + Default.ucd().getCodeAndName(s) + " => " + Default.ucd().getCodeAndName(t));
            }
        } catch (final Exception e) {
            throw (RuntimeException) new RuntimeException("Failure on line "
                    + line).initCause(e);
        }
        br.close();
    }

    public Set getConfusables(int cp, String type) {
        try {
            if (type_equivalences == null) {
                loadConfusables();
            }
        } catch (final IOException e) {
            return null;
        }
        final XEquivalenceClass ec = (XEquivalenceClass) type_equivalences.get(type);
        return ec.getEquivalences(UTF16.valueOf(cp));
    }

    void loadWholeScriptConfusables(String filterType) throws IOException {
        final UnicodeSet[][] script_script_set = new UnicodeSet[UScript.CODE_LIMIT][UScript.CODE_LIMIT];
        for (int i = 0; i < UScript.CODE_LIMIT; ++i) {
            script_script_set[i] = new UnicodeSet[UScript.CODE_LIMIT];
        }
        final BufferedReader br = FileUtilities.openUTF8Reader(indir, "confusablesWholeScript.txt");
        String line = null;
        try {
            while (true) {
                line = Utility.readDataLine(br);
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                final String[] pieces = Utility.split(line, ';');
                // part 0 is range
                final String range = pieces[0].trim();
                final int rangeDivider = range.indexOf("..");
                int start, end;
                if (rangeDivider < 0) {
                    start = end = Integer.parseInt(range, 16);
                } else {
                    start = Integer.parseInt(range.substring(0, rangeDivider),
                            16);
                    end = Integer.parseInt(range.substring(rangeDivider + 2),
                            16);
                }
                // part 1 is script1
                final int script1 = UScript.getCodeFromName(pieces[1].trim());
                // part 2 is script2
                final int script2 = UScript.getCodeFromName(pieces[2].trim());
                final String type = pieces[3].trim();
                if (!type.equals(filterType)) {
                    continue;
                }
                if (script_script_set[script1][script2] == null) {
                    script_script_set[script1][script2] = new UnicodeSet();
                }
                script_script_set[script1][script2].add(start, end);
            }
            for (int i = 0; i < script_script_set.length; ++i) {
                final UnicodeSet accept = new UnicodeSet();
                final List curr = new ArrayList();
                for (int j = 0; j < script_script_set[i].length; ++j) {
                    if (script_script_set[i][j] == null) {
                        continue;
                    }
                    accept.addAll(script_script_set[i][j]);
                    curr.add(new UnicodeSetToScript().setScript(j).setSet(
                            script_script_set[i][j]));
                    if (DEBUG && i == UScript.LATIN) {
                        System.out.println(UScript.getName(i) + "; "
                                + UScript.getName(j) + "; "
                                + script_script_set[i][j]);
                    }
                }
                if (curr.size() == 0) {
                    continue;
                }
                scriptToUnicodeSetToScript[i] = (UnicodeSetToScript[]) curr
                        .toArray(new UnicodeSetToScript[curr.size()]);
                fastReject[i] = accept.complement();
                if (DEBUG && i == UScript.LATIN) {
                    System.out.println(UScript.getName(i) + "; "
                            + fastReject[i]);
                }
            }
        } catch (final Exception e) {
            throw (RuntimeException) new RuntimeException("Failure on line "
                    + line).initCause(e);
        }
        br.close();
    }

    /*
     * for this routine, we don't care what the targetScripts are, just whether
     * there is at least one whole-script confusable.
     */
    boolean hasWholeScriptConfusable(String givenString, BitSet resultingScripts) {
        final int givenScript = getSingleScript(givenString);
        if (givenScript == UScript.INVALID_CODE) {
            return false;
        }
        final UnicodeSet givenSet = new UnicodeSet().addAll(givenString).removeAll(
                commonAndInherited);
        return hasWholeScriptConfusable(givenScript, givenSet, resultingScripts);
    }

    /**
     * 
     */
    private boolean hasWholeScriptConfusable(int givenScript,
            UnicodeSet givenSet, BitSet resultingScripts) {
        resultingScripts.clear();
        if (fastReject[givenScript] == null) {
            return false;
        }
        if (fastReject[givenScript].containsSome(givenSet)) {
            return false;
        }
        final UnicodeSetToScript[] possibles = scriptToUnicodeSetToScript[givenScript];
        for (final UnicodeSetToScript possible : possibles) {
            if (possible.set.containsAll(givenSet)) {
                resultingScripts.set(possible.script);
            }
        }
        return !resultingScripts.isEmpty();
    }

    /*
     * for this routine, we don't care what the targetScripts are, just
     * whether there is at least one whole-script confusable.
     */
    boolean hasMixedScriptConfusable(String givenString) {
        final UnicodeSet givenSet = new UnicodeSet().addAll(givenString).removeAll(
                commonAndInherited);
        final UnicodeSet[] byScript = getScripts(givenSet);
        final BitSet wholeScripts = new BitSet();
        boolean result = false;
        main: for (int i = 0; i < byScript.length; ++i) {
            if (byScript[i] == null) {
                continue;
            }
            // see if the other characters have whole script confusables in
            // my script
            for (int j = 0; j < byScript.length; ++j) {
                if (j == i || byScript[j] == null) {
                    continue;
                }
                if (!hasWholeScriptConfusable(j, byScript[j], wholeScripts)) {
                    continue main;
                }
                if (!wholeScripts.get(i))
                {
                    continue main; // doesn't have the
                }
                // one we want
                result = true;
            }
            return result; // passed the guantlet
        }
        return false;
    }

    /*
     * Returns UScript.INVALID_CODE if mixed script, otherwise the script
     */
    public static int getSingleScript(String source) {
        int lastScript = UScript.INVALID_CODE;
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            int script = UScript.getScript(cp);
            if (script == UScript.COMMON || script == UScript.INHERITED) {
                if (XIDContinueSet.contains(cp)) {
                    if (lastScript == UScript.INVALID_CODE) {
                        lastScript = script;
                    }
                    continue; // skip if not identifier
                }
                script = UScript.COMMON;
            }
            if (lastScript == UScript.INVALID_CODE) {
                lastScript = script;
            } else if (script != lastScript) {
                return UScript.INVALID_CODE;
            }
        }
        return lastScript;
    }

    public static UnicodeSet[] getScripts(UnicodeSet sourceSet) {
        final UnicodeSet[] byScript = new UnicodeSet[UScript.CODE_LIMIT];
        for (final UnicodeSetIterator usi = new UnicodeSetIterator(sourceSet); usi
                .next();) {
            final int script = UScript.getScript(usi.codepoint);
            if (byScript[script] == null) {
                byScript[script] = new UnicodeSet();
            }
            byScript[script].add(usi.codepoint);
        }
        return byScript;
    }

}