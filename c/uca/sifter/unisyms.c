// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**      Unilib
**      Copyright 2018
**      Ken Whistler, All rights reserved.
*/

/*
 * unisyms.c
 *
 * Tables for dumping weighted values in POSIX style symbol
 * lists, as per FCD 14651.
 *
 * Important: The label arrays need to be updated if any change is
 * made to the hardcoded default orders of tertiary, secondary, and/or
 * secondary-combination weights.
 *
 * Recent change history:
 *   2016-Jun-07 Corrected hack that dumps comments for tertiary weights.
 *   2016-Jul-07 Adjusted symbols for implicit bases to use "R" instead
 *               of "S" to avoid potential collisions.
 *               Small tweak for Lao digraph collation element handling.
 *   2016-Aug-10 More tweaks to comments in output.
 *   2017-Feb-28 Add Soyombo gemination mark to secondaries list.
 *   2017-Mar-01 Add Nushu to computed implicit weights.
 *   2017-Mar-08 Major rework of the output of 4th level weight symbols
 *               for the CTT.
 *   2017-Jun-22 Finishing rework for 4th level weight symbols for CTT.
 *   2018-Feb-19 Updated for Unicode 11.0 repertoire additions.
 *   2018-Oct-02 Corrected format type for parameters in printf statements
 *               for Thai and Lao contractions.
 *   2018-Oct-15 Added 4 secondary weight symbols for Wancho tones.
 *   2018-Oct-16 Code warning removals.
 */

#define _CRT_SECURE_NO_WARNINGS
#include "unisift.h"

#include <malloc.h>

extern int debugLevel;

/*
 * Node to build up tree of symbolic line entries for the table.
 *
 * The thestrings field is the head of a string list that is used
 * to store all strings that end up with the same key in the tree.
 * With the nulling out of all weights for many control characters,
 * for instance, there are now quite a few duplicate keys constructed.
 */

typedef struct strnode
  {
    struct strnode *next;
    char           *outputstring;
  } STRLISTNODE;

typedef STRLISTNODE *PSTRLISTNODE;

typedef struct symnode
  {
    struct symnode *left;
    struct symnode *right;
    UInt32   *key;
    int             keylen;
    STRLISTNODE    *thestrings;
    STRLISTNODE    *theucastrings;
  } SYMTREENODE;

typedef SYMTREENODE *PSYMTREENODE;

static PSYMTREENODE symTreeRoot = NULL;

static int greatestDepth = 0;

static int unisift_InsertSym ( UInt32 *key, int keylen, char *s,
                               char* ucastr );

/*
 * Node definition for building a small btree to sort the symbolic
 * labels for primary weights into their weight order.
 */

typedef struct symwtnode
  {
    struct symwtnode *left;
    struct symwtnode *right;
    UInt32           weight; /* Actual weight for the symbol */
    UInt32           value;  /* Unicode value of the base, used to construct its label */
  } SYMWTTREENODE;

typedef SYMWTTREENODE *PSYMWTTREENODE;

static PSYMWTTREENODE symwtTreeRoot = NULL;

/*
 * External declarations
 */

extern void dumpUCAProlog ( FILE *fd );

extern char *GetUnidataFileName ( void );

/*
 * Constants used to construct symbolic label tables.
 */

#define NUMTERTSYMS (32)

/*
 * NUMSECONDSYMS and the following tables needs to be revisited whenever
 * any new NSM is added or reordered.
 */
#define NUMSECONDSYMS (251)

static const char *tertSyms[NUMTERTSYMS] =
                   { "<RES-1>",
                     "<BLK>",
                     "<MIN>",
                     "<WIDE>",
                     "<COMPAT>",
                     "<FONT>",
                     "<CIRCLE>",
                     "<RES-2>",
                     "<CAP>",
                     "<WIDECAP>",
                     "<COMPATCAP>",
                     "<FONTCAP>",
                     "<CIRCLECAP>",
                     "<HIRA-SMALL>",
                     "<HIRA>",
                     "<SMALL>",
                     "<SMALL-NARROW>",
                     "<KATA>",
                     "<NARROW>",
                     "<CIRCLE-KATA>",
                     "<MNN>",
                     "<MNS>",
                     "<VERTICAL>",
                     "<AINI>",
                     "<AMED>",
                     "<AFIN>",
                     "<AISO>",
                     "<NOBREAK>",
                     "<SQUARED>",
                     "<MISCCAP>",
                     "<FRACTION>",
                     "<MAX>"
                     };

/*
 * NB: Any changes to this array must be matched by changes to
 * the secondSymVals array below.
 */
