(ns cdq.render.when-not-paused.update-time
  (:require [cdq.graphics :as graphics]
            cdq.time))

(defn render [context]
  (let [delta-ms (min (graphics/delta-time)
                      cdq.time/max-delta)]
    (-> context
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))
