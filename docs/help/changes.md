# Changes

The Unicode Utilities have been modified to support both properties from the
released version of Unicode (via ICU) and from the new Unicode beta.

To get the beta version of the property, insert `Uβ:` *before* the property name.
The explicit version number for the β can be used;
the resulting property is then only valid when that specific β is current.
Examples:

| `\p{Word_Break=ALetter}` | Released version of Unicode. |
| `\p{Uβ:Word_Break=ALetter}` | Beta version of Unicode; error outside of beta review. |
| `\p{U16β:Word_Break=ALetter}` | Beta version of Unicode 16.0; error during the beta review of any other version. |


For example, to see additions to that property value in the beta version, use:

<center>

[`\p{Uβ:Word_Break=ALetter}-\p{Word_Break=ALetter}`](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7BU%CE%B2%3AWord_Break%3DALetter%7D-%5Cp%7BWord_Break%3DALetter%7D&g=&i=)

</center>


## Caveats

The support is not completely done, and there are some known problems.

1.  The General_Category groupings such as \\p{Uβ:L} are not correctly implemented.
    Only actual values, such as \\p{Uβ:Lu} etc., work.
2.  Tools for segmentation, etc. use the release properties; there isn't a way
    to have them use the beta properties.
3.  There are probably others...

If you find a problem, please file a ticket at
https://github.com/unicode-org/unicodetools/issues.

[Back to Unicode Utilities Help Home](index)