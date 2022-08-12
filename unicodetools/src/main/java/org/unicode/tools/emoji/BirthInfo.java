package org.unicode.tools.emoji;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.tools.emoji.Emoji.CharSource;

public class BirthInfo implements Comparable<BirthInfo> {
    private static final VersionInfo ZERO_VERSION = VersionInfo.getInstance(0);
    private static final BirthInfo MISSING = new BirthInfo(-1, ZERO_VERSION);

    public BirthInfo(int year, VersionInfo versionInfo) {
        super();
        this.year = year;
        this.emojiVersionInfo = versionInfo;
    }

    public final int year;
    public final VersionInfo emojiVersionInfo;

    @Override
    public int compareTo(BirthInfo o) {
        return emojiVersionInfo.compareTo(o.emojiVersionInfo);
    }

    @Override
    public boolean equals(Object obj) {
        return 0 == compareTo((BirthInfo) obj);
    }

    @Override
    public int hashCode() {
        return emojiVersionInfo.hashCode();
    }

    @Override
    public String toString() {
        return "v" + emojiVersionInfo.getVersionString(2, 2) + " (" + year + ")";
    }

    static void checkYears() {
        UnicodeMap<BirthInfo> yearData = getBirthInfoMap();
        for (BirthInfo value : new TreeSet<BirthInfo>(yearData.values())) {
            UnicodeSet set = yearData.getSet(value);
            System.out.println(value + "\t" + set.size() + "\t" + set.toPattern(false));
        }
    }

    static final UnicodeMap<BirthInfo> birthYear = new UnicodeMap<BirthInfo>();
    static Map<Integer, UnicodeSet> yearToEmoji;
    static Map<VersionInfo, UnicodeSet> emojiVersionToEmoji;
    // static final UnicodeMap<Integer> birthYearWithVarians = new UnicodeMap<Integer>();

    public static BirthInfo getBirthInfo(String s) {
        UnicodeMap<BirthInfo> years = getBirthInfoMap();
        final String cleaned = EmojiData.removeEmojiVariants(s);
        if (cleaned.isEmpty()) {
            return BirthInfo.MISSING;
        }
        return years.get(cleaned);
    }

    public static UnicodeMap<BirthInfo> getBirthInfoMap() {
        buildYears();
        return birthYear;
    }

    //    public static UnicodeMap<BirthInfo> getYearMapWithVariants() {
    //        buildYears();
    //        return birthYear; // birthYearWithVariants;
    //    }

