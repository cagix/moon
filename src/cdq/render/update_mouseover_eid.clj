(ns cdq.render.update-mouseover-eid
  (:require [cdq.world.grid :as grid]
            [cdq.raycaster :as raycaster]
            [clojure.utils :as utils]))

(defn do!
  [{:keys [ctx/mouseover-actor
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (let [{:keys [world/grid
                world/raycaster
                world/mouseover-eid
                world/player-eid]} world
        new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid world-mouse-position))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(raycaster/line-of-sight? raycaster player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))
