(ns cdq.levelgen
  (:require [cdq.create.db :as create.db]
            [cdq.level.modules :as modules]))

(defrecord Context [])

(defn create! [config]
  ; 1. generate level
  (let [ctx (->Context)
        ctx (assoc ctx :ctx/config
                   {:db {:schemas "schema.edn"
                         :properties "properties.edn"}})
        ctx (create.db/do! ctx)
        level (modules/create ctx)]
    (println level)))

(defn dispose! [])

(defn render! [])

(defn resize! [width height])
