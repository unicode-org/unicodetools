package org.unicode.props;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.util.CollectionUtilities;

public enum PropertyStatus {
    Obsolete, Stabilized, Deprecated, Internal, Contributory, Provisional, Informative, Normative, Immutable, Unknown;

    enum PropertyOrigin {UCD, Nameslist, Unicode, UTS10, UTS18, UTS39, UTS46, UTS51, ICU, Extra, Unknown}

    public enum PropertyScope {Bidirectional,
        Case,
        CJK,
        Emoji(PropertyOrigin.UTS51),
        General,
        Identifiers,
        IDNA(PropertyOrigin.UTS46),
        Miscellaneous,
        Normalization,
        Numeric,
        Regex(PropertyOrigin.UTS18),
        Security(PropertyOrigin.UTS39),
        Shaping_and_Rendering,
        UCA(PropertyOrigin.UTS10),
        Unknown;

        final PropertyOrigin origin;

        PropertyScope() {
            origin = PropertyOrigin.UCD;
        }
        PropertyScope(PropertyOrigin origin) {
            this.origin = origin;
        }
    }

    private static final EnumSet<UcdProperty> DEPRECATED_PROPERTY = EnumSet.of(
            UcdProperty.Grapheme_Link,
            UcdProperty.Hyphen,
            UcdProperty.ISO_Comment,
            UcdProperty.Expands_On_NFC,
            UcdProperty.Expands_On_NFD,
            UcdProperty.Expands_On_NFKC,
            UcdProperty.Expands_On_NFKD,
            UcdProperty.FC_NFKC_Closure,
            UcdProperty.Confusable_ML,
            UcdProperty.Confusable_SA,
            UcdProperty.Confusable_SL);

    private static final EnumSet<UcdProperty> STABLIZED_PROPERTY = EnumSet.of(
            UcdProperty.Hyphen,
            UcdProperty.ISO_Comment);

    private static final EnumSet<UcdProperty> OBSOLETE_PROPERTY = EnumSet.of(
            UcdProperty.Unicode_1_Name,
            UcdProperty.ISO_Comment,
            UcdProperty.Grapheme_Base);

    private static final EnumSet<UcdProperty> INTERNAL_PROPERTY = EnumSet.of(
            UcdProperty.kCompatibilityVariant,
            UcdProperty.Decomposition_Mapping,
            UcdProperty.Composition_Exclusion,
            UcdProperty.Full_Composition_Exclusion,
            UcdProperty.kIICore, UcdProperty.kIRG_GSource, UcdProperty.kIRG_HSource, UcdProperty.kIRG_JSource,
            UcdProperty.kIRG_KPSource, UcdProperty.kIRG_KSource, UcdProperty.kIRG_TSource, UcdProperty.kIRG_USource,
            UcdProperty.kIRG_VSource, UcdProperty.kIRG_MSource, UcdProperty.kBigFive, UcdProperty.kCCCII,
            UcdProperty.kCNS1986, UcdProperty.kCNS1992, UcdProperty.kEACC, UcdProperty.kGB0, UcdProperty.kGB1,
            UcdProperty.kGB3, UcdProperty.kGB5, UcdProperty.kGB7, UcdProperty.kGB8, UcdProperty.kHKSCS,
            UcdProperty.kIBMJapan, UcdProperty.kJa, UcdProperty.kJis0, UcdProperty.kJis1, UcdProperty.kJIS0213,
            UcdProperty.kKPS0, UcdProperty.kKPS1, UcdProperty.kKSC0, UcdProperty.kKSC1, UcdProperty.kMainlandTelegraph,
            UcdProperty.kPseudoGB1, UcdProperty.kTaiwanTelegraph, UcdProperty.kXerox,
            UcdProperty.Emoji_DCM, UcdProperty.Emoji_KDDI, UcdProperty.Emoji_SB
            );

