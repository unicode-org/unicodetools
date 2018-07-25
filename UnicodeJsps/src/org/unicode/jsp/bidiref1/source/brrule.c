/*
**      Unicode Bidirectional Algorithm
**      Reference Implementation
**      Copyright 2018, Unicode, Inc. All rights reserved.
**      Unpublished rights reserved under U.S. copyright laws.
*/

/*
 * brrule.c
 *
 * Module to implement the UBA rules.
 *
 * Exports:
 *	br_UBA
 *	br_Check
 */

/*
 * Change history:
 *
 * 2013-May-31  kenw   Created.
 * 2013-June-05 kenw   Added use of dirtyBit in rules and display.
 * 2013-June-07 kenw   Add canonical equivalence for bracket matching.
 * 2013-June-19 kenw   Add use of br_ErrPrint.
 * 2013-June-27 kenw   Correct error in N0 handling of EN/AN.
 * 2013-June-29 kenw   Correct another error in br_ResolveOnePair.
 * 2014-Feb-18  kenw   Correct return in br_ConstructBidiRunListElement.
 * 2014-Apr-17  kenw   Add use of accelerator flags for Version 2.0.
 * 2015-June-04 kenw   Adjustments to work for versions past UBA63.
 * 2015-June-05 kenw   Rule changes to track UBA80.
 *                     Update error return code handling in main dispatch.
 * 2015-June-15 kenw   Make N0 rule more strictly backward compatible with
 *                     UBA63 implementation.
 * 2015-Dec-04  kenw   Correct off-by-one error in stack full check.
 *                     Also fix bracket limits for output display.
 * 2016-Sep-22  kenw   Suppress security warning on compile.
 * 2018-Jul-22  kenw   Slight update to br_UBA_ResolveTerminators to
 *                     prevent overzealous code integrity checkers from
 *                     complaining about possible read of uninitialized
 *                     local variable.
 *                     Adjust output display to account for SMP characters.
 */

#define _CRT_SECURE_NO_WARNINGS
#include <string.h>
#include <malloc.h>
#include <stdio.h>

#include "bidirefp.h"

/******************************************************/

/*
 * SECTION: Stack declarations.
 */

/*
 * statusStack
 *
 * For simplicity the static stack is allocated here using the larger 
 * MAX_DEPTH value required for UBA63. The actual maximum_depth
 * used by the stack handling is set contingent on the
 * version of UBA being run.
 */

static STATUSSTACKELEMENT statusStack[MAX_DEPTH_63 + 2];

static STACKPTR stackTop;
static STACKPTR stackMax;

static int maximum_depth;

/*
 * bracketStack
 *
 * This is the stack used by the bracket pairing algorithm
 * in Rule N0.
 *
 * For the reference implementation, this stack is just declared
 * statically here. In an optimized implementation, this
 * stack might be handled differently.
 */

static BRACKETSTACKELEMENT bracketStack[MAXPAIRINGDEPTH + 1];

static BRACKETSTACKPTR bracketStackTop;
static BRACKETSTACKPTR bracketStackMax;

/*
 * pairList
 *
 * Also used in the bracket pairing algorithm in
 * Rule N0.
 */

static PAIRINGPTR pairList;

/******************************************************/

/*
 * SECTION: Forward declaration of rule methods.
 */

static int br_UBA_ResolveCombiningMarks ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveEuropeanNumbers ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveAL ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveSeparators ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveTerminators ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveESCSET ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveEN ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolvePairedBrackets ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveNeutralsByContext ( BIDIRULECTXTPTR brcp );
static int br_UBA_ResolveNeutralsByLevel ( BIDIRULECTXTPTR brcp );

static int br_Dummy_Rule ( BIDIRULECTXTPTR brcp );

/*
 * bidi_rules
 *
 * This is the table of rule methods used by br_UBA_RuleDispatch
 *
 * Method-specific diagnostic strings and error messages are also
 * stored here, for convenience and generality.
 */

static RULE_TBL_ELEMENT bidi_rules[UBA_RULE_Last + 1] =
	{
		{ br_UBA_ResolveCombiningMarks, "ResolveCombiningMarks", "W1",
			"resolving combining marks" }, /* UBA_RULE_W1 */
		{ br_UBA_ResolveEuropeanNumbers, "ResolveEuropeanNumbers", "W2", 
			"resolving European numbers" }, /* UBA_RULE_W2 */
		{ br_UBA_ResolveAL, "ResolveAL", "W3", 
			"resolving AL" }, /* UBA_RULE_W3 */
		{ br_UBA_ResolveSeparators, "ResolveSeparators", "W4", 
			"resolving separators" }, /* UBA_RULE_W4 */
		{ br_UBA_ResolveTerminators, "ResolveTerminators", "W5", 
			"resolving terminators" }, /* UBA_RULE_W5 */
		{ br_UBA_ResolveESCSET, "ResolveESCSET", "W6", 
			"resolving ES, CS, ET" },  /* UBA_RULE_W6 */
		{ br_UBA_ResolveEN, "ResolveEN", "W7", 
			"resolving EN" },  /* UBA_RULE_W7 */
		{ br_UBA_ResolvePairedBrackets, "ResolvePairedBrackets", "N0", 
			"resolving paired brackets" },  /* UBA_RULE_N0 */
		{ br_UBA_ResolveNeutralsByContext, "ResolveNeutralsByContext", "N1", 
			"resolving neutrals by context" },  /* UBA_RULE_N1 */
		{ br_UBA_ResolveNeutralsByLevel, "ResolveNeutralsByLevel", "N2", 
			"resolving neutrals by level" },  /* UBA_RULE_N2 */
		{ br_Dummy_Rule, "Dummy", "ZZ", "Err" }
	};

static int br_Dummy_Rule ( BIDIRULECTXTPTR brcp )
{
	return ( 1 );
}

/*
 * SECTION: Miscellaneous property check utilities.
 */

/*
 * br_IsIsolateInitiator
 *
 * Encapsulate the checking for Bidi_Class values of isolate
 * initiator types (LRI, RLI, FSI).
 */
#ifdef NOTDEF
static int br_IsIsolateInitiator ( BIDIPROP bc )
{
	if ( ( bc == BIDI_LRI ) || ( bc == BIDI_RLI ) || ( bc == BIDI_FSI ) )
	{
		return ( 1 );
	}
	else
	{
		return ( 0 );
	}
}
#endif
/*
 * br_IsIsolateControl
 *
 * Encapsulate the checking for Bidi_Class values of isolate
 * initiator types (LRI, RLI, FSI) or PDI.
 */

static int br_IsIsolateControl ( BIDIPROP bc )
{
	if ( ( bc == BIDI_LRI ) || ( bc == BIDI_RLI ) || ( bc == BIDI_FSI ) ||
		( bc == BIDI_PDI ) )
	{
		return ( 1 );
	}
	else
	{
		return ( 0 );
	}
}

/*
 * br_FirstSignificantBC
 *
 * Return the first Bidi_Class value in the run for a non-deleted
 * element of the run (or Bidi_None, if all elements of the run
 * are deleted).
 *
 * This function is used to identify a matching PDI in a discontiguous
 * run for a sequence, when the first element (or elements) of the run is actually
 * a deleted format character. This accounts for matching across such
 * deletions, when brp->first->bc results in the wrong value.
 */

static BIDIPROP br_FirstSignificantBC ( BIDIRUNPTR brp )
{
BIDIUNITPTR bdu;

	bdu = brp->first;
	while ( bdu <= brp->last )
	{
		if ( bdu->level != NOLEVEL )
		{
			return ( bdu->bc );
		}
		bdu++;
	}
	return ( BIDI_None );
}

/******************************************************/

/*
 * SECTION: Display of Debug Output
 */

/*
 * BidiClassLabels
 *
 * Used for converting the enumerated Bidi_Class values to
 * readable, fixed-width labels.
 */

static char *BidiClassLabels[BIDI_MAX] =
	{ "NONE", "UNKN", "   L", "  ON", "   R", "  EN", "  ES", "  ET",
      "  AN", "  CS", "   B", "   S", "  WS", "  AL", " NSM", "  BN",
      " PDF", " LRE", " LRO", " RLE", " RLO", " LRI", " RLI", " FSI",
      " PDI" };

/*
 * BidiClassLabelsTrimmed
 *
 * Like BidiClassLabels, but with the initial spaces trimmed
 * out, for non-justified display.
 */
static char *BidiClassLabelsTrimmed[BIDI_MAX] =
	{ "NONE", "UNKN", "L",   "ON",  "R",   "EN",  "ES",  "ET",
      "AN",   "CS",   "B",   "S",   "WS",  "AL",  "NSM", "BN",
      "PDF",  "LRE",  "LRO", "RLE", "RLO", "LRI", "RLI", "FSI",
      "PDI" };

/*
 * RunFragments
 *
 * Used to store all the string fragments used to produce the
 * display of runs and sequences.
 */

typedef enum {
	RF_LL, RF_LR, RF_RL, RF_RR, RF_LOpen, RF_ROpen, RF_OpenL, RF_OpenR,
	RF_OpenOpen, 
	RF_LInit, RF_RInit, RF_OpenInit, RF_PDIOpen, RF_ISOL, RF_None,
	RF_Last
} RUNFRAGTYPE;

static char *RunFragments[RF_Last] =
	{
		/* Used for both run and sequence displays */
		" <LL>", " <LR>", " <RL>", " <RR>",
		" <L--", " <R--", 
		"---L>", "---R>",
		"-----",
		/* Used only for sequence displays */
		" <L-[", " <R-[",
		"----[", ".]---", ".....", "     "
	};

static void br_PrintRunFragment ( RUNFRAGTYPE rf )
{
	printf ( RunFragments[rf] );
}

/*
 * br_DisplayBlankRun
 *
 * This prints out spaces for each element of a run.
 * It is used to justify the spacing of runs when
 * printing out runs for sequences.
 */

static void br_DisplayBlankRun ( BIDIRUNPTR brp )
{
int i;

	for ( i = 1; i <= brp->len; i++ )
	{
		br_PrintRunFragment ( RF_None );
	}
}

/*
 * br_DisplayIsolateRun
 *
 * This prints out a row of dots for each element of a run.
 * It is used to justify the spacing of runs when
 * printing out runs for sequences.
 */

static void br_DisplayIsolateRun ( BIDIRUNPTR brp )
{
int i;

	for ( i = 1; i <= brp->len; i++ )
	{
		br_PrintRunFragment ( RF_ISOL );
	}
}

/*
 * br_DisplayRunRange
 *
 * This prints out runs for runIDs in
 * the first..last range, according to the fragment
 * type specified.
 */

static void br_DisplayRunRange ( BIDIRUNPTR brp, int first, int last, RUNFRAGTYPE rft )
{
BIDIRUNPTR tbrp;

    if ( first > last )
    {
    	return;
    }

	tbrp = brp;
	while ( tbrp != NULL )
	{
		if ( ( tbrp->runID >= first ) && ( tbrp->runID <= last ) )
		{
			if ( rft == RF_ISOL )
			{
				br_DisplayIsolateRun ( tbrp );
			}
			else
			{
				br_DisplayBlankRun ( tbrp );
			}
		}
		if ( tbrp->runID > last )
		{
			break;
		}
		tbrp = tbrp->next;
	}
}

/*
 * br_DisplayOneRun
 *
 * Display a single run as part of a run list.
 *
 * The sor and eor values are displayed by using an "L" or "R"
 * next to the arrows indicating the start and end of the runs.
 *
 * The seqSyntax paramater is False for printing out just
 * the list of level runs.
 *
 * The seqSyntax parameter is True for printing out level
 * runs as part of the isolating run sequence display, to
 * enhance it to show isolating runs correctly.
 */

static void br_DisplayOneRun ( BIDIRUNPTR brp, int seqSyntax )
{
int i;

	for ( i = 1; i <= brp->len; i++ )
	{
		if ( i == 1 )
		{
			if ( brp->len == 1 )
			{
				if ( brp->sor == BIDI_L )
				{
					if ( seqSyntax && brp->first->bc_isoinit )
					{
						br_PrintRunFragment ( RF_LInit );
					}
					else if ( brp->eor == BIDI_L )
					{
						br_PrintRunFragment ( RF_LL );
					}
					else
					{
						br_PrintRunFragment ( RF_LR );
					}
				}
				else
				{
					if ( seqSyntax && brp->first->bc_isoinit )
					{
						br_PrintRunFragment ( RF_RInit );
					}
					else if ( brp->eor == BIDI_L )
					{
						br_PrintRunFragment ( RF_RL );
					}
					else
					{
						br_PrintRunFragment ( RF_RR );
					}
				}
			}
			else
			{
				if ( seqSyntax && ( br_FirstSignificantBC ( brp ) == BIDI_PDI ) )
				{
					br_PrintRunFragment ( RF_PDIOpen );
				}
				else if ( brp->sor == BIDI_L )
				{
					br_PrintRunFragment ( RF_LOpen );
				}
				else
				{
					br_PrintRunFragment ( RF_ROpen );
				}
			}
		}
		else if ( i == brp->len )
		{
			if ( seqSyntax && brp->last->bc_isoinit )
			{
				br_PrintRunFragment ( RF_OpenInit );
			}
			else if ( brp->eor == BIDI_L )
			{
				br_PrintRunFragment ( RF_OpenL );
			}
			else
			{
				br_PrintRunFragment ( RF_OpenR );
			}
		}
		else
		{
			br_PrintRunFragment ( RF_OpenOpen );
		}
	}
}

/*
 * br_DisplayRunList
 *
 * Dump a list of runs in a single display line, formatted to 
 * complement and align with the listing of levels.
 */
static void br_DisplayRunList ( BIDIRUNPTR brp )
{
BIDIRUNPTR tbrp;

	printf ( "  Runs:       " );
	tbrp = brp;
	while ( tbrp != NULL )
	{
		br_DisplayOneRun ( tbrp, 0 );
		tbrp = tbrp->next;
	}
 	printf ( "\n" );
}

/*
 * br_DisplayOneSeqRunList
 *
 * Process the list of runs associated with the sequence,
 * displaying each in turn.
 *
 * For proper display when a run list consists of more than
 * one run, use a special justification function for any
 * runs *between* the ones displayed, to visually show
 * the enclosed, isolated runs.
 */

static void br_DisplayOneSeqRunList ( BIDIRUNPTR brp, BIDIRUNLISTPTR brlp )
{
BIDIRUNLISTPTR tbrlp;
int currentrunid;
int firstrun;

	tbrlp = brlp;
	/* 
	 * The between displays start having effect the second
	 * time through the loop, if there is a gap between
	 * the previous run displayed and the next run to
	 * display.
	 */
	currentrunid = 0;
	firstrun = 1;
	while ( tbrlp != NULL )
	{
		if ( !firstrun )
		{
			br_DisplayRunRange ( brp, currentrunid, 
				tbrlp->run->runID - 1, RF_ISOL );
		}
		br_DisplayOneRun ( tbrlp->run, 1 );
		firstrun = 0;
		currentrunid = tbrlp->run->runID + 1;
		tbrlp = tbrlp->next;
	}
}

/*
 * br_DisplaySequenceAtLevel
 *
 * Dump a single line of sequences at the specified level.
 *
 * Strategy: To provide appropriate justification of the runs
 * printed at each level, we print a blank run for each
 * run not part of the sequences at this level. To do this,
 * keep a currentrunid value. Each time a sequence run list
 * is displayed, update the currentrunid to the last run id
 * of that sequence run list. That makes it possible to
 * keep track of the runs *between* sequences which need
 * to be displayed as blanks to keep the justification
 * correct.
 */

static void br_DisplaySequenceAtLevel ( UBACTXTPTR ctxt, int level )
{
IRSEQPTR tirp;
int currentrunid;

	printf ( "  Seqs (L=%2d):", level );
	tirp = ctxt->theSequences;
	currentrunid = 1;
	while ( tirp != NULL )
	{
		if ( tirp->level == level )
		{
			/* printf ( "{seq %d}", tirp->seqID ); */
			/*
			 * display blank runs from the currentrunid to one less
			 * than the run id of the start of the runs in the sequence.
			 * If the first run in the sequence is also the very first
			 * run, the last value will be zero, and no
			 * justification is done.
			 */
			br_DisplayRunRange ( ctxt->theRuns, currentrunid, 
				tirp->theRuns->run->runID - 1, RF_None );
			br_DisplayOneSeqRunList ( ctxt->theRuns, tirp->theRuns );
			/*
			 * Set the currentrunid to one more than the run id of the
			 * last run in the sequence.
			 */
			currentrunid = tirp->lastRun->run->runID + 1;
		}
		tirp = tirp->next;
	}
	printf ( "\n" );
}

/*
 * br_HaveSequenceAtLevel
 *
 * A quick check to see if there is *any* sequence defined
 * with this level. This check is used to skip over what would
 * otherwise be empty display lines.
 */

static int br_HaveSequenceAtLevel ( IRSEQPTR irp, int level )
{
IRSEQPTR tirp;

	tirp = irp;
	while ( tirp != NULL )
	{
		if ( tirp->level == level )
		{
			return ( 1 );
		}
		tirp = tirp->next;
	}
	return ( 0 );
}

/*
 * br_DisplaySequenceList
 *
 * Dump a list of sequence in multiple display lines, formatted to 
 * complement and align with the listing of runs.
 *
 * A separate line is printed for the sequences at each embedding
 * level, from lowest to hightest, to better display the hierarchical
 * structure of the levels.
 *
 * The sos and eos values are displayed by using an "L" or "R"
 * next to the arrows indicating the start and end of the sequences.
 *
 * An isolate initiator is indicated with a "[" at the end of a run.
 * A PDI is inidicated with a "]" at the start of a run.
 *
 * If a sequence contains a nested isolating run sequence, that
 * nested sequence is displayed with a row of dots between the
 * "[...............]" brackets that mark its start and end.
 */
