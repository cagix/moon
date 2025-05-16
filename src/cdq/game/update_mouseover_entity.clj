(ns cdq.game.update-mouseover-entity
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.stage :as stage]
            [cdq.utils :as utils]
            [cdq.world.grid :as grid]
            [gdl.graphics.viewport :as viewport]))

(defn do! []
  (let [new-eid (if (stage/mouse-on-actor? ctx/stage)
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities ctx/grid
                                                           (viewport/mouse-position ctx/world-viewport)))]
                    (->> ctx/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(entity/line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (utils/bind-root #'ctx/mouseover-eid new-eid)))