    private static final EnumSet<UcdProperty> CONTRIBUTORY_PROPERTY = EnumSet.of(
            UcdProperty.Jamo_Short_Name,
            UcdProperty.Other_Alphabetic,
            UcdProperty.Other_Default_Ignorable_Code_Point,
            UcdProperty.Other_Grapheme_Extend,
            UcdProperty.Other_ID_Continue,
            UcdProperty.Other_ID_Start,
            UcdProperty.Other_Lowercase,
            UcdProperty.Other_Math,
            UcdProperty.Other_Uppercase,
            UcdProperty.Grapheme_Extend
            );

    private static final EnumSet<UcdProperty> INFORMATIVE_PROPERTY = EnumSet.of(
            UcdProperty.Alphabetic,
            UcdProperty.Bidi_Mirroring_Glyph,
            UcdProperty.Case_Ignorable,
            UcdProperty.Cased,
            UcdProperty.Changes_When_Casefolded,
            UcdProperty.Changes_When_Casemapped,
            UcdProperty.Changes_When_Lowercased,
            UcdProperty.Changes_When_NFKC_Casefolded,
            UcdProperty.Changes_When_Titlecased,
            UcdProperty.Changes_When_Uppercased,
            UcdProperty.Dash,
            UcdProperty.Diacritic,
            UcdProperty.East_Asian_Width,
            UcdProperty.Extender,
            UcdProperty.Grapheme_Cluster_Break,
            UcdProperty.Grapheme_Link,
            UcdProperty.Hex_Digit,
            UcdProperty.Hyphen,
            UcdProperty.ID_Continue,
            UcdProperty.ID_Start,
            UcdProperty.Ideographic,
            UcdProperty.ISO_Comment,
            UcdProperty.Lowercase,
            UcdProperty.Lowercase_Mapping,
            UcdProperty.Math,
            UcdProperty.NFKC_Casefold,
            UcdProperty.Prepended_Concatenation_Mark,
            UcdProperty.Quotation_Mark,
            UcdProperty.Script,
            UcdProperty.Script_Extensions,
            UcdProperty.Sentence_Break,
            UcdProperty.Sentence_Terminal,
            UcdProperty.Terminal_Punctuation,
            UcdProperty.Titlecase_Mapping,
            UcdProperty.Unicode_1_Name,
            UcdProperty.Uppercase,
            UcdProperty.Uppercase_Mapping,
            UcdProperty.Word_Break,
            UcdProperty.XID_Continue,
            UcdProperty.XID_Start,
            // moved
            UcdProperty.Expands_On_NFC,
            UcdProperty.Expands_On_NFD,
            UcdProperty.Expands_On_NFKC,
            UcdProperty.Expands_On_NFKD,
            UcdProperty.FC_NFKC_Closure,

            UcdProperty.kAccountingNumeric,
            UcdProperty.kMandarin,
            UcdProperty.kOtherNumeric,
            UcdProperty.kPrimaryNumeric,
            UcdProperty.kRSUnicode,
            UcdProperty.kTotalStrokes,

            UcdProperty.Bidi_Paired_Bracket,
            UcdProperty.Bidi_Paired_Bracket_Type,
            UcdProperty.CJK_Radical,
            UcdProperty.Confusable_MA,
            UcdProperty.Confusable_ML,
            UcdProperty.Confusable_SA,
            UcdProperty.Confusable_SL,
            UcdProperty.Emoji,
            UcdProperty.Emoji_Component,
            UcdProperty.Emoji_DCM,
            UcdProperty.RGI_Emoji_Flag_Sequence,
            UcdProperty.Emoji_KDDI,
            UcdProperty.RGI_Emoji_Keycap_Sequence,
            UcdProperty.Emoji_Modifier,
            UcdProperty.Emoji_Modifier_Base,
            UcdProperty.RGI_Emoji_Modifier_Sequence,
            UcdProperty.Basic_Emoji,
            UcdProperty.Emoji_Presentation,
            UcdProperty.Emoji_SB,
            UcdProperty.RGI_Emoji_Tag_Sequence,
            UcdProperty.RGI_Emoji_Zwj_Sequence,
            UcdProperty.Identifier_Status,
            UcdProperty.Identifier_Type,
            UcdProperty.Idn_2008,
            UcdProperty.Idn_Mapping,
            UcdProperty.Idn_Status,
            UcdProperty.Indic_Positional_Category,
            UcdProperty.Indic_Syllabic_Category,
            UcdProperty.Named_Sequences,
            UcdProperty.Named_Sequences_Prov,
            UcdProperty.Regional_Indicator,
            UcdProperty.Standardized_Variant,
            UcdProperty.Vertical_Orientation);

