#!/usr/bin/python3 -B
# © 2023 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script copies (changed) generated files
#   * from the <unicodetools_repo>/Generated/UCD/$CURRENT_UVERSION/ tree
#     INTO <unicodetools_repo>/unicodetools/data/ucd/dev
#   * from the <unicodetools_repo>/Generated/security/$CURRENT_UVERSION/ tree
#     INTO <unicodetools_repo>/unicodetools/data/security/dev
#
# This should be run after running UCD.Main to generate files after making changes to certain data
# files (e.g. PropsList.txt) which cause UCD.Main to generate derived files.
#
# Files in the tree that start with "ZZZ-UNCHANGED-" will be skipped.

from pathlib import Path
import os
import sys


def copy_files(gen_dir, data_subdir, out_of_source, cwd, uversion):
    """Copy files from a generated directory to the appropriate data directory."""
    gen_path = (cwd / ".." if out_of_source else cwd) / "Generated" / gen_dir / uversion
    if not gen_path.exists():
        print(f"Note: Generated directory not found at {gen_path.absolute()}")
        return []

    to_move = []
    for p in gen_path.rglob("*.txt"):
        if p.name.startswith("ZZZ-UNCHANGED-") or p.parent.name in ("cldr", "extra"):
            continue
        to_move.append((p, gen_path, data_subdir))

    return to_move


def main():
    out_of_source = '--out-of-source' in sys.argv[1:]
    cwd = Path().cwd()
    uversion = os.getenv("CURRENT_UVERSION")

    # Collect files to move from both UCD and security directories
    to_move = []
    to_move.extend(copy_files("UCD", "ucd", out_of_source, cwd, uversion))
    to_move.extend(copy_files("security", "security", out_of_source, cwd, uversion))

    if not to_move:
        print("No files found to move.")
        return

    print("THE FOLLOWING FILES WILL BE MOVED:\n")
    for p, gen_path, data_subdir in to_move:
        dest_dir = cwd / "unicodetools" / "data" / data_subdir / "dev"
        dest_file = dest_dir / p.relative_to(gen_path)
        print(f"{str(p.name)} --> {dest_file}")

    confirm = bool("-y" in sys.argv[1:])  # enable running this in automation
    if not confirm:
        confirm = input("\nProceed [y/N]?").lower() == "y"

    if confirm:
        for p, gen_path, data_subdir in to_move:
            dest_dir = cwd / "unicodetools" / "data" / data_subdir / "dev"
            dest_file = dest_dir / p.relative_to(gen_path)

            # Create parent directory if it doesn't exist
            dest_file.parent.mkdir(parents=True, exist_ok=True)

            p.rename(dest_file)
            print(f"Moved {p.name} to {dest_file}")


if __name__ == '__main__':
    # pre-check that $CURRENT_UVERSION is defined
    if not os.getenv("CURRENT_UVERSION"):
        raise Exception("CURRENT_UVERSION is not defined.")
    main()
