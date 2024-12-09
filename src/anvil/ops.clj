(ns anvil.ops
  (:refer-clojure :exclude [remove apply])
  (:require [clojure.utils :refer [defsystem defmethods]]))

(defsystem op-apply)

(defsystem op-order)

(defmethods :op/inc
  (op-apply [[_ value] base-value]
    (+ base-value value))

  (op-order [_]
    0))

(defmethods :op/mult
  (op-apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (op-order [_]
    1))

(defn apply [ops value]
  (reduce (fn [value op]
            (op-apply op value))
          value
          (sort-by op-order ops)))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))
