# See publish-ucd.yml

UNITOOLS_DATA=unicodetools/data
DRAFT=dist

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

# Update the readmes in-place (-i) as set up above.
find $DRAFT -name '*ReadMe.txt' | xargs sed -i -f $DRAFT/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
rm $DRAFT/UCD/ucd/UCD.zip
(cd $DRAFT/UCD/ucd; zip -r UCD.zip * && mv UCD.zip $DRAFT/zipped)

# Cleanup
rm $DRAFT/sed-readmes.txt
