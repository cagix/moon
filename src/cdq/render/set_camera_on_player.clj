(ns cdq.render.set-camera-on-player
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/player-eid]}]
  (graphics/set-camera-position! graphics
                                 (:body/position (:entity/body @player-eid))))
