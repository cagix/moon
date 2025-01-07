(ns cdq.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]))

(defn -main []
  (-> "gdl.app.edn"
      io/resource
      slurp
      edn/read-string
      app/start))
