(ns oplog-pubsub.core
  (:require ["eventemitter3" :refer [EventEmitter]]
            ["debug" :as debug]
            ["mongodb" :as mongodb]
            ["/vendor/mongo-oplog/src/index.js" :as MongoOplog]
            ["monk" :as monk]
            [applied-science.js-interop :as j]
            [cljs.pprint :as pp]
            [kitchen-async.promise :as p]
            [oplog-pubsub.env :as env]
            [oplog-pubsub.op :as op]
            [oplog-pubsub.pubsub :as pubsub]))

(defonce db (monk env/MONGODB_URI))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keep track of position

(defonce cursor-coll (j/call db :get "legacyLogCursor"))
(def cursor-client (doto (str env/PUBSUB_TOPIC "-client-" env/PUBSUB_CLIENT_ID) prn))

(defn record-ts! [ts]
  (j/call cursor-coll :findOneAndUpdate
          (j/lit {:client cursor-client})
          (j/lit {:$set {:ts ts}})
          (j/lit {:upsert true})))

(defn get-ts []
  (p/-> cursor-coll
        (j/call :findOne (j/lit {:client cursor-client}))
        (j/get :ts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OpLog

(defonce op-log nil)
(defn make-oplog! []
  (p/let [ts (get-ts)
          ts (or ts (-> (.now js/Date)
                        (/ 1000)
                        Math/round))
          _ (prn :starting-from ts)
          OpLog (MongoOplog env/MONGODB_OPLOG_URI #js{:since (some-> ts inc)
                                                      :libs  #js{:debug   debug
                                                                 :mongodb mongodb
                                                                 :eventemitter3 EventEmitter}})]
    (set! op-log OpLog)
    OpLog))

(defn handle-op! [op]
  (when-let [op (op/normalize-op op)]
    (pubsub/publish! env/PUBSUB_TOPIC op)
    (record-ts! (j/get op :ts))
    (prn [:processed (j/get op :collection) (j/get op :operation)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Runtime state

(defonce will-reload (atom false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System setup

(defn ^:dev/before-load-async stop [done]
  (reset! will-reload true)
  (p/do
    (some-> op-log (j/call :destroy))
    (reset! will-reload false)
    (done)))

(defn quit []
  (stop
    (fn [err] (j/call js/process :exit (if err 1 0)))))

(defn throw-err [err]
  (when err
    (js/console.error err)
    (quit)))

(defn ^:dev/after-load start []
  (println "Starting...")

  (p/do
    (make-oplog!)
    (doto op-log
      (.on "op" handle-op!)
      (.on "error" throw-err)
      (.on "end" (fn [err] (when-not @will-reload (stop #(throw-err err)))))
      (j/call :tail
              (fn [err]
                (if err
                  (js/console.error err)
                  (println "Tailing")))))))

(defn init []
  (doto js/process
    (.on "SIGINT" quit)
    (.on "SIGTERM" quit))
  (j/call-in js/process [:stdout :on] "error" quit)
  (start))



