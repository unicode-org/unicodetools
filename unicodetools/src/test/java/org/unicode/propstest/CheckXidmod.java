package org.unicode.propstest;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.MissingStatus;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.propstest.TestProperties.ExemplarExceptions;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CheckXidmod {
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);

    private static final  CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    private static final  Factory cldrFactory = testInfo.getCldrFactory();
    private static final  Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();
    private static final  Normalizer2 nfd = Normalizer2.getNFDInstance();
    private static final  Normalizer2 nfkc = Normalizer2.getNFKCInstance();

    public static void main(String[] args) {
        new CheckXidmod().TestBuildingIdModType();
    }
    public void TestBuildingIdModType() {
        UnicodeSet LGC = new UnicodeSet("[[:sc=Latin:][:sc=Greek:][:sc=Cyrl:]]").freeze();
        UnicodeSet AE = new UnicodeSet("[[:sc=Arab:][:sc=Ethiopic:]]").freeze();
        UnicodeMap<String> simulateType = new UnicodeMap();
        // we load the items in reverse order, so that we override with more "powerful" categories
        //UnicodeMap<String> Script_Extensions = iup.load(UcdProperty.Script_Extensions);
        // now simulate
        //ScriptCategories sc = new ScriptCategories();

        // start big, and whittle down
        simulateType.putAll(0,0x10FFFF, "not-CLDR");

        UnicodeMap<String> cldrExemplars = getCldrExemplars();

        simulateType.putAll(cldrExemplars.keySet(), "recommended");
        // hack for comparison
        UnicodeMap<String> Unified_Ideograph = iup.load(UcdProperty.Unified_Ideograph);
        simulateType.putAll(Unified_Ideograph.getSet("Yes"), "recommended");


        // Script Metadata

        // we do it this way so that if any of the scripts for a character are recommended, it is recommended.
        ScriptMetadata smd = new ScriptMetadata();

        //        UnicodeMap<String> Script = iup.load(UcdProperty.Script);
        //        for (String script : Script.values()) {
        //            UnicodeSet chars = Script.getSet(script);
        //            Info info = ScriptMetadata.getInfo(script);
        //            switch (info.idUsage) {
        //            case LIMITED_USE:
        //            case ASPIRATIONAL:
        //                simulateType.putAll(chars, "limited_use");
        //                break;
        //            case EXCLUSION:
        //                simulateType.putAll(chars, "historic");
        //                break;
        //            }
        //        }
        // this works because items with real values below will be Common above, and have no effect
        UnicodeMap<String> Script_Extensions = iup.load(UcdProperty.Script_Extensions);
        for (String value : Script_Extensions.values()) {
            IdUsage bestIdUsage = getBestIdUsage(value);
            UnicodeSet chars = Script_Extensions.getSet(value);
            switch (bestIdUsage) {
            case LIMITED_USE:
            case ASPIRATIONAL:
                //simulateType.putAll(chars, "limited-use");
                simulateType.putAll(chars, "historic");
                break;
            case EXCLUSION:
                simulateType.putAll(chars, "historic");
                break;
                //            case RECOMMENDED:
                //                simulateType.putAll(chars, "historic");
                //                break;
            }
        }

        simulateType.putAll(iup.load(UcdProperty.Deprecated).getSet("Yes"), "obsolete");

        simulateType.putAll(iup.load(UcdProperty.XID_Continue).getSet("No"), "not-xid");


        simulateType.putAll(iup.load(UcdProperty.NFKC_Quick_Check).getSet("No"), "not-NFKC");

        UnicodeMap<String> General_Category = iup.load(UcdProperty.General_Category);
        UnicodeSet White_Space = iup.load(UcdProperty.White_Space).getSet("Yes");
        for (General_Category_Values gc : EnumSet.of(
                General_Category_Values.Unassigned,
                General_Category_Values.Private_Use,
                General_Category_Values.Surrogate,
                General_Category_Values.Control
                )) {
            UnicodeSet set = General_Category.getSet(gc.toString());
            set = new UnicodeSet(set).removeAll(White_Space);
            simulateType.putAll(set, "not-chars");
        }
        simulateType.putAll(iup.load(UcdProperty.Default_Ignorable_Code_Point).getSet("Yes"), "default-ignorable");
        simulateType.putAll(new UnicodeSet("['\\-.\\:·͵֊׳״۽۾་‌‍‐’‧゠・_]"), "inclusion");
        // map technical to historic
        UnicodeMap<String> typeMap = new UnicodeMap().putAll(iup.load(UcdProperty.Identifier_Type));
        typeMap.putAll(typeMap.getSet("technical"), "not-CLDR");
        typeMap.putAll(typeMap.getSet("limited-use"), "not-CLDR");
        typeMap.putAll(typeMap.getSet("historic"), "not-CLDR");

        TreeSet<String> values = new TreeSet(typeMap.values());
        values.addAll(simulateType.values());

        for (String type : typeMap.values()) {
            UnicodeSet idmodSet = typeMap.getSet(type);
            UnicodeSet simSet = simulateType.getSet(type);
            UnicodeSet idmodMinusSim = new UnicodeSet(idmodSet).removeAll(simSet);
            UnicodeSet same = new UnicodeSet(idmodSet).retainAll(simSet);
            UnicodeSet simMinusIdmod = new UnicodeSet(simSet).removeAll(idmodSet);
            logln(type 
                    + "\tsame:\t" + same.size()
                    + "\n\t\tsim-idmod:\t" + simMinusIdmod.size() 
                    + "\t" + simMinusIdmod.toPattern(false)
                    + "\n\t\tidmod-sim:\t" + idmodMinusSim.size() 
                    + "\t" + idmodMinusSim.toPattern(false));
        }
        UnicodeSet typeOk = new UnicodeSet(typeMap.getSet("inclusion"))
        .addAll(typeMap.getSet("recommended")).freeze();
        UnicodeSet simOk = new UnicodeSet(simulateType.getSet("inclusion"))
        .addAll(simulateType.getSet("recommended")).freeze();
        UnicodeSet simMinusType = new UnicodeSet(simOk).removeAll(typeOk);
        UnicodeSet typeMinusSim = new UnicodeSet(typeOk).removeAll(simOk);
        showDiff(cldrExemplars, simMinusType);

        logln("Current - new, Latin+Greek+Cyrillic");
        showDiff(new UnicodeSet(typeMinusSim).retainAll(LGC));
        UnicodeSet x = new UnicodeSet(typeMinusSim).removeAll(LGC);
        logln("Current - new, Arab+Ethiopic");
        showDiff(new UnicodeSet(x).retainAll(AE));
        logln("Current - new, Remainder");
        showDiff(new UnicodeSet(x).removeAll(AE));
    }
    private void logln(String string) {
        System.out.println(string);        
    }
    private void showDiff(UnicodeSet target) {
        logln(target.toPattern(false));
        for (int i = 0; i < UScript.CODE_LIMIT; ++i) {
            UnicodeSet script = new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, i);
            if (script.containsSome(target)) {
                UnicodeSet diff = new UnicodeSet(target).retainAll(script);
                logln(UScript.getName(i) + "\thttp://unicode.org/cldr/utility/list-unicodeset.jsp?abb=on&g=sc+gc+subhead&"
                        + diff.toPattern(false));
            }
        }
    }
    
    private UnicodeMap<String> getCldrExemplars() {
        LanguageTagParser ltp = new LanguageTagParser();
        UnicodeMap<String> result = new UnicodeMap<>();
        Map<LstrType, Map<String, Map<LstrField, String>>> lstreg = StandardCodes.getEnumLstreg();
        Map<String, Map<LstrField, String>> langInfo = lstreg.get(LstrType.language);
        Map<String, String> likely = SUPPLEMENTAL_DATA_INFO.getLikelySubtags();
        CoverageData coverageData = new CoverageData();

        for (String locale : cldrFactory.getAvailable()) {
            if (defaultContents.contains(locale) 
                    || !ltp.set(locale).getRegion().isEmpty()
                    || ltp.getScript().equals("Dsrt")) {
                continue;
            }
            String baseLanguage = ltp.getLanguage();
            Map<LstrField, String> info = langInfo.get(baseLanguage);
            Type langType = Iso639Data.getType(baseLanguage);
            if (langType != Type.Living) {
                if (locale.equals("eo")) {
                    logln("Retaining special 'non-living':\t" + getLanguageNameAndCode(locale));
                } else {
                    logln("Not Living:\t" + getLanguageNameAndCode(baseLanguage));
                    continue;
                }
            }
            PopulationData languageInfo = SUPPLEMENTAL_DATA_INFO.getLanguagePopulationData(baseLanguage);
            if (languageInfo == null) {
                String max = LikelySubtags.maximize(baseLanguage, likely);
                languageInfo = SUPPLEMENTAL_DATA_INFO.getLanguagePopulationData(ltp.set(max).getLanguageScript());
            }
            if (languageInfo == null) {
                logln("No literate-population data:\t" + getLanguageNameAndCode(locale));
                continue;
            }

            CLDRFile f = cldrFactory.make(locale, true, DraftStatus.approved);
            Map<Level, Double> coverage = coverageData.getData(f);
            if (languageInfo.getLiteratePopulation() < 1000000) {
                if (coverage.get(Level.MODERN) < 0.5) {
                    logln("Small literate-population:\t" + getLanguageNameAndCode(locale) + "\t" + languageInfo.getLiteratePopulation());
                    continue;
                } else {
                    logln("Retaining Small literate-population:\t" + getLanguageNameAndCode(locale) + "\t" + languageInfo.getLiteratePopulation()
                            + "\tCoverage:\t" + coverage);
                }
            }

            //CLDRFile f = cldrFactory.make(locale, false, DraftStatus.approved);
            UnicodeSet uset = f.getExemplarSet("", WinningChoice.WINNING);
            if (uset == null) {
                continue;
            }
            ExemplarExceptions ee = ExemplarExceptions.get(locale);
            uset = new UnicodeSet(uset).addAll(ee.additions).removeAll(ee.subtractions);

            UnicodeSet flattened = new UnicodeSet();
            for (String cp : uset) {
                flattened.addAll(nfkc.normalize(cp));
            }
            for (String cp : flattened) {
                String s = result.get(cp);
                result.put(cp, s == null ? locale : s + "; " + locale);
            }
        }
        return result;
    }
    private IdUsage getBestIdUsage(String value) {
        String[] scripts = value.split(" ");
        IdUsage bestIdUsage = IdUsage.UNKNOWN;
        for (String script : scripts) {
            Info info = ScriptMetadata.getInfo(script);
            IdUsage idUsage = info == null ? IdUsage.UNKNOWN : info.idUsage;
            if (bestIdUsage.compareTo(idUsage) < 0) {
                bestIdUsage = idUsage;
            }
        }
        return bestIdUsage;
    }
    private String getLanguageNameAndCode(String baseLanguage) {
        return testInfo.getEnglish().getName(baseLanguage) + " (" + baseLanguage + ")";
    }

    private void showDiff(UnicodeMap<String> cldrExemplars,
            UnicodeSet simMinusIdmod) {
        for (String locales : cldrExemplars.values()) {
            UnicodeSet exemplars = cldrExemplars.getSet(locales);
            if (simMinusIdmod.containsSome(exemplars)) {
                UnicodeSet uset = new UnicodeSet(exemplars).retainAll(simMinusIdmod);
                showSet(locales, uset);
            }
        }
    }

    private void showSet(String title, UnicodeSet uset) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.nextRange();) {
            logln("\t\t" + getCodeAndName(UTF16.valueOf(it.codepoint)) + "\t//" + title);
            if (it.codepoint != it.codepointEnd) {
                logln("\t\t... " + getCodeAndName(UTF16.valueOf(it.codepointEnd)) + "\t//" + title);
            }
        }
    }
    public String getCodeAndName(String cp) {
        return Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp);
    }
    private static final  UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);
    private static class CoverageData {
        // setup for coverage
        Counter<Level> foundCounter = new Counter<Level>();
        Counter<Level> unconfirmedCounter = new Counter<Level>();
        Counter<Level> missingCounter = new Counter<Level>();
        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getCldrFactory().make("en", true));
        Comparator<String> ldmlComp2 = CLDRFile.getComparator(DtdType.ldml);
        Relation<MissingStatus, String> missingPaths = Relation.of(new EnumMap<MissingStatus, Set<String>>(
                MissingStatus.class), TreeSet.class, ldmlComp2);
        Set<String> unconfirmed = new TreeSet(ldmlComp2);

        Map<Level, Double> getData(CLDRFile f) {
            Map<Level, Double> confirmedCoverage = new EnumMap(Level.class);
            VettingViewer.getStatus(testInfo.getEnglish().fullIterable(), f,
                    pathHeaderFactory, foundCounter, unconfirmedCounter,
                    missingCounter, missingPaths, unconfirmed);
            int sumFound = 0;
            int sumMissing = 0;
            int sumUnconfirmed = 0;
            for (Level level : Level.values()) {
                sumFound += foundCounter.get(level);
                sumUnconfirmed += unconfirmedCounter.get(level);
                sumMissing += missingCounter.get(level);
                confirmedCoverage.put(level, (sumFound) / (double) (sumFound + sumUnconfirmed + sumMissing));
            }
            return confirmedCoverage;
        }
    }

}
