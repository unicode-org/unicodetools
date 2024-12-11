# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-ucd-snapshot
#
# Invoke like this:
#
# pub/copy-ucd-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DRAFT=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

# Adjust the following for each year and version as needed.
COPY_YEAR=2024
UNI_VER=17.0.0
EMOJI_VER=17.0

TODAY=`date --iso-8601`

mkdir -p $DRAFT

cat > $DRAFT/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$TODAY/
s/PUB_STATUS/draft/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s%PUBLIC_EMOJI%Public/draft/emoji%
s%PUBLIC_UCD%Public/draft/UCD%
eof

mkdir -p $DRAFT/UCD/ucd
mkdir -p $DRAFT/zipped
cp -r $UNITOOLS_DATA/ucd/dev/* $DRAFT/UCD/ucd
rm -r $DRAFT/UCD/ucd/Unihan
rm -r $DRAFT/UCD/ucd/emoji
mv $DRAFT/UCD/ucd/version-ReadMe.txt $DRAFT/UCD/ReadMe.txt
mv $DRAFT/UCD/ucd/zipped-ReadMe.txt $DRAFT/zipped/ReadMe.txt

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DRAFT

# Update the readmes in-place (-i) as set up above.
find $DRAFT -name '*ReadMe.txt' | xargs sed -i -f $DRAFT/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DRAFT/UCD/ucd/UCD.zip
(cd $DRAFT/UCD/ucd; zip -r UCD.zip * && mv UCD.zip $DRAFT/zipped)

# Cleanup
rm $DRAFT/sed-readmes.txt

rm $DRAFT/ucd-snapshot.zip
(cd $DRAFT; zip -r ucd-snapshot.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DRAFT/UCD/ucd"
echo "- Unihan.zip to $DRAFT/zipped"

