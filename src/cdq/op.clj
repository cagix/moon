(ns cdq.op
  (:require [clojure.component :refer [defsystem defcomponent]]))

(defsystem apply)
(defsystem order)
(defsystem value-text)

(defcomponent :op/inc
  (value-text [[_ value]]
    (str value))

  (apply [[_ value] base-value]
    (+ base-value value))

  (order [_]
    0))

(defcomponent :op/mult
  (value-text [[_ value]]
    (str value "%"))

  (apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (order [_]
    1))
