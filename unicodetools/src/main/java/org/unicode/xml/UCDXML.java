package org.unicode.xml;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.util.VersionInfo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.TransformerConfigurationException;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.text.utility.Settings;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Utility for generating UCDXML files. The utility can build flat or grouped versions of UCDXML for
 * non-Unihan code points, Unihan code points, or the complete range of code points.
 */
public class UCDXML {

    private static final String NAMESPACE = "http://www.unicode.org/ns/2003/ucd/1.0";

    private enum UCDXMLOUTPUTRANGE {
        ALL,
        NOUNIHAN,
        UNIHAN;
    }

    private enum UCDXMLOUTPUTTYPE {
        FLAT,
        GROUPED;
    }

    private enum Range {
        RESERVED("reserved"),
        SURROGATE("surrogate"),
        NONCHARACTER("noncharacter"),
        CHARACTER("char"),
        CJKUNIFIEDIDEOGRAPH("char"),
        NONRANGE("nonrange");

        private final String tag;

        Range(String tag) {
            this.tag = tag;
        }

        public String toString() {
            return tag;
        }
    }

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.create("ucdversion", 'v', UOption.OPTIONAL_ARG),
        UOption.create("range", 'r', UOption.REQUIRES_ARG),
        UOption.create("output", 'o', UOption.REQUIRES_ARG),
        UOption.create("outputfolder", 'f', UOption.OPTIONAL_ARG)
    };
    private static final int HELP = 0, UCDVERSION = 1, RANGE = 2, OUTPUT = 3, OUTPUTFOLDER = 4;

    public static void main(String[] args) throws Exception {

        VersionInfo ucdVersion = null;
        UCDXMLOUTPUTRANGE[] ucdxmloutputranges =
                new UCDXMLOUTPUTRANGE[] {
                    UCDXMLOUTPUTRANGE.ALL, UCDXMLOUTPUTRANGE.NOUNIHAN, UCDXMLOUTPUTRANGE.UNIHAN
                };
        UCDXMLOUTPUTTYPE[] ucdxmloutputtypes =
                new UCDXMLOUTPUTTYPE[] {UCDXMLOUTPUTTYPE.FLAT, UCDXMLOUTPUTTYPE.GROUPED};
        File destinationFolder = null;

        UOption.parseArgs(args, options);

        if (options[HELP].doesOccur) {
            System.out.println(
                    "UCDXML [--ucdversion {version number}] [--outputfolder {destination}] "
                            + "--range [ALL|NOUNIHAN|UNIHAN] --output [FLAT|GROUPED]");
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
            if (options[RANGE].doesOccur) {
                try {
                    ucdxmloutputranges =
                            new UCDXMLOUTPUTRANGE[] {
                                UCDXMLOUTPUTRANGE.valueOf(
                                        options[RANGE].value.toUpperCase(Locale.ROOT))
                            };
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Could not convert "
                                    + options[RANGE].value
                                    + " to one of [ALL|NOUNIHAN|UNIHAN]");
                }
            }
            if (options[OUTPUT].doesOccur) {
                try {
                    ucdxmloutputtypes =
                            new UCDXMLOUTPUTTYPE[] {
                                UCDXMLOUTPUTTYPE.valueOf(
                                        options[OUTPUT].value.toUpperCase(Locale.ROOT))
                            };
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Could not convert "
                                    + options[OUTPUT].value
                                    + " to one of [FLAT|GROUPED]");
                }
            }
            if (options[OUTPUTFOLDER].doesOccur) {
                try {
                    destinationFolder =
                            new File(
                                    options[OUTPUTFOLDER].value
                                            + ucdVersion.getVersionString(3, 3)
                                            + "/");
                    if (!destinationFolder.exists()) {
                        if (!destinationFolder.mkdirs()) {
                            throw new IOException();
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Could not find or create " + options[OUTPUTFOLDER].value);
                }
            } else {
                try {
                    destinationFolder =
                            new File(
                                    Settings.Output.GEN_DIR
                                            + "ucdxml\\"
                                            + ucdVersion.getVersionString(3, 3)
                                            + "\\");
                    if (!destinationFolder.exists()) {
                        if (!destinationFolder.mkdirs()) {
                            throw new IOException();
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Could not find or create "
                                    + Settings.Output.GEN_DIR
                                    + "ucdxml\\"
                                    + ucdVersion.getVersionString(3, 3)
                                    + "\\");
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (ucdVersion != null && destinationFolder.exists()) {
            for (UCDXMLOUTPUTRANGE ucdxmloutputrange : ucdxmloutputranges) {
                for (UCDXMLOUTPUTTYPE ucdxmloutputtype : ucdxmloutputtypes) {
                    System.out.println(
                            "Building the "
                                    + ucdxmloutputrange
                                    + " "
                                    + ucdxmloutputtype
                                    + " UcdXML file for "
                                    + ucdVersion);
                    buildUcdXMLFile(
                            ucdVersion, destinationFolder, ucdxmloutputrange, ucdxmloutputtype);
                }
            }
            System.out.println("End");
            System.exit(0);
        } else {
            System.err.println("Unexpected error when building UcdXML file.");
            System.exit(1);
        }
    }

    private static void buildUcdXMLFile(
            VersionInfo ucdVersion,
            File destinationFolder,
            UCDXMLOUTPUTRANGE outputRange,
            UCDXMLOUTPUTTYPE outputType)
            throws IOException, TransformerConfigurationException, SAXException {
        int lowCodePoint = 0x0;
        int highCodePoint = 0x10FFFF;
        // Tangut
        // int lowCodePoint = 0x17000;
        // int highCodePoint = 0x1B2FB;
        // 0x10FFFF

        File tempFile = new File(destinationFolder, "temp.xml");
        String outputFilename =
                "ucd."
                        + outputRange.toString().toLowerCase(Locale.ROOT)
                        + "."
                        + outputType.toString().toLowerCase(Locale.ROOT)
                        + ".xml";
        File destinationFile = new File(destinationFolder, outputFilename);

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        UCDXMLWriter writer = new UCDXMLWriter(fileOutputStream);

        IndexUnicodeProperties iup = IndexUnicodeProperties.make(ucdVersion);
        AttributeResolver attributeResolver = new AttributeResolver(iup);
        UCDDataResolver ucdDataResolver = new UCDDataResolver(iup, NAMESPACE, writer);

        writer.startFile();
        writer.startElement("ucd");
        {
            writer.startElement("description");
            {
                writer.addContent("Unicode " + ucdVersion.getVersionString(3, 3));
                writer.endElement("description");
            }
            buildRepertoire(
                    writer,
                    attributeResolver,
                    ucdVersion,
                    lowCodePoint,
                    highCodePoint,
                    outputRange,
                    outputType);
            if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.BLOCKS);
                ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.NAMEDSEQUENCES);
                ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.PROVISIONALNAMEDSEQUENCES);
                ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.NORMALIZATIONCORRECTIONS);
                ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.STANDARDIZEDVARIANTS);
                if (ucdVersion.compareTo(VersionInfo.getInstance(5, 2, 0)) >= 0) {
                    ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.CJKRADICALS);
                }
                if (ucdVersion.compareTo(VersionInfo.getInstance(6, 0, 0)) >= 0) {
                    ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.EMOJISOURCES);
                }
                if (ucdVersion.compareTo(VersionInfo.getInstance(16, 0, 0)) >= 0) {
                    ucdDataResolver.buildSection(UCDSectionDetail.UcdSection.DONOTEMIT);
                }
            }
            writer.endElement("ucd");
        }
        writer.endFile();
        fileOutputStream.close();
        cleanUcdXMLFile(tempFile, destinationFile);
        if (!tempFile.delete()) {
            throw new IOException("Could not delete temporary file " + tempFile);
        }
    }

    private static void cleanUcdXMLFile(File tempFile, File destinationFile) throws IOException {
        // XALAN writes out characters outside the BMP as entities.
        // Use this code to replace the entities with the correct characters.
        // See: https://issues.apache.org/jira/browse/XALANJ-2595

        FileInputStream fileInputStream = new FileInputStream(tempFile);
        FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);

        InputStreamReader inputStreamReader =
                new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
        OutputStreamWriter outputStreamWriter =
                new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher matcher = Pattern.compile("&#(\\d+);").matcher(line);
            line =
                    matcher.replaceAll(
                            matchResult ->
                                    new String(
                                            Character.toChars(Integer.parseInt(matcher.group(1)))));
            bufferedWriter.append(line);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        fileInputStream.close();
        fileOutputStream.close();
    }

    private static void buildRepertoire(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            int lowCodePoint,
            int highCodePoint,
            UCDXMLOUTPUTRANGE outputRange,
            UCDXMLOUTPUTTYPE outputType)
            throws SAXException {

        writer.startElement("repertoire");
        {
            for (int CodePoint = lowCodePoint; CodePoint <= highCodePoint; CodePoint++) {
                if (isWritableCodePoint(CodePoint, outputRange, attributeResolver)) {
                    if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                        CodePoint =
                                buildGroup(
                                        writer,
                                        attributeResolver,
                                        ucdVersion,
                                        CodePoint,
                                        highCodePoint,
                                        outputRange,
                                        outputType);
                    } else {
                        CodePoint =
                                buildChars(
                                        writer,
                                        attributeResolver,
                                        ucdVersion,
                                        CodePoint,
                                        highCodePoint,
                                        outputRange,
                                        outputType,
                                        null);
                    }
                }
            }
            writer.endElement("repertoire");
        }
    }

    private static int buildGroup(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            int lowCodePoint,
            int highCodePoint,
            UCDXMLOUTPUTRANGE outputRange,
            UCDXMLOUTPUTTYPE outputType)
            throws SAXException {

        int lastCodePointInGroup =
                getLastCodePointInGroup(attributeResolver, lowCodePoint, highCodePoint);

        AttributesImpl groupAttrs =
                getGroupAttributes(
                        ucdVersion,
                        attributeResolver,
                        lowCodePoint,
                        lastCodePointInGroup,
                        outputRange);

        writer.startElement("group", groupAttrs);
        {
            buildChars(
                    writer,
                    attributeResolver,
                    ucdVersion,
                    lowCodePoint,
                    lastCodePointInGroup,
                    outputRange,
                    outputType,
                    groupAttrs);
            writer.endElement("group");
        }
        return lastCodePointInGroup;
    }

    private static int buildChars(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            int lowCodePoint,
            int highCodePoint,
            UCDXMLOUTPUTRANGE outputRange,
            UCDXMLOUTPUTTYPE outputType,
            AttributesImpl groupAttrs)
            throws SAXException {

        ArrayList<Integer> range = new ArrayList<>();
        Range rangeType = Range.NONRANGE;
        for (int CodePoint = lowCodePoint; CodePoint <= highCodePoint; CodePoint++) {
            if (attributeResolver.isUnassignedCodePoint(CodePoint)
                    || (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN
                            && attributeResolver.isUnifiedIdeograph(CodePoint))) {
                Range currentRangeType = getRangeType(attributeResolver, CodePoint);
                if (!range.isEmpty()) {
                    if (!currentRangeType.equals(rangeType)
                            || attributeResolver.isDifferentRange(
                                    ucdVersion, CodePoint, CodePoint - 1)) {
                        if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                            if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                                buildGroupedRange(
                                        writer,
                                        attributeResolver,
                                        ucdVersion,
                                        range,
                                        rangeType,
                                        groupAttrs);
                            } else {
                                buildUngroupedRange(
                                        writer, attributeResolver, ucdVersion, range, rangeType);
                            }
                        }
                        range.clear();
                    }
                }
                range.add(CodePoint);
                rangeType = currentRangeType;
            } else {
                if (!range.isEmpty()) {
                    if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                        if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                            buildGroupedRange(
                                    writer,
                                    attributeResolver,
                                    ucdVersion,
                                    range,
                                    rangeType,
                                    groupAttrs);
                        } else {
                            buildUngroupedRange(
                                    writer, attributeResolver, ucdVersion, range, rangeType);
                        }
                    }
                    range.clear();
                    rangeType = Range.NONRANGE;
                }
                if (isWritableCodePoint(CodePoint, outputRange, attributeResolver)) {
                    if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                        buildGroupedChar(
                                writer,
                                attributeResolver,
                                ucdVersion,
                                CodePoint,
                                outputRange,
                                groupAttrs);
                    } else {
                        buildUngroupedChar(
                                writer, attributeResolver, ucdVersion, CodePoint, outputRange);
                    }
                }
            }
        }
        // Handle any range before the end of the repertoire element.
        if (!range.isEmpty()) {
            if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                    buildGroupedRange(
                            writer, attributeResolver, ucdVersion, range, rangeType, groupAttrs);
                } else {
                    buildUngroupedRange(writer, attributeResolver, ucdVersion, range, rangeType);
                }
            }
        }
        return highCodePoint;
    }

    private static void buildUngroupedChar(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            int CodePoint,
            UCDXMLOUTPUTRANGE outputRange)
            throws SAXException {

        AttributesImpl charAttributes =
                getAttributes(ucdVersion, attributeResolver, CodePoint, outputRange);
        buildChar(writer, attributeResolver, CodePoint, charAttributes);
    }

    private static void buildGroupedChar(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            int CodePoint,
            UCDXMLOUTPUTRANGE outputRange,
            AttributesImpl groupAttrs)
            throws SAXException {

        AttributesImpl orgCharAttributes =
                getAttributes(ucdVersion, attributeResolver, CodePoint, outputRange);
        AttributesImpl charAttributes = new AttributesImpl();
        charAttributes.addAttribute(
                NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(CodePoint));

        for (UCDPropertyDetail propDetail : UCDPropertyDetail.ucdxmlValues()) {
            String qName = propDetail.getUcdProperty().getShortName();
            if (qName.startsWith("cjk")) {
                qName = qName.substring(2);
            }
            String orgCharAttributesValue = orgCharAttributes.getValue(qName);
            String groupAttributeValue = groupAttrs.getValue(qName);
            if (!Objects.equals(orgCharAttributesValue, groupAttributeValue)) {
                charAttributes.addAttribute(
                        NAMESPACE,
                        qName,
                        qName,
                        "CDATA",
                        Objects.requireNonNullElse(orgCharAttributesValue, ""));
            }
        }
        buildChar(writer, attributeResolver, CodePoint, charAttributes);
    }

    private static void buildChar(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            int CodePoint,
            AttributesImpl charAttributes)
            throws SAXException {
        writer.startElement("char", charAttributes);
        {
            HashMap<String, String> nameAliases = attributeResolver.getNameAliases(CodePoint);
            if (null != nameAliases && !nameAliases.isEmpty()) {
                for (String alias : nameAliases.keySet()) {
                    AttributesImpl nameAliasAt = new AttributesImpl();
                    nameAliasAt.addAttribute(NAMESPACE, "alias", "alias", "CDATA", alias);
                    String type = nameAliases.get(alias);
                    if (!Objects.equals(type, "none")) {
                        nameAliasAt.addAttribute(
                                NAMESPACE, "type", "type", "CDATA", nameAliases.get(alias));
                    }
                    writer.startElement("name-alias", nameAliasAt);
                    {
                        writer.endElement("name-alias");
                    }
                }
            }
            writer.endElement("char");
        }
    }

    private static void buildGroupedRange(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            ArrayList<Integer> range,
            Range rangeType,
            AttributesImpl groupAttrs)
            throws SAXException {
        AttributesImpl orgRangeAttributes =
                getReservedAttributes(ucdVersion, attributeResolver, range);
        AttributesImpl rangeAttributes = new AttributesImpl();
        if (range.size() == 1) {
            rangeAttributes.addAttribute(
                    NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(range.get(0)));
        } else {
            rangeAttributes.addAttribute(
                    NAMESPACE,
                    "first-cp",
                    "first-cp",
                    "CDATA",
                    attributeResolver.getHexString(range.get(0)));
            rangeAttributes.addAttribute(
                    NAMESPACE,
                    "last-cp",
                    "last-cp",
                    "CDATA",
                    attributeResolver.getHexString(range.get(range.size() - 1)));
        }

        for (UCDPropertyDetail propDetail : UCDPropertyDetail.ucdxmlValues()) {
            String qName = propDetail.getUcdProperty().getShortName();
            if (qName.startsWith("cjk")) {
                qName = qName.substring(2);
            }
            String orgCharAttributesValue = orgRangeAttributes.getValue(qName);
            String groupAttributeValue = groupAttrs.getValue(qName);
            if (!Objects.equals(orgCharAttributesValue, groupAttributeValue)) {
                rangeAttributes.addAttribute(
                        NAMESPACE,
                        qName,
                        qName,
                        "CDATA",
                        Objects.requireNonNullElse(orgCharAttributesValue, ""));
            }
        }
        writer.startElement(rangeType.tag, rangeAttributes);
        {
            writer.endElement(rangeType.tag);
        }
    }

    private static void buildUngroupedRange(
            UCDXMLWriter writer,
            AttributeResolver attributeResolver,
            VersionInfo ucdVersion,
            ArrayList<Integer> range,
            Range rangeType)
            throws SAXException {
        AttributesImpl rangeAttributes =
                getReservedAttributes(ucdVersion, attributeResolver, range);
        writer.startElement(rangeType.tag, rangeAttributes);
        {
            writer.endElement(rangeType.tag);
        }
    }

    private static boolean isWritableCodePoint(
            int CodePoint, UCDXMLOUTPUTRANGE outputRange, AttributeResolver attributeResolver) {
        return outputRange == UCDXMLOUTPUTRANGE.ALL
                || (outputRange == UCDXMLOUTPUTRANGE.UNIHAN
                        && attributeResolver.isUnihanAttributeRange(CodePoint))
                || (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN
                        && !attributeResolver.isUnifiedIdeograph(CodePoint));
    }

    private static Range getRangeType(AttributeResolver attributeResolver, int CodePoint) {
        String NChar = attributeResolver.getNChar(CodePoint);
        UcdPropertyValues.General_Category_Values gc = attributeResolver.getgc(CodePoint);

        if (attributeResolver.isUnihanAttributeRange(CodePoint)) {
            return Range.CJKUNIFIEDIDEOGRAPH;
        }
        if (gc.equals(UcdPropertyValues.General_Category_Values.Surrogate)) {
            return Range.SURROGATE;
        }
        if (gc.equals(UcdPropertyValues.General_Category_Values.Private_Use)) {
            return Range.CHARACTER;
        }
        if (NChar.equals(UcdPropertyValues.Binary.Yes.getShortName())) {
            return Range.NONCHARACTER;
        }
        return Range.RESERVED;
    }

    private static int getLastCodePointInGroup(
            AttributeResolver attributeResolver, int lowCodePoint, int highCodePoint) {
        String blk = attributeResolver.getAttributeValue(UcdProperty.Block, lowCodePoint);
        for (int CodePoint = lowCodePoint; CodePoint <= highCodePoint; CodePoint++) {
            if (!blk.equals(attributeResolver.getAttributeValue(UcdProperty.Block, CodePoint))) {
                return CodePoint - 1;
            }
            if (CodePoint == 0x20 - 1 // put the C0 controls in their own group
                    || CodePoint == 0xa0 - 1 // put the C1 controls in their own group
                    || CodePoint == 0x1160 - 1 // split the jamos into three groups
                    || CodePoint == 0x11a8 - 1 // split the jamos into three groups
                    || CodePoint == 0x1f1e6 - 1 // put the regional indicators in their own group
            ) {
                return CodePoint;
            }
        }
        return highCodePoint;
    }

    private static AttributesImpl getAttributes(
            VersionInfo version,
            AttributeResolver attributeResolver,
            int CodePoint,
            UCDXMLOUTPUTRANGE outputRange) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(
                NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(CodePoint));

        for (UCDPropertyDetail propDetail : UCDPropertyDetail.ucdxmlValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0
                    && (propDetail.getMaxVersion() == null
                            || version.compareTo(propDetail.getMaxVersion()) < 0)) {
                String attrValue = attributeResolver.getAttributeValue(prop, CodePoint);
                boolean isAttributeIncluded =
                        getIsAttributeIncluded(
                                attrValue,
                                attributeResolver.isUnihanAttributeRange(CodePoint),
                                propDetail,
                                prop,
                                outputRange);
                if (isAttributeIncluded) {
                    String propName = prop.getShortName();
                    if (propName.startsWith("cjk")) {
                        propName = prop.getNames().getAllNames().get(1);
                    }
                    attributes.addAttribute(NAMESPACE, propName, propName, "CDATA", attrValue);
                }
            }
        }
        return attributes;
    }

    private static AttributesImpl getGroupAttributes(
            VersionInfo version,
            AttributeResolver attributeResolver,
            int lowCodePoint,
            int highCodePoint,
            UCDXMLOUTPUTRANGE outputRange) {
        AttributesImpl attributes = new AttributesImpl();

        for (UCDPropertyDetail propDetail : UCDPropertyDetail.ucdxmlValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0
                    && (propDetail.getMaxVersion() == null
                            || version.compareTo(propDetail.getMaxVersion()) < 0)) {
                int totalCount = 0;
                Map<String, Integer> counters = new LinkedHashMap<>();

                for (int CodePoint = lowCodePoint; CodePoint <= highCodePoint; CodePoint++) {
                    if (!attributeResolver.isUnassignedCodePoint(CodePoint)) {
                        String attrValue = attributeResolver.getAttributeValue(prop, CodePoint);
                        int currentCount =
                                (counters.get(attrValue) == null) ? 0 : counters.get(attrValue);
                        currentCount++;
                        totalCount++;
                        counters.put(attrValue, currentCount);
                    }
                }
                int max = Integer.MIN_VALUE;
                String bestAttrValue = null;
                for (String attrValue : counters.keySet()) {
                    int thisCount = counters.get(attrValue);
                    if (thisCount > max) {
                        max = thisCount;
                        bestAttrValue = attrValue;
                    }
                }
                switch (prop) {
                    case Decomposition_Mapping:
                    case Simple_Uppercase_Mapping:
                    case Simple_Lowercase_Mapping:
                    case Simple_Titlecase_Mapping:
                    case Uppercase_Mapping:
                    case Lowercase_Mapping:
                    case Titlecase_Mapping:
                    case Simple_Case_Folding:
                    case Case_Folding:
                        if (bestAttrValue != null) {
                            bestAttrValue = "#";
                        }
                }
                if (max > 0.2 * totalCount && max > 1) {
                    boolean isAttributeIncluded =
                            getIsAttributeIncluded(
                                    bestAttrValue,
                                    attributeResolver.isUnihanAttributeRange(lowCodePoint),
                                    propDetail,
                                    prop,
                                    outputRange);
                    if (isAttributeIncluded) {
                        String propName = prop.getShortName();
                        if (propName.startsWith("cjk")) {
                            propName = prop.getNames().getAllNames().get(1);
                        }
                        attributes.addAttribute(
                                NAMESPACE, propName, propName, "CDATA", bestAttrValue);
                    }
                }
            }
        }
        return attributes;
    }

    private static boolean getIsAttributeIncluded(
            String attrValue,
            boolean isUnihanAttributeRange,
            UCDPropertyDetail propDetail,
            UcdProperty prop,
            UCDXMLOUTPUTRANGE outputRange) {
        if (attrValue == null) {
            return false;
        }
        if (isUnihanAttributeRange) {
            if (outputRange == UCDXMLOUTPUTRANGE.UNIHAN) {
                if (prop.equals(UcdProperty.Numeric_Type) && !attrValue.equals("None")) {
                    return true;
                }
                if (prop.equals(UcdProperty.Numeric_Value) && !attrValue.equals("NaN")) {
                    return true;
                }
                return propDetail.isCJKAttribute()
                        && (propDetail.isCJKShowIfEmpty() || !attrValue.isEmpty());
            }
            if (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN && propDetail.isCJKAttribute()) {
                return false;
            }
            if (propDetail.isCJKShowIfEmpty()) {
                return true;
            }
        }
        if (propDetail.isBaseAttribute()) {
            return true;
        }
        return !attrValue.isEmpty();
    }

    private static AttributesImpl getReservedAttributes(
            VersionInfo version, AttributeResolver attributeResolver, ArrayList<Integer> range) {
        AttributesImpl attributes = new AttributesImpl();

        if (range.size() == 1) {
            attributes.addAttribute(
                    NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(range.get(0)));
        } else {
            attributes.addAttribute(
                    NAMESPACE,
                    "first-cp",
                    "first-cp",
                    "CDATA",
                    attributeResolver.getHexString(range.get(0)));
            attributes.addAttribute(
                    NAMESPACE,
                    "last-cp",
                    "last-cp",
                    "CDATA",
                    attributeResolver.getHexString(range.get(range.size() - 1)));
        }
        for (UCDPropertyDetail propDetail : UCDPropertyDetail.baseValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0
                    && (propDetail.getMaxVersion() == null
                            || version.compareTo(propDetail.getMaxVersion()) <= 0)) {
                String attrValue =
                        attributeResolver.getAttributeValue(
                                propDetail.getUcdProperty(), range.get(0));

                attributes.addAttribute(
                        NAMESPACE, prop.getShortName(), prop.getShortName(), "CDATA", attrValue);
            }
        }
        return attributes;
    }
}