static void br_DisplaySequenceList ( UBACTXTPTR ctxt )
{
IRSEQPTR tirp;
#ifdef NOTDEF
BIDIRUNLISTPTR tbrlp;
#endif
int highestlevel;
int lowestlevel;
int levelix;
int linesDisplayed;

	/*
	 * This section is temporary, to display diagnostics
	 * about the sequences until the full, hierarchical
	 * display is worked out.
	 */
#ifdef NOTDEF
	printf ( "  Sequences:  " );
	tirp = ctxt->theSequences;
	while ( tirp != NULL )
	{
		printf ( " %d (runs: ", tirp->seqID );
		tbrlp = tirp->theRuns;
		while ( tbrlp != NULL )
		{
			printf ( "%d", tbrlp->run->runID );
			if ( tbrlp->next != NULL )
			{
				printf (",");
			}
			tbrlp = tbrlp->next;
		}
		printf ( ")" );
		if ( tirp->next != NULL )
		{
			printf (",");
		}
		tirp = tirp->next;
	}
 	printf ( "\n" );
#endif
 	/*
 	 * First scan the list of sequences to find the lowest and
 	 * highest levels.
 	 */
	highestlevel = 0;
	lowestlevel = maximum_depth + 1;
	tirp = ctxt->theSequences;

	while ( tirp != NULL )
	{
		if ( tirp->level > highestlevel )
		{
			highestlevel = tirp->level;
		}
		if ( tirp->level < lowestlevel )
		{
			lowestlevel = tirp->level;
		}
		tirp = tirp->next;
	}

	/*
	 * Next scan the sequence list repeatedly, from lowest to highest
	 * level, displaying a representation of all sequences at that level
	 * on a separate line.
	 *
	 * Make several checks to throttle back display output, to keep
	 * it from becoming unreadable for artificially elaborate test
	 * cases:
	 *
	 * 1. Don't display levels higher than 99.
	 * 2. Don't display more than 10 sequence levels in toto.
	 * 3. Skip over levels which don't actually have sequences.
	 */

	linesDisplayed = 0;
	for ( levelix = lowestlevel; levelix <= highestlevel; levelix++ )
	{
		if ( br_HaveSequenceAtLevel ( ctxt->theSequences, levelix ) )
		{
			br_DisplaySequenceAtLevel ( ctxt, levelix );
			linesDisplayed++;
		}
		if ( linesDisplayed > 10 )
		{
			printf ( "  Sequence display has been truncated at 10 levels.\n" );
			break;
		}
		if ( levelix >= 99 )
		{
			printf ( "  Sequence display has been truncated at level 99.\n" );
			break;
		}
	}
}

/*
 * br_LabelForDirection
 *
 * Provide a readable label for the enumerated direction type.
 */

static const char* br_LabelForDirection ( Paragraph_Direction pdir )
{
	switch ( pdir )
	{
		case Dir_LTR:
			return ( "Dir_LTR" );
		case Dir_RTL:
			return ( "Dir_RTL" );
		case Dir_Auto:
			return ( "Dir_Auto" );
		default:
			return ( "Bad Value" );
	}
}

/*
 * br_DisplayState
 *
 * Dump the complete current state of the context, showing
 * the characters, their Bidi_Class settings and their level
 * settings at the current point in the processing.
 *
 * TBD: Update the line handling for display, so this can automatically
 * deal with longer text input without creating bad wraps from long lines.
 * But it may be better to just leave this as is, and assume that long
 * lines will be handled in the programming editors likely to be
 * used by those examining the output.
 */
static void br_DisplayState ( UBACTXTPTR ctxt )
{
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
int mismatch;
int started;
const char *tmp;

	/* 
	 * Use Trace11 as the master control for this state display.
	 * If not set, just exit.
	 *
	 * TBD: Add more trace flags for finer control over the display
	 */

	if ( !Trace ( Trace11 ) )
	{
		return;
	}

	/*
	 * If Trace14 is set and the dirtyBit is not set, then
	 * exit without printing any display.
	 */

	if ( Trace ( Trace14 ) && !(ctxt->dirtyBit ) )
	{
		return;
	}

	printf ( "Current State: %d\n", ctxt->state );

	endOfText = ctxt->theText + ctxt->textLen;

	if ( ctxt->state <= State_P3Done )
	/*
	 * Don't bother repeating this fixed information after every rule is applied.
	 */
	{
		tmp = br_LabelForDirection ( ctxt->paragraphDirection );
	    printf ( "Paragraph Dir: %d (%s), Paragraph Embed Level: %d, TextLen: %d\n\n",
	    	    ctxt->paragraphDirection, tmp, ctxt->paragraphEmbeddingLevel,
	    	    ctxt->textLen );
	}

	/*
	 * When printing output for the rules that identify runs (and
	 * isolating run sequences for UBA63) also print out index positions
	 * to help with identification of the spans.
	 *
	 * These are printed above the Text (if any) and above the Bidi_Class
	 * values.
	 */
	if ( ( ctxt->state == State_RunsDone )  || ( ctxt->state == State_X10Done ) )
	{
	int ix;

	    printf ( "  Position:   " );
		bdu = ctxt->theText;
		ix = 0;
		while ( bdu < endOfText )
		{
			printf (" %4d", ix );
			ix++;
			bdu++;
		}
		printf ( "\n" );
	} 

    /*
     * Omit printing the text when processing BidiText.txt, because
     * that format has no original text -- only sequences of Bidi_Class
     * values.
     */
    if ( GetFileFormat() == FORMAT_A )
    {
	    printf ( "  Text:       " );
		bdu = ctxt->theText;
		while ( bdu < endOfText )
		{
		/*
		 * Adjust the printing of the code points, so that SMP
		 * code points don't cause the rest of the display to
		 * end up column-deregistered.
		 */
		    if ( bdu->c > 0xFFFF )
		    {
		    	printf ( "%05X", bdu->c );
		    }
		    else
		    {
				printf ( " %04X", bdu->c );
		    }
			bdu++;
		}
		printf ( "\n" );
    }
    printf ( "  Bidi_Class: " );
	bdu = ctxt->theText;
	while ( bdu < endOfText )
	{
		printf (" %s", BidiClassLabels[bdu->bc] );
		bdu++;
	}
 	printf ( "\n" );
	printf ( "  Levels:     " );
	bdu = ctxt->theText;
	while ( bdu < endOfText )
	{
		if ( bdu->level == NOLEVEL )
		{
			printf ( "    x" );
		}
		else
		{
			printf (" %4d", bdu->level );
		}
		bdu++;
	}
 	printf ( "\n" );
 	/*
 	 * If the algorithm is complete and we are in the checking phase,
 	 * add listing of the expected levels. Check for mismatches, and
 	 * print an extra diagnostic line, if found.
 	 */
 	if ( ctxt->state == State_Complete )
 	{
		printf ( "  Exp Levels: " );
		mismatch = 0;
		bdu = ctxt->theText;
		while ( bdu < endOfText )
		{
			if ( bdu->expLevel == NOLEVEL )
			{
				printf ( "    x" );
			}
			else
			{
				printf (" %4d", bdu->expLevel );
			}
			if ( bdu->level != bdu->expLevel )
			{
				mismatch = 1;
			}
			bdu++;
		}
	 	printf ( "\n" );
	 	if ( mismatch )
	 	{
			printf ( "  Mismatches: " );
			bdu = ctxt->theText;
			while ( bdu < endOfText )
			{
				if ( bdu->level != bdu->expLevel )
				{
					printf ( "    ^" );
				}
				else
				{
					printf ( "     " );
				}
				bdu++;
			}
		 	printf ( "\n" );
	 	}
 	}
 	if ( ( ctxt->state >= State_RunsDone ) && ( ctxt->theRuns != NULL ) )
 	{
 		br_DisplayRunList ( ctxt->theRuns );
 	}
	if ( ( ctxt->state == State_X10Done ) && ( ctxt->theSequences != NULL ) )
	{
 		br_DisplaySequenceList ( ctxt );
	}
	printf ( "\n" );
	if ( ctxt->state >= State_L2Done )
	{
		printf ( "  Order:      [" );
		bdu = ctxt->theText;
		started = 0;
		while ( bdu < endOfText )
		{
		/* Skip any "deleted" elements. */
			if ( bdu->order != -1 )
			{
				if ( !started )
				{
					printf ("%d", bdu->order );
					started = 1;
				}
				else
				{
					printf (" %d", bdu->order );
				}
			}
			bdu++;
		}
		printf ( "]\n" );
	}
 	/*
 	 * If the algorithm is complete and we are in the checking phase,
 	 * add listing of the expected order.
 	 */
 	if ( ctxt->state == State_Complete )
 	{
 		if ( ctxt->expOrder != NULL )
 		{
			printf ( "  Exp Order:  [%s]\n", ctxt->expOrder );
 		}
 	}
}

/******************************************************/

/*
 * SECTION: Paragraph Embedding Level Rules: P1 - P3
 */

/*
 * br_UBA_ParagraphEmbeddingLevel
 *
 * This function runs Rules P2 and P3.
 *
 * The paragraph embedding level defaults to zero.
 *
 * Note that this reference implementation assumes that
 * paragraph breaking has already been done, so P1 is
 * basically out of scope. Input is assumed to already
 * consist of single "paragraph" units, as is the true
 * for all the test case data for BidiTest.txt and
 * for BidiCharacterTest.txt.
 *
 * If the directionality is Dir_RTL, that signals an
 * override of rules P2 and P3. Set the paragraph
 * embedding level to 1.
 *
 * If the directionality is Dir_Auto (default), scan
 * the text, and if the *first* character with strong
 * directionality is bc=R or bc=AL, set the paragraph
 * embedding level to 1.
 */

static int br_UBA_ParagraphEmbeddingLevel ( UBACTXTPTR ctxt )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_ParagraphEmbeddingLevel [P2, P3]\n" );
	}

	if ( ctxt->paragraphDirection == Dir_RTL )
	{
		ctxt->paragraphEmbeddingLevel = 1;
	}
	else if ( ctxt->paragraphDirection == Dir_Auto )
	{
		bdu = ctxt->theText;
		endOfText = ctxt->theText + ctxt->textLen;
		while ( bdu < endOfText )
		{
			bc = bdu->bc;
			if ( Trace ( Trace9 ))
			{
				printf ( "Debug: bc=%d (%s)\n", bc, BidiClassLabelsTrimmed[bc] );
			}
			if ( bc == BIDI_L )
			{
				break;
			}
			else if ( ( bc == BIDI_R ) || ( bc == BIDI_AL ) )
			{
				ctxt->paragraphEmbeddingLevel = 1;
				break;
			}
			/* Dropped through without finding a strong type yet. */
			bdu++;
		}
	}
	
	ctxt->dirtyBit = 1;
	ctxt->state = State_P3Done;
	return ( 1 );
}

/*
 * br_HasMatchingPDI
 *
 * Scan forwards from the current pointer, looking for a matching
 * PDI for an isolate initiator.
 *
 * This scan needs to check for multiple isolate sequences, and find the
 * PDI which matches this isolate initiator.
 *
 * The strategy is to create a dumb
 * level counter, starting at 1 and increment or decrement it
 * for each PDI or isolate initiator encountered. A match condition
 * consists of first encounter of a PDI (while scanning
 * forwards) when the dumb level counter is set to zero.
 *
 * Return 1 if a match is found, 0 if no match is found.
 */

static int br_HasMatchingPDI ( BIDIUNITPTR current, BIDIUNITPTR endOfText,
	BIDIUNITPTR *pdiPtr )
{
BIDIPROP bc;
int dumblevelctr = 1;
BIDIUNITPTR bdu = current + 1;

	while ( bdu < endOfText )
	{
	/* Check the Bidi_Class */
		bc = bdu->bc;
		if ( Trace ( Trace9 ) )
		{
			printf ( "Debug: br_HasMatchingPDI bc=%s, dumblevelctr=%d\n", 
				BidiClassLabelsTrimmed[bdu->bc], dumblevelctr );
		}

	/* If we hit a PDI, decrement the level counter */
		if ( bc == BIDI_PDI )
		{
			dumblevelctr--;
	/* 
	 * If the level counter has decremented back to zero, we have a match.
	 * Set a pointer to the PDI we have found.
	 */
			if ( dumblevelctr == 0 )
			{
				*pdiPtr = bdu;
				return ( 1 );
			}
		}
	/* If we hit another isolate initiator, increment the level counter */
		else if ( bdu->bc_isoinit )
		{
			dumblevelctr++;
		}
	/* Increment the unit pointer */
		bdu++;
	}
	/* Fell through without a match. Return 0. */
	return ( 0 );
}

/*
 * br_UBA63_ParagraphEmbeddingLevel
 *
 * This function runs Rules P2 and P3.
 *
 * The paragraph embedding level defaults to zero.
 *
 * If the directionality is Dir_RTL, that signals an
 * override of rules P2 and P3. Set the paragraph
 * embedding level to 1.
 *
 * If the directionality is Dir_Auto (default), scan
 * the text, and if the *first* character with strong
 * directionality is bc=R or bc=AL, set the paragraph
 * embedding level to 1.
 *
 * The difference between this rule for UBA62 and UBA63 is
 * that the UBA63 version needs to ignore any characters
 * between an isolate initiator and a matching PDI.
 */

static int br_UBA63_ParagraphEmbeddingLevel ( UBACTXTPTR ctxt )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
BIDIUNITPTR pdiPtr;
int hasPDIMatch;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA63_ParagraphEmbeddingLevel [P2, P3]\n" );
	}

	if ( ctxt->paragraphDirection == Dir_RTL )
	{
		ctxt->paragraphEmbeddingLevel = 1;
	}
	else if ( ctxt->paragraphDirection == Dir_Auto )
	{
		bdu = ctxt->theText;
		endOfText = ctxt->theText + ctxt->textLen;
		while ( bdu < endOfText )
		{
			bc = bdu->bc;
			if ( Trace ( Trace9 ) )
			{
				printf ( "Debug: bc=%d (%s)\n", bc, BidiClassLabelsTrimmed[bc] );
			}
			if ( bc == BIDI_L )
			{
				break;
			}
			else if ( ( bc == BIDI_R ) || ( bc == BIDI_AL ) )
			{
				ctxt->paragraphEmbeddingLevel = 1;
				break;
			}
			else if ( bdu->bc_isoinit )
			{
				hasPDIMatch = br_HasMatchingPDI ( bdu, endOfText, &pdiPtr );
				if ( hasPDIMatch )
				{
					if ( Trace ( Trace9 ) )
					{
						printf ( "Bingo!\n" );
					}
					/*
					 * Set bdu past the PDI which marks the end of
					 * the isolated sequence.
					 */
					bdu = pdiPtr + 1;
				}
				else
				{
					/* 
					 * If there is no matching PDI, leave
					 * the embedding level at 0 and return.
					 */
					break;
				}
			}
			/* Dropped through without finding a strong type yet. */
			else
			{
				bdu++;
			}
		}
	}

	ctxt->dirtyBit = 1;
	ctxt->state = State_P3Done;
	return ( 1 );
}

/*************************************************************/

/*
 * SECTION: Explicit Embedding Level Rules: X1 - X10
 */

/* Directional Status Stack */

static void br_InitStack ( void )
{
	stackTop = &(statusStack[0]);
	stackMax = &(statusStack[maximum_depth]);
}

/*
 * Version-specific Stack Handling.
 *
 * The UBA62 stack handling only pushes an embedding level and
 * an override status on the stack.
 *
 * The UBA63 stack handling additionally pushes an isolate status
 * on the stack.
 *
 * To simplify the expression of the stack processing for this
 * implementation, while sacrificing some efficiency, the UBA62
 * versus UBA63 distinction is encapsulated in distinct routines
 * to assign values to a STATUSSTACKELEMENT. In this way, the
 * main stack operations can be expressed generically.
 */

/*
 * br_AssembleStackElement_62
 *
 * Assign values to a stack element per UBA 6.2.
 */
static void br_AssembleStackElement_62 ( STACKPTR sptr, int level, 
	D_Override_Status ors )
{
	sptr->embedding_level = level;
	sptr->override_status = ors;
	sptr->isolate_status = 0;
}

/*
 * br_AssembleStackElement_63
 *
 * Assign values to a stack element per UBA 6.3.
 */
static void br_AssembleStackElement_63 ( STACKPTR sptr, int level, 
	D_Override_Status ors, int isos )
{
	sptr->embedding_level = level;
	sptr->override_status = ors;
	sptr->isolate_status = isos;
}

/* 
 * br_PushStack
 *
 * Push an element on the stack. 
 */
static void br_PushStack ( STACKPTR sptr )
{
	/* Check for stack full */
	if ( stackTop < stackMax )
	{
		if ( Trace ( Trace3 ) )
		{
			if ( GetUBAVersion() > UBA62 )
			{
				printf ( "Trace: br_PushStack, level=%d, override status=%d, isolate status=%d\n",
					sptr->embedding_level, sptr->override_status, sptr->isolate_status );
			}
			else
			{
				printf ( "Trace: br_PushStack, level=%d, override status=%d\n",
					sptr->embedding_level, sptr->override_status );
			}
		}
		stackTop++;
		stackTop->embedding_level = sptr->embedding_level;
		stackTop->override_status = sptr->override_status;
		stackTop->isolate_status  = sptr->isolate_status;
	}
	else
	{
		if ( Trace ( Trace3 ) )
		{
			printf ( "Trace: br_PushStack, stack full\n" );
		}
	}
}

/* 
 * br_PopStack
 *
 * Pop an element off the stack.
 */
static void br_PopStack ( STACKPTR sptr )
{
	/* Check for stack empty */
	if ( stackTop > statusStack )
	{
		if ( Trace ( Trace3 ) )
		{
			if ( GetUBAVersion() > UBA62 )
			{
				printf ( "Trace: br_PopStack,  level=%d, override status=%d, isolate status=%d\n",
					stackTop->embedding_level, stackTop->override_status, stackTop->isolate_status );
			}
			else
			{
				printf ( "Trace: br_PopStack,  level=%d, override status=%d\n",
					stackTop->embedding_level, stackTop->override_status );
			}
		}
		sptr->embedding_level = stackTop->embedding_level;
		sptr->override_status = stackTop->override_status;
		sptr->isolate_status  = stackTop->isolate_status;
		stackTop--;
	}
	else
	{
		if ( Trace ( Trace3 ) )
		{
			printf ( "Trace: br_PopStack,  stack empty\n" );
		}
	}
}

/* 
 * br_PeekStack
 *
 * Examine an element on the stack, but don't pop it.
 */
static void br_PeekStack ( STACKPTR sptr )
{
	/* Check for stack empty */
	if ( stackTop > statusStack )
	{
		if ( Trace ( Trace3 ) )
		{
			printf ( "Trace: br_PeekStack, level=%d, override status=%d, isolate status=%d\n",
				stackTop->embedding_level, stackTop->override_status, stackTop->isolate_status );
		}
		sptr->embedding_level = stackTop->embedding_level;
		sptr->override_status = stackTop->override_status;
		sptr->isolate_status  = stackTop->isolate_status;
	}
	else
	{
		if ( Trace ( Trace3 ) )
		{
			printf ( "Trace: br_PeekStack, stack empty\n" );
		}
	}
}

/*
 * br_StackEntryCount
 *
 * Return a count of how many elements are on the stack.
 */

static int br_StackEntryCount ( void )
{
	return ((int)( stackTop - statusStack ));
}

/*
 * Encapsulate the calculation of least greater odd or
 * even embedding levels.
 *
 * These functions return -1 if the resulting odd or even
 * embedding level would not be valid (exceeds the maximum
 * allowable level).
 */

static int br_leastGreaterOddLevel ( int level )
{
int templevel;

	if ( level % 2 == 1 )
	{
		templevel = level + 2;
	}
	else
	{
		templevel = level + 1;
	}
	return ( ( templevel > maximum_depth ) ? -1 : templevel ) ;
}

static int br_leastGreaterEvenLevel ( int level )
{
int templevel;

	if ( level % 2 == 0 )
	{
		templevel = level + 2;
	}
	else
	{
		templevel = level + 1;
	}
	return ( ( templevel > maximum_depth - 1 ) ? -1 : templevel ) ;
}

