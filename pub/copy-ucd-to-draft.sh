# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-ucd-snapshot
#
# Invoke like this:
#
# pub/copy-ucd-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DEST=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

# Adjust the following for each year and version as needed.
COPY_YEAR=2025
UNI_VER=17.0.0
EMOJI_VER=17.0

TODAY=`date --iso-8601`

mkdir -p $DEST

cat > $DEST/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$TODAY/
s/PUB_STATUS/draft/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s%PUBLIC_EMOJI%Public/draft/emoji%
s%PUBLIC_UCD%Public/draft%
eof

mkdir -p $DEST/ucd
cp -r $UNITOOLS_DATA/ucd/dev/* $DEST/ucd
rm -r $DEST/ucd/Unihan
rm -r $DEST/ucd/emoji
mv $DEST/ucd/version-ReadMe.txt $DEST/ReadMe.txt

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DEST

# Update the readmes in-place (-i) as set up above.
find $DEST -name '*ReadMe.txt' | xargs sed -i -f $DEST/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DEST/ucd/UCD.zip
(cd $DEST/ucd; zip -r UCD.zip *)

# Cleanup
rm $DEST/sed-readmes.txt

rm $DEST/ucd-snapshot.zip
(cd $DEST; zip -r ucd-snapshot.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DEST/ucd"

