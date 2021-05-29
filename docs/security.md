# UTS #39

## Modifying

Create new revision directory, such as .../unicodetools/data/security/6.3.0. The
folder will match the version of the UCD used (perhaps with an incrementing 3rd
field).

*   As usual, use `git cp` to copy the previous directory to the new one. Do not
    just "mkdir" and copy the files!

To add or fix xidmodifications, look at source/removals.txt.

To add or fix confusables, there are multiple source files. Many were
machine-generated, then tweaked. They have names like
source/confusables-winFonts.txt. The main file is confusables-source.txt.

***There is fairly complex processing for the confusables, so carefully diff the
results. Sometimes you may get an unexpected union of two equivalence sets. Look
at Testing below for help.***

Look at the following spreadsheets / bugs to see if there are any additional
suggestions.

*   **[Confusable
    Suggestions](https://docs.google.com/spreadsheet/ccc?key=0ArRWBHdd5mx-dHRXelRVbXRYSVp2QTNDdTBlV1I5X1E&usp=drive_web#gid=0)**
*   **[Identifier Restriction
    Suggestions](https://docs.google.com/spreadsheet/ccc?key=0ArRWBHdd5mx-dEJJWkdzZzk4cDRYbEVLTmhraGN0Q3c&usp=drive_web#gid=0)**
*   *[Unicode
    Bugs](http://www.unicode.org/edcom/bugtrack/query?status=accepted&status=assigned&status=new&status=reopened&group=component&order=priority&col=id&col=summary&col=status&col=type&col=priority&col=milestone&col=component&owner=mark&report=10)
    (under TR #36/39)*\
    :construction: **TODO**: That Trac instance is gone.
    Markus thinks we decided that there was nothing useful in it,
    and deleted it without saving data. Check with Mark.

If so, assess and add to confusables-source.txt—*if needed.*

Then in the spreadsheets, move the "new stuff" line to the end.

## Generating

Fix the version string (which will appear inside GenerateConfusables.java) and
the REVISION (which will match the new directory).

The version/revision strings are shared with other tools; no need to set them separately.

Run GenerateConfusables -c -b to generate the files. They will appear in two places.

*   *for posting, after review*:
    *    {Generated}/security/11.0.0/*
*   reformatted source, log
    *   $UNICODETOOLS_DIR/data/security/11.0.0/* *including log.txt*

**Run TestSecurity to verify that the confusable mappings are idempotent!**

With the same VM arguments as the generator.

Copy the following from the output directory to the top level of the revision directory:

*   confusables.txt
*   confusablesSummary.txt
*   confusablesWholeScript.txt
*   intentional.txt
*   ReadMe.txt
*   xidmodifications.txt

### IdentifierStatus.txt & IdentifierType.txt

Markus 2020-feb-07 for Unicode 13.0:

*   Mostly same as above for GenerateConfusables but for these files I ran
    IdentifierInfo.java.
    *   org.unicode.text.UCD.IdentifierInfo
    *   :point_right: **Note**: When you run GenerateConfusables it also invokes
        IdentifierInfo.
*   The version/revision strings are shared with other tools; no need to set
    them separately.

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

Once you've resolved all the problems, copy certain generated files to
https://www.unicode.org/Public/security/beta/

*   confusables.txt
*   confusablesSummary.txt
*   confusablesWholeScript.txt
*   intentional.txt
*   ReadMe.txt
*   xidmodifications.txt

Check that the files are copied to https://www.unicode.org/Public/security/beta/.
