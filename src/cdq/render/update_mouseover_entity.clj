(ns cdq.render.update-mouseover-entity
  (:require [cdq.grid :as grid]
            [cdq.utils :as utils]
            [cdq.w :as w]
            [gdl.c :as c]))

(defn do! [{:keys [ctx/world]
            :as ctx}]
  (let [new-eid (if (c/mouseover-actor ctx)
                  nil
                  (let [player @(:world/player-eid world)
                        hits (remove #(= (:body/z-order @%) :z-order/effect)
                                     (grid/point->entities (:world/grid world) (c/world-mouse-position ctx)))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order @%))
                         reverse
                         (filter #(w/line-of-sight? world player @%))
                         first)))]
    (when-let [eid (:world/mouseover-eid world)]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))
