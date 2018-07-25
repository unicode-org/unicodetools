/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2018, Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * brtable.c
 *
 * Module to create and access tables of Bidi_Class and any
 * other Unicode character properties required for correct
 * implementation of the UBA.
 *
 * Values needed to initialize the tables are read
 * from UnicodeData.txt and other UCD data files of explicit
 * versions, as needed.
 *
 * Exports:
 *  br_InitTable
 *  br_Init
 *  br_InitWithPath
 *  br_GetInitDone
 *  br_GetBC
 *  br_GetBPT
 *  br_GetBPB
 */

/*
 * Change history:
 *
 * 2013-Jun-02 kenw   Created.
 * 2013-Jun-20 kenw   Added use of br_ErrPrint.
 * 2013-Jun-26 kenw   Added Trace15 to br_Init.
 * 2013-Jun-27 kenw   Correct incorrect error printing in
 *                       br_parsePropertyData.
 * 2014-Apr-15 kenw   Updates for Unicode 7.0.
 * 2015-Jun-03 kenw   Further updates for Unicode 7.0.
 * 2015-Jun-05 kenw   Updates for Unicode 8.0.
 * 2015-Aug-19 kenw   Add CJK Extension E to getBidiDataPtr.
 * 2015-Dec-04 kenw   Corrected a return value in br_GetBPT.
 * 2016-Sep-22 kenw   Updates for Unicode 9.0.
 *                       Add datapath to br_InitTable.
 * 2016-Oct-05 kenw   Add output of version number to br_InitTable.
 * 2017-Jun-27 kenw   Updates for Unicode 10.0.
 * 2018-Jul-22 kenw   Updates for Unicode 11.0.
 *                       Adjust data file loading to allow for
 *                       relative paths and to allow unversioned
 *                       file name loading, independent of the
 *                       path chosen.
 */

#define _CRT_SECURE_NO_WARNINGS
#include <string.h>
#include <ctype.h>
#include <stdio.h>

#include "bidirefp.h"

static int linesProcessed;

static int initDone = 0;

#define UNICODE_DATA ( 0 )
#define BRACKET_DATA ( 1 )

/*
 * Arrays to store basic data for each character.
 */
static BIDIDATA planes01[0x20000];  /* 2048 K  Inefficient, but gets the job done. */

static BIDIDATA plane2[0x800];   /* Only needed for compatibility CJK range. */

static BIDIDATA plane14[0x200];  /* Just for tag and variation selector characters. */

static BIDIDATA handata;         /* Static value shared for all unified CJK. */

/***********************************************************/

/*
 * SECTION: Utility routines.
 */

int br_GetInitDone ( void )
{
    return ( initDone );
}

#define DELIM ';'

/*
 * skipField
 *
 * Skip over one semi-colon delimited field.
 */

static char *skipField ( char *src )
{
char *sp;

    sp = src;
    while ( (*sp != DELIM) && (*sp != '\0') && (*sp != '\n') )
    {
        sp++;
    }
    if ( ( *sp == DELIM ) || ( *sp == '\n') )
    {
        sp++;
    }
    return sp;
}

/*
 * skipSpace
 *
 * Skip over spaces.
 */

static char *skipSpace ( char *src )
{
char *sp;

    sp = src;
    while ( (*sp == ' ') && (*sp != '\0') && (*sp != '\n') )
    {
        sp++;
    }
  return sp;
}

/*
 * br_PrintErr
 *
 * Print out a diagnostic error for a parsing evaluationa failure.
 */

static void br_PrintErr ( int n, char *s )
{
    if ( Trace ( Trace15 ) )
    {
        if ( n == -1 )
        {
            printf ( "Error: Uneven hex digits in code point [%s].\n", s );
        }
        else
        {
            sprintf ( "Error: Invalid hex digit in code point [%s].\n", s );
        }
    }
}


/***********************************************************/

/*
 * SECTION: Array access utility routines.
 *
 * Abstract the access to the BidiData array by code point.
 */

