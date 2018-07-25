/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2018, Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * bidiref.h
 *
 * Public API for libbidir.
 */

/*
 * Change history:
 *
 * 2013-Jun-02 kenw   Created.
 * 2013-Jun-19 kenw   Added Trace15.
 * 2013-Jul-08 kenw   Public BR_MAXINPUTLEN.
 * 2014-Apr-17 kenw   Increased BR_MAXINPUTLEN to 200.
 * 2015-Jun-03 kenw   Extended versions supported.
 * 2015-Jun-05 kenw   Added defines for return codes.
 * 2016-Sep-22 kenw   Add UBA90. Add br_InitWithPath
 * 2016-Oct-05 kenw   Adjust testCaseNumber to 64-bit integer for
 *                      huge test runs.
 * 2017-Jun-26 kenw   Add UBA100.
 * 2018-Jul-19 kenw   Add UBA110 and UBACUR.
 */

#ifndef __BIDIREF_LOADED
#define __BIDIREF_LOADED

#ifdef  __cplusplus
extern "C" {
#endif

#include <stdint.h>

typedef unsigned long U_Int_32;

/*
 * Set a limit to the longest input string the bidiref implementation
 * will handle. This helps bombproof the input handling against
 * various kinds of bad input data. Increase this value to enable
 * checking of longer strings.
 *
 * The longest string length needed as of Unicode 7.0 for the UBA
 * test data (BidiTest.txt, BidiCharacterTest.txt) is 76, but the
 * length limit is extended here to 200, to make it easier to
 * experiment with deeply embedded bracket pairs, to check behavior
 * at the algorithm limits.
 */

#define BR_MAXINPUTLEN 200

/*
 * Trace flags for control of debug output.
 *
 * These trace flags are bits set in the global traceFlags
 * unsigned 32-bit int.
 */

#define Trace0 (1)     /* On by default: print final test results */
#define Trace1 (1<<1)  /* Trace main algorithm function entry. */
#define Trace2 (1<<2)  /* Trace initialization code function entry. */
#define Trace3 (1<<3)  /* Trace stack handling in X1-X8 */
#define Trace4 (1<<4)  /* Additional debug output for X1-X8 */
#define Trace5 (1<<5)  /* Trace run and sequence handling in X10 */
#define Trace6 (1<<6)  /* Trace stack handling in N0 */
#define Trace7 (1<<7)  /* Additional debug output for N0 */
#define Trace8 (1<<8)  /* Trace reordering in L2 */
#define Trace9 (1<<9)  /* Additional debug output for P2/P3 */
#define Trace10 (1<<10)  /* Trace property file parsing. */

#define Trace11 (1<<11)  /* Display all intermediate UBA results */
#define Trace12 (1<<12)  /* Additional debug output for test parsing */

/*
 * Trace13 is off by default. When Trace13 is off, only FAIL
 * results for test cases as explicitly noted on a per test
 * basis. If Trace13 is turned on, then each PASS result will
 * also be explicitly noted.
 *
 * Trace13 is not recommended when running conformance testing
 * with BidiTest.txt, as that would produce hundreds of thousands
 * of lines of output, one for each passing test case.
 */

#define Trace13 (1<<13)  /* Explicitly list each PASS result for tests */

/*
 * Trace14 is off by default.
 *
 * When Trace14 is on, then any intermediate UBA results display
 * (Trace11) is omitted
 * when application of that rule is vacuous and has no effect on
 * the string being processed.
 *
 * Trace14 does not cause omission of additional debug output
 * related to other trace flags.
 */
#define Trace14 (1<<14)  /* Omit vacuous rule application from display */

/*
 * Trace15 is on by default.
 *
 * When Trace15 is on, errors in processing are logged to the
 * console.
 *
 * When Trace15 is off, no console error logging is done. 
 */
#define Trace15 (1<<15)  /* Turn on console error logging */

/*
 * TraceAll
 *
 * A convenience macro to turn on (or off) all trace flags.
 */
#define TraceAll (Trace0 | Trace1 | Trace2 | Trace3 | Trace4 | \
                  Trace5 | Trace6 | Trace7 | Trace8 | Trace9 | \
                  Trace10 | Trace11 | Trace12 | Trace13 | \
                  Trace14 | Trace15)

/*
 * Version enumeration for support of distinct versions
 * of UBA.
 *
 * This enumeration may be extended, if further distinctions
 * make sense. UBA62 is considered the base version.
 * UBA63 is a distinct version for
 * the purposes of separating the details of the
 * algorithm which have changed for UBA 6.3.0.
 * UBA70, UBA80, etc. extend for future version support.
 */

