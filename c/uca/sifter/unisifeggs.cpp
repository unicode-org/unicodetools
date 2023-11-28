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
    constexpr CodePointRange(char32_t front, char32_t back)
        : front_(front), back_(back) {
        if (empty()) {
            front_ = 1;
            back_ = 0;
        }
    }

    constexpr bool empty() const {
        return front_ > back_;
    }

    constexpr bool contains(char32_t c) const {
        return c >= front_ && c <= back_;
    }

    constexpr CodePointRange intersection(CodePointRange const& other) const {
        return CodePointRange(std::max(front_, other.front_),
                              std::min(back_, other.back_));
    }

    constexpr char32_t front() const {
        return front_;
    }

    constexpr char32_t back() const {
        return back_;
    }

    struct iterator {
        char32_t operator*() const {
            return value;
        }

        void operator++() {
            ++value;
        }

        bool operator==(iterator const& other) const = default;
        bool operator!=(iterator const& other) const = default;

        char32_t value;
    };

    iterator begin() const {
        return {front_};
    }

    iterator end() const {
        return {back_ + 1};
    }

   private:
    char32_t front_;
    char32_t back_;
};

constexpr CodePointRange convex_hull(CodePointRange const& r1,
                                     CodePointRange const& r2) {
    return CodePointRange(std::min(r1.front(), r2.front()),
                          std::max(r1.back(), r2.back()));
}

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

    void Include(CodePointRange range) {
        auto it = std::partition_point(
            ranges_.begin(), ranges_.end(), [&range](CodePointRange const& r) {
                return r.back() < range.front();
            });
        if (it == ranges_.end() || it->intersection(range).empty()) {
            it = ranges_.insert(it, range);
            if ((it + 1) != ranges_.end() &&
                (it + 1)->front() == it->back() + 1) {
                *it = convex_hull(*it, *(it + 1));
                ranges_.erase(it + 1);
            }
            if (it != ranges_.begin() && (it - 1)->back() + 1 == it->front()) {
                *(it - 1) = convex_hull(*(it - 1), *it);
                ranges_.erase(it);
            }
        } else {
            *it = convex_hull(*it, range);
        }
    }

    CodePointSet Include(CodePointSet const& other) {
        for (auto const& range : other.ranges_) {
            Include(range);
        }
    }

   private:
    // Non-overlapping ranges in ascending order.
    std::vector<CodePointRange> ranges_;
};

constexpr std::string_view strip_comment(std::string_view const line) {
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
        auto const begin = std::min(field_with_spaces.find_first_not_of(" "),
                                    field_with_spaces.size());
        auto const end = field_with_spaces.find_last_not_of(" ") + 1;
        field = field_with_spaces.substr(begin, end - begin);
    }
    return result;
}

constexpr char32_t parse_hex_codepoint(std::string_view hex) {
    std::uint32_t c;
    std::from_chars(hex.data(), hex.data() + hex.size(), c, 16);
    return static_cast<char32_t>(c);
}

constexpr CodePointRange parse_hex_codepoint_range(std::string_view hex) {
    auto const front_end = hex.find("..");
    if (front_end == std::string_view::npos) {
        char32_t const front = parse_hex_codepoint(hex);
        return CodePointRange(front, front);
    }
    return CodePointRange(parse_hex_codepoint(hex.substr(0, front_end)),
                          parse_hex_codepoint(hex.substr(front_end + 2)));
}

