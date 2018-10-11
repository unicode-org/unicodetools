// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**      Unilib
**      Copyright 2018
**      Ken Whistler, All rights reserved.
*/


/*
 * unisifex.c
 *
 * Wrappers for external functions called by sifter.
 *
 */

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unisift.h"

/*
 * Character property routines.
 */

static UBool isSifterNonSpacingCombining(UChar32 c) {
    // The DUCET Sifter is using these PUA code points in artificial decompositions.
    // They are treated like gc=Mn, so that they get secondary weights.
    return 0xF8F0 <= c && c <= 0xF8FF;
}

// Ken: The following tone marks should be both Alphabetic and Diacritic
// but some of them only have one of those properties, and some have neither
// (in UCD 11).
static UBool isSEAsianToneMark(UChar32 c) {
    return
        c == 0x1063 || c == 0x1064 ||  // Sgaw Karen tones
        (0x1069 <= c && c <= 0x106D) ||  // Western Pwo Karen tones
        (0x1087 <=c && c <= 0x108D) ||  // Shan tones
        c == 0x108F ||  // Rumai Palaung tone
        c == 0x109A || c == 0x109B ||  // Khamti tone
        (0xAA7B <= c && c <= 0xAA7D);  // Pao Karen and Tai Laing tones
}

static UBool isSpecialCJKIdeograph(UInt32 c) {
    return 0xFA0E <= c && c <= 0xFA29 &&
      (c <= 0xFA0F || c == 0xFA11 || c == 0xFA13 || c == 0xFA14 || c == 0xFA1F ||
      c == 0xFA21 || c == 0xFA23 || c == 0xFA24 || 0xFA27 <= c);
}

int unisift_IsCombining ( UInt32 c )
{
    return (U_GET_GC_MASK(c) & U_GC_M_MASK) != 0 || isSifterNonSpacingCombining(c);
}

int unisift_IsAlphabetic ( UInt32 c )
{
    return
        (u_hasBinaryProperty(c, UCHAR_ALPHABETIC) &&
            // HACK: TODO: Deal with the 12 special CJK ideographs
            // ("compatibility" but no decompositions) in sift() before
            // testing Alphabetic.
            !isSpecialCJKIdeograph(c) &&
            // Ken: U+135F ETHIOPIC COMBINING GEMINATION MARK *could* be Alphabetic,
            // but diacritic length marks typically aren't.
            // The related marks U+135D..U+135E are not Alphabetic either.
            c != 0x135F &&
            // Ken: 16B30..16B36 are all non-spacing combining diacritical marks.
            // They should not be Other_Alphabetic, but should be Diacritic.
            !(0x16B30 <= c && c <= 0x16B36)) ||
        // Ken: These two seem to be clear errors in UCD.
        // Both are dependent vowels, and should be Other_Alphabetic.
        // Note that A802 is also missing its Indic_Syllabic_Category value.
        // Should be Dependent_Vowel, like A8FF.
        // Coincidentally, it seems to represent the -ai vowel in Sylheti.
        c == 0xA8FF || c == 0xA802 ||
        // Ken: These 4 positional tones are the moral equivalent of tone letters,
        // which are also encoded for Miao, but we have the problem that
        // the 4 positional tones in the range 16F8F..16F92 are gc=Mn
        // (and functional a little like format controls in rendering),
        // while the tone letters 16F93.. 16F9F (used in two other orthographies) are gc=Lm.
        // All 3 sets are already +Diacritic in UCD, which is correct.
        // However, the regular tone letters are +Alphabetic,
        // while the 4 positional tones are not.
        // 16F8F..16F92 should be added to Other_Alphabetic.
        (0x16F8F <= c && c <= 0x16F92) ||
        // Ken: For collation, LEPCHA SIGN RAN needs to get a fully primary weight,
        // because Lepcha has a full syllabic collation order.
        // And the syllables with RAN have their own order where they rank higher than
        // the final consonants, which Lepcha also has. See L2/05-158.
        // It should be Other_Alphabetic.
        c == 0x1C36 ||
        // Ken: Similar to the Lepcha RAN case.
        // This appears to be some kind of odd mark of a dependent vowel.
        // It occurs only in a single formation for a symbol -- possibly an abbreviation --
        // in Shan, but also occurs more generally in Tai Laing.
        // The DUCET just slots it in with other Myanmar dependent vowels.
        // And it already has InSC=Vowel_Dependent.
        // Should also be Other_Alphabetic.
        c == 0xA9E5 ||
        isSEAsianToneMark(c);
}