/*
 * br_HasMatch
 *
 * Scan backwards from the current pointer, looking for a matching
 * LRE, RLE, LRO, or RLO for a PDF.
 *
 * This scan needs to check for multiple embeddings, and find any
 * opening code which matches the PDF.
 *
 * This cannot use the level values in the BIDIUNIT vector, because
 * it is invoked from the code which is trying to determine the
 * embedding levels. Instead, the strategy is to create a dumb
 * level counter, starting at 1 and increment or decrement it
 * for each PDF or start control encountered. A match condition
 * consists of first encounter of a opening code (while scanning
 * backwards) when the dumb level counter is set to zero.
 *
 * Return 1 if a match is found, 0 if no match is found.
 */

static int br_HasMatch ( BIDIUNITPTR current, BIDIUNITPTR start )
{
BIDIPROP bc;
int dumblevelctr = 1;
BIDIUNITPTR bdu = current;

	while ( bdu > start )
	{
	/* Decrement the unit pointer */
		bdu--;
	/* Check the Bidi_Class */
		bc = bdu->bc;
		if ( Trace ( Trace4 ) )
		{
			printf ( "Debug: br_HasMatch bc=%s, dumblevelctr=%d\n", 
				BidiClassLabelsTrimmed[bdu->bc], dumblevelctr );
		}
	/* If we hit another PDF, increment the level counter */
		if ( bc == BIDI_PDF )
		{
			dumblevelctr++;
		}
	/* If we hit an opening code, decrement the level counter */
		else if ( ( bc == BIDI_LRE ) || ( bc == BIDI_RLE ) ||
			      ( bc == BIDI_LRO ) || ( bc == BIDI_RLO ) )
		{
			dumblevelctr--;
	/* If the level counter has decremented back to zero, we have a match. */
			if ( dumblevelctr == 0 )
			{
				return ( 1 );
			}
		}

	}
	/* Fell through without a match. Return 0. */
	return ( 0 );
}

/*
 * br_UBA_ExplicitEmbeddingLevels
 *
 * This function runs Rules X1 through X8.
 *
 * The paragraph embedding level defaults to zero.
 *
 */

static int br_UBA_ExplicitEmbeddingLevels ( UBACTXTPTR ctxt )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
int templevel;
int currentEmbeddingLevel;
int currentOverrideStatus;
STATUSSTACKELEMENT stack_element;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_ExplicitEmbeddingLevels [X1-X8]\n" );
	}

	ctxt->dirtyBit = 0;

/*
 * X1:
 * Set the current embedding level to the paragraph embedding level.
 * Set the current directional override status to neutral.
 *
 * Because rules X2 through X8, which use the stack and refer
 * to the current embedding level and current override status,
 * are handled here in a single pass, these values can be
 * stored in local variables, and do not need to be saved
 * in the context.
 */
    currentEmbeddingLevel = ctxt->paragraphEmbeddingLevel;
    currentOverrideStatus = Override_Neutral;
/*
 * Initialize the stack each time into this function, before
 * running the X2..X8 rules.
 */
 	br_InitStack();

/*
 * X2..X8:
 *
 * Process each character in the input, setting embedding levels
 * and override status.
 */
	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		bc = bdu->bc;
		if ( Trace ( Trace4 ) )
		{
			printf ( "Debug: bc=%d (%s)\n", bc, BidiClassLabelsTrimmed[bc] );
		}
		switch (bc)
		{
		case BIDI_RLE: /* X2 */
			templevel = br_leastGreaterOddLevel ( currentEmbeddingLevel );
			if ( templevel != -1 )
			{
				br_AssembleStackElement_62 ( &stack_element, 
					currentEmbeddingLevel, currentOverrideStatus );
				br_PushStack ( &stack_element );
				currentEmbeddingLevel = templevel;
    			currentOverrideStatus = Override_Neutral;
			}
			break;
		case BIDI_LRE: /* X3 */
			templevel = br_leastGreaterEvenLevel ( currentEmbeddingLevel );
			if ( templevel != -1 )
			{
				br_AssembleStackElement_62 ( &stack_element, 
					currentEmbeddingLevel, currentOverrideStatus );
				br_PushStack ( &stack_element );
				currentEmbeddingLevel = templevel;
    			currentOverrideStatus = Override_Neutral;
			}
			break;
		case BIDI_RLO: /* X4 */
			templevel = br_leastGreaterOddLevel ( currentEmbeddingLevel );
			if ( templevel != -1 )
			{
				br_AssembleStackElement_62 ( &stack_element, 
					currentEmbeddingLevel, currentOverrideStatus );
				br_PushStack ( &stack_element );
				currentEmbeddingLevel = templevel;
    			currentOverrideStatus = Override_RTL;
			}
			break;
		case BIDI_LRO: /* X5 */
			templevel = br_leastGreaterEvenLevel ( currentEmbeddingLevel );
			if ( templevel != -1 )
			{
				br_AssembleStackElement_62 ( &stack_element, 
					currentEmbeddingLevel, currentOverrideStatus );
				br_PushStack ( &stack_element );
				currentEmbeddingLevel = templevel;
    			currentOverrideStatus = Override_LTR;
			}
			break;
		case BIDI_PDF: /* X7 */
			if ( br_HasMatch ( bdu, ctxt->theText ) )
			{
				br_PopStack ( &stack_element );
				currentEmbeddingLevel = stack_element.embedding_level;
				currentOverrideStatus = stack_element.override_status;
			}
			break;
		case BIDI_BN:
			break;
		case BIDI_B:  /* X8 */
			/*
			 * A paragraph break terminates all embedding contexts.
			 * Just set the level back to the paragraph embedding level.
			 * A BIDI_B should only be encountered as the very last element
			 * in a paragraph. If not, the paragraph chunking was not
			 * done correctly.
			 */
			bdu->level = ctxt->paragraphEmbeddingLevel;
			ctxt->dirtyBit = 1;
			break;
		default: /* X6 */
			bdu->level = currentEmbeddingLevel;
			if ( currentOverrideStatus == Override_RTL )
			{
				bdu->bc = BIDI_R;
				bdu->bc_numeric = 0;
			}
			else if ( currentOverrideStatus == Override_LTR )
			{
				bdu->bc = BIDI_L;
				bdu->bc_numeric = 0;
			}
			ctxt->dirtyBit = 1;
		}

		if ( Trace ( Trace4 ) )
		{
			if ( bc != bdu->bc )
			{
				printf ( "Debug: override Bidi_Class bc=%d (%s)\n", 
					bdu->bc, BidiClassLabelsTrimmed[bdu->bc] ) ;
			}
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_X8Done;
	return ( 1 );
}

/*
 * br_DecideParaLevel
 *
 * Run rules P2 and P3 on the range start to endOfText, and return
 * a paragraph level value.
 */

static int br_DecideParaLevel ( BIDIUNITPTR start, BIDIUNITPTR endOfText )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR pdiPtr;
int hasPDIMatch;

	bdu = start;
	while ( bdu < endOfText )
	{
		bc = bdu->bc;
		if ( Trace ( Trace4 ) )
		{
			printf ( "Debug: bc=%d (%s)\n", bc, BidiClassLabelsTrimmed[bc] );
		}
		if ( bc == BIDI_L )
		{
			return ( 0 );
		}
		else if ( ( bc == BIDI_R ) || ( bc == BIDI_AL ) )
		{
			return ( 1 );
		}
		else if ( bdu->bc_isoinit )
		{
			hasPDIMatch = br_HasMatchingPDI ( bdu, endOfText, &pdiPtr );
			if ( hasPDIMatch )
			{
				if ( Trace ( Trace4 ) )
				{
					printf ( "Bingo!\n" );
				}
				/*
				 * Set bdu past the PDI which marks the end of
				 * the isolated sequence.
				 */
				bdu = pdiPtr + 1;
				continue;
			}
			else
			{
				/* 
				 * If there is no matching PDI, return 0.
				 */
				return ( 0 );
			}
		}
		/* Dropped through without finding a strong type yet. */
		bdu++;
	}
	return ( 0 );
}

/*
 * br_UBA63_ExplicitEmbeddingLevels
 *
 * This function runs Rules X1 through X8.
 *
 * The paragraph embedding level defaults to zero.
 *
 * The UBA63 version of these rules is considerably
 * more complex than for UBA62. It takes into account
 * explicit embedding and override levels and *also*
 * handles explicit isolate sequences.
 *
 * Some state information is stored on the top element
 * of the stack, so a PeekStack function which examines
 * those values without popping the stack is required.
 */

static int br_UBA63_ExplicitEmbeddingLevels ( UBACTXTPTR ctxt )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
int templevel;
int overflowIsolateCount;
int overflowEmbeddingCount;
int validIsolateCount;
STATUSSTACKELEMENT stack_element;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA63_ExplicitEmbeddingLevels [X1-X8]\n" );
	}

	ctxt->dirtyBit = 0;
/*
 * X1:
 * Initialize the stack and the other variables.
 *
 * Because rules X2 through X8, which use the stack,
 * are handled here in a single pass, the required values can be
 * stored in local variables, and do not need to be saved
 * in the context.
 */

/*
 * Initialize the stack each time into this function, before
 * running the X2..X8 rules.
 */
 	br_InitStack();

	br_AssembleStackElement_63 ( &stack_element, 
					ctxt->paragraphEmbeddingLevel, Override_Neutral, 0 );

	br_PushStack ( &stack_element );

	overflowIsolateCount = 0;
	overflowEmbeddingCount = 0;
	validIsolateCount = 0;

/*
 * X2..X8:
 *
 * Process each character in the input, setting embedding levels
 * and override status.
 */
	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		bc = bdu->bc;
		if ( Trace ( Trace4 ) )
		{
			printf ( "Debug: bc=%d (%s)\n", bc, BidiClassLabelsTrimmed[bc] );
		}
		switch (bc)
		{
		case BIDI_RLE: /* X2 */
			br_PeekStack ( &stack_element );
			templevel = br_leastGreaterOddLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_Neutral, 0 );
				br_PushStack ( &stack_element );
			}
			else
			{
				if ( overflowIsolateCount == 0 )
				{
					overflowEmbeddingCount++;
				}
			}
			break;
		case BIDI_LRE: /* X3 */
			br_PeekStack ( &stack_element );
			templevel = br_leastGreaterEvenLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_Neutral, 0 );
				br_PushStack ( &stack_element );
			}
			else
			{
				if ( overflowIsolateCount == 0 )
				{
					overflowEmbeddingCount++;
				}
			}
			break;
		case BIDI_RLO: /* X4 */
			br_PeekStack ( &stack_element );
			templevel = br_leastGreaterOddLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_RTL, 0 );
				br_PushStack ( &stack_element );
			}
			else
			{
				if ( overflowIsolateCount == 0 )
				{
					overflowEmbeddingCount++;
				}
			}
			break;
		case BIDI_LRO: /* X5 */
			br_PeekStack ( &stack_element );
			templevel = br_leastGreaterEvenLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_LTR, 0 );
				br_PushStack ( &stack_element );
			}
			else
			{
				if ( overflowIsolateCount == 0 )
				{
					overflowEmbeddingCount++;
				}
			}
			break;
		case BIDI_RLI: /* X5a */
			br_PeekStack ( &stack_element );
			bdu->level = stack_element.embedding_level;
			/*
			 * For UBA80 and later, if the directional override status of the last
			 * entry is not neutral, change the current bc of the RLI explicitly to
			 * either L or R, accordingly.
			 */
			if ( GetUBAVersion() >= UBA80 )
			{
				if ( stack_element.override_status != Override_Neutral )
				{
					bdu->bc = ( stack_element.override_status == Override_LTR ) ? BIDI_L : BIDI_R ;
				}
			}
			ctxt->dirtyBit = 1;
			templevel = br_leastGreaterOddLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				validIsolateCount++;
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_Neutral, 1 );
				br_PushStack ( &stack_element );
			}
			else
			{
				overflowIsolateCount++;
			}
			break;
		case BIDI_LRI: /* X5b */
			br_PeekStack ( &stack_element );
			bdu->level = stack_element.embedding_level;
			/*
			 * For UBA80 and later, if the directional override status of the last
			 * entry is not neutral, change the current bc of the LRI explicitly to
			 * either L or R, accordingly.
			 */
			if ( GetUBAVersion() >= UBA80 )
			{
				if ( stack_element.override_status != Override_Neutral )
				{
					bdu->bc = ( stack_element.override_status == Override_LTR ) ? BIDI_L : BIDI_R ;
				}
			}
			ctxt->dirtyBit = 1;
			templevel = br_leastGreaterEvenLevel ( stack_element.embedding_level );
			if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
			{
				validIsolateCount++;
				br_AssembleStackElement_63 ( &stack_element, 
					templevel, Override_Neutral, 1 );
				br_PushStack ( &stack_element );
			}
			else
			{
				overflowIsolateCount++;
			}
			break;
		case BIDI_FSI: /* X5c This is a complicated mix of X5a/X5b */
			{
			BIDIUNITPTR pdiPtr;
			int hasPDIMatch;
			int tmpParaEmbedLevel;

				/* Check if there is a matching PDI */
				hasPDIMatch = br_HasMatchingPDI ( bdu, endOfText, &pdiPtr );
				if ( hasPDIMatch )
				{
					tmpParaEmbedLevel = br_DecideParaLevel ( bdu + 1, pdiPtr );
				}
				else
				{
					tmpParaEmbedLevel = br_DecideParaLevel ( bdu + 1, endOfText );
				}
				br_PeekStack ( &stack_element );
				bdu->level = stack_element.embedding_level;
				/*
				 * For UBA80 and later, if the directional override status of the last
				 * entry is not neutral, change the current bc of the FSI explicitly to
				 * either L or R, accordingly.
				 */
				if ( GetUBAVersion() >= UBA80 )
				{
					if ( stack_element.override_status != Override_Neutral )
					{
						bdu->bc = ( stack_element.override_status == Override_LTR ) ? BIDI_L : BIDI_R ;
					}
				}
				ctxt->dirtyBit = 1;
				/*
				 * If the calculated paragraph embedding level is 1, treat
				 * this FSI as an RLI. Otherwise, treat it as an LRI.
				 */
				if ( tmpParaEmbedLevel == 1 )
				{
					templevel = br_leastGreaterOddLevel ( stack_element.embedding_level );
				}
				else
				{
					templevel = br_leastGreaterEvenLevel ( stack_element.embedding_level );
				}
				if ( ( templevel != -1 ) && ( overflowIsolateCount == 0 ) && ( overflowEmbeddingCount == 0 ) )
				{
					validIsolateCount++;
					br_AssembleStackElement_63 ( &stack_element, 
						templevel, Override_Neutral, 1 );
					br_PushStack ( &stack_element );
				}
				else
				{
					overflowIsolateCount++;
				}
			}
			break;
		case BIDI_PDI: /* X6a */
			if ( overflowIsolateCount > 0 )
			{
				overflowIsolateCount--;
			}
			else if ( validIsolateCount == 0 )
			{
				/* do nothing */
			}
			else
			{
			int continuepopping;

				overflowEmbeddingCount = 0;
				continuepopping = 1;
				while ( continuepopping )
				{
					br_PeekStack ( &stack_element );
					if ( stack_element.isolate_status == 0 )
					{
						br_PopStack ( &stack_element );
					}
					else
					{
						continuepopping = 0;
					}
				}
				br_PopStack ( &stack_element );
				validIsolateCount--;
			}
			br_PeekStack ( &stack_element );
			bdu->level = stack_element.embedding_level;
			/*
			 * For UBA80 and later, if the directional override status of the last
			 * entry is not neutral, change the current bc of the PDI explicitly to
			 * either L or R, accordingly.
			 */
			if ( GetUBAVersion() >= UBA80 )
			{
				if ( stack_element.override_status != Override_Neutral )
				{
					bdu->bc = ( stack_element.override_status == Override_LTR ) ? BIDI_L : BIDI_R ;
				}
			}
			ctxt->dirtyBit = 1;
			break;
		case BIDI_PDF: /* X7 */
			if ( overflowIsolateCount > 0 )
			{
				/* do nothing */
			}
			else if ( overflowEmbeddingCount > 0 )
			{
				overflowEmbeddingCount--;
			}
			else
			{
				br_PeekStack ( &stack_element );
				if ( stack_element.isolate_status == 0 )
				{
					if ( br_StackEntryCount() >= 2 )
					{
						br_PopStack ( &stack_element );
					}
				}
			}
			break;
		case BIDI_BN:
			break;
		case BIDI_B:  /* X8 */
			/*
			 * A paragraph break terminates all embedding contexts.
			 * Just set the level back to the paragraph embedding level.
			 * A BIDI_B should only be encountered as the very last element
			 * in a paragraph. If not, the paragraph chunking was not
			 * done correctly.
			 */
			bdu->level = ctxt->paragraphEmbeddingLevel;
			ctxt->dirtyBit = 1;
			break;
		default: /* X6 */
			br_PeekStack ( &stack_element );
			bdu->level = stack_element.embedding_level;
			if ( stack_element.override_status == Override_RTL )
			{
				bdu->bc = BIDI_R;
				bdu->bc_numeric = 0;
			}
			else if ( stack_element.override_status == Override_LTR )
			{
				bdu->bc = BIDI_L;
				bdu->bc_numeric = 0;
			}
			ctxt->dirtyBit = 1;
		}

		if ( Trace ( Trace4 ) )
		{
			if ( bc != bdu->bc )
			{
				printf ( "Debug: override Bidi_Class bc=%d (%s)\n", 
					bdu->bc, BidiClassLabelsTrimmed[bdu->bc] ) ;
			}
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_X8Done;
	return ( 1 );
}

/*
 * br_UBA_DeleteFormatCharacters
 *
 * This function runs Rule X9.
 *
 * Characters are not actually deleted. Instead, their level
 * is set to NOLEVEL, which allows ignoring them in later steps.
 *
 * This setting could, of course, be done more efficiently by
 * also setting these levels as part of the processing of X2..X8,
 * but is pulled out separately here to make the impact of
 * X9 clearer for the didactic implementation.
 */

static int br_UBA_DeleteFormatCharacters ( UBACTXTPTR ctxt )
{
BIDIPROP bc;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_DeleteFormatCharacters [X9]\n" );
	}

	ctxt->dirtyBit = 0;

	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		bc = bdu->bc;
		switch (bc)
		{
		case BIDI_RLE: 
		case BIDI_LRE: 
		case BIDI_RLO: 
		case BIDI_LRO: 
		case BIDI_PDF: 
		case BIDI_BN: 
			bdu->level = NOLEVEL;
			ctxt->dirtyBit = 1;
			break;
		}

		/* 
		 * Now reset the order positions.
		 * Use -1 as the position for all "deleted" characters.
		 * This is later checked when actually reordering levels.
		 */
		if ( bdu->level == NOLEVEL )
		{
			bdu->order  = -1;
			bdu->order2 = -1;
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_X9Done;
	return ( 1 );
}

/*
 * br_ConstructBidiRun
 *
 * Allocate and initialize a BIDIRUN. These structs are used to construct
 * a linked list of runs.
 */

