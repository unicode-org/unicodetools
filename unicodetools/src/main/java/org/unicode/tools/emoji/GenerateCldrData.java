package org.unicode.tools.emoji;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableSet;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.Tabber;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Attribute;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

public class GenerateCldrData {
    static final CandidateData candidateData = CandidateData.getInstance();
    static final EmojiDataSource betaPlusCandidateData = new EmojiDataSourceCombined();
    static final EmojiDataSource releaseData = EmojiData.EMOJI_DATA_RELEASED;

    public static void main(String[] args) throws IOException {
        EmojiData EMOJI_DATA_PREVIOUS = EmojiData.EMOJI_DATA;
        UnicodeMap<Annotations> cldrEnglishData = org.unicode.cldr.util.Annotations.getData("en");

        UnicodeSet onlyNew =
                new UnicodeSet(betaPlusCandidateData.getAllEmojiWithoutDefectives())
                        .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                // .removeAll(previousEmoji.getZwjSequencesAll()) // catch the eye
                // witness
                ;
        Set<String> found = getSortedFiltered(onlyNew, candidateData.comparator, true);

        System.out.println(
                "\n*** CLDR Instructions"
                        + "\n• Add to the files root.xml and en.xml, as below"
                        + "\n• Replace the contents of org.unicode.cldr.util.data.emoji.emoji-test.txt as below"
                        + "\n• Also add new images to /cldr-apps/WebContent/images/emoji/."
                        + " They need to have the prefix 'emoji-', like emoji_0023_20e3.png."
                        + " If you don't do this, you'll get failures in TestAnnotations.testEmojiImages."
                        + "\n• May need to change org.unicode.cldr.util.Emoji.SPECIALS to have TestAnnotations.testAnnotationPaths pass."
                        + " See notes below SPECIALS for instructions."
                        + "\n• May need to change the name composition algorithm (for new components like hair styles) and "
                        + "\n\tmodify the documentation of composition of names in LDML.");

        System.out.println("\n*** Add to root.xml");

        getLines(found, true, cldrEnglishData);

        System.out.println("\n*** Add to en.xml");

        getLines(found, false, cldrEnglishData);

        System.out.println(
                "\n*** Replace contents of org.unicode.cldr.util.data.emoji.emoji-test.txt by");
        // # subgroup: transport-ground
        // 1F682                                      ; fully-qualified     # 🚂 locomotive

        //        final String OUTPUT_DIR = CLDRPaths.GEN_DIRECTORY + "cldr/emoji/" +
        // Emoji.VERSION_BETA_STRING;

        //        UnicodeSet temp = new UnicodeSet(onlyNew).addAll(found);

        // GenerateEmojiTestFile.showLines(EmojiOrder.BETA_ORDER, temp, Target.propFile,
        // OUTPUT_DIR);
        //        System.out.println(OUTPUT_DIR);

        getEmojiDataLines();

        // printXML(onlyNew);
    }

