package org.unicode.xml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.util.VersionInfo;
import com.thaiopensource.resolver.Input;
import org.unicode.props.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.TransformerConfigurationException;


public class UcdXML {

    private static final String NAMESPACE = "http://www.unicode.org/ns/2003/ucd/1.0";

    private enum OutputType {
        STRICT,
        COMPATIBLE
    }

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

    public static void main(String[] args) throws Exception {

        VersionInfo ucdVersion = VersionInfo.getInstance(15, 1, 0);
        File destinationFolder = new File(
                "C:\\_git\\Unicode\\ucdxml\\data\\" +
                        getVersionString(ucdVersion, 3) + "\\xmltest\\");
        if(!destinationFolder.exists()) {
            destinationFolder.mkdir();
        }
        buildUcdXMLFile(ucdVersion, destinationFolder, UCDXMLOUTPUTRANGE.ALL, UCDXMLOUTPUTTYPE.FLAT);

        System.out.println("end");
    }

    private static void buildUcdXMLFile(
            VersionInfo ucdVersion, File destinationFolder, UCDXMLOUTPUTRANGE outputRange, UCDXMLOUTPUTTYPE outputType)
            throws IOException, TransformerConfigurationException, SAXException {
        int lowCodepoint = 0x0;
        int highCodepoint = 0x10FFFF;
        // Tangut
        //int lowCodepoint = 0x17000;
        //int highCodepoint = 0x1B2FB;
        //0x10FFFF

        File tempFile = new File(destinationFolder, "temp.xml");
        String outputFilename = "ucd." +
                outputRange.toString().toLowerCase() + "." +
                outputType.toString().toLowerCase() + ".xml";
        File destinationFile = new File(destinationFolder, outputFilename);

        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        UCDXMLWriter writer = new UCDXMLWriter(fileOutputStream);

        IndexUnicodeProperties iup = IndexUnicodeProperties.make(ucdVersion);
        AttributeResolver attributeResolver = new AttributeResolver(iup);
        UCDDataResolver ucdDataResolver = new UCDDataResolver(iup, NAMESPACE, writer);

        writer.startFile();
        writer.startElement("ucd");  {
            writer.startElement("description"); {
                writer.addContent("Unicode " + getVersionString(ucdVersion, 3));
                writer.endElement("description");
            }
            buildRepertoire(writer, attributeResolver, ucdVersion, lowCodepoint, highCodepoint, outputRange);
            if(outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
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
        fileOutputStream.close ();
        cleanUcdXMLFile(tempFile, destinationFile);
        tempFile.delete();
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
            Matcher matcher = Pattern.compile("&#([\\d]+);").matcher(line);
            line = matcher.replaceAll(matchResult -> new String(Character.toChars(Integer.parseInt(matcher.group(1)))));
            bufferedWriter.append(line);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        fileInputStream.close();
        fileOutputStream.close();
    }

    private static void buildRepertoire(
            UCDXMLWriter writer, AttributeResolver attributeResolver, VersionInfo ucdVersion,
            int lowCodepoint, int highCodepoint, UCDXMLOUTPUTRANGE outputRange)
            throws SAXException {

        writer.startElement("repertoire"); {


            ArrayList<Integer> range = new ArrayList<>();
            Range rangeType = Range.NONRANGE;

            for (int codepoint = lowCodepoint; codepoint <= highCodepoint; codepoint++) {
                if (attributeResolver.isUnassignedCodepoint(codepoint) ||
                        (outputRange == UCDXMLOUTPUTRANGE.NOUNIHAN && attributeResolver.isUnifiedIdeograph(codepoint))) {
                    Range currentRangeType = getRangeType(attributeResolver, codepoint);
                    if (!range.isEmpty()){
                        if (!currentRangeType.equals(rangeType) || attributeResolver.isDifferentRange(codepoint, codepoint - 1)) {
                            if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                                buildRange(writer, attributeResolver, ucdVersion, range, rangeType);
                            }
                            range.clear();
                        }
                    }
                    range.add(codepoint);
                    rangeType = currentRangeType;
                }
                else {
                    if (!range.isEmpty()) {
                        if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                            buildRange(writer, attributeResolver, ucdVersion, range, rangeType);
                        }
                        range.clear();
                        rangeType = Range.NONRANGE;
                    }
                    buildChar(writer, attributeResolver, ucdVersion, codepoint, outputRange);
                }
            }
            //Handle any range before the end of the repertoire element.
            if (!range.isEmpty()) {
                if (outputRange != UCDXMLOUTPUTRANGE.UNIHAN) {
                    buildRange(writer, attributeResolver, ucdVersion, range, rangeType);
                }
            }
            writer.endElement("repertoire");
        }
    }