    private static final EnumSet<UcdProperty> NORMATIVE_PROPERTY = EnumSet.of(
            UcdProperty.Age,
            UcdProperty.ASCII_Hex_Digit,
            UcdProperty.Bidi_Class,
            UcdProperty.Bidi_Control,
            UcdProperty.Bidi_Mirrored,
            UcdProperty.Block,
            UcdProperty.Canonical_Combining_Class,
            UcdProperty.Case_Folding,
            UcdProperty.Composition_Exclusion,
            UcdProperty.Decomposition_Mapping,
            UcdProperty.Decomposition_Type,
            UcdProperty.Default_Ignorable_Code_Point,
            UcdProperty.Deprecated,
            UcdProperty.Full_Composition_Exclusion,
            UcdProperty.General_Category,
            UcdProperty.Grapheme_Base,
            UcdProperty.Grapheme_Extend,
            UcdProperty.Hangul_Syllable_Type,
            UcdProperty.IDS_Binary_Operator,
            UcdProperty.IDS_Trinary_Operator,
            UcdProperty.Join_Control,
            UcdProperty.Joining_Group,
            UcdProperty.Joining_Type,
            UcdProperty.Line_Break,
            UcdProperty.Logical_Order_Exception,
            UcdProperty.Name,
            UcdProperty.Name_Alias,
            UcdProperty.NFC_Quick_Check,
            UcdProperty.NFD_Quick_Check,
            UcdProperty.NFKC_Quick_Check,
            UcdProperty.NFKD_Quick_Check,
            UcdProperty.Noncharacter_Code_Point,
            UcdProperty.Numeric_Type,
            UcdProperty.Numeric_Value,
            UcdProperty.Pattern_Syntax,
            UcdProperty.Pattern_White_Space,
            UcdProperty.Radical,
            UcdProperty.Simple_Case_Folding,
            UcdProperty.Simple_Lowercase_Mapping,
            UcdProperty.Simple_Titlecase_Mapping,
            UcdProperty.Simple_Uppercase_Mapping,
            UcdProperty.Soft_Dotted,
            UcdProperty.Unified_Ideograph,
            UcdProperty.Variation_Selector,
            UcdProperty.White_Space,
            // Unihan
            UcdProperty.kCompatibilityVariant,
            UcdProperty.kIICore,
            UcdProperty.kIRG_GSource,
            UcdProperty.kIRG_HSource,
            UcdProperty.kIRG_JSource,
            UcdProperty.kIRG_KPSource,
            UcdProperty.kIRG_KSource,
            UcdProperty.kIRG_MSource,
            UcdProperty.kIRG_TSource,
            UcdProperty.kIRG_USource,
            UcdProperty.kIRG_VSource);
    private static final EnumSet<UcdProperty> IMMUTABLE_PROPERTY = EnumSet.of(
            UcdProperty.Name,
            UcdProperty.Jamo_Short_Name,
            UcdProperty.Canonical_Combining_Class,
            UcdProperty.Decomposition_Mapping,
            UcdProperty.Pattern_Syntax,
            UcdProperty.Pattern_White_Space);
    public static final EnumSet<UcdProperty> PROVISIONAL_PROPERTY;

    static {
        final EnumSet<UcdProperty> temp2 = EnumSet.allOf(UcdProperty.class);
        temp2.removeAll(PropertyStatus.NORMATIVE_PROPERTY);
        temp2.removeAll(PropertyStatus.INFORMATIVE_PROPERTY);
        temp2.removeAll(PropertyStatus.CONTRIBUTORY_PROPERTY);
        PROVISIONAL_PROPERTY = temp2;
    }

