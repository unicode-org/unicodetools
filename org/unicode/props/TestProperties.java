package org.unicode.props;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Numeric_Type_Values;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestProperties extends TestFmwk {
    public static void main(String[] args) {
        new TestProperties().run(args);
    }

    // TODO generate list of versions, plus 'latest'

    static IndexUnicodeProperties iup = IndexUnicodeProperties.make("6.3");

    public void TestIdn() {
        show(UcdProperty.Idn_Status);
        show(UcdProperty.Idn_2008);
        show(UcdProperty.Idn_Mapping);
    }

    public void TestIdmod() {
        show(UcdProperty.Id_Mod_Status);
        show(UcdProperty.Id_Mod_Type);
        show(UcdProperty.Confusable_MA);
    }

    public void TestExemplarsAgainstIdmod() {
        UnicodeMap<String> statusMap = iup.load(UcdProperty.Id_Mod_Status);
        UnicodeMap<String> typeMap = iup.load(UcdProperty.Id_Mod_Type);
        UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);
        UnicodeMap<String> xidContinue = iup.load(UcdProperty.XID_Continue);

        Normalizer2 nfd = Normalizer2.getNFDInstance();
        Normalizer2 nfkc = Normalizer2.getNFKCInstance();

        TestInfo testInfo = TestInfo.getInstance();
        Factory cldrFactory = testInfo.getCldrFactory();
        CLDRFile english = testInfo.getEnglish();
        LanguageTagParser ltp = new LanguageTagParser();

        Set<String> nonapprovedLocales = new LinkedHashSet();
        UnicodeMap<String> restricted = new UnicodeMap();
        Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();
        UnicodeSet allowedHangulTypes = new UnicodeSet("[ᄀ-ᄒ ᅡ-ᅵ ᆨ-ᇂ]").freeze();

        for (String locale : cldrFactory.getAvailable()) {
            if (defaultContents.contains(locale) 
                    || !ltp.set(locale).getRegion().isEmpty()
                    || ltp.getScript().equals("Dsrt")) {
                continue;
            }
            CLDRFile f = cldrFactory.make(locale, false, DraftStatus.approved);
            UnicodeSet uset = f.getExemplarSet("", WinningChoice.WINNING);
            if (uset == null) {
                nonapprovedLocales.add(locale);
                continue;
            }
            String localeName = english.getName(locale) + " (" + locale + ")";
            UnicodeSet flattened = new UnicodeSet();
            for (String cp : uset) {
                flattened.addAll(nfd.normalize(cp));
            }
            UnicodeSet suspicious = new UnicodeSet();
            for (String cp : flattened) {
                if (!nfkc.isNormalized(cp)) {
                    continue;
                }
                if (!"Yes".equals(xidContinue.get(cp))) {
                    continue;
                }
                if (allowedHangulTypes.contains(cp)) {
                    continue;
                }
                if (!"allowed".equals(statusMap.get(cp))) {
                    String s = restricted.get(cp);
                    String info = localeName + " " + typeMap.get(cp);
                    restricted.put(cp, s == null ? info : s + "; " + info);
                    suspicious.add(cp);
                }
            }
            if (!suspicious.isEmpty()) {
                for (String path : f){
                    if (path.contains("character")) {
                        continue;
                    }
                    String value = f.getStringValue(path);
                    suspicious.removeAll(nfd.normalize(value));
                    suspicious.removeAll(nfd.normalize(UCharacter.toUpperCase(ULocale.ROOT, value)));
                    suspicious.removeAll(nfd.normalize(UCharacter.toLowerCase(ULocale.ROOT, value)));
                }
            }
            if (!suspicious.isEmpty()) {
                logln(localeName + "\tSuspicious characters; never in CLDR data: ");
                for (String cp : suspicious) {
                    logln("\t" + Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp));
                }
            }
        }
        for (String cp : restricted) {
            System.out.println(Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp) + "\t" + restricted.get(cp));
        }
        for (String locale :  nonapprovedLocales) {
            logln("No approved exemplars for " + english.getName(locale) + "\t" + locale);
        }
    }

    private UnicodeMap<String> show(UcdProperty ucdProperty) {
        UnicodeMap<String> propMap = iup.load(ucdProperty);
        int count = 0;
        for (String value : propMap.values()) {
            if (++count > 50) {
                logln("...");
                break;
            }
            UnicodeSet set = propMap.getSet(value);
            logln(ucdProperty + "\t" + value + "\t" + set);
        }
        return propMap;
    }

    public void TestValues() {
        for (final UcdProperty prop : UcdProperty.values()) {
            logln(prop + "\t" + prop.getNames() + "\t" + prop.getEnums());
            //            Collection<Enum> values = PropertyValues.valuesOf(prop);
            //            logln("values: " + values);
        }
        for (final UcdPropertyValues.General_Category_Values prop : UcdPropertyValues.General_Category_Values.values()) {
            logln(prop + "\t" + prop.getNames());
            //            Collection<Enum> values = PropertyValues.valuesOf(prop);
            //            logln("values: " + values);
        }

        final UcdPropertyValues.General_Category_Values q = UcdPropertyValues.General_Category_Values.Unassigned;
        logln(q.getNames().toString());

        //        Enum x = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
        //        //Bidi_Mirrored_Values y = Properties.Bidi_Mirrored_Values.No;
        //        Enum z = PropertyValues.forValueName(UcdProperty.Bidi_Mirrored, "N");
        //        Enum w = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
        //        logln(x + " " + z + " " + w);
    }

    public void TestNumbers() {
        for (final Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned) { //  || age.compareTo(Age_Values.V4_0) < 0
                continue;
            }
            final PropertyNames<Age_Values> names = age.getNames();
            //logln(names.getShortName());
            final IndexUnicodeProperties props = IndexUnicodeProperties.make(names.getShortName());
            final UnicodeMap<String> gc = props.load(UcdProperty.General_Category);
            final UnicodeMap<String> nt = props.load(UcdProperty.Numeric_Type);
            final UnicodeSet gcNum = new UnicodeSet()
            .addAll(gc.getSet(General_Category_Values.Decimal_Number.toString()))
            .addAll(gc.getSet(General_Category_Values.Letter_Number.toString()))
            .addAll(gc.getSet(General_Category_Values.Other_Number.toString()))
            ;
            final UnicodeSet ntNum = new UnicodeSet()
            .addAll(nt.getSet(Numeric_Type_Values.Decimal.toString()))
            .addAll(nt.getSet(Numeric_Type_Values.Digit.toString()))
            .addAll(nt.getSet(Numeric_Type_Values.Numeric.toString()))
            ;
            UnicodeSet diff;
            //            diff = new UnicodeSet(ntNum).removeAll(gcNum);
            //            logln(age + ", nt-gc:N" + diff);
            diff = new UnicodeSet(gcNum).removeAll(ntNum);
            logln(age + ", gc:N-nt" + diff);
        }

    }


}
