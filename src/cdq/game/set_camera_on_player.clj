(ns cdq.game.set-camera-on-player
  (:require [cdq.gdx.graphics.camera :as camera]))

(defn do!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (:body/position (:entity/body @player-eid)))
  ctx)
