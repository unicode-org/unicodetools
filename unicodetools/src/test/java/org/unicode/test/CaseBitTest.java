package org.unicode.test;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.text.UCA.CEList;
import org.unicode.text.UCA.UCA;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.unittest.TestFmwkMinusMinus;
import org.unicode.unused.CaseBit;
import org.unicode.unused.CaseBit.Casing;
import org.unicode.unused.CaseBit.CasingList;

public class CaseBitTest extends TestFmwkMinusMinus {
    static ToolUnicodePropertySource propertySource =
            ToolUnicodePropertySource.make(Default.ucdVersion());

    //    public void TestKana() {
    //        UnicodeSet kata = propertySource.getSet("script=kana");
    //        UnicodeSet hira = propertySource.getSet("script=hira");
    //        UnicodeSet kana = new UnicodeSet(kata).addAll(hira).freeze();
    //        UnicodeSet small = propertySource.getSet("name=.*SMALL.*", new RegexMatcher());
    //        UnicodeSet smallKana = new UnicodeSet(kana).retainAll(small).freeze();
    //        UnicodeSet generatedSmall = new UnicodeSet();
    //        for (String s : kana) {
    //            String smallFromFunction = CaseBit.toSmallKana(s);
    //            if (!smallFromFunction.equals(s)) {
    //                generatedSmall.add(smallFromFunction);
    //            }
    //        }
    //        assertEquals("", smallKana, generatedSmall);
    //
    //        //        UnicodeProperty name = propertySource.getProperty("name");
    //        //        for (String s : smallKana) {
    //        //            String smallName = name.getValue(s.codePointAt(0));
    //        //            UnicodeSet largeNames = name.getSet(smallName.replace(" SMALL",""));
    //        //            if (largeNames.size() != 1) {
    //        //                System.out.println("Bad name match" + s);
    //        //            } else {
    //        //                System.out.println("{\"" + largeNames.iterator().next() + "\", \"" +
    // s + "\"},");
    //        //            }
    //        //        }
    //    }

    @Test
    @Disabled("FAILED: MIXED test fails: 'expected MIXED was UPPER'")
    public void TestCase() {
        assertEquals("", Casing.UNCASED, CaseBit.getPropertyCasing("$".codePointAt(0)));
        assertEquals("", Casing.LOWER, CaseBit.getPropertyCasing("a".codePointAt(0)));
        assertEquals("", Casing.MIXED, CaseBit.getPropertyCasing("Ab".codePointAt(0)));
        assertEquals("", Casing.UPPER, CaseBit.getPropertyCasing("A".codePointAt(0)));
    }

    @Test
    @Disabled("FAILED: expected 0 was 4")
    public void TestCases() {
        final UnicodeSet normal = new UnicodeSet("[:^C:]");
        final UCA uca = getUca();
        final UnicodeMap<Row.R2<Casing, Casing>> status = new UnicodeMap<Row.R2<Casing, Casing>>();
        // checkString("ǅ", uca, status);
        checkString("ᴭ", uca, status);
        checkString("㌀", uca, status);
        checkString("⅍", uca, status);
        // checkString("ぁ", uca, status);
        checkString("㌁", uca, status);
        checkString("ᾈ", uca, status);
        checkString("ￇ", uca, status);

        for (final String s : normal) {
            checkString(s, uca, status);
        }
        for (final R2<Casing, Casing> value : status.values()) {
            errln(value + "\t" + status.getSet(value).toPattern(false));
        }
    }

    UCA uca = null;

