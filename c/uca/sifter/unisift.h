// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**      Unilib
**      Copyright 2017
**      Ken Whistler, All rights reserved.
*/

/*
 * unisift.h
 *
 * Shared structs and declarations.
 *
 * 2009-Nov-24 Clean up code for archiving.
 * 2010-May-24 Added explicit setting of BYTESWAP flag.
 * 2012-Jul-25 Removed declaration of unisift_GetWeight.
 * 2012-Nov-27 Removed declaration of getFirstDigitSecWeight.
 *             Removed unneeded define of Digit decomp type.
 * 2017-Mar-08 Renamed "ignorable" field to "variable"for clarity.
 */

#include <stdio.h>
#include <string.h>

/*
 * Build the sifter with a /D INTEL flag on byteswapping platforms.
 * This will define BYTESWAP, which is used in unisyms.c during
 * building of the weight symbols tree used to sort all of the
 * weights for allkeys.txt and ctt14651.txt generation.
 */

#if defined (INTEL)

#define BYTESWAP

#endif /* INTEL */

/*
 * Definitions of basic integral and string types used in the code.
 *
 * (In lieu of inclusion of proprietary header file.)
 */
typedef unsigned char  UChar8;

typedef unsigned short UShort16;

typedef unsigned int   UInt32;

typedef UShort16 unichar;

typedef UInt32 utf32char;

typedef unichar* unistring;

#define UNINULL ((unichar)(0))

/*
 * Definition of decomposition types.
 *
 * Atomic is the default. It refers to characters that have
 * no decomposition and have no complications on weight
 * assignment.
 *
 * Canonical decompositions are sequences of other characters.
 *
 * Implicit decompositions involve expansions to constructed
 * sequences of weights.
 *
 * Compatibility decompositions are subcategorized using
 * the token types.
 */

#define Atomic (-1)
#define Canonical (0)
#define Implicit (-2)

/*
 * Definition of token types used in the sifter.
 */

#define NullToken (0)
#define FormatToken (1)
#define CharToken (2)
#define FontToken (3)
#define NoBreakToken (4)
#define InitialToken (5)
#define MedialToken (6)
#define FinalToken (7)
#define IsolatedToken (8)
#define CircleToken (9)
#define SuperToken (10)
#define SubToken (11)
#define VerticalToken (12)
#define WideToken (13)
#define NarrowToken (14)
#define SmallToken (15)
#define SquareToken (16)
#define CompatToken (17)
#define FractionToken (18)
#define SmallNarrowToken (19)
#define CircleKataToken (20)
#define SortToken (21)
#define SortCanonToken (22)
#define FillerToken (23)

/* Weight ranges used by the sifter program to create weighted keys. */

#define FIRST_TERTIARY  (2)
#define FIRST_SECONDARY (0x20)  /* = 32 */
#define FIRST_PRIMARY   (0x200) /* = 512 */

#define LAST_TERTIARY   (0x1F)
#define LAST_SECONDARY  (0x1FF)
#define LAST_PRIMARY    (0xFF3F)  /* Reserve the last 192 (0xC0) values. */

#define IGN_DELIM (FIRST_TERTIARY - 1)  /* 0x01 */

#define CJK_BASE (0xFB40)
#define CJK_EXT_BASE (0xFB80)
#define UNASSIGNED_BASE (0xFBC0)

/*
 * Definition of tertiary weights for sortkeys.
 *
 * These generally match the compatibility labels from the UnicodeData
 * file, but have been extended for Hiragana and Katakana to enable the
 * correct specification of tertiary levels for those scripts, which
 * must intermix correctly.
 */

#define SKEY_TWT_SMALL      (2)
#define SKEY_TWT_WIDE       (3)
#define SKEY_TWT_COMPAT     (4)
#define SKEY_TWT_FONT       (5)
#define SKEY_TWT_CIRCLE     (6)

#define SKEY_TWT_CAPITAL    (8)
#define SKEY_TWT_WIDE_CAP   (9)
#define SKEY_TWT_COMPAT_CAP (10)
#define SKEY_TWT_FONT_CAP   (11)
#define SKEY_TWT_CIRCLE_CAP (12)

#define SKEY_TWT_SMALL_HIRA  (13) /* Small form Hiragana letter */
#define SKEY_TWT_NORM_HIRA   (14) /* Normal Hiragana letter */
#define SKEY_TWT_SMALLCJK    (15) /* Small form Katakana, or small CJK */
#define SKEY_TWT_SMALL_NAR   (16) /* Small form halfwidth Katakana */
#define SKEY_TWT_NORM_KATA   (17) /* Normal Katakana letter */
#define SKEY_TWT_NARROW      (18) /* Halfwidth Katakana or other letter */
#define SKEY_TWT_CIRCLE_KATA (19) /* Circled Katakana */

