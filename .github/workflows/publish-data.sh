# See publish-data.yml

TMP=pub/tmp
mkdir $TMP

PUB_DATE=$(date --iso-8601)

cat > $TMP/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$PUB_DATE/
s/PUB_STATUS/draft/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s/TR10_REV/$TR10_REV/
s%PUBLIC_EMOJI%Public/draft/emoji%
s%PUBLIC_UCD%Public/draft/UCD%
eof

mkdir $TMP/UCD
cp -R unicodetools/data/ucd/dev $TMP/UCD/ucd
mv $TMP/UCD/ucd/version-ReadMe.txt $TMP/UCD/ReadMe.txt
rm -r $TMP/UCD/ucd/Unihan

if [ "$RELEASE_PHASE" = "Dev" ]; then
    rm -r $TMP/UCD/ucd/emoji
fi

if [ "$RELEASE_PHASE" = "Alpha" ] || [ "$RELEASE_PHASE" = "Beta" ]; then
    cp -R unicodetools/data/emoji/dev $TMP/emoji

    cp -R unicodetools/data/idna/dev $TMP/idna

    mkdir $TMP/idna2008derived
    cp unicodetools/data/idna/idna2008derived/ReadMe.txt $TMP/idna2008derived
    cp unicodetools/data/idna/idna2008derived/Idna2008-$UNI_VER.txt $TMP/idna2008derived
fi

if [ "$RELEASE_PHASE" = "Beta" ]; then
    cp -R unicodetools/data/uca/dev $TMP/UCA
    sed -i -f $TMP/sed-readmes.txt $TMP/UCA/CollationTest.html

    cp -R unicodetools/data/security/dev $TMP/security
fi

# Update the readmes in-place (-i) as set up above.
find $TMP -name '*ReadMe.txt' | xargs sed -i -f $TMP/sed-readmes.txt
rm $TMP/sed-readmes.txt

mkdir $TMP/zipped
mv $TMP/UCD/ucd/zipped-ReadMe.txt $TMP/zipped/ReadMe.txt
(cd $TMP/UCD/ucd; zip -r UCD.zip *)
mv $TMP/UCD/ucd/UCD.zip $TMP/zipped

if [ "$RELEASE_PHASE" = "Beta" ]; then
    (cd $TMP/UCA; zip -r CollationTest.zip CollationTest; rm -r CollationTest)

    (cd $TMP/security; zip -r uts39-data-$UNI_VER.zip *)
fi
