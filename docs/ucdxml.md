# Generating TR42

## Step 1 - Generate property value fragments

- mvn compile exec:java '-Dexec.mainClass="org.unicode.xml.GeneratePropertyValues"' '-Dexec.args="--ucdversion 16.0.0 -f $(cd ./unicodetools/src/main/resources/org/unicode/uax42/fragments; pwd)"' -DCLDR_DIR=$(cd ../cldr ; pwd) -DUNICODETOOLS_GEN_DIR=$(cd ../Generated ; pwd) -DUNICODETOOLS_REPO_DIR=$(pwd)

## Step 2 - Generate TR42 index.html and index.rnc 

- mvn xml:transform -f $(cd ./unicodetools/src/main/resources/org/unicode/uax42/fragments; pwd) -Doutputdir=../Generated/uax42/

## Step 3 - Validate generated UAX XML files

You'll need a [RELAX NG](https://relaxng.org/) schema validator. We'll use [jing-trang](https://github.
com/relaxng/jing-trang) in this example.

1. Clone and build [jing-trang](https://github.com/relaxng/jing-trang)
2. Run the following:
    ```
   java -jar C:\_git\jing-trang\build\jing.jar -c UNICODETOOLS_REPO_DIR\uax\uax42\output\index.rnc <path to UAX xml file>
   ```
   Note that the UAX xml file has to be saved as NFD as the Unihan syntax regular expressions are expecting NFD.

