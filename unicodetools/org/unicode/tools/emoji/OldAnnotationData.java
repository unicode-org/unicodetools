package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class OldAnnotationData {
    private static final boolean NEWSTYLE = true;

    private static final ULocale NORWEGIAN_NB = new ULocale("nb");
    private static final ULocale NORWEGIAN_NO = new ULocale("no");
    private static final ULocale HEBREW = new ULocale("he");
    private static final ULocale HEBREW_OLD = new ULocale("iw");
    private static final ULocale ZHTW = new ULocale("zh_TW");
    static final LocaleDisplayNames ENGLISH_DISPLAY_NAMES = LocaleDisplayNames.getInstance(new ULocale("en"), DialectHandling.STANDARD_NAMES);
    static final Splitter TAB = Splitter.on("\t").trimResults();

    public final ULocale locale;
    public final UnicodeMap<Set<String>> map = new UnicodeMap<>();
    public final UnicodeMap<String> tts = new UnicodeMap<>();

    static final Set<String> GROUP_ANNOTATIONS = new HashSet<>(Arrays.asList(
    "default-text-style",
    "fitz-primary",
    "fitz-secondary",
    "nature",
    "nature-android",
    "nature-apple",
    "object",
    "object-android",
    "object-apple",
    "person",
    "person-android",
    "person-apple",
    "place",
    "place-android",
    "place-apple",
    "symbol",
    "symbol-android",
    "symbol-apple",
    "other-android",
    "flag",
    "other",
    "travel",
    "office",
    "animal",
    "sign",
    "word",
    "time",
    "food",
    "entertainment",
    "activity",
    "restaurant",
    "sound",
    "sport",
    "emotion",
    "communication",
    "education"
    ));
    
    private static final Set<String> SKIP = new HashSet<>(OldAnnotationData.GROUP_ANNOTATIONS);
    static {
        SKIP.add("flag");
    }

    OldAnnotationData(String file) {
        this(fixLocale(file));
    }

    private static ULocale fixLocale(String file) {
        String locale = file.substring(0,file.indexOf('.'));
        ULocale ulocale = new ULocale(locale);
        if (ulocale.equals(ZHTW)) {
            ulocale = ULocale.TRADITIONAL_CHINESE;
        } else if (ulocale.equals(HEBREW_OLD)) {
            ulocale = HEBREW;
        } else if (ulocale.equals(NORWEGIAN_NO)) {
            ulocale = NORWEGIAN_NB;
        }
        return ULocale.minimizeSubtags(ulocale);
    }
    public OldAnnotationData(ULocale inLocale) {
        locale = inLocale;
    }
    
    OldAnnotationData freeze() {
        if (!map.keySet().equals(tts.keySet())) {
            //map: flags
            //tts: groups
            throw new IllegalArgumentException(
                    new UnicodeSet(map.keySet()).removeAll(tts.keySet()).toPattern(false)
                    + " ; "
                    + new UnicodeSet(tts.keySet()).removeAll(map.keySet()).toPattern(false));
        }
        for (String s : tts) {
            Set<String> list = map.get(s);
            if (list.size() > 1) {
                list = new LinkedHashSet<>(list);
                list.remove(tts.get(s));
            }
            map.put(s, Collections.unmodifiableSet(list));
        }
        map.freeze();
        tts.freeze();
        return this;
    }

    static final Pattern FILENAME = Pattern.compile("(.*\\s-\\s)?(.*)"); // Emoji Annotations Project- Tier 1 - de.tsv

    static final Splitter SEMISPACE = Splitter.on(";").trimResults();

    static OldAnnotationData load(String dir, String infile) {
        // New file structure
        // "Emoji Annotations Project- Tier 1 - de.tsv"
        // or
        // de.tsv
        final Matcher matcher = FILENAME.matcher(infile);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Bad file name: " + infile);
        }
        String baseName = matcher.group(2);
        if (NEWSTYLE) {
            return loadNew(dir, infile, baseName);
        }
        String file = infile + ".tsv";

        OldAnnotationData data = new OldAnnotationData(file);
        //LocaleData ld = LocaleData.getInstance(data.locale);
        Splitter localeSplitter = data.locale.equals(ULocale.JAPANESE) ? GenerateOldestAnnotations.SPACE 
                : GenerateOldestAnnotations.COMMA;
        //LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(data.locale, DialectHandling.STANDARD_NAMES);
        int annotationField = 3;
        int lineCount = 0;
        for (String line : FileUtilities.in(dir, file)) {
            ++lineCount;
            if (line.startsWith("Unicode")) {
                if (line.contains("guide")) {
                    annotationField = 5;
                }
                continue;
            }
            if (line.isEmpty() || line.contains("REGIONAL INDICATOR SYMBOL")) {
                continue;
            }
            //de:
            //U+01F004<tab>🀄<tab>MAHJONG TILE RED DRAGON<tab>Mahjong-Stein, roter Drache, Mahjong<tab><tab>N
            //as:
            //U+00A9<tab>©<tab>COPYRIGHT SIGN<tab>Copyright sign, Copyright<tab><tab>স্বত্বাধিকাৰ চিন, স্বত্বাধিকাৰ<tab>Copyright sign, Copyright<tab>N
            List<String> parts = GenerateOldestAnnotations.TAB.splitToList(line);
            String chars = parts.get(1);
            if (parts.size() <= annotationField || chars.length() == 0) {
                System.out.println(data.locale + " (" + lineCount + "): Line/Chars too short, skipping: " + line);
                continue;
            }
            String annotationString = parts.get(annotationField);
            Set<String> annotations = new LinkedHashSet<>(localeSplitter.splitToList(annotationString));

            // the first item is special. It may be a TTS item
            // use heuristics to remove it from rest
            if (annotationField == 3) {
                Iterator<String> it = annotations.iterator();
                String firstItem = it.next();
                data.tts.put(chars, firstItem);
                //                    if (it.hasNext() && firstItem.contains(" ")) {
                //                        it.remove();
                //                    }
            }
            data.map.put(chars, Collections.unmodifiableSet(annotations));
        }
        return data;
    }

    static OldAnnotationData loadNew(String dir, String file, String baseName) {
        OldAnnotationData data = new OldAnnotationData(baseName);
        Splitter localeSplitter = SEMISPACE;
        int fieldTtsNative = 3;
        int fieldTtsFixed = 4;
        int fieldAnnotationNative = 7;
        int fieldAnnotationFixed = 8;
        int lineCount = 0;
        for (String line : FileUtilities.in(dir, file)) {
            ++lineCount;
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            List<String> parts = GenerateOldestAnnotations.TAB.splitToList(line);
            if (parts.size() <= fieldAnnotationFixed) {
                System.out.println(data.locale + " (" + lineCount + "): Line/Chars too short, skipping: " + parts);
                continue;
            }
            String chars;
            try {
                chars = Utility.fromHex(parts.get(0));
            } catch (Exception e) {
                System.out.println(data.locale + " (" + lineCount + "): Bad hex value: " + parts);
                continue;
            }
            String ttsString = parts.get(fieldTtsFixed);
            if (ttsString.isEmpty()) {
                ttsString = parts.get(fieldTtsNative);
            }
            boolean hasSemi = ttsString.contains(";");
            if (ttsString.contains(",")) {
                System.err.println(data.locale 
                        + "\t" + parts.get(0) 
                        + "\thas commas "
                        + (hasSemi ? "or semicolons " : "")
                        + "in the TTS string. Should be a single name."
                        + "\t" + ttsString);
            } else if (hasSemi) {
                System.err.println(data.locale 
                        + "\t" + parts.get(0) 
                        + "\thas semicolons in the TTS string. Should be a single name."
                        + "\t" + ttsString);
            }

            data.tts.put(chars, ttsString);
            String annotationString = parts.get(fieldAnnotationFixed);
            if (annotationString.isEmpty()) {
                annotationString = parts.get(fieldAnnotationNative);
            }
            if (annotationString.contains(",")) {
                System.err.println(data.locale 
                        + "\t" + parts.get(0) 
                        + "\thas commas in the annotation string, converting to semicolons:"
                        + "\t" + annotationString);
                annotationString = annotationString.replace(",", ";");
            }
            Set<String> annotations = new LinkedHashSet<>(localeSplitter.splitToList(annotationString));
            annotations.remove(ttsString);
            data.map.put(chars, Collections.unmodifiableSet(annotations));
        }
        return data;
    }


     static OldAnnotationData getEnglishOldRaw() {
        OldAnnotationData data = new OldAnnotationData(ULocale.ENGLISH);
        for (String line : FileUtilities.in(GenerateOldestAnnotations.class, "en-tts.tsv")) {
            if (line.startsWith("#") || line.isEmpty()) continue;
            List<String> list = TAB.splitToList(line);
            String source = org.unicode.text.utility.Utility.fromHex(list.get(0));
            data.tts.put(source, list.get(1));
        }

        for (String s : new UnicodeSet(EmojiData.EMOJI_DATA.getChars()).addAll(EmojiData.EMOJI_DATA.getZwjSequencesNormal())) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                String regionCode = Emoji.getRegionCodeFromEmoji(s);
                String name = ENGLISH_DISPLAY_NAMES.regionDisplayName(regionCode);
                result.add(name);
                data.tts.put(s, name);
            } else {
                result.add(data.tts.get(s));
            }
            for (String annotation : EmojiAnnotations.ANNOTATIONS_TO_CHARS.getKeys(s)) {
                if (SKIP.contains(annotation)) {
                    continue;
                }
                result.add(annotation);
            }
            data.map.put(s, result);
        }

        Set<String> missing = new TreeSet<>(GenerateEmoji.EMOJI_COMPARATOR);
        EmojiData.EMOJI_DATA.getChars().addAllTo(missing);

        //        for (String label : Arrays.asList("people",
        //                "nature",
        //                "objects",
        //                "places",                
        //                "symbols")) {
        //            Set<String> plain = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(label);
        //            missing.removeAll(plain);
        //            Set<String> apple = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(label+"-apple");
        //            Set<String> android = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(label+"-android");
        //
        //            showDiff(label, "apple", apple, "android", android);
        //
        //            Set<String> joint = new HashSet<String>(apple);
        //            joint.addAll(android);
        //
        //            showDiff(label, "(apple ∪ android)", joint, "plain", plain);
        //
        //            System.out.println();
        //        }
        showAndRemove("flag", missing);

        System.out.println("missing" + "\t\t" + missing.size() + "\t" + CollectionUtilities.join(missing, " "));


        for (String annotation : EmojiAnnotations.ANNOTATIONS_TO_CHARS.keySet()) {
            Set<String> sorted = new TreeSet<String>(GenerateEmoji.EMOJI_COMPARATOR);
            sorted.addAll(EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(annotation));
            // System.out.println(annotation + "\t" + sorted.size() + "\t" + CollectionUtilities.join(sorted, " "));
        }
        return data.freeze();
    }

    private static void showAndRemove(String label, Set<String> missing) {
        Set<String> plain = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(label);
        missing.removeAll(plain);
        System.out.println(label + "\t\t" + plain.size() + "\t" + CollectionUtilities.join(plain, " "));
    }
}