static BIDIRUNPTR br_ConstructBidiRun ( int id, int level, BIDIUNITPTR first, 
	              BIDIUNITPTR last, int len )
{
BIDIRUNPTR brp;

	brp = (BIDIRUNPTR)malloc(sizeof(BIDIRUN));
	if (brp == NULL)
	{
		return ( NULL );
	}
	brp->runID = id;
	brp->seqID = 0;
	brp->level = level;
	brp->first = first;
	brp->last = last;
	brp->len = len;
	brp->sor = BIDI_Unknown;
	brp->eor = BIDI_Unknown;
	brp->textChain = NULL;
	brp->next = NULL;

	return ( brp );
}

/*
 * br_ConstructBidiRunListElement
 *
 * Allocate and initialize a BIDIRUNLISTELEMENT.
 */

static BIDIRUNLISTPTR br_ConstructBidiRunListElement ( void )
{
BIDIRUNLISTPTR brlp;

	brlp = (BIDIRUNLISTPTR)malloc(sizeof(BIDIRUNLISTELEMENT));
	if ( brlp == NULL )
	{
		return ( NULL );
	}
	brlp->run = NULL;
	brlp->next = NULL;

	return ( brlp );
}

/*
 * br_ConstructIsolatingRunSequence
 *
 * Allocate and initialize an ISOLATING_RUN_SEQUENCE. These structs are used to construct
 * a linked list of isolating run sequences.
 *
 * Start the list of runs associated with the sequence and assign brp to
 * the head of that list.
 */

static IRSEQPTR br_ConstructIsolatingRunSequence ( int id, BIDIRUNPTR brp )
{
IRSEQPTR irp;
BIDIRUNLISTPTR brlp;

	irp = (IRSEQPTR)malloc(sizeof(ISOLATING_RUN_SEQUENCE));
	if ( irp == NULL )
	{
		return ( NULL );
	}
	brlp = br_ConstructBidiRunListElement();
	if ( brlp == NULL )
	{
		free ( irp );
		return ( NULL );
	}
	brlp->run = brp;

	irp->seqID = id;
	irp->level = brp->level;
	irp->theRuns = brlp;
	irp->lastRun = brlp;
	irp->sos = BIDI_Unknown;
	irp->eos = BIDI_Unknown;
	irp->textChain = NULL;
	irp->next = NULL;

	return ( irp );
}

/*
 * br_AppendBidiRun
 *
 * Append an allocated and initialized BIDIRUN to the linked list of
 * runs in the context.
 *
 * Maintain the lastRun pointer in the context to make this appending
 * easy for the linked list.
 */

static void br_AppendBidiRun ( UBACTXTPTR ctxt, BIDIRUNPTR brp )
{
	if ( ctxt->theRuns == NULL )
	{
		ctxt->theRuns = brp;
	}
	else
	{
		ctxt->lastRun->next = brp;
	}
	ctxt->lastRun = brp;
}

/*
 * br_AppendIsolatingRunSequence
 *
 * Append an allocated and initialized ISOLATING_RUN_SEQUENCE to the linked list of
 * sequences in the context.
 *
 * Maintain the lastSequence pointer in the context to make this appending
 * easy for the linked list.
 */

static void br_AppendIsolatingRunSequence ( UBACTXTPTR ctxt, IRSEQPTR irp )
{
	if ( ctxt->theSequences == NULL )
	{
		ctxt->theSequences = irp;
	}
	else
	{
		ctxt->lastSequence->next = irp;
	}
	ctxt->lastSequence = irp;
}

/*
 * br_AppendBidiRunToSequence
 *
 * Append an allocated and initialized BIDIRUNLISTPTR (which contains
 * a pointer to a BIDIRUN) to the linked list of
 * runs in the isolating run sequence.
 */

static void br_AppendBidiRunToSequence ( IRSEQPTR irp, BIDIRUNLISTPTR brlp )
{

	if ( irp->theRuns == NULL )
	{
		irp->theRuns = brlp;
	}
	else
	{
		irp->lastRun->next = brlp;
	}
	irp->lastRun = brlp;
}

/*
 * br_SpanOneRun
 *
 * Take two pointers to a source BIDIUNIT vector.
 * Extract the first run containing characters all with
 * the same level value (or NOLEVEL).
 *
 * This spanning has to be tweaked a bit to work for
 * UBA63, because an isolate initiator needs to terminate
 * a level run:
 *
 *     R  RLI  PDF    R
 * <R-------[ <R-----R>  <== correct
 *
 * <R-----------R> <RR>  <== incorrect
 *
 * This tweak is a no-op for UBA62, which does not deal
 * with isolate format controls.
 *
 * Returns True if the spanning is done, False otherwise.
 */

static int br_SpanOneRun ( BIDIUNITPTR first, BIDIUNITPTR last,
		BIDIUNITPTR *next, int *newlevel, int *spanlen )
{
int spanlevel;
int level;
int isolateInitiatorFound;
BIDIUNITPTR bdu;

	bdu = first;
	spanlevel = NOLEVEL;
	while ( bdu <= last )
	{
		level = bdu->level;
		isolateInitiatorFound = 0;
		/* skip past "deleted" format characters marked with no level */
		if ( level != NOLEVEL )
		{
			if ( ( GetUBAVersion() > UBA62 ) && bdu->bc_isoinit )
			{
				isolateInitiatorFound = 1;
			}
			/* the first time a valid level is hit, set spanlevel */
			if ( spanlevel == NOLEVEL )
			{
				spanlevel = level;
			}
			/* when level changes, break from the while loop */
			else if ( level != spanlevel )
			{
				break;
			}
		}
		bdu++;
		if ( isolateInitiatorFound )
		{
			/* 
			 * Found an isolate initiator in UBA63 processing.
			 * Terminate the level run here, including the isolate
			 * initiator.
			 */
			break;
		}
	}
	/* 
	 * Set the newlevel. Note that this could be NOLEVEL if the
	 * entire vector consists of BN or bidi embedding controls.
	 */
	*newlevel = spanlevel;
	*spanlen = (int)(bdu - first);
	/* Now check whether we are at the end of the vector */
	if ( bdu > last )
	{
		/* ran off the end of the vector while in a span */
		*next = NULL;
		return ( 1 ); /* spanning done */
	}
	else
	{
		/* Change of level terminated span. Set next pointer. */
		*next = bdu;
		return ( 0 ); /* spanning not done */
	}
}

/*
 * br_UBA_CalculateSorEor
 *
 * Process the run list, calculating sor and eor values for
 * each run. Those values default to BIDI_Unknown when the
 * runs are first identified. But each needs to be set to
 * either L or R.
 */

static void br_UBA_CalculateSorEor ( UBACTXTPTR ctxt )
{
int priorRunLevel;
int nextRunLevel;
int higherLevel;
BIDIRUNPTR brp;

	brp = ctxt->theRuns;
	if ( brp == NULL )
	{
		/* No runs to process */
		return;
	}
	
	/*
	 * Default the priorRunLevel for the first run to
	 * the paragraph embedding level.
	 */
	priorRunLevel = ctxt->paragraphEmbeddingLevel;
	while ( brp != NULL )
	{
		/*
		 * If we have reached the last run, set the nextRunLevel
		 * to the paragraphEmbedding Level, otherwise set it
		 * to the level of the next run.
		 */
		if ( brp->next == NULL )
		{
			nextRunLevel = ctxt->paragraphEmbeddingLevel;
		}
		else
		{
			nextRunLevel = brp->next->level;
		}
		/*
		 * Set sor based on the higher of the priorRunLevel and
		 * the current level.
		 */
		higherLevel = ( priorRunLevel > brp->level ) ? priorRunLevel : brp->level;
		brp->sor = ( higherLevel % 2 == 1 ) ?  BIDI_R : BIDI_L;

		/*
		 * Set eor based on the higher of the nextRunLevel and
		 * the current level.
		 */
		higherLevel = ( nextRunLevel > brp->level ) ? nextRunLevel : brp->level;
		brp->eor = ( higherLevel % 2 == 1 ) ?  BIDI_R : BIDI_L;

		/*
		 * Set priorRunLevel to the current level and
		 * move to the next run in the list.
		 */
		priorRunLevel = brp->level;
		brp = brp->next;
	}
}

/*
 * br_UBA_CalculateSosEos
 *
 * Process the isolating run sequence list, calculating sos and eos values for
 * each sequence. Those values default to BIDI_Unknown when the
 * sequences are first identified. But each needs to be set to
 * either L or R.
 *
 * Strategy: Instead of recalculating all the sos and eos values from
 * scratch, as specified in X10, we can take a shortcut here, because
 * we already have sor and eor values assigned to all the level runs.
 * For any isolating run sequence, simply assign sos to the value of
 * sor for the *first* run in that sequence, and assign eos to the
 * value of eor for the *last* run in that sequence. This provides
 * equivalent values, and is more straightforward to implement and
 * understand.
 *
 * This strategy has to be modified for defective isolating run sequences,
 * where the sequence ends with an LRI/RLI/FSI.
 * In those cases the eot needs to be calculated based on
 * the paragraph embedding level, rather than from the level run.
 * Note that this only applies when an isolating run sequence
 * terminating in an LRI/RLI/FSI but with no matching PDI.
 * An example would be:
 *
 *    R  RLI    R
 * <L-----R> <RR>
 * <L------[          <== eot would be L, not R
 *           <RR>
 *
 */

static void br_UBA_CalculateSosEos ( UBACTXTPTR ctxt )
{
int nextRunLevel;
int higherLevel;
IRSEQPTR tirp;

	tirp = ctxt->theSequences;
	while ( tirp != NULL )
	{
		/*
		 * First inherit the sos and eos values from the
		 * first and last runs in the sequence.
		 */
		tirp->sos = tirp->theRuns->run->sor;
		tirp->eos = tirp->lastRun->run->eor;
		/*
		 * Next adjust for the special case when an isolating
		 * run sequence terminates in an unmatched isolate
		 * initiator.
		 */
		if ( tirp->lastRun->run->last->bc_isoinit )
		{
			nextRunLevel = ctxt->paragraphEmbeddingLevel;
			higherLevel = ( nextRunLevel > tirp->level ) ? nextRunLevel : tirp->level;
			tirp->eos = ( higherLevel % 2 == 1 ) ?  BIDI_R : BIDI_L;
		}
		tirp = tirp->next;
	}
}

/*
 * br_UBA_AddTextChainsForRuns
 *
 * Walk through a run list, allocating, initializing and
 * linking in a text chain for each run.
 */
static int br_UBA_AddTextChainsForRuns ( BIDIRUNPTR brp )
{
int len;
BIDIRUNPTR tbrp;
BIDIUNITPTR bup;
BIDIUNITPTR *tcp;
BIDIUNITPTR *tcp2;

	if ( brp == NULL )
	{
		/* No runs to process */
		return ( 1 );
	}
	tbrp = brp;
	while ( tbrp != NULL )
	{
		/* 
		 * Each run has a len defined already. Use that value
		 * to allocate a text chain.
		 */
		len = tbrp->len;
		tcp = (BIDIUNITPTR *)malloc(len * sizeof ( BIDIUNITPTR ) );
		if ( tcp == NULL )
		{
			/* 
			 * Let br_DropContext do any cleanup
			 * needed for already allocated arrays.
			 */
			return ( 0 );
		}
		/* Copy BIDIUNIT pointers for the level run into the text chain. */
		for ( bup = tbrp->first, tcp2=tcp; bup <= tbrp->last; bup++, tcp2++ )
		{
			*tcp2 = bup;
		}
		/* Attach the initialized text chain to the bidi run. */
		tbrp->textChain = tcp;

		tbrp = tbrp->next;
	}
	return ( 1 );
}

/*
 * br_UBA_AddTextChainsForSequences
 *
 * Walk through a sequence list, allocating, initializing and
 * linking in a text chain for each isolating run sequence.
 */
static int br_UBA_AddTextChainsForSequences ( IRSEQPTR irp )
{
int len;
IRSEQPTR tirp;
BIDIRUNPTR brp;
BIDIUNITPTR bup;
BIDIUNITPTR *tcp;
BIDIUNITPTR *tcp2;
BIDIRUNLISTPTR brlp;

	if ( irp == NULL )
	{
		/* No sequences to process */
		return ( 1 );
	}
	tirp = irp;
	while ( tirp != NULL )
	{
		/* 
		 * An isolating run sequence consists of a sequence of
		 * runs, which may be discontiguous. To find the length
		 * of text chain to allocate, we first need to traverse
		 * the run list, accumulating the lengths of the runs.
		 */
		len = 0;
		brlp = tirp->theRuns;
		while ( brlp != NULL )
		{
			len += brlp->run->len;
			brlp = brlp->next;
		}
		/*
		 * Write the length value back into the sequence, to store
		 * the length of the calculated text chain.
		 */
		tirp->len = len;
		if ( len == 0 )
		{
			printf ( "Error: sequence %d associated will null runs.\n", tirp->seqID );
			return ( 0 );
		}
		tcp = (BIDIUNITPTR *)malloc(len * sizeof ( BIDIUNITPTR ) );
		if ( tcp == NULL )
		{
			/* 
			 * Let br_DropContext do any cleanup
			 * needed for already allocated arrays.
			 */
			return ( 0 );
		}
		if ( Trace ( Trace5 ) )
		{
			printf ( "Allocated text chain: len=%d\n", len );
		}

		/*
		 * Copy BIDIUNIT pointers for the isolating run sequence into the
		 * text chain.
		 *
		 * This differs from the initialization for the level runs, because
		 * we have to read sequentially through each level run and
		 * append the BIDIUNIT pointers to the allocated array.
		 *
		 * First initialize the tcp2 pointer to the allocated text chain.
		 */
		tcp2 = tcp;
		/*
		 * Process the run list in order.
		 */
		brlp = tirp->theRuns;
		while ( brlp != NULL )
		{
			brp = brlp->run;
			/* Append this run to the text chain. */
			for ( bup = brp->first; bup <= brp->last; bup++, tcp2++ )
			{
				*tcp2 = bup;
			}
			brlp = brlp->next;
		}

		/* Attach the initialized text chain to the bidi run. */
		tirp->textChain = tcp;

		tirp = tirp->next;
	}
	return ( 1 );
}

/*
 * br_UBA_IdentifyRuns
 *
 * This function runs Rule X10.
 *
 * X10 is treated with a separate function.
 * It logically occurs after the completion of the application
 * of rules X2-X8 sequentionally through the input string
 * and after X9 has identified and tagged any NOLEVEL values.
 * All levels have to be set before the level runs can be
 * accurately identified.
 *
 * Instead of marking up the BIDIUNIT vector with more
 * character-by-character information, the runs are best
 * handled by a separate structure. A linked list of runs is hung
 * off the UBACONTEXT struct, with each run containing pointers
 * to its start and end BIDIUNITs. This list can then be
 * traversed by the subsequent rules which operate on a
 * run-by-run basis.
 *
 * Note that this code presupposes that the input vector is
 * at least one unit long. Tests for a zero-length vector
 * should occur before hitting this function.
 *
 * It is possible that an input vector may consist *entirely*
 * of units set to NOLEVEL. This would happen if all the
 * units were bc=BN or bidi embedding/override controls,
 * which would have been "deleted" by this point from the
 * input by setting their levels to NOLEVEL. In that case
 * the input vector will have no level spans at all.
 */

static int br_UBA_IdentifyRuns ( UBACTXTPTR ctxt )
{
int rc;
BIDIUNITPTR bdu;     /* point to start of span */
BIDIUNITPTR bduend;  /* point to last BIDIUNIT in vector */ 
BIDIUNITPTR bdunext;
BIDIRUNPTR brp;
int spanid;
int spanningDone;
int spanlevel;
int spanlen;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_IdentifyRuns [X10]\n" );
	}

	ctxt->dirtyBit = 0;

    /* Initialize for looping to extract spans. */

    spanid = 1;
    spanningDone = 0;

	bdu = ctxt->theText;
	bduend = bdu + ( ctxt->textLen - 1 );

	while ( !spanningDone )
	{
		spanningDone = br_SpanOneRun ( bdu, bduend, &bdunext, &spanlevel, &spanlen );

		if ( Trace ( Trace5 ) )
		{
			printf ( "Spanned run id=%d, level=%d, pos=%d, len=%d\n", spanid,
				spanlevel, ( bdu - ctxt->theText ), spanlen );
		}

		/*
		 * Process the extracted span. If the spanlevel was
		 * set to NOLEVEL, skip this step, as there is no
		 * span to process.
		 *
		 * The minimum length
		 * of a span should be 1, in which case the start of
		 * the span and the end of the span point at the same
		 * BIDIUNIT.
		 */

		if ( spanlevel != NOLEVEL ) 
		{
			if ( spanningDone )
			{
				brp = br_ConstructBidiRun ( spanid, spanlevel, 
					bdu, bduend, spanlen );				
			}
			else
			{
				brp = br_ConstructBidiRun ( spanid, spanlevel, 
					bdu, bdunext - 1, spanlen );				
			}
			if ( brp == NULL )
			{
				br_ErrPrint ( "Error in allocation of bidi run.\n");
				return ( 0 );
			}
			/* 
			 * If we have a valid run, append it to the run list
			 * in the context.
			 */
			br_AppendBidiRun ( ctxt, brp );
		}

		/* If that was the last span, exit the while loop. */

		if ( !spanningDone )
		{
		/* Set bdu to the next span start, increment the span id and come around. */
			bdu = bdunext;
			spanid++;
		}
	}

	if ( Trace ( Trace5 ) )
	{
		if ( spanid == 1 )
		{
			printf ( "1 run identified\n" );
		}
		else
		{
			printf ( "%d runs identified\n", spanid );
		}
	}

	/*
	 * Add text chains for each run, for uniform rule application.
	 */

	rc = br_UBA_AddTextChainsForRuns ( ctxt->theRuns );

	if ( rc != 1 )
	{
		return ( rc );
	}

	/*
	 * Now that we have identified the runs, calculate the sor and eor
	 * values for each run.
	 */
	br_UBA_CalculateSorEor ( ctxt );

	/*
	 * This rule always has an impact, so just always set the dirtyBit
	 * on exit.
	 */
	ctxt->dirtyBit = 1;

	if ( GetUBAVersion() > UBA62 )
	{
		ctxt->state = State_RunsDone;
	}
	else
	{
		ctxt->state = State_X10Done;
	}
	return ( 1 );
}

