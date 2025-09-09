(ns cdq.db-complete
  (:require [cdq.db-impl :as db]
            [cdq.schemas-impl :as schemas]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn create [{:keys [schemas properties]}]
  (db/create {:schemas (schemas/->Schemas (-> "schema.edn" io/resource slurp edn/read-string))
              :properties "properties.edn"}))
