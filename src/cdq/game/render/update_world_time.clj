(ns cdq.game.render.update-world-time
  (:require [clojure.gdx :as gdx]))

(defn- update-world-time* [{:keys [world/max-delta]
                           :as world}
                          delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn step
  [{:keys [ctx/gdx
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world update-world-time* (gdx/delta-time gdx))))
