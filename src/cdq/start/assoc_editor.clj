(ns cdq.start.assoc-editor
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]))

(defn do! [ctx edn-resource]
  (assoc ctx :ctx/editor (-> edn-resource
                             io/resource
                             slurp
                             edn/read-string
                             symbol/require-resolve-symbols)))
