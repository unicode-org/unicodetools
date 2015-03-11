/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/Main.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import org.unicode.text.utility.CallArgs;
import org.unicode.text.utility.DirectoryIterator;
import org.unicode.text.utility.FastBinarySearch;
import org.unicode.text.utility.SampleEnum;
import org.unicode.text.utility.Utility;

public final class Main implements UCD_Types {

    static final String classPrefix = "org.unicode.text.UCD.";

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
        //"OtherDerivedProperties",
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

    static final String[] ALL_FILES = {
        "Core", "Extracted"
    };

    public static void main (String[] args) throws Exception {
        System.out.println("*** Start *** " + Default.getDate());

        try {
            for (int i = 0; i < args.length; ++i) {
                final String arg = args[i];
                if (arg.charAt(0) == '#')
                {
                    return; // skip rest of line
                }

                Utility.fixDot();
                System.out.println();
                System.out.println("** Argument: " + args[i] + " ** " + Default.getDate());

                // Expand string arguments

                if (arg.equalsIgnoreCase("ALL")) {
                    args = Utility.append(ALL_FILES, Utility.subarray(args, i+1));
                    i = -1;
                    continue;
                }

                if (arg.equalsIgnoreCase("CORE")) {
                    args = Utility.append(CORE_FILES, Utility.subarray(args, i+1));
                    i = -1;
                    continue;
                }

                if (arg.equalsIgnoreCase("EXTRACTED")) {
                    args = Utility.append(EXTRACTED_FILES, Utility.subarray(args, i+1));
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
                    ConvertUCD.main(new String[]{Default.ucdVersion()});
                } else if (arg.equalsIgnoreCase("statistics")) {
                    VerifyUCD.statistics();
                } else if (arg.equalsIgnoreCase("NFSkippable")) {
                    NFSkippable.main(null);
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
                } else if (arg.equalsIgnoreCase("quicktest")) {
                    QuickTest.test();
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
                } else if (arg.equalsIgnoreCase("checkcollator")) {
                    CheckCollator.main(null);
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
                } else if (arg.equalsIgnoreCase("GenerateThaiBreaks")) {
                    GenerateThaiBreaks.main(null);
                } else if (arg.equalsIgnoreCase("TestData")) {
                    TestData.main(new String[]{args[++i]});
                } else if (arg.equalsIgnoreCase("MakeUnicodeFiles")) {
                    MakeUnicodeFiles.main(new String[]{});
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
                    //else if (arg.equalsIgnoreCase("checkDifferences")) GenerateData.checkDifferences("3.2.0");
                } else if (arg.equalsIgnoreCase("Compare14652")) {
                    Compare14652.main(null);
                } else if (arg.equalsIgnoreCase("StandardizedVariants")) {
                    GenerateStandardizedVariants.generate();

                    // OTHER STANDARD PROPERTIES

                } else if (arg.equalsIgnoreCase("CaseFolding")) {
                    GenerateCaseFolding.makeCaseFold(true);
                    GenerateCaseFolding.makeCaseFold(false);

                } else if (arg.equalsIgnoreCase("SpecialCasing")) {
                    GenerateCaseFolding.generateSpecialCasing(true);
                    GenerateCaseFolding.generateSpecialCasing(false);

                    /*               } else if (arg.equalsIgnoreCase("CompositionExclusions")) {
                    GenerateData.generateCompExclusions();

                } else if (arg.equalsIgnoreCase("DerivedAge")) {
                    GenerateData.generateAge("DerivedData/", "DerivedAge");

                } else if (arg.equalsIgnoreCase("backwardsCompat")) {
                    GenerateData.backwardsCompat("DerivedData/extracted/", "Compatibility_ID_START",
            			new int[] {ID_Start, ID_Continue_NO_Cf, Mod_ID_Start, Mod_ID_Continue_NO_Cf});

                } else if (arg.equalsIgnoreCase("DerivedCoreProperties")) {
                    GenerateData.generateDerived(DERIVED_CORE, true, GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedCoreProperties");

                } else if (arg.equalsIgnoreCase("DerivedNormalizationProps")) {
                    GenerateData.generateDerived(DERIVED_NORMALIZATION, true, GenerateData.HEADER_DERIVED, "DerivedData/",
                        "DerivedNormalizationProps" );

                } else if (arg.equalsIgnoreCase("NormalizationTest")) {
                    GenerateData.writeNormalizerTestSuite("DerivedData/", "NormalizationTest");

                } else if (arg.equalsIgnoreCase("PropertyAliases")) {
                    GenerateData.generatePropertyAliases();

                } else if (arg.equalsIgnoreCase("PropList")) {
                    GenerateData.generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + NEXT_ENUM,
                            GenerateData.HEADER_EXTEND, "DerivedData/", "PropList");

                } else if (arg.equalsIgnoreCase("Scripts")) {
                    GenerateData.generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM,
                            GenerateData.HEADER_SCRIPTS, "DerivedData/", "Scripts");
        // OTHER TESTING

                } else if (arg.equalsIgnoreCase("OtherDerivedProperties")) {
                    //mask = Utility.setBits(0, NFC_Leading, NFC_Resulting);
                    GenerateData.generateDerived((byte)(ALL & ~DERIVED_CORE & ~DERIVED_NORMALIZATION), false, GenerateData.HEADER_DERIVED, "OtherData/", "OtherDerivedProperties");

                } else if (arg.equalsIgnoreCase("AllBinary")) {
                    GenerateData.generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES + NEXT_ENUM,
                            GenerateData.HEADER_EXTEND, "OtherDerived/", "AllBinary");

                } else if (arg.equalsIgnoreCase("DerivedGeneralCategoryTEST")) {
                    GenerateData.generateVerticalSlice(CATEGORY+29, CATEGORY+32, GenerateData.HEADER_DERIVED,
                        "DerivedData/", "DerivedGeneralCategory" );

                } else if (arg.equalsIgnoreCase("listDifferences")) {
                    CompareProperties.listDifferences();

    			} else if (arg.equalsIgnoreCase("partition")) {
    				CompareProperties.partition();

    			} else if (arg.equalsIgnoreCase("propertyStatistics")) {
    				CompareProperties.statistics();

                } else if (arg.equalsIgnoreCase("listAccents")) {
                    GenerateData.listCombiningAccents();

                } else if (arg.equalsIgnoreCase("listGreekVowels")) {
                    GenerateData.listGreekVowels();

                } else if (arg.equalsIgnoreCase("listKatakana")) {
                    GenerateData.listKatakana();
                     */
                    /*
                } else if (arg.equalsIgnoreCase("DerivedFullNormalization")) {
                    mask = Utility.setBits(0, DerivedProperty.GenNFD, DerivedProperty.GenNFKC);
                    GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedFullNormalization" );
                } else if (arg.equalsIgnoreCase("caseignorable")) {
                    mask = Utility.setBits(0, DerivedProperty.Other_Case_Ignorable, DerivedProperty.Type_i);
                    GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "OtherData/", "CaseIgnorable" );
                } else if (arg.equalsIgnoreCase("nfunsafestart")) {
                    mask = Utility.setBits(0, NFD_UnsafeStart, NFKC_UnsafeStart);
                    GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "OtherData/", "NFUnsafeStart");
                     */

                } else {
                    CallArgs.call(new String[]{arg}, classPrefix);
                }


                //checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
                //checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");


                //GenerateData.generateDerived(Utility.setBits(0, DerivedProperty.PropMath, DerivedProperty.Mod_ID_Continue_NO_Cf),
                //    GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedPropData2" );
                //GenerateData.generateVerticalSlice(SCRIPT, SCRIPT+1, "ScriptCommon" );
                //listStrings("LowerCase" , 0,0);
                //GenerateData.generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedData/", "DerivedPropData1" );

                // AGE stuff
                //UCD ucd = UCD.make();
                //System.out.println(ucd.getAgeID(0x61));
                //System.out.println(ucd.getAgeID(0x2FA1D));

                //
            }
        } catch (Exception e) {
            // Print and rethrow.
            System.err.println(e);
            e.printStackTrace();
            throw e;
        } finally {
            System.out.println("*** Done *** " + Default.getDate());
        }
    }
}
