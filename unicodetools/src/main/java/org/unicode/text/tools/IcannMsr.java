package org.unicode.text.tools;

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
import org.unicode.text.UCD.IdentifierInfo;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Status;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Type;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.util.ULocale;

public class IcannMsr {
    private static final String ICANN_DIR = Settings.Output.UNICODETOOLS_OUTPUT_DIR + "DATA/icann/";

    // Change these with new versions

    private static final String XML_DATA = "msr-2-wle-rules-13apr15-en.xml";
    private static final String DELTA_LIST = "MSR-2.0v5Delta.lst";

    static final SetMaker<String>                SM        = new SetMaker<String>() {
        public Set<String> make() {
            return new LinkedHashSet<String>();
        }
    };
    static final SetMaker<Identifier_Type>        ITM       = new SetMaker<Identifier_Type>() {
        public EnumSet<Identifier_Type> make() {
            return EnumSet.noneOf(Identifier_Type.class);
        }
    };
    static final UnicodeRelation<String>         DATA      = new UnicodeRelation<String>(SM);
    static final UnicodeRelation<Identifier_Type> DATA2TYPE = new UnicodeRelation<IdentifierInfo.Identifier_Type>(ITM);
    static final UnicodeRelation<String>         REMAP     = new UnicodeRelation<String>(SM);
    static {
        UnicodeSet found = new UnicodeSet();
        boolean comment = false;
        int cp = -1;
        for (String line : FileUtilities.in(ICANN_DIR, DELTA_LIST)) {
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
                Identifier_Type identifierType = getIdentifierType(cp, dataLine);
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
        //            BufferedReader f = FileUtilities.openFile(ICANN_DIR, "msr-wle-rules-04dec14-en.xml");
        //            int line = f.read();
        //            System.out.println(Utility.hex(line));
        //        } catch (IOException e) {
        //        }
        Matcher first = Pattern.compile("@first-cp=\"([0-9A-Fa-f]+)\"").matcher("");
        Matcher last = Pattern.compile("@last-cp=\"([0-9A-Fa-f]+)\"").matcher("");
        Matcher only = Pattern.compile("@cp=\"([0-9A-Fa-f]+)\"").matcher("");
        XMLFileReader.loadPathValues(ICANN_DIR
                + XML_DATA, data, false);
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
        DATA2TYPE.add(onlyCp, Identifier_Type.recommended);
    }

    private static Identifier_Type getIdentifierType(int cp, String dataLine) {
        try {
            int spacePos = dataLine.indexOf(' ');
            String firstWord = spacePos < 0 ? dataLine : dataLine.substring(0, spacePos);

            if (firstWord.equalsIgnoreCase("contexto")) {
                return Identifier_Type.inclusion;
            } else if (firstWord.equalsIgnoreCase("punctuation")
                    || firstWord.equalsIgnoreCase("symbol")
                    || firstWord.equalsIgnoreCase("deferred")
                    || firstWord.equalsIgnoreCase("religious")
                    || firstWord.equalsIgnoreCase("unstable")
                    || firstWord.equalsIgnoreCase("use")
                    || firstWord.equalsIgnoreCase("numeric")
                    || firstWord.equalsIgnoreCase("homoglyph")) {
                REMAP.add(cp, "technical\t← " + dataLine);
                return Identifier_Type.technical;
            } else if (firstWord.equalsIgnoreCase("limited")) {
                return Identifier_Type.uncommon_use;
            } else if (firstWord.equalsIgnoreCase("Dagbani")) {
                REMAP.add(cp, "ignored " + dataLine);
                return null;
            }
            Identifier_Type identifierType = Identifier_Type.fromString(firstWord);
            return identifierType;
        } catch (Exception e) {
            throw new IllegalArgumentException(Utility.hex(cp)
                    + "\t" + getName(cp)
                    + "\t" + dataLine);
        }
    }

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
        showValues("values.txt", DATA2TYPE, Identifier_Type.recommended);

        String path = Settings.UnicodeTools.getDataPathStringForLatestVersion("security");
        XIDModifications xidModOld = new XIDModifications(path);
        UnicodeMap<Set<Identifier_Type>> xidMod = xidModOld.getType();

        UnicodeMap<Set<String>> cldrChars = CLDRCharacterUtility.getCLDRCharacters();

        UnicodeMap<Pair<Identifier_Type, Identifier_Type>> diff = new UnicodeMap<>();
        for (EntryRange x : new UnicodeSet("[[:age=6.3:]-[[:nd:][:cn:][:co:][:cs:][:cwcf:]]]").ranges()) {
            for (int i = x.codepoint; i <= x.codepointEnd; ++i) {
                Set<Identifier_Type> unicodeSet = xidMod.get(i);
                Identifier_Type unicode = unicodeSet.iterator().next();
                Identifier_Status unicodeStatus = unicode.identifierStatus;
                Set<Identifier_Type> icann = DATA2TYPE.get(i);
                Identifier_Status icannStatus = icann == null ? Identifier_Status.restricted : icann.iterator().next().identifierStatus;
                if (unicodeStatus != icannStatus) {
                    Identifier_Type icann1 = icann == null ? null : icann.iterator().next();
                    diff.put(i, Pair.of(unicode, icann1));
                }
            }
        }
        showValues("diff.txt", diff);
    }

