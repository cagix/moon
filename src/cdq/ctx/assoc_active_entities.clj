(ns cdq.ctx.assoc-active-entities
  (:require [cdq.world.assoc-active-entities :as assoc-active-entities]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world assoc-active-entities/do!))