/*
 * br_UBA_IdentifyIsolatingRunSequences
 *
 * This function applies only to UBA63. Once the embedding
 * levels are identified, UBA63 requires further processing
 * to assign each of the level runs to an isolating run sequence.
 *
 * Each level run must be uniquely assigned to exactly one
 * isolating run sequence. Each isolating run sequence must
 * have at least one level run, but may have more.
 *
 * The exact details on how to match up isolating run sequences
 * with level runs are specified in BD13.
 *
 * The strategy taken here is to scan the level runs in order.
 *
 * If a level run is not yet assigned to an isolating run sequence,
 * its seqID will be zero. Create a new isolating run sequence
 * and add this level run to it.
 *
 * If the last BIDIUNIT of *this* level run is an isolate
 * initiator (LRI/RLI/FSI), then scan ahead in the list of
 * level runs seeking the next level run which meets the
 * following criteria:
 *   1. seqID = 0 (not yet assigned to an isolating run sequence)
 *   2. its level matches the level we are processing
 *   3. the first BIDIUNIT is a PDI
 * If all those conditions are met, assign that next level run
 * to this isolating run sequence (set its seqID, and append to
 * the list).
 *
 * Repeat until we hit a level run that doesn't terminate with
 * an isolate initiator or we hit the end of the list of level
 * runs.
 *
 * That terminates the definition of the isolating run sequence
 * we are working on. Append it to the list of isolating run
 * sequences in the UBACONTEXT.
 *
 * Then advance to the next level run which has not yet been
 * assigned to an isolating run sequence and repeat the process.
 *
 * Continue until all level runs have been assigned to an
 * isolating run sequence.
 */

static int br_UBA_IdentifyIsolatingRunSequences ( UBACTXTPTR ctxt )
{
int rc;
int seqid;
IRSEQPTR irp;
BIDIRUNPTR brp;
BIDIRUNPTR brp2;
int savelevel;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_IdentifyIsolatingRunSequences [X10]\n" );
	}

	ctxt->dirtyBit = 0;

	rc = 1;
	seqid = 0;

	brp = ctxt->theRuns;

	while ( brp != NULL )
	{
		/*
		 * Skip past any run which already has a seqID assigned
		 * to it. Only process runs with seqID == 0.
		 */
		if ( brp->seqID == 0 )
		{
			seqid++;
			irp = br_ConstructIsolatingRunSequence ( seqid, brp );
			if ( irp == NULL )
			{
				br_ErrPrint ( "Error in allocation of isolating run sequence.\n");
				return ( 0 );
			}
			br_AppendIsolatingRunSequence ( ctxt, irp );
			brp->seqID = seqid;
			/*
			 * Next check whether this run ends in an isolate initiator.
			 * If so, scan ahead looking for the run with the matching PDI.
			 */
			if ( brp->last->bc_isoinit )
			{
				if ( Trace ( Trace5 ))
				{
					printf ( "Debug: found trailing isolate initiator\n");
				}
				/*
				 * Use a temporary brp2 run pointer for this scan, so
				 * the outer loop resumes correctly from where it left
				 * off in the main scan through the runs.
				 */
				brp2 = brp;
				savelevel = brp->level;
				while ( brp2->next != NULL )
				{
					brp2 = brp2->next;
					if ( Trace ( Trace5 ) )
					{
						printf ( "Debug: runID=%d, seqID=%d, level=%d, first->bc=%d\n", brp2->runID,
							brp2->seqID, brp2->level, br_FirstSignificantBC ( brp2 ) );
					}
					if ( ( brp2->seqID == 0 ) && ( brp2->level == savelevel ) &&
						 ( br_FirstSignificantBC ( brp2 ) == BIDI_PDI ) )
					{
					BIDIRUNLISTPTR brlp;
					/*
					 * We matched the criteria for adding this run to the
					 * sequence. Construct a BIDIRUNLISTELEMENT and append
					 * it to the sequence.
					 */
						brlp = br_ConstructBidiRunListElement();
						if ( brlp == NULL )
						{
							br_ErrPrint ( "Error in allocation of isolating run sequence.\n");
							/* Let the br_DropContext do the cleanup. Less messy. */
							return ( 0 );
						}
						/* Set the seq ID of the run to the seq ID of this sequence. */
						brp2->seqID = seqid;
						brlp->run = brp2;
						/* Append it to the seqeunce. */
						br_AppendBidiRunToSequence ( irp, brlp );
						if ( Trace ( Trace5 ) )
						{
							printf ( "Appended run id=%d to sequence id=%d\n", 
								brp2->runID, seqid );
						}
						/*
						 * Check is the last unit in *this* run is also
						 * an isolate initiator. If not, we are done with
						 * this sequence. If so, come around and scan for
						 * another run with a matching PDI.
						 */
						if ( !( brp2->last->bc_isoinit ) )
						{
							break;
						}
					}
				}
			}

			if ( Trace ( Trace5 ) )
			{
				printf ( "Scanned sequence id=%d, level=%d\n", seqid,
					irp->level );
			}
		}
		/* Advance to the next run */
		brp = brp->next;
	}

	if ( Trace ( Trace5 ) )
	{
		if ( seqid == 1 )
		{
			printf ( "1 sequence identified.\n" );
		}
		else
		{
			printf ( "%d sequences identified.\n", seqid );
		}
	}

	/*
	 * Add text chains for each sequence, for uniform rule application.
	 */

	rc = br_UBA_AddTextChainsForSequences ( ctxt->theSequences );

	/*
	 * Now that we have identified the sequences, calculate the sos and eos
	 * values for each sequence.
	 */
	br_UBA_CalculateSosEos ( ctxt );

	/*
	 * This rule always has an impact, so just always set the dirtyBit
	 * on exit.
	 */
	ctxt->dirtyBit = 1;
	ctxt->state = State_X10Done;
	return ( 1 );
}

/*******************************************************************/

/*
 * SECTION: Resolving Weak Types: Rules W1-W7
 *
 * This section runs the resolving weak type rules of the UBA.
 *
 * In an optimized implementation, these resolutions would probably
 * be combined into fewer passes, but for clarity, this reference
 * implementation does a distinct pass for each rule, dealing with
 * different types of resolution. This also makes it easier to
 * see the separate resolutions as they occur in the display status,
 * if desired.
 *
 * Each rule processes a single text chain.
 *
 * The br_UBA_RuleDispatch handles the dispatch of rules and
 * decides whether they are applying to a list of runs (UBA62)
 * or to a list of isolating run sequences (UBA63).
 */

/*
 * br_IsStrongType
 *
 * Encapsulate the checking for Bidi_Class values of strong
 * type (R, L, AL).
 */
#ifdef NOTDEF
static int br_IsStrongType ( BIDIPROP bc )
{
	if ( ( bc == BIDI_L ) || ( bc == BIDI_R ) || ( bc == BIDI_AL ) )
	{
		return ( 1 );
	}
	else
	{
		return ( 0 );
	}
}
#endif
/*
 * br_IsNeutralType
 *
 * Encapsulate the checking for Bidi_Class values of neutral
 * type (B, S, WS, ON).
 *
 * Note that BIDI_B is kept at the end of each "paragraph" to
 * be run through the algorithm, so it *can* occur as the very
 * last element of the paragraph and has to be checked here.
 *
 * To keep the expression of the rules fairly simple, the
 * extension of "neutral" to "NI" in UBA63, including all
 * the isolate format controls, is implemented in this rule
 * with a test on UBA version.
 *
 * Note that optimized implementations can speed up this kind
 * of checking by keeping precomputed boolean or bit arrays indexed by
 * Bidi_Class values and returning these kinds of True/False
 * queries by a single array lookup instead of using chains of
 * individual Bidi_Class equality tests.
 */

static int br_IsNeutralType ( BIDIPROP bc )
{
	if ( ( bc == BIDI_ON ) || ( bc == BIDI_WS ) || ( bc == BIDI_S ) || ( bc == BIDI_B ) )
	{
		return ( 1 );
	}
	else if ( ( GetUBAVersion() > UBA62 ) && ( br_IsIsolateControl ( bc ) ) )
	{
		return ( 1 );
	}
	else
	{
		return ( 0 );
	}
}

/*
 * br_IsPriorContext
 *
 * Scan backwards in a text chain, checking if the first prior character matches
 * the bc value passed in. Skip over any "deleted" controls, which
 * have NOLEVEL.
 */ 

