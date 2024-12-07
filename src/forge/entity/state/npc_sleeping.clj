(ns forge.entity.state.npc-sleeping
  (:require [clojure.utils :refer [defmethods]])
  )

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (state-exit [[_ {:keys [eid]}]]
    (delayed-alert (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (swap! eid string-effect/add "[WHITE]!"))

  (e-tick [_ eid]
    (let [entity @eid
          cell (get world-grid (e-tile entity))] ; pattern!
      (when-let [distance (nearest-entity-distance @cell (e-enemy entity))]
        (when (<= distance (stat/->value entity :entity/aggro-range))
          (send-event eid :alert)))))

  (render-above [_ entity]
    (let [[x y] (:position entity)]
      (draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
