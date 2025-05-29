(ns cdq.render.update-time
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/paused?]
            :as ctx}]
  (if paused?
    ctx
    (let [delta-ms (min (graphics/delta-time graphics)
                        ctx/max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
