// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
/*
**      Unilib
**      Copyright 2023
**      Ken Whistler, All rights reserved.
*/

/*
 * unisift.c
 *
 * Unicode Default Collation Key Weight Generator.
 *
 * Recent change history:
 *   2016-Jun-07 Small updates for Marc Lodewijck review issues in CTT.
 *   2016-Jul-07 Continuing small updates for Lodewijck review issues.
 *   2016-Aug-09 Continuing small updates for Lodewijck review issues.
 *   2017-Feb-09 Updates for Unicode 10.0.
 *   2017-Mar-01 Continued updates for Unicode 10.0.
 *   2017-Apr-26 Remove special case tertiary processing for U+1B001.
 *   2017-Jun-22 More CTT fixes for UCA 10.0.
 *   2018-Feb-19 Updates for Unicode 11.0.
 *   2018-Oct-04 Corrected two notes about identity of spanned fields in input.
 *               Added note clarifying the main sift routine's handling of reserved
 *               PUA code points in the range F8F0..F8FF.
 *   2018-Oct-05 Add exception branch for a few odd gc=Mc diacritic marks.
 *               Add more exceptional gc=Lm diacritic marks and more
 *               exceptional number letters (gc=Nl) to the
 *               isAlphabeticException list.
 *   2018-Oct-08 Add 02EC to isAlphabeticException list.
 *   2018-Oct-11 Updates for Unicode 12.0.
 *   2018-Oct-16 Update kana handling for Small Kana Extensions block.
 *               Code warning removals.
 *   2019-Jan-25 Update copyright year to 2019 for Unicode 12.0.
 *   2019-Mar-11 Updates for Unicode 12.1.
 *   2019-Oct-05 Updates for Unicode 13.0.
 *   2020-Jan-28 Tweaked isAlphabeticExceiption for 16FF0, 16FF1.
 *               Updated copyright year to 2020 for Unicode 13.0.
 *   2021-May-11 Updates for Unicode 14.0.
 *   2021-Jun-07 Added exceptions for Katakana Minnan tone letters
 *               to the isAlphabeticException list.
 *               Updates to unisift_GetKatakanaBase to account for
 *               4 new archaic kana: U+1B11F..U+1B122.
 *   2021-Jul-06 Fix for 3 local array overflows for sprintf in getName().
 *   2021-Jul-12 Bump version number for memory leak fix in unisyms.c.
 *   2022-May-12 Updates for Unicode 15.0.
 *   2023-Jan-31 Updates for Unicode 15.1.
 */

/*
 * Strategy:
 *
 * Parse unidata.txt.
 *
 * On first pass, extract fields:
 * 0 : Codepoint in 4-digit hex   
 * 1 : Unicode Character Name
 * 3 : Character decomposition
 *
 * Identify characters as having a decomposition or not. Those which
 * have a decomposition wait for the second pass. For those which do
 * not, weightings are assigned automatically as follows.
 *   1. For base letters, assign ascending primary key weights as
 *      they are encountered. Also identify case pairs, and assign
 *      tertiary weightings for case.
 *   2. For combining marks, assign ascending secondary key weights as
 *      they are encountered.
 *
 * The second pass processes the WALNUTS array sequentially, searching
 * for characters with decompositions. These are treated as follows.
 *
 *   1. Canonical (single) equivalences are weighted exactly as their
 *      equivalent.
 *   2. Canonical sequences are recursively decomposed, and then are
 *      weighted by the primary and tertiary keys of the base and the
 *      secondary keys of the combining character sequence.
 *   3. Compatibility (single) equivalences are weighted as for their
 *      baseform, with additional tertiary weighting by the
 *      class of compatibility.
 *   4. Compatibility sequences are exploded to a sequence of keys.
 *   5. A number of special cases are built in to deal with unusual
 *      decompositions.
 *
 * The unidata.col data file is marked up with IGNOREON and IGNOREOFF
 * directives to control handling of ignorables. Most ignorables are
 * specified by Unicode character property classes, but exceptions
 * can be specifically marked in the data file.
 *
 * Outputs:
 *
 *    basekeys.txt
 *
 *       Listing of key weighting and name of character for characters
 *       which are not decomposed into more than one sort key element for
 *       weighting. Information about compatibility and/or canonical
 *       equivalences are suffixed to character names, where appropriate.
 *
 *    compkeys.txt
 *
 *       Listing of key weightings and name of character for characters
 *       which are exploded into a sequence of sort key elements for
 *       weighting. These are mostly the result of compatibility
 *       decompositions.
 *
 *    ctrckeys.txt
 *
 *       Listing of key weights and name of character for contractions
 *       (of canonical equivalent sequences) that are weighted identically
 *       to a precomposed character.
 *
 *    decomps.txt
 *
 *       Generated only when started with the -t flag.
 *       This is a formatted listing, in Unicode numeric order,
 *       of just those characters with decompositions, with the
 *       following format:
 *
 * 00BE VULGAR FRACTION THREE QUARTERS
 *   <fraction>
 *   0033 DIGIT THREE
 *   2044 FRACTION SLASH
 *   0034 DIGIT FOUR
 * 
 * 00C0 LATIN CAPITAL LETTER A WITH GRAVE
 *   0041 LATIN CAPITAL LETTER A
 *   0300 COMBINING GRAVE ACCENT
 * 
 *       etc. Full names are provided for the character elements of
 *       the decompositions, to make it easy to check their values and
 *       validity.
 *
 *       A new format has been created for decomps.txt as of 2012-07-20,
 *       to make it easier for parsers to deal with the data. This
 *       format is as follows, with the original name and the decomposition
 *       names appended as part of the line comment field.
 *
 * 00BE;<fraction>;0030 2044 0034 # { NAME } => { NAME } + { NAME } ...
 * 00C0;;0041 0300 # { NAME } => { NAME } + { NAME }
 *
 *    allkeys.txt
 *
 *       Generated only when started with the -s flag. This is a roughly
 *       sorted merge of all of the collation element records, including
 *       basekeys.txt, compkeys.txt, and ctrckeys.txt in one file.
 *       This is the file now used for UCA data releases.
 *
 *    ctt14651.txt
 *
 *       Generated only when started with the -s flag flag.
 *       This is 14651 table dump of the weightings,
 *       using symbols instead of absolute numeric weightings.
 *       It is generated in strict synchrony with the contents of
 *       allkeys.txt and in roughly the same order for the weighted
 *       elements themselves.
 *
 */

#define _CRT_SECURE_NO_WARNINGS
#include <assert.h>
#include <string.h>
#include <ctype.h>
#include <stdlib.h>
#include <malloc.h>

#include <time.h>

#include "unisift.h"

#define PATHNAMELEN (256)
#define LONGESTARG  (256)

static char versionString[] = "Sifter version 15.1.0d1, 2023-01-31\n";

static char unidatafilename[] = "unidata-15.1.0.txt";
static char allkeysfilename[] = "allkeys-15.1.0.txt";
static char decompsfilename[] = "decomps-15.1.0.txt";

static char versionstring[] = "@version 15.1.0\n\n";

#define COPYRIGHTYEAR (2023)

#define defaultInfile "unidata.txt"

char infile[PATHNAMELEN];
char outfile1[] = "basekeys.txt"; /* sortable list of base keys */
char outfile2[] = "compkeys.txt"; /* sortable list of composite keys */
char outfile3[] = "decomps.txt";  /* formatted list of full decompositions */
char outfile5[] = "ctt14651.txt"; /* symbolic weight strings for ISO 14651 */
char outfile7[] = "ctrckeys.txt"; /* sortable list of contracted keys */
char outfile8[] = "allkeys.txt";  /* sorted list of all keys */

char outpath[PATHNAMELEN];

FILE *fdo1;
FILE *fdo2;
FILE *fdo3;
FILE *fdo5;
FILE *fdo6;
FILE *fdo7;

int badValues;
int badChars;
int badNames;
int numCanonicals;
int numCompatibles;
int numCanonicalSequences;
int numCanonicalCombSequences;
int numCompatibleSequencesArab;
int numCompatibleSequences;
int numIgnorables;

int ignoreOn;    /* Override flag, forces an entry to be ignorable */
int ignoreOff;   /* Override flag, forces an ignorable class to be weighted */
int contractOn;  /* Override flag, forces a precomposed character to be
                    weighted as a unit, and weights the decomposition as a unit */

int debugLevel;
int debugCounter;
int dumpDecomps; /* True if dumping formatted decomposition list */
int dumpSymbols; /* True if dumping POSIX-style symbols list */

typedef enum { DFORM_Classic, DFORM_New } DumpFileFormatType;

DumpFileFormatType dumpFileFormat;

/*
 * These arrays predefine tertiary weights for lowercase and uppercase
 * characters, with or without various format markings. Indexed by the
 * token type parsed from the decomp string.
 *
 * Treat <sort> and <sortcanon> tokens as having generic <compat>
 * weights, rather than as completely equivalent to CHAR tokens.
 */
int lowerTertiaryWeights[24] =
    {0,0,2, 5,27,23,24,25,26, 6,20,21,22,3,18,15,28, 4,30,16,19, 4, 4};
int upperTertiaryWeights[24] =
    {0,0,8,11,27,23,24,25,26,12,29,29,22,9,18,15,29,10,30,16,19,10,10};

/*
 * Note: April 6, 2005, for Unicode 4.0, superscript and subscript
 * uppercase values are mapped to 29 = square uppercase. This allows
 * for a systematic distinction in particular for lowercase versus
 * uppercase modifier letters, which have been introduced into the
 * standard now for phonetic letters. 29 is overloaded, because it
 * is very, very unlikely that any character could be superscript,
 * subscript, and square at the same time!
 */

const char *tags[] = { "NULL", "FORMAT", "CHAR", "<font>", "<noBreak>",
                 "<initial>", "<medial>", "<final>", "<isolated>",
                 "<circle>", "<super>", "<sub>", "<vertical>",
                 "<wide>", "<narrow>", "<small>", "<square>", "<compat>",
                 "<fraction>", "<smallnarrow>", "<circlekata>",
                 "<sort>", "<sortcanon>" };

/*
 * Digits are handled differently than other characters, so that digits
 * of the same numeric value are all grouped with the same primary weight.
 *
 * Ths array is initialized the first time a digit is hit.
 */

int digitPrimaryWeights[10];

int digitsInitialized;

/*
 * Constants defining Unihan ranges.
 */

#define CJK_URO_FIRST  (0x4E00)
#define CJK_URO_LAST   (0x9FFF)
#define CJK_EXTA_FIRST (0x3400)
#define CJK_EXTA_LAST  (0x4DBF)
#define CJK_EXTB_FIRST (0x20000)
#define CJK_EXTB_LAST  (0x2A6DF)
#define CJK_EXTC_FIRST (0x2A700)
#define CJK_EXTC_LAST  (0x2B739)
#define CJK_EXTD_FIRST (0x2B740)
#define CJK_EXTD_LAST  (0x2B81D)
#define CJK_EXTE_FIRST (0x2B820)
#define CJK_EXTE_LAST  (0x2CEA1)
#define CJK_EXTF_FIRST (0x2CEB0)
#define CJK_EXTF_LAST  (0x2EBE0)
#define CJK_EXTG_FIRST (0x30000)
#define CJK_EXTG_LAST  (0x3134A)
#define CJK_EXTH_FIRST (0x31350)
#define CJK_EXTH_LAST  (0x323AF)

/*
 * Constants defining other ideographic ranges for implicit weights.
 */

#define TANGUT_FIRST      (0x17000)
#define TANGUT_LAST       (0x187F7)
#define TANGUT_COMP_FIRST (0x18800)
#define TANGUT_COMP_LAST  (0x18AFF)
#define KHITAN_FIRST      (0x18B00)
#define KHITAN_LAST       (0x18CD5)
#define TANGUT_SUP_FIRST  (0x18D00)
#define TANGUT_SUP_LAST   (0x18D08)
#define NUSHU_FIRST       (0x1B170)
#define NUSHU_LAST        (0x1B2FB)

/*
 * Constants used to branch processing by decomposition type.
 */
#define WK_Unassigned (0)
#define WK_Character  (1)
#define WK_CanonEquiv (2)
#define WK_CompEquiv  (3)
#define WK_CanonEquivSeq (4)
#define WK_CompEquivSeq (5)
#define WK_CanonEquivCombSeq (6)
#define WK_CompEquivSeqArab (8)
#define WK_Ideograph (9)
#define WK_CompEquivSeqLDot (10)

/*
 * Current values of primary and secondary weights. These
 * values are incremented systematically as weights are
 * assigned to characters on the sift pass.
 */
int currentPrimary;
int currentSecondary;

/*
 * Arrays to store basic data for each character.
 */
WALNUTS planes01[0x20000];  /* 2048 K  Inefficient, but gets the job done. */

WALNUTS plane2[0x800];   /* Only needed for compatibility CJK range. */

WALNUTS plane14[0x200];  /* Just for tag and variation selector characters. */

WALNUTS handata;         /* Static value shared for all unified CJK. */

/* The handata record, for now, is also shared with Tangut & Nushu. */

/***************************************************************************/

/*
 * SECTION: Utility routines.
 */

/*
 * Convert an "ASCII" representation of sb/mb int to real int.
 *
 * Bounded to 0 .. 0xFFFFFFFF.
 *
 * i.e. "c2ba" to 0xC2BA, "8000000A" to 0x8000000A, etc.
 *
 * This routine can now 5 byte hex, as well as 4, 6, or 8.
 *
 * RETURNS:
 *    0  converted o.k.
 *   -1  input string too long to convert or too short to be valid.
 *   -2  non-hex digit input.
 */
static int convertHexToInt ( UInt32 *dest, char *source )
{
int slen;
int i;
char *sp;
char b1;
char n1;

    slen = (int)strlen ( source );
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
            n1 = (UChar8)(b1 - 55);
        }
        else if ( ( b1 >= 0x30 ) && ( b1 <= 0x39 ) )
        {
            n1 = (UChar8)(b1 - 48);
        }
        else
        {
            return ( -2 );
        }
        *dest |= n1;
    }

    return 0 ;
}

char *GetUnidataFileName ( void )
{
    return ( unidatafilename );
}

/***********************************************************/

/*
 * SECTION: Hacks.
 *
 * I give up. Just list the exceptions and be done with it.
 */

/*
 * decompIsHinky is a complete hack just to pick up the
 * 6 predetermined cases where the character has a canonical
 * decomposition to a sequence which in turn has a compatibility
 * decomposition, and which therefore require special
 * handling to get the non-spacing mark weighted in such
 * a way as to preserve weights across normalization forms.
 *
 * 2005-Apr-13: Add 01E2, 01E3, 01FC, 01FD, because those
 * have been *forced* to have hinky decompositions now in
 * the entry file.
 */ 
static int decompIsHinky ( WALNUTPTR t )
{
    switch ( t->uvalue )
    {
    case 0x03D3 :
    case 0x03D4 :
    case 0x01E2 :
    case 0x01E3 :
    case 0x01FC :
    case 0x01FD :
    case 0x1E9B :
    case 0xFB1F :
    case 0xFB3A :
    case 0xFB43 : return ( 1 );
    default:      return ( 0 );
    }
}

/***********************************************************/

/*
 * SECTION: Array access utility routines.
 *
 * Abstract the access to the WALNUT array by code point.
 */

WALNUTPTR getSiftDataPtr ( UInt32 i )
{
    if ( i < CJK_EXTA_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= CJK_EXTA_LAST )
    {
        return ( &handata );
    }
    else if ( i < CJK_URO_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= CJK_URO_LAST )
    {
        return ( &handata );
    }
    /* 
     * For now, treat Tangut, Khitan & Nushu equivalently to Han data
     * These are not given explicit weights in the table,
     * as for Han, but instead get marked for implicit
     * weighting.
     */
    else if ( i < TANGUT_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= TANGUT_LAST )
    {
        return ( &handata );
    }
    else if ( i < TANGUT_COMP_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= TANGUT_COMP_LAST )
    {
        return ( &handata );
    }
    else if ( i < KHITAN_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= KHITAN_LAST )
    {
        return ( &handata );
    }
    else if ( i < TANGUT_SUP_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= TANGUT_SUP_LAST )
    {
        return ( &handata );
    }
    else if ( i < NUSHU_FIRST )
    {
        return ( &(planes01[i] ) );
    }
    else if ( i <= NUSHU_LAST )
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
    else if ( i <= CJK_EXTG_LAST )
    {
        return ( &handata );
    }
    else if ( i <= CJK_EXTH_LAST )
    {
        return ( &handata );
    }
    else if ( ( i >= 0xE0000 ) && ( i <= 0xE01FF ) )
    {
        return ( &(plane14[i - 0xE0000]) );
    }
    else
    {
        return ((WALNUTPTR)NULL);
    }
}

/***********************************************************/

/*
 * SECTION: More utility routines.
 */


/*
 * Get a name for a Unicode character by looking up the value
 * now stored in the WALNUTS array.
 */
static void getName ( char *dp, UInt32 c )
{
WALNUTPTR tmp;
char localbuf[20];

    tmp = getSiftDataPtr ( c );

    if ( tmp == NULL )
    {
        strcpy ( dp, "!!BAD NAME!!" );
        printf ( "Bad Name in getName for: %04X\n", c );
        badNames++;
        return;
    }

    if ( tmp->haveData == 0 )
    {
        if ( ( c >= 0xAC00 ) && ( c <= 0xD7A3 ) )
        {
            sprintf ( localbuf, "HANGUL%04X", c );
            strcpy ( dp, localbuf );
        }
        else
        {
            strcpy ( dp, "!!BAD CHAR!!" );
            badChars++;
        }
    }
    else if ( tmp->name == NULL )
    {
        if ( ( ( c >= CJK_URO_FIRST ) && ( c <= CJK_URO_LAST ) ) ||
             ( ( c >= 0xF900 ) && ( c <= 0xFAD9 ) ) ||
             ( ( c >= CJK_EXTA_FIRST ) && ( c <= CJK_EXTA_LAST ) ) ||
             ( ( c >= CJK_EXTB_FIRST ) && ( c <= CJK_EXTB_LAST ) ) ||
             ( ( c >= CJK_EXTC_FIRST ) && ( c <= CJK_EXTC_LAST ) ) ||
             ( ( c >= CJK_EXTD_FIRST ) && ( c <= CJK_EXTD_LAST ) ) ||
             ( ( c >= CJK_EXTE_FIRST ) && ( c <= CJK_EXTE_LAST ) ) ||
             ( ( c >= CJK_EXTF_FIRST ) && ( c <= CJK_EXTF_LAST ) ) ||
             ( ( c >= CJK_EXTG_FIRST ) && ( c <= CJK_EXTG_LAST ) ) ||
             ( ( c >= CJK_EXTH_FIRST ) && ( c <= CJK_EXTH_LAST ) ) )
        {
            sprintf ( localbuf, "HAN%04X", c );
            strcpy ( dp, localbuf );
        }
        else if ( ( ( c >= TANGUT_FIRST ) && ( c <= TANGUT_LAST ) ) ||
                    ( ( c >= TANGUT_COMP_FIRST ) && ( c <= TANGUT_COMP_LAST ) ) ||
                    ( ( c >= TANGUT_SUP_FIRST ) && ( c <= TANGUT_SUP_LAST ) ) )
        {
            sprintf ( localbuf, "TANGUT%04X", c );
            strcpy ( dp, localbuf );
        }
        else if ( ( c >= NUSHU_FIRST ) && ( c <= NUSHU_LAST ) )
        {
            sprintf ( localbuf, "NUSHU%04X", c );
            strcpy ( dp, localbuf );
        }
        else if ( ( c >= KHITAN_FIRST ) && ( c <= KHITAN_LAST ) )
        {
            sprintf ( localbuf, "KHITAN%04X", c );
            strcpy ( dp, localbuf );
        }
        else
        {
            strcpy ( dp, "!!BAD NAME!!" );
            printf ( "Bad Name in getName for: %04X\n", c );
            badNames++;
        }
    }
    else
    {
        strcpy ( dp, tmp->name );
    }
}

/*
 * Calculate and return a primary weighting.
 *
 * For most types of characters, just return currentPrimary and
 * increment it by one.
 *
 * Intercalate three empty positions after
 * each alphabetic base character for most Latin and Cyrillic
 * to allow for n->1 weighting for the fairly common Latin digraphs
 * which impact collation order. Digraphs can then be intercalated into
 * the table without having to shift primary weights. The gaps also make
 * it easier to reorder the baseform letters (e.g. to move a-ring for
 * Scandinavian) without reweighting the basic alphabets.
 *
 * Note that the Latin for Extended-C and Extended-D and the Cyrillic
 * extensions are not included in these ranges, as they are generally
 * too abstruse to bother with for this kind of intercalation hacking.
 */