    private static void getEmojiDataLines() {
        Set<String> found =
                getSortedFiltered(
                        betaPlusCandidateData.getAllEmojiWithDefectives(),
                        candidateData.comparator,
                        false); // releaseData, betaPlusCandidateData
        if (false) {
            Set<String> found2 =
                    getSortedFiltered(
                            betaPlusCandidateData.getAllEmojiWithDefectives(),
                            EmojiOrder.STD_ORDER.codepointCompare,
                            false); // releaseData, betaPlusCandidateData
            ArrayList<String> foundArray = new ArrayList<>(found);
            ArrayList<String> found2Array = new ArrayList<>(found2);
            if (!foundArray.equals(found2Array)) {
                String last = null;
                for (String s : found2) {
                    if (last != null && candidateData.comparator.compare(s, last) < 0) {
                        candidateData.comparator.compare(s, last);
                    }
                    last = s;
                }
            }
        }

        MajorGroup oldMajorCat = null;
        String oldMinorCat = null;
        String versionString = Emoji.VERSION_BETA.getVersionString(1, 2);
        System.out.println(
                "# Generated lines from Emoji Data v"
                        + versionString
                        + ", using GenerateCldrData.java");
        Set<String> doneAlready = new HashSet<>();
        // get sizes
        // 1F1F9 1F1F2                                ; fully-qualified     # 🇹🇲 flag:
        // Turkmenistan

        int maxField1 = 0;
        int maxField2 = "minimally-qualified".length();
        for (String s : found) {
            maxField1 = Math.max(maxField1, Utility.hex(s, " ").length());
        }
        Tabber tabber =
                new Tabber.MonoTabber()
                        .add(maxField1 + 1, Tabber.LEFT)
                        .add(maxField2 + 3, Tabber.LEFT);
        ;

        Set<String> minorSeen = new HashSet<>();
        for (String s : found) {
            // TODO fix to incorporate beta data
            String minorCat = candidateData.getCategory(s);
            if (!minorCat.equals(oldMinorCat)) {
                if (minorSeen.contains(minorCat)) {
                    throw new ICUException(
                            "Bad ordering for "
                                    + minorCat
                                    + "\t"
                                    + s
                                    + "\nMaybe a mismatch between candidateData.txt and emojiOrdering.txt");
                }
                minorSeen.add(minorCat);
                oldMinorCat = minorCat;
                MajorGroup majorCat = EmojiOrder.BETA_ORDER.getMajorGroupFromCategory(minorCat);
                if (!majorCat.equals(oldMajorCat)) {
                    oldMajorCat = majorCat;
                    System.out.println("\n# group: " + majorCat.toPlainString());
                }
                System.out.println("\n# subgroup: " + minorCat);
            }
            String name = betaPlusCandidateData.getName(s);
            String withVS = betaPlusCandidateData.addEmojiVariants(s);
            String withVSFirst = betaPlusCandidateData.addEmojiVariants(s, Emoji.Qualified.first);
            String withoutVS = withVS.replace(Emoji.EMOJI_VARIANT_STRING, "");

            addDataLine(withVS, name, doneAlready, "fully-qualified", tabber);
            addDataLine(withVSFirst, name, doneAlready, "minimally-qualified", tabber);
            addDataLine(
                    withoutVS.replace(Emoji.EMOJI_VARIANT_STRING, ""),
                    name,
                    doneAlready,
                    "unqualified",
                    tabber);
        }
    }

    private static void addDataLine(
            String s, String name, Set<String> doneAlready, String classification, Tabber tabber) {
        if (!doneAlready.contains(s)) {
            System.out.println(
                    tabber.process(
                            Utility.hex(s, " ")
                                    + "\t; "
                                    + classification
                                    + "\t# "
                                    + s
                                    + " "
                                    + name));
            doneAlready.add(s);
        }
    }

    private static Set<String> getSortedFiltered(
            UnicodeSet onlyNew, Comparator<String> comparator, boolean filtered) {
        Set<String> found = new LinkedHashSet<>();
        // TODO, handle beta data also
        for (String s : onlyNew.addAllTo(new TreeSet<>(comparator))) {
            if (filtered) {
                Category bucket = Category.getBucket(s);
                if (bucket.hasAttribute(Attribute.skin) || bucket.hasAttribute(Attribute.hair)) {
                    continue;
                }
            }
            found.add(s);
        }
        return ImmutableSet.copyOf(found);
    }

    static final String versionString = Emoji.VERSION_BETA.getVersionString(1, 2);

    static final BiMap<String, String> emojiToCode = HashBiMap.create();

    static {
        AnnotationSet rootAnnotations = Annotations.getDataSet("root");
        for (String emoji : rootAnnotations.keySet()) {
            emojiToCode.put(emoji, rootAnnotations.getShortName(emoji));
        }
        // System.out.println(emojiToCode);
    }

    static int lastUsed = 0;
    static final NumberFormat nf = new DecimalFormat("000");

    static String getRootValue(String emoji) {
        String result = emojiToCode.get(emoji);
        if (result == null) {
            while (true) { // find free value. Don't care if it is fast
                result = "E" + versionString + "-" + nf.format(++lastUsed);
                if (!emojiToCode.containsValue(result)) {
                    break;
                }
            }
        }
        return result;
    }

