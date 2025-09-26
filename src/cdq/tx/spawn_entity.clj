(ns cdq.tx.spawn-entity
  (:require [cdq.world :as world]))

(defn do! [{:keys [ctx/world]} entity]
  (world/spawn-entity! world entity))