    private static void buildChar(
            UCDXMLWriter writer, AttributeResolver attributeResolver, VersionInfo ucdVersion, int codepoint,
            UCDXMLOUTPUTRANGE outputRange)
            throws SAXException {

        if(outputRange != UCDXMLOUTPUTRANGE.UNIHAN || attributeResolver.isUnihanAttributeRange(codepoint)) {
            AttributesImpl at = getAttributes(ucdVersion, attributeResolver, codepoint, outputRange);
            writer.startElement("char", at); {
                HashMap<String, String> nameAliases = attributeResolver.getNameAliases(codepoint);
                if (null != nameAliases && !nameAliases.isEmpty()) {
                    for (String alias : nameAliases.keySet()) {
                        AttributesImpl nameAliasAt = new AttributesImpl();
                        nameAliasAt.addAttribute(
                                NAMESPACE, "alias", "alias", "CDATA", alias);
                        nameAliasAt.addAttribute(
                                NAMESPACE, "type", "type", "CDATA", nameAliases.get(alias));
                        writer.startElement("name-alias", nameAliasAt); {
                            writer.endElement("name-alias");
                        }
                    }
                }
                writer.endElement("char");
            }
        }
    }

    private static void buildRange(UCDXMLWriter writer, AttributeResolver attributeResolver, VersionInfo ucdVersion,
                                   ArrayList<Integer> range, Range rangeType)
            throws SAXException {
        AttributesImpl at = getReservedAttributes(ucdVersion, attributeResolver, range);
        writer.startElement(rangeType.tag, at); {
            writer.endElement(rangeType.tag);
        }
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

    private static AttributesImpl getAttributes(
            VersionInfo version, AttributeResolver attributeResolver, int codepoint, UCDXMLOUTPUTRANGE outputRange) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(
                NAMESPACE, "cp", "cp", "CDATA", attributeResolver.getHexString(codepoint));

        for (UcdPropertyDetail propDetail : UcdPropertyDetail.ucdxmlValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0 &&
                    (propDetail.getMaxVersion() == null || version.compareTo(propDetail.getMaxVersion()) < 0))
            {
                String attrValue = attributeResolver.getAttributeValue(prop, codepoint);
                boolean isAttributeIncluded = getIsAttributeIncluded(
                        attrValue,
                        attributeResolver.isUnihanAttributeRange(codepoint),
                        propDetail, prop,
                        outputRange);

                if(isAttributeIncluded) {
                    String propName = prop.getShortName();
                    if(propName.startsWith("cjk")) {
                        propName = propName.substring(2);
                    }
                    attributes.addAttribute(
                            NAMESPACE,
                            propName,
                            propName,
                            "CDATA",
                            attrValue
                    );
                }
            }
        }
        return attributes;
    }

    private static boolean getIsAttributeIncluded(
            String attrValue,
            boolean isUnihanAttributeRange,
            UcdPropertyDetail propDetail,
            UcdProperty prop,
            UCDXMLOUTPUTRANGE outputRange) {
        if (attrValue == null) { return false; }
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


    private static AttributesImpl getReservedAttributes(
            VersionInfo version, AttributeResolver attributeResolver, ArrayList<Integer> range) {
        AttributesImpl attributes = new AttributesImpl();

        if (range.size() == 1) {
            attributes.addAttribute(
                    NAMESPACE, "cp", "cp", "CDATA",
                    attributeResolver.getHexString(range.get(0)));
        }
        else {
            attributes.addAttribute(
                    NAMESPACE, "first-cp", "first-cp", "CDATA",
                    attributeResolver.getHexString(range.get(0)));
            attributes.addAttribute(
                    NAMESPACE, "last-cp", "last-cp", "CDATA",
                    attributeResolver.getHexString(range.get(range.size() - 1)));
        }
        for (UcdPropertyDetail propDetail : UcdPropertyDetail.baseValues()) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (version.compareTo(propDetail.getMinVersion()) >= 0 &&
                    (propDetail.getMaxVersion() == null || version.compareTo(propDetail.getMaxVersion()) <= 0))
            {
                String attrValue = attributeResolver.getAttributeValue(propDetail.getUcdProperty(), range.get(0));

                attributes.addAttribute(
                        NAMESPACE,
                        prop.getShortName(),
                        prop.getShortName(),
                        "CDATA",
                        attrValue
                );
            }
        }
        return attributes;
    }

    private static String getVersionString(VersionInfo version, int maxDigits) {
        if (maxDigits >= 1 && maxDigits <= 4) {
            int[] digits = new int[]{version.getMajor(), version.getMinor(), version.getMilli(), version.getMicro()};
            StringBuilder verStr = new StringBuilder(7);
            verStr.append(digits[0]);
            for(int i = 1; i < maxDigits; ++i) {
                verStr.append(".");
                verStr.append(digits[i]);
            }
            return verStr.toString();
        } else {
            throw new IllegalArgumentException("Invalid maxDigits range");
        }
    }
}