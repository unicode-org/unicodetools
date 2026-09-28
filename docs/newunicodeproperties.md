# New Unicode Properties

.../unicodetools/org/unicode/props

This is a set of revised Unicode property tools. Rather than the old-style tools
that were written a long time ago, when Java was much more primitive (and slow,
and had memory restrictions), this reads the Unicode data files and constructs
"modern" versions of the properties. Each Unicode property is represented by an
enum value, with property values backed by a UnicodeMap. The reading process is
data-driven, and uses regexes to check the values.

## Adding Non-Standard Properties

Occasionally you need to add a 'non-standard' property. Here's what to do, with
some examples of changes in the links.

*   Add properties to
    [ExtraPropertyAliases.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/src/main/resources/org/unicode/props/ExtraPropertyAliases.txt)
*   Add file and the field locations to
    [IndexUnicodeProperties.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/src/main/resources/org/unicode/props/IndexUnicodeProperties.txt)
*   Add @missing and enum values to
    [ExtraPropertyValueAliases.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/src/main/resources/org/unicode/props/ExtraPropertyValueAliases.txt)
*   Add regex values, etc. to
    [IndexPropertyRegex.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/src/main/resources/org/unicode/props/IndexPropertyRegex.txt)
*   If the file is in a different location, you'll have to modify
    [Utility.getMostRecentUnicodeDataFile.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/src/main/java/org/unicode/text/utility/Utility.java)
*   For an old example of adding properties see [commit 2ff83c6](https://github.com/unicode-org/unicodetools/commit/2ff83c6a0d0eef7286e98c3b94b3de538b44e404).
*   Ideally, add a test in TestProperties.

## Run GenerateEnums.java

*If you are building before the UCD tools have been completely updated to new
release X.Y.Z, you need to:*

1.  Ensure that PropertyAliases.txt and PropertyValueAliases.txt are updated for
    the new properties/values in data.ucd/X.Y.Z
2.  Make sure that DerivedAge is correct.
3.  Copy the previous version of data.idna/X⁻¹.Y.Z to data.idna/X.Y.Z
4.  Copy the last version of data.security/X⁻¹.Y.Z to data.security/X.Y.Z

This generates enums corresponding to the properties and property values. Do
this whenever the PropertyAliases.txt or PropertyValueAliases.txt files change.

It will regenerate the following files (see [commit 2ff83c6](https://github.com/unicode-org/unicodetools/commit/2ff83c6a0d0eef7286e98c3b94b3de538b44e404) for examples of changes):

*   [UcdProperty.java](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/org/unicode/props/UcdProperty.java)
*   [UcdPropertyValues.java](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/org/unicode/props/UcdPropertyValues.java)

Run UCD.Main to generate new PropertyAliases.txt and PropertyValueAliases.txt.

***Note***: For some properties and values, it is sufficient to add them to the
input PA.txt & PVA.txt files, run GenerateEnums and UCD.Main. Sometimes you need
to change additional .java files.

*   For a new ccc value, edit UCD_Names.java.
*   We should probably always edit UCD_Names.java & UCD_Types.java, in addition
    to the new mechanism, for as long as there are still call sites to the old
    stuff.
*   For a new *normative* Unihan property, edit PropertyAliases.txt,
    UCD.java, ToolUnicodePropertySource.java and IndexUnicodeProperties.txt.
*   For a new *informative* or *provisional* Unihan property, edit
    ExtraPropertyAliases.txt, not PropertyAliases.txt; and do not
    addFakeProperty() in ToolUnicodePropertySource.java (which keeps these out
    of the generated PA.txt/PVA.txt files).
    *   In particular, provisional properties should not show up in the standard
        PA.txt file, in case they are removed later. (stability)
*   Obsolete? If a Unihan property moves from one file to another, also add an
    override into IndexUnicodeProperties.txt.
    *   See kRSUnicode & kTotalStrokes for examples.
    *   This is probably no longer necessary now that we split the Unihan data
        into single-property files and parse those.

The properties can be directly compared, such as
```
if (prop == UcdProperty.Unicode_1_Name) {
    NOT_IN_ICU.add(prop.toString());
    return;
}
```

From the enum you can get the type, and the names, and create an enum from any
of the names.

To use the property values, call:
```
final IndexUnicodeProperties iup = IndexUnicodeProperties.make("6.2.0");
```

## **Internals**

When any property is accessed, the in-memory version is checked. If there is
none, then the on-disk version is checked (in Generated/BIN/x.y.z). If there is
none, then the right Unicode file(s) are accessed to build, and then the
property is cached on disk and in memory.

**IMPORTANT NOTE: If you change the files in ucd/, then you *must* delete the files in Generated/BIN/x.y.z**

## Checking XML properties

To test the XML properties from https://www.unicode.org/Public/XXX/ucdxml/

1.  Make sure that the following files from ftp://unicode.org/Public/xxx/ucdxml/
    are checked into <workspace>/unicodetools/data/ucdxml/xxx (unzipping as
    necessary).
    *   ucd.nounihan.grouped.xml
    *   ucd.unihan.grouped.xml
    *   ucdxml.readme.txt
2.  Ensure that Utility.searchPath is up-to-date.
3.  Then run (with no parameters) **CheckXmlProperties**

**NOTE: the following *(until the test is fixed)* are false differences:**
```
*FAIL* kAccountingNumeric with 1114086 errors.
*FAIL* kOtherNumeric with 1114082 errors.
*FAIL* kPrimaryNumeric with 1114095 errors.
*FAIL* kCompatibilityVariant with 1113110 errors.
*FAIL* Bidi_Paired_Bracket with 11 errors.
*FAIL* Name with 11 errors.
```

The problem is a difference in how missing values are handled.

## Checking Other Properties

For a general test of properties, run CheckProperties. You can supply any of the
following as parameters:
```
enum Action {SHOW, COMPARE, ICU, EMPTY, INFO, SPACES, DETAILS, DEFAULTS, JSON, NAMES}

enum Extent {SOME, ALL}
```

or a version, eg 6.2.0

The defaults are: COMPARE ALL {lastversion}

For example, ICU compares the ICU values.

**NOTE: false differences with:**

Age     «2.0»   ≠       «V2_0», etc.