static const char *secondSyms[NUMSECONDSYMS] = {

"<BASE>", /* no accent/aucun accent */
/* Section 1 of combining marks; adapted to 12199 and Canadian standard */
/* And to the specification of European Ordering Rules for Greek */
"<LOWLINE>", /* low line */
"<PSILI>", /* psili */
"<DASIA>", /* dasia  */
"<AIGUT>", /* acute/aigu */
"<GRAVE>", /* grave/grave */
"<BREVE>", /* breve */
"<CIRCF>", /* circumflex/circonflexe */
"<CARON>", /* caron/caron */
"<CRCLE>", /* ring/rond  */
"<PERIS>", /* perispomeni  */
"<TREMA>", /* diaeresis */
"<2AIGU>", /* double acute/double aigu  */
"<TILDE>", /* tilde/tilde */
"<POINT>", /* dot/point */
"<OBLIK>", /* stroke/barre oblique */
"<CEDIL>", /* cedilla */
"<OGONK>", /* ogonek/ogonek */
"<MACRO>", /* macron/macron */

/*
 * Section 2: artificial generic combining marks used to
 * collapse the actual list of required secondary weights.
 */

"<DABOVE>", /* generic mark above */
"<DBELOW>", /* generic mark below */
"<DTHRU>",  /* generic mark through */
"<DAROUND>", /* generic mark around */

/*
 * Section 3a: A few high-frequency combining marks.
 */
"<KNVCE>", /* Katakana-Hiragana voiced sound mark */
"<KNSMV>", /* Katakana-Hiragana semi-voiced sound mark*/
"<BARRE>", /* bar/barre  */

/* 
 * Section 3b of combining marks; everything else.
 * more or less in Unicode order,
 * except for the Indic combining marks, which have some particular
 * desired order: nukta's first, then candrabindu, anusvara, visarga.
 */
"<OVERLINE>",  /* overline */
"<CROOK>", /* hook/crochet */
#ifdef NOTDEF
"<D030D>", /* vertical line above  */
"<D030E>", /* double vertical line above */
#endif
"<2GRAV>", /* double grave/double grave */
"<D0310>", /* combining candrabindu */
"<BREVR>", /* inverted breve */
#ifdef NOTDEF
"<D0312>",  /* turned comma above */
"<D0315>", /* comma above right */
"<D0316>", /* grave accent below */
"<D0317>", /* acute accent below */
"<D0318>", /* left tack below */
"<D0319>", /* right tack below */
"<D031A>", /* left angle above */
#endif
"<HORNU>", /* horn/cornu */
#ifdef NOTDEF
"<D031C>", /* left half ring */
"<D031D>", /* up tack below */
"<D031E>", /* down tack below */
"<D031F>", /* plus sign below */
"<D0320>", /* minus sign below */
#endif
"<PALCR>", /* palatal hook  */
"<RETCR>", /* retroflex hook  */
"<POINS>", /* dot below/point souscrit */
"<TREMS>", /* diaeresis below  */
"<CRCLS>", /* ring below/rond souscrit */
"<COMMS>", /* comma below */
#ifdef NOTDEF
"<D0329>", /* vertical line below */
"<D032A>", /* bridge below */
"<D032B>", /* inverted double arch below */
"<D032C>", /* caron below */
#endif
"<CIRCS>", /* circumflex below/circonflexe souscrit */
"<BREVS>", /* breve below */
#ifdef NOTDEF
"<D032F>", /* inverted breve below */
#endif
"<TILDS>", /* tilde below/tilde souscrit */
"<MACRS>", /* macron below/macron souscrit */
#ifdef NOTDEF
"<D0333>", /* double low line */
#endif
"<TILDX>", /* middle tilde  */
#ifdef NOTDEF
"<D0336>", /* long stroke overlay */
"<D0337>", /* short solidus overlay */
#endif
"<CRCL2>", /* right half ring below  */
#ifdef NOTDEF
"<D033A>", /* inverted bridge below */
"<D033B>", /* square below */
"<D033C>", /* seagull below */
"<D033D>", /* x above */
"<D033E>", /* vertical tilde */
"<D033F>", /* double overline */
#endif
"<YPOGE>", /* ypogegrammeni = iota subscript */
#ifdef NOTDEF
"<D0346>", /* bridge above */
"<D0347>", /* equals sign below */
"<D0348>", /* double vertical line below */
"<D0349>", /* left angle below */
"<D034A>", /* not tilde above */
"<D034B>", /* homothetic above */
"<D034C>", /* almost equal to above */
"<D034D>", /* left right arrow below */
"<D034E>", /* upwards arrow below */
"<D0350>", /* right arrowhead above */
"<D0351>", /* left half ring above */
"<D0352>", /* fermata */
"<D0353>", /* x below */
"<D0354>", /* left arrowhead below */
"<D0355>", /* right arrowhead below */
"<D0356>", /* right arrowhead and up arrowhead below */
"<D0357>", /* right half ring above */
#endif
"<D0358>", /* dot above right */
#ifdef NOTDEF
"<D0359>", /* asterisk below */
"<D035A>", /* double ring below */
"<D035B>", /* zigzag above */
"<D035C>", /* double breve below */
"<D035D>", /* double breve */
"<D035E>", /* double macron */
"<D035F>", /* double macron below */
#endif
"<D0360>", /* double tilde */
"<D0361>", /* double inverted breve */
#ifdef NOTDEF
"<D0362>", /* double rightwards arrow below */

"<D1DC0>", /* dotted grave accent */
"<D1DC1>", /* dotted acute accent */
"<D1DC2>", /* snake below */
"<D1DC3>", /* suspension mark */
"<D1DC4>", /* macron-acute */
"<D1DC5>", /* grave-macron */
"<D1DC6>", /* macron-grave */
"<D1DC7>", /* acute-macron */
"<D1DC8>", /* grave-acute-grave */
"<D1DC9>", /* acute-grave-acute */
"<D1DCB>", /* breve-macron */
"<D1DCC>", /* macron-breve */
"<D1DCD>", /* double circumflex above */
"<D1DCE>", /* ogonek above */
"<D1DCF>", /* zigzag below */
"<D1DD0>", /* is below */
"<D1DD1>", /* ur above */

"<D1DFD>", /* almost equal to below */
"<D1DFE>", /* left arrowhead above */
"<D1DFF>", /* right arrowhead and down arrowhead below */

"<D2CEF>", /* Coptic ni above */
"<D2CF0>", /* Coptic spiritus asper --> 0x0314 */
"<D2CF1>", /* Coptic spiritus lenis --> 0x0313 */
#endif

"<D0483>", /* Cyrillic titlo */
#ifdef NOTDEF
"<D0484>", /* Cyrillic palatalization */
"<D0485>", /* Cyrillic dasia pneumata */
"<D0486>", /* Cyrillic psili pneumata */
"<D0487>", /* Cyrillic pokryutie */
#endif
"<DA66F>", /* Cyrillic vzmet */
#ifdef NOTDEF
"<DA67C>", /* Cyrillic kavyka */
"<DA67D>", /* Cyrillic payerok */
#endif

"<SHEVA>", /* sheva  */
"<HTFSG>", /* hataf segol  */
"<HTFPT>", /* hataf patah */
"<HTFQM>", /* hataf qamats */
"<HIRIQ>", /* hiriq  */
"<TSERE>", /* tsere  */
"<SEGOL>", /* segol  */
"<PATAH>", /* patah  */
"<QAMAT>", /* qamats  */
#ifdef NOTDEF
"<D05C7>", /* qamats qatan */
#endif
"<HOLAM>", /* holam  */
#ifdef NOTDEF
"<HOLAMHASER>", /* holam haser for vav  */
#endif
"<QUBUT>", /* qubuts  */
"<SINPT>", /* sin dot/point sin  */
"<SHINP>", /* shin dot/point shin  */
"<DAGES>", /* dagesh  */
"<RAPHE>", /* raphe   */
"<VARIKA>",  /* varika */

"<D081D>",  /* Samaritan vowel sign e */
"<D0820>",  /* Samaritan vowel sign aa */
"<D0823>",  /* Samaritan vowel sign a */
"<D0825>",  /* Samaritan vowel sign short a */
"<D0827>",  /* Samaritan vowel sign u */
"<D082A>",  /* Samaritan vowel sign i */
"<D082B>",  /* Samaritan vowel sign o */
"<D082C>",  /* Samaritan vowel sign sukun */

"<D0818>",  /* Samaritan occlusion */
"<D0819>",  /* Samaritan dagesh */
"<D082D>",  /* Samaritan nequdaa */

"<FATHATAN>", /* fathatan */
"<D08F0>", /* open fathatan */
"<D08E7>", /* curly fathatan */
"<DAMMATAN>", /* dammatan */
"<D08F1>", /* open dammatan */
"<D08E8>", /* curly dammatan */
"<KASRATAN>", /* kasratan */
"<D08F2>", /* open kasratan */
"<D08E9>", /* curly kasratan */
"<FATHA>", /* fatha */
"<D08E4>", /* curly fatha */
"<D08F4>", /* fatha with ring */
"<D08F5>", /* fatha with dot above */
"<DAMMA>", /* damma */
"<D08E5>", /* curly damma */
"<D08FE>", /* damma with dot */
"<KASRA>", /* kasra */
"<D08E6>", /* curly kasra */
"<D08F6>", /* kasra with dot below */
"<SHADDA>", /* shadda */
"<SUKUN>", /* sukun */
"<MADDA>", /* maddah above */
"<HAMZA>", /* hamza above */
"<HAMZB>", /* hamza below */
"<D065F>", /* wavy hamza below */
"<SUBALEF>", /* subscript alef */
"<D0657>", /* inverted damma */
"<D0658>", /* noon ghunna */
"<D08FF>", /* sideways noon ghunna */
"<D0659>", /* zwarakay */
"<D065A>", /* small v above */
"<D065B>", /* inverted small v above */
"<D065C>", /* dot below */
"<D065D>", /* reversed damma */
"<D065E>", /* fatha with two dots */
"<D08E3>", /* turned damma below */
"<D08F7>", /* left arrowhead above */
"<D08F8>", /* right arrowhead above */
"<D08FD>", /* right arrowhead above with dot */
"<D08FC>", /* double right arrowhead above */
"<D08FB>", /* double right arrowhead above with dot */
"<D08F9>", /* left arrowhead below */
"<D08FA>", /* right arrowhead below */
"<SUPERALEF>", /* superscript alef */

"<D0711>", /* Syriac superscript alaph */
"<D0730>", /* Syriac pthaha above */
"<D0731>", /* Syriac pthaha below */
"<D0732>", /* Syriac pthaha dotted */
"<D0733>", /* Syriac zqapha above */
"<D0734>", /* Syriac zqapha below */
"<D0735>", /* Syriac zqapha dotted */
"<D0736>", /* Syriac rbasa above */
"<D0737>", /* Syriac rbasa below */
"<D0738>", /* Syriac dotted zlama horizontal */
"<D0739>", /* Syriac dotted zlama angular */
"<D073A>", /* Syriac hbasa above */
"<D073B>", /* Syriac hbasa below */
"<D073C>", /* Syriac hbasa-esasa dotted */
"<D073D>", /* Syriac esasa above */
"<D073E>", /* Syriac esasa below */
"<D073F>", /* Syriac rwaha */
#ifdef NOTDEF
"<D0741>", /* Syriac qushshaya */
"<D0742>", /* Syriac rukkakha */
"<D0745>", /* Syriac three dots above */
"<D0746>", /* Syriac three dots below */
#endif

"<D07EB>", /* N'Ko short high tone */
"<D07EC>", /* N'Ko short low tone */
"<D07ED>", /* N'Ko short rising tone */
"<D07EE>", /* N'Ko long descending tone */
"<D07EF>", /* N'Ko long high tone */
"<D07F0>", /* N'Ko long low tone */
"<D07F1>", /* N'Ko long rising tone */
"<D07F2>", /* N'Ko nasalization mark */
"<D07F3>", /* N'Ko double dot above */
"<D135F>", /* Ethiopic gemination mark */
"<D135E>", /* Ethiopic vowel length mark */
"<D135D>", /* Ethiopic gemination and vowel length mark */
"<DA6F0>", /* Bamum koqndon */
"<DA6F1>", /* Bamum tukwentis */
"<D16AF0>", /* Bassa Vah high tone */
"<D16AF1>", /* Bassa Vah low tone */
"<D16AF2>", /* Bassa Vah mid tone */
"<D16AF3>", /* Bassa Vah low-mid tone */
"<D16AF4>", /* Bassa Vah high-low tone */
"<D1E944>", /* Adlam alif lengthener */
"<D1E94A>", /* Adlam nukta */
"<D1E947>", /* Adlam hamza */
"<D1E948>", /* Adlam consonant modifier */
"<D1E949>", /* Adlam geminate consonant modifier */
/*
 * The Devanagari nukta, candrabindu, anusvara, and visarga
 * are used for the common weights of these same characters
 * across most Indic scripts. Accordingly, they are given
 * generic labels for the 14651 table.
 */
"<NUKTA>", /* Devanagari sign nukta */
"<CANDRABINDU>", /* Devanagari candrabindu */
"<ANUSVARA>", /* Devanagari anusvara */
"<VISARGA>", /* Devanagari visarga */
#ifdef NOTDEF
"<D0951>", /* Devanagari stress sign udatta  */
"<D0952>", /* Devanagari stress sign anudatta */
"<D0953>", /* Devanagari grave accent */
"<D0954>", /* Devanagari acute accent */
"<D09BC>", /* Bengali sign nukta */
"<D0981>", /* Bengali candrabindu */
"<D0982>", /* Bengali anusvara */
"<D0983>", /* Bengali visarga */
#endif
"<D09FE>", /* Bengali sandhi mark */
#ifdef NOTDEF
"<D0A3C>", /* Gurmukhi sign nukta */
"<D0A01>", /* Gurmukhi adak bindi */
"<D0A02>", /* Gurmukhi bindi */
"<D0A03>", /* Gurmukhi visarga */
#endif
"<D0A70>", /* Gurmukhi tippi */
"<D0A71>", /* Gurmukhi addak */
#ifdef NOTDEF
"<D0ABC>", /* Gujarati sign nukta */
"<D0A81>", /* Gujarati candrabindu */
"<D0A82>", /* Gujarati anusvara */
"<D0A83>", /* Gujarati visarga */
"<D0B3C>", /* Oriya sign nukta */
"<D0B01>", /* Oriya candrabindu */
"<D0B02>", /* Oriya anusvara */
"<D0B03>", /* Oriya visarga */
"<D0B82>", /* Tamil anusvara */
"<D0C01>", /* Telugu candrabindu */
"<D0C02>", /* Telugu anusvara */
"<D0C03>", /* Telugu visarga */
"<D0CBC>", /* Kannada nukta */
"<D0C82>", /* Kannada anusvara */
"<D0C83>", /* Kannada visarga */
"<D0D02>", /* Malayalam anusvara */
"<D0D03>", /* Malayalam visarga */
"<D0D82>", /* Sinhala anusvaraya */
"<D0D83>", /* Sinhala visargaya */
"<D1B34>", /* Balinese nukta */
"<D1B00>", /* Balinese ardhacandra */
"<D1B01>", /* Balinese candrabindu */
"<D1B02>", /* Balinese anusvara */
#endif
"<D1B03>", /* Balinese repha */
#ifdef NOTDEF
"<D1B04>", /* Balinese visarga */
"<DA9B3>", /* Javanese nukta */
"<DA980>", /* Javanese candrabindu */
"<DA981>", /* Javanese anusvara */
#endif
"<DA982>", /* Javanese repha */
#ifdef NOTDEF
"<DA983>", /* Javanese visarga */
"<D1B80>", /* Sundanese anusvara */
#endif
"<D1B81>", /* Sundanese repha */
#ifdef NOTDEF
"<D1B82>", /* Sundanese visarga */
#endif
"<DABEC>", /* Meetei Mayek tone mark */
#ifdef NOTDEF
"<DA80B>", /* Syloti Nagri anusvara */
"<DA880>", /* Saurashtra anusvara */
"<DA881>", /* Saurashtra visarga */
"<DA8C5>", /* Saurashtra candrabindu */
"<D11000>", /* Brahmi candrabindu */
"<D11001>", /* Brahmi anusvara */
"<D11002>", /* Brahmi visarga */
"<D10A0D>", /* Kharoshthi double ring below */
"<D10A0E>", /* Kharoshthi anusvara */
"<D10A0F>", /* Kharoshthi visarga */
#endif
"<D10A38>", /* Kharoshthi bar above */
"<D10A39>", /* Kharoshthi cauda */
"<D10A3A>", /* Kharoshthi dot below */
#ifdef NOTDEF
"<D110BA>", /* Kaithi nukta */
"<D11080>", /* Kaithi candrabindu */
"<D11081>", /* Kaithi anusvara */
"<D11082>", /* Kaithi visarga */
"<D11100>", /* Chakma candrabindu */
"<D11101>", /* Chakma anusvara */
"<D11102>", /* Chakma visarga */
"<D11180>", /* Sharada candrabindu */
"<D11181>", /* Sharada anusvara */
"<D111C9>", /* Sharada sandhi mark */
#endif
"<D111CB>", /* Sharada vowel modifier */
"<D111CC>", /* Sharada extra short vowel mark */
#ifdef NOTDEF
"<D11182>", /* Sharada visarga */
"<D116AB>", /* Takri anusvara */
"<D116AC>", /* Takri visarga */
"<D116B7>", /* Takri nukta */
#endif
"<D11A98>", /* Soyombo gemination mark */
"<D0E4E>", /* Thai character yamakkan */
"<D0E47>", /* Thai character maitaikhu */
"<D0E48>", /* Thai character mai ek*/
"<D0E49>", /* Thai character mai tho */
"<D0E4A>", /* Thai character mai tri */
"<D0E4B>", /* Thai character mai chattawa */
"<D0E4C>", /* Thai character thanthakhat */
"<D0E4D>", /* Thai character nikhahit */
"<D0EC8>", /* Lao tone mai ek */
"<D0EC9>", /* Lao tone mai tho */
"<D0ECA>", /* Lao tone mai ti */
"<D0ECB>", /* Lao tone mai catawa */
"<D0ECC>", /* Lao cancellation mark */
"<D0ECD>", /* Lao niggahita */
"<DAABF>", /* Tai Viet mai ek */
"<DAAC1>", /* Tai Viet mai tho */
"<D0F39>", /* Tibetan mark tsa -phru */
#ifdef NOTDEF
"<D0F7E>", /* Tibetan anusvara */
"<D0F7F>", /* Tibetan visarga */
"<D11CB5>" /* Marchen anusvara */
"<D11CB6>" /* Marchen candrabindu */
"<D1C37>", /* Lepcha nukta */
#endif
"<DA92B>", /* Kayah Li tone plophu */
"<DA92C>", /* Kayah Li tone calya */
"<DA92D>", /* Kayah Li tone calya plophu */
#ifdef NOTDEF
"<D1036>", /* Myanmar anusvara */
#endif
"<D1037>", /* Myanmar dot below */
#ifdef NOTDEF
"<D1038>", /* Myanmar visarga */
"<D17C6>", /* Khmer sign nikahit */
"<D17C7>", /* Khmer sign reahmuk */
#endif
"<D17C8>", /* Khmer sign yuukaleapintu */
"<D17C9>", /* Khmer sign muusikatoan */
"<D17CA>", /* Khmer sign triisap */
#ifdef NOTDEF
"<D17CB>", /* Khmer sign bantoc */
"<D17CC>", /* Khmer sign robat */
"<D17CD>", /* Khmer sign toandakhiat */
"<D17CE>", /* Khmer sign kakabat */
"<D17CF>", /* Khmer sign ahsda */
"<D17D0>", /* Khmer sign samyok sannya */
"<D17D1>", /* Khmer sign viriam */
"<D17DD>", /* Khmer sign atthacan */
"<D1A74>", /* Tai Tham mai kang = bindi */
#endif
"<D1A75>", /* Tai Tham tone-1 */
"<D1A76>", /* Tai Tham tone-2 */
"<D1A77>", /* Tai Tham khuen tone-3 */
"<D1A78>", /* Tai Tham khuen tone-4 */
"<D1A79>", /* Tai Tham khuen tone-5 */
"<D1A7A>", /* Tai Tham ra haam */
"<D1A7B>", /* Tai Tham mai sam */
"<D1A7C>", /* Tai Tham khuen-lue karan */
"<D1939>", /* Limbu mukphreng */
"<D193A>", /* Limbu kemphreng */
"<D193B>", /* Limbu sa-i */
"<D16B30>", /* Pahawh Hmong cim tub */
"<D16B31>", /* Pahawh Hmong cim so */
"<D16B32>", /* Pahawh Hmong cim kes */
"<D16B33>", /* Pahawh Hmong cim khav */
"<D16B34>", /* Pahawh Hmong cim suam */
"<D16B35>", /* Pahawh Hmong cim hom */
"<D16B36>", /* Pahawh Hmong cim taum */
"<D1E2EC>", /* Wancho tone tup */
"<D1E2ED>", /* Wancho tone tup mang */
"<D1E2EE>", /* Wancho tone okoi */
"<D1E2EF>", /* Wancho tone okoi mang */

"<D302A>", /* ideographic level tone mark */
"<D302B>", /* ideographic rising tone mark*/
"<D302C>", /* ideographic departing tone mark*/
"<D302D>", /* ideographic centering tone mark*/
"<D302E>", /* Hangul single dot tone mark */
"<D302F>", /* Hangul double dot tone mark*/

"<D20D0>", /* left harpoon above */
"<D20D1>", /* right harpoon above */
#ifdef NOTDEF
"<D20D2>", /* long vertical line overlay */
#endif
"<D20D3>", /* short vertical line overlay */
"<D20D4>", /* anticlockwise arrow above */
"<D20D5>", /* clockwise arrow above */
"<D20D6>", /* left arrow above */
"<D20D7>", /* right arraw above */
#ifdef NOTDEF
"<D20D8>", /* ring overlay */
"<D20D9>", /* clockwise ring overlay */
"<D20DA>", /* anticlockwise ring overlay */
#endif
"<D20DB>", /* three dots above */
"<D20DC>", /* four dots above */
#ifdef NOTDEF
"<D20DD>", /* enclosing circle */
"<D20DE>", /* enclosing square */
"<D20DF>", /* enclosing diamond */
"<D20E0>", /* enclosing circle backslash */
#endif
"<D20E1>", /* left right arrow above */
#ifdef NOTDEF
"<D20E2>", /* enclosing screen */
"<D20E3>", /* enclosing keycap */
"<D20E4>", /* enclosing upward pointing triangle */
"<D20E5>", /* reverse solidus overlay */
#endif
"<D20E6>", /* double vertical stroke overlay */
"<D20E7>", /* annuity symbol */
"<D20E8>", /* triple underdot */
"<D20E9>", /* wide bridge above */
#ifdef NOTDEF
"<D20EA>", /* leftwards arrow overlay */
"<D20EB>", /* long double solidus overlay */
"<D20EC>", /* right harpoon with bar down*/
"<D20ED>", /* left harpoon with bar down */
"<D20EE>", /* left arrow below */
"<D20EF>", /* right arrow below */
"<D20F0>", /* asterisk above */
#endif
"<D101FD>", /* Phaistos disc combining oblique stroke */
/*
 * Secondary weights for variant marks created by hack using user-defined
 * characters.
 */
"<VRNT1>",
"<VRNT2>",
"<VRNT3>",
"<VRNT4>",
"<VRNT5>"
};

