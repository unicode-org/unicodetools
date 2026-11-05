# Identifier_Type

How to think about Identifier_Type and choose values for new characters.

Required reading: https://www.unicode.org/reports/tr39/#Identifier_Status_and_Type

## Choosing Identifier_Type values

### Basics

- Recommended means: Needed *in identifiers* because of customary modern widespread use.
- Newly encoded scripts are probably Excluded.
- Make sure that the
  [UAX31](https://www.unicode.org/reports/tr31/)
  script tables are in sync with the
  [CLDR script metadata](https://cldr.unicode.org/development/updating-codes/updating-script-metadata).
- New characters in existing not-Excluded scripts are probably Uncommon_Use.
- Ask SEW if any of these should have a different ID_Type.
- Combining marks that are only needed in NFD are Uncommon_Use.
- Nothing to choose if there are strong reasons for restriction (Default_Ignorable, Not_NFKC).

### Some details

[UTS39 section 3.1.2 Choosing Identifier_Type Values](https://unicode-org.github.io/unicode-reports/tr39/tr39.html#Choosing_Type)

TODO: After Unicode 17 is published, change this link to point to the normal location.

Possible data sources:
- character encoding proposal docs
- EGIDS = https://en.wikipedia.org/wiki/Expanded_Graded_Intergenerational_Disruption_Scale
  For ICANN work, according to Asmus:
  We found level 4, which has some institutional support,
  a good cutoff for assuming that the language (and therefore its writing system)
  is in everyday use in the community.
  However, for any language at that boundary, we always look for additional info,
  sometimes making exceptions for level 5.
  (Sometimes, research shows a language, while vigorous, is only used orally,
  so then we downgrade it for domain names).
- Data from icann.org/idn under Root Zone LGR (look for "proposal documents").
  Each proposal evaluates which languages written in the script are common enough to
  support top-level domain names.
  A machine readable version is found in the XML files for the current version of the RZ-LGR
  (each character is annotated with a reference identifying the language that requires it).
- ethnologue.com

Asmus recommends for characters to be Recommended to look for positive evidence of
- large population
- stable, well supported language
- evidence it's (commonly) written in that script
- digitally supported
- not a specialized use in the writing system

One or the other factor, except "specialized use", may be offset by other factors.
Consider whether the community conducts its business in writing in that language,
and if so, in that script.

### Lots of details

[L2/25-069](https://www.unicode.org/L2/L2025/25069-determining-identifier-type.pdf)
Factors used in determining the Identifier_Type of characters

