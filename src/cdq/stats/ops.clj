; TODO SCHEMA FOR OPS -> can be just {:ops/inc and :ops/mult} ? or multiples?
; then just {:op/inc :op/mult} in one map ?
(ns cdq.stats.ops
  (:require [cdq.stats.op :as op])
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
