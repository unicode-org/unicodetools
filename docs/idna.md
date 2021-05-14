# UTS#46

1.  Setup
    1.  Make sure Settings.latestVersion is set right. (If you've already done
        this for the UCD, no problem).
    2.  Delete all the bin files to make sure that the unicode tools get the
        release versions of the data. (See [Building Unicode Tools](index.md))
2.  Run GenerateIdna.java
    *   It will generate
        {generated}/Public/idna/<version>/**IdnaMappingTable.txt**
    *   ~~The data for the last 4 columns (h, i, j, k) of Table 4 IDNA
        Comparisons for the UTR are listed at the bottom of the console
        output.~~
        *   ~~Fix Table 4 (h, i, j, k) with that data, and check into SVN.~~
    *   ~~Diff with the previous version, and make sure everything is
        understood, then check into SVN.~~
    *   The results will be in
        <http://unicode.org/draft/Public/idna/><version>/**IdnaMappingTable.txt**
3.  Now run GenerateIdnaTest.java, in order to generate the test file.
    1.  It will generate {generated}/Public/idna/<version>/**IdnaTest.txt**
    2.  Diff with the previous version, and make sure everything is understood,
        then check into SVN.
    3.  The results will be in
        <http://unicode.org/draft/Public/idna/><version>/**IdnaTest.txt**
4.  Edit the ReadMe.txt if necessary.
    1.  Fix the copyright date
    2.  Add or remove "draft" in front of "data files", according to the status
        of the data.

To push to production

1.  Diff the old and new files, and sanity check.
2.  Copy the files
    *   from <http://unicode.org/draft/Public/idna/><version>/\*
    *   to <http://unicode.org/Public/idna/><version>/\*
