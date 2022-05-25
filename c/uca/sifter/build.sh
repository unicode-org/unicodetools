#! /bin/sh
# © 2018 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# Build script for the Unicode DUCET "Sifter"

# 1. "make install" ICU, for example with
#   .../runConfigureICU Linux --prefix=/usr/local/google/home/mscherer/icu/mine/inst/icu4c
# 2. Adjust the local-machine variables below, and then do this:
#   .../src/c/uca/sifter$ ./build.sh
# 3. Check the output files in ../../../.. (next to the src tree).

HOME=/usr/local/google/home/mscherer
PREFIX=$HOME/icu/mine/inst/icu4c
ICU_INCLUDE=$PREFIX/include
ICU_LIB=$PREFIX/lib
SIFTER_OUT=../../../..
UNIDATA=unidata.txt

CC=clang
CXX=clang++
# TODO: CXX with --std=c++11
CXXFLAGS="-g -W -Wall -pedantic -Wpointer-arith -Wwrite-strings -Wno-long-long -DU_USING_ICU_NAMESPACE=0"
CPPFLAGS="-DINTEL -DU_NO_DEFAULT_INCLUDE_UTF_HEADERS=1 -DU_DEBUG=1 -D_REENTRANT -DU_HAVE_ELF_H=1 -DUNISTR_FROM_CHAR_EXPLICIT=explicit -DUNISTR_FROM_STRING_EXPLICIT= -I$ICU_INCLUDE"
LINKFLAGS="-L$ICU_LIB -licuuc -licudata -lpthread -ldl -lm"
LDFLAGS=-fsanitize=bounds
# Do not build with the address sanitizer: The sifter has known memory leaks.

$CC $CPPFLAGS $CXXFLAGS $LINKFLAGS $LDFLAGS *.c -o $SIFTER_OUT/sifter && \
LD_LIBRARY_PATH=$ICU_LIB $SIFTER_OUT/sifter -s $UNIDATA && \
LD_LIBRARY_PATH=$ICU_LIB $SIFTER_OUT/sifter -t $UNIDATA

# TODO: add sifter option for output folder
mv allkeys.txt decomps.txt basekeys.txt compkeys.txt ctrckeys.txt ctt14651.txt $SIFTER_OUT

# $ sifter -h
#   To get a synopsis of the command line flags
#
# sifter unidata.txt
#
#   To execute sifter, and just get the basic output: basekeys.txt, compkeys.txt, etc.
#
# sifter -t unidata.txt
#
#   Outputs the decomps.txt file.
#
# sifter -s unidata.txt
#   Does the full sorted output, constructing allkeys.txt, and also outputs 
#   ctt14651.txt with matching values.

# Related misc. stuff --
#
# cd ../../../..
#
# meld src/unicodetools/data/uca/dev/allkeys.txt allkeys.txt
#
# sed -r -f ~/cldr/uni/src/tools/scripts/uca/blankweights.sed src/unicodetools/data/uca/dev/allkeys.txt > allkeys-blanked-ken.txt
# sed -r -f ~/cldr/uni/src/tools/scripts/uca/blankweights.sed allkeys.txt > allkeys-blanked-markus.txt
# meld allkeys-blanked-ken.txt allkeys-blanked-markus.txt