    public static PropertyStatus getPropertyStatus(UcdProperty prop) {
        if (OBSOLETE_PROPERTY.contains(prop)) {
            return Obsolete;
        } else if (STABLIZED_PROPERTY.contains(prop)) {
            return Stabilized;
        } else if (INTERNAL_PROPERTY.contains(prop)) {
            return Stabilized;
        } else if (DEPRECATED_PROPERTY.contains(prop)) {
            return Deprecated;
        } else if (CONTRIBUTORY_PROPERTY.contains(prop)) {
            return Contributory;
        } else if (INFORMATIVE_PROPERTY.contains(prop)) {
            return Informative;
        } else if (IMMUTABLE_PROPERTY.contains(prop)) {
            return Immutable;
        } else if (NORMATIVE_PROPERTY.contains(prop)) {
            return Normative;
        }
        return Provisional;
    }

    public static EnumSet<PropertyStatus> getPropertyStatusSet(UcdProperty prop) {
        final EnumSet<PropertyStatus> result = EnumSet.noneOf(PropertyStatus.class);
        if (OBSOLETE_PROPERTY.contains(prop)) {
            result.add(Obsolete);
        }
        if (STABLIZED_PROPERTY.contains(prop)) {
            result.add(Stabilized);
        }
        if (INTERNAL_PROPERTY.contains(prop)) {
            result.add(Internal);
        }
        if (DEPRECATED_PROPERTY.contains(prop)) {
            result.add(Deprecated);
        }
        if (CONTRIBUTORY_PROPERTY.contains(prop)) {
            result.add(Contributory);
        }
        if (INFORMATIVE_PROPERTY.contains(prop)) {
            result.add(Informative);
        }
        if (IMMUTABLE_PROPERTY.contains(prop)) {
            result.add(Immutable);
        }
        if (NORMATIVE_PROPERTY.contains(prop)) {
            result.add(Normative);
        }
        // Normative or Informative or Contributory or Provisional.
        if (!(result.contains(Normative)
                || result.contains(Informative)
                || result.contains(Contributory))) {
            result.add(Provisional);
        }
        return result;
    }

    public static PropertyScope getScope(String propName) {
        return CldrUtility.ifNull(SCOPE.get(propName), PropertyScope.Unknown);
    }

    public static PropertyOrigin getOrigin(String propName) {
        return CldrUtility.ifNull(ORIGIN.get(propName), PropertyOrigin.Unknown);
    }

    public static PropertyType getDatatype(String propName) {
        return CldrUtility.ifNull(DATATYPE.get(propName), PropertyType.Unknown);
    }

