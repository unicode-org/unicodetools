/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2018, Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * bidirefp.h
 *
 * Private types, constants, and declarations used by bidiref.
 */

/*
 * Change history:
 *
 * 2013-Jun-02 kenw   Created.
 * 2013-Jun-05 kenw   Added diretyBit to UBACONTEXT.
 * 2013-Jun-19 kenw   Add declaration for br_ErrPrint.
 * 2013-Jul-08 kenw   Move BR_MAXINPUTLEN to bidiref.h.
 * 2014-Apr-17 kenw   Add accelerator flags to BIDIUNIT.
 * 2015-Jun-05 kenw   Set MAXPAIRINGDEPTH to 63 for UBA 8.0.
 * 2015-Aug-19 kenw   Add CJK Extension E range.
 * 2015-Dec-04 kenw   Bumped up bracket stack size by one.
 * 2016-Sep-22 kenw   Add datapath param to br_InitTable.
 * 2016-Oct-05 kenw   Add NUMVERSIONS define
 *                       and declaration of GetUBAVersionStr.
 * 2017-Jun-26 kenw   Add CJK Extension F range for UCD 10.0.
 * 2018-Jul-22 kenw   Update NUMVERSIONS for UBACUR and UBA110.
 */

#ifndef __BIDIREFP_LOADED
#define __BIDIREFP_LOADED

