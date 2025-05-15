(ns cdq.game.set-camera-on-player
  (:require [cdq.ctx :as ctx]
            [clojure.graphics.camera :as camera]))

(defn do! []
  (camera/set-position! (:camera ctx/world-viewport)
                        (:position @ctx/player-eid)))
