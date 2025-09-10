(ns cdq.tx.effect
  (:require [cdq.effect :as effect]))

(defn do! [ctx effect-ctx effects]
  (mapcat #(effect/handle % effect-ctx ctx)
          (effect/filter-applicable? effect-ctx effects)))
