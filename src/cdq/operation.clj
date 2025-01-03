(ns cdq.operation
  (:refer-clojure :exclude [apply remove])
  (:require [gdl.component :refer [defsystem defcomponent]]
            [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [k->pretty-name]]))

(defsystem ^:private -apply)
(defsystem ^:private -order)
(defsystem ^:private -value-text)

(defcomponent :op/inc
  (-value-text [[_ value]]
    (str value))

  (-apply [[_ value] base-value]
    (+ base-value value))

  (-order [_]
    0))

(defcomponent :op/mult
  (-value-text [[_ value]]
    (str value "%"))

  (-apply [[_ value] base-value]
    (* base-value (inc (/ value 100))))

  (-order [_]
    1))

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

(defn info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (-value-text component) " " (k->pretty-name k))))
             (sort-by -order op))))
