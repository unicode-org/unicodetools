#!/usr/bin/python3 -B
# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script checks .java source files for the presence of tab characters. If a tab is encountered,
# the script will print the name (path) of the file. If any file(s) had tabs, the script will exit
# with a non-zero status code (unless the --exit-zero option is specified, which forces a
# zero/success status code return).

import argparse
from pathlib import Path
import sys

def main() -> bool:
    ap = argparse.ArgumentParser()
    ap.add_argument("ROOT_DIR",
                    nargs='?',
                    default=Path.cwd(),
                    help="Top-level directory which will be recursively searched "
                         "for .java files to be checked.")
    ap.add_argument('--exit-zero',
                    action='store_true',
                    help="Force exit with status code 0 even if there are errors.")

    args = ap.parse_args()
    root = Path(args.ROOT_DIR)

    if not root.exists():
        print(f'{root.absolute()}: directory not found; aborting.')
        return False

    error_count = 0
    for p in root.rglob("*.java"):
        with open(p, 'r') as f:
            s = f.read()
            if '\t' in s:
                print(f"tabs found in {p.relative_to(root)}", flush=True)
                error_count += 1

    return args.exit_zero or not(bool(error_count))


if __name__ == '__main__':
    ok = main()
    if not ok:
        sys.exit("Errors encountered.")
