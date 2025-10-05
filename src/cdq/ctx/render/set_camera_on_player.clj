(ns cdq.ctx.render.set-camera-on-player
  (:require [cdq.graphics.camera :as camera]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (camera/set-position! graphics
                        (:body/position
                         (:entity/body
                          @(:world/player-eid world))))
  ctx)
