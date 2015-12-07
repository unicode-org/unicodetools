package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.text.tools.EmojiData.DefaultPresentation;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class GenerateOtherAnnotations {

    private static final Set<String> SKIP = new HashSet<>(GenerateEmoji.GROUP_ANNOTATIONS);
    static {
        SKIP.add("flag");
    }

    /*
de:
U+01F004<tab>🀄<tab>MAHJONG TILE RED DRAGON<tab>Mahjong-Stein, roter Drache, Mahjong<tab><tab>N

as:
U+00A9<tab>©<tab>COPYRIGHT SIGN<tab>Copyright sign, Copyright<tab><tab>স্বত্বাধিকাৰ চিন, স্বত্বাধিকাৰ<tab>Copyright sign, Copyright<tab>N

Special
ta.tsv
pa.tsv
te.tsv
or.tsv
ml.tsv
kn.tsv
mr.tsv
gu.tsv
hi.tsv
bn.tsv
as.tsv
     */

    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);

    static final Splitter TAB = Splitter.on("\t").trimResults();
    static final Splitter COMMA = Splitter.on(",").trimResults();
    static final Splitter SPACE = Splitter.on(" ").trimResults();

    static final LocaleDisplayNames ENGLISH_DISPLAY_NAMES = LocaleDisplayNames.getInstance(new ULocale("en"), DialectHandling.STANDARD_NAMES);

    public static class AnnotationData {
        final ULocale locale;
        final UnicodeMap<Set<String>> map = new UnicodeMap<>();
        final UnicodeMap<String> tts = new UnicodeMap<>();
        AnnotationData(String file) {
            this(new ULocale(file.substring(0,file.indexOf('.'))));
        }
        public AnnotationData(ULocale inLocale) {
            locale = inLocale;
        }
        AnnotationData freeze() {
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
    }

    static final AnnotationData english = getEnglish();
    
    static final Set<String> SORTED_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, english.map.keySet());

    public static String dir = Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/emoji/annotations/";

    public static void main(String[] args) throws IOException {
        if (false) showSimple();

        UnicodeSet missing = new UnicodeSet();
        printXml(english, missing);
        printText(english);

        for (String file : new File(dir).list()) {
            if (!file.endsWith(".tsv")) {
                continue;
            }
            AnnotationData data = load(dir, file);
            printXml(data, missing);
            printText(data);
        }

        //        for (String s: GenerateEmoji.SORTED_EMOJI_CHARS_SET) {
        //            if (!missing.contains(s)) {
        //                continue;
        //            }
        //            String englishAnn = english.get(s);
        //            int pos = englishAnn.indexOf(';');
        //            System.out.println("U+" + Utility.hex(s, ",U+") 
        //                    + "\t" + s 
        //                    + "\t" + englishAnn.substring(0,pos) 
        //                    + "\t" + englishAnn.substring(pos+1).trim());
        //        }
    }


    private static void showSimple() {
        int count = 0;
        final UnicodeSet textStyle = emojiData.getDefaultPresentationSet(DefaultPresentation.text);
        for (String s : SORTED_EMOJI_CHARS_SET) {
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            String s2 = GenerateEmoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING);

            System.out.println(++count
                    + "\t" + GenerateEmoji.getHex(s)
                    + "\t" + s2
                    + "\t" + GenerateEmoji.getName(s, true)
                    );
        }
    }

    private static void printText(AnnotationData data) throws IOException {
        try (PrintWriter outText = BagFormatter.openUTF8Writer(Settings.OTHER_WORKSPACE_DIRECTORY + "Generated/emoji/", data.locale + ".tsv")) {
//#Code Image   TTS English TTS German  Annotations English Annotations German  Comments    INTERNAL
//U+1F600   =vlookup(A2,Internal!A:B,2,0)   grinning face   Lachender Smiley    face; grin  Lachender Smiley; Gesicht; lustig; lol       
            int line = 1;
            outText.println("#Code"
                    + "\tImage"
                    + "\tTTS English"
                    + "\tTTS " + data.locale.getDisplayName(ULocale.ENGLISH)
                    + "\t=\"TTS? \" & countif(E2:E,\"ERR\")"
                    + "\tAnnotations English"
                    + "\tAnnotations" + data.locale.getDisplayName(ULocale.ENGLISH)
                    + "\tComments"
                    + "\tINTERNAL");
            for (String s : GenerateEmoji.SORTED_EMOJI_CHARS_SET) {
                if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                    continue;
                }
                String tts = data.tts.get(s);
                String ttsString = tts == null ? "MISSING" : tts;
                Set<String> annotations = data.map.get(s);
                if (annotations == null) {
                    annotations = Collections.singleton("MISSING");
                } else if (tts != null) {
                    annotations = new LinkedHashSet<>(annotations);
                    annotations.remove(tts);
                }
                String annotationString = annotations == null ? "MISSING" : CollectionUtilities.join(annotations, "; ");
                outText.println("U+" + Utility.hex(s, 4, " U+")
                        + "\t=vlookup(A" + (++line) + ",Internal!A:B,2,0)"
                        + "\t" + english.tts.get(s) 
                        + "\t" + ttsString 
                        + "\t=if(countif(D:D,D2)=1,\"\",\"ERR\")"
                        + "\t" + CollectionUtilities.join(english.map.get(s), "; ")
                        + "\t" + annotationString
                        + "\t" // comment
                        + "\t\u00A0" // comment
                        );
            }
        }
    }
    
    private static LocaleData printXml(AnnotationData data, UnicodeSet missing) throws IOException {
        final boolean isEnglish = data.locale.equals(ULocale.ENGLISH);
        LocaleData ld = LocaleData.getInstance(data.locale);
        try (PrintWriter outText = BagFormatter.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "/annotations/", data.locale + ".xml")) {
            outText.append(GenerateEmoji.ANNOTATION_HEADER
                    + "\t\t<language type='" + data.locale + "'/>\n"
                    + "\t</identity>\n"
                    + "\t<annotations>\n");
            for (String s : GenerateEmoji.SORTED_EMOJI_CHARS_SET) {
                Set<String> annotations = data.map.get(s);
                if (annotations == null) {
                    annotations = Collections.emptySet();
                }
                if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                    continue;
                }
                //                    if (annotations.isEmpty() && Emoji.isRegionalIndicator(s.codePointAt(0))) {
                //                        String regionCode = Emoji.getRegionCodeFromEmoji(s);
                //                        String name = ldn.regionDisplayName(regionCode);
                //                        if (!name.equals(regionCode)) {
                //                            annotations = Collections.singleton(name);
                //                        } else {
                //                            System.out.println(locale + "\tmissing\t" + regionCode);
                //                        }
                //                    }
                if (annotations.isEmpty()) {
                    missing.add(s);
                    continue;
                }

                String annotationString = CollectionUtilities.join(annotations, "; ");
                if (!isEnglish) {
                    LinkedHashSet<String> list = new LinkedHashSet<>();
                    list.add(english.tts.get(s));
                    list.addAll(english.map.get(s));
                    String englishAnnotationString = CollectionUtilities.join(list, "; ");
                    outText.append("\n\t\t<!-- " + fix(englishAnnotationString, ld) + " -->\n");
                }
                outText.append("\t\t<annotation cp='")
                .append(new UnicodeSet().add(s).toPattern(false));
                String ttsString = data.tts.get(s);
                if (ttsString != null) {
                    outText.append("' tts='")
                    .append(fix(ttsString, ld));
                }
                outText.append("'"
                        + (isEnglish ? "" : " draft='provisional'")
                        + ">")
                .append(fix(annotationString, ld))
                .append("</annotation>\n")
                ;
            }
            outText.write("\t</annotations>\n"
                    + "</ldml>");
        }
        if (missing.isEmpty()) {
            System.out.println(missing);
        }
        return ld;
    }

    private static AnnotationData load(String dir, String file) {
        AnnotationData data = new AnnotationData(file);
        LocaleData ld = LocaleData.getInstance(data.locale);
        Splitter localeSplitter = data.locale.equals(ULocale.JAPANESE) ? SPACE : COMMA;
        LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(data.locale, DialectHandling.STANDARD_NAMES);
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
            List<String> parts = TAB.splitToList(line);
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



    static final int[] delimiters = {LocaleData.QUOTATION_START, LocaleData.QUOTATION_END};

    private static String fix(String source, LocaleData ld) {
        if (source.contains("\"")) {
            // replace every other " with these:
            StringBuilder b = new StringBuilder();
            int current = 0;
            int lastPos = 0;
            while (true) {
                int pos = source.indexOf('"', lastPos);
                if (pos == -1) {
                    b.append(source.substring(lastPos));
                    source = b.toString();
                    break;
                }
                b.append(source.substring(lastPos, pos));
                b.append(ld.getDelimiter(delimiters[current]));
                lastPos = pos+1;
                current = (current + 1) % 2;
            }
        }
        return TransliteratorUtilities.toXML.transform(source);
    }


    private static AnnotationData getEnglish() {
        AnnotationData data = new AnnotationData(ULocale.ENGLISH);
        for (String line : FileUtilities.in(GenerateOtherAnnotations.class, "en-tts.tsv")) {
            if (line.startsWith("#") || line.isEmpty()) continue;
            List<String> list = TAB.splitToList(line);
            String source = org.unicode.text.utility.Utility.fromHex(list.get(0));
            data.tts.put(source, list.get(1));
        }

        for (String s : new UnicodeSet(Emoji.EMOJI_CHARS).addAll(Emoji.APPLE_COMBOS)) {
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
        Emoji.EMOJI_CHARS.addAllTo(missing);

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
            System.out.println(annotation + "\t" + sorted.size() + "\t" + CollectionUtilities.join(sorted, " "));
        }
        return data.freeze();
    }

    private static void showAndRemove(String label, Set<String> missing) {
        Set<String> plain = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getValues(label);
        missing.removeAll(plain);
        System.out.println(label + "\t\t" + plain.size() + "\t" + CollectionUtilities.join(plain, " "));
    }


    private static void showDiff(String label, String a, Set<String> aSet, String b, Set<String> bSet) {
        showDiff2(label, a, aSet, b, bSet, true);
        showDiff2(label, b, bSet, a, aSet, true);
        showDiff2(label, a, bSet, b, aSet, false);
    }


    private static void showDiff2(String label, String a, Set<String> aSet, String b, Set<String> bSet, boolean diff) {
        Set<String> aMinusB = new TreeSet<String>(GenerateEmoji.EMOJI_COMPARATOR);
        aMinusB.addAll(aSet);
        String rel; 
        if (diff) {
            aMinusB.removeAll(bSet);
            rel = " ⊖ ";
        } else {
            aMinusB.retainAll(bSet);
            rel = " ∩ ";
        }
        System.out.println(label + "\t" + a + rel + b + "\t" + aMinusB.size() + "\t" + CollectionUtilities.join(aMinusB, " "));
    }

}
