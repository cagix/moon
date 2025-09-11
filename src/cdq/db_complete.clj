(ns cdq.db-complete
  (:require [cdq.db :as db]
            [cdq.schemas :as schemas]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn create [{:keys [schemas properties]}]
  (db/create {:schemas (schemas/->TSchemas (-> "schema.edn" io/resource slurp edn/read-string))
              :properties "properties.edn"}))