class UCD {
   public:
    explicit UCD() {
        auto const ucd_directory = std::filesystem::current_path()
                                       .parent_path()
                                       .parent_path()
                                       .parent_path() /
                                   "unicodetools" / "data" / "ucd" / "dev";
        {
            std::ifstream unicode_data(ucd_directory / "UnicodeData.txt");
            CHECK(unicode_data.good());
            std::cout << "Reading UnicodeData.txt...\n";
            for (std::string line; std::getline(unicode_data, line);) {
                // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
                auto const [cp,
                            name,
                            gc,
                            ccc,
                            bc,
                            dt_dm,
                            nv_decimal,
                            nv_digit,
                            nv_numeric,
                            bm,
                            unicode_1_name,
                            iso_comment,
                            suc,
                            slc,
                            stc] = fields<15>(line);
                char32_t const code_point = parse_hex_codepoint(cp);
                // TODO(egg): Handle ranges.
                auto const range = CodePointRange(code_point, code_point);
                coarse_general_category_[gc.front()].Include(range);
                general_category_[std::string(gc)].Include(range);
                canonical_combining_class_.emplace(code_point,
                                                   std::stoi(std::string(ccc)));
                if (!suc.empty()) {
                    simple_uppercase_mapping_.emplace(code_point,
                                                      parse_hex_codepoint(suc));
                }
                if (!slc.empty()) {
                    simple_lowercase_mapping_.emplace(code_point,
                                                      parse_hex_codepoint(slc));
                }
            }
        }
        ReadMultiPropertyFile(ucd_directory / "PropList.txt");
        ReadMultiPropertyFile(ucd_directory / "DerivedCoreProperties.txt");
        {
            std::ifstream derived_numeric_value(ucd_directory / "extracted" /
                                                "DerivedNumericValues.txt");
            CHECK(derived_numeric_value.good());
            std::cout << "Reading DerivedNumericValues.txt...\n";
            for (std::string line; std::getline(derived_numeric_value, line);) {
                // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
                auto const record = strip_comment(line);
                if (record.empty()) {
                    continue;
                }
                auto const [cp, decimal_nv, _, rational_nv] = fields<4>(record);
                auto const range = parse_hex_codepoint_range(cp);
                code_points_with_numeric_value_.Include(range);
                if (rational_nv.find_first_not_of("0123456789") ==
                    std::string_view::npos) {
                    for (char32_t const code_point : range) {
                        natural_numeric_value_.emplace(
                            code_point, std::stoll(std::string(rational_nv)));
                    }
                }
            }
        }
    }

    CodePointSet const& GeneralCategorySet(std::string_view const gc) {
        if (gc.size() == 1) {
            return coarse_general_category_.at(gc.front());
        }
        auto it = general_category_.find(gc);
        CHECK(it != general_category_.end()) << gc;
        return it->second;
    }

    CodePointSet const& BinaryPropertySet(
        std::string_view const binary_property_name) {
        auto it = binary_properties_.find(binary_property_name);
        CHECK(it != binary_properties_.end()) << binary_property_name;
        return it->second;
    }

