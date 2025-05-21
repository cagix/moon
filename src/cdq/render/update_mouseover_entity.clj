(ns cdq.render.update-mouseover-entity
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.utils :refer [sort-by-order]]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage
                   ctx/ui-viewport
                   ctx/player-eid
                   ctx/grid
                   ctx/world-viewport
                   ctx/mouseover-eid]
            :as ctx}]
  (let [new-eid (if (ui/hit stage (viewport/mouse-position ui-viewport))
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid
                                                           (viewport/mouse-position world-viewport)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(entity/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))
