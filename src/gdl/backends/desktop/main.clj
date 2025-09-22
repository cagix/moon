(ns gdl.backends.desktop.main
  (:require [clojure.config :as config])
  (:gen-class))

(defn execute! [executions]
  (doseq [[f & params] executions]
    (apply f params)))

(defn -main [path]
  (-> path
      config/edn-resource
      execute!))
