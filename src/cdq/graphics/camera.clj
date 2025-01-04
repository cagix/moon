(ns cdq.graphics.camera
  (:require [gdl.graphics.camera :as cam]))

(defn set-on-player-position [{:keys [gdl.context/world-viewport
                                      cdq.context/player-eid]
                               :as context}]
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  context)