    public UCA getUca() {
        if (uca == null) {
            try {
                final Path path = Settings.UnicodeTools.getDataPathForLatestVersion("uca");
                final String file = Utility.searchDirectory(path.toFile(), "allkeys", true, ".txt");
                uca = new UCA(file, Default.ucdVersion());
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return uca;
    }

    @Test
    @Disabled("FAILS")
    public void TestDucet() {

        // get sorted list
        // we don't care for this purpose if there are duplicates
        final UCA uca = getUca();
        final Relation<CEList, String> sorted =
                Relation.of(
                        new TreeMap<CEList, Set<String>>(),
                        TreeSet.class,
                        new UTF16.StringComparator(true, false, 0));
        int regular = 0;
        int contractions = 0;
        for (int cp = 0; cp < 0x10FFFF; ++cp) {
            final int cat = Default.ucd().getCategory(cp);
            if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                continue;
            }
            if (!uca.codePointHasExplicitMappings(cp)) {
                continue;
            }
            final String s = Default.nfd().normalize(cp);
            sorted.put(uca.getCEList(s, true), s);
            regular++;
        }
        for (String s : uca.getContractions()) {
            if (UTF16.countCodePoint(s) != 1) { // don't count twice
                s = Default.nfd().normalize(s);
                sorted.put(uca.getCEList(s, true), s);
                contractions++;
            }
        }
        logln("Regular: " + regular);
        logln("Contractions: " + contractions);

        // Now check each pair, to see if the case level makes a difference
        CEList ceListOld = null;
        String stringOld = null;
        final UnicodeSet unordered = new UnicodeSet();
        for (final Entry<CEList, Set<String>> entry : sorted.keyValuesSet()) {
            final CEList ceList = entry.getKey();
            for (final String string : entry.getValue()) {
                try {
                    // if first time, or if there is a primary or secondary difference skip.
                    if (stringOld == null) {
                        continue;
                    }
                    if (ceList.compareTo(ceListOld, 0xFFFFFF00L) != 0) {
                        continue;
                    }
                    final CasingList casingOld = CaseBit.getPropertyCasing(stringOld, true);
                    final CasingList casing = CaseBit.getPropertyCasing(string, true);
                    if (casing.compareTo(casingOld) < 0) {
                        errln(
                                "Unordered"
                                        + "\told:\t"
                                        + showInfo(stringOld, ceListOld, casingOld)
                                        + "\tnew:\t"
                                        + showInfo(string, ceList, casing));
                        unordered.add(string);
                    }
                } finally {
                    ceListOld = ceList;
                    stringOld = string;
                }
            }
        }
    }

    public String showInfo(String stringOld, CEList ceListOld, CasingList casingOld) {
        return (stringOld
                + "\t'"
                + Utility.hex(stringOld)
                + "\t"
                + Default.ucd().getName(stringOld)
                + "\t"
                + ceListOld
                + "\t"
                + casingOld);
    }

    static final UnicodeSet LOWERCASE = propertySource.getSet("Lowercase=true");

    public void checkString(String s, UCA uca, UnicodeMap<R2<Casing, Casing>> status) {
        Casing unicodeCasing = CaseBit.getPropertyCasing(s.codePointAt(0));
        if (unicodeCasing == Casing.LOWER) {
            unicodeCasing = Casing.UNCASED; // don't distinguish in UCA
        }
        final CEList ceList = uca.getCEList(s, true);
        Casing composedUcaCasing = Casing.UNCASED;
        for (int i = 0; i < ceList.length(); ++i) {
            final int ce = ceList.at(i);
            final char tertiary = UCA.getTertiary(ce);
            final Casing itemUcaCasing = CaseBit.getCaseFromTertiary(tertiary);
            composedUcaCasing = composedUcaCasing.composeCasing(itemUcaCasing);
        }
        // HACK for mixed
        if (composedUcaCasing == Casing.UPPER) {
            final String nfkc = Default.nfkc().transform(s);
            if (LOWERCASE.containsSome(nfkc)) {
                composedUcaCasing = Casing.MIXED;
            }
        }
        if (composedUcaCasing == Casing.LOWER) {
            composedUcaCasing = Casing.UNCASED; // don't distinguish in UCA
        }

        if (unicodeCasing != composedUcaCasing) {
            // assertEquals(s, unicodeCasing, composedUcaCasing);
            status.put(s, Row.of(unicodeCasing, composedUcaCasing));
        }
    }
}