static utf32char secondSymVals[NUMSECONDSYMS] = {
/* Generic */
    0, 0x0332, 0x0313, 0x0314,
    0x0301, 0x0300,
    0x0306, 0x0302,
    0x030C, 0x030A, 0x0342, 0x0308,
    0x030B, 0x0303, 0x0307, 0x0338, 0x0327,
    0x0328, 0x0304,
/* User-defined characters used as generic diacritics, to allow fewer secondary weights. */
    0xF8F5, 0xF8F6, 0xF8F7, 0xF8F8,
/* Three high-frequency combining marks. */
    0x3099, 0x309A, 0x0335,
/* Continuation of generic combining marks */
    0x0305, 0x0309, /* 0x030D, 0x030E,*/ 0x030F, 0x0310, 0x0311,
    /* 0x0312, 0x0315, 0x0316, 0x0317, */
    /* 0x0318, 0x0319, 0x031A,*/ 0x031B, /* 0x031C, 0x031D,
    0x031E, 0x031F, 0x0320,*/ 0x0321, 0x0322, 0x0323,
    0x0324, 0x0325, 0x0326, /* 0x0329, 0x032A, 0x032B, 0x032C,*/ 0x032D, 0x032E,
    /* 0x032F,*/ 0x0330, 0x0331, /* 0x0333,*/ 0x0334, 
    /* 0x0336, 0x0337,*/ 0x0339, /* 0x033A,
    0x033B, 0x033C, 0x033D, 0x033E, 0x033F,*/ 0x0345,
    /* 0x0346, 0x0347, 0x0348, 0x0349, 0x034A, 0x034B, 0x034C, 0x034D, 0x034E,*/
    /* 0x0350, 0x0351, 0x0352, 0x0353, 0x0354, 0x0355, 0x0356, 0x0357,*/
    0x0358, /* 0x0359, 0x035A, 0x035B, 0x035C,
    0x035D, 0x035E, 0x035F,*/
    0x0360, 0x0361, /* 0x0362,*/
/* Generic extensions */
    /* 0x1DC0, 0x1DC1, 0x1DC2, 0x1DC3, 0x1DC4, 0x1DC5, 0x1DC6, 0x1DC7,
    0x1DC8, 0x1DC9, 0x1DCB, 0x1DCC, 0x1DCD, 0x1DCE, 0x1DCF, 0x1DD0, 0x1DD1,
    0x1DFE, 0x1DFE, 0x1DFF,*/
/* Coptic */
    /* 0x2CEF, 0x2CF0, 0x2CF1 */
/* Cyrillic */
    0x0483, /* 0x0484, 0x0485, 0x0486, 0x0487,*/ 0xA66F, /* 0xA67C, 0xA67D,*/
/* Hebrew */
    0x05B0, 0x05B1,
    0x05B2, 0x05B3, 0x05B4, 0x05B5, 0x05B6, 0x05B7, 0x05B8, /* 0x05C7,*/ 0x05B9, 
    /* 0x05BA,*/ 0x05BB, 
    0x05C2, 0x05C1, 0x05BC, 0x05BF, 0xFB1E,
/* Samaritan */
    0x081D, 0x0820, 0x0823, 0x0825, 0x0827, 0x082A, 0x082B, 0x082C,
    0x0818, 0x0819, 0x082D,
/* Arabic */
    0x064B, 0x08F0, 0x08E7,
    0x064C, 0x08F1, 0x08E8,
    0x064D, 0x08F2, 0x08E9,
    0x064E, 0x08E4, 0x08F4, 0x08F5,
    0x064F, 0x08E5, 0x08FE,
    0x0650, 0x08E6, 0x08F6,
    0x0651, 0x0652, 0x0653, 0x0654, 0x0655, 0x065F,
    0x0656, 0x0657, 0x0658, 0x08FF, 0x0659, 0x065A, 0x065B, 0x065C, 0x065D, 0x065E,
    0x08E3, 0x08F7, 0x08F8, 0x08FD, 0x08FB, 0x08FC, 0x08F9, 0x08FA,
    0x0670,
/* Syriac */
    0x0711, 0x0730, 0x0731, 0x0732, 0x0733, 0x0734, 0x0735, 0x0736, 0x0737, 0x0738,
    0x0739, 0x073A, 0x073B, 0x073C, 0x073D, 0x073E, 0x073F, 
    /* 0x0741, 0x0742, 0x0745, 0x0746, */
/* N'Ko */
    0x07EB, 0x07EC, 0x07ED, 0x07EE, 0x07EF, 0x07F0, 0x07F1, 0x07F2, 0x07F3,
/* Ethiopic */
    0x135F, 0x135E, 0x135D,
/* Bamum */
    0xA6F0, 0xA6F1,
/* Bassa Vah */
    0x16AF0, 0x16AF1, 0x16AF2, 0x16AF3, 0x16AF4,
/* Adlam */
    0x1E944, 0x1E94A, 0x1E947, 0x1E948, 0x1E949,
/* Devanagari */
    0x093C, 0x0901, 0x0902, 0x0903, /* 0x0951, 0x0952, 0x0953, 0x0954, */
/* Bengali */
    /* 0x09BC, 0x0981, 0x0982, 0x0983, */ 0x09FE,
/* Gurmukhi */
    /* 0x0A3C, 0x0A01, 0x0A02, 0x0A03, */ 0x0A70, 0x0A71,
/* Gujarati */
    /* 0x0ABC, 0x0A81, 0x0A82, 0x0A83, */
/* Oriya */
    /* 0x0B3C, 0x0B01, 0x0B02, 0x0B03, */
/* Tamil */
    /* 0x0B82, */
/* Telugu */
    /* 0x0C01, 0x0C02, 0x0C03, */
/* Kannada */
    /* 0x0CBC, 0x0C82, 0x0C83, */
/* Malayalam */
    /* 0x0D02, 0x0D03, */
/* Sinhala */
    /* 0x0D82, 0x0D83, */
/* Balinese */
    /* 0x1B34, 0x1B00, 0x1B01, 0x1B02, */ 0x1B03, /* 0x1B04, */
/* Javanese */
    /* 0x0A9B3, 0xA980, 0xA981, */ 0xA982, /* 0xA983, */
/* Sundanese */
    /* 0x1B80, */ 0x1B81, /* 0x1B82, */
/* Meetei Mayek */
    0xABEC,
/* Syloti Nagri, Saurashtra */
    /* 0xA80B, 0xA880, 0xA881, 0xA8C5 */
/* Brahmi */
    /* 0x11000, 0x11001, 0x11002, */
/* Kharoshthi */
    /* 0x10A0D, 0x10A0E, 0x10A0F, */ 0x10A38, 0x10A39, 0x10A3A,
/* Kaithi */
    /* 0x110BA, 0x11080, 0x11081, 0x11082, */
/* Chakma */
    /* 0x11100, 0x11101, 0x11102, */
/* Sharada */
    /* 0x11180, 0x11181, 0x11182, 0x111C9, */ 0x111CB, 0x111CD,
/* Takri */
    /* 0x116AB, 0x116AC, 0x116B7, */
/* Soyombo */
    0x11A98,
/* Thai, Lao, Tai View */
    0x0E4E, 0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D, 
    0x0EC8, 0x0EC9, 0x0ECA, 0x0ECB, 0x0ECC, 0x0ECD,
    0xAABF, 0xAAC1,
/* Tibetan */
    0x0F39, /* 0x0F7E, 0x0F7F, */
/* Marchen */
    /* 0x11CB5, 0x11CB6, */
/* Lepcha */
    /* 0x1C37, */
/* Kayah Li */
    0xA92B, 0xA92C, 0xA92D,
/* Myanmar */
    /* 0x1036, */ 0x1037, /* 0x1038, */
/* Khmer */
    /* 0x17C6, 0x17C7, */ 0x17C8, 0x17C9, 0x17CA, 
    /* 0x17CB, 0x17CC, 0x17CD, 0x17CE, 0x17CF, 0x17D0, 0x17D1, 0x17DD, */
/* Tai Tham */
    /* 0x1A74, */ 0x1A75, 0x1A76, 0x1A77, 0x1A78, 0x1A79, 0x1A7A, 0x1A7B, 0x1A7C,
/* Limbu */
    0x1939, 0x193A, 0x193B,
/* Pahawh Hmong */
    0x16B30, 0x16B31, 0x16B32, 0x16B33, 0x16B34, 0x16B35, 0x16B36,
/* Wancho */
    0x1E2EC, 0x1E2ED, 0x1E2EE, 0x1E2EF,
/* CJK */
    0x302A, 0x302B, 0x302C, 0x302D, 0x302E, 0x302F,
/* Symbols */
    0x20D0, 0x20D1, /* 0x20D2,*/ 0x20D3, 0x20D4, 0x20D5, 0x20D6, 0x20D7, 
    /* 0x20D8, 0x20D9, 0x20DA,*/ 0x20DB, 0x20DC, 
    /* 0x20DD, 0x20DE, 0x20DF, 0x20E0,*/ 0x20E1,
    /* 0x20E2, 0x20E3, 0x20E4, 0x20E5,*/ 0x20E6, 0x20E7, 0x20E8, 0x20E9, 
    /* 0x20EA, 0x20EB, 0x20EC, 0x20ED, 0x20EE, 0x20EF, 0x20F0,*/
/* Phaistos Disc symbol */
    0x101FD,
/* User-defined characters used to hack in variants for secondary weights. */
    0xF8F0, 0xF8F1, 0xF8F2, 0xF8F3, 0xF8F4
};

