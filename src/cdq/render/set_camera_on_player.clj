(ns cdq.render.set-camera-on-player
  (:require [cdq.ctx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-camera-position! graphics
                                 (:body/position (:entity/body @(:world/player-eid world))))
  ctx)
