/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/Main.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import org.unicode.text.utility.DirectoryIterator;
import org.unicode.text.utility.FastBinarySearch;
import org.unicode.text.utility.SampleEnum;
import org.unicode.text.utility.Utility;

public final class Main {

    static final String[] CORE_FILES = {
        "CaseFolding",
        "CompositionExclusions",
        "DerivedCoreProperties",
        "DerivedNormalizationProps",
        "NormalizationTest",
        "PropertyAliases",
        "PropList",
        "Scripts",
        "SpecialCasing",
        "HangulSyllableType",
        "DerivedAge",
        "StandardizedVariants",
        "HangulSyllableType",
        // "OtherDerivedProperties",
    };

    static final String[] EXTRACTED_FILES = {
        "DerivedBidiClass",
        "DerivedBinaryProperties",
        "DerivedCombiningClass",
        "DerivedDecompositionType",
        "DerivedEastAsianWidth",
        "DerivedGeneralCategory",
        "DerivedJoiningGroup",
        "DerivedJoiningType",
        "DerivedLineBreak",
        "DerivedNumericType",
        "DerivedNumericValues",
    };

    static final String[] ALL_FILES = {"Core", "Extracted"};

    public static void main(String[] args) throws Exception {
        System.out.println("*** Start *** " + Default.getDate());

        try {
            for (int i = 0; i < args.length; ++i) {
                final String arg = args[i];
                if (arg.charAt(0) == '#') {
                    return; // skip rest of line
                }

                Utility.fixDot();
                System.out.println();
                System.out.println("** Argument: " + args[i] + " ** " + Default.getDate());

                // Expand string arguments

                if (arg.equalsIgnoreCase("ALL")) {
                    args = Utility.append(ALL_FILES, Utility.subarray(args, i + 1));
                    i = -1;
                    continue;
                }

                if (arg.equalsIgnoreCase("CORE")) {
                    args = Utility.append(CORE_FILES, Utility.subarray(args, i + 1));
                    i = -1;
                    continue;
                }

                if (arg.equalsIgnoreCase("EXTRACTED")) {
                    args = Utility.append(EXTRACTED_FILES, Utility.subarray(args, i + 1));
                    i = -1;
                    continue;
                }

                // make sure the UCD is set up

                if (arg.equalsIgnoreCase("version")) {
                    Default.setUCD(args[++i]);
                    continue;
                }

                // Now handle other options

                if (arg.equalsIgnoreCase("verify")) {
                    VerifyUCD.verify();
                    VerifyUCD.checkCanonicalProperties();
                    VerifyUCD.CheckCaseFold();
                    VerifyUCD.checkAgainstUInfo();
                } else if (arg.equalsIgnoreCase("build")) {
                    ConvertUCD.main(new String[] {Default.ucdVersion()});
                } else if (arg.equalsIgnoreCase("statistics")) {
                    VerifyUCD.statistics();
                } else if (arg.equalsIgnoreCase("diffIgnorable")) {
                    VerifyUCD.diffIgnorable();
                } else if (arg.equalsIgnoreCase("generateXML")) {
                    VerifyUCD.generateXML();
                } else if (arg.equalsIgnoreCase("checkSpeed")) {
                    VerifyUCD.checkSpeed();
                } else if (arg.equalsIgnoreCase("onetime")) {
                    VerifyUCD.oneTime();
                } else if (arg.equalsIgnoreCase("verifyNormalizationStability")) {
                    VerifyUCD.verifyNormalizationStability();
                } else if (arg.equalsIgnoreCase("definitionTransliterator")) {
                    GenerateHanTransliterator.main(0);
                } else if (arg.equalsIgnoreCase("romajiTransliterator")) {
                    GenerateHanTransliterator.main(1);
                } else if (arg.equalsIgnoreCase("pinYinTransliterator")) {
                    GenerateHanTransliterator.main(2);
                } else if (arg.equalsIgnoreCase("hanproperties")) {
                    GenerateHanTransliterator.readUnihan();
                } else if (arg.equalsIgnoreCase("fixChineseOverrides")) {
                    GenerateHanTransliterator.fixChineseOverrides();
                } else if (arg.equalsIgnoreCase("compareBlueberry")) {
                    VerifyUCD.compareBlueberry();
                } else if (arg.equalsIgnoreCase("testenum")) {
                    SampleEnum.test();
                } else if (arg.equalsIgnoreCase("TernaryStore")) {
                    TernaryStore.test();
                } else if (arg.equalsIgnoreCase("checkBIDI")) {
                    VerifyUCD.checkBIDI();
                } else if (arg.equalsIgnoreCase("Buildnames")) {
                    BuildNames.main(null);
                } else if (arg.equalsIgnoreCase("TestNormalization")) {
                    TestNormalization.main(null);
                } else if (arg.equalsIgnoreCase("binary")) {
                    FastBinarySearch.test();
                } else if (arg.equalsIgnoreCase("GenerateCaseTest")) {
                    GenerateCaseTest.main(null);
                } else if (arg.equalsIgnoreCase("checkDecompFolding")) {
                    VerifyUCD.checkDecompFolding();
                } else if (arg.equalsIgnoreCase("breaktest")) {
                    GenerateBreakTest.main(null);
                } else if (arg.equalsIgnoreCase("iana")) {
                    IANANames.testSensitivity();
                } else if (arg.equalsIgnoreCase("testDerivedProperties")) {
                    DerivedProperty.test();
                } else if (arg.equalsIgnoreCase("checkCase")) {
                    VerifyUCD.checkCase();
                } else if (arg.equalsIgnoreCase("checkCase3")) {
                    VerifyUCD.checkCase3();
                } else if (arg.equalsIgnoreCase("checkCaseLong")) {
                    VerifyUCD.checkCase2(true);
                } else if (arg.equalsIgnoreCase("checkCaseShort")) {
                    VerifyUCD.checkCase2(false);
                } else if (arg.equalsIgnoreCase("checkCanonicalProperties")) {
                    VerifyUCD.checkCanonicalProperties();
                } else if (arg.equalsIgnoreCase("CheckCaseFold")) {
                    VerifyUCD.CheckCaseFold();
                } else if (arg.equalsIgnoreCase("genIDN")) {
                    VerifyUCD.genIDN();
                } else if (arg.equalsIgnoreCase("VerifyIDN")) {
                    VerifyUCD.VerifyIDN();
                } else if (arg.equalsIgnoreCase("NFTest")) {
                    VerifyUCD.NFTest();
                } else if (arg.equalsIgnoreCase("test1")) {
                    VerifyUCD.test1();
                } else if (arg.equalsIgnoreCase("TestData")) {
                    TestData.main(new String[] {args[++i]});
                } else if (arg.equalsIgnoreCase("MakeUnicodeFiles")) {
                    MakeUnicodeFiles.main(new String[] {});
                } else if (arg.equalsIgnoreCase("checkScripts")) {
                    VerifyUCD.checkScripts();
                } else if (arg.equalsIgnoreCase("IdentifierTest")) {
                    VerifyUCD.IdentifierTest();
                } else if (arg.equalsIgnoreCase("BuildNames")) {
                    BuildNames.main(null);
                } else if (arg.equalsIgnoreCase("JavascriptProperties")) {
                    WriteJavaScriptInfo.assigned();
                } else if (arg.equalsIgnoreCase("TestDirectoryIterator")) {
                    DirectoryIterator.test();
                } else if (arg.equalsIgnoreCase("testnameuniqueness")) {
                    TestNameUniqueness.checkNameList();
                } else if (arg.equalsIgnoreCase("StandardizedVariants")) {
                    GenerateStandardizedVariants.generate();

                    // OTHER STANDARD PROPERTIES

                } else if (arg.equalsIgnoreCase("CaseFolding")) {
                    GenerateCaseFolding.makeCaseFold(true);
                    GenerateCaseFolding.makeCaseFold(false);

                } else if (arg.equalsIgnoreCase("SpecialCasing")) {
                    GenerateCaseFolding.generateSpecialCasing(true);
                    GenerateCaseFolding.generateSpecialCasing(false);
                } else {
                    throw new IllegalArgumentException(
                            "UCD.Main unrecognized command line argument \"" + arg + "\"");
                }
            }
        } catch (Throwable e) {
            // Print and rethrow.
            System.err.println(e);
            e.printStackTrace();
            throw e;
        } finally {
            System.out.println("*** Done *** " + Default.getDate());
        }
    }
}