/******************************************************************/

/*
 * SECTION: Access to secondary and tertiary symbol arrays.
 */

static const char badValue[] = "BADVALUE";

static const char *GetTertSyms (int n)
{
    if ( ( n < 0 ) || ( n >= NUMTERTSYMS ) )
    {
        return ( badValue );
    }
    else
    {
        return ( tertSyms[n] );
    }
}

static const char *GetSecondSyms (int n)
{
int n2 = n - FIRST_SECONDARY;

    if ( ( n2 < 0 ) || ( n2 >= NUMSECONDSYMS ) )
    {
        return ( badValue );
    }
    else
    {
        return ( secondSyms[n2] );
    }
}

/*
 * Count the number of symbols in a level_token of the
 * form: "<AIGUT>", "<AIGUT><POINT>", etc.
 * Simply count the number of left angle brackets without doing
 * any other syntax processing.
 */

static int symbolCount ( const char *token )
{
const char *s;
int count;

    count = 0;
    for ( s = token; *s != '\0'; s++ )
    {
        if ( *s == '<' )
        {
            count++;
        }
    }
    return ( count );
}

/******************************************************************/

/*
 * SECTION: Dump prolog, collating-symbols, weights, & epilog to symbol file.
 */

void dumpTableProlog ( FILE *fd )
{
char localbuf[48];

    fputs ( "% escape_char /\n% comment_char %\n\n% LC_COLLATE\n\n", fd );
    fputs ( "% Decomment the lines above to create an\n", fd );
    fputs ( "%   LC_COLLATE definition in the style of ISO/IEC TR 14652:2004.\n\n", fd );
    fputs ( "%   Note that ISO/IEC TR 14652:2004 has been replaced by ISO/IEC TR 30112:2014.\n\n", fd );
    fputs ( "% Autogenerated Common Template Table\n", fd );
    sprintf ( localbuf, "%%   created from %s\n\n", GetUnidataFileName() );
    fputs ( localbuf, fd );
}

/*
 * Dump the list of collating symbols.
 */
void dumpCollatingSymbols ( FILE *fd )
{
int i;
int n;
char localbuf[120];

    fputs ( "% Declaration of collating symbols\n\n", fd );
    fputs ( "% Third-level collating symbols\n\n", fd );
    for ( i = 0; i < NUMTERTSYMS; i++ )
    {
/*
 * This hack is put in at the behest of Sweden, which asked that tertiary
 * weights not actually referred to in the Common Template Table also not be
 * printed in the list of tertiary collating-symbol definitions.
 *
 * 2001-Sep-19 Mark Davis asked for them to be added back, for consistency
 * between UCA and 14651, so I print them with an annotation.
 *
 * 2016-June-07 tweak after review by Marc Lodewijck.
 */
        if ( ( i >= FIRST_TERTIARY ) && ( i != SKEY_TWT_NOBREAK ) && ( i != 7 ) && ( i != LAST_TERTIARY ) )
        {
            sprintf ( localbuf, "collating-symbol %s\n", tertSyms[i] );
            fputs ( localbuf, fd );
        }
        else
        {
            sprintf ( localbuf, "collating-symbol %s  %% unused in table\n", tertSyms[i] );
            fputs ( localbuf, fd );
        }
    }
    fputs ( "\n% Second-level collating symbols\n\n", fd );
    for ( i = 0; i < NUMSECONDSYMS; i++ )
    {
    /*
     * For printing the symbols to be weighted, check for list_token
     * containing more than one symbol. These get commented in the list,
     * since they are not weighted as units themselves.
     */
        n = symbolCount ( secondSyms[i] );
        if ( n > 1 )
        {
            sprintf ( localbuf, "%% \"%s\"", secondSyms[i] );
        }
        else
        {
            sprintf ( localbuf, "collating-symbol %s", secondSyms[i] );
        }
        fputs ( localbuf, fd );
        if ( secondSymVals[i] != 0 )
        {
        WALNUTPTR tmp;
        int ii = (int)secondSymVals[i];

            tmp = getSiftDataPtr ( ii );
            if ( tmp->name == NULL )
            {
                fputs ( "% NUMBERING ERROR!!!\n", fd );
            }
            else
            {
                sprintf ( localbuf, "  %% %s\n", tmp->name );
                fputs ( localbuf, fd );
            }
        }
        else
        {
            fputs ( "\n", fd );
        }
    }
    fputs ( "\n% First-level collating symbols\n\n", fd );
    fputs ( "collating-symbol <S0009>..<S327F> % Alphabetics, syllabics, general symbols\n", fd );
    fputs ( "collating-symbol <S4DC0>..<S4DFF> % Yijing hexagram symbols\n", fd );
    fputs ( "collating-symbol <SA000>..<SABFF> % Alphabetics, syllabics, general symbols\n", fd );
    fputs ( "collating-symbol <SFD3E>..<SFD3F> % Ornate parentheses (Arabic)\n", fd );
    fputs ( "collating-symbol <SFDFD>          % Bismillah symbol (Arabic)\n", fd );
    fputs ( "collating-symbol <SFE45>..<SFE46> % Sesame dots\n", fd );
    fputs ( "collating-symbol <SFFFC>..<SFFFD> % Specials\n", fd );
    fputs ( "collating-symbol <SAC00>..<SD7A3> % Symbols for Hangul syllables (weights must be constructed)\n", fd );
    fputs ( "collating-symbol <SD7B0>..<SD7FB> % Hangul Jamo\n", fd );
    fputs ( "collating-symbol <RFB00>          % Symbol for first element of computed weights for Tangut ideographs and components\n", fd );
    fputs ( "collating-symbol <RFB01>          % Symbol for first element of computed weights for Nushu ideographs\n", fd );
    fputs ( "collating-symbol <RFB40>..<RFB41> % Symbols for first element of computed weights for core Han unified ideographs\n", fd );
    fputs ( "collating-symbol <RFB80>          % Symbol for first element of computed weights for Han unified ideographs Ext-A\n", fd );
    fputs ( "collating-symbol <RFB84>..<RFB85> % Symbols for first element of computed weights for Han unified ideographs Ext-B, ...\n", fd );
    fputs ( "collating-symbol <RFBC0>..<RFBE1> % Symbols for first element of computed weights for unassigned characters and others\n", fd );
    fputs ( "collating-symbol <T8000>..<TFFFF> % Symbols for second element of computed weights for Han, Tangut, Nushu and others\n", fd );
    fputs ( "collating-symbol <S10000>..<S12543> % Alphabetics from SMP\n", fd );
    fputs ( "collating-symbol <S13000>..<S1343F> % Alphabetics from SMP\n", fd );
    fputs ( "collating-symbol <S14400>..<S14646> % Alphabetics from SMP\n", fd );
    fputs ( "collating-symbol <S16800>..<S16FFF> % Alphabetics and symbols from SMP\n", fd );
    fputs ( "collating-symbol <S1B000>..<S1BCAF> % Alphabetics and symbols from SMP\n", fd );
    fputs ( "collating-symbol <S1D000>..<S1D37F> % Symbols from SMP\n", fd );
    fputs ( "collating-symbol <S1D800>..<S1DA8B> % Sutton SignWriting\n", fd );
    fputs ( "collating-symbol <S1E800>..<S1E95F> % Alphabetics from SMP\n", fd );
    fputs ( "collating-symbol <S1EC70>..<S1EEFF> % Symbols from SMP\n", fd );
    fputs ( "collating-symbol <S1F000>..<S1FAFF> % Symbols from SMP\n\n", fd );
    fputs ( "collating-symbol <SFFFF> % Guaranteed largest symbol value, used as a special fourth-level collating symbol\n\n", fd );
    fputs ( "% Keep <SFFFF> at the end of this list.\n\n", fd );
#ifdef NOTDEF
    fputs ( "collating-symbol <PLAIN> % Maximal level 4 weight\n\n", fd );
#endif
}

