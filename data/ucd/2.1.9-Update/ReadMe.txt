
UNICODE 2.1 CHARACTER DATABASE (update 2.1.9)

Copyright (c) 1991-1999 Unicode, Inc.
All Rights reserved.

DISCLAIMER

The Unicode Character Database "UnicodeData-Latest.txt" is provided as-is by
Unicode, Inc. (The Unicode Consortium). No claims are made as to fitness for any
particular purpose. No warranties of any kind are expressed or implied. The
recipient agrees to determine applicability of information provided. If this
file has been purchased on magnetic or optical media from Unicode, Inc.,
the sole remedy for any claim will be exchange of defective media within
90 days of receipt.

This disclaimer is applicable for all other data files accompanying the
Unicode Character Database, some of which have been compiled by the
Unicode Consortium, and some of which have been supplied by other vendors.

LIMITATIONS ON RIGHTS TO REDISTRIBUTE THIS DATA

Recipient is granted the right to make copies in any form for internal
distribution and to freely use the information supplied in the creation of
products supporting the Unicode (TM) Standard. This file can be redistributed
to third parties or other organizations (whether for profit or not) as long
as this notice and the disclaimer notice are retained.

EXPLANATORY INFORMATION

The Unicode Character Database defines the default Unicode character
properties, and internal mappings. Particular implementations may choose to
override the properties and mappings that are not normative. If that is done,
it is up to the implementer to establish a protocol to convey that
information. For more information about character properties and mappings,
see "The Unicode Standard, Worldwide Character Encoding, Version 2.0",
published by Addison-Wesley. For information about other data files
accompanying the Unicode Character Database, see the section of the
Unicode Standard they were extracted from, or the explanatory readme
files and/or header sections with those files.

The Unicode Character Database has been updated to reflect Version 2.1
of the Unicode Standard, with two additional characters added to those
published in Version 2.0:

   U+20AC EURO SIGN
   U+FFFC OBJECT REPLACEMENT CHARACTER

A number of corrections have also been made to case mappings or other
errors in the database noted since the publication of Version 2.0. And
normative bidirectional properties have been modified to reflect
decisions of the Unicode Technical Committee.

The Unicode Character Database is a plain ASCII text file consisting of lines
containing fields terminated by semicolons. Each line represents the data for
one encoded character in the Unicode Standard, Version 2.1. Every encoded
character has a data entry, with the exception of certain special ranges, as
detailed below.

There are four special ranges of characters that are represented only by
their start and end characters, since the properties in the file are uniform,
except for code values (which are all sequential and assigned). The names of CJK
ideograph characters and Hangul syllable characters are algorithmically
derivable. (See the Unicode Standard for more information). Surrogate
code values and private use characters have no names.

The exact ranges represented by start and end characters are:

   CJK Ideographs (U+4E00 - U+9FA5)
   Hangul Syllables (U+AC00 - U+D7A3)
   The Surrogates Area (U+D800 - U+DFFF)
   The Private Use Area (U+E000 - U+F8FF)

The following table describes the format and meaning of each field in a
data entry in the Unicode Character Database. Fields which contain
normative information are so indicated.

Note that the term "normative" when applied to a property or field of
the Unicode Character Database, does not mean that the value of that
field will *never* change. Corrections and extensions to the standard
in the future may require minor changes to normative values, even
though the Unicode Technical Committee strives to minimize such changes.
"Normative" means that implementations that claim conformance to the
Unicode Standard (at a particular version) and which make use of
a particular property or field must follow the specifications of the
standard for that property or field in order to be conformant. If a
property or field is only "informative", a conformant implementation
is free to use or change such values as it may require, while still
being conformant to the standard.

