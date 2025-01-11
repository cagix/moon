(ns cdq.render
  (:require [gdl.graphics.camera :as cam]))

(defn set-camera-on-player! [{:keys [context/g
                                     cdq.context/player-eid]
                               :as context}]
  (cam/set-position! g (:position @player-eid))
  context)
