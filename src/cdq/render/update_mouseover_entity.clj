(ns cdq.render.update-mouseover-entity
  (:require cdq.grid
            cdq.stage
            cdq.utils
            cdq.world))

(defn render [{:keys [cdq.context/grid
                      cdq.context/mouseover-eid
                      cdq.context/player-eid] :as c}]
  (let [new-eid (if (cdq.stage/mouse-on-actor? c)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (cdq.graphics/world-mouse-position c)))]
                    (->> cdq.world/render-z-order
                         (cdq.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(cdq.world/line-of-sight? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))
