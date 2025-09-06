(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            cdq.ui.select-box
            cdq.ui.stack
            cdq.ui.text-field
            cdq.ui.widget
            )
  (:gen-class))

(defn -main []
  (doseq [[f config] (-> "cdq.start.edn"
                         io/resource
                         slurp
                         edn/read-string)]
    ((requiring-resolve f) config)))