    /*
     * Unihan_DictionaryIndices.txt Unihan_DictionaryLikeData.txt
     * Unihan_IRGSources.txt Unihan_NumericValues.txt Unihan_OtherMappings.txt
     * Unihan_RadicalStrokeCounts.txt Unihan_Readings.txt Unihan_Variants.txt
     */
    static Map<String,PropertyType> DATATYPE = new TreeMap<>();
    static Map<String,PropertyOrigin> ORIGIN = new HashMap<>();
    static Map<String,PropertyScope> SCOPE = new HashMap<>();
    static void process(String propName, String _origin, String _scope, String _datatype) {
        try {
            PropertyScope scope = PropertyScope.valueOf(_scope);
            if (SCOPE.containsKey(propName)) {
                throw new IllegalArgumentException("dup scope for " + propName);
            }
            SCOPE.put(propName, scope);

            PropertyOrigin origin;
            try {
                origin = PropertyOrigin.valueOf(_origin);
            } catch (Exception e) {
                origin = scope.origin;
            }
            if (ORIGIN.containsKey(propName)) {
                throw new IllegalArgumentException("dup origin for " + propName);
            }
            ORIGIN.put(propName, origin);

            if (DATATYPE.containsKey(propName)) {
                throw new IllegalArgumentException("dup datatype for " + propName);
            }
            DATATYPE.put(propName, PropertyType.valueOf(_datatype));

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    static {
        process("Case_Sensitive","ICU","Case","Binary");
        process("NFKD_Inert","ICU","Normalization","Binary");
        process("NFKC_Inert","ICU","Normalization","Binary");
        process("NFD_Inert","ICU","Normalization","Binary");
        process("NFC_Inert","ICU","Normalization","Binary");
        process("Trail_Canonical_Combining_Class","ICU","Normalization","Enumerated");
        process("Lead_Canonical_Combining_Class","ICU","Normalization","Enumerated");
        process("isNFM","ICU","Normalization","Binary");
        process("toNFM","ICU","Normalization","String");
        process("Segment_Starter","ICU","Shaping_and_Rendering","Binary");
        process("Subhead","Nameslist","General","String");
        process("Bidi_Control","UCD","Bidirectional","Binary");
        process("Bidi_Mirrored","UCD","Bidirectional","Binary");
        process("Bidi_Class","UCD","Bidirectional","Enumerated");
        process("Bidi_Mirroring_Glyph","UCD","Bidirectional","String");
        process("Bidi_Paired_Bracket","UCD","Bidirectional","String");
        process("Bidi_Paired_Bracket_Type","UCD","Bidirectional","Enumerated");
        process("Case_Ignorable","UCD","Case","Binary");
        process("Cased","UCD","Case","Binary");
        process("Changes_When_Casefolded","UCD","Case","Binary");
        process("Changes_When_Casemapped","UCD","Case","Binary");
        process("Changes_When_Lowercased","UCD","Case","Binary");
        process("Changes_When_Titlecased","UCD","Case","Binary");
        process("Changes_When_Uppercased","UCD","Case","Binary");
        process("Lowercase","UCD","Case","Binary");
        process("Soft_Dotted","UCD","Case","Binary");
        process("Uppercase","UCD","Case","Binary");
        process("Case_Folding","UCD","Case","String");
        process("Lowercase_Mapping","UCD","Case","String");
        process("Simple_Case_Folding","UCD","Case","String");
        process("Simple_Lowercase_Mapping","UCD","Case","String");
        process("Simple_Titlecase_Mapping","UCD","Case","String");
        process("Simple_Uppercase_Mapping","UCD","Case","String");
        process("Titlecase_Mapping","UCD","Case","String");
        process("Uppercase_Mapping","UCD","Case","String");
        process("Ideographic","UCD","CJK","Binary");
        process("IDS_Binary_Operator","UCD","CJK","Binary");
        process("IDS_Trinary_Operator","UCD","CJK","Binary");
        process("Radical","UCD","CJK","Binary");
        process("Unified_Ideograph","UCD","CJK","Binary");
        process("CJK_Radical","UCD","CJK","String");
        process("kSimplifiedVariant","UCD","CJK","String");
        process("kTraditionalVariant","UCD","CJK","String");
        process("Regional_Indicator","UCD","Emoji","Enumerated");
        process("Alphabetic","UCD","General","Binary");
        process("Default_Ignorable_Code_Point","UCD","General","Binary");
        process("Deprecated","UCD","General","Binary");
        process("Logical_Order_Exception","UCD","General","Binary");
        process("Noncharacter_Code_Point","UCD","General","Binary");
        process("Variation_Selector","UCD","General","Binary");
        process("White_Space","UCD","General","Binary");
        process("Age","UCD","General","Catalog");
        process("Block","UCD","General","Catalog");
        process("Script","UCD","General","Catalog");
        process("General_Category","UCD","General","Enumerated");
        process("Hangul_Syllable_Type","UCD","General","Enumerated");
        process("Name","UCD","General","String");
        process("Script_Extensions","UCD","General","String");
        process("Name_Alias","UCD","General","Enumerated");
        process("Named_Sequences_Prov","UCD","General","Enumerated");
        process("Named_Sequences","UCD","General","Enumerated");
        process("ID_Continue","UCD","Identifiers","Binary");
        process("ID_Start","UCD","Identifiers","Binary");
        process("Pattern_Syntax","UCD","Identifiers","Binary");
        process("Pattern_White_Space","UCD","Identifiers","Binary");
        process("XID_Continue","UCD","Identifiers","Binary");
        process("XID_Start","UCD","Identifiers","Binary");
        process("Idn_Mapping","UCD","IDNA","Enumerated");
        process("Idn_Status","UCD","IDNA","Enumerated");
        process("Dash","UCD","Miscellaneous","Binary");
        process("Diacritic","UCD","Miscellaneous","Binary");
        process("Extender","UCD","Miscellaneous","Binary");
        process("Grapheme_Base","UCD","Miscellaneous","Binary");
        process("Grapheme_Extend","UCD","Miscellaneous","Binary");
        process("Grapheme_Link","UCD","Miscellaneous","Binary");
        process("Hyphen","UCD","Miscellaneous","Binary");
        process("Math","UCD","Miscellaneous","Binary");
        process("Quotation_Mark","UCD","Miscellaneous","Binary");
        process("Sentence_Terminal","UCD","Miscellaneous","Binary");
        process("Terminal_Punctuation","UCD","Miscellaneous","Binary");
        process("ISO_Comment","UCD","Miscellaneous","Miscellaneous");
        process("Unicode_1_Name","UCD","Miscellaneous","Miscellaneous");
        process("Indic_Positional_Category","UCD","Miscellaneous","Enumerated");
        process("Indic_Syllabic_Category","UCD","Miscellaneous","Enumerated");
        process("Changes_When_NFKC_Casefolded","UCD","Normalization","Binary");
        process("Full_Composition_Exclusion","UCD","Normalization","Binary");
        process("Canonical_Combining_Class","UCD","Normalization","Enumerated");
        process("Decomposition_Type","UCD","Normalization","Enumerated");
        process("NFC_Quick_Check","UCD","Normalization","Enumerated");
        process("NFD_Quick_Check","UCD","Normalization","Enumerated");
        process("NFKC_Quick_Check","UCD","Normalization","Enumerated");
        process("NFKD_Quick_Check","UCD","Normalization","Enumerated");
        process("NFKC_Casefold","UCD","Normalization","String");
        process("ASCII_Hex_Digit","UCD","Numeric","Binary");
        process("Hex_Digit","UCD","Numeric","Binary");
        process("Numeric_Type","UCD","Numeric","Enumerated");
        process("Numeric_Value","UCD","Numeric","Numeric");
        process("kAccountingNumeric","UCD","Numeric","Enumerated");
        process("kOtherNumeric","UCD","Numeric","Enumerated");
        process("kPrimaryNumeric","UCD","Numeric","Enumerated");
        process("Join_Control","UCD","Shaping_and_Rendering","Binary");
        process("East_Asian_Width","UCD","Shaping_and_Rendering","Enumerated");
        process("Grapheme_Cluster_Break","UCD","Shaping_and_Rendering","Enumerated");
        process("Joining_Group","UCD","Shaping_and_Rendering","Enumerated");
        process("Joining_Type","UCD","Shaping_and_Rendering","Enumerated");
        process("Line_Break","UCD","Shaping_and_Rendering","Enumerated");
        process("Sentence_Break","UCD","Shaping_and_Rendering","Enumerated");
        process("Word_Break","UCD","Shaping_and_Rendering","Enumerated");
        process("Prepended_Concatenation_Mark","UCD","Shaping_and_Rendering","Enumerated");
        process("Standardized_Variant","UCD","Shaping_and_Rendering","Enumerated");
        process("Vertical_Orientation","UCD","Shaping_and_Rendering","Enumerated");
        process("isUppercase","Unicode","Case","Binary");
        process("isTitlecase","Unicode","Case","Binary");
        process("isLowercase","Unicode","Case","Binary");
        process("isCasefolded","Unicode","Case","Binary");
        process("isCased","Unicode","Case","Binary");
        process("toUppercase","Unicode","Case","String");
        process("toTitlecase","Unicode","Case","String");
        process("toLowercase","Unicode","Case","String");
        process("toCasefold","Unicode","Case","String");
        process("isNFKD","Unicode","Normalization","Binary");
        process("isNFKC","Unicode","Normalization","Binary");
        process("isNFD","Unicode","Normalization","Binary");
        process("isNFC","Unicode","Normalization","Binary");
        process("toNFKD","Unicode","Normalization","String");
        process("toNFKC","Unicode","Normalization","String");
        process("toNFD","Unicode","Normalization","String");
        process("toNFC","Unicode","Normalization","String");
        process("Emoji","UTS","Emoji","Binary");
        process("Emoji_Modifier","UTS","Emoji","Binary");
        process("Emoji_Modifier_Base","UTS","Emoji","Binary");
        process("Emoji_Presentation","UTS","Emoji","Binary");
        process("Emoji_All","UTS","Emoji","Binary");
        process("Emoji_Component","UTS","Emoji","Binary");
        process("Extended_Pictographic","UTS","Emoji","Binary");
        process("Basic_Emoji","UTS","Emoji","Binary");
        process("RGI_Emoji_Flag_Sequence","UTS","Emoji","Binary");
        process("RGI_Emoji_Keycap_Sequence","UTS","Emoji","Binary");
        process("RGI_Emoji_Modifier_Sequence","UTS","Emoji","Binary");
        process("RGI_Emoji_Zwj_Sequence","UTS","Emoji","Binary");
        process("RGI_Emoji_Tag_Sequence","UTS","Emoji","Binary");
        process("Idn_2008","UTS","IDNA","Enumerated");
        process("uts46","UTS","IDNA","Enumerated");
        process("idna2008c","UTS","IDNA","Enumerated");
        process("idna2003","UTS","IDNA","Enumerated");
        process("toUts46t","UTS","IDNA","String");
        process("toUts46n","UTS","IDNA","String");
        process("toIdna2003","UTS","IDNA","String");
        process("idna2008","UTS","IDNA","Binary");
        process("xdigit","UTS","Regex","Binary");
        process("print","UTS","Regex","Binary");
        process("graph","UTS","Regex","Binary");
        process("blank","UTS","Regex","Binary");
        process("alnum","UTS","Regex","Binary");
        process("ANY","UTS","Regex","Binary");
        process("ASCII","UTS","Regex","Binary");
        process("bmp","UTS","Regex","Binary");
        process("Identifier_Status","UTS","Security","Enumerated");
        process("Identifier_Type","UTS","Security","Enumerated");
        process("confusable","UTS","Security","Enumerated");
        process("uca","UTS","UCA","Binary");
        process("uca2","UTS","UCA","Binary");
        process("uca2.5","UTS","UCA","Binary");
        process("uca3","UTS","UCA","Binary");
        process("HanType","Extra","CJK","Enumerated");
    }
    public static void main(String[] args) {
        Set<String> props = new TreeSet<>();
        props.addAll(DATATYPE.keySet());
        for (UcdProperty value : UcdProperty.values()) {
            props.add(value.toString());
        }

        for (String propName : props) {
            PropertyType dataType = getDatatype(propName);
            PropertyOrigin origin = getOrigin(propName);
            PropertyScope scope = getScope(propName);
            Set<PropertyStatus> status = Collections.singleton(PropertyStatus.Informative);
            ValueCardinality cardinality = ValueCardinality.Singleton;
            String defaultValue = null;
            try {
                UcdProperty prop = UcdProperty.valueOf(propName);
                dataType = prop.getType();
                status = PropertyStatus.getPropertyStatusSet(prop);
                cardinality = prop.getCardinality();
                defaultValue = IndexUnicodeProperties.getDefaultValue(prop);
            } catch (Exception e) {}
            
            System.out.println(propName 
                    + "\tType:\t" + dataType 
                    + "\tScope:\t" + scope 
                    + "\tOrigin:\t" + origin
                    + "\tStatus:\t"+ CollectionUtilities.join(status, ", ")
                    + "\tCard:\t" + cardinality
                    + "\tDefVal:\t" + defaultValue
                    );
        }
    }
}