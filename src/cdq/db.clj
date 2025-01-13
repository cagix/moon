(ns cdq.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.schema :as schema]))

(defn create [_context]
  (let [properties-file (io/resource "properties.edn")
        schemas (-> "schema.edn" io/resource slurp edn/read-string)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schema/validate! schemas) properties)
    {:db/data (zipmap (map :property/id properties) properties)
     :db/properties-file properties-file
     :db/schemas schemas}))
