(ns forge.world.mouseover-entity
  (:require [forge.app.world-viewport :refer [world-mouse-position]]
            [forge.screens.stage :refer [mouse-on-actor?]]
            [forge.world :refer [render-z-order
                                 line-of-sight?]]
            [forge.world.grid :refer [point->entities]]
            [forge.world.player :refer [player-eid]]
            [forge.utils :refer [bind-root sort-by-order]]))

(def mouseover-eid nil)

(defn mouseover-entity []
  (and mouseover-eid
       @mouseover-eid))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn frame-tick []
  (let [new-eid (if (mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))