static int getNextPrimary ( UInt32 cc )
{
int temp;

    if ( cc == 0 )
    {
        return currentPrimary++;
    }
    else if ( ( cc <= 0x02AF ) || ( ( cc >= 0x0400 ) && ( cc <= 0x04FF ) ) )
    {
        temp = currentPrimary;
        currentPrimary += 4;
        return ( temp );
    }
    else
    {
        return currentPrimary++;
    }
}

/*
 * Calculate and return a secondary weighting.
 *
 * This function has hard-coded gaps built in to account for the
 * interpolation of accent combinations. 
 *
 * These gaps need to be revisited if new combinations of accents
 * are introduced into the encoding--or the whole scheme needs to
 * be generalized to calculate these gaps automatically.
 *
 * 2012-Nov-26: All secondary gaps have been removed. They are no
 * longer needed at all. The UCA implementation long ago changed
 * over to use explicit sequences of secondary weights. The gaps
 * here have been recovered to compact the secondary weight space.
 */
static int getNextSecondary ( UInt32 cc )
{
int temp;

    if ( ( debugLevel > 2 ) && ( currentSecondary >= 215 ) )
    {
        printf ( "Hit getNextSecondary with currentSecondary = %d, cc = %04X\n",
            currentSecondary, cc );
    }

    temp = currentSecondary++;
    return ( temp );
}

/*
 * A little housekeeping access function, to enable the
 * doDecomps routine to tell whether a particular secondary
 * weight is a real diacritic or a constructed secondary.
 */
static int getFirstVariantSecWeight ( void )
{
    return ( ( getSiftDataPtr ( 0xF8F0 ) )->level2 );
}

/*
 * Initialize the primary and secondary weights for digits.
 */
static void InitializeDigits ( void )
{
int i;

    if ( debugLevel > 2 )
    {
        printf ( "InitializeDigits with currentSecondary = %d\n",
            currentSecondary );
    }
    for ( i = 0; i < 10; i++ )
    {
        digitPrimaryWeights[i] = getNextPrimary( 0 );
    }
}

/*
 * Identify a token type from the format string in a decomp.
 */
static int getTokenType ( char *sp )
{
    if ( strstr ( sp, "compat" ) != NULL )
    {
        return CompatToken;
    }
    else if ( strstr ( sp, "square" ) != NULL )
    {
        return SquareToken;
    }
    else if ( strstr ( sp, "font" ) != NULL )
    {
        return FontToken;
    }
    else if ( strstr ( sp, "initial" ) != NULL )
    {
        return InitialToken;
    }
    else if ( strstr ( sp, "medial" ) != NULL )
    {
        return MedialToken;
    }
    else if ( strstr ( sp, "final" ) != NULL )
    {
        return FinalToken;
    }
    else if ( strstr ( sp, "isolated" ) != NULL )
    {
        return IsolatedToken;
    }
    else if ( strstr ( sp, "circlekata" ) != NULL )
    {
        return CircleKataToken;
    }
    else if ( strstr ( sp, "circle" ) != NULL )
    {
        return CircleToken;
    }
    else if ( strstr ( sp, "wide" ) != NULL )
    {
        return WideToken;
    }
    else if ( strstr ( sp, "smallnarrow" ) != NULL )
    {
        return SmallNarrowToken;
    }
    else if ( strstr ( sp, "narrow" ) != NULL )
    {
        return NarrowToken;
    }
    else if ( strstr ( sp, "super" ) != NULL )
    {
        return SuperToken;
    }
    else if ( strstr ( sp, "sub" ) != NULL )
    {
        return SubToken;
    }
    else if ( strstr ( sp, "vertical" ) != NULL )
    {
        return VerticalToken;
    }
    else if ( strstr ( sp, "small" ) != NULL )
    {
        return SmallToken;
    }
    else if ( strstr ( sp, "noBreak" ) != NULL )
    {
        return NoBreakToken;
    }
    else if ( strstr ( sp, "fraction" ) != NULL )
    {
        return FractionToken;
    }
    else if ( strstr ( sp, "sort" ) != NULL )
    {
        return SortToken;
    }
    else
    {
        printf ("Bad Parse! %s\n", sp);
        return FormatToken;
    }
}

/***************************************************************************/

/*
 * SECTION: Recursive processing of decomposition string.
 */

/*
 * Parse a token from the decomposition string.
 * For a format token (e.g. "<font>"), simply report the token type.
 * For a char token (e.g. 2012), convert to
 * a unichar and output in the c parameter. Report the token type
 * itself in tokenT, and return a pointer to the next position
 * in the decomp string.
 */

static char *parseToken ( char *sp, int *tokenT, UInt32 *c )
{
int localToken;
int rc;
UInt32 uvalue;
char *dp;
char localbuf[80];

    localToken = NullToken;
    dp = localbuf;

    /*
     * Stop on commas, as well as spaces, so as not to
     * misinterpret tokens when secondary decompositions
     * are present in a decomp string entry.
     */
    while ( (*sp != ' ') && (*sp != ',') && (*sp != '\0') )
    {
        if ( localToken == NullToken )
        {
            if ( *sp == '<' )
                localToken = getTokenType ( sp );
            else
                localToken = CharToken;
        }
        *dp++ = *sp++;
    }
    *dp = '\0';

    if ( localToken == CharToken )
    {
        rc = convertHexToInt ( &uvalue, localbuf );
        if ( rc < 0 )
        {
            badValues++;
            printf ( "Bad Value in parseToken: %s\n", localbuf );
        }
        else
        {
            *c = uvalue;
        }
    }
    /* 
     * Span terminal spaces. Do *not* span terminal commas,
     * so that the next call to parseToken for a string
     * starting with a comma will return nullToken.
     */
    while ( (*sp == ' ') && (*sp != '\0') ) sp++;
    *tokenT = localToken;
    return ( sp );
}

/*
 * doRecursiveDecomp()
 *
 * Do a recursive decomposition of the decomposition
 * string from the UnicodeData file, passed in as parameter sp.
 *
 * NB: This code is very tricky. Do not mess with it lightly!
 *
 * Return the decomposition in the UInt32 *d, treated as an output
 * buffer, whose length is bounded by dlen. (Effectively, the
 * processing here is done in UTF-32, for simplicity.)
 *
 * If the decomposition is a compatibility decomposition, interpret the
 * tag from the decomposition string as a token and return that value
 * in the token parameter.
 *
 * Return the length of the constructed decomposition in code points, or
 * 0 if an error is encountered.
 *
 * The function is designed so that it can be called recursively to
 * further decompose elements which themselves have decompositions.
 *
 */
static int doRecursiveDecomp ( UInt32 *d, int dlen, char *sp, int *token,
                               UInt32 scalarValue )
{
char *s;
char *newsp;
int tokenType;
UInt32 cc;
int numCharTokens;
int totalCharTokens;
int compatibility;
int i;
UInt32 *ss; 
UInt32 *sd;
UInt32 *si;
UInt32 buf[30];  /* decomp string stored as UTF-32 */
UInt32 buf2[30];
UInt32 buf3[60]; /* temporary hold for constructed decomp */

    numCharTokens = 0;
    compatibility = 0;
/*
 * Parse the decomposition string, storing character tokens in buf
 * and interpreting any compatibility tag found at the beginning of
 * the decomposition string.
 */
    if ( sp == NULL )
    {
        badValues++;
        printf ( "Bad Value Null Decomp: %04X\n", scalarValue );
        return ( 0 );
    }
    s = sp;
    while ( 1 )
    {
        newsp = parseToken ( s, &tokenType, &cc );
        if ( tokenType == NullToken )
        {
            break;
        }
        else
        {
            if ( tokenType == CharToken )
            {
                if ( ( numCharTokens == 29 ) || ( numCharTokens >= dlen ) )
                {
                    badValues++;
                    printf ( "Bad Value: %s\n", sp );
                    break;
                }
                if ( cc == scalarValue )
                {
                    badValues++;
                    printf ( "Infinite recursion: %08X %s\n", scalarValue, 
                             sp );
                    return ( 0 );
                }
                buf[numCharTokens] = cc;
                numCharTokens++;
            }
            else
            {
                compatibility = 1;
                *token = tokenType;
            }
            s = newsp;
        }
    }
    if ( !compatibility )
    {
        *token = CharToken;
        if ( numCharTokens > 2 )
        {
            printf ("Non-binary: %s\n", sp );
        }
    }

    totalCharTokens = numCharTokens;
    sd = buf3;


/*
 * BRANCH 1 : Process each position in the decomposition string,
 *   calling doRecursiveDecomp recursively for each non-atomic token
 *   encountered.
 *
 */
    for ( i = 0, ss = buf; i < numCharTokens; i++, ss++ )
    {
    WALNUTPTR t1;

        t1 = getSiftDataPtr ( *ss );
        if ( t1 == NULL )
        {
            printf ( "Bad decomposition: %04X\n", *ss );
            return ( 0 );
        }

        if ( ( t1->decompType == Atomic ) || ( t1->decompType == Implicit ) )
        {
            /* simply copy this atomic element onto the decomp */
            /* Implicit values will be separated out into weight elements
             * later. */
            *sd++ = *ss;
        }
        else if ( t1->contract )
        {
            /* 
             * Block further recursion if the decomposition target
             * is marked for contraction in the markup file. That
             * means it will be weighted as if it were atomic. And
             * anything that decomposes to it should also have that
             * part of its decomposition weight as atomic.
             */
            *sd++ = *ss;
        }
        else
        {
        int numCharTokens2;
        int localToken;
        int j;
    
            numCharTokens2 = doRecursiveDecomp ( buf2, 30, t1->decomp, 
                &localToken, buf[0] ) ;
    
/*
 * Check if a bad value was returned from the lower call.
 */
            if ( numCharTokens2 == 0 )
            {
                badValues++;
                printf ( "Error on %04X, num tokens %d, token type %d\n", *ss,
                    numCharTokens2, localToken );
                return ( 0 );
            }
/*
 * Check if the concatenation will overrun the caller's buffer.
 */
            if ( numCharTokens2 + totalCharTokens - 1 >= dlen )
            {
                badValues++;
                printf ( "Bad Value: concatenation overrun in recursion.\n" );
                return ( 0 );
            }
/*
 * Now concatenate the decomposition from the lower call onto the string being built
 * in buf3.
 */
            for ( si = buf2, j = 0; j < numCharTokens2; j++)
            {
                *sd++ = *si++;
            }
/*
 * Propagate the token type up if it is a compatibility type and
 * *token is not already set to a compatibility type.
 *
 * This is so the top level decompType determination (based on testing
 * just the mapping line for the character) can be overridden if the
 * recursion detects a compatibility decomposition at a lower level.
 * Cf. FB1F, which recurses compatibility on canonical.
 */
            if ( ( localToken != CharToken ) && ( !compatibility ) )
            {
                *token = localToken;
            }
/*
 * Now update totalCharTokens by subtracting 1 for the character that
 * was decomposed, and adding numCharTokens2 for the decomposed string
 * it was replaced with.
 */
            totalCharTokens += ( numCharTokens2 - 1 );
        } /* if */
    } /* for */

/*
 * Now that we are out of the for loop, terminate the temporary buffer
 * (buf3) with a 32-bit NULL and then copy it
 * into the caller's buffer. Return the total length.
 */
    *sd = 0;
    for ( si = buf3, sd = d; *si != 0; )
    {
        *sd++ = *si++;
    }
    *sd = 0;
    return ( totalCharTokens );
}

/***************************************************************************/

/*
 * SECTION: Key assembly and weighting.
 *
 * This section of the code deals with the construction of actual
 * entries that are inserted into allkeys.txt.
 */

/*
 * Key templates.
 *
 * Prior to UCA 6.3, allkeys.txt was output with 4 fields.
 * Starting with UCA 6.3, allkeys.txt is now output with just 3 fields,
 * omitting the more or less useless 4th field, which isn't well-defined,
 * and which was causing confusion in the spec.
 */
#ifdef NOTDEF
static char keyTemplate1[] = "[.%04X.%04X.%04X.%04X]";
static char keyTemplate2[] = "[*%04X.%04X.%04X.%04X]";
static char keyTemplate3[] = "[.%04X.%04X.%04X.%04X][.%04X.%04X.%04X.%04X]";
#endif
static char keyTemplate1[] = "[.%04X.%04X.%04X]";
static char keyTemplate2[] = "[*%04X.%04X.%04X]";
static char keyTemplate3[] = "[.%04X.%04X.%04X][.%04X.%04X.%04X]";

/*
 * calculateImplicitBase()
 *
 * Based on a code point, determine which implicit base
 * value will be used for calculating a weight for a CJK
 * character (or unassigned code point).
 */
static void calculateImplicitBase ( UInt32 i, UShort16 *base1, UShort16 *base2 )
{
UShort16 base;

    if ( ( i >= CJK_URO_FIRST ) && ( i <= CJK_URO_LAST ) )
    {
        base = 0xFB40; /* URO */
    }
    else if ( ( i >= 0xFA0E ) && ( i <= 0xFA29 ) )
    {
        base = 0xFB40; /* URO additions in FAXX CJK compat block */
    }
    else if ( ( ( i >= CJK_EXTA_FIRST ) && ( i <= CJK_EXTA_LAST ) ) ||
              ( ( i >= CJK_EXTB_FIRST ) && ( i <= CJK_EXTB_LAST ) ) ||
              ( ( i >= CJK_EXTC_FIRST ) && ( i <= CJK_EXTC_LAST ) ) ||
              ( ( i >= CJK_EXTD_FIRST ) && ( i <= CJK_EXTD_LAST ) ) ||
              ( ( i >= CJK_EXTE_FIRST ) && ( i <= CJK_EXTE_LAST ) ) ||
              ( ( i >= CJK_EXTF_FIRST ) && ( i <= CJK_EXTF_LAST ) ) ||
              ( ( i >= CJK_EXTG_FIRST ) && ( i <= CJK_EXTG_LAST ) ) || 
              ( ( i >= CJK_EXTH_FIRST ) && ( i <= CJK_EXTH_LAST ) ) )
    {
        base = 0xFB80; /* Tangut ideographs and Tangut components */
    }
    else if ( ( ( i >= TANGUT_FIRST ) && ( i <= TANGUT_LAST ) ) ||
              ( ( i >= TANGUT_COMP_FIRST ) && ( i <= TANGUT_COMP_LAST ) ) || 
              ( ( i >= TANGUT_SUP_FIRST ) && ( i <= TANGUT_SUP_LAST ) ) )
    {
        base = 0xFB00; /* Tangut */
    }
    else if ( ( i >= NUSHU_FIRST ) && ( i <= NUSHU_LAST ) )
    {
        base = 0xFB01; /* Nushu */
    }
    else if ( ( i >= KHITAN_FIRST ) && ( i <= KHITAN_LAST ) )
    {
        base = 0xFB02; /* Khitan Small Script */
    }
    else
    {
        base = 0xFBC0; /* Other, unassigned characters */
    }
    *base1 = base + (UShort16)( i >> 15 ) ;
    *base2 = ( i & 0x7FFF ) | 0x8000;
}

/*
 * setRawKeys()
 *
 * Set rawkeys values, based on t1. This now takes into
 * account the calculation of implicit key weights.
 *
 * Returns the number of rawkeys consumed.
 *
 * Raw key generation has special a special case for Implicit
 * decompType, to parallel the key generation done
 * in assembleKey.
 */
static int setRawKeys ( RAWKEYPTR rk, WALNUTPTR t1 )
{
UShort16 base1;
UShort16 base2;
RAWKEYPTR lrk;

    if ( t1->decompType == Implicit )
    {
        calculateImplicitBase ( t1->uvalue, &base1, &base2 );
        rk->symbolBase = base1;
        rk->level1 = base1;
        rk->level2 = FIRST_SECONDARY;
        rk->level3 = t1->level3;
        rk->level4 = t1->uvalue;
        rk->variable = t1->variable;
        lrk = rk + 1;
        lrk->symbolBase = base2;
        lrk->level1 = base2;
        lrk->level2 = 0;  /* Note, NOT FIRST_SECONDARY */
        lrk->level3 = 0;  /* Note, NOT FIRST_TERTIARY */
        lrk->level4 = t1->uvalue;
        lrk->variable = t1->variable;
        return ( 2 );
    }
    else
    {
        rk->symbolBase = t1->symbolBase;
        rk->level1 = t1->level1;
        rk->level2 = t1->level2;
        rk->level3 = t1->level3;
        rk->level4 = t1->level4;
        rk->variable = t1->variable;
        return ( 1 );
    }
}

/*
 * assembleKey()
 *
 * Put a "*" in the first digit of the primary level field for variables.
 * This emulates processing with a special bit for variables, and
 * guarantees that variables will sift out first in the sorted list,
 * even when they have multilevel weightings.
 *
 * 2001-Jan-04 note: The additional test for non-zeros in levels 1, 2, and 3
 * has been added, since by strict UCA definition, the "*" marks variables,
 * rather than ignorables, and weights of the form 0000.0000.0000.XXXX
 * are always ignorable, and cannot be variables.
 *
 * For non-ignorables, use ".". This keeps the primary field at four digits
 * for both, but ensures that the non-ignorables all sort after the
 * ignorables on a simple binary sort of the weights.
 *
 * Implicit decompTypes get expanded with the base calculation into
 * two collation elements. This is for Han characters, primarily.
 */
static void assembleKey ( WALNUTPTR t1, char *buf )
{
UShort16 base1;
UShort16 base2;

    if ( t1->decompType == Implicit )
    {
        calculateImplicitBase ( t1->uvalue, &base1, &base2 );
        sprintf ( buf, keyTemplate3, base1, FIRST_SECONDARY, t1->level3, /* t1->uvalue, */ 
                  base2, 0, 0/*, t1->uvalue */ );
    }
    else if ( ( t1->variable ) && 
            !( ( t1->level1 == 0 ) && ( t1->level2 == 0 ) && ( t1->level3 == 0 ) ) )
    {
        sprintf ( buf, keyTemplate2, t1->level1, t1->level2,
                  t1->level3/*, t1->level4 */ );
    }
    else
    {
        sprintf ( buf, keyTemplate1, t1->level1, t1->level2,
                  t1->level3/*, t1->level4 */ );
    }
}

/*
 * weightFromToken()
 *
 * Based on a tokenType value and the values in the WALNUT
 * for a character, calculate an appropriate tertiary weight value.
 */
int weightFromToken ( WALNUTPTR t1, int tokenType )
{
    switch ( tokenType )
    {
    case InitialToken  :
    case MedialToken   :
    case FinalToken    :
    case IsolatedToken :
    case NarrowToken   :
    case VerticalToken :
    case FractionToken :
    case SuperToken    :
        return ( lowerTertiaryWeights[tokenType] );
/*
 * For circled compatibility forms, preserve case distinctions if
 * they are present. At the moment these should all be lower, since
 * no mixed-case decomps are present. The only circled decomps involve
 * numbers.
 */
    case CircleToken   :
        if ( t1->level3 == upperTertiaryWeights[CharToken] )
            return ( upperTertiaryWeights[CircleToken] );
        else
            return ( lowerTertiaryWeights[CircleToken] );
/*
 * For squared compatibility forms, preserve case distinctions if
 * they are present.
 */
    case SquareToken   :
        if ( t1->level3 == upperTertiaryWeights[CharToken] )
            return ( upperTertiaryWeights[SquareToken] );
        else
            return ( lowerTertiaryWeights[SquareToken] );
/*
 * For other compatibility forms, preserve case distinctions if
 * they are present.
 *
 * Combinations marked with the <sort> label get weighted as
 * if they had a <compat> label, so that they get distinguished
 * from the same sequence of weights not derived from a combination.
 * (Example: sharp-s).
 */
    case CompatToken   :
    case SortToken     :
    case SortCanonToken :
        if ( t1->level3 == upperTertiaryWeights[CharToken] )
            return ( upperTertiaryWeights[CompatToken] );
        else
            return ( lowerTertiaryWeights[CompatToken] );
    case FillerToken :
        return ( LAST_TERTIARY );
    default :
        return ( t1->level3 );
    }
}

