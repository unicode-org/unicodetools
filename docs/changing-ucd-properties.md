# Changing UCD Properties & Printout

## Simple Properties

The properties built directly from files have constants defined in
UCD_Types/Names. You normally have to add a constant and update a LIMIT value in
UCD_Types, and then add a long name and a short name in UCD_Names.

**Some of this is outdated**; see also the [New Unicode Properties](newunicodeproperties.md) page.
Sometimes you have to make changes in both the old & new mechanisms.

Examples:

```
  // line break
public static final byte
...
LB_ZJ = 39,
LIMIT_LINE_BREAK = 40,

static final String[] LINE_BREAK = {
  ..., "ZJ"

static final String[] LONG_LINE_BREAK = {
  ...,
  "Zero_Width_Joiner"
```

### Blocks

Blocks are special. You need to modify ShortBlockNames.txt for the short name.

## Derived Properties

The derived properties are found in a couple of places (history...),
DerivedProperties.java and ToolUnicodePropertySource.java. Easiest way to find
them is to search the /UCD/ directory for a word in the property name.

### ToolUnicodePropertySource.java

Most are here. For example, to change WB, look there for word_break. You'll add
a new property just by adding values to a unicodeMap.

**Remember to remove the character from the old property, since it checks for
collisions.** Example:

```
unicodeMap.putAll(getProperty("Grapheme_Extend").getSet(UCD_Names.YES).addAll(
                  cat.getSet("Spacing_Mark")).remove(0x200D), "Extend");

unicodeMap.put('\u200D', "Joiner");

unicodeMap.putAll(0x1F1E6, 0x1F1FF, "After_Joiner");
```

**If you fail, you'll get a message like:**

```
Exception in thread "main" java.lang.UnsupportedOperationException: Attempt to reset value for 200D when that is disallowed. Old: Extend; New: Joiner
at com.ibm.icu.dev.test.util.UnicodeMap._put(UnicodeMap.java:280)
at com.ibm.icu.dev.test.util.UnicodeMap.put(UnicodeMap.java:376)
```

To add the alias, look down for the setMain. You'll find a list of items, and
just add to it:
```
{ "Joiner", "J" }, { "After_Joiner", "AJ" },
```

### DerivedProperty.java

Some derived properties are here, such as DefaultIgnorable. If you search for
the name, you'll see something like:
```
dprops[DefaultIgnorable] = new UCDProperty() {
    {
        type = DERIVED_CORE;
        name = "Default_Ignorable_Code_Point";
        hasUnassigned = true;
        shortName = "DI";
    }
```

## Segmentation Rules

Changing Segmentation rules involves more work. Open Segmenter.java. Scroll to
the bottom to see the rules.

2019nov21: This appears to be outdated. The rules are in two text files now:

*   For UCD default behavior:
    [unicodetools/org/unicode/tools/SegmenterDefault.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/org/unicode/tools/SegmenterDefault.txt)
*   For CLDR behavior:
    [unicodetools/org/unicode/tools/SegmenterCldr.txt](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/org/unicode/tools/SegmenterCldr.txt)
*   Keep these two in sync, except for pre-existing intended differences. CLDR
    additions exist in both files to facilitate syncing, just commented out with
    a specific prefix in the default version.

Add new variables, like:
```
"$J=\\p{Grapheme_Cluster_Break=Joiner}",

"$AJ=\\p{Grapheme_Cluster_Break=After_Joiner}",
```

In everything but Grapheme_Clusters, also add a rewrite. For example, for SJ
there is:
```
"$SY=\\p{Line_Break=Break_Symbols}",
```

but then lower down you also need to add:
```
"$SY=($SY $X)",
```

Translate the rule into the right syntax:

> GB8b. Joiner × After_Joiner

becomes:
```
"8.1) $Joiner \u00D7 $After_Joiner",
```

***It may be useful to look at the history of the file to see the kinds
of rule changes that are made!***

For example: http://unicode.org/utility/trac/changeset/1009 which became [git commit f130761](https://github.com/unicode-org/unicodetools/commit/f130761d01fb52cd52b44e33a6dce2ad73a846de)

### Adding Segmentation Sample Strings

To add new sample strings, go to GenerateBreakTest. You'll find code that looks
like the following. Add the strings you want as samples to one of the list of
strings (see below for an example).
```
static class GenerateGraphemeBreakTest extends XGenerateBreakTest {

    public GenerateGraphemeBreakTest(UCD ucd) {
        super(ucd, Segmenter.make(..., "Grapheme",
            new String[]{
              unicodePropertySource.getSet("GC=Cn").iterator().next(),
              "\uD800",
              // ADD HERE FOR SAMPLES USED IN COMBINATION
            }, 
            new String[]{
              // ADD HERE FOR SAMPLES USED INDIVIDUALLY
        });
    } 
}
```

The difference between the two types of samples is that the first are used in
combination in the tests; if you add one string, then you more than double the
number of test cases.

If you want to add strings that show up as samples in the .html file, then you
add to the second location.

## Property(Value)Aliases

If you look in MakeUnicodeFile.txt, there's a listing of properties and formats
for generating the files. Certain files are "SPECIAL", which means that in
MakeUnicodeFile.java there is special code for them, such as:
```
public static void generateFile(String filename) throws IOException {
    if (filename.endsWith("Aliases")) {
        if (filename.endsWith("ValueAliases")) {
            generateValueAliasFile(filename);
        } else {
            generateAliasFile(filename);
        }
    } else {
```

So that requires a change in the code (eg in generateValueAliasFile)
