(ns oplog-pubsub.env)

(defn env [n] (aget js/process "env" (name n)))
(def MONGODB_URI (env "MONGODB_URI_2020"))
(def MONGODB_OPLOG_URI (env "MONGODB_OPLOG_URI"))
(def FIREBASE_SERVICE_ACCOUNT (env "FIREBASE_SERVICE_ACCOUNT"))
(def PUBSUB_TOPIC (env "PUBSUB_TOPIC"))
(def PUBSUB_CLIENT_ID (env "PUBSUB_CLIENT_ID"))
(def service-account (js/JSON.parse FIREBASE_SERVICE_ACCOUNT))
