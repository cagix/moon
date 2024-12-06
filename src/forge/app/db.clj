(ns forge.app.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.core :refer [schemas
                                properties-file
                                validate!
                                db-properties]]
            [forge.utils :refer [bind-root]]))

(defn create [[_ {:keys [schema properties]}]]
  (bind-root schemas (-> schema io/resource slurp edn/read-string))
  (bind-root properties-file (io/resource properties))
  (let [properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! validate! properties)
    (bind-root db-properties (zipmap (map :property/id properties) properties))))
