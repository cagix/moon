(ns cdq.render.update-mouseover-entity
  (:require [cdq.ctx :as ctx]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.utils :refer [sort-by-order]]
            [gdl.ui :as ui]))

(defn do! [{:keys [ctx/player-eid
                   ctx/grid
                   ctx/mouseover-eid]
            :as ctx}]
  (let [new-eid (if (g/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (g/world-mouse-position ctx)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(g/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))
