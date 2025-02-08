package org.unicode.xml;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import org.unicode.props.UcdProperty;

/**
 * Utility for comparing two UCDXML files. Originally intended to compare UCDXML files generated
 * using https://github.com/eric-muller/ucdxml to UCDXML files generated using
 * org.unicode.xml.UCDXML.
 */
public class CompareUCDXML {

    private static final String NEWLINE = System.getProperty("line.separator");
    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.create("fileA", 'a', UOption.REQUIRES_ARG),
        UOption.create("fileB", 'b', UOption.REQUIRES_ARG)
    };

    private static final UcdProperty[] codepointSequenceProperties =
            new UcdProperty[] {
                UcdProperty.Named_Sequences,
                UcdProperty.Named_Sequences_Prov,
                UcdProperty.Standardized_Variant,
                UcdProperty.Emoji_DCM,
                UcdProperty.Emoji_KDDI,
                UcdProperty.Emoji_SB,
                UcdProperty.Do_Not_Emit_Preferred
            };

    private static final HashMap<Integer, String[]> knownDifferences;

    static {
        knownDifferences = new HashMap<>();

        // https://github.com/unicode-org/properties/issues/296
        knownDifferences.put(0x31E4, new String[] {"Hani", "Zyyy"});
        knownDifferences.put(0x31E5, new String[] {"Hani", "Zyyy"});

        // https://github.com/unicode-org/unicodetools/issues/325
        knownDifferences.put(0x109F7, new String[] {"1/6", "2/12"});
        knownDifferences.put(0x109F8, new String[] {"1/4", "3/12"});
        knownDifferences.put(0x109F9, new String[] {"1/3", "4/12"});
        knownDifferences.put(0x109FB, new String[] {"1/2", "6/12"});
        knownDifferences.put(0x109FD, new String[] {"2/3", "8/12"});
        knownDifferences.put(0x109FE, new String[] {"3/4", "9/12"});
        knownDifferences.put(0x109FF, new String[] {"5/6", "10/12"});

        // https://github.com/unicode-org/properties/issues/172
        knownDifferences.put(0x5146, new String[] {"1000000", "1000000 1000000000000"});
        knownDifferences.put(0x79ED, new String[] {"1000000000", "1000000000 1000000000000"});
    }

    private static final int HELP = 0, FILE_A = 1, FILE_B = 2, LOGFILE = 3;

    public static void main(String[] args) throws Exception {
        File fileA = null;
        File fileB = null;
        int errorCount = 0;

        UOption.parseArgs(args, options);

        if (options[HELP].doesOccur) {
            System.out.println("CompareUcdXML --fileA {file path} --fileB {file path}");
            System.exit(0);
        }

        if (options[FILE_A].doesOccur) {
            try {
                fileA = new File(options[FILE_A].value);
                if (!fileA.exists()) {
                    throw new IOException();
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not find " + options[FILE_A].value);
            }
        } else {
            throw new IllegalArgumentException("Missing command line option: --fileA (or -a)");
        }

        if (options[FILE_B].doesOccur) {
            try {
                fileB = new File(options[FILE_B].value);
                if (!fileB.exists()) {
                    throw new IOException();
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not find " + options[FILE_B].value);
            }
        } else {
            throw new IllegalArgumentException("Missing command line option: --fileB (or -b)");
        }

        System.out.println("Comparing " + fileA + " and " + fileB);

        final XMLProperties xmlPropsA = new XMLProperties(fileA);
        final XMLProperties xmlPropsB = new XMLProperties(fileB);

        // First, iterate through the UcdProperties on each codepoint.
        for (final UcdProperty prop : UcdProperty.values()) {
            UnicodeMap<String> fileAMap = xmlPropsA.getMap(prop);
            UnicodeMap<String> fileBMap = xmlPropsB.getMap(prop);
            if (!fileAMap.equals(fileBMap)) {
                for (int i = 0; i <= 0x10ffff; ++i) {
                    try {
                        String xmlValA = fileAMap.get(i);
                        String xmlValB = fileBMap.get(i);
                        if (!Objects.equals(xmlValA, xmlValB)) {
                            // At least one string is != null and the strings are different, but we
                            // don't care if one
                            // is null and one is empty_string
                            // As far as we care, empty_string == null == "00000"
                            int lenA =
                                    (xmlValA == null
                                            ? 0
                                            : (xmlValA.equals("00000") ? 0 : xmlValA.length()));
                            int lenB =
                                    (xmlValB == null
                                            ? 0
                                            : (xmlValB.equals("00000") ? 0 : xmlValB.length()));
                            if (!(lenA == 0 && lenB == 0)
                                    && !isKnownDifference(i, xmlValA, xmlValB)) {
                                errorCount++;
                                System.out.println(
                                        "For UCDProperty "
                                                + prop.name()
                                                + " ("
                                                + prop.getShortName()
                                                + ") ["
                                                + String.format("0x%04X", i)
                                                + "], ");
                                System.out.println("\t" + fileA + " = " + xmlValA);
                                System.out.println("\t" + fileB + " = " + xmlValB);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Exception thrown for " + String.format("0x%04X", i));
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        // Now handle anything that contains codepoint sequences.
        for (UcdProperty prop : codepointSequenceProperties) {
            UnicodeMap<String> fileAMap = xmlPropsA.getMap(prop);
            UnicodeMap<String> fileBMap = xmlPropsB.getMap(prop);
            UnicodeSet differences = fileAMap.keySet().addAll(fileBMap.keySet());
            for (String key : differences) {
                try {
                    String xmlValA = fileAMap.get(key);
                    String xmlValB = fileBMap.get(key);
                    if (!Objects.equals(xmlValA, xmlValB)) {
                        // At least one string is != null and the strings are different, but we
                        // don't care if one
                        // is null and one is empty_string
                        // As far as we care, empty_string == null == "00000"
                        int lenA =
                                (xmlValA == null
                                        ? 0
                                        : (xmlValA.equals("00000") ? 0 : xmlValA.length()));
                        int lenB =
                                (xmlValB == null
                                        ? 0
                                        : (xmlValB.equals("00000") ? 0 : xmlValB.length()));
                        if (!(lenA == 0 && lenB == 0)) {
                            errorCount++;
                            System.out.println(
                                    "For UCDProperty "
                                            + prop.name()
                                            + " ("
                                            + prop.getShortName()
                                            + ") ["
                                            + key
                                            + "], ");
                            System.out.println("\t" + fileA + " = " + xmlValA);
                            System.out.println("\t" + fileB + " = " + xmlValB);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception thrown for " + String.format("0x%04X", key));
                    System.out.println(e.getMessage());
                }
            }
        }
        System.exit(errorCount);
    }

    private static boolean isKnownDifference(int codepoint, String xmlValA, String xmlValB) {
        if (knownDifferences.containsKey(codepoint)) {
            String knownValue1 = knownDifferences.get(codepoint)[0];
            String knownValue2 = knownDifferences.get(codepoint)[1];
            return (knownValue1.equals(xmlValA) && knownValue2.equals(xmlValB))
                    || (knownValue1.equals(xmlValB) && knownValue2.equals(xmlValA));
        }
        return false;
    }
}