    private static void getLines(
            Set<String> found, boolean isRoot, UnicodeMap<Annotations> cldrData) {
        int counter = 0;
        AnnotationSet english = Annotations.getDataSet("en");

        System.out.println(
                "<!-- Generated lines from Emoji Data v"
                        + versionString
                        + ", using GenerateCldrData.java"
                        + "-->");

        Set<String> warnings = new TreeSet<>();
        for (String s : found) {
            String noVariants = EmojiData.removeEmojiVariants(s);
            String rootCode = getRootValue(s);
            Annotations annotations = cldrData.get(noVariants);
            if (annotations != null) {
                if (!isRoot) {
                    String candidateName = candidateData.getName(s);
                    final String cldrName = annotations.getShortName();
                    if (!Objects.equals(candidateName, cldrName)) {
                        warnings.add(
                                rootCode
                                        + " Names Differ:"
                                        + "\tcldr:\t"
                                        + cldrName
                                        + "\tcandidate:\t"
                                        + candidateName);
                    }
                    Set<String> candidateKeywords = candidateData.getAnnotations(s);
                    Set<String> cldrKeywords = annotations.getKeywords();
                    if (!Objects.equals(candidateKeywords, cldrKeywords)) {
                        warnings.add(
                                rootCode
                                        + " Keywords Differ:"
                                        + "\tcldr:\t"
                                        + cldrKeywords
                                        + "\tcandidate:\t"
                                        + candidateKeywords);
                    }
                }
                continue;
            }

            //  <annotation cp="🏻">emosiekoonwysiger | kleur | vel | veltipe-1-2</annotation>
            //  <annotation cp="🏻" type="tts">veltipe-1-2</annotation>
            String keywords = rootCode;
            String name = rootCode;
            String cldrName = EmojiData.EMOJI_DATA.getName(s);

            if (!isRoot) {
                Set<String> plainAnnotations = Collections.emptySet();
                if (s.startsWith("🧑")) {
                    String sourcem = s.replace("🧑", "👩");
                    Set<String> annotations2 = english.getKeywords(sourcem);
                    if (annotations2 != null) {
                        plainAnnotations = new LinkedHashSet<>();
                        for (String item : annotations2) {
                            if (!item.contains("woman")) {
                                plainAnnotations.add(item);
                            }
                        }
                    }
                } else {
                    plainAnnotations = candidateData.getAnnotations(s);
                }

                keywords = CollectionUtilities.join(plainAnnotations, " | ");
                name = cldrName;
            }
            String prefix = "\t\t<annotation cp=\"" + noVariants + "\"";
            System.out.println(
                    prefix
                            + ">"
                            + keywords
                            + "</annotation>"
                            + " \t<!-- "
                            + Utility.hex(noVariants)
                            + (isRoot ? ": " + cldrName : "")
                            + " -->");
            System.out.println(prefix + " type=\"tts\"" + ">" + name + "</annotation>");
        }
        if (!isRoot) {
            if (!warnings.isEmpty()) {
                System.out.println(
                        "Differences from candidate to cldr"
                                + "\nJust double check these against CLDR, which may be newer.");
                for (String warning : warnings) {
                    System.out.println(warning);
                }
            }
        } else {
            UnicodeSet extraInRoot =
                    new UnicodeSet()
                            .addAll(found)
                            .removeAll(betaPlusCandidateData.getAllEmojiWithoutDefectives());
            System.out.println("Extra annotations in root: " + extraInRoot.toPattern(false));
        }
    }

    //    private static LocaleData printXml(OldAnnotationData data, UnicodeSet missing) throws
    // IOException {
    //        final boolean isEnglish = data.locale.equals(ULocale.ENGLISH);
    //        LocaleData ld = LocaleData.getInstance(data.locale);
    //        String language = data.locale.getLanguage();
    //        String script = data.locale.getScript();
    //        String territory = data.locale.getCountry();
    //        try (PrintWriter outText = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY +
    // "/annotations/", data.locale + ".xml")) {
    //            outText.append(DtdType.ldml.header(MethodHandles.lookup().lookupClass())
    //                    + "\t<identity>\n"
    //                    + "\t\t<version number=\"$Revision" /*hack to stop SVN changing this*/ +
    // "$\"/>\n"
    //                    + "\t\t<language type='" + language + "'/>\n"
    //                    + (script.isEmpty() ? "" : "\t\t<script type='" + script + "'/>\n")
    //                    + (territory.isEmpty() ? "" : "\t\t<territory type='" + territory +
    // "'/>\n")
    //                    + "\t</identity>\n"
    //                    + "\t<annotations>\n");
    //            for (String s : ANNOTATED_CHARS) {
    //                Set<String> annotations = data.map.get(s);
    //                if (annotations == null) {
    //                    annotations = Collections.emptySet();
    //                }
    //                //                    if (annotations.isEmpty() &&
    // Emoji.isRegionalIndicator(s.codePointAt(0))) {
    //                //                        String regionCode = Emoji.getRegionCodeFromEmoji(s);
    //                //                        String name = ldn.regionDisplayName(regionCode);
    //                //                        if (!name.equals(regionCode)) {
    //                //                            annotations = Collections.singleton(name);
    //                //                        } else {
    //                //                            System.out.println(locale + "\tmissing\t" +
    // regionCode);
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
    //                    outText.append("\n\t\t<!-- " + fix(englishAnnotationString, ld) + "
    // -->\n");
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
