(ns moon.operation.mult
  (:require [moon.operation :as op]))

(defmethods :op/mult
  {:let value}
  (op/value-text [_]
    (str value "%"))

  (op/apply [_ base-value]
    (* base-value (inc (/ value 100))))

  (op/order [_]
    1))
