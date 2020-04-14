(ns oplog-pubsub.op
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]))

(defn normalize-collection-name [coll-name]
  (-> coll-name
      (str/replace #"s{1,2}(?:chemas)?$" "")
      (str/replace "project" "group")))

(def log-collection? #{"group"
                       "user"
                       "discussion"
                       "notification"
                       "thread"})

;; https://medium.com/@atharva.inamdar/understanding-mongodb-oplog-249f3996f528
(j/defn normalize-op [^:js {:keys [ts op o o2 fromMigrate ns]}]
  (let [op ({"u" "update"
             "i" "insert"
             "d" "delete"} op)
        collection (some-> ^string ns (.split ".") last normalize-collection-name)]
    (when (and (not fromMigrate)
               (#{"insert" "update" "delete"} op)
               (log-collection? collection))
      (j/lit {:ts (j/get ts :high_)
              :operation op
              :data (case op "insert" o
                             "update" o2
                             "delete" o)
              :collection collection
              :_id (str
                     (case op "insert" (j/get o :_id)
                              "update" (j/get o2 :_id)
                              "delete" (j/get o :_id)))
              :source "mongodb-oplog-pubsub"}))))