# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-release
#
# Invoke like this:
#
# pub/copy-final.sh  ~/unitools/mine/src  /tmp/unicode/Public/final

UNICODETOOLS=$1
DEST=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

# Adjust the following for each year and version as needed.
COPY_YEAR=2023
UNI_VER=15.1.0
EMOJI_VER=15.1
# UTS #10 release revision number to be used in CollationTest.html:
# *Two* more than the last release revision number.
TR10_REV=tr10-49

TODAY=`date --iso-8601`

mkdir -p $DEST

cat > $DEST/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$TODAY/
s/PUB_STATUS/final/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s/TR10_REV/$TR10_REV/
s%PUBLIC_EMOJI%Public/emoji/$EMOJI_VER/%
s%PUBLIC_UCD_EMOJI%Public/$UNI_VER/ucd/emoji/%
eof

mkdir -p $DEST/$UNI_VER/ucd
mkdir -p $DEST/zipped/$UNI_VER
cp -r $UNITOOLS_DATA/ucd/dev/* $DEST/$UNI_VER/ucd
rm -r $DEST/$UNI_VER/ucd/Unihan
mv $DEST/$UNI_VER/ucd/version-ReadMe.txt $DEST/$UNI_VER/ReadMe.txt
mv $DEST/$UNI_VER/ucd/zipped-ReadMe.txt $DEST/zipped/$UNI_VER/ReadMe.txt

mkdir -p $DEST/UCA/$UNI_VER
cp -r $UNITOOLS_DATA/uca/dev/* $DEST/UCA/$UNI_VER
sed -i -f $DEST/sed-readmes.txt $DEST/UCA/$UNI_VER/CollationTest.html

mkdir -p $DEST/emoji/$EMOJI_VER
cp $UNITOOLS_DATA/emoji/dev/* $DEST/emoji/$EMOJI_VER

mkdir -p $DEST/idna/$UNI_VER
cp $UNITOOLS_DATA/idna/dev/* $DEST/idna/$UNI_VER

mkdir -p $DEST/idna/idna2008derived
rm $DEST/idna/idna2008derived/*
cp $UNITOOLS_DATA/idna/idna2008derived/Idna2008-$UNI_VER.txt $DEST/idna/idna2008derived
cp $UNITOOLS_DATA/idna/idna2008derived/ReadMe.txt $DEST/idna/idna2008derived

mkdir -p $DEST/security/$UNI_VER
cp $UNITOOLS_DATA/security/dev/* $DEST/security/$UNI_VER

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DEST

# Update the readmes in-place (-i) as set up above.
find $DEST -name '*ReadMe.txt' | xargs sed -i -f $DEST/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DEST/$UNI_VER/ucd/UCD.zip
(cd $DEST/$UNI_VER/ucd; zip -r UCD.zip * && mv UCD.zip $DEST/zipped/$UNI_VER)

rm $DEST/UCA/$UNI_VER/CollationTest.zip
(cd $DEST/UCA/$UNI_VER; zip -r CollationTest.zip CollationTest && rm -r CollationTest)

rm $DEST/security/$UNI_VER/*.zip
(cd $DEST/security/$UNI_VER; zip -r uts39-data-$UNI_VER.zip *)

# Fix permissions again to catch the zip files
chmod a+rX -R $DEST

# Cleanup
rm $DEST/sed-readmes.txt

# Zip file to deliver the whole set of data files
rm $DEST/final.zip
(cd $DEST; zip -r final.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DEST/$UNI_VER/ucd"
echo "- UCDXML files to $DEST/$UNI_VER/ucdxml"
echo "- final charts to $DEST/$UNI_VER/charts"