Field	Explanation
-----	-----------

  0	Code value in 4-digit hexadecimal format.
  	This field is normative.

  1	Unicode 2.1 Character Name. These names match exactly the
	names published in Chapter 7 of the Unicode Standard, Version
	2.0, except for the two additional characters.
  	This field is normative.

  2	General Category. This is a useful breakdown into various "character
	types" which can be used as a default categorization in implementations.
 	Some of the values are normative, and some are informative.
 	See below for a brief explanation.

  3	Canonical Combining Classes. The classes used for the
	Canonical Ordering Algorithm in the Unicode Standard. These
	classes are also printed in Chapter 4 of the Unicode Standard.
        This field is normative. See below for a brief explanation.

  4	Bidirectional Category. See the list below for an explanation of the
	abbreviations used in this field. These are the categories required
	by the Bidirectional Behavior Algorithm in the Unicode Standard.
	These categories are summarized in Chapter 4 of the Unicode Standard.
	This field is normative.

  5	Character Decomposition Mapping. In the Unicode Standard, not all of
	the mappings are full (maximal) decompositions. Recursive
	application of look-up for decompositions will, in all cases, lead to
	a maximal decomposition. The decomposition mappings match exactly the
	decomposition mappings published with the character names in Chapter 7
	of the Unicode Standard. This field is normative.

  6	Decimal digit value. This is a numeric field. If the character
	has the decimal digit property, as specified in Chapter 4 of
	the Unicode Standard, the value of that digit is represented
	with an integer value in this field. This field is normative.

  7	Digit value. This is a numeric field. If the character represents a
	digit, not necessarily a decimal digit, the value is here. This
	covers digits which do not form decimal radix forms, such as the
	compatibility superscript digits. This field is informative.

  8	Numeric value. This is a numeric field. If the character has the
	numeric property, as specified in Chapter 4 of the Unicode
	Standard, the value of that character is represented with an
	integer or rational number in this field. This includes fractions as,
	e.g., "1/5" for U+2155 VULGAR FRACTION ONE FIFTH.
	Also included are numerical values for compatibility characters
	such as circled numbers. This field is normative.

  9	If the characters has been identified as a "mirrored" character in
        bidirectional text, this field has the value "Y"; otherwise "N".
	The list of mirrored characters is also printed in Chapter 4 of
	the Unicode Standard. This field is normative.

 10	Unicode 1.0 Name. This is the old name as published in Unicode 1.0.
	This name is only provided when it is significantly different from
	the Unicode 2.1 name for the character. This field is informative.

 11	10646 Comment field. This field is informative.

 12	Upper case equivalent mapping. If a character is part of an
	alphabet with case distinctions, and has an upper case equivalent,
	then the upper case equivalent is in this field. See the explanation
	below on case distinctions. These mappings are always one-to-one,
	not one-to-many or many-to-one. This field is informative.

 13	Lower case equivalent mapping. Similar to 12. This field is informative.

 14	Title case equivalent mapping. Similar to 12. This field is informative.

GENERAL CATEGORY

The values in this field are abbreviations for the following. Some of the
values are normative, and some are informative. For more information, see
the Unicode Standard. Note: the standard does not assign information to
control characters (except for TAB in the Bidirectional Algorithm).
Implementations will generally also assign categories to certain control
characters, notably CR and LF, according to platform conventions.

Normative
    Lu = Letter, Uppercase
    Ll = Letter, Lowercase
    Lt = Letter, Titlecase

    Mn = Mark, Non-Spacing
    Mc = Mark, Spacing Combining
    Me = Mark, Enclosing

    Nd = Number, Decimal Digit
    Nl = Number, Letter
    No = Number, Other

    Zs = Separator, Space
    Zl = Separator, Line
    Zp = Separator, Paragraph

    Cc = Other, Control
    Cf = Other, Format
    Cs = Other, Surrogate
    Co = Other, Private Use
    Cn = Other, Not Assigned

Informative
    Lm = Letter, Modifier
    Lo = Letter, Other

    Pc = Punctuation, Connector
    Pd = Punctuation, Dash
    Ps = Punctuation, Open
    Pe = Punctuation, Close
    Pi = Punctuation, Initial quote (may behave like Ps or Pe depending
                              on usage)
    Pf = Punctuation, Final quote (may behave like Ps or Pe depending
                              on usage)
    Po = Punctuation, Other

    Sm = Symbol, Math
    Sc = Symbol, Currency
    Sk = Symbol, Modifier
    So = Symbol, Other

BIDIRECTIONAL PROPERTIES

