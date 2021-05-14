# aac

[TOC]

## AacOrder.java

Once the emoji are finalized for new version of TR51, or there is a new version
of CLDR, run AacOrder.java to generate 3 new files which will be checked in.

Fix the versions at the top of the file, such as:

private static final VersionInfo VERSION = Emoji.VERSION12;

private static final VersionInfo UCD_VERSION = Emoji.VERSION12;

The emoji version will be ≥ the UCD version.

**Results:**

1.  <http://unicode.org/draft/consortium/aac-order-ranges.txt>
    *   Contains a list of all valid characters and strings for aac, in the
        proper order, and with names for emoji. (Names for other characters will
        just use the Unicode names.)
2.  <http://unicode.org/draft/consortium/aac-order-us.txt>
    *   Contains an expression that defines a UnicodeSet in C or Java. This can
        be used for validity checking.

Before committing, diff the files with the current file using SVN. Spot-check
that:

*   There are normally only new characters, or reorderings.
*   The new emoji in the release are present.
*   Any change in ordering is present.

### aac-order-ranges.txt

This file can be used both to determine AAC validity, and to get the sort order
and emoji names on the sponsors' page, and get the names for the sponsor badges.

> AacOrder.java uses ICU (as checked into CLDR) for sorting all characters other
> than emoji.

> It sorts all emoji at the end, in the CLDR sorting order.

The file format is:

> # Format: codepoint/range/string ; index ; name (if emoji)

> 0488..0489 ; 1

> 0591..05AF ; 3

> 05BD ; 34

> ...

> 1F1FF 1F1E6 ; 129029 ;  South Africa

> 1F1FF 1F1F2 ; 129030 ;  Zambia

> 1F1FF 1F1FC ; 129031 ;  Zimbabwe

1.  The index is the sorting index to use on the sponsors page.
2.  The name is included just for the emoji characters and strings: for others,
    use UnicodeData.txt. (Could include those names also if that's easier.) The
    emoji names are lowercase; we should uppercase on the page with CSS.

There is an abbreviated version of the file, that doesn't have the sort-index.
It is aac-order.txt. You compute the index by adding up the number of items on
each line, instead of having it in a separate field.

### aac-order-us.txt

This file is used for programmatic verification of input from users. It simply
defines a UnicodeSet. The format is:

UnicodeSet EMOJI_ALLOWED = new UnicodeSet(

// (!) EXCLAMATION MARK ..      (~) TILDE

0x21,0x7e,

// (¡) INVERTED EXCLAMATION MARK        ..      (¬) NOT SIGN

0xa1,0xac,

...

.add("🧞‍♂")

// (🧟‍♀) woman zombie

.add("🧟‍♀")

// (🧟‍♂) man zombie

.add("🧟‍♂")

.freeze();

// Total code points: 136314

// Total strings: 1577

// Total: 137891

*You need to make sure, if used with C or C++, that the file encoding and C
settings are for UTF-8 (Ascii would muck with the strings).*

## Emoji Names

If anything has a name field in aac-order-ranges.txt, then you have to used that
instead of the Unicode names. Only if the name field is blank do you use the
Unicode name.

## Sponsors page

Use
[aac-order-ranges.txt](http://unicode.org/draft/consortium/aac-order-ranges.txt)
to rebuild the sponsors page:
[adopted-characters.html](http://www.unicode.org/consortium/adopted-characters.html)

Compare with the previous version page to make sure that any changes are
understood. A simple way to do that is to copy the contents of each page and
past into plain text files and diff the two. Each should look something like:

> \[Unicode\]      Adopt-a-Character

> Sponsors of Adopted Characters

> AAC Animation

> Character sponsors help support the work of the Unicode Consortium, to help
> modern software and computing systems support the widest range of human
> languages. More than 120,000 characters can be adopted—see Adopt a Character.

> The Unicode Consortium gratefully acknowledges the following generous
> character sponsors. Each adoption is permanent.

> 13 Gold Sponsors

> ,

> Mark Davis and Anne Gundelfinger

> {

> Elastic

> }

> Elastic

> &

> Adobe Systems Incorporated

> α

> Ann Lewnes and Greg Welch

> 💩

> Jason Jenkins

> ...

## Backend for sponsorship form

Use
[aac-order-ranges.txt](http://unicode.org/draft/consortium/aac-order-ranges.txt)
to rebuild the backend for
[adopt-a-character.html#sponsorship_form](http://www.unicode.org/consortium/adopt-a-character.html#sponsorship_form)

### Spot check1

Spot-check that a few **new** characters and code point numbers from
[emoji-released.html](http://www.unicode.org/emoji/charts/emoji-released.html)
**can** be entered.

🤣 1F923

...

🛒 1F6D2

### Spot-check2

Spot-check that a **few** currently-adopted characters and code point numbers
from
[adopted-characters.html](http://www.unicode.org/consortium/adopted-characters.html)
and [choosing.html](http://www.unicode.org/consortium/choosing.html) **can** be
entered.

Including one of each of the following:

*   normal character (eg, Greg Welch)
*   emoji singleton (eg, Vinton G. Cerf)
*   emoji modifier sequence (eg, Drew Raines)
*   emoji flag sequence (eg, Yagudin)
*   emoji zwj sequence (eg, 👩‍👩‍👦 from
    [choosing.html](http://www.unicode.org/consortium/choosing.html))

### Spot-check3

Spot-check that a **few** invalid characters and code point numbers from
**cannot** be entered, such as from:

[list-unicodeset.jsp?abb=on&g=gc&a=\[\[:c:\]\[:z:\]\[:di:\]࿕-࿘卍卐\]](http://unicode.org/cldr/utility/list-unicodeset.jsp?abb=on&g=gc&a=%5B%5B:c:%5D%5B:z:%5D%5B:di:%5D%E0%BF%95-%E0%BF%98%20%E5%8D%8D%20%E5%8D%90%5D)

> **Ignore the Unicode set generated at the top; for compactness it uses
> \[^...\] where ... are characters that are not in the invalid set.**

Note: all of the charts are built so that if you select an image (drag across)
and paste into a plain-text browser box, you get the character. To get the hex,
you can use Inspect (or whatever it is called in your browser) to see the
source, then copy.

### Fix other pages

1. Update pages for AAC that have char counts in SVN, *as needed*:

*   <http://www.unicode.org/consortium/adopt-a-character.html>
*   <http://www.unicode.org/consortium/choosing.html>
*   others?

2. Update http://unicode.org/consortium/adopt-a-character.html in SVN to have
images of the newest emoji (you can use the ones in the recent blog post) and
sync back to SVN.

3. Prepare a block post in <http://goo.gl/lSaQNE> , and run it by
[pr-unicode@googlegroups.com](mailto:pr-unicode@googlegroups.com) and
[unicode-web-presence-comm@googlegroups.com](mailto:unicode-web-presence-comm@googlegroups.com)

## Check in

Once everything checks out,

*   check the backend code in,
*   post the revised pages from SVN
*   post the regenerated
    [adopted-characters.html](http://www.unicode.org/consortium/adopted-characters.html)
*   post the new blog post
