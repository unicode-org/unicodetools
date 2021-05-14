# DUCET & allkeys.txt

## UCA 10.0

The following is a work log from the UCA 10.0 update (2017) done by Ken.

### Basic properties for new characters

<editorial committee folder>/kenfiles/uca100/uc90to1000add.txt

```none
0860;SYRIAC LETTER MALAYALAM NGA;Lo;;;;;;
0861;SYRIAC LETTER MALAYALAM JA;Lo;;;;;;
0862;SYRIAC LETTER MALAYALAM NYA;Lo;;;;;;
0863;SYRIAC LETTER MALAYALAM TTA;Lo;;;;;;
...
1F9E3;SCARF;So;;;;;;
1F9E4;GLOVES;So;;;;;;
1F9E5;COAT;So;;;;;;
1F9E6;SOCKS;So;;;;;;
```

That is the delta of explicit character records that I need to add to the sifter
input file. I convert it from the delta of records added to created the first
draft of UnicodeData-10.0.0.txt with a simple little utility that basically just
omits irrelevant fields that clutter the data. For the unidata.txt input file
for the sifter, I omit the bidi class, old comment fields, redundant numeric
fields, etc., and just leave the gc, decomposition, a single numeric field, one
comment field, and the casing fields. The code to do that is basically trivial,
and could be emulated with a perl script or whatever. The exact choice of fields
is not critical -- it was just an old decision that makes it easier to see
context for character identities in the input file, and I have maintained it
that way for continuity between versions.

When parsing the input file, the only thing the sifter cares about is the code
point and its order in the file. All of the rest of the property information is
pulled from my library implementation, rather than being parsed separately from
the input file. That prevents the occasionally stale entries in the sifter input
file (a General_Category change that I forgot to post up, a change in casing
status, etc.) from breaking the generation of allkeys.txt.

The input file processing starts with the last version I have for UCA 9.0:
unidata-9.0.0d4.txt. I have just updated the header version/date information and
posted the starting point for 10.0:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

Don't bother archiving that as a known delta -- I haven't started adding the new
records to it yet, so the content is still all exactly as it was for 9.0. I'll
explain as I go for salting in the new records, and let you know when I have a
more or less complete initial draft for 10.0 that can stand as the d1 draft of
record.

### allkeys development phase 1

I've pushed up another increment in the development of the initial values for
allkeys.txt for 10.0. See:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

<editorial committee folder>/kenfiles/uca100/allkeys-10.0.0d1.txt

Again, these are just unstable, intermediate drafts -- not the eventual d1
drafts that will get pushed live for the beta.

For this phase I have done two things:

1. Completed some basic version housekeeping updates for the sifter code:

a. Copyright year and 9.0 --> 10.0 version updates.

b. Extension of the URO CJK range to 9FEA, and addition of the CJK Extension F
range.

2. Started populating the input file (unidata.txt) with records which don't
require any new primary or secondary weights. I do this kind of intercalation
first, because they tend to be the oddballs, rather than the big ranges of new
scripts and/or symbols, and because they don't disturb primary or secondary
weights, the results can be fairly easily compared against allkeys-9.0.0.txt
still. This work proceeded in 7 chunks, as follows. After each chunk, I run the
sifter and compare the new allkeys.txt it outputs, so I can verify the additions
a small piece at a time. That avoids a big bang problem, where diagnosis of
issues gets more difficult if too many things change all at once.

a. Intercalate the 10 Masaram Gondi digits with the other digits. (This shook
out a bug in my library, where I had the new Masaram Gondi digits as numeric,
but not as decimal digits. So I then had to take a detour to fix the library.)

b. Intercalate the 11 Zanabazar Square, Soyombo, and Masaram Gondi anusvara,
visarga, candrabindus, and nuktas, equating them to the existing secondary
values.

c. Intercalate the 1 new Malayalam anusvara and 3 new Gujarati nuktas, equating
them to the existing secondary values.

d. Add the 2 new variant Malayalam viramas as sort variants of the existing
Malayalam virama.

e. Add 1CF7 Vedic sign atikrama to the ignored Vedic cantillation marks.

f. Add new combining marks 1DF6..1DF9 as generic above or below marks.

g. Make the Gujarati sukun, shadda, and maddah equivalent to the corresponding
Arabic marks. (Precedent for this is the handling of similar marks for Khojki.)

After each regeneration step, I check the diagnostics from the sifter, to verify
that the number of primary and secondary weights has stayed stable -- another
verification that this intercalation is working correctly. And I scan the
results for allkeys.txt (and also in some cases for ctt14651.txt, where I
suspect there might be an issue), to verify new collation elements ended up
where I expect them to.

