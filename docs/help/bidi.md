# bidi - Unicode Bidi Algorithm (UBA) Demos

There are two demos of the
[Unicode Bidirectional Algorithm](https://www.unicode.org/reports/tr9/) (UBA):

- [bidi](#bidi---java-demo): a Java implementation, frozen at the Unicode 6.2
  rules. It explains the rules visually, character by character.
- [bidi-c](#bidi-c---c-reference-demo): the UBA C Reference Implementation,
  supporting UBA versions from 6.2 to the current version. Use this one to see
  the behavior of the current UBA (including the isolate controls LRI, RLI,
  FSI, and PDI, and bracket pairs, added in Unicode 6.3).

## bidi - Java Demo

[Demo](https://util.unicode.org/UnicodeJsps/bidi.jsp)

Type in the sample text, and hit "Show Bidi". Below, you'll see how the rules in
the UBA are applied to the characters. Clicking on any character class (like
"EN") will show the Unicode characters with that property value. Clicking on a
rule (like "W5") will take you to the rule in the UBA spec.

If you turn on "ASCII Hack", then ASCII characters in the sample will be
interpreted as if they were Arabic, Hebrew, etc. for the purpose of
illustration.

This implementation only supports the UBA rules and character properties of
Unicode 6.2. For later versions, use bidi-c.

## bidi-c - C Reference Demo

[Demo](https://util.unicode.org/UnicodeJsps/bidic.jsp)

Runs a single paragraph of text through the
[BidiReferenceC](https://www.unicode.org/Public/PROGRAMS/BidiReferenceC/)
implementation of the UBA and shows the result.

### Input

Type the text into the box. The buttons above it insert characters that are
hard to type: Tab, the marks (LRM, RLM, ALM), the embedding and override
controls (LRE, RLE, LRO, RLO, PDF), and the isolate controls (LRI, RLI, FSI,
PDI). As you type, the table under the box shows each character with its
memory position (index) and code point.

The input is limited to 200 code points and must be a single paragraph: if it
contains a paragraph separator (such as a newline), the table turns red and
"Run UBA" is disabled.

Options:

- **Paragraph**: the paragraph direction. "Auto" determines it from the text
  (rules P2 and P3); "LTR" and "RTL" force it.
- **UBA Version**: which version of the algorithm, and of the Bidi character
  properties, to use. Defaults to the latest.
- **Detail**: how much of the algorithm's trace to show in the Analysis
  section: "Low", "High", or "Full".
- **Show vacuous rules**: also list rules in the trace that did not change
  anything for this input.

Hit "Run UBA" to see the results. Changing the input or options greys out the
previous results until you run again.

### Output

- **Resolved Levels**: the embedding level resolved for each character, by
  memory position. Even levels are left-to-right, odd levels right-to-left.
  Characters removed by rule X9 (such as embedding controls) have no level.
- **Reordered Display**: the characters in visual order (left to right), after
  reordering by rule L2. Each column gives the display position, the memory
  position the character came from, the character, and its code point.
- **Analysis**: the trace output of the reference implementation, showing the
  state of the text after each rule was applied, at the chosen level of detail.

The URL of the results page encodes the input and options, so it can be shared
to show the same example to others.

[Back to Unicode Utilities Help Home](index)
