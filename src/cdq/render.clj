(ns cdq.render
  (:require [gdl.graphics.camera :as cam]))

(defn set-camera-on-player! [{:keys [gdl.graphics/world-viewport
                                     cdq.context/player-eid]
                               :as context}]
  {:pre [world-viewport
         player-eid]}
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  context)
