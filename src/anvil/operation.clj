(ns anvil.operation
  (:refer-clojure :exclude [remove])
  (:require [anvil.component :as component]
            [gdl.utils :refer [defmethods]]))

(defmethods :op/inc
  (component/apply [[_ value] base-value]
    (+ base-value value))

  (component/order [_]
    0))

(defmethods :op/mult
  (component/apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (component/order [_]
    1))

(defn apply [op value]
  (reduce (fn [value op]
            (component/apply op value))
          value
          (sort-by component/order op)))

(defn add    [op other-op] (merge-with + op other-op))
(defn remove [op other-op] (merge-with - op other-op))