static BDPTR getBidiDataPtr ( U_Int_32 i )
{
    if ( i <= 0x33FF )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= CJK_EXTA_LAST )
    {
        return ( &handata );
    }
    else if ( i <= 0x4DFF )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= CJK_URO_LAST )
    {
        return ( &handata );
    }
    else if ( i <= 0x1FFFF )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= CJK_EXTB_LAST )
    {
        return ( &handata );
    }
    else if ( i <= CJK_EXTC_LAST )
    {
        return ( &handata );
    }
    else if ( i <= CJK_EXTD_LAST )
    {
        return ( &handata );
    }
    else if ( i <= CJK_EXTE_LAST )
    {
        return ( &handata );
    }
    else if ( i <= CJK_EXTF_LAST )
    {
        return ( &handata );
    }
    else if ( ( i >= 0x2F800 ) && ( i <= 0x2FFFF ) )
    {
        return ( &(plane2[i - 0x2F800] ) );
    }
    else if ( ( i >= 0xE0000 ) && ( i <= 0xE01FF ) )
    {
        return ( &(plane14[i - 0xE0000]) );
    }
    else
    {
        return ((BDPTR)NULL);
    }
}

/*
 * Exported functions to get property values from the
 * property tables for particular characters.
 */

BIDIPROP br_GetBC ( U_Int_32 c )
{
BDPTR dp;
    
    dp = getBidiDataPtr ( c );
    return ( dp != NULL ) ? dp->bidivalue : BIDI_Unknown ;
}

BPTPROP br_GetBPT ( U_Int_32 c )
{
BDPTR dp;
    
    dp = getBidiDataPtr ( c );
    return ( dp != NULL ) ? dp->bpt : BPT_None ;
}

U_Int_32 br_GetBPB ( U_Int_32 c )
{
BDPTR dp;
    
    dp = getBidiDataPtr ( c );
    return ( dp != NULL ) ? dp->bpb : BPB_None ;
}

/***********************************************************/

/*
 * SECTION: Initialize the BidiData arrays.
 *
 * Special case the Han and Hangul ranges, which are not
 * explicitly listed in the UnicodeData files.
 */

