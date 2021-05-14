# Starting With New Props

You can start a new release with the /props folder. Here's how.

Copy old folders into new, for any that don't exist, eg.

> unicodetools/data/ucd/10.0.0-Update => 11.0.0-Update

> unicodetools/data/security/10.0.0 => 11.0.0

> unicodetools/data/idna/10.0.0 => 11.0.0

> unicodetools/data/emoji/10.0.0 => 11.0.0

Update the properties and values to add new aliases. You will always add new Age
value, and new Blocks.

/unicodetools/data/ucd/11.0.0-Update/PropertyValueAliases.txt

Look at http://www.unicode.org/reports/tr38/proposed.html to see if there are
more properties there. These take modifying a number of files in
/unicodetools/org/unicode/props/GenerateEnums.java. Here is an example:

> ExtraPropertyAliases.txt

> // add the property with the short(!) value having an additional "cj"

> cjkJinmeiyoKanji ; kJinmeiyoKanji

> IndexPropertyRegex.txt

> // look at http://www.unicode.org/reports/tr38/proposed.html#kJinmeiyoKanji

> // if Delimiter is not N/A, use MULTI_VALUED; then copy in the regex as the
> 3rd field

> kJinmeiyoKanji ;                 MULTI_VALUED. ;
> (20\[0-9\]{2})(:U\\+2?\[0-9A-F\]{4})?

> IndexUnicodeProperties.txt

> // Use the category in
> http://www.unicode.org/reports/tr38/proposed.html#kJinmeiyoKanji to get the
> file.

> Unihan_OtherMappings ; kJinmeiyoKanji

> IndexPropertyRegexRevised.txt // skip this, in progress

> Multivalued.txt // skip this, old

Run /unicodetools/org/unicode/props/GenerateEnums.java, which rewrites

> /unicodetools/org/unicode/props/UcdProperty.java

> /unicodetools/org/unicode/props/UcdPropertyValues.java

Check diffs.

Delete Generated/BIN/11.0.0.0 (you have to do this as you are bootstrapping, if
you have to make changes)

Run /unicodetools/org/unicode/propstest/ListProps.java

It will list properties, like:

➕       Numeric_Value   Type:   Numeric Status: Normative       Card:
Singleton       DefVal: NaN     Scope:  Numeric Origin: UCD     Values: \[-1/2,
0, 1/160, 1/40, 3/80, 1/20, 1/16, 1/12, 1/10, 1/9, 1/8, 1/7, 3/20, 1/6, 3/16,
1/5, 1/4, 1/3, 3/8, 2/5, 5/12, 1/2, 7/12\], …

➕       kAccountingNumeric      Type:   Numeric Status: Informative     Card:
Singleton       DefVal: NaN     Scope:  Numeric Origin: UCD     Values: \[1000,
5, 100, 3, 1, 2, 10, 8, 7, 9, 4, 10000, 6, NaN\]

➕       kOtherNumeric   Type:   Numeric Status: Informative     Card:
Singleton       DefVal: NaN     Scope:  Numeric Origin: UCD     Values: \[5, 2,
7, 4, 10, 3, 20, 30, 40, 1, 9, 6, NaN\]

➕       kPrimaryNumeric Type:   Numeric Status: Informative     Card:
Singleton       DefVal: NaN     Scope:  Numeric Origin: UCD     Values: \[1, 7,
10000, 3, 9, 2, 5, 100000000, 1000000000000, 8, 6, 10, 1000, 4, 100, 0, NaN\]

Run /unicodetools/org/unicode/propstest/CheckProperties.java to see what changed
and sanity check.

Fix any failures, such as:

Exception in thread "main" com.ibm.icu.util.ICUException:
kBigFive(/Users/markdavis/Documents/workspace/unicodetools/data/ucd/11.0.0-Update/Unihan/Unihan_OtherMappings.txt)

at
org.unicode.props.IndexUnicodeProperties.load(IndexUnicodeProperties.java:411)

at org.unicode.propstest.ListProps.main(ListProps.java:75)

Caused by: java.lang.IllegalArgumentException: \[U+3447, kTGH, 2013:6602\]

at
org.unicode.props.PropertyParsingInfo.parseSourceFile(PropertyParsingInfo.java:490)

at
org.unicode.props.IndexUnicodeProperties.load(IndexUnicodeProperties.java:408)

... 1 more

Caused by: java.lang.NullPointerException

at
org.unicode.props.PropertyParsingInfo.parseSourceFile(PropertyParsingInfo.java:488)

... 2 more
