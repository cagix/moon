(ns cdq.start.load-info
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]))

(defn do! [ctx edn-resource]
  (assoc ctx :ctx/info (-> edn-resource
                           io/resource
                           slurp
                           edn/read-string
                           symbol/require-resolve-symbols)))
