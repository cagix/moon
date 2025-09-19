; TODO SCHEMA FOR OPS -> can be just {:ops/inc and :ops/mult} ? or multiples?
; then just {:op/inc :op/mult} in one map ?
(ns cdq.stats.ops
  (:require [cdq.stats.op :as op]
            [clojure.math :as math]
            [clojure.string :as str])
  (:refer-clojure :exclude [apply remove]))

(defn apply [ops value]
  (reduce (fn [value op]
            (op/apply op value))
          value
          (sort-by op/order ops)))

(defn add [ops other-ops]
  (merge-with + ops other-ops))

(defn remove [ops other-ops]
  (merge-with - ops other-ops))

(defn info-text [ops k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (case (math/signum v)
                        0.0 ""
                        1.0 "+"
                        -1.0 "")
                      (op/value-text component)
                      " "
                      (str/capitalize (name k)))))
             (sort-by op/order ops))))
