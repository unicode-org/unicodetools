# UCDXML

There are three separate processes for generating and validating UCDXML files and their corresponding UAX42 report.

1. Generate the UCDXML files.
2. (Optional) You can compare the generated UCDXML files against each other (e.g., Flat vs Grouped) or against 
   previous versions.
3. Generate UAX42. There are three steps involved:

   1. Generate the property value fragments. The updated versions should live in 
      unicodetools/src/main/resources/org/unicode/uax42/fragments
   2. Generate the index.html and index.rnc files for UAX42.
   3. (Optional) Validate the UCDXML files using index.rnc.

## Generate UCDXML files

- You can generate flat or grouped versions of UCDXML.
- You can generate UCDXML files for:
  - the full range of code points
  - the Unihan code points
  - code points that are not Unihan code points

```
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range ALL --output FLAT"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range UNIHAN --output FLAT"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range NOUNIHAN --output FLAT"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range ALL --output GROUPED"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range UNIHAN --output GROUPED"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.UCDXML"' '-Dexec.args="--range NOUNIHAN --output GROUPED"' -DCLDR_DIR=$(cd ../cldr; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
```

## Compare UCDXML files

After generating UCDXML files, you can compare:

- Different versions of the same type (range and output) of UCDXML file
- Grouped and flat versions of the same code point range

```
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.CompareUCDXML"' '-Dexec.args="-a {path to file} -b {path to file}"'
```

## Generating TR42

### Step 1 - Generate property value fragments

```
mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.GeneratePropertyValues"' -DCLDR_DIR=$(cd ../cldr ; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated ; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)
```

UAX42 fragments live in unicodetools/src/main/resources/org/unicode/uax42/fragments

### Step 2 - Generate TR42 index.html and index.rnc 

```
mvn xml:transform -f $(cd ./unicodetools/src/main/resources/org/unicode/uax42; pwd) -Doutputdir=$(cd ../Generated/uax42; pwd)
```

### Step 3 - Validate generated UAX XML files

You'll need a [RELAX NG](https://relaxng.org/) schema validator.
We'll use [jing-trang](https://github.com/relaxng/jing-trang) in this example.

1. Clone and build [jing-trang](https://github.com/relaxng/jing-trang)
2. Run the following:
    ```
   java -jar C:\_git\jing-trang\build\jing.jar -c UNICODETOOLS_REPO_DIR\uax\uax42\output\tr42.rnc <path to UAX xml file>
   ```
   Note that the UAX xml file has to be saved as NFD as the Unihan syntax regular expressions are expecting NFD.
   
   To convert to NFD, use ICU's uconv.exe:
   ```
   uconv.exe uconv -f utf8 -t utf8 -x nfd -o {outputfile} {originalfile}
   ```

