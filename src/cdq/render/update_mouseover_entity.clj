(ns cdq.render.update-mouseover-entity
  (:require cdq.grid
            [cdq.line-of-sight :as los]
            [cdq.stage :as stage]
            clojure.utils
            cdq.world))

(defn render [{:keys [cdq.context/grid
                      cdq.context/mouseover-eid
                      cdq.context/player-eid
                      cdq.graphics/world-viewport
                      cdq.context/stage] :as c}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (cdq.graphics/world-mouse-position world-viewport)))]
                    (->> cdq.world/render-z-order
                         (clojure.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))
