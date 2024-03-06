// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#pragma once
#include <algorithm>
#include <array>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <map>
#include <ranges>
#include <string>
#include <vector>

namespace unicode {

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

    static constexpr CodePointRange inclusive(char32_t first, char32_t last) {
        return CodePointRange(first, last + 1);
    }

    constexpr bool empty() const {
        return first_ >= pastTheEnd_;
    }

    constexpr std::size_t size() const {
        return empty() ? 0 : pastTheEnd_ - first_;
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

    std::size_t size() const {
        std::size_t result = 0;
        for (const auto& range : ranges_) {
            result += range.size();
        }
        return result;
    }

    CodePointSet& addAll(CodePointRange range) {
        // All earlier ranges end before the new one and are not adjacent to
        // |range|.
        const auto firstModifiedRange =
                std::partition_point(ranges_.begin(),
                                     ranges_.end(),
                                     [&range](const CodePointRange& r) {
                                         return r.back() + 1 < range.front();
                                     });
        // First range that starts after the new one and is not adjacent to
        // |range|.
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
        if (pastModifiedRanges != ranges_.begin() &&
            pastModifiedRanges - 1 >= firstModifiedRange) {
            const auto lastModifiedRange = pastModifiedRanges - 1;
            insertedRangeLast =
                    std::max(lastModifiedRange->back(), insertedRangeLast);
        }
        const auto it = ranges_.erase(firstModifiedRange, pastModifiedRanges);
        ranges_.insert(it,
                       CodePointRange::inclusive(insertedRangeFirst,
                                                 insertedRangeLast));
        return *this;
    }

    CodePointSet& addAll(const CodePointSet& other) {
        for (const auto& range : other.ranges_) {
            addAll(range);
        }
        return *this;
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

constexpr std::optional<char32_t> parseOptionalHexCodePoint(
        std::string_view hex) {
    if (hex.empty()) {
        return std::nullopt;
    }
    std::uint32_t c;
    std::from_chars(hex.data(), hex.data() + hex.size(), c, 16);
    return static_cast<char32_t>(c);
}

constexpr CodePointRange parseHexCodePointRange(std::string_view hex) {
    const auto first_dot = hex.find("..");
    if (first_dot == std::string_view::npos) {
        const char32_t code_point = parseHexCodePoint(hex);
        return CodePointRange::inclusive(code_point, code_point);
    }
    return CodePointRange::inclusive(
            parseHexCodePoint(hex.substr(0, first_dot)),
            parseHexCodePoint(hex.substr(first_dot + 2)));
}

class CharacterDatabase {
  public:
    explicit CharacterDatabase(std::string_view versionDirectory);

    const CodePointSet& generalCategorySet(std::string_view const gc) const;

    const CodePointSet& binaryPropertySet(
            std::string_view const binary_property_name) const;

    char32_t simpleLowercaseMapping(const char32_t c) const;

    char32_t simpleUppercaseMapping(const char32_t c) const;

    std::uint8_t canonicalCombiningClass(const char32_t c) const;

    std::optional<std::uint64_t> naturalNumericValue(const char32_t c) const;

    const CodePointSet& codePointsWithNumericValue() const;

  private:
    void readMultiPropertyFile(const std::filesystem::path& file);

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

}  // namespace unicode