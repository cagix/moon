(ns cdq.application.render.update-world-time
  (:require [cdq.graphics :as graphics]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world world/update-time (graphics/delta-time graphics))))
