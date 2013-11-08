#!/usr/bin/python
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
_file_version_re = re.compile("([a-zA-Z0-9]+)" +
                              "-[0-9]+(?:\\.[0-9]+)*(?:d[0-9]+)?" +
                              "(\\.[a-z]+)$")

def main():
  if len(sys.argv) < 2:
    print ("Usage: %s  path/to/UCD/root" % sys.argv[0])
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
        print "Removing version suffix from " + source_file
        # ... so that we can easily compare UCD files.
        new_source_file = os.path.join(folder, new_basename)
        shutil.move(source_file, new_source_file)


if __name__ == "__main__":
  main()