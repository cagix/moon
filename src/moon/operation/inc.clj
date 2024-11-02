(ns moon.operation.inc
  (:require [moon.operation :as op]))

(defmethods :op/inc
  {:let value}
  (op/value-text [_]
    (str value))

  (op/apply [_ base-value]
    (+ base-value value))

  (op/order [_]
    0))
