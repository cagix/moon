(ns cdq.game.update-mouseover-eid
  (:require [cdq.ctx.world :as world]
            [cdq.utils :as utils]
            [cdq.world.grid :as grid]))

(defn do!
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/player-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (let [new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities (:world/grid world) world-mouse-position))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))
