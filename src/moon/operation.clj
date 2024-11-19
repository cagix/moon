(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [moon.system :refer [defsystem defmethods]]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

(defmethods :op/inc
  (value-text [[_ value]]
    (str value))

  (apply [[_ value] base-value]
    (+ base-value value))

  (order [_]
    0))

(defmethods :op/mult
  (value-text [[_ value]]
    (str value "%"))

  (apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (order [_]
    1))
