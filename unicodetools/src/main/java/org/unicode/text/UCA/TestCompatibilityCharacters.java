package org.unicode.text.UCA;

import com.ibm.icu.text.UTF16;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

public final class TestCompatibilityCharacters {
    private static final byte MULTIPLES = 0x20, COMPRESSED = 0x40, OTHER_MASK = 0x1F;
    private static final BitSet compressSet = new BitSet();
    private static int[] markCes = new int[50];

    static void testCompatibilityCharacters(UCA uca) throws IOException {
        final String fullFileName = "UCA_CompatComparison.txt";
        final PrintWriter testLog =
                Utility.openPrintWriter(
                        UCA.getOutputDir() + File.separator + "log",
                        fullFileName,
                        Utility.UTF8_WINDOWS);

        final int[] kenCes = new int[50];
        final int[] markCes = new int[50];
        final int[] kenComp = new int[50];
        final Map<String, String> forLater = new TreeMap<String, String>();
        int count = 0;
        int typeLimit = UCD_Types.CANONICAL;
        boolean decompType = false;
        final boolean TESTING = false;
        if (TESTING) {
            typeLimit = UCD_Types.COMPATIBILITY;
            decompType = true;
        }

        // first find all the characters that cannot be generated "correctly"

        for (int i = 0; i < 0xFFFF; ++i) {
            int type = Default.ucd().getDecompositionType(i);
            if (type < typeLimit) {
                continue;
            }
            if (!uca.codePointHasExplicitMappings(i)) {
                continue;
            }
            // fix type
            type = getDecompType(i);

            final String s = String.valueOf((char) i);
            final int kenLen = uca.getCEs(s, decompType, kenCes); // true
            final int markLen = fixCompatibilityCE(uca, s, true, markCes, false);

            if (!arraysMatch(kenCes, kenLen, markCes, markLen)) {
                final int kenCLen = fixCompatibilityCE(uca, s, true, kenComp, true);
                final String comp = CEList.toString(kenComp, kenCLen);

                if (arraysMatch(kenCes, kenLen, kenComp, kenCLen)) {
                    forLater.put((char) (COMPRESSED | type) + s, comp);
                    continue;
                }
                if (type == UCD_Types.CANONICAL && multipleZeroPrimaries(markCes, markLen)) {
                    forLater.put((char) (MULTIPLES | type) + s, comp);
                    continue;
                }
                forLater.put((char) type + s, comp);
            }
        }

        final Iterator<String> it = forLater.keySet().iterator();
        byte oldType = (byte) 0xFF; // anything unique
        int caseCount = 0;
        WriteCollationData.writeVersionAndDate(testLog, fullFileName, true);
        // log.println("# UCA Version: " + collator.getDataVersion() + "/" +
        // collator.getUCDVersion());
        // log.println("Generated: " + getNormalDate());
        while (it.hasNext()) {
            final String key = it.next();
            final byte type = (byte) key.charAt(0);
            if (type != oldType) {
                oldType = type;
                testLog.println("===============================================================");
                testLog.print("CASE " + (caseCount++) + ": ");
                final byte rType = (byte) (type & OTHER_MASK);
                testLog.println(
                        "    Decomposition Type = " + UCD.getDecompositionTypeID_fromIndex(rType));
                if ((type & COMPRESSED) != 0) {
                    testLog.println("    Successfully Compressed a la Ken");
                    testLog.println("    [XXXX.0020.YYYY][0000.ZZZZ.0002] => [XXXX.ZZZZ.YYYY]");
                } else if ((type & MULTIPLES) != 0) {
                    testLog.println("    PLURAL ACCENTS");
                }
                testLog.println("===============================================================");
                testLog.println();
            }
            final String s = key.substring(1);
            final String comp = forLater.get(key);

            final int kenLen = uca.getCEs(s, decompType, kenCes);
            final String kenStr = CEList.toString(kenCes, kenLen);

            final int markLen = fixCompatibilityCE(uca, s, true, markCes, false);
            final String markStr = CEList.toString(markCes, markLen);

            if ((type & COMPRESSED) != 0) {
                testLog.println(
                        "COMPRESSED #" + (++count) + ": " + Default.ucd().getCodeAndName(s));
                testLog.println("         : " + comp);
            } else {
                testLog.println(
                        "DIFFERENCE #" + (++count) + ": " + Default.ucd().getCodeAndName(s));
                testLog.println("generated : " + markStr);
                if (!markStr.equals(comp)) {
                    testLog.println("compressed: " + comp);
                }
                testLog.println("Ken's     : " + kenStr);
                final String nfkd = Default.nfkd().normalize(s);
                testLog.println("NFKD      : " + Default.ucd().getCodeAndName(nfkd));
                final String nfd = Default.nfd().normalize(s);
                if (!nfd.equals(nfkd)) {
                    testLog.println("NFD       : " + Default.ucd().getCodeAndName(nfd));
                }
                // kenCLen = collator.getCEs(decomp, true, kenComp);
                // log.println("decomp ce: " + CEList.toString(kenComp, kenCLen));
            }
            testLog.println();
        }
        testLog.println("===============================================================");
        testLog.println();
        testLog.println("Compressible Secondaries");
        for (int i = 0; i < compressSet.size(); ++i) {
            if ((i & 0xF) == 0) {
                testLog.println();
            }
            if (!compressSet.get(i)) {
                testLog.print("-  ");
            } else {
                testLog.print(Utility.hex(i, 3) + ", ");
            }
        }
        testLog.close();
    }

