(ns cdq.stats.op
  (:refer-clojure :exclude [apply]))

(defmulti apply (fn [[k] base-value] k))

(defmulti order (fn [[k]] k))

(defmulti value-text (fn [[k]] k))

(defmethod apply :op/inc
  [[_ value] base-value]
  (+ base-value value))

(defmethod order :op/inc [_]
  0)

(defmethod value-text :op/inc
  [[_ value]]
  (str value))

(defmethod apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defmethod order :op/mult [_]
  1)

(defmethod value-text :op/mult
  [[_ value]]
  (str value "%"))
