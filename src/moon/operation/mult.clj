(ns moon.operation.mult
  (:require [moon.operation :as op]))

(defmethods :op/mult
  (op/value-text [[_ value]]
    (str value "%"))

  (op/apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (op/order [_]
    1))
