package org.unicode.xml;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.util.VersionInfo;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyParsingInfo;
import org.unicode.props.UcdLineParser;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UnicodeProperty;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Used by UCDXML to get string values of attributes for each code point from IndexUnicodeProperties.
 */

public class AttributeResolver {

    private final IndexUnicodeProperties indexUnicodeProperties;
    private final UnicodeMap<UcdPropertyValues.Age_Values> map_age;
    private final UnicodeMap<UcdPropertyValues.Block_Values> map_block;
    private final UnicodeMap<UcdPropertyValues.Decomposition_Type_Values> map_decomposition_type;
    private final UnicodeMap<UcdPropertyValues.General_Category_Values> map_general_category;
    private final UnicodeMap<UcdPropertyValues.Script_Values> map_script;
    private final UnicodeMap<String> map_script_extensions;
    private final HashMap<Integer, LinkedList<NameAlias>> map_NameAlias;

    // If there is a change in any of these properties between two adjacent characters, it will
    // result in a new range.
    private final UCDPropertyDetail[] rangeDefiningPropertyDetails = {
        UCDPropertyDetail.Age_Detail,
        UCDPropertyDetail.Bidi_Class_Detail,
        UCDPropertyDetail.Block_Detail,
        UCDPropertyDetail.Decomposition_Mapping_Detail,
        UCDPropertyDetail.Numeric_Type_Detail,
        UCDPropertyDetail.Numeric_Value_Detail,
        UCDPropertyDetail.Vertical_Orientation_Detail
    };

    public AttributeResolver(IndexUnicodeProperties iup) {
        indexUnicodeProperties = iup;
        map_age = indexUnicodeProperties.loadEnum(UcdProperty.Age);
        map_block = indexUnicodeProperties.loadEnum(UcdProperty.Block);
        map_decomposition_type = indexUnicodeProperties.loadEnum(UcdProperty.Decomposition_Type);
        map_general_category = indexUnicodeProperties.loadEnum(UcdProperty.General_Category);
        map_script = indexUnicodeProperties.loadEnum(UcdProperty.Script);
        map_script_extensions =
                indexUnicodeProperties.getProperty(UcdProperty.Script_Extensions).getUnicodeMap();

        // UCD code is only set up to read a single Alias value from NameAliases.txt
        // Instead, we'll load the Alias and the Type data as part of the constructor. We'll keep in
        // memory as it
        // NameAliases isn't too large.
        map_NameAlias = loadNameAliases();
    }

    protected enum AliasType {
        ABBREVIATION("abbreviation"),
        ALTERNATE("alternate"),
        CONTROL("control"),
        CORRECTION("correction"),
        FIGMENT("figment"),
        NONE("none");

        private final String aliasType;

        AliasType(String aliasType) {
            this.aliasType = aliasType;
        }

        public String toString() {
            return aliasType;
        }
    }

    private static class NameAlias {

        private String alias;
        private final AliasType type;

        private NameAlias(String alias, AliasType type) {
            this.alias = alias;
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public AliasType getType() {
            return type;
        }
    }

    private static class NameAliasComparator implements java.util.Comparator<NameAlias> {

        @Override
        public int compare(NameAlias o1, NameAlias o2) {
            return o1.getAlias().compareTo(o2.getAlias());
        }
    }

    private HashMap<Integer, LinkedList<NameAlias>> loadNameAliases() {
        HashMap<Integer, LinkedList<NameAlias>> nameAliasesByCodePoint = new HashMap<>();
        final PropertyParsingInfo fileInfo =
                PropertyParsingInfo.getPropertyInfo(UcdProperty.Name_Alias);
        String fullFilename = fileInfo.getFullFileName(indexUnicodeProperties.getUcdVersion());
        UcdLineParser parser = new UcdLineParser(FileUtilities.in("", fullFilename));
        NameAliasComparator nameAliasComparator = new NameAliasComparator();

        for (UcdLineParser.UcdLine line : parser) {
            String[] parts = line.getParts();
            int codepoint = Integer.parseInt(parts[0], 16);
            NameAlias nameAlias;
            if (parts.length < 3) {
                nameAlias = new NameAlias(parts[1], AliasType.NONE);
            } else {
                nameAlias =
                        new NameAlias(
                                parts[1], AliasType.valueOf(parts[2].toUpperCase(Locale.ROOT)));
            }

            if (nameAliasesByCodePoint.containsKey(codepoint)) {
                LinkedList<NameAlias> nameAliases =
                        new LinkedList<>(nameAliasesByCodePoint.get(codepoint));
                nameAliases.add(nameAlias);
                nameAliases.sort(nameAliasComparator);
                nameAliasesByCodePoint.replace(codepoint, nameAliases);
            } else {
                nameAliasesByCodePoint.put(codepoint, new LinkedList<>(List.of(nameAlias)));
            }
        }
        return nameAliasesByCodePoint;
    }

