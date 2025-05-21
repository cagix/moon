(ns cdq.render.set-camera-on-player
  (:require [gdl.graphics.camera :as camera]))

(defn do! [{:keys [ctx/world-viewport
                   ctx/player-eid]}]
  (camera/set-position! (:camera world-viewport)
                        (:position @player-eid))
  nil)
