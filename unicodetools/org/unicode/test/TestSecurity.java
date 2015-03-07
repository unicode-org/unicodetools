package org.unicode.test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Names;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.UCD.IdentifierInfo.IdentifierStatus;
import org.unicode.text.UCD.IdentifierInfo.IdentifierType;
import org.unicode.text.tools.XIDModifications;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Confusables;
import org.unicode.tools.Confusables.Style;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class TestSecurity extends TestFmwkPlus {
    private static final String SECURITY_PUBLIC = Settings.UNICODE_DRAFT_PUBLIC + "security/";
    private static final String SECURITY = Settings.UNICODETOOLS_DIRECTORY + "data/security/";

    public static void main(String[] args) {
        new TestSecurity().run(args);
    }

    static XIDModifications XIDMOD = new XIDModifications(SECURITY_PUBLIC + Settings.latestVersion);

    static Confusables CONFUSABLES = new Confusables(SECURITY_PUBLIC + Settings.latestVersion);

    public void TestIdempotence() {
        for (Entry<String, EnumMap<Style, String>> entry : CONFUSABLES.getChar2data().entrySet()) {
            String code = entry.getKey();
            EnumMap<Style, String> map = entry.getValue();
            for (Entry<Style, String> codeToValue : map.entrySet()) {
                Style style = codeToValue.getKey();
                boolean warningOnly = style != Style.MA;

                UnicodeMap<String> transformMap = CONFUSABLES.getStyle2map().get(style);
                String value = codeToValue.getValue();
                String value2 = transformMap.transform(value);
                if (!value2.equals(value)) {
                    final String message = style
                            + "\tU+" + Utility.hex(code)+ " ( " + code + " ) "  + Default.ucd().getName(code)
                            + "\t\texpect:\tU+" + Utility.hex(value2, "U+") + " ( " + value2 + " ) " + Default.ucd().getName(value2)
                            + "\t\tactual:\tU+" + Utility.hex(value, "U+") + " ( " + value + " ) " + " " + Default.ucd().getName(value)
                            ;
                    if (warningOnly) {
                        warnln(message);
                    } else { 
                        errln(message); 
                    }
                }
            }
        }
    }

    public void TestConfusables() {
        Confusables confusablesOld = new Confusables(SECURITY + Settings.lastVersion);
        showDiff("Confusable", confusablesOld.getStyle2map().get(Style.MA), CONFUSABLES.getStyle2map().get(Style.MA), new UTF16.StringComparator(), getLogPrintWriter());
    }

    public void TestXidMod() {
        XIDModifications xidModOld = new XIDModifications(SECURITY + Settings.lastVersion);
        UnicodeMap<IdentifierStatus> newStatus = XIDMOD.getStatus();
        showDiff("Status", xidModOld.getStatus(), newStatus, new EnumComparator<IdentifierStatus>(), getLogPrintWriter());
        showDiff("Type", xidModOld.getType(), XIDMOD.getType(), new EnumComparator<IdentifierType>(), getLogPrintWriter());

        UnicodeSet newRecommended = newStatus.getSet(IdentifierStatus.allowed);
        UnicodeSet oldRecommended = xidModOld.getStatus().getSet(IdentifierStatus.allowed);

        UnicodeSet itemsToCheck = new UnicodeSet(newRecommended).removeAll(oldRecommended);
        System.out.println(itemsToCheck);

        // if it is a new character in an old modern script, mark provisionally as uncommon use
        UCD ucd = Default.ucd();
        UCD oldUcd = UCD.make(Settings.lastVersion);
        System.out.println(ucd.getVersion());

        for (String s : ScriptMetadata.getScripts()) {
            if (s.equals("Jpan") || s.equals("Kore") || s.equals("Hans") || s.equals("Hant")) {
                continue;
            }
            Info info = ScriptMetadata.getInfo(s);
            if (info.idUsage == IdUsage.RECOMMENDED) {
                //System.out.println(s + "\t" + info);
                short currentScriptNumber = Utility.lookupShort(s, UCD_Names.SCRIPT, true);
                boolean first = true;
                UnicodeSet all = new UnicodeSet();
                for (String ss : itemsToCheck) {
                    int i = ss.codePointAt(0);
                    if (i == 0x08B3) {
                        int debug = 0;
                    }
                    if (oldUcd.isAssigned(i)) {
                        continue;
                    }
                    short script = ucd.getScript(i);
                    if (script != currentScriptNumber) {
                        continue;
                    }
                    all.add(i);
                }
                if (s.equals("Hani")) {
                    System.out.println(all + "; uncommon-use");
                } else {
                    for (String i : all) {
                        if (first) {
                            System.out.println("# " + s);
                            first = false;
                        }
                        System.out.println(Utility.hex(i) + "; uncommon-use" + " # " + ucd.getName(i));
                    }
                }
            }
        }
    }

    public IdUsage getScriptUsage(int codepoint) {
        String script = Default.ucd().getScriptID(codepoint);
        Info info = ScriptMetadata.getInfo(script);
        return info.idUsage;
    }

    public void TestCldrConsistency() {
        UnicodeMap<Set<String>> fromCLDR = getCLDRCharacters();
        UnicodeSet vi = new UnicodeSet();
        for (String s : fromCLDR) {
            if (s.equals("ə")) {
                System.out.println("ə" + fromCLDR.get('ə'));
            }
            IdentifierType item = XIDMOD.getType().get(s);
            switch (item) {
            case obsolete:
            case technical:
            case uncommon_use:
                IdUsage idUsage = getScriptUsage(s.codePointAt(0));
                Set<String> locales = fromCLDR.get(s);
                System.out.println("?Overriding with CLDR: "
                        + Utility.hex(s, "+")
                        + "; " + s 
                        + "; " + UCharacter.getName(s," ") 
                        + "; " + item 
                        + " => " + idUsage + "; "
                        + locales);
                if (locales.contains("vi")) {
                    vi.add(s);
                }
                break;
            default: 
                break;
            }
        }
        System.out.println("vi: " + vi.toPattern(false));
    }

    public static UnicodeMap<Set<String>> getCLDRCharacters() {
        UnicodeMap<Set<String>> result = new UnicodeMap<>();
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        //        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY)
        //        //, new File(CLDRPaths.SEED_DIRECTORY), new File(CLDRPaths.EXEMPLARS_DIRECTORY) 
        //        };
        //        Factory factory = SimpleFactory.make(paths, ".*");
        Set<String> localeCoverage = StandardCodes.make().getLocaleCoverageLocales("cldr");
        Set<String> skipped = new LinkedHashSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String localeId : localeCoverage) { //  factory.getAvailableLanguages()
            ltp.set(localeId);
            if (!ltp.getRegion().isEmpty()) {
                continue;
            }
            Iso639Data.Type type = Iso639Data.getType(ltp.getLanguage());
            if (type != Iso639Data.Type.Living) {
                skipped.add(localeId);
                continue;
            }
            CLDRFile cldrFile = factory.make(localeId, false);
            UnicodeSet exemplars = cldrFile
                    .getExemplarSet("", WinningChoice.WINNING);
            if (exemplars != null) {
                exemplars.closeOver(UnicodeSet.CASE);
                for (String s : flatten(exemplars)) { // flatten
                    Set<String> old = result.get(s);
                    if (old == null) {
                        result.put(s, Collections.singleton(localeId));
                    } else {
                        old = new TreeSet<String>(old);
                        old.add(localeId);
                        result.put(s, Collections.unmodifiableSet(old));
                    }
                }
            }
        }
        System.out.println("Skipped non-living languages " + skipped);
        return result;
    }

    private static UnicodeSet flatten(UnicodeSet result) {
        UnicodeSet result2 = new UnicodeSet();
        for (String s : result) { // flatten
            result2.addAll(s);
        }
        return result2;
    }



    public class EnumComparator<T extends Enum<?>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return o1.ordinal() - o2.ordinal();
        }
    }

    private <T> void showDiff(String title, UnicodeMap<T> mapOld, UnicodeMap<T> mapNew, Comparator<T> comparator, Appendable output) {
        int diffCount;
        try {
            diffCount = 0;
            TreeMap<T, Map<T, UnicodeSet>> diff = new TreeMap<T,Map<T,UnicodeSet>>(comparator);
            for (int i = 0; i <= 0x10FFFF; ++i) {
                T vOld = mapOld.get(i);
                T vNew = mapNew.get(i);
                if (!Objects.equal(vOld, vNew)) {
                    Map<T, UnicodeSet> submap = diff.get(vOld);
                    if (submap == null) {
                        diff.put(vOld, submap = new TreeMap<T,UnicodeSet>(comparator));
                    }
                    UnicodeSet us = submap.get(vNew);
                    if (us == null) {
                        submap.put(vNew, us = new UnicodeSet());
                    }
                    us.add(i);
                    diffCount++;
                }
            }
            for (Entry<T, Map<T, UnicodeSet>> value1 : diff.entrySet()) {
                T vOld = value1.getKey();
                for (Entry<T, UnicodeSet> value2 : value1.getValue().entrySet()) {
                    T vNew = value2.getKey();
                    UnicodeSet intersection = value2.getValue();
                    output.append(title + "\t«" + vOld + "» => «" + vNew + "»:\t" + intersection.size() + "\t" + intersection.toPattern(false) + "\n");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        //        Collection<T> oldValues = mapOld.values();
        //        Collection<T> newValues = mapNew.values();

        //        for (T oldValue : oldValues) {
        //            for (T newValue : newValues) {
        //                if (newValue.equals(oldValue)) {
        //                    continue;
        //                }
        //                UnicodeSet oldChars = mapOld.getSet(oldValue);
        //                UnicodeSet newChars = mapNew.getSet(newValue);
        //                if (oldChars.containsSome(newChars)) {
        //                    UnicodeSet intersection = new UnicodeSet(oldChars).retainAll(newChars);
        //                    diffCount += intersection.size();
        //                    System.out.println(title + "\t«" + oldValue + "» => «" + newValue + "»:\t" + intersection.size() + "\t" + intersection.toPattern(false));
        //                }
        //            }
        //        }
        System.out.println(title + " total differences: " + diffCount);
        //        LinkedHashSet<T> values = new LinkedHashSet<>(mapNew.values());
        //        values.addAll(mapOld.values());
        //        for (T value : values) {
        //            UnicodeSet oldChars = mapOld.getSet(value);
        //            UnicodeSet newChars = mapNew.getSet(value);
        //            if (oldChars != newChars) {
        //                System.out.println()
        //            }
        //        }
    }

    //    private <K, V, U extends Collection<Row.R2<K,V>>> U addAllTo(Iterable<Entry<K,V>> source, U target) {
    //        for (Entry<K,V> t : source) {
    //            target.add(Row.of(t.getKey(), t.getValue()));
    //        }
    //        return target;
    //    }
}

