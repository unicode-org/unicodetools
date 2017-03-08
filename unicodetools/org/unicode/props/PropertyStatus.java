package org.unicode.props;

import java.util.EnumSet;

public enum PropertyStatus {
    Obsolete, Stabilized, Deprecated, Contributory, Provisional, Informative, Normative, Immutable;

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
            UcdProperty.Confusable_SL
            );
    private static final EnumSet<UcdProperty> STABLIZED_PROPERTY = EnumSet.of(
            UcdProperty.Hyphen,
            UcdProperty.ISO_Comment);
    
    private static final EnumSet<UcdProperty> OBSOLETE_PROPERTY = EnumSet.of(
            UcdProperty.Unicode_1_Name);
    
    private static final EnumSet<UcdProperty> CONTRIBUTORY_PROPERTY = EnumSet.of(
            UcdProperty.Jamo_Short_Name,
            UcdProperty.Other_Alphabetic,
            UcdProperty.Other_Default_Ignorable_Code_Point,
            UcdProperty.Other_Grapheme_Extend,
            UcdProperty.Other_ID_Continue,
            UcdProperty.Other_ID_Start,
            UcdProperty.Other_Lowercase,
            UcdProperty.Other_Math,
            UcdProperty.Other_Uppercase
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
            UcdProperty.kTotalStrokes
            );
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
            UcdProperty.kIRG_VSource
            );
    private static final EnumSet<UcdProperty> IMMUTABLE_PROPERTY = EnumSet.of(
            UcdProperty.Name,
            UcdProperty.Jamo_Short_Name,
            UcdProperty.Canonical_Combining_Class,
            UcdProperty.Decomposition_Mapping,
            UcdProperty.Pattern_Syntax,
            UcdProperty.Pattern_White_Space
            );
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

}