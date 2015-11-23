#!/bin/sh
#ICU_URL=http://download.icu-project.org/files/icu4c/56.1/icu4c-56_1-RHEL6-x64.tgz
# PREFIX=${HOME}/icu-inst
PREFIX=/usr/local/icu
ICU_URL=http://download.icu-project.org/files/icu4c/56.1/icu4c-56_1-src.tgz
ICU_BASE=`basename $ICU_URL`
ICU_BUILD=icu_build
set -x
rm -rf ./${ICU_BUILD} && mkdir ${ICU_BUILD} || exit 1
wget -c ${ICU_URL} || exit 1
cd ${ICU_BUILD} || exit 1
tar xfpz ../${ICU_BASE} || exit 1
( cd icu/source && ./configure --enable-rpath --prefix=${PREFIX} --disable-debug --enable-release --disable-extras --disable-tests && make && make install ) || exit 1
cd ..
# if [ ! -d pkg-config ];
# then
# 	git clone git://anongit.freedesktop.org/pkg-config
# else
# 	(cd pkg-config ; git pull )
# fi


