(ns cdq.render.update-time
  (:require [clojure.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics
           ctx/max-delta]
    :as ctx}]
  (if (:ctx/paused? ctx)
    ctx
    (let [delta-ms (min (graphics/delta-time graphics) max-delta)]
      (-> ctx
          (assoc :ctx/delta-time delta-ms)
          (update :ctx/elapsed-time + delta-ms)))))
