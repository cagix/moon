(ns cdq.render.set-camera-on-player
  (:require [cdq.entity :as entity]
            [gdl.graphics.camera :as camera]))

(defn do! [{:keys [ctx/player-eid
                   ctx/graphics]
            :as ctx}]
  (camera/set-position! (:camera (:world-viewport graphics))
                        (entity/position @player-eid))
  ctx)