    private static synchronized void buildYears() {
        if (birthYear.isEmpty()) {
            UnicodeMap<Integer> _years = new UnicodeMap<>();
            UnicodeMap<VersionInfo> _emojiVersionToEmoji = new UnicodeMap<>();

            //            Collection<Age_Values> output = new TreeSet<>(Collections.reverseOrder());
            // // latest first
            //            VersionInfo firstVersion = null;

            EmojiData beta = EmojiData.EMOJI_DATA_BETA;
            for (String s : beta.getAllEmojiWithDefectives()) {
                if ("👨‍⚕".equals(s)) {
                    int debug = 0;
                }
                String withoutVariants = EmojiData.removeEmojiVariants(s);
                String withVariants = beta.addEmojiVariants(s);
                // if single code point, remove var
                if (Character.charCount(withoutVariants.codePointAt(0))
                        == withoutVariants.length()) {
                    s = withoutVariants;
                }
                if (birthYear.containsKey(s)) {
                    continue;
                }
                int year = -1;
                if (s.equals("☝🏻")) {
                    int debug = 0;
                }
                VersionInfo versionInfo = Emoji.VERSION1;
                Set<CharSource> sources = EmojiData.getCharSources(s);
                // ARIB U5.2 - 2009
                // JCarrier U6.0 - 2010
                // WDings U7.0 - 2014

                if (sources.contains(CharSource.JCarrier)) {
                    year = 2010;
                    versionInfo = Emoji.VERSION0_6;
                } else if (sources.contains(CharSource.ARIB)
                        || sources.contains(CharSource.WDings)) {
                    year = 2014;
                    versionInfo = Emoji.VERSION0_7;
                } else if (s.codePointAt(0) == EmojiData.HANDSHAKE
                        && s.length() > 2 && EmojiData.MODIFIERS.contains(s.codePointAt(2))) {
                    // Handshake is E3.0 but mods were not added until E14.0
                    year = 2021;
                    versionInfo = Emoji.VERSION14;
                } else {
                    for (Entry<VersionInfo, Integer> entry :
                            Emoji.EMOJI_VERSION_TO_YEAR.entrySet()) {
                        versionInfo = entry.getKey();
                        EmojiData data = EmojiData.of(versionInfo);
                        if (data.getAllEmojiWithDefectives().contains(s)) {
                            year = entry.getValue();
                            //                            if (firstVersion == null) {
                            //                                firstVersion = versionInfo;
                            //                            }
                            //                            if (versionInfo == firstVersion) {
                            //                                year = 2015;
                            //                                versionInfo = Emoji.VERSION1;
                            //
                            //                                // handle specially
                            //                                // get the ages of all the components
                            //                                Collection<Age_Values> items =
                            // Emoji.getValues(s, Emoji.VERSION_ENUM, output);
                            //                                Age_Values ageValue =
                            // output.iterator().next(); // output is latest first
                            //                                // TODO: have E0.1, E0.2 ... for years
                            // between 2010 and 2014
                            //                                long date =
                            // VersionToAge.ucd.getLongDate(ageValue);
                            //                                year = new Date(date).getYear()+1900;
                            //                                if (year < 2010) { //  &&
                            // !Emoji.isSingleCodePoint(s)
                            //                                    // keycaps, etc. came in with
                            // Japanese
                            //                                    year = 2010;
                            //                                    versionInfo = Emoji.VERSION1;
                            //                                } else {
                            //                                    int debug = 0;
                            //                                }
                            // }
                            break;
                        }
                    }
                }
                BirthInfo value = new BirthInfo(year, versionInfo);
                _years.put(s, year);
                _emojiVersionToEmoji.put(s, versionInfo);

                birthYear.put(s, value);
                birthYear.put(withVariants, value);
                birthYear.put(withoutVariants, value);
                // birthYearWithVariants.put(withVariants, year);
                //                if (s.contains("⚕")) {
                //                    int debug = 0;
                //                }
                //                String plusFef0 = beta.addEmojiVariants(s);
                //                if (!s.equals(plusFef0)) {
                //                    birthYear.put(plusFef0, year);
                //                }
                //                String minusFef0 = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
                //                if (!s.equals(minusFef0)) {
                //                    birthYear.put(minusFef0, year);
                //                }
            }
            birthYear.freeze();
            // birthYearWithVariants.freeze();
            TreeMap<Integer, UnicodeSet> _years2 = new TreeMap<>(Collections.reverseOrder());
            TreeMap<VersionInfo, UnicodeSet> _emojiVersionToEmoji2 =
                    new TreeMap<>(Collections.reverseOrder());
            _years.addInverseTo(_years2);
            _emojiVersionToEmoji.addInverseTo(_emojiVersionToEmoji2);
            // protect
            for (Entry<Integer, UnicodeSet> entry : _years2.entrySet()) {
                entry.getValue().freeze();
            }
            for (Entry<VersionInfo, UnicodeSet> entry : _emojiVersionToEmoji2.entrySet()) {
                entry.getValue().freeze();
            }
            yearToEmoji = ImmutableMap.copyOf(_years2);
            emojiVersionToEmoji = ImmutableMap.copyOf(_emojiVersionToEmoji2);
        }
    }
    /**
     * Return the year values, from largest to smallest
     *
     * @return
     */
    public static Set<Integer> years() {
        return yearToEmoji.keySet();
    }

    public static Set<VersionInfo> versions() {
        return emojiVersionToEmoji.keySet();
    }

    public static UnicodeSet getSetForYears(int year2) {
        return yearToEmoji.get(year2);
    }

    public static UnicodeSet getSetForVersion(VersionInfo version) {
        return emojiVersionToEmoji.get(version);
    }

    public static int getYear(String s) {
        BirthInfo data = getBirthInfo(s);
        return data == null ? -1 : data.year;
    }

    public static VersionInfo getVersionInfo(String s) {
        BirthInfo data = getBirthInfo(s);
        return data == null ? ZERO_VERSION : data.emojiVersionInfo;
    }

    public static int getYear(VersionInfo versionInfo) {
        Integer year = Emoji.EMOJI_VERSION_TO_YEAR.get(versionInfo);
        return year == null ? -1 : year;
    }

    public static void main(String[] args) {
        getBirthInfoMap();
        for (CharSource charSource : Emoji.CharSource.values()) {
            UnicodeSet uset = EmojiData.getCharSourceSet(charSource);
            System.out.println(charSource + "\t" + uset.toPattern(false));
            // ARIB U5.2
            // JCarrier U6.0
            // WDings U7.0
        }
        for (VersionInfo version : BirthInfo.versions()) {
            final UnicodeSet setForVersion = BirthInfo.getSetForVersion(version);
            System.out.println(
                    version + "\t" + setForVersion.size() + "\t" + setForVersion.toPattern(false));
        }
    }
}
