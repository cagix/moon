(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:gen-class))

(defn -main []
  (doseq [[f config] (-> "cdq.start.edn"
                         io/resource
                         slurp
                         edn/read-string)]
    (println f)
    ((requiring-resolve f) config)))
