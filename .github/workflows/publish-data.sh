# See publish-ucd.yml

mkdir dist

PUB_DATE=$(date --iso-8601)

cat > dist/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$PUB_DATE/
s/PUB_STATUS/draft/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s/TR10_REV/$TR10_REV/
s%PUBLIC_EMOJI%Public/draft/emoji%
s%PUBLIC_UCD%Public/draft/UCD%
eof

mkdir dist/UCD
cp -R unicodetools/data/ucd/dev dist/UCD/ucd
mv dist/UCD/ucd/version-ReadMe.txt dist/UCD/ReadMe.txt
rm -r dist/UCD/ucd/Unihan

if [ "$MODE" = "Snapshot" ]; then
    rm -r dist/UCD/ucd/emoji
fi

if [ "$MODE" = "Alpha" ] || [ "$MODE" = "Beta" ]; then
    cp -R unicodetools/data/emoji/dev dist/emoji

    cp -R unicodetools/data/idna/dev dist/idna

    mkdir dist/idna2008derived
    cp unicodetools/data/idna/idna2008derived/ReadMe.txt dist/idna2008derived
    cp unicodetools/data/idna/idna2008derived/Idna2008-$UNI_VER.txt dist/idna2008derived
fi

if [ "$MODE" = "Beta" ]; then
    cp -R unicodetools/data/uca/dev dist/UCA
    sed -i -f dist/sed-readmes.txt dist/UCA/CollationTest.html

    cp -R unicodetools/data/security/dev dist/security
fi

# Update the readmes in-place (-i) as set up above.
find dist -name '*ReadMe.txt' | xargs sed -i -f dist/sed-readmes.txt
rm dist/sed-readmes.txt

mkdir dist/zipped
mv dist/UCD/ucd/zipped-ReadMe.txt dist/zipped/ReadMe.txt
(cd dist/UCD/ucd; zip -r UCD.zip *)
mv dist/UCD/ucd/UCD.zip dist/zipped

if [ "$MODE" = "Beta" ]; then
    (cd dist/UCA; zip -r CollationTest.zip CollationTest; rm -r CollationTest)

    (cd dist/security; zip -r uts39-data-$UNI_VER.zip *)
fi
