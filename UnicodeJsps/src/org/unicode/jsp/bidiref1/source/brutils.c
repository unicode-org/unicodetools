/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2018 Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * brutils.c
 *
 * Collection of utility functions used by other modules.
 * These utility functions are used both by modules inside
 * the library and by functions in the bidiref executable.
 *
 * Exports:
 *  GetFileFormat
 *  SetFileFormat
 *
 *  GetUBAVersion
 *  GetUBAVersionStr
 *  SetUBAVarsion
 *  
 *  Trace
 *  TraceOn
 *  TraceOff
 *  TraceOffAll
 *
 *  convertHexToInt
 *  br_Evaluate
 *  copyField
 *  copySubField
 *  br_GetBCFromLabel
 *  br_ErrPrint
 */

/*
 * Change history:
 *
 * 2013-Jun-01 kenw   Created.
 * 2013-Jun-19 kenw   Add br_ErrPrint.
 * 2016-Sep-22 kenw   Add cast to suppress strlen warning.
 * 2016-Oct-05 kenw   Add GetUBAVersionStr.
 * 2017-Jun-27 kenw   Updated for Unicode 10.0.
 * 2018-Jul-22 kenw   Updated for Unicode 11.0.
 */

#include <string.h>
#include <ctype.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>

#include "bidirefp.h"


/*
 * SECTION: Generic version information.
 */

static int ubaVersion = UBA110;   /* Default to UBA110 */
static int fileFormat = FORMAT_A; /* Format of input data file */

static char *versionNums[NUMVERSIONS] =
    {
        "X.X", "6.2", "6.3", "7.0", "8.0", "9.0", "10.0", "11.0", "def:11.0"
    };

int GetFileFormat ( void )
{
    return ( fileFormat );
}

void SetFileFormat ( int format )
{
    fileFormat = format;
}

int GetUBAVersion ( void )
{
    return ( ubaVersion );
}

char* GetUBAVersionStr ( void )
{
    return ( versionNums[ubaVersion] );
}

/*
 * SetUBAVersion()
 *
 * Set ubaVersion to the version passed in.
 *
 * UBACUR (unspecified) will default to latest version.
 */

void SetUBAVersion ( int version )
{
    ubaVersion = version;
}

/***********************************************************/

/*
 * SECTION: Traces.
 *
 * This set of routines supports a very simple set of trace
 * flags, with set, unset, and check functions, to enable
 * precise control over what types of debug output are
 * printed during execution.
 *
 * One 32-bit traceFlags variable is defined here, which
 * allows up to 32 individual trace bits to be defined for
 * the flags.
 *
 * The actual values of the trace flags are established
 * in bidiref.h.
 *
 * Initial default trace flag settings are established
 * based on chosen debug levels during program start, but
 * can be changed programmatically anytime during execution.
 */

static U_Int_32 traceFlags = 0; /* Trace flags for control of debug output */

/*
 * Trace: check a trace bit
 */
int Trace ( U_Int_32 traceValue )
{
    return ( ( ( traceValue & traceFlags ) == traceValue ) ? 1 : 0 );
}

/*
 * TraceOn: turn on a trace bit.
 */
void TraceOn ( U_Int_32 traceValue )
{
    traceFlags |= traceValue;
}

/*
 * TraceOff: turn off a trace bit.
 */
void TraceOff ( U_Int_32 traceValue )
{
    traceFlags &= ~traceValue;
}

/*
 * TraceOffAll: turn off all trace bits.
 */
void TraceOffAll ( void )
{
    traceFlags = 0;
}

/***********************************************************/

/*
 * SECTION: Error Printing
 */

/*
 * br_ErrPrint
 *
 * Encapsulate console error logging, so it can be turned on
 * or off simply with a trace flag.
 */

void br_ErrPrint ( char *errString )
{
    if ( Trace ( Trace15 ) )
    {
        printf ( errString );
    }
}

/***********************************************************/

/*
 * SECTION: Numeric evaluation utility routines.
 */

/*
 * Convert an "ASCII" representation of sb/mb int to real int.
 *
 * Bounded to 0 .. 0xFFFFFFFF.
 *
 * i.e. "c2ba" to 0xC2BA, "8000000A" to 0x8000000A, etc.
 *
 * RETURNS:
 *    0  converted o.k.
 *   -1  input string too long to convert or too short to be valid.
 *   -2  non-hex digit input.
 */

int convertHexToInt ( U_Int_32 *dest, char *source )
{
int slen;
int i;
char *sp;
char b1;
unsigned char n1;

    slen = (int) strlen ( source );
    if ( ( slen > 8 ) || ( slen < 4 ) )
    {
        return ( -1 );
    }
    sp = source;
    *dest = 0;
    for ( i = 0; i < slen; i++ )
    {
        *dest <<= 4;
        b1 = (char)toupper(*sp++);
        if ( ( b1 >= 0x41 ) && ( b1 <= 0x46 ) )
        {
            n1 = (unsigned char)(b1 - 55);
        }
        else if ( ( b1 >= 0x30 ) && ( b1 <= 0x39 ) )
        {
            n1 = (unsigned char)(b1 - 48);
        }
        else
        {
            return ( -2 );
        }
        *dest |= n1;
    }

    return 0 ;
}

/*
 * br_Evaluate
 *
 * Use the error checking strtol() function, rather
 * that atoi(), to provide a modicum of validation for
 * bad data in the test case file.
 *
 * This routine does not bother checking underflow
 * and overflow cases or the cast, because it is far more likely
 * that typos or missing data will be the source of
 * the problem, rather than monstrously large numbers.
 */

