(ns cdq.game.render.update-world-time
  (:require [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn step
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world world/update-time (graphics/delta-time graphics))))
