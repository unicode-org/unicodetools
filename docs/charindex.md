# Building the character name index

## Latest version (during α, β, and γ)

Out-of-source:
```powershell
mvn compile exec:java '-Dexec.mainClass="org.unicode.text.tools.Indexer"' -am -pl unicodetools "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." "-DCLDR_DIR=..\cldr\"
```

Upload Generated\charindex.html to https://www.unicode.org/Public/draft/charts/charindex.html.

## Last version (post-release updates)

TODO(egg): Add an option to use LAST_VERSION_INFO.