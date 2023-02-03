# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-an-alpha-snapshot
#
# Invoke like this:
#
# pub/copy-alpha-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DRAFT=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

mkdir -p $DRAFT/UCD/ucd
cp -r $UNITOOLS_DATA/ucd/dev/* $DRAFT/UCD/ucd
rm -r $DRAFT/UCD/ucd/Unihan
mv $DRAFT/UCD/ucd/version-ReadMe.txt $DRAFT/UCD/ReadMe.txt
rm $DRAFT/UCD/ucd/zipped-ReadMe.txt

mkdir -p $DRAFT/emoji
cp $UNITOOLS_DATA/emoji/dev/* $DRAFT/emoji

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DRAFT/UCD/ucd"
echo "- alpha charts to $DRAFT/UCD/charts"