/*
 * assembleKeyWithOverride()
 *
 * The same as assembleKey, but with a tokenType passed in to override
 * values in certain cases.
 *
 * This is to support
 *   1. distinguishing halfwidth compatibility sequences (e.g. for Hangul)
 *   2. distinguishing positional variants of Arabic ligatures
 *   3. for one vertical positional variant for Chinese.
 *   4. compatibility fractions
 *   5. squared compatibility forms
 *   6. superscript compatibility sequences
 * The tertiary weighting is done on the basis of the tokenType
 * passed in, rather than the tertiary weights of each of the individual
 * characters for just those cases.
 *
 */
static void assembleKeyWithOverride ( WALNUTPTR t1, char *buf, int tokenType )
{
int tertwt;
int secwt;
UShort16 base1;
UShort16 base2;

    tertwt = weightFromToken ( t1, tokenType );
    secwt = t1->level2;

    if ( t1->decompType == Implicit )
    {
        calculateImplicitBase ( t1->uvalue, &base1, &base2 );
        sprintf ( buf, keyTemplate3, base1, FIRST_SECONDARY, tertwt, /* t1->uvalue, */
                  base2, 0, 0/*, t1->uvalue */ );
    }
    else if ( ( t1->variable ) && 
        !( ( t1->level1 == 0 ) && ( secwt == 0 ) && ( tertwt == 0 ) ) )
    {
        sprintf ( buf, keyTemplate2, t1->level1, secwt, tertwt/*, t1->level4 */ );
    }
    else
    {
        sprintf ( buf, keyTemplate1, t1->level1, secwt, tertwt/*, t1->level4 */ );
    }
    
}

/*
 * assembleKeyExplicitWeights()
 *
 * Wrapper for doing the key assembly with an arbitrary input of
 * three weights. Simplifies the abstraction for the special processing
 * required for Catalan l-dots.
 */
static void assembleKeyExplicitWeights ( char *buf, int lvl1, int lvl2, int lvl3 )
{
    sprintf ( buf, keyTemplate1, lvl1, lvl2, lvl3 );
}

/***************************************************************************/

/*
 * SECTION: Utilities used for decomposition processing and for alpha
 * weighting.
 */

/*
 * unisift_ForceToSecondary()
 *
 * Boolean function which abstracts the determination of
 * the list of exceptional combining marks which are
 * alphabetic, but which get secondary weighting.
 */
static int unisift_ForceToSecondary ( UInt32 c )
{
/* Deal with the Semitic scripts as a range. All Hebrew & Arabic
 * vowel points will be dealt with as secondary weights. 
 * Exclude Thaana here, as Thaana vowels are treated like letters. 
 */
    if ( ( c >= 0x05B0 ) && ( c <= 0x074F ) )
    {
        return ( 1 );
    }
/*
 * Pick up Samaritan vowel points.
 */
    if ( ( c >= 0x081C ) && ( c <= 0x082C ) )
    {
        return ( 1 );
    }
/*
 * Pick up Extended Arabic vowels for African languages,
 * and more Koranic tanween.
 */
    if ( ( c >= 0x08E3 ) && ( c <= 0x08FF ) )
    {
        return ( 1 );
    }
    switch ( c )
    {
/* iota-subscript */
    case 0x0345 :
/* 
 * Bindis, anusvaras, visargas and explicit rephas in Brahmi-based scripts.
 *
 * Note that in some respects some of these should be instead equated to
 * full primaries, with visarga = ha and repha = ra. But by default, these
 * are all equally given secondary weights, and further tailoring is
 * required in any case for exact behavior.
 *
 * Starting somewhere around UCA 6.1, the bindis, anusvaras, and visargas were
 * all collapsed into equivalences with the corresponding Devanagari
 * marks, to preserve secondary numeric weighting space. Hence, the
 * further formal update of the case statement is no longer needed.
 * The values have been added up through UCA 8.0, but are commented
 * out. 
 */
/* 
 * Note that Tamil aytham U+0B83 was mistakenly named a visarga,
 * and has been corrected to be a spacing letter. It is omitted
 * from this list
 */
/*
 * First the characters for Devanagari prototypes.
 */
    case 0x0901 :
    case 0x0902 :
    case 0x0903 :
    case 0x0981 :
    case 0x0982 :
    case 0x0983 :
/*
 * A few other miscellaneous alphabetic gc=Mn marks which
 * also need to be forced to secondaries.
 */
    case 0x0A70 :
    case 0x0A71 :
    case 0x17C8 :
    case 0x1B03 :
    case 0x1B81 :
    case 0xA982 :
    case 0xFB1E :
    case 0x1E947 :
/*
 * Equivalent bindis, anusvaras, and visargas, which can be
 * omitted from the switch testing, because they are drained
 * by the equivalences to Devanagari, first.
 */
#ifdef NOTDEF
    case 0x0A01 :
    case 0x0A02 :
    case 0x0A03 :
    case 0x0A81 :
    case 0x0A82 :
    case 0x0A83 :
    case 0x0B01 :
    case 0x0B02 :
    case 0x0B03 :
    case 0x0B82 :
    case 0x0C01 :
    case 0x0C02 :
    case 0x0C03 :
    case 0x0C82 :
    case 0x0C83 :
    case 0x0CF3 :
    case 0x0D02 :
    case 0x0D03 :
    case 0x0D82 :
    case 0x0D83 :
    case 0x0F7E :    /* Tibetan */
    case 0x0F7F :
    case 0x1036 :    /* Myanmar */
    case 0x1038 :
    case 0x17C6 :    /* Khmer */
    case 0x17C7 :
    case 0x1A74 :    /* Tai Tham */
    case 0x1B00 :    /* Balinese */
    case 0x1B01 :
    case 0x1B02 :
    case 0x1B04 :
    case 0x1B80 :    /* Sundanese */
    case 0x1B82 :
    case 0xA80B :    /* Syloti Nagri */
    case 0xA880 :    /* Saurashtra */
    case 0xA881 :
    case 0xA980 :    /* Javanese */
    case 0xA981 :
    case 0xA983 :
    case 0xA9B3 :
    case 0x10A0E :   /* Kharoshthi */
    case 0x10A0F :
    case 0x11000 :   /* Brahmi */
    case 0x11001 :
    case 0x11002 :
    case 0x11080 :   /* Kaithi */
    case 0x11081 :
    case 0x11082 :
    case 0x11100 :   /* Chakma */
    case 0x11101 :
    case 0x11102 :
    case 0x11180 :   /* Sharada */
    case 0x11181 :
    case 0x11182 :
    case 0x11234 :   /* Khojki */
    case 0x112DF :   /* Khudawadi */
    case 0x11300 :   /* Grantha */
    case 0x11301 :
    case 0x11302 :
    case 0x11303 :
    case 0x114BF :   /* Tirhuta */
    case 0x114C0 :
    case 0x114C1 :
    case 0x115BC :   /* Siddham */
    case 0x115BD :
    case 0x115BE :
    case 0x1163D :   /* Modi */
    case 0x1163E :
    case 0x116AB :   /* Takri */
    case 0x116AC :
    case 0x11F00 :   /* Kawi */
    case 0x11F01 :
    case 0x11F03 :
#endif
        return ( 1 );
    default: 
        return ( 0 );

    }
}

/*
 * isEligibleMark()
 *
 * Encapsulates the checking to determine if
 * a combining mark is suitable for collapsing its secondary
 * weight onto the baseform of a combining character sequence.
 *
 * This test allows non-spacing marks that are either non-alphabetic
 * (acute, grave, etc.) or that are alphabetic marks forced to
 * secondary weighting (Semitic vowel points, Indic bindis, anusvaras,
 * and visargas).
 *
 * The practical use of this is in processDecomps, where it is used
 * to catch combining sequences in the middle of longer decompositions.
 * The exception for Semitic vowel points is to catch yod-yod-patah, for
 * example.
 */
static int isEligibleMark ( UInt32 c )
{
    if ( unisift_IsNonSpacing ( c ) &&
         ( !unisift_IsAlphabetic ( c ) || unisift_ForceToSecondary ( c ) ) )
    {
        return ( 1 );
    }
    else
    {
        return ( 0 );
    }
}

/***************************************************************************/

/*
 * SECTION: Output processing for allkeys.txt and decomps.txt.
 */

/*
 * printDiagnosticsInHeader()
 *
 * This routine prints the weight ranges calculated during a sifter run
 * into the header for allkeys.txt. That information can be parsed out
 * and used by implementations parting allkeys.txt, to optimize tables
 * and derived weight assignments, if desired.
 */
void printDiagnosticsInHeader ( FILE *fd )
{
char buffer[128];

    fputs ( "# Diagnostic weight ranges\n", fd );
    sprintf ( buffer, "# Primary weight range:   %04X..%04X (%d)\n", FIRST_PRIMARY,
                currentPrimary - 1, currentPrimary - FIRST_PRIMARY );
    fputs ( buffer, fd );
    sprintf ( buffer, "# Secondary weight range: %04X..%04X (%d)\n", FIRST_SECONDARY,
                currentSecondary - 1, currentSecondary - FIRST_SECONDARY );
    fputs ( buffer, fd );
/*
 * Currently the number of variant secondaries is fixed at the 5 variant marks
 * used, F8F0..F8F4. These will be the last 5 (highest) secondary weight values.
 */
    sprintf ( buffer, "# Variant secondaries:    %04X..%04X (%d)\n", currentSecondary - 5,
                currentSecondary - 1, 5 );
    fputs ( buffer, fd );
    sprintf ( buffer, "# Tertiary weight range:  0002..001F (30)\n" );
    fputs ( buffer, fd );
    fputs ( "#\n", fd );
}

/*
 * Print a standard header at the top of allkeys.txt.
 */

void dumpUCAProlog ( FILE *fd )
{
char localbuf[128];
time_t tempt;
struct tm *temptptr; 

    sprintf ( localbuf, "# %s\n", allkeysfilename );
    fputs ( localbuf, fd );
    time ( &tempt );
    temptptr = localtime ( &tempt );
    strftime ( localbuf, 56, "# Date: %Y-%m-%d, %H:%M:%S GMT [KW]\n", temptptr );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# Copyright %d Unicode, Inc.\n", COPYRIGHTYEAR );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# For terms of use, see https://www.unicode.org/terms_of_use.html\n#\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# This file defines the Default Unicode Collation Element Table\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "#   (DUCET) for the Unicode Collation Algorithm\n#\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# See UTS #10, Unicode Collation Algorithm, for more information.\n#\n" );
    fputs ( localbuf, fd );
    printDiagnosticsInHeader ( fd );
    fputs ( versionstring, fd );
    /* UCA 9.0 new syntax: add @implicitweights for each new ideographic range */
    sprintf ( localbuf, "@implicitweights 17000..18AFF; FB00 # Tangut and Tangut Components\n\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "@implicitweights 18D00..18D8F; FB00 # Tangut Supplement\n\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "@implicitweights 1B170..1B2FF; FB01 # Nushu\n\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "@implicitweights 18B00..18CFF; FB02 # Khitan Small Script\n\n" );
    fputs ( localbuf, fd );
}

/*
 * Print a standard header at the top of decomps.txt.
 *
 * Grab a bunch of documentation material from the file "decompshdr.txt", instead of
 * putting it all inline in the code here. The code assumes the decompshdr.txt is
 * in the current directory.
 */

int dumpDecompsFileProlog ( FILE *fd )
{
char localbuf[128];
time_t tempt;
struct tm *temptptr; 
FILE *fdi;

    sprintf ( localbuf, "# %s\n", decompsfilename );
    fputs ( localbuf, fd );
    time ( &tempt );
    temptptr = localtime ( &tempt );
    strftime ( localbuf, 56, "# Date: %Y-%m-%d, %H:%M:%S GMT [KW]\n", temptptr );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# Copyright %d Unicode, Inc.\n", COPYRIGHTYEAR );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# For terms of use, see https://www.unicode.org/terms_of_use.html\n#\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# This file lists decompositions used in generating the Default Unicode Collation Element Table\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "#   (DUCET) for the Unicode Collation Algorithm\n#\n" );
    fputs ( localbuf, fd );
    sprintf ( localbuf, "# See UTS #10, Unicode Collation Algorithm, for more information.\n#\n" );
    fputs ( localbuf, fd );
    fdi = fopen ( "decompshdr.txt", "rt" );
    if ( fdi == NULL )
    {
        printf ( "Cannot open decompshdr.txt.\n" );
        return -1 ;
    }
    while ( fgets( localbuf, 128, fdi ) != NULL )
    {
        fputs ( localbuf, fd );
    }
    fclose ( fdi );
    return ( 0 );
}

/*
 * dumpToDecompsFileClassic()
 *
 * This formats an entry to dump out to fdo3, for decomps.txt.
 * This older form is the original format used in generating
 * decomps.txt. It was mostly to aid in debugging of the processing of the input.
 */

static int dumpToDecompsFileClassic ( WALNUTPTR t1, UInt32 *decomp, int tokenType )
{
int rc;
UInt32 *s;
char localbuf[256];

    sprintf ( localbuf, "%s\t%s\n", t1->value, t1->name );
    fputs ( localbuf, fdo3 );
    if ( tokenType != CharToken )  /* Compatibility */
    {
        sprintf ( localbuf, "\t%s\n", tags[tokenType] );
        fputs ( localbuf, fdo3 );
    }
    for ( s = decomp; *s != 0; s++ )
    {
        sprintf ( localbuf, "\t%04X ", *s );
        fputs ( localbuf, fdo3 );
        getName ( localbuf, *s );
        fputs ( localbuf, fdo3 );
        fputs ( "\n", fdo3 );
    }
    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo3 );
        assert ( rc == 0 );
    }

    return ( 0 );
}


/*
 * dumpToDecompsFileNew()
 *
 * This formats an entry to dump out to fdo3, for decomps.txt.
 * This newer format is easier to parse, and is intended to
 * create a version of decomps.txt which can be released together with
 * allkeys.txt for UCA.
 */

static int dumpToDecompsFileNew ( WALNUTPTR t1, UInt32 *decomp, int tokenType )
{
int rc;
UInt32 *s;
int i;
char localbuf[256];

    sprintf ( localbuf, "%s;", t1->value );
    fputs ( localbuf, fdo3 );
    if ( tokenType != CharToken )  /* Compatibility */
    {
        sprintf ( localbuf, "%s;", tags[tokenType] );
        fputs ( localbuf, fdo3 );
    }
    else
    {
        fputs ( ";", fdo3 );
    }
    for ( s = decomp; *s != 0; s++ )
    {
        sprintf ( localbuf, "%04X ", *s );
        fputs ( localbuf, fdo3 );
    }
    fputs ( "# ", fdo3 );
    sprintf ( localbuf, "%s =>", t1->name );
    fputs ( localbuf, fdo3 );
    for ( s = decomp, i=0; *s != 0; s++, i++ )
    {
        if ( i== 0 )
        {
            fputs ( " ", fdo3 );
        }
        else
        {
            fputs ( " + ", fdo3 );
        }
        getName ( localbuf, *s );
        fputs ( localbuf, fdo3 );
    }
    fputs ( "\n", fdo3 );
    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo3 );
        assert ( rc == 0 );
    }

    return ( 0 );
}

/*
 * dumpToDecompsFile()
 *
 * This formats an entry to dump out to fdo3, for decomps.txt.
 */
static int dumpToDecompsFile ( WALNUTPTR t1, UInt32 *decomp, int tokenType )
{
    if ( dumpFileFormat == DFORM_Classic )
    {
        return ( dumpToDecompsFileClassic ( t1, decomp, tokenType ) );
    }
    else
    {
        return ( dumpToDecompsFileNew ( t1, decomp, tokenType ) );
    }
}

/***************************************************************************/

/*
 * SECTION: Output processing for characters with decompositions.
 */

/*
 * IsRomanNumeralWithIs()
 *
 * A utility hack to spell out the ranges of compatibility
 * roman numerals which have decompositions longer than 1 element,
 * which also then contain "I"s in their decompositions.
 * These are singled out for 1F last tertiary mashing, to avoid
 * overlaps.
 *
 * This is called from the decomps code only for characters with
 * non-singleton decomps, so the range checking here can be
 * quite simple.
 */

#ifdef NOTDEF
int IsRomanNumeralWithIs ( UInt32 c )
{
    if ( ( c >= 0x2161 ) && ( c <= 0x217B ) )
    {
        printf("TRACE: Roman Is, c=%04X\n", c );
        return ( 1 );
    }
    return ( 0 );
}
#endif

/*
 * processDecomps()
 *
 * This routine does the output for a character that has been
 * identified as having a decomposition (of various types).
 *
 * NB: This code is very tricky and sensitive to changes.
 * Beware changing it without understanding what is going on!
 */
