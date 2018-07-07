/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2016, Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * brtest.c
 *
 * Module to run a single test case, consisting of
 * the string input to submit to the UBA
 * processing and the expected output of the UBA processing.
 *
 * This module defines the public API for running a test.
 *
 * Exports:
 *  br_ProcessOneTestCase
 *  br_QueryOneTestCase
 */

/*
 * Change history:
 *
 * 2013-Jun-01 kenw   Created.
 * 2013-Jun-05 kenw   Added initialization of dirtyBit.
 * 2013-Jun-19 kenw   Updated to use br_ErrPrint.
 * 2013-Jul-08 kenw   Fix memory leak bug in br_DropSequence.
 * 2014-Apr-16 kenw   Initialize accelerator flags in
 *                      br_ConstructBidiUnitVector.
 * 2015-Jun-05 kenw   Update return code handling.
 * 2015-Aug-19 kenw   Remove duplicate reporting of errors.
 * 2016-Sep-22 kenw   Suppress security warning on compile.
 * 2016-Oct-06 kenw   Update br_ProcessOneTestCase to pass
 *                      64-bit testCaseNumber parameter.
 */

#define _CRT_SECURE_NO_WARNINGS
#include <string.h>
#include <malloc.h>
#include <stdio.h>

#include "bidirefp.h"

/***********************************************************/

/*
 * SECTION: Allocation of UBACONTEXT
 */

/*
 * textBuf
 *
 * Static allocation of a text vector of BIDIUNIT.
 * The static allocation of this buffer allows for its
 * reuse without repeated allocations and deallocations,
 * when running long suites of test cases.
 */ 

static BIDIUNIT b_textBuf[BR_MAXINPUTLEN];

/*
 * br_ConstructBidiUnitVector()
 *
 * Initialize the text values from some source.
 * Look up GC and BC values for each character,
 * and initialize all the levels to zero.
 */

static BIDIUNITPTR br_ConstructBidiUnitVector ( int len, U_Int_32 *text,
    int fileFormat )
{
BIDIUNITPTR bp;
BIDIUNITPTR tbp;
int i;
U_Int_32 tc;

#ifdef NOTDEF
	bp = (BIDIUNITPTR)malloc(len * sizeof(BIDIUNIT));
	if ( bp == NULL )
	{
		return ( NULL );
	}
#endif
    bp = b_textBuf;
	for ( i = 0, tbp = bp; i < len; i++, tbp++)
	{
		tc = text[i];
        if ( fileFormat == FORMAT_A )
        {
        /*
         * For FORMAT_A, the text vector actually contains
         * code points. Look up 
         * Bidi_Class and initialize on that basis.
         */
            tbp->c = tc;
            tbp->bc = br_GetBC ( tc );
            tbp->bpb = br_GetBPB ( tc );
            tbp->bpt = br_GetBPT ( tc );
        }
        else
        {
        /*
         * For FORMAT_B, the text vector contains
         * only Bidi_Class values. Copy those over into
         * the bc and orig_bc fields. Initialize c to a U+FFFF
         * noncharacter value, so it will be clear what
         * is going on.
         */
            tbp->c = 0xFFFF; 
            tbp->bc = tc;
            tbp->bpb = BPB_None;
            tbp->bpt = BPT_None;    
        }
        /*
         * Save the original Bidi_Class value.
         */
        tbp->orig_bc = tbp->bc;
        /*
         * Set all accelerator flags.
         */
        tbp->bc_numeric = 0;
        tbp->bc_isoinit = 0;
        if ( ( tbp->bc == BIDI_AN ) || ( tbp->bc == BIDI_EN ) )
        {
            tbp->bc_numeric = 1;
        }
        else if ( ( tbp->bc == BIDI_LRI ) || ( tbp->bc == BIDI_RLI ) || ( tbp->bc == BIDI_FSI ) )
        {
            tbp->bc_isoinit = 1;
        }
        /*
         * Initialize all the levels to zero.
         */
		tbp->level = 0;
        tbp->expLevel = 0;
        /*
         * Initialize the order fields based on index.
         */
        tbp->order = i;
        tbp->order2 = i;
	}
	return ( bp );
}

/*
 * br_ConstructContext
 *
 * Allocate a UBACONTEXT and initialize it with input
 * values. Set calculated and expected results values to
 * defaults.
 */

