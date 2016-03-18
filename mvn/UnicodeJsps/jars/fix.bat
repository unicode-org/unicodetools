@rem this file is updated on UNIX by fix.sh
mvn install:install-file -Dfile=cldr.jar -Dversion=0.0-LOCAL -DgroupId=org.unicode.localjar -DartifactId=cldr -Dpackaging=jar
mvn install:install-file -Dfile=icu4j.jar -Dversion=0.0-LOCAL -DgroupId=org.unicode.localjar -DartifactId=icu4j -Dpackaging=jar
mvn install:install-file -Dfile=utilities.jar -Dversion=0.0-LOCAL -DgroupId=org.unicode.localjar -DartifactId=utilities -Dpackaging=jar
