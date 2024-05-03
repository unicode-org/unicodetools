# Building UnicodeJsp

- Note 2: there are some notes on updated processes for using GCP at [gcp-run.md](./gcp-run.md) - at present, automated deployment is being worked on.

## Compiling
### Prerequisites
- Java
- Maven (download instructions [here](http://cldr.unicode.org/development/maven#TOC-Installing-Maven)
- GitHub account
- UnicodeJsp source downloaded

### Maven Environment setup

Setup your maven environment’s `.m2/settings.xml` as per
[CLDR instructions](http://cldr.unicode.org/development/maven#TOC-Getting-Started---GitHub-token). You don’t
need to actually need to download or build CLDR itself.

### Building from the command line

Run `mvn -am -pl UnicodeJsps package -DskipTests` at the root directory of the repository to build the JSPs.

### Building from Eclipse

UnicodeJsps can be imported into eclipse using the _Import->existing maven project_ menu option.
If you already have `UnicodeJsps` in eclipse, it might be better to remove it from eclipse before the import.

## Running

### Command Line

```shell
mvn -DCLDR_DIR=/path/to/cldr -DUNICODETOOLS_REPO_DIR=/path/to/unicodetools org.eclipse.jetty:jetty-maven-plugin:run
```

If port 8080 is in use, another port can be specified with `-Djetty.port=⟨port number⟩`.

The following system properties described in [Building Unicode Tools](../build.md#java-system-properties-used-in-unicode-tools) must be set in order for the JSPs to function properly: `CLDR_DIR`, `UNICODETOOLS_REPO_DIR`; this can be done with `-DCLDR_DIR=⟨some path⟩ -DUNICODETOOLS_REPO_DIR=.`.

You can now connect to <http://127.0.0.1:8080> as suggested from the command line.
Use Control-C to stop the server.

> Note: this used to be `mvn jetty:run` but a more verbose command is needed at present.

### Running from within Eclipse

This screenshot shows creating a `org.eclipse.jetty:jetty-maven-plugin:run` maven run configuration.

![Running org.eclipse.jetty:jetty-maven-plugin:run from eclipse](eclipse-jetty-run.png)

> Note: Please change the Goal to `org.eclipse.jetty:jetty-maven-plugin:run` instead of `jetty:run`

### Debugging from within eclipse

Jetty provides instructions [here](https://www.eclipse.org/jetty/documentation/jetty-9/index.html#debugging-with-eclipse) for debugging
within eclipse.  It is unknown whether this has been attempted with the UnicodeJsps.

## Upgrading the UnicodeJsps

### Run `org.unicode.tools.UpdateJspFiles`

This will copy a number of files from the unicodetools to the Jsp directory.
The items it doesn't cover are discussed below.

### Other

`{$workspace}/unicodetools/data/emoji/\<VERSION\>`

*   *\<emoji-variants\>* :construction: **TODO**

Other files:

*   alpha2_3.txt — mapping from 3 letter to 2 letter. No change needed.
*   fixCodes.txt — *dump of language/territory/region alias from CLDR. Fix to use
    ICU or CLDR directly instead.*

Files to investigate

*   annotations.txt - hieroglyphs
*   Categories.txt
*   Transforms?
    *   Deva-IPA.txt
    *   en-IPA.txt
*   Globe.txt
*   nameprep.txt
*   nfm.txt — *not sure what this is; investigate and use ICU instead.*
*   tables.txt
*   temp.html
*   test.htm

## Adding/Updating New Properties

Go to `XPropertyFactory.java` to add new properties other than the ones in /props/
### Using Beta Properties

In [org.unicode.text.Settings](/unicodetools/src/main/java/org/unicode/text/utility/Settings.java), set `latestVersionPhase` to `BETA`.

Build & Test

1.  Run `mvn test`

:construction: **TODO**: These need a lot of work; they mostly print out a lot of gorp that you
need to scan over.

Run the server (see above)

Look at <http://localhost:8080/UnicodeJsps/properties.jsp>, and make sure that
there aren't any Z-Other props at the bottom (you'll need to update via Adding
New Properties if there are).

### Running a Docker-based build

Download the Last Resort font.

```
wget https://github.com/unicode-org/last-resort-font/releases/latest/download/LastResort-Regular.ttf
mv ./LastResort-Regular.ttf ./UnicodeJsps/src/main/webapp/
```

compile java stuff

- `mvn -B package -am -pl UnicodeJsps -DskipTests=true`

- make a copy of CLDR - lots of ways to do this. `--reference-if-able ~/src/cldr` is another directory on my disk which has a copy of CLDR, to save copying. The CLDR_REF calculation is to make sure you have the same CLDR version as the build.

```
git clone https://github.com/unicode-org/cldr.git     --reference-if-able ~/src/cldr
CLDR_REF=$(mvn help:evaluate -Dexpression=cldr.version -q -DforceStdout | cut -d- -f3)
(cd cldr ; git reset --hard ${CLDR_REF})
mkdir -p UnicodeJsps/target && tar -cpz --exclude=.git -f UnicodeJsps/target/cldr-unicodetools.tgz ./cldr/ ./unicodetools/
```

- Regenerate the property cache files:

```
mvn compile exec:java '-Dexec.mainClass="org.unicode.jsp.RebuildPropertyCache"' -am -pl unicodetools   "-DUNICODETOOLS_GEN_DIR=Generated"  "-DUNICODETOOLS_REPO_DIR=." "-DCLDR_DIR=<somewhere>"
tar -cpz -f UnicodeJsps/target/generated.tgz ./Generated/
```

Now, finally build.

- `docker build -t unicode/unicode-jsp:latest UnicodeJsps/`

… And run. Control-C to cancel it, otherwise visit <http://127.0.0.1:8080>

```
docker run --rm -p 8080:8080 unicode/unicode-jsp:latest
```

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
        will autocomplete if you want to mention a specific PR or issue.\
        ![GitHub release page](CLDR-14145-makerelease.png)
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
    1.  At the top of the page, switch the project to [`goog-unicode-dev`](https://console.cloud.google.com/run?project=goog-unicode-dev)
        ![choose a deployment](chooseimage.png)
    2.  From the left nav / hamburger menu, find "Cloud Run".
        Click on the link for the service "**unicode-jsps-staging**". This way
        you can update the staging release before going to production.
    3.  Now, choose "Edit and Deploy a New Revision"
    4.  You will get a page that prompts you to choose a docker image to be
        deployed. See the illustration below.

        ![Google Cloud Run Image Selection Page](CLDR-14145-processb.png)

        2.  Click Select
        3.  expand the "us-central1-docker.pkg.dev/goog-unicode-dev/unicode-jsps" images
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
        <https://unicode-jsps-staging-o2ookmn2oq-uc.a.run.app>
    2.  To check new characters (for example):
        <https://unicode-jsps-staging-o2ookmn2oq-uc.a.run.app/UnicodeJsps/list-unicodeset.jsp?a=\p{age=8.0}-\p{age=7.0}>
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
        <http://unicode.org/cldr/utility/list-unicodeset.jsp?a=\p{age=8.0}-\p{age=7.0}>
    3.  Check at the bottom for the right Unicode and ICU versions.
    4.  Click once on each top link to do simple sanity check.
8.  Revert?
    1.  You can choose a back-level image tag (Step 4.4 above) in order to
        revert to previous versions.
