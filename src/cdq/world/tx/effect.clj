(ns cdq.world.tx.effect
  (:require [cdq.effect :as effect]))

(defn do! [{:keys [ctx/world]} effect-ctx effects]
  (mapcat #(effect/handle % effect-ctx world)
          (filter #(effect/applicable? % effect-ctx) effects)))
