# list-unicodeset - Manipulate sets of Unicode characters

[TOC]

UnicodeSets use regular-expression syntax to allow for arbitrary set operations
(Union, Intersection, Difference) on sets of Unicode characters. The base sets
can be specified explicitly, such as `[a-m w-z]`, or using a combinations of
Unicode Properties such as the following, for **the Arabic script characters
that have a canonical decomposition:**

\[\[:script=arabic:\]&\[:decompositiontype=canonical:\]\]

## Operation

Enter a UnicodeSet into the **Input** box, and hit **Show Set**. You can also
choose certain combinations of options for display, such as abbreviated or not.

The values you use are encapsulated into a URL for reference, such as

<http://unicode.org/cldr/utility/list-unicodeset.jsp?a=\\p{sc:Greek}>

If you add properties to the **Group By** box, you can sort the results by
property values. For example, if you set it to "General_Category Numeric_Value"
(or the short form "gc nv"), you'll see the results sorted first by the general
category of the characters, and then by the numeric value.

## **Syntax**

**UnicodeSets are defined according to the description on [UTS #35: Locale Data
Markup Language (LDML)](http://www.unicode.org/reports/tr35/), but has some
useful extensions in these online demos.**

**Properties can be specified either with Perl-style notation
(`\p{script=arabic}`) or with POSIX-style notation (`[:script=arabic:]`).
Properties and values can either use a long form (like "script") or a short form
(like "sc").**

**No argument is equivalent to "Yes"; mostly useful with binary properties, like
\\p{isLowercase}!**

### **Convenience**

The following examples illustrate the syntax with a particular property, value
pair: the property *age* and the value *3.2:*

The : can be used in the place of =. (Mostly because : doesn't require
percent-encoding in URLs.)

*   \\p{age:3.2} and \[:age:3.2:\]

The Perl and Posix syntax for negations are \\P{...} and \[:^...:\],
respectively. The characters ≠ and ! are added for convenience:

*   \\p{age≠3.2} and \[:age≠3.2:\]
*   \\p{age!=3.2} and \[:age!=3.2:\]
*   \\p{age!:3.2} and \[:age!=3.2:\]

### **Regular Expressions**

For the *name* property, regular expressions can be used for the value, enclosed
in /.../. For example in the following expression, the first term will select
all those Unicode characters whose names contain "CJK". The rest of the
expression will then subtract the ideographic characters, showing that these can
be used in arbitrary combinations.

*   `[[[:name=/CJK/:]-[:ideographic:]]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B%5B:name=/CJK/:%5D-%5B:ideographic:%5D%5D)`
    - the set of all characters with names that contain CJK that are not
    Ideographic
*   `[[:name=/\bDOT$/:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:name=/%5CbDOT$/:%5D)`
    - the set of all characters with names that end with the word DOT
*   `[[:block=/(?i)arab/:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:block=/(?i)arab/:%5D)`
    - the set of all characters in blocks that contain the sequence of letters
    "arab" (case-insensitive)
*   `[[:toNFKC=/\./:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toNFKC=/%5C./:%5D)`
    - the set of all characters with toNFKC values that contain a literal period

Some particularly useful regex features are:

*   \\b means a word break, ^ means front of the string, and $ means end. So
    /^DOT\\b/ means the word DOT at the start.
*   (?i) means case-insensitive matching.

***Caveats:***

1.  The regex uses the standard [Java
    Pattern](http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html).
    In particular, it does not have the extended functions in UnicodeSet, nor is
    it up-to-date with the latest Unicode. So be aware that you shouldn't depend
    on properties inside of the /.../ pattern.
2.  If you do use properties, then use \[:...:\] syntax on the outside, such as:
    *   [\[:block=/\\p{Digit}/:\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:block:/%5Cp%7BDigit%7D/:%5D&g=)
3.  The Unassigned, Surrogate, and Private Use code points are skipped in the
    Regex comparison, so \[:Block=/Aegean_Numbers/:\] returns a different number
    of characters than \[:Block=Aegean_Numbers:\], because it skips Unassigned
    code points.
4.  None of the normal "loose matching" is enabled. So \[:Block=aegeannumbers:\]
    works, but \[:Block=/aegeannumbers/:\] fails -- you have to use
    \[:Block=/Aegean_Numbers/:\] or \[:Block=/(?i)aegean_numbers/:\].

### Property Comparison

Property values can be compared to those for other properties, using the syntax
@...@. For example:

*   Find the characters for which IDNA2003 is not the same as UTS46:
    [\\p{idna2003!=@uts46@}](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5Cp%7Bidna2003!%3D@uts46@%7D)
*   The same thing, but limited to Unicode 3.2:
    [\\p{idna2003!=@uts46@}&\\p{age=3.2}](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5Cp%7Bidna2003!%3D@uts46@%7D%26%5Cp%7Bage%3D3.2%7D&g=)

There is a special property "cp" that returns the code point itself. For
example:

*   Find the characters whose lowercase is different:
    [\\p{toLowercase!=@cp@}](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5Cp%7BtoLowercase!%3D%40cp%40%7D&g=)

## **Available Properties**

You can see a full listing of the possible properties on
<http://unicode.org/cldr/utility/properties.jsp>. The standard Unicode
properties are supported, plus the extra ICU properties. There are some
additional properties just in this demo. The easiest way to see the properties
for a range of characters is to use a set like \[:Greek:\] in the Input, and
then set the Group By box to the property name.

### List

1.  **International Domain Names:**
    1.  idna2003, uts46, idna2008, idna2008c, plus the tranforms:
    2.  toIdna2003, toUTS46t (transitional form), toUTS46n (the normal form).
2.  **Security:**
    1.  usage (the xmod properties from the security mechanisms),
    2.  idr (identifier restrictions),
    3.  confusables
3.  **HanType:**
    1.  Hans (Simplified-only), Hant (Traditional-only), or Han (both) (based on
        Unicode properties)
4.  **Casing:**
    1.  toCaseFold, toLowerCase, toUpperCase, toTitleCase, toNFKC_CF
    2.  isCaseFolded, isUppercase, isLowercase, isTitlecase, isCased, isNFKC_CF
5.  **Normalization:**
    1.  toNFC, toNFD, toNFKD, toNFKC;
    2.  isNFC, isNFKC, isNFD, isNFKD
6.  **Informational:**
    1.  subhead (the subhead from the Unicode charts, simplified slightly to
        remove variations like plurals and use of terms like "Additional")
7.  **Misc:**
    1.  ASCII, ANY (matches any code point), BMP,
    2.  *emoji* (the emoji characters, both new and old)
8.  **Scripts:**
    1.  scs (the script extensions in Unicode 6.0 -- also adds HanType)
9.  **Encodings:**
    1.  enc_GBK, is_enc_GBK
    2.  (and a few other common encodings)
10. **Sorting:**
    1.  uca (the primary UCA weight -- after the CLDR transforms),
    2.  uca2 (the primary and secondary weights)

Normally, \\p{isX} is equivalent to \\p{toX=@cp@}. There are some exceptions and
missing cases.

Note: The Unassigned, Surrogate, and Private Use code points are skipped in the
generation of some of these sets.

The following provides details for some cases.

### Casing Properties

Unicode defines a number of string casing functions in *Section 3.13 Default
Case Algorithms*. These string functions can also be applied to single
characters.***Warning:*** the first three sets may be somewhat misleading:
isLowercase means that the character is the same as its lowercase version, which
includes *all uncased* characters. To get those characters that are *cased*
characters and lowercase, use
[`[[:isLowercase:]&[:isCased:]]`](http://cldr.unicode.org/unicode-utilities)

1.  The binary testing operations take no argument:
    *   `[[:isLowercase:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:isLowercase:%5D)`
    *   [`[:isUppercase:]`](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:isUppercase:%5D)
    *   [`[:isTitlecase:]`](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:isTitlecase:%5D)
    *   [`[:isCaseFolded:]`](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:isCaseFolded:%5D)
    *   `[[:isCased:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:isCased:%5D).`
2.  The string functions are also provided, and require an argument. For
    example:
    *   `[[:toLowercase=a:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toLowercase=a:%5D)
        `- the set of all characters X such that toLowercase(X) = a
    *   `[[:toCaseFold=a:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toCaseFold=a:%5D)`
    *   `[[:toUppercase=A:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toUppercase=A:%5D)`
    *   `[[:toTitlecase=A:]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toTitlecase=A:%5D)`

    Note: The Unassigned, Surrogate, and Private Use code points are skipped in
    generation of the sets.

### **Normalization Properties**

Unicode defines a number of string normalization functions UAX #15. These string
functions can also be applied to single characters.

*   [\[:toNFC=a:\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toNFC=a:%5D)
    - the set of all characters X such that toNFC(X) = a
*   [\[:toNFD=A\\u0300:\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toNFD=A%5Cu0300:%5D)
*   [\[:toNFKC=A:\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toNFKC=A:%5D)
*   [\[:toNFKD=A\\u0300:\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?a=%5B:toNFKD=A%5Cu0300:%5D)
