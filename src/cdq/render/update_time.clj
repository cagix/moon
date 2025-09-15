(ns cdq.render.update-time
  (:require [cdq.ctx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:ctx/paused? ctx)
    ctx
    (let [delta-ms (min (graphics/delta-time graphics) (:world/max-delta world))]
      (-> ctx
          (assoc-in [:ctx/world :world/delta-time] delta-ms)
          (update-in [:ctx/world :world/elapsed-time] + delta-ms)))))
