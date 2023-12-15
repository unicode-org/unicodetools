// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include <algorithm>
#include <array>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <map>
#include <ranges>
#include <string>
#include <string_view>
#include <vector>

namespace {

class CheckFailure {
   public:
    ~CheckFailure() {
        std::terminate();
    }
    void operator&(std::ostream&) {}
};

#define CHECK(condition) \
    (condition)          \
            ? (void)0    \
            : CheckFailure() & std::cerr << ("Check failed: " #condition "\n")

class CodePointRange {
   public:
    struct iterator {
        constexpr char32_t operator*() const {
            return value;
        }

        constexpr void operator++() {
            ++value;
        }

        constexpr std::strong_ordering operator<=>(
                const iterator& other) const = default;

        char32_t value;
    };

    static constexpr CodePointRange Inclusive(char32_t first, char32_t last) {
        return CodePointRange(first, last + 1);
    }

    constexpr bool empty() const {
        return first_ >= pastTheEnd_;
    }

    constexpr bool contains(char32_t c) const {
        return c >= first_ && c < pastTheEnd_;
    }

    constexpr CodePointRange intersection(const CodePointRange& other) const {
        return CodePointRange(std::max(first_, other.first_),
                              std::min(pastTheEnd_, other.pastTheEnd_));
    }

    constexpr char32_t front() const {
        CHECK(!empty());
        return first_;
    }

    constexpr char32_t back() const {
        CHECK(!empty());
        return pastTheEnd_ - 1;
    }

    iterator begin() const {
        return {first_};
    }

    iterator end() const {
        return {pastTheEnd_};
    }

   private:
    constexpr CodePointRange(char32_t first, char32_t past_the_end)
        : first_(first), pastTheEnd_(past_the_end) {}

    char32_t first_;
    char32_t pastTheEnd_;
};

// TODO(egg): Make this behave like a container.
class CodePointSet {
   public:
    // Empty set.
    CodePointSet() = default;

    bool contains(char32_t c) const {
        const auto it = std::partition_point(
                ranges_.begin(),
                ranges_.end(),
                [c](const CodePointRange& range) { return range.back() < c; });
        return it != ranges_.end() && it->contains(c);
    }

    void addAll(CodePointRange range) {
        // All earlier ranges end before the new one and are not adjacent.
        const auto firstModifiedRange =
                std::partition_point(ranges_.begin(),
                                     ranges_.end(),
                                     [&range](const CodePointRange& r) {
                                         return r.back() + 1 < range.front();
                                     });
        // First range that starts after the new one and is not adjacent.
        const auto pastModifiedRanges =
                std::partition_point(ranges_.begin(),
                                     ranges_.end(),
                                     [&range](const CodePointRange& r) {
                                         return r.front() <= range.back() + 1;
                                     });
        char32_t insertedRangeFirst = range.front();
        if (firstModifiedRange != ranges_.end()) {
            insertedRangeFirst =
                    std::min(firstModifiedRange->front(), insertedRangeFirst);
        }
        char32_t insertedRangeLast = range.back();
        if (pastModifiedRanges != ranges_.begin()) {
            const auto lastModifiedRange = pastModifiedRanges - 1;
            insertedRangeLast =
                    std::max(lastModifiedRange->back(), insertedRangeLast);
        }
        const auto it = ranges_.erase(firstModifiedRange, pastModifiedRanges);
        ranges_.insert(it,
                       CodePointRange::Inclusive(insertedRangeFirst,
                                                 insertedRangeLast));
    }

    CodePointSet addAll(const CodePointSet& other) {
        for (const auto& range : other.ranges_) {
            addAll(range);
        }
    }

    const std::vector<CodePointRange>& ranges() const {
        return ranges_;
    }

   private:
    // Non-overlapping, non-adjacent ranges, in ascending order.
    std::vector<CodePointRange> ranges_;
};

constexpr std::string_view stripComment(std::string_view const line) {
    return line.substr(0, line.find_first_of('#'));
}

template<std::size_t n>
constexpr std::array<std::string_view, n> fields(std::string_view const line) {
    std::array<std::string_view, n> result;
    auto split = std::views::split(line, ';');
    auto it = split.begin();
    for (auto& field : result) {
        CHECK(it != split.end()) << line;
        std::string_view field_with_spaces(*it++);
        const auto field_first =
                std::min(field_with_spaces.find_first_not_of(" "),
                         field_with_spaces.size());
        const auto field_last = field_with_spaces.find_last_not_of(" ");
        const auto field_size = field_last - field_first + 1;
        field = field_with_spaces.substr(field_first, field_size);
    }
    return result;
}

constexpr char32_t parseHexCodePoint(std::string_view hex) {
    std::uint32_t c;
    std::from_chars(hex.data(), hex.data() + hex.size(), c, 16);
    return static_cast<char32_t>(c);
}

constexpr CodePointRange parseHexCodePointRange(std::string_view hex) {
    const auto first_dot = hex.find("..");
    if (first_dot == std::string_view::npos) {
        const char32_t code_point = parseHexCodePoint(hex);
        return CodePointRange::Inclusive(code_point, code_point);
    }
    return CodePointRange::Inclusive(
            parseHexCodePoint(hex.substr(0, first_dot)),
            parseHexCodePoint(hex.substr(first_dot + 2)));
}

class UCD {
   public:
    explicit UCD() {
        const auto ucdDirectory = std::filesystem::current_path()
                                          .parent_path()
                                          .parent_path()
                                          .parent_path() /
                                  "unicodetools" / "data" / "ucd" / "dev";
        {
            std::ifstream unicodeData(ucdDirectory / "UnicodeData.txt");
            CHECK(unicodeData.good())
                    << "Run this tool from the c/uca/sifter directory";
            std::cout << "Reading UnicodeData.txt...\n";
            for (std::string line; std::getline(unicodeData, line);) {
                // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
                const auto [cp,
                            name,
                            gc,
                            ccc,
                            bc,
                            dtDm,
                            nvDecimal,
                            nvDigit,
                            nvNumeric,
                            bm,
                            unicode1Name,
                            isoComment,
                            suc,
                            slc,
                            stc] = fields<15>(line);
                const char32_t codePoint = parseHexCodePoint(cp);
                // TODO(egg): Handle ranges.
                const auto range =
                        CodePointRange::Inclusive(codePoint, codePoint);
                coarseGeneralCategory_[gc.front()].addAll(range);
                generalCategory_[std::string(gc)].addAll(range);
                canonicalCombiningClass_.emplace(codePoint,
                                                 std::stoi(std::string(ccc)));
                if (!suc.empty()) {
                    simpleUppercaseMapping_.emplace(codePoint,
                                                    parseHexCodePoint(suc));
                }
                if (!slc.empty()) {
                    simpleLowercaseMapping_.emplace(codePoint,
                                                    parseHexCodePoint(slc));
                }
            }
        }
        readMultiPropertyFile(ucdDirectory / "PropList.txt");
        readMultiPropertyFile(ucdDirectory / "DerivedCoreProperties.txt");
        {
            std::ifstream derivedNumericValue(ucdDirectory / "extracted" /
                                              "DerivedNumericValues.txt");
            CHECK(derivedNumericValue.good());
            std::cout << "Reading DerivedNumericValues.txt...\n";
            for (std::string line; std::getline(derivedNumericValue, line);) {
                // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
                const auto record = stripComment(line);
                if (record.empty()) {
                    continue;
                }
                const auto [cp, decimalNV, _, rationalNV] = fields<4>(record);
                const auto range = parseHexCodePointRange(cp);
                codePointsWithNumericValue_.addAll(range);
                if (rationalNV.find_first_not_of("0123456789") ==
                    std::string_view::npos) {
                    for (const char32_t codePoint : range) {
                        naturalNumericValue_.emplace(
                                codePoint, std::stoll(std::string(rationalNV)));
                    }
                }
            }
        }
    }

    const CodePointSet& generalCategorySet(std::string_view const gc) {
        if (gc.size() == 1) {
            return coarseGeneralCategory_.at(gc.front());
        }
        auto it = generalCategory_.find(gc);
        CHECK(it != generalCategory_.end()) << gc;
        return it->second;
    }

    const CodePointSet& binaryPropertySet(
            std::string_view const binary_property_name) {
        auto it = binaryProperties_.find(binary_property_name);
        CHECK(it != binaryProperties_.end()) << binary_property_name;
        return it->second;
    }

    char32_t simpleLowercaseMapping(const char32_t c) const {
        const auto it = simpleLowercaseMapping_.find(c);
        if (it == simpleLowercaseMapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    char32_t simpleUppercaseMapping(const char32_t c) const {
        const auto it = simpleUppercaseMapping_.find(c);
        if (it == simpleUppercaseMapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    std::uint8_t canonicalCombiningClass(const char32_t c) const {
        const auto it = canonicalCombiningClass_.find(c);
        if (it == canonicalCombiningClass_.end()) {
            return 0;
        } else {
            return it->second;
        }
    }

    std::optional<std::uint64_t> naturalNumericValue(const char32_t c) const {
        const auto it = naturalNumericValue_.find(c);
        if (it == naturalNumericValue_.end()) {
            return std::nullopt;
        } else {
            return it->second;
        }
    }

    const CodePointSet& codePointsWithNumericValue() const {
        return codePointsWithNumericValue_;
    }

   private:
    void readMultiPropertyFile(const std::filesystem::path& file) {
        std::ifstream stream(file);
        CHECK(stream.good());
        std::cout << "Reading " << file << "...\n";
        for (std::string line; std::getline(stream, line);) {
            // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
            const auto record = stripComment(line);
            if (record.empty()) {
                continue;
            }
            switch (std::count(record.begin(), record.end(), ';')) {
                case 1: {
                    const auto [cp, propertyName] = fields<2>(record);
                    const CodePointRange range = parseHexCodePointRange(cp);
                    binaryProperties_[std::string(propertyName)].addAll(range);
                    break;
                }
                case 2:  // Enumerated property.
                    break;
                default:
                    std::terminate();
            }
        }
    }

    // Keys are C, L, M, N, P, S, Z.
    std::map<char, CodePointSet> coarseGeneralCategory_;
    // Keyed on short alias.
    std::map<std::string, CodePointSet, std::less<>> generalCategory_;
    std::map<std::string, CodePointSet, std::less<>> binaryProperties_;
    // Default <code point> omitted.
    std::map<char32_t, char32_t> simpleUppercaseMapping_;
    // Default <code point> omitted.
    std::map<char32_t, char32_t> simpleLowercaseMapping_;
    // Default 0 omitted.
    std::map<char32_t, std::uint8_t> canonicalCombiningClass_;
    std::map<char32_t, std::uint64_t> naturalNumericValue_;
    // Characters with a non-NaN Numeric_Value.
    CodePointSet codePointsWithNumericValue_;
};

UCD* const ucd = new UCD();

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
    return ucd->binaryPropertySet("Alphabetic").contains(c) &&
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
    return ucd->generalCategorySet("Mn").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsCombining(std::uint32_t c) {
    return ucd->generalCategorySet("M").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsExtender(std::uint32_t c) {
    return ucd->binaryPropertySet("Extender").contains(c);
}
extern "C" int unisift_IsCurrency(std::uint32_t c) {
    return ucd->generalCategorySet("Sc").contains(c);
}
extern "C" int unisift_IsPunctuation(std::uint32_t c) {
    return ucd->generalCategorySet("P").contains(c);
}
extern "C" int unisift_IsDiacritic(std::uint32_t c) {
    return ucd->binaryPropertySet("Diacritic").contains(c);
}
extern "C" int unisift_IsNumeric(std::uint32_t c) {
    return ucd->generalCategorySet("N").contains(c);
}
extern "C" int unisift_IsDecimalDigit(std::uint32_t c) {
    return ucd->generalCategorySet("Nd").contains(c);
}
extern "C" int unisift_IsWhiteSpace(std::uint32_t c) {
    return ucd->binaryPropertySet("White_Space").contains(c);
}
extern "C" int unisift_IsMath(std::uint32_t c) {
    return ucd->binaryPropertySet("Math").contains(c);
}
extern "C" int unisift_IsIdeographic(std::uint32_t c) {
    return ucd->binaryPropertySet("Ideographic").contains(c);
}
extern "C" int unisift_IsMiscSymbolic(std::uint32_t c) {
    return ucd->generalCategorySet("Sk").contains(c) ||
           ucd->generalCategorySet("So").contains(c);
}
extern "C" int unisift_IsIgnorable(std::uint32_t c) {
    return ((ucd->generalCategorySet("Cc").contains(c) ||
             ucd->generalCategorySet("Cf").contains(c)) &&
            !ucd->binaryPropertySet("White_Space").contains(c)) ||
           ucd->binaryPropertySet("Variation_Selector").contains(c) ||
           c == U'\u00AD';
}

extern "C" int unisift_IsUpper(std::uint32_t c) {
    return ucd->binaryPropertySet("Uppercase").contains(c);
}

extern "C" std::uint32_t unisift_ToLower(std::uint32_t c) {
    return ucd->simpleLowercaseMapping(c);
}

extern "C" std::uint32_t unisift_ToUpper(std::uint32_t c) {
    return ucd->simpleUppercaseMapping(c);
}

extern "C" int unisift_ToIntValue(std::uint32_t c) {
    // TODO(egg): The conversion to the return type is UB for large numbers.
    return ucd->naturalNumericValue(c).value_or(
            ucd->codePointsWithNumericValue().contains(c) ? -2 : -1);
}

extern "C" int unisift_GetCombiningClass(std::uint32_t c) {
    return ucd->canonicalCombiningClass(c);
}
