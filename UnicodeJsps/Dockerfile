# Copyright (c) 2020 and later Unicode, Inc. and others. All Rights Reserved.
FROM alpine as cbuild
ARG CVERSION=13.0.0
ARG CPATH=https://www.unicode.org/Public/PROGRAMS/BidiReferenceC/
WORKDIR /build
RUN apk add --update -q wget make gcc musl-dev
RUN wget -np -nv --reject-regex='.*\.(lib|exe)$' --cut-dirs=4 -nH -r ${CPATH}${CVERSION}/
RUN cd source && gcc -I ../include/ -static -Os -o3 -o bidiref1 bidiref1.c brutils.c brtest.c brtable.c brrule.c
RUN ls -lh /build/source/bidiref1 && (/build/source/bidiref1 || true)
FROM jetty:9-jre11-slim AS run
# FROM tomcat:9-jdk14-openjdk-slim-buster AS run
ADD port-entrypoint.sh /port-entrypoint.sh
ADD ./jetty.d/ROOT /var/lib/jetty/webapps/ROOT/
ENTRYPOINT [ "/port-entrypoint.sh" ]
# copy the .war
ADD target/UnicodeJsps.war /var/lib/jetty/webapps/
# copy the UCD for bidiref1
COPY src/main/resources/org/unicode/jsp/bidiref1/ucd/ /usr/local/share/ucd/
# this is the parent to 'ucd'
ENV BIDIREFHOME /usr/local/share
# copy the bidiref1 bin
ENV BIDIREF1 /usr/local/bin/bidiref1
COPY --from=cbuild /build/source/bidiref1 /usr/local/bin/
# This is the default PORT. Override by setting PORT.
EXPOSE 8080
