(ns cdq.game.render.update-potential-fields
  (:require [cdq.world :as world]))

(defn step
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))