/*
 * Dump all the symbolic weights themselves.
 */

void dumpWeights ( FILE *fd )
{
int i;
int n;
char localbuf[120];

    fputs ( "\n% Symbolic weight assignments\n\n", fd );
    fputs ( "% Third-level weight assignments\n\n", fd );
    for ( i = 0; i < NUMTERTSYMS; i++ )
    {
/*
 * This hack is put in at the behest of Sweden, which asked that tertiary
 * weights not actually referred to in the Common Template Table also not be
 * printed in the list of tertiary collating-symbol definitions.
 *
 * 2001-Sep-19 Mark Davis asked for them to be added back, for consistency
 * between UCA and 14651, so I print them with an annotation.
 *
 * 2016-June-07 tweak after review by Marc Lodewijck.
 */
        if ( ( i >= FIRST_TERTIARY ) && ( i != SKEY_TWT_NOBREAK ) && ( i != 7 ) && ( i != LAST_TERTIARY ) )
        {
            sprintf ( localbuf, "%s\n", tertSyms[i] );
            fputs ( localbuf, fd );
        }
        else
        {
            sprintf ( localbuf, "%s  %% unused in table\n", tertSyms[i] );
            fputs ( localbuf, fd );
        }
    }
    fputs ( "\n% Second-level weight assignments\n\n", fd );
    for ( i = 0; i < NUMSECONDSYMS; i++ )
    {
    /*
     * For printing the symbols to be weighted, check for list_token
     * containing more than one symbol. These are omitted in this list,
     * since they are not weighted as units themselves.
     */
        n = symbolCount ( secondSyms[i] );
        if ( n == 1 )
        {
            sprintf ( localbuf, "%s", secondSyms[i] );
            fputs ( localbuf, fd );
            if ( secondSymVals[i] != 0 )
            {
            WALNUTPTR tmp;
            int ii = (int)secondSymVals[i];
    
                tmp = getSiftDataPtr ( ii );
                if ( tmp->name == NULL )
                {
                    fputs ( "% NUMBERING ERROR!!!\n", fd );
                }
                else
                {
                    sprintf ( localbuf, "  %% %s\n", tmp->name );
                    fputs ( localbuf, fd );
                }
            }
            else
            {
                fputs ( "\n", fd );
            }
        }
    }
    fputs ( "\n% First-level weight assignments\n\n", fd );
}

void dumpEntriesHeader ( FILE *fd )
{
    fputs ( "\n% order_start forward;forward;forward;forward,position\n\n", fd );
    fputs ( "% order_start forward;backward;forward;forward,position\n\n", fd );
    fputs ( "% Decomment the first order_start line to specify directions for each level.\n", fd );
    fputs ( "%   To tailor for French accent handling, instead decomment the second\n", fd );
    fputs ( "%   order_start statement.\n\n", fd );
    fputs ( "% Note: The following list of symbol_element's has been generated in\n", fd );
    fputs ( "%   a roughly sorted order, to assist in understanding the string ordering that\n", fd );
    fputs ( "%   results from use of this Common Template Table.\n", fd );
    fputs ( "%   This rough order is most meaningful for characters which do not have\n", fd );
    fputs ( "%   IGNORE for the first three weight levels.\n\n", fd );
    fputs ( "% <Uxxxx> <Base>;<Accent>;<Case>;<Special>\n\n", fd );
}

/*
 * dumpTableEpilog
 *
 * The table epilog is now just grabbed from a text file cttfooter.txt,
 * which is assumed to be in the same directory.
 *
 * This makes for easier editing of the contents of the footer.
 */

void dumpTableEpilog ( FILE *fd )
{
char localbuf[128];
FILE *fdi;

    fdi = fopen ( "cttfooter.txt", "rt" );
    if ( fdi == NULL )
    {
        printf ( "Cannot open cttfooter.txt.\n" );
    }
    while ( fgets( localbuf, 128, fdi ) != NULL )
    {
        fputs ( localbuf, fd );
    }
    fclose ( fdi );
}


/******************************************************************/

/*
 * SECTION: Construct symbolic entries for the table, and insert into BTree.
 */

/*
 * SwapLong()
 *
 * Utility for byte-swapping longs.
 */
#ifdef BYTESWAP
static void SwapLong ( UInt32 *data, int numlongs )
{
  UInt32 *dptr;
  int n = numlongs;

  for (dptr = data; n > 0; n--, dptr++)
  {
    *dptr = ( *dptr << 24 ) |
          ( ( *dptr << 8 ) & 0x00FF0000 ) |
          ( ( *dptr >> 8 ) & 0x0000FF00 ) |
          ( ( *dptr >> 24 ) & 0x000000FF ) ;
  }
}
#endif

/*
 * assembleWeightString
 *
 * Local routine to construct the weight string for a symbol.
 * This is for the CTT table output.
 *
 * Major rework 2017-Mar-08 to address inconsistencies in the
 * handling of 4th level weights, to better synch CTT with the
 * effect of use of the shifted option for variable weighting
 * of the values in DUCET.
 */
static void assembleWeightString ( char* buf, WALNUTPTR t1 )
{
char wt1[10];
char wt2[50];
char wt3[20];
char wt4[20];

    if ( ( t1->level1 == 0 ) || ( t1->variable ) )
    {
        strcpy ( wt1, "IGNORE" );
    }
    else
    {
        sprintf ( wt1, "<S%04X>", t1->symbolBase );
    }
    if ( ( t1->level2 == 0 ) || ( t1->variable ) )
    {
        strcpy ( wt2, "IGNORE" );
    }
    else
    {
        wt2[49] = '\0';
        if ( ( t1->level1 == 0 ) || ( t1->level2 == FIRST_SECONDARY ) )
/*
 * We are dealing with a combining mark, or a letter without an
 * accent at the second level, or a digit. Don't add an extra
 * <BASE> at the front of the symbol.
 */
        {
            strncpy ( wt2, GetSecondSyms( t1->level2 ), 49 );
        }
        else
/*
 * All other cases should be dealt with by assembleCompositeSym.
 */
        {
            printf ( "Bad value in assembleSym: %s\n", t1->value );
        }
    }
    if ( ( t1->level3 == 0 ) || ( t1->variable ) )
    {
        strcpy ( wt3, "IGNORE" );
    }
    else
    {
        wt3[19] = '\0';
        strncpy ( wt3, GetTertSyms( t1->level3 ), 19 );
    }
#ifdef NOTDEF
    /* Old implementation for UCA 9.0 and earlier. */
    if ( t1->level4 == 0 )
    {
        sprintf ( buf, " %s;%s;%s;IGNORE %% %s\n", wt1, wt2, wt3, t1->name );
    }
    else
    {
        sprintf ( buf, " %s;%s;%s;<U%s> %% %s\n", wt1, wt2, wt3, t1->value, t1->name );
    }
#endif
    if ( t1->level4 == 0 )
    {
        strcpy ( wt4, "IGNORE" );
    }
    else if ( ( t1->level1 == 0 ) && ( t1->level2 == 0 ) && ( t1->level3 == 0 ) )
    {
        strcpy ( wt4, "IGNORE" );
    }
    else if ( t1->variable ) 
    {
    /* Handle as the shifted case. Move the variable primary weight to level 4. */
        sprintf ( wt4, "<S%04X>", t1->symbolBase );
    }
    else
    {
    /* Artificial high 4th-level weight appended. */
        strcpy ( wt4, "<SFFFF>"); 
    }
    sprintf ( buf, " %s;%s;%s;%s %% %s\n", wt1, wt2, wt3, wt4, t1->name );
}

/*
 * assembleKey
 *
 * Local routine to construct a sortkey based on the weights in t1.
 */
static void assembleSymKey ( WALNUTPTR t1, UInt32* keybufptr )
{
UInt32* bufptr;
int numkeys;

    bufptr = keybufptr;
    numkeys = 0;
    /*
     * To create variable keys, stick three 0's on the front of any
     * variable character. This will correctly sort them ahead of
     * the combining marks, which are getting constructed as 0 - X - 2 - 1 - U
     */
    if ( !t1->variable )
    {
        if ( t1->level1 != 0 )
        {
            *bufptr++ = t1->level1;
            numkeys++;
        }
        if ( t1->level2 != 0 )
        {
            *bufptr++ = t1->level2;
            numkeys++;
        }
        if ( t1->level3 != 0 )
        {
            *bufptr++ = t1->level3;
            numkeys++;
        }
    }
    *bufptr++ = IGN_DELIM;
    numkeys++;
    *bufptr++   = t1->level4;
    numkeys++;
/*
 * For values that are zero all the way across, append a fifth
 * level based on the code value to produce a defined order in
 * the tree and table.
 */
    if ( ( t1->variable ) && ( t1->level4 == 0 ) )
    {
        *bufptr++ = t1->uvalue;
        numkeys++;
    }
#ifdef BYTESWAP
    /*
     * On Intel platforms, swap the shorts in the key, so that the
     * memcmp operation for the comparison gets the bytes in the right
     * order.
     */
    SwapLong ( keybufptr, numkeys );
#endif
}

