(ns forge.operation
  (:refer-clojure :exclude [apply]))

(defsystem apply [_ base-value])

(defmethod apply :op/inc [[_ value] base-value]
  (+ base-value value))

(defmethod apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defsystem order)

(defmethod order :op/inc [_]
  0)

(defmethod order :op/mult [_]
  1)
