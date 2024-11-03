(ns moon.operations
  (:refer-clojure :exclude [remove apply])
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [gdl.utils :refer [k->pretty-name]]
            [moon.operation :as op]))

(defn add    [ops other-ops] (merge-with + ops other-ops))
(defn remove [ops other-ops] (merge-with - ops other-ops))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn info [ops k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as op}]
               (when-not (zero? v)
                 (str (+? v) (op/value-text op) " " (k->pretty-name k))))
             (sort-by op/order ops))))

(defn apply [value-ops value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order value-ops)))
