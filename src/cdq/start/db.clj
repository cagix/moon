(ns cdq.start.db
  (:require [cdq.db :as db]))

(defn do! [ctx]
  (assoc ctx :ctx/db (db/create {:schemas "schema.edn"
                                 :properties "properties.edn"})))
