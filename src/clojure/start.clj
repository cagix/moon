(ns clojure.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.object :as object]
            [clojure.symbol :as symbol])
  (:gen-class))

(defn return-identity [_ v _db]
  v)

(defn -main []
  (-> "clojure.start.edn"
      io/resource
      slurp
      edn/read-string
      symbol/require-resolve-symbols
      object/pipeline))