int unisift_IsCurrency ( UInt32 c )
{
    return u_charType(c) == U_CURRENCY_SYMBOL;
}

int unisift_IsDecimalDigit ( UInt32 c )
{
    return u_charType(c) == U_DECIMAL_DIGIT_NUMBER;
}

int unisift_IsNumeric ( UInt32 c )
{
    return (U_GET_GC_MASK(c) & U_GC_N_MASK) != 0;
}

int unisift_IsPunctuation ( UInt32 c )
{
    return (U_GET_GC_MASK(c) & U_GC_P_MASK) != 0;
}

int unisift_IsDiacritic ( UInt32 c )
{
    return
        u_hasBinaryProperty(c, UCHAR_DIACRITIC) ||
        // See comment about these in unisift_IsAlphabetic().
        (0x16B30 <= c && c <= 0x16B36) ||
        // Ken: The following tone marks should be Diacritic but are not (in UCD 11).
        (0xA700 <= c && c <= 0xA716) ||  // modifier tone letters for Chinese, gc=Sk
        isSEAsianToneMark(c);
}

int unisift_IsIdeographic ( UInt32 c )
{
    return u_hasBinaryProperty(c, UCHAR_IDEOGRAPHIC);
}

int unisift_IsNonSpacing ( UInt32 c )
{
    return u_charType(c) == U_NON_SPACING_MARK || isSifterNonSpacingCombining(c);
}

int unisift_IsExtender ( UInt32 c )
{
    return u_hasBinaryProperty(c, UCHAR_EXTENDER);
}

int unisift_IsWhiteSpace ( UInt32 c )
{
    return u_isUWhiteSpace(c);
}

int unisift_IsMath ( UInt32 c )
{
    return u_hasBinaryProperty(c, UCHAR_MATH);
}

int unisift_IsMiscSymbolic ( UInt32 c )
{
    return (U_GET_GC_MASK(c) & (U_GC_SK_MASK | U_GC_SO_MASK)) != 0;
}

int unisift_IsIgnorable ( UInt32 c )
{
    return
        ((U_GET_GC_MASK(c) & (U_GC_CC_MASK | U_GC_CF_MASK)) != 0 && !u_isUWhiteSpace(c)) ||
        u_hasBinaryProperty(c, UCHAR_VARIATION_SELECTOR) ||
        c == 0xAD;
}

/*
 * Character casing routines.
 */
int unisift_IsUpper ( UInt32 c )
{
    return u_hasBinaryProperty(c, UCHAR_UPPERCASE);
}

UInt32 unisift_ToLower ( UInt32 c )
{
    return u_tolower(c);
}

UInt32 unisift_ToUpper ( UInt32 c )
{
    return u_toupper(c);
}

int unisift_ToIntValue ( UInt32 c )
{
    double value = u_getNumericValue(c);
    if (value == U_NO_NUMERIC_VALUE) {
        return -1;
    }
    int intValue = (int)value;
    if (intValue == value) {
        return intValue;
    } else {
        return -2;
    }
}

/*
 * String operations.
 */

unistring unisift_unistrcpy ( unistring s1, const unistring s2 )
{
    return u_strcpy(s1, s2);
}

unistring unisift_unistrcat ( unistring s1, const unistring s2 )
{
    return u_strcat(s1, s2);
}

/*
 * Get combining class for a character.
 */
int unisift_GetCombiningClass ( UInt32 c )
{
    return u_getCombiningClass(c);
}
