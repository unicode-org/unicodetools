# Building the character name index

Windows, out-of-source:
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.tools.Indexer"' -am -pl unicodetools "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." "-DCLDR_DIR=..\cldr\"
```
Upload Generated\charindex.html and Generated\charindex-draft.html to
https://www.unicode.org/charts/charindex.html and
https://www.unicode.org/charts/charindex-draft.html.

> [!CAUTION]
> The tool also generates a file at Generated\fr\charindex.html based on the French NamesList.txt.
> This French version of the index has major searchability shortcomings, and a considerable amount
> of untranslated English.  It should not be deployed at this time.