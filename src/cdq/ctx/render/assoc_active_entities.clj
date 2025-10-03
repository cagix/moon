(ns cdq.ctx.render.assoc-active-entities
  (:require [cdq.world :as world]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))
