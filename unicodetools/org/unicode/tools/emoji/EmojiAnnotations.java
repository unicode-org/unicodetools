package org.unicode.tools.emoji;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.text.utility.Birelation;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class EmojiAnnotations extends Birelation<String,String> {
    public enum Status {missing, gender, constructed, found}

    final Map<String,UnicodeSet> TO_UNICODE_SET;
    final private UnicodeMap<String> shortNames = new UnicodeMap<>();
    final private UnicodeMap<Status> statusValues = new UnicodeMap<>();

    // Add to CLDR
    private static final UnicodeSet MALE_SET = new UnicodeSet("[👦  👨  👴 🎅   🤴  🤵  👲🕴 🕺]");
    private static final UnicodeSet FEMALE_SET = new UnicodeSet("[ 👧  👩  👵 🤶👸👰🤰💃]");


    //    private static final Splitter TAB = Splitter.on("\t").trimResults();
    //
    //    private static final boolean SHOW = false;

    //    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("the", "of", "for", "a", "and", "state", 
    //            "c�te", "verde▪cape", "dhekelia", "akrotiri", "comros", "pdr", "jamahiriya", "part",
    //            "yugoslav", "tfyr", "autonomous", "rawanda", "da", "rb", "yugoslavia",
    //            "states", "sar", "people's", "minor",
    //            "sts."));

    public static final EmojiAnnotations          ANNOTATIONS_TO_CHARS        = new EmojiAnnotations("en", EmojiOrder.STD_ORDER.codepointCompare);
    public static final AnnotationSet ANNOTATION_SET = Annotations.getDataSet("en");
    /**
     * @deprecated Use {@link #EmojiAnnotations(String,Comparator<String>,String...)} instead
     */
    public EmojiAnnotations(Comparator<String> codepointCompare, String... filenames) {
        this("en", codepointCompare, filenames);
    }

    public EmojiAnnotations(String localeString, Comparator<String> codepointCompare, String... filenames) {
        super(new TreeMap(EmojiOrder.FULL_COMPARATOR), 
                new HashMap(), 
                TreeSet.class, 
                TreeSet.class, 
                EmojiOrder.UCA_COLLATOR, 
                EmojiOrder.FULL_COMPARATOR);

        //        for (String s : sorted) {
        //            Set<String> plainAnnotations = CandidateData.getInstance().getAnnotations(s);
        //            if (plainAnnotations.isEmpty()) {
        //                plainAnnotations = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s);
        //            }
        //            System.out.println(s + "\t" + CollectionUtilities.join(plainAnnotations, " | "));
        //        }
        final AnnotationSet annotationData = Annotations.getDataSet(localeString);
        if (annotationData == null) {
            throw new IllegalArgumentException("No annotation data for " + localeString);
        }
        //Loader loader = new Loader(CLDRConfig.getInstance().getCLDRFile(localeString, true), annotationData);
        

        final Set<String> keywords = new LinkedHashSet<>();
        Output<String> outShortName = new Output<>();
        for (String s : EmojiData.EMOJI_DATA.getChars()) {
            keywords.clear();
            final String sNoVariants = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
            outShortName.value = annotationData.getShortName(s);
            Status status = Status.found;
            if (outShortName.value == null || outShortName.value.contains(Annotations.ENGLISH_MARKER)) {
                status = Status.missing;
            } else if (outShortName.value.contains(Annotations.BAD_MARKER)) {
                status = Status.gender;
            }
            keywords.addAll(annotationData.getKeywords(s));
            if (keywords.isEmpty()) {
                status = Status.missing; // incomplete
            }
            for (String annotation : keywords) {
                add(annotation, s);
            }
            if (!s.equals(sNoVariants)) {
                for (String annotation : keywords) {
                    add(annotation, sNoVariants);
                }
            }
            statusValues.put(s,status);
            shortNames.put(s, outShortName.value);
            if (!s.equals(sNoVariants)) {
                shortNames.put(sNoVariants, outShortName.value);
            }
        }
        statusValues.freeze();
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
        //        UnicodeSet annotationCharacters = new UnicodeSet().addAll(valuesSet());
        //        if (!annotationCharacters.containsAll(EmojiData.EMOJI_DATA.getChars())) {
        //            UnicodeSet missing = new UnicodeSet().addAll(EmojiData.EMOJI_DATA.getChars()).removeAll(annotationCharacters);
        //            throw new IllegalArgumentException("Missing annotations: " + missing.toPattern(false));
        //        }
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

    static class Loader {
        final CLDRFile cldrFile;
        final UnicodeMap<Annotations> data;
        final SimpleFormatter sep;
        final boolean isEnglish;
        private final SimpleFormatter KEYCAP_PATTERN;
        private final SimpleFormatter COMBINE_PATTERN;
        
        static final UnicodeSet FAMILY_PLUS = new UnicodeSet(Emoji.FAMILY_MARKERS)
        .add(Emoji.JOINER)
        .add(Emoji.EMOJI_VARIANT)
        .freeze();
        static final String KISS = "\u2764\uFE0F\u200D\uD83D\uDC8B\u200D";
        static final String HEART = "\u2764\uFE0F\u200D";

        public Loader(CLDRFile cldrFile, UnicodeMap<Annotations> data) {
            this.cldrFile = cldrFile;
            this.isEnglish = cldrFile.getLocaleID().equals("en");
            this.data = data;
            //           <listPatternPart type="2">{0}, {1}</listPatternPart>  type="unit-short"
            this.KEYCAP_PATTERN = SimpleFormatter.compile(cldrFile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"keycap\"]"));
            this.COMBINE_PATTERN = SimpleFormatter.compile(cldrFile.getStringValue("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]"));
            sep = SimpleFormatter.compile(cldrFile.getStringValue("//ldml/listPatterns/listPattern[@type=\"unit-short\"]/listPatternPart[@type=\"2\"]"));
        }

        private Status getNameAndAnnotations(String s, final Set<String> keywordsToAppendTo, Output<String> outShortName) {
            if (s.equals("💃")) {
                int debug = 0;
            }
            outShortName.value = null;
            // TODO put into CLDR
            if (isEnglish) {
                switch(s) {
                case Emoji.UN:
                    outShortName.value = "United Nations"; 
                    keywordsToAppendTo.add("UN");
                    return Status.found;
                }
            }
            Annotations datum = data.get(s);
            // try without variant
            if (datum == null) {
                String s1 = s.replace(Emoji.EMOJI_VARIANT_STRING,"");
                if (!s.equals(s1)) {
                    datum = data.get(s1);
                }
            }
            if (datum != null) {
                outShortName.value = fix(s, sep, null, datum.getShortName());
                keywordsToAppendTo.addAll(datum.getKeywords());
                return Status.found;
            } else if (Emoji.REGIONAL_INDICATORS.containsAll(s)) {
                String countryCode = Emoji.getFlagCode(s);
                outShortName.value = cldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode);
                keywordsToAppendTo.addAll(Collections.singleton("flag"));
                return outShortName.value == null ? Status.missing : Status.found;
            } else if (s.contains(Emoji.KEYCAP_MARK_STRING)) {
                Annotations keycapDatum = data.get("🔟");
                outShortName.value = fix(s, sep, outShortName.value, KEYCAP_PATTERN.format(UTF16.valueOf(s.charAt(0))));
                if (keycapDatum != null && keycapDatum.getShortName().contains("#")) {
                    keywordsToAppendTo.addAll(keycapDatum.getKeywords());
                }
                return Status.found;
            } else if (EmojiData.MODIFIERS.containsSome(s)) {
                // TODO Handle multiple modifiers
                String rem = EmojiData.MODIFIERS.stripFrom(s, false);
                s = EmojiData.MODIFIERS.stripFrom(s, true);
                s = EmojiData.EMOJI_DATA.addEmojiVariants(s); // modifiers replace EV characters.
                if (s.isEmpty()) {
                    return Status.missing;
                }
                Status status = getNameAndAnnotations(s, keywordsToAppendTo, outShortName);
                Annotations skinDatum = data.get(rem.codePointAt(0));
                if (skinDatum != null) {
                    outShortName.value = fix(s, sep, outShortName.value, skinDatum.getShortName());
                    keywordsToAppendTo.add(skinDatum.getShortName());
                    status = Status.found;
                } else {
                    status = Status.missing;
                }
                //String type = EmojiData.shortModNameZ(rem.codePointAt(0));
                //                if (status != Status.missing) {
                //                    outShortName.value = sep.format(outShortName.value.toLowerCase(Locale.ENGLISH), type);
                //                    keywordsToAppendTo.add(type);
                //                }
                return status;
            } else if (s.contains(Emoji.JOINER_STRING)) {
                // shortName = EmojiData.EMOJI_DATA.getName(s, true);
                Status status = Status.constructed;
                String base = null;
                //s = s.replace(Emoji.JOINER_STRING,"");
                if (s.contains(KISS)) {
                    Annotations familyDatum = data.get("💏");
                    s = s.replace(KISS, "");
                    if (familyDatum != null) {
                        base = familyDatum.getShortName(); 
                        keywordsToAppendTo.addAll(familyDatum.getKeywords());
                        status = Status.found;
                    } else {
                        status = Status.missing;
                    }
                } else if (s.contains(HEART)) {
                    Annotations familyDatum = data.get("💑");
                    s = s.replace(HEART, "");
                    if (familyDatum != null) {
                        base = familyDatum.getShortName(); 
                        keywordsToAppendTo.addAll(familyDatum.getKeywords());
                        status = Status.found;
                    } else {
                        status = Status.missing;
                    }
                } else if (FAMILY_PLUS.containsAll(s)) {
                    Annotations familyDatum = data.get("👪");//        <annotation cp="👪" type="tts">Familie</annotation>
                    if (familyDatum != null) {
                        base = familyDatum.getShortName(); 
                        keywordsToAppendTo.addAll(familyDatum.getKeywords());
                        status = Status.found;
                    } else {
                        status = Status.missing;
                    }
                } else if (Emoji.GENDER_MARKERS.containsSome(s)) {
                    String rem = Emoji.GENDER_MARKERS.stripFrom(s, false);
                    s = Emoji.GENDER_MARKERS.stripFrom(s, true);
                    Annotations familyDatum = data.get(rem.contains("♂") ? "👨" : "👩");//        <annotation cp="👪" type="tts">Familie</annotation>
                    if (familyDatum != null) {
                        outShortName.value = fix(s, sep, outShortName.value, familyDatum.getShortName());
                        keywordsToAppendTo.addAll(familyDatum.getKeywords());
                        status = Status.gender;
                    } else {
                        status = Status.missing;
                    }
                }
                for (int cp : CharSequences.codePoints(s)) {
                    if (cp == Emoji.EMOJI_VARIANT || cp == Emoji.JOINER) {
                        continue;
                    }
                    Annotations partDatum = data.get(cp);
                    if (partDatum != null) {
                        outShortName.value = fix(s, sep, outShortName.value, partDatum.getShortName());
                        keywordsToAppendTo.addAll(partDatum.getKeywords());
                    } else {
                        outShortName.value = fix(s, sep, outShortName.value, "???");
                        keywordsToAppendTo.add("???");
                        status = Status.missing;
                    }
                }
                if (base != null) {
                    outShortName.value = COMBINE_PATTERN.format(base, outShortName.value);
                }
                return status;
            }
            // TODO Add to CLDR
            //        if (MALE_SET.contains(s)) {
            //            keywords.add("male");
            //        } else if (FEMALE_SET.contains(s)) {
            //            keywords.add("female");
            //        }
            return Status.missing;
        }

        private String fix(String cps, SimpleFormatter simpleFormatter, String oldTts, String tts) {
            return oldTts == null ? tts : simpleFormatter.format(oldTts, tts);
        }
    }

    //    private static boolean addString(Set<String> keywords, String string) {
    //        boolean result = false;
    //        for (String s : string.split(",")) {
    //            result |= keywords.add(s.trim());
    //        }
    //        return result;
    //    }

    public String getShortName(String s) {
        return shortNames.get(s);
    }

    public Status getStatus(String s) {
        return CldrUtility.ifNull(statusValues.get(s), Status.missing);
    }
    
    public UnicodeSet getStatusKeys() {
        return statusValues.keySet();
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
            show(s, missing);
            String sNoVariants = s.replace(Emoji.EMOJI_VARIANT_STRING, "");
            if (!s.equals(sNoVariants)) {
                show(sNoVariants, missing);
            }
        }
        System.out.println("Missing: " + missing);
    }

    private static void show(String s, LinkedHashSet<String> missing) {
        String shortName = EmojiAnnotations.ANNOTATIONS_TO_CHARS.shortNames.get(s);
        Set<String> keywords = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s);
        if (shortName == null) {
            missing.add(s);
            return;
        }
        System.out.println(s + "\t" + shortName + "\t" + CollectionUtilities.join(keywords, " | "));
    }

    //    public static String stripModifiers(String source, Output<String> modsFound) {
    //        StringBuilder newSource = new StringBuilder();
    //        StringBuilder newMods = new StringBuilder();
    //        for (int cp : CharSequences.codePoints(source)) {
    //            if (EmojiData.MODIFIERS.contains(cp)) {
    //                
    //            }
    //        }
    //    }
}