After working through those steps, the remaining delta for 10.0 has had most of
the combining marks removed (except for dependent vowels and new viramas, which
will be getting primary weights). The remaining new delta to deal with consists
of: 1. new Indic scripts, including all the dependent vowels, etc.; 2. ranges of
new emoji, plus a few other symbols and punctuation that will end up as
variables (but which will, of course, then be pushing up the primary weights, as
well); 3. Hentaigana, which may need some special handling; 4. Nushu; 5. Syriac
additions for Malayalam, which may need some research, too. I'll probably deal
with each of those chunks of new stuff separately, and will report as I go.

### allkeys development phase 2

I've pushed up another increment. These files just overwrite the previous drop,
because they are still temporary, interim files:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

<editorial committee folder>/kenfiles/uca100/allkeys-10.0.0d1.txt

For this phase, I took care of all of the punctuation and symbols. Those \*do\*
impact the primary weight assignments, but don't require any special research
regarding script-specific ordering details. And they all turn into variables in
allkeys.txt.

I interpolated all of the new emoji into the relevant input file sections, in
code point order by the sections already there. There were a few other stray
symbols (group mark, observer symbol, bitcoin...), whose locations were also
pretty obvious. For the punctuation, I just interpolated again into existing
similar groups. The Zanabazar and Soyombo punctuation just went into swaths
after Tibetan and Marchen, which are related. Others went in like-to-like
locations, as best as I could determine. The details aren't too critical one way
or the other for these -- mostly it is just finding the things already in the
input file that are most like what was added, and salting them into those
locations.

This phase required no further changes to the sifter code. It added 101 new
primary weights (for a total of 21257 so far) and no new secondary weights.

Next I'll move on to the main primary weighting for the 3 new Indic scripts, all
of which should also be pretty straightforward this time around.

### allkeys development phase 3

A small increment, with the input and allkeys files posted in the same location:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

```none
# unidata-10.0.0.txt
# Date: 2017-02-14, 00:00:00 GMT [KW]
# © 2017 Unicode®, Inc.
# For terms of use, see http://www.unicode.org/terms_of_use.html
#
# Source data for sifter
#
# This input file is used by the sifter to generate the
# Default Unicode Collation Element Table (DUCET) for
# the Unicode Collation Algorithm.
#
# Version 10.0.0 draft 1 (Unicode Version: 10.0.0)
# based on Unicode data file UnicodeData-10.0.0d2.txt
# Ordering for Unicode 10.0
#
# Fields:
# Unicode;Name;Category;Decomposition;Num value;Comment;Uppercase;Lowercase;Titlecase
# Control characters start here. Ignorable by default.
0000;NULL (in ISO 6429);Cc;;;;;;
0001;START OF HEADING (in ISO 6429);Cc;;;;;;
...
14644;ANATOLIAN HIEROGLYPH A528;Lo;;;;;;
14645;ANATOLIAN HIEROGLYPH A529;Lo;;;;;;
14646;ANATOLIAN HIEROGLYPH A530;Lo;;;;;;
```

<editorial committee folder>/kenfiles/uca100/allkeys-10.0.0d1.txt

```none
# allkeys-10.0.0.txt
# Date: 2017-02-14, 14:05:28 GMT [KW]
# Copyright 2017 Unicode, Inc.
# For terms of use, see http://www.unicode.org/terms_of_use.html
#
# This file defines the Default Unicode Collation Element Table
#   (DUCET) for the Unicode Collation Algorithm
#
# See UTS #10, Unicode Collation Algorithm, for more information.
#
# Diagnostic weight ranges
# Primary weight range:   0200..5513 (21268)
# Secondary weight range: 0020..0114 (245)
# Variant secondaries:    0110..0114 (5)
# Tertiary weight range:  0002..001F (30)
#
@version 10.0.0
@implicitweights 17000..18AFF; FB00 # Tangut and Tangut Components
0000  ; [.0000.0000.0000] # NULL (in ISO 6429)
0001  ; [.0000.0000.0000] # START OF HEADING (in ISO 6429)
...
14644 ; [.5511.0020.0002] # ANATOLIAN HIEROGLYPH A528
14645 ; [.5512.0020.0002] # ANATOLIAN HIEROGLYPH A529
14646 ; [.5513.0020.0002] # ANATOLIAN HIEROGLYPH A530
2F00  ; [.FB40.0020.0004][.CE00.0000.0000] # KANGXI RADICAL ONE
3220  ; [*0318.0020.0004][.FB40.0020.0004][.CE00.0000.0000][*0319.0020.0004] # PARENTHESIZED IDEOGRAPH ONE
...
2F88F ; [.FB85.0020.0002][.A392.0000.0000] # CJK COMPATIBILITY IDEOGRAPH-2F88F
2FA1D ; [.FB85.0020.0002][.A600.0000.0000] # CJK COMPATIBILITY IDEOGRAPH-2FA1D
FFFD  ; [.FFFD.0020.0002] # REPLACEMENT CHARACTER
```

