(ns forge.app.db
  (:require [anvil.db :as db]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ {:keys [schema properties]}]]
  (bind-root db/schemas (-> schema io/resource slurp edn/read-string))
  (bind-root db/properties-file (io/resource properties))
  (let [properties (-> db/properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! db/validate! properties)
    (bind-root db/db-data (zipmap (map :property/id properties) properties))))