int br_Evaluate ( char *data, int *outval )
{
char *endptr;
long val;

    errno = 0;
    val = strtol ( data, &endptr, 10 );
    if ( ( errno != 0 ) && ( val == 0 ) )
    {
        *outval = 0;
        return ( -1 );
    }
    if ( endptr == data )
    {
        *outval = 0;
        return ( -1 );
    }
    *outval = (int)val;
    return ( 1 );
}

/***********************************************************/

/*
 * SECTION: Field parsing utility routines.
 */

#define DELIM ';'

/*
 * copyField
 *
 * Copy one semicolon-delimited field from a line parsed from
 * a UCD-style property file.
 *
 * This routine does not stop on spaces, but *does* stop on
 * EOLN characters, which are not trimmed from lines by
 * the generic line parsers.
 *
 * This routine, like all others in this set of utilities,
 * returns a pointer to the start of the *next* field in
 * in the src string, so it can be called repeated to parse
 * out multiple fields.
 *
 * Check for s = "" or *s = '\0' as an end of input condition.
 */

char *copyField ( char *dest, char *src )
{
char *sp;
char *dp;

    sp = src;
    dp = dest;
    while ( (*sp != DELIM) && (*sp != '\0') && (*sp != '\n') )
    {
        *dp++ = *sp++;
    }
    *dp = '\0';
    if ( ( *sp == DELIM ) || ( *sp == '\n') )
    {
        sp++;
    }
    return sp;
}

/*
 * copySubField
 *
 * As for copyField, but copies one space-delimited subfield
 * out of a multi-value field.
 *
 * This is used, for instance, to parse out a sequence of
 * levels, one at a time.
 *
 * This routine does not check for EOLN, because it assumes
 * the src string is itself an already-parsed null-delimited
 * string.
 */

char *copySubField ( char *dest, char *src )
{
char *sp;
char *dp;

    sp = src;
    dp = dest;
    while ( (*sp != ' ') && (*sp != '\0') )
    {
        *dp++ = *sp++;
    }
    *dp = '\0';
    if ( ( *sp == ' ' ) )
    {
        sp++;
    }
    return sp;
}

/***********************************************************/

/*
 * SECTION: Property alias parsing utility routines.
 */

/*
 * br_GetBCFromLabel
 *
 * Get a Bidi_Class enumerated value from a string parsed
 * from a bidi field in a test case, from UnicodeData.txt,
 * or some other source. This supports turning the
 * symbolic alias (assumed here to be expressed in
 * uppercase, as in UnicodeData.txt) into a defined
 * enumerated value, for use in further processing.
 */

int br_GetBCFromLabel ( char* src )
{
    if ( ( strlen ( src ) > 3 ) || ( strlen ( src ) < 1 ) )
    {
        return ( BIDI_Unknown );
    }
    switch ( src[0] )
    {
    case 'L':
        if ( strlen ( src ) == 1 )
            return ( BIDI_L );
        else if ( strcmp ( src, "LRE" ) == 0 )
            return ( BIDI_LRE );
        else if ( strcmp ( src, "LRO" ) == 0 )
            return ( BIDI_LRO );
        else if ( strcmp ( src, "LRI" ) == 0 )
            return ( BIDI_LRI );
        else return ( BIDI_Unknown );
    case 'R':
        if ( strlen ( src ) == 1 )
            return ( BIDI_R );
        else if ( strcmp ( src, "RLE" ) == 0 )
            return ( BIDI_RLE );
        else if ( strcmp ( src, "RLO" ) == 0 )
            return ( BIDI_RLO );
        else if ( strcmp ( src, "RLI" ) == 0 )
            return ( BIDI_RLI );
        else return ( BIDI_Unknown );
    case 'B':
        if ( strlen ( src ) == 1 )
            return ( BIDI_B );
        else if ( strcmp ( src, "BN" ) == 0 )
            return ( BIDI_BN );
        else return ( BIDI_Unknown );
    case 'S': return ( BIDI_S );
    case 'P':
        if ( strcmp ( src, "PDF" ) == 0 )
            return ( BIDI_PDF );
        if ( strcmp ( src, "PDI" ) == 0 )
            return ( BIDI_PDI );
        else return ( BIDI_Unknown );
    case 'N':
        if ( strcmp ( src, "NSM" ) == 0 )
            return ( BIDI_NSM );
        else return ( BIDI_Unknown );
    case 'F':
        if ( strcmp ( src, "FSI" ) == 0 )
            return ( BIDI_FSI );
        else return ( BIDI_Unknown );
    case 'E':
        switch ( src[1] )
        {
        case 'N' : return ( BIDI_EN );
        case 'S' : return ( BIDI_ES );
        case 'T' : return ( BIDI_ET );
        default  : return ( BIDI_Unknown );
        }
    case 'A':
        switch ( src[1] )
        {
        case 'N' : return ( BIDI_AN );
        case 'L' : return ( BIDI_AL );
        default  : return ( BIDI_Unknown );
        }
    case 'C':
        switch ( src[1] )
        {
        case 'S' : return ( BIDI_CS );
        default  : return ( BIDI_Unknown );
        }
    case 'W':
        switch ( src[1] )
        {
        case 'S' : return ( BIDI_WS );
        default  : return ( BIDI_Unknown );
        }
    case 'O':
        switch ( src[1] )
        {
        case 'N' : return ( BIDI_ON );
        default  : return ( BIDI_Unknown );
        }
    default : return ( BIDI_Unknown );
    }
}

