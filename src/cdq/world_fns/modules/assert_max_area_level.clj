(ns cdq.world-fns.modules.assert-max-area-level)

(defn do!
  [{:keys [world/map-size
           world/max-area-level]
    :as world-fn-ctx}]
  (assert (<= max-area-level map-size))
  world-fn-ctx)
