(ns cdq.render.update-mouseover-eid
  (:require [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.world.grid :as grid]))

(defn do!
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/player-eid
           ctx/render-z-order
           ctx/raycaster
           ctx/grid
           ctx/world-mouse-position]
    :as ctx}]
  (let [new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid world-mouse-position))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(raycaster/line-of-sight? raycaster player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))
