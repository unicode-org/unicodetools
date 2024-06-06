package org.unicode.xml;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.util.VersionInfo;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.TransformerConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UcdXML {

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
        RESERVED ("reserved"),
        SURROGATE ("surrogate"),
        NONCHARACTER ("noncharacter"),
        CHARACTER ("char"),
        CJKUNIFIEDIDEOGRAPH ("char"),
        NONRANGE ("nonrange");

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
            UOption.create("ucdversion", 'v', UOption.REQUIRES_ARG),
            UOption.create("range", 'r', UOption.REQUIRES_ARG),
            UOption.create("output", 'o', UOption.REQUIRES_ARG),
            UOption.create("outputfolder", 'f', UOption.REQUIRES_ARG)
    };
    private static final int
            HELP = 0,
            UCDVERSION = 1,
            RANGE = 2,
            OUTPUT = 3,
            OUTPUTFOLDER = 4;


    public static void main(String[] args) throws Exception {
        VersionInfo ucdVersion = null;
        UCDXMLOUTPUTRANGE ucdxmloutputrange = null;
        UCDXMLOUTPUTTYPE ucdxmloutputtype = null;
        File destinationFolder = null;

        UOption.parseArgs(args, options);

        if (options[HELP].doesOccur) {
            System.out.println("UcdXML --ucdversion {version number} --outputfolder {destination} " +
                    "--range [ALL|NOUNIHAN|UNIHAN] --output [FLAT|GROUPED]");
            System.exit(0);
        }

        try {
            if (options[UCDVERSION].doesOccur) {
                try {
                    ucdVersion = VersionInfo.getInstance(options[UCDVERSION].value);
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Could not convert " + options[UCDVERSION].value +
                            " to a valid UCD version");
                }
            }
            else {
                throw new IllegalArgumentException("Missing command line option: --ucdversion (or -v)");
            }
            if (options[RANGE].doesOccur) {
                try {
                    ucdxmloutputrange = UCDXMLOUTPUTRANGE.valueOf(options[RANGE].value.toUpperCase(Locale.ROOT));
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Could not convert " + options[RANGE].value +
                            " to one of [ALL|NOUNIHAN|UNIHAN]");
                }
            }
            else {
                throw new IllegalArgumentException("Missing command line option: --range (or -r)");
            }
            if (options[OUTPUT].doesOccur) {
                try {
                    ucdxmloutputtype = UCDXMLOUTPUTTYPE.valueOf(options[OUTPUT].value.toUpperCase(Locale.ROOT));
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Could not convert " + options[OUTPUT].value +
                            " to one of [FLAT|GROUPED]");
                }
            }
            else {
                throw new IllegalArgumentException("Missing command line option: --output (or -o)");
            }
            if (options[OUTPUTFOLDER].doesOccur) {
                try {
                    destinationFolder = new File(options[OUTPUTFOLDER].value + getVersionString(ucdVersion, 3) +
                            "\\xmltest\\");
                    if (!destinationFolder.exists()) {
                        if(!destinationFolder.mkdir()) {
                            throw new IOException();
                        }
                    }
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Could not find or create " + options[OUTPUTFOLDER].value);
                }
            }
            else {
                throw new IllegalArgumentException("Missing command line option: --outputfolder (or -f)");
            }

        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (ucdVersion != null && destinationFolder.exists()) {
            buildUcdXMLFile(ucdVersion, destinationFolder, ucdxmloutputrange, ucdxmloutputtype);
            System.out.println("end");
            System.exit(0);
        }
        else {
            System.err.println("Unexpected error when building UcdXML file.");
            System.exit(1);
        }


    }

    private static void buildUcdXMLFile(VersionInfo ucdVersion, File destinationFolder, UCDXMLOUTPUTRANGE outputRange
            , UCDXMLOUTPUTTYPE outputType) throws IOException, TransformerConfigurationException, SAXException {
        int lowCodepoint = 0x0;
        int highCodepoint = 0x10FFFF;
        // Tangut
        //int lowCodepoint = 0x17000;
        //int highCodepoint = 0x1B2FB;
        //0x10FFFF

        File tempFile = new File(destinationFolder, "temp.xml");
        String outputFilename =
                "ucd." + outputRange.toString().toLowerCase(Locale.ROOT) + "." +
                        outputType.toString().toLowerCase(Locale.ROOT) + ".xml";
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
                writer.addContent("Unicode " + getVersionString(ucdVersion, 3));
                writer.endElement("description");
            }
            buildRepertoire(writer, attributeResolver, ucdVersion, lowCodepoint, highCodepoint, outputRange,
                    outputType);
            if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.BLOCKS);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.NAMEDSEQUENCES);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.NORMALIZATIONCORRECTIONS);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.STANDARDIZEDVARIANTS);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.CJKRADICALS);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.EMOJISOURCES);
                ucdDataResolver.buildSection(UcdSectionDetail.UcdSection.DONOTEMIT);
            }
            writer.endElement("ucd");
        }
        writer.endFile();
        fileOutputStream.close();
        cleanUcdXMLFile(tempFile, destinationFile);
        if(!tempFile.delete()) {
            throw new IOException("Could not delete temporary file " + tempFile);
        }
    }

    private static void cleanUcdXMLFile(File tempFile, File destinationFile) throws IOException {
        //XALAN writes out characters outside the BMP as entities.
        //Use this code to replace the entities with the correct characters.
        //See: https://issues.apache.org/jira/browse/XALANJ-2595

        FileInputStream fileInputStream = new FileInputStream(tempFile);
        FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);

        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher matcher = Pattern.compile("&#(\\d+);").matcher(line);
            line = matcher.replaceAll(matchResult -> new String(Character.toChars(Integer.parseInt(matcher.group(1)))));
            bufferedWriter.append(line);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        fileInputStream.close();
        fileOutputStream.close();
    }

    private static void buildRepertoire(UCDXMLWriter writer, AttributeResolver attributeResolver,
                                        VersionInfo ucdVersion, int lowCodepoint, int highCodepoint,
                                        UCDXMLOUTPUTRANGE outputRange, UCDXMLOUTPUTTYPE outputType) throws SAXException {

        writer.startElement("repertoire");
        {
            for (int codepoint = lowCodepoint; codepoint <= highCodepoint; codepoint++) {
                if (isWritableCodepoint(codepoint, outputRange, attributeResolver)) {
                    if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                        codepoint = buildGroup(writer, attributeResolver, ucdVersion, codepoint, highCodepoint,
                                outputRange, outputType);
                    } else {
                        codepoint = buildChars(writer, attributeResolver, ucdVersion, codepoint, highCodepoint,
                                outputRange, outputType, null);
                    }
                }
            }
            writer.endElement("repertoire");
        }
    }

    private static int buildGroup(UCDXMLWriter writer, AttributeResolver attributeResolver, VersionInfo ucdVersion,
                                  int lowCodepoint, int highCodepoint, UCDXMLOUTPUTRANGE outputRange,
                                  UCDXMLOUTPUTTYPE outputType) throws SAXException {

        int lastCodepointInGroup = getLastCodepointInGroup(attributeResolver, lowCodepoint, highCodepoint);

        AttributesImpl groupAttrs = getGroupAttributes(ucdVersion, attributeResolver, lowCodepoint,
                lastCodepointInGroup, outputRange);

        writer.startElement("group", groupAttrs);
        {
            buildChars(writer, attributeResolver, ucdVersion, lowCodepoint, lastCodepointInGroup, outputRange,
                    outputType, groupAttrs);
            writer.endElement("group");
        }
        return lastCodepointInGroup;
    }

    private static int buildChars(UCDXMLWriter writer, AttributeResolver attributeResolver, VersionInfo ucdVersion,
                                  int lowCodepoint, int highCodepoint, UCDXMLOUTPUTRANGE outputRange,
                                  UCDXMLOUTPUTTYPE outputType, AttributesImpl groupAttrs) throws SAXException {

        ArrayList<Integer> range = new ArrayList<>();
        Range rangeType = Range.NONRANGE;
        for (int codepoint = lowCodepoint; codepoint <= highCodepoint; codepoint++) {
            if (attributeResolver.isUnassignedCodepoint(codepoint) ||
                    (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN && attributeResolver.isUnifiedIdeograph(codepoint))) {
                Range currentRangeType = getRangeType(attributeResolver, codepoint);
                if (!range.isEmpty()) {
                    if (!currentRangeType.equals(rangeType) || attributeResolver.isDifferentRange(codepoint,
                            codepoint - 1)) {
                        if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                            if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                                buildGroupedRange(writer, attributeResolver, ucdVersion, range, rangeType, groupAttrs);
                            } else {
                                buildUngroupedRange(writer, attributeResolver, ucdVersion, range, rangeType);
                            }
                        }
                        range.clear();
                    }
                }
                range.add(codepoint);
                rangeType = currentRangeType;
            } else {
                if (!range.isEmpty()) {
                    if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                        if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                            buildGroupedRange(writer, attributeResolver, ucdVersion, range, rangeType, groupAttrs);
                        } else {
                            buildUngroupedRange(writer, attributeResolver, ucdVersion, range, rangeType);
                        }
                    }
                    range.clear();
                    rangeType = Range.NONRANGE;
                }
                if (isWritableCodepoint(codepoint, outputRange, attributeResolver)) {
                    if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                        buildGroupedChar(writer, attributeResolver, ucdVersion, codepoint, outputRange, groupAttrs);
                    } else {
                        buildUngroupedChar(writer, attributeResolver, ucdVersion, codepoint, outputRange);
                    }
                }
            }
        }
        //Handle any range before the end of the repertoire element.
        if (!range.isEmpty()) {
            if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                if (outputType == UCDXMLOUTPUTTYPE.GROUPED) {
                    buildGroupedRange(writer, attributeResolver, ucdVersion, range, rangeType, groupAttrs);
                } else {
                    buildUngroupedRange(writer, attributeResolver, ucdVersion, range, rangeType);
                }
            }
        }
        return highCodepoint;
    }

    private static void buildUngroupedChar(UCDXMLWriter writer, AttributeResolver attributeResolver,
                                           VersionInfo ucdVersion, int codepoint, UCDXMLOUTPUTRANGE outputRange)
            throws SAXException {

        AttributesImpl charAttributes = getAttributes(ucdVersion, attributeResolver, codepoint, outputRange);
        buildChar(writer, attributeResolver, codepoint, charAttributes);
    }

    private static void buildGroupedChar(UCDXMLWriter writer, AttributeResolver attributeResolver,
                                         VersionInfo ucdVersion, int codepoint, UCDXMLOUTPUTRANGE outputRange,
                                         AttributesImpl groupAttrs) throws SAXException {

        AttributesImpl orgCharAttributes = getAttributes(ucdVersion, attributeResolver, codepoint, outputRange);
        AttributesImpl charAttributes = new AttributesImpl();
        for (int index = 0; index < orgCharAttributes.getLength(); index++) {
            String attributeQName = orgCharAttributes.getQName(index);
            String orgCharAttributesValue = orgCharAttributes.getValue(index);
            String groupAttributeValue = groupAttrs.getValue(attributeQName);
            if (!orgCharAttributesValue.equals(groupAttributeValue)) {
                charAttributes.addAttribute(NAMESPACE, attributeQName, attributeQName, "CDATA", orgCharAttributesValue);
            }
        }
        buildChar(writer, attributeResolver, codepoint, charAttributes);
    }

    private static void buildChar(UCDXMLWriter writer, AttributeResolver attributeResolver, int codepoint,
                                  AttributesImpl charAttributes) throws SAXException {
        writer.startElement("char", charAttributes);
        {
            HashMap<String, String> nameAliases = attributeResolver.getNameAliases(codepoint);
            if (null != nameAliases && !nameAliases.isEmpty()) {
                for (String alias : nameAliases.keySet()) {
                    AttributesImpl nameAliasAt = new AttributesImpl();
                    nameAliasAt.addAttribute(NAMESPACE, "alias", "alias", "CDATA", alias);
                    nameAliasAt.addAttribute(NAMESPACE, "type", "type", "CDATA", nameAliases.get(alias));
                    writer.startElement("name-alias", nameAliasAt);
                    {
                        writer.endElement("name-alias");
                    }
                }
            }
            writer.endElement("char");
        }
    }

    private static void buildGroupedRange(UCDXMLWriter writer, AttributeResolver attributeResolver,
                                          VersionInfo ucdVersion, ArrayList<Integer> range, Range rangeType,
                                          AttributesImpl groupAttrs) throws SAXException {
        AttributesImpl orgRangeAttributes = getReservedAttributes(ucdVersion, attributeResolver, range);
        AttributesImpl rangeAttributes = new AttributesImpl();
        for (int index = 0; index < orgRangeAttributes.getLength(); index++) {
            String attributeQName = orgRangeAttributes.getQName(index);
            String orgCharAttributesValue = orgRangeAttributes.getValue(index);
            String groupAttributeValue = groupAttrs.getValue(attributeQName);
            if (!orgCharAttributesValue.equals(groupAttributeValue)) {
                rangeAttributes.addAttribute(NAMESPACE, attributeQName, attributeQName, "CDATA",
                        orgCharAttributesValue);
            }
        }
        writer.startElement(rangeType.tag, rangeAttributes);
        {
            writer.endElement(rangeType.tag);
        }
    }

    private static void buildUngroupedRange(UCDXMLWriter writer, AttributeResolver attributeResolver,
                                            VersionInfo ucdVersion, ArrayList<Integer> range, Range rangeType)
            throws SAXException {
        AttributesImpl rangeAttributes = getReservedAttributes(ucdVersion, attributeResolver, range);
        writer.startElement(rangeType.tag, rangeAttributes);
        {
            writer.endElement(rangeType.tag);
        }
    }

    private static boolean isWritableCodepoint(int codepoint, UCDXMLOUTPUTRANGE outputRange,
                                               AttributeResolver attributeResolver) {
        return outputRange == UCDXMLOUTPUTRANGE.ALL ||
                (outputRange == UCDXMLOUTPUTRANGE.UNIHAN && attributeResolver.isUnihanAttributeRange(codepoint)) ||
                (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN && !attributeResolver.isUnifiedIdeograph(codepoint));
    }

    private static Range getRangeType(AttributeResolver attributeResolver, int codepoint) {
        String NChar = attributeResolver.getNChar(codepoint);
        UcdPropertyValues.General_Category_Values gc = attributeResolver.getgc(codepoint);

        if (attributeResolver.isUnihanAttributeRange(codepoint)) {
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

    private static int getLastCodepointInGroup(AttributeResolver attributeResolver, int lowCodepoint,
                                               int highCodepoint) {
        String blk = attributeResolver.getAttributeValue(UcdProperty.Block, lowCodepoint);
        for (int codepoint = lowCodepoint; codepoint <= highCodepoint; codepoint++) {
            if (!blk.equals(attributeResolver.getAttributeValue(UcdProperty.Block, codepoint))) {
                return codepoint - 1;
            }
            if (codepoint == 0x20 - 1            // put the C0 controls in their own group
                    || codepoint == 0xa0 - 1    // put the C0 controls in their own group
                    || codepoint == 0x1160 - 1  // split the jamos into three groups
                    || codepoint == 0x11a8 - 1  // split the jamos into three groups
                    || codepoint == 0x1f1e6 - 1 // put the regional indicators in their own group
            ) {
                return codepoint;
            }
        }
        return highCodepoint;
    }

    private static AttributesImpl getAttributes(VersionInfo version, AttributeResolver attributeResolver,
                                                int codepoint, UCDXMLOUTPUTRANGE outputRange) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(codepoint));

        for (UcdPropertyDetail propDetail : UcdPropertyDetail.ucdxmlValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0 &&
                    (propDetail.getMaxVersion() == null || version.compareTo(propDetail.getMaxVersion()) < 0)) {
                String attrValue = attributeResolver.getAttributeValue(prop, codepoint);
                boolean isAttributeIncluded = getIsAttributeIncluded(attrValue,
                        attributeResolver.isUnihanAttributeRange(codepoint), propDetail, prop, outputRange);
                if (isAttributeIncluded) {
                    String propName = prop.getShortName();
                    if (propName.startsWith("cjk")) {
                        propName = propName.substring(2);
                    }
                    attributes.addAttribute(NAMESPACE, propName, propName, "CDATA", attrValue);
                }
            }
        }
        return attributes;
    }

    private static AttributesImpl getGroupAttributes(VersionInfo version, AttributeResolver attributeResolver,
                                                     int lowCodepoint, int highCodepoint,
                                                     UCDXMLOUTPUTRANGE outputRange) {
        AttributesImpl attributes = new AttributesImpl();

        for (UcdPropertyDetail propDetail : UcdPropertyDetail.ucdxmlValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0 &&
                    (propDetail.getMaxVersion() == null || version.compareTo(propDetail.getMaxVersion()) < 0)) {
                int totalCount = 0;
                Map<String, Integer> counters = new LinkedHashMap<>();

                for (int codepoint = lowCodepoint; codepoint <= highCodepoint; codepoint++) {
                    if (!attributeResolver.isUnassignedCodepoint(codepoint)) {
                        String attrValue = attributeResolver.getAttributeValue(prop, codepoint);
                        int currentCount = (counters.get(attrValue) == null) ? 0 : counters.get(attrValue);
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
                    boolean isAttributeIncluded = getIsAttributeIncluded(bestAttrValue,
                            attributeResolver.isUnihanAttributeRange(lowCodepoint), propDetail, prop, outputRange);
                    if (isAttributeIncluded) {
                        String propName = prop.getShortName();
                        if (propName.startsWith("cjk")) {
                            propName = propName.substring(2);
                        }
                        attributes.addAttribute(NAMESPACE, propName, propName, "CDATA", bestAttrValue);
                    }
                }
            }
        }
        return attributes;
    }

    private static boolean getIsAttributeIncluded(String attrValue, boolean isUnihanAttributeRange,
                                                  UcdPropertyDetail propDetail, UcdProperty prop,
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
                return propDetail.isCJKAttribute() && (propDetail.isCJKShowIfEmpty() || !attrValue.isEmpty());
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


    private static AttributesImpl getReservedAttributes(VersionInfo version, AttributeResolver attributeResolver,
                                                        ArrayList<Integer> range) {
        AttributesImpl attributes = new AttributesImpl();

        if (range.size() == 1) {
            attributes.addAttribute(NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(range.get(0)));
        } else {
            attributes.addAttribute(NAMESPACE, "first-cp", "first-cp", "CDATA",
                    attributeResolver.getHexString(range.get(0)));
            attributes.addAttribute(NAMESPACE, "last-cp", "last-cp", "CDATA",
                    attributeResolver.getHexString(range.get(range.size() - 1)));
        }
        for (UcdPropertyDetail propDetail : UcdPropertyDetail.baseValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0 &&
                    (propDetail.getMaxVersion() == null || version.compareTo(propDetail.getMaxVersion()) <= 0)) {
                String attrValue = attributeResolver.getAttributeValue(propDetail.getUcdProperty(), range.get(0));

                attributes.addAttribute(NAMESPACE, prop.getShortName(), prop.getShortName(), "CDATA", attrValue);
            }
        }
        return attributes;
    }

    private static String getVersionString(VersionInfo version, int maxDigits) {
        if (maxDigits >= 1 && maxDigits <= 4) {
            int[] digits = new int[]{version.getMajor(), version.getMinor(), version.getMilli(), version.getMicro()};
            StringBuilder verStr = new StringBuilder(7);
            verStr.append(digits[0]);
            for (int i = 1; i < maxDigits; ++i) {
                verStr.append(".");
                verStr.append(digits[i]);
            }
            return verStr.toString();
        } else {
            throw new IllegalArgumentException("Invalid maxDigits range");
        }
    }
}