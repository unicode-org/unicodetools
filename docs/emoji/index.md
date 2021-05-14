# UTS #51

## Data Generation

1.  Get the UCD tools updated (see [Building Unicode Tools](../index.md))
2.  \[If New version\]
    1.  Set **the version as per instructions in** Emoji.java.
    2.  Create a new folder **{unicode-draft}**/emoji/**X**.0/
    3.  Copy in the ReadMe.txt from the last version, but add the word "draft"
        in front of "emoji".
3.  Run **{unicodetools/org/unicode/text}**/tools/GenerateEmoji.java
    1.  For VM arguments see the section below.
4.  That will generate updated files in one of two places, according to the
    setting Emoji.IS_BETA.
    1.  **CURRENT VERSION**
        1.  **{unicode-draft}**/Public/emoji/**CURRENT**.0/\*
        2.  **{unicode-draft}**/emoji/**charts-CURRENT**.0/\*
    2.  **BETA VERSION**
        1.  **{unicode-draft}**/Public/emoji/**NEXT.0**/\*
        2.  **{unicode-draft}**/emoji/**charts-NEXT.0**/\*
5.  Special small versions of the charts are in corresponding directories like:
    1.  **{unicode-draft}**/emoji/🏴charts-11.0/emoji-list.html
6.  Sanity-check them, diff against old files, and check in.
    1.  Don't use Eclipse diff for the big chart files: they are too big for it.

### GenerateEmoji: VM Arguments

Eclipse > Debug Configurations > Arguments --> lower box "VM Arguments"

Markus VM arguments for in-progress Unicode 13/emoji 13:
```
-ea
-Demoji-beta=true
-DUVERSION=13.0.0
-DSVN_WORKSPACE=/usr/local/google/home/mscherer/svn.unitools/trunk
-DOTHER_WORKSPACE=/usr/local/google/home/mscherer/svn.unitools
-DUNICODETOOLS_DIR=/usr/local/google/home/mscherer/svn.unitools/trunk
-DUNICODE_DRAFT_DIR=/usr/local/google/home/mscherer/svn.unidraft/trunk
-DUCD_DIR=/usr/local/google/home/mscherer/svn.unitools/trunk/data
-DCLDR_DIR=/usr/local/google/home/mscherer/cldr/uni/src
```

## File Locations in the repo

:construction: **TODO**: Work with Mark on working replacements for "draft" URLs.

1.  https://www.unicode.org/draft/Public/emoji/X.0/*
2.  https://www.unicode.org/draft/emoji/charts-X.0/*

## Posting (Rick)

Once you get the OK from Peter/Mark that the files look good, you can start the
rollout.

1.  Get the announcement ready on the blog site, and let Peter/Mark know to
    review.
2.  Post from the *File Locations in the repo* (see above) to the corresponding
    public directories.
    1.  https://www.unicode.org/emoji/charts-X.0/*
        1.  skip internal directories
        2.  you can copy the flag directories to a location for sanity checking.
    2.  https://www.unicode.org/Public/emoji/X.0/
        1.  ReadMe.txt
        2.  emoji-data.txt
        3.  emoji-sequences.txt
        4.  emoji-zwj-sequences.txt
        5.  **(and others!)**
    3.  https://www.unicode.org/reports/tr51/
        1.  tr51.html
            1.  Copy up as a specific version, like tr51-13.html
            2.  Also copy and rename to either **proposed.html** or to
                **index.html**, depending on release status.
        2.  images/\*
3.  Sanity check the files, now that they are on the regular web site, and in
    the right locations. If there are serious problems, then notify Mark/Peter
4.  Otherwise you are golden, and you can post the announcement.

## Release Process (Rick)

*   Post the final files. Right now:
    *   we are doing emoji/charts, and Public/emoji early in the year
    *   tr51 sync'ed with the Unicode standard
    *   *That will change in 2019, when it will all be at the same time.*
*   Modify **{unicode-draft}**/emoji/**X**.0/ReadMe.txt to remove the word
    'draft' (if not already done).
*   Check that there is no "β" on the release charts, once posted, eg
    *   http://www.unicode.org/emoji/charts-11.0/
*   Change redirections. Examples below use 11.0, but it should be whatever is
    current:
    *   https://www.unicode.org/emoji/charts/ → https://www.unicode.org/emoji/charts-11.0/
    *   https://www.unicode.org/Public/emoji/latest/ →
        https://www.unicode.org/Public/emoji/11.0/
