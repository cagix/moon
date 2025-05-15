(ns cdq.game.set-camera-on-player
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]))

(defn do! []
  (camera/set-position! (:camera ctx/world-viewport)
                        (:position @ctx/player-eid)))
