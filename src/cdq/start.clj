(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.object :as object])
  (:gen-class))

(defn -main []
  (-> "ctx.edn"
      io/resource
      slurp
      edn/read-string
      (update :functions #(map requiring-resolve %))
      object/pipeline))
