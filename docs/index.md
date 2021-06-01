# Building Unicode Tools

This file provides instructions for building and running the UnicodeTools, which
can be used to:

*   build the Derived Unicode files in the UCD (Unicode Character Database),
*   build the transformed UCA (Unicode Collation Algorithm) files needed by
    Unicode.
*   run consistency checks on beta releases of the UCD and the UCA.
*   build 4 chart folders on the unicode site.
*   build files for ICU (collation, NFSkippable)

**WARNING!!**

*   This is NOT production level code, and should never be used in programs.
*   The API is subject to change without notice, and will not be maintained.
*   The source is uncommented, and has many warts; since it is not production
    code, it has not been worth the time to clean it up.
*   It will probably need some adjustments on Unix or Windows, such as changing
    the file separator.
*   Currently it uses hard-coded directory names.
*   The contents of multiple versions of the UCD must be copied to a local
    directory, as described below.
*   ***It will be useful to look at the history of the files in git to see the
    kinds of rule changes that are made!***
    *   Unfortunately, we lost some change history of about 1.5 years(?) leading up to April 2020.

## Instructions

1.  Set up Eclipse and CLDR, according to instructions:
    http://cldr.unicode.org/development/maven
    1.  Edit the cldr-code project’s Build Path:
        Under “Order and Export”, set the check mark next to “Maven Dependencies”
        so that CLDR makes its dependencies available to the Unicode Tools project.
2.  Get your github account authorized for https://github.com/unicode-org/unicodetools,
    create a fork under your account, and create a local clone.
