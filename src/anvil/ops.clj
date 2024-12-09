(ns anvil.ops
  (:refer-clojure :exclude [apply remove])
  (:require [clojure.component :as component]
            [clojure.utils :refer [defmethods]]))

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

(defn apply [ops value]
  (reduce (fn [value op]
            (component/apply op value))
          value
          (sort-by component/order ops)))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))
