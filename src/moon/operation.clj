(ns moon.operation
  (:require [forge.component :refer [defsystem]])
  (:refer-clojure :exclude [apply]))

(defsystem value-text)

(defsystem apply [_ base-value])

(defsystem order)

(defmethod value-text :op/inc [[_ value]] (str value))
(defmethod apply      :op/inc [[_ value] base-value] (+ base-value value))
(defmethod order      :op/inc [_] 0)

(defmethod value-text :op/mult [[_ value]] (str value "%"))
(defmethod apply      :op/mult [[_ value] base-value] (* base-value (inc (/ value 100))))
(defmethod order      :op/mult [_] 1)