Please refer to the Unicode Standard for an explanation of the algorithm for
Bidirectional Behavior and an explanation of the significance of these categories.
These values are normative.

Strong types:
	L	Left-Right; Most alphabetic, syllabic, and logographic
			characters (e.g., CJK ideographs)
	R	Right-Left; Arabic, Hebrew, and
			punctuation specific to those scripts
Weak types:
	EN	European Number
	ES	European Number Separator
	ET	European Number Terminator
	AN	Arabic Number
	CS	Common Number Separator

Separators:
	B	Block Separator
	S	Segment Separator

Neutrals:
	WS	Whitespace
	ON	Other Neutrals ; All other characters: punctuation, symbols

CHARACTER DECOMPOSITION TAGS

The decomposition is a normative property of a character. The tags supplied
with certain decomposition mappings generally indicate formatting information.
Where no such tag is given, the mapping is designated as canonical.
Conversely, the presence of a formatting tag also indicates
that the mapping is a compatibility mapping and not a canonical
mapping. In the absence of other formatting information in a
compatibility mapping, the tag <compat> is used to distinguish it from
canonical mappings.

In some instances a canonical mapping or a compatibility mapping
may consist of a single character. For a canonical mapping, this
indicates that the character is a canonical equivalent of another single
character. For a compatibility mapping, this indicates that the
character is a compatibility equivalent of another single character.

The compatibility formatting tags used are:

	<font>		A font variant (e.g. a blackletter form).
	<noBreak>	A no-break version of a space or hyphen.
	<initial>	An initial presentation form (Arabic).
	<medial>	A medial presentation form (Arabic).
	<final>		A final presentation form (Arabic).
	<isolated>	An isolated presentation form (Arabic).
	<circle>	An encircled form.
	<super>		A superscript form.
	<sub>		A subscript form.
	<vertical>	A vertical layout presentation form.
	<wide>		A wide (or zenkaku) compatibility character.
	<narrow>	A narrow (or hankaku) compatibility character.
	<small>		A small variant form (CNS compatibility).
	<square>	A CJK squared font variant.
	<fraction>	A vulgar fraction form.
	<compat>	Otherwise unspecified compatibility character.

Reminder:

There is a difference between decomposition and decomposition mapping. 
The decomposition mappings are defined in the Unicode Character Database, 
while the decomposition (also termed "full decomposition") is defined in 
Chapter 3 to use those mappings recursively.

The canonical decomposition is formed by recursively applying the canonical
mappings, then normalizing the order.

The compatibility decomposition is formed by recursively applying the
canonical and compatibility mappings, then normalizing the order.

CANONICAL COMBINING CLASSES

  0: Spacing, enclosing, reordrant, and surrounding
  1: Overlays and interior
  7: Nuktas
  8: Hiragana/Katakana voiced marks
  9: Viramas
 10: Start of fixed position classes
199: End of fixed position classes
200: Below left attached
202: Below attached
204: Below right attached
208: Left attached (reordrant around single base character)
210: Right attached
212: Above left attached
214: Above attached
216: Above right attached
218: Below left
220: Below
222: Below right
224: Left (reordrant around single base character)
226: Right
228: Above left
230: Above
232: Above right
234: Double above
240: Below (iota subscript)

Note: some of the combining classes in this list do not currently have
members but are specified here for completeness.

DECOMPOSITIONS AND NORMALIZATION

The Unicode Technical Report #15 Normalization Forms is found on
http://www.unicode.org/unicode/reports/tr15/.

That report specifies how the decompositions defined in the Unicode
Character Database are used to derive normalized forms of Unicode
text.

Note that as of the 2.1.9 update of the Unicode Character Database,
the decompositions in the UnicodeData.txt file can be used to recursively
derive the full decomposition in canonical order, without the need
to separately apply canonical reordering. However, canonical reordering
of combining character sequences must still be applied in decomposition
when normalizing source text which contains any combining marks.

CASE MAPPINGS

In addition to uppercase and lowercase, because of the inclusion of certain
composite characters for compatibility, such as "01F1;LATIN CAPITAL LETTER
DZ", there is a third case, called titlecase, which is used where the first
character of a word is to be capitalized (e.g. UPPERCASE, Titlecase,
lowercase). An example of such a character is "01F2;LATIN CAPITAL LETTER D
WITH SMALL LETTER Z".

