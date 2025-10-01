(ns cdq.ctx.update-potential-fields
  (:require [cdq.world.update-potential-fields :as update-potential-fields]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (update-potential-fields/do! world)
     ctx)))