    public String getAttributeValue(UcdProperty prop, int codepoint) {
        String resolvedValue = indexUnicodeProperties.getResolvedValue(prop, codepoint);
        switch (prop.getType()) {
            case Numeric:
                switch (prop) {
                    case kOtherNumeric:
                    case kPrimaryNumeric:
                    case kAccountingNumeric:
                        return (resolvedValue.equals("NaN")) ? null : resolvedValue;
                    default:
                        return Optional.ofNullable(resolvedValue).orElse("NaN");
                }
            case String:
                switch (prop) {
                    case Equivalent_Unified_Ideograph:
                        String EqUIdeo = getMappingValue(codepoint, resolvedValue, false, "");
                        return (EqUIdeo.equals("#")) ? null : EqUIdeo;
                    case kCompatibilityVariant:
                        String kCompatibilityVariant =
                                getMappingValue(codepoint, resolvedValue, false, "U+");
                        return (kCompatibilityVariant.equals("#")) ? "" : kCompatibilityVariant;
                    case kSimplifiedVariant:
                    case kTraditionalVariant:
                        String kVariant =
                                getMappingValue(
                                        codepoint,
                                        resolvedValue,
                                        isUnihanAttributeRange(codepoint),
                                        "U+");
                        return (kVariant.equals("#")) ? "" : kVariant;
                    case Bidi_Mirroring_Glyph:
                        // Returning empty string for bmg to maintain compatibility with older
                        // generated files.
                        String bmg = getMappingValue(codepoint, resolvedValue, false, "");
                        return (bmg.equals("#")) ? "" : bmg;
                    default:
                        return getMappingValue(codepoint, resolvedValue, false, "");
                }
            case Miscellaneous:
                switch (prop) {
                    case Jamo_Short_Name:
                        // return map_jamo_short_name.get(codepoint).getShortName();
                        return Optional.ofNullable(resolvedValue).orElse("");
                    case Name:
                        if (resolvedValue != null
                                && resolvedValue.startsWith("CJK UNIFIED IDEOGRAPH-")) {
                            return "CJK UNIFIED IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
                            return "CJK COMPATIBILITY IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("TANGUT IDEOGRAPH-")) {
                            return "TANGUT IDEOGRAPH-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("KHITAN SMALL SCRIPT CHARACTER-")) {
                            return "KHITAN SMALL SCRIPT CHARACTER-#";
                        }
                        if (resolvedValue != null && resolvedValue.startsWith("NUSHU CHARACTER-")) {
                            return "NUSHU CHARACTER-#";
                        }
                        if (resolvedValue != null
                                && resolvedValue.startsWith("EGYPTIAN HIEROGLYPH-")) {
                            return "EGYPTIAN HIEROGLYPH-#";
                        }
                        return Optional.ofNullable(resolvedValue).orElse("");
                    case kDefinition:
                        return resolvedValue;
                    default:
                        if (resolvedValue != null) {
                            return resolvedValue.replaceAll("\\|", " ");
                        }
                        return "";
                }
            case Catalog:
                switch (prop) {
                    case Age:
                        String age = map_age.get(codepoint).getShortName();
                        return (age.equals("NA")) ? "unassigned" : age;
                    case Block:
                        return map_block.get(codepoint).getShortName();
                    case Script:
                        return map_script.get(codepoint).getShortName();
                    case Script_Extensions:
                        StringBuilder extensionBuilder = new StringBuilder();
                        String[] extensions = map_script_extensions.get(codepoint).split("\\|", 0);
                        for (String extension : extensions) {
                            extensionBuilder.append(
                                    UcdPropertyValues.Script_Values.valueOf(extension)
                                            .getShortName());
                            extensionBuilder.append(" ");
                        }
                        return extensionBuilder.toString().trim();
                    default:
                        throw new RuntimeException("Missing Catalog case");
                }
            case Enumerated:
                switch (prop) {
                    case Decomposition_Type:
                        // Returning lower case to maintain compatibility with older generated
                        // files.
                        return map_decomposition_type
                                .get(codepoint)
                                .getShortName()
                                .toLowerCase(Locale.ROOT);
                    default:
                        final UnicodeProperty property = indexUnicodeProperties.getProperty(prop);
                        final List<String> valueAliases = property.getValueAliases(property.getValue(codepoint));
                        return valueAliases.get(0);
                }
            case Binary:
                {
                    switch (resolvedValue) {
                            // Seems overkill to get this from UcdPropertyValues.Binary
                        case "No":
                            return "N";
                        case "Yes":
                            return "Y";
                        default:
                            throw new RuntimeException("Unexpected Binary value");
                    }
                }
            default:
                throw new RuntimeException("Missing PropertyType case");
        }
    }

