package org.unicode.text.UCA;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.Utility;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;

public class GenerateUcaDecompositions {
    private static final int LEAST_FAKE_SECONDARY = 0x139;
    static final UCA uca = UCA.buildCollator(null);
    static Normalizer2 nfkcCf = Normalizer2.getNFKCCasefoldInstance();
    static Normalizer2 nfd = Normalizer2.getNFDInstance();
    static Normalizer2 nfkd = Normalizer2.getNFKDInstance();
    static UCD ucd = UCD.make();
    static Map<Integer,Best> ceToChar = new HashMap();
    static Map<Integer,CEList> char2Ces = new TreeMap();
    static int lastSpecial = '\uE000' - 1;

    static class Best {
        int value;

        public Best set(int cp) {
            String decomp = nfkcCf.getDecomposition(cp);
            if (decomp != null) {
                if (decomp.codePointCount(0, decomp.length()) == 1) {
                    value = decomp.codePointAt(0);
                    return this;
                }
            }
            value = ucd.getCase(cp, UCD.SIMPLE, UCD.LOWER).codePointAt(0);
            return this;
        }
    }

    public static void main(String[] args) {

        compareUcaDecomp();

        System.out.println("low variable " + Utility.hex(UCA.getPrimary(uca.getVariableLowCE())) + ", high variable " + Utility.hex(UCA.getPrimary(uca.getVariableHighCE())));
        for (int i = 0; i <= 0x10FFFF; ++i) {
            int cat = ucd.getCategory(i);
            if (cat == ucd.Cn || cat == ucd.Co || cat == ucd.Cs) {
                continue;
            }
            String nfdOrNull = nfd.getDecomposition(i);
            if (nfdOrNull != null) continue;

            String s = UTF16.valueOf(i);
            CEList ceList = uca.getCEList(s, true);

            char2Ces.put(i, ceList);
            if (ceList.length() == 1) {
                int ceRaw = ceList.at(0);
                Integer ce = ceRaw & ~0x7F; // nuke tertiary
                addCe(i, ce);
            }
        }
        int count = 0;
        UnicodeSet ignored = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (char2Ces.get(i) == null) {
                continue;
            }

            if ((count++ % 1000) == 0) {
                //System.out.println("#" + count);
                int debug = 0;
            }

            String nfkdOrNull = nfkcCf.getDecomposition(i);
            String ucaDecomp = getUcaDecomp(i);
            if (nfkdOrNull == null && ucaDecomp == null) {
                continue;
            }
            if (ucaDecomp == null) {
                ucaDecomp = UTF16.valueOf(i);
            }
            if (nfkdOrNull == null) {
                nfkdOrNull = UTF16.valueOf(i);
            }
            if (nfkdOrNull.equals(ucaDecomp)) {
                continue;
            }
            if (ucaDecomp.isEmpty()) {
                ignored.add(i);
                continue;
            }
            int gc = UCharacter.getIntPropertyValue(i, UProperty.GENERAL_CATEGORY);
            String gcName = UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, gc, NameChoice.SHORT);
            System.out.println(Utility.hex(i) + ";\t" + Utility.hex(ucaDecomp)  + ";\t" + Utility.hex(nfkdOrNull)
                    + ";\t#\t" + gcName
                    + "\t" + UTF16.valueOf(i) + "\t" + UCharacter.getExtendedName(i)
                    + "\t→\t" + ucaDecomp + "\t" + UCharacter.getName(ucaDecomp, " + ")
                    + "\t≠\t" + nfkdOrNull + "\t" + UCharacter.getName(nfkdOrNull, " + ")
                    );
        }
        System.out.println(ignored + ";\t<empty>");
    }

    public static void compareUcaDecomp() {
        UnicodeMap<String> kensDecomp = new UnicodeMap();
        UnicodeMap<String> kensDecompType = new UnicodeMap();
        for (String line : FileUtilities.in("/Users/markdavis/Documents/indigo/DATA/UCA/6.2.0/","decomps-6.2.0.txt")) {
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            int cp = Integer.parseInt(parts[0],16);
            if (!parts[1].isEmpty()) {
                kensDecompType.put(cp, parts[1]);
            }
            String decomp = Utility.fromHex(parts[2]);
            kensDecomp.put(cp, decomp);
        }
        kensDecomp.freeze();
        kensDecompType.freeze();
        UnicodeSet onlyKens = new UnicodeSet();
        UnicodeSet onlyKensNfd = new UnicodeSet();
        UnicodeSet onlyNFKD = new UnicodeSet();
        UnicodeSet same = new UnicodeSet();
        UnicodeSet diff = new UnicodeSet();
        UnicodeSet spaces = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            int cat = ucd.getCategory(i);
            if (cat == ucd.Cn || cat == ucd.Co || cat == ucd.Cs) {
                continue;
            }
            String kens = kensDecomp.get(i);
            String nfkdString = nfkd.getDecomposition(i);
            if (kens == nfkdString) {
                // don't care
            } else if (kens == null) {
                onlyNFKD.add(i);
            } else if (nfkdString == null) {
                onlyKens.add(i);
            } else if (kens.equals(nfkdString)) {
                same.add(i);
            } else {
                String kensNfd = nfd.normalize(kens);
                if (kensNfd.equals(nfkdString)) {
                    onlyKensNfd.add(i);
                } else if (nfkdString.length() > 1 && (nfkdString.startsWith(" ") || nfkdString.startsWith("\u0640"))) {
                    spaces.add(i);
                } else {
                    diff.add(i);
                }
            }
        }
        System.out.println("\n# Both map, same results:\t" + same.size() + "\t" + same + "\n");
        System.out.println("\n# Only difference is DUCET is not NFD:\t" + onlyKensNfd.size() + "\n");
        for (String s : onlyKensNfd) {
            String kens = kensDecomp.get(s);
            System.out.println("*DUCET:\t" + Utility.hex(s) + ";" + Utility.hex(kens, " ") + " # " + UCharacter.getName(s, " ") + " => " + UCharacter.getName(kens, " + "));
        }
        System.out.println("\n# Only mapped in NFKD, not in DUCET:\t" + onlyNFKD.size() + "\t" + onlyNFKD + "\n");
        System.out.println("\n# Only mapped in DUCET, not in NFKD:\t" + onlyKens.size() + "\n");
        for (String s : onlyKens) {
            String kens = kensDecomp.get(s);
            System.out.println("DUCET:\t" + Utility.hex(s) + ";" + Utility.hex(kens, " ") + " # " + UCharacter.getName(s, " ") + " => " + UCharacter.getName(kens, " + "));
        }
        System.out.println("\n# Each have different results (SPACES/TATWEEL):\t" + spaces.size() + "\n");
        showDifs(kensDecomp, spaces);
        System.out.println("\n# Each have different results (OTHER):\t" + diff.size() + "\n");
        showDifs(kensDecomp, diff);
    }

    public static void showDifs(UnicodeMap<String> kensDecomp, UnicodeSet diff) {
        for (String s : diff) {
            String kens = kensDecomp.get(s);
            String nfkdString = nfkd.normalize(s);
            System.out.println("NFKD: \t" + Utility.hex(s) + ";" + Utility.hex(nfkdString, " ") + " # " + UCharacter.getName(s, " ") + " => " + UCharacter.getName(nfkdString, " + "));
            System.out.println("DUCET:\t" + Utility.hex(s) + ";" + Utility.hex(kens, " ") + " # " + UCharacter.getName(s, " ") + " => " + UCharacter.getName(kens, " + "));
            System.out.println();
        }
    }

    private static String getUcaDecomp(int cp) {
        CEList ceList = char2Ces.get(cp);
        if (ceList.length() == 1) {
            return "";
        }
        if (ceList.length() == 1) {
            int ce = ceList.at(0);
            Integer correspondingChar = getCorresponding(cp, ce);
            if (cp == correspondingChar.intValue()) {
                return null;
            }
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ceList.length(); ++i) {
            int ce = ceList.at(i);
            if (UCA.getPrimary(ce) == 0 && UCA.getSecondary(ce) >= LEAST_FAKE_SECONDARY) {
                continue; // skip fake secondaries
            }
            int correspondingCharacter;
            if (UCA.isImplicitLeadCE(ce)) {
                int ce2 = ceList.at(++i);
                correspondingCharacter = UCA.ImplicitToCodePoint(UCA.getPrimary(ce), UCA.getPrimary(ce2));
            } else {
                correspondingCharacter = getCorresponding(cp, ce);
            }
            b.appendCodePoint(correspondingCharacter);
        }
        return b.toString();
    }

    public static Best addCe(int i, Integer ce) {
        Best best = ceToChar.get(ce);
        if (best == null) {
            ceToChar.put(ce, best = new Best().set(i));
        }
        return best;
    }

    public static int getCorresponding(int cp, int ce) {
        ce = ce & ~0x7F; // nuke tertiary
        Best correspondingChar = ceToChar.get(ce);
        if (correspondingChar == null) {
            correspondingChar = addCe(++lastSpecial, ce);
            System.out.println("adding " + Utility.hex(correspondingChar.value) + " for U+" + Utility.hex(cp) + " " + UCharacter.getExtendedName(cp));
        }
        return correspondingChar.value;
    }
}
