(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [anvil.app :as app]))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      app/start))
