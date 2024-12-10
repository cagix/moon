(ns anvil.op
  (:refer-clojure :exclude [apply])
  (:require [anvil.utils :refer [defsystem  defmethods]]))

(defsystem apply)
(defsystem order)
(defsystem value-text)

(defmethods :op/inc
  (apply [[_ value] base-value]
    (+ base-value value))

  (order [_]
    0))

(defmethods :op/mult
  (apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (order [_]
    1))

