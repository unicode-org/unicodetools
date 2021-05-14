# Refactoring

For historical reasons, we have certain classes that are duplicated in ICU,
CLDR, and/or Unicode tools (perhaps with changes that need merging!). Or we have
classes that are in ICU (for example), that are only needed in CLDR.

Over time, we want to straighten that out. The goal is to move classes down the
following list where not needed higher up.

*   ICU
*   ICU /unittest
*   CLDR
*   CLDR /unittest
*   unicodetools
*   unicodetools /unittest
*   UnicodeJsps
*   UnicodeJspsTests

The UnicodeJsps are actually at the same level as unicodetools, and need to be
self-contained, only depending on ICU and CLDR (and its needed libraries: guava,
etc.)

Starting

The easiest way to do this is with Eclipse's refactoring tools.

1.  Make sure all the above are sync'ed to trunk.
2.  Change the build paths so that they use your local projects. (You'll restore
    this afterwards, so be careful not to check in the changed classpath in the
    meantime.)
    1.  See "Refactor ICU4J+CLDR+Unicode Tools" on
        <http://site.icu-project.org/processes/coverage>

Finding code and refactor. \[Details and tips TBD\]

Finishing up

1.  Sync to trunk.
2.  Run all the tests, and make sure they pass.
3.  Build the icu jars and add to
    1.  cldr/tools/java/libs/
    2.  UnicodeJsps/WebContent/WEB-INF/lib/
4.  Build cldr.jar and add to
    1.  UnicodeJsps/WebContent/WEB-INF/lib/
5.  Change the build paths back.
6.  Run all cldr, UnicodeJspTest, and unicodetools tests and make sure they
    pass.
7.  Check everything in.
