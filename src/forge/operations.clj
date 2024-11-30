(ns forge.operations
  (:refer-clojure :exclude [remove apply])
  (:require [forge.operation :as op]))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))

(defn apply [ops value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order ops)))
