# Copyright (c) 2020 and later Unicode, Inc. and others. All Rights Reserved.
# Use -eJDKVERSION=13 -eTOMCATVERSION=8 for example to change these
ARG JDKVERSION=16
FROM openjdk:${JDKVERSION}-alpine AS build
# Need ant, add it. Yes, this pulls in JDK8, but it's an easier
# way to manage this.
RUN apk add --update apache-ant
# Some version of tomcat. Just used for API, does not have to match TOMCATVERSION
ARG TOMCAT_API=https://www.apache.org/dyn/mirrors/mirrors.cgi?action=download&filename=tomcat/tomcat-9/v9.0.37/bin/apache-tomcat-9.0.37.tar.gz
RUN mkdir -p /usr/local/lib && cd /usr/local/lib/ && wget "${TOMCAT_API}" -O - | tar xfpz - && ln -svf apache-tomcat-* ./tomcat

WORKDIR /home
ADD . /home/
ENV CATALINA_HOME /usr/local/lib/tomcat
RUN ant -DCATALINA_HOME=${CATALINA_HOME} war

# ARG TOMCATVERSION=9
# Was not able to parameterize this
FROM jetty:9-jre11-slim AS run
# FROM tomcat:9-jdk14-openjdk-slim-buster AS run
ADD port-entrypoint.sh /port-entrypoint.sh
ENTRYPOINT [ "/port-entrypoint.sh" ]
COPY --from=build /home/UnicodeJsps.war /var/lib/jetty/webapps/
# This is the default PORT. Override by setting PORT.
EXPOSE 8080
