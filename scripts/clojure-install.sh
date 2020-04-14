#!/usr/bin/env bash

PREFIX=$(pwd)/.heroku/node
FILENAME="linux-install-1.10.1.536.sh"
FILEPATH=$(pwd)/${FILENAME}
LOCAL_BIN=$(pwd)/node_modules/.bin

# brittle, depends on the downloaded install script
curl "https://download.clojure.org/install/${FILENAME}" > ${FILEPATH} && \

chmod +x ${FILEPATH} && \
${FILEPATH} -p $PREFIX && \

rm ${FILEPATH} && \
echo "Finished Clojure Installation"