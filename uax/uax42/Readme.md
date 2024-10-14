# Generating TR42

## Step 1 - Generate property value fragments

- Run org.unicode.xml.GeneratePropertyValues to populate the UNICODETOOLS_REPO_DIR/uax/uax42/fragments/ folder.

## Step 2 - Generate TR42 index.html and index.rnc 

- In UNICODETOOLS_REPO_DIR/uax/uax42/ run `mvn xml:transform`

  index.html and index.rnc will be generated in UNICODETOOLS_REPO_DIR/uax/uax42/output/

## Step 3 - Validate generated UAX XML files

You'll need a [RELAX NG](https://relaxng.org/) schema validator. We'll use [jing-trang](https://github.
com/relaxng/jing-trang) in this example.

1.  Clone and build [jing-trang](https://github.com/relaxng/jing-trang)
2. Run the following:
    ```
   java -jar C:\_git\jing-trang\build\jing.jar -c UNICODETOOLS_REPO_DIR\uax\uax42\output\index.rnc <path to UAX xml file>
   ```