    public boolean isUnassignedCodePoint(int codepoint) {
        return UcdPropertyValues.General_Category_Values.Unassigned.equals(getgc(codepoint))
                || UcdPropertyValues.General_Category_Values.Private_Use.equals(getgc(codepoint))
                || UcdPropertyValues.General_Category_Values.Surrogate.equals(getgc(codepoint));
    }

    public UcdPropertyValues.General_Category_Values getgc(int codepoint) {
        return map_general_category.get(codepoint);
    }

    public String getNChar(int codepoint) {
        return getAttributeValue(UcdProperty.Noncharacter_Code_Point, codepoint);
    }

    public HashMap<String, String> getNameAliases(int codepoint) {
        HashMap<String, String> nameAliases = new LinkedHashMap<>();
        LinkedList<NameAlias> nameAliasList = map_NameAlias.get(codepoint);
        if (null != nameAliasList && !nameAliasList.isEmpty()) {
            for (NameAlias nameAlias : nameAliasList) {
                nameAliases.put(nameAlias.getAlias(), nameAlias.getType().toString());
            }
            return nameAliases;
        }
        return null;
    }

    private String getMappingValue(
            int codepoint, String resolvedValue, boolean ignoreUnihanRange, String prefix) {
        if (null == resolvedValue) {
            return "#";
        }
        int[] resolvedValueInts = resolvedValue.codePoints().toArray();
        if (resolvedValueInts.length == 1
                && resolvedValueInts[0] == codepoint
                && !ignoreUnihanRange) {
            return "#";
        }
        StringBuilder sb = new StringBuilder();
        for (int i : resolvedValueInts) {
            sb.append(prefix).append(getCPString(i)).append(" ");
        }
        return sb.toString().trim();
    }

    public boolean isDifferentRange(VersionInfo ucdVersion, int codepointA, int codepointB) {
        boolean isDifference = false;
        for (UCDPropertyDetail propDetail : rangeDefiningPropertyDetails) {
            UcdProperty prop = propDetail.getUcdProperty();
            if (ucdVersion.compareTo(propDetail.getMinVersion()) >= 0
                    && (propDetail.getMaxVersion() == null
                            || ucdVersion.compareTo(propDetail.getMaxVersion()) < 0)) {
                isDifference =
                        isDifference
                                || !getAttributeValue(prop, codepointA)
                                        .equals(getAttributeValue(prop, codepointB));
            }
        }
        return isDifference;
    }

    private static String getCPString(int codepoint) {
        return String.format("%4s", Integer.toHexString(codepoint))
                .replace(" ", "0")
                .toUpperCase(Locale.ROOT);
    }

    public String getHexString(int codepoint) {
        return getCPString(codepoint);
    }

    public boolean isUnihanAttributeRange(int codepoint) {
        return getAttributeValue(UcdProperty.Unified_Ideograph, codepoint).equals("Y")
                || !getAttributeValue(UcdProperty.kCompatibilityVariant, codepoint).isEmpty();
    }

    public boolean isUnifiedIdeograph(int codepoint) {
        return getAttributeValue(UcdProperty.Unified_Ideograph, codepoint).equals("Y")
                && getAttributeValue(UcdProperty.Name, codepoint).equals("CJK UNIFIED IDEOGRAPH-#");
    }
}
