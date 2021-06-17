# UCA (UTS #10)

## DUCET

For each new Unicode version, once the repertoire is final and
the character properties are pretty stable (coming up on the beta),
Ken inserts all of the new characters into the default sort order.

For a few releases, he has documented his incremental progress with valuable notes
sent to the ucd-dev mailing list.
Markus has been taking the incremental file changes, and the notes, into this repo.

See the history of commits that changed decomps.txt and allkeys.txt.

For UCA 14 see https://github.com/unicode-org/unicodetools/pull/71

For the collection of notes for UCA 10 see ducet.md.

## Before generating

(Same prerequisite as for [security data](../security.md).)

First, in CLDR, update the script metadata:
http://cldr.unicode.org/development/updating-codes/updating-script-metadata

We need the script “ID Usage” (e.g., Limited_Use) and script sample characters
for the CLDR/ICU FractionalUCA.txt data.

## Tools & tests

1.  Note: This will only work after building the UCD files for this version.
2.  Download UCA files (mostly allkeys.txt) from
    `https://www.unicode.org/Public/UCA/{beta version}/`
3.  Run `desuffixucd.py` (see the inputdata subpage)
4.  Update the input files for the UCA tools, at
    {this repo}/unicodetools/data/uca/{version} e.g.
    <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/uca/14.0.0>
5.  You will use `org.unicode.text.UCA.Main` as your main class, creating along
    the same lines as above.
    1.  Possible additional options (VM arguments):
    2.  -DNODATE (suppresses date output, to avoid gratuitous diffs during
        development)
    3.  -DAUTHOR (suppresses only the author suffix from the date)
    4.  -DAUTHOR=XYZ (sets the author suffix to " \[XYZ\]")
