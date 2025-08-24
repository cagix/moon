(ns cdq.render.set-camera-on-player
  (:require [cdq.entity :as entity]
            [cdq.graphics.camera :as camera]))

(defn do! [{:keys [ctx/world
                   ctx/graphics]
            :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (entity/position @(:world/player-eid world)))
  ctx)
