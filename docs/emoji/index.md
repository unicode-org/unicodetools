# UTS #51

## Data Generation

1.  Get the UCD tools updated (see [Building Unicode Tools](../index.md))
2.  \[If New version\]
    1.  Set **the version as per instructions in** the header of Emoji.java.
    2.  Create a new folder **{unicode-draft}**/emoji/**X**.0/
    3.  Copy in the ReadMe.txt from the last version, but add the word "draft"
        in front of "emoji".
    4. And add the images to the images repository.
    5. Make sure that docRegistry.txt is up to date.
      1. Look at the last line in docRegistry.txt, eg L2/20-153 ...
      2. Go to the https://www.unicode.org/L2/L-curdoc.htm
      3. Find the year, and copy the first column after L2/20-153 down to the last real line.
      3. Paste at the end of docRegistry.txt. Make sure there are 4 tab delimited columns.
      4. Repeat for any later years.
      5. NOTE: sometimes there are some glytches in the columns. Typically when there are multiple lines in a cell. You'll see that when a line doesn't start with L2/... In that case, fix the lines (typically by joining with previous line)
    6. *Note that the data files with the new emoji will not be generated until candidateData.txt has Status=Draft Candidate.*
3.  Run **{unicodetools/org/unicode/text}**/tools/GenerateEmoji.java
    1.  For VM arguments see the section below.
    2. Sometimes the candidateData.txt file will be malformed, and you'll see some errors. Fix them.
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
6.  Sanity-check data files, diffing against old files.
    1. Old data files will be in unicodetools/unicodetools/data/emoji/**CURRENT**.0/
    2. New data files in **{unicode-draft}**
    3. If all looks good, copy the new data files into will go into unicodetools/unicodetools/data/emoji/**NEXT**.0/
7. If you are not doing this the first time:
    1. Sanity check the new charts (Don't use Eclipse diff for the big chart files: they are too big for it.)
    2. Copy them into  https://github.com/unicode-org/emoji/tree/main/docs/emoji
      1. /**NEXT**.0/
      2. /future/


 \[If New version\]
Some things will not be fixed yet, so you have to take a second pass.

1. Update emojiOrdering.txt
Diff unicodetools/src/main/resources/org/unicode/tools/emoji/emojiOrdering.txt with the new 
unicodetools/data/emoji/**NEXT**/internal/emojiOrdering.txt
Copy in the new characters from internal/emojiOrdering.txt to emoji/emojiOrdering.txt. 
**Don't** remove the sets of ZWJ sequences like 👱‍♀ 👱‍♀️ 👱‍♂ 👱‍♂️ 👱🏻‍♀ 👱🏻‍♀️ 👱🏻‍♂ 👱🏻‍♂️ 👱🏼‍♀ 👱🏼‍♀️ 👱🏼‍♂ 👱🏼‍♂️ 👱🏽‍♀ 👱🏽‍♀️ ; those are still needed.

2. Run GenerateEmoji.java again.
If you missed one of the new characters you will probably get an error in building the EmojiOrder. 
This will update the generated files to have **NEXT** instead of E0.0. Example:
``
1F6DC         ; Emoji                # E0.0   [1] (🛜)       wireless
becomes
``
1F6DC         ; Emoji                # E15.0  [1] (🛜)       wireless
``
It will also add the new characters to emoji-test.txt (they will be missing in the first pass.)

3. Copy over the data files again
From: **{unicode-draft}**/Public/emoji/**NEXT.0**/\*
To: unicodetools/unicodetools/data/emoji/**NEXT**.0/

1. Go to step 7 above to copy the charts.
2. 

### CLDR
CLDR uses some files once the correct emoji-test.txt file is built. For details, see https://cldr.unicode.org/development/generate-emoji-paths

### GenerateEmoji: VM Arguments

Eclipse > Debug Configurations > Arguments --> lower box "VM Arguments"

In addition to the [regular VM arguments](../index.md) —

Markus VM arguments for in-progress Unicode 13/emoji 13:
```
-Demoji-beta=true
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
