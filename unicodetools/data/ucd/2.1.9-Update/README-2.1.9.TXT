April 28, 1999

This directory contains the fourth update for Unicode 2.1.

The fourth update consists of corrections to the Unicode
Character Database and contributory data files. 
The current version is 2.1.9, updated
from the last publicly released version 2.1.8, which is
posted in the 2.1-Update3 directory.

UnicodeData-2.1.9.txt (the Unicode Character Database)
ReadMe-2.1.9.txt (the explanation file for the database)
- see that file for the list of corrections to the Unicode
  Character Database.

PropList-2.1.9.txt (informative properties list)

SpecialCasing-2.txt (informative list of special casing)
- corrected production problems that resulted in incorrect
  mappings in the 2.1.8 update.

Blocks-2.txt (normative list of block names)
- corrected block boundaries for Specials (including U+FEFF)
  and added newline to the end of the file.

Jamo-2.txt (normative list of Jamo short names)
- corrected the short name for an initial RIEUL.

diff218v219d2.txt
diff219d2v219d3.txt
diff219d3v219d4.txt
diff219d4v219d5.txt
diff219d5v219d9.txt diff files for the accumulated differences
                    between 2.1.8 and 2.1.9 versions.


Note regarding NamesList.txt
- The information in NamesList.txt file matches the Unicode 2.0 book.
  Since then, the character decompositions and block names have changed
  and the 20AC EURO SIGN and FFFC OBJECT REPLACEMENT CHARACTER were added.
  Look at UnicodeData.txt and Blocks.txt for the correct data.
  Since NamesList.txt is primarily of archival interest, it has
  not been updated to match the current status of Unicode 2.1.