The uppercase, titlecase and lowercase fields are only included for characters
that have a single corresponding character of that type. Composite characters
(such as "339D;SQUARE CM") that do not have a single corresponding character
of that type can be cased by decomposition.

The case mapping is an informative, default mapping. Case itself, on
the other hand, has normative status. Thus, for example, 0041 LATIN
CAPITAL LETTER A is normatively uppercase, but its lowercase mapping
the 0061 LATIN SMALL LETTER A is informative. The reason for this is
that case can be considered to be an inherent property of a particular
character (and is usually, but not always, derivable from the presence
of the terms "CAPITAL" or "SMALL" in the character name), but case
mappings between characters are occasionally influenced by local
conventions. For example, certain languages, such
as Turkish, German, French, or Greek may have small deviations from the
default mappings listed in the Unicode Character Database.

For compatibility with existing parsers, the Unicode Character Database
only contains case mappings for characters where they are one-to-one
mappings; it also omits information about locale-specific
case mappings. Information about these special cases can be found in
a separate data file, SpecialCasing.txt, which has been added starting with
the 2.1.8 update to the Unicode data files. SpecialCasing.txt contains
additional informative case mappings that are either not one-to-one or
which are context-sensitive.

PROPERTY INVARIANTS

Values in the Unicode Character Database are subject to correction
as errors are found; however, some characteristics of the categories
themselves can be considered invariants. Applications may wish
to take these invariants into account when choosing how to implement
character properties. The following is a partial list of known
invariants for the Unicode Character Database.

Database Fields

- The number of fields in the Unicode Character Database is fixed.

- The order of the fields is also fixed.

* Any additional information about character properties to be added
in the future will appear in separate data tables, rather than being
added on to the existing table or by subdivision or reinterpretation
of existing fields.

General Category

- There will never be more than 32 General Category values.

* It is very unlikely that the Unicode Technical Committee will subdivide 
the General Category partition any further, since that can cause 
implementations to misbehave. Because the General Category is limited
to 32 values, 5 bits can be used to represent the information, and a 32-bit
integer can be used as a bitmask to represent arbitrary sets of categories.

Combining Classes

- Combining classes are limited to the values 0 to 255.

* In practice, there are far fewer than 256 values used. Implementations
may take advantage of this fact for compression, since only the ordering
of the non-zero values matters for the Canonical Reordering Algorithm.
It is possible for up to 256 values to be used in the future; however,
UTC decisions in the future may restrict the number of values to 128,
since this has implementation advantages. [Signed bytes can be used
without widening to ints in Java, for example.]

- All characters other than those of General Category M* have the combining
  class 0.

* Currently, all characters other than those of General Category Mn have
the value 0. However, some characters of General Category Me or Mc may
be given non-zero values in the future.

* The precise values above the value 0 are not invariant--only the relative
ordering is considered normative.

Case

- Characters of type Lu, Lt, or Ll are called "cased". All characters
with an Upper, Lower, or Titlecase mapping are cased characters.

