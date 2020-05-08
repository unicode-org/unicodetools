# Copyright (c) 2020 and later Unicode, Inc. and others. All Rights Reserved.
# Use -eJDKVERSION=13 -eTOMCATVERSION=8 for example to change these
ARG JDKVERSION=14
FROM openjdk:${JDKVERSION}-alpine AS build
# Need ant, add it. Yes, this pulls in JDK8, but it's an easier
# way to manage this.
RUN apk add --update apache-ant
# Some version of tomcat. Just used for API, does not have to match TOMCATVERSION
ARG TOMCAT_API=http://apache.mirrors.hoobly.com/tomcat/tomcat-9/v9.0.34/bin/apache-tomcat-9.0.34.tar.gz
RUN mkdir -p /usr/local/lib && cd /usr/local/lib/ && wget ${TOMCAT_API} -O - | tar xfpz - && ln -svf apache-tomcat-* ./tomcat

WORKDIR /home
ADD . /home/
ENV CATALINA_HOME /usr/local/lib/tomcat
RUN ant -DCATALINA_HOME=${CATALINA_HOME} war

# ARG TOMCATVERSION=9
# Was not able to parameterize this
FROM tomcat:9-jdk${JDKVERSION}-openjdk-slim-buster AS run
# FROM tomcat:9-jdk14-openjdk-slim-buster AS run
COPY --from=build /home/UnicodeJsps.war /usr/local/tomcat/webapps/
# TODO PORT! We do not support the PORT variable to control the port.
EXPOSE 8080
