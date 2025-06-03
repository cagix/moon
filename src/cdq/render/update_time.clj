(ns cdq.render.update-time
  (:require [gdl.graphics :as g]))

(defn do! [{:keys [ctx/graphics
                   ctx/paused?
                   ctx/max-delta]
            :as ctx}]
  (if paused?
    ctx
    (let [delta-ms (min (g/delta-time graphics) max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
