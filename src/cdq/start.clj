(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.app :as app])
  (:gen-class))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      app/start))
