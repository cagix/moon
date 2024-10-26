(ns moon.operations
  (:refer-clojure :exclude [remove apply])
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [safe-remove-one update-kv k->pretty-name]]
            [moon.operation :as op]))

(defn add    [ops value-ops] (update-kv conj            ops value-ops))
(defn remove [ops value-ops] (update-kv safe-remove-one ops value-ops))

(defn sum-vals [ops]
  (for [[k values] ops
        :let [value (clojure.core/apply + values)]
        :when (not (zero? value))]
    [k value]))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info-text [value-ops k]
  (str/join "\n"
            (for [[k v] (sort-by op/order value-ops)]
              (str (+? v) (op/value-text [k v]) " " (k->pretty-name k)))))

(defn apply [value-ops value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order value-ops)))
