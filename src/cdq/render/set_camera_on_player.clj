(ns cdq.render.set-camera-on-player
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (graphics/set-camera-position! ctx
                                 (:body/position (:entity/body @player-eid))))
