# See publish-ucd.yml

TODAY=`date --iso-8601`

mkdir dist

cat > dist/sed-readmes.txt << eof
s/COPY_YEAR/$COPY_YEAR/
s/PUB_DATE/$TODAY/
s/PUB_STATUS/draft/
s/UNI_VER/$UNI_VER/
s/EMOJI_VER/$EMOJI_VER/
s%PUBLIC_EMOJI%Public/draft/emoji%
s%PUBLIC_UCD%Public/draft/UCD%
eof

mkdir -p dist/UCD/ucd
mkdir dist/zipped
cp -r unicodetools/data/ucd/dev/* dist/UCD/ucd
rm -r dist/UCD/ucd/Unihan
mv dist/UCD/ucd/version-ReadMe.txt dist/UCD/ReadMe.txt
mv dist/UCD/ucd/zipped-ReadMe.txt dist/zipped/ReadMe.txt

if [ $MODE = "Alpha" ]; then
    mkdir dist/emoji
    cp unicodetools/data/emoji/dev/* dist/emoji

    mkdir dist/idna
    cp unicodetools/data/idna/dev/* dist/idna

    mkdir dist/idna2008derived
    cp unicodetools/data/idna/idna2008derived/Idna2008-$UNI_VER.txt dist/idna2008derived
    cp unicodetools/data/idna/idna2008derived/ReadMe.txt dist/idna2008derived
else
    rm -r dist/UCD/ucd/emoji
fi

# Update the readmes in-place (-i) as set up above.
find dist -name '*ReadMe.txt' | xargs sed -i -f dist/sed-readmes.txt

# Zip files for some types of data, after fixing permissions
(cd dist/UCD/ucd; zip -r UCD.zip *)
mv dist/UCD/ucd/UCD.zip dist/zipped

# Cleanup
rm dist/sed-readmes.txt
