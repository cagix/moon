(ns cdq.render.set-camera-on-player
  (:require [cdq.graphics.camera :as camera]))

(defn do!
  [{:keys [ctx/player-eid
           ctx/world-viewport]}]
  (camera/set-position! (:viewport/camera world-viewport)
                        (:body/position (:entity/body @player-eid))))
