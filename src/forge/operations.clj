(ns forge.operations
  (:refer-clojure :exclude [remove apply]))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))

(defsystem op-apply [_ base-value])

(defmethod op-apply :op/inc [[_ value] base-value]
  (+ base-value value))

(defmethod op-apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defsystem op-order)

(defmethod op-order :op/inc [_]
  0)

(defmethod op-order :op/mult [_]
  1)

(defn apply [ops value]
  (reduce (fn [value op]
            (op-apply op value))
          value
          (sort-by op-order ops)))