#ifdef  __cplusplus
extern "C" {
#endif

#include "bidiref.h"

/*
 * The number of supported UBA versions. Used to define a static
 * table of strings, for diagnostic output.
 */

#define NUMVERSIONS (9)

/*
 * Constants defining Unihan ranges.
 */

#define CJK_URO_FIRST  (0x4E00)
#define CJK_URO_LAST   (0x9FEA)
#define CJK_EXTA_FIRST (0x3400)
#define CJK_EXTA_LAST  (0x4DB5)
#define CJK_EXTB_FIRST (0x20000)
#define CJK_EXTB_LAST  (0x2A6D6)
#define CJK_EXTC_FIRST (0x2A700)
#define CJK_EXTC_LAST  (0x2B734)
#define CJK_EXTD_FIRST (0x2B740)
#define CJK_EXTD_LAST  (0x2B81D)
#define CJK_EXTE_FIRST (0x2B820)
#define CJK_EXTE_LAST  (0x2CEA1)
#define CJK_EXTF_FIRST (0x2CEB0)
#define CJK_EXTF_LAST  (0x2EBE0)

/*
 * Enumerated types for relevant Unicode properties,
 * to make reference to them easier to understand in the algorithm.
 */
typedef enum 
  {
	  BIDI_None,
	  BIDI_Unknown,
    BIDI_L,    /* 0x00 strong: left-to-right (bc=L) */
    BIDI_ON,   /* 0x01 neutral: Other Neutral (bc=ON) */
    BIDI_R,    /* 0x02 strong: right-to-left (bc=R) */
    BIDI_EN,   /* 0x03 weak: European Number (bc=EN) */
    BIDI_ES,   /* 0x04 weak: European Number Separator (bc=ES) */
    BIDI_ET,   /* 0x05 weak: European Number Terminator (bc=ET) */
    BIDI_AN,   /* 0x06 weak: Arabic Number (bc=AN) */
    BIDI_CS,   /* 0x07 weak: Common Number Separator (bc=CS) */
    BIDI_B,    /* 0x08 neutral: Paragraph Separator (bc=B) */
    BIDI_S,    /* 0x09 neutral: Segment Separator (bc=S) */
    BIDI_WS,   /* 0x0A neutral: White Space (bc=WS) */
    BIDI_AL,   /* 0x0B strong: right-to-left Arabic (bc=AL) */
    BIDI_NSM,  /* 0x0C weak: non-spacing mark (bc=NSM) */
    BIDI_BN,   /* 0x0D weak: boundary neutral (bc=BN) */
    BIDI_PDF,  /* 0x0E format: pop directional formatting (bc=PDF) */
    BIDI_LRE,  /* format: left-to-right embedding */
    BIDI_LRO,  /* format: left-to-right override */
    BIDI_RLE,  /* format: right-to-left embedding */
    BIDI_RLO,  /* format: right-to-left override */
    BIDI_LRI,  /* format: left-to-right isolate */
    BIDI_RLI,  /* format: right-to-left isolate */
    BIDI_FSI,  /* format: first strong isolate */
    BIDI_PDI,  /* format: pop directional isolate */
    BIDI_MAX
    } BIDIPROP;

typedef enum 
  {
    BPT_O, BPT_C, BPT_None  
  } BPTPROP;

/*
 * File format defines.
 *
 * These defines can be used to distinguish between formats
 * of data to be read in. They are used to branch the
 * parsing functions in brinput.c.
 */

#define FORMAT_A (0) /* Format used in BidiCharacterTest.txt */
#define FORMAT_B (1) /* Format used in BidiTest.txt */

/*
 * ALGORITHM_STATE is used to store how far along in
 * algorithm the data has been processed. This can be
 * used to help the debug display to show relevant
 * information.
 */
typedef enum 
  {
    State_Unitialized, /* context allocated, but no data */
    State_Initialized, /* test case data read in and parsed */
    State_P3Done,      /* rules done through P3 */
    State_X8Done,      /* rules done through X8 */
    State_X9Done,      /* rules done through X9 */
    State_RunsDone,    /* rules done through X10 part 1: runs are identified */
    State_X10Done,     /* rules done through X10: runs & seqs are identified */
    State_W1Done,      /* rules done through W1:  combining marks are resolved */
    State_W2Done,      /* rules done through W2: */
    State_W3Done,      /* rules done through W3: */
    State_W4Done,      /* rules done through W4: */
    State_W5Done,      /* rules done through W5: */
    State_W6Done,      /* rules done through W6: */
    State_W7Done,      /* rules done through W7:  weak types are resolved */
    State_N0Done,      /* rules done through N0:  bracket pairs are resolved */
    State_N1Done,      /* rules done through N1: */
    State_N2Done,      /* rules done through N2:  neutral types are resolved */
    State_I2Done,      /* rules done through I2:  implicit levels are resolved */
    State_L1Done,      /* rules done through L1:  trailing whitespace resolved */
    State_L2Done,      /* rules done through L2:  reordering data available */
    State_Complete     /* finished application of rules: ready for checking */
  } ALGORITHM_STATE;

/*
 * BIDIPROPDATA
 *
 * This struct is used to store property information
 * about characters and code points.
 *
 * Only limited property data is stored -- just the data
 * needed for running the UBA algorithm.
*/
typedef struct {
  int han;
  BIDIPROP bidivalue;
  U_Int_32 bpb;
  BPTPROP bpt;
  } BIDIDATA;

typedef BIDIDATA *BDPTR;

#define BPB_None (0xFFFFFFFF)

typedef enum
  {
    Dir_LTR, Dir_RTL, Dir_Auto, Dir_Unknown
  } Paragraph_Direction;

typedef enum 
  {
    Override_Neutral, Override_LTR, Override_RTL  
  } D_Override_Status;

/*
 * A single status stack definition is shared by both the
 * UBA62 and UBA63 reference implementations. The
 * UBA62 implementation makes no use of the isolate_status
 * field.
 *
 * The status stack is used by rules X1-X8.
 */
typedef struct 
  {
    int embedding_level;
    D_Override_Status override_status; /* direction */
    int isolate_status; /* boolean */
  } STATUSSTACKELEMENT;

typedef STATUSSTACKELEMENT *STACKPTR;

/*
 * The maximum_depth for embedding for UBA62.
 */
#define MAX_DEPTH_62 61
/*
 * The maximum_depth for embedding for UBA63.
 */
#define MAX_DEPTH_63 125

/*
 * Typedefs used in the bracket pairing algorithm in rule N0.
 */

typedef struct {
    U_Int_32 bracket;
    int pos;
  } BRACKETSTACKELEMENT;

typedef BRACKETSTACKELEMENT *BRACKETSTACKPTR;

typedef struct Pairing_Element_Struct {
    struct Pairing_Element_Struct *next;
    int openingpos;
    int closingpos;
  } PAIRINGELEMENT;

typedef PAIRINGELEMENT *PAIRINGPTR;

/*
 * MAXPAIRINGDEPTH is used to declare the bracket pairing stack.
 *
 * For UBA 8.0 and subsequent versions, this depth is specified
 * to be exactly 63. (See brrule.c, where the stack allocation is
 * made as MAXPAIRINGDEPTH +1.)
 *
 * For UBA 6.3 (and 7.0), these depth was implementation-dependent,
 * and not specified by UAX #9. For the 6.3 and 7.0 versions of
 * bidiref it was set to half the MAXINPUTLEN, because in principle an
 * input string could consist *only* of brackets to pair up: "(({{[]}}))",
 * etc., in which case the maximum possible set of pairs would be
 * half the text length.
 *
 * Starting with the 8.0 version of bidiref, however, 63 is used
 * also for testing the 6.3 and 7.0 versions of UBA, and for
 * consistency, the stack full error handling is also based
 * on the 8.0 specification.
 */

#define MAXPAIRINGDEPTH (63)

/* 
 * Provide a reasonable length for input-handling buffers, based
 * on MAXINPUTLEN, which limits how many code points the
 * implementation will handle for an input string.
 */

#define BUFFERLEN (5 * BR_MAXINPUTLEN)   /* for data chunks parsed from input */
#define RAWBUFFERLEN (2 * BUFFERLEN)  /* full unparsed input line */

/*
 * BIDIUNIT
 *
 * This struct is the primitive manipulated by
 * the UBA reference implementation. It is used
 * to construct a vector in the UBACONTEXT,
 * containing the character data (stored as UTF-32),
 * the looked-up property data, so that the UBA,
 * which works primarily on the basis of the Bidi_Class
 * values, has the correct input, along with the
 * level and order information.
 *
 * Starting with Version 2.0 of bidiref, a number of
 * accelerator flags are added, to speed up checking of
 * common combinations of Bidi_Class values. Each flag
 * is conceptually a Boolean, and could be stored as a bit
 * in a bitfield, but in this reference implementation
 * is simply stored as another int field in the BIDIUNIT.
 * These values are all initialized when the BIDIUNIT
 * vector is initialized for a testcase string. Some need
 * to be reset, whenever a rule changes the current bc
 * value for a character.
 */
typedef struct {
  U_Int_32 c;       /* character value, stored as UTF-32 */
  BIDIPROP bc;      /* current Bidi_Class value */
  BIDIPROP orig_bc; /* store original value for Bidi_Class */
  int bc_numeric;   /* accelerator for bc = (AN or EN) */
  int bc_isoinit;   /* accelerator for bc = (LRI or RLI or FSI) */
  U_Int_32  bpb;    /* bidi paired bracket property */
  BPTPROP bpt;      /* bidi paired bracket type property */
  int level;    /* current embedding level */
  int expLevel; /* store expected level for test case */
  int order;    /* store position for reordering */
  int order2;   /* spare order array used in reversal algorithm */
} BIDIUNIT;

typedef BIDIUNIT *BIDIUNITPTR;

#define NOLEVEL (-1)

/*
 * A text chain is simply an array of pointers to BIDIUNITs.
 *
 * This data structure is introduced to help in the abstraction
 * of rule application between UBA62 (and earlier), where
 * rules apply to level runs, and UBA63 (and later), where
 * rules apply to isolating run sequences. Because
 * isolating run sequences may contain indefinite lists
 * of discontiguous level runs, it is difficult to
 * specify rule application directly to the isolating
 * run sequences -- the concept of previous character
 * and next character in the sequence gets rather complex
 * and makes the implementation difficult.
 *
 * One possible approach to the isolating run sequences
 * would be to simply clone the entire text into
 * allocated contiguous BIDIUNIT vectors for each 
 * isolating run sequence identified. However, that makes
 * bookkeeping in the original input structure a bit more
 * complex. Instead, whenever an isolating run sequence
 * is identified, a contiguous text chain is allocated
 * and initialized instead. This keeps all the data
 * in the original input structure, and simply embodies
 * the traversal order (and prior and next relation)
 * needed for rule application.
 *
 * So that the rule application can be uniform, a
 * text chain is also allocated for level runs, as well.
 *
 * The text chain is hung off the "textChain" field in
 * both the BIDIRUN and the ISOLATING_RUN_SEQUENCE
 * struct definitions. The "len" field defines the
 * length of that array.
 */


/*
 * BIDIRUN
 *
 * Once embedding levels are determined, the UBA
 * treats each contiguous sequence at the same level
 * as a distinct run.
 *
 * To simplify the processing of runs, a list of runs
 * is constructed as a linked list, which hangs off
 * the UBACONTEXT.
 *
 * Each BIDIRUN consists of pointers to the first
 * and last BIDIUNIT of the run, and then additional
 * information calculated during the X10 rule, when
 * the runs are identified.
 *
 * This may not be the most efficient approach to
 * an implementation, but it makes it much easier
 * to express the X10 rule and subsequent rules which
 * process the runs individually.
 *
 * The seqID is only used in processing run lists
 * into isolating run sequence lists in UBA63.
 * It is initialized to zero. During processing
 * to identify isolating run sequences, it is set
 * to the sequence id of the isolating run sequence
 * that a run is assigned to.
 */

typedef struct BidiRunStruct {
  struct BidiRunStruct *next; /* next run */
  BIDIUNITPTR first;  /* first element of this run */
  BIDIUNITPTR last;   /* last element of this run */
  int len;            /* explicit len, to simplify processing */
  int runID;          /* stamp run with id for debugging */
  int seqID;          /* isolating run sequence id */
  int level;          /* embedding level of this run */
  BIDIPROP sor;       /* direction of start of run: L or R */
  BIDIPROP eor;       /* direction of end of run: L or R */
  BIDIUNITPTR *textChain;  /* constructed text chain */
} BIDIRUN;

typedef BIDIRUN *BIDIRUNPTR;

/*
 * The BidiRunListStruct abstracts the creation of
 * a list of bidi runs for attachment to the isolating
 * run lists. Instead of duplicating all the run information
 * into that list, the list consists just of pointers to
 * the already allocated basic list of runs.
 */
typedef struct BidiRunListStruct {
  struct BidiRunListStruct *next;
  BIDIRUNPTR run;
} BIDIRUNLISTELEMENT;

typedef BIDIRUNLISTELEMENT *BIDIRUNLISTPTR;

/*
 * ISOLATING_RUN_SEQUENCE
 *
 * This is a concept introduced in UBA63.
 *
 * Essentially it consists of an ordered sequence of
 * bidi runs, as defined in BD13.
 *
 * It is implemented here by attaching the list of runs
 * associated with this particular isolating run sequence.
 * The attached list just contains pointers to each of
 * the relevant BIDIRUN structs in the already constructed
 * sequential list of runs attached to the UBACONTEXT.
 *
 * All runs associated with a single isolating run sequence
 * are at the same level, so that level can be stored
 * in the isolating run sequence struct for ease of access.
 *
 * Each isolating run sequence has a start of sequence (sos)
 * and end of sequence (eos) directional value assigned.
 * These are calculated based on the sor and eor values
 * for the associated list of runs, but again, are stored
 * in the isolating run sequence struct for ease of access.
 */

typedef struct IRSequenceStruct {
  struct IRSequenceStruct *next; /* next sequence */
  BIDIRUNLISTPTR theRuns;  /* list of runs in this sequence */
  BIDIRUNLISTPTR lastRun;  /* for list appending */
  int len;             /* explicit len, to simplify processing */
  int seqID;           /* stamp seq with id for debugging */
  int level;           /* embedding level of this seq */
  BIDIPROP sos;        /* direction of start of seq: L or R */
  BIDIPROP eos;        /* directino of end of seq: L or R */
  BIDIUNITPTR *textChain;   /* constructed text chain */
} ISOLATING_RUN_SEQUENCE;

typedef ISOLATING_RUN_SEQUENCE *IRSEQPTR;

/*
 * UBACONTEXT
 *
 * This struct is used to store all context associated
 * with the bidi reference UBA processing, including
 * input, expected test output,
 * and the constructed runs and other intermediate data.
 *
 * theText, testLen, paragraphDirection are input.
 * theRuns and paragraphEmbeddingLevel are calculated.
 * expEmbeddingLevel, expOrder are parsed
 *    from the testcase data and checked against
 *    calculated values.
 * For simplicity, the expected levels data parsed
 * from the test case are stored with theText.
 *
 * Starting from Version 2.0, theText pointer is
 * allocated statically, simply pointing to a static
 * buffer of BR_MAXINPUTLEN length, to cut down on
 * repeated dynamic allocations during long testcase
 * runs.
 */
typedef struct {
  ALGORITHM_STATE state; /* track state */
  int dirtyBit;          /* used for debug output control */
  int64_t testId;        /* 64-bit id used for tagging trace output */
  Paragraph_Direction paragraphDirection; /* input */
  int paragraphEmbeddingLevel;       /* calculated */
  int textLen;                            /* input */
  BIDIUNITPTR theText;                    /* input */
  BIDIRUNPTR theRuns;     /* calculated */
  BIDIRUNPTR lastRun;     /* for list appending */
  IRSEQPTR theSequences;  /* calculated: UBA63 only */
  IRSEQPTR lastSequence;  /* for list appending */
  int expEmbeddingLevel;  /* expected test result */
  char *expOrder;         /* expected test result */
} UBACONTEXT;

typedef UBACONTEXT *UBACTXTPTR;

/*
 * BIDI_RULE_TYPE
 *
 * An enumerated type which facilitates lookup of
 * rule callbacks and other data by rule type.
 */

typedef enum {
  UBA_RULE_W1, 
  UBA_RULE_W2, 
  UBA_RULE_W3, 
  UBA_RULE_W4,
  UBA_RULE_W5,
  UBA_RULE_W6,
  UBA_RULE_W7,
  UBA_RULE_N0,
  UBA_RULE_N1,
  UBA_RULE_N2,
  UBA_RULE_Last
} BIDI_RULE_TYPE;

/*
 * RULE_CONTEXT
 *
 * A struct used to package up parameter information
 * for a class of rules, to make parameter passing
 * and function prototype neater.
 */

typedef struct {
  BIDIUNITPTR *textChain;
  int len;
  int level;
  BIDIPROP sot;
  BIDIPROP eot;
} BIDI_RULE_CONTEXT;

typedef BIDI_RULE_CONTEXT *BIDIRULECTXTPTR;

/*
 * BIDIRUN_RULE_FPTR
 *
 * Function type of a UBA rule which operates on
 * a single text chain.
 *
 * This type is used to better abstract the bidi rule
 * dispatch mechanism, to distinguish version-specific
 * operations to extract level runs from the logic
 * which then applies to that run.
 */

typedef int (*BIDIRUN_RULE_FPTR)( BIDIRULECTXTPTR brp );

typedef struct {
  BIDIRUN_RULE_FPTR ruleMethod; /* actual function callback for rule */
  char *ruleLabel;  /* printable label for the function */
  char *ruleNumber; /* W1, W2, ... */
  char *ruleError;
} RULE_TBL_ELEMENT;

typedef RULE_TBL_ELEMENT *RTEPTR;

/*
 * Private Function Declarations
 */

/*
 * Exported from libbidir library
 */

extern int Trace ( U_Int_32 traceValue );
extern void TraceOffAll ( void );

extern int GetFileFormat ( void );
extern void SetFileFormat ( int format );
extern int GetUBAVersion ( void );
extern char* GetUBAVersionStr ( void );
extern void SetUBAVersion ( int version );

extern int convertHexToInt ( U_Int_32 *dest, char *source );
extern int br_Evaluate ( char *data, int *outval );
extern char *copyField ( char *dest, char *src );
extern char *copySubField ( char *dest, char *src );
extern void br_ErrPrint ( char *errString );

extern int br_GetBCFromLabel ( char* src );
extern BIDIPROP br_GetBC ( U_Int_32 c );
extern BPTPROP br_GetBPT ( U_Int_32 c );
extern U_Int_32 br_GetBPB ( U_Int_32 c );
extern int br_InitTable ( int version, char *datapath );
extern int br_UBA ( UBACTXTPTR ctxt );
extern int br_Check ( UBACTXTPTR ctxt );

/*
 * Exported from brinput.c for use in bidiref.c
 */

extern int br_RunStaticTest ( void );
extern int br_RunFileTests ( int version, char *datapath, char *input );

/*
 * Exported from brtable.c for use in brtest.c
 */

extern int br_GetInitDone ( void );

#ifdef  __cplusplus
}
#endif

#endif /* __BIDIREFP_LOADED */

