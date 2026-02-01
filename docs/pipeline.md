# From the pipeline to the UCD

The following checklist for preparing a pull request with the UCD changes for an encoding proposal was (mostly) followed for https://github.com/unicode-org/unicodetools/pulls?q=label%3Apipeline-16.0.
The plan is for this process to be part of the PAG’s review of encoding proposals going forward.

## Checklist

Prerequisites: proposal posted to L2, SAH agreed to recommend for provisional assignment (or the proposal is already in the pipeline).

- [ ] UnicodeData.txt — Prepend lines from proposal
- [ ] Commit
- [ ] UTC decision — Check counts, code points, names, properties
- [ ] SAH report — Check counts, code points, names, properties
- [ ] Ken’s UnicodeData draft — [Check consistent](#ken-unicodedata)

---
If the proposal supplies LineBreak.txt:
- [ ] LineBreak.txt — Prepend lines from proposal
- [ ] Commit

If the proposal does not supply LineBreak.txt:
- [ ] LineBreak.txt — [Regenerate](#regenerate-linebreak) [TODO(markus): This should become « invoke Ken’s tool »]
- [ ] Update modified lines
- [ ] Commit

---
New scripts only:
- [ ] UCD_Names — Check script name
- [ ] PropertyValueAliases.txt — [Regenerate](#regenerate-propertyvaluealiases)
- [ ] Enums — [Regenerate](#generateenums)

---
- [ ] Scripts.txt — Prepend ranges (carefully mind any gaps)
- [ ] Commit

---
New blocks only:
- [ ] ShortBlockNames.txt — Update, keep sorted
- [ ] Blocks.txt — Update, keep sorted [TODO(egg): This one wants to be generated…]
- [ ] Commit
- [ ] PropertyValueAliases.txt — [Regenerate](#regenerate-propertyvaluealiases)
- [ ] Enums — [Regenerate](#generateenums)

---
Joining scripts only:
- [ ] ArabicShaping.txt — Merge from proposal, keep sorted
- [ ] Commit

---
Indic scripts only:
- [ ] IndicPositionalCategory — Prepend lines from proposal
- [ ] IndicSyllabicCategory — Prepend lines from proposal
- [ ] Commit

---
If the change affects emoji properties, including reserved Extended_Pictographic codepoints that should no longer be 
reserved:
- [ ] [GenerateEmojiData](#generateemojidata)
- [ ] Commit

---
- [ ] UCD — [Regenerate](#regenerate-ucd)

---
- [ ] In unicodetools/src/main/resources/org/unicode/text/UCD/AdditionComparisons,
      copy template.txt to [RMG issue number].txt.
- [ ] Comparison tests — Write
  - Examples:
    - [straightforward characters](https://github.com/unicode-org/unicodetools/blob/08748760e371d9dbdc6a0fc883c68dff944648e2/unicodetools/src/main/resources/org/unicode/text/UCD/AdditionComparisons/182.txt#L11-L18),
    - [various Latin (lowercase-only, case pairs, modifiers)](https://github.com/unicode-org/unicodetools/blob/4f8a581c77fdda2d572a16b28e74d865a689108e/unicodetools/src/main/resources/org/unicode/text/UCD/AdditionComparisons/155.txt#L11-L72),
    - [numeric characters](https://github.com/unicode-org/unicodetools/blob/84f6110737037e74c22a66d812b398f4e3adb5b7/unicodetools/src/main/resources/org/unicode/text/UCD/AdditionComparisons/175.txt#L11-L34),
    - [characters decomposing to sequences](https://github.com/unicode-org/unicodetools/blob/5f6bc190766ed9104cebc828f1e193517f4d74ec/unicodetools/src/main/resources/org/unicode/text/UCD/AdditionComparisons/141.txt#L13-L20).
  - Until tests pass:    
    - [ ] Comparison tests — [Run](#run-comparison-tests)
    - [ ] Correct properties (often in PropList.txt, but also VerticalOrientation.txt, East_Asian_Width.txt, etc.).
    - [ ] Commit
    - [ ] UCD — [Regenerate](#regenerate-ucd)

---
PR preparation:
- [ ] UTC decision — Cite if available
  - Copy from the minutes (this includes a link), or, if unavailable, use the form UTC-\d\d\d-[MC]\d+.
  - If there is no UTC decision but an L2 document is available, cite as L2/\d\d-\d+.
- [ ] Working group — Mention:
  - Proposals from SAH — Link SAH issue
  - Proposals from ESC or CJK — Mention ESC or CJK in the PR description
- [ ] RMG issue — Link
- [ ] data-for-new — Set label
- [ ] pipeline-* — Set label:
  - **pipeline-recommended-to-UTC** if the characters are not yet in the pipeline,
  - **pipeline-provisionally-assigned**, or
  - **pipeline-`<version>`** depending on their status in [the Pipeline](https://unicode.org/alloc/Pipeline.html#future).
- [ ] PR button — Set to DRAFT pull request
  - unless approved for the upcoming version
- [ ] PR button — Press
  - The **Check UCA data** and **Check security data invariants** CI checks are
    suppressed; many character additions need separate handling there,
    but that is out of scope for the PAG work of preparing `data-for-new`,
    so reporting those failures could distract from real issues
    in the UCD invariants.
    UCA and security data issues are addressed later in the process,
    before the start of β review.
- [ ] PAG review summary for the report — Write
  - For proposals from SAH, use the following template in the SAH issue:
    ```markdown
    # PAG Review

    [Name] drafted the UCD change in https://github.com/unicode-org/unicodetools/pull/[number].

    ## PAG report

    [Summarize the propertywise tests, omitting the uninteresting, _e.g._, differences in Block or
    Unicode_1_Name, and calling out the nontrivial (in particular, any issues that were caught by
    the tests).]
    ```
  - For proposals from CJK, file a PAG issue of type `Document`, citing the proposal.
    Put the review in the `Background information / discussion` section, and link the pull request
    in the `Internal` section. See, _e.g._, https://github.com/unicode-org/properties/issues/366.
- [ ] PAG dashboard status of SAH or PAG issue — Set to `Review`
- [ ] Pipeline dashboard PAG status of RMG issue — Set to `data review`
## Scripts

There are a variety of setups for unicodetools, depending on OS, in-source vs. out-of-source, git practices, etc.
If you take part in UCD development, feel free to add your own.

### Ken UnicodeData

Ken's files come from [here](https://corp.unicode.org/~book/incoming/kenfiles/) (select appropriate ucd version e.g. `ucd160` for Unicode 16.0). NOTE: this check is probably not applicable for `pipeline-provisionally-assigned` data where Ken does not yet have a draft.

eggrobin (Windows, in-source; the remote corresponding to unicode-org is called la-vache, Ken’s files are downloaded next to the unicodetools repository).

```powershell
$latestKenFile = (ls ..\UnicodeData-*.txt | sort LastWriteTime)[-1]
$kenUnicodeData = (Get-Content $latestKenFile)
git diff la-vache/main */UnicodeData.txt |
sls ^\+[0-9A-F]                          |
% {
  $headLine = $_.line.Substring(1)
  if (-not $kenUnicodeData.Contains($headLine)) {
    $codepoint = $headLine.Split(";")[0];
    echo "Mismatch for U+$codepoint";
    echo "HEAD : $headLine";
    echo "Ken  : $($kenUnicodeData.Where({$_.Split(";")[0] -eq $codepoint}))";
  }
}
```

### Merge

eggrobin (Windows, in-source; the remote corresponding to unicode-org is called la-vache).
```powershell
git fetch la-vache
git merge la-vache/main
git checkout la-vache/main unicodetools/data/ucd/dev/Derived*;
git checkout la-vache/main unicodetools/data/ucd/dev/extracted/*;
git checkout la-vache/main unicodetools/data/ucd/dev/auxiliary/*;
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.MakeUnicodeFiles"'  '-Dexec.args="-c"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=.";
git add ./unicodetools/data
git merge --continue
```

markusicu (Linux, out-of-source; main tracks unicode-org/main)
<!--FIX_FOR_NEW_VERSION-->
```sh
git merge main
# complains about merge conflicts as expected
git checkout main unicodetools/data/ucd/dev/Derived*
git checkout main unicodetools/data/ucd/dev/extracted/*
git checkout main unicodetools/data/ucd/dev/auxiliary/*
mvn -s ~/.m2/settings.xml compile exec:java -Dexec.mainClass="org.unicode.text.UCD.Main"  -Dexec.args="version 18.0.0 build MakeUnicodeFiles" -am -pl unicodetools  -DCLDR_DIR=$(cd ../../../cldr/mine/src ; pwd)  -DUNICODETOOLS_GEN_DIR=$(cd ../Generated ; pwd)  -DUNICODETOOLS_REPO_DIR=$(pwd)  -DUVERSION=18.0.0
# fix merge conflicts in unicodetools/src/main/java/org/unicode/text/UCD/UCD_Types.java
#   and in UCD_Names.java
# rerun mvn
cp -r ../Generated/UCD/18.0.0/* unicodetools/data/ucd/dev
rm unicodetools/data/ucd/dev/ZZZ-UNCHANGED-*
rm unicodetools/data/ucd/dev/*/ZZZ-UNCHANGED-*
rm unicodetools/data/ucd/dev/extra/*
rm unicodetools/data/ucd/dev/cldr/*
git add unicodetools/src/main/java/org/unicode/text/UCD/UCD_Names.java
git add unicodetools/src/main/java/org/unicode/text/UCD/UCD_Types.java
git add unicodetools/data
git merge --continue
```

macchiati (IDE) 
```
sync github
run MakeUnicodeFiles.java -c
```
Cf. https://github.com/unicode-org/unicodetools/pull/636

### Regenerate UCD

eggrobin (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.MakeUnicodeFiles"'  '-Dexec.args="-c"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=.";
git add unicodetools/data/ucd/dev/*
git commit -m "Regenerate UCD"
```

### Regenerate LineBreak

eggrobin (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.MakeUnicodeFiles"'  '-Dexec.args="-c --generate ^LineBreak$"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=."
```

### Regenerate PropertyValueAliases

eggrobin (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.MakeUnicodeFiles"'  '-Dexec.args="-c --generate ^PropertyValueAliases$"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=."
git add unicodetools/data/ucd/dev/PropertyValueAliases.txt
git commit -m "Regenerate PropertyValueAliases"
```

### GenerateEmojiData

jowilco (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.tools.emoji.GenerateEmojiData"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=."
```

### GenerateEnums

eggrobin (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.props.GenerateEnums"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." -U
mvn spotless:apply
git add *.java
git commit -m GenerateEnums
```


### Run comparison tests
<!--FIX_FOR_NEW_VERSION-->
eggrobin (Windows, in-source; replace $RMG_ISSUE by the RMG issue number, or define it as that number).
```powershell
mvn test -am -pl unicodetools "-DCLDR_DIR=$(gl|split-path -parent)\cldr\"  "-DUNICODETOOLS_GEN_DIR=$(gl|split-path -parent)\unicodetools\Generated\"  "-DUNICODETOOLS_REPO_DIR=$(gl|split-path -parent)\unicodetools\" "-DUVERSION=18.0.0" "-Dtest=TestTestUnicodeInvariants#testAdditionComparisons" -DfailIfNoTests=false -DtrimStackTrace=false "-DRMG_ISSUE=$RMG_ISSUE"
```
Results are in Generated\UnicodeTestResults-addition-comparisons-[RMG issue number].html.
