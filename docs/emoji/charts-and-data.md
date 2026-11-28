# Posting Emoji Charts and Data

The following use 15.0 as the example version. Change that to whatever the version is.

## Posting Emoji beta charts

Check and post the chart files that have changed in:

https://github.com/unicode-org/emoji/tree/main/docs/emoji/

That is:

*   First sanity-check the charts.
    *   You can do a link check on the "flagged" charts, since they are built to be small.
    *   Visually inspect them also; 
        *   make sure that the new characters are in, 
        *   version numbers are right,
        *   and images are present (at least one per row)
*   https://github.com/unicode-org/emoji/tree/main/docs/emoji/charts-15.0
    *   All files with .html
    *   Skip other files, and /internal/
*   https://github.com/unicode-org/emoji/tree/main/docs/emoji/future
    *   All files with .html

NOTE: make sure that all the emoji images are in a proper directory with proper names:
*  **not** proposed/proposed_...png, but rather
*  sample/sample_...png, or
*  android/androic_...png, or
*  ...

[Not sure this comment is still relevant]
First time posting a new set of beta charts, make sure that charts-beta is a
newly created symlink to charts-BBBB.

## Posting Emoji beta data

Copy all files under https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/emoji/15.0 files to Public
*   First sanity-check the files.
    *   The Readme.txt should say "draft" in the contents, and have the version 15.0.
    *   The other files should have the new 15.0 characters and sequences.
    *   The previous characters should be unchanged, except that the CLDR names may change.
*   Don't copy anything under the /internal/ directory.
*   Copy the following to https://unicode.org/Public/emoji/15.0/
    *   ReadMe.txt
    *   emoji-sequences.txt
    *   emoji-test.txt
    *   emoji-zwj-sequences.txt 
*   Copy the following to https://unicode.org/Public/15.0.0/ucd/emoji/
    *  ReadMe-ucd-emoji.txt // and change the name to drop the "-ucd-emoji"	 
    *  emoji-data.txt
    *  emoji-variation-sequences.txt
