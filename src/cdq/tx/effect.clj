(ns cdq.tx.effect
  (:require [cdq.effect :as effect]))

(defn do! [[_ effect-ctx effects] ctx]
  (mapcat #(effect/handle % effect-ctx ctx)
          (effect/filter-applicable? effect-ctx effects)))
