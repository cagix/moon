(ns cdq.render.set-camera-on-player
  (:require [cdq.graphics.camera :as camera]))

(defn render
  [{:keys [cdq.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  {:pre [world-viewport
         player-eid]}
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)
