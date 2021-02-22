This directory contains files needed for the bidiref1 tool invoked from
the Bidi C Reference demo at http://www.unicode.org/cldr/utility/bidic.jsp

The executable is built from the source files of the Bidi C Reference
implementation.  Copies of the C source files used to be found in the
source subdirectory here.  Since the minor changes needed for the
bidiref1 executable have been merged into the C Reference itself, the
copies have been deleted.  They can be obtained from the C Reference
under http://www.unicode.org/Public/PROGRAMS/BidiReferenceC/

For example, for Version 11.0, the file tree would be as follows:

unicodetools-trunk/UnicodeJsps/src/org/unicode/jsp/bidiref1
├───source
│       bidiref.h
│       bidiref1.c
│       bidirefp.h
│       brrule.c
│       brtable.c
│       brtest.c
│       brutils.c
│       makefile
│
└───ucd
        BidiBrackets-6.3.0.txt
        BidiBrackets-7.0.0.txt
        BidiBrackets-8.0.0.txt
        BidiBrackets-9.0.0.txt
        BidiBrackets-10.0.0.txt
        BidiBrackets-11.0.0.txt
        UnicodeData-6.2.0.txt
        UnicodeData-6.3.0.txt
        UnicodeData-7.0.0.txt
        UnicodeData-8.0.0.txt
        UnicodeData-9.0.0.txt
        UnicodeData-10.0.0.txt
        UnicodeData-11.0.0.txt
