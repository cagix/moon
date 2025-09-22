(ns gdl.backends.desktop.main
  (:require [clojure.config :as config])
  (:gen-class))

(defn -main [path]
  (doseq [[f & params] (config/edn-resource path)]
    (apply f params)))
