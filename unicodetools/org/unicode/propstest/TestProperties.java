package org.unicode.propstest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Counter;
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
import org.unicode.props.PropertyNames;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Numeric_Type_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class TestProperties extends TestFmwk {
    private static final boolean CHECK_DIFFS = false;

    public static void main(String[] args) {
        new TestProperties().run(args);
    }

    // TODO generate list of versions, plus 'latest'

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> newName = iup.load(UcdProperty.Name);
    static final UnicodeMap<String> blocks = iup.load(UcdProperty.Block);
    static final IndexUnicodeProperties lastVersion = IndexUnicodeProperties.make("6.3");
    static final UnicodeMap<String> generalCategory = iup.load(UcdProperty.General_Category);
    static final UnicodeSet newChars = iup.load(UcdProperty.Age).getSet(UcdPropertyValues.Age_Values.V7_0.name());

    public void TestAAScripts() {
        UnicodeMap<String> scriptInfo = iup.load(UcdProperty.Script);
        UnicodeSet unknownScript = scriptInfo.getSet(
                UcdPropertyValues.Script_Values.Unknown.toString());
        unknownScript.removeAll(generalCategory.getSet(
                UcdPropertyValues.General_Category_Values.Unassigned.toString()))
                .removeAll(generalCategory.getSet(
                UcdPropertyValues.General_Category_Values.Private_Use.toString()))
                .removeAll(generalCategory.getSet(
                UcdPropertyValues.General_Category_Values.Surrogate.toString()))
                ;
        UnicodeSet unknownMarks = new UnicodeSet(generalCategory.getSet(
                UcdPropertyValues.General_Category_Values.Nonspacing_Mark.toString()))
        .addAll(generalCategory.getSet(
                UcdPropertyValues.General_Category_Values.Enclosing_Mark.toString()))
                .addAll(generalCategory.getSet(
                        UcdPropertyValues.General_Category_Values.Spacing_Mark.toString()))
                        .retainAll(unknownScript)
                        ;
        unknownScript.removeAll(unknownMarks);
        assertEquals("Missing Inherited", UnicodeSet.EMPTY, unknownMarks);
        assertEquals("Missing Common", UnicodeSet.EMPTY, unknownScript);
        for (UnicodeSetIterator it = new UnicodeSetIterator(unknownScript); 
                it.nextRange();) {
            System.out.println(Utility.hex(it.codepoint) + ".." + Utility.hex(it.codepointEnd)
                    + "; Common # (" + generalCategory.get(it.codepoint) + ".." + generalCategory.get(it.codepointEnd) + ") "
                    + nameMap.get(it.codepoint) + ".." + nameMap.get(it.codepointEnd)
                    );
        }
    }
    public void TestScripts() {

        System.out.println("New chars: " + newChars.size());
        {
            LinkedHashSet values = new LinkedHashSet(
                    Arrays.asList(Script_Values.values()));
            values.remove(Script_Values.Unknown);
            values.remove(Script_Values.Katakana_Or_Hiragana);
            listValues(UcdProperty.Script, values,
                    new Transform<Script_Values, String>() {
                @Override
                public String transform(Script_Values source) {
                    return source.getShortName();
                }
            });
        }
        {
            LinkedHashSet values = new LinkedHashSet(
                    Arrays.asList(General_Category_Values.values()));
            listValues(UcdProperty.General_Category, values,
                    new Transform<General_Category_Values, String>() {
                @Override
                public String transform(General_Category_Values source) {
                    return source.getShortName();
                }
            });
        }
    }

    public <T extends Enum<T>> void listValues(UcdProperty ucdProperty, Collection<T> values, 
            Transform<T, String> transform) {
        UnicodeMap<String> scripts = iup.load(ucdProperty);
        UnicodeMap<String> oldScripts = lastVersion.load(ucdProperty);

        Counter<T> newScriptCounts = new Counter<T>();
        Counter<T> oldScriptCounts = new Counter<T>();
        for (T s : values) {
            String name = s.name();
            UnicodeSet set = scripts.getSet(name);
            if (set.size() == 0) {
                continue;
            }
            UnicodeSet newSet = new UnicodeSet(set).retainAll(newChars);
            int oldSize = set.size() - newSet.size();
            if (oldSize > 0) {
                oldScriptCounts.add(s, oldSize);
            }
            if (newSet.size() > 0) {
                newScriptCounts.add(s, newSet.size());
            }
        }
        System.out.println("#counts");
        LinkedHashSet<T> sorted = new LinkedHashSet(newScriptCounts.getKeysetSortedByCount(false));
        sorted.addAll(oldScriptCounts.getKeysetSortedByCount(false));
        NumberFormat nf = NumberFormat.getInstance();
        for (T c : sorted) {
            System.out.println(
                    nf.format(oldScriptCounts.get(c)) 
                    + "\t" + nf.format(newScriptCounts.get(c))
                    + "\t" + transform.transform(c)
                    + "\t" + c);
        }
    }

    public HashMap<String, String> getSkeletonMap(Collection<String> collection, boolean skeletonToNormal) {
        HashMap<String, String> capNewScripts = new HashMap<String, String>();
        for (String normal : collection) {
            String skeleton = normal.toUpperCase(Locale.ENGLISH).replace("_","");
            if (skeletonToNormal) {
                capNewScripts.put(skeleton, normal);
            } else {
                capNewScripts.put(normal,skeleton);
            }
        }
        return capNewScripts;
    }

    public void showDiffs(UnicodeMap<String> generalCategory,
            UnicodeMap<String> oldGeneralCategory) {
        showDiffs(oldGeneralCategory, generalCategory, findDifferences2(oldGeneralCategory, generalCategory));
    }
    private void showValues(UnicodeSet us) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.nextRange();) {
            int start = it.codepoint;
            int end = it.codepointEnd;
            System.out.println(Utility.hex(start)
                    + (start == end ? "" : ".." + Utility.hex(end))
                    + "\t#\t" + newName.get(start)
                    + (start == end ? "" : ".." + newName.get(end))
                    );
        }
    }

    private <T> void showValues(UnicodeMap<T> us) {
        Iterable<EntryRange> ers = us.entryRanges();
        for (EntryRange<T> it : ers) {
            if (it.value == null) {
                continue;
            }
            int start = it.codepoint;
            int end = it.codepointEnd;
            System.out.println(Utility.hex(start)
                    + (start == end ? "" : ".." + Utility.hex(end))
                    + " ;\t" + it.value
                    + "\t#\t" + newName.get(start)
                    + (start == end ? "" : ".." + newName.get(end))
                    );
        }
    }


    public void TestProp() {
        boolean skipIt = false;
        String skipTo = "Lowercase_Mapping";
        for (String s : iup.getAvailableNames()) {
            if (s.equals(skipTo)) {
                skipIt = false;
            }
            if (skipIt) {
                logln("Skipping\t" + s);
            } else {
                checkProp(s);
            }
        }
        //        for (UcdProperty up : iup.getAvailableUcdProperties()) {
        //            logln(up.getType() + "\t" + up.getNames());
        //            Set<Enum> enums = up.getEnums();
        //            if (enums != null) {
        //                for (Enum enumValue : enums) {
        //                    logln("\t" + ((PropertyNames.Named)enumValue).getNames());
        //                }
        //            }
        //        }
    }
    public void checkProp(String s) {
        UnicodeProperty prop = iup.getProperty(s);
        List<String> availableValues = prop.getAvailableValues();
        UnicodeProperty oldProp = lastVersion.getProperty(s);

        UnicodeMap<String> propUmOld = oldProp.getUnicodeMap();
        UnicodeMap<String> propUmNew = prop.getUnicodeMap();
        String message = UnicodeProperty.getTypeName(prop.getType()) 
                + "\t" + CollectionUtilities.join(prop.getNameAliases(), ", ") 
                + "\tvalues: " + availableValues.size();
        if (propUmOld.equals(propUmNew)) {
            logln(message);
            return;
        }
        logln(message + "\t!DIFF!");
        UnicodeSet diffs = findDifferences(propUmOld, propUmNew);
        if (CHECK_DIFFS) {
            UnicodeSet diffs2 = findDifferences2(propUmOld, propUmNew);
            if (!diffs.equals(diffs2)) {
                throw new IllegalArgumentException(); 
            }
        }
        showDiffs(propUmOld, propUmNew, diffs);
    }
    public void showDiffs(UnicodeMap<String> propUmOld,
            UnicodeMap<String> propUmNew, UnicodeSet diffs) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(diffs); it.nextRange();) {
            int start = it.codepoint;
            String oldValue = propUmOld.get(start);
            String newValue = propUmNew.get(start);
            for (int i = start+1; i < it.codepointEnd; ++i) {
                String oldValue2 = propUmOld.get(i);
                String newValue2 = propUmNew.get(i);
                if (!UnicodeMap.areEqual(oldValue, oldValue2) || !UnicodeMap.areEqual(newValue, newValue2)) {
                    logCharValues(start, i-1, oldValue, newValue);                 
                    start = i;
                    oldValue = oldValue2;
                    newValue = newValue2;
                }
            }
            logCharValues(start, it.codepointEnd, oldValue, newValue);   

            //            UnicodeSet newSet = prop.getSet(value);
            //            UnicodeSet oldSet = oldProp.getSet(value);
            //            //                logln("\t" + value + "\t" 
            //            //                        + CollectionUtilities.join(prop.getValueAliases(value), ", "));
            //            if (!newSet.equals(oldSet)) {
            //                logln("\t\t" + value
            //                        + "\told:\t" + new UnicodeSet(oldSet).removeAll(newSet)
            //                        + "\tnew:\t" + new UnicodeSet(newSet).removeAll(oldSet)
            //                        );
            //            }
        }
    }

    public void logCharValues(int start, int end, String oldValue, String newValue) {
        logln("\t\t" + Utility.hex(start)
                + (start == end ? "" : ".." + Utility.hex(end))
                + "\told:\t" + oldValue
                + "\tnew:\t" + newValue
                + "\t" + newName.get(start)
                + (start == end ? "" : ".." + newName.get(end))
                );
    }

    private <T> UnicodeSet findDifferences2(UnicodeMap<T> m1,
            UnicodeMap<T> m2) {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!UnicodeMap.areEqual(m1.get(i), m2.get(i))) {
                result.add(i);
            }
        }
        return result;
    }
    // should be method on UnicodeMap
    private <T> UnicodeSet findDifferences(UnicodeMap<T> m1,
            UnicodeMap<T> m2) {
        if (m1.size() == 0) {
            return m2.keySet();
        } else if (m2.size() == 0) {
            return m1.keySet();
        }
        UnicodeSet result = new UnicodeSet();
        Iterator<EntryRange> it1 = m1.entryRanges().iterator();
        Iterator<EntryRange> it2 = m2.entryRanges().iterator();
        EntryRange er1 = it1.next();
        EntryRange er2 = it2.next();
        while (true) {
            if (er1.string != null || er2.string != null) {
                throw new UnsupportedOperationException(); // don't handle yet
            } else if (er1.codepoint == er2.codepoint 
                    && er1.codepointEnd == er2.codepointEnd) { // fast path for equals
                if (!CollectionUtilities.equals(er1.value, er2.value)) {
                    result.add(er1.codepoint, er1.codepointEnd);
                }
                if (it1.hasNext() && it2.hasNext()) {
                    er1 = it1.next();
                    er2 = it2.next();
                } else {
                    break; // add remainders there
                }
            } else if (er1.codepointEnd < er2.codepoint) { // fast path for no overlap
                if (er1.value != null) {
                    result.add(er1.codepoint, er1.codepointEnd);
                }
                if (it1.hasNext()) {
                    er1 = it1.next();
                } else {
                    break; // add remainders there
                }
            } else if (er2.codepointEnd < er1.codepoint) { // fast path for no overlap
                if (er2.value != null) {
                    result.add(er2.codepoint, er2.codepointEnd);
                }
                if (it2.hasNext()) {
                    er2 = it2.next();
                } else {
                    break; // add remainders there
                }
            } else { // not the same, and there is overlap
                int min = Math.min(er1.codepoint, er2.codepoint);
                int minOverlap = Math.max(er1.codepoint, er2.codepoint);
                int maxOverlap = Math.min(er1.codepointEnd, er2.codepointEnd);
                int max = Math.min(er1.codepointEnd, er2.codepointEnd);
                // add the bits
                if (er1.codepoint == min && er1.value != null
                        || er2.codepoint == min && er2.value != null) {
                    result.add(min, minOverlap-1);
                }
                if (!CollectionUtilities.equals(er1.value, er2.value)) {
                    result.add(minOverlap, maxOverlap);
                }
                // now the shorter one is done, and the longer one gets shortened
                if (er1.codepointEnd < er1.codepointEnd) {
                    er2.codepoint = er1.codepointEnd + 1;
                    if (it1.hasNext()) {
                        er1 = it1.next();
                    } else {
                        result.add(er2.codepoint, er2.codepointEnd); // shortened bit
                        break; // add remainders there
                    }
                } else {
                    er1.codepoint = er2.codepointEnd + 1;
                    if (it2.hasNext()) {
                        er2 = it2.next();
                    } else {
                        result.add(er1.codepoint, er1.codepointEnd); // shortened bit
                        break; // add remainders there
                    }
                } 
            }
        }
        // now add the remainders. 
        Iterator<EntryRange> remainder = it2.hasNext() ? it2 : it1;
        while (remainder.hasNext()) {
            EntryRange er = remainder.next();
            result.add(er.codepoint, er.codepointEnd);
        }
        return result;
    }

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
        UnicodeMap<String> typeMap = new UnicodeMap().putAll(iup.load(UcdProperty.Id_Mod_Type));
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

    public void showDiff(UnicodeMap<String> cldrExemplars,
            UnicodeSet simMinusIdmod) {
        for (String locales : cldrExemplars.values()) {
            UnicodeSet exemplars = cldrExemplars.getSet(locales);
            if (simMinusIdmod.containsSome(exemplars)) {
                UnicodeSet uset = new UnicodeSet(exemplars).retainAll(simMinusIdmod);
                showSet(locales, uset);
            }
        }
    }

    public void showSet(String title, UnicodeSet uset) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.nextRange();) {
            logln("\t\t" + getCodeAndName(UTF16.valueOf(it.codepoint)) + "\t//" + title);
            if (it.codepoint != it.codepointEnd) {
                logln("\t\t... " + getCodeAndName(UTF16.valueOf(it.codepointEnd)) + "\t//" + title);
            }
        }
    }

    public IdUsage getBestIdUsage(String value) {
        String[] scripts = value.split(" ");
        IdUsage bestIdUsage = IdUsage.UNKNOWN;
        for (String script : scripts) {
            Info info = ScriptMetadata.getInfo(script);
            IdUsage idUsage = info.idUsage;
            if (bestIdUsage.compareTo(idUsage) < 0) {
                bestIdUsage = idUsage;
            }
        }
        return bestIdUsage;
    }

    static final  UnicodeMap<String> nameMap = iup.load(UcdProperty.Name);

    static final  CLDRConfig testInfo = CLDRConfig.getInstance();

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    static final  Factory cldrFactory = testInfo.getCldrFactory();
    static final  Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();
    static final  Normalizer2 nfd = Normalizer2.getNFDInstance();
    static final  Normalizer2 nfkc = Normalizer2.getNFKCInstance();

    static class ExemplarExceptions {
        static final Map<String,ExemplarExceptions> exemplarExceptions = new HashMap();
        UnicodeSet additions = new UnicodeSet();
        UnicodeSet subtractions = new UnicodeSet();
        ExemplarExceptions add(String additions) {
            if (additions!= null) {
                this.additions.addAll(new UnicodeSet(additions));
            }
            return this;
        }
        ExemplarExceptions remove(String subtractions) {
            if (subtractions != null) {
                this.subtractions.addAll(subtractions);
            }
            return this;
        }
        static ExemplarExceptions get(String locale) {
            ExemplarExceptions ee = exemplarExceptions.get(locale);
            if (ee == null) {
                exemplarExceptions.put(locale, ee = new ExemplarExceptions());
            }
            return ee;
        }
        public static void add(String locale, String chars) {
            ExemplarExceptions.get(locale).add(chars);
        }
        public static void remove(String locale, String chars) {
            ExemplarExceptions.get(locale).remove(chars);
        }

        static {
            add("en", "[0-9]"); // good enough
            add("ar", "[٠-٩]"); // arab
            add("fa", "[۰-۹]"); // arabext
            add("ks", "[۰-۹]"); // arabext
            add("pa_Arab", "[۰-۹]"); // arabext
            add("ps", "[۰-۹]"); // arabext
            add("ur_IN", "[۰-۹]"); // arabext
            add("uz_Arab", "[۰-۹]"); // arabext
            add("as", "[০-৯]"); // beng
            add("bn", "[০-৯]"); // beng
            add("mr", "[०-९]"); // deva
            add("ne", "[०-९]"); // deva
            add("my", "[၀-၉]"); // mymr
            add("dz", "[༠-༩]"); // tibt
            remove("ks", "[ٖٗٚٛٮ۪ۭ]");
            remove("kn", "[ೞ]");
            remove("km", "[់-៑]");
            remove("si", "[ෟ]");
        }
    }

    public UnicodeMap<String> getCldrExemplars() {
        LanguageTagParser ltp = new LanguageTagParser();
        UnicodeMap<String> result = new UnicodeMap();
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

    static class CoverageData {
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
    public String getLanguageNameAndCode(String baseLanguage) {
        return testInfo.getEnglish().getName(baseLanguage) + " (" + baseLanguage + ")";
    }

    public void TestExemplarsAgainstIdmod() {
        UnicodeMap<String> statusMap = iup.load(UcdProperty.Id_Mod_Status);
        UnicodeMap<String> typeMap = iup.load(UcdProperty.Id_Mod_Type);
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
                logln(localeName + "\tSuspicious characters; never in CLDR data: ");
                for (String cp : suspicious) {
                    logln("\t" + getCodeAndName(cp));
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

    public String getCodeAndName(String cp) {
        return Utility.hex(cp) + " (" + cp + ") " + nameMap.get(cp);
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
