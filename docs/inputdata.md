# Input data setup

## Unicode 15+ workflow

Starting with Unicode 15, we are developing most of the Unicode data files
in this Unicode Tools project, and publish them to the Public folder
only for alpha/beta/final releases.
That is, we are reversing the flow of files.
(See [issue #144](https://github.com/unicode-org/unicodetools/issues/144).)

We are also no longer generating and posting files with version suffixes.

Except: Some files, such as Unihan and ucdxml data files, are developed elsewhere,
and we continue to ingest them as before.

## Source Files

The source files that you will need for a release such as 8.0.0 are in:

*   [ftp://unicode.org/Public/8.0.0/ucd](ftp://unicode.org/Public/8.0.0/ucd)
*   [ftp://unicode.org/Public/UCA/8.0.0/](ftp://unicode.org/Public/UCA/8.0.0/)
*   [ftp://unicode.org/Public/idna/8.0.0/](ftp://unicode.org/Public/idna/8.0.0/)
*   [ftp://unicode.org/Public/security/8.0.0/](ftp://unicode.org/Public/security/8.0.0/)
*   [ftp://unicode.org/Public/emoji/1.0/](ftp://unicode.org/Public/emoji/1.0/)
    *— note version*
*   [ftp://unicode.org/Public/8.0.0/ucdxml/](ftp://unicode.org/Public/8.0.0/ucdxml/)
    *— but NOT:*
    *   ucd.\*.flat.zip or
    *   ucd.all.\*.zip

You will go to each one of these and download the contents into the appropriate
local git directory, such as:

*   <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/ucd>
*   <https://github.com/unicode-org/unicodetools/tree/main/unicodetools/data/uca>
*   ...

**Important:** if the files in a directory are not yet released, and may have
changed, then you'll need to delete these cached files (the exact location may
depend on your configuration):

*   {workspace}/Generated/BIN/8.0.0.0/\*
*   {workspace}/Generated/BIN/UCD_Data8.0.0.bin

Below are the original instructions for how to create these data files. Note
that they do not 100% reflect the files published on
<https://www.unicode.org/Public/>

Historical note about svn (Subversion):
In svn, each file has version history, as much as possible. For each Unicode
version, the entire previous version's file folder was copied via `svn cp`, then
files removed (`svn rm`) that are no longer part of the release, files renamed
(`svn mv`) that had their name changed, files moved into other folders
(`svn mkdir` + `svn mv`) as the organization changed.

In git, this is optional: git tries to figure out file moves on its own.
This works well if the file contents has not changed much.
However, once files are checked into git, it is still convenient to use
`git add` for new files, `git rm` for ones that are going away, and
`git mv` for ones that are renamed or move to a different folder.

In the unicodetools repo, we only use filenames without version suffixes, to support the version
history without `git mv` renaming for each file each time. Exception: In some
UCD versions there were both CamelCase `ReadMe.txt` and uppercase `README.TXT` which
does not work on case-insensitive file systems (Mac & Windows). The uppercase
files have the version suffix.

### Removing Suffixes

Only for Unicode 14 and earlier:
For the ucd and uca files, you will have to remove the suffixes.

Tip: On Linux, you can remove version suffixes on the command line like this:
```
data/ucd/6.3.0-Update$ rename "s/-\d(\.\d\.\d)?//" *.txt *.html
```

On other platforms, you can use a script like this Python script:

`desuffixucd.py`

```python
#!/usr/bin/python3
#
# desuffixucd.py
#
# 2013-05-02 Carved out of ICU's preparseucd.py.
# Markus Scherer

"""Takes a tree of files and renames each file if necessary,
removing Unicode-data-file-style version number suffixes."""

import os
import os.path
import re
import shutil
import sys

# Get the standard basename from a versioned filename.
# For example, match "UnicodeData-6.1.0d8.txt"
# so we can turn it into "UnicodeData.txt".
_file_version_re = re.compile("([a-zA-Z0-9_-]+)" +
                              "-[0-9]+(?:\\.[0-9]+)*(?:d[0-9]+)?" +
                              "(\\.[a-z]+)$")

def main():
  if len(sys.argv) < 2:
    print("Usage: %s  path/to/UCD/root" % sys.argv[0])
    return
  ucd_root = sys.argv[1]
  source_files = []
  for root, dirs, files in os.walk(ucd_root):
    for file in files:
      source_files.append(os.path.join(root, file))
  for source_file in source_files:
    (folder, basename) = os.path.split(source_file)
    match = _file_version_re.match(basename)
    if match:
      new_basename = match.group(1) + match.group(2)
      if new_basename != basename:
        print("Removing version suffix from " + source_file)
        # ... so that we can easily compare UCD files.
        new_source_file = os.path.join(folder, new_basename)
        shutil.move(source_file, new_source_file)


if __name__ == "__main__":
  main()
```

This is checked into /data, so you can clean a target directory (such as "staging") with:
```
$ cd {workspace}/unicodetools/data/ucd/staging
$ ../../desuffixucd.py .
```

### Unihan

You may need to manually change the "Unihan-8.0.0d2 Folder" to "Unihan".

Unzip the Unihan.zip file into a "Unihan" subfolder.

Starting with Unicode 13, we split the Unihan data into single-property files
and parse those.

Run the script that is checked in at
[py/splitunihan.py](../py/splitunihan.py)
with one argument, the path to the Unihan folder.

Ignore or delete the Unihan\*.txt files now. Do not check them into the tools
any more.

Check for new and no-longer-present files (Unihan properties).
`git add` and `git rm` as necessary.

## Original data file setup instructions

### 2. Download all of the UnicodeData files for each version into UCD_DIR.

TODO(Markus): Adjust notes about data files when they are in svn.

The folder names must be of the form: "3.2.0-Update", so rename the folders on
the
Unicode site to this format. If the folder contains /ucd/, then make the
contents of that directory be the contents of the x.x.x-Update directory. That
is, each directory will directly contain files like PropList....txt

#### 2a Ensure Complete Release

If you are downloading any "incomplete" release (one that does not contain a
complete set of data files for that release, you need to also download the
previous complete release). Most of the N.M-Update directories are complete,
*except*:

*   4.0-Update, which does not contain a copy of Unihan.txt and some other files
*   3.1-Update, which does not contain a copy of BidiMirroring.txt

Also, make the following changes to UnicodeData for 1.1.5:

**Delete**

```
3400;HANGUL SYLLABLE KIYEOK A;Lo;0;L;1100 1161;;;;N;;;;;
...
4DFF;HANGUL SYLLABLE MIEUM WEO RIEUL-THIEUTH;Lo;0;L;1106 116F 11B4;;;;N;;;;;
4E00;;Lo;0;L;;;;;N;;;;;
```

**Add:**

```
4E00;;Lo;0;L;;;;;N;;;;;
9FA5;;Lo;0;L;;;;;N;;;;;
E000;;Co;0;L;;;;;N;;;;;
F8FF;;Co;0;L;;;;;N;;;;;
```

**And from a later version of Unicode, add:**

```
F900;CJK COMPATIBILITY IDEOGRAPH-F900;Lo;0;L;8C48;;;;N;;;;;
...
FA2D;CJK COMPATIBILITY IDEOGRAPH-FA2D;Lo;0;L;9DB4;;;;N;;;;;
```

#### 2b. UCA data

If you are building any of the UCA tools, you need to get a copy of the UCA data file
from https://www.unicode.org/reports/tr10/#AllKeys. The default location for this is:
```
BASE_DIR + "Collation\allkeys" + VERSION + ".txt".
```

If you have it in a different location, change that value for KEYS in UCA.java, and the value for BASE_DIR

#### 2c. Here is an example of the default directory structure with files.

*   workspace/DATA/
    *   uca/
        *   4.0.0/
            *   allkeys-4.0.0.txt
        *   ...
        *   6.0.0
            *   allkeys-6.0.0d1.txt
    *   UCD
        *   1.1.0-Update
            *   UnicodeData-1.1.5.txt
        *   ...
        *   6.0.0-Update/
            *   auxiliary/
            *   extracted/
            *   Unihan/ (see Unihan section above)
            *   ArabicShaping-6.0.0d2.txt
            *   ...
            *   UnicodeData-6.0.0d6.txt
