#!/usr/bin/env bash
brew services restart mongodb-community;
mongo --eval "rs.initiate({_id: \"replocal\", members: [{_id: 0, host: \"127.0.0.1:27017\"}] })"