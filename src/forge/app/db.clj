(ns ^:no-doc forge.app.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer [validate!]]
            [forge.system :as system]))

(defmethods :app/db
  (system/create [[_ {:keys [schema properties]}]]
    (bind-root #'system/schemas (-> schema io/resource slurp edn/read-string))
    (bind-root #'system/properties-file (io/resource properties))
    (let [properties (-> system/properties-file slurp edn/read-string)]
      (assert (or (empty? properties)
                  (apply distinct? (map :property/id properties))))
      (run! validate! properties)
      (bind-root #'system/properties (zipmap (map :property/id properties) properties)))))
