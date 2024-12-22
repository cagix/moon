(ns anvil.world.tick.mouseover-entity
  (:require [anvil.world.tick :as tick]
            [anvil.world :as world :refer [mouseover-eid line-of-sight?]]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.utils :refer [bind-root sort-by-order defn-impl]]))

(defn- calculate-eid []
  (let [player @world/player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (world/point->entities
                      (g/world-mouse-position)))]
    (->> world/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defn-impl tick/mouseover-entity []
  (update-mouseover-entity))
