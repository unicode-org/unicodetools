/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/Main.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;
import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.NFSkippable;
import org.unicode.text.utility.Utility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterEnums.ECharacterCategory;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;


public class Main {
    static final String[] ICU_FILES = {
        "WriteAllKeys", "writeFractionalUCA", "writeCollationValidityLog", "WriteAllKeysDucet",
        "WriteRules", "WriteRulesCLDR",  // "WriteRulesXML", "WriteRulesCLDRXML",
        "writeconformance", "writeConformanceShifted", "writeConformanceCldr", "writeConformanceShiftedCldr",
        // "short",
        // "WriteRules", "WriteRulesCLDR",  // "WriteRulesXML", "WriteRulesCLDRXML",
        // "writeconformance", "writeConformanceShifted", "writeConformanceCldr", "writeConformanceShiftedCldr",
        "noCE",  // "short",
        "WriteRules", "WriteRulesCLDR",  // "WriteRulesXML", "WriteRulesCLDRXML"
    };

    static final String[] CHART_FILES = {
        "collationChart",
        "scriptChart",
        "normalizationChart",
        "caseChart",
//        "indexChart",
        "nameslistChart"
    };

    public static void main(String args[]) throws Exception {
        System.setProperty("line.separator", "\n");

        // NOTE: so far, we don't need to build the UCA with anything but the latest versions.
        // A few changes would need to be made to the code to do older versions.

        if (args.length == 0)
        {
            args = new String[] {"?"}; // force the help comment
        }
        boolean shortPrint = false;
        boolean noCE = false;

        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];
            System.out.println("OPTION: " + arg);
            if (arg.charAt(0) == '#')
            {
                return; // skip rest of line
            }

