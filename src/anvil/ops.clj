(ns anvil.ops
  (:refer-clojure :exclude [remove])
  (:require [anvil.system :as system]
            [clojure.utils :refer [defmethods]]))

(defmethods :op/inc
  (system/apply [[_ value] base-value]
    (+ base-value value))

  (system/order [_]
    0))

(defmethods :op/mult
  (system/apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (system/order [_]
    1))

(defn apply [ops value]
  (reduce (fn [value op]
            (system/apply op value))
          value
          (sort-by system/order ops)))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))
