(ns cdq.render.when-not-paused.update-time
  (:require cdq.time
            clojure.gdx.graphics))

(defn render [context]
  (let [delta-ms (min (clojure.gdx.graphics/delta-time)
                      cdq.time/max-delta)]
    (-> context
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))