3.  Import the unicodetools project into Eclipse. (*Not* Maven: General > Existing Projects into Workspace)
    1.  Edit the unicodetools project’s Build Path:
        1.  Projects (“Required projects on the build path”): Add... cldr-code
        2.  Libraries: Aside from the JRE System Library,
            Add Class Folder: cldr-all/cldr-code/target/test-classes
        3.  Order and Export: Make sure cldr-code is *above* unicodetools.
            See the ucd-dev email thread “Unicode Tools vs. UnicodeProperty.java”.
            (= issue #66)
4.  Also create the project **and directory** Generated. Various results are
    deposited there. You need the directory, but the Eclipse project is optional.
    1.  New... -> Project... -> General/Project
    2.  Project Name=Generated
    3.  Uncheck "Use default location" (so that it's not inside your Eclipse workspace)
    4.  Browse or type a folder path like <unitools>/Generated
        1.  Create this folder
        2.  Create a subfolder BIN
5.  Project > Clean... > Clean all projects is your friend
6.  For the tools to work, you need to set environment variables according to your workspace layout.
    1.  You can set these for each single tool in the Run|Debug Configurations... (x)= Arguments tab, in the VM arguments;
    2.  or you can set the common variables globally in Window > Preferences... > Java > Installed JREs, select the active JRE, Edit... Default VM arguments: `-Dvar1=path1 -Dvar2=path2 ...`
    3.  Depending on which tool you are running, you may need some or all of the following.
        Adjust the paths to your workspace.
        ```
        -DCLDR_DIR=/usr/local/google/home/mscherer/cldr/uni/src
        -DUNICODETOOLS_REPO_DIR=/usr/local/google/home/mscherer/unitools/mine/src
        -DUNICODETOOLS_OUTPUT_DIR=/usr/local/google/home/mscherer/unitools/mine
        -DUNICODE_DRAFT_DIR=/usr/local/google/home/mscherer/svn.unidraft/trunk/TODO
        -DUVERSION=14.0.0
        ```
    4.  Please also use the VM argument `-ea` (enable assertions) in your Preferences
        or in your Run/Debug configurations, so that failed assertions don’t just slip through.

### Input data files

The input data files for the Unicode Tools are checked into the repo since
2012-dec-21:

*   <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/ucd>
*   <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/ucd>

This is inside the unicodetools file tree, and the Java code has been updated to
assume that. Any old Eclipse setup needs its path variables checked.

For details see [Input data setup](inputdata.md).

## Generating new data

To generate new data files, you can run the `org.unicode.text.UCD.Main` class
(yes, the `Main` class has a `main()` function)
with program arguments `build MakeUnicodeFiles`. You may optionally include e.g.
`version 14.0.0` if you wish to just generate the files for a single version.
Make sure you have the `-DGEN_DIR=...` etc. VM arguments set up as described above.

## Updating to a new Unicode version

All of the following have "version 14.0.0" (or whatever the latest version is)
in the options given to Java.

Example changes for adding properties:
<https://github.com/unicode-org/unicodetools/pull/40>. Throughout these steps we
will walk through updating unicodetools to support Unicode 14.

Firstly, fetch the latest data files for this version from
<https://www.unicode.org/Public/14.0.0/ucd/>, matching your new version number.
If this does not exist, request this be created from
[ucd-dev@googlegroups.com](mailto:ucd-dev@googlegroups.com). You may also need
to fetch the emoji files from <https://www.unicode.org/Public/emoji/13.1>, using
a previous version if a new one does not exist.

You may need to use the tools from [Input data setup](inputdata.md) to
desuffix the files (removing the -dN suffixes). Copy these into
`unicodetools/data/emoji/14.0` and `unicodetools/data/ucd/14.0.0-Update`.

to set up the inputs correctly. For some updates you may need to pull in other
(uca, security, idna, etc) files, see [Input data setup](inputdata.md) for more information.

Now, update the following files:

`MakeUnicodeFiles.txt` (find in Eclipse via Navigate/Resource or Ctrl+Shift+R)

```
Generate: .*
DeltaVersion: 1 (this will generate files suffixed with -d1, figure out what the latest delta is)
CopyrightYear: 2021 (or whatever)
....
File: DerivedAge
..... add a value for the latest version at the bottom:
Value:  V14_0
```

Update `String[] LONG_AGE` and `String[] SHORT_AGE` in `UCD_Names.java`.

Update `latestVersion` and `lastVersion` in `org.unicode.text.utility.Settings.java` to fix:

```
public static final String latestVersion = "14.0.0";
public static final String lastVersion = "13.1.0"; // last released version
```

Update `LIMIT_AGE` and `AGE_VERSIONS` in `UCD_Types.java`.

Update `enum AGE_Values` in `UcdPropertyValues.java`.

Update `searchPath` in `org.unicode.text.utility.Utility.java`.

If there are new CJK characters
(if there are changes to entries in UnicodeData.txt that are for `<CJK Ideograph ..., First>` etc.),
`UCD.java` and `UCD_Types.java` need to be updated to handle these ranges.
See [PR #47](https://github.com/unicode-org/unicodetools/pull/47) for an example.

For CJK, you'll first need to compute the composite version, as `(major << 16) | (minor << 8) |` update.
E.g. Unicode 14 is 0xe0000.
Since the ranges change based on the version, the code here needs to be updated in a version-aware way.

If any range has changed its end point, say, CJK Extension C, update `CJK_C_LIMIT` in `UCD_Types.java`
(make sure to update the comment next to it with the latest Unicode version).

Then edit `mapToRepresentative()` in `UCD.java` to add the range. Make sure the
range is added *only* for the latest Unicode version, by using sections like
`if (ch <= 0x2B737 && rCompositeVersion >= 0xe0000)` .

If a new range has been introduced, add it to `UCD_Types.java` near `CJK_E_BASE`,
add it to `mapToRepresentative()`, update `hasComputableName` and `get()` in `UCD.java`
to add the first character.

Also search (case-insensitively) unicodetools for 2A700 (start of Extension C)
and add the new range accordingly.

When `CJK_LIMIT` moves, search for 9FCC and update near there as necessary.

If the main Tangut block has been extended, then in `UCD.java`
`mapToRepresentative()` add another per-version block for returning `TANGUT_BASE`.

You can now run the steps in “Generating new data” above to attempt to generate the files.
It will likely error due to missing enum values for new blocks and scripts.

### New blocks

Compare Blocks.txt to the old version (or check the errors from your attempt
to generate new files). For all the new ones:

*   Add to `ShortBlockNames.txt` (you need to know what the short name is, you can
    find it in `PropertyValueAliases.txt`)
*   Add long & short names to `UcdPropertyValues.java` `enum Block_Values`
    *   You may not have to do this for all of them, update `ShortBlockNames` and
        see if you still get errors.

### New scripts

*   Add long & short names to `UcdPropertyValues.java` `enum Script_Values`, in
    alphabetical order
*   Add the script code to `UCD_Types.java` below `SCRIPT_CODE`, in alphabetical
    order grouped by Unicode version. Update `LIMIT_SCRIPT` to use the name of the
    new last script
*   Update `SCRIPT` and `LONG_SCRIPT` in `UCD_Names.java`, in alphabetical order
    grouped by Unicode version. (Important: this must be in the same order as the
    previous one.)
*   After first run of UCD Main, take the `DerivedAge.txt` lines for the new
    version, copy them into the input `Scripts.txt` file, and change the new
    version number to the appropriate script (which can be new or old or `Common`
    etc.). Then run UCD Main again and check the generated `Scripts.txt`.

Make a pull request to incorporate these updates, and upload the generated files
in a way that can be shared with ucd-dev.

Ideally, diff the files to check for any discrepancies. The script will do this
automatically, you can search the output for lines that say "Found difference in
`<filename>`", however note that it will only display the first line of the diff,
so if there are additional discrepancies you may miss them.

### New enum property values

***When you run, it will break if there are new enum property values.***

Note: For more information and newer code see the pages

*   [Changing UCD Properties & Printout](changing-ucd-properties.md)
*   [NewUnicodeProperties](newunicodeproperties.md)

To fix that:

Go into `org.unicode.text.UCD/`
*   `UCD_Names.java` and
*   `UCD_Types.java`

(These contain ugly items that should be enums nowadays.)

Find the property (easiest is to search for some other properties in the enum).
Add at end in `UCD_Types`. Be sure to update the limit, like

```
LIMIT_SCRIPT = Mandaic + 1;
```

Then in `UCD_Names`, change the corresponding name entry, both the full and abbreviated names.
Follow the format of the existing values.

For example:

In `UCDNames.java` in `BIDI_CLASS` add `"LRI", "RLI", "FSI", "PDI",`

In `UCDNames.java` in `LONG_BIDI_CLASS` add
`"LeftToRightIsolate", "RightToLeftIsolate", "FirstStrongIsolate", "PopDirectionalIsolate",`

In `UCD_Types.java` add & adjust

```
BIDI_LRI = 20,
BIDI_RLI = 21,
BIDI_FSI = 22,
BIDI_PDI = 23,
LIMIT_BIDI_CLASS = 24;
```

Some changes may cause collisions in the UnicodeMaps used for derived properties.
You'll find that out with an exception like:

> Exception in thread "main" java.lang.IllegalArgumentException: Attempt to
> reset value for 17B4 when that is disallowed. Old: Control; New: Extend at
> org.unicode.text.UCD.ToolUnicodePropertySource$28.\<init\>(ToolUnicodePropertySource.java:578)

### New scripts

Add new scripts like other new property values. In addition, make sure there are
ISO 15924 script codes, and collect CLDR script metadata. See

<http://cldr.unicode.org/development/updating-codes/updating-script-metadata>

<http://www.unicode.org/iso15924/codechanges.html>

### Break Rules

If there are new break rules (or changes), see
[Segmentation-Rules](changing-ucd-properties.md).

### Building Files

#### Setup

1.  In Eclipse, open the Package Explorer (Use Window>Show View if you don't
    see it)
2.  Open UnicodeTools
    *   package org.unicode.text.UCD
        *   MakeUnicodeFiles.txt

            This file drives the production of the derived Unicode files.
            The first three lines contain parameters that you may want to
            modify at some times:
            ```
            Generate: .*script.* // this is a regular expression. Use .* for all files
            DeltaVersion: 10     // This gets appended to the file name. Pick 1+ the highest value in Public
            CopyrightYear: 2010  // Pick the current year
            ```
3.  Open in Package Explorer
    *   package org.unicode.text.UCD
        *   Main.java
4.  Run>Run As...
    1.  Choose Java Application
        *   it will fail, don't worry; you need to set some parameters.
5.  Run>Run...
    *   Select the Arguments tab, and fill in the following
        *   Program arguments: `build MakeUnicodeFiles`
            *   For a specific version, prepend `version 6.3.0 ` or similar.
        *   VM arguments: CLDR_DIR etc., see the setup instructions near the top of this page;
            easiest to set them in the global Preferences.
            Otherwise copy them into each Run/Debug configuration.
    *   Close and Save

#### Run

1.  You'll see it build the 5.0 files, with something like the following
    results:
    ```
    Writing UCD_Data
    Data Size: 109,802
    Wrote Data 109802
    ```
    For each version, the tools build a set of binary data in BIN that contain the
    information for that release. This is done automatically, or you can
    manually do it with the Program Arguments
2.  As options, use: `version 5.0.0 build`

    This builds a compressed format of all the UCD data (except blocks and
    Unihan) into the BIN directory. Don't worry about the voluminous console
    messages, unless one says "FAIL".

    *You have to manually do this if you change any of the data files in
    that version! This ought to have build files, but I haven't worked
    around to it.*

    Note: if for any reason you modify the binary format of the BIN files,
    you also have to bump the value in that file:
    ```
    static final byte BINARY_FORMAT = 8; // bumped if binary format of UCD changes
    ```

#### Results in Generated

1.  The files will be in this directory.
2.  (Note: these don't get generated anymore!) There are also DIFF folders,
    that contain BAT files that you can run on Windows with CompareIt. (You
    can modify the code to build BATs with another Diff program if you
    want).
    1.  For any file with a significant difference, it will build two BAT
        files, such as the first two below.
        ```
        Diff_PropList-5.0.0d10.txt.bat
        OLDER-Diff_PropList-5.0.0d10.txt.bat

        UNCHANGED-Diff_PropertyValueAliases-5.0.0d10.txt.bat
        ```
3.  Any files without significant changes will have "UNCHANGED" as a prefix:
    ignore them. The OLDER prefix is the comparison to the last version of
    Unicode.
4.  On Windows you can run these BATs to compare files: TODO??

### Upload for Ken Whistler & editorial committee

1.  Check diffs for problems
2.  First drop for a version: Upload **all** files
3.  Subsequent drop for a version: Upload *only modified* files

### Invariant Checking

***Note: Also build and run the [New Unicode Properties](newunicodeproperties.md) programs, since they have some additional checks.***

#### Setup
1.  Open in Package Explorer
    *   org.unicode.text.UCD
        *   TestUnicodeInvariants.java
2.  Run>Run As... Java Application\
    Will create the following file of results:
    ```
    ...workspace/Generated/UnicodeTestResults.html
    ```

    And on the console will list whether any problems are found. Thus in the
    following case there was one failure:
    ```
    ParseErrorCount=0
    TestFailureCount=1
    ```
3.  The header of the result file explains the syntax of the tests.
4.  Open that file and search for `**** START Test Failure`.
5.  Each such point provides a dump of comparison information.
    1.  Failures print a list of differences between two sets being
        compared. So if A and B are being compared, it prints all the items
        in A-B, then in B-A, then in A&B.
    2.  For example, here is a listing of a problem that must be corrected.
        Note that usually there is a comment that explains what the
        following line or lines are supposed to test. Then will come FALSE
        (indicating that the test failed), then the detailed error report.
        ```
        # Canonical decompositions (minus exclusions) must be identical across releases
        [$Decomposition_Type:Canonical - $Full_Composition_Exclusion] = [$ï¿½Decomposition_Type:Canonical - $ï¿½Full_Composition_Exclusion]

        FALSE
        **** START Error Info ****

        In [$ï¿½Decomposition_Type:Canonical - $ï¿½Full_Composition_Exclusion], but not in [$Decomposition_Type:Canonical - $Full_Composition_Exclusion] :

        # Total code points: 0

        Not in [$ï¿½Decomposition_Type:Canonical - $ï¿½Full_Composition_Exclusion], but in [$Decomposition_Type:Canonical - $Full_Composition_Exclusion] :
        1B06           # Lo       BALINESE LETTER AKARA TEDUNG
        1B08           # Lo       BALINESE LETTER IKARA TEDUNG
        1B0A           # Lo       BALINESE LETTER UKARA TEDUNG
        1B0C           # Lo       BALINESE LETTER RA REPA TEDUNG
        1B0E           # Lo       BALINESE LETTER LA LENGA TEDUNG
        1B12           # Lo       BALINESE LETTER OKARA TEDUNG
        1B3B           # Mc       BALINESE VOWEL SIGN RA REPA TEDUNG
        1B3D           # Mc       BALINESE VOWEL SIGN LA LENGA TEDUNG
        1B40..1B41     # Mc   [2] BALINESE VOWEL SIGN TALING TEDUNG..BALINESE VOWEL SIGN TALING REPA TEDUNG
        1B43           # Mc       BALINESE VOWEL SIGN PEPET TEDUNG

        # Total code points: 11

        In both [$ï¿½Decomposition_Type:Canonical - $ï¿½Full_Composition_Exclusion], and in [$Decomposition_Type:Canonical - $Full_Composition_Exclusion] :
        00C0..00C5     # L&   [6] LATIN CAPITAL LETTER A WITH GRAVE..LATIN CAPITAL LETTER A WITH RING ABOVE
        00C7..00CF     # L&   [9] LATIN CAPITAL LETTER C WITH CEDILLA..LATIN CAPITAL LETTER I WITH DIAERESIS
        00D1..00D6     # L&   [6] LATIN CAPITAL LETTER N WITH TILDE..LATIN CAPITAL LETTER O WITH DIAERESIS
        ...
        30F7..30FA     # Lo   [4] KATAKANA LETTER VA..KATAKANA LETTER VO
        30FE           # Lm       KATAKANA VOICED ITERATION MARK
        AC00..D7A3     # Lo [11172] HANGUL SYLLABLE GA..HANGUL SYLLABLE HIH

        # Total code points: 12089
        **** END Error Info ****
        ```
6.  Options:
    1.  -r Print the failures as a range list.
    2.  -fxxx Use a different input file, such as -fInvariantTest.txt

### Options

1.  If you want to see files that are opened while processing, do the following:
    1.  Run>Run
    2.  Select the Arguments tab, and add the following
        1.  VM arguments: `-DSHOW_FILES`

## UCA

1.  Note: This will only work after building the UCD files for this version.
2.  Download UCA files (mostly allkeys.txt) from
    `http://www.unicode.org/Public/UCA/<beta version>/`
3.  Run `desuffixucd.py` (see the inputdata subpage)
4.  Update the input files for the UCA tools, at
    `~/svn.unitools/trunk/data/uca/8.0.0/` =
    <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/uca/8.0.0>
5.  You will use `org.unicode.text.UCA.Main` as your main class, creating along
    the same lines as above.
    1.  Options (VM arguments):
    2.  -DNODATE (suppresses date output, to avoid gratuitous diffs during
        development)
    3.  -DAUTHOR (suppresses only the author suffix from the date)
    4.  -DAUTHOR=XYZ (sets the author suffix to " \[XYZ\]")
6.  ***Only for UCA 6.2 and before:*** If you change any of the CJK constants,
    **you also need to modify the same constants in ICU's ImplicitCEGenerator.**
    1.  **If you don't, you'll see a message like:**

        **Exception in thread "main" java.lang.IllegalArgumentException: FA0E: overlap: 9FCC (E2FA6A90) > FA0E(E0AB8800)**

7.  To test whether the UCA files are valid, use the options (*note: you must
    also build the ICU files below, since they test other aspects*).
    ```
    writeCollationValidityLog
    ```

    It will create a file:
    ```
    ...\5.0.0\CheckCollationValidity.html
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

### UCA for ICU

To build all the UCA files used by ICU, use the option:
```
ICU
```

They will be built into:
```
../Generated/uca/8.0.0/
```

1.  NFSkippable
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

## Charts

To build all the charts, use org.unicode.text.UCA.Main, with the option:
```
charts
```

They will be built into

<http://unicode.org/draft/charts/>

**Once UCA is released, then copy those files up to the right spots in the
Unicode site:**

*   <http://www.unicode.org/charts/normalization/>
*   <http://www.unicode.org/charts/collation/>
*   <http://www.unicode.org/charts/case/>
*   <http://www.unicode.org/charts/collation/>
*   ...
