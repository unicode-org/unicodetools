# regex - Generate corrected regex

Regex engines often provide limited regex support. However, the expressions they
handle can be modified to explicitly contain correct UnicodeSet contents based
on Unicode properties.

The input uses Java's comments mode: unescaped ASCII whitespace is ignored,
and `#` starts a comment that runs to the end of the line, including inside
UnicodeSets. For example, `[a b]` matches `a` or `b`, and `[\#]` matches a literal
number sign. Inline flags such as `(?-x:...)` can disable comments for part of
the pattern. Property expansion preserves literal characters, so `\p{Po}`
still includes `#`.

[Back to Unicode Utilities Help Home](index)
