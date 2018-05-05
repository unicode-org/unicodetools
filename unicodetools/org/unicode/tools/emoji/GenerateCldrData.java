package org.unicode.tools.emoji;

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
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Attribute;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.GenerateEmoji.CandidateStyle;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class GenerateCldrData {

    public static void main(String[] args) {
        EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_TO_GENERATE_PREVIOUS);

        UnicodeSet onlyNew = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                // .removeAll(previousEmoji.getZwjSequencesAll()) // catch the eye
                // witness
                ;
        System.out.println("\nAdd to root.xml");
        
        getLines(onlyNew, true);
        
        System.out.println("\nAdd to en.xml");

        getLines(onlyNew, false);
        
        System.out.println("CLDR:"
                + "\nCopy final tr51 emoji data file (emoji-test.txt) into org.unicode.cldr.util.data.emoji"
                + "\nMay need to change org.unicode.cldr.util.Emoji.SPECIALS to have TestAnnotations pass"
                + "\nAlso add images to XXX"
                + "\nAlso change the name composition algorithm"
                + "\nAnd add documentation of composition of names (for new components like hair styles) to LDML"
                ); 
        
        //printXML(onlyNew);
    }

    private static void getLines(UnicodeSet onlyNew, boolean isRoot) {
        int counter = 0;
        NumberFormat nf = new DecimalFormat("000");
        for (String s : onlyNew.addAllTo(new TreeSet<>(new UTF16.StringComparator(true, false, 0)))) {
            Category bucket = Category.getBucket(s);
            if (bucket.hasBaseCategory(Attribute.skin) || bucket.hasBaseCategory(Attribute.hair)) {
                continue;
            }
            String rootCode = "E11-" + nf.format(++counter); // TODO pick up version

            //  <annotation cp="🏻">emosiekoonwysiger | kleur | vel | veltipe-1-2</annotation>
            //  <annotation cp="🏻" type="tts">veltipe-1-2</annotation>
            String keywords = rootCode;
            String name = rootCode;
            String cldrName = EmojiData.EMOJI_DATA.getName(s);

            if (!isRoot) {
                Set<String> plainAnnotations = CandidateData.getInstance().getAnnotations(s);
                keywords = CollectionUtilities.join(plainAnnotations, " | " );
                name = cldrName;
            }
            String noVariants = EmojiData.removeEmojiVariants(s);
            String prefix = "<annotation cp=\"" + noVariants + "\"";

            System.out.println(prefix + ">" + keywords + "</annotation>"
                    + " \t<!-- " + Utility.hex(noVariants)
                    + (isRoot ? ": " + cldrName : "")
                    +  " -->");
            System.out.println(prefix + " type=\"tts\"" + ">" + name + "</annotation>");
        }
    }

    //    private static LocaleData printXml(OldAnnotationData data, UnicodeSet missing) throws IOException {
    //        final boolean isEnglish = data.locale.equals(ULocale.ENGLISH);
    //        LocaleData ld = LocaleData.getInstance(data.locale);
    //        String language = data.locale.getLanguage();
    //        String script = data.locale.getScript();
    //        String territory = data.locale.getCountry();
    //        try (PrintWriter outText = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "/annotations/", data.locale + ".xml")) {
    //            outText.append(DtdType.ldml.header(MethodHandles.lookup().lookupClass())
    //                    + "\t<identity>\n"
    //                    + "\t\t<version number=\"$Revision" /*hack to stop SVN changing this*/ + "$\"/>\n"
    //                    + "\t\t<language type='" + language + "'/>\n"
    //                    + (script.isEmpty() ? "" : "\t\t<script type='" + script + "'/>\n")
    //                    + (territory.isEmpty() ? "" : "\t\t<territory type='" + territory + "'/>\n")
    //                    + "\t</identity>\n"
    //                    + "\t<annotations>\n");
    //            for (String s : ANNOTATED_CHARS) {
    //                Set<String> annotations = data.map.get(s);
    //                if (annotations == null) {
    //                    annotations = Collections.emptySet();
    //                }
    //                //                    if (annotations.isEmpty() && Emoji.isRegionalIndicator(s.codePointAt(0))) {
    //                //                        String regionCode = Emoji.getRegionCodeFromEmoji(s);
    //                //                        String name = ldn.regionDisplayName(regionCode);
    //                //                        if (!name.equals(regionCode)) {
    //                //                            annotations = Collections.singleton(name);
    //                //                        } else {
    //                //                            System.out.println(locale + "\tmissing\t" + regionCode);
    //                //                        }
    //                //                    }
    //                if (annotations.isEmpty()) {
    //                    missing.add(s);
    //                    continue;
    //                }
    //
    //                String annotationString = CollectionUtilities.join(annotations, "; ");
    //                if (!isEnglish) {
    //                    LinkedHashSet<String> list = new LinkedHashSet<>();
    //                    list.add(english.tts.get(s));
    //                    list.addAll(english.map.get(s));
    //                    String englishAnnotationString = CollectionUtilities.join(list, "; ");
    //                    outText.append("\n\t\t<!-- " + fix(englishAnnotationString, ld) + " -->\n");
    //                }
    //                outText.append("\t\t<annotation cp='")
    //                .append(new UnicodeSet().add(s).toPattern(false));
    //                String ttsString = data.tts.get(s);
    //                if (ttsString != null) {
    //                    outText.append("' tts='")
    //                    .append(fix(ttsString, ld));
    //                }
    //                outText.append("'"
    //                        //+ (isEnglish ? "" : " draft='provisional'")
    //                        + ">")
    //                        .append(fix(annotationString, ld))
    //                        .append("</annotation>\n")
    //                        ;
    //            }
    //            outText.write("\t</annotations>\n"
    //                    + "</ldml>");
    //        }
    //        if (missing.isEmpty()) {
    //            System.out.println(missing);
    //        }
    //        return ld;
    //    }
}
