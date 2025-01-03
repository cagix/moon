(ns cdq.operation
  (:refer-clojure :exclude [apply remove])
  (:require [cdq.op :as op]
            [clojure.math :as math]
            [clojure.string :as str]
            [clojure.utils :refer [k->pretty-name]]))

(defn apply [op value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order op)))

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
                 (str (+? v) (op/value-text component) " " (k->pretty-name k))))
             (sort-by op/order op))))
