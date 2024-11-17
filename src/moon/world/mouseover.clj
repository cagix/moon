(ns moon.world.mouseover
  (:refer-clojure :exclude [update])
  (:require [gdl.graphics.world-view :as world-view]
            [gdl.stage :as stage]
            [gdl.utils :refer [sort-by-order]]
            [moon.body :as body]
            [moon.player :as player]
            [moon.world.grid :as grid]
            [moon.world.line-of-sight :refer [line-of-sight?]]))

(def eid nil)

(defn entity []
  (and eid @eid))

(defn- calculate-eid []
  (let [player @player/eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (grid/point->entities
                      (world-view/mouse-position)))]
    (->> body/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn update []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when eid
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'eid new-eid)
    nil))
