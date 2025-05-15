(ns cdq.game.set-camera-on-player
  (:require [cdq.camera :as camera]
            [cdq.ctx :as ctx]))

(defn do! []
  (camera/set-position! (:camera (:world-viewport ctx/graphics))
                        (:position @ctx/player-eid)))