6.  To test whether the UCA files are valid, use the options (*note: you must
    also build the ICU files below, since they test other aspects*).
    ```
    writeCollationValidityLog
    ```

    It will create a file:
    ```
    {Generated}/UCA/{version}/CheckCollationValidity.html
    ```
    1.  Review this file. It will list errors. Some of those are actually
        warnings, and indicate possible problems (this is indicated in the text,
        such as by: "These are not necessarily errors, but should be examined
        for *possible* errors"). In those cases, the items should be reviewed to
        make sure that there are no inadvertent problems.
    2.  If it is not so marked, it is a true error, and must be fixed.
    3.  At the end, there is section **11. Coverage**. There are two sections:
        1.  In UCDxxx, but not in allkeys. Check this over to make sure that
            these are all the characters that should get ***implicit*** weights.
        2.  In allkeys, but not in UCD. These should be ***only*** contractions.
            Check them over to make sure they look right also.

### UCA for CLDR & ICU

To build all the UCA files used by CLDR and ICU, use the option:
```
ICU
```

They will be built into:
```
{Generated}/UCA/{version}/
```

Sometimes there are errors, and the tool stops with an exception,
especially the first time we run the tool for a new Unicode version.

A common error is when we add a script (or just some additional characters)
to a group of scripts that share a compressible primary lead byte
in the CLDR/ICU FractionalUCA.txt data file, and
we now get too many primary weights for that lead byte.
Check the Console output for error messages.
(Sometimes the stdout/stderr output is out of order,
so an error message may not fit to its immediate surroundings.)
For example:
```
[76 F3 02]  # Osage first primary
last weight: 76 F3 FE
[76 F5 02]  # CANADIAN-ABORIGINAL first primary
error in class PrimaryWeight: overflow of compressible lead byte last weight: 77 0D ED
[77 0E 02]  # OGHAM first primary
last weight: 77 0E B8
...
reordering group {Tale Talu Lana Cham Bali Java Mong Olck Cher Osge Cans Ogam} marked for compression but uses more than one lead byte 76..77
```

For that, read the comments in the PrimariesToFractional constructor and
adjust the code there that sets per-script collation “properties”.
In particular, look for existing adjustments with comments like
“Ancient script, avoid lead byte overflow.”
You may just need to change the script in one of these existing lines,
starting a new lead byte for an earlier script than in the previous version.

Watch for new scripts that get precious two-byte primaries although
they have very small user communities.
Ensuring that they use three-byte primary weights also avoids lead byte overflows.
On the other hand, when a minor script is cased (has lowercase+uppercase forms),
then it may make sense to use two-byte primaries in order to minimize the
size of the binary ICU data file. (The tool should default to doing this.)
Judgment call. See Cherokee, Deseret, Osage, Vithkuqi for examples.

After running the tool, diff the main mapping file and look for bad changes
(for example, more bytes per weight for common characters).
```
~/unitools/mine/src$ sed -r -f ~/cldr/uni/src/tools/scripts/uca/blankweights.sed ~/cldr/uni/src/common/uca/FractionalUCA.txt > ../frac-13.0.txt
~/unitools/mine/src$ sed -r -f ~/cldr/uni/src/tools/scripts/uca/blankweights.sed ../Generated/UCA/14.0.0/CollationAuxiliary/FractionalUCA.txt > ../frac-14.0.txt
~/unitools/mine/src$ meld ../frac-13.0.txt ../frac-14.0.txt
```

CLDR root data files are checked into $CLDR_SRC/common/uca/
```
cp {Generated}/UCA/{version}/CollationAuxiliary/* $CLDR_SRC/common/uca/
```

See the [Unihan for CLDR](../unihan.md) page for generating new versions of
CJK collation tailorings and transliterator (transform) rules.

> :point_right: **Note**: Some of the following is outdated.
> Markus has been keeping a
> [log of what he has been doing in the ICU repo](https://github.com/unicode-org/icu/blob/main/icu4c/source/data/unidata/changes.txt).
> Look there for the latest (top-most) section “collation: CLDR collation root, UCA DUCET”.

----

1.  NFSkippable
    1.  Obsolete: ICU does not actually need/use this file any more.
    1.  A file is needed by ICU that is generated with the same tool. Just use
        the input parameter "NFSkippable" to generate the file NFSafeSets.txt.
        This is also a default if you do the ICU files.

1.  You should then build a set of the ICU files for the previous version, if
    you don't have them. Use the options:
    ```
    version 4.2.0 ICU
    ```

    Or whatever the last version was.

2.  Now, you will want to compare versions. The key file is `UCA_Rules_NoCE.txt`.
    It contains the rules expressed in ICU format, which
    allows for comparison across versions of UCA without spurious variations of
    the numbers getting in the way.
    1.  Do a Diff between the last and current versions of these files, and
        verify that all the differences are either new characters, or were
        authorized to be changed by the UTC.

Review the generated data; compare files, use
[blankweights.sed](https://github.com/unicode-org/cldr/blob/master/tools/scripts/uca/blankweights.sed)
or similar:
```
~/svn.unitools/Generated$ sed -r -f ~/svn.cldr/trunk/tools/scripts/uca/blankweights.sed ~/svn.cldr/trunk/common/uca/FractionalUCA.txt > ../frac-9.txt
~/svn.unitools/Generated$ sed -r -f ~/svn.cldr/trunk/tools/scripts/uca/blankweights.sed uca/10.0.0/CollationAuxiliary/FractionalUCA.txt > ../frac-10.txt && meld ../frac-9.txt ../frac-10.txt
```

Copy all generated files to unicode.org for review & staging by Ken & editors.

Once the files look good:

*   Make sure there is a CLDR ticket for the new UCA version.
*   Create a branch for it.
*   Copy the generated `CollationAuxiliary/*` files to the CLDR branch at `common/uca/` and commit for review.
    ```
    ~/svn.unitools$ cp Generated/uca/8.0.0/CollationAuxiliary/* ~/svn.cldr/trunk/common/uca/
    ```
    Ignore files that were copied but are not version-controlled, that is,
    `git status` shows a question mark status for them.

### UCA for previous version

Some of the tools code only works with the latest UCD/UCA versions. When I
(Markus) worked on UCA 7 files while UCD/UCA 8 were under way,
I set `version 7.0.0` on the command line and made the following temporary
(not committed to the repository) code changes:

```
Index: org/unicode/text/UCA/UCA.java

===================================================================

--- org/unicode/text/UCA/UCA.java (revision 742)

+++ org/unicode/text/UCA/UCA.java (working copy)

@@ -1354,7 +1354,7 @@

         {0x10FFFE},

         {0x10FFFF},

         {UCD_Types.CJK_A_BASE, UCD_Types.CJK_A_LIMIT},

-        {UCD_Types.CJK_BASE, UCD_Types.CJK_LIMIT},

+        {UCD_Types.CJK_BASE, 0x9FCC+1},  // TODO: restore for UCA 8.0!  {UCD_Types.CJK_BASE, UCD_Types.CJK_LIMIT},

         {0xAC00, 0xD7A3},

         {0xA000, 0xA48C},

         {0xE000, 0xF8FF},

@@ -1361,7 +1361,7 @@

         {UCD_Types.CJK_B_BASE, UCD_Types.CJK_B_LIMIT},

         {UCD_Types.CJK_C_BASE, UCD_Types.CJK_C_LIMIT},

         {UCD_Types.CJK_D_BASE, UCD_Types.CJK_D_LIMIT},

-        {UCD_Types.CJK_E_BASE, UCD_Types.CJK_E_LIMIT},

+        // TODO: restore for UCA 8.0!  {UCD_Types.CJK_E_BASE, UCD_Types.CJK_E_LIMIT},

         {0xE0000, 0xE007E},

         {0xF0000, 0xF00FD},

         {0xFFF00, 0xFFFFD},

Index: org/unicode/text/UCD/UCD.java

===================================================================

--- org/unicode/text/UCD/UCD.java (revision 743)

+++ org/unicode/text/UCD/UCD.java (working copy)

@@ -1345,7 +1345,7 @@

             if (ch <= 0x9FCC && rCompositeVersion >= 0x60100) {

                 return CJK_BASE;

             }

-            if (ch <= 0x9FD5 && rCompositeVersion >= 0x80000) {

+            if (ch <= 0x9FD5 && rCompositeVersion > 0x80000) {  // TODO: restore ">=" when really going to 8.0!

                 return CJK_BASE;

             }

             if (ch <= 0xAC00)

Index: org/unicode/text/UCD/UCD_Types.java

===================================================================

--- org/unicode/text/UCD/UCD_Types.java (revision 742)

+++ org/unicode/text/UCD/UCD_Types.java (working copy)

@@ -24,7 +24,7 @@

     // 4E00;<CJK Ideograph, First>;Lo;0;L;;;;;N;;;;;

     // 9FD5;<CJK Ideograph, Last>;Lo;0;L;;;;;N;;;;;

     CJK_BASE = 0x4E00,

-    CJK_LIMIT = 0x9FD5+1,

+    CJK_LIMIT = 0x9FCC+1,  // TODO: restore for UCD 8.0!  0x9FD5+1,

 

     CJK_COMPAT_USED_BASE = 0xFA0E,

     CJK_COMPAT_USED_LIMIT = 0xFA2F+1,
```

