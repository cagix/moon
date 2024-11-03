(ns moon.operation.inc
  (:require [moon.operation :as op]))

(defmethods :op/inc
  (op/value-text [[_ value]]
    (str value))

  (op/apply [[_ value] base-value]
    (+ base-value value))

  (op/order [_]
    0))
