(ns cdq.render.update-time
  (:require [cdq.graphics :as g]))

(defn do! [{:keys [ctx/paused?
                   ctx/max-delta]
            :as ctx}]
  (if paused?
    ctx
    (let [delta-ms (min (g/delta-time ctx) max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
