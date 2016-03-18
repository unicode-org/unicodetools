package org.unicode.propstest;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CompareExemplarsToIdmod {
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final  UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);

    static final  CLDRConfig testInfo = CLDRConfig.getInstance();

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    static final  Factory cldrFactory = testInfo.getCldrFactory();
    static final  Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();
    static final  Normalizer2 nfd = Normalizer2.getNFDInstance();
    static final  Normalizer2 nfkc = Normalizer2.getNFKCInstance();

    public static void main(String[] args) {
        UnicodeMap<String> statusMap = iup.load(UcdProperty.Identifier_Status);
        UnicodeMap<String> typeMap = iup.load(UcdProperty.Identifier_Type);
        UnicodeMap<String> xidContinue = iup.load(UcdProperty.XID_Continue);


        CLDRFile english = testInfo.getEnglish();
        LanguageTagParser ltp = new LanguageTagParser();

        Set<String> nonapprovedLocales = new LinkedHashSet();
        UnicodeMap<String> restricted = new UnicodeMap();
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
                System.out.println(localeName + "\tSuspicious characters; never in CLDR data: ");
                for (String cp : suspicious) {
                    System.out.println("\t" + getCodeAndName(cp));
                }
            }
        }
        for (String cp : restricted) {
            System.out.println(Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp) + "\t" + restricted.get(cp));
        }
        for (String locale :  nonapprovedLocales) {
            System.out.println("No approved exemplars for " + english.getName(locale) + "\t" + locale);
        }
    }
    
    public static String getCodeAndName(String cp) {
        return Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp);
    }

}