For this one, all I did was add in the 11 Garshuni Malayalam letters. I handled
this as a unit, because there was an associated action item (144-A32), where I
needed to check the original UTC documents. When I did that, I realized that the
names list also needed updating with xrefs to Malayalam letters for these
additions. So I did that separately as well. (The names list is not generated
and posted yet for that -- I tend to save those up, so there aren't so many
small changes for the NamesList.txt deltas.)

The change for allkeys.txt is straightforward -- this just added 11 primary
weights, in order, at the end of the current range of Syriac letters.

### allkeys development phase 4

I've gotten back to finishing off the DUCET development for 10.0. Here is
another delta drop, in the same location:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

<editorial committee folder>/kenfiles/uca100/allkeys-10.0.0d1.txt

This delta takes care of miscellaneous small additions with primary weights,
plus the Zanabazar Square script. In detail:

1. 3 Old Italic letters

For those, there was no good collation information. I just tacked them on to the
end of the other Old Italic letters, where they are in the code chart, anyway,
and mimicking their addition to the end of the lists of letters specific for
Venetic and Raetic in the last revision of the proposal.

2. Bopomofo letter o with dot (312E)

This one is an early, alternate form of the letter e, so I made it a sort
equivalent of the existing Bopomofo letter e.

3. Bengali letter Vedic anusvara (09FC)

For this one, I based the sorting behavior on the analogous Devanagari letter
A8F3.

4. Nushu iteration mark

This one I just stuck right after the Tangut iteration mark as another extender.

5. Zanabazar Square script

This one was a meatier problem. The proposal (L2/15-337) has an explicit
collation order specified. I followed that as much as seemed reasonable, but
departed in a couple instances.

First of all, note that the collation order has some idiosyncrasies, where it
doesn't follow the chart order directly. This is o.k., but it departs from the
usual Indic script case, where I just drop the code points into the UCA input
data file in order, because the code chart is already in optimal order. So I
took care of the entry rearrangement to follow Anshuman's specified primary
order.

Zanabazar Square also has a bunch of medial consonants -- what Anshuman calls
"cluster-final" -- in other words the offglides in kla, kra, kya, kva, etc. The
collation order in the proposal shows these as following the main consonants
with a secondary difference. But here I departed in the DUCET handling of them,
following conventions used in some other scripts for positional variants of
consonants. I entered them in the input as "<final>" variants of the
corresponding consonants, which ends up giving them tertiary weight
distinctions, instead of secondary. There are a number of precedents for other
scripts in this general area.

Another departure is that the proposal shows:

> cluster-initial letter ra << ra < cluster-final letter ra

I think that is a mistake for:

> cluster-initial letter ra < ra << cluster-final letter ra

because otherwise it breaks the pattern for the cluster-final (medial)
consonants. So I used my corrected interpretation. The "cluster-initial" ra is a
kind of initial form, but there is no way to give it secondary weight less than
the basic letter ra, without creating an anomaly for ra itself. So I think what
I've got suffices for the moment. But it might be best in the long run to go
with:

> ra <<< cluster-initial letter ra <<< cluster-final letter ra

which would be a relatively simple fix from what I have in the table currently.

Another departure is that Anshuman shows 11A33 final consonant mark as having a
primary weight distinction at the very end of the ordering. But examination of
the proposal shows that it is functioning essentially as a nukta. In the input
table, I grouped it with the rest of the Indic nuktas, so that it doesn't end up
requiring a distinct primary weight (or a superfluous new secondary weight,
either).

The two viramas for Zanabazar Square (a killer and a subjoiner) require special
treatment in the sifter. I identify them by their combining class (ccc=9) and
then exempt them from some of the usual processing for combining marks, giving
them primary weights, instead. To do that, I had to take a detour back to my
library, to make sure that I had correctly accounted for all of the non-zero
combining class characters, and get that built and working correctly, so the
sifter would get the correct values to use.

