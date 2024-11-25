(ns moon.operation
  (:refer-clojure :exclude [apply])
  (:require [forge.system :refer [defsystem]]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

(defmethod value-text :op/inc [[_ value]] (str value))
(defmethod apply      :op/inc [[_ value] base-value] (+ base-value value))
(defmethod order      :op/inc [_] 0)

(defmethod value-text :op/mult [[_ value]] (str value "%"))
(defmethod apply      :op/mult [[_ value] base-value] (* base-value (inc (/ value 100))))
(defmethod order      :op/mult [_] 1)
