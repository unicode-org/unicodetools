#!/usr/bin/python3 -B
# © 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script copies (changed) generated UCD files
#   * from the <unicodetools_repo>/Generated/UCD/$CURRENT_UVERSION/ tree
#   * INTO <unicodetools_repo>/unicodetools/data/dev
#
# This should be run after running UCD.Main to generate files after making changes to certain data
# files (e.g. PropsList.txt) which cause UCD.Main to generate derived files.
#
# Files in the tree that start with "ZZZ-UNCHANGED-" will be skipped.

from pathlib import Path
import os
import sys


def main():
    out_of_source = '--out-of-source' in sys.argv[1:]
    print(sys.argv,file=sys.stderr)
    print(out_of_source,file=sys.stderr)
    cwd = Path().cwd()
    uversion = os.getenv("CURRENT_UVERSION")
    genucddir = (cwd / ".." if out_of_source else cwd) / "Generated" / "UCD" / uversion
    if not genucddir.exists():
        raise Exception(f"Generated directory not found at {genucddir.absolute()}")

    to_move = []
    for p in genucddir.rglob("*.txt"):
        if p.name.startswith("ZZZ-UNCHANGED-") or p.parent.name in ("cldr", "extra"):
            continue
        to_move.append(p)

    if to_move:
        devucddir = cwd / "unicodetools" / "data" / "ucd" / "dev"
        print("THE FOLLOWING FILES WILL BE MOVED:\n")
        print("\n".join([f"{str(p.name)} --> {devucddir / p.relative_to(genucddir)}" for p in to_move]))  # noqa: E501

        confirm = bool("-y" in sys.argv[1:])  # enable running this in automation
        if not confirm:
            confirm = input("\nProceed [y/N]?").lower() == "y"

        if confirm:
            for mf in to_move:
                dst = devucddir / mf.relative_to(genucddir)
                mf.rename(dst)


if __name__ == '__main__':
    # pre-check that $CURRENT_UVERSION is defined
    if not os.getenv("CURRENT_UVERSION"):
        raise Exception("CURRENT_UVERSION is not defined.")
    main()
