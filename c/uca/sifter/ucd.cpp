// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "ucd.hpp"

namespace unicode {

CharacterDatabase::CharacterDatabase(const std::string_view versionDirectory) {
    const auto ucdDirectory = std::filesystem::current_path()
                                      .parent_path()
                                      .parent_path()
                                      .parent_path() /
                              "unicodetools" / "data" / "ucd" /
                              versionDirectory;
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
            auto range = CodePointRange::inclusive(codePoint, codePoint);
            if (name.ends_with(", First>")) {
                std::string nextLine;
                std::getline(unicodeData, nextLine);
                const auto [lastCp, lastName] = fields<2>(nextLine);
                CHECK(lastName.ends_with(", Last>")) << line << "\n"
                                                     << nextLine;
                CHECK(lastName.starts_with(name.substr(0, name.size() - 8)))
                        << line << "\n"
                        << nextLine;
                CHECK(line.substr(cp.size() + 1 + name.size()) ==
                      nextLine.substr(lastCp.size() + 1 + lastName.size()))
                        << line << "\n"
                        << nextLine;
                range = CodePointRange::inclusive(codePoint,
                                                  parseHexCodePoint(lastCp));
            }
            coarseGeneralCategory_[gc.front()].addAll(range);
            generalCategory_[std::string(gc)].addAll(range);
            const std::uint8_t combiningClass = std::stoi(std::string(ccc));
            const auto simpleUppercase = parseOptionalHexCodePoint(suc);
            const auto simpleLowercase = parseOptionalHexCodePoint(slc);
            for (const char32_t c : range) {
                canonicalCombiningClass_.emplace(c, combiningClass);
                if (simpleUppercase.has_value()) {
                    simpleUppercaseMapping_.emplace(c, *simpleUppercase);
                }
                if (simpleLowercase.has_value()) {
                    simpleLowercaseMapping_.emplace(c, *simpleLowercase);
                }
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
                std::uint64_t const naturalNumericValue =
                        std::stoll(std::string(rationalNV));
                for (const char32_t codePoint : range) {
                    naturalNumericValue_.emplace(codePoint,
                                                 naturalNumericValue);
                }
            }
        }
    }
}

const CodePointSet& CharacterDatabase::generalCategorySet(
        std::string_view const gc) const {
    if (gc.size() == 1) {
        return coarseGeneralCategory_.at(gc.front());
    }
    auto it = generalCategory_.find(gc);
    CHECK(it != generalCategory_.end()) << gc;
    return it->second;
}

const CodePointSet& CharacterDatabase::binaryPropertySet(
        std::string_view const binary_property_name) const {
    auto it = binaryProperties_.find(binary_property_name);
    CHECK(it != binaryProperties_.end()) << binary_property_name;
    return it->second;
}

char32_t CharacterDatabase::simpleLowercaseMapping(const char32_t c) const {
    const auto it = simpleLowercaseMapping_.find(c);
    if (it == simpleLowercaseMapping_.end()) {
        return c;
    } else {
        return it->second;
    }
}

char32_t CharacterDatabase::simpleUppercaseMapping(const char32_t c) const {
    const auto it = simpleUppercaseMapping_.find(c);
    if (it == simpleUppercaseMapping_.end()) {
        return c;
    } else {
        return it->second;
    }
}

std::uint8_t CharacterDatabase::canonicalCombiningClass(
        const char32_t c) const {
    const auto it = canonicalCombiningClass_.find(c);
    if (it == canonicalCombiningClass_.end()) {
        return 0;
    } else {
        return it->second;
    }
}

std::optional<std::uint64_t> CharacterDatabase::naturalNumericValue(
        const char32_t c) const {
    const auto it = naturalNumericValue_.find(c);
    if (it == naturalNumericValue_.end()) {
        return std::nullopt;
    } else {
        return it->second;
    }
}

const CodePointSet& CharacterDatabase::codePointsWithNumericValue() const {
    return codePointsWithNumericValue_;
}

void CharacterDatabase::readMultiPropertyFile(
        const std::filesystem::path& file) {
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

}  // namespace unicode