static UBACTXTPTR br_ConstructContext ( int len, U_Int_32 *text, 
    Paragraph_Direction paraDirection )
{
UBACTXTPTR p;
BIDIUNITPTR bp;
int fileFormat;

	p = (UBACTXTPTR)malloc(sizeof(UBACONTEXT));
	if ( p == NULL )
	{
		return ( NULL );
	}
    /*
     * The way the BIDIUNIT vector is constructed will
     * depend on the input file format.
     */
    fileFormat = GetFileFormat();

	bp = br_ConstructBidiUnitVector ( len, text, fileFormat );
	if ( bp == NULL )
	{
		free ( p );
		return ( NULL );
	}
    p->state = State_Initialized;
    p->testId = 0;
    p->dirtyBit = 0;
    /*
     * The enumerated Paragraph_Direction. 
     * Dir_Auto will be resolved to either LTR or RTL, depending
     * on the first strong type.
     */ 
    p->paragraphDirection = paraDirection;
	p->paragraphEmbeddingLevel = 0;
	p->textLen = len;
	p->theText = bp;
	p->theRuns = NULL;
    p->lastRun = NULL;
    p->theSequences = NULL;
    p->lastSequence = NULL;
    p->expEmbeddingLevel = -1; /* default to illegal value */
    p->expOrder = NULL;
	return ( p );
}

/*
 * br_AttachExpectedResults
 *
 * Attach the expected results parsed from a test case to
 * an existing UBACONTEXT. This can be used by br_Check
 * to check whether the processing of a test case should be
 * judged to pass or fail.
 *
 * If any allocations fail here, br_DropContext will be
 * called after return, so just let it do any cleanup
 * needed.
 */

static int br_AttachExpectedResults ( UBACTXTPTR ctxt,
    int expEmbeddingLevel, char *expLevels, char *expOrder )
{
char *tmp;
int rc;
int len;
int level;
char *sp;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
char localbuf[BUFFERLEN];
char errString[80];

    /* 
     * Parse the expected levels and store in
     * the expLevel field in theText.
     *
     * First check that the number of expected levels matches
     * the text length.
     */

    sp = expLevels;
    len = 0;
    while ( *sp != '\0' )
    {
        sp = copySubField ( localbuf, sp );
        len++;
    }
    if ( len != ctxt->textLen )
    {
        br_ErrPrint ( "Expected levels in test case input data do not match text length.\n" );
        return ( BR_INITERR );
    }

    /* Next commit to evaluating and storing the expected levels. */

    bdu = ctxt->theText;
    endOfText = ctxt->theText + ctxt->textLen;
    sp = expLevels;

    while ( bdu < endOfText )
    {
        sp = copySubField ( localbuf, sp );
        /*
         * Note that an expected level associated with a deleted element
         * in the input (BN, bidi format controls) is represented with an "x".
         * Check for those values and interpret as NOLEVEL, before attempting to
         * evaluate the parsed value as an integral numeric value for a level.
         */
        if ( strcmp ( localbuf, "x" ) == 0 )
        {
            bdu->expLevel = NOLEVEL; 
        }
        else
        {
            rc = br_Evaluate ( localbuf, &level );

            if ( rc != 1 )
            {
                if ( strlen (localbuf) > 20 )
                {
                    /* truncate bad data */
                    localbuf[20] = '\0';
                    strcat ( localbuf, "..." );
                }
                sprintf ( errString, "Bad value \"%s\" in expected level in test case input data.\n", 
                    localbuf );
                br_ErrPrint ( errString );
                return ( BR_INITERR );
            }

            bdu->expLevel = level;
        }

        bdu++;
    }

    len = (int) strlen ( expOrder );
    /*
     * Even a zero-length string is a valid expected order, if all
     * of the elements of the string resolve to NOLEVEL.
     */
    tmp = (char *)malloc( len + 1 );
    if ( tmp == NULL )
    {
        return ( BR_ALLOCERR );
    }
    strcpy ( tmp, expOrder );
    ctxt->expOrder = tmp; 

    ctxt->expEmbeddingLevel = expEmbeddingLevel;

    return ( BR_TESTOK );
}

/***********************************************************/

/*
 * SECTION: Dropping of UBACONTEXT
 */

/*
 * br_DropRun
 *
 * Deallocate dynamically allocated BIDIRUN struct.
 */

static void br_DropRun ( BIDIRUNPTR brp )
{
    if ( brp->textChain != NULL )
    {
        free ( brp->textChain );
    }
    free ( brp );
}

/*
 * br_DropSequence
 *
 * Deallocate dynamically allocated ISOLATING_RUN_SEQUENCE struct.
 */

static void br_DropSequence ( IRSEQPTR irp )
{
BIDIRUNLISTPTR brl1, brl2;

    /* deallocate any attached list of runs first */
    brl1 = irp->theRuns;
    while ( brl1 != NULL )
    {
        brl2 = brl1->next;
        free ( brl1 );
        brl1 = brl2;
    }
    if ( irp->textChain != NULL )
    {
        free ( irp->textChain );
    }
    free ( irp );
}

