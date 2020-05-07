#!/usr/bin/python3 -B
# © 2020 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Takes one filename command-line argument and changes CR LF and CR
# line endings to LF.

import sys

CR = 0xd
LF = 0xa

def main():
  filename = sys.argv[1]
  with open(filename, "rb") as f: data = f.read()
  buffer = bytearray(data)
  changed = False
  i = 0
  while i < len(buffer):
    j = i + 1
    if buffer[i] == CR:
      if j < len(buffer) and buffer[j] == LF:
        # Delete CR from CR LF.
        del buffer[i]
      else:
        # Replace CR with LF.
        buffer[i] = LF
      changed = True
    i = j
  if changed:
    print("eol2lf:", filename)
    with open(filename, "wb") as f: f.write(buffer)


if __name__ == "__main__":
  main()