/*
 * assembleSym
 *
 * Construct a symbol key for an atomic unit (including
 * contractions) and insert in the BTree.
 *
 * symbol is an already assembled symbolic name for the element.
 */
void assembleSym ( WALNUTPTR t1, char* symbol, char* ucastr )
{
char buf1[256];
char buf2[256];
UInt32 keybuf[5];
int d;

    assembleWeightString ( buf1, t1 );
    strcpy ( buf2, symbol );
    strcat ( buf2, buf1 );
    /*
     * Now construct a sort key value to use in the tree insertion.
     */

    assembleSymKey ( t1, keybuf );
    /*
     * Now insert the whole shebang into the BTree storing everything.
     *
     * The greatestDepth diagnostic is to check for grossly unbalanced
     * trees. The greatestDepth for the 6.2.0 datafile is 5470.
     */
    d = unisift_InsertSym ( keybuf, 5, buf2, ucastr );
    if ( d > greatestDepth )
    {
#ifdef NOTDEF
        printf ( "depth = %d for %04X\n", d, t1->level4 );
#endif
        greatestDepth = d;
    }
    else if ( d == -1 )
    {
        printf( "Insertion failure (atomic/contraction): %s\n", t1->value );
    }
}


/*
 * assembleThaiContractionSym
 *
 * Construct a symbol key for a Thai reordering contraction and insert in the BTree.
 *
 * symbol is an already assembled symbolic name for the contraction.
 */
int assembleThaiContractionSym ( UInt32 consonant, UInt32 vowel, char* symbol, char* ucastr )
{
char buf[256];
UInt32 keybuf[8];
char wt1[22];
char wt2[22];
char wt3[22];
WALNUTPTR t1;
WALNUTPTR t2;
int d;

    t1 = getSiftDataPtr (consonant) ;
    t2 = getSiftDataPtr (vowel) ;

/*
 * Mark the second element with the LastTertiary weight (FillerToken)
 * to distinguish it.
 */
    sprintf ( wt1, "\"<S%04X><S%04X>\"", t1->symbolBase, t2->symbolBase );
    sprintf ( wt2, "\"%s%s\"", GetSecondSyms( t1->level2 ), GetSecondSyms( t2->level2 ) );
    sprintf ( wt3, "\"%s%s\"", GetTertSyms( t1->level3 ), GetTertSyms( t2->level3 ) );
    sprintf ( buf, "%s %s;%s;%s;\"<SFFFF><SFFFF>\" %% <%s, %s>\n", symbol, wt1, wt2, wt3, 
                   /* t1->value, t2->value, */ t2->name, t1->name );    
    /*
     * Now construct a sort key value to use in the tree insertion.
     * Just do this manually here, since the values for the Thai
     * reorderings are all well-known ahead of time. This doesn't
     * have to be general.
     */

    keybuf[0] = t1->level1;
    keybuf[1] = t2->level1;
    keybuf[2] = t1->level2;
    keybuf[3] = t2->level2;
    keybuf[4] = t1->level3;
    keybuf[5] = t2->level3;
    keybuf[6] = t1->level4;
    keybuf[7] = t2->level4;
#ifdef BYTESWAP
    /*
     * On Intel platforms, swap the longs in the key, so that the
     * memcmp operation for the comparison gets the bytes in the right
     * order.
     */
    SwapLong ( keybuf, 8 );
#endif


    /*
     * Now insert the whole shebang into the BTree storing everything.
     */
    d = unisift_InsertSym ( keybuf, 8, buf, ucastr );
    if ( d > greatestDepth )
    {
#ifdef NOTDEF
        printf ( "depth = %d for %04X\n", d, t1->level4 );
#endif
        greatestDepth = d;
    }
    else if ( d == -1 )
    {
        printf( "Insertion failure (Thai contraction): %08X %08X\n", consonant, vowel );
    }

    return ( 0 );
}

/*
 * assembleLaoDigraphContractionSym
 *
 * Construct a symbol key for a Lao digraph reordering contraction and insert in the BTree.
 *
 * symbol is an already assembled symbolic name for the contraction.
 */
int assembleLaoDigraphContractionSym ( UInt32 consonant, UInt32 vowel, 
                                       char* symbol, char* ucastr )
{
char buf[356];
UInt32 keybuf[12];
char wt1[26];
char wt2[26];
char wt3[26];
WALNUTPTR t1;
WALNUTPTR t2;
WALNUTPTR t3;
WALNUTPTR t4;
UInt32 c2;
int d;

    if ( consonant == 0x0EDC )
    {
        c2 = 0x0E99;
    }
    else
    {
        c2 = 0x0EA1;
    }
    t1 = getSiftDataPtr (0x0EAB) ;
    t2 = getSiftDataPtr (c2) ;
    t3 = getSiftDataPtr (vowel) ;
    t4 = getSiftDataPtr (consonant);

    sprintf ( wt1, "\"<S%04X><S%04X><S%04X>\"", t1->symbolBase, t2->symbolBase, t3->symbolBase );
    sprintf ( wt2, "\"%s%s%s\"", GetSecondSyms( t1->level2 ), GetSecondSyms( t2->level2 ),
                             GetSecondSyms( t3->level2 ) );
    sprintf ( wt3, "\"%s%s%s\"", GetTertSyms( SKEY_TWT_COMPAT ), GetTertSyms( SKEY_TWT_COMPAT ),
                             GetTertSyms( SKEY_TWT_SMALL ) );
    sprintf ( buf, "%s %s;%s;%s;\"<SFFFF><SFFFF><SFFFF>\" %% <%s, %s>\n", symbol, wt1, wt2, wt3, 
                   /* t4->value, t3->value, */ t3->name, t4->name );    
    /*
     * Now construct a sort key value to use in the tree insertion.
     * Just do this manually here, since the values for the Lao
     * reorderings are all well-known ahead of time. This doesn't
     * have to be general.
     */

    keybuf[0] = t1->level1;
    keybuf[1] = t2->level1;
    keybuf[2] = t3->level1;
    keybuf[3] = t1->level2;
    keybuf[4] = t2->level2;
    keybuf[5] = t3->level2;
    keybuf[6] = SKEY_TWT_COMPAT;
    keybuf[7] = SKEY_TWT_COMPAT;
    keybuf[8] = SKEY_TWT_SMALL;
    keybuf[9] = t4->level4;
    keybuf[10] = t4->level4;
    keybuf[11] = t3->level4;
#ifdef BYTESWAP
    /*
     * On Intel platforms, swap the longs in the key, so that the
     * memcmp operation for the comparison gets the bytes in the right
     * order.
     */
    SwapLong ( keybuf, 12 );
#endif


    /*
     * Now insert the whole shebang into the BTree storing everything.
     */
    d = unisift_InsertSym ( keybuf, 12, buf, ucastr );
    if ( d > greatestDepth )
    {
#ifdef NOTDEF
        printf ( "depth = %d for %04X\n", d, t1->level4 );
#endif
        greatestDepth = d;
    }
    else if ( d == -1 )
    {
        printf( "Insertion failure (Lao digraph contraction): %08X %08X\n", 
                consonant, vowel );
    }

    return ( 0 );
}



/*
 * assembleCompositeSym
 *
 * Assemble a composite symbol key string for a compatibility or canonical
 * sequence of more than one collation element, and insert it into
 * the BTree.
 *
 * This is now calculated based on the rkeys vector.
 */
int assembleCompositeSym ( WALNUTPTR t1, char *symbol, RAWKEYPTR rkeys, int numrkeys,
    char* ucastr )
{
int i;
char buf[800];
char buf2[512];
char tempbuf[52];
UInt32 keybuf[81]; /* 4 * 20 + 1 */
UInt32* bufptr;
RAWKEYPTR rkptr;
int d;
int expand;
int count;
int save2count;
int numkeys;
int hitimplicitbase;

    strcpy ( buf, symbol );
    strcat ( buf, " " );
    expand = 0;
/*
 * First check if all the elements of the decomposition are themselves
 * marked as variable. If so, then the weights will be printed
 * as IGNORE;IGNORE;IGNORE, as for single character ignorables.
 * Otherwise, we have to print the primary, secondary, and tertiary
 * symbols for any non-ignorable part of the decomposition, so that
 * the weighting of the composite unit is the same as is the
 * weighting of the decomposed pieces taken separately.
 */
#ifdef NOTDEF
    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        if ( !rkptr->variable )
        {
            expand = 1;
        }
    }
