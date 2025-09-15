(ns clojure.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       symbol/require-resolve-symbols))
