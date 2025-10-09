(ns cdq.game.render.set-camera-on-player
  (:require [cdq.graphics :as graphics]))

(defn step
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-position! graphics
                          (:body/position
                           (:entity/body
                            @(:world/player-eid world))))
  ctx)
