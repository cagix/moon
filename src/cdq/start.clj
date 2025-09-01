(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (let [fns (->> "cdq.start.edn"
                 io/resource
                 slurp
                 edn/read-string
                 (map requiring-resolve))]
    (doseq [f fns]
      (f))))
