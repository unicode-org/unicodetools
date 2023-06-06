# UTS #46

1.  Setup
    1.  Make sure Settings.latestVersion is set right. (If you've already done
        this for the UCD, no problem).
    2.  Delete all the bin files to make sure that the unicode tools get the
        release versions of the data. (See [Building Unicode Tools](build.md))
2.  Run GenerateIdna.java
    *   It will generate
        {Generated}/idna/{version}/**IdnaMappingTable.txt**
    *   Before [UTS #46 table 4](https://www.unicode.org/reports/tr46/#Table_IDNA_Comparisons)
        was fixed at Unicode 11:
        *   The data for the last 4 columns (h, i, j, k) of Table 4 IDNA
            Comparisons for the UTR are listed at the bottom of the console output.
        *   Fix Table 4 (h, i, j, k) with that data, and check into the repo.
    *   Diff with the previous version, and make sure everything is understood,
        then copy back into the dev folder.
        ```
        Generated$ meld ../src/unicodetools/data/idna/dev/IdnaMappingTable.txt idna/15.1.0/IdnaMappingTable.txt
        Generated$ cp idna/15.1.0/IdnaMappingTable.txt ../src/unicodetools/data/idna/dev/IdnaMappingTable.txt
        ```
    *   *Important:* The mapping table file must be copied into the dev folder
        before running GenerateIdnaTest.java!
        Otherwise that tool will see the old version of the data.
3.  Now run GenerateIdnaTest.java, in order to generate the test file.
    1.  It will generate {Generated}/idna/{version}/**IdnaTestV2.txt**
    2.  Diff with the previous version, and make sure everything is understood,
        then copy back into the dev folder.
        ```
        Generated$ meld ../src/unicodetools/data/idna/dev/IdnaTestV2.txt idna/15.1.0/IdnaTestV2.txt
        Generated$ cp idna/15.1.0/IdnaTestV2.txt ../src/unicodetools/data/idna/dev/IdnaTestV2.txt
        ```
4.  Edit the ReadMe.txt if necessary.
    1.  Fix the copyright date
    2.  Add or remove "draft" in front of "data files", according to the status
        of the data.
5.  Run TestIdna.java as a JUnit test.

To push to production

1.  Diff the old and new files, and sanity check.
2.  Copy the files
    *   from {Generated}/idna/{version}/*
    *   to https://www.unicode.org/Public/idna/{version}/*
