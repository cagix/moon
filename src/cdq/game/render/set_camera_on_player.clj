(ns cdq.game.render.set-camera-on-player
  (:require [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn step
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-position! graphics (world/player-position world))
  ctx)
