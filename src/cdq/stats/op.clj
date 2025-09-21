(ns cdq.stats.op
  (:refer-clojure :exclude [apply]))

(defmulti apply (fn [[k] base-value] k))

(defmulti order (fn [[k]] k))

(defmethod apply :op/inc
  [[_ value] base-value]
  (+ base-value value))

(defmethod order :op/inc [_]
  0)

(defmethod apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defmethod order :op/mult [_]
  1)