static int br_IsPriorContext ( BIDIUNITPTR *buppfirst, BIDIUNITPTR *bupp, BIDIPROP bc )
{
BIDIUNITPTR *tbupp;

	if ( bupp == buppfirst )
	{
		return ( 0 );
	}
	tbupp = bupp - 1;
	while ( tbupp >= buppfirst )
	{
		if ( (*tbupp)->bc == bc )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp--;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( 0 );
}

/*
 * br_IsFollowingContext
 *
 * Scan forwards in a text chain, checking if the first subsequent character matches
 * the bc value passed in. Skip over any "deleted" controls, which
 * have NOLEVEL.
 */ 

static int br_IsFollowingContext ( BIDIUNITPTR *bupplast, BIDIUNITPTR *bupp, BIDIPROP bc )
{
BIDIUNITPTR *tbupp;

	if ( bupp == bupplast )
	{
		return ( 0 );
	}
	tbupp = bupp + 1;
	while ( tbupp <= bupplast )
	{
		if ( (*tbupp)->bc == bc )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp++;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( 0 );
}

/*
 * br_UBA_ResolveCombiningMarks
 *
 * This is the method for Rule W1.
 *
 * Resolve combining marks for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For characters of bc=NSM, change the Bidi_Class
 * value to that of the preceding character. Formatting characters
 * (Bidi_Class RLE, LRE, RLO, LRO, PDF) and boundary neutral (Bidi_Class BN)
 * are skipped over in this calculation, because they have been
 * "deleted" by Rule X9.
 *
 * If a bc=NSM character occurs at the start of a text chain, it is given
 * the Bidi_Class of sot (either R or L).
 */

static int br_UBA_ResolveCombiningMarks ( BIDIRULECTXTPTR brcp )
{
int priorbc;
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppend;
int dirtyBit = 0;

	/*
	 * Default the priorbc to the sot value passed in.
	 */
	priorbc = brcp->sot;
	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	bupp = brcp->textChain;
	buppend = bupp + brcp->len;
	while ( bupp < buppend )
	{
		if ( (*bupp)->bc == BIDI_NSM )
		/* Reset any NSM to the Bidi_Class stored in priorbc. */
		{
			(*bupp)->bc = priorbc;
			if ( ( priorbc == BIDI_AN ) || ( priorbc == BIDI_EN ) )
			{
				(*bupp)->bc_numeric = 1;
			}
			dirtyBit = 1;
		}
		/* For a "deleted" BIDIUNIT, do nothing. */
		else if ( (*bupp)->level != NOLEVEL )
		{
		/* For all other Bidi_Class, set priorbc to the current value. */
			priorbc = (*bupp)->bc;
		}
		bupp++;
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveEuropeanNumbers
 *
 * This is the method for Rule W2.
 *
 * Resolve European numbers for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For characters of bc=EN, scan back to find the first
 * character of strong type (or sot). If the strong type is bc=AL,
 * change the Bidi_Class EN to AN. Formatting characters
 * (Bidi_Class RLE, LRE, RLO, LRO, PDF) and boundary neutral (Bidi_Class BN)
 * are skipped over in this calculation, because they have been
 * "deleted" by Rule X9.
 */

static int br_UBA_ResolveEuropeanNumbers ( BIDIRULECTXTPTR brcp )
{
int firststrongbc;
BIDIUNITPTR *bupp;
BIDIUNITPTR *bupp2;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;
	/*
	 * Process the text chain in reverse from last to first BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = bupplast; bupp >= buppfirst; bupp-- )
	{
		if ( (*bupp)->bc == BIDI_EN )
		/* For any EN found, scan back in the run to find the first strong type */
		{
			/* Default firststrongbc to sot */
			firststrongbc = brcp->sot;
			if ( bupp > buppfirst )
			{
				for ( bupp2 = bupp - 1; bupp2 >= buppfirst; bupp2-- )
				{
					if ( ( (*bupp2)->bc == BIDI_L ) || ( (*bupp2)->bc == BIDI_R ) || ( (*bupp2)->bc == BIDI_AL ) )
					{
						firststrongbc = (*bupp2)->bc;
						break;
					}
				}
			}
			/*
			 * Check if the first strong type is AL. If so
			 * reset this EN to AN. This change does not affect
			 * the bc_numeric flag.
			 */
			if ( firststrongbc == BIDI_AL )
			{
				(*bupp)->bc = BIDI_AN;
				dirtyBit = 1;
			}
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveAL
 *
 * This is the method for Rule W3.
 *
 * Resolve Bidi_Class=AL for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For characters of bc=AL, change the Bidi_Class
 * value to R.
 */

static int br_UBA_ResolveAL ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;
	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = buppfirst; bupp <= bupplast; bupp++ )
	{
		if ( (*bupp)->bc == BIDI_AL )
		/* Reset any AL to the Bidi_Class R. */
		{
			(*bupp)->bc = BIDI_R;
			dirtyBit = 1;
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveSeparators
 *
 * This is the method for Rule W4.
 *
 * Resolve Bidi_Class=ES and CS for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class.
 *
 * For characters of bc=ES, check if they are *between* EN.
 * If so, change their Bidi_Class to EN.
 *
 * For characters of bc=CS, check if they are *between* EN
 * or between AN. If so, change their Bidi_Class to match.
 *
 * Update the bc_numeric flag for any ES or CS changed.
 */

static int br_UBA_ResolveSeparators ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;
	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = buppfirst; bupp <= bupplast; bupp++ )
	{
		if ( (*bupp)->bc == BIDI_ES )
		/* Check to see if ES is in context EN ES EN */
		{
			if ( br_IsPriorContext ( buppfirst, bupp, BIDI_EN ) &&
				br_IsFollowingContext ( bupplast, bupp, BIDI_EN ) )
			{
				(*bupp)->bc = BIDI_EN;
				(*bupp)->bc_numeric = 1;
				dirtyBit = 1;
			}
		}
		else if ( (*bupp)->bc == BIDI_CS )
		/* Check to see if CS is in context EN CS EN  or AN CS AN */
		{
			if ( br_IsPriorContext ( buppfirst, bupp, BIDI_EN ) &&
				br_IsFollowingContext ( bupplast, bupp, BIDI_EN ) )
			{
				(*bupp)->bc = BIDI_EN;
				(*bupp)->bc_numeric = 1;
				dirtyBit = 1;
			}
			else if ( br_IsPriorContext ( buppfirst, bupp, BIDI_AN ) &&
				br_IsFollowingContext ( bupplast, bupp, BIDI_AN ) )
			{
				(*bupp)->bc = BIDI_AN;
				(*bupp)->bc_numeric = 1;
				dirtyBit = 1;
			}
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveTerminators
 *
 * This is the method for Rule W5.
 *
 * Resolve Bidi_Class=ET for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class.
 *
 * For characters of bc=ET, check if they are *next to* EN.
 * If so, change their Bidi_Class to EN. This includes
 * ET on either side of EN, so the context on both sides
 * needs to be checked.
 *
 * Because this rule applies to indefinite sequences of ET,
 * and because the context which triggers any change is
 * adjacency to EN, the strategy taken here is to seek for
 * EN first. If found, scan backwards, changing any eligible
 * ET to EN. Then scan forwards, changing any eligible ET
 * to EN. Then continue the search from the point of the
 * last ET changed (if any).
 * 
 * Update the bc_numeric flag for any ET changed.
 */

static int br_UBA_ResolveTerminators ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *bupp2;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;

	/*
	 * Process the run from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	bupp = buppfirst;
	while ( bupp <= bupplast )
	{
		if ( (*bupp)->bc == BIDI_EN )
		/* Check to see if there are any adjacent ET */
		{
			/* First scan left for ET, skipping any NOLEVEL characters */
			if ( bupp > buppfirst )
			{
				for ( bupp2 = bupp - 1; bupp2 >= buppfirst; bupp2-- )
				{
					if ( (*bupp2)->bc == BIDI_ET )
					{
						(*bupp2)->bc = BIDI_EN;
						(*bupp2)->bc_numeric = 1;
						dirtyBit = 1;
					}
					else if ( (*bupp2)->level != NOLEVEL )
					{
						break;
					}	
				}
			}
			/* Next scan right for ET, skipping any NOLEVEL characters */
			if ( bupp < bupplast )
			{
				for ( bupp2 = bupp + 1; bupp2 <= bupplast; bupp2++ )
				{
					if ( (*bupp2)->bc == BIDI_ET )
					{
						(*bupp2)->bc = BIDI_EN;
						(*bupp2)->bc_numeric = 1;
						dirtyBit = 1;
					}
					else if ( (*bupp2)->level != NOLEVEL )
					{
						break;
					}	
				}
		/*
		 * If we scanned ahead, reset bupp to bupp2, to prevent 
		 * reprocessing characters
		 * that have already been checked and/or changed.
		 */
				bupp = bupp2;
			}
			else
		/*
		 * Otherwise just increment bupp and come around.
		 */
			{
				bupp++;
			}
		}
		else
		{
		/* Increment bupp and come around. */
			bupp++;
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveESCSET
 *
 * This is the method for Rule W6.
 *
 * Resolve remaining Bidi_Class=ES, CS, or ET for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For characters of bc=ES, bc=CS, or bc=ET, change 
 * the Bidi_Class value to ON. This resolves any remaining
 * separators or terminators which were not already processed
 * by Rules W4 and W5.
 */

static int br_UBA_ResolveESCSET ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;

	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = buppfirst; bupp <= bupplast; bupp++ )
	{
		if ( ( (*bupp)->bc == BIDI_ES ) || ( (*bupp)->bc == BIDI_CS ) ||
			( (*bupp)->bc == BIDI_ET ) )
		/* Reset any ES CS ET to the Bidi_Class ON. */
		{
			(*bupp)->bc = BIDI_ON;
			dirtyBit = 1;
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveEN
 *
 * This is the method for Rule W7.
 *
 * Resolve Bidi_Class=EN for a single level text chain.
 *
 * Process the text chain in reverse order. For each character in the text chain, examine its
 * Bidi_Class. For characters of bc=EN, scan back to find the first strong
 * directional type. If that type is L, change the Bidi_Class
 * value of the number to L.
 */

static int br_UBA_ResolveEN ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *bupp2;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
BIDIPROP bc;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;
	/*
	 * Process the text chain from last to first BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = bupplast; bupp >= buppfirst; bupp-- )
	{
		if ( (*bupp)->bc == BIDI_EN )
		/* Scan back to find the first strong type,
		 * R, L, or sot.
		 */
		{
			bc = BIDI_None;
			if ( bupp == buppfirst )
			{
				bc = brcp->sot;
			}
			else
			{
				bupp2 = bupp - 1;
				while ( bupp2 >= buppfirst )
				{
					bc = (*bupp2)->bc;
					if ( ( bc == BIDI_L ) || ( bc == BIDI_R ) )
					{
						break;
					}
					if ( bupp2 == buppfirst )
					{
						bc = brcp->sot;
						break;
					}
					bupp2--;
				}
			}
			/*
			 * If the first strong type is L, reset the
			 * bc of the number to L. Update the bc_numeric 
			 * flag accordingly.
			 */
			if ( bc == BIDI_L )
			{
				(*bupp)->bc = BIDI_L;
				(*bupp)->bc_numeric = 0;
				dirtyBit = 1;
			}
		}
	}

	return ( dirtyBit );
}

/*******************************************************************/

/*
 * SECTION: Resolving Neutral Types: Rules N0-N2
 *
 * This section runs the resolving neutral type rules of the UBA.
 *
 * Each rule processes a text chain from start to finish.
 */

/*
 * Context scanning utility functions.
 *
 * These are partly shared by both Rule N0 and Rule N1.
 *
 * These functions differ from
 * those used for resolving weak types, in that they skip by any
 * sequence of neutral types encountered, looking for the first
 * strong type, instead of checking only the immediately adjacent
 * context.
 *
 * If the routines hit the edge of a run, they return true or
 * false based on the type of sot or eot for that run.
 *
 * Rather than collapsing these six scanning functions into a
 * single context checking function, they are unrolled here into
 * six separate functions, to make each one's processing clearer.
 */

/*
 * br_IsPriorContextL
 *
 * Scan backwards in a text chain, checking if the first non-neutral character is an "L" type.
 * Skip over any "deleted" controls, which have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsPriorContextL ( BIDIUNITPTR *buppfirst, BIDIUNITPTR *bupp, BIDIPROP sot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == buppfirst )
	{
		return ( ( sot == BIDI_L ) ? 1 : 0 );
	}
	tbupp = bupp - 1;
	while ( tbupp >= buppfirst )
	{
		if ( (*tbupp)->bc == BIDI_L )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp--;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp--;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( sot == BIDI_L ) ? 1 : 0 );
}

/*
 * br_IsFollowingContextL
 *
 * Scan forwards in a text chain, checking if the first non-neutral character is an "L" type.
 * Skip over any "deleted" controls, which have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsFollowingContextL ( BIDIUNITPTR *bupplast, BIDIUNITPTR *bupp, BIDIPROP eot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == bupplast )
	{
		return ( ( eot == BIDI_L ) ? 1 : 0 );
	}
	tbupp = bupp + 1;
	while ( tbupp <= bupplast )
	{
		if ( (*tbupp)->bc == BIDI_L )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp++;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp++;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( eot == BIDI_L ) ? 1 : 0 );
}

/*
 * br_IsPriorContextR
 *
 * Used by Rule N0.
 *
 * Scan backwards in a text chain, checking if the first non-neutral character is an "R" type.
 * Skip over any "deleted" controls, which have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsPriorContextR ( BIDIUNITPTR *buppfirst, BIDIUNITPTR *bupp, BIDIPROP sot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == buppfirst )
	{
		return ( ( sot == BIDI_R ) ? 1 : 0 );
	}
	tbupp = bupp - 1;
	while ( tbupp >= buppfirst )
	{
		if ( (*tbupp)->bc == BIDI_R )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp--;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp--;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( sot == BIDI_R ) ? 1 : 0 );
}

/*
 * br_IsFollowingContextR
 *
 * Used by Rule N0.
 *
 * Scan forwards in a text chain, checking if the first non-neutral character is an "R" type.
 * Skip over any "deleted" controls, which have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsFollowingContextR ( BIDIUNITPTR *bupplast, BIDIUNITPTR *bupp, BIDIPROP eot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == bupplast )
	{
		return ( ( eot == BIDI_R ) ? 1 : 0 );
	}
	tbupp = bupp + 1;
	while ( tbupp <= bupplast )
	{
		if ( (*tbupp)->bc == BIDI_R )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp++;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp++;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( eot == BIDI_R ) ? 1 : 0 );
}

/*
 * br_IsPriorContextRANEN
 *
 * Used by Rule N1.
 *
 * Scan backwards in a text chain, checking if the first non-neutral character is an "R" type.
 * (BIDI_R, BIDI_AN, BIDI_EN) Skip over any "deleted" controls, which
 * have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsPriorContextRANEN ( BIDIUNITPTR *buppfirst, BIDIUNITPTR *bupp, BIDIPROP sot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == buppfirst )
	{
		return ( ( sot == BIDI_R ) ? 1 : 0 );
	}
	tbupp = bupp - 1;
	while ( tbupp >= buppfirst )
	{
		if ( ( (*tbupp)->bc == BIDI_R ) || ( (*tbupp)->bc_numeric ) )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp--;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp--;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( sot == BIDI_R ) ? 1 : 0 );
}

/*
 * br_IsFollowingContextRANEN
 *
 * Used by Rule N1.
 *
 * Scan forwards in a text chain, checking if the first non-neutral character is an "R" type.
 * (BIDI_R, BIDI_AN, BIDI_EN) Skip over any "deleted" controls, which
 * have NOLEVEL, as well as any neutral types.
 */ 

static int br_IsFollowingContextRANEN ( BIDIUNITPTR *bupplast, BIDIUNITPTR *bupp, BIDIPROP eot )
{
BIDIUNITPTR *tbupp;

	if ( bupp == bupplast )
	{
		return ( ( eot == BIDI_R ) ? 1 : 0 );
	}
	tbupp = bupp + 1;
	while ( tbupp <= bupplast )
	{
		if ( ( (*tbupp)->bc == BIDI_R ) || ( (*tbupp)->bc_numeric ) )
		{
			return ( 1 );
		}
		else if ( (*tbupp)->level == NOLEVEL )
		{
			tbupp++;
		}
		else if ( br_IsNeutralType ( (*tbupp)->bc ) )
		{
			tbupp++;
		}
		else
		{
			return ( 0 );
		}
	}
	return ( ( eot == BIDI_R ) ? 1 : 0 );
}

/* 
 * Bracket Pairing Stack
 *
 * The bracket stack is specific to Rule N0. 
 */

static void br_InitBracketStack ( void )
{
	bracketStackTop = &(bracketStack[0]);
	bracketStackMax = &(bracketStack[MAXPAIRINGDEPTH]);
}

/* 
 * br_PushBracketStack
 *
 * Push an element on the stack. 
 *
 * For UBA63 (and UBA70), the behavior for a stack overflow
 * here was implementation defined.
 *
 * Starting with UBA80, a stack overflow must terminate
 * processing of bracket pairs for the immediate isolating
 * run. To implement this, br_PushBracketStack now
 * returns a value:
 *   1  Stack had room, push was successful.
 *   0  Stack was full, push failed.
 */
static int br_PushBracketStack ( BRACKETSTACKPTR sptr )
{
	/* Check for stack full */
	if ( bracketStackTop < bracketStackMax )
	{
		if ( Trace ( Trace6 ) )
		{
			printf ( "Trace: br_PushBracketStack, bracket=%04X, pos=%d\n",
				sptr->bracket, sptr->pos );
		}
		bracketStackTop++;
		bracketStackTop->bracket = sptr->bracket;
		bracketStackTop->pos = sptr->pos;
		return ( 1 );
	}
	else
	{
		if ( Trace ( Trace6 ) )
		{
			printf ( "Trace: br_PushBracketStack, stack full\n" );
		}
		return ( 0 );
	}
}

/* 
 * br_PopBracketStack
 *
 * Pop n elements off the stack.
 * This pop does not recover element data. It just
 * moves the stack pointer.
 *
 * If n is greater than the stack depth, just empty the stack.
 */
static void br_PopBracketStack ( int n )
{
int stackDepth;

	stackDepth = (int)(bracketStackTop - bracketStack);
	if ( stackDepth > n )
	{
		bracketStackTop -= n;
	}
	else
	{
		bracketStackTop = bracketStack;
	}
	if ( Trace ( Trace6 ) )
	{
		printf ( "Trace: br_PopBracketStack,  #elements=%d\n", n );
	}
}

/* 
 * br_PeekBracketStack
 *
 * Examine an element at depth n on the stack, but don't pop it.
 * A depth of 1 is defined as the top of the stack.
 *
 * Return 1 for peek o.k.
 * Return 0 for stack empty or peek past bottom.
 */
static int br_PeekBracketStack ( BRACKETSTACKPTR sptr, int depth )
{
BRACKETSTACKPTR tsptr;

	tsptr = bracketStackTop - depth + 1;
	if ( Trace ( Trace6 ) )
	{
		printf ( "Trace: br_PeekBracketStack, stack=%p, top=%p, tsptr=%p\n", bracketStack,
			bracketStackTop, tsptr );
	}

	/* Check for stack empty */
	if ( tsptr > bracketStack )
	{
		if ( Trace ( Trace6 ) )
		{
			printf ( "Trace: br_PeekBracketStack, bracket=%04X, pos=%d\n",
				tsptr->bracket, tsptr->pos );
		}
		sptr->bracket = tsptr->bracket;
		sptr->pos = tsptr->pos;
		return ( 1 );
	}
	else
	{
		return ( 0 );
	}
}

/*
 * pairingList
 *
 * The bracket pair list is specific to Rule N0.
 */

/*
 * br_ConstructPair
 *
 * Allocate and initiate a PAIRINGELEMENT
 */

static PAIRINGPTR br_ConstructPair ( int pos1, int pos2 )
{
PAIRINGPTR pair;

	pair = (PAIRINGPTR)malloc(sizeof(PAIRINGELEMENT));
	if ( pair == NULL )
	{
		return ( NULL );
	}
	pair->openingpos = pos1;
	pair->closingpos = pos2;
	pair->next = NULL;
	return ( pair );
}

/*
 * br_AppendToPairList
 *
 * Append a pair to the pairList.
 */

static void br_AppendToPairList ( PAIRINGPTR pair )
{
PAIRINGPTR tpair;

	if ( pairList == NULL )
	{
		pairList = pair;
	}
	else
	{
		tpair = pairList;
		while ( tpair->next != NULL )
		{
			tpair = tpair->next;
		}
		tpair->next = pair;
	}
}

/*
 * br_DropPairList
 *
 * Clean up the allocations in the pairList when done.
 * Also reset the static pairList to NULL.
 */

static void br_DropPairList ( void )
{
PAIRINGPTR tpair;
PAIRINGPTR tpair2;

	tpair = pairList;
	while ( tpair != NULL )
	{
		tpair2 = tpair->next;
		free ( tpair );
		tpair = tpair2;
	}
	pairList = NULL;
}

/*
 * br_SeekOpeningBracketMatch
 *
 * Seek an opening bracket pair for the closing bracket
 * passed in.
 *
 * This is a stack based search.
 * Start with the top element in the stack and search
 * downwards until we either find a match or reach the
 * bottom of the stack.
 *
 * If we find a match, construct and append the bracket
 * pair to the pairList. Then pop the stack for all the
 * levels down to the level where we found the match.
 * (This approach is designed to discard pairs that
 * are not cleanly nested.)
 *
 * If we search all the way to the bottom of the stack
 * without finding a match, just return without changing
 * state. This represents a closing bracket with no
 * opening bracket to match it. Just discard and move on.
 */

static int br_SeekOpeningBracketMatch ( U_Int_32 closingcp, int pos )
{
int rc;
int depth;
PAIRINGPTR pair;
BRACKETSTACKELEMENT bracketData;

	depth = 1;
	while ( depth <= MAXPAIRINGDEPTH )
	{
		rc = br_PeekBracketStack ( &bracketData, depth );
		if ( rc == 0 )
		{
			/* 
			 * Either the bracket stack is empty or the value of
			 * depth exceeds the current depth of the stack.
			 * Return no match.
			 */
			if ( Trace ( Trace6 ) )
			{
				printf ( "Debug: br_PeekBracketStack rc=0, depth=%d\n", depth );
			}
			return ( 0 );
		}
		/*
		 * The basic test is for the closingcp equal to the bpb value
		 * stored in the bracketData. But to account for the canonical
		 * equivalences for U+2329 and U+232A, tack on extra checks here
		 * for the asymmetrical matches. This hard-coded check avoids
		 * having to require full normalization of all the bracket code
		 * points before checking. It is highly unlikely that additional
		 * canonical singletons for bracket pairs will be added to future
		 * versions of the UCD.
		 */
		if ( ( bracketData.bracket == closingcp ) ||
			 ( ( bracketData.bracket == 0x232A ) && ( closingcp == 0x3009 ) ) ||
			 ( ( bracketData.bracket == 0x3009 ) && ( closingcp == 0x232A ) ) )
		{
			/*
			 * This is a match. Construct a pair and append
			 * it to the pair list.
			 */
			pair = br_ConstructPair ( bracketData.pos, pos );
			if ( pair == NULL )
			{
				/* clean up any list allocation already done */
				br_DropPairList();
				return ( 0 );
			}
			else
			{
				if ( Trace ( Trace7 ) )
				{
					printf ( "Appended pair: opening pos %d, closing pos %d\n",
						pair->openingpos, pair->closingpos );
				}
				br_AppendToPairList ( pair );
				/* pop through the stack to this depth */
				br_PopBracketStack ( depth );
				return ( 1 );
			}
		}
		depth++;
	}

	/* Not reached, but return no match */
	return ( 0 );
}

/*
 * br_DisplayPairList
 *
 * For debugging the list sorting, print out a display
 * of the contents of the pair list.
 */

static void br_DisplayPairList ( PAIRINGPTR theList )
{
PAIRINGPTR pair;
int loopcounter;

	printf ( "Pair list: ");
	pair = theList;
	loopcounter = 0;
	while ( pair != NULL )
	{
		if ( loopcounter > MAXPAIRINGDEPTH )
		{
			/* 
			 * This should not occur, but bail out here,
			 * to avoid problems in list processing
			 * during development.
			 */
			br_ErrPrint ( "Error: Loop limit exceeded in br_DisplayPairList\n" );
			return;
		}
		printf ( " {%d,%d}", pair->openingpos, pair->closingpos );
		pair = pair->next;
		loopcounter++;
	}
	printf ( "\n" );
}

/*
 * br_InsertPairInList
 *
 * Insert into a pair list in sorted order by the openingpos value
 * of the pair.
 *
 * Always returns a pointer to the head of the list.
 */

static PAIRINGPTR br_InsertPairInList ( PAIRINGPTR theList, PAIRINGPTR pair )
{
PAIRINGPTR plp;
PAIRINGPTR plp2;

	/*
	 * If the list is empty, set pair->next to NULL and return pair
	 * as the head of the list.
	 */
	if ( theList == NULL )
	{
		/* Create the head of the list */
		if ( Trace ( Trace7 ) )
		{
			printf ( "Create head\n" );
		}
		pair->next = NULL;
		return ( pair );
	}
	plp = theList;
	/*
	 * To insert at the head of the list, set pair->next to plp
	 * and return pair as the head of the list.
	 */
	if ( pair->openingpos < plp->openingpos )
	{
		/* Insert at the head of the list */
		if ( Trace ( Trace7 ) )
		{
			printf ( "Insert at head\n" );
		}
		pair->next = plp;
		return ( pair );
	}
	else
	{
	/*
	 * Scan down the list searching for an insertion point.
	 *
	 * The exist always return theList unchanged as the head
	 * of the list.
	 */
		while ( plp != NULL )
		{
			plp2 = plp->next;
			if ( plp2 == NULL )
			{
				/* Append at the end of the list */
				if ( Trace ( Trace7 ) )
				{
					printf ( "Append at end\n" );
				}
				pair->next = NULL;
				plp->next = pair;
				return ( theList );
			}
			else if ( pair->openingpos < plp2->openingpos )
			{
				/* Insert at this point in the list */
				plp->next = pair;
				pair->next = plp2;
				if ( Trace ( Trace7 ) )
				{
					printf ( "Insert in middle\n" );
				}
				return ( theList );
			}

			plp = plp2;
			if ( Trace ( Trace7 ) )
			{
				printf ( "Seeking insertion point...\n" );
			}
		}

	}
	/*
	 * Not reached
	 */
	return ( theList );
}

/* 
 * br_SortPairList
 *
 * Because of the way the stack
 * processing works, the pairs may not be in the best order
 * in the pair list for further processing. Sort them
 * by position order of the opening bracket.
 *
 * Strategy: Nothing fancy here. These sort pair lists
 * will typically be fairly short, so this reference
 * implementation doesn't worry about seeking sort
 * optimization schemes. A temporary pair list is
 * constructed. The existing pair list is traversed
 * once and moved into the temporary pair list by
 * a list insertion sort. Then the pairList head
 * is reset to the head of this sorted list.
 *
 * This scheme isn't the fastest possible sorting, but
 * is saves doing any reallocation of the pairs in
 * the list, simply rehooking them in sorted order
 * in a new list.
 */

static void br_SortPairList ( void )
{
PAIRINGPTR tempPairList;
PAIRINGPTR plp;
PAIRINGPTR plp2;
int loopcounter;

    if ( Trace ( Trace7 ) )
    {
		printf ( "Trace: Entering br_SortPairList\n" );
		br_DisplayPairList ( pairList );
    }
	tempPairList = NULL;
	plp = pairList;
	loopcounter = 0;
	if ( Trace ( Trace7 ) )
	{
		printf ("loopcounter:sort action\n");
	}
	while ( plp != NULL )
	{
		if ( loopcounter > MAXPAIRINGDEPTH )
		{
			/* 
			 * Should not occur, but do a safety check
			 * to prevent runaway processing during
			 * development.
			 */
			br_ErrPrint ( "Error: Loop limit exceeded in br_SortPairList\n" );
			return;
		}
		loopcounter++;
		if ( Trace ( Trace7 ) )
		{
			printf ("%d:", loopcounter);
		}
		/* Save the next pointer in the list */
		plp2 = plp->next;
		/* Insert plp into the new list */
		tempPairList = br_InsertPairInList ( tempPairList, plp );
		/* Advance plp to the saved next pointer */
		plp = plp2;
	}

	/* Reset pairList to the sorted tempPairList */

	pairList = tempPairList;

    if ( Trace ( Trace7 ))
    {
		printf ( "Trace: Exiting br_SortPairList\n" );
		br_DisplayPairList ( pairList );
    }

	return;
}

/*
* br_SetBracketPairBC
*
* Set the Bidi_Class of a bracket pair, based on the
* direction determined by the N0 rule processing in
* br_ResolveOnePair().
*
* The direction passed in will either be BIDI_R or BIDI_L.
*
* This setting is abstracted in a function here, rather than
* simply being done inline in br_ResolveOnePair, because of
* an edge case added to rule N0 as of UBA80. For UBA63 (and
* UBA70), no special handling of combining marks following
* either of the brackets is done. However, starting with UBA80,
* there is an edge case fix-up done which echoes the processing
* of rule W1. The text run needs to be scanned to find any
* combining marks (orig_bc=NSM) following a bracket which has
* its Bidi_Class changed by N0. Then those combining marks
* can again be adjusted to match the Bidi_Class of the
* bracket they apply to. This is an odd edge case, as combining
* marks do not typically occur with brackets, but the UBA80
* specification is now explicit about requiring this fix-up
* to be done.
*/

static void br_SetBracketPairBC ( BIDIUNITPTR *buppopening, 
	BIDIUNITPTR *buppclosing, BIDIUNITPTR *bupplast, BIDIPROP direction )
{
BIDIUNITPTR *bupp;

	(*buppopening)->bc = direction;
	(*buppclosing)->bc = direction;
	if ( GetUBAVersion() >= UBA80 )
	/*
	 * Here is the tricky part.
	 *
	 * First scan from the opening bracket for any subsequent
	 * character whose *original* Bidi_Class was NSM, and set
	 * the current bc for it to direction also, to match the bracket.
	 * Break out of the loop at the first character with any other
	 * original Bidi_Class, so that this change only impacts
	 * actual combining mark sequences.
	 *
	 * Then repeat the process for the matching closing bracket.
	 *
	 * The processing for the opening bracket is bounded to the
	 * right by the position of the matching closing bracket.
	 * The processing for the closing bracket is bounded to the
	 * right by the end of the text run.
	 */
	{
		for ( bupp = buppopening + 1; bupp <= buppclosing - 1; bupp++ )
		{
			if ( (*bupp)->orig_bc == BIDI_NSM )
			{
				(*bupp)->bc = direction;
			}
			else
			{
				break;
			}
		}
		for ( bupp = buppclosing + 1; bupp <= bupplast; bupp++ )
		{
			if ( (*bupp)->orig_bc == BIDI_NSM )
			{
				(*bupp)->bc = direction;
			}
			else
			{
				break;
			}			
		}
	}
}

/*
 * br_ResolveOnePair
 *
 * Resolve the embedding levels of one pair of matched brackets.
 *
 * This determination is based on the embedding direction.
 * See BD3 in the UBA specification.
 *
 * If embedding level is even, embedding direction = L.
 * If embedding level is odd,  embedding direction = R.
 */

static void br_ResolveOnePair ( BIDIRULECTXTPTR brcp, int firstpos, int lastpos )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
BIDIUNITPTR *buppopening;
BIDIUNITPTR *buppclosing;
BIDIPROP embeddingdirection;
BIDIPROP oppositedirection;
BIDIPROP tempbc;
int strongtypefound;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;
	buppopening = buppfirst + firstpos;
	buppclosing = buppfirst + lastpos;

	/*
	 * First establish the embedding direction, based on the embedding
	 * level passed in as a paramter with the context for this text chain.
	 */

	embeddingdirection = ( brcp->level % 2 == 1 ) ? BIDI_R : BIDI_L ;
	oppositedirection  = ( embeddingdirection == BIDI_L ) ? BIDI_R : BIDI_L ;
	strongtypefound = 0;

	/*
	 * Next check for a strong type (R or L) between firstpos and lastpos,
	 * i.e., one between the matched brackets. If a strong type is found
	 * which matches the embedding direction, then set the type of both
	 * brackets to match the embedding direction, too.
	 */

	if ( firstpos < lastpos - 1 )
	{
		for ( bupp = buppopening + 1; bupp <= buppclosing - 1; bupp++ )
		{
			/*
			 * For the purposes of this direction checking, any EN or AN
			 * which have not been reset by the Weak rules count as
			 * BIDI_R. Set up a temporary bc variable based on mapping
			 * any EN or AN value to BIDI_R.
			 */
			tempbc = ( (((*bupp)->bc) == BIDI_R )  || 
			           ((*bupp)->bc_numeric) ) ? BIDI_R :
                       (((*bupp)->bc) == BIDI_L ) ? BIDI_L : BIDI_None ;
			if ( tempbc == embeddingdirection )
			{
				if ( Trace ( Trace7 ) )
				{
					printf ("Debug: Strong direction e between brackets\n");
				}
				/* N0 Step b, */
				br_SetBracketPairBC ( buppopening, buppclosing, bupplast, embeddingdirection );
				return;
			}
			else if ( tempbc == oppositedirection )
			{
				strongtypefound = 1;
			} 
		}
	}

	/*
	 * Next branch on whether we found a strong type opposite the embedding
	 * direction between the brackets or not.
	 */

	if ( strongtypefound )
	{
		if ( Trace ( Trace7 ) )
		{
			printf ("Debug: Strong direction o between brackets\n");
		}
		/*
		 * First attempt to resolve direction by checking the prior context for
		 * a strong type matching the opposite direction. N0 Step c1.
		 */
		if  ( ( ( oppositedirection == BIDI_L ) && 
			    ( br_IsPriorContextL ( buppfirst, buppopening, brcp->sot ) ) ) ||
			  ( ( oppositedirection == BIDI_R ) && 
			  	( br_IsPriorContextRANEN ( buppfirst, buppopening, brcp->sot ) ) ) )
		{
			br_SetBracketPairBC ( buppopening, buppclosing, bupplast, oppositedirection );
		}
		/*
		 * Next attempt to resolve direction by checking the following context for
		 * a strong type matching the opposite direction. Former N0 Step c2a. Removed.
		 */
#ifdef NOTDEF
		else if ( ( ( oppositedirection == BIDI_L ) && 
			        ( br_IsFollowingContextL ( bupplast, buppclosing, brcp->eot ) ) ) ||
			      ( ( oppositedirection == BIDI_R ) && 
			      	( br_IsFollowingContextRANEN ( bupplast, buppclosing, brcp->eot ) ) ))
		{
			br_SetBracketPairBC ( buppopening, buppclosing, bupplast, oppositedirection );
		}
#endif
		else
		{
		/*
		 * No strong type matching the oppositedirection was found either
		 * before or after these brackets in this text chain. Resolve the
		 * brackets based on the embedding direction. N0 Step c2.
		 */
			br_SetBracketPairBC ( buppopening, buppclosing, bupplast, embeddingdirection );
		}
	}
	else
	{
		/* 
		 * No strong type was found between the brackets. Leave
		 * the brackets with unresolved direction.
		 */
		if ( Trace ( Trace7 ) )
		{
			printf ("Debug: No strong direction between brackets\n");
		}
	}
}

/*
 * br_ResolvePairEmbeddingLevels
 *
 * Scan through the pair list, resolving the embedding
 * levels of each pair of matched brackets.
 */

static void br_ResolvePairEmbeddingLevels ( BIDIRULECTXTPTR brcp )
{
PAIRINGPTR plp;

	/*
	 * Scan through the pair list and resolve each pair in order.
	 */
	plp = pairList;
	while ( plp != NULL )
	{
		/*
		 * Now for each pair, we have the first and last position
		 * of the substring in this isolating run sequence
		 * enclosed by those brackets (inclusive
		 * of the brackets). Resolve that individual pair.
		 */
		br_ResolveOnePair ( brcp, plp->openingpos, plp->closingpos );
		plp = plp->next;
	}
	return;
}

/*
 * br_UBA_ResolvePairedBrackets
 *
 * This is the method for Rule N0. (New in UBA63)
 *
 * Resolve paired brackets for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For any character with the bpt value open or close,
 * scan its context seeking a matching paired bracket. If found,
 * resolve the type of both brackets to match the embedding
 * direction.
 *
 * For UBA63 (and unchanged in UBA70), the error handling for
 * a stack overflow was unspecified for this rule.
 *
 * Starting with UBA80, the exact stack size is specified (63),
 * and the specification declares that if a stack overflow
 * condition is encountered, the BD16 processing for this
 * particular isolating run ceases immediately. This condition
 * does not treated as a fatal error, however, so the rule
 * should not return an error code here, which would stop
 * all processing for *all* runs of the input string.
 *
 * For clarity in this reference implementation, this same
 * behavior is now also used when running UBA63 (or UBA70)
 * rules.
 */

static int br_UBA_ResolvePairedBrackets ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int pos;
int rc;
int testONisNotRequired;
BRACKETSTACKELEMENT bracketData;

	/*
	 * Initialize the bracket stack and the pair list.
	 */

	br_InitBracketStack();
	pairList = NULL;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;

	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Paired_Bracket_Type of each character.
	 *
	 * BD16 examples used 1-based indexing, but this implementation
	 * used 0-based indexing, for consistency with other processing
	 * of text chains.
	 *
	 * UBA80 provided a clarification that the testing for bracket
	 * pairs only applies to brackets whose current Bidi_Class is
	 * still BIDI_ON at this point in the algorithm. This testing
	 * is now done. It can affect outcomes for complicated mixes
	 * of explicit embedding controls and bracket pairs, but does
	 * not seem to have ever been tested in conformance test cases
	 * for earlier implementations. In this bidiref implementation,
	 * this check is not assumed to apply to UBA63 (and UBA70),
	 * in order to maintain backward compatibility with the bidiref
	 * implementation for those versions. By the UBA80 rules,
	 * any bracket that has been forced to R or L already
	 * by the resolution of explicit embedding (see X6) is simply
	 * passed over for bracket pair matching.
	 */
	testONisNotRequired = GetUBAVersion() < UBA80;
	for ( bupp = buppfirst, pos = 0; bupp <= bupplast; bupp++, pos++ )
	{
		if ( ( (*bupp)->bpt != BPT_None ) && 
			 ( (*bupp)->bc == BIDI_ON ) || testONisNotRequired )
		{
			if ( (*bupp)->bpt == BPT_O )
			{
				/* Process an opening bracket. Push on the stack. */
				bracketData.bracket = (*bupp)->bpb;
				bracketData.pos = pos;
				rc = br_PushBracketStack ( &bracketData );
				if ( rc == 0 )
				/* 
				 * Stack overflow. Stop processing this text chain. 
				 * Return 0 to indicate no change was made.
				 */
				{
					return ( 0 );
				}
			}
			else
			{
				/* 
				 * Process a closing bracket.
				 * br_SeekOpeningBracketMatch handles the search
				 * through the stack and the pairslist building if
				 * any match is found.
				 */
				rc = br_SeekOpeningBracketMatch ( (*bupp)->c, pos );
				if ( ( rc == 1 ) && ( Trace ( Trace7 ) ) )
				{
					printf ( "Matched bracket\n" );
				}
			}
		}
	}

	if ( pairList == NULL )
	{
		/* 
		 * The pairList pointer will still be NULL if no paired brackets
		 * were found. In this case, no further processing is
		 * necessary. Just return 0 (no change to set the dirtyBit).
		 */
		return ( 0 );
	}

	/* 
	 * Do further processing on the calculated pair list.
	 *
	 * First sort the pair list.
	 */

	br_SortPairList();

	/*
	 * Next scan through the pair list, resolving the
	 * embedding levels of each identified pair of brackets.
	 */

	br_ResolvePairEmbeddingLevels ( brcp );

	/* Clean up the pair list before return. */

	br_DropPairList();

	return ( 1 );
}

/*
 * br_UBA_ResolveNeutralsByContext
 *
 * This is the method for Rule N1.
 *
 * Resolve neutrals by context for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For any character of neutral type, examine its
 * context.
 *
 * L N L --> L L L
 * R N R --> R R R [note that AN and EN count as R for this rule]
 *
 * Here "N" stands for "any sequence of neutrals", so the neutral
 * does not have to be immediately adjacent to a strong type
 * to be resolved this way.
 */

static int br_UBA_ResolveNeutralsByContext ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;

	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = buppfirst; bupp <= bupplast; bupp++ )
	{
		if ( br_IsNeutralType ( (*bupp)->bc ) )
		{
		/* Check to see if N is in context L N L */
			if ( br_IsPriorContextL ( buppfirst, bupp, brcp->sot ) &&
				br_IsFollowingContextL ( bupplast, bupp, brcp->eot ) )
			{
				(*bupp)->bc = BIDI_L;
				dirtyBit = 1;
			}
		/* Check to see if N is in context R N R */
			else if ( br_IsPriorContextRANEN ( buppfirst, bupp, brcp->sot ) &&
				br_IsFollowingContextRANEN ( bupplast, bupp, brcp->eot ) )
			{
				(*bupp)->bc = BIDI_R;
				dirtyBit = 1;
			}
		}
	}

	return ( dirtyBit );
}

/*
 * br_UBA_ResolveNeutralsByLevel
 *
 * This is the method for Rule N2.
 *
 * Resolve neutrals by level for a single text chain.
 *
 * For each character in the text chain, examine its
 * Bidi_Class. For any character of neutral type, examine its
 * embedding level and resolve accordingly.
 *
 * N --> e
 * where e = L for an even level, R for an odd level
 */

static int br_UBA_ResolveNeutralsByLevel ( BIDIRULECTXTPTR brcp )
{
BIDIUNITPTR *bupp;
BIDIUNITPTR *buppfirst;
BIDIUNITPTR *bupplast;
int dirtyBit = 0;

	buppfirst = brcp->textChain;
	bupplast = buppfirst + brcp->len - 1;

	/*
	 * Process the text chain from first to last BIDIUNIT,
	 * checking the Bidi_Class of each character.
	 */
	for ( bupp = buppfirst; bupp <= bupplast; bupp++ )
	{
		if ( br_IsNeutralType ( (*bupp)->bc ) )
		{
		/* Check to see if N is in even embedding level */
			if ( (*bupp)->level % 2 == 0 )
			{
				(*bupp)->bc = BIDI_L;
			}
			else 
			{
				(*bupp)->bc = BIDI_R;
			}
			dirtyBit = 1;
		}
	}

	return ( dirtyBit );
}

/*******************************************************************/

/*
 * SECTION: Resolving Implicit Levels: Rules I1-I2
 *
 * This section runs the resolving implicit levels rules of the UBA.
 *
 * Each pass processes the text, and does not need to work on a
 * run-by-run basis, because it is simply resolving all the implicit
 * levels in all runs.
 */

/*
 * br_UBA_ResolveImplicitLevels
 *
 * This function runs Rules I1 and I2 together.
 */

static int br_UBA_ResolveImplicitLevels ( UBACTXTPTR ctxt )
{
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
int oddlevel;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_ResolveImplicitLevels [I1, I2]\n" );
	}

	ctxt->dirtyBit = 0;

	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		if ( bdu->level != NOLEVEL )
		{
			oddlevel = ( bdu->level % 2 == 1 );
			if ( oddlevel )
			{
				if ( ( bdu->bc == BIDI_L ) || ( bdu->bc_numeric ) )
				{
					bdu->level += 1;
					ctxt->dirtyBit = 1;
				}
			}
			else
			{
				if ( bdu->bc == BIDI_R )
				{
					bdu->level += 1;
					ctxt->dirtyBit = 1;
				}
				else if ( bdu->bc_numeric )
				{
					bdu->level += 2;
					ctxt->dirtyBit = 1;
				}
			}
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_I2Done;
	return ( 1 );
}

/*******************************************************************/

/*
 * SECTION: Reordering Resolved Levels: Rules L1-L4
 *
 * This section runs the reordering resolved levels rules of the UBA.
 *
 * Each pass processes the text, and does not need to work on a
 * run-by-run basis.
 *
 * Note: This reference implementation assumes that the text to
 * reorder consists of a single line, because it is running the
 * algorithm in the absence of any actual rendering context with
 * access to glyphs, knowledge of line length, and access to a
 * line breaking algorithm.
 */

/*
 * br_UBA_ResetWhitespaceLevels
 *
 * This function runs Rule L1.
 *
 * The strategy here for Rule L1 is to scan forward through
 * the text searching for segment separators or paragraph
 * separators. If a segment separator or paragraph
 * separator is found, it is reset to the paragraph embedding
 * level. Then scan backwards from the separator to
 * find any contiguous stretch of whitespace characters
 * and reset any which are found to the paragraph embedding
 * level, as well. When we reach the *last* character in the
 * text (which will also constitute, by definition, the last
 * character in the line being processed here), check if it
 * is whitespace. If so, reset it to the paragraph embedding
 * level. Then scan backwards to find any contiguous stretch
 * of whitespace characters and reset those as well.
 *
 * These checks for whitespace are done with the *original*
 * Bidi_Class values for characters, not the resolved values.
 *
 * As for many rules, this rule simply ignores any character
 * whose level has been set to NOLEVEL, which is the way
 * this reference algorithm "deletes" boundary neutrals and
 * embedding and override controls from the text.
 */

static int br_UBA_ResetWhitespaceLevels ( UBACTXTPTR ctxt )
{
BIDIUNITPTR bdu;
BIDIUNITPTR bdu2;
BIDIUNITPTR endOfText;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_ResetWhitespaceLevels [L1]\n" );
	}

	ctxt->dirtyBit = 0;

	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		if ( ( bdu->orig_bc == BIDI_S ) || ( bdu->orig_bc == BIDI_B ) )
		{
			if ( bdu->level != ctxt->paragraphEmbeddingLevel )
			{
				/* Only set the dirtyBit if the level has actually changed. */
				bdu->level = ctxt->paragraphEmbeddingLevel;
				ctxt->dirtyBit = 1;
			}
			/* scan back looking for contiguous whitespace */
			if ( bdu > ctxt->theText )
			{
				bdu2 = bdu - 1;
				while ( bdu2 >= ctxt->theText )
				{
					if ( bdu2->orig_bc == BIDI_WS )
					{
						bdu2->level = ctxt->paragraphEmbeddingLevel;
					}
					else if ( bdu2->level == NOLEVEL )
					{
						/* skip over *deleted* controls */
					}
					else
					{
						/* break out of loop for any other character */
						break;
					}
					bdu2--;
				}
			}
		}

		/* If at end of string, scan back checking for terminal whitespace */
		if ( bdu == endOfText - 1 )
		{
			bdu2 = bdu ;
			while ( bdu2 >= ctxt->theText )
			{
				if ( bdu2->orig_bc == BIDI_WS )
				{
					bdu2->level = ctxt->paragraphEmbeddingLevel;
					ctxt->dirtyBit = 1;
				}
				else if ( bdu2->level == NOLEVEL )
				{
					/* skip over *deleted* controls */
				}
				else
				{
					/* break out of loop for any other character */
					break;
				}
				bdu2--;
			}
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_L1Done;
	return ( 1 );
}

/*
 * br_UBA63_ResetWhitespaceLevels
 *
 * This function runs Rule L1.
 *
 * As for br_UBA_ResetWhitespaceLevels, except that for
 * UBA63, the scanback context which is reset to the
 * paragraph embedding level includes all contiguous
 * sequences of whitespace *and* any isolate format
 * controls. The test for the Bidi_Class of the isolate
 * format controls could be done on either the current
 * or original Bidi_Class, as the preceding rules do not
 * change the Bidi_Class of isolate format controls, but
 * all tests are done here on the original Bidi_Class
 * values in the BIDIUNIT, for consistency.
 */

static int br_UBA63_ResetWhitespaceLevels ( UBACTXTPTR ctxt )
{
BIDIUNITPTR bdu;
BIDIUNITPTR bdu2;
BIDIUNITPTR endOfText;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA63_ResetWhitespaceLevels [L1]\n" );
	}

	ctxt->dirtyBit = 0;

	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	while ( bdu < endOfText )
	{
		if ( ( bdu->orig_bc == BIDI_S ) || ( bdu->orig_bc == BIDI_B ) )
		{
			if ( bdu->level != ctxt->paragraphEmbeddingLevel )
			{
				/* Only set the dirtyBit if the level has actually changed. */
				bdu->level = ctxt->paragraphEmbeddingLevel;
				ctxt->dirtyBit = 1;
			}
			/* scan back looking for contiguous whitespace */
			if ( bdu > ctxt->theText )
			{
				bdu2 = bdu - 1;
				while ( bdu2 >= ctxt->theText )
				{
					if ( bdu2->orig_bc == BIDI_WS )
					{
						bdu2->level = ctxt->paragraphEmbeddingLevel;
					}
					else if ( br_IsIsolateControl ( bdu2->orig_bc ) )
					{
						bdu2->level = ctxt->paragraphEmbeddingLevel;
					}
					else if ( bdu2->level == NOLEVEL )
					{
						/* skip over *deleted* controls */
					}
					else
					{
						/* break out of loop for any other character */
						break;
					}
					bdu2--;
				}
			}
		}

		/* If at end of string, scan back checking for terminal whitespace */
		if ( bdu == endOfText - 1 )
		{
			bdu2 = bdu ;
			while ( bdu2 >= ctxt->theText )
			{
				if ( bdu2->orig_bc == BIDI_WS )
				{
					bdu2->level = ctxt->paragraphEmbeddingLevel;
					ctxt->dirtyBit = 1;
				}
				else if ( br_IsIsolateControl ( bdu2->orig_bc ) )
				{
					bdu2->level = ctxt->paragraphEmbeddingLevel;
					ctxt->dirtyBit = 1;
				}
				else if ( bdu2->level == NOLEVEL )
				{
					/* skip over *deleted* controls */
				}
				else
				{
					/* break out of loop for any other character */
					break;
				}
				bdu2--;
			}
		}

		/* Advance to the next character to process. */

		bdu++;
	}

	ctxt->state = State_L1Done;
	return ( 1 );
}

/*
 * br_ReverseRange
 *
 * For a specified range firstpos to lastpos, invert the position
 * values stored in the order field in the array of BIDIUNITs.
 *
 * Rather than reordering in place, which requires fancier manipulation
 * of indices, just use a spare order array in the BIDIUNIT vector.
 * Write the values into the range in reverse order, then copy them
 * back into the main order array in the reversed order.
 * This is a simple and easy to understand approach.
 */

static void br_ReverseRange ( UBACTXTPTR ctxt, int firstpos, int lastpos )
{
int ix;
int newpos;

	/*
	 * First copy the range from the order field into the order2 field
	 * in reversed order.
	 */
	for ( ix = firstpos, newpos = lastpos; ix <= lastpos; ix++, newpos-- )
	{
		ctxt->theText[ix].order2 = ctxt->theText[newpos].order;
	}
	/*
	 * Then copy the order2 values back into the order field.
	 */
	for ( ix = firstpos; ix <= lastpos; ix++ )
	{
		ctxt->theText[ix].order = ctxt->theText[ix].order2;
	}
}

/*
 * br_UBA_ReverseLevels
 *
 * This function runs Rule L2.
 *
 * Find the highest level among the resolved levels.
 * Then from that highest level down to the lowest odd
 * level, reverse any contiguous runs at that level or higher.
 */

static int br_UBA_ReverseLevels ( UBACTXTPTR ctxt )
{
int ix;
BIDIUNITPTR bdu;
BIDIUNITPTR endOfText;
int highestlevel;
int lowestoddlevel;
int level;
int nolevels;
int inrange;
int significantrange;
int firstpos;
int lastpos;

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_ReverseLevels [L2]\n" );
	}

	ctxt->dirtyBit = 0;

	/*
	 * First scan the text to determine the highest level and
	 * the lowest odd level.
	 */
	bdu = ctxt->theText;
	endOfText = ctxt->theText + ctxt->textLen;
	highestlevel = 0;
	lowestoddlevel = maximum_depth + 1;
	nolevels = 1;

	while ( bdu < endOfText )
	{
		if ( bdu->level != NOLEVEL )
		{
			/* Found something other than NOLEVEL. */
			nolevels = 0;
			if ( bdu->level > highestlevel )
			{
				highestlevel = bdu->level;
			}
			if ( ( bdu->level % 2 == 1 ) && ( bdu->level < lowestoddlevel ) )
			{
				lowestoddlevel = bdu->level;
			}
		}
		/* Advance to the next character to process. */

		bdu++;
	}

	if ( Trace ( Trace8 ) )
	{
		if ( nolevels )
		{
			printf ( "Debug: No levels found.\n" );
		}
		else if ( lowestoddlevel == maximum_depth + 1 )
		{
			printf ( "Debug: highestlevel=%d, lowestoddlevel=NONE\n", highestlevel );
		}
		else
		{
			printf ( "Debug: highestlevel=%d, lowestoddlevel=%d\n", highestlevel,
				lowestoddlevel );
		}
	}

	/* If there are no levels set, don't bother with reordering anything. */

	if ( nolevels )
	{
		ctxt->state = State_L2Done;
		return ( 1 );
	}

	/* 
	 * Next reverse contiguous runs at each level (or higher),
	 * starting with the highest level and decrementing to
	 * the lowest odd level.
	 */
	for ( level = highestlevel; level >= lowestoddlevel; level-- )
	{
		if ( Trace ( Trace8 ) )
		{
			printf ( "Level %d:", level );
		}
		/*
		 * For each relevant level, scan the text to find
		 * contiguous ranges at that level (or higher).
		 * A *significant* contiguous range is figured as
		 * one that contains at least two characters with
		 * an explicit level. Don't bother reversing ranges
		 * which contain only one character with an explicit
		 * level and then one or more trailing NOLEVEL "deleted"
		 * characters.
		 */
		ix = 0;
		bdu = ctxt->theText;
		inrange = 0;
		significantrange = 0;
		firstpos = -1;
		lastpos = -1;
		while ( ix < ctxt->textLen )
		{
			if ( bdu->level >= level )
			{
				if ( !inrange )
				{
					inrange = 1;
					firstpos = ix;
				}
				else
				/* Hit a second explicit level character. */
				{
					significantrange = 1;
					lastpos = ix;
				}
			}
			else if ( bdu->level == NOLEVEL )
			{
				/* don't break ranges for "deleted" controls */
				if ( inrange )
				{
					lastpos = ix;
				}
			}
			else
			{
				/* End of a range. Reset the range flag and reverse the range. */
				inrange = 0;
				if ( ( lastpos > firstpos ) && significantrange )
				{
					if ( Trace ( Trace8 ))
					{
						printf ( " reversing %d to %d ", firstpos, lastpos );
					}
					br_ReverseRange ( ctxt, firstpos, lastpos );
					ctxt->dirtyBit = 1;
				}
				firstpos = -1;
				lastpos = -1;
			}

			/* Advance to the next character to process. */

			ix++;
			bdu++;
		}
		/* 
		 * If we reached the end of the input while the inrange flag is
		 * set, then we need to do a reversal before exiting this level.
		 */
		if ( inrange && ( lastpos > firstpos ) && significantrange )
		{
			if ( Trace ( Trace8 ) )
			{
				printf ( " reversing %d to %d ", firstpos, lastpos );
			}
			br_ReverseRange ( ctxt, firstpos, lastpos );
			ctxt->dirtyBit = 1;
		}
		if ( Trace ( Trace8 ) )
		{
			printf ( "\n" );
		}
	}

	ctxt->state = State_L2Done;
	return ( 1 );
}

/*******************************************************************/

/*
 * SECTION: Main invocation of UBA.
 *
 * Running the algorithm is broken up into sequential steps which
 * mirror the rule sections in UAX #9.
 */

/*
 * br_UBA_RuleDispatch
 *
 * The rule dispatcher abstracts the version-specific extraction
 * of text chains from the UBACONTEXT and dispatches text chains,
 * one at a time, to the appropriate rule method.
 *
 * This enables better encapsulation of the distinction between
 * UBA62 rules, which apply to lists of level runs, and
 * UBA63 rules, which apply to isolating run sequences (which
 * in turn consist of sets of level runs).
 *
 * The logic for the rule itself, which is applied on a per text chain
 * basis, can be abstracted to the ruleMethod, which often does
 * not need to be made version-specific.
 *
 * The return value from the ruleMethod is used to set the
 * dirtyBit in the UBACONTEXT.
 *
 * br_UBA_RuleDispatch is only used for rules which can be
 * analyzed as applying independently to a single text chain, and
 * which are not version-specific in their application to the runs.
 * (Currently the W rules and the N rules.)
 *
 * br_UBA_RuleDispatch also handles printing out an error message,
 * if any, and printing the debug display diagnostics, to cut
 * down on repetitive code in the main UBA call routines.
 */

static int br_UBA_RuleDispatch ( UBACTXTPTR ctxt, BIDI_RULE_TYPE rule )
{
int rc;
RTEPTR rtp;
BIDI_RULE_CONTEXT brc;
char errString[80];

	rtp = &(bidi_rules[rule]);

	if ( Trace ( Trace1 ) )
	{
		printf ( "Trace: Entering br_UBA_%s [%s]\n",
			rtp->ruleLabel, rtp->ruleNumber );
	}

	ctxt->dirtyBit = 0;

	rc = 1;

	if ( GetUBAVersion() == UBA62 )
	{
	BIDIRUNPTR brp;

		brp = ctxt->theRuns;

		while ( brp != NULL )
		{
			/* Initialize the BIDI_RULE_CONTEXT. */
			brc.textChain = brp->textChain;
			brc.len = brp->len;
			brc.level = brp->level;
			brc.sot = brp->sor;
			brc.eot = brp->eor;

			/*
			 * Invoke the appropriate rule method for this rule,
			 * applying it to the text chain for this run.
			 */

			rc = rtp->ruleMethod ( &brc );
			if ( rc == 1 )
			{
				ctxt->dirtyBit = 1;
			}
			else if ( rc == -1 )
			{
				break;
			}
			/* Advance to the next run */
			brp = brp->next;
		}
	}
	else
	{
	IRSEQPTR irp;

		irp = ctxt->theSequences;

		while ( irp != NULL )
		{
			/* Initialize the BIDI_RULE_CONTEXT. */
			brc.textChain = irp->textChain;
			brc.len = irp->len;
			brc.level = irp->level;
			brc.sot = irp->sos;
			brc.eot = irp->eos;

			/*
			 * Invoke the appropriate rule method for this rule,
			 * applying it to the text chain for this run.
			 */

			rc = rtp->ruleMethod ( &brc );
			if ( rc == 1 )
			{
				ctxt->dirtyBit = 1;
			}
			else if ( rc == -1 )
			{
				break;
			}
			/* Advance to the next run */
			irp = irp->next;
		}
	}

    if ( rc == -1 )
    {
    	sprintf ( errString, "Error in: %s.\n",  rtp->ruleError );
    	br_ErrPrint ( errString );
    }

    switch ( rule )
    {
    	case UBA_RULE_W1:
    			ctxt->state = State_W1Done;
    			break;
    	case UBA_RULE_W2:
    			ctxt->state = State_W2Done;
    			break;
    	case UBA_RULE_W3:
    			ctxt->state = State_W3Done;
    			break;
    	case UBA_RULE_W4:
    			ctxt->state = State_W4Done;
    			break;
    	case UBA_RULE_W5:
    			ctxt->state = State_W5Done;
    			break;
    	case UBA_RULE_W6:
    			ctxt->state = State_W6Done;
    			break;
    	case UBA_RULE_W7:
    			ctxt->state = State_W7Done;
    			break;
    	case UBA_RULE_N0:
    			ctxt->state = State_N0Done;
    			break;
    	case UBA_RULE_N1:
    			ctxt->state = State_N1Done;
    			break;
    	case UBA_RULE_N2:
    			ctxt->state = State_N2Done;
    			break;
    }

    br_DisplayState ( ctxt );

	return ( ( rc == -1 ) ? 0 : 1 );
}

/*
 * br_UBA_WRulesDispatch
 *
 * Dispatch rules W1-W7.
 *
 * The dispatch and order of application is the same for UBA62
 * and for UBA63.
 */

static int br_UBA_WRulesDispatch ( UBACTXTPTR ctxt )
{
int rc;

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W1 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W2 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W3 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W4 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W5 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W6 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_W7 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

	return ( BR_TESTOK );
}

/*
 * br_UBA_62
 *
 * Run the Version 6.2 UBA algorithm on the text (or Bidi_Class vector) stored in
 * the ctxt pointer.
 */

static int br_UBA_62 ( UBACTXTPTR ctxt )
{
int rc;

    if ( Trace ( Trace1 ) )
    {
		printf ( "Trace: Entering br_UBA_62\n" );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_ParagraphEmbeddingLevel ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in: processing paragraph embedding level.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_ExplicitEmbeddingLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in: processing explicit embedding levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_DeleteFormatCharacters ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in: processing deletion of format characters.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_IdentifyRuns ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in identifying runs.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    /*
     * Next we start a stretch of rules which apply systematically
     * to the list of level runs (for UBA62).
     *
     * For compactness and abstraction, these are dispatched
     * through a central rule handler which handles rules of
     * this generic type.
     */

    rc = br_UBA_WRulesDispatch ( ctxt );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_N1 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_N2 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_ResolveImplicitLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in resolving implicit levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_ResetWhitespaceLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in resetting whitespace levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_ReverseLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in reversing levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

 	return ( BR_TESTOK );
}

/*
 * br_UBA_63
 *
 * Run the Version 6.3 UBA algorithm on the text (or Bidi_Class vector) stored in
 * the ctxt pointer.
 */

static int br_UBA_63 ( UBACTXTPTR ctxt )
{
int rc;

    if ( Trace ( Trace1 ) )
    {
		printf ( "Trace: Entering br_UBA_63\n" );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA63_ParagraphEmbeddingLevel ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in processing paragraph embedding level.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA63_ExplicitEmbeddingLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in processing explicit embedding levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_DeleteFormatCharacters ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in processing deletion of format characters.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_IdentifyRuns ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in identifying runs.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_IdentifyIsolatingRunSequences ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in identifying isolating run sequences.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    /*
     * Next we start a stretch of rules which apply systematically
     * to the list of isolating run sequences (for UBA63).
     *
     * For compactness and abstraction, these are dispatched
     * through a central rule handler which handles rules of
     * this generic type.
     */

    rc = br_UBA_WRulesDispatch ( ctxt );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_N0 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_N1 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_RuleDispatch ( ctxt, UBA_RULE_N2 );
    if ( rc != 1 )
    {
    	return ( BR_TESTERR );
    }

    rc = br_UBA_ResolveImplicitLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in resolving implicit levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA63_ResetWhitespaceLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in resetting whitespace levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

    rc = br_UBA_ReverseLevels ( ctxt );
    if ( rc != 1 )
    {
    	br_ErrPrint ( "Error in reversing levels.\n" );
    	return ( BR_TESTERR );
    }
    br_DisplayState ( ctxt );

	return ( BR_TESTOK );
}

/*
 * br_UBA
 *
 * Run the UBA algorithm on the text stored in
 * the ctxt pointer.
 *
 * Check which version to run. Set any required
 * global parameters, and dispatch appropriately.
 */

int br_UBA ( UBACTXTPTR ctxt )
{
int rc;

	if ( GetUBAVersion() == UBA62 )
	{
		maximum_depth = MAX_DEPTH_62;
		rc = br_UBA_62 ( ctxt );
	}
	else
	{
		maximum_depth = MAX_DEPTH_63;
		rc = br_UBA_63 ( ctxt );
	}
	return ( rc );
}

/*******************************************************************/

/*
 * br_Check
 *
 * Check the result of running the UBA algorithm
 * against criteria for what the expected outcome
 * is supposed to be.
 *
 * The expected results for the levels and the order
 * are simply stored as strings hung off the UBACONTEXT,
 * parsed from the test case data. So when we hit
 * br_Check, the finishing of the checking involves
 * doing the parsing of that expected results data.
 * Conceivably, there may be errors in that data, including
 * syntax errors, so it has to be checked at this step
 * during parsing, to provide meaningful results.
 */

int br_Check ( UBACTXTPTR ctxt )
{
int ix;
int started;
BIDIUNITPTR bdu;
char localbuf[5];
char localbuf2[BUFFERLEN];

	if ( Trace ( Trace1 ) )
	{
		printf ( "\nTrace: Entering br_Check\n" );
	}

	/* 
	 * Start by checking that the calculated resolved paragraph
	 * embedding level matches the expected value provided
	 * with the test case data.
	 *
	 * This step is omitted for FORMAT_B, because BidiTest.txt does
	 * not supply the expected resolved paragraph embedding level.
	 */

	if ( GetFileFormat() == FORMAT_A )
	{
		if ( ctxt->paragraphEmbeddingLevel != ctxt->expEmbeddingLevel )
		{
			if ( Trace ( Trace0 ) )
			{
				printf ( "Mismatch in expected paragraph embedding level\n" );
			}
			return ( BR_TESTERR );
		}
	}

	/*
	 * The tests for matching expected levels and order are more
	 * complicated to express. Display the context again.
	 * When the algorithm is State_Complete, this will also add
	 * display of the expected levels and the expected order,
	 * parsed from the test case.
	 */
	 
    br_DisplayState ( ctxt );

	bdu = ctxt->theText;
	ix = 0;
	while ( ix < ctxt->textLen )
	{
		if ( bdu->level != bdu->expLevel )
		{
			if ( Trace ( Trace0 ) )
			{
				printf ( "Level does not match expected level in test case\n" );
			}
			return ( BR_TESTERR );
		}
		ix++;
		bdu++;
	}

	/*
	 * For order, just compare the expOrder string parsed from
	 * the test case, with an order string constructed from the
	 * order fields in theText. This is the same as the string
	 * constructed for display, which seems to be matching the
	 * test case values o.k.
	 *
	 * TBD: Figure out how to compare expected results when there
	 * are NOLEVEL values in the text to be reordered. This may
	 * require revisiting the reorder function first. As a
	 * first approximation to the expected order results, just
	 * omit printing any resolved order for an element with
	 * resolved level of NOLEVEL.
	 *
	 * TBD: Consider whether a more elaborate testing of expected
	 * order makes any sense to bother with. In that case, it
	 * might make sense to do the parsing of the expected order
	 * when the test data is first attached, rather than doing
	 * this handling after the fact.
	 */

	if ( ctxt->expOrder == NULL )
	{
		/* 
		 * If no expected order has been attached, there is
		 * no point in continuing. Just return PASS.
		 */
		return ( BR_TESTOK );
	}

	bdu = ctxt->theText;
	ix = 0;
	started = 0;
	/* Null out the localbuf2 accumulator */
	localbuf2[0] = '\0'; 
	while ( ix < ctxt->textLen )
	{
		/* Skip any "deleted" elements */
		if ( bdu->order != -1 )
		{
			if ( !started )
			{
				/* Don't add an initial space to the first element. */
				sprintf ( localbuf, "%d", bdu->order );
				started = 1;
			}
			else
			{
				sprintf ( localbuf, " %d", bdu->order );
			}
			strcat ( localbuf2, localbuf );
		}
		ix++;
		bdu++;
	}

	/* 
	 * The order values have all been appended.
	 * Now compare against the expected value.
	 */
	if ( strcmp ( ctxt->expOrder, localbuf2 ) != 0 )
	{
		if ( Trace ( Trace0 ) )
		{
			printf ( "\nOrder [%s] does not match expected order [%s] in test case\n",
				localbuf2, ctxt->expOrder );
		}
		return ( BR_TESTERR );
	}
	return ( BR_TESTOK );
}
