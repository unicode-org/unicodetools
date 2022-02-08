#!/bin/sh

#
# Run this to keep the ucd data in src/main/resources up to date.
#

UCD=../unicodetools/data/ucd
DEST=src/main/resources/org/unicode/jsp/bidiref1/ucd
# TODO: revisit after U99.0.0
mkdir -pv ${DEST}
for dir in $(cd ${UCD} && ls -d [6789].*-Update [1-9][0-9].*-Update); do
    ver=$(echo ${dir} | cut -d- -f1)
    for kind in UnicodeData BidiBrackets;
    do
	SRCF=${UCD}/${dir}/${kind}.txt
	DSTF=${DEST}/${kind}-${ver}.txt
	if [ -f ${SRCF} -a ! -f ${DSTF} ];
	then
	   cp -v ${SRCF} ${DSTF}
	fi
    done
done
