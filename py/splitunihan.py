#!/usr/bin/python -B
# -*- coding: utf-8 -*-
# © 2019 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script reads Unihan*.txt files and writes one new file per property.
# Invoke with one argument, the path to the Unihan data folder.

import glob
import os.path
import sys

# Maps from a property like "kTotalStrokes" to a list of data:
# - output file object
# - first range code point (string)
# - last range code point (string)
# - last range code point (int)
# - last value
_prop_data = {}

def ParseFile(in_file, root_path):
  for line in in_file:
    line = line.strip()
    if not line: continue  # empty line
    if line.startswith("#"): continue  # comment line
    fields = line.split("\t")
    assert len(fields) == 3, "expect three TAB-delimited fields\n  %s\n" % line
    assert fields[0].startswith("U+")
    (cp_str, prop, value) = fields
    cp_str = cp_str[2:]  # Strip off the "U+" prefix.
    cp = int(cp_str, 16)
    if prop not in _prop_data:
      print "new file for " + prop
      out_path = os.path.join(root_path, prop + ".txt")
      out_file = open(out_path, "w")
      _prop_data[prop] = [out_file, cp_str, cp_str, cp, value]
    else:
      data = _prop_data[prop]
      (last_cp, last_value) = data[3:5]
      if last_cp + 1 == cp and last_value == value:
        # Extend the range.
        data[2] = cp_str
        data[3] = cp
      else:
        # Write the last range.
        (out_file, first_cp_str, last_cp_str) = data[0:3]
        if first_cp_str == last_cp_str:
          out_file.write(first_cp_str + "\t" + last_value + "\n")
        else:
          out_file.write(first_cp_str + ".." + last_cp_str + "\t" + last_value + "\n")
        # Start a new range.
        data[1:] = (cp_str, cp_str, cp, value)


def main():
  global _prop_data
  root_path = sys.argv[1]
  pattern = os.path.join(root_path, "Unihan*.txt")
  paths = glob.glob(pattern)
  for path in paths:
    print path
    with open(path, "r") as in_file:
      ParseFile(in_file, root_path)
  # Emit the last range for each file.
  for data in _prop_data.values():
    (out_file, first_cp_str, last_cp_str, _, last_value) = data
    if first_cp_str == last_cp_str:
      out_file.write(first_cp_str + "\t" + last_value + "\n")
    else:
      out_file.write(first_cp_str + ".." + last_cp_str + "\t" + last_value + "\n")
    out_file.close()


if __name__ == "__main__":
  main()