static int processDecomps ( WALNUTPTR t1 )
{
int tokenType;
UInt32 cc;
UInt32 cc0;
UInt32 cc1;
int temp;
int i;
int slen;
int tlen;
int limit;
WALNUTPTR t2;
WALNUTPTR t3;
RAWKEYPTR rkptr;
UInt32 decompstr[20];
int numCharTokens;
int finalNumTokens = 0;
int compatibility;
char outputbuf[512];
char localbuf2[256];
RAWKEY rawkeys[20];

/*
 * The recursion sets the tokenType, which may differ from the stored
 * decompType for a character, since the recursion may hit a
 * compatibility decomposition underlying a canonical decomposition.
 */

    if ( t1->decomp == NULL )
    {
        printf ( "Bad call to processDecomps with null decomp: %04X\n", t1->uvalue);
        return ( 0 );
    }
    numCharTokens = doRecursiveDecomp ( decompstr, 20, t1->decomp, &tokenType,
                                        t1->uvalue );
    if ( numCharTokens == 0 )
    {
        return ( 0 );
    }

    compatibility = tokenType != CharToken;
/*
 * For characters with decompositions, even single values,
 * print out the entire decomposition in readable form in output file 3.
 */
    if ( dumpDecomps )
    {
        dumpToDecompsFile ( t1, decompstr, tokenType );
    }

/*
 * At this point, treat decompstr as the *full* decomposition.
 */

/*
 * For ease of expression in the branches that follow, give the
 * level1, level2, level3, and ignorable values reasonable defaults
 * in *all* cases, and override them below as needed.
 */
    cc = decompstr[0];  
    t3 = getSiftDataPtr ( cc );
    if ( t3 == NULL )
    {
        printf ( "SiftDataPtr NULL error %04X.\n", cc );
        return ( 0 );
    }
    if ( t3->decompType == Implicit )
    {
        t1->decompType = Implicit;
        t1->uvalue = cc; /* Implicit key calculated based on code point */
        t1->symbolBase = cc;
        t1->level2 = FIRST_SECONDARY;
        t1->level3 = FIRST_TERTIARY; /* may be overridden below for compats */
    }
    else
    {
        t1->symbolBase = t3->symbolBase;
        t1->level1 = t3->level1;
        t1->level2 = t3->level2;
        t1->level3 = t3->level3;
    }
    t1->variable = t3->variable;

/*
 * Now branch to handle compatibility or canonical decompositions.
 */

    if ( compatibility )
    {
        if ( numCharTokens > 1 )
        {
            cc0 = decompstr[0]; 
            cc1 = decompstr[1];
            if ( ( unisift_IsNonSpacing ( cc0 ) ) &&
                 ( unisift_IsNonSpacing ( cc1 ) ) &&
                 ( cc1 >= 0x064B ) && ( cc1 <= 0x0655 ) )
            {

/*
 * Bleed off a few special cases for
 * compatible equivalent sequences involving a positional
 * combination of two Arabic points.
 */
                t1->whichKind = WK_CompEquivSeqArab;
            }
            else if ( ( t1->uvalue == 0x013F ) || ( t1->uvalue == 0x0140 ) )
/*
 * Special case Catalan l-dots. These have a compatibility decomposition,
 * to 006C 00B7, which needs to be used for the contraction processing
 * later, but for weighting need to be treated as if they were decomposed
 * to an l plus a diacritic, needing special secondary weighting.
 */
            {
                t1->whichKind = WK_CompEquivSeqLDot;
            }
            else
            {
/*
 * Compatible equivalent sequences not consisting merely
 * of non-spacing Arabic combining marks or Catalan l-dots.
 * These include most compatibility sequences.
 */
                t1->whichKind = WK_CompEquivSeq;
            }
        }
        else
/* 
 * Single compatibility character. Assign level 1 and level 2
 * from the base form, then calculate whether this is an uppercase
 * letter and get the tertiary weight from the precalculated
 * table for each format token type. 
 */
        {
            t1->whichKind = WK_CompEquiv;
            if ( unisift_IsUpper(decompstr[0]) )
            {
                t1->level3 = upperTertiaryWeights[tokenType];
            }
            else
            {
                t1->level3 = lowerTertiaryWeights[tokenType];
            }
            if ( debugLevel > 1 )
            {
                printf ( "Char %04X Token %d Weight %d\n", decompstr[0],
                    tokenType, t1->level3 );
            }
        }
    }
    else
/*
 * Canonical processing.
 */
    {
        if ( numCharTokens > 1 )
        {
            cc0 = decompstr[0];
            cc1 = decompstr[1];
            if ( unisift_IsNonSpacing ( cc0 ) && !unisift_IsAlphabetic ( cc0 ) )
            {
/*
 * Canonical equivalent sequence of combining marks (e.g. dialytika tonos).
 * Actually, U+0344 is the *only* code point which hits this branch.
 * Assign level3 from the first accent.
 * Exclude non-spacing combining vowels, e.g. from Telugu, Tamil, Tibetan.
 */
                t1->whichKind = WK_CanonEquivCombSeq;
            }
            else
            {
/*
 * Test for non-alphabetic non-spacing marks, or for the particular
 * subset of alphabetic non-spacing marks that may occur in decompositions and
 * which are forced to secondary weight. This includes U+0345 combining
 * iota subscript, as well as all the Semitic vowel points (but not Thaana).
 */
                if ( isEligibleMark ( cc1 )  )
                {

/*
 * Canonical equivalent sequence involving baseform followed by one
 * or more non-spacing combining marks.
 * Exclude non-spacing combining vowels, e.g. from Telugu, Tamil, Tibetan.
 * Assign level 1 and level 3 from
 * the base form, and assign level 2 based on a weighting for the
 * combining characters.
 *
 * Also match whether the baseform is ignorable or not (for math).
 */
                    t1->whichKind = WK_CanonEquivCombSeq;
                }
                else
                {
/*
 * Canonical equivalent sequences without non-spacing combining marks.
 * This includes the Indic surroundrant vowels, which may be canonically
 * equivalent to a sequence of combining marks, but which are now bled
 * off as contractions.
 */
                    t1->whichKind = WK_CanonEquivSeq;
                }
            }
        }
        else
        {
/* Single canonical equivalent characters. Assign level 1, level2,
 * and level 3 from the base form. Level 4 should also be inherited
 * from the canonical equivalent form -- and this will ordinarily be
 * the case when the normalized form of the string is constructed
 * for key weight assignment. However, for building the table, this
 * results in duplicate collation weights and complicates the table
 * building and sorting. Just leave Level 4 as the initial value
 * of the character.
 */
            t1->whichKind = WK_CanonEquiv;
        /*  t1->level4 = getSiftDataPtr(cc)->level4;  */
        }
    } /* if compatibility else */

/*
 * PHASE 1
 *
 * Now based on the WK type, assemble key or keys.
 */

    strcpy ( outputbuf, t1->value );
    if ( strlen ( t1->value ) == 4 )
    {
        strcat ( outputbuf, " " );  /* Pad to align. */
    }
    strcat ( outputbuf, " ; " ); /* Append a field separator. */

    switch ( t1->whichKind )
    {
    case WK_CanonEquiv    :
    case WK_CompEquiv     :
        assembleKey ( t1, localbuf2 );
        finalNumTokens = setRawKeys ( rawkeys, t1 );
        strcat ( outputbuf, localbuf2 );
        break;
    case WK_CanonEquivSeq :
        t1->numChars = (UChar8)numCharTokens;
        tlen = 506; /* 512 - 6 */
        finalNumTokens = 0;
        limit = numCharTokens;
        for ( i = 0, rkptr = rawkeys; i < limit; i++ )
        {
            t2 = getSiftDataPtr(decompstr[i]);
            if ( t2 == NULL )
            {
                printf ( "SiftDataPtr NULL error in WK_CanonEquivSeq.\n" );
                return ( -1 );
            }
            /*
             * For Implicit weights (getSiftDataPtr points to handata),
             * set the uvalue and symbolBase to the code point.
             */
            if ( t2->decompType == Implicit )
            {
                t2->uvalue = decompstr[i];
                t2->symbolBase = decompstr[i];
            }
/*
 * Assemble the weighted key strings for output to compkeys.txt.
 */
            assembleKey ( t2, localbuf2 );
            temp = setRawKeys ( rkptr, t2 );
            rkptr += temp;
            finalNumTokens += temp;
            slen = (int)strlen ( localbuf2 );
            tlen -= slen;
            if ( tlen <= 0 )
            {
                printf ( "Output 3 buffer overrun! %s i= %d\n", t2->value, i );
                return ( -1 );
            }
            strcat ( outputbuf, localbuf2 );
        }
        break;
/*
 * For UCA, canonical equivalent combining sequences are treated just
 * like other canonical equivalent sequences.
 */
    case WK_CanonEquivCombSeq :
        t1->numChars = (UChar8)numCharTokens;
        tlen = 506; /* 512 - 6 */
        finalNumTokens = 0;
        limit = numCharTokens;
        for ( i = 0, rkptr = rawkeys; i < limit; i++ )
        {
            t2 = getSiftDataPtr(decompstr[i]);
            if ( t2 == NULL )
            {
                printf ( "SiftDataPtr NULL error in WK_CanonEquivCombSeq.\n" );
                return ( -1 );
            }
            /*
             * For Implicit weights (getSiftDataPtr points to handata),
             * set the uvalue and symbolBase to the code point.
             */
            if ( t2->decompType == Implicit )
            {
                t2->uvalue = decompstr[i];
                t2->symbolBase = decompstr[i];
            }
/*
 * Assemble the weighted key strings for output to compkeys.txt.
 */
            assembleKey ( t2, localbuf2 );
            temp = setRawKeys ( rkptr, t2 );
/*
 * For these combining mark sequences, inherit the variable flag
 * from the setting for the class of the original character.
 * This prevents anomalies whereby applying diacritics to variable
 * symbols will make them sort (nonignorably) by the diacritic.
 *
 * Don't worry about the implicit expansions, as they will already
 * be non-ignorables (for Han characters).
 */
            rkptr->variable = t1->variable;
            rkptr += temp;
            finalNumTokens += temp;
            slen = (int)strlen ( localbuf2 );
            tlen -= slen;
            if ( tlen <= 0 )
            {
                printf ( "Output 3 buffer overrun! %s i= %d\n", t2->value, i );
                return ( -1 );
            }
            strcat ( outputbuf, localbuf2 );
        }
        break;
/*
 * Special processing branch for the Arabic isolated and medial
 * forms of sequences of exactly two harakat.
 */
    case WK_CompEquivSeqArab :
        t1->numChars = 2;
        finalNumTokens = 2;
        for ( i = 0, rkptr = rawkeys; i < 2; i++ )
        {
            t2 = getSiftDataPtr(decompstr[i]);
            if ( t2 == NULL )
            {
                printf ( "SiftDataPtr NULL error in WK_CombEquivSeqArab.\n" );
                return ( -1 );
            }
/*
 * Assemble the weighted key strings for output to compkeys.txt.
 */
            assembleKeyExplicitWeights ( localbuf2, t2->level1, t2->level2,
                                         lowerTertiaryWeights[tokenType] );
/*
 * For these combining mark sequences, inherit the variable flag
 * from the setting for the class of the original character.
 * This prevents anomalies whereby applying diacritics to variable
 * symbols will make them sort (nonignorably) by the diacritic.
 *
 * Rawkey values are simply assigned explicitly here, for clarity in
 * expression.
 */
            rkptr->variable = t1->variable;
            rkptr->level1 = t2->level1;
            rkptr->level2 = t2->level2;
            rkptr->level3 = lowerTertiaryWeights[tokenType];
            rkptr->level4 = t1->uvalue;
            rkptr++;
            strcat ( outputbuf, localbuf2 );
        }
        break;
/*
 * To avoid mucking up the other processing, the l-dots get their own
 * processing branch here. Unless things radically change again,
 * the l-dots are treated as l's with a virtual secondary weight.
 * The way to make this weight is to assemble two keys. The first
 * is for l, and the second is for the virtual secondary weight U+F8F0.
 */
    case WK_CompEquivSeqLDot :
        {
        WALNUTPTR tmp;

            rkptr = rawkeys;
            assembleKey ( t1, localbuf2 );
            finalNumTokens = setRawKeys ( rkptr, t1 );
            rkptr++;
            strcat ( outputbuf, localbuf2 );
            tmp = getSiftDataPtr(0xF8F0);
            assembleKeyExplicitWeights ( localbuf2, tmp->level1, tmp->level2,
                                         tmp->level3 );
            finalNumTokens += setRawKeys ( rkptr, tmp );
            rkptr->level4 = t1->uvalue;
            strcat ( outputbuf, localbuf2 );
            t1->numChars = 2;
        }
        break;
    case WK_CompEquivSeq :
        tlen = 506; /* 512 - 6 */
        finalNumTokens = 0;
        limit = numCharTokens;
/*
 * i counts the number of unichars in the decomposition.
 *
 * This branch used to have a subloop in it which processed canonical
 * equivalent sequences into a single unit and then did composite
 * weighting on that sequence. To match the UCA spec, that approach
 * had to be dropped. Now every unit is separately weighted as
 * decomposed. Special handling has to be added, however, to identify
 * those canonically decomposed combining marks *inside* the compatibility
 * sequences, so that they don't inherit the compatibility tertiary weight
 * assigned to the rest of the sequences.
 */
        for ( i = 0, rkptr = rawkeys; i < limit; i++ )
        {
        WALNUTS t4;
        WALNUTPTR tmp;
        int tertwt;

/*
 * Copy the entire WALNUT record for the decomposition element into a local copy,
 * so we can modify it for this particular character, if needed.
 */
            tmp = getSiftDataPtr(decompstr[i]);
            if ( tmp == NULL )
            {
                printf ( "SiftDataPtr NULL error in WK_CompEquivSeq.\n" );
                return ( -1 );
            }
            t4 = *tmp;
            /*
             * For Implicit weights (getSiftDataPtr points to handata),
             * set the uvalue and symbolBase to the code point.
             */
            if ( t4.decompType == Implicit )
            {
                t4.uvalue = decompstr[i];
                t4.symbolBase = decompstr[i];
            }
            if ( debugLevel > 1 )
            {
                printf ( "COMPAT %s: %04X, numChars %d\n", t1->value, t4.level4, numCharTokens );
            }
/*
 * Reset t4.level4 to have the
 * value of the input character, rather than the decomposition.
 * This preserves information about the source of the compatibility
 * decomposition, and is most consistent with the treatment of single
 * compatibility characters.
 *
 * This fixes up the 4th level weight for items like the U+017F long s,
 * as well as for regular compatibility decompositions.
 */
            t4.level4 = t1->level4;


/*
 * Override the tertiary weight with the tokenType value.
 *
 * New 01-Aug-10: For compatibility sequences, use the tokenType
 * for the *first* element of the sequence, but use LAST_TERTIARY
 * for any subsequent elements. This eliminates an anomaly when
 * comparing alternate orders for compatibility sequences that
 * otherwise only would have identical sequences of tertiary weights.
 *
 * Assemble the weighted key strings for output to compkeys.txt.
 * If building unistbl.hc, also get the UNISORTKEY value and store
 * it in decompkey array, for printing out the expansions in
 * unistbl.hc below.
 *
 */
            if ( ( decompIsHinky ( t1 ) ) && 
                 ( t4.level1 == 0 ) && 
                 ( t4.level2 < getFirstVariantSecWeight ( ) ) )
/*
 * Special-case for combining marks that are involved in a canonical
 * decomposition, with a base form that in turn has a
 * compatibility decomposition. These will be
 * identified by having primary weight = 0 and a secondary weight
 * less than the variant marks (which are virtual, constructed weights).
 * For those, do not override the tertiary weight.
 *
 * The problem is addressed only at 6 characters: 1E9B, 03D3, 03D4, FB1F, FB3A,
 * and FB43. (Those recurse canonical->compatibility.) Those 6 are
 * preidentified by the hack routine, decompIsHinky(), so as to save
 * the complication of identifying and tracking the exact situation inside 
 * the recursion.
 *
 * 2005-Apr-13 4 more characters have to be added to decompIsHinky(),
 * 01E2, 01E3, 01FC, 01FD, because these have now been forced to
 * have <sort> decomps with a variant weight, but still have a canonical
 * decomposition involving an accent.
 * 
 */
            {
                assembleKey ( &t4, localbuf2 );
                tertwt = t4.level3;
            }
#ifdef NOTDEF
            else if ( ( i == limit - 1 ) && ( t4.level1 != 0 ) && 
                 ( ( tokenType == CompatToken ) || ( tokenType == VerticalToken ) ) && 
                 ( ( !unisift_IsAlphabetic ( t4.uvalue ) ) || ( IsRomanNumeralWithIs ( t1->uvalue ) ) ) )
            {
            /*
             * For the *last* CE processed, and *only* the last one,
             * override the tertiary weight, and set it to the
             * LAST_TERTIARY value. 
             * Don't override for CE's without primary weights.
             *
             * 2012-Aug-14 note:
             * Further constrain the override to only apply when
             * the tokenType is CompatToken. This picks up only the
             * <compat> and <vertical> decompositions for non-alphabetics, and leaves all
             * other casing distinctions and other tertiary distinctions
             * intact. (Only the odd "overlap" instances need this
             * override.) The IsRomanNumeralWithIs() hack is to let
             * through the compatibility decomps for "II", "III", "VIII", etc.,
             * which can also end up with overlaps.
             *
             * 2012-Nov-21 note:
             * Per UTC decision, just drop all usage of the LAST_TERTIARY value,
             * even for overlaps. Not actually useful.
             */
                assembleKeyWithOverride ( &t4, localbuf2, FillerToken );
                tertwt = LAST_TERTIARY;
            }
#endif
            else
            {
                assembleKeyWithOverride ( &t4, localbuf2, tokenType );
                tertwt = weightFromToken ( &t4, tokenType );
            }
            temp = setRawKeys ( rkptr, &t4 );
/*
 * When using assembleKeyWithOverride, the tertiary weight is
 * derived from the tokenType, rather than t4.level3, so when
 * constructing the rawkeys values to pass to the symbolic weighter,
 * also calculate the revised tertiary weight for that.
 */
            rkptr->level3 = tertwt;
            rkptr += temp;
            finalNumTokens += temp;
            slen = (int)strlen ( localbuf2 );
            tlen -= slen;
            if ( tlen <= 0 )
            {
                printf ( "Output 3 buffer overrun! %s i= %d\n", t1->value, i );
                return ( -1 );
            }
            strcat ( outputbuf, localbuf2 );
/*
 * Make sure that if this is the first pass through that t1 itself gets
 * updated with the new values from t4, so that a compatibility sequence that
 * ends up being treated like a canonical sequence with diacritic(s) also
 * posts up values at the array entry for that character itself. These have
 * to be correct, so that the loop below that prints out the symbolic
 * weights picks up the correct values.
 */
            if ( i == 0 )
            {
                t1->level2 = t4.level2;
                t1->level3 = t4.level3;
            }
        } /* for i */
/*
 * Now set t1->numChars based on the *final* number of tokens, to take
 * into account any shortenings due to combining character sequence
 * processing.
 */
        t1->numChars = (UChar8)finalNumTokens;
        break;
    } /* switch */


/*
 * PHASE 2
 *
 * Append a comment label following the assembled key,
 * followed by the character name.
 *
 * If dumping decomps, add a label there as well.
 */
    strcat ( outputbuf, " # " );  /* Add a comment separator */

/*
 * Add the character name. If doing a debug run, append a
 * diagnostic label which distinguishes the various WK types
 * which pass through the sifter in different ways.
 */
    strcpy ( localbuf2, t1->name );
    if ( debugLevel > 0 )
    {
        switch ( t1->whichKind )
        {
        case WK_CanonEquiv    :
                strcat ( localbuf2, "; QQC" );
                break;
        case WK_CanonEquivCombSeq :
                strcat ( localbuf2, "; QQCM" );
                break;
        case WK_CompEquivSeqArab :
                strcat ( localbuf2, "; QQKA" );
                break;
        case WK_CompEquiv     :
                strcat ( localbuf2, "; QQK" );
                break;
        case WK_CompEquivSeq  :
                if ( finalNumTokens > 1 )
                {
                    strcat ( localbuf2, "; QQKN" );
                }
                else
                {
                    strcat ( localbuf2, "; QQK" );
                }
                break;
        case WK_CompEquivSeqLDot :
                strcat ( localbuf2, "; QQKL" );
                break;
        case WK_CanonEquivSeq :
                strcat ( localbuf2, "; QQCN" );
                break;
        default               :
                if ( dumpDecomps )
                {
                    fputs ( "\tBADPARSE\n", fdo3 );
                }
                break;
        }  /* switch */
    }
    strcat ( localbuf2, "\n" );
/*
 * Update the global statistics on the WK types.
 */
    switch ( t1->whichKind )
    {
    case WK_CanonEquiv    :
            numCanonicals++;
            break;
    case WK_CanonEquivCombSeq :
            numCanonicalCombSequences++;
            break;
    case WK_CompEquivSeqArab :
            numCompatibleSequencesArab++;
            break;
    case WK_CompEquiv     :
            numCompatibles++;
            break;
    case WK_CompEquivSeq  :
            if ( finalNumTokens > 1 )
            {
                numCompatibleSequences++;
            }
            else
            {
                numCompatibles++;
            }
            break;
    case WK_CompEquivSeqLDot :
            numCompatibles++;
            break;
    case WK_CanonEquivSeq :
            numCanonicalSequences++;
            break;
    }  /* switch */

    strcat ( outputbuf, localbuf2 );

/*
 * PHASE 3
 *
 * For single key values, dump the assembled single keys out in fdo1.
 *
 * For multiple, composite key values, dump out the
 * composite keys in fdo2, based on the decompositions.
 *
 */
    if ( finalNumTokens > 1 )
    {
        fputs ( outputbuf, fdo2 );
    }
    else
    {
        fputs ( outputbuf, fdo1 );
    }
    

/*
 * PHASE 4
 *
 * Additional processing for ISO 14651.
 *
 * The sorting of the
 * tree for the entire collection of UCA keys, for output to allkeys.txt
 * and for the ctt14651.txt list of weight elements, is done by a separate
 * pass. See call to unisift_BuildSymWtTree() in the main() function.
 */

/*
 * If dumping symbolic file (ctt14651.txt), add appropriate
 * symbols and collation elements to index tree by calling assembleCompositeSym.
 */
    if ( dumpSymbols )
    {
    int rc;

        sprintf ( localbuf2, "<U%s>", t1->value );
        rc = assembleCompositeSym ( t1, localbuf2, rawkeys, finalNumTokens, outputbuf );
        if ( rc == -1 )
        {
            return ( -1 );
        }
    }

/*
 * File flushing after each operation will slow things down, but
 * will considerably increase the precision for narrowing down a
 * problem if a processing step causes the program to crash.
 */
    if ( debugLevel > 0 )
    {
    int rc;

        rc = fflush ( fdo2 );
        assert ( rc == 0 );
        rc = fflush ( fdo1 );
        assert ( rc == 0 );
    }

    return ( 0 );
}

/***************************************************************************/

/*
 * SECTION: Output processing for characters without decompositions.
 */

/*
 * processNonDecomps()
 *
 * This is a much simpler routine, which deals with the output processing
 * for characters without decompositions.
 */
static int processNonDecomps ( WALNUTPTR t1 )
{
int rc;
char outputbuf[256];
char localbuf2[128];
RAWKEY rawkeys[2];

    strcpy ( outputbuf, t1->value );
    if ( strlen ( t1->value ) == 4 )
    {
        strcat ( outputbuf, " " );  /* Pad to align. */
    }
    strcat ( outputbuf, " ; " ); /* Append a field separator. */

    assembleKey ( t1, localbuf2 );
    strcat ( outputbuf, localbuf2 );

    sprintf ( localbuf2, " # %s\n", t1->name );
    strcat ( outputbuf, localbuf2 );

    /* Now actually write the formatted string out to file. */
    fputs ( outputbuf, fdo1 );

    if ( dumpSymbols )
    {
    /*
     * For Implicit weighting, there are two collation elements
     * constructed. Take advantage of the rawkeys mechanism used
     * for processDecomps to hand off the correctly constructed
     * set of weights to assembleCompositeSym for insertion
     * into the tree. This prevents having to clone the complications
     * of composite 14651 symbol construction into assembleSym as
     * well.
     */
        sprintf ( localbuf2, "<U%s>", t1->value );
        if ( t1->decompType == Implicit )
        {
            setRawKeys ( rawkeys, t1 );
            rc = assembleCompositeSym ( t1, localbuf2, rawkeys, 2, outputbuf );
            if ( rc == -1 )
            {
                return ( -1 );
            }
        }
        else
        {
            assembleSym ( t1, localbuf2, outputbuf );
        }
    }

    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo1 );
        assert ( rc == 0 );
    }

    return ( 0 );
}