/*
 * br_DropContext
 *
 * Deallocate dynamically allocated context data.
 */

static void br_DropContext ( UBACTXTPTR ctxt )
{
BIDIRUNPTR br1, br2;
IRSEQPTR ir1, ir2;

	if ( ctxt == NULL )
    {
    	return;
    }
#ifdef NOTDEF
    if ( ctxt->theText != NULL )
    {
    	free ( ctxt->theText);
    }
#endif
    /* Hand over hand list disposal of run list */
    br1 = ctxt->theRuns;
    while ( br1 != NULL )
    {
    	br2 = br1->next;
    	br_DropRun ( br1 );
    	br1 = br2;
    }
    /* Hand over hand list disposal of sequence list */
    ir1 = ctxt->theSequences;
    while ( ir1 != NULL )
    {
        ir2 = ir1->next;
        br_DropSequence ( ir1 );
        ir1 = ir2;
    }
    if ( ctxt->expOrder != NULL )
    {
        free ( ctxt->expOrder );
    }
    free ( ctxt );
}

/***********************************************************/

/*
 * SECTION: Routines to run test cases
 */

/*
 * br_RunOneTest
 *
 * Take a UBACONTEXT, run the UBA algorithm on it, and
 * check the results against the expected results.
 *
 * The checkResults parameter can be set to 0 to skip the
 * checking when running static tests during implementation
 * development.
 */

static int br_RunOneTest ( UBACTXTPTR ctxt, int checkResults )
{
int rc;

    /* Run the UBA algorithm steps on the text. */

    rc = br_UBA ( ctxt );
    if ( rc != BR_TESTOK )
    {
        return ( rc );
    }

    /* Mark the algorithm state as complete. */

    ctxt->state = State_Complete;

    /* Examine the results in the context data for correctness. */

    if ( checkResults )
    {        
        rc = br_Check ( ctxt );
        if ( rc != BR_TESTOK )
        {
            return ( rc );
        }
    }

    return ( BR_TESTOK ); /* Success */
}

/*
 * br_ProcessOneTestCase
 *
 * Public API exported from the library.
 *
 * Take input values for one test case from the input file.
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

int br_ProcessOneTestCase ( int64_t testCaseNumber, U_Int_32 *text, int textLen, 
    int paragraphDirection, int expEmbeddingLevel, char *expLevels, char *expOrder )
{
int rc;
UBACTXTPTR ctxt;
int checkResults;
char errString[80];

    if ( Trace ( Trace2 ) )
    {
        printf ( "\nTrace: Entering br_ProcessOneTestCase\n" );
    }

    /*
     * This is a public API. Double check that the br_Init call was
     * done correctly before making this call.
     */

    if ( !br_GetInitDone() )
    {
        br_ErrPrint ( "Error: Initialization not completed.\n" );
        return ( BR_INITERR );
    }

    ctxt = br_ConstructContext ( textLen, text, paragraphDirection );

    if ( ctxt == NULL )
    {
        br_ErrPrint ( "Error: Bad return in context construction.\n" );
        return ( BR_ALLOCERR );
    }

    checkResults = ( expEmbeddingLevel >= 0 );

    if ( checkResults )
    {
        rc = br_AttachExpectedResults ( ctxt, expEmbeddingLevel, expLevels,
            expOrder );

        if ( rc != BR_TESTOK )
        {
            br_ErrPrint ( "Error: Unable to attach expected results to context.\n" );
            br_DropContext ( ctxt );
            return ( rc );
        }
    }

    /*
     * Set the testCaseNumber into the context, so it can be used to
     * tag trace output.
     */
    ctxt->testId = testCaseNumber;

    if ( Trace ( Trace11) )
    {
        printf ( "\n===================== Testcase %lld =====================\n",
            testCaseNumber );
    }

    rc = br_RunOneTest ( ctxt, checkResults );

    if ( ( rc != BR_TESTOK ) && Trace ( Trace0 ) )
    {
        sprintf ( errString, "Error: Bad return %d from UBA processing.\n", rc );
        br_ErrPrint ( errString );
    }

    br_DropContext ( ctxt );

    return ( rc );
}

static char truncationErr[] = "Error: Output value was truncated.\n";

/*
 * br_FormatLevelOutput
 *
 * Take the list of levels and format it as a string, using the
 * format of BidiCharacterTest.txt. Push the result into the
 * output parameter.
 *
 * Return BR_TESTOK (=1) if o.k.
 * Return BR_OUTPUTERR (=-1) and print a truncation error if the output parameter is too short.
 */