#endif
/*
 * For UCA 10.0 and later, just always expand. The handling
 * of variables drops through, and the 4th level is handled
 * as part of the fourth level loop below.
 *
 * Later: This code can be cleaned up for 11.0.
 */
    expand = 1;
    if ( !expand )
    {
        strcat ( buf, "IGNORE;IGNORE;IGNORE;" );
        /* UCA 10.0 and later. Append a shifted value of level 1.
         * Symbolically, this is the S symbol of the first element
         * of the decomp
         */
        sprintf ( tempbuf, "<S%04X>", t1->symbolBase );
        strcat ( buf, tempbuf );
    }
    else  /* expand */
    {
/* Print the primary weight symbols for non-ignorables. */
        buf2[0] = '\0';
        count = 0;
        hitimplicitbase = 0;
        for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
        {
            if ( ( !rkptr->variable ) && ( rkptr->level1 > 0 ) )
            {
                if ( hitimplicitbase )
                {
                /*
                 * Special symbol handling for second element of
                 * computed implicit weights.
                 */
                    sprintf ( tempbuf, "<T%04X>", rkptr->symbolBase );
                    hitimplicitbase = 0;
                }
                else if ( ( rkptr->level1 >= 0xFB40 ) && ( rkptr->level1 <= 0xFBFF ) )
                {
                    sprintf ( tempbuf, "<R%04X>", rkptr->symbolBase );
                    hitimplicitbase = 1;
                }
                else
                {
                    sprintf ( tempbuf, "<S%04X>", rkptr->symbolBase );
                }
                strcat ( buf2, tempbuf );
                count++;
            }
        }
        if ( count == 0 )
        {
            strcat ( buf, "IGNORE" );
        }
        else
        {
/*
 * Check and surround by quotes if there is a multiple symbol.
 */
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
            strcat ( buf, buf2 );
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
        }
        strcat ( buf, ";" );
/* Print the secondary weight symbols for non-ignorables. */
        buf2[0] = '\0';
        count = 0;
        for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
        {
            if ( ( !rkptr->variable ) && ( rkptr->level2 > 0 ) )
            {
            const char *ss;
            int  n;
/*
 * At the secondary level, some of the symbols accessed from
 * the secondSyms table are already composite, with up to three
 * symbols. Since we need to deal with the case of quoting these,
 * even if there is only one CE (i.e., numrkeys == 1), actually
 * count up the number of symbols explicitly.
 */
                ss = GetSecondSyms ( rkptr->level2 );
                n = symbolCount ( ss );
                count += n;
/*
 * Check whether we are dealing with a non-base secondary weight,
 * for a character which was not ignorable at the first level,
 * and if so, prepend an extra <BASE> weight.
 * Note that all the virtual diacritics weighted above the real ones
 * behave in this regard just like real diacritics. They get a <BASE>
 * weight, too.
 */
                if ( ( rkptr->level2 > FIRST_SECONDARY ) &&
                     ( rkptr->level1 != 0 ) )
                {
                    count++;
                    strcat ( buf2, "<BASE>" );
                }
                strcat ( buf2, ss );
            }
        }
        if ( count == 0 )
        {
            strcat ( buf, "IGNORE" );
        }
        else
        {
/*
 * Check and surround by quotes if there is a multiple symbol.
 */
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
            strcat ( buf, buf2 );
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
        }
        strcat ( buf, ";" );
/* Print the tertiary weight symbols for non-ignorables. */
        buf2[0] = '\0';
        save2count = count;
        count = 0;
        for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
        {
            if ( ( !rkptr->variable ) && ( rkptr->level3 > 0 ) )
            {
                count++;
                strcat ( buf2, GetTertSyms( rkptr->level3 ) );
            }
/*
 * Check whether we are dealing with a non-base secondary weight,
 * for a character which was not ignorable at the first level,
 * and if so, append an extra <MIN> weight.
 * Note that all the virtual diacritics weighted above the real ones
 * behave in this regard just like real diacritics. They get a <BASE>
 * weight, too.
 */
            if ( ( rkptr->level2 > FIRST_SECONDARY ) &&
                 ( rkptr->level1 != 0 ) )
            {
                count++;
                strcat ( buf2, "<MIN>" );
            }
        }
        if ( count == 0 )
        {
            strcat ( buf, "IGNORE" );
        }
        else
        {
/*
 * Check and surround by quotes if there is a multiple symbol.
 */
            if ( save2count > 1 )
            {
                strcat ( buf, "\"" );
            }
            strcat ( buf, buf2 );
/*
 * Now for instances where the secondary weight_token has
 * has been built up from composite diacritics,
 * append enough <MIN> symbols to expand the third level to
 * the same number of symbols.
 */
            if ( count < save2count )
            {
                for ( i = 0; i < save2count - count; i++ )
                {
                    strcat ( buf, "<MIN>" );
                }
            }
            if ( save2count > 1 )
            {
                strcat ( buf, "\"" );
            }
        }
        strcat ( buf, ";" );
        /* 
         * UCA 10.0 and later. Level 4 
         * If the element is not variable, just use <SFFFF>.
         * Otherwise if it is variable and teh symbolBase is not 0,
         * use the symbolBase value.
         */
/* Print the fourth level weight symbols for non-ignorables. */
        buf2[0] = '\0';
        count = 0;
        for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
        {
            if ( ( !rkptr->variable ) && ( rkptr->level3 > 0 ) )
            {
                count++;
                strcat ( buf2, "<SFFFF>" );
            }
            else if ( ( rkptr->variable ) && ( rkptr->symbolBase > 0 ) )
            {
                count++;
                sprintf ( tempbuf, "<S%04X>", rkptr->symbolBase );
                strcat ( buf2, tempbuf );
            }
        }
        if ( count == 0 )
        {
            strcat ( buf, "IGNORE" );
        }
        else
        {
/*
 * Check and surround by quotes if there is a multiple symbol.
 */
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
            strcat ( buf, buf2 );
            if ( count > 1 )
            {
                strcat ( buf, "\"" );
            }
        }
    }

/* Print the Unicode values, as a quoted string, in any case. */
/*
 * Use the level 4 assignments in the keys, which derive from
 * the original value of the Unicode character being weighted,
 * and which are thus guaranteed to be unique.
 *
 * The alternative approach, of appending the full
 * decomposition, creates massive duplication of keys when the
 * first three levels are ignored, as required for all the
 * specials in 14651.
 */
/* UCA 9.0 and earlier */
#ifdef NOTDEF
    sprintf ( tempbuf, "<U%s>", t1->value );
    strcat ( buf, tempbuf );
#endif

#ifdef NOTDEF
    buf2[0] = '\0';
    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        sprintf ( tempbuf, "<U%04X>", rkptr->level4 );
        strcat ( buf2, tempbuf );
    }
    if ( numrkeys > 1 )
    {
        strcat ( buf, "\"" );
    }
    strcat ( buf, buf2 );
    if ( numrkeys > 1 )
    {
        strcat ( buf, "\"" );
    }
#endif
/* Append the character name and linefeed. */
    strcat ( buf, " % " );
    strcat ( buf, t1->name );
    strcat ( buf, "\n" );
    /*
     * Now construct a sort key value to use in the tree insertion.
     */
    bufptr = keybuf;
    /*
     * To create variable keys, stick three 0's on the front of any
     * variable character. This will correctly sort them in with the
     * non-composite ignorables:
     *     0 - 0 - 0 - 1 - U
     * composite ignorables:
     *     0 - 0 - 0 - 1 - U U U...
     */
    numkeys = 0;

    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        if ( ( !rkptr->variable ) && ( rkptr->level1 != 0 ) )
        {
            *bufptr++ = rkptr->level1;
            numkeys++;
        }
    }
    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        if ( !rkptr->variable )
        {
            *bufptr++ = rkptr->level2;
            numkeys++;
        }
    }
    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        if ( !rkptr->variable )
        {
            *bufptr++ = rkptr->level3;
            numkeys++;
        }
    }

    *bufptr++ = IGN_DELIM;
    numkeys++;
    *bufptr++ = t1->level4;
    numkeys++;
#ifdef NOTDEF
    for ( rkptr = rkeys, i = 0; i < numrkeys; rkptr++, i++ )
    {
        *bufptr++ = rkptr->level4;
        numkeys++;
    }
#endif

#ifdef BYTESWAP
    /*
     * On Intel platforms, swap the longs in the key, so that the
     * memcmp operation for the comparison gets the bytes in the right
     * order.
     */
    SwapLong ( keybuf, numkeys );
#endif

    /*
     * Now insert the whole shebang into the BTree storing everything.
     *
     * unisift_InsertSym will return -1 for a failed insertion due to
     * duplication.
     */
    d = unisift_InsertSym ( keybuf, numkeys, buf, ucastr );
    if ( d > greatestDepth )
    {
#ifdef NOTDEF
        printf ( "depth = %d for %04X\n", d, t1->level4 );
#endif
        greatestDepth = d;
    }
    else if ( d == -1 )
    {
        printf( "Insertion failure (composite): %s\n", t1->value );
    }
    return ( d );
}

/******************************************************************/

/*
 * SECTION: BTree insertion and traversal for full keys.
 */

static PSYMTREENODE unisift_NewNode ( void )
{
PSYMTREENODE p;

    p = (PSYMTREENODE)malloc( sizeof ( SYMTREENODE ) );
    if ( p == NULL )
    {
        return ( NULL );
    }
    p->left = NULL;
    p->right = NULL;
    p->key = NULL;
    p->keylen = 0;
    p->thestrings = NULL;
    p->theucastrings = NULL;
    return ( p );
}

static UInt32 *unisift_NewKey ( UInt32 *k, int keylen )
{
UInt32 *p;

    p = (UInt32 *)malloc( 4 * keylen );
    if ( p == NULL )
    {
        return ( NULL );
    }
    memcpy ( p, k, 4 * keylen );
    return ( p );
}

static PSTRLISTNODE unisift_NewString ( char *s )
{
char *p;
PSTRLISTNODE p2;

    p = (char *)malloc( strlen ( s ) + 1 );
    if ( p == NULL )
    {
        return ( NULL );
    }
    p2 = (PSTRLISTNODE)malloc( sizeof ( STRLISTNODE ) );
    if ( p2 == NULL )
    {
        free ( p );
        return ( NULL );
    }
    strcpy ( p, s );
    p2->outputstring = p;
    p2->next = NULL;
    return ( p2 );
}

static int unisift_CompareKey ( UInt32 *k1, UInt32 *k2, int k1len,
    int k2len )
{
int tlen;
int rc;

    tlen = ( k1len < k2len ) ? 4 * k1len : 4 * k2len ;
    rc = memcmp ( k1, k2, tlen );
    if ( rc == 0 )
    {
        if ( k1len < k2len )
        {
            return ( -1 );
        }
        else if ( k1len > k2len )
        {
            return ( 1 );
        }
        else
        {
            return ( 0 );
        }
    }
    else
    {
        return ( rc );
    }
}

/*
 * Append PSTRLISTNODE at end of a linked list, and return length of the list.
 */

static int unisift_AppendStr ( PSTRLISTNODE p, PSTRLISTNODE p2 )
{
PSTRLISTNODE temp;
int k;

    temp = p;
    k = 1;
    while ( temp->next != NULL )
    {
        temp = temp->next;
        k++;
    }
    temp->next = p2;
    return ( k );
}

/*
 * Insert entry in BTree.
 */

static int unisift_InsertSym ( UInt32 *key, int keylen, char *s,
                               char* ucastr )
{
PSYMTREENODE p;
PSTRLISTNODE p2;
int depth;
int listlen;

    p = symTreeRoot;
    depth = 0;
/*
 * Deal with setting root for very first entry.
 */
    if ( p == NULL )
    {
        p = unisift_NewNode();
        if ( p == NULL )
        {
            return ( -1 );
        }
        symTreeRoot = p;
        depth = 1;
        p->key = unisift_NewKey ( key, keylen );
        p->keylen = keylen;
        p->thestrings = unisift_NewString ( s );
        p->theucastrings = unisift_NewString ( ucastr );
        return ( depth );
    }
    while ( 1 )
    {
    int comp;

        comp = unisift_CompareKey ( key, p->key, keylen, p->keylen );
        if ( comp < 0 )
        {
            if ( p->left == NULL )
            {
                p->left = unisift_NewNode();
                if ( p->left == NULL )
                {
                    return ( -1 );
                }
                p = p->left;
                p->key = unisift_NewKey ( key, keylen );
                p->keylen = keylen;
                p->thestrings = unisift_NewString ( s );
                p->theucastrings = unisift_NewString ( ucastr );
                return ( depth );
            }
            else
            {
                p = p->left;
                depth++;
            }
        }
        else if ( comp > 0 )
        {
            if ( p->right == NULL )
            {
                p->right = unisift_NewNode();
                if ( p->right == NULL )
                {
                    return ( -1 );
                }
                p = p->right;
                p->key = unisift_NewKey ( key, keylen );
                p->keylen = keylen;
                p->thestrings = unisift_NewString ( s );
                p->theucastrings = unisift_NewString ( ucastr );
                return ( depth );
            }
            else
            {
                p = p->right;
                depth++;
            }
        }
        else
/*
 * The key value is already in the tree. Append the descriptive string
 * at the end of the already existing list.
 * If debugging, report a duplicate key and its number in the list.
 * The output string doesn't require another newline, as the newline
 * is still stored with the original string.
 */
        {
            p2 = unisift_NewString ( s );
            listlen = unisift_AppendStr ( p->thestrings, p2 );
            if ( debugLevel > 1 )
            {
                printf ( "Duplicate key %d for: %s", listlen, s );
            }
            p2 = unisift_NewString ( ucastr );
            listlen = unisift_AppendStr ( p->theucastrings, p2 );
            return ( depth );
        }
    }
}

