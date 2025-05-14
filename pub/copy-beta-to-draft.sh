# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-beta-snapshot
#
# Invoke like this:
#
# pub/copy-beta-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DRAFT=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

# Adjust the following for each year and version as needed.
COPY_YEAR=2025
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
s%PUBLIC_UCD%Public/draft%
eof

mkdir -p $DRAFT/ucd
cp -r $UNITOOLS_DATA/ucd/dev/* $DRAFT/ucd
rm -r $DRAFT/ucd/Unihan
mv $DRAFT/ucd/version-ReadMe.txt $DRAFT/ReadMe.txt

mkdir -p $DRAFT/uca
cp -r $UNITOOLS_DATA/uca/dev/* $DRAFT/uca

mkdir -p $DRAFT/emoji
cp $UNITOOLS_DATA/emoji/dev/* $DRAFT/emoji

mkdir -p $DRAFT/idna
cp $UNITOOLS_DATA/idna/dev/* $DRAFT/idna

mkdir -p $DRAFT/security
cp $UNITOOLS_DATA/security/dev/* $DRAFT/security

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DRAFT

# Update the readmes in-place (-i) as set up above.
find $DRAFT -name '*ReadMe.txt' | xargs sed -i -f $DRAFT/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DRAFT/ucd/UCD.zip
(cd $DRAFT/ucd; zip -r UCD.zip *)

rm $DRAFT/uca/CollationTest.zip
(cd $DRAFT/uca; zip -r CollationTest.zip CollationTest && rm -r CollationTest)

rm $DRAFT/security/*.zip
(cd $DRAFT/security; zip -r uts39-data-$UNI_VER.zip *)

# Fix permissions again to catch the zip files
chmod a+rX -R $DRAFT

# Cleanup
rm $DRAFT/sed-readmes.txt

# Zip file to deliver the whole set of data files
rm $DRAFT/beta.zip
(cd $DRAFT; zip -r beta.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DRAFT/ucd"
echo "- UCDXML files to $DRAFT/ucdxml"
echo "- beta charts to $DRAFT/charts"

