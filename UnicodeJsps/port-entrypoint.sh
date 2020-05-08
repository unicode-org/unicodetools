#!/bin/sh
# This entrypoint is sensitive to the PORT variable.
PORT=${PORT-8080}
export PORT
set -x
exec /docker-entrypoint.sh $* jetty.http.port=${PORT}