The net net of all these changes is that the primary weight range has expanded
from 0200..5513 in the last drop to 0200..554E in the current drop (21,268 -->
21,327 distinct weights). No change in secondaries.

Next I will take care of Soyombo, which is generically similar to Zanabazar
Square, and Masaram Gondi, which should be a little easier.

### allkeys development phase 5

Updated again in the same locations with another delta:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

<editorial committee folder>/kenfiles/uca100/allkeys-10.0.0d1.txt

This delta takes care of Soyombo.

The collation specification in the proposal for Soyombo (L2/15-004) had some
similar glitches to those for Zanabazar Square. I followed pretty much what
Anshuman specified, although there is an interesting structural difference in
Soyombo versus Zanabazar Square: in ZSq, Anshuman has the dependent vowels
following the consonants in primary order. This is pretty standard for Indic
scripts. For Soyombo, on the other hand, they \*precede\* all the consonants. I
presume this is because they end up sitting on a vowel carrier, the letter A, at
the beginning of the order. But it might be wise to further check on the
differences here, to see if the order makes sense.

Soyombo had the same business with some cluster-initial and some final consonant
signs. I ended up just making them tertiary variants of the main consonant
letters, and went back to the one case in ZSq (RA) and did the same there for
consistency. This treatment may not be correct in detail, but it ought to do a
decent job of grouping like letters together for default collation.

The other unexplained bit for Soyombo was 11A98 Soyombo gemination mark. That is
a \*combining\* mark, unlike most of the extenders that mark lengthening,
gemination, or iteration. Anshuman has it with a primary weight in the order
after all the consonants and before the virama. But making that work for
collation would require pushing it to be an alphabetic, rather than a diacritic,
and I'm not sure that is right, either. In the end, I analogized it to another
existing combining gemination mark, 0A71 Gurmukhi addak. In the default table, I
end up letting it acquire its own secondary weight -- it doesn't make sense to
treat it as another nukta.

Masaram Gondi will be next.

### allkeys development phase 6

I have now finished off the initial allkeys.txt drafting for 10.0, and have an
initial candidate staged up:

<http://www.unicode.org/Public/UCA/10.0.0/allkeys-10.0.0d1.txt>

<http://www.unicode.org/Public/UCA/10.0.0/decomps-10.0.0d1.txt>

The corresponding sifter input data file (not part of the release) has been
refreshed in my book incoming directory:

<editorial committee folder>/kenfiles/uca100/unidata-10.0.0d1.txt

Because I have now posted this draft, which covers all the new additions
finally, I will now freeze the "d1" draft. Any future drafts, if needed, will
become a "d2", etc. going forward.

Notes about this last set of changes:

1. Masaram Gondi

This was a fairly straightforward addition of another Indic script. The primary
weights are just in code point order except for the extra repha and ra-kara
characters. I tweaked the specification that Anshuman had in L2/15-090 (p. 14)
for those: repha << ra << ra-kara. Instead, I went with the solution I also
applied to similar characters for Zanabazar Square and for Soyombo: ra <<< repha
<<< ra-kara

Incidentally, all these tweaks for positional variants of glides and other
consonants for those 3 scripts can now be seen explicitly in the
decomps-10.0.0d1.txt. Just grep it for ZANABAZAR, SOYOMBO, and MASARAM to find
the relevent synthetic equivalences added to the input file.

2. Hentaigana

The blocks of hentaigana are just dropped in for primary ordering \*after\* the
main range of standard hiragana and katakana. That was what Japan wanted,
instead of trying to interdigitate hentaigana syllables among the standard
syllables. The one notable tweak (also required by Japan) was to ensure that the
existing 1B001 was moved to collate in the order for hentaigana e-1.

3. Nushu

The only visible change for allkeys.txt required for Nushu was of course to add
a new @implicitweights line near the top of the file.

Under the covers, however, there was a lot of work on the sifter code for this.
Basically, I needed to ensure that the part of the sifter that generates keys to
put the table in order knows that Nushu is now part of the implicit weight range
generation. And the adaptation for the CTT table generation for 14651 is more
complicated, because I need to drop the \*symbol\* for the implicit weight, plus
a bunch of additional explanation lines, at the appropriate points in the
generation of that table.

The next step will be to move on to the 4th level weight issues raised by Marc
Lodewijck. Fortunately, that is mostly a consideration for the generation of the
CTT, because we no longer print any 4th level weights as part of DUCET. So
fiddling with that stuff in the code may not result in any change at all for
allkeys.txt.
