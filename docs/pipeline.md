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

---
- [ ] Scripts.txt — Prepend ranges
- [ ] Commit

---
New blocks only:
- [ ] ShortBlockNames.txt — Update, keep sorted
- [ ] Blocks.txt — Update, keep sorted [TODO(egg): This one wants to be generated…]
- [ ] Commit

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
- [ ] PropsList.txt — Add Other_Alphabetic, Diacritic, and Extender to satisfy invariants, or to taste
- [ ] Commit

---
- [ ] UCD — [Regenerate](#regenerate-ucd)
- [ ] Enums — [Regenerate](#generateenums)

---
PR preparation:
- [ ] When for a UTC decision — Cite in the format UTC-\d\d\d-[MC]\d+
- [ ] Whenever there is a Proposal document — Cite L2 number in the format L2/yy-nnn
- [ ] data-for-new — Set label
- [ ] pipeline-* — Set label to **pipeline-provisionally-assigned** or **pipeline-`<version>`**, depending on whether provisional or not.
- [ ] PR button — Set to DRAFT pull request
  - unless approved for the upcoming version
- [ ] PR button — Press

## Scripts

There are a variety of setups for unicodetools, depending on OS, in-source vs. out-of-source, git practices, etc.
If you take part in UCD development, feel free to add your own.

### Ken UnicodeData

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
rm .\Generated\* -recurse -force;
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.Main"'  '-Dexec.args="build MakeUnicodeFiles"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=.";
cp .\Generated\UCD\16.0.0\* .\unicodetools\data\ucd\dev -recurse -force;
rm unicodetools\data\ucd\dev\zzz-unchanged-*;
rm unicodetools\data\ucd\dev\*\zzz-unchanged-*;
rm .\unicodetools\data\ucd\dev\extra\*;
rm .\unicodetools\data\ucd\dev\cldr\*;
git add ./unicodetools/data
git merge --continue
```

markusicu (Linux, out-of-source; main tracks unicode-org/main)
```sh
git merge main
# complains about merge conflicts as expected
git checkout main unicodetools/data/ucd/dev/Derived*
git checkout main unicodetools/data/ucd/dev/extracted/*
git checkout main unicodetools/data/ucd/dev/auxiliary/*
rm -r ../Generated/BIN/16.0.0.0/
rm -r ../Generated/BIN/UCD_Data16.0.0.bin
mvn -s ~/.m2/settings.xml compile exec:java -Dexec.mainClass="org.unicode.text.UCD.Main"  -Dexec.args="version 16.0.0 build MakeUnicodeFiles" -am -pl unicodetools  -DCLDR_DIR=$(cd ../../../cldr/mine/src ; pwd)  -DUNICODETOOLS_GEN_DIR=$(cd ../Generated ; pwd)  -DUNICODETOOLS_REPO_DIR=$(pwd)  -DUVERSION=16.0.0
# fix merge conflicts in unicodetools/src/main/java/org/unicode/text/UCD/UCD_Types.java
#   and in UCD_Names.java
# rerun mvn
cp -r ../Generated/UCD/16.0.0/* unicodetools/data/ucd/dev
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
rm .\Generated\* -recurse -force
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.Main"'  '-Dexec.args="build MakeUnicodeFiles"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=."
cp .\Generated\UCD\16.0.0\* .\unicodetools\data\ucd\dev -recurse -force
rm unicodetools\data\ucd\dev\zzz-unchanged-*
rm unicodetools\data\ucd\dev\*\zzz-unchanged-*
rm .\unicodetools\data\ucd\dev\extra\*
rm .\unicodetools\data\ucd\dev\cldr\*
git add unicodetools/data/ucd/dev/*
git commit -m "Regenerate UCD"
```

### Regenerate LineBreak

eggrobin (Windows, in-source).
```powershell
rm .\Generated\* -recurse -force
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.UCD.Main"'  '-Dexec.args="build MakeUnicodeFiles"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=."
cp .\Generated\UCD\16.0.0\LineBreak.txt .\unicodetools\data\ucd\dev
```

### GenerateEnums

eggrobin (Windows, in-source).
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.props.GenerateEnums"' -am -pl unicodetools  "-DCLDR_DIR=..\cldr\"  "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." -U
mvn spotless:apply
git add *.java
git commit -m GenerateEnums
```