#define SKEY_TWT_SUPER      (20)
#define SKEY_TWT_SUB        (21)
#define SKEY_TWT_VERTICAL   (22)
#define SKEY_TWT_INITIAL    (23)
#define SKEY_TWT_MEDIAL     (24)
#define SKEY_TWT_FINAL      (25)
#define SKEY_TWT_ISOLATED   (26)
#define SKEY_TWT_NOBREAK    (27)
#define SKEY_TWT_SQUARE     (28)
#define SKEY_TWT_SQUARE_CAP (29)
#define SKEY_TWT_FRACTION   (30)

/*
 * Struct used for storing basic weight information. Used
 * for sorting the tree.
 */
typedef struct {
  int symbolBase;
  int level1;
  int level2;
  int level3;
  int level4;
  int variable;
  } RAWKEY;

typedef RAWKEY *RAWKEYPTR;

/*
 * Basic data struct used by the sifter for storing information
 * about characters for the sift.
 */
typedef struct {
  UInt32 uvalue;   /* stored as Unicode scalar value */
  char   value[7]; /* as hex string -- up to 6 digits for Astral Planes */
  UChar8 numChars;
  UShort16 haveData;
  short  decompType;
  char*  name;
  char*  decomp;   /* Unanalyzed decomposition, parsed from unidata. */
  UInt32 *udecomp; /* Recursive full decomposition, stored as UTF-32 string */
  char   variable;   /* boolean: true for variable collation element */
  char   contract;   /* boolean */
  short  whichKind;  /* enumerated type */
  UInt32 symbolBase; /* base for generating a 14651 level 1 symbol */
  int    level1;
  int    level2;
  int    level3;
  int    level4;
  } WALNUTS;

typedef WALNUTS *WALNUTPTR;

/*
 * Routines exported from unisyms.c; called from unisift.c
 */
extern void assembleSym ( WALNUTPTR t1, char* symbol, char* ucastr );
extern int assembleThaiContractionSym ( UInt32 consonant, UInt32 vowel, char* symbol,
    char* ucastr );
extern int assembleLaoDigraphContractionSym ( UInt32 consonant, UInt32 vowel, 
    char* symbol, char* ucastr );
extern int assembleCompositeSym ( WALNUTPTR t1, char* symbol, RAWKEYPTR rkeys, int numrkeys,
    char* ucastr );

extern void dumpTableProlog( FILE *fd );
extern void dumpCollatingSymbols( FILE *fd );
extern void dumpWeights( FILE *fd );
extern void dumpEntriesHeader( FILE *fd );
extern void dumpTableEpilog( FILE *fd );

extern void unisift_PrintSymTree ( FILE *fd );
extern void unisift_PrintUCATree ( FILE *fd );
extern void unisift_DropSymTree ( void );

extern int  unisift_BuildSymWtTree ( void );
extern void unisift_PrintSymWtTree ( FILE *fd );
extern void unisift_DropSymWtTree ( void );


/*
 * Utility routines exported from unisift.c; called from unisyms.c.
 */

extern int weightFromToken ( WALNUTPTR t1, int tokenType );

extern WALNUTPTR getSiftDataPtr ( UInt32 c );

/*
 * Wrapper functions for external routines. Defined in unisifex.c.
 */
int unisift_IsAlphabetic   ( UInt32 c );
int unisift_IsNonSpacing   ( UInt32 c );
int unisift_IsCombining    ( UInt32 c );
int unisift_IsExtender     ( UInt32 c );
int unisift_IsCurrency     ( UInt32 c );
int unisift_IsPunctuation  ( UInt32 c );
int unisift_IsDiacritic    ( UInt32 c );
int unisift_IsNumeric      ( UInt32 c );
int unisift_IsDecimalDigit ( UInt32 c );
int unisift_IsWhiteSpace   ( UInt32 c );
int unisift_IsMath         ( UInt32 c );
int unisift_IsIdeographic  ( UInt32 c );
int unisift_IsMiscSymbolic ( UInt32 c );
int unisift_IsIgnorable    ( UInt32 c );

int unisift_IsUpper ( UInt32 c );

UInt32 unisift_ToLower ( UInt32 c );

UInt32 unisift_ToUpper ( UInt32 c );

int unisift_ToIntValue ( UInt32 c );

unistring unisift_unistrcpy ( unistring s1, const unistring s2 );

unistring unisift_unistrcat ( unistring s1, const unistring s2 );

int unisift_GetCombiningClass ( UInt32 c );

