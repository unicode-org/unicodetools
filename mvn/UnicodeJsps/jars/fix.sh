#!/bin/sh
cat >fix.bat <<EOF
@rem this file is updated on UNIX by fix.sh
EOF

for jar in *.jar;
do
	CMD="mvn install:install-file -Dfile=${jar} -Dversion=0.0-LOCAL -DgroupId=org.unicode.localjar -DartifactId=$(basename ${jar} .jar) -Dpackaging=jar"
	echo ${CMD} >> fix.bat
	echo "# ${CMD}"
	${CMD}
done
