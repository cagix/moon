(ns cdq.render.update-time
  (:require [cdq.ctx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/max-delta]
    :as ctx}]
  (if (:ctx/paused? ctx)
    ctx
    (let [delta-ms (min (graphics/delta-time graphics) max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update-in [:ctx/world :world/elapsed-time] + delta-ms)))))