/***************************************************************************/

/*
 * SECTION: Output processing for contractions.
 */

/*
 * Local functions separated out to make the logic of
 * the output processing in
 * processDecomps() easier to follow and to modify.
 */

/*
 * dumpToContractionsFile()
 *
 * This formats an entry to dump out to fdo6, for ctrckeys.txt.
 * and returns the formatted string in UCAweights.
 */

static int dumpToContractionsFile ( WALNUTPTR t1, UInt32 *decomp, char* UCAweights )
{
int rc;
UInt32 *s;
WALNUTPTR tmp;
char localbuf[256];
char localbuf2[30];

    UCAweights[0] = '\0';
    for ( s = decomp; *s != UNINULL; s++ )
    {
        sprintf ( localbuf, "%04X ", *s );
        strcat ( UCAweights, localbuf );
    }
    
    strcat ( UCAweights, "; " ); /* Append a field separator. (no extra space) */

    assembleKey ( t1, localbuf );
    strcat ( UCAweights, localbuf );
/*
 * Special handling for l-dots, which get a weight constructed with
 * a secondary virtual weight, but which then need contraction
 * handling as well.
 */
    if ( ( t1->uvalue == 0x013F ) || ( t1->uvalue == 0x0140 ) )
    {
        tmp = getSiftDataPtr( 0xF8F0 ); /* virtual secondary weight */
        assembleKeyExplicitWeights ( localbuf2, tmp->level1, tmp->level2,
                                     tmp->level3 );
        strcat ( localbuf, localbuf2 );
        strcat ( UCAweights, localbuf2 );
    }
/*
 * Append character name as comment.
 */
    sprintf ( localbuf, " # %s\n", t1->name );
    strcat ( UCAweights, localbuf );

    /* Now actually write the formatted string out to file. */
    fputs ( UCAweights, fdo6 );

    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo6 );
        assert ( rc == 0 );
    }

    return ( 0 );

}

/*
 * dumpThaiToContractionsFile()
 *
 * This formats an entry to dump out to fdo6, for ctrckeys.txt.
 *
 * This is a special case for the Thai syllables, which takes a
 * consonant-vowel pair and creates the appropriate reordered
 * entry, outputs it to ctrckeys.txt and returns the formatted
 * string in UCAweights.
 */

static int dumpThaiToContractionsFile ( UInt32 consonant, UInt32 vowel, char* UCAweights )
{
int rc;
WALNUTPTR t1;
WALNUTPTR t2;
char localbuf[256];

    sprintf ( UCAweights, "%04X %04X ; ", vowel, consonant );
    
    t1 = getSiftDataPtr (consonant) ;
    assembleKey ( t1, localbuf );
    strcat ( UCAweights, localbuf );

    t2 = getSiftDataPtr (vowel) ;
    assembleKey ( t2, localbuf );
    strcat ( UCAweights, localbuf );

    sprintf ( localbuf, " # <%s, %s>\n", t2->name, t1->name );
    strcat ( UCAweights, localbuf );

    /* Now actually write the formatted string out to file. */
    fputs ( UCAweights, fdo6 );

    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo6 );
        assert ( rc == 0 );
    }

    return ( 0 );

}

/*
 * dumpLaoDigraphToContractionsFile()
 *
 * As for dumpThaiToContractionsFile, but deal with a sequence
 * of consonants, instead of a single consonant.
 */

static int dumpLaoDigraphToContractionsFile ( UInt32 consonant, 
                                              UInt32 vowel, char* UCAweights )
{
int rc;
UInt32 c2;
WALNUTPTR t1;
WALNUTPTR t2;
WALNUTPTR t3;
WALNUTPTR t4;
char localbuf[256];

    sprintf ( UCAweights, "%04X %04X ; ", vowel, consonant );
    
    t4 = getSiftDataPtr ( consonant );
    if ( consonant == 0x0EDC )
    {
        c2 = 0x0E99;
    }
    else
    {
        c2 = 0x0EA1;
    }
/*
 * Force the 3rd and 4th levels to what they should be.
 */
    t1 = getSiftDataPtr (0x0EAB) ;
    sprintf ( localbuf, keyTemplate1, t1->level1, t1->level2,
              SKEY_TWT_COMPAT/*, t4->level4 */ );
    strcat ( UCAweights, localbuf );

    
    t2 = getSiftDataPtr (c2) ;
    sprintf ( localbuf, keyTemplate1, t2->level1, t2->level2,
              SKEY_TWT_COMPAT/*, t4->level4 */ );
    strcat ( UCAweights, localbuf );

    t3 = getSiftDataPtr (vowel) ;
    assembleKey ( t3, localbuf );
    strcat ( UCAweights, localbuf );

    sprintf ( localbuf, " # <%s, %s>\n", t3->name, t4->name );
    strcat ( UCAweights, localbuf );

    /* Now actually write the formatted string out to file. */
    fputs ( UCAweights, fdo6 );

    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo6 );
        assert ( rc == 0 );
    }

    return ( 0 );

}

/*
 * dumpToSymbolFile()
 *
 * This formats an entry for dumping to the collation symbol
 * section of ctt14651.txt.
 *
 * It also constructs and inserts an entry into the symbol btree,
 * for later sorting and dumping into allkeys.txt and the
 * the sorted portion of ctt14651.txt. 
 */

static int dumpToSymbolFile ( WALNUTPTR t1, UInt32 *decomp, char *UCAweights )
{
UInt32 *s;
int i;
int rc;
char localbuf[256];
char localbuf2[64];
char savesymbol[64];

/*
 * First drop a collating-element definition in the output file.
 */
    fputs ( "collating-element ", fdo5 );
    strcpy ( localbuf, "<U" );
    for ( i = 0, s = decomp; *s != UNINULL; i++, s++ )
    {
        if ( i == 0 )
        {
            sprintf ( localbuf2, "%04X", *s );
        }
        else
        {
            sprintf ( localbuf2, "_%04X", *s );
        }
        strcat ( localbuf, localbuf2 );
    }
    strcat ( localbuf, ">" );
    strcpy ( savesymbol, localbuf );
    strcat ( localbuf, " from \"" );
    for ( s = decomp; *s != UNINULL; s++ )
    {
        sprintf ( localbuf2, "<U%04X>", *s );
        strcat ( localbuf, localbuf2 );
    }
    strcat ( localbuf, "\" % decomposition of " );
    strcat ( localbuf, t1->name );
    strcat ( localbuf, "\n" );
    fputs ( localbuf, fdo5 );
/*
 * Then insert an entry for the collating-element symbol in the btree.
 *
 * This stores both the 14651 symbol and the UCA weight entry,
 * which was passed in the UCAweights parameter.
 *
 * Special case Catalan l-dots, which require construction of a
 * composite weight symbol, instead of a unitary symbol.
 */
    if ( ( t1->uvalue == 0x013F ) || ( t1->uvalue == 0x0140 ) )
    {
    RAWKEY rawkeys[2];
    RAWKEY* rkptr;
    WALNUTPTR tmp;

    
        rkptr = rawkeys;
        rc = setRawKeys ( rkptr, t1 );
        rkptr++;
        tmp = getSiftDataPtr(0xF8F0);
        rc = setRawKeys ( rkptr, tmp );
        rkptr->level4 = t1->uvalue;
        rc = assembleCompositeSym ( t1, savesymbol, rawkeys, 2, UCAweights );
    }
    else
    {
        assembleSym ( t1, savesymbol, UCAweights );
    }

    if ( debugLevel > 0 )
    {
        rc = fflush ( fdo5 );
        assert ( rc == 0 );
    }

    return ( 0 );
}

/*
 * processSingleThaiElement()
 *
 * Construct the actual entries for the consonant/vowel
 * combination passed in.
 */

static int processSingleThaiElement ( UInt32 consonant, UInt32 vowel, const char* script )
{
int rc;
char localbuf[256];
char localbuf2[64];
char savesymbol[64];
char weightstr[128];

/*
 * Format and dump the UCA weights in ctrckeys.txt.
 *
 * Temporarily store the formatted UCA weights in weightstr.
 */
    rc = dumpThaiToContractionsFile ( consonant, vowel, weightstr );

    if ( dumpSymbols )
    {
/*
 * First drop a collating-element definition in the output file.
 */
        fputs ( "collating-element ", fdo5 );
        sprintf ( localbuf, "<U%04X_%04X>", vowel, consonant );
        strcpy ( savesymbol, localbuf );
        sprintf ( localbuf2, " from \"<U%04X><U%04X>", vowel, consonant );
        strcat ( localbuf, localbuf2 );
        sprintf ( localbuf2, "\" %% collation-element for reordered %s syllable\n", script );
        strcat ( localbuf, localbuf2 );
        fputs ( localbuf, fdo5 );
/*
 * Then insert an entry for the collating-element symbol in the btree.
 *
 * This stores both the 14651 symbol and the UCA weight entry.
 */

/*
 * TBD: assembleCompositeSym assumes the symbol can be derived
 * from t1, but does the correct work in extracting and weighting
 * a sequence from the rkeys array. assembleContractionSym
 * uses savesymbol to get the correct symbol, but assumes
 * that the (single) weight can be derived from t1. This
 * probably needs a 3rd hack based on both to get the right
 * combination for the Thai "contractions".
 */
        rc = assembleThaiContractionSym ( consonant, vowel, savesymbol, weightstr );

        if ( debugLevel > 0 )
        {
            rc = fflush ( fdo5 );
            assert ( rc == 0 );
        }
    }

    return ( 0 );
}

/*
 * processLaoDigraphElement()
 *
 * As for processSingleThaiElement(), but with special handling for
 * the Lao digraphs.
 */

static int processLaoDigraphElement ( UInt32 consonant, UInt32 vowel )
{
int rc;
char localbuf[256];
char localbuf2[64];
char savesymbol[64];
char weightstr[128];

/*
 * Format and dump the UCA weights in ctrckeys.txt.
 *
 * Temporarily store the formatted UCA weights in weightstr.
 */
    rc = dumpLaoDigraphToContractionsFile ( consonant, vowel, weightstr );

    if ( dumpSymbols )
    {
/*
 * First drop a collating-element definition in the output file.
 */
        fputs ( "collating-element ", fdo5 );
        sprintf ( localbuf, "<U%04X_%04X>", vowel, consonant );
        strcpy ( savesymbol, localbuf );
        sprintf ( localbuf2, " from \"<U%04X><U%04X>", vowel, consonant );
        strcat ( localbuf, localbuf2 );
        strcat ( localbuf, "\" % collation-element for reordered Lao syllable\n" );
        fputs ( localbuf, fdo5 );
/*
 * Then insert an entry for the collating-element symbol in the btree.
 *
 * This stores both the 14651 symbol and the UCA weight entry.
 */

/*
 * TBD: assembleCompositeSym assumes the symbol can be derived
 * from t1, but does the correct work in extracting and weighting
 * a sequence from the rkeys array. assembleContractionSym
 * uses savesymbol to get the correct symbol, but assumes
 * that the (single) weight can be derived from t1. This
 * probably needs a 3rd hack based on both to get the right
 * combination for the Thai "contractions".
 */
        rc = assembleLaoDigraphContractionSym ( consonant, vowel, 
                                                savesymbol, weightstr );

        if ( debugLevel > 0 )
        {
            rc = fflush ( fdo5 );
            assert ( rc == 0 );
        }
    }

    return ( 0 );
}

/*
 * processThaiSyllables()
 *
 * This is a hack to force the dumping of
 * 375 "contractions" that are actually order reversals for
 * each Thai and Lao consonant and the relevant 5 Thai
 * or Lao left-side vowels which require order reversal to
 * be properly weighted in logical order.
 *
 * 2009-Sep-01 Added in processing for Tai Viet, which is
 * structured the same way. This adds another 48 x 5 = 240
 * "contractions" to the output file. Fortunately, Tai Viet
 * does not have the additional complexity of the Lao digraphs.
 *
 * 2012-Aug-09 Added in Khmu U+0EDE, U+0EDF for Lao.
 *
 * 2014-Nov-17 Added in processing for New Tai Lue.
 * This adds another 44 x 4 = 176 "contractions" to the output file.
 */
static int processThaiSyllables ( void )
{
UInt32 i;
UInt32 j;
int rc;

/*
 * First dump all the Thai combinations.
 */
    for ( i = 0x0E01; i <= 0x0E2E; i++ )
    {
        for ( j = 0x0E40; j <= 0x0E44; j++ )
        {
            rc = processSingleThaiElement ( i, j, "Thai" );
            if ( rc != 0 )
            {
                return ( rc );
            }
        }
    }
/*
 * Next dump all the basic Lao combinations, omitting
 * any unassigned code points in the consonant range.
 */
    for ( i = 0x0E81; i <= 0x0EAE; i++ )
    {
        if ( unisift_IsAlphabetic ( i ) )
        {
            for ( j = 0x0EC0; j <= 0x0EC4; j++ )
            {
                rc = processSingleThaiElement ( i, j, "Lao" );
                if ( rc != 0 )
                {
                    return ( rc );
                }
            }
        }
    }
/*
 * Two Khmu consonants added to the Lao script
 */
    for ( i = 0x0EDE; i <= 0x0EDF; i++ )
    {
        if ( unisift_IsAlphabetic ( i ) )
        {
            for ( j = 0x0EC0; j <= 0x0EC4; j++ )
            {
                rc = processSingleThaiElement ( i, j, "Lao" );
                if ( rc != 0 )
                {
                    return ( rc );
                }
            }
        }
    }
/*
 * Pick up the combinations for Lao diagraph consonants.
 *
 * These are U+0EDC LAO HO NO and U+0EDD LAO HO MO. Because
 * each of these decomposes for the regular weighting, these
 * reordered elements are just hacked in with the hard-coded
 * decompositions.
 */
    for ( j = 0x0EC0; j <= 0x0EC4; j++ )
    {
        rc = processLaoDigraphElement ( 0x0EDC, j );
        if ( rc != 0 )
        {
            return ( rc );
        }
        rc = processLaoDigraphElement ( 0x0EDD, j );
        if ( rc != 0 )
        {
            return ( rc );
        }
    }
/*
 * Next dump all the New Tai Lue combinations.
 *
 * These 4 vowels are in discontiguous ranges. Loop once and
 * then do the other to pick up both ranges.
 */
    for ( i = 0x1980; i <= 0x19AB; i++ )
    {
        for ( j = 0x19B5; j <= 0x19B7; j++ )
        {
            rc = processSingleThaiElement ( i, j, "New Tai Lue" );
            if ( rc != 0 )
            {
                return ( rc );
            }
        }
        rc = processSingleThaiElement ( i, 0x19BA, "New Tai Lue" );
        if ( rc != 0 )
        {
            return ( rc );
        }
    }
/*
 * Next dump all the Tai Viet combinations.
 *
 * These 5 vowels are in discontiguous ranges. Just loop 3 times
 * to pick up all three ranges.
 */
    for ( i = 0xAA80; i <= 0xAAAF; i++ )
    {
        for ( j = 0xAAB5; j <= 0xAAB6; j++ )
        {
            rc = processSingleThaiElement ( i, j, "Tai Viet" );
            if ( rc != 0 )
            {
                return ( rc );
            }
        }
        for ( j = 0xAAB9; j <= 0xAAB9; j++ )
        {
            rc = processSingleThaiElement ( i, j, "Tai Viet" );
            if ( rc != 0 )
            {
                return ( rc );
            }
        }
        for ( j = 0xAABB; j <= 0xAABC; j++ )
        {
            rc = processSingleThaiElement ( i, j, "Tai Viet" );
            if ( rc != 0 )
            {
                return ( rc );
            }
        }
    }

    return ( 0 );
}

/*
 * preprocessDecomp()
 *
 * Check if any secondary decomposition has been stored
 * in t1->decomp. If so, extract it and output in decomp2.
 * Fix up t1->decomp
 * to remove the comma delimited secondary decomposition.
 *
 * Returns the number of charTokens found in the secondary
 * decomposition.
 */

static int preprocessDecomp ( WALNUTPTR t1, UInt32 *d, int dlen )
{
char *s;
char *newsp;
UInt32 cc;
int tokenType;
int numCharTokens;
UInt32 *sd;
UInt32 *si;
UInt32 buf[30];  /* decomp string stored as UTF-32 */

    s = strchr ( t1->decomp, ',' );
    if ( s == NULL )
    {
/*
 * No secondary, comma-delimited decomposition was given.
 * Just return and continue processing.
 */
        return ( 0 );
    }
/*
 * Found a comma. Poke a null to terminate t1->decomp at
 * that position. Advance s past the comma and space. It now points
 * to the remainder of the string -- the secondary decomposition.
 */
    *s++ = '\0';
    s++;
/*
 * Now convert the decomp string into a UInt32 array in buf.
 */
    numCharTokens = 0;
    while ( 1 )
    {
        newsp = parseToken ( s, &tokenType, &cc );
        if ( tokenType == NullToken )
        {
            break;
        }
        else
        {
            if ( tokenType == CharToken )
            {
                if ( numCharTokens >= dlen )
                {
                    badValues++;
                    printf ( "Bad Value in preprocessDecomp: %s\n", s );
                    break;
                }
                buf[numCharTokens] = cc;
                numCharTokens++;
            }
            else
            {
                badValues++;
                printf ( "Bad Value in preprocessDecomp: %s\n", s );
                return ( 0 );
            }
            s = newsp;
        }
    }
/*
 * Now that we are out of the for loop, terminate the temporary buffer
 * (buf) with a 32-bit NULL and then copy it
 * into the caller's buffer. Return the total length.
 */
    buf[numCharTokens] = 0;
    for ( si = buf, sd = d; *si != 0; )
    {
        *sd++ = *si++;
    }
    *sd = 0;
    return ( numCharTokens );
}

/*
 * processContractions()
 *
 * This is the main function for handling characters that have been
 * equated to a contraction. Their main entry has already been dumped
 * by a call to processNonDecomp(). This function does the follow-up
 * work which processes the contraction equivalents to that main entry
 * weight.
 *
 * U+0140 & U+013F (the Catalan l-dot characters) are exceptions.
 * They get a call *first* to processDecomp() to set the main entry,
 * and then get a call to processContractions().
 */

static int processContractions ( WALNUTPTR t1 )
{
int tokenType;
int rc;
UInt32 decompstr[20];
UInt32 decompstr2[8];
int numCharTokens;
int numCharTokens2;
char weightstr[512];
char weightstr2[512];

    if ( t1->decomp == NULL )
    {
        printf ( "Bad call to processContractions with null decomp: %04X\n", t1->uvalue);
        return ( 0 );
    }
/*
 * First check if any secondary decomposition has been stored
 * in the decomp field. If so, extract it and store in decompstr2
 * for temporary processing here, before attempting recursive
 * decomposition on the primary decomposition. Fix up t1->decomp
 * to remove the comma delimited secondary decomposition.
 */
    numCharTokens2 = preprocessDecomp ( t1, decompstr2, 8 );

    if ( ( debugLevel > 1 ) && ( numCharTokens2 > 0 ) )
    {
        printf ( "Secondary decomp: char=%04X, num=%d\n", t1->uvalue, numCharTokens2 );
    }

    numCharTokens = doRecursiveDecomp ( decompstr, 20, t1->decomp, &tokenType,
                                        t1->uvalue );
    if ( numCharTokens == 0 )
    {
        return ( 0 );
    }

    if ( dumpDecomps && !( ( t1->uvalue == 0x013F ) || ( t1->uvalue == 0x0140 ) ) )
    {
/*
 * For characters with decompositions, even single values,
 * print out the entire decomposition in readable form in decomps.txt.
 *
 * Exclude the l-dots, which already got dumped in their decomposition
 * processing pass.
 */
        rc = dumpToDecompsFile ( t1, decompstr, tokenType );
        if ( numCharTokens2 > 0 )
        {
            rc = dumpToDecompsFile ( t1, decompstr2, tokenType );
        }
    }

/*
 * Format and dump the UCA weights in ctrckeys.txt.
 *
 * Temporarily store the formatted UCA weights in weightstr.
 */
    rc = dumpToContractionsFile ( t1, decompstr, weightstr );
    if ( numCharTokens2 > 0 )
    {
        rc = dumpToContractionsFile ( t1, decompstr2, weightstr2 );
    }


    if ( dumpSymbols )
    {
/*
 * Add the contraction to the btree for the 14651 table.
 *
 * Pass the formatted UCA entry in weightstr, for storage in
 * the btree as well.
 */
        rc = dumpToSymbolFile ( t1, decompstr, weightstr );
        if ( numCharTokens2 > 0 )
        {
            rc = dumpToSymbolFile ( t1, decompstr2, weightstr2 );
        }
    }

    return ( 0 );
}

