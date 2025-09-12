(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn reduce-over [object functions]
  (reduce (fn [object f]
            (f object))
          object
          functions))

(defn -main []
  (let [{:keys [context
                pipeline]} (-> "ctx.edn" io/resource slurp edn/read-string)]
    (assert (map? context))
    (assert (vector? pipeline))
    (reduce-over context
                 (map requiring-resolve pipeline))))