    char32_t SimpleLowercaseMapping(char32_t const c) const {
        auto const it = simple_lowercase_mapping_.find(c);
        if (it == simple_lowercase_mapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    char32_t SimpleUppercaseMapping(char32_t const c) const {
        auto const it = simple_uppercase_mapping_.find(c);
        if (it == simple_uppercase_mapping_.end()) {
            return c;
        } else {
            return it->second;
        }
    }

    std::uint8_t CanonicalCombiningClass(char32_t const c) const {
        auto const it = canonical_combining_class_.find(c);
        if (it == canonical_combining_class_.end()) {
            return 0;
        } else {
            return it->second;
        }
    }

    std::optional<std::uint64_t> NaturalNumericValue(char32_t const c) const {
        auto const it = natural_numeric_value_.find(c);
        if (it == natural_numeric_value_.end()) {
            return std::nullopt;
        } else {
            return it->second;
        }
    }

    CodePointSet const& CodePointsWithNumericValue() const {
        return code_points_with_numeric_value_;
    }

   private:
    void ReadMultiPropertyFile(std::filesystem::path const& file) {
        std::ifstream prop_list(file);
        CHECK(prop_list.good());
        std::cout << "Reading " << file << "...\n";
        for (std::string line; std::getline(prop_list, line);) {
            // See https://www.unicode.org/reports/tr44/#UnicodeData.txt.
            auto const record = strip_comment(line);
            if (record.empty()) {
                continue;
            }
            switch (std::count(record.begin(), record.end(), ';')) {
                case 1: {
                    auto const [cp, property_name] = fields<2>(record);
                    CodePointRange const range = parse_hex_codepoint_range(cp);
                    binary_properties_[std::string(property_name)].Include(
                        range);
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
    std::map<char, CodePointSet> coarse_general_category_;
    // Keyed on short alias.
    std::map<std::string, CodePointSet, std::less<>> general_category_;
    std::map<std::string, CodePointSet, std::less<>> binary_properties_;
    // Default <code point> omitted.
    std::map<char32_t, char32_t> simple_uppercase_mapping_;
    // Default <code point> omitted.
    std::map<char32_t, char32_t> simple_lowercase_mapping_;
    // Default 0 omitted.
    std::map<char32_t, std::uint8_t> canonical_combining_class_;
    std::map<char32_t, std::uint64_t> natural_numeric_value_;
    // Characters with a non-NaN Numeric_Value.
    CodePointSet code_points_with_numeric_value_;
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

extern "C" int unisift_IsAlphabetic(UInt32 c) {
    return ucd->BinaryPropertySet("Alphabetic").contains(c) &&
           !isSpecialCJKIdeograph(c) &&
           !(c == 0x1E13C || c == 0x1E13D || c == 0x16B42 || c == 0x16B43);
}
extern "C" int unisift_IsNonSpacing(UInt32 c) {
    return ucd->GeneralCategorySet("Mn").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsCombining(UInt32 c) {
    return ucd->GeneralCategorySet("M").contains(c) ||
           isSifterNonSpacingCombining(c);
}
extern "C" int unisift_IsExtender(UInt32 c) {
    return ucd->BinaryPropertySet("Extender").contains(c);
}
extern "C" int unisift_IsCurrency(UInt32 c) {
    return ucd->GeneralCategorySet("Sc").contains(c);
}
extern "C" int unisift_IsPunctuation(UInt32 c) {
    return ucd->GeneralCategorySet("P").contains(c);
}
extern "C" int unisift_IsDiacritic(UInt32 c) {
    return ucd->BinaryPropertySet("Diacritic").contains(c);
}
extern "C" int unisift_IsNumeric(UInt32 c) {
    return ucd->GeneralCategorySet("N").contains(c);
}
extern "C" int unisift_IsDecimalDigit(UInt32 c) {
    return ucd->GeneralCategorySet("Nd").contains(c);
}
extern "C" int unisift_IsWhiteSpace(UInt32 c) {
    return ucd->BinaryPropertySet("White_Space").contains(c);
}
extern "C" int unisift_IsMath(UInt32 c) {
    return ucd->BinaryPropertySet("Math").contains(c);
}
extern "C" int unisift_IsIdeographic(UInt32 c) {
    return ucd->BinaryPropertySet("Ideographic").contains(c);
}
extern "C" int unisift_IsMiscSymbolic(UInt32 c) {
    return ucd->GeneralCategorySet("Sk").contains(c) ||
           ucd->GeneralCategorySet("So").contains(c);
}
extern "C" int unisift_IsIgnorable(UInt32 c) {
    return ((ucd->GeneralCategorySet("Cc").contains(c) ||
             ucd->GeneralCategorySet("Cf").contains(c)) &&
            !ucd->BinaryPropertySet("White_Space").contains(c)) ||
           ucd->BinaryPropertySet("Variation_Selector").contains(c) ||
           c == U'\u00AD';
}

extern "C" int unisift_IsUpper(UInt32 c) {
    return ucd->BinaryPropertySet("Uppercase").contains(c);
}

extern "C" UInt32 unisift_ToLower(UInt32 c) {
    return ucd->SimpleLowercaseMapping(c);
}

extern "C" UInt32 unisift_ToUpper(UInt32 c) {
    return ucd->SimpleUppercaseMapping(c);
}

extern "C" int unisift_ToIntValue(UInt32 c) {
    // TODO(egg): The conversion to the return type is UB for large numbers.
    return ucd->NaturalNumericValue(c).value_or(
        ucd->CodePointsWithNumericValue().contains(c) ? -2 : -1);
}

extern "C" int unisift_GetCombiningClass(UInt32 c) {
    return ucd->CanonicalCombiningClass(c);
}
