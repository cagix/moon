(ns cdq.application.render.update-mouseover-entity
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.utils :refer [bind-root
                               sort-by-order]]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]))

(defn do! []
  (let [new-eid (if (ui/hit ctx/stage (viewport/mouse-position ctx/ui-viewport))
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities ctx/grid
                                                           (viewport/mouse-position ctx/world-viewport)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(entity/line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'ctx/mouseover-eid new-eid)))
