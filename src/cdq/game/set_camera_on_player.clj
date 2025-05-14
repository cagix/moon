(ns cdq.game.set-camera-on-player
  (:require [cdq.ctx :as ctx]
            [cdq.graphics.camera :as camera]))

(defn do! []
  (camera/set-position! (:camera (:world-viewport ctx/graphics))
                        (:position @ctx/player-eid)))
