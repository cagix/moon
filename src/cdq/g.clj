(ns cdq.g
  (:require [cdq.game :as game]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn -main []
  (-> "cdq.game.edn"
      io/resource
      slurp
      edn/read-string
      game/execute!))
