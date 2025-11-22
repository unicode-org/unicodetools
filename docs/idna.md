# UTS #46

1.  Setup
    1.  Make sure Settings.latestVersion is set right. (If you've already done
        this for the UCD, no problem).
    2.  Delete all the bin files to make sure that the unicode tools get the
        release versions of the data. (See [Building Unicode Tools](build.md))
2.  Run GenerateIdna.java
    *   It will generate
        {Generated}/idna/{version}/**IdnaMappingTable.txt**
    *   Diff with the previous version, and make sure everything is understood,
        then copy back into the dev folder.
        ```
        Generated$ meld ../src/unicodetools/data/idna/dev/IdnaMappingTable.txt idna/17.0.0/IdnaMappingTable.txt
        Generated$ cp idna/17.0.0/IdnaMappingTable.txt ../src/unicodetools/data/idna/dev/IdnaMappingTable.txt
        ```
    *   *Important:* The mapping table file must be copied into the dev folder
        before running GenerateIdnaTest.java!
        Otherwise that tool will see the old version of the data.
3.  Now run GenerateIdnaTest.java, in order to generate the test file.
    1.  It will generate {Generated}/idna/{version}/**IdnaTestV2.txt**
    2.  Diff with the previous version, and make sure everything is understood,
        then copy back into the dev folder.
        ```
        Generated$ meld ../src/unicodetools/data/idna/dev/IdnaTestV2.txt idna/17.0.0/IdnaTestV2.txt
        Generated$ cp idna/17.0.0/IdnaTestV2.txt ../src/unicodetools/data/idna/dev/IdnaTestV2.txt
        ```
4.  Run TestIdna.java as a JUnit test.
5.  Diff the old and new files, and sanity check. Pull request...
6.  The idna files are published in the alpha, beta, and final drops.
    See [Data Files Workflow](data-workflow.md)

