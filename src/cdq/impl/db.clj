(ns cdq.impl.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.schema :as schema]
            [cdq.property :as property]))

(defn- validate-properties! [properties schemas]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! schemas (property/type %) %) properties))

(defn create [properties-path {:keys [cdq/schemas]}]
  (let [properties-file (io/resource properties-path)
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    {:db/data (zipmap (map :property/id properties) properties)
     :db/properties-file properties-file}))
