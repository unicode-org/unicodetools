# Building UnicodeJsp

[TOC]

## Source

<https://github.com/unicode-org/unicodetools>

## Jars

### Command Line

### **Build icu4j jars**

*   download/checkout [icu4j](http://site.icu-project.org/download)
*   from the command line:
    **ant releaseCLDR**
*   output in: four jars in {$workspace}/icu4j/release_cldr/

**Build [cldr](http://cldr.unicode.org).jar**

*   **from the command line:**
    **ant jar**
*   output in: {$workspace}/cldr/tools/java/cldr.jar

(Note: These jars are slated to be available via Maven… keep an eye on
[ICU-21251](https://unicode-org.atlassian.net/browse/ICU-21251) )

### Eclipse

Do the same as above, except:

1.  Right-click on build.xml in the respective directories
    1.  {$workspace}/icu4j/build.xml
    2.  {$workspace}/cldr/tools/java/build.xml
2.  Select Run As...
3.  Pick the target as above.

### Copy

Copy the following into {$workspace}/UnicodeJsps/WebContent/WEB-INF/lib/

*   failureaccess-sources.jar
*   failureaccess.jar
*   failureaccess.txt
*   cldr.jar
*   gson-sources.jar
*   gson-version.txt
*   gson.jar
*   guava-sources.jar
*   guava-version.txt
*   guava.jar
*   icu4j-src.jar
*   icu4j-version.txt
*   icu4j.jar
*   utilities-src.jar
*   utilities.jar
*   xercesImpl.jar

## Text Files

Into {$workspace}/unicode-jsps/src/org/unicode/jsp

Copy in newest versions of:

{$workspace}/unicodetools/data/security/<VERSION>

*   confusables.txt
*   IdentifierStatus.txt
*   IdentifierType.txt

{$workspace}/unicodetools/data/ucd/<VERSION>-Update/

*   NameAliases.txt
*   NamesList.txt
*   ScriptExtensions.txt
*   StandardizedVariants.txt

{$workspace}/unicodetools/data/idna/<VERSION>

*   IdnaMappingTable.txt

{$workspace}/unicodetools/data/emoji/<VERSION>

*   emoji-sequences.txt
*   emoji-zwj-sequences.txt
*   <emoji-variants> TODO

Run \[cldr\] GenerateSubtagNames to generate results on the console; paste the
results into subtagNames.txt

Other files:

*   alpha2_3.txt — mapping from 3 letter to 2 letter. No change needed.
*   fixCodes.txt — dump of language/territory/region alias from CLDR. Fix to use
    ICU or CLDR directly instead.

Files to investigate

*   annotations.txt - hieroglyphs
*   Categories.txt
*   Transforms?
    *   Deva-IPA.txt
    *   en-IPA.txt
*   Globe.txt
*   nameprep.txt
*   nfm.txt — not sure what this is; investigate and use ICU instead.
*   tables.txt
*   temp.html
*   test.htm

**TODO Change CopyPropsToUnicodeJsp to copy all the necessary files!**

## Adding/Updating New Properties

Go to XPropertyFactory.java to add new properties other than the ones in /props/

### Adding "/Prop/" Properties

#### UnicodeJsps/org/unicode/jsp/data

Update the following files by copying from org/unicode/props

*   ExtraPropertyAliases.txt
*   ExtraPropertyValueAliases.txt

Update the following files by copying from unicodetools/data/ucd/XX.0.0-Update

*   PropertyAliases.txt
*   PropertyValueAliases.txt

#### UnicodeJsps/org/unicode/jsp/props/

Run ListProps. It will copy .bin files into {generated}bin/XX.0.0.0. Examples:

*   Age.bin
*   Alphabetic.bin
*   ASCII_Hex_Digit.bin
*   Bidi_Class.bin
*   ...

Copy those into /UnicodeJsps/src/org/unicode/jsp/props using
CopyPropsToUnicodeJsp

### Using Beta Properties

Set CachedProps.IS_BETA to true.

Build & Test

1.  Run {$workspace}/unicode-jsps-tst/src/org/unicode/jsptest/TestAll.java.

TODO: These need a lot of work; they mostly print out a lot of gorp that you
need to scan over.

Run the server.

Look at <http://localhost:8080/UnicodeJsps/properties.jsp>, and make sure that
there aren't any Z-Other props at the bottom (you'll need to update via Adding
New Properties if there are).

(TODO: explain how to do a Docker-based build here.)

## Running Locally

See <https://github.com/unicode-org/unicodetools/pull/41#issuecomment-783553959>

## Commit/PR

1.  Commit the code to your own branch, create a PR on GitHub
2.  Verify that all of the checks (build steps) succeeded, for example
    <https://github.com/unicode-org/unicodetools/pull/22/checks> in particular
    "build JSP.". Request reviews
3.  Merge PR into master when reviewed.

## Make a release / Deploy

When it is time to push a new version of the JSPs and tools, it is time to make
a "release". A release is a snapshot of the entire repository, including all
Unicode Tools.

1.  Go to the UnicodeTools repository release page at
    <https://github.com/unicode-org/unicodetools/releases>
2.  *Draft a New Release*Click the button,
    1.  Set the "Tag version" field to a release tag with the current date, such
        as "**release-2020-09-15**"
    2.  The *Target* is what the tag refers to. Leave this at **master** to tag
        the current master.
    3.  Enter a descriptive title (such as "2020-09-15 Release"
    4.  Optionally fill in some content (see image at right). PRs and issues
        will autocomplete if you want to mention a specific PR or issue.
    5.  click Publish Release (Green button at the bottom)
3.  Now, go to the (A) [ Actions tab in GitHub, and click on the (B) "Push to
    GCR Github Action"
    workflow](https://github.com/unicode-org/unicodetools/actions?query=workflow%3A%22Push+to+GCR+Github+Action%22).
    (see illustration)

    ![GitHub Actions Tab](CLDR-14145-process.png)

    1.  Every tagged release creates a new run of this workflow. Look for your
        new tag to show up and wait for it to finish building (should only take
        a few minutes) and show a green checkmark. ( Example at (C) : "test3"
        and "test4" )
4.  Now, login to https://console.cloud.google.com/ with your unicode.org
    account
    1.  At the top of the page, switch the project to Unicode Dev Infra
    2.  From the left nav / hamburger menu, find "Cloud Run".
        Click on the link for the service "**unicode-jsps-staging**". This way
        you can update the staging release before going to production.
    3.  Now, choose "Edit and Deploy a New Revision"
    4.  You will get a page that prompts you to choose a docker image to be
        deployed. See the illustration below.

            ![Google Cloud Run Image Selection Page](CLDR-14145-processb.png)

        2.  Click Select
        3.  expand the "us.gcr.io/dev-infra…/unicode-jsps" images
        4.  You should see the release tags (test3, test4) show up as "images".
        5.  Choose the image that corresponds with your deployment (such as
            **2020-09-15-release**) - the full title may not show up.
        6.  Click Select below the image.
    5.  Make sure "**√ Serve this revision immediately**" at the bottom is
        checked.
    6.  Finally, click the Deploy button (way at the bottom) to schedule the
        deployment
    7.  Allow 15 seconds or so to update (you will be given visual progress as
        the new revision takes over traffic.
5.  Check
    1.  Test the new deployment at
        <https://unicode-jsps-staging-5ocgitonaa-uw.a.run.app>
    2.  To check new characters (for example):
        <https://unicode-jsps-staging-5ocgitonaa-uw.a.run.app/UnicodeJsps/list-unicodeset.jsp?a=\\p{age=8.0}-\\p{age=7.0}>
    3.  Check at the bottom for the right Unicode and ICU versions.
    4.  Click once on each top link to do simple sanity check.
    5.  Once that's successful then:
6.  **Deploy to Production**
    1.  Repeat step §4.2 above, but choose the service **unicode-jsps** instead
        of **unicode-jsps-staging**.
7.  Check
    1.  Go to <http://unicode.org/cldr/utility/> to check that that works, using
        the same steps as #3, that is:
    2.  To check new characters (for example):
        <http://unicode.org/cldr/utility/list-unicodeset.jsp?a=\\p{age=8.0}-\\p{age=7.0}>
    3.  Check at the bottom for the right Unicode and ICU versions.
    4.  Click once on each top link to do simple sanity check.
8.  Revert?
    1.  You can choose a back-level image tag (Step 4.4 above) in order to
        revert to previous versions.
