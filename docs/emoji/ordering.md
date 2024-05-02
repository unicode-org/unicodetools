# UTS #51

## Ordering

Occasionally the default emoji ordering needs to be changed. The process is mechanically
straightforward:

1.  Open [emojiOrdering.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/org/unicode/tools/emoji/emojiOrdering.txt),
2.  Reorder the emoji in question, moving them between groups as necessary.
3.  Follow the process to generate emoji described in generate-emoji.md.
    1.  Check [emoji-test.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/data/emoji/dev/emoji-test.txt)
        to make sure the changes are reflected there.
    2.  Check the generated emoji-ordering.html to make sure the changes are reflected there as well.
    3.  Note that there may also be minor changes to other pages, for example emoji-proposals.html
        uses the order when listing multiple emoji for a single proposal.

The final step is to provide the updated data to CLDR, instructions TBD.
