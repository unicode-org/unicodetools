// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "ucd.hpp"

namespace {
int testFailures = 0;
}

#define EXPECT_EQ(left, right)                                           \
    do {                                                                 \
        const auto leftValue = (left);                                   \
        const auto rightValue = (right);                                 \
        if (leftValue != rightValue) {                                   \
            ++testFailures;                                              \
            std::cerr << "[FAILED] Expected equality of these values:\n" \
                         "           " #left " which is "                \
                      << leftValue                                       \
                      << "\n"                                            \
                         "           " #right " which is "               \
                      << rightValue << "\n";                             \
        }                                                                \
    } while (false)

int main(int argc, char** argv) {
    const auto ucd = unicode::CharacterDatabase("15.1.0");
    const unicode::CodePointSet punctuation = ucd.generalCategorySet("P");
    const unicode::CodePointSet letters = ucd.generalCategorySet("L");
    const unicode::CodePointSet symbols = ucd.generalCategorySet("S");
    const unicode::CodePointSet alphabetic =
            ucd.binaryPropertySet("Alphabetic");
    EXPECT_EQ(punctuation.size(), 842);
    EXPECT_EQ(symbols.size(), 7'775);
    EXPECT_EQ(letters.size(), 136'726);
    EXPECT_EQ(
            unicode::CodePointSet().addAll(punctuation).addAll(letters).size(),
            punctuation.size() + letters.size());
    EXPECT_EQ(unicode::CodePointSet()
                      .addAll(punctuation)
                      .addAll(letters)
                      .addAll(symbols)
                      .size(),
            punctuation.size() + letters.size() + symbols.size());
    EXPECT_EQ(unicode::CodePointSet().addAll(alphabetic).addAll(letters).size(),
              138'387);
    std::cout << "Test " << (testFailures > 0 ? "failed." : "passed.");
    return testFailures;
}
