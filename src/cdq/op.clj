(ns cdq.op
  (:require [clojure.math :as math]
            [clojure.string :as str])
  (:refer-clojure :exclude [apply remove]))

(defmulti -apply (fn [[k] base-value]
                   k))
(defmulti -order (fn [[k]]
                   k))
(defmulti -value-text (fn [[k]]
                        k))

(defmethod -apply :op/inc
  [[_ value] base-value]
  (+ base-value value))

(defmethod -order :op/inc [_]
  0)

(defmethod -value-text :op/inc
  [[_ value]]
  (str value))

(defmethod -apply :op/mult [[_ value] base-value]
  (* base-value (inc (/ value 100))))

(defmethod -order :op/mult [_]
  1)

(defmethod -value-text :op/mult
  [[_ value]]
  (str value "%"))

(defn apply [op value]
  (reduce (fn [value op]
            (-apply op value))
          value
          (sort-by -order op)))

(defn add [op other-op]
  (merge-with + op other-op))

(defn remove [op other-op]
  (merge-with - op other-op))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info-text [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (-value-text component) " " (str/capitalize (name k)))))
             (sort-by -order op))))
