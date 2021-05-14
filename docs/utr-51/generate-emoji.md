# Generate Emoji

TODO flesh this out with the build details.

## Candidate Data

Open
[candidateData.txt](http://unicode.org/repos/unicodetools/trunk/unicodetools/org/unicode/tools/emoji/candidateData.txt),
and update to current status from the UTC.

*   Status=Final Candidate, Draft Candidate, or Provisional Candidate
    *   It affects all following characters.
    *   So when as set moves from Provisional to Draft, the Status needs to be
        changed.
*   Quarter, Proposal, and After are **prefix**, and affect *all* characters up
    to when changed.
*   The comes the code point
    *   Provisional names have a 6 digit code, which is
        10<2digit-year><2digits>, like 101956
        *   The 2digit-year is the year it is targeted for.
        *   The 2digits are assigned sequentially, but ordering doesn't matter
    *   Draft codes are regular Unicode codes (assigned by UTC)
    *   The code is used to fetch the image from:
        *   {workspace}/unicode-draft/reports/tr51/images/proposed/
        *   like: proposed_101956.png
*   Then specific modifiers like:
    *   Name=red-haired person
    *   Keywords= ginger | redhead
    *   Emoji_Modifier_Base
    *   Emoji_Gender_Base
*   The names can be in any case (they are lowercased)
*   The keywords should be in the final case (allowing uppercase like Jolly
    Roger)
*   The gender and modifier variants are generated automatically
    *   The modifier variants are generated according to the emoji properties
    *   The gender variants are generated according to
        EmojiData.getGenderBases()

Example:

> Proposal=L2/17-082, L2/17-011, L2/16-147, L2/16130, L2/16-008, L2/14-173

> After=👃🏿

> U+1F9B0

> Name=red-haired

> UName=EMOJI COMPONENT RED HAIR

> Keywords= ginger | redhead

> Emoji_Component

> Comment=Component for use in <a target='doc'
> href='http://unicode.org/reports/tr51/proposed.html#def_RGI'>RGI</a>
> sequences. Isolated images should have dotted borders.

Open GenerateEmoji.java and look at showCandidateStyle. Fix topHeader (right
instance) and getDoubleLink("Provisional Candidates") as necessary for new
status (eg changing Provisional to Draft).

Open tr51/images/proposed/ and copy in any missing images. Follow the
requirements on
[selection.html#images](http://unicode.org/emoji/selection.html#images).

## Running the tool

Run GenerateEmoji.java to regenerate all charts (including emoji-candidates).
Sanity check, fix, and iterate as necessary.

There are two environment variables (in Eclipse, you can create different
Configurations for these)

> -Demoji-abbr => creates an abbreviated version

> -Demoji-beta => creates a beta version (only necessary if producing both a
> current and past version

## Draft Candidates

Once a character is added to Draft candidates, then the draft data files have to
be updated as well. In particular, they need to be added to
/unicodetools/data/emoji/X.0:

1.  /source/emojiOrdering.txt
2.  /emoji-data.txt
    1.  The gender and skin-tone variants are generated automatically
3.  Move the proposal information from candidateData.text to proposalData.txt.
    Use
    CandidateData.generateProposalData to get the list, but verify
4.  ...

## Final Candidates

Moving from final candidates involves a number of changes.
