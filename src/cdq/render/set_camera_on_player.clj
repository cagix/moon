(ns cdq.render.set-camera-on-player
  (:require [cdq.entity :as entity]
            [clojure.gdx.graphics.camera :as camera]))

(defn do! [{:keys [ctx/graphics
                   ctx/player-eid]}]
  (camera/set-position! (:camera (:world-viewport graphics))
                        (entity/position @player-eid)))
