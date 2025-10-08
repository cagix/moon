(ns cdq.start
  (:require [clojure.config :as config])
  (:gen-class))

(defn -main []
  (let [runs (config/edn-resource "runs.edn")]
    (doseq [[f & params] runs]
      (println [f params])
      (apply (requiring-resolve f) params))))