            if (arg.equalsIgnoreCase("ICU")) {
                args = Utility.append(ICU_FILES, Utility.subarray(args, i+1));
                i = -1;
                continue;
            }
            if (arg.equalsIgnoreCase("charts")) {
                args = Utility.append(CHART_FILES, Utility.subarray(args, i+1));
                i = -1;
                continue;
            }
            if (arg.equalsIgnoreCase("version")) {
                Default.setUCD(args[++i]); // get next arg
                continue;
            }
            if (arg.equalsIgnoreCase("GenOverlap")) {
                GenOverlap.test(WriteCollationData.getCollator(CollatorType.ducet));
            } else if (arg.equalsIgnoreCase("NFSkippable")) {
                NFSkippable.main(null);
            } else if (arg.equalsIgnoreCase("validateUCA")) {
                GenOverlap.validateUCA(WriteCollationData.getCollator(CollatorType.ducet));
                //else if (arg.equalsIgnoreCase("writeNonspacingDifference")) WriteCollationData.writeNonspacingDifference();
            } else if (arg.equalsIgnoreCase("collationChart")) {
                WriteCharts.collationChart(WriteCollationData.getCollator(CollatorType.ducet));
            } else if (arg.equalsIgnoreCase("scriptChart")) {
                WriteCharts.scriptChart();
            } else if (arg.equalsIgnoreCase("normalizationChart")) {
                WriteCharts.normalizationChart();
            } else if (arg.equalsIgnoreCase("caseChart")) {
                WriteCharts.caseChart();
//            } else if (arg.equalsIgnoreCase("indexChart")) {
//                WriteCharts.indexChart();
            } else if (arg.equalsIgnoreCase("nameslistChart")) {
                MakeNamesChart.main(null);
            } else if (arg.equalsIgnoreCase("special")) {
                WriteCharts.special();
            } else if (arg.equalsIgnoreCase("writeCompositionChart")) {
                WriteCharts.writeCompositionChart();
            } else if (arg.equalsIgnoreCase("CheckHash")) {
                GenOverlap.checkHash(WriteCollationData.getCollator(CollatorType.ducet));
            } else if (arg.equalsIgnoreCase("generateRevision")) {
                GenOverlap.generateRevision(WriteCollationData.getCollator(CollatorType.ducet));
            } else if (arg.equalsIgnoreCase("listCyrillic")) {
                GenOverlap.listCyrillic(WriteCollationData.getCollator(CollatorType.ducet));
            } else if (arg.equalsIgnoreCase("WriteRules")) {
                WriteCollationData.writeRules(WriteCollationData.WITHOUT_NAMES, shortPrint, noCE, CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("WriteRulesCLDR")) {
                WriteCollationData.writeRules(WriteCollationData.WITHOUT_NAMES, false /*shortPrint*/, noCE, CollatorType.cldr);
                if (!noCE) {
                    // short omits CEs anyway
                    WriteCollationData.writeRules(WriteCollationData.WITHOUT_NAMES, true, noCE, CollatorType.cldr);
                }
            } else if (arg.equalsIgnoreCase("WriteRulesXML")) {
                WriteCollationData.writeRules(WriteCollationData.IN_XML, shortPrint, noCE, CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("WriteRulesCLDRXML")) {
                WriteCollationData.writeRules(WriteCollationData.IN_XML, shortPrint, noCE, CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("checkDisjointIgnorables")) {
                WriteCollationData.checkDisjointIgnorables();
            } else if (arg.equalsIgnoreCase("writeContractions")) {
                WriteCollationData.writeContractions();
            } else if (arg.equalsIgnoreCase("writeFractionalUCA")) {
                FractionalUCA.writeFractionalUCA("FractionalUCA");
            } else if (arg.equalsIgnoreCase("WriteAllKeys")) {
                WriteAllkeys.writeAllkeys("allkeys", CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("WriteAllKeysDucet")) {
                WriteAllkeys.writeAllkeys("allkeys_DUCET", CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformance")) {
                WriteConformanceTest.writeConformance("CollationTest_NON_IGNORABLE", UCA_Types.NON_IGNORABLE, false /*shortPrint*/, CollatorType.ducet);
                WriteConformanceTest.writeConformance("CollationTest_NON_IGNORABLE", UCA_Types.NON_IGNORABLE, true, CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformanceShifted")) {
                WriteConformanceTest.writeConformance("CollationTest_SHIFTED", UCA_Types.SHIFTED, false /*shortPrint*/, CollatorType.ducet);
                WriteConformanceTest.writeConformance("CollationTest_SHIFTED", UCA_Types.SHIFTED, true, CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformanceCldr")) {
                WriteConformanceTest.writeConformance("CollationTest_NON_IGNORABLE", UCA_Types.NON_IGNORABLE, false /*shortPrint*/, CollatorType.cldr);
                WriteConformanceTest.writeConformance("CollationTest_NON_IGNORABLE", UCA_Types.NON_IGNORABLE, true, CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("writeConformanceShiftedCldr")) {
                WriteConformanceTest.writeConformance("CollationTest_SHIFTED", UCA_Types.SHIFTED, false /*shortPrint*/, CollatorType.cldr);
                WriteConformanceTest.writeConformance("CollationTest_SHIFTED", UCA_Types.SHIFTED, true, CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("testCompatibilityCharacters")) {
                TestCompatibilityCharacters.testCompatibilityCharacters(WriteCollationData.getCollator(CollatorType.cldrWithoutFFFx));
            } else if (arg.equalsIgnoreCase("writeCollationValidityLog")) {
                Validity.writeCollationValidityLog();
            } else if (arg.equalsIgnoreCase("writeJavascriptInfo")) {
                WriteCollationData.writeJavascriptInfo();
            } else if (arg.equalsIgnoreCase("writeCaseFolding")) {
                WriteCollationData.writeCaseFolding();
            } else if (arg.equalsIgnoreCase("javatest")) {
                WriteCollationData.javatest();
            } else if (arg.equalsIgnoreCase("short")) {
                shortPrint = !shortPrint;
            } else if (arg.equalsIgnoreCase("noCE")) {
                noCE = !noCE;
            } else if (arg.equalsIgnoreCase("checkCanonicalIterator")) {
                checkCanonicalIterator();
            } else if (arg.equalsIgnoreCase("writeAllocation")) {
                WriteCharts.writeAllocation();
                // else if (arg.equalsIgnoreCase("probe")) Probe.test();
            } else {
                System.out.println();
                System.out.println("UNKNOWN OPTION (" + arg + "): must be one of the following (case-insensitive)");
                System.out.println("\tWriteRulesXML, WriteRulesWithNames, WriteRules, WriteRulesCLDR,");
                System.out.println("\tcheckDisjointIgnorables, writeContractions,");
                System.out.println("\twriteFractionalUCA, writeConformance, writeConformanceShifted, testCompatibilityCharacters,");
                System.out.println("\twriteCollationValidityLog, writeCaseExceptions, writeJavascriptInfo, writeCaseFolding");
                System.out.println("\tjavatest, hex (used for conformance)");
            }
        }

        System.out.println("Done");

        /*
        String s = WriteCollationData.collator.getSortKey("\u1025\u102E", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0595\u0325") + ", " + WriteCollationData.collator.toString(s));
        String t = WriteCollationData.collator.getSortKey("\u0596\u0325", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0596\u0325") + ", " + WriteCollationData.collator.toString(t));


        Normalizer foo = new Normalizer(Normalizer.NFKD);
        char x = '\u1EE2';
        System.out.println(Utility.hex(x) + " " + ucd.getName(x));
        String nx = foo.normalize(x);
        for (int i = 0; i < nx.length(); ++i) {
            char c = nx.charAt(i);
            System.out.println(ucd.getCanonicalClass(c));
        }
        System.out.println(Utility.hex(nx, " ") + " " + ucd.getName(nx));
         */
    }

    /**
     * 
     */
    private static void checkCanonicalIterator() {
        /* We do not compute fractional implicit weights any more.
        int firstImplicit = FractionalUCA.getImplicitPrimary(UCD_Types.CJK_BASE);
        System.out.println("UCD_Types.CJK_BASE: " + Utility.hex(UCD_Types.CJK_BASE));
        System.out.println("first implicit: " + Utility.hex((long)(firstImplicit & 0xFFFFFFFFL)));
         */

        final CanonicalIterator it = new CanonicalIterator("");
        final String[] tests = new String[] {"\uF900", "\u00C5d\u0307\u0327"};
        for (final String test : tests) {
            System.out.println(Default.ucd().getCodeAndName(test));
            it.setSource(test);
            String ss;
            for (int i = 0; (ss = it.next()) != null; ++i) {
                System.out.println(i + "\t" + Default.ucd().getCodeAndName(ss));
            }
        }
        // verify that nothing breaks
        for (int i = 0; i < 0x10FFFF; ++i) {
            final int cat = UCharacter.getType(i);
            if (cat == ECharacterCategory.UNASSIGNED || cat == ECharacterCategory.PRIVATE_USE || cat == ECharacterCategory.SURROGATE) {
                continue;
            }
            final String s = UTF16.valueOf(i);
            try {
                it.setSource(s);
            } catch (final RuntimeException e) {
                System.out.println("Failure with U+" + Utility.hex(i));
                e.printStackTrace();
            }
        }
    }
}