* However, characters with the General Categories of Lu, Ll, or Lt may
not always have case mappings, and case mappings may vary by locale.
(See ftp://ftp.unicode.org/Public/UNIDATA/SpecialCasing.txt).

Canonical Decomposition

- Canonical mappings are always in canonical order.

- Canonical mappings have only the first of a pair possibly further
decomposing.

- Canonical decompositions are "transparent" to other character data:

   BIDI(a) = BIDI(principal(canonicalDecomposition(a))
   Category(a) = Category(principal(canonicalDecomposition(a))
   CombiningClass(a) = CombiningClass(principal(canonicalDecomposition(a))

where principal(a) is the first character not of type Mn, or the first
character if all characters are of type Mn.

* However, because there are sometimes missing case pairs, and because
of some legacy characters, it is only generally true that:

   upper(canonicalDecomposition(a)) = canonicalDecomposition(upper(a))
   lower(canonicalDecomposition(a)) = canonicalDecomposition(lower(a))
   title(canonicalDecomposition(a)) = canonicalDecomposition(title(a))

MODIFICATION HISTORY

Modifications made for Version 2.1.9 of the Unicode Character Database:

* Corrected combining class for U+05AE HEBREW ACCENT ZINOR.
* Corrected combining class for U+20E1 COMBINING LEFT RIGHT ARROW ABOVE.
* Corrected combining class for U+0F35 and U+0F37 to 220.
* Corrected combining class for U+0F71 to 129.
* Added a decomposition for U+0F0C TIBETAN MARK DELIMITER TSHEG BSTAR.
* Added <compat> decompositions for several Greek symbol letters
  U+03D0..U+03D2, U+03D5, U+03D6, U+03F0..U+03F2.
* Removed <compat> decompositions from the conjoining jamo block:
  U+1100..U+11F8.
* Changes to decomposition mappings for some Tibetan vowels for consistency
  in normalization. (U+0F71, U+0F73, U+0F77, U+0F79, U+0F81)
* Updated the decomposition mappings for several Vietnamese characters
  with two diacritics (U+1EAC, U+1EAD, U+1EB6, U+1EB7, U+1EC6, U+1EC7,
  U+1ED8, U+1ED9), so that the recursive decomposition can be
  generated directly in canonically reordered form (not a normative
  change).
* Updated the decomposition mappings for several Arabic compatibility
  characters involving shadda (U+FC5E..U+FC62, U+FCF2..U+FCF4),
  and two Latin characters (U+1E1C, U+1E1D), so
  that the decompositions are generated directly in canonically
  reordered form (not a normative change).
* Changed BIDI category for: U+00A0 NO-BREAK SPACE, U+2007
  FIGURE SPACE, U+2028 LINE SEPARATOR.
* Changed BIDI category for extenders of General Category Lm:
  U+3005, U+3021..U+3035, U+FF9E, U+FF9F.
* Changed General Category and BIDI category for the Greek numeral
  signs: U+0374, U+0375.
* Corrected General Category for U+FFE8 HALFWIDTH FORMS LIGHT VERTICAL.
* Added Unicode 1.0 names for many Tibetan characters (informative).


Modifications made for Version 2.1.8 of the Unicode Character Database:

* Added combining class 240 for U+0345 COMBINING GREEK YPOGEGRAMMENI
	so that decompositions involving iota subscript are derivable
	directly in canonically reordered form; this also has a bearing
	on simplification of casing of polytonic Greek.
* Changes in decompositions related to Greek tonos. These result from
	the clarification that monotonic Greek "tonos" should be
	equated with U+0301 COMBINING ACUTE, rather than with
	U+030D COMBINING VERTICAL LINE ABOVE. (All Greek characters
	in the Greek block involving "tonos"; some Greek characters
	in the polytonic Greek in the 1FXX block.)
* Changed decompositions involving dialytika tonos. (U+0390, U+03B0)
* Changed ternary decompositions to binary. (U+0CCB, U+FB2C, U+FB2D)
	These changes simplify normalization.
* Removed canonical decomposition for Latin Candrabindu. (U+0310)
* Corrected error in canonical decomposition for U+1FF4.
* Added compatibility decompositions to clarify collation tables.
	(U+2100, U+2101, U+2105, U+2106, U+1E9A)
* A series of general category changes to assist the convergence of
	of Unicode definition of identifier with ISO TR 10176:
	So > Lo: U+0950, U+0AD0, U+0F00, U+0F88..U+0F8B
	Po > Lo: U+0E2F, U+0EAF, U+3006
	Lm > Sk: U+309B, U+309C
	Po > Pc: U+30FB, U+FF65
	Ps/Pe > Mn: U+0F3E, U+0F3F
* A series of bidi property changes for consistency.
	L > ET: U+09F2, U+09F3
	ON > L: U+3007
	L > ON: U+0F3A..U+0F3D, U+037E, U+0387
* Add case mapping: U+01A6 <-> U+0280
* Updated symmetric swapping value for guillemets: U+00AB, U+00BB,
	U+2039, U+203A.
* Changes to combining class values. Most Indic fixed position class
	non-spacing marks were changed to combining class 0. This fixes
	some inconsistencies in how canonical reordering would apply
	to Indic scripts, including Tibetan. Indic interacting top/bottom
	fixed position classes were merged into single (non-zero)
	classes as part of this change. Tibetan subjoined consonants
	are changed from combining class 6 to combining class 0.
	Thai pinthu (U+0E3A) moved to combining class 9. Moved two
	Devanagari stress marks into generic above and below
	combining classes (U+0951, U+0952).
* Corrected placement of semicolon near symmetric swapping field.
	(U+FA0E, etc., scattered positions to U+FA29)

[Note that Versions 2.1.6 and 2.1.7 of the Unicode Character
Database were for internal change tracking only, and were never
finally approved for public release.]

Modifications made for Version 2.1.5 of the Unicode Character Database:

* Changed decomposition for U+FF9E and U+FF9F so that correct collation
	weighting will automatically result from the canonical
	equivalences.
* Removed canonical decompositions for U+04D4, U+04D5, U+04D8, U+04D9,
	U+04E0, U+04E1, U+04E8, U+04E9 (the implication being that
	no canonical equivalence is claimed between these 8 characters
	and similar Latin letters), and updated 4 canonical decompositions
	for U+04DB, U+04DC, U+04EA, U+04EB to reflect the implied
	difference in the base character.
* Added Pi, and Pf categories and assigned the relevant quotation
        marks to those categories, based on the Unicode Technical
        Corrigendum on Quotation Characters.
* Updating of many bidi properties, following the advice of the ad hoc
	committee on bidi, and to make the bidi properties of compatibility
	characters more consistent.
* Changed category of several Tibetan characters: U+0F3E, U+0F3F,
	U+0F88..U+0F8B to make them non-combining, reflecting the
	combined opinion of Tibetan experts.
* Added case mapping for U+03F2.
* Corrected case mapping for U+0275.
* Added titlecase mappings for U+03D0, U+03D1, U+03D5, U+03D6, U+03F0..
	U+03F2.
* Corrected compatibility label for U+2121.
* Add specific entries for all the CJK compatibility ideographs,
	U+F900..U+FA2D, so the canonical decomposition for each
	(the URO character it is equivalent to) can be carried
	in the database.

[Note that Versions 2.1.3 and 2.1.4 of the Unicode Character
Database were for internal change tracking only, and were never
finally approved for public release.]

Modifications made in updating the Unicode Character Database to
Version 2.1.2 for the Unicode Standard, Version 2.1 (from Version 2.0) are:

* Added two characters (U+20AC and U+FFFC).
* Amended bidi properties for U+0026, U+002E, U+0040, U+2007.
* Corrected case mappings for U+018E, U+019F, U+01DD, U+0258, U+0275,
	U+03C2, U+1E9B.
* Changed combining order class for U+0F71.
* Corrected canonical decompositions for U+0F73, U+1FBE.
* Changed decomposition for U+FB1F from compatibility to canonical.
* Added compatibility decompositions for U+FBE8, U+FBE9, U+FBF9..U+FBFB.
* Corrected compatibility decompositions for U+2469, U+246A, U+3358.


Some of the modifications made in updating the Unicode Character Database
to Version 2.0.14 for the Unicode Standard, Version 2.0 are:

* Fixed decompositions with TONOS to use correct NSM: 030D.
* Removed old Hangul Syllables; mapping to new characters are
	in a separate table.
* Marked compatibility decompositions with additional tags.
* Changed old tag names for clarity.
* Revision of decompositions to use first-level decomposition, instead
	of maximal decomposition.
* Correction of all known errors in decompositions from earlier versions.
* Added control code names (as old Unicode names).
* Added Hangul Jamo decompositions.
* Added Number category to match properties list in book.
* Fixed categories of Koranic Arabic marks.
* Fixed categories of precomposed characters to match decomposition where possible.
* Added Hebrew cantillation marks and the Tibetan script.
* Added place holders for ranges such as CJK Ideographic Area and the
	Private Use Area.
* Added categories Me, Sk, Pc, Nl, Cs, Cf, and rectified a number of mistakes in
	the database.
