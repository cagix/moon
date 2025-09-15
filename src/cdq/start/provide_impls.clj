(ns cdq.start.provide-impls
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]
            clojure.provide))

(defn do! [ctx impls-resource]
  (clojure.provide/do! (-> impls-resource
                           io/resource
                           slurp
                           edn/read-string
                           symbol/require-resolve-symbols))
  ctx)