    private static final byte getDecompType(int cp) {
        final byte result = Default.ucd().getDecompositionType(cp);
        if (result == UCD_Types.CANONICAL) {
            final String d = Default.nfd().normalize(cp); // TODO
            int cp1;
            for (int i = 0; i < d.length(); i += UTF16.getCharCount(cp1)) {
                cp1 = UTF16.charAt(d, i);
                final byte t = Default.ucd().getDecompositionType(cp1);
                if (t > UCD_Types.CANONICAL) {
                    return t;
                }
            }
        }
        return result;
    }

    private static final boolean multipleZeroPrimaries(int[] a, int aLen) {
        int count = 0;
        for (int i = 0; i < aLen; ++i) {
            if (CEList.getPrimary(a[i]) == 0) {
                if (count == 1) {
                    return true;
                }
                count++;
            } else {
                count = 0;
            }
        }
        return false;
    }

    private static int kenCompress(int[] markCes, int markLen) {
        if (markLen == 0) {
            return 0;
        }
        int out = 1;
        for (int i = 1; i < markLen; ++i) {
            final int next = markCes[i];
            final int prev = markCes[out - 1];
            if (CEList.getPrimary(next) == 0
                    && CEList.getSecondary(prev) == 0x20
                    && CEList.getTertiary(next) == 0x2) {
                markCes[out - 1] =
                        UCA.makeKey(
                                CEList.getPrimary(prev),
                                CEList.getSecondary(next),
                                CEList.getTertiary(prev));
                compressSet.set(CEList.getSecondary(next));
            } else {
                markCes[out++] = next;
            }
        }
        return out;
    }

    private static boolean arraysMatch(int[] a, int aLen, int[] b, int bLen) {
        if (aLen != bLen) {
            return false;
        }
        for (int i = 0; i < aLen; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static int fixCompatibilityCE(
            UCA uca, String s, boolean decompose, int[] output, boolean compress) {
        final byte type = getDecompType(UTF16.charAt(s, 0));
        // char ch = s.charAt(0);

        final String decomp = Default.nfkd().normalize(s);
        int len = 0;
        int markLen = uca.getCEs(decomp, true, markCes);
        if (compress) {
            markLen = kenCompress(markCes, markLen);
        }

        // for (int j = 0; j < decomp.length(); ++j) {
        for (int k = 0; k < markLen; ++k) {
            int t = CEList.getTertiary(markCes[k]);
            t = CEList.remap(k, type, t);
            /*
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
            */
            output[len++] =
                    UCA.makeKey(CEList.getPrimary(markCes[k]), CEList.getSecondary(markCes[k]), t);
            // }
        }
        return len;
    }
}
