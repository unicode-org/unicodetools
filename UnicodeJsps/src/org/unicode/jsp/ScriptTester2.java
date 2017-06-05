package org.unicode.jsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class ScriptTester2 {
    private final UnicodeMap<UnicodeSet> toEquivalents;
    private final UnicodeMap<Set<String>> toScripts;
    private final Set<String> allScripts;
    private final UnicodeSet allowedCharacters;
    private final UnicodeMap<String> confusables;
    private final SortedMap<String, Integer> multipleToSingleConfusable;

    private ScriptTester2(Set<String> all, UnicodeMap<String> confusables, SortedMap<String, Integer> multipleToSingleConfusable, UnicodeMap<Set<String>> scripts, UnicodeMap<UnicodeSet> equiv2, UnicodeSet allowed) {
        this.allScripts = all;
        this.confusables = confusables;
        toEquivalents = equiv2;
        this.toScripts = scripts;
        this.allowedCharacters = allowed;
        this.multipleToSingleConfusable = multipleToSingleConfusable;
    }

    public static ScriptTester2 getInstance(VersionInfo version, UnicodeSet allowed) {
        allowed = allowed.isFrozen() ? allowed : new UnicodeSet(allowed).freeze();
        CachedProps props = CachedProps.getInstance(version);
        //System.out.println(new TreeSet(props.getAvailable()));
        UnicodeMap<String> confusables = props.getProperty("Confusable_MA").getUnicodeMap();
        UnicodeMap<UnicodeSet> equiv = new UnicodeMap();
        SortedMap<String,Integer> multipleToSingle = new TreeMap<>(new UTF16.StringComparator(true,false,0));
        for (String value : confusables.values()) {
            UnicodeSet us = new UnicodeSet(confusables.getSet(value)).add(value).retainAll(allowed);
            if (us.isEmpty()) {
                continue;
            }
            us.freeze();
            equiv.putAll(us, us);
            if (value.codePointCount(0, value.length()) > 1) {
                int shortest = us.getRangeStart(0);
                multipleToSingle.put(value, shortest);
            }
        }

        Splitter bar = Splitter.on('|').trimResults();

        UnicodeMap<String> scripts = props.getProperty("Script_Extensions").getUnicodeMap();
        ImmutableSet<String> haniset = ImmutableSet.of("Hanb", "Jpan", "Kore");
        ImmutableSet<String> hiraset = ImmutableSet.of("Jpan");
        ImmutableSet<String> kataset = ImmutableSet.of("Jpan");
        ImmutableSet<String> hangset = ImmutableSet.of("Kore");
        ImmutableSet<String> boposet = ImmutableSet.of("Hanb");

        UnicodeMap<String> script = props.getProperty("Script").getUnicodeMap();
        Set<String> all = new TreeSet(script.values());
        all.addAll(haniset);
        all = ImmutableSet.copyOf(all);

        UnicodeMap<Set<String>> toScripts = new UnicodeMap();
        for (String value : scripts.values()) {
            Set<String> scriptSet;
            if (value.equals("Common") || value.equals("Inherited")) {
                scriptSet = all;
            } else {
                scriptSet = new TreeSet(bar.splitToList(value));
                if (scriptSet.contains("Han")) {
                    scriptSet.addAll(haniset);
                } else if (scriptSet.contains("Hiragana")) {
                    scriptSet.addAll(hiraset);
                } else if (scriptSet.contains("Katakana")) {
                    scriptSet.addAll(kataset);
                } else if (scriptSet.contains("Hangul")) {
                    scriptSet.addAll(hangset);
                } else if (scriptSet.contains("Bopomofo")) {
                    scriptSet.addAll(boposet);
                }
            }
            UnicodeSet us = scripts.getSet(value);
            toScripts.putAll(us, ImmutableSet.copyOf(scriptSet));
        }

        return new ScriptTester2(all, confusables, Collections.unmodifiableSortedMap(multipleToSingle), toScripts, equiv, allowed);
    }

    public static UnicodeSet getAllowedStatus(VersionInfo version) {
        CachedProps props = CachedProps.getInstance(version);
        return props.getProperty("Identifier_Status").getUnicodeMap().getSet("Allowed").freeze();
    }

    public static UnicodeSet getNFKD_Quick_CheckNo(VersionInfo version) {
        CachedProps props = CachedProps.getInstance(version);
        return props.getProperty("NFKD_Quick_Check").getUnicodeMap().getSet("No").freeze();
    }

    public Set<String> getScripts(CharSequence value) {
        Set<String> intersection = null;
        for (int cp : CharSequences.codePoints(value)) {
            Set<String> current = toScripts.get(cp);
            if (intersection == null) {
                intersection = new LinkedHashSet(current);
            } else {
                intersection.retainAll(current);
            }
            if (intersection.isEmpty()) {
                break;
            }
        }
        return intersection;
    }

    public enum ScriptRestriction {any, wholeScript}

    public List<Multimap<String, String>> getData(CharSequence value, ScriptRestriction scriptRestriction) {
        value = getSkeleton(value);
        List<Multimap<String, String>> result = new ArrayList();
        HashSet<String> foundScripts = scriptRestriction == ScriptRestriction.any ? null : new HashSet<String>();
        for (int cp : CharSequences.codePoints(value)) {
            UnicodeSet current = toEquivalents.get(cp);
            if (current == null) {
                current = new UnicodeSet(cp, cp);
            }
            Multimap<String, String> scriptsToChars = getScriptsToChars(current);
            result.add(scriptsToChars);
            if (foundScripts != null) {
                foundScripts.addAll(scriptsToChars.keySet());
            }
        }
        if (foundScripts != null) {
            for (int i = 0; i < result.size(); ++i) {
                Multimap<String, String> scriptsToChars = result.get(i);
                // if Common is not present, then restrict the set
                if (!scriptsToChars.containsKey("Common")) {
                    foundScripts.retainAll(scriptsToChars.keySet());
                }
            }
            foundScripts.add("Common");
            HashSet<String> missingScripts = new HashSet<String>(allScripts);
            missingScripts.removeAll(foundScripts);
            for (int i = 0; i < result.size(); ++i) {
                Multimap<String, String> scriptsToChars = result.get(i);
                for (String toRemove : missingScripts) {
                    scriptsToChars.removeAll(toRemove);
                }
            }
        }
        return result;
    }

    private Multimap<String, String> getScriptsToChars(UnicodeSet current) {
        Multimap<String,String> result = TreeMultimap.create();
        for (String s : current) {
            Set<String> scriptSet = toScripts.get(s);
            if (scriptSet.equals(allScripts)) {
                result.put("Common", s);
            } else {
                for (String script : scriptSet) {
                    result.put(script, s);
                }
            }
        }
        return result;
    }

    static final Normalizer2 nfd = Normalizer2.getNFDInstance();

    public String getSkeleton(CharSequence input) {
        StringBuilder result = new StringBuilder();
        for (int cp : CharSequences.codePoints(nfd.normalize(input))) {
            String paradigm = confusables.get(cp);
            if (paradigm == null) {
                result.appendCodePoint(cp);
            } else {
                result.append(paradigm);
            }
        }
        return nfd.normalize(result.toString());
    }

    void checkData() {
        for (String value : confusables.values()) {
            // check results
            int[] codePoints = CharSequences.codePoints(value);
            if (codePoints.length > 1) {
                for (int cp : codePoints) {
                    String v2 = confusables.get(cp);
                    if (v2 != null) {
                        throw new IllegalArgumentException("not recursive");
                    }
                }
                Set<String> scripts = getScripts(value);
                if (scripts.isEmpty()) {
                    System.err.println("mixedscript results: " + value + "\t" + scripts);
                }
            }
        }
        for (Entry<String, Integer> entry : multipleToSingleConfusable.entrySet()) {
            //System.out.println(entry.getKey() + "\t" + UTF16.valueOf(entry.getValue()));
            // check for overlaps
            String source = entry.getKey();
            String partial = source;
            while (!partial.isEmpty()) {
                partial = partial.substring(UCharacter.charCount(partial.codePointAt(0)));
                SortedMap<String, Integer> sub = multipleToSingleConfusable.tailMap(partial);
            }
        }
    }

    public static void main(String[] args) {
        VersionInfo version = VersionInfo.getInstance(10);
        UnicodeSet allowedStatus = ScriptTester2.getAllowedStatus(version);
        UnicodeSet nfkd_Quick_CheckNo = ScriptTester2.getNFKD_Quick_CheckNo(version);
        ScriptTester2 tester = ScriptTester2.getInstance(version, 
                new UnicodeSet(0,0x10ffff)
                //                .removeAll(nfkd_Quick_CheckNo)
                //                .removeAll(new UnicodeSet("[^[:scx=cyrl:][:scx=latn:][:scx=common:][:scx=inherited:]]"))
//                .retainAll(allowedStatus)
                );
        
        
        tester.checkData();
        
        for (String s : Arrays.asList("came", "apple", "scope", "Circle", "СігсӀе", "Сirсlе", "Circ1e", "C𝗂𝗋𝖼𝗅𝖾", "〆切", "ねガ", 
                "abcdefghijklmnopqrstuvwxyz",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                )) {
            System.out.println(s + "\t" + tester.getScripts(s));
            for (Multimap<String, String> data : tester.getData(s, ScriptRestriction.any)) {
                System.out.println("\t" + data);
            }
        }
        //        for (Set<String> s : tester.scripts.values()) {
        //            String sample = tester.scripts.getSet(s).iterator().next();
        //            UnicodeSet equivs = tester.equivalents.get(sample);
        //            System.out.println(sample 
        //                    + "\n\t" + (equivs == null ? "?" : equivs.toPattern(false))
        //                    + "\n\t" + tester.scripts.get(sample));
        //        }
    }
}