static void initBidiData ( void )
{
int i;
BDPTR tmp;

/*
 * First initialize the static record for handata.
 */
    handata.han = 1;
    handata.bidivalue = BIDI_L;
    handata.bpb = BPB_None;
    handata.bpt = BPT_None;

/*
 * Then scan through the arrays and initialize
 * all values. Skip setting values if tmp turns up
 * pointing to the handata record.
 *
 * For this initial setting, default to GC_Cn
 * (unassigned) and to Bidi_Class L.
 */

    for ( i = 0; i <= 0x1FFFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        if ( tmp == &handata )
            continue;
        tmp->han = 0;
        tmp->bidivalue = BIDI_L;
        tmp->bpb = BPB_None;
        tmp->bpt = BPT_None;
    }
    for ( i = 0x2F800; i <= 0x2FFFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->han = 0;
        tmp->bidivalue = BIDI_L;
        tmp->bpb = BPB_None;
        tmp->bpt = BPT_None;
    }
    for ( i = 0xE0000; i <= 0xE01FF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->han = 0;
        tmp->bidivalue = BIDI_L;
        tmp->bpb = BPB_None;
        tmp->bpt = BPT_None;
    }
/*
 * Special case the ranges for default values
 * for the Bidi_Class which are not L. This covers
 * the various right-to-left block ranges.
 * See DerivedBidiClass.txt for details.
 */
    for ( i = 0x0600; i <= 0x07BF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0x08A0; i <= 0x08FF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0xFB50; i <= 0xFDCF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0xFDF0; i <= 0xFDFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0xFE70; i <= 0xFEFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0x1EE00; i <= 0x1EEFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_AL;
    }
    for ( i = 0x0590; i <= 0x05FF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
    for ( i = 0x07C0; i <= 0x089F; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
    for ( i = 0xFB1D; i <= 0xFB4F; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
    for ( i = 0x10800; i <= 0x10FFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
    for ( i = 0x1E800; i <= 0x1EDFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
    for ( i = 0x1EF00; i <= 0x1EFFF; i++ )
    {
        tmp = getBidiDataPtr ( i );
        tmp->bidivalue = BIDI_R;
    }
}

/***********************************************************/

/*
 * processUnicodeData
 *
 * Process a line from UnicodeData.txt
 */

static int processUnicodeData ( char *buf )
{
char *sp;
char localbuf[256];
char errString[80];
int nn;
int nn2;
U_Int_32 n;
BDPTR tmp;

    sp = buf;

    sp = copyField ( localbuf, sp );

    nn = convertHexToInt ( &n, localbuf );
    if ( nn != 0 )
    {
        br_PrintErr ( nn, localbuf );
        return ( -1 );
    }

    /* Field (field 1): Unicode name */

    sp = copyField ( localbuf, sp );

    /* 
     * Special case range values in UnicodeData.txt, which are
     * marked by first and last point labels in angle brackets.
     * Just skip these entries without processing.
     * The General_Category and Bidi_Class values are set
     * instead by default initialization of the table ranges.
     */

    if ( localbuf[0] == '<' )
    {
    	if ( ( strstr ( localbuf, "First" ) != NULL )  ||
    		 ( strstr ( localbuf, "Last" ) != NULL ) )
    	{
    		return ( 0 );
    	}
    }

    /* Omit 1 field (field 2): Unicode category */

    sp = skipField ( sp );

    /* Omit 1 field (field 3): Combining class */

    sp = skipField ( sp );

    /* Span and copy 1 field (field 4): Bidi category */

    sp = copyField ( localbuf, sp );
    nn2 = br_GetBCFromLabel ( localbuf );
    if ( nn2 == BIDI_Unknown )
    {
        sprintf ( errString, "U+%04X Bidi_Class value %s unknown!\n", n, localbuf );
        br_ErrPrint ( errString );
        return ( -1 );
    }

    /* At this point we have a valid BC value. Stuff it in the table. */

    tmp = getBidiDataPtr ( n );
    tmp->bidivalue = (BIDIPROP)nn2;

    /* Omit 7 fields (fields 5 - 11) */

    /* Diagnostic output */
#ifdef NOTDEF
	if ( ( n >= 0x200B ) && ( n <= 0x202F ) )
	{
		printf ( "n=%04X, gc=%d, bc=%d\n", n, nn, nn2);
	}
#endif
    return ( 1 );
}

/***********************************************************/

/*
 * processBracketData
 *
 * Process a line from BidiBrackets.txt
 *
 * Lines are of the format:
 *
 * 0028; 0029; o # LEFT PARENTHESIS
 * 0029; 0028; c # RIGHT PARENTHESIS
 *
 * field 0 is a Unicode code point.
 * field 1 is the bpb property: a Unicode code point or "<none>"
 * field 2 is the bpt property: "o" or "c" or "n"
 *
 * For all of the lines actually defined in the file, bpb != none.
 *
 * Note that unlike UnicodeData.txt, BidiBrackets.txt includes
 * spaces after the semicolons. These need to be skipped over
 * before parsing the subsequent fields.
 */

static int processBracketData ( char *buf )
{
char *sp;
char localbuf[256];
int nn;
U_Int_32 n;
U_Int_32 n2;
BPTPROP bpt;
BDPTR tmp;

    sp = buf;

    sp = copyField ( localbuf, sp );

    nn = convertHexToInt ( &n, localbuf );
    if ( nn != 0 )
    {
        br_PrintErr ( nn, localbuf );
        return ( -1 );
    }

    sp = skipSpace ( sp );

    /* Field (field 1): bpb value, another code point */

    sp = copyField ( localbuf, sp );

    nn = convertHexToInt ( &n2, localbuf );
    if ( nn != 0 )
    {
        br_PrintErr ( nn, localbuf );
        return ( -1 );
    }

    sp = skipSpace ( sp );

    /* Field (field 1): bpt value, a symbolic value, "o" or "c" */

    /* 
     * This parse uses copySubField atm, because this field in
     * BidiBrackets.txt is not terminated by a semicolon, but
     * rather is followed by " # " and the character name as
     * a comment.
     */

    sp = copySubField ( localbuf, sp );

    if ( strcmp ( localbuf, "o" ) == 0 )
    {
        bpt = BPT_O;
    }
    else if ( strcmp ( localbuf, "c" ) == 0 )
    {
        bpt = BPT_C;
    }
    else
    {
        bpt = BPT_None;
    }

    /* At this point we have valid bpb and bpt values. Stuff them in the table. */

    tmp = getBidiDataPtr ( n );
    tmp->bpb = n2;
    tmp->bpt = bpt;

    return ( 1 );
}

/***********************************************************/

/*
 * SECTION: parsePropertyData
 *
 * Load and parse the UnicodeData.txt file (or any other file using
 * the same generic format), branching by status value to specific
 * parse routines for each line.
 *
 * If version == UBACUR, use the unversioned file name, otherwise,
 * attempt to load a specific, versioned file name that matches
 * the UBA version number.
 */

static int br_parsePropertyData ( int status, int version, char *datapath )
{
FILE *fdi;
int rc;
int i;
int usepath;
char *fileName;
char fqfn[512];
char buffer[512];

    if ( Trace ( Trace10 ) )
    {
        printf ( "Trace: Entering br_parsePropertyData\n" );
    }

    linesProcessed = 0;

    /*
     * If datapath is NULL or is a zero-length string, ignore it.
     * Otherwise use it.
     */

    if ( datapath == NULL )
    {
        usepath = 0;
    }
    else if ( strcmp ( datapath, "" ) == 0 )
    {
        usepath = 0;
    }
    else
    {
        usepath = 1;
    }

    if ( status == UNICODE_DATA )
    {
        if ( version == UBACUR )
        {
            fileName = "UnicodeData.txt";
        }
        else if ( version == UBA110 )
        {
            fileName = "UnicodeData-11.0.0.txt";
        }
        else if ( version == UBA100 )
        {
            fileName = "UnicodeData-10.0.0.txt";
        }
        else if ( version == UBA90 )
        {
            fileName = "UnicodeData-9.0.0.txt";
        }
        else if ( version == UBA80 )
        {
            fileName = "UnicodeData-8.0.0.txt";
        }
        else if ( version == UBA70 )
        {
            fileName = "UnicodeData-7.0.0.txt";
        }
        else if ( version == UBA63 )
        {
            fileName = "UnicodeData-6.3.0.txt";
        }
        else
        {
            fileName = "UnicodeData-6.2.0.txt";
        }
    }
    else if ( status == BRACKET_DATA )
    {
        if ( version == UBACUR )
        {
            fileName = "BidiBrackets.txt";
        }
        else if ( version == UBA110 )
        {
            fileName = "BidiBrackets-11.0.0.txt";
        }
        else if ( version == UBA100 )
        {
            fileName = "BidiBrackets-10.0.0.txt";
        }
        else if ( version == UBA90 )
        {
            fileName = "BidiBrackets-9.0.0.txt";
        }
        else if ( version == UBA80 )
        {
            fileName = "BidiBrackets-8.0.0.txt";
        }
        else if ( version == UBA70 )
        {
            fileName = "BidiBrackets-7.0.0.txt";
        }
        else if ( version == UBA63 )
        {
            fileName = "BidiBrackets-6.3.0.txt";
        }
    }
    else
    {
        fileName = "Not Defined";
    }

    if ( usepath )
    {
        strcpy ( fqfn, datapath );
        strcat ( fqfn, fileName );
    }
    else
    {
        strcpy ( fqfn, fileName );
    }

    fdi = fopen ( fqfn, "rt" );

    if ( fdi == NULL )
    {
        if ( Trace ( Trace15 ) )
        {
            printf ( "Cannot open property data file: \"%s\"\n", fqfn );
        }
        return -1 ;
    }
    /* Do the work */

    while ( fgets ( buffer, 512, fdi ) != NULL )
    {
    int slen;
    int lineIsBlank;

    /* Don't process empty lines or comments. */
        slen = (int) strlen ( buffer );
        if ( ( slen == 0 ) || ( buffer[0] == '#' ) || ( buffer[0] == ';' ) )
            continue ;
    /* Also check for non-zero length lines with just whitespace */
        lineIsBlank = 1;
        i = 0 ;
        while ( lineIsBlank && ( i < slen ) )
        {
            if ( !isspace (buffer[i]) )
            {
                lineIsBlank = 0;
            }
            i++;
        }
        if ( lineIsBlank )
            continue;

/*      fputs ( buffer, stdout ); */

        switch ( status )
        {
            case UNICODE_DATA :
                rc = processUnicodeData ( buffer );
                linesProcessed++;
                break;
            case BRACKET_DATA :
                rc = processBracketData ( buffer );
                linesProcessed++;
                break;
            default :
                rc = -1;
        }

        if ( rc == -1 )
            break;
    }
    fclose ( fdi );

    if ( rc < 0 )
    {
        return ( rc );
    }
    else if ( Trace ( Trace10 ) )
    {
    	printf ( "Debug: Processed %d lines from %s\n", linesProcessed, fqfn );
    }

    return ( 0 );
}

/***********************************************************/

/*
 * SECTION: Initialize the property tables.
 */

/*
 * br_InitTable
 *
 * Initialize the arrays, then read in values from
 * UnicodeData.txt.
 *
 * If the input is set to FORMAT_B, then no parsed
 * property data is required. Exit without reading
 * in property data files.
 */

int br_InitTable ( int version, char *datapath )
{
int rc;

    /* Bombproof against repeat invocations */

    if ( initDone )
    {
        return ( 1 );
    }

    /* 
     * Forcing Trace10 on enables checking at runtime which
     * version of property data files are being read and
     * parsed for initializing the tables.
     */

#ifdef NOTDEF
    TraceOn ( Trace10 );
    printf ("Forced Trace10 on.\n");
#endif

    if ( Trace ( Trace10 ) )
    {
        printf ( "Trace: Entering br_InitTable with version=%d\n", version );
    }

    if ( GetFileFormat() == FORMAT_B )
    {
        initDone = 1;
        return ( 1 );
    }

    /*
     * Initialize the property arrays with
     * default values.
     */

	initBidiData();

	rc = br_parsePropertyData ( UNICODE_DATA, version, datapath );
    if ( rc < 0 )
    {
        if ( Trace ( Trace15 ) )
        {
            printf ( "Error: Failure in parsing UnicodeData.txt: %d.\n", rc );
        }
        return ( rc );
    }

    if ( version > UBA62 )
    {
        rc = br_parsePropertyData ( BRACKET_DATA, version, datapath );
        if ( rc < 0 )
        {
            if ( Trace ( Trace15 ) )
            {
                printf ( "Error: Failure in parsing BidiBrackets.txt: %d.\n", rc );
            }
            return ( rc );
        }
    }

    if ( Trace ( Trace0 ) )
    {
        printf ( "Note:  Initialized bidiref 11.0.0 library for UBA version %s\n", GetUBAVersionStr() );
    }

    initDone = 1;
	return ( 1 );
}

/*
 * br_Init
 *
 * Public API. This forces the file format to FORMAT_A and
 * then initializes the tables.
 *
 * Set Trace0 and Trace15 on as the default for output display. These
 * may be turned off for the br_QueryTestResults API, or the
 * client may turn on other trace flags for br_ProcessTestResults.
 *
 * Used for an external application making use of the public APIs
 * to run and/or query individual test cases.
 */

int br_Init ( int version )
{
int rc;

    /* Bombproof against repeat invocations */

    if ( initDone )
    {
        return ( 1 );
    }

    SetFileFormat ( FORMAT_A );

    SetUBAVersion ( version );

    TraceOn ( Trace0 );
    TraceOn ( Trace15 );

    rc = br_InitTable ( version, "" );

    return ( rc );
}

/*
 * br_InitWithPath
 *
 * As for br_Init, but with an explicit datapath passed in.
 */

int br_InitWithPath ( int version, char *datapath )
{
int rc;

    /* Bombproof against repeat invocations */

    if ( initDone )
    {
        return ( 1 );
    }

    SetFileFormat ( FORMAT_A );

    SetUBAVersion ( version );

    TraceOn ( Trace0 );
    TraceOn ( Trace15 );

    rc = br_InitTable ( version, datapath );

    return ( rc );
}

