mongodb-oplog-publisher
=============================

Streams a MongoDB oplog to a Google Pub/Sub topic.

Each published operation is an object containing:

- `:ts` - timestamp (in seconds)
- `:operation` - #{"create", "update", "insert"}
- `:data` - for inserts/deletes, the whole document. for updates, an object containing {:$set {...fields}, :$unset {...fields}}
- `:collection` - #{"group", "user", "discussion", "notification", "thread"}
- `:_id` - document id (string)
- `:source` - "mongodb-oplog-pubsub"

Required environment variables:

```
MONGODB_URI              # for reading docs
MONGODB_OPLOG_URI        # for reading oplog, see https://docs.mlab.com/oplog/
MONGODB_LOGGING_URI      # for writing audit log
FIREBASE_SERVICE_ACCOUNT # for writing to Google PubSub topic
PUBSUB_TOPIC             # the name of the topic changes are published to
```

In dev, these can be supplied inside an `.env` file,
then start the worker via `yarn dev`.

If you have installed mongodb-community via brew, start a local mongodb replica set via:

```
./scripts/start-replSet.sh
```