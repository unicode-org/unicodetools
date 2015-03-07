package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeRelation;
import org.unicode.props.UnicodeRelation.SetMaker;
import org.unicode.test.TestSecurity;
import org.unicode.text.UCD.IdentifierInfo;
import org.unicode.text.UCD.IdentifierInfo.IdentifierStatus;
import org.unicode.text.UCD.IdentifierInfo.IdentifierType;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.AlphabeticIndex.Record;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class IcannMsr {
    static final SetMaker<String>                SM        = new SetMaker<String>() {
        public Set<String> make() {
            return new LinkedHashSet<String>();
        }
    };
    static final SetMaker<IdentifierType>        ITM       = new SetMaker<IdentifierType>() {
        public EnumSet<IdentifierType> make() {
            return EnumSet.noneOf(IdentifierType.class);
        }
    };
    static final UnicodeRelation<String>         DATA      = new UnicodeRelation<String>(SM);
    static final UnicodeRelation<IdentifierType> DATA2TYPE = new UnicodeRelation<IdentifierInfo.IdentifierType>(ITM);
    static final UnicodeRelation<String>         REMAP     = new UnicodeRelation<String>(SM);
    static {
        UnicodeSet found = new UnicodeSet();
        boolean comment = false;
        int cp = -1;
        for (String line : FileUtilities.in("/Users/markdavis/Google Drive/workspace/DATA/icann/", "MSR-2.0v4Delta.lst")) {
            // System.out.println(line);
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            } else if (line.startsWith("@")) {
                comment = true;
            } else if (line.startsWith("\t*")) {
                if (comment) {
                    continue;
                }
                String dataLine = line.substring(2).trim();
                DATA.add(cp, dataLine);
                IdentifierType identifierType = getIdentifierType(cp, dataLine);
                if (identifierType != null) {
                    DATA2TYPE.add(cp, identifierType);
                }
            } else if (line.startsWith("\t")) {
                continue;
            } else {
                int tabPos = line.indexOf('\t');
                cp = Integer.parseInt(line.substring(0, tabPos), 16);
                found.add(cp);
                comment = false;
            }
        }
        // double-check that every found code point has data
        if (!found.equals(DATA.keySet())) {
            throw new IllegalArgumentException();
        }
        // now read recommended
        List<Pair<String, String>> data = new ArrayList<>();

        //        try {
        //            BufferedReader f = FileUtilities.openFile("/Users/markdavis/Google Drive/workspace/DATA/icann/", "msr-wle-rules-04dec14-en.xml");
        //            int line = f.read();
        //            System.out.println(Utility.hex(line));
        //        } catch (IOException e) {
        //        }
        Matcher first = Pattern.compile("@first-cp=\"([0-9A-Fa-f]+)\"").matcher("");
        Matcher last = Pattern.compile("@last-cp=\"([0-9A-Fa-f]+)\"").matcher("");
        Matcher only = Pattern.compile("@cp=\"([0-9A-Fa-f]+)\"").matcher("");
        XMLFileReader.loadPathValues("/Users/markdavis/Google Drive/workspace/DATA/icann/msr-wle-rules-04dec14-en.xml", data, false);
        for (Pair<String, String> datum : data) {
            String path = datum.getFirst();
            // (//lgr/data/range[@first-cp="20E0E"][@last-cp="20E0F"][@tag="sc:Hani"][@ref="4 ZH IIC"],)
            // (//lgr/data/char[@cp="20731"][@tag="sc:Hani"][@ref="4 ZH IIC"],)
            if (path.startsWith("//lgr/data/char")) {
                only.reset(path).find();
                int onlyCp = Integer.parseInt(only.group(1),16);
                addIfEmpty(onlyCp);
            } else if (path.startsWith("//lgr/data/range")) {
                first.reset(path).find();
                int firstCp = Integer.parseInt(first.group(1),16);
                last.reset(path).find();
                int lastCp = Integer.parseInt(last.group(1),16);
                for (int i = firstCp; i <= lastCp; ++i) {
                    addIfEmpty(i);
                }
            } else {
                //System.out.println("Skipping " + datum);
            }
        }
        DATA.freeze();
        DATA2TYPE.freeze();
        REMAP.freeze();
    }

    private static void addIfEmpty(int onlyCp) {
        if (DATA2TYPE.get(onlyCp) != null) {
            throw new IllegalArgumentException("Duplicate item");
        }
        DATA2TYPE.add(onlyCp, IdentifierType.recommended);
    }

    private static IdentifierType getIdentifierType(int cp, String dataLine) {
        try {
            int spacePos = dataLine.indexOf(' ');
            String firstWord = spacePos < 0 ? dataLine : dataLine.substring(0, spacePos);

            if (firstWord.equalsIgnoreCase("contexto")) {
                return IdentifierType.inclusion;
            } else if (firstWord.equalsIgnoreCase("punctuation")
                    || firstWord.equalsIgnoreCase("symbol")
                    || firstWord.equalsIgnoreCase("deferred")
                    || firstWord.equalsIgnoreCase("religious")
                    || firstWord.equalsIgnoreCase("unstable")
                    || firstWord.equalsIgnoreCase("use")
                    || firstWord.equalsIgnoreCase("numeric")
                    || firstWord.equalsIgnoreCase("homoglyph")) {
                REMAP.add(cp, "technical\t← " + dataLine);
                return IdentifierType.technical;
            } else if (firstWord.equalsIgnoreCase("limited")) {
                return IdentifierType.uncommon_use;
            } else if (firstWord.equalsIgnoreCase("Dagbani")) {
                REMAP.add(cp, "ignored " + dataLine);
                return null;
            }
            IdentifierType identifierType = IdentifierType.fromString(firstWord);
            return identifierType;
        } catch (Exception e) {
            throw new IllegalArgumentException(Utility.hex(cp)
                    + "\t" + UCharacter.getName(cp)
                    + "\t" + dataLine);
        }
    }

    private static final String SECURITY = Settings.UNICODETOOLS_DIRECTORY + "data/security/";

    public static void main(String[] args) {
        // for (Entry<String, Set<IdentifierType>> x : DATA2TYPE.keyValues()) {
        // String s = x.getKey();
        // System.out.println(s
        // + "\t" + UCharacter.getName(s,"+")
        // + "\t" + x.getValue()
        // );
        // }
        System.out.println("\nREMAPPED");
        showValues("remapped.txt", REMAP, null);

        System.out.println("\nValues");
        showValues("values.txt", DATA2TYPE, IdentifierType.recommended);

        XIDModifications xidModOld = new XIDModifications(SECURITY + Settings.latestVersion);
        UnicodeMap<IdentifierType> xidMod = xidModOld.getType();

        UnicodeMap<Set<String>> cldrChars = TestSecurity.getCLDRCharacters();

        UnicodeMap<Pair<IdentifierType, IdentifierType>> diff = new UnicodeMap<>();
        for (EntryRange x : new UnicodeSet("[^[:cn:][:co:][:cs:][:cwcf:]]").ranges()) {
            for (int i = x.codepoint; i <= x.codepointEnd; ++i) {
                IdentifierType unicode = xidMod.get(i);
                IdentifierStatus unicodeStatus = unicode.identifierStatus;
                Set<IdentifierType> icann = DATA2TYPE.get(i);
                IdentifierStatus icannStatus = icann == null ? IdentifierStatus.restricted : icann.iterator().next().identifierStatus;
                if (unicodeStatus != icannStatus) {
                    IdentifierType icann1 = icann == null ? null : icann.iterator().next();
                    diff.put(i, Pair.of(unicode, icann1));
                }
            }
        }
        showValues("diff.txt", diff);
    }

    static final Comparator<Pair<IdentifierType, IdentifierType>> IDPAIR = new Comparator<Pair<IdentifierType, IdentifierType>>() {

        @Override
        public int compare(Pair<IdentifierType, IdentifierType> o1, Pair<IdentifierType, IdentifierType> o2) {
            IdentifierType o11 = o1.getFirst();
            IdentifierType o21 = o2.getFirst();
            int diff = o11.compareTo(o21);
            if (diff != 0) return diff;
            IdentifierType o12 = o1.getSecond();
            IdentifierType o22 = o2.getSecond();
            if (o12 == null) {
                return o22 == null ? 0 : -1;
            } else if (o22 == null) {
                return 1;
            }
            return o12.compareTo(o22);
        }
    };

    private static <T> boolean equalsSet(T a, Set<T> b) {
        if (b == null) {
            return a == null;
        }
        return b.contains(a);
    }

    static final Collator COL = Collator.getInstance(ULocale.ROOT);
    static {
        COL.setStrength(Collator.IDENTICAL);
    }

    private static <T> void showValues(String file, UnicodeRelation<T> remap2, T skipValue) {
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "icann/", file)) {
            TreeSet<T> sorted = new TreeSet<T>(remap2.values());
            for (T value : sorted) {
                UnicodeSet set = remap2.getKeys(value);
                out.println("\n" + value + "\t" + set.size() + "\t" + set.toPattern(false));
                if (value == skipValue) {
                    continue;
                }
                showSortedSet(out, set);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> Script_Extensions = iup.load(UcdProperty.Script_Extensions);

    private static void showSortedSet(PrintWriter out, UnicodeSet set) {

        for (String script : Script_Extensions.values()) {
            UnicodeSet ss = Script_Extensions.getSet(script);
            if (ss.containsNone(set)) {
                continue;
            }
            UnicodeSet inScript = new UnicodeSet(ss).retainAll(set);
            for (EntryRange x : inScript.ranges()) {
                show(out, x.codepoint, x.codepointEnd);
//                if (x.codepoint == x.codepointEnd) {
//                    continue;
//                } else if (x.codepoint + 1 == x.codepointEnd) {
//                    show(out, "", x.codepointEnd);
//                } else {
//                    show(out, "..", x.codepointEnd);
//                }
            }
            out.println();
        }
    }

    private static void show(PrintWriter out, int cpStart, int cpEnd) {
        out.println("\t" + Utility.hex(cpStart)
                + (cpStart == cpEnd ? "\t" : ".." + Utility.hex(cpEnd))
                + "\t " + UTF16.valueOf(cpStart)
                + (cpStart == cpEnd ? "\t" : ".." + UTF16.valueOf(cpEnd))
                + " \t" + getGc(cpStart)
                + (cpStart == cpEnd ? "\t" : "..")
                + "\t" + Script_Extensions.get(cpStart)
                + (cpStart == cpEnd ? "\t" : "..")
                + "\t" + UCharacter.getName(cpStart)
                + (cpStart == cpEnd ? "\t" : "..")
                );
    }

    private static String getGc(int cpStart) {
        return UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, UCharacter.getType(cpStart), UProperty.NameChoice.SHORT);
    }

    private static void showValues(String file, UnicodeMap<Pair<IdentifierType, IdentifierType>> diff) {
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "icann/", file)) {
            Set<Pair<IdentifierType, IdentifierType>> sorted = new TreeSet<>(IDPAIR);
            sorted.addAll(diff.values());

            for (Pair<IdentifierType, IdentifierType> value : sorted) {
                UnicodeSet us = diff.getSet(value);
                out.println("\n#39:" + value.getFirst()
                        + "\tMSR:" + value.getSecond()
                        + "\tcount:" + us.size()
                        + "\t" + us.toPattern(false)
                        );
                showSortedSet(out, us);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
