/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/Main.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCA;

import org.unicode.text.UCA.UCA.CollatorType;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

public class Main {
    static final String[] ICU_FILES = {
        "WriteAllKeys",
        "writeFractionalUCA",
        "writeCollationValidityLog",
        "WriteAllKeysDucet",
        "WriteRules",
        "WriteRulesCLDR",
        "writeconformance",
        "writeConformanceShifted",
        "writeConformanceCldr",
        "writeConformanceShiftedCldr",
        "noCE",
        "WriteRules",
        "WriteRulesCLDR",
    };

    public static void main(String args[]) throws Exception {
        System.setProperty("line.separator", "\n");

        // NOTE: so far, we don't need to build the UCA with anything but the latest versions.
        // A few changes would need to be made to the code to do older versions.

        if (args.length == 0) {
            args = new String[] {"?"}; // force the help comment
        }
        boolean noCE = false;

        for (int i = 0; i < args.length; ++i) {
            final String arg = args[i];
            System.out.println("OPTION: " + arg);
            if (arg.charAt(0) == '#') {
                return; // skip rest of line
            }

            if (arg.equalsIgnoreCase("ICU")) {
                args = Utility.append(ICU_FILES, Utility.subarray(args, i + 1));
                i = -1;
                continue;
            }
            if (arg.equalsIgnoreCase("version")) {
                Default.setUCD(args[++i]); // get next arg
                continue;
            }
            if (arg.equalsIgnoreCase("WriteRules")) {
                WriteCollationData.writeRules(false /*shortPrint*/, noCE, CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("WriteRulesCLDR")) {
                WriteCollationData.writeRules(false /*shortPrint*/, noCE, CollatorType.cldr);
                if (!noCE) {
                    // short omits CEs anyway
                    WriteCollationData.writeRules(true, noCE, CollatorType.cldr);
                }
            } else if (arg.equalsIgnoreCase("writeFractionalUCA")) {
                FractionalUCA.writeFractionalUCA();
            } else if (arg.equalsIgnoreCase("WriteAllKeys")) {
                WriteAllkeys.writeAllkeys("allkeys", CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("WriteAllKeysDucet")) {
                WriteAllkeys.writeAllkeys("allkeys_DUCET", CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformance")) {
                WriteConformanceTest.writeConformance(
                        "CollationTest_NON_IGNORABLE",
                        UCA_Types.Alternate.NON_IGNORABLE,
                        false /*shortPrint*/,
                        CollatorType.ducet);
                WriteConformanceTest.writeConformance(
                        "CollationTest_NON_IGNORABLE",
                        UCA_Types.Alternate.NON_IGNORABLE,
                        true,
                        CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformanceShifted")) {
                WriteConformanceTest.writeConformance(
                        "CollationTest_SHIFTED",
                        UCA_Types.Alternate.SHIFTED,
                        false /*shortPrint*/,
                        CollatorType.ducet);
                WriteConformanceTest.writeConformance(
                        "CollationTest_SHIFTED",
                        UCA_Types.Alternate.SHIFTED,
                        true,
                        CollatorType.ducet);
            } else if (arg.equalsIgnoreCase("writeConformanceCldr")) {
                WriteConformanceTest.writeConformance(
                        "CollationTest_NON_IGNORABLE",
                        UCA_Types.Alternate.NON_IGNORABLE,
                        false /*shortPrint*/,
                        CollatorType.cldr);
                WriteConformanceTest.writeConformance(
                        "CollationTest_NON_IGNORABLE",
                        UCA_Types.Alternate.NON_IGNORABLE,
                        true,
                        CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("writeConformanceShiftedCldr")) {
                WriteConformanceTest.writeConformance(
                        "CollationTest_SHIFTED",
                        UCA_Types.Alternate.SHIFTED,
                        false /*shortPrint*/,
                        CollatorType.cldr);
                WriteConformanceTest.writeConformance(
                        "CollationTest_SHIFTED",
                        UCA_Types.Alternate.SHIFTED,
                        true,
                        CollatorType.cldr);
            } else if (arg.equalsIgnoreCase("writeCollationValidityLog")) {
                Validity.writeCollationValidityLog();
            } else if (arg.equalsIgnoreCase("noCE")) {
                noCE = !noCE;
            } else {
                System.out.println();
                System.out.println(
                        "UNKNOWN OPTION ("
                                + arg
                                + "): must be one of the following (case-insensitive)");
                System.out.println("\tWriteRulesWithNames, WriteRules, WriteRulesCLDR,");
                System.out.println(
                        "\twriteFractionalUCA, writeConformance, writeConformanceShifted,");
                System.out.println("\twriteCollationValidityLog,");
                System.out.println("\thex (used for conformance)");
            }
        }

        System.out.println("Done");
    }
}
