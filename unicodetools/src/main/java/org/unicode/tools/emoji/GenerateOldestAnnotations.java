package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class GenerateOldestAnnotations {

    private static final Set<String> ANNOTATED_CHARS = new UnicodeSet(EmojiData.EMOJI_DATA.getSortingChars())
    .removeAll(EmojiData.EMOJI_DATA.getModifierSequences())
    .removeAll(EmojiData.EMOJI_DATA.getFlagSequences())
    .removeAll(EmojiData.EMOJI_DATA.getZwjSequencesAll())
    .addAllTo(new TreeSet<String>(EmojiOrder.STD_ORDER.codepointCompare));

    private static final String MISSING = "";
    
    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);

    static final Splitter TAB = Splitter.on("\t").trimResults();
    static final Splitter COMMA = Splitter.on(",").trimResults();
    static final Splitter SPACE = Splitter.on(" ").trimResults();


    static final OldAnnotationData english = OldAnnotationData.getEnglishOldRaw();

    static final Set<String> SORTED_EMOJI_CHARS_SET
    = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, english.map.keySet());

    public static String dir = Settings.Output.GEN_DIR + "emoji/annotations/";

    public static void main(String[] args) throws IOException {
        if (false) showSimple();

        UnicodeSet missing = new UnicodeSet();
        printXml(english, missing);
        printText(english);

        for (String file : new File(dir).list()) {
            if (!file.endsWith(".tsv")) {
                continue;
            }
            OldAnnotationData data = OldAnnotationData.load(dir, file);
            printXml(data, missing);
            //printText(data);
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
        for (String s : SORTED_EMOJI_CHARS_SET) {
            if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                continue;
            }
            String s2 = EmojiData.EMOJI_DATA.addEmojiVariants(s);
            // Emoji.getEmojiVariant(s, Emoji.EMOJI_VARIANT_STRING);

            System.out.println(++count
                    + "\t" + Emoji.toUHex(s)
                    + "\t" + s2
                    + "\t" + EmojiData.EMOJI_DATA.getName(s)
                    );
        }
    }

    private static void printText(OldAnnotationData data) throws IOException {
        try (PrintWriter outText = FileUtilities.openUTF8Writer(Settings.Output.GEN_DIR + "emoji/tts/", data.locale + ".tsv")) {
            //#Code Image   TTS English TTS German  Annotations English Annotations German  Comments    INTERNAL
            //U+1F600   =vlookup(A2,Internal!A:B,2,0)   grinning face   Lachender Smiley    face; grin  Lachender Smiley; Gesicht; lustig; lol       
            int line = 1;
            outText.println("#Code"
                    + "\tImage"
                    + "\tTTS: English"
                    + "\tTTS: Native"
                    + "\tTTS: Fixed"
                    + "\t=\"DUP \"&countif(F2:F9999,\"DUP\")&\" MISS \"&countif(F2:F9999,\"MISS\")"
                    + "\tAnnotations: English"
                    + "\tAnnotations: Native"
                    + "\tAnnotations: Fixed"
                    + "\tComments"
                    + "\tOrder"
                    + "\tINTERNAL"
                    + "\tTTS: Combined"
                    );
            for (String s : ANNOTATED_CHARS) {
                String tts = data.tts.get(s);
                String ttsString = tts == null ? MISSING : tts;
                Set<String> annotations = data.map.get(s);
                if (annotations == null) {
                    annotations = Collections.singleton(MISSING);
                } else if (tts != null) {
                    annotations = new LinkedHashSet<>(annotations);
                    annotations.remove(tts);
                }
                final String englishAnnotationString = CollectionUtilities.join(english.map.get(s), "; ");
                final String annotationString = annotations == null ? MISSING : CollectionUtilities.join(annotations, "; ");
                ++line;
                outText.println("U+" + Utility.hex(s, 4, " U+")
                        + "\t=vlookup(A" + line + ",Internal!A:B,2,0)"
                        + "\t" + english.tts.get(s) 
                        + "\t" + ttsString 
                        + "\t" + "" 
                        + "\t=if(ISBLANK(M" + line + "),\"MISS\",if(countif($M$2:$M$1026,M" + line + ")<=1,\"\",\"DUP\"))"
                        + "\t" + englishAnnotationString
                        + "\t" + annotationString
                        + "\t" // fixed
                        + "\t" // comment
                        + "\t" + (line-1)
                        + "\t\u00A0" // internal, for line spacing
                        + "\t=if(not(ISBLANK(E" + line + ")),E" + line + ",D" + line + ")" // internal, for line spacing
                        );
            }
        }
    }

    private static LocaleData printXml(OldAnnotationData data, UnicodeSet missing) throws IOException {
        final boolean isEnglish = data.locale.equals(ULocale.ENGLISH);
        LocaleData ld = LocaleData.getInstance(data.locale);
        String language = data.locale.getLanguage();
        String script = data.locale.getScript();
        String territory = data.locale.getCountry();
        try (PrintWriter outText = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "/annotations/", data.locale + ".xml")) {
            outText.append(DtdType.ldml.header(MethodHandles.lookup().lookupClass())
                    + "\t<identity>\n"
                    + "\t\t<version number=\"$Revision" /*hack to stop SVN changing this*/ + "$\"/>\n"
                    + "\t\t<language type='" + language + "'/>\n"
                    + (script.isEmpty() ? "" : "\t\t<script type='" + script + "'/>\n")
                    + (territory.isEmpty() ? "" : "\t\t<territory type='" + territory + "'/>\n")
                    + "\t</identity>\n"
                    + "\t<annotations>\n");
            for (String s : ANNOTATED_CHARS) {
                Set<String> annotations = data.map.get(s);
                if (annotations == null) {
                    annotations = Collections.emptySet();
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
                        //+ (isEnglish ? "" : " draft='provisional'")
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