/***************************************************************************/

/*
 * SECTION: Abstract the weighting of alphabetics, taking into account
 * special processing for kana and for combining marks.
 */

/*
 * kanaMap
 *
 * This table maps Hiragana to Katakana values--analogous to a
 * folding operation to lowercase Latin letters. Katakana is taken
 * as the base, since it has a few more characters defined (for va, vi, ve,
 * vo).
 *
 * The small variants are also mapped to the corresponding Katakana base.
 * Hiragana with ten (voiced and semi-voiced) are simply mapped to
 * the corresponding Katakana with ten. The decompositions will account
 * for the appropriate weightings for the baseforms + ten.
 *
 * This map applies to the range U+3041 .. U+30FA. The ten and iteration
 * characters, etc. at U+3099 .. U+30A0 map to themselves for simplicity.
 */

static unichar kanaMap[] =
    {
              0x30A2, 0x30A2, 0x30A4, 0x30A4, 0x30A6, 0x30A6, 0x30A8, /* 4 */
      0x30A8, 0x30AA, 0x30AA, 0x30AB, 0x30AC, 0x30AD, 0x30AE, 0x30AF,
      0x30B0, 0x30B1, 0x30B2, 0x30B3, 0x30B4, 0x30B5, 0x30B6, 0x30B7, /* 5 */
      0x30B8, 0x30B9, 0x30BA, 0x30BB, 0x30BC, 0x30BD, 0x30BE, 0x30BF,
      0x30C0, 0x30C1, 0x30C2, 0x30C4, 0x30C4, 0x30C5, 0x30C6, 0x30C7, /* 6 */
      0x30C8, 0x30C9, 0x30CA, 0x30CB, 0x30CC, 0x30CD, 0x30CE, 0x30CF,
      0x30D0, 0x30D1, 0x30D2, 0x30D3, 0x30D4, 0x30D5, 0x30D6, 0x30D7, /* 7 */
      0x30D8, 0x30D9, 0x30DA, 0x30DB, 0x30DC, 0x30DD, 0x30DE, 0x30DF,
      0x30E0, 0x30E1, 0x30E2, 0x30E4, 0x30E4, 0x30E6, 0x30E6, 0x30E8, /* 8 */
      0x30E8, 0x30E9, 0x30EA, 0x30EB, 0x30EC, 0x30ED, 0x30EF, 0x30EF,
      0x30F0, 0x30F1, 0x30F2, 0x30F3, 0x30F4, 0x30AB, 0x30B1, 0x0000, /* 9 */
      0x0000, 0x3099, 0x309A, 0x309B, 0x309C, 0x309D, 0x309E, 0x309F,
      0x30A0, 0x30A2, 0x30A2, 0x30A4, 0x30A4, 0x30A6, 0x30A6, 0x30A8, /* A */
      0x30A8, 0x30AA, 0x30AA, 0x30AB, 0x30AC, 0x30AD, 0x30AE, 0x30AF,
      0x30B0, 0x30B1, 0x30B2, 0x30B3, 0x30B4, 0x30B5, 0x30B6, 0x30B7, /* B */
      0x30B8, 0x30B9, 0x30BA, 0x30BB, 0x30BC, 0x30BD, 0x30BE, 0x30BF,
      0x30C0, 0x30C1, 0x30C2, 0x30C4, 0x30C4, 0x30C5, 0x30C6, 0x30C7, /* C */
      0x30C8, 0x30C9, 0x30CA, 0x30CB, 0x30CC, 0x30CD, 0x30CE, 0x30CF,
      0x30D0, 0x30D1, 0x30D2, 0x30D3, 0x30D4, 0x30D5, 0x30D6, 0x30D7, /* D */
      0x30D8, 0x30D9, 0x30DA, 0x30DB, 0x30DC, 0x30DD, 0x30DE, 0x30DF,
      0x30E0, 0x30E1, 0x30E2, 0x30E4, 0x30E4, 0x30E6, 0x30E6, 0x30E8, /* E */
      0x30E8, 0x30E9, 0x30EA, 0x30EB, 0x30EC, 0x30ED, 0x30EF, 0x30EF,
      0x30F0, 0x30F1, 0x30F2, 0x30F3, 0x30F4, 0x30AB, 0x30B1, 0x30F7, /* F */
      0x30F8, 0x30F9, 0x30FA
    };
/*
 * A similar kana map to deal with the Katakana phonetic extensions
 * at U+31F0..U+31FF.
 */
static unichar kanaMap2[] =
    {
      0x30AF, 0x30B7, 0x30B9, 0x30C8, 0x30CC, 0x30CF, 0x30D2, 0x30D5,
      0x30D8, 0x30DB, 0x30E0, 0x30E9, 0x30EA, 0x30EB, 0x30EC, 0x30ED
    };

/*
 * Another kana map to deal with the Small Kana Extension block
 * at U+1B130..U+1B16F. The full range is defined here, even though
 * most of the characters are still unassigned as of Unicode 15.0,
 * to simplify the task of filling in the rest once they are added.
 */
static unichar kanaMap3[] =
    {
      0x0000, 0x0000, 0x30B3, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, /* 3 */
      0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
      0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, /* 4 */
      0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
      0x30F0, 0x30F1, 0x30F2, 0x0000, 0x0000, 0x30B3, 0x0000, 0x0000, /* 5 */
      0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
      0x0000, 0x0000, 0x0000, 0x0000, 0x30F0, 0x30F1, 0x30F2, 0x30F3, /* 6 */
      0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
    };

/*
 * tertwtMap
 *
 * This table precalculates the tertiary weight for the Kana value,
 * from one of four tertiary weights: Normal Hiragana, Small Hiragana,
 * Normal Katakana, Small Katakana.
 */

#define HIRA_N (SKEY_TWT_NORM_HIRA)
#define HIRA_S (SKEY_TWT_SMALL_HIRA)
#define KATA_N (SKEY_TWT_NORM_KATA)
#define KATA_S (SKEY_TWT_SMALLCJK)
#define OTHERS (FIRST_TERTIARY)

static UChar8 tertwtMap[] =
    {
              HIRA_S, HIRA_N, HIRA_S, HIRA_N, HIRA_S, HIRA_N, HIRA_S,
      HIRA_N, HIRA_S, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_S, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_S, HIRA_N, HIRA_S, HIRA_N, HIRA_S,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_S, HIRA_N,
      HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_N, HIRA_S, HIRA_S,      0,
           0, OTHERS, OTHERS, OTHERS, OTHERS, OTHERS, OTHERS, OTHERS,
      OTHERS, KATA_S, KATA_N, KATA_S, KATA_N, KATA_S, KATA_N, KATA_S,
      KATA_N, KATA_S, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_S, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_S, KATA_N, KATA_S, KATA_N, KATA_S,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_S, KATA_N,
      KATA_N, KATA_N, KATA_N, KATA_N, KATA_N, KATA_S, KATA_S, KATA_N,
      KATA_N, KATA_N, KATA_N
    };

static UChar8 tertwtMap3[] =
    {
           0,      0,      HIRA_S, 0,      0,      0,      0,      0,
           0,      0,      0,      0,      0,      0,      0,      0,
           0,      0,      0,      0,      0,      0,      0,      0,
           0,      0,      0,      0,      0,      0,      0,      0,
           HIRA_S, HIRA_S, HIRA_S, 0,      0,      KATA_S, 0,      0,
           0,      0,      0,      0,      0,      0,      0,      0,
           0,      0,      0,      0,      KATA_S, KATA_S, KATA_S, KATA_S,
           0,      0,      0,      0,      0,      0,      0,      0
    };


/*
 * unisift_GetKatakanaBase()
 *
 * Calculate the Katakana base and the tertiary weight for any
 * Hiragana or Katakana character passed in.
 *
 * Only atomic kana should get processed through this routine,
 * since the sifter will bleed off the decomposed characters--those
 * are processed in the second pass, after the weights have been
 * assigned to all the atomic characters. But the tables are
 * built for the complete range of Kana, simply because it is
 * easier that way.
 *
 * Special case the archaic kana from the 1B000 and 1B100 blocks. If a
 * large number of them are added eventually, then a table
 * for them can be constructed. As it stands, the hentaigana are not
 * mapped to existing katakana characters, but get their
 * own independent primary weights. They do, however, get
 * initialized with hiragana or katakana tertiary weights.
 */
static UInt32 unisift_GetKatakanaBase ( UInt32 c, int* tertwt )
{
int lsb;

    if ( ( c >= 0x3041 ) && ( c <= 0x30FA ) )
    {
        lsb = (int)c & 0x00FF;
        *tertwt = tertwtMap [ lsb - 0x41 ] ;
        return ( (UInt32)kanaMap [ lsb - 0x41 ] );
    }
    else if ( ( c >= 0x31F0 ) && ( c <= 0x31FF ) )
    {
        lsb = (int)c & 0x00FF;
        *tertwt = KATA_S ;
        return ( (UInt32)kanaMap2 [ lsb - 0xF0 ] );
    }
    else if ( c == 0x1B000 ) /* katakana archaic e */
    {
        *tertwt = KATA_N;
        return ( c );
    }
    else if ( c == 0x1B11F ) /* hiragana archaic wu */
    {
        *tertwt = HIRA_N;
        return ( 0x1B122 ); /* maps to katakana archaic wu */
    }
    else if ( ( c >= 0x1B120 ) && ( c <= 0x1B122 ) ) /* katakana archaic yi, ye, wu */
    {
        *tertwt = KATA_N;
        return ( c );
    }
    else if ( ( c >= 0x1B130 ) && ( c <= 0x1B16F ) )
    {
        lsb = (int)c & 0x00FF;
        *tertwt = tertwtMap3 [ lsb - 0x30 ] ;
        return ( (UInt32)kanaMap3 [ lsb - 0x30 ] );
    }
    else
    {
        *tertwt = OTHERS;
        return ( c );
    }
}

/*
 * unisift_WeightAlphas()
 *
 * For base letters, first try to assign a case value for the
 * tertiary field. Combining alpha forms will fall through that test
 * and get the default value.
 *
 * This branch also picks up Indic vowel matras, which, though
 * combining, sort correctly if given primary weighting in the
 * ISCII order.
 *
 * Then check the primary value of the case pair, if any. If it
 * is already set, use that value, otherwise assign a new
 * primary weight and increment currentPrimary for the next base
 * letter.
 */
static void unisift_WeightAlphas ( WALNUTPTR p, UInt32 cc )
{
UInt32 tc;
int tertwt;

/*
 * Test for Semitic vowel points, which are treated like NSM's, given
 * secondary weightings and lowest tertiary values.
 *
 * This forced secondary weighting also applies to a number of Indic
 * combining marks (bindi, anusvara, visarga, etc.).
 *
 * It does *not* apply to the Indic combining vowels (matras), which
 * get primary weights of their own.
 */
    if ( unisift_IsCombining ( cc ) && unisift_ForceToSecondary ( cc ) )
    {
        p->level2 = getNextSecondary ( cc );
        p->level3 = FIRST_TERTIARY;
    }
/*
 * Next, special-case for Hiragana and Katakana, which get special
 * tertiary weights and whose primary weights are mapped together.
 * This special case does not extend to Hentaigana, which simply
 * get default tertiary weights.
 */
    else if ( ( ( cc >= 0x3041 ) && ( cc <= 0x30FA ) ) ||
              ( ( cc >= 0x31F0 ) && ( cc <= 0x31FF ) ) ||
              ( cc == 0x1B000 ) ||
              ( ( cc >= 0x1B11F ) && ( cc <= 0x1B122 ) ) ||
              ( ( cc >= 0x1B130 ) && ( cc <= 0x1B16F ) ) )
    {
    WALNUTPTR tt1;

        tc = unisift_GetKatakanaBase ( cc, &tertwt );
        tt1 = getSiftDataPtr(tc);
        if ( tt1->level1 != 0 )
        {
            p->level1 = tt1->level1;
        }
        else
        {
            p->level1 = getNextPrimary ( cc );
/*
 * Save this value back into the big array, regardless, so the
 * sifter weighting won't be sensitive to which of the four possible
 * characters (Small Hiragana, Hiragana, Small Katakana, Katakana) are
 * encountered first in unidata.txt. Otherwise, since everything is
 * normalized to Katakana, if Hiragana is encountered first, new
 * weights keep being assigned until the Katakana character itself
 * is found.
 */
            tt1->level1 = p->level1;
        }
        p->symbolBase = tc;
        p->level2 = FIRST_SECONDARY;
        p->level3 = tertwt;
    }
    else
    {
/*
 * Set level 3 based on the case of the character.
 *
 * Then set tc to the case pair of the character. That will be checked
 * to see if the primary weight has already been set for the case pair,
 * regardless of which order the two characters are encountered in
 * the input file.
 */
    WALNUTPTR tt1;

        if ( unisift_IsUpper ( cc ) )
        {
            p->level3 = upperTertiaryWeights[CharToken];
            tc = unisift_ToLower ( cc );
        }
        else
        {
            p->level3 = lowerTertiaryWeights[CharToken];
            tc = unisift_ToUpper ( cc );
        }
        tt1 = getSiftDataPtr ( tc );
        if ( cc == 0x0131 )
/*
 * Special case U+0131 dotless-i, to force it to weight separately
 * from regular i, despite the default case mappings.
 */
        {
            p->level1 = getNextPrimary ( cc );
            p->level2 = FIRST_SECONDARY;
        }
        else if ( tt1->level1 == 0 )
/*
 * level1 hasn't been set yet for this case pair, so force
 * assignment of the next primary weight for it.
 */
        {
            p->level1 = getNextPrimary ( cc );
            p->level2 = FIRST_SECONDARY;
        }
        else
/*
 * level1 has already been set for this case pair, so assign
 * the same value.
 */
        {
            p->level1 = tt1->level1;
            p->level2 = FIRST_SECONDARY;
        }
/*
 * Always set the symbolBase value from the lowercase, so that the
 * symbols for 14651 are generated consistently -- and not as a mix
 * of uppercase and lowercase values.
 */
        p->symbolBase = unisift_ToLower ( cc );

    }
}

/***************************************************************************/

/*
 * SECTION: Take a valid line from the input file, tokenize it, and
 *          pass the results through a sift algorithm to assign weights.
 */

/*
 * isAlphabeticException()
 *
 * Small routine to screen out a few alphabetic characters which
 * are exceptional and should not get primary weights.
 *
 * U+0345 iota subscript
 * U+0E4D, U+0ECD Thai/Lao nasalization dots
 * U+2180..U+2182 some Roman numerals, gc=Nl, not compat equivalents
 * U+2185..U+2188 some Roman numerals, gc=Nl, not compat equivalents
 * U+10140..U+10174 Greek acrophonics, gc=Nl
 * U+103D1..U+103D5 Old Persian numbers, gc=Nl
 * U+12400..U+1246E Cuneiform numbers, gc=Nl
 *
 * 2018-October-05, 2018-October-08: 
 * Added another batch of exceptions for Diacritic
 *   modifier letters (gc=Lm) which should also stay as variables.
 * U+02B9..U+02BA single and double prime
 * U+02C6..U+02CF various non-letterform modifiers
 * U+02EC IPA voicing mark
 * U+A717..U+A71F Chinantec and Africanist tone marks
 * U+A788 low circumflex accent
 * Added another batch of exceptions for ideographic numbers
 *   from the 3000 block (gc=Nl). These need to fall through
 *   to get tested for numeric values.
 * U+3007 ideographic zero
 * U+3021..U+3029, U+3038..U+303A Hangzhou numerals
 *
 * 2020-January 28:
 * Added exceptions for Vietnamese reading marks, which need
 * to fall through to get secondary weights.
 *
 * 2021-June-07:
 * Added exceptions for Katakana Minnan tone marks (gc=Lm),
 *   in the block 1AFF0..1AFFF.
 *
 */
static int isAlphabeticException ( UInt32 c )
{
    if ( ( ( c >= 0x02B9 ) && ( c <= 0x02BA ) ) ||
         ( ( c >= 0x02C6 ) && ( c <= 0x02CF ) ) ||
         ( c == 0x02EC ) ||
         ( c == 0x0345 ) ||
         ( c == 0x0E4D ) ||
         ( c == 0x0ECD ) ||
         ( ( c >= 0x2180 ) && ( c <= 0x2182 ) ) ||
         ( ( c >= 0x2185 ) && ( c <= 0x2188 ) ) ||
         ( c == 0x3007 ) ||
         ( ( c >= 0x3021 ) && ( c <= 0x3029 ) ) ||
         ( ( c >= 0x3038 ) && ( c <= 0x303A ) ) ||
         ( ( c >= 0xA717 ) && ( c <= 0xA71F ) ) ||
         ( c == 0xA788 ) ||
         ( ( c >= 0x10140 ) && ( c <= 0x10174 ) ) ||
         ( ( c >= 0x103D1 ) && ( c <= 0x103D5 ) ) ||
         ( ( c >= 0x12400 ) && ( c <= 0x1246E ) ) ||
         ( ( c >= 0x16FF0 ) && ( c <= 0x16FF1 ) ) ||
         ( ( c >= 0x1AFF0 ) && ( c <= 0x1AFFF ) ) )
    {
        return ( 1 );
    }
    return ( 0 ) ;
}

/*
 * isDiacriticException()
 *
 * Small routine to identify a few diacritics which
 * are exceptional. These are gc=Mc and would normally
 * end up handled as spacing diacritics, but are treated
 * as "honorary" non-spacing marks and get secondary
 * weights, instead.
 *
 * U+302E..U+302F   Hangul tone marks
 * U+ABEC           Meetei Mayek
 * U+16FF0..U+16FF1 Vietnamese reading marks
 */
static int isDiacriticException ( UInt32 c )
{
    if ( ( c == 0xABEC ) ||
         ( ( c >= 0x302E ) && ( c <= 0x302F ) ) ||
         ( ( c >= 0x16FF0 ) && ( c <= 0x16FF1 ) ) )
    {
        return ( 1 );
    }
    return ( 0 ) ;
}

/*
 * isIgnorableException()
 *
 * Small routine to screen out a few format controls which
 * are exceptional and which should not get zeroes
 * across the board, although tagged as ignorables in
 * the weighting.
 *
 * U+0600..U+0605, U+06DD, U+0890..U+0891, U+08E2  Arabic prepended concatenation marks.
 * U+070F            Syriac abbreviation mark.
 * U+2061..U+2064    Mathematical invisible operators.
 * U+110BD, U+110CD  Kaithi prepended concatenation marks.
 */
static int isIgnorableException ( UInt32 c )
{
    if ( ( c == 0x06DD ) ||
         ( ( c >= 0x0600 ) && ( c <= 0x0605 ) ) ||
         ( ( c >= 0x0890 ) && ( c <= 0x0891 ) ) ||
         ( c == 0x070F ) || ( c == 0x08E2 ) || 
         ( ( c >= 0x2061 ) && ( c <= 0x2064 ) ) ||
         ( c == 0x110BD ) || ( c == 0x110CD ) )
    {
        return ( 1 );
    }
    return ( 0 ) ;
}

#define SIFT_TRACE(p) \
    if (doTrace) { \
        printf("%6s  sift() line %d  %s\n", (p)->value, __LINE__, (p)->name); \
    }

/*
 * sift() 
 *
 * This is the central function of unisift. This routine
 * tokenizes a line from the unidata source file, breaking it up
 * into codepoint, name, and decomposition, checks for IGNOREON and
 * IGNOREOFF directives, and builds values for storage in the
 * array of WALNUTS.
 *
 * The "sift" consists of the progressive testing of character properties
 * to choose which branches to take for auto-assignement of primary,
 * secondary, and tertiary weights for non-decomposible characters.
 */

