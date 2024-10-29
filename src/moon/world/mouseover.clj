(ns moon.world.mouseover
  (:refer-clojure :exclude [update])
  (:require [gdl.utils :refer [sort-by-order]]
            [moon.body :as body]
            [moon.entity.player :as player]
            [moon.graphics.world-view :as world-view]
            [moon.stage :as stage]
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
    (->> body/render-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn update []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    [(when eid
       [:e/dissoc eid :entity/mouseover?])
     (when new-eid
       [:e/assoc new-eid :entity/mouseover? true])
     (fn []
       (bind-root #'eid new-eid)
       nil)]))
