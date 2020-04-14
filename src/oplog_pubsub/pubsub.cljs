(ns oplog-pubsub.pubsub
  (:require ["@google-cloud/pubsub" :refer [PubSub]]
            [applied-science.js-interop :as j]
            [oplog-pubsub.env :as env]))

(defonce pubsub (new PubSub
                     (j/lit {:projectId (j/get env/service-account :project_id)
                             :credentials env/service-account})))

(defn publish!
  "Publish `data` to `topic-name`"
  [topic-name data]
  (-> pubsub
      (j/call :topic topic-name)
      (j/call-in [:publisher :publish] (-> data (js/JSON.stringify) (js/Buffer.from)))))