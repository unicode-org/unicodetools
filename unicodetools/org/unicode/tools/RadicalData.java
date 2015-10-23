package org.unicode.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.tools.Ids.Nameslist;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;

class RadicalData {
    private final String radical;
    private List<Integer> strokeCounts;
    private final Relation<String, String> codeToReason = Relation.of(new TreeMap<String,Set<String>>(Ids.UNIHAN), LinkedHashSet.class);

    public RadicalData(String _radical) {
        radical = _radical;
    }

    public Set<String> getChars() {
        return codeToReason.keySet();
    }
    public List<Integer> getStrokeCounts() {
        return strokeCounts;
    }
    public void addItem(String string, String reason) {
        codeToReason.put(string, reason);
    }

    public void print(Appendable out) {
        if (strokeCounts == null) {
            throw new IllegalArgumentException("Must call finish first");
        }
        Tabber tabber = new Tabber.MonoTabber()
        .add(8, Tabber.LEFT)
        .add(5, Tabber.RIGHT)
        .add(9, Tabber.RIGHT)
        .add(50, Tabber.LEFT)
        .add(60, Tabber.LEFT)
        ;
        try {
            for (Entry<String, Set<String>> entry : codeToReason.keyValuesSet()) {
                String codePoint = entry.getKey();

                final Set<String> reasons = entry.getValue();
                //multipleRadicals.put(codePoint, key2);
                List<Integer> strokes = Ids.kTotalStrokes.get(codePoint);
                List<Integer> strokes2 = Ids.RADICAL.contains(codePoint) ? getStrokeCounts() : null;
                out.append(
                        tabber.process(
                                Utility.hex(codePoint)
                                + " ;\t" + radical
                                + " ;\t" + (strokes != null ? CollectionUtilities.join(strokes, "/")
                                        : strokes2 != null ? CollectionUtilities.join(strokes2, "/")
                                                : "?")
                                                + "\t # (" + codePoint + ") " + UCharacter.getName(codePoint, ", ")
                                                + " ;\t" + (reasons == null ? "" : CollectionUtilities.join(reasons, ", "))
                                                + "\n")
                        );
            }
            out.append("\n");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private void addItems(UnicodeSet sourceItems, String reason) {
        if (sourceItems != null && !sourceItems.isEmpty()) {
            codeToReason.putAll(sourceItems.addAllTo(new HashSet<String>()), reason);
        }
    }

    private void addItems(Set<String> sourceItems, String reason) {
        if (sourceItems != null && !sourceItems.isEmpty()) {
            codeToReason.putAll(sourceItems, reason);
        }
    }

    private void addRadicals(int intRadical, Map<Integer, UnicodeSet> radicalSource, String reason) {
        UnicodeSet us = radicalSource.get(intRadical);
        if (us != null) {
            codeToReason.putAll(us.addAllTo(new HashSet<String>()), reason);
        }
    }

    public void finish() {
        codeToReason.freeze();
        TreeSet<Integer> _strokeCounts = new TreeSet<Integer>();
        for (Entry<String, Set<String>> entry : codeToReason.keyValuesSet()) {
            String codePoint = entry.getKey();
            final List<Integer> strokes = Ids.kTotalStrokes.get(codePoint);
            if (strokes != null) {
                _strokeCounts.addAll(strokes);
            }
        }
        strokeCounts = Collections.unmodifiableList(new ArrayList<>(_strokeCounts));
        RadicalDataCache.put(radical, this);
    }
    static Map<String, RadicalData> RadicalDataCache = new HashMap<>();
    static Set<Entry<String, RadicalData>> entrySet() {
        return RadicalDataCache.entrySet();
    }
    
    static {
        Map<Double, R2<Set<String>,Set<String>>> sorted = new TreeMap<>();
        for (Entry<String, Set<String>> entry : Ids.radToUnicode.keyValuesSet()) {
            final String key = entry.getKey();
            final double clean = Ids.cleanRadical(key);
            sorted.put(clean, Row.of(entry.getValue(), Ids.rawRadToUnicode.get(key)));
        }
        int count = 0;
        Set<String> adobeItems = new TreeSet<>();
        //Set<String> sortedChars = new TreeSet<>(UNIHAN);
        UnicodeSet missingCjkRadicals = new UnicodeSet(Ids.CJK_Radicals_Supplement_BLOCK);
        Output<String> cjkRadValue = new Output<String>();
        Relation<String, String> multipleRadicals = Relation.of(new TreeMap<String,Set<String>>(Ids.UNIHAN), TreeSet.class);

        for (Entry<Double, R2<Set<String>, Set<String>>> entry : sorted.entrySet()) {
            ++count;
            //Relation<Integer, String> reasonMap = Relation.of(new HashMap(), LinkedHashSet.class);
            final Double key = entry.getKey();
            final R2<Set<String>, Set<String>> rad2 = entry.getValue();
            final Set<String> raw = rad2.get1();
            double doubleRadical = key.doubleValue();
            int intRadical = (int)doubleRadical;
            final boolean alt = intRadical != doubleRadical;
            String key2 = intRadical + (alt ? "'" : "");
            RadicalData radicalData = new RadicalData(key2);

            final Set<String> cjkRad = alt ? Collections.EMPTY_SET : IdsFileData.radToCjkRad.get(intRadical);
            UnicodeSet RSUnicode = Ids.USTROKE.get(intRadical, alt);
            M3<Integer, Integer, UnicodeSet> adobe = Ids.ADOBE_RADICAL_STROKESINRADICAL_REMAINDER_USET.get(intRadical);
            adobeItems.clear();
            if (!alt) {
                for (Entry<Integer, Map<Integer, UnicodeSet>> entry2 : adobe) {
                    Map<Integer, UnicodeSet> remStrokesToSet = entry2.getValue();
                    UnicodeSet us = remStrokesToSet.get(0);
                    if (us != null) {
                        UnicodeSet temp = us;
                        if (RSUnicode != null) {
                            temp = new UnicodeSet(us).removeAll(RSUnicode);
                        }
                        temp.addAllTo(adobeItems);
                    }
                }
            }

            radicalData.addItems(raw, "CJKRadicals.txt");
            radicalData.addItems(RSUnicode, "kRSUnicode");
            Ids.radToUnicode.get(intRadical+"'");
            boolean hasAltRadical = Ids.USTROKE.get(intRadical, true) != null || Ids.radToUnicode.get(intRadical+"'") != null;

            if (!alt && !hasAltRadical) {
                radicalData.addRadicals(intRadical, Ids.kRSKangXiRadicals, "kRSKangXi");
            }
            radicalData.addItems(adobeItems, "kRSAdobe_Japan1_6");
            radicalData.addItems(cjkRad, "idsCjkRadicals.txt");

            if (!alt && !hasAltRadical) {
                radicalData.addRadicals(intRadical, Ids.kRSJapaneseRadicals, "kRSJapanese");
                radicalData.addRadicals(intRadical, Ids.kRSKanWaRadicals, "kRSKanWa");
                radicalData.addRadicals(intRadical, Ids.kRSKoreanRadicals, "kRSKorean");
            }

            missingCjkRadicals.removeAll(radicalData.getChars());
            //Wikiwand.check(sortedChars);
            String extra = Nameslist.check(key2, radicalData.getChars(), cjkRadValue);
            if (extra != null) {
                radicalData.addItem(extra, "Nameslist");
                radicalData.addItem(cjkRadValue.value, "Nameslist");
            }
            radicalData.finish();

            //                for (String codePoint : sortedChars) {
            //                    multipleRadicals.put(codePoint, key2);
            //                    final Set<String> reasons = reasonMap.get(codePoint.codePointAt(0));
            //                    List<String> strokes = kTotalStrokes.get(codePoint);
            //                    out.println(Utility.hex(codePoint)
            //                            + " ;\t" + key2
            //                            + " ; " + (strokes == null ? "?" : CollectionUtilities.join(strokes, ", "))
            //                            + " \t# (" + codePoint + ") " + UCharacter.getName(codePoint, ", ")
            //                            + " ;\t" + (reasons == null ? "" : CollectionUtilities.join(reasons, ", "))
            //                            );
            //                }
            //                out.println();
        }
        System.out.println("items:\t" + count);
        System.out.println("missing cjk radicals:\t" + missingCjkRadicals);
        for (Entry<String, Set<String>> entry : multipleRadicals.keyValuesSet()) {
            final String codepoint = entry.getKey();
            final Set<String> radicals = entry.getValue();
            if (radicals.size() > 1) {
                System.out.println(codepoint + " ??? " + radicals);
            }            
        }
    }

}