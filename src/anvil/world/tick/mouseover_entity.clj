(ns anvil.world.tick.mouseover-entity
  (:require [anvil.world.tick :as tick]
            [cdq.context :as world :refer [mouseover-eid line-of-sight?]]
            [gdl.context :as c]
            [gdl.stage :as stage]))

(defn- calculate-eid [c]
  (let [player @world/player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (world/point->entities c (c/world-mouse-position c)))]
    (->> world/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn- update-mouseover-entity [c]
  (let [new-eid (if (stage/mouse-on-actor? c)
                  nil
                  (calculate-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defn-impl tick/mouseover-entity [c]
  (update-mouseover-entity c))
