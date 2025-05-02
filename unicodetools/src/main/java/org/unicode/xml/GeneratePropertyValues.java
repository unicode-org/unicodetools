package org.unicode.xml;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.props.PropertyParsingInfo;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.*;
import org.unicode.props.ValueCardinality;
import org.unicode.text.utility.Settings;

/**
 * Utility for generating fragments that describe the property values in a format that can be
 * displayed in UAX42. UAX42 fragments live in
 * unicodetools/src/main/resources/org/unicode/uax42/fragments
 */
public class GeneratePropertyValues {

    private enum VALUESOUTPUTTYPE {
        VALUE_PER_LINE,
        ALPHABETICAL_GROUP,
        NUMERICAL_GROUP,
        MAX_LINE_LENGTH;
    }

    private enum SCHEMA {
        // Manual indicates a fragment file that is maintained manually rather than generated from
        // this utility.
        // Manual
        NAMESPACE("namespace"),
        // Manual
        DATATYPES("datatypes"),
        // Manual
        START("start"),
        BOOLEAN("boolean"),
        // Manual
        DESCRIPTION("description"),
        // Manual
        REPERTOIRE("repertoire"),
        PROPERTIES("properties"),
        TANGUT("tangut"),
        NUSHU("nushu"),
        EMOJI_DATA("emoji-data"),
        UNIKEMET("unikemet"),
        // Manual
        BLOCK("block"),
        // Manual
        NAMED_SEQUENCES("named-sequences"),
        // Manual
        NORMALIZATION_CORRECTIONS("normalization-corrections"),
        // Manual
        STANDARDIZED_VARIANTS("standardized-variants"),
        // Manual
        CJK_RADICALS("cjk-radicals"),
        // Manual
        EMOJI_SOURCES("emoji-sources"),
        DO_NOT_EMIT("do-not-emit");

        final String name;

        SCHEMA(String name) {
            this.name = name;
        }

        String getName() {
            return this.name;
        }
    }

    private static final class TRDetails {
        boolean isList;
        String syntax;

        public TRDetails(boolean isList, String syntax) {
            this.isList = isList;
            this.syntax = syntax;
        }

        public boolean isList() {
            return isList;
        }

        public String getSyntax() {
            return syntax;
        }
    }

    private static final int MAX_LINE_LENGTH = 70;
    private static final String NEWLINE = System.lineSeparator();
    private static final String DOUBLELINE = System.lineSeparator() + System.lineSeparator();
    private static final String TRIPLELINE =
            System.lineSeparator() + System.lineSeparator() + System.lineSeparator();
    private static File destinationFolder = null;

