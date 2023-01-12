# Data Files Workflow

Starting with Unicode 15.1, the “source of truth” for not-yet-released versions of
most of the data files is in
https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/ucd/dev
and parallel .../uca/dev etc. folders.

## UCD/UCA/emoji/idna/security data files

First: Files with a special process.

### Unihan data

List of these files (see https://www.unicode.org/Public/UCD/latest/ucd/):
*   USourceData.txt
*   USourceGlyphs.pdf
*   USourceRSChart.pdf
*   Unihan.zip

Process:
*   The “source of truth” is the Unihan database maintained by the CJK/Unihan group, including data maintained by Michel.
*   The CJK/Unihan group posts data files into an internal location.
*   KenW vets these files and posts them to https://www.unicode.org/Public/draft/UCD/ucd/ .
*   A unicodetools GitHub contributor fetches these files, preprocesses the contents of Unihan.zip,
    and creates a pull request as for “regular” data files.
    (The processed data files go into .../unicodetools/data/ucd/dev/Unihan.)

### NamesList.txt

*   Iterate between KenW and Michel.
*   Generated from UnicodeData.txt and an annotations file, using some C program.
*   Used for generating code charts.
*   KenW posts NamesList.txt into https://www.unicode.org/Public/draft/UCD/ucd/ .
*   A unicodetools GitHub contributor fetches this file
    and creates a pull request as for “regular” data files.

### Folder readmes

The “source of truth” for these is outside of GitHub for now.
KenW updates or vets these files and posts them to https://www.unicode.org/Public/draft/ .
A unicodetools GitHub contributor fetches these files and creates a pull request as above.

See https://github.com/unicode-org/properties/issues/8 “simplify versioning of readme files”

### “Regular” data files

Changes are made in a GitHub pull request.
*   Contributors who don’t use GitHub or don’t contribute to the unicodetools project
    can send changes (updated files or deltas) to someone who is comfortable using GitHub.
*   Updated files could be shared in various ways including via email or via private FTP areas.
*   Updated files should be based on the latest (or fairly recent) data in the unicodetools repo.
*   Updated files should not be posted directly to https://www.unicode.org/Public/...

Pull request cycle:
*   One commit for manual or contributed data changes.
*   Another commit for tool code updates, generated/derived data files,
    and for fixing test failures (continuous-integration tests or manual tests).
*   Peer review of the changes.
*   QA may include pushing files to a private area, e.g., for KenW to review.
*   More commits as necessary until changes look good, tests pass,
    and someone (preferably KenW) approves.
*   **Squash**-and-merge.
    *   Or squash locally, force-push, and rebase-merge.
    *   Or, if you are comfortable with git, you may locally squash down to two commits,
        one with the input changes and one with all of the generated changes and code fixes,
        and rebase-merge.

### Notes about emoji data files

One difference here: Multiple stages, generating charts along the way.
Initial draft versions of eg annotations for candidates,
which is in addition to the current data for the next version of Unicode.
Once they get code points, they go into the regular files.
The emoji tools also read annotation data from CLDR and from the candidates file,
and use the CLDR emoji collation data (and interpolates the candidates data).

Another difference:
The charts are not normative; they get updated out of cycle, for example with new vendor images.

https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/emoji/dev

## Publication

Certain snapshots of the .../dev/ files are copied into https://www.unicode.org/Public/draft/
for Unicode alpha, beta, and final releases, and more as appropriate.
*   UCD files go into https://www.unicode.org/Public/draft/UCD/
*   UCA files go into https://www.unicode.org/Public/draft/UCA/
*   emoji files go into https://www.unicode.org/Public/draft/emoji/
*   etc.
*   Inside “draft” there are no folder levels with version numbers.

Make sure to publish exactly the intended set of files.
Skip the NamesList.txt and Unihan data files (see above),
and skip any others that are only for internal use.

### Publish an alpha snapshot

TODO: Define the minimal set of files to be published for an alpha.

TODO: Write a script like /pub/copy-alpha-to-draft.sh that will be run on the unicode.org server
and copy the set of the .../dev/ data files for an alpha snapshot
from a unicodetools workspace to the location behind https://www.unicode.org/Public/draft/ .

Note: No version/delta infixes in names of data files.
We simply use the “draft” folder and the file-internal time stamps for versioning.

### Publish a beta snapshot

TODO: Write a script like /pub/copy-beta-to-draft.sh that will be run on the unicode.org server
and copy the set of the .../dev/ data files for a beta snapshot
from a unicodetools workspace to the location behind https://www.unicode.org/Public/draft/ .

### Publish a release

TODO: Write a script like /pub/copy-release-to-draft.sh that will be run on the unicode.org server
and copy the set of the .../dev/ data files for a beta snapshot
from a unicodetools workspace to the location behind https://www.unicode.org/Public/draft/ .

Verify the final set of files in the draft folder.

TODO: Write a script like /pub/copy-draft-to-release.sh that will be run on the unicode.org server
and copy the files from the location behind https://www.unicode.org/Public/draft/
to the locations behind the version-specific release folders.
For example:
*   https://www.unicode.org/Public/draft/UCD/ → https://www.unicode.org/Public/15.1.0/
*   https://www.unicode.org/Public/draft/UCA/ → https://www.unicode.org/Public/UCA/15.1.0/
*   https://www.unicode.org/Public/draft/emoji/ → https://www.unicode.org/Public/emoji/15.1/
*   etc.

After a Unicode release, copy a snapshot of the unicodetools repo .../dev/ files
(matching the released files, of course) to a versioned unicodetools folder;
for example: .../unicodetools/data/ucd/15.1.0/ .
(We no longer append a “-Update” suffix to the folder name.)

