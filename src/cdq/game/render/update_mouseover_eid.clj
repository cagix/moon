(ns cdq.game.render.update-mouseover-eid
  (:require [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.utils :as utils]))

(defn- get-mouseover-entity
  [{:keys [world/grid
           world/mouseover-eid
           world/player-eid
           world/render-z-order]
    :as world}
   position]
  (let [player @player-eid
        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                     (grid/point->entities grid position))]
    (->> render-z-order
         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
         reverse
         (filter #(raycaster/line-of-sight? world player @%))
         first)))

(defn step
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))
        mouseover-eid (:world/mouseover-eid world)
        new-eid (if mouseover-actor
                  nil
                  (get-mouseover-entity world (:graphics/world-mouse-position graphics)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))