    private static HashMap<String, TRDetails> syntaxTR38;
    private static HashMap<String, TRDetails> syntaxTR57;
    private static final String NAMESPACE = "http://unicode.org/ns/2001/ucdxml";
    private static final String TR38URL = "https://www.unicode.org/reports/tr38/proposed.html";
    private static final String TR57URL = "https://www.unicode.org/reports/tr57/proposed.html";
    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.create("ucdversion", 'v', UOption.OPTIONAL_ARG),
        UOption.create("outputfolder", 'f', UOption.OPTIONAL_ARG)
    };

    private static final int HELP = 0, UCDVERSION = 1, OUTPUTFOLDER = 2;

    public static void main(String[] args) throws Exception {

        VersionInfo ucdVersion = null;

        UOption.parseArgs(args, options);

        if (options[HELP].doesOccur) {
            System.out.println(
                    "GeneratePropertyValuesList [--ucdversion {version number}] [--outputfolder {destination}]");
            System.exit(0);
        }

        try {
            if (options[UCDVERSION].doesOccur) {
                try {
                    ucdVersion = VersionInfo.getInstance(options[UCDVERSION].value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Could not convert "
                                    + options[UCDVERSION].value
                                    + " to a valid UCD version");
                }
            } else {
                ucdVersion = VersionInfo.getInstance(Settings.latestVersion);
            }
            destinationFolder =
                    options[OUTPUTFOLDER].doesOccur
                            ? new File(options[OUTPUTFOLDER].value)
                            : new File(Settings.Output.GEN_DIR + "uax42/fragments/");
            try {
                if (!destinationFolder.exists()) {
                    if (!destinationFolder.mkdirs()) {
                        throw new IOException();
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not find or create " + destinationFolder);
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (ucdVersion != null && destinationFolder.exists()) {
            buildPropertyValues(ucdVersion);
            System.out.println("End");
            System.exit(0);
        } else {
            System.err.println("Unexpected error when generating uax42 fragment files.");
            System.exit(1);
        }
    }

    private static void buildPropertyValues(
            // It would be nice to be able to generate values by ucdVersion. Leaving this here for
            // now...
            VersionInfo ucdVersion) throws IOException, URISyntaxException {
        syntaxTR38 = parseTR(TR38URL);
        syntaxTR57 = parseTR(TR57URL);

        createPropertyFragment(
                SCHEMA.BOOLEAN,
                getFormattedValues(SCHEMA.BOOLEAN, VALUESOUTPUTTYPE.MAX_LINE_LENGTH));
        createPropertyFragment(
                UcdProperty.Age,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(UcdProperty.Age, VALUESOUTPUTTYPE.NUMERICAL_GROUP));
        createPropertyFragment(
                UcdProperty.Name, SCHEMA.PROPERTIES, getFormattedSyntax(UcdProperty.Name));
        createPropertyFragment(
                UcdProperty.Unicode_1_Name,
                SCHEMA.PROPERTIES,
                getFormattedSyntax(UcdProperty.Unicode_1_Name));
        createPropertyFragment(
                UcdProperty.Name_Alias.getShortName() + ".xml",
                "name-alias element",
                SCHEMA.PROPERTIES,
                getFormattedElement(UcdProperty.Name_Alias));
        createPropertyFragment(
                UcdProperty.Block,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(UcdProperty.Block, VALUESOUTPUTTYPE.VALUE_PER_LINE));
        createPropertyFragment(
                UcdProperty.General_Category,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.General_Category, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP));
        createPropertyFragment(
                UcdProperty.Canonical_Combining_Class,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Canonical_Combining_Class, VALUESOUTPUTTYPE.VALUE_PER_LINE));
        createPropertyFragment(
                UcdProperty.Bidi_Class,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(UcdProperty.Bidi_Class, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP));
        createPropertyFragment(
                UcdProperty.Bidi_Mirrored,
                SCHEMA.PROPERTIES,
                getFormattedBoolean(UcdProperty.Bidi_Mirrored));
        createPropertyFragment(
                UcdProperty.Bidi_Mirroring_Glyph,
                SCHEMA.PROPERTIES,
                getFormattedSyntax(UcdProperty.Bidi_Mirroring_Glyph));
        createPropertyFragment(
                UcdProperty.Bidi_Control,
                SCHEMA.PROPERTIES,
                getFormattedBoolean(UcdProperty.Bidi_Control));
        createPropertyFragment(
                UcdProperty.Bidi_Paired_Bracket_Type,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Bidi_Paired_Bracket_Type, VALUESOUTPUTTYPE.MAX_LINE_LENGTH));
        createPropertyFragment(
                UcdProperty.Bidi_Paired_Bracket,
                SCHEMA.PROPERTIES,
                getFormattedSyntax(UcdProperty.Bidi_Paired_Bracket));
        createPropertyFragment(
                "decomposition.xml",
                "decomposition properties",
                SCHEMA.PROPERTIES,
                getFormattedDecompositionProperties());
        createPropertyFragment(
                "composition.xml",
                "composition properties",
                SCHEMA.PROPERTIES,
                getFormattedCompositionProperties());
        createPropertyFragment(
                "quickcheck.xml",
                "quick check properties",
                SCHEMA.PROPERTIES,
                getFormattedQuickCheckProperties());
        createPropertyFragment(
                "numeric.xml",
                "numeric properties",
                SCHEMA.PROPERTIES,
                getFormattedNumericProperties());
        createPropertyFragment(
                "joining.xml",
                "joining properties",
                SCHEMA.PROPERTIES,
                getFormattedJoiningProperties());
        createPropertyFragment(
                UcdProperty.Join_Control.getShortName() + ".xml",
                "joining properties",
                SCHEMA.PROPERTIES,
                getFormattedBoolean(UcdProperty.Join_Control));
        createPropertyFragment(
                UcdProperty.Line_Break,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(UcdProperty.Line_Break, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP));
        createPropertyFragment(
                UcdProperty.East_Asian_Width,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.East_Asian_Width, VALUESOUTPUTTYPE.MAX_LINE_LENGTH));
        createPropertyFragment(
                "casing.xml",
                "casing properties",
                SCHEMA.PROPERTIES,
                getFormattedCasingProperties());
        createPropertyFragment(
                "simple_case_mapping.xml",
                "casing properties",
                SCHEMA.PROPERTIES,
                getFormattedSimpleCaseMappingProperties());
        createPropertyFragment(
                "case_mapping.xml",
                "casing properties",
                SCHEMA.PROPERTIES,
                getFormattedCaseMappingProperties());
        createPropertyFragment(
                "case_folding.xml",
                "casing properties",
                SCHEMA.PROPERTIES,
                getFormattedCaseFoldingProperties());
        createPropertyFragment(
                "case_other.xml",
                "casing properties",
                SCHEMA.PROPERTIES,
                getFormattedCaseOtherProperties());
        createPropertyFragment(
                "script.xml",
                "script properties",
                SCHEMA.PROPERTIES,
                getFormattedScriptProperties());
        createPropertyFragment(
                UcdProperty.Hangul_Syllable_Type,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Hangul_Syllable_Type, VALUESOUTPUTTYPE.MAX_LINE_LENGTH));
        createPropertyFragment(
                UcdProperty.Jamo_Short_Name,
                SCHEMA.PROPERTIES,
                getFormattedSyntax(UcdProperty.Jamo_Short_Name));
        createPropertyFragment(
                UcdProperty.Indic_Syllabic_Category,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Indic_Syllabic_Category, VALUESOUTPUTTYPE.VALUE_PER_LINE));
        createPropertyFragment(
                UcdProperty.Indic_Positional_Category,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Indic_Positional_Category, VALUESOUTPUTTYPE.VALUE_PER_LINE));
        createPropertyFragment(
                UcdProperty.Indic_Conjunct_Break,
                SCHEMA.PROPERTIES,
                getFormattedAttribute(
                        UcdProperty.Indic_Conjunct_Break, VALUESOUTPUTTYPE.VALUE_PER_LINE));
        createPropertyFragment(
                "identifier.xml",
                "identifier properties",
                SCHEMA.PROPERTIES,
                getFormattedIdentifierProperties());
        createPropertyFragment(
                "pattern.xml",
                "pattern properties",
                SCHEMA.PROPERTIES,
                getFormattedPatternProperties());
        createPropertyFragment(
                "function_graphic.xml",
                "properties related to function and graphic characteristics",
                SCHEMA.PROPERTIES,
                getFormattedFunctionGraphicProperties());
        createPropertyFragment(
                "boundaries.xml",
                "properties related to boundaries",
                SCHEMA.PROPERTIES,
                getFormattedBoundaryProperties());
        createPropertyFragment(
                "ideographs.xml",
                "properties related to ideographs",
                SCHEMA.PROPERTIES,
                getFormattedIdeographProperties());
        createPropertyFragment(
                "miscellaneous.xml",
                "miscellaneous properties",
                SCHEMA.PROPERTIES,
                getFormattedMiscellaneousProperties());
        createPropertyFragment(
                "Unihan.xml",
                "Unihan properties",
                SCHEMA.PROPERTIES,
                getFormattedUnihanProperties());
        createPropertyFragment(
                "Tangut.xml", "Tangut data", SCHEMA.TANGUT, getFormattedTangutProperties());
        createPropertyFragment(
                "Nushu.xml", "Nushu data", SCHEMA.NUSHU, getFormattedNushuProperties());
        createPropertyFragment(
                "Emoji.xml", "Emoji properties", SCHEMA.EMOJI_DATA, getFormattedEmojiProperties());
        createPropertyFragment(
                "Unikemet.xml", "Unikemet data", SCHEMA.UNIKEMET, getFormattedUnikemetProperties());
        createPropertyFragment(
                "do-not-emit.xml",
                "do-not-emit",
                SCHEMA.DO_NOT_EMIT,
                getFormattedDoNotEmit(VALUESOUTPUTTYPE.VALUE_PER_LINE));
    }

    private static void createPropertyFragment(SCHEMA schema, String formattedFragment)
            throws IOException {
        createPropertyFragment(
                schema.getName() + ".xml", schema.getName(), schema, formattedFragment);
    }

    private static void createPropertyFragment(
            UcdProperty ucdProperty, SCHEMA schema, String formattedFragment) throws IOException {
        createPropertyFragment(
                ucdProperty.getShortName() + ".xml",
                ucdProperty.getShortName() + " attribute",
                schema,
                formattedFragment);
    }

    private static void createPropertyFragment(
            String filename, String title, SCHEMA schema, String formattedFragment)
            throws IOException {
        BufferedWriter writer = getFragmentWriter(filename);
        writer.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + NEWLINE
                        + "<ucdxml:block xmlns:ucdxml=\""
                        + NAMESPACE
                        + "\" title=\""
                        + title
                        + "\" id='schema."
                        + schema.getName()
                        + "'>"
                        + NEWLINE);
        writer.write(formattedFragment);
        writer.write(NEWLINE + "</ucdxml:block>");
        writer.flush();
        writer.close();
    }

    private static BufferedWriter getFragmentWriter(String filename) throws IOException {
        File fragmentFolder = new File(destinationFolder + File.separator);
        if (!fragmentFolder.exists()) {
            if (!fragmentFolder.mkdir()) {
                throw new IOException();
            }
        }
        File outputFile = new File(fragmentFolder, filename);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        OutputStreamWriter outputStreamWriter =
                new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
        return new BufferedWriter(outputStreamWriter);
    }

    private static String getFormattedAttribute(
            UcdProperty ucdProperty, VALUESOUTPUTTYPE valuesoutputtype) {
        String attributeString = "    attribute " + ucdProperty.getShortName() + " ";
        List<String> values;
        StringBuilder stringBuilder = new StringBuilder();

        switch (ucdProperty) {
            case Age:
                values = getAgeValues();
                break;
            case Block:
                values = getBlockValues();
                break;
            case General_Category:
                values = getGeneralCategoryValues();
                break;
            case Canonical_Combining_Class:
                values = getCanonicalCombiningClassValues();
                break;
            case Bidi_Class:
                values = getBidirectionalValues();
                break;
            case Bidi_Paired_Bracket_Type:
                values = getBidiPairedBracketTypeValues();
                break;
            case Decomposition_Type:
                values = getDecompositionTypeValues();
                break;
            case NFC_Quick_Check:
                values = getNFCQuickCheckValues();
                break;
            case NFD_Quick_Check:
                values = getNFDQuickCheckValues();
                break;
            case NFKC_Quick_Check:
                values = getNFKCQuickCheckValues();
                break;
            case NFKD_Quick_Check:
                values = getNFKDQuickCheckValues();
                break;
            case Numeric_Type:
                values = getNumericTypeValues();
                break;
            case Joining_Type:
                values = getJoiningTypeValues();
                break;
            case Joining_Group:
                values = getJoiningGroupValues();
                break;
            case Line_Break:
                values = getLineBreakValues();
                break;
            case East_Asian_Width:
                values = getEastAsianWidthValues();
                break;
            case Hangul_Syllable_Type:
                values = getHangulSyllableTypeValues();
                break;
            case Indic_Syllabic_Category:
                values = getIndicSyllabicCategoryValues();
                break;
            case Indic_Positional_Category:
                values = getIndicPositionalCategoryValues();
                break;
            case Indic_Conjunct_Break:
                values = getIndicConjunctBreakValues();
                break;
            case Vertical_Orientation:
                values = getVerticalOrientationValues();
                break;
            case Grapheme_Cluster_Break:
                values = getGraphemeClusterBreakValues();
                break;
            case Word_Break:
                values = getWordBreakValues();
                break;
            case Sentence_Break:
                values = getSentenceBreakValues();
                break;
            case Do_Not_Emit_Type:
                values = getDoNotEmitTypeValues();
                break;
            case kEH_Core:
                values = getkEHCoreValues();
                break;

            default:
                throw new IllegalStateException(
                        ucdProperty.getShortName()
                                + " is not handled by "
                                + "getFormattedAttribute.");
        }
        String formattedValues = formatValues(attributeString.length(), values, valuesoutputtype);
        stringBuilder
                .append("  code-point-attributes &amp;=")
                .append(NEWLINE)
                .append(attributeString)
                .append("{ ");
        if (formattedValues.contains(NEWLINE)) {
            stringBuilder.append(formattedValues).append(NEWLINE);
            stringBuilder.append(
                    String.format("%" + (attributeString.length() + "}?".length()) + "s", "}?"));
        } else {
            stringBuilder.append(formattedValues).append(" }?");
        }
        return stringBuilder.toString();
    }

    private static String getFormattedSyntax(UcdProperty ucdProperty) {
        final PropertyParsingInfo propInfo = PropertyParsingInfo.getPropertyInfo(ucdProperty);
        if (propInfo.getRegex() == null) {
            if (ucdProperty != UcdProperty.kEH_AltSeq) {
                throw new NullPointerException(
                        "Could not find syntax for " + ucdProperty.getShortName());
            }
        }

        String attributeString =
                ucdProperty.getShortName().startsWith("cjk")
                        ? "    attribute " + ucdProperty.getShortName().substring(2) + " "
                        : "    attribute " + ucdProperty.getShortName() + " ";
        String formattedAttributeString;
        switch (ucdProperty) {
                // { text }
            case ISO_Comment:
                formattedAttributeString = attributeString + "{ text }?";
                break;

                // { single-code-point }
            case Equivalent_Unified_Ideograph:
                formattedAttributeString = attributeString + "{ single-code-point }?";
                break;

                // { "" | single-code-point }
            case Bidi_Mirroring_Glyph:
                formattedAttributeString = attributeString + "{ \"\" | single-code-point }?";
                break;

                // { "#" | single-code-point }
            case Bidi_Paired_Bracket:
            case Simple_Uppercase_Mapping:
            case Simple_Lowercase_Mapping:
            case Simple_Titlecase_Mapping:
            case Simple_Case_Folding:
                formattedAttributeString = attributeString + "{ \"#\" | single-code-point }?";
                break;

                // { "#" | zero-or-more-code-points }
            case Decomposition_Mapping:
            case NFKC_Casefold:
            case NFKC_Simple_Casefold:
                formattedAttributeString =
                        attributeString + "{ \"#\" | zero-or-more-code-points }?";
                break;

                // { "#" | one-or-more-code-points }
            case Uppercase_Mapping:
            case Lowercase_Mapping:
            case Titlecase_Mapping:
            case Case_Folding:
                formattedAttributeString = attributeString + "{ \"#\" | one-or-more-code-points }?";
                break;

                // { "NaN" | RegEx }
            case Numeric_Value:
                formattedAttributeString =
                        attributeString
                                + "{ \"NaN\" | xsd:string { pattern=\""
                                + cleanRegex(propInfo.getRegex().toString())
                                + "\" } }?";
                break;

                // Special cases
            case Name:
                formattedAttributeString =
                        attributeString
                                + "{ \"\" |"
                                + NEWLINE
                                + "                   \"CJK UNIFIED IDEOGRAPH-#\" |"
                                + NEWLINE
                                + "                   \"CJK COMPATIBILITY IDEOGRAPH-#\" |"
                                + NEWLINE
                                + "                   \"EGYPTIAN HIEROGLYPH-#\" |"
                                + NEWLINE
                                + "                   \"TANGUT IDEOGRAPH-#\" |"
                                + NEWLINE
                                + "                   \"KHITAN SMALL SCRIPT CHARACTER-#\" |"
                                + NEWLINE
                                + "                   \"NUSHU CHARACTER-#\" |"
                                + NEWLINE
                                + "                   xsd:string { pattern=\""
                                + cleanRegex(propInfo.getRegex().toString())
                                + "\" }"
                                + NEWLINE
                                + "                 }?";
                break;
            case Unicode_1_Name:
                formattedAttributeString =
                        attributeString
                                + "{ \"\" | xsd:string { pattern=\""
                                + cleanRegex(propInfo.getRegex().toString())
                                + "\" } }?";
                break;
            case Script:
                formattedAttributeString = attributeString + "{ script }?";
                break;
            case Script_Extensions:
                formattedAttributeString = attributeString + "{ list { script + } }?";
                break;
            case kTGT_MergedSrc:
                // Ideally, should be obtained from a TR.
                String kTGT_MergedSrc =
                        NEWLINE
                                + "     { xsd:string {pattern=\"H2004-[AB]-\\d{4}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"H2021-\\d{6}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"L(19(86|97)|20(06|12))-\\d{4}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"L2008-\\d{4}([AB]|-\\d{4})?\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"N1966-\\d{3}-\\d{2}[0-9A-Z]{1,2}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"N5217-\\d{2}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"S1968-\\d{4}\"}"
                                + NEWLINE
                                + "     | xsd:string {pattern=\"UTN42-\\d{3}\"}"
                                + NEWLINE
                                + "     }?";
                formattedAttributeString = attributeString + kTGT_MergedSrc;
                break;
            case kNSHU_Reading:
                // Ideally, should be obtained from a TR.
                String kReading = "{ xsd:string }?";
                formattedAttributeString = attributeString + kReading;
                break;
                // Currently, kEH_FVal is a Provisional property, and some of the values don't match
                // the Syntax in
                // UAX 57. Use this for now.
            case kEH_FVal:
                String kEH_FVal = "{ text }?";
                formattedAttributeString = attributeString + kEH_FVal;
                break;
                // Currently, kEH_AltSeq isn't listed in the proposed UAX 57. Use this for now.
            case kEH_AltSeq:
                String kEH_AltSeq = "{ text }?";
                formattedAttributeString = attributeString + kEH_AltSeq;
                break;

            default:
                if (propInfo.getMultivalued() == ValueCardinality.Unordered
                        || propInfo.getMultivalued() == ValueCardinality.Ordered) {
                    formattedAttributeString =
                            attributeString
                                    + "{ list { xsd:string { pattern=\""
                                    + cleanRegex(propInfo.getRegex().toString())
                                    + "\" }+ } }?";
                } else {
                    formattedAttributeString =
                            attributeString
                                    + "{ xsd:string { pattern=\""
                                    + cleanRegex(propInfo.getRegex().toString())
                                    + "\" } }?";
                }
        }
        return "  code-point-attributes &amp;=" + NEWLINE + formattedAttributeString;
    }

    private static String getFormattedTR38Syntax(UcdProperty ucdProperty) {
        // TODO: We should determine whether we still want to show empty values in the XML files.
        // TODO: See org.unicode.xml.UcdPropertyDetail.isCJKShowIfEmpty()
        boolean isShowIfEmpty = false;
        for (UCDPropertyDetail propDetail : UCDPropertyDetail.cjkValues()) {
            if (propDetail.getUcdProperty().equals(ucdProperty)) {
                isShowIfEmpty = propDetail.isCJKShowIfEmpty();
            }
        }

        String attributeString = " attribute " + ucdProperty.getShortName().substring(2);
        TRDetails trDetails = syntaxTR38.get(ucdProperty.name());
        if (trDetails == null) {
            throw new NullPointerException(
                    "Could not locate details for " + ucdProperty.name() + " in " + TR38URL);
        }
        String formattedSyntax = formatTRSyntax(trDetails, isShowIfEmpty);

        return "  code-point-attributes &amp;=" + attributeString + NEWLINE + formattedSyntax;
    }

    private static String getFormattedTR57Syntax(UcdProperty ucdProperty) {
        String attributeString = " attribute " + ucdProperty.getShortName();
        TRDetails trDetails = syntaxTR57.get(ucdProperty.name());
        if (trDetails == null) {
            throw new NullPointerException(
                    "Could not locate details for " + ucdProperty.name() + " in " + TR57URL);
        }
        String formattedSyntax = formatTRSyntax(trDetails, false);

        return "  code-point-attributes &amp;=" + attributeString + NEWLINE + formattedSyntax;
    }

    private static String getFormattedElement(UcdProperty ucdProperty) {
        // Currently scoped to UcdProperty.Name_Alias, but might need to handle different
        // properties.
        String nameAliasElement = "name-alias";
        List<String> values = getNameAliasTypeValues();
        PropertyParsingInfo propInfo = PropertyParsingInfo.getPropertyInfo(ucdProperty);

        String elementString = "    element " + nameAliasElement + " {" + NEWLINE;
        String attributeAliasString =
                "      attribute alias { xsd:string { pattern=\""
                        + cleanRegex(propInfo.getRegex().toString())
                        + "\" } }?,"
                        + NEWLINE;
        String attributeTypeString = "      attribute type  ";

        String formattedValues =
                formatValues(
                        attributeTypeString.length(), values, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP);

        return "  code-point-attributes &amp;="
                + NEWLINE
                + elementString
                + attributeAliasString
                + attributeTypeString
                + "{ "
                + formattedValues
                + NEWLINE
                + String.format(
                        "%" + (attributeTypeString.length() + "}? } *".length()) + "s", "}? } *");
    }

    private static String getFormattedBoolean(UcdProperty ucdProperty) {
        String attributeString = "    attribute " + ucdProperty.getShortName() + " ";

        return "  code-point-attributes &amp;=" + NEWLINE + attributeString + "{ boolean }?";
    }

    private static String getFormattedValues(SCHEMA schema, VALUESOUTPUTTYPE valuesoutputtype) {
        List<String> values = getBinaryValues();
        String formattedValues = formatValues(2, values, valuesoutputtype);
        return "  " + schema.getName() + " = " + formattedValues;
    }

    private static String getFormattedPropertyValues(
            UcdProperty ucdProperty, VALUESOUTPUTTYPE valuesoutputtype) {
        List<String> values = getScriptValues();
        String formattedValues = formatValues(11, values, valuesoutputtype);
        return "  " + ucdProperty.name().toLowerCase() + " = " + formattedValues;
    }

    private static String getFormattedDoNotEmit(VALUESOUTPUTTYPE valuesoutputtype) {
        List<String> values = getDoNotEmitTypeValues();
        String formattedValues = formatValues(26, values, valuesoutputtype);
        return "  ucd.content &amp;=\n"
                + "    element do-not-emit {\n"
                + "      element instead {\n"
                + "        attribute of { one-or-more-code-points },\n"
                + "        attribute use { one-or-more-code-points },\n"
                + "        attribute because { "
                + formattedValues
                + NEWLINE
                + "      } }+ }?";
    }

    private static String formatTRSyntax(TRDetails trDetails, boolean isShowIfEmpty) {
        // TODO: We should determine whether we still want to show empty values in the XML files.
        // TODO: See org.unicode.xml.UcdPropertyDetail.isCJKShowIfEmpty()
        boolean isList = trDetails.isList();
        String syntax = cleanRegex(trDetails.getSyntax());
        // This is a kludge as it depends on only having single OR double quotes in the syntax. If
        // we have both, we'll
        // need to do more investigation on what RELAXNG Compact supports.
        String QUOTMARK = syntax.contains("\"") ? "'" : "\"";

        boolean hasNewlines = syntax.contains("\n");
        if (hasNewlines) {
            int indent;
            String firstLinePrefix;
            String ending = isList ? "    )+}}?" : "    }?";
            if (isShowIfEmpty) {
                indent = (isList ? 15 : 8);
                firstLinePrefix = isList ? "    { \"\" | list { " : "    { \"\" | ";
            } else {
                indent = (isList ? 12 : 4);
                firstLinePrefix = isList ? "    { list { ( " : "    { ";
            }
            String padding = String.format("%" + indent + "s", "");
            StringBuilder formattedSyntaxBuilder = new StringBuilder();
            Pattern syntaxPattern = Pattern.compile("([^\r\n]+)");
            Matcher matcher = syntaxPattern.matcher(syntax);
            while (matcher.find()) {
                if (formattedSyntaxBuilder.length() == 0) {
                    // First line
                    formattedSyntaxBuilder
                            .append(firstLinePrefix)
                            .append("xsd:string { pattern=")
                            .append(QUOTMARK)
                            .append(matcher.group(1))
                            .append(QUOTMARK)
                            .append(" }")
                            .append(NEWLINE);
                } else {
                    // Everything else
                    formattedSyntaxBuilder
                            .append(padding)
                            .append(
                                    matcher.group(1)
                                            .replaceAll(
                                                    "^[| ]*",
                                                    " | xsd:string { pattern=" + QUOTMARK))
                            .append(QUOTMARK)
                            .append(" }")
                            .append(NEWLINE);
                }
            }
            formattedSyntaxBuilder.append(ending);
            return formattedSyntaxBuilder.toString();

        } else {
            if (isShowIfEmpty) {
                if (isList) {
                    return "    { \"\" | list { xsd:string { pattern="
                            + QUOTMARK
                            + syntax
                            + QUOTMARK
                            + " }+ } }?";
                } else {
                    return "    { \"\" | xsd:string { pattern="
                            + QUOTMARK
                            + syntax
                            + QUOTMARK
                            + " } }?";
                }
            } else {
                if (isList) {
                    return "    { list { xsd:string { pattern="
                            + QUOTMARK
                            + syntax
                            + QUOTMARK
                            + " }+ } }?";
                } else {
                    return "    { xsd:string { pattern=" + QUOTMARK + syntax + QUOTMARK + " } }?";
                }
            }
        }
    }

    private static String formatValues(
            int indent, List<String> values, VALUESOUTPUTTYPE valuesoutputtype) {
        StringBuilder valueBlock = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        String padding = String.format("%" + indent + "s", "");
        String groupPrefix = "";
        for (String value : values) {
            StringBuilder formattedValue = new StringBuilder();
            if (valueBlock.length() > 0 || currentLine.length() > 0) {
                formattedValue.append("| ");
            }
            if (value.startsWith("xsd")) {
                formattedValue.append(value);
            } else {
                formattedValue.append("\"").append(value).append("\"");
            }

            switch (valuesoutputtype) {
                case NUMERICAL_GROUP:
                case ALPHABETICAL_GROUP:
                    String valuePrefix = getValuePrefix(value, valuesoutputtype);
                    if (groupPrefix.isEmpty()) {
                        currentLine.append(formattedValue);
                        groupPrefix = valuePrefix;
                    } else if (valuePrefix.equals(groupPrefix)) {
                        int testLength =
                                valueBlock.length() == 0
                                        ? padding.length() + currentLine.length() + " ".length()
                                        : currentLine.length() + " ".length();
                        if ((testLength + formattedValue.length()) > MAX_LINE_LENGTH) {
                            valueBlock.append(currentLine).append(NEWLINE);
                            currentLine.setLength(0);
                            currentLine.append(padding).append(formattedValue);
                        } else {
                            if (currentLine.length() > 0) {
                                currentLine.append(" ");
                            }
                            currentLine.append(formattedValue);
                        }
                    } else {
                        valueBlock.append(currentLine).append(NEWLINE);
                        currentLine.setLength(0);
                        currentLine.append(padding).append(formattedValue);
                        groupPrefix = valuePrefix;
                    }
                    break;

                case MAX_LINE_LENGTH:
                    int testLength =
                            valueBlock.length() == 0
                                    ? padding.length() + currentLine.length() + " ".length()
                                    : currentLine.length() + " ".length();
                    if ((testLength + formattedValue.length()) > MAX_LINE_LENGTH) {
                        valueBlock.append(currentLine).append(NEWLINE);
                        currentLine.setLength(0);
                        currentLine.append(padding).append(formattedValue);
                    } else {
                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                        }
                        currentLine.append(formattedValue);
                    }
                    break;

                case VALUE_PER_LINE:
                default:
                    if (valueBlock.length() > 0) {
                        valueBlock.append(NEWLINE).append(padding).append("| ");
                    }
                    if (value.startsWith("xsd")) {
                        valueBlock.append(value);
                    } else {
                        valueBlock.append("\"").append(value).append("\"");
                    }
            }
        }
        valueBlock.append(currentLine);
        return valueBlock.toString();
    }

    private static String getValuePrefix(String value, VALUESOUTPUTTYPE valuesoutputtype) {
        if (valuesoutputtype == VALUESOUTPUTTYPE.ALPHABETICAL_GROUP) {
            return value.substring(0, 1);
        }
        if (valuesoutputtype == VALUESOUTPUTTYPE.NUMERICAL_GROUP) {
            if (value.contains(".")) {
                return value.substring(0, value.indexOf("."));
            } else {
                // String value in list of numbers. See Age_Values for an example.
                return value;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String cleanRegex(String regex) {
        return regex.replaceAll("\\[-", "[\\\\-")
                .replaceAll("\\\\/", "/")
                .replaceAll("\\\\'", "'")
                .replaceAll("\\t", "");
    }

    // ********************* Combined properties ********************//

    private static String getFormattedDecompositionProperties() {
        return getFormattedAttribute(
                        UcdProperty.Decomposition_Type, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Decomposition_Mapping);
    }

    private static String getFormattedCompositionProperties() {
        return getFormattedBoolean(UcdProperty.Composition_Exclusion)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Full_Composition_Exclusion);
    }

    private static String getFormattedQuickCheckProperties() {
        return getFormattedAttribute(UcdProperty.NFC_Quick_Check, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.NFD_Quick_Check, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.NFKC_Quick_Check, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.NFKD_Quick_Check, VALUESOUTPUTTYPE.MAX_LINE_LENGTH);
    }

    private static String getFormattedNumericProperties() {
        return getFormattedAttribute(UcdProperty.Numeric_Type, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Numeric_Value);
    }

    private static String getFormattedJoiningProperties() {
        return getFormattedAttribute(UcdProperty.Joining_Type, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.Joining_Group, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP);
    }

    private static String getFormattedCasingProperties() {
        return getFormattedBoolean(UcdProperty.Uppercase)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Lowercase)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Uppercase)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Lowercase);
    }

    private static String getFormattedSimpleCaseMappingProperties() {
        return getFormattedSyntax(UcdProperty.Simple_Uppercase_Mapping)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Simple_Lowercase_Mapping)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Simple_Titlecase_Mapping);
    }

    private static String getFormattedCaseMappingProperties() {
        return getFormattedSyntax(UcdProperty.Uppercase_Mapping)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Lowercase_Mapping)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Titlecase_Mapping);
    }

    private static String getFormattedCaseFoldingProperties() {
        return getFormattedSyntax(UcdProperty.Simple_Case_Folding)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Case_Folding);
    }

    private static String getFormattedCaseOtherProperties() {
        return getFormattedBoolean(UcdProperty.Case_Ignorable)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Cased)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_Casefolded)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_Casemapped)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_Lowercased)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_NFKC_Casefolded)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_Titlecased)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Changes_When_Uppercased)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.NFKC_Casefold)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.NFKC_Simple_Casefold);
    }

    private static String getFormattedScriptProperties() {
        return getFormattedPropertyValues(UcdProperty.Script, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Script)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Script_Extensions);
    }

    private static String getFormattedIdentifierProperties() {
        return getFormattedBoolean(UcdProperty.ID_Start)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_ID_Start)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.XID_Start)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.ID_Continue)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_ID_Continue)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.XID_Continue)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.ID_Compat_Math_Start)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.ID_Compat_Math_Continue);
    }

    private static String getFormattedPatternProperties() {
        return getFormattedBoolean(UcdProperty.Pattern_Syntax)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Pattern_White_Space);
    }

    private static String getFormattedFunctionGraphicProperties() {
        return getFormattedBoolean(UcdProperty.Dash)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Quotation_Mark)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Terminal_Punctuation)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Sentence_Terminal)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Diacritic)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Extender)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Soft_Dotted)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Alphabetic)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Alphabetic)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Math)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Math)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Hex_Digit)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.ASCII_Hex_Digit)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Default_Ignorable_Code_Point)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Default_Ignorable_Code_Point)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Logical_Order_Exception)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Prepended_Concatenation_Mark)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Modifier_Combining_Mark)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.White_Space)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.Vertical_Orientation, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Regional_Indicator);
    }

    private static String getFormattedBoundaryProperties() {
        return getFormattedBoolean(UcdProperty.Grapheme_Base)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Grapheme_Extend)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Other_Grapheme_Extend)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.Grapheme_Cluster_Break, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP)
                + DOUBLELINE
                + getFormattedAttribute(UcdProperty.Word_Break, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP)
                + DOUBLELINE
                + getFormattedAttribute(
                        UcdProperty.Sentence_Break, VALUESOUTPUTTYPE.ALPHABETICAL_GROUP);
    }

    private static String getFormattedIdeographProperties() {
        return getFormattedBoolean(UcdProperty.Ideographic)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Unified_Ideograph)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.Equivalent_Unified_Ideograph)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.IDS_Binary_Operator)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.IDS_Trinary_Operator)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.IDS_Unary_Operator)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Radical);
    }

    private static String getFormattedMiscellaneousProperties() {
        return getFormattedBoolean(UcdProperty.Deprecated)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Variation_Selector)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Noncharacter_Code_Point);
    }

    private static String getFormattedUnihanProperties() {
        return getFormattedTR38Syntax(UcdProperty.kAccountingNumeric)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kAlternateTotalStrokes)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kBigFive)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCangjie)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCantonese)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCCCII)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCheungBauer)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCheungBauerIndex)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCihaiT)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCNS1986)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCNS1992)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCompatibilityVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kCowles)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kDaeJaweon)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kDefinition)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kEACC)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kFanqie)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kFenn)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kFennIndex)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kFourCornerCode)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGB0)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGB1)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGB3)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGB5)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGB8)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGradeLevel)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kGSR)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHangul)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHanYu)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHanyuPinlu)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHanyuPinyin)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHDZRadBreak)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kHKGlyph)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIBMJapan)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIICore)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_GSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_HSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_JSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_KPSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_KSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_MSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_SSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_TSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_UKSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_USource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRG_VSource)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRGDaeJaweon)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRGHanyuDaZidian)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kIRGKangXi)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJa)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJapanese)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJapaneseKun)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJapaneseOn)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJinmeiyoKanji)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJis0)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJis1)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJIS0213)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kJoyoKanji)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kKangXi)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kKarlgren)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kKorean)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kKoreanEducationHanja)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kKoreanName)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kLau)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMainlandTelegraph)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMandarin)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMatthews)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMeyerWempe)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMojiJoho)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kMorohashi)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kNelson)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kOtherNumeric)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kPhonetic)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kPrimaryNumeric)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kPseudoGB1)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kRSAdobe_Japan1_6)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kRSUnicode)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSBGY)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSemanticVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSimplifiedVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSMSZD2003Index)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSMSZD2003Readings)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSpecializedSemanticVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kSpoofingVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kStrange)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTaiwanTelegraph)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTang)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTGH)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTGHZ2013)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTotalStrokes)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kTraditionalVariant)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kUnihanCore2020)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kVietnamese)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kVietnameseNumeric)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kXerox)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kXHC1983)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kZhuang)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kZhuangNumeric)
                + DOUBLELINE
                + getFormattedTR38Syntax(UcdProperty.kZVariant);
    }

    private static String getFormattedTangutProperties() {
        return getFormattedSyntax(UcdProperty.kTGT_RSUnicode)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.kTGT_MergedSrc);
    }

    private static String getFormattedNushuProperties() {
        return getFormattedSyntax(UcdProperty.kNSHU_DubenSrc)
                + DOUBLELINE
                + getFormattedSyntax(UcdProperty.kNSHU_Reading);
    }

    private static String getFormattedEmojiProperties() {
        return getFormattedBoolean(UcdProperty.Emoji)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Emoji_Presentation)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Emoji_Modifier)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Emoji_Modifier_Base)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Emoji_Component)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.Extended_Pictographic);
    }

    private static String getFormattedUnikemetProperties() {
        return getFormattedTR57Syntax(UcdProperty.kEH_Cat)
                + DOUBLELINE
                + getFormattedAttribute(UcdProperty.kEH_Core, VALUESOUTPUTTYPE.MAX_LINE_LENGTH)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_Desc)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_Func)
                + DOUBLELINE
                // Force kEH_FVal to text while under development.
                + getFormattedSyntax(UcdProperty.kEH_FVal)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_UniK)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_JSesh)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_HG)
                + DOUBLELINE
                + getFormattedTR57Syntax(UcdProperty.kEH_IFAO)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.kEH_NoMirror)
                + DOUBLELINE
                + getFormattedBoolean(UcdProperty.kEH_NoRotate)
                + DOUBLELINE
                // Force kEH_AltSeq to text while under development.
                + getFormattedSyntax(UcdProperty.kEH_AltSeq);
    }

    // ********************* Attribute values ********************//

    private static List<String> getBinaryValues() {
        List<String> values = new ArrayList<>();
        for (Binary binaryValues : Binary.values()) {
            values.add(binaryValues.getShortName());
        }
        // Binary should display as Y | N.
        values.sort(Collections.reverseOrder());
        return values;
    }

    private static List<String> getAgeValues() {
        List<String> values = new ArrayList<>();
        for (Age_Values ageValues : Age_Values.values()) {
            String shortName = ageValues.getShortName();
            if (shortName.equals("NA")) {
                values.add("unassigned");
            } else if (shortName.equals("13.1")) {
                // https://github.com/unicode-org/unicodetools/issues/100
            } else {
                values.add(shortName);
            }
        }
        return values;
    }

    private static List<String> getNameAliasTypeValues() {
        List<String> values = new ArrayList<>();
        for (AttributeResolver.AliasType aliastypeValues : AttributeResolver.AliasType.values()) {
            if (!aliastypeValues.equals(AttributeResolver.AliasType.NONE)) {
                values.add(aliastypeValues.toString());
            }
        }
        return values;
    }

    private static List<String> getBlockValues() {
        List<String> values = new ArrayList<>();
        for (Block_Values blockValues : Block_Values.values()) {
            values.add(blockValues.getShortName());
        }
        return values;
    }

    private static List<String> getGeneralCategoryValues() {
        List<String> values = new ArrayList<>();
        for (General_Category_Values generalCategoryValues : General_Category_Values.values()) {
            if (!generalCategoryValues
                    .getShortName()
                    .toUpperCase()
                    .equals(generalCategoryValues.getShortName())) {
                // Some of the General_Category_Values (LC, L, M, N, P, S, Z, C) stand for grouping
                // of related
                // General_Category values. They won't occur on any individual code point, so can be
                // ignored.
                values.add(generalCategoryValues.getShortName());
            }
        }
        return values;
    }

    private static List<String> getCanonicalCombiningClassValues() {
        List<String> values = new ArrayList<>();
        values.add("xsd:integer { minInclusive=\"0\" maxInclusive=\"254\" }");
        // Because the set of values that this property has taken across the various versions of the
        // UCD is rather
        // large, our schema does not restrict the possible values to those actually used.
        // for (Canonical_Combining_Class_Values canonicalCombiningClassValues :
        //        Canonical_Combining_Class_Values.values()) {
        //    values.add(canonicalCombiningClassValues.getShortName());
        // }
        return values;
    }

    private static List<String> getBidirectionalValues() {
        List<String> values = new ArrayList<>();
        for (Bidi_Class_Values bidiClassValues : Bidi_Class_Values.values()) {
            values.add(bidiClassValues.getShortName());
        }
        return values;
    }

    private static List<String> getBidiPairedBracketTypeValues() {
        List<String> values = new ArrayList<>();
        // Order should be Open/Close/None
        values.add(Bidi_Paired_Bracket_Type_Values.Open.getShortName());
        values.add(Bidi_Paired_Bracket_Type_Values.Close.getShortName());
        values.add(Bidi_Paired_Bracket_Type_Values.None.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (Bidi_Paired_Bracket_Type_Values bidiPairedBracketTypeValue :
                Bidi_Paired_Bracket_Type_Values.values()) {
            if (!values.contains(bidiPairedBracketTypeValue.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getDecompositionTypeValues() {
        List<String> values = new ArrayList<>();
        for (Decomposition_Type_Values decompositionTypeValues :
                Decomposition_Type_Values.values()) {
            // We want "none" to be last.
            if (decompositionTypeValues != Decomposition_Type_Values.None) {
                values.add(decompositionTypeValues.getNames().getOtherNames().get(0));
            }
        }
        values.add(Decomposition_Type_Values.None.getNames().getOtherNames().get(0));
        return values;
    }

    private static List<String> getNFCQuickCheckValues() {
        List<String> values = new ArrayList<>();
        // Order should be Yes/No/Maybe
        values.add(NFC_Quick_Check_Values.Yes.getShortName());
        values.add(NFC_Quick_Check_Values.No.getShortName());
        values.add(NFC_Quick_Check_Values.Maybe.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (NFC_Quick_Check_Values nfcQuickCheckValues : NFC_Quick_Check_Values.values()) {
            if (!values.contains(nfcQuickCheckValues.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getNFDQuickCheckValues() {
        List<String> values = new ArrayList<>();
        // Order should be Yes/No
        values.add(NFD_Quick_Check_Values.Yes.getShortName());
        values.add(NFD_Quick_Check_Values.No.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (NFD_Quick_Check_Values nfdQuickCheckValues : NFD_Quick_Check_Values.values()) {
            if (!values.contains(nfdQuickCheckValues.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getNFKCQuickCheckValues() {
        List<String> values = new ArrayList<>();
        // Order should be Yes/No/Maybe
        values.add(NFKC_Quick_Check_Values.Yes.getShortName());
        values.add(NFKC_Quick_Check_Values.No.getShortName());
        values.add(NFKC_Quick_Check_Values.Maybe.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (NFKC_Quick_Check_Values nfkcQuickCheckValues : NFKC_Quick_Check_Values.values()) {
            if (!values.contains(nfkcQuickCheckValues.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getNFKDQuickCheckValues() {
        List<String> values = new ArrayList<>();
        // Order should be Yes/No
        values.add(NFKD_Quick_Check_Values.Yes.getShortName());
        values.add(NFKD_Quick_Check_Values.No.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (NFKD_Quick_Check_Values nfkdQuickCheckValues : NFKD_Quick_Check_Values.values()) {
            if (!values.contains(nfkdQuickCheckValues.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getNumericTypeValues() {
        List<String> values = new ArrayList<>();
        // Order should be Decimal/Digit/Numeric/None
        values.add(Numeric_Type_Values.Decimal.getShortName());
        values.add(Numeric_Type_Values.Digit.getShortName());
        values.add(Numeric_Type_Values.Numeric.getShortName());
        values.add(Numeric_Type_Values.None.getShortName());
        // Now let's check to see if there is anything else that we didn't expect
        for (Numeric_Type_Values numericTypeValues : Numeric_Type_Values.values()) {
            if (!values.contains(numericTypeValues.getShortName())) {
                throw new IllegalArgumentException();
            }
        }
        return values;
    }

    private static List<String> getJoiningTypeValues() {
        List<String> values = new ArrayList<>();
        for (Joining_Type_Values joiningTypeValues : Joining_Type_Values.values()) {
            values.add(joiningTypeValues.getShortName());
        }
        return values;
    }

    private static List<String> getJoiningGroupValues() {
        List<String> values = new ArrayList<>();
        for (Joining_Group_Values joiningGroupValues : Joining_Group_Values.values()) {
            values.add(joiningGroupValues.getShortName());
        }
        return values;
    }

    private static List<String> getLineBreakValues() {
        List<String> values = new ArrayList<>();
        for (Line_Break_Values lineBreakValues : Line_Break_Values.values()) {
            values.add(lineBreakValues.getShortName());
        }
        return values;
    }

    private static List<String> getEastAsianWidthValues() {
        List<String> values = new ArrayList<>();
        for (East_Asian_Width_Values eastAsianWidthValues : East_Asian_Width_Values.values()) {
            values.add(eastAsianWidthValues.getShortName());
        }
        return values;
    }

    private static List<String> getScriptValues() {
        List<Script_Values> excludedValues =
                Arrays.asList(
                        Script_Values.Han_with_Bopomofo,
                        Script_Values.Japanese,
                        Script_Values.Korean,
                        Script_Values.Math_Symbols,
                        Script_Values.Emoji_Symbols,
                        Script_Values.Other_Symbols,
                        Script_Values.Unwritten);
        List<String> values = new ArrayList<>();
        for (Script_Values scriptValue : Script_Values.values()) {
            if (!excludedValues.contains(scriptValue)) {
                values.add(scriptValue.getShortName());
            }
            // Include the following if you want to add other names
            // if (!scriptValue.getNames().getOtherNames().isEmpty()) {
            //    values.add(scriptValue.getNames().getOtherNames().get(0));
            // }
        }
        Collections.sort(values);
        return values;
    }

    private static List<String> getHangulSyllableTypeValues() {
        List<String> values = new ArrayList<>();
        for (Hangul_Syllable_Type_Values hangulSyllableTypeValues :
                Hangul_Syllable_Type_Values.values()) {
            values.add(hangulSyllableTypeValues.getShortName());
        }
        return values;
    }

    private static List<String> getIndicSyllabicCategoryValues() {
        List<String> values = new ArrayList<>();
        for (Indic_Syllabic_Category_Values indicSyllabicCategoryValues :
                Indic_Syllabic_Category_Values.values()) {
            values.add(indicSyllabicCategoryValues.getShortName());
        }
        return values;
    }

    private static List<String> getIndicPositionalCategoryValues() {
        List<String> values = new ArrayList<>();
        for (Indic_Positional_Category_Values indicPositionalCategoryValues :
                Indic_Positional_Category_Values.values()) {
            values.add(indicPositionalCategoryValues.getShortName());
        }
        return values;
    }

    private static List<String> getIndicConjunctBreakValues() {
        List<String> values = new ArrayList<>();
        for (Indic_Conjunct_Break_Values indicConjunctBreakValues :
                Indic_Conjunct_Break_Values.values()) {
            values.add(indicConjunctBreakValues.getShortName());
        }
        return values;
    }

    private static List<String> getVerticalOrientationValues() {
        List<String> values = new ArrayList<>();
        for (Vertical_Orientation_Values verticalOrientationValues :
                Vertical_Orientation_Values.values()) {
            values.add(verticalOrientationValues.getShortName());
        }
        return values;
    }

    private static List<String> getGraphemeClusterBreakValues() {
        List<String> values = new ArrayList<>();
        for (Grapheme_Cluster_Break_Values graphemeClusterBreakValues :
                Grapheme_Cluster_Break_Values.values()) {
            values.add(graphemeClusterBreakValues.getShortName());
        }
        return values;
    }

    private static List<String> getWordBreakValues() {
        List<String> values = new ArrayList<>();
        for (Word_Break_Values wordBreakValues : Word_Break_Values.values()) {
            values.add(wordBreakValues.getShortName());
        }
        return values;
    }

    private static List<String> getSentenceBreakValues() {
        List<String> values = new ArrayList<>();
        for (Sentence_Break_Values sentenceBreakValues : Sentence_Break_Values.values()) {
            values.add(sentenceBreakValues.getShortName());
        }
        return values;
    }

    private static List<String> getDoNotEmitTypeValues() {
        List<String> values = new ArrayList<>();
        for (Do_Not_Emit_Type_Values doNotEmitTypeValues : Do_Not_Emit_Type_Values.values()) {
            values.add(doNotEmitTypeValues.getShortName());
        }
        Collections.sort(values);
        return values;
    }

    private static List<String> getkEHCoreValues() {
        List<String> values = new ArrayList<>();
        for (kEH_Core_Values kEHCoreValues : kEH_Core_Values.values()) {
            values.add(kEHCoreValues.getShortName());
        }
        Collections.sort(values);
        return values;
    }

    // ********************* Utility methods ********************//

    private static HashMap<String, TRDetails> parseTR(String url)
            throws IOException, URISyntaxException {
        HashMap<String, TRDetails> syntaxTR = new HashMap<>();
        URI uri = new URI(url);
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream is = uri.toURL().openStream()) {
            int ptr = 0;
            while ((ptr = is.read()) != -1) {
                stringBuilder.append((char) ptr);
            }
        }
        Pattern syntaxPattern =
                Pattern.compile(
                        ">Property</td>.*?<strong>(.*?)</strong>.*?>Delimiter</td>.*?>(.*?)</td>.*?>Syntax</td>.*?>(.*?)</td>",
                        Pattern.DOTALL);
        Matcher matcher = syntaxPattern.matcher(stringBuilder.toString());
        while (matcher.find()) {
            String delimiter = matcher.group(2).trim();
            if (delimiter.contains("</span>")) {
                delimiter =
                        delimiter
                                .replaceAll("<span class=\"removed\">[^<]*</span>", "")
                                .replaceAll("<span class=\"changed\">", "")
                                .replaceAll("</span>", "");
            }
            boolean isList = false;
            switch (delimiter) {
                case "N/A":
                    break;
                    // The next two are to support two Provisional attributes in Unikemet. We'll
                    // process these as
                    // exceptions for now
                case "/ (see description)": // kEH_Func
                case "/ or | (see description)": // kEH_FVal
                    break;
                case "space":
                    isList = true;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Only \"space\" or \"N/A\" are supported values for Delimiter."
                                    + " Found: "
                                    + delimiter);
            }
            TRDetails trDetails =
                    new TRDetails(
                            isList,
                            matcher.group(3)
                                    .trim()
                                    .replaceAll("<br>", "")
                                    .replaceAll("<span class=\"removed\">.*?</span>", "")
                                    .replaceAll("<span class=\"changed\">", "")
                                    .replaceAll("</span>", ""));
            syntaxTR.put(matcher.group(1).trim(), trDetails);
        }
        return syntaxTR;
    }
}
