(ns cdq.application.render.set-camera-on-player
  (:require [cdq.entity :as entity]
            [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-camera-position! graphics (entity/position @(:world/player-eid world)))
  ctx)
