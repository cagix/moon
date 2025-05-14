(ns cdq.g
  (:require [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn -main []
  (-> "cdq.game.edn"
      io/resource
      slurp
      edn/read-string
      utils/execute!))
