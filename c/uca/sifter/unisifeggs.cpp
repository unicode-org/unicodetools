// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include <string_view>

#include "ucd.hpp"

namespace {

static unicode::CharacterDatabase const& ucd() {
    static unicode::CharacterDatabase latest("dev");
    return latest;
}

static bool isSifterNonSpacingCombining(char32_t c) {
    // The DUCET Sifter is using these PUA code points in artificial
    // decompositions. They are treated like gc=Mn, so that they get secondary
    // weights.
    return 0xF8F0 <= c && c <= 0xF8FF;
}

static bool isSpecialCJKIdeograph(char32_t c) {
    return 0xFA0E <= c && c <= 0xFA29 &&
           (c <= 0xFA0F || c == 0xFA11 || c == 0xFA13 || c == 0xFA14 ||
            c == 0xFA1F || c == 0xFA21 || c == 0xFA23 || c == 0xFA24 ||
            0xFA27 <= c);
}

}  // namespace

extern "C" int unisift_IsAlphabetic(std::uint32_t c) {
    return ucd().binaryPropertySet("Alphabetic").contains(c) &&
           // HACK: TODO: Deal with the 12 special CJK ideographs
           // ("compatibility" but no decompositions) in sift() before testing
           // Alphabetic.
           !isSpecialCJKIdeograph(c) &&
           // Ken: Exceptions for gc=Lm extenders to match his sifter.
           // TODO: Look to modify this behavior for gc=Lm extenders in the
           // future.
           !(c == 0x1E13C || c == 0x1E13D || c == 0x16B42 || c == 0x16B43);
}

extern "C" int unisift_IsNonSpacing(std::uint32_t c) {
    return ucd().generalCategorySet("Mn").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsCombining(std::uint32_t c) {
    return ucd().generalCategorySet("M").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsExtender(std::uint32_t c) {
    return ucd().binaryPropertySet("Extender").contains(c);
}
extern "C" int unisift_IsCurrency(std::uint32_t c) {
    return ucd().generalCategorySet("Sc").contains(c);
}
extern "C" int unisift_IsPunctuation(std::uint32_t c) {
    return ucd().generalCategorySet("P").contains(c);
}
extern "C" int unisift_IsDiacritic(std::uint32_t c) {
    return ucd().binaryPropertySet("Diacritic").contains(c);
}
extern "C" int unisift_IsNumeric(std::uint32_t c) {
    return ucd().generalCategorySet("N").contains(c);
}
extern "C" int unisift_IsDecimalDigit(std::uint32_t c) {
    return ucd().generalCategorySet("Nd").contains(c);
}
extern "C" int unisift_IsWhiteSpace(std::uint32_t c) {
    return ucd().binaryPropertySet("White_Space").contains(c);
}
extern "C" int unisift_IsMath(std::uint32_t c) {
    return ucd().binaryPropertySet("Math").contains(c);
}
extern "C" int unisift_IsIdeographic(std::uint32_t c) {
    return ucd().binaryPropertySet("Ideographic").contains(c);
}
extern "C" int unisift_IsMiscSymbolic(std::uint32_t c) {
    return ucd().generalCategorySet("Sk").contains(c) ||
           ucd().generalCategorySet("So").contains(c);
}
extern "C" int unisift_IsIgnorable(std::uint32_t c) {
    return ((ucd().generalCategorySet("Cc").contains(c) ||
             ucd().generalCategorySet("Cf").contains(c)) &&
            !ucd().binaryPropertySet("White_Space").contains(c)) ||
           ucd().binaryPropertySet("Variation_Selector").contains(c) ||
           c == U'\u00AD';
}

extern "C" int unisift_IsUpper(std::uint32_t c) {
    return ucd().binaryPropertySet("Uppercase").contains(c);
}

extern "C" std::uint32_t unisift_ToLower(std::uint32_t c) {
    return ucd().simpleLowercaseMapping(c);
}

extern "C" std::uint32_t unisift_ToUpper(std::uint32_t c) {
    return ucd().simpleUppercaseMapping(c);
}

extern "C" int unisift_ToIntValue(std::uint32_t c) {
    // TODO(egg): The conversion to the return type is UB for large numbers.
    return ucd().naturalNumericValue(c).value_or(
            ucd().codePointsWithNumericValue().contains(c) ? -2 : -1);
}

extern "C" int unisift_GetCombiningClass(std::uint32_t c) {
    return ucd().canonicalCombiningClass(c);
}
