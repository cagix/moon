(ns cdq.schemas
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn create [path]
  (->> path
       io/resource
       slurp
       edn/read-string))