static int sift ( char *buf )
{
char *sp;
char *dp;
char *tp;
int i;
int rc;
int gotDecomp;
int tlen;
int tlen2;
UInt32 uvalue;
WALNUTPTR p;
char token1[20];  /* Codepoint in Hex */
char token2[128]; /* Unicode name */
char token3[128]; /* decomposition string */

int doTrace;

    sp = buf;

    /* Span and copy first token (field 0) */
    dp = token1;
    i = 0;
    while ( (*sp != ';') && (*sp != '\0') && ( i < 20 ) )
    {
        *dp++ = *sp++;
        i++;
    }
    *dp = '\0';
/*
 * Check for inline directives.
 *
 * IGNOREON forces a character to be weighted as ignorable, even if
 * it would otherwise be in a weighted class. This overrides *all*
 * property tests.
 *
 * IGNOREOFF forces a character to be weighted normally, even if it would
 * otherwise be in an ignored class. This impacts only miscellaneous
 * symbols which would otherwise be ignorable.
 *
 * CONTRACTION forces a character to be weighted as a unit, even if
 * it has a decomposition, and weights that decomposition as a contraction.
 *
 * DEFAULT turns all of these directives off, letting the sifter assign values
 * by property classes and decompositions, instead.
 */
    if ( strncmp ( token1, "IGNOREON", 8 ) == 0 )
    {
        ignoreOn = 1;
        ignoreOff = 0;
        contractOn = 0;
        return ( 0 );
    }
    else if ( strncmp ( token1, "IGNOREOFF", 9 ) == 0 )
    {
        ignoreOff = 1;
        ignoreOn = 0;
        contractOn = 0;
        return ( 0 );
    }
    else if ( strncmp ( token1, "CONTRACTION", 11 ) == 0 )
    {
        ignoreOff = 0;
        ignoreOn = 0;
        contractOn = 1;
        return ( 0 );
    }
    else if ( strncmp ( token1, "DEFAULT", 7 ) == 0 )
    {
        ignoreOff = 0;
        ignoreOn = 0;
        contractOn = 0;
        return ( 0 );
    }

    if ( *sp == '\0' ) return -1;
    /* Span semicolon */
    sp++;
    if ( *sp == '\0' ) return -1;

    /* Span and copy second token (field 1): character name */
    dp = token2;
    i = 0;
    while ( (*sp != ';') && (*sp != '\0') && ( i < 128 ) )
    {
        *dp++ = *sp++;
        i++;
    }
    *dp = '\0';
    if ( *sp == '\0' ) return -1;

    /* For the purposes of building the collation, omit the
     * special entries whose names start with "<", e.g.
     * <Hangul Syllable, First>, etc. Pass through the instances
     * of "<control>", however.
     */
    if ( token2[0] == '<' )
    {
        if ( strstr ( token2, "control" ) == NULL )
        {
            return (0);
        }
    }

    tlen = (int)strlen(token2);

    /* Span semicolon */
    sp++;
    if ( *sp == '\0' ) return -1;

    /* Span field 2: General_Category */
    for ( i = 0; i < 1; i++)
    {
        while ( (*sp != ';') && (*sp != '\0') ) sp++;
        if ( *sp == '\0' ) return -1;
        sp++;
        if ( *sp == '\0' ) return -1;
    }

    /* Span and copy third token (field 3): decomposition */
    dp = token3;
    i = 0;
    while ( (*sp != ';') && (*sp != '\0') && ( i < 128 ) )
    {
        *dp++ = *sp++;
        i++;
    }
    *dp = '\0';

    tlen2 = (int)strlen(token3);

    rc = convertHexToInt ( &uvalue, token1 );
    if ( rc < 0 )  /* Bad value in file */
    {
        return rc ;
    }

    gotDecomp = ( tlen2 > 0 );
    
    /* Now build up an entry in the data array to process decomps later */

    p = getSiftDataPtr(uvalue);

    if ( ( p->haveData == 1 ) && ( p->decompType == Implicit ) )
    {
/*
 * No further processing is necessary if we hit an ideograph in the
 * big blocks of ideographs. These have weights constructed
 * algorithmically.
 *
 */
        return ( 0 );
    }
    else
    {
        p->haveData = 1;
        p->uvalue = uvalue;
        p->whichKind = WK_Character; /* default */
    }
/*
 * Checking the gotDecomp value is more robust, as it allows characters
 * that might be inside CONTRACTION directives in the unidata input file
 * to pass through without contraction processing if they don't actually
 * have a decomposition to contract. This prevents an input file editing
 * oversight from causing a problem in the contraction processing.
 */
    if ( contractOn && gotDecomp )
    {
        p->contract = 1;
    }
    strcpy ( p->value, token1 );
    if ( tlen > 0 )
    {
        tp = (char *)malloc(tlen+1);
        if ( tp == NULL )
        {
            printf( "Memory allocation failure.\n" );
            return ( -1 );
        }
        strcpy (tp, token2);
        p->name = tp;
    }

    if ( tlen2 > 0 )
    {
        tp = (char *)malloc(tlen2+1);
        if ( tp == NULL )
        {
            printf( "Memory allocation failure.\n" );
            return ( -1 );
        }
        strcpy (tp, token3);
        p->decomp = tp;
/*
 * Store the decomposition type, which defaults to "Atomic".
 */
        if ( token3[0] == '<' )
        {
            p->decompType = (short) getTokenType ( token3 );
        }
        else
        {
            p->decompType = Canonical;
        }
    }

    // Turn SIFT_TRACE(p) on for certain characters.
    // For example, doTrace = uvalue == 0xA700;
    // Turn off via doTrace = FALSE;
    doTrace = uvalue == 0x16FF0;

/*
 * Autogenerate primary key weightings for base letters and
 * secondary key weightings for non-spacing combining marks.
 *
 * If the ignoring flag has been set by an explicit "IGNOREON" directive
 * in the source, mark this entry as ignorable, even if it has a decomp.
 * In this case, don't override the default values for primary, secondary,
 * or tertiary weights (i.e. these behave like controls).
 *
 * Handle the alphabetic case first, so that combining vowels in
 * the Indic scripts, etc., get alpha assignments instead of being
 * treated like accents, even if they are non-spacing combining marks.
 *
 * For now filter out ISO controls and Unicode controls as ignorables.
 * Pick up digits for multi-level key assignment.
 * Pick up space for multi-level key assignment.
 * Pick up punctuation and math symbols for multi-level key assignment.
 * Pick up currency symbols for multi-level key assignment.
 * Treat all the rest as ignorables.
 * See below for finer markup.
 *
 */
    if ( ignoreOn )
    {
        SIFT_TRACE(p);
        p->variable = 1;
        numIgnorables++;
    }
/*
 * Pass through characters with decompositions, except those that
 * have been explicitly marked with the CONTRACTION directive.
 *
 * The Catalan l's with dots are also passed through. These have
 * compatibility decompositions, but need special handling as
 * if they were secondary diacritic weighted first, before their
 * contractions are then also processed.
 */
    else if ( gotDecomp && 
         ( !contractOn || ( uvalue == 0x013F ) || ( uvalue == 0x0140 ) ) )
    {
        SIFT_TRACE(p);
        return ( rc );
    }
    else
    {
/*
 * This is the primary sifter. The order in which properties are
 * processed is critical to gradually sifting out the desired
 * weightings, so this algorithm is expressed as a long if-then-else
 * statement, rather than a switch statement.
 *
 * This branch handles all atomic characters (except those getting
 * implicit weighting), as well as all ordinary contractions.
 *
 * Contractions get a primary weight, as if they were a base letter.
 * Later, on the second pass, their decompositions will be handled
 * and given a weighting equal to that of the precomposed form.
 *
 * First weight any alphabetics, letting some exceptional
 * cases (iota subscript and some gc=Nl numerics) fall through.
 *
 */
        if ( unisift_IsAlphabetic ( uvalue ) && ! isAlphabeticException ( uvalue ) )
        {
            SIFT_TRACE(p);
            unisift_WeightAlphas ( p, uvalue );
        }
        else if ( unisift_IsIgnorable ( uvalue ) )
/*
 * Check for the property ignorable first, since some ignorable
 * characters (variation marks) are also combining marks. Treat these
 * as *completely* ignorable, even at the 4th level.
 *
 * Some specific format controls are treated as not ignorable
 * at the 4th level, but are still tagged as "ignorable". These
 * are U+0600..U+0603, U+06DD, and U+2061..U+2063, handled by
 * an explicit exceptions.
 *
 * This branch also picks up miscellaneous ISO control codes, which are
 * also marked ignorable.
 */
        {
            SIFT_TRACE(p);
            if ( isIgnorableException ( uvalue ) )
            {
                p->level4 = uvalue;
            }
            else
            {
                p->level4 = 0;
            }
            p->variable = 1;
            numIgnorables++;
        }
/*
 * Special-case the viramas, which must be weighted primary. They behave
 * like the absence of a vowel and have the same level of significance
 * as the vowel matras. Keep them weighted primary, so they will fall out
 * in ISCII order, for the Indic scripts at least. For now, treat all
 * the Brahmi-based script viramas identically, until shown otherwise.
 *
 * Separate killers in active use in scripts like Myanmar and Meetei Mayek
 * are also weighted this way, for consistency.
 *
 * The most reliable way to find these is by their combining class,
 * which is always set to 9.
 */
        else if ( unisift_GetCombiningClass ( uvalue ) == 9 )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( uvalue );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
/*
 * Assign secondary weightings to non-spacing, non-alphabetic combining marks,
 * other than the viramas.
 */
        else if ( unisift_IsNonSpacing ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->level2 = getNextSecondary( uvalue );
            p->level3 = FIRST_TERTIARY;
        }
/*
 * Assign secondary weightings to a few exceptional spacing diacritic marks
 * that should be treated as parallel to non-spacing marks.
 */
        else if ( isDiacriticException ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->level2 = getNextSecondary( uvalue );
            p->level3 = FIRST_TERTIARY;
        }
/*
 * Next pick up currency signs, so that all
 * the compatibility
 * equivalents for these will be properly weighted, too. For the
 * default table, do not treat these as ignorable.
 */
        else if ( unisift_IsCurrency ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
/*
 * Special-case processing for decimal digits, so that they are
 * grouped by numeric value.
 */
        else if ( unisift_IsDecimalDigit ( uvalue ) )
        {
        int val;

            SIFT_TRACE(p);
            if ( !digitsInitialized )
            {
                InitializeDigits();
                digitsInitialized = 1;
            }
            val = unisift_ToIntValue ( uvalue );
            if ( ( val < 0 ) || ( val > 9 ) )
            {
                printf ( "ERROR: Digit evaluated to %d for %04X\n", val, uvalue );
                return ( -1 );
            }
/*
 * Set the symbolBase value to that of the ASCII digit.
 */
            p->symbolBase = val + 0x30;
            p->level1 = digitPrimaryWeights[val];
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
        else if ( unisift_IsNumeric ( uvalue ) )
        {
        int val;

            SIFT_TRACE(p);
            val = unisift_ToIntValue ( uvalue );
            if ( ( val >= 0 ) && ( val <= 9 ) )
/*
 * This branch picks up Khmer divining numbers, Hangzhou
 * numerals, misc. Bengali, Aegean, Old Italic, etc.
 *
 * All the Dingbat circled digits are now bled off and treated
 * like the plain circled digits through artificial decomps.
 */
            {
                SIFT_TRACE(p);
                if ( !digitsInitialized )
                {
                    InitializeDigits();
                    digitsInitialized = 1;
                }
/*
 * Set the symbolBase value to that of the ASCII digit.
 */
                p->symbolBase = val + 0x30;
                p->level1 = digitPrimaryWeights[val];
                p->level2 = FIRST_SECONDARY;
                p->level3 = FIRST_TERTIARY;
            }
            else
/*
 * Numerics with values outside the range 0..9 are treated just
 * like miscellaneous symbols.
 */
            {
                SIFT_TRACE(p);
                p->symbolBase = uvalue;
                p->level1 = getNextPrimary( 0 );
                p->level2 = FIRST_SECONDARY;
                p->level3 = FIRST_TERTIARY;
                p->variable = 1;
                numIgnorables++;
            }
        }
/*
 * Next test for the IGNOREOFF directive flag, to allow overriding
 * the ignorable status of any of the rest of the characters, including
 * whitespace, a few format characters, and punctuation.
 */
        else if ( ignoreOff )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
/*
 * Branches to handle all other properties.
 *
 * Assign a primary weight to whitespace, punctuation, and
 * diacritics, but mark as ignorable by default.
 * This will allow compatibility
 * equivalents of punctuation and spaces to sift in correctly.
 *
 * Math symbols are treated as ignorable by default. (If they are to
 * be made non-ignorable by default, they should be
 * processed *after* punctuation, so that parentheses and the like would
 * get assigned as ignorables, first.) Math symbols are unusual in
 * that they contain numerous instances of characters decomposable
 * with the NOT operator. These would fall out more correctly if math symbols
 * as a class were not ignorable, but they are also punctuation-like, and
 * ordering them as non-ignorable by default results in various anomalies
 * for resemblant symbols and punctuation that are not math symbols.
 *
 * Pick up any stray compatibility ideographs which don't have
 * compatibility decomps, and so will get passed through the non-decomp
 * path. These are non-ignorables.
 *
 * Generic symbols not captured by one of the categories above are
 * treated as ignorables.
 *
 * For controls, assign
 * no primary, secondary, or tertiary weight, and mark as variable.
 */
        else if ( unisift_IsWhiteSpace ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
            p->variable = 1;
            numIgnorables++;
        }
        else if ( unisift_IsPunctuation ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
            p->variable = 1;
            numIgnorables++;
        }
/*
 * Extenders are handled after punctuation, to allow the punctuation
 * category to bleed off ignorable, ambiguous characters like
 * U+00B7 MIDDLE DOT.
 */
        else if ( unisift_IsExtender ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
        else if ( unisift_IsDiacritic ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
            p->variable = 1;
            numIgnorables++;
        }
        else if ( unisift_IsMath ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
            p->variable = 1;
            numIgnorables++;
        }
        else if ( unisift_IsIdeographic ( uvalue ) )
        {
        /* 
         * Compatibility Han ideographs not otherwise equated to a
         * character in the URO are treated as the URO. Their
         * values are all flagged special, so that their weights
         * are constructed as implicit expansions. The difference
         * is that they already have explicit names stored with them.
         *
         * Those treated as canonical equivalences to other
         * ideographs have already been bled off as having canonical
         * decompositions.
         */
            SIFT_TRACE(p);
            if ( ( uvalue >= 0xF900 ) && ( uvalue <= 0xFA2D ) )
            {
                p->level1 = 0xFFFF ;
                p->decompType = Implicit;
                p->whichKind = WK_Ideograph;
                p->uvalue = uvalue;
                p->symbolBase = uvalue;
                p->level2 = FIRST_SECONDARY;
                p->level3 = FIRST_TERTIARY;
            }
            else
        /*
         * This branch accounts for the few remaining ideographic
         * symbols, such as the Suzhou numerals, ideographic zero,
         * and the like. They are treated just as nonignorable symbols.
         */
            {
                p->level1 = getNextPrimary( 0 );
                p->symbolBase = uvalue;
                p->level2 = FIRST_SECONDARY;
                p->level3 = FIRST_TERTIARY;
            }
        }
        /*
         * Catch U+FFFD and force it to the specific high primary weight 0xFFFD.
         */
        else if ( uvalue == 0xFFFD )
        {
            p->symbolBase = uvalue;
            p->level1 = uvalue;
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
        else if ( unisift_IsMiscSymbolic ( uvalue ) )
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
            p->variable = 1;
            numIgnorables++;
        }
        else if ( ( uvalue >= 0xE000 ) && ( uvalue <= 0xF8FF ) )
/*
 * Allow generation of primary weights for PUA code points in the input file.
 * This makes it possible to generate contractions for unencoded
 * text elements.
 *
 * Currently this is a no-op, as no actual values are defined in
 * unidata.txt. But having this hook here allows experimentation
 * with PUA for such effects.
 *
 * Note that the sifter reserves F8F0..F8F4 as pseudo-variant
 * combining marks to allow for secondary weights not associated
 * with specific combining marks, and F8F5..F8F8 as generic combining marks,
 * to allow for collapsing of multiple different combining mark characters
 * onto certain generic secondary weights. F8F9..F8FF are reserved,
 * but currently unused. All values in the range F8F0..F8FF are
 * treated as non-spacing combining marks, and are intercepted much
 * earlier in the sift. They should not be used for experimentation
 * with primary weights for BMP PUA characters.
 */
        {
            SIFT_TRACE(p);
            p->symbolBase = uvalue;
            p->level1 = getNextPrimary( 0 );
            p->level2 = FIRST_SECONDARY;
            p->level3 = FIRST_TERTIARY;
        }
        else
/*
 * This branch currently should not be executed. All characters
 * should be caught earlier in the sift.
 */
        {
            printf ( "ERROR: exception branch executed for %04X\n", uvalue );
            p->level4 = 0;
            p->variable = 1;
            numIgnorables++;
        }

    }

    return ( rc );
}

/***************************************************************************/

/*
 * SECTION: Parse input file and pass valid lines through the sift.
 */

/*
 * parseInputFile()
 */
static int parseInputFile()
{
int i;
int rc;
int slen;
int lineCount;
int lineIsBlank;
FILE *fdi;
char buffer[512];

    fdi = fopen ( infile, "rt" );
    if ( fdi == NULL )
    {
        printf ( "Cannot open input.\n" );
        return -1 ;
    }
    lineCount = 0;
    rc = -1;
    while ( fgets( buffer, 512, fdi ) != NULL )
    {
    /* Don't process empty lines or comments. */
        slen = (int)strlen ( buffer );
        if ( ( slen == 0 ) || ( buffer[0] == ';' ) || ( buffer[0] == '#' ) )
            continue ;
    /* Also check for non-zero length lines with just whitespace */
        lineIsBlank = 1;
        i = 0 ;
        while ( lineIsBlank && ( i < slen ) )
        {
            if (!isspace(buffer[i]))
                lineIsBlank = 0;
            i++;
        }
        if ( lineIsBlank )
            continue;
#ifdef NOTDEF
        fputs ( buffer, stdout );
#endif
        lineCount++;
        if ( ( debugCounter > 0 ) && ( lineCount >= debugCounter ) )
        {
            printf( "Stopped at debugCounter %d.\n", debugCounter );
            break;
        }
        rc = sift ( buffer );
        if ( rc != 0 )
        {
            printf( "Failure at line %d.\n", lineCount );
            printf( "%s", buffer );
            printf( "\n" );

            break;
        }
    }
    fclose ( fdi );
    return ( rc );
}

/***************************************************************************/

/*
 * SECTION: Manipulation of the big array of Walnuts.
 */

static void pvInit( WALNUTPTR plane, UInt32 limit, int startingoffset )
{
UInt32 i;
WALNUTPTR p;

    for ( i = 0, p = plane; i < limit; i++, p++ )
    {
        p->haveData = 0;
        p->value[0] = '\0';
        p->numChars = 0;
        p->decompType = Atomic;
        p->name = NULL;
        p->decomp = NULL;
        p->udecomp = NULL;
        p->whichKind = WK_Unassigned;
        p->contract = 0;
        p->variable = 0;
        p->symbolBase = 0;
        p->level1 = 0;
        p->level2 = 0;
        p->level3 = 0;
/*
 * Initialize level4 in all cases to the value of the Unicode character.
 * This will have to be adjusted when dealing with characters off the BMP.
 */
        p->level4 = i + startingoffset;
    }
}


static void InitBagOfWalnuts(void)
{
    pvInit ( planes01, 0x20000, 0 );
    pvInit ( plane2, 0x800, 0x2F800 );
    pvInit ( plane14, 0x1F0, 0xE0000 );
}


static void InitHanData(void)
{
    handata.haveData = 1;
    handata.uvalue = 0xFFFF;
    strcpy ( handata.value, "FFFF" );
    handata.numChars = 1;
    handata.decompType = Implicit;
    handata.name = NULL;
    handata.decomp = NULL;
    handata.udecomp = NULL;
    handata.whichKind = WK_Ideograph;
    handata.contract = 0;
    handata.variable = 0;
    handata.symbolBase = 0xFFFF;
    handata.level1 = 0xFFFF;
    handata.level2 = FIRST_SECONDARY;
    handata.level3 = FIRST_TERTIARY;
    handata.level4 = 0xFFFF;
}

static void pvFree ( WALNUTPTR plane, int limit )
{
int i;
WALNUTPTR p;

    for ( i = 0, p = plane; i < limit; i++, p++ )
    {
        if (p->name != NULL)
            free ( p->name );
        if (p->decomp != NULL)
            free ( p->decomp );
        if (p->udecomp != NULL)
            free ( p->udecomp );
    }
}

static void FreeBagOfWalnuts(void)
{
    pvFree ( planes01, 0x20000 );
    pvFree ( plane2, 0x800 );
    pvFree ( plane14, 0x1F0 );
}

/*
 * CrackWalnuts()
 *
 * This routine does the work of processing the values from
 * the input file that have been distributed into the planes arrays.
 *
 * The NonDecomps processing deals with characters that have no
 * decompositions, or which have been explicitly marked as variable.
 * These dump a single weight into basekeys.txt, and for 14651 a
 * single weight entry in the tree to be printed later.
 *
 * The Decomps processing deals with characters that have a decomposition.
 * These dump a single, composite entry into compkeys.txt, and for
 * 14651 a single, composite entry in the tree to be printed later.
 *
 * The Contractions processing deals with characters that have a
 * decomposition but which have been marked for contracting.
 * These first dump a single weight entry exactly as for the
 * NonDecomps processing. They then do special contraction processing
 * that calculates the decomposition and dumps a contraction entry
 * into basekeys. For 14651 a collation-element is defined, and a composite
 * entry is also entered as a weight entry in the tree to be printed later. 
 */
static void CrackWalnuts( UInt32 i )
{
int rc;
WALNUTPTR t;
WALNUTS temp;

    t = getSiftDataPtr ( i );

    if ( t->haveData )
    {
        if ( t->decompType == Implicit )
        {
        /*
         * For implicit values, t points at handata. Make a copy of
         * the handata, set the uvalue
         * to the value passed in, and then process as
         * a nondecomp. The processNonDecomps routine detects implicit
         * cases, and expands the key according to the uvalue.
         */
            temp = *t;
            temp.uvalue = i;
            rc = processNonDecomps ( &temp ) ;
        }
        else if ( ( t->decomp == NULL ) || ( t->variable == 1 ) )
        {
            rc = processNonDecomps ( t );
        }
        else if ( t->contract )
        {
            if ( ( t->uvalue == 0x013F ) || ( t->uvalue == 0x0140 ) )
            /*
             * Special case Catalan l-dots, which need to be processed
             * as specialized decompositions with secondary weights first.
             */
            {
                rc = processDecomps ( t );
            }
            else
            /*
             * All other contractions just get primary weights.
             */
            {
                rc = processNonDecomps ( t );
            }
            if ( rc == 0 )
            {
                rc = processContractions ( t );
            }
        }
        else
        {
            rc = processDecomps ( t );
        }
        if ( rc == -1 )
        {
            printf ( "Processing error detected, c = %04X.\n", i );
        }
    }
}

/*
 * CrackAllWalnuts()
 *
 * This routine iterates through the entire assigned codespace,
 * calling CrackWalnuts() in turn for each code point.
 *
 * The routine encapsulates some ordering anomalies which
 * require the processing of a few characters out of code
 * point order because of unusual decomposition relationships.
 */
static void CrackAllWalnuts ( void )
{
int i;

    for ( i = 0 ; i <= 0x1FDC; i++ )
    {
        CrackWalnuts ( i );
    }
/*
 * Order hack to deal with Greek dasia. U+1FDD, U+1FDE, and U+1FDF
 * are canonically equivalent to dasia, which *follows* them in the
 * table.
 */
    CrackWalnuts ( 0x1FFE );
    for ( i = 0x1FDD; i <= 0x1FFD; i++ )
    {
        CrackWalnuts ( i );
    }
    CrackWalnuts ( 0x1FFF );
/*
 * Order hack to deal with en quad and em quad, which are canonically
 * equivalent to the characters which *follow* them in the table.
 */
    for ( i = 0x2002 ; i <= 0x2003; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0x2000 ; i <= 0x2001; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0x2004 ; i <= 0x33FF; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0x4DC0 ; i <= 0x4DFF; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0xA000 ; i <= 0xABFF; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0xD7B0 ; i <= 0xD7FF; i++ )
    {
        CrackWalnuts ( i );
    }
/* Skip Han and Hangul. */

#ifdef NOTDEF
/*
 * Temp hack for Kayah Li in PUA. Modify this
 * accordingly for any usage of PUA for special effects
 * in unidata.col. Also would need to make corresponding changes
 * for the output of the CTT for 14651, to ensure all and only correct entries
 * are dumped for that table, too.
 *
 * Also check the dumping of allkeys.txt, etc., to ensure that PUA
 * characters are filtered on output, if necessary.
 */
    for ( i = 0xE000 ; i <= 0xE009; i++ )
    {
        CrackWalnuts ( i );
    }
#endif
    for ( i = 0xF900 ; i <= 0xFFFD; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0x10000 ; i <= 0x16FFF; i++ )
    {
        CrackWalnuts ( i );
    }
/*
 * Skip the Tangut & Khitan ranges (17000..18D8F), which are treated like CJK.
 */
    for ( i = 0x18D90 ; i <= 0x1B16F; i++ )
    {
        CrackWalnuts ( i );
    }
/*
 * Skip the Nushu range (1B170..1B2FF), which is treated like CJK.
 */
    for ( i = 0x1B300 ; i <= 0x1FFFD; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0x2F800 ; i <= 0x2FFFD; i++ )
    {
        CrackWalnuts ( i );
    }
    for ( i = 0xE0000 ; i <= 0xE01EF; i++ )
    {
        CrackWalnuts ( i );
    }
}

/***************************************************************************/

/*
 * SECTION: Output file handling.
 */

static int openOutputFiles(void)
{
int f1, f2, f3, f5, f6;

    fdo1 = fopen ( outfile1, "wt" );
    f1 = ( fdo1 != NULL );

    fdo2 = fopen ( outfile2, "wt" );
    f2 = ( fdo2 != NULL );

    fdo6 = fopen ( outfile7, "wt" );
    f6 = ( fdo6 != NULL );

    if ( dumpDecomps )
    {
        fdo3 = fopen ( outfile3, "wt" );
        f3 = ( fdo3 != NULL );
    }
    else
    {
        f3 = 0;
    }

    if ( dumpSymbols )
    {
        fdo5 = fopen ( outfile5, "wt" );
        fdo7 = fopen ( outfile8, "wt" );
        f5 = ( ( fdo5 != NULL ) && ( fdo7 != NULL ) );
    }
    else
    {
        f5 = 0;
    }

    /*
     * Check and close all files, if any fopen failed.
     */
    if ( ( f1 == 0 ) || ( f2 == 0 ) || ( f6 == 0 ) ||
       ( dumpDecomps && ( f3 == 0 ) ) ||
       ( dumpSymbols && ( f5 == 0 ) ) )
    {
        if ( f1 )
        {
            fclose ( fdo1 );
        }
        if ( f2 )
        {
            fclose ( fdo2 );
        }
        if ( f3 )
        {
            fclose ( fdo3 );
        }
        if ( f5 )
        {
            fclose ( fdo5 );
            fclose ( fdo7 );
        }
        if ( f6 )
        {
            fclose ( fdo6 );
        }
        return ( -1 );
    }
    else
    {
        return ( 0 );
    }
}

static void closeOutputFiles(void)
{
    fclose ( fdo1 );
    fclose ( fdo2 );
    fclose ( fdo6 );
    if ( dumpDecomps )
    {
        fclose ( fdo3 );
    }
    if ( dumpSymbols )
    {
        fclose ( fdo5 );
        fclose ( fdo7 );
    }
}

/***************************************************************************/

/*
 * SECTION: Initialization and Summary.
 */

static void initGlobalVariables(void)
{
    /*
     * Debug counter. Defaults to 0.
     */
    debugCounter = 0;

    /*
     * Global variables needed for the input parse.
     */
    currentPrimary = FIRST_PRIMARY + 1;
    currentSecondary = FIRST_SECONDARY + 1;
    digitsInitialized = 0;
    numIgnorables = 0;
    ignoreOn = 0;
    ignoreOff = 0;
    contractOn = 0;

    /*
     * Global variables needed for the CrackWalnuts() loop.
     */
    badValues = 0;
    badChars = 0;
    badNames = 0;
    numCanonicals = 0;
    numCompatibles = 0;
    numCanonicalSequences = 0;
    numCanonicalCombSequences = 0;
    numCompatibleSequencesArab = 0;
    numCompatibleSequences = 0;

}

static void printSummaryResults ( void )
{
    printf ( "Bad values:     %d\n", badValues );
    printf ( "Bad chars:      %d\n", badChars );
    printf ( "Bad names:      %d\n", badNames );
    printf ( "Canonicals:     %d\n", numCanonicals );
    printf ( "Compatibles:    %d\n", numCompatibles );
    printf ( "CanonSeqs:      %d\n", numCanonicalSequences );
    printf ( "CanonCombSeqs:  %d\n", numCanonicalCombSequences );
    printf ( "CompatSeqsArab: %d\n", numCompatibleSequencesArab );
    printf ( "CompatSeqs:     %d\n", numCompatibleSequences );
    printf ( "Ignorables:     %d\n", numIgnorables );
    printf ( "Primary:        %04X..%04X (%d)\n", FIRST_PRIMARY,
                currentPrimary - 1, currentPrimary - FIRST_PRIMARY );
    printf ( "Secondary:      %04X..%04X (%d)\n", FIRST_SECONDARY,
                currentSecondary - 1, currentSecondary - FIRST_SECONDARY );
}

/***************************************************************************/

/*
 * SECTION: Table codas and epilogs.
 */

/*
 * dumpEOF()
 *
 * Drop the # EOF line for consistency with other UCD data files.
 */

void dumpEOF ( FILE *fd )
{
    fputs ( "\n# EOF\n", fd );
}

/*
 * dumpUCTableCoda()
 *
 * Starting with UCA 6.2, a small coda is dumped at the end of allkeys.txt.
 *
 * NB: Late breaking news. The UTC agreed to revert this change for UCA 6.2.
 * To keep things simple here, the relevant dumping code is simply
 * commented out, so the dumping will be easy to add back, in case the
 * UTC changes its mind again. For now, the DUCET for UCA 6.2 contains
 * no prefixcontractions section.
 *
 * This coda consists of an identifying label @prefixcontractions, for now
 * in a comment line, plus contraction productions for a small,
 * pre-calculated set of prefix sequences which are needed for closure of
 * contraction processing in the DUCET table.
 *
 * The list of exceptions which require this handling is small and well-known,
 * all involving the odd contractions needed for some unrecommended Tibetan
 * composite vowels. It is unlikely to expand in the future.
 * So the list is simply hard-coded here in the sifter.
 *
 * The calls to assembleKey construct the DUCET CE strings, which use whatever
 * weights have been assigned to the corresponding code points involved in
 * the contractions during the current sifter run, so that the hard-coded
 * entries will have CE's consistent with the rest of the table.
 */

void dumpUCATableCoda ( FILE *fd )
{
#ifdef NOTDEF
char buf1[30];
char buf2[30];
char buffer[128];

    fputs ( "\n# @prefixcontractions\n", fd );
    assembleKey ( getSiftDataPtr ( 0x0FB2 ), buf1 );
    assembleKey ( getSiftDataPtr ( 0x0F71 ), buf2 );
    sprintf ( buffer, "0FB2 0F71 ; %s%s\n", buf1, buf2 );
    fputs ( buffer, fd );
    assembleKey ( getSiftDataPtr ( 0x0FB3 ), buf1 );
    sprintf ( buffer, "0FB3 0F71 ; %s%s\n", buf1, buf2 );
    fputs ( buffer, fd );
#endif
    dumpEOF ( fd );
}

void dumpDecompsEpilog ( FILE *fd )
{
    dumpEOF ( fd );
}

/***************************************************************************/

/*
 * SECTION: Argument processing.
 */

static void usageMsg(void)
{
    fputs ("Usage: sifter (-v)(-?)(-dn)(-t)(-s)(-o path) filename\n", stdout );
    fputs ("       -v   show version.\n", stdout );
    fputs ("       -?   show this usage message.\n", stdout );
    fputs ("       -h   show this usage message.\n", stdout );
    fputs ("       -dn  set diagnostic mode: 1 minimum, 2 medium, 3 maximum.\n",
        stdout );
    fputs ("       -t   output formatted decomposition list.\n", stdout );
    fputs ("       -s   output POSIX-style symbolic forms.\n", stdout );
    fputs ("       -o path      specify a fully qualified path for output.\n",
        stdout );
}

static void versionMsg(void)
{
    fputs ( versionString, stdout );
}

static int processArguments( int argc, char *argv[] )
{
char argstring[LONGESTARG];
char* tmp;
char c;
int numargs = argc;
int foundInfile = 0;

/*
 * Set up default values for input parameters.
 */
    dumpDecomps = 0;
    dumpFileFormat = DFORM_New;
    dumpSymbols = 0;
    debugLevel   = 0;
    strcpy ( outpath, "" );

    while ( numargs > 1 )
    {
        strncpy (argstring, *++argv, LONGESTARG );
        argstring[LONGESTARG - 1] = '\0';
        numargs--;
        tmp = argstring;
        c = *tmp++;
        if ( c == '-' )
        {
            c = *tmp;
            switch ( c )
            {
            case 't' :
                dumpDecomps = 1;
                break;
            case 's' :
                dumpSymbols = 1;
                break;
            case 'd' :
                tmp++;
                if ( *tmp == '1' )
                {
                    debugLevel = 1;
                }
                else if ( *tmp == '2' )
                {
                    debugLevel = 2;
                }
                else if ( *tmp == '3' )
                {
                    debugLevel = 3;
                }
                else
                {
                    usageMsg();
                    return -1;
                }
                break;
            case 'o' :
                if ( numargs > 1 )
                {
                    strncpy (argstring, *++argv, PATHNAMELEN );
                    /* Truncate if necessary */
                    argstring[PATHNAMELEN - 1] = '\0';
                    numargs--;
                    strcpy ( outpath, argstring );
                }
                else
                {
                    usageMsg();
                    return -1 ;
                }
                break;
            case 'v' :
                versionMsg();
                return 0;
            case '?' :
            case 'h' :
                usageMsg();
                return 0;
            default:
                usageMsg();
                return -1;
            }
        }
        else
        {
            strncpy ( infile, argstring, PATHNAMELEN );
            infile[PATHNAMELEN - 1] = '\0';
            foundInfile = 1;
        }
    }
    if ( !foundInfile )
    {
        strcpy ( infile, defaultInfile );
    }

    return 1;

}

/***************************************************************************/

/* 
 * SECTION: debug routines
 */

/*
 * debugSift()
 *
 * This routine can be invoked with different ranges defined to try to
 * pinpoint problems in particular processing. It is designed
 * to grab and print out relevant data, and then stop processing.
 *
 * The values 0x021C and 0x0223 are just left over from the last
 * time this was compiled and used for a particular problem, and
 * are not critical values in any way.
 *
 */
#ifdef NOTDEF 
static int debugSift( void )
{
int i;
WALNUTPTR p;

    for ( i = 0x021C; i <= 0x0223; i++ )
    {
        p = getSiftDataPtr ( i );
        printf("p->haveData %d, i= %04X\n", p->haveData, i );
    }
    return ( -1 );   /* To stop the program */
}
#endif

/***************************************************************************/

/*
 * SECTION: main().
 */

int main( int argc, char *argv[] )
{
int rc;

/*
 * Command line processing to pick up flags to set debug level
 * or to change input and output options.
 */
    rc = processArguments ( argc, argv );
    if ( rc != 1 )
    {
        return rc;
    }

    initGlobalVariables();

    /* Initialize the big data arrays */

    InitBagOfWalnuts();

    /* 
     * Do special initialization for Han characters (and Tangut and Khitan and Nushu),
     * which are not in the input list. All decompositions
     * involving Han characters will be built algorithmically.
     */
    InitHanData();
    
    /*
     * Parse the input file completely, running the results through
     * the sift and storing the results in the data array.
     * 
     * debugCounter sets a limit on number of times through the
     * sift loop (if set to other than 0), to help pin down
     * problems in that process.
     */
#ifdef NOTDEF
    debugCounter = 100;
#endif

    rc = parseInputFile();

    /*
     * To debug any issues in the input parsing before the
     * CrackWalnuts processing, invoke debugSift() at this
     * point.
     */
#ifdef NOTDEF
    rc = debugSift();
#endif   
    if ( rc < 0 )
    {
        printf ( "Abnormal termination of input parse.\n" );
        FreeBagOfWalnuts();
        return ( -1 );
    }

/*
 * Attempt to open all required output files.
 *
 * In the steps below "fdo5" refers to the file for output
 * of the CTT (ctt14651.txt) for ISO 14651. "fdo7" refers to the file for
 * output of the DUCET (allkeys.txt) for UCA.
 */

    rc = openOutputFiles();
    if ( rc < 0 )
    {
        printf ( "Unable to open output files.\n" );
        FreeBagOfWalnuts();
        return ( -1 );
    }

/*
 * If dumping decomps, dump the prolog for that file.
 */

    if ( dumpDecomps )
    {
        rc = dumpDecompsFileProlog ( fdo3 );
        if ( rc != 0 )
        {
            FreeBagOfWalnuts();
            return ( -1 );
        }
    }
/*
 * Dump the prolog for the CTT for ISO 14651.
 * Then dump the list of collating symbols defined for that
 * table, which occurs ahead of the list of symbol weights.
 */

    if ( dumpSymbols )
    {
        dumpTableProlog ( fdo5 );
        dumpCollatingSymbols ( fdo5 );
    }

/*
 * CrackAllWalnuts() is the main loop which processes all the
 * characters in the array sequentially, dumping out weighted
 * keys for the single characters, and processing all decompositions
 * for the composed characters or compatibility sequences.
 *
 * The loop skips processing the big chunks of Han and precomposed
 * Hangul syllables, to avoid pushing all that redundant data into
 * the output files.
 */
    CrackAllWalnuts();

 /*
  * After CrackAllWalnuts has assigned all the character weights,
  * the next step is to hack in all Thai syllabic reordering
  * additions.
  *
  * These will be printed out to ctrckeys.txt.
  * If generating the symbol trees for allkeys.txt and
  * ctt14651.txt, this will also add these syllabic
  * weights into the symbol tree.
  */
    rc = processThaiSyllables();

/*
 * Now all the character weights have been assigned.
 *
 * Depending on the input parameters, the partial output listings
 * done in processing order (basekeys.txt, compkeys.txt, ctrckeys.txt)
 * will have been completely generated and dumped by this point,
 * during the CrackAllWalnuts main loop and processThaiSyllables.
 */
    rc = 0;
/*
 * If dumpSymbols was specified on the input line,
 * build and dump the sorted tables for ISO 14651 (ctt14651.txt)
 * and allkeys.txt for UCA.
 */
    if ( dumpSymbols )
    {
/*
 * First dump the statically constructed tertiary and secondary
 * weight symbols for the CTT.
 */
        dumpWeights ( fdo5 );
/*
 * Next build a sorted btree of the primary weight symbols, and
 * then dump them in sorted order for CTT.
 */
        rc = unisift_BuildSymWtTree();
        if ( rc == 0 )
        {
            unisift_PrintSymWtTree ( fdo5 );
        }
        else
        {
            printf ( "Failure to build symbol weight tree.\n" );
            rc = -1;
        }
        unisift_DropSymWtTree();

/*
 * Dump the entries header for the CTT.
 */
        dumpEntriesHeader ( fdo5 );
/*
 * Print the content of the sorted symbol list to the CTT table.
 * This is a recursive call on the btree. It is a no-op if the
 * build of the btree failed.
 */
        printf ( "Dumping symbolic table for the CTT in sorted order.\n" );
        unisift_PrintSymTree ( fdo5 );
/*
 * Next print the content of the sorted symbol list to the DUCET (allkeys.txt)
 * table for UCA. This is also a recursive call on the btree.
 * unisift_PrintUCATree encapsulates the dump of most of allkeys.txt,
 * including its prolog.
 */
        printf ( "Dumping allkeys.txt for UCA in sorted order.\n" );
        unisift_PrintUCATree ( fdo7 );
/*
 * Drop the in memory sorted tree of weights. This tree will have
 * been built up incrementally during the CrackAllWalnuts() loop, if
 * dumpSymbols is true.
 */
        unisift_DropSymTree();
/*
 * Dump the table epilog for the CTT.
 */
        dumpTableEpilog ( fdo5 );
/*
 * Dump the table code for the UCA. This consists of a hard-coded small
 * listing of prefix contractions needed for closure of contractions.
 */
        dumpUCATableCoda ( fdo7 );
    }

    if (dumpDecomps)
    {
        dumpDecompsEpilog ( fdo3 );
    }

/*
 * No longer need name and decomp strings, so free them.
 */
    FreeBagOfWalnuts();

    closeOutputFiles();

/*
 * Print a summary of the results to stdout.
 */
    printSummaryResults();

    return ( rc );
}
