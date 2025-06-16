(ns cdq.render.update-time
  (:require [gdl.graphics :as g]))

(defn do! [{:keys [ctx/graphics
                   ctx/world]
            :as ctx}]
  (let [delta-ms (min (g/delta-time graphics) (:world/max-delta world))]
    (-> ctx
        (assoc-in [:ctx/world :world/delta-time] delta-ms)
        (update :ctx/elapsed-time + delta-ms))))
