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
            // Ken: Exceptions for gc=Lm extenders to match his sifter.
            // TODO: Look to modify this behavior for gc=Lm extenders in the future.
            !(c == 0x1E13C || c == 0x1E13D || c == 0x16B42 || c == 0x16B43));
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
    return u_hasBinaryProperty(c, UCHAR_DIACRITIC);
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
