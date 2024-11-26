(ns moon.world.mouseover
  (:refer-clojure :exclude [update])
  (:require [forge.utils :refer [sort-by-order]]
            [forge.graphics :refer [mouse-on-actor? world-mouse-position]]
            [moon.world :as world :refer [player-eid line-of-sight?]]))

(def eid nil)

(defn entity []
  (and eid @eid))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (world/point->entities
                      (world-mouse-position)))]
    (->> world/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn update []
  (let [new-eid (if (mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when eid
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (.bindRoot #'eid new-eid)
    nil))
