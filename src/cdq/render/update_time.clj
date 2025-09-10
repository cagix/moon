(ns cdq.render.update-time
  (:require [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/max-delta]
    :as ctx}]
  (if (:ctx/paused? ctx)
    ctx
    (let [delta-ms (min (graphics/delta-time ctx) max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