static int br_FormatLevelOutput ( char *d, int dlen, UBACTXTPTR ctxt )
{
char *dp;
char *dend;
int tlen;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
char localbuf[4];

    bdu = ctxt->theText;
    endOfText = ctxt->theText + ctxt->textLen;
    dp = d;
    dend = d + dlen;
    /* Initialize output parameter to an empty string. */
    *dp = '\0';

    while ( bdu < endOfText )
    {
        if ( dp > d )
        {
            if ( dp < dend - 1 )
            {
                strcat ( dp, " " );
                dp++;
            }
            else
            {
                br_ErrPrint ( truncationErr );
                return ( BR_OUTPUTERR );
            }
        }
        if ( bdu->level == NOLEVEL )
        {
            sprintf ( localbuf, "x" );
        }
        else
        {
            sprintf ( localbuf, "%d", bdu->level );
        }
        tlen = (int) strlen ( localbuf );
        if ( dp < dend - tlen )
        {
            strcat ( dp, localbuf );
            dp += tlen;
        }
        else
        {
            br_ErrPrint ( truncationErr );
            return ( BR_OUTPUTERR );
        }
        bdu++;
    }

    return ( BR_TESTOK );
}

/*
 * br_FormatOrderOutput
 *
 * Take the order list and format it as a string, using the
 * format of BidiCharacterTest.txt. Push the result into the
 * output parameter.
 *
 * Return BR_TESTOK (=1) if o.k.
 * Return BR_OUTPUTERR (=-1) and print a truncation error if the output parameter is too short.
 */

static int br_FormatOrderOutput ( char *d, int dlen, UBACTXTPTR ctxt )
{
char *dp;
char *dend;
int tlen;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
char localbuf[4];

    bdu = ctxt->theText;
    endOfText = ctxt->theText + ctxt->textLen;
    dp = d;
    dend = d + dlen;
    /* Initialize output parameter to an empty string. */
    *dp = '\0';

    while ( bdu < endOfText )
    {
        if ( bdu->order != NOLEVEL )
        {
            if ( dp > d )
            {
                if ( dp < dend - 1 )
                {
                    strcat ( dp, " " );
                    dp++;
                }
                else
                {
                    br_ErrPrint ( truncationErr );
                    return ( BR_OUTPUTERR );
                }
            }
            sprintf ( localbuf, "%d", bdu->order );
            tlen = (int) strlen ( localbuf );
            if ( dp < dend - tlen )
            {
                strcat ( dp, localbuf );
                dp += tlen;
            }
            else
            {
                br_ErrPrint ( truncationErr );
                return ( BR_OUTPUTERR );
            }
        }
        bdu++;
    }

    return ( BR_TESTOK );
}

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
 * br_QueryOneTestCase forces all trace flags off, so that
 * no debug output will be displayed.
 *
 * Returns BR_TESTOK (=1) if all goes well.
 * Error return values:
 *   BR_OUTPUTERR -1  Error in formatting output parameters.
 *   BR_INITERR   -2  Initialization not completed before call.
 *   BR_ALLOCERR  -3  Error in context construction.
 *   BR_TESTERR   -4  Bad return from UBA processing
 */

int br_QueryOneTestCase ( U_Int_32 *text, int textLen, 
    int paragraphDirection, int *embeddingLevel, char *levels, int levelsLen,
    char *order, int orderLen )
{
int rc;
int rc2;
UBACTXTPTR ctxt;

    /*
     * This is a public API. Double check that the br_Init call was
     * done correctly before making this call.
     */

    if ( !br_GetInitDone() )
    {
        return ( BR_INITERR );
    }

    ////TraceOffAll();

    ctxt = br_ConstructContext ( textLen, text, paragraphDirection );

    if ( ctxt == NULL )
    {
        *embeddingLevel = -1;
        levels[0] = '\0';
        order[0] = '\0';
        return ( BR_ALLOCERR );
    }

    rc = br_RunOneTest ( ctxt, 0 );

    if ( rc != BR_TESTOK )
    {
        *embeddingLevel = -1;
        levels[0] = '\0';
        order[0] = '\0';
        br_DropContext ( ctxt );
        return ( BR_TESTERR );
    }

    /*
     * Now extract the results from the UBACONTEXT and push into
     * the client output parameters.
     */

    *embeddingLevel = ctxt->paragraphEmbeddingLevel;

    rc  = br_FormatLevelOutput ( levels, levelsLen, ctxt );

    rc2 = br_FormatOrderOutput ( order, orderLen, ctxt );

    br_DropContext ( ctxt );

    if ( rc != BR_TESTOK )
    {
        return ( rc );
    }
    else if ( rc2 != BR_TESTOK )
    {
        return ( rc2 );
    }
    return ( BR_TESTOK );
}

