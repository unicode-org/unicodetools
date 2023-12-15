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
        ? (void)0        \
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
            iterator const& other) const = default;

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

    constexpr CodePointRange intersection(CodePointRange const& other) const {
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

class CodePointSet {
   public:
    // Empty set.
    CodePointSet() = default;

    bool contains(char32_t c) const {
        auto const it = std::partition_point(
            ranges_.begin(), ranges_.end(), [c](CodePointRange const& range) {
                return range.back() < c;
            });
        return it != ranges_.end() && it->contains(c);
    }

    void addAll(CodePointRange range) {
        auto const first_modified_range = std::partition_point(
            ranges_.begin(), ranges_.end(), [&range](CodePointRange const& r) {
                return r.back() + 1 < range.front();
            });
        auto const past_modified_ranges = std::partition_point(
            ranges_.begin(), ranges_.end(), [&range](CodePointRange const& r) {
                return r.front() <= range.back() + 1;
            });
        char32_t inserted_range_first = range.front();
        if (first_modified_range != ranges_.end()) {
            inserted_range_first = std::min(first_modified_range->front(), inserted_range_first);
        }
        char32_t inserted_range_last = range.back();
        if (past_modified_ranges != ranges_.begin()) {
            auto const last_modified_range = past_modified_ranges - 1;
            inserted_range_last =
                std::max(last_modified_range->back(), inserted_range_last);
        }
        auto const it = ranges_.erase(first_modified_range, past_modified_ranges);
        ranges_.insert(it, CodePointRange::Inclusive(inserted_range_first, inserted_range_last));
    }

    CodePointSet addAll(CodePointSet const& other) {
        for (auto const& range : other.ranges_) {
            addAll(range);
        }
    }

    std::vector<CodePointRange> const& ranges() const {
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
        auto const field_first = std::min(
            field_with_spaces.find_first_not_of(" "), field_with_spaces.size());
        auto const field_last = field_with_spaces.find_last_not_of(" ");
        auto const field_size = field_last - field_first + 1;
        field = field_with_spaces.substr(field_first, field_size);
    }
    return result;
}

constexpr char32_t parseHexCodepoint(std::string_view hex) {
    std::uint32_t c;
    std::from_chars(hex.data(), hex.data() + hex.size(), c, 16);
    return static_cast<char32_t>(c);
}

constexpr CodePointRange parseHexCodepointRange(std::string_view hex) {
    auto const first_dot = hex.find("..");
    if (first_dot == std::string_view::npos) {
        char32_t const code_point = parseHexCodepoint(hex);
        return CodePointRange::Inclusive(code_point, code_point);
    }
    return CodePointRange::Inclusive(
        parseHexCodepoint(hex.substr(0, first_dot)),
        parseHexCodepoint(hex.substr(first_dot + 2)));
}

class UCD {
   public:
    explicit UCD() {
        auto const ucdDirectory = std::filesystem::current_path()
                                      .parent_path()
                                      .parent_path()
                                      .parent_path() /
                                  "unicodetools" / "data" / "ucd" / "dev";
        {
            std::ifstream unicode_data(ucdDirectory / "UnicodeData.txt");
            CHECK(unicode_data.good())
                << "Run this tool from the c/uca/sifter directory";
            std::cout << "Reading UnicodeData.txt...\n";
            for (std::string line; std::getline(unicode_data, line);) {
                // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
                auto const [cp,
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
                char32_t const codepoint = parseHexCodepoint(cp);
                // TODO(egg): Handle ranges.
                auto const range =
                    CodePointRange::Inclusive(codepoint, codepoint);
                coarseGeneralCategory_[gc.front()].addAll(range);
                generalCategory_[std::string(gc)].addAll(range);
                canonicalCombiningClass_.emplace(codepoint,
                                                 std::stoi(std::string(ccc)));
                if (!suc.empty()) {
                    simpleUppercaseMapping_.emplace(codepoint,
                                                    parseHexCodepoint(suc));
                }
                if (!slc.empty()) {
                    simpleLowercaseMapping_.emplace(codepoint,
                                                    parseHexCodepoint(slc));
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
                auto const record = stripComment(line);
                if (record.empty()) {
                    continue;
                }
                auto const [cp, decimalNV, _, rationalNV] = fields<4>(record);
                auto const range = parseHexCodepointRange(cp);
                codePointsWithNumericValue_.addAll(range);
                if (rationalNV.find_first_not_of("0123456789") ==
                    std::string_view::npos) {
                    for (char32_t const codepoint : range) {
                        naturalNumericValue_.emplace(
                            codepoint, std::stoll(std::string(rationalNV)));
                    }
                }
            }
        }
    }

    CodePointSet const& generalCategorySet(std::string_view const gc) {
        if (gc.size() == 1) {
            return coarseGeneralCategory_.at(gc.front());
        }
        auto it = generalCategory_.find(gc);
        CHECK(it != generalCategory_.end()) << gc;
        return it->second;
    }

    CodePointSet const& binaryPropertySet(
        std::string_view const binary_property_name) {
        auto it = binaryProperties_.find(binary_property_name);
        CHECK(it != binaryProperties_.end()) << binary_property_name;
        return it->second;
    }

    char32_t simpleLowercaseMapping(char32_t const c) const {
        auto const it = simpleLowercaseMapping_.find(c);
        if (it == simpleLowercaseMapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    char32_t simpleUppercaseMapping(char32_t const c) const {
        auto const it = simpleUppercaseMapping_.find(c);
        if (it == simpleUppercaseMapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    std::uint8_t canonicalCombiningClass(char32_t const c) const {
        auto const it = canonicalCombiningClass_.find(c);
        if (it == canonicalCombiningClass_.end()) {
            return 0;
        } else {
            return it->second;
        }
    }

    std::optional<std::uint64_t> naturalNumericValue(char32_t const c) const {
        auto const it = naturalNumericValue_.find(c);
        if (it == naturalNumericValue_.end()) {
            return std::nullopt;
        } else {
            return it->second;
        }
    }

    CodePointSet const& codePointsWithNumericValue() const {
        return codePointsWithNumericValue_;
    }

   private:
    void readMultiPropertyFile(std::filesystem::path const& file) {
        std::ifstream stream(file);
        CHECK(stream.good());
        std::cout << "Reading " << file << "...\n";
        for (std::string line; std::getline(stream, line);) {
            // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
            auto const record = stripComment(line);
            if (record.empty()) {
                continue;
            }
            switch (std::count(record.begin(), record.end(), ';')) {
                case 1: {
                    auto const [cp, propertyName] = fields<2>(record);
                    CodePointRange const range = parseHexCodepointRange(cp);
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

typedef unsigned short UShort16;

typedef unsigned int UInt32;

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

extern "C" int unisift_IsAlphabetic(UInt32 c) {
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

extern "C" int unisift_IsNonSpacing(UInt32 c) {
    return ucd->generalCategorySet("Mn").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsCombining(UInt32 c) {
    return ucd->generalCategorySet("M").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsExtender(UInt32 c) {
    return ucd->binaryPropertySet("Extender").contains(c);
}
extern "C" int unisift_IsCurrency(UInt32 c) {
    return ucd->generalCategorySet("Sc").contains(c);
}
extern "C" int unisift_IsPunctuation(UInt32 c) {
    return ucd->generalCategorySet("P").contains(c);
}
extern "C" int unisift_IsDiacritic(UInt32 c) {
    return ucd->binaryPropertySet("Diacritic").contains(c);
}
extern "C" int unisift_IsNumeric(UInt32 c) {
    return ucd->generalCategorySet("N").contains(c);
}
extern "C" int unisift_IsDecimalDigit(UInt32 c) {
    return ucd->generalCategorySet("Nd").contains(c);
}
extern "C" int unisift_IsWhiteSpace(UInt32 c) {
    return ucd->binaryPropertySet("White_Space").contains(c);
}
extern "C" int unisift_IsMath(UInt32 c) {
    return ucd->binaryPropertySet("Math").contains(c);
}
extern "C" int unisift_IsIdeographic(UInt32 c) {
    return ucd->binaryPropertySet("Ideographic").contains(c);
}
extern "C" int unisift_IsMiscSymbolic(UInt32 c) {
    return ucd->generalCategorySet("Sk").contains(c) ||
           ucd->generalCategorySet("So").contains(c);
}
extern "C" int unisift_IsIgnorable(UInt32 c) {
    return ((ucd->generalCategorySet("Cc").contains(c) ||
             ucd->generalCategorySet("Cf").contains(c)) &&
            !ucd->binaryPropertySet("White_Space").contains(c)) ||
           ucd->binaryPropertySet("Variation_Selector").contains(c) ||
           c == U'\u00AD';
}

extern "C" int unisift_IsUpper(UInt32 c) {
    return ucd->binaryPropertySet("Uppercase").contains(c);
}

extern "C" UInt32 unisift_ToLower(UInt32 c) {
    return ucd->simpleLowercaseMapping(c);
}

extern "C" UInt32 unisift_ToUpper(UInt32 c) {
    return ucd->simpleUppercaseMapping(c);
}

extern "C" int unisift_ToIntValue(UInt32 c) {
    // TODO(egg): The conversion to the return type is UB for large numbers.
    return ucd->naturalNumericValue(c).value_or(
        ucd->codePointsWithNumericValue().contains(c) ? -2 : -1);
}

extern "C" int unisift_GetCombiningClass(UInt32 c) {
    return ucd->canonicalCombiningClass(c);
}
