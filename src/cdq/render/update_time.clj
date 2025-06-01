(ns cdq.render.update-time
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as g]))

(defn do! [{:keys [ctx/paused?]
            :as ctx}]
  (if paused?
    ctx
    (let [delta-ms (min (g/delta-time ctx) ctx/max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