typedef enum 
    {
    UBAXX,  /* Undefined rule set */
  	UBA62,  /* Base version. UBA 6.2.0 rule set */
  	UBA63,  /* UBA 6.3.0 rule set */
  	UBA70,  /* UBA 6.3.0 rule set; Unicode 7.0.0 repertoire */
    UBA80,  /* UBA 8.0.0 rule set */
    UBA90,  /* UBA 8.0.0 rule set; Unicode 9.0.0 repertoire */
    UBA100, /* UBA 8.0.0 rule set; Unicode 10.0.0 repertoire */
    UBA110, /* UBA 8.0.0 rule set; Unicode 11.0.0 repertoire */
    UBACUR  /* Unspecified version: default to current UBA rules and
               use unversioned file names for data. */ 
    } UBA_Version_Type ;

/*
 * Return codes.
 *
 * These are used both by the public API and by the
 * compiled bidiref executable as return values.
 */

#define BR_TESTOK    ( 1) /* Successful completion */
#define BR_NOACTION  ( 0) /* Command line execution with no action taken */
#define BR_OUTPUTERR (-1) /* Problem in generation of output */
#define BR_INITERR   (-2) /* Failure to read or parse input data */
#define BR_ALLOCERR  (-3) /* Usually a memory allocation failure */
#define BR_TESTERR   (-4) /* Test runs but does not return expected result */

/*
 * Public Function Declarations
 */

extern void TraceOn ( U_Int_32 traceValue );
extern void TraceOff ( U_Int_32 traceValue );

/*
 * br_Init
 *
 * Initialize all property tables and prepare for processing bidi input strings.
 *
 * When using this initialization, versions of UnicodeData.txt and BidiBrackets.txt
 * with specific version names must be in the *current* directory. See
 * documentation in ReadMe.txt for details.
 */
extern int br_Init ( int version );

/*
 * br_InitWithPath
 *
 * As for br_Init, but passing in an explicit datapath.
 *
 * When using a datapath, UnicodeData.txt and BidiBrackets.txt are
 * read from the directory specified by the datapath.
 */
extern int br_InitWithPath ( int version, char *datapath );

/*
 * br_ProcessOneTestCase
 *
 * Public API exported from the library.
 *
 * Take input values for one test case.
 *
 * Input:
 *    testCaseNumber  test case id, expressed as 64-bit integer
 *    text            test case input expressed in UTF-32
 *    textLen         length of test case input
 *    paragraphDirection 0=LTR, 1=RTL, 2=Auto (to match BidiCharacterTest.txt)
 * Input expected test output:
 *    expEmbeddingLevel (see below)
 *    expLevels       a space-delimited string of expected output levels
 *    expOrder        a space-delimited string of expected output order
 *    
 * testCaseNumber is expressed as a 64-bit int, for tracking
 * very large numbers of test cases.
 *
 * Construct a UBACONTEXT and run that test case.
 *
 * If expEmbeddingLevel is -1, that is a signal that there
 * are no expected results. Run the test case without them.
 * (expLevels and expOrder should be set to NULL, but will
 * be ignored in any case).
 *
 * If expEmbeddingLevel is 0 or 1, that is a signal that there
 * are expected results. Attach them to the context.
 *
 * Returns BR_TESTOK (=1) if all goes well.
 * Error return values:
 *   BR_INITERR   -2  Initialization failure (problem with input).
 *   BR_ALLOCERR  -3  Error in context construction.
 *   BR_TESTERR   -4  Bad return from UBA processing
 */

extern int br_ProcessOneTestCase ( int64_t testCaseNumber, U_Int_32 *text, int textLen, 
    int paragraphDirection, int expEmbeddingLevel, char *expLevels, char *expOrder );

/*
 * br_QueryOneTestCase
 *
 * Public API exported from the library.
 *
 * Take input values for one test case.
 *
 * Construct a UBACONTEXT and run that test case.
 *
 * Do no checking against expected values. Instead, return
 * the results in the output parameters for embeddingLevel,
 * levels, and order, and let the client process them.
 *
 * Returns BR_TESTOK (=1) if all goes well.
 * Error return values:
 *   BR_OUTPUTERR -1  Error in formatting output parameters.
 *   BR_INITERR   -2  Initialization not completed before call.
 *   BR_ALLOCERR  -3  Error in context construction.
 *   BR_TESTERR   -4  Bad return from UBA processing
 *
 */

extern int br_QueryOneTestCase ( U_Int_32 *text, int textLen, 
    int paragraphDirection, int *embeddingLevel, char *levels, int levelsLen,
    char *order, int orderLen );

#ifdef  __cplusplus
}
#endif

#endif /* __BIDIREF_LOADED */

