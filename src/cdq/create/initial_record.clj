(ns cdq.create.initial-record
  (:require [qrecord.core :as q]))

(q/defrecord Context [ctx/graphics])

(defn merge-into-record [ctx]
  (merge (map->Context {})
         ctx))
