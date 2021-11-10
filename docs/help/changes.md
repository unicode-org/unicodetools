# Changes

The Unicode Utilities have been modified to support both properties from the
released version of Unicode (via ICU) and from the new Unicode beta.

To get the beta version of the property, insert β *after* the property name.
Examples:

| `\p{Word_Break=ALetter}` | Released version of Unicode |
| `\p{Word_Breakβ=ALetter}` | Beta version of Unicode     |


For example, to see additions to that property value in the beta version, use:

<center>

[`\p{Word_Breakβ=ALetter}-\\p{Word_Break=ALetter}`](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7BWord_Break%CE%B2%3DALetter%7D-%5Cp%7BWord_Break%3DALetter%7D&g=&i=)

</center>


## Caveats

The support is not complete done, and there are some known problems.

1.  Some properties are not supported in beta versions. See
    <https://util.unicode.org/UnicodeJsps/properties.jsp>
    for the list.
2.  When characters are listed, the new blocks and subheads don't show up.
3.  If you use a property that has a β version but no ICU version, you get no
    error: just an empty listing.
4.  The beta properties don't yet have the "shorthands" for cases like \\p{Lu}.
    So make sure the property is listed, eg \\p{gcβ=Lu}
    1.  Example:
        [`\p{gcβ=Lu}-\\p{gc=Lu}`](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7Bgc%CE%B2%3DLu%7D-%5Cp%7Bgc%3DLu%7D&g=&i=)
5.  Tools for segmentation, etc. use the release properties; there isn't a way
    to have them use the beta properties.
6.  There are probably others...

If you find a problem, please file a ticket at
<https://cldr.unicode.org/index/bug-reports>: make sure to start the summary with
"Unicode Utilities: "
