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
            // TODO(egg): Handle ranges.
            const auto range = CodePointRange::inclusive(codePoint, codePoint);
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