(ns cdq.ctx.create-db
  (:require [cdq.db :as db]
            [cdq.db.schema-fn-map :as schema-fn-map]))

(defn do! [ctx]
  (assoc ctx :ctx/db (db/create {:schemas "schema.edn"
                                 :properties "properties.edn"
                                 :schema-fn-map schema-fn-map/fn-map})))
