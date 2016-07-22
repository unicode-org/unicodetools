package org.unicode.tools.emoji;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.text.utility.Birelation;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class EmojiAnnotations extends Birelation<String,String> {

    final Map<String,UnicodeSet> TO_UNICODE_SET;

    final private UnicodeMap<String> shortNames = new UnicodeMap<>();

    // Add to CLDR
    static final UnicodeSet MALE_SET = new UnicodeSet("[👦  👨  👴 🎅   🤴  🤵  👲🕴 🕺]");
    static final UnicodeSet FEMALE_SET = new UnicodeSet("[ 👧  👩  👵 🤶👸👰🤰💃]");


    //    private static final Splitter TAB = Splitter.on("\t").trimResults();
    //
    //    private static final boolean SHOW = false;

    //    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("the", "of", "for", "a", "and", "state", 
    //            "c�te", "verde▪cape", "dhekelia", "akrotiri", "comros", "pdr", "jamahiriya", "part",
    //            "yugoslav", "tfyr", "autonomous", "rawanda", "da", "rb", "yugoslavia",
    //            "states", "sar", "people's", "minor",
    //            "sts."));

    public static final EmojiAnnotations          ANNOTATIONS_TO_CHARS        = new EmojiAnnotations(
            GenerateEmoji.EMOJI_COMPARATOR
            //,
            //            "emojiAnnotations.txt",
            //            "emojiAnnotationsFlags.txt",
            //            "emojiAnnotationsGroups.txt",
            //            "emojiAnnotationsZwj.txt"
            );

    public EmojiAnnotations(Comparator<String> codepointCompare, String... filenames) {
        super(new TreeMap(EmojiOrder.FULL_COMPARATOR), 
                new TreeMap(codepointCompare), 
                TreeSet.class, 
                TreeSet.class, 
                codepointCompare, 
                EmojiOrder.FULL_COMPARATOR);

        //        for (String s : sorted) {
        //            Set<String> plainAnnotations = CandidateData.getInstance().getAnnotations(s);
        //            if (plainAnnotations.isEmpty()) {
        //                plainAnnotations = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s);
        //            }
        //            System.out.println(s + "\t" + CollectionUtilities.join(plainAnnotations, " | "));
        //        }
        UnicodeMap<Annotations> data = Annotations.getData("en");
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        final Set<String> keywords = new LinkedHashSet<>();
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            keywords.clear();
            String shortName = getNameAndAnnotations(s, english, data, keywords);
            if (shortName == null) {
                continue;
            }
            for (String annotation : keywords) {
                add(annotation, s);
            }
            shortNames.put(s, shortName);
        }
        shortNames.freeze();

        //        CandidateData candidateData = CandidateData.getInstance();
        //        for (String s : candidateData.getCharacters()) {
        //            for (String annotation : candidateData.getAnnotations(s)) {
        //                add(annotation, s);
        //            }
        //        }
        //Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>(codepointCompare));
        //        for (String filename : filenames) {
        //            int lineCount = 0;
        //            int lineNumber = 0;
        //            EmojiIterator ei = new EmojiIterator(EmojiData.of(Emoji.VERSION_LAST_RELEASED), true);
        //
        //            for (String line : FileUtilities.in(EmojiAnnotations.class, filename)) {
        //                line = line.trim();
        //                lineNumber++;
        //                if (line.isEmpty() || line.startsWith("#")) {
        //                    continue;
        //                }
        //                if (line.contains("closed")) {
        //                    int debug = 0;
        //                }
        //                lineCount++;
        //                for (String string : ei.set(line)) {
        //                    if (Emoji.ASCII_LETTERS.containsSome(string)) {
        //                        UnicodeSet overlap = new UnicodeSet().addAll(string).retainAll(Emoji.ASCII_LETTERS);
        //                        String withPosition = line.replaceAll("("+overlap+")", "###$1");
        //                        throw new IllegalArgumentException(lineNumber + "\tStrange line with ASCII emoji: " + overlap + "; "+ withPosition);
        //                    }
        //                    if (EmojiData.EMOJI_DATA.skipEmojiSequence(string)) {
        //                        continue;
        //                    }
        //                    string = EmojiData.EMOJI_DATA.normalizeVariant(string);
        //                    for (String item : ei.newLabel) {
        //                        add(fixAnnotation(item), string);
        //                    }
        //                }
        //            }
        //            if (SHOW) System.out.println(lineCount + "\tannotation lines from " + filename);
        //        }
        //        addOther("-apple", EmojiData.EMOJI_DATA.getChars());
        //        addOther("-android", EmojiData.EMOJI_DATA.getChars());
        //        addOther("", EmojiData.EMOJI_DATA.getChars());

        //        final Set<String> personAndroid = getValues("person-android");
        //        if (personAndroid != null) {
        //            UnicodeSet temp = new UnicodeSet(Emoji.APPLE)
        //            .removeAll(personAndroid)
        //            .removeAll(getValues("nature-android"))
        //            .removeAll(getValues("object-android"))
        //            .removeAll(getValues("place-android"))
        //            .removeAll(getValues("symbol-android"));
        //            System.out.println(temp.size() + " other-android: " + temp.toPattern(false));
        //            for (String s : temp) {
        //                add("other-android", s);
        //            }
        //        }
        //
        //        UnicodeSet temp = new UnicodeSet(Emoji.EMOJI_CHARS)
        //        .removeAll(getValues("person"))
        //        .removeAll(getValues("nature"))
        //        .removeAll(getValues("object"))
        //        .removeAll(getValues("place"))
        //        .removeAll(getValues("symbol"))
        //        .removeAll(getValues("flag"));
        //        System.out.println(temp.size() + " other: " + temp.toPattern(false));
        //        for (String s : temp) {
        //            add("other", s);
        //        }


        //        for (String s : FITZ_MINIMAL) {
        //            ANNOTATIONS_TO_CHARS.add("fitz-minimal", s);
        //        }
        //        for (String s : FITZ_OPTIONAL) {
        //            ANNOTATIONS_TO_CHARS.add("fitz-optional", s);
        //        }
        // for programmatic additions, take this and modify
        //        for (String s : Emoji.EMOJI_CHARS) {
        //            String charName = UCharacter.getName(s.codePointAt(0));
        //            if (charName.contains("MARK")) {
        //                ANNOTATIONS_TO_CHARS.add("mark", s);
        //            }
        //        }
        //        for (int cp1 = Emoji.FIRST_REGIONAL; cp1 <= Emoji.LAST_REGIONAL; ++cp1) {
        //            for (int cp2 = Emoji.FIRST_REGIONAL; cp2 <= Emoji.LAST_REGIONAL; ++cp2) {
        //                String emoji = new StringBuilder().appendCodePoint(cp1).appendCodePoint(cp2).toString();
        //                if (EmojiData.EMOJI_DATA.getChars().contains(emoji)) {
        //                    add("flag", emoji);
        //                }
        //                //String regionCode = GenerateEmoji.getFlagCode(emoji);
        //            }
        //        }
        // get extra names
        //        for (String name : CountryCodeConverter.names()) {
        //            String regionCode = CountryCodeConverter.getCodeFromName(name);
        //            if (regionCode == null || regionCode.length() != 2) {
        //                continue;
        //            }
        //            if (regionCode.equals("RS") 
        //                    && name.contains("montenegro")) {
        //                continue;
        //            }
        //            String emoji = Emoji.getEmojiFromRegionCode(regionCode);
        //            //System.out.println(regionCode + "=>" + name);
        //            addParts(emoji, name);
        //        }
        freeze();
        Map<String,UnicodeSet> _TO_UNICODE_SET = new HashMap<>();
        for (Entry<String, Set<String>> entry : this.keyValuesSet()) {
            _TO_UNICODE_SET.put(entry.getKey(), new UnicodeSet().addAll(entry.getValue()).freeze());
        }
        TO_UNICODE_SET = Collections.unmodifiableMap(_TO_UNICODE_SET);
        UnicodeSet annotationCharacters = new UnicodeSet().addAll(valuesSet());
        if (!annotationCharacters.containsAll(EmojiData.EMOJI_DATA.getChars())) {
            UnicodeSet missing = new UnicodeSet().addAll(EmojiData.EMOJI_DATA.getChars()).removeAll(annotationCharacters);
            throw new IllegalArgumentException("Missing annotations: " + missing.toPattern(false));
        }
    }

    //    static final UnicodeMap<String> TTS = new UnicodeMap<>();
    //    static {
    //        for (String line : FileUtilities.in(GenerateOtherAnnotations.class, "en-tts.tsv")) {
    //            if (line.startsWith("#") || line.isEmpty()) continue;
    //            List<String> list = TAB.splitToList(line);
    //            String source = org.unicode.text.utility.Utility.fromHex(list.get(0));
    //            TTS.put(source, list.get(1));
    //        }
    //        TTS.freeze();
    //    }

    public UnicodeSet getUnicodeSet(String annotation) {
        return TO_UNICODE_SET.get(annotation);
    }

    public Set<Entry<String, UnicodeSet>> getStringUnicodeSetEntries() {
        return TO_UNICODE_SET.entrySet();
    }

    //    private void addOther(String suffix, UnicodeSet core) {
    //        final Set<String> personApple = getValues("person" + suffix);
    //        if (personApple != null) {
    //            UnicodeSet temp = new UnicodeSet(core)
    //            .removeAll(personApple)
    //            .removeAll(getValues("nature" + suffix))
    //            .removeAll(getValues("object" + suffix))
    //            .removeAll(getValues("place" + suffix))
    //            .removeAll(getValues("symbol" + suffix));
    //            if (SHOW) System.out.println(temp.size() + " other" + suffix
    //                    + ": " + temp.toPattern(false));
    //            for (String s : temp) {
    //                add("other" + suffix, s);
    //            }
    //        }
    //    }
    //
    //    private String fixAnnotation(String item) {
    //        String result = item.toLowerCase(Locale.ENGLISH);
    //        return result
    //                .replace("minimal", "primary")
    //                .replace("optional", "secondary");
    //    }

    //    public void addParts(String emoji, String name) {
    //        name = name.toLowerCase(Locale.ENGLISH);
    //        for (String namePart : name.split("[- ,&\\(\\)]+")) {
    //            if (STOP_WORDS.contains(namePart)) {
    //                continue;
    //            }
    //            if (namePart.startsWith("d’") || namePart.startsWith("d'")) {
    //                namePart = namePart.substring(2);
    //            }
    //            add(namePart, emoji);
    //        }
    //    }


    public static String getNameAndAnnotations(String s, CLDRFile english, UnicodeMap<Annotations> data, final Set<String> keywords) {
        String shortName = null;
        switch(s) {
        // put into CLDR
        case Emoji.MALE:
            shortName = "male sign"; 
            addString(keywords, "male, man");
            return shortName;
        case Emoji.FEMALE:
            shortName = "female sign"; 
            addString(keywords, "female, woman");
            return shortName;
        case Emoji.HEALTHCARE:
            shortName = "medical symbol"; 
            addString(keywords, "staff, aesculapius");
            return shortName;
        case Emoji.UN:
            shortName = "United Nations"; 
            keywords.add("UN");
            return shortName;
        case "🌾":
            addString(keywords, "farmer, rancher, gardener");
            break;
        }
        Annotations datum = data.get(s);
        if (datum != null) {
            shortName = fix(s, datum.tts);
            keywords.addAll(datum.annotations);
        } else if (Emoji.REGIONAL_INDICATORS.containsAll(s)) {
            String countryCode = Emoji.getFlagCode(s);
            shortName = english.getName(CLDRFile.TERRITORY_NAME, countryCode);
            keywords.addAll(Collections.singleton("flag"));
        } else if (s.contains(Emoji.KEYCAP_MARK_STRING)) {
            shortName = "keycap " + UCharacter.getName(s.codePointAt(0)).toLowerCase(Locale.ENGLISH);
            keywords.addAll(ImmutableSet.of("keycap", UCharacter.getName(s.codePointAt(0)).toLowerCase(Locale.ENGLISH)));
        } else if (EmojiData.MODIFIERS.containsSome(s)) {
            String rem = EmojiData.MODIFIERS.stripFrom(s, false);
            String type = EmojiData.shortModName(rem.codePointAt(0)).toLowerCase(Locale.ENGLISH);
            s = EmojiData.MODIFIERS.stripFrom(s, true);
            shortName = getNameAndAnnotations(s, english, data, keywords) + ", " + type;
            if (shortName != null) {
                shortName = shortName.toLowerCase(Locale.ENGLISH);
                keywords.add(type);
            }
        } else if (s.contains(Emoji.JOINER_STRING)) {
            shortName = EmojiData.EMOJI_DATA.getName(s, true);
            for (int cp : CharSequences.codePoints(s)) {
                switch (cp) {
                case Emoji.EMOJI_VARIANT:
                case Emoji.JOINER:
                    continue;
                case 0x1F33E:
                    addString(keywords, "farmer, rancher, gardener");
                    break;
                case 0x1F373:
                    addString(keywords, "cook, chef");
                    break;
                case 0x1F3ED:
                    addString(keywords, "industrial, assembly, factory, worker");
                    break;
                case 0x2695:
                    addString(keywords, "healthcare, doctor, nurse, therapist");
                    break;
                case 0x1F527:
                    addString(keywords, "tradesperson, mechanic, plumber, electrician");
                    break;
                case 0x1F4BC:
                    addString(keywords, "office, business, manager, architect, white-collar");
                    break;
                case 0x1F52C:
                    addString(keywords, "scientist, engineer, mathematician, chemist, physicist, biologist");
                    break;
                case 0x1F3A4:
                    addString(keywords, "singer, entertainer, rock, star, actor");
                    break;
                case 0x1F393:
                    addString(keywords, "student, graduate");
                    break;
                case 0x1F3EB:
                    addString(keywords, "professor, instructor, teacher");
                    break;
                case 0x1F4BB:
                    addString(keywords, "technologist, coder, software, developer, inventor");
                    break;
                }
                getNameAndAnnotations(UTF16.valueOf(cp), english, data, keywords);
            }
        }
        if (MALE_SET.contains(s)) {
            keywords.add("male");
        } else if (FEMALE_SET.contains(s)) {
            keywords.add("female");
        }
        return shortName;
    }

    private static String fix(String s, String tts) {
        switch(s) { // add to CLDR
        case "👮":
            return "police officer";
        case "🕵":
            return "sleuth";
        case "💂":
            return "guard";
        case "👷":
            return "construction worker";
        case "👳":
            return "person with turban";
        case "👱":
            return "blond person";
        case "🙍":
            return "person frowning";
        case "🙎":
            return "person pouting";
        case "🙅":
            return "person gesturing not ok";
        case "🙆":
            return "person gesturing ok";
        case "💁":
            return "person tipping hand";
        case "🙋":
            return "person raising hand";
        case "🙇":
            return "person bowing";
        case "🤦":
            return "person facepalming";
        case "🤷":
            return "person shrugging";
        case "💆":
            return "person getting face massage";
        case "💇":
            return "person getting haircut";
        case "🚶":
            return "person walking";
        case "🏃":
            return "person running";
        case "👯":
            return "people partying";
        case "🏌":
            return "person golfing";
        case "🏄":
            return "person surfing";
        case "🚣":
            return "person rowing boat";
        case "🏊":
            return "person swimming";
        case "⛹":
            return "person with ball";
        case "🏋":
            return "person weight lifting";
        case "🚴":
            return "person biking";
        case "🚵":
            return "person mountain biking";
        case "🤸":
            return "person doing cartwheel";
        case "🤼":
            return "people wrestling";
        case "🤽":
            return "person playing water polo";
        case "🤾":
            return "person playing handball";
        case "🤹":
            return "person juggling";
        default:         
            return tts;
        }
    }

    private static void addString(Set<String> keywords, String string) {
        for (String s : string.split(",")) {
            keywords.add(s.trim());
        }
    }

    public String getShortName(String s) {
        return shortNames.get(s);
    }

    public static void main(String[] args) {
        TreeSet<String> sorted = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        EmojiData.EMOJI_DATA.getChars().addAllTo(sorted);
        //        for (String s : sorted) {
        //            Set<String> plainAnnotations = CandidateData.getInstance().getAnnotations(s);
        //            if (plainAnnotations.isEmpty()) {
        //                plainAnnotations = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s);
        //            }
        //            System.out.println(s + "\t" + CollectionUtilities.join(plainAnnotations, " | "));
        //        }
        //        UnicodeMap<Annotations> data = Annotations.getData("en");
        //        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        //        final Set<String> keywords = new LinkedHashSet<>();

        LinkedHashSet<String> missing = new LinkedHashSet<>();

        for (String s : sorted) {
            //            keywords.clear();
            //            String shortName = getNameAndAnnotations(s, english, data, keywords);
            String shortName = EmojiAnnotations.ANNOTATIONS_TO_CHARS.shortNames.get(s);
            Set<String> keywords = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s);
            if (shortName == null) {
                missing.add(s);
                continue;
            }
            //            if (s.endsWith("♀️") && !s.equals("♀️") && !EmojiData.MODIFIERS.containsSome(s)) {
            //                int first = s.codePointAt(0);
            //                System.out.println("case \"" + UTF16.valueOf(first) + "\": return \""
            //                        + shortName.replace("woman", "person")
            //                        .replace("women", "people")
            //                        .replace("female", "person")
            //                        + "\";");
            //                //             case "👮": return "police officer";
            //
            //            }
            System.out.println(s + "\t" + shortName + "\t" + CollectionUtilities.join(keywords, " | "));
        }
        System.out.println("Missing: " + missing);
    }
}
