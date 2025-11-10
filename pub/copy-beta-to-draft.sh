# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-beta-snapshot
#
# Invoke like this:
#
# pub/copy-beta-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DEST=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

# Adjust the following for each year and version as needed.
COPY_YEAR=2025
UNI_VER=18.0.0
EMOJI_VER=18.0

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
mv $DEST/ucd/version-ReadMe.txt $DEST/ReadMe.txt

mkdir -p $DEST/uca
cp -r $UNITOOLS_DATA/uca/dev/* $DEST/uca

mkdir -p $DEST/emoji
cp $UNITOOLS_DATA/emoji/dev/* $DEST/emoji

mkdir -p $DEST/idna
cp $UNITOOLS_DATA/idna/dev/* $DEST/idna

mkdir -p $DEST/security
cp $UNITOOLS_DATA/security/dev/* $DEST/security

mkdir -p $DEST/links
cp $UNITOOLS_DATA/links/dev/* $DEST/links

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DEST

# Update the readmes in-place (-i) as set up above.
find $DEST -name '*ReadMe.txt' | xargs sed -i -f $DEST/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DEST/ucd/UCD.zip
(cd $DEST/ucd; zip -r UCD.zip *)

rm $DEST/uca/CollationTest.zip
(cd $DEST/uca; zip -r CollationTest.zip CollationTest && rm -r CollationTest)

rm $DEST/security/*.zip
(cd $DEST/security; zip -r uts39-data-$UNI_VER.zip *)

# Fix permissions again to catch the zip files
chmod a+rX -R $DEST

# Cleanup
rm $DEST/sed-readmes.txt

# Zip file to deliver the whole set of data files
rm $DEST/beta.zip
(cd $DEST; zip -r beta.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DEST/ucd"
echo "- UCDXML files to $DEST/ucdxml"
echo "- beta charts to $DEST/charts"

