(ns clojure.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})))