    static final Comparator<Pair<Identifier_Type, Identifier_Type>> IDPAIR = new Comparator<Pair<Identifier_Type, Identifier_Type>>() {

        @Override
        public int compare(Pair<Identifier_Type, Identifier_Type> o1, Pair<Identifier_Type, Identifier_Type> o2) {
            Identifier_Type o11 = o1.getFirst();
            Identifier_Type o21 = o2.getFirst();
            int diff = o11.compareTo(o21);
            if (diff != 0) return diff;
            Identifier_Type o12 = o1.getSecond();
            Identifier_Type o22 = o2.getSecond();
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
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "icann/", file)) {
            TreeSet<T> sorted = new TreeSet<T>(remap2.values());
            for (T value : sorted) {
                UnicodeSet set = remap2.getKeys(value);
                out.println("\n" + value + "\t" + set.size() + "\t" + set.toPattern(false));
                if (value == skipValue) {
                    continue;
                }
                showSortedSet(out, set, null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> Script_Extensions = iup.load(UcdProperty.Script_Extensions);

    private static void showSortedSet(PrintWriter out, UnicodeSet set, Identifier_Type _type) {

        for (String script : Script_Extensions.values()) {
            UnicodeSet ss = Script_Extensions.getSet(script);
            if (ss.containsNone(set)) {
                continue;
            }
            UnicodeSet inScript = new UnicodeSet(ss).retainAll(set);
            for (EntryRange x : inScript.ranges()) {
                show(out, _type, x.codepoint, x.codepointEnd);
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

    // 1F54F; uncommon-use # BOWL OF HYGIEIA
    //  0138         ĸ      Ll      Latin       LATIN SMALL LETTER KRA
    private static void show(PrintWriter out, Identifier_Type _type, int cpStart, int cpEnd) {
        out.println(Utility.hex(cpStart)
                + (cpStart == cpEnd ? "\t" : ".." + Utility.hex(cpEnd))
                + " ; " + (_type == null ? "???" : _type)
                + "\t # " + (_type == null ? "" : "according to MSR 5 ")
                + "( " + UTF16.valueOf(cpStart)
                + (cpStart == cpEnd ? "" : ".." + UTF16.valueOf(cpEnd))
                + " ) [" + getGc(cpStart)
                + (cpStart == cpEnd ? "" : "..")
                + ", " + Script_Extensions.get(cpStart)
                + (cpStart == cpEnd ? "" : "..")
                + "] " + getNames(cpStart, cpEnd)
                );
    }

    private static String getNames(int cpStart, int cpEnd) {
        String result = getName(cpStart);
        if (cpStart == cpEnd) {
            return result;
        }
        final String name2 = getName(cpEnd);
        return result.isEmpty() && name2.isEmpty() ? "" : result + ".." + name2;
    }

    private static String getName(int cpStart) {
        String name = UCharacter.getName(cpStart);
        if (name.startsWith("CJK UNIFIED IDEOGRAPH")) {
            return "";
        }
        return name;
    }

    private static String getGc(int cpStart) {
        return UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, UCharacter.getType(cpStart), UProperty.NameChoice.SHORT);
    }

    private static void showValues(String file, UnicodeMap<Pair<Identifier_Type, Identifier_Type>> diff) {
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "icann/", file)) {
            Set<Pair<Identifier_Type, Identifier_Type>> sorted = new TreeSet<>(IDPAIR);
            sorted.addAll(diff.values());

            for (Pair<Identifier_Type, Identifier_Type> value : sorted) {
                UnicodeSet us = diff.getSet(value);
                final Identifier_Type msrValue = value.getSecond();
                final Identifier_Type uts39Value = value.getFirst();
                out.println("\n#39:" + uts39Value
                        + "\tMSR:" + msrValue
                        + "\tcount:" + us.size()
                        + "\t" + us.toPattern(false)
                        );
                showSortedSet(out, us, msrValue);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
