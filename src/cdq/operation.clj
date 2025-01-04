(ns cdq.operation
  (:refer-clojure :exclude [apply remove])
  (:require [gdl.utils :refer [defsystem defcomponent]]))

(defsystem -apply)
(defsystem -order)

(defcomponent :op/inc
  (-apply [[_ value] base-value]
    (+ base-value value))

  (-order [_]
    0))

(defcomponent :op/mult
  (-apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (-order [_]
    1))

(defn apply [op value]
  (reduce (fn [value op]
            (-apply op value))
          value
          (sort-by -order op)))

(defn add [op other-op]
  (merge-with + op other-op))

(defn remove [op other-op]
  (merge-with - op other-op))
