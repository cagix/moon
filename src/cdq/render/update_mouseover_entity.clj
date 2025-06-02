(ns cdq.render.update-mouseover-entity
  (:require [cdq.grid :as grid]
            [clojure.ctx :as ctx]
            [clojure.utils :as utils]))

(defn do! [{:keys [ctx/player-eid
                   ctx/mouseover-eid
                   ctx/grid
                   ctx/render-z-order]
            :as ctx}]
  (let [new-eid (if (ctx/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (ctx/world-mouse-position ctx)))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(ctx/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))
