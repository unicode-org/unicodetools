# Script for
# https://github.com/unicode-org/unicodetools/blob/main/docs/data-workflow.md#publish-a-beta-snapshot
#
# Invoke like this:
#
# pub/copy-beta-to-draft.sh  ~/unitools/mine/src  /tmp/unicode/Public/draft

UNICODETOOLS=$1
DRAFT=$2

UNITOOLS_DATA=$UNICODETOOLS/unicodetools/data

mkdir -p $DRAFT/UCD/ucd
cp -r $UNITOOLS_DATA/ucd/dev/* $DRAFT/UCD/ucd
rm -r $DRAFT/UCD/ucd/Unihan
mv $DRAFT/UCD/ucd/version-ReadMe.txt $DRAFT/UCD/ReadMe.txt
rm $DRAFT/UCD/ucd/zipped-ReadMe.txt

mkdir -p $DRAFT/UCA
cp -r $UNITOOLS_DATA/uca/dev/* $DRAFT/UCA

mkdir -p $DRAFT/emoji
cp $UNITOOLS_DATA/emoji/dev/* $DRAFT/emoji

mkdir -p $DRAFT/idna
cp $UNITOOLS_DATA/idna/dev/* $DRAFT/idna

mkdir -p $DRAFT/idna2008derived
rm $DRAFT/idna2008derived/*
cp $UNITOOLS_DATA/idna/idna2008derived/Idna2008-15.1.0.txt $DRAFT/idna2008derived
cp $UNITOOLS_DATA/idna/idna2008derived/ReadMe.txt $DRAFT/idna2008derived

mkdir -p $DRAFT/security
cp $UNITOOLS_DATA/security/dev/* $DRAFT/security

# Fix permissions. Everyone can read, and search directories.
chmod a+rX -R $DRAFT

# Zip files for some types of data, after fixing permissions
rm $DRAFT/UCA/CollationTest.zip
(cd $DRAFT/UCA; zip -r CollationTest.zip CollationTest && rm -r CollationTest)

rm $DRAFT/security/*.zip
(cd $DRAFT/security; zip -r uts39-data-15.1.0.zip *)

# Fix permissions again to catch the zip files
chmod a+rX -R $DRAFT

# Zip file to deliver the whole set of beta data files
rm $DRAFT/beta.zip
(cd $DRAFT; zip -r beta.zip *)

echo "--------------------"
echo "Copy files from elsewhere:"
echo "- Unihan.zip to $DRAFT/UCD/ucd"
echo "- beta charts to $DRAFT/UCD/charts"

