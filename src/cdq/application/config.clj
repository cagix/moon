(ns cdq.application.config
  (:refer-clojure :exclude [load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load [path]
  (-> path
      io/resource
      slurp
      edn/read-string))
