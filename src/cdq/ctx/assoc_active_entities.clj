(ns cdq.ctx.assoc-active-entities
  (:require [cdq.world :as world]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/assoc-active-entities))
