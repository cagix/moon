(ns cdq.create.db
  (:require [cdq.db-impl :as db]))

(defn do!
  [ctx]
  (assoc ctx :ctx/db (db/create {:schemas "schema.edn"
                                 :properties "properties.edn"})))
