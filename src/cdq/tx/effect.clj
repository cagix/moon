(ns cdq.tx.effect
  (:require [cdq.world.effect :as effect]))

(defn do! [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))
