# Emoji frequency

## Input Format

[TOC]

Emoji frequency data supplied to the ESC must follow the following format, in a
plain text file.

    Data lines are of the form: <hex> ; <frequency>

        They do not have to be sorted by either field.

    The hex value is of the form \\x{hex1 … hexN}. You can omit the FE0F values
    from the hex (see below); the hex will be normalized when processing the
    data. (This hex format is chosen to be compact yet durable in spreadsheets.)

    The frequency value can be either integer or decimal (eg 57686832 or
    57686831.98). The numbers will be normalized in processing, so that they all
    add up to a particular value (eg 1,000,000,000) for comparison across
    sources.

    Comments. Text after a # are comments; you can put the plain text emoji
    and/or name on each line in a comment if that is easier to read/manage, but
    comments are completely optional and ignored.

Examples

# Data from ABC, 2019-03-17

\\x{1F602} ; 57686831    # 😂 face with tears of joy

\\x{31 FE0F 20E3} ; 139909 # 1️⃣ keycap: 1

\\x{1F3CC FE0F} ; 53769 # 🏌️ person golfing

Variation Selectors. Some emoji normally need variation selectors (FE0F) in
their representation, such as \\x{1F3CC FE0F} (🏌️person golfing). However,
vendors can override this behavior, and show (for example) \\x{1F3CC} as an
emoji. For such cases, the vendor can supply separate frequency information for
the forms with and without the FE0F.

## Processing

Copy the data into a TSV (tab-separated values) file in the folder
DATA/frequency/emoji/==vendor==Raw.tsv, where ==vendor== is one of gboard,
facebook, twitter, etc.

Open EmojiFrequency.java and follow the instructions there, then run.

You'll get files named: Generated/emoji/frequency/...

Copy those into the spreadsheet in the appropriate tab, Raw==Vendor==Snapshot

Example:

Hex     Count   Rank    Emoji

\\x{2B1A}       1565637 1       ⬚

\\x{1F602}      1200855 2       😂

When you do this for the first time after a new release, you'll get 2 kinds of
failures. Here are the fixes:

1.  **RawVendorSnapshot**
    1.  The first column should have a # sign
    2.  The first 4 columns come in from the file, but the columns rows are
        computed. So near the end of the file columns will be missing. Fix that
        by copying columns E-H in the last full row down to the end.
    3.  Many of the computed values will have #N/A in the cells. To fix that:
2.  **EmojiInfo**
    1.  Copy the emojiInfo.tsv file into the **EmojiInfo** tab. That will copy
        columns A-I (Hex .. Year). There is a computed column J that indicates
        whether there is a missing value in the Main tab.
    2.  Copy the one of the Column J cells into the missing cells.
    3.  Sort by Column J, Z-A
    4.  You'll see a batch of #N/A values at the top. That means the hex values
        are missing from the Main sheet.
    5.  Copy the first two columns for all the #N/A values (eg 84 rows)
        1.  Go to MainList sheet, scroll to the bottom, and add 1 more row. In
            the newly empty row, paste the cells from the 84 rows.
        2.  If some of the other Vendor sheets, eg Twitter, are not up to date,
            copy the new cells (last 84), go to that Vendor sheet, and add them
            to the end.
    6.  You'll still be missing some columns in MainList. Scroll back up to the
        last full row, and copy the computed columns (C.. end) down to the end
        of the file.
    7.  All of the #N/A values in columns C..X should be gone (though may will
        be blank
    8.  When you go back to EmojiInfo, the #N/A values should be gone there
        also.

Known problem: the "canonical value for couples" are single characters, but the
code needs fixing to take care of \\x{1F9D1 200D 2764 FE0F 200D 1F48B 200D
1F9D1} and \\x{1F9D1 200D 2764 FE0F 200D 1F9D1}; plus the 'unmarked gender'

TBD: fixing the PIE chart data