/*
 * Free up the BTree. Bottomup (preorder) traversal.
 */

static void unisift_DropSymTreeP ( PSYMTREENODE p )
{
PSTRLISTNODE temp1;
PSTRLISTNODE temp2;

    if ( p->left != NULL )
    {
        unisift_DropSymTreeP ( p->left );
    }
    if ( p->right != NULL )
    {
        unisift_DropSymTreeP ( p->right );
    }
    if ( p->key != NULL )
    {
        free ( p->key );
    }
/*
 * Hand-over-hand free of the list of strings.
 */
    if ( p->thestrings != NULL )
    {
        temp1 = p->thestrings;
        while ( temp1 != NULL )
        {
            temp2 = temp1->next;
            free ( temp1 );
            temp1 = temp2;
        }
    }
    if ( p->theucastrings != NULL )
    {
        temp1 = p->theucastrings;
        while ( temp1 != NULL )
        {
            temp2 = temp1->next;
            free ( temp1 );
            temp1 = temp2;
        }
    }
    free ( p );
}

void unisift_DropSymTree ( void )
{
    printf ( "Freeing up symbol tree\n" );
    if ( symTreeRoot != NULL )
    {
        unisift_DropSymTreeP ( symTreeRoot );
        symTreeRoot = NULL;
    }
}

/*
 * Print the Symbol strings in the BTree. Inorder traversal.
 */

static void unisift_PrintSymTreeP ( PSYMTREENODE p, FILE *fd )
{
PSTRLISTNODE temp1;

    if ( p->left != NULL )
    {
        unisift_PrintSymTreeP ( p->left, fd );
    }
    temp1 = p->thestrings;
    while ( temp1 != NULL )
    {
        fputs ( temp1->outputstring, fd );
        temp1 = temp1->next;
    }
    if ( p->right != NULL )
    {
        unisift_PrintSymTreeP ( p->right, fd );
    }
}

void unisift_PrintSymTree ( FILE *fd )
{
    printf ( "Printing symbol tree: depth = %d\n", greatestDepth );
    unisift_PrintSymTreeP ( symTreeRoot, fd );
}

/*
 * Print the UCA strings in the BTree. Inorder traversal.
 */

static void unisift_PrintUCATreeP ( PSYMTREENODE p, FILE *fd )
{
PSTRLISTNODE temp1;

    if ( p->left != NULL )
    {
        unisift_PrintUCATreeP ( p->left, fd );
    }
    temp1 = p->theucastrings;
    while ( temp1 != NULL )
    {
        fputs ( temp1->outputstring, fd );
        temp1 = temp1->next;
    }
    if ( p->right != NULL )
    {
        unisift_PrintUCATreeP ( p->right, fd );
    }
}

void unisift_PrintUCATree ( FILE *fd )
{
    printf ( "Printing UCA tree: depth = %d\n", greatestDepth );
    dumpUCAProlog ( fd );
    unisift_PrintUCATreeP ( symTreeRoot, fd );
}

/******************************************************************/

/*
 * SECTION: BTree insertion and traversal for full symbol weights.
 */

static PSYMWTTREENODE unisift_WtNewNode ( void )
{
PSYMWTTREENODE p;

    p = (PSYMWTTREENODE)malloc( sizeof ( SYMWTTREENODE ) );
    if ( p == NULL )
    {
        return ( NULL );
    }
    p->left = NULL;
    p->right = NULL;
    p->weight = 0;
    p->value = 0;
    return ( p );
}

/*
 * Insert entry in BTree.
 */

static int unisift_InsertSymWt ( UInt32 weight, UInt32 value )
{
PSYMWTTREENODE p;
int depth;

    p = symwtTreeRoot;
    depth = 0;
/*
 * Deal with setting root for very first entry.
 */
    if ( p == NULL )
    {
        p = unisift_WtNewNode();
        if ( p == NULL )
        {
            return ( -1 );
        }
        symwtTreeRoot = p;
        depth = 1;
        p->weight = weight;
        p->value = value;
        return ( depth );
    }
    while ( 1 )
    {
        if ( weight < p->weight )
        {
            if ( p->left == NULL )
            {
                p->left = unisift_WtNewNode();
                if ( p->left == NULL )
                {
                    return ( -1 );
                }
                p = p->left;
                p->weight = weight;
                p->value = value;
                return ( depth );
            }
            else
            {
                p = p->left;
                depth++;
            }
        }
        else if ( weight > p->weight )
        {
            if ( p->right == NULL )
            {
                p->right = unisift_WtNewNode();
                if ( p->right == NULL )
                {
                    return ( -1 );
                }
                p = p->right;
                p->weight = weight;
                p->value = value;
                return ( depth );
            }
            else
            {
                p = p->right;
                depth++;
            }
        }
        else
/*
 * The weight value is already in the tree.
 *
 * Report an error if the value in the tree does not match the
 * value passed in. 
 */
        {
            if ( value != p->value )
            {
                printf ( "Duplicate weight err %04X for values %04X & %04X\n", 
                          weight, value, p->value );
            }
            return ( depth );
        }
    }
}

/*
 * Build the symbol weight tree, based on the level 1 weights and
 * the symbolBase values stored in the big array.
 */

int unisift_BuildSymWtTreeRange ( UInt32 start, UInt32 finish, int *depth )
{
UInt32 i;
int d;
WALNUTPTR p;

    for ( i = start; i <= finish; i++ )
    {
        p = getSiftDataPtr ( i );
        if ( ( p->symbolBase != 0 ) && ( p->decompType != Implicit ) )
        {
            d = unisift_InsertSymWt ( p->level1, p->symbolBase );
            if ( d == -1 )
            {
                return ( -1 );
            }
            if ( d > *depth )
            {
                *depth = d;
            }
        }
    }
    return ( 0 );
}

/*
 * Note: When new blocks are added to the SMP, be sure to revisit this
 * function, to ensure they are included for output.
 */

int unisift_BuildSymWtTree ( void )
{
int rc;
int greatestdepth;

    greatestdepth = 0;

    rc = unisift_BuildSymWtTreeRange ( 0, 0x33FF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x4DC0, 0x4DFF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0xA000, 0xABFF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    /* Skip Hangul and Han, including compatibility CJK */

    rc = unisift_BuildSymWtTreeRange ( 0xD7B0, 0xD7FF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0xFB00, 0x11CBF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x12000, 0x1343F, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x14400, 0x1467F, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x16800, 0x16FFF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x1B000, 0x1BCAF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x1D000, 0x1D37F, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x1D800, 0x1E2FF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }

    rc = unisift_BuildSymWtTreeRange ( 0x1E800, 0x1FAFF, &greatestdepth );
    if ( rc < 0 )
    {
        return ( rc );
    }
    /* Skip math alphanumerics, which have no unique symbols, and
     * Plane 14 tags and variation selectors, which are all ignored.
     */

    printf( "Symbol weight tree constructed. Depth = %d\n", greatestdepth );
    return ( 0 );
}

/*
 * Free up the BTree. Bottomup (preorder) traversal.
 */

static void unisift_DropSymWtTreeP ( PSYMWTTREENODE p )
{
    if ( p->left != NULL )
    {
        unisift_DropSymWtTreeP ( p->left );
    }
    if ( p->right != NULL )
    {
        unisift_DropSymWtTreeP ( p->right );
    }
    free ( p );
}

void unisift_DropSymWtTree ( void )
{
    printf ( "Freeing up symbol weight tree\n" );
    if ( symwtTreeRoot != NULL )
    {
        unisift_DropSymWtTreeP ( symwtTreeRoot );
        symwtTreeRoot = NULL;
    }
}

/*
 * Print the BTree. Inorder traversal.
 */

static char symwtbuf[120];

void unisift_PrintSymWtTreeP ( PSYMWTTREENODE p, FILE *fd )
{

    if ( p->left != NULL )
    {
        unisift_PrintSymWtTreeP ( p->left, fd );
    }

    if ( p->value > 0x1FFFF )
    {
        printf ( "WARNING: p->value %05X too large.\n", p->value );
    }
    else if ( p->value != 0xFFFD )
    /*
     * 0xFFFD is omitted, because it is artificially fixed at a high
     * primary weight. See below, where an entry for SFFFD is dumped
     * explicitly at the end of the table.
     */
    {
    WALNUTPTR tmp;

        sprintf ( symwtbuf, "<S%04X> %% ", p->value );
        tmp = getSiftDataPtr ( p->value );
        if ( tmp->name == NULL )
        {
            strcat ( symwtbuf, "BAD NAME!\n" );
        }
        else
        {
            strncat ( symwtbuf, tmp->name, 108 );
            strcat ( symwtbuf, "\n" );
        }
        fputs ( symwtbuf, fd );
    }

    if ( p->right != NULL )
    {
        unisift_PrintSymWtTreeP ( p->right, fd );
    }
}

void unisift_PrintSymWtTree ( FILE *fd )
{
    printf ( "Printing symbol weight tree.\n" );
    unisift_PrintSymWtTreeP ( symwtTreeRoot, fd );
    fputs ( "<RFB00> % Symbol for first element of computed weights for Tangut ideographs\n", fd );
    fputs ( "<RFB01> % Symbol for first element of computed weights for Nushu ideographs\n", fd );
    fputs ( "<RFB40>..<RFB41> % Symbols for first element of computed weights for core Han unified ideographs\n", fd );
    fputs ( "<RFB80> % Symbol for first element of computed weights for Han unified ideographs Ext-A\n", fd );
    fputs ( "<RFB84>..<RFB85> % Symbols for first element of computed weights for Han unified ideographs Ext-B, ...\n", fd );
    fputs ( "<RFBC0>..<RFBE1> % Symbols for first element of computed weights for unassigned characters\n", fd );
    fputs ( "<T8000>..<TFFFF> % Symbols for second element of computed weights for Han, Tangut, Nushu and others\n", fd );
    fputs ( "<SFFFD> % Special weight for replacement character\n\n", fd );
    fputs ( "<SFFFF> % Largest primary weight\n\n", fd );
}

