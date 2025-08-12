# UTS #39

## Modifying

To add or fix xidmodifications, look at source/removals.txt.

**For Identifier_Type values for new characters** see
https://github.com/unicode-org/unicodetools/blob/main/docs/security/id_type.md

To add or fix confusables, there are multiple source files. Many were
machine-generated, then tweaked. They have names like
source/confusables-winFonts.txt. The main file is confusables-source.txt.

***There is fairly complex processing for the confusables, so carefully diff the
results. Sometimes you may get an unexpected union of two equivalence sets.
Look at Testing below for help.***

[Sample PR for confusables changes](https://github.com/unicode-org/unicodetools/pull/841)

### File Format
There is a brief description of the file format at the top. 
Each line represents a mapping from a code point or set of code points to a sequence of one or more code points.

For example:
```
0021 ;  01C3    # ( ! → ǃ) EXCLAMATION MARK → LATIN LETTER RETROFLEX CLICK
```

The ordering of characters doesn't matter. 
So it doesn't matter whether you have the above line, or
```
01C3 ; 0021    # ( ǃ → !) LATIN LETTER RETROFLEX CLICK → EXCLAMATION MARK
```
It also doesn't matter if you have identical lines; the second one will be a NOOP.

The mappings are used to generate equivalence classes.
From each equivalence class, one representative member will be chosen,
and in the resulting data file, all the other characters will map to that representative.
Because of transitivity, the equivalence class will tend to be somewhat looser than expected.

We've discussed possible future enhancements:
- Have a second, narrower mapping that is more exact.
- Allow for mappings from sequences to sequences (instead of just code points to sequences).
- Provide for context, perhaps like the Transform rules. 
  Eg [x { a } y → A](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5B%3Aarabic_type%3A%5D&g=&i=)

## Before generating

First, in CLDR, update the script metadata:
http://cldr.unicode.org/development/updating-codes/updating-script-metadata

The identifier type & status take this data into account.

We need a recent CLDR repo snapshot, and update our Maven dependency to use that.
When there is a blocker for updating our CLDR dependency, we sometimes need to hack new script data into our tools code.

## Generating

Fix the version string (which will appear inside GenerateConfusables.java) and
the REVISION (which will match the new directory).

The version/revision strings are shared with other tools; no need to set them separately.

Run GenerateConfusables -c -b to generate the files. They will appear in two places.

*   *for posting, after review*:
    *    {Generated}/security/11.0.0/*
*   reformatted source, log
    *   $UNICODETOOLS_DIR/data/security/11.0.0/* *including log.txt*

The TestSecurity.java test is part of the unit test suite, run by a github CI.
It verifies that the confusable mappings are idempotent.

Copy the following from the output directory to the top level of the revision directory, and check in. Omit files where only the time stamp changes, unless this is the first set of files in a year.

*   confusables.txt
*   confusablesSummary.txt
*   confusablesWholeScript.txt
*   intentional.txt
*   ReadMe.txt
*   xidmodifications.txt

### Review

Review the mappings to make sure that there are no surprises.
The biggest issue is if two equivalence classes are mistakenly joined. 
For example, if you map b to d, then that will join the equivalence class for b with that of d.

### IdentifierStatus.txt & IdentifierType.txt

Markus 2020-feb-07 for Unicode 13.0:

*   Mostly same as above for GenerateConfusables but for these files I ran
    IdentifierInfo.java.
    *   org.unicode.text.UCD.IdentifierInfo
    *   :point_right: **Note**: When you run GenerateConfusables it also invokes
        IdentifierInfo.
*   The version/revision strings are shared with other tools; no need to set
    them separately.

### Common problems

You may see Identifier_Type=Recommended for characters/scripts/blocks that should not be recommended.
For example, the initial generation for Unicode 14 "recommended" Znamenny combining marks.
Add these to unicodetools/data/security/dev/data/source/removals.txt.
You can use block properties like
```
\p{block=Znamenny_Musical_Notation} ; technical
```

## Stability

We should preserve the target from old versions wherever possible. For example,
when the 6.3.0 files were first done, the following reversed order:
```
0259 ;  01DD ;  MA      # ( ə → ǝ ) LATIN SMALL LETTER SCHWA → LATIN SMALL LETTER TURNED E      #
```

That was because the LATIN SMALL LETTER TURNED E changed identifier status (to
become better). Since stability of the ordering is important, that was fixed
with the following change.
```
// EXCEPTIONAL CASES
// added to preserve source-target ordering in output.

lowerIsBetter.put('\u0259', MARK_NFC);
```

Where `Mark_NFC` was the former status. At some point, the code should be modified
to read the older version of the file, and favor characters that were there as
targets, but for now there are few enough of these that it is simple enough to
just add them to this list.

## Testing

After making any changes:

1.  Look at the log.txt file to see if there are any problems recorded there.
2.  Examine the formatted-xxx.txt for the confusables-xxx.txt that you modified.
3.  Review the differences between the generated files and the old versions.
4.  The summary file is often the most useful.

Because of transitive closure, it is sometimes tricky to track down why two
items are marked as confusable. The transitive closure not only does x ~ y, y ~
z, therefore x ~ z, but also handled substrings. So if x ~ y, then ax ~ ay. You
can end up with conflicts, like if you have x => "", and someplace else x => y,
or if you have x ~ xy (and y !~ "").

In confusables.txt, each line that is the product of transitive closure shows
you a path after a second #.
```
248F ;  0038 005F ;     SA      #\* ( ⒏ → 8_ ) DIGIT EIGHT FULL STOP → DIGIT EIGHT, LOW LINE    # →8.→
```

Find the link in the chain that shouldn't be there. Sometimes that is because of
a substring mapping. In the above case, it is mapping _ to .

Then search through the source/ for that character, to see what is happening.
Sometimes the formatted-xxx.txt is easier to search, since it has both the hex
and the character.

Searching for a regex expression that contains both the literal characters and
the hex is useful. For example, if you see the line:
```
#       ර       ?

(‎ ර ‎) 0DBB     SINHALA LETTER RAYANNA

←       (‎ ? ‎) 0DEE     SINHALA LITH DIGIT EIGHT
```

Then do a regex search in /data/source on `[ර?]|ODBB|ODEE`

Some problems can arise when the NFKC form is very different, like for:
```
    || cp == '﬩'  || cp == '︒'
```

In those cases, modify getSkipNFKD.

Other problems can arise from:

1.  Incorrect syntax, eg `[1234 1235]` instead of `[\u1234 \u1235]`
2.  Illegal containment. If you have a~ab, you'd get a circular closure, or if
    you have x => "", and someplace else x => y.
3.  Lowercase or too-short hex codes. 1a is interpreted as `\u0031\u0061`, not
    as `\u001A`.

Illegal containment: U+0645 ARABIC LETTER MEEM, U+062D ARABIC LETTER HAH, U+005F
LOW LINE overlaps U+0645 ARABIC LETTER MEEM, U+062D ARABIC LETTER HAH

from U+0645 ARABIC LETTER MEEM, U+062D ARABIC LETTER HAH, U+0640 ARABIC TATWEEL

with reason `[[arabic]] plus [[arabic]→&#x1eef0;, [arabic]]`

## Posting

The security files are published together with other data files for beta & final, see [Data Files Workflow](../data-workflow.md).
