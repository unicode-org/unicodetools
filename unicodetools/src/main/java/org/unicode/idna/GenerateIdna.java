package org.unicode.idna;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.BagFormatter;
import org.unicode.props.BagFormatter.NameLabel;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodeProperty.UnicodeMapProperty;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateIdna {
    static {
        // MUST BE FIRST
        GenerateIdnaTest.setUnicodeVersion();
    }
    public static final String GEN_IDNA_DIR = Settings.Output.GEN_DIR + "idna/" + Default.ucdVersion() + "/";

    // Utility.WORKSPACE_DIRECTORY + "draft/reports/tr46/data";
    private static final int MAX_STATUS_LENGTH = "disallowed_STD3_mapped".length();
    private static final boolean TESTING = true;
    private static final boolean DISALLOW_BIDI_CONTROLS = true;
    public static UnicodeSet U32;
    public static UnicodeSet U40;
    public static UnicodeSet VALID_ASCII;
    public static UnicodeSet NSTD3_ASCII;
    static ToolUnicodePropertySource properties;
    static UnicodeSet cn;
    static UnicodeSet bidiControls;
    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    static UnicodeSet IDNA2008Valid = GenerateIdnaTest.getIdna2008Valid();

    public static void main(String[] args) throws IOException {
        System.setProperty("line.separator", "\n");

        switch (args.length){
        case 0:
            break;
        case 1:
            Default.setUCD(args[0]);
            break;
        default: throw new IllegalArgumentException("Only single argument allowed:\t" + Arrays.asList(args));
        }

        U32 = new UnicodeSet("[:age=3.2:]").freeze();
        VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
        NSTD3_ASCII = new UnicodeSet("[[\\u0000-\\u007F]-[.]]").freeze();
        properties = ToolUnicodePropertySource.make(Default.ucdVersion());
        cn = properties.getSet("gc=Cn").freeze();
        bidiControls = properties.getSet("bidi_control=true");



        final UnicodeMap<Row.R2<IdnaType, String>> mappingTable = createMappingTable(true);
        final UnicodeMap<Row.R2<IdnaType, String>> mappingTableNSTD3 = createMappingTable(false);
        {
            final UnicodeMap<String> mappings = new UnicodeMap<String>();
            final UnicodeMap<IdnaType> types = new UnicodeMap<IdnaType>();
            StringPrepData.getIdna2003Tables(mappings, types, true);

            verifyDifferences(mappings, types, mappingTable);
        }

        final UnicodeSet validSet = new UnicodeSet();
        final UnicodeSet disallowedSet = new UnicodeSet();
        final UnicodeSet mappedSet = new UnicodeSet();

        final UnicodeMap<IdnaType> rawStatus = new UnicodeMap<IdnaType>();
        final UnicodeMap<IdnaType> filteredStatus = new UnicodeMap<IdnaType>();

        final UnicodeMap<String> stringMappingTable = new UnicodeMap<String>();

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            final Row.R2<IdnaType, String> value = mappingTable.get(cp);
            final Row.R2<IdnaType, String> valueNstd3 = mappingTableNSTD3.get(cp);
            if (value == null || valueNstd3 == null) {
                throw new IllegalArgumentException("Expected value for " + Utility.hex(cp));
            }
            final IdnaType status = value.get0();
            rawStatus.put(cp, status);
            Age_Values age = AGE.get(cp);
            if (age.compareTo(Age_Values.V3_2) > 0 && age != Age_Values.Unassigned) {
                filteredStatus.put(cp, status == IdnaType.deviation || status == IdnaType.ignored ? IdnaType.mapped : status);
            }

            final IdnaType statusNstd3 = valueNstd3.get0();
            String endStatus = statusNstd3 == status ? status.toString() : status + "_STD3_" + statusNstd3;
            final String mapping = value.get1();
            final String mappingNstd3 = valueNstd3.get1();
            // if mapped, add info
            if (status == IdnaType.mapped || status == IdnaType.deviation || statusNstd3 == IdnaType.mapped || statusNstd3 == IdnaType.deviation) {
                endStatus += Utility.repeat(" ", MAX_STATUS_LENGTH-endStatus.length()) + " ; ";
                if (mapping != null && mapping.length() != 0) {
                    endStatus += Utility.hex(mapping);
                } else if (mappingNstd3 != null && mappingNstd3.length() != 0) {
                    endStatus += Utility.hex(mappingNstd3);
                }
            } else {
                if (mapping != null) {
                    throw new IllegalArgumentException("bad mapping:\t" + value);
                }
            }

            if (status == IdnaType.valid && !IDNA2008Valid.contains(cp) && cp != '.') {
                endStatus += Utility.repeat(" ", MAX_STATUS_LENGTH-endStatus.length()) + " ;      ; NV8";
            } else if (Idna2008.GRANDFATHERED_VALID.contains(cp)) {
                endStatus += Utility.repeat(" ", MAX_STATUS_LENGTH-endStatus.length()) + " ;      ; XV8";
            }
            stringMappingTable.put(cp, endStatus);

            if (!U32.contains(cp) && !UNASSIGNED.contains(cp)) {
                switch(status) {
                case mapped:
                case ignored:
                case deviation:
                    mappedSet.add(cp);
                    break;
                case disallowed:
                    disallowedSet.add(cp);
                    break;
                case valid:
                    validSet.add(cp);
                    break;
                default:
                    throw new IllegalAccessError();
                }
            }
        }
        filteredStatus.freeze();
        rawStatus.freeze();

        writeDataFile(stringMappingTable);

        showAge(rawStatus);
        showAge("ValidSet", validSet);

        //        System.out.println("After running, copy the data file to the jsp directory, and run org.unicode.jsptest.TestGenerate to generate the differences table.\n" +
        //        "Then run org.unicode.jsptest.TestUt6s46 with the argument 'generate' to generate the tests.");

        final UnicodeSet IDNA2008Disallowed = new UnicodeSet(IDNA2008Valid).complement().freeze();

        //        showSet("Valid\tValid", validSet, IDNA2008Valid);
        //        showSet("Disallowed\tDisallowed", disallowedSet, IDNA2008Disallowed);
        //        showSet("Valid\tDisallowed", validSet, IDNA2008Disallowed);
        //        showSet("Mapped/Ignored\tDisallowed", mappedSet, IDNA2008Disallowed);

        showSet("Valid\tValid", filteredStatus.getSet(IdnaType.valid), IDNA2008Valid);
        showSet("Disallowed\tDisallowed", filteredStatus.getSet(IdnaType.disallowed), IDNA2008Disallowed);
        showSet("Valid\tDisallowed", filteredStatus.getSet(IdnaType.valid), IDNA2008Disallowed);
        showSet("Mapped/Ignored\tDisallowed", filteredStatus.getSet(IdnaType.mapped), IDNA2008Disallowed);

        int missing = 0;
        for (Age_Values age : AGE.values()) {
            if (age.compareTo(Age_Values.V6_3) > 0 && age != Age_Values.Unassigned) {
                UnicodeSet ageSet = AGE.getSet(age);
                missing += ageSet.size();
            }
        }

        System.out.println("Missing IDNA2008 Official Values: " + missing);
    }

    static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make();
    static final UnicodeMap<Age_Values> AGE = IUP.loadEnum(UcdProperty.Age, Age_Values.class);
    static final UnicodeSet UNASSIGNED = AGE.getSet(Age_Values.Unassigned);
    static {
        for (Age_Values age : AGE.values()) {
            UnicodeSet ageSet = AGE.getSet(age);
            System.out.println(age + "\t" + ageSet.size());
        }
    }

    private static void showSet(String title, UnicodeSet validSet, UnicodeSet iDNA2008Valid2) {
        final UnicodeSet intersect = new UnicodeSet(validSet).retainAll(iDNA2008Valid2);
        System.out.println(intersect.size() + "\t" + title + intersect.toPattern(false));
        //showAge(title, intersect);
    }

    private static void showAge(String title, final UnicodeSet intersect) {
        for (Age_Values age : Age_Values.values()) {
            UnicodeSet ageSet = AGE.getSet(age);
            if (intersect.containsSome(ageSet)) {
                UnicodeSet intersect2 = new UnicodeSet(ageSet).retainAll(intersect);
                System.out.println("\t" + age + "\t" + intersect2.size() + "\t" + title + intersect2.toPattern(false));
            }
        }
    }

    private static <T> void showAge(final UnicodeMap<T> map) {
        Set<T> values = map.values();
        for (Age_Values age : Age_Values.values()) {
            UnicodeSet ageSet = AGE.getSet(age);
            System.out.println("Age: " + age);
            for (T idnaType : values) {
                UnicodeSet intersect = map.getSet(idnaType);
                if (intersect.containsSome(ageSet)) {
                    UnicodeSet intersect2 = new UnicodeSet(ageSet).retainAll(intersect);
                    System.out.println("\t" + idnaType + "\t" + intersect2.size() + intersect2.toPattern(false));
                }
            }
        }
    }

    private static void verifyDifferences(UnicodeMap<String> mappings, UnicodeMap<IdnaType> types, UnicodeMap<Row.R2<IdnaType, String>> mappingTable) {
        System.out.println("Verifying Differences");
        final UnicodeMap<Row.R3<String, R2<IdnaType, String>, R2<IdnaType, String>>> diff = new UnicodeMap<Row.R3<String, R2<IdnaType, String>, R2<IdnaType, String>>>();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!U32.contains(i)) {
                continue;
            }
            final R2<IdnaType, String> data46 = mappingTable.get(i);
            final R2<IdnaType, String> data2003 = Row.of(types.get(i), mappings.get(i));
            if (!equals(data46, data2003)) {
                diff.put(i, Row.of(
                        (data2003.get0() == IdnaType.disallowed ? "D" : "-")
                        + (data46.get0() == IdnaType.disallowed ? "D" : "-")
                        , data2003, data46));
                //System.out.println(Utility.hex(i) + " - ust46: " + data46 + "\t idna2003: " + data2003 + "\t" + UCharacter.getExtendedName(i) + status);
            }
        }
        for (final R3<String, R2<IdnaType, String>, R2<IdnaType, String>> item : new TreeSet<R3<String, R2<IdnaType, String>, R2<IdnaType, String>>>(diff.values())) {
            final UnicodeSet set = diff.getSet(item);
            final String ok = item.get0();
            final R2<IdnaType, String> data2003 = item.get1();
            final R2<IdnaType, String> data46 = item.get2();

            System.out.println(ok + "\tidna2003: " + data2003 + "\tust46: " + data46 + "\t" + set.size() + "\t" + set);
        }
    }

    private static boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static UnicodeMap<Row.R2<IdnaType, String>> createMappingTable(boolean STD3) {

        final UnicodeMap<String> nfkc_cfMap = properties.getProperty("NFKC_CF").getUnicodeMap();
        final UnicodeMap<String> baseMapping = new UnicodeMap<String>().putAll(nfkc_cfMap);
        baseMapping.put(0xFF0E, "\u002E");
        baseMapping.put(0x3002, "\u002E");
        baseMapping.put(0xFF61, "\u002E");
        baseMapping.putAll(bidiControls, null);
        baseMapping.freeze();


        final UnicodeSet labelSeparator = new UnicodeSet("[\\u002E \\uFF0E \\u3002 \\uFF61]").freeze();

        final UnicodeSet baseValidSet = new UnicodeSet(0,0x10FFFF)
                .removeAll(properties.getSet("Changes_When_NFKC_Casefolded=true"))
                .removeAll(properties.getSet("gc=Cc"))
                .removeAll(properties.getSet("gc=Cf"))
                .removeAll(cn)
                .removeAll(properties.getSet("gc=Co"))
                .removeAll(properties.getSet("gc=Cs"))
                .removeAll(properties.getSet("gc=Zl"))
                .removeAll(properties.getSet("gc=Zp"))
                .removeAll(properties.getSet("gc=Zs"))
                .removeAll(properties.getSet("Block=Ideographic_Description_Characters"))
                .removeAll(new UnicodeSet("[\\u0000-\\u007F]"))
                //.addAll(0x200c, 0x200d)
                .addAll(STD3 ? VALID_ASCII : NSTD3_ASCII).freeze()
                ;

        System.out.println(STD3 + " Base Valid Set & nfkcqc=n" + new UnicodeSet("[:nfkcqc=n:]").retainAll(baseValidSet));

        final R2<UnicodeSet, UnicodeSet> baseExclusionSetInfo = computeBaseExclusionSet(baseMapping, baseValidSet, STD3);
        final UnicodeSet disallowedExclusionSet = baseExclusionSetInfo.get0();
        final UnicodeSet mappingChanged = baseExclusionSetInfo.get1();
        final UnicodeSet baseExclusionSet = new UnicodeSet(disallowedExclusionSet).addAll(mappingChanged);
        final UnicodeSet baseExclusionSet2 = new UnicodeSet("[" +
                "\\u04C0 \\u10A0-\\u10C5 \\u2132 \\u2183" +
                "\\U0002F868  \\U0002F874 \\U0002F91F \\U0002F95F \\U0002F9BF" +
                "\u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806 \uFFFC \uFFFD" +
                "[\\u200E\\u200F\\u202A-\\u202E\\u2061-\\u2063\\u206A-\\u206F\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F]" +
                "[\u200B\u2060\uFEFF]" +
                "]").freeze(); //.addAll(cn)

        System.out.println(STD3 + " base valid set:\t" + baseValidSet);
        System.out.println(STD3 + " computed base exclusion disallowed:\t" + disallowedExclusionSet);
        System.out.println(STD3 + " computed base exclusion mapping changed:\t" + mappingChanged);

        if (false && !baseExclusionSet.equals(baseExclusionSet2)) {
            System.out.println("computed-static:\t" + new UnicodeSet(baseExclusionSet).removeAll(baseExclusionSet2));
            System.out.println("static-computed:\t" + new UnicodeSet(baseExclusionSet2).removeAll(baseExclusionSet));
            throw new IllegalArgumentException();
        }

        System.out.println(STD3 + " ***Overlap with baseValidSet and baseExclusionSet:\t" + new UnicodeSet(
                baseValidSet).retainAll(baseExclusionSet));

        final UnicodeSet deviationSet = new UnicodeSet("[\u200C \u200D \u00DF \u03C2]").freeze(); // \u200C \u200D

        /**
         * 1. If the code point is in the deviation set the status is deviation and
         * the mapping value is the base mapping value for that code point<br>
         * 2. Otherwise, if (a) the code point is in the base exclusion set, or if
         * (b) any code point in its base mapping value is not in the base valid set
         * the status is disallowed and there is no mapping value in the table<br>
         * 3. Otherwise, if the base mapping value is an empty string the status is
         * ignored and there is no mapping value in the table<br>
         * 4. Otherwise, if the base mapping value is the same as the code point the
         * status is valid and there is no mapping value in the table<br>
         * 5. Otherwise, the status is mapping and the mapping value is the base
         * mapping value for that code point
         */
        final UnicodeMap<Row.R2<IdnaType, String>> mappingTable = new UnicodeMap<R2<IdnaType, String>>();
        final R2<IdnaType, String> disallowedResult = Row.of(IdnaType.disallowed, (String)null);
        final R2<IdnaType, String> ignoredResult = Row.of(IdnaType.ignored, (String)null);
        final R2<IdnaType, String> validResult = Row.of(IdnaType.valid, (String)null);

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            if (TESTING && cp == 0x10C7) {
                System.out.println("??TEST");
            }
            final String cpString = UTF16.valueOf(cp);
            Row.R2<IdnaType, String> result;
            String baseMappingValue = baseMapping.get(cp);
            if (baseMappingValue == null) {
                baseMappingValue = cpString;
            }
            if (deviationSet.contains(cp)) {
                result = Row.of(IdnaType.deviation, baseMappingValue);
            } else if (baseExclusionSet.contains(cp)
                    || false && bidiControls.contains(cp)) { // Step 5.
                result = disallowedResult;
            } else if (!labelSeparator.contains(cp) && !baseValidSet.containsAll(baseMappingValue)) {
                result = disallowedResult;
            } else if (cn.contains(cp)) { // do this in a different order just for debuggin
                result = disallowedResult;
            } else if (baseMappingValue.length() == 0) {
                result = ignoredResult;
            } else if (baseMappingValue.equals(cpString)) {
                result = validResult;
            } else {
                result = Row.of(IdnaType.mapped, baseMappingValue);
            }
            //if (0==(cp&0xFFF)) System.out.println(cp + " = " + result);
            mappingTable.put(cp, result);
        }
        final UnicodeSet excluded = new UnicodeSet();
        do {
            excluded.clear();
            final UnicodeSet validSet = mappingTable.getSet(validResult);
            final UnicodeSet disallowedSet = mappingTable.getSet(disallowedResult);
            final UnicodeSet ignoredSet = mappingTable.getSet(ignoredResult);
            for (final String valid : validSet) {
                final String nfd = Default.nfd().normalize(valid);
                if (!validSet.containsAll(nfd)) {
                    excluded.add(valid);
                }
            }
            final UnicodeSet mappedSet = new UnicodeSet(0,0x10FFFF).removeAll(validSet)
                    .removeAll(disallowedSet).removeAll(ignoredSet);
            for (final String mapped : mappedSet) {
                final R2<IdnaType, String> mappedValue = mappingTable.get(mapped);
                final String mapResult = mappedValue.get1();
                final String nfd = Default.nfd().normalize(mapResult);
                if (!validSet.containsAll(nfd)) {
                    excluded.add(mapped);
                }
            }
            mappingTable.putAll(excluded, disallowedResult);
            System.out.println(STD3 + " ***InvalidDecomposition Exclusion: " + excluded);
        } while (excluded.size() != 0);

        // detect errors, where invalid character doesn't have at least one invalid in decomposition
        final UnicodeSet invalidSet = mappingTable.getSet(disallowedResult).freeze();
        for (final String valid : invalidSet) {
            final String nfd = Default.nfd().normalize(valid);
            if (invalidSet.containsNone(nfd)) {
                System.out.println("SUSPICIOUS: " + valid + "\t" + nfd);
            }
        }

        return mappingTable.freeze();
    }


    private static R2<UnicodeSet,UnicodeSet> computeBaseExclusionSet(UnicodeMap<String> baseMapping, UnicodeSet baseValidSet, boolean STD3) {
        final Idna Idna2003Data = STD3 ? Idna2003.SINGLETON : Idna2003.SINGLETON_NSTD3;
        final UnicodeSet disallowed = new UnicodeSet();
        final UnicodeSet mappingChanged = new UnicodeSet();
        for (final UnicodeSetIterator it = new UnicodeSetIterator(U32); it.next();) {
            final int i = it.codepoint;
            if (TESTING && i == 0x41) {
                System.out.println("??TEST??");
            }
            final IdnaType type = Idna2003Data.types.get(i);
            switch (type) {
            case disallowed:
                if (baseValidSet.contains(i)) {
                    disallowed.add(i);
                    break;
                }
                final String base2 = baseMapping.get(i);
                if (base2 != null && baseValidSet.containsAll(base2)) {
                    disallowed.add(i);
                }
                break;
            default:
                String idna2003 = Idna2003Data.mappings.get(i);
                String base = baseMapping.get(i);
                if (base == idna2003) {
                    continue;
                }
                if (base == null) {
                    base = UTF16.valueOf(i);
                }
                if (idna2003 == null) {
                    idna2003 = UTF16.valueOf(i);
                }
                if (!base.equals(idna2003)) {
                    mappingChanged.add(i);
                }
                break;
            }
        }
        return Row.of(disallowed.freeze(), mappingChanged.freeze());
    }

    private static void writeDataFile(UnicodeMap<String> mappingTable) throws IOException {
        final String filename = "IdnaMappingTable-" + Default.ucdVersion() + ".txt";
        final String unversionedFileName = "IdnaMappingTable.txt";
        final PrintWriter writer = FileUtilities.openUTF8Writer(GEN_IDNA_DIR, unversionedFileName);

        writer.println(Utility.getBaseDataHeader(
            unversionedFileName,
            46,
            "Unicode IDNA Compatible Preprocessing",
            Default.ucdVersion()));
//        writer.println(
//                "#\n" +
//                        "# Unicode IDNA Compatible Preprocessing (UTS #46)\n" +
//                "# For documentation, see http://www.unicode.org/reports/tr46/\n");

        final UnicodeProperty ASSIGNED = new UnicodeProperty.SimpleProperty() {
            @Override
            protected String _getValue(int codepoint) {
                return cn.contains(codepoint) ? "Cn" : "As";
            }
        };
        final UnicodeProperty age = properties.getProperty("age");
        //        UnicodeMap ageValue = age0.getUnicodeMap();
        //        UnicodeSet unassigned = ageValue.getSet("unassigned");
        //        ageValue.putAll(unassigned, "n/a");
        //        UnicodeMapProperty age = new UnicodeProperty.UnicodeMapProperty().set(ageValue);

        final NameLabel name = new BagFormatter.NameLabel(properties);
        final BagFormatter bf = new BagFormatter();
        bf.setLineSeparator("\n");
        bf.setLabelSource(age);
        bf.setRangeBreakSource(ASSIGNED);
        bf.setShowCount(false);
        bf.setNameSource(name);

        final UnicodeMapProperty prop = new UnicodeProperty.UnicodeMapProperty().set(mappingTable);
        bf.setValueSource(prop);
        bf.setValueWidthOverride(MAX_STATUS_LENGTH + 16);
        bf.setLabelWidthOverride(3);
        String showSetNames = bf.showSetNames(mappingTable.keySet());
        if (showSetNames.contains("\r")) {
            throw new IllegalArgumentException("Bad, CR");
        }
        writer.println(showSetNames);
        writer.close();
    }